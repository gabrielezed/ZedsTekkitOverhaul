package zed.tekkitoverhaul.survival.tactical;

import java.io.File;
import java.util.UUID;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.WorldSavedData;

/** Single custody for the local Overworld. A Thief's entity tag references this stack by token. */
public final class ThiefTrail extends WorldSavedData {
    private static final String KEY = "zto_thief_trail";
    private ItemStack loot;
    private String token = "", victim = "";
    private int x, y, z;
    private int originX, originY, originZ;
    private boolean cached;
    private boolean valid;

    public ThiefTrail(String key) { super(key); }

    public static boolean local(World world) {
        return world != null && !world.isRemote && world.worldProvider.worldType == 0;
    }

    static ThiefTrail get(World world, boolean create) {
        if (!local(world)) return null;
        ThiefTrail trail = (ThiefTrail) world.loadItemData(ThiefTrail.class, KEY);
        if (trail != null) return trail.valid ? trail : null;
        if (!create) return null;
        File file = world.getSaveHandler().getMapFileFromName(KEY);
        if (file == null || file.exists()) return null;
        trail = new ThiefTrail(KEY);
        trail.valid = true;
        world.setItemData(KEY, trail);
        return trail;
    }

    boolean occupied() { return loot != null; }

    boolean carries(String candidate) { return loot != null && !cached && token.equals(candidate); }

    String claim(EntityThief thief, EntityPlayer player, int slot) {
        if (occupied()) return "";
        String next = UUID.randomUUID().toString();
        ItemStack removed = player.inventory.decrStackSize(slot, 1);
        if (removed == null) return "";
        loot = removed;
        token = next;
        victim = player.username;
        locate(thief);
        originX = MathHelper.floor_double(player.posX);
        originY = MathHelper.floor_double(player.boundingBox.minY);
        originZ = MathHelper.floor_double(player.posZ);
        markDirty();
        return token;
    }

    String description() {
        try {
            return loot.getItem().getItemDisplayName(loot);
        } catch (RuntimeException invalidName) {
            return "item #" + loot.itemID;
        }
    }

    void locate(EntityThief thief) {
        int nextX = MathHelper.floor_double(thief.posX), nextZ = MathHelper.floor_double(thief.posZ);
        int nextY = MathHelper.floor_double(thief.boundingBox.minY);
        if (nextY < 1 || nextY >= thief.worldObj.getHeight() || !thief.worldObj.blockExists(nextX, nextY, nextZ)) return;
        if (x == nextX && y == nextY && z == nextZ) return;
        x = nextX;
        y = nextY;
        z = nextZ;
        markDirty();
    }

    void release(EntityThief thief, String carrier) {
        if (!carries(carrier)) return;
        World world = thief.worldObj;
        locate(thief);
        EntityPlayer player = world.getClosestPlayerToEntity(thief, -1);
        if (drop(world, x, y, z)) {
            if (samePlayer(player)) player.addChatMessage("The Thief dropped your item near " + coordinates() + ".");
        } else {
            cached = true;
            x = originX;
            y = originY;
            z = originZ;
            markDirty();
            if (samePlayer(player)) player.addChatMessage("Your stolen item is cached. Follow its trail with a compass.");
        }
    }

    public static ChunkCoordinates inspect(World world, EntityPlayer player) {
        ThiefTrail trail = get(world, false);
        if (trail == null || !trail.occupied() || !trail.samePlayer(player)) {
            player.addChatMessage("The compass finds no Enderkin trail.");
            return null;
        }
        if (player.getDistanceSq(trail.x + 0.5, trail.y, trail.z + 0.5) <= 36
                && (trail.cached || trail.missingCarrier(world))) {
            if (trail.drop(world, MathHelper.floor_double(player.posX),
                    MathHelper.floor_double(player.boundingBox.minY + 0.01), MathHelper.floor_double(player.posZ))) {
                player.addChatMessage("You recovered the Thief's cache.");
                return null;
            }
        }
        player.addChatMessage((trail.cached ? "Thief cache near " : "Enderkin trail last seen near ")
                + trail.coordinates() + ".");
        return new ChunkCoordinates(trail.x, trail.y, trail.z);
    }

    private boolean samePlayer(EntityPlayer player) { return player != null && victim.equals(player.username); }

    private String coordinates() { return x + ", " + y + ", " + z; }

    private boolean missingCarrier(World world) {
        if (!world.checkChunksExist(x - 8, 0, z - 8, x + 8, 0, z + 8)) return false;
        AxisAlignedBB area = AxisAlignedBB.getBoundingBoxFromPool(x - 8, Math.max(0, y - 8), z - 8,
                x + 9, y + 9, z + 9);
        for (Object entity : world.getEntitiesWithinAABB(EntityThief.class, area))
            if (((EntityThief) entity).carries(token)) return false;
        return true;
    }

    private boolean drop(World world, int bx, int by, int bz) {
        for (int dy : new int[] {0, 1, -1, 2, -2}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int px = bx + dx, py = by + dy, pz = bz + dz;
                    if (py < 1 || py + 1 >= world.getHeight() || !world.blockExists(px, py, pz)
                            || world.getBlockId(px, py, pz) != 0 || world.getBlockId(px, py + 1, pz) != 0
                            || !world.isBlockNormalCube(px, py - 1, pz)) continue;
                    EntityItem item = new EntityItem(world, px + 0.5, py + 0.1, pz + 0.5, loot);
                    item.motionX = item.motionY = item.motionZ = 0;
                    item.delayBeforeCanPickup = 10;
                    if (!world.spawnEntityInWorld(item)) continue;
                    x = px;
                    y = py;
                    z = pz;
                    loot = null;
                    token = victim = "";
                    cached = false;
                    markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    static boolean validItem(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.itemID >= 0
                && stack.itemID < Item.itemsList.length && stack.getItem() != null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        valid = tag.getInteger("Version") == 1;
        if (!valid || !tag.hasKey("Loot")) return;
        token = tag.getString("Token");
        victim = tag.getString("Victim");
        x = tag.getInteger("X");
        y = tag.getInteger("Y");
        z = tag.getInteger("Z");
        originX = tag.getInteger("OriginX");
        originY = tag.getInteger("OriginY");
        originZ = tag.getInteger("OriginZ");
        cached = tag.getBoolean("Cached");
        NBTTagCompound contents = tag.getCompoundTag("Loot");
        int id = contents.getShort("id");
        loot = null;
        if (id >= 0 && id < Item.itemsList.length) loot = ItemStack.loadItemStackFromNBT(contents);
        valid = token.length() == 36 && validItem(loot) && loot.stackSize == 1
                && x >= -30000000 && x < 30000000 && z >= -30000000 && z < 30000000 && y > 0 && y < 256;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Version", 1);
        if (!occupied()) return;
        tag.setString("Token", token);
        tag.setString("Victim", victim);
        tag.setInteger("X", x);
        tag.setInteger("Y", y);
        tag.setInteger("Z", z);
        tag.setInteger("OriginX", originX);
        tag.setInteger("OriginY", originY);
        tag.setInteger("OriginZ", originZ);
        tag.setBoolean("Cached", cached);
        tag.setCompoundTag("Loot", loot.writeToNBT(new NBTTagCompound()));
    }
}
