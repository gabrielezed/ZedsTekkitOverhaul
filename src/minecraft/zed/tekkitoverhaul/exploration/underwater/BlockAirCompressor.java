package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.*;

final class BlockAirCompressor extends BlockContainer {
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
        if (side < 2) return cap;
        return side == 2 || side == 3 ? grille : blockIndexInTexture;
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
