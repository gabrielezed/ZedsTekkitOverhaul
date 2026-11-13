package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.EntityList;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Facing;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

final class ItemTacticalSpawnEgg extends Item {
    private final String entityName;
    private final int shell, spots;

    ItemTacticalSpawnEgg(int id, String entityName, String key, int shell, int spots) {
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

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
        if (world.isRemote) return true;
        if (world.difficultySetting == 0 || stack.stackSize < 1) return false;
        int clicked = world.getBlockId(x, y, z);
        x += Facing.offsetsXForSide[side];
        y += Facing.offsetsYForSide[side];
        z += Facing.offsetsZForSide[side];
        if (y < 0 || y >= world.getHeight() || !world.blockExists(x, y, z)
                || !player.canPlayerEdit(x, y, z)) return false;
        double offset = side == 1 && (clicked == Block.fence.blockID || clicked == Block.netherFence.blockID)
                ? 0.5D : 0.0D;
        TacticalHostile mob = (TacticalHostile) EntityList.createEntityByName(entityName, world);
        if (mob == null) return false;
        mob.setLocationAndAngles(x + 0.5D, y + offset, z + 0.5D, world.rand.nextFloat() * 360.0F, 0);
        if (mob.boundingBox.maxY > world.getHeight() || !world.checkIfAABBIsClear(mob.boundingBox)
                || !world.getCollidingBoundingBoxes(mob, mob.boundingBox).isEmpty()) return false;
        if (!world.spawnEntityInWorld(mob)) return false;
        mob.playLivingSound();
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }
}
