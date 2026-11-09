package zed.tekkitoverhaul.hopper;

import java.util.ArrayList;
import net.minecraft.src.*;

public final class BlockHopper extends BlockContainer {
    public BlockHopper(int id) {
        super(id, Material.iron);
        setHardness(3.0F); setResistance(8.0F); setStepSound(soundMetalFootstep);
        setBlockName("ztoHopper");
    }
    public TileEntity getBlockEntity() { return new TileHopper(); }
    public boolean isOpaqueCube() { return false; }
    public boolean renderAsNormalBlock() { return false; }
    public int getRenderType() { return HopperRegistry.model; }
    public int getBlockTextureFromSide(int side) { return side == 1 ? HopperRegistry.top : HopperRegistry.outer; }
    public void onBlockPlaced(World world, int x, int y, int z, int side) {
        int face = side ^ 1;
        world.setBlockMetadataWithNotify(x, y, z, face == 1 ? 0 : face);
    }
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (player.isSneaking() && player.getCurrentEquippedItem() != null
                && player.getCurrentEquippedItem().getItem() instanceof ItemBlock) return false;
        if (!world.isRemote) {
            TileEntity tile = world.getBlockTileEntity(x,y,z);
            if (tile instanceof TileHopper) ModLoader.openGUI(player, new GuiHopper(player.inventory, (TileHopper) tile));
        }
        return true;
    }
    public void onBlockRemoval(World world, int x, int y, int z) {
        TileEntity tile = world.getBlockTileEntity(x,y,z);
        if (!world.isRemote && tile instanceof TileHopper) {
            TileHopper hopper = (TileHopper) tile;
            for (int slot = 0; slot < 5; slot++) {
                ItemStack stack = hopper.getStackInSlotOnClosing(slot);
                if (stack != null) world.spawnEntityInWorld(new EntityItem(world, x + .5, y + .5, z + .5, stack));
            }
        }
        super.onBlockRemoval(world,x,y,z);
    }
    public void getCollidingBoundingBoxes(World world, int x, int y, int z, AxisAlignedBB mask, ArrayList boxes) {
        float[][] bounds = {{0,0,0,1,.625F,1},{0,0,0,.125F,1,1},
            {.875F,0,0,1,1,1},{0,0,0,1,1,.125F},{0,0,.875F,1,1,1}};
        for (int i=0;i<bounds.length;i++) {
            float[] b=bounds[i]; setBlockBounds(b[0],b[1],b[2],b[3],b[4],b[5]);
            super.getCollidingBoundingBoxes(world,x,y,z,mask,boxes);
        }
        setBlockBounds(0,0,0,1,1,1);
    }
}
