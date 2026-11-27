package zed.tekkitoverhaul.survival;

import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityList;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Facing;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import net.minecraft.src.Vec3D;
import net.minecraft.src.MathHelper;
import net.minecraft.src.IMob;
import zed.tekkitoverhaul.exploration.abyssal.guardian.AquaticDevelopment;

public class ItemSurvivalSpawnEgg extends Item {
    private final String entityName;
    private final int shell, spots;

    public ItemSurvivalSpawnEgg(int id, String entityName, String key, int shell, int spots) {
        super(id);
        this.entityName = entityName;
        this.shell = shell;
        this.spots = spots;
        setItemName(key);
        setIconIndex(Item.monsterPlacer.getIconFromDamage(0));
    }

    @Override
    public boolean requiresMultipleRenderPasses() { return true; }

    @Override
    public int func_46057_a(int damage, int pass) {
        return getIconFromDamage(damage) + (pass == 0 ? 0 : 16);
    }

    @Override
    public int getColorFromDamage(int damage, int pass) { return pass == 0 ? shell : spots; }

    protected String entityName(int damage) { return entityName; }
    protected boolean aquatic(int damage) { return false; }
    protected boolean placementAllowed(ItemStack stack, EntityPlayer player, World world) { return true; }

    public boolean validVariant(int damage) { return entityName(damage) != null; }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
        if (world.isRemote) return true;
        if (stack.stackSize < 1 || !world.blockExists(x, y, z) || !placementAllowed(stack, player, world)) return false;
        int clicked = world.getBlockId(x, y, z);
        x += Facing.offsetsXForSide[side];
        y += Facing.offsetsYForSide[side];
        z += Facing.offsetsZForSide[side];
        double offset = side == 1 && (clicked == Block.fence.blockID || clicked == Block.netherFence.blockID)
                ? 0.5D : 0.0D;
        return insert(stack, player, world, x + 0.5, y + offset, z + 0.5);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || stack.stackSize < 1 || !aquatic(stack.getItemDamage())
                || !placementAllowed(stack, player, world)) return stack;
        Vec3D look = player.getLook(1);
        double y = player.posY + player.getEyeHeight();
        for (int range = 3; range <= 7; range++) {
            double x = player.posX + look.xCoord * range, z = player.posZ + look.zCoord * range;
            double aim = y + look.yCoord * range;
            if (!world.checkChunksExist(MathHelper.floor_double(Math.min(player.posX, x)), 0,
                    MathHelper.floor_double(Math.min(player.posZ, z)), MathHelper.floor_double(Math.max(player.posX, x)),
                    world.getHeight() - 1, MathHelper.floor_double(Math.max(player.posZ, z)))) break;
            if (world.rayTraceBlocks_do_do(Vec3D.createVector(player.posX, y, player.posZ),
                    Vec3D.createVector(x, aim, z), false, true) != null) break;
            if (insert(stack, player, world, x, aim - 0.9, z)) return stack;
        }
        player.addChatMessage("Aim at deeper open water with room for the creature.");
        return stack;
    }

    private boolean insert(ItemStack stack, EntityPlayer player, World world, double x, double y, double z) {
        int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
        if (!world.blockExists(bx, by, bz) || !player.canPlayerEdit(bx, by, bz)) return false;
        String name = entityName(stack.getItemDamage());
        if (name == null) return false;
        Entity entity = EntityList.createEntityByName(name, world);
        if (!(entity instanceof EntityLiving) || world.difficultySetting == 0 && entity instanceof IMob) return false;
        EntityLiving mob = (EntityLiving) entity;
        mob.setLocationAndAngles(x, y, z, world.rand.nextFloat() * 360.0F, 0);
        if (mob.boundingBox.intersectsWith(player.boundingBox) || !isSpawnClear(mob, world)) return false;
        if (aquatic(stack.getItemDamage())) {
            if (!AquaticDevelopment.insertManual(mob)) return false;
        } else if (!world.spawnEntityInWorld(mob)) return false;
        mob.playLivingSound();
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }

    protected boolean isSpawnClear(EntityLiving mob, World world) {
        return mob.boundingBox.minY >= 0 && mob.boundingBox.maxY <= world.getHeight()
                && world.checkChunksExist(MathHelper.floor_double(mob.boundingBox.minX), 0,
                        MathHelper.floor_double(mob.boundingBox.minZ), MathHelper.floor_double(mob.boundingBox.maxX),
                        world.getHeight() - 1, MathHelper.floor_double(mob.boundingBox.maxZ))
                && world.checkIfAABBIsClear(mob.boundingBox)
                && world.getCollidingBoundingBoxes(mob, mob.boundingBox).isEmpty();
    }
}
