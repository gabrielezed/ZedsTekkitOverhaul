package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.*;

final class BlockAirCompressor extends BlockContainer {
    private static final int[] FRONT_BY_YAW = {2, 5, 3, 4};
    private static final int ITEM_FRONT = 3;
    private final int cap;
    private final int grille;

    BlockAirCompressor(int id) {
        super(id, Material.iron);
        setHardness(3.0F);
        setResistance(8.0F);
        setStepSound(soundMetalFootstep);
        setBlockName("ztoAirCompressor");
        blockIndexInTexture = UnderwaterRegistry.sprite("/terrain.png", "compressor_side");
        cap = UnderwaterRegistry.sprite("/terrain.png", "compressor_cap");
        grille = UnderwaterRegistry.sprite("/terrain.png", "compressor_grille");
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        return getBlockTextureFromSideAndMetadata(side, 0);
    }

    @Override
    public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
        if (side < 2) return cap;
        int front = metadata >= 2 && metadata <= 5 ? metadata : ITEM_FRONT;
        return side == front ? grille : blockIndexInTexture;
    }

    @Override
    public int getBlockTexture(IBlockAccess world, int x, int y, int z, int side) {
        int metadata = world.getBlockMetadata(x, y, z);
        // Existing compressors were saved without facing and showed the grille on both Z faces.
        if (metadata == 0 && side == 2) return grille;
        return getBlockTextureFromSideAndMetadata(side, metadata);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer) {
        int quadrant = MathHelper.floor_double(placer.rotationYaw * 4F / 360F + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, FRONT_BY_YAW[quadrant]);
    }

    @Override
    public TileEntity getBlockEntity() { return new TileAirCompressor(); }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (player.isSneaking() && player.getCurrentEquippedItem() != null
                && player.getCurrentEquippedItem().getItem() instanceof ItemBlock) return false;
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (!(tile instanceof TileAirCompressor)) return false;
        if (!world.isRemote) ModLoader.openGUI(player, new GuiAirCompressor(player.inventory, (TileAirCompressor) tile));
        return true;
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (!world.isRemote && tile instanceof TileAirCompressor) {
            ItemStack stack = ((TileAirCompressor) tile).getStackInSlotOnClosing(0);
            if (stack != null) world.spawnEntityInWorld(new EntityItem(world, x + .5, y + .5, z + .5, stack));
        }
        super.onBlockRemoval(world, x, y, z);
    }
}
