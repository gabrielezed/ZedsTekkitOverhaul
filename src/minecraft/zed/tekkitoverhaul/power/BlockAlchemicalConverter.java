package zed.tekkitoverhaul.power;

import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

final class BlockAlchemicalConverter extends BlockContainer {
    BlockAlchemicalConverter(int id) {
        super(id, Material.iron);
        setBlockName("ztoAlchemicalConverter");
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundMetalFootstep);
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        if (side == 1) return Block.glowStone.getBlockTextureFromSide(side);
        if (side == 0) return Block.blockSteel.getBlockTextureFromSide(side);
        return Block.stoneOvenIdle.getBlockTextureFromSide(3);
    }

    @Override
    public TileEntity getBlockEntity() {
        return new TileAlchemicalConverter();
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (player.inventory.getCurrentItem() != null) return false;
        if (!world.isRemote) {
            TileEntity tile = world.getBlockTileEntity(x, y, z);
            if (tile instanceof TileAlchemicalConverter) {
                ModLoader.openGUI(player, new GuiAlchemicalConverter((TileAlchemicalConverter) tile));
            }
        }
        return true;
    }
}
