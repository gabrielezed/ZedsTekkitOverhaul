package zed.tekkitoverhaul.hopper;

import net.minecraft.src.*;

/** Flat official item sprite, independent of the placed block's terrain atlas. */
public final class ItemHopper extends ItemBlock {
    public ItemHopper(int id) { super(id); setIconIndex(HopperRegistry.itemIcon); }
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world,
                                  int x, int y, int z, int side) {
        if (world.isRemote || !player.isSneaking()) return false;
        int count = stack.stackSize;
        if (onItemUse(stack, player, world, x, y, z, side)
                && player.capabilities.isCreativeMode) stack.stackSize = count;
        return true;
    }

    public String getTextureFile() { return "/gui/items.png"; }
}
