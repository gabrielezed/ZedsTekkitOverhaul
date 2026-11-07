package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.Material;
import net.minecraft.src.TileEntity;
import zed.tekkitoverhaul.render.TileEntityFoundryVisual;

/** Placeable Foundry test block with a visual-only tile entity. */
public final class BlockFoundry extends BlockContainer {
    public BlockFoundry(int id) {
        super(id, Block.stoneOvenIdle.blockIndexInTexture, Material.rock);
        setHardness(3.5F);
        setResistance(8.0F);
        setStepSound(soundStoneFootstep);
        setBlockName("ztoFoundry");
    }

    @Override
    public TileEntity getBlockEntity() {
        return new TileEntityFoundryVisual();
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        return Block.stoneOvenIdle.getBlockTextureFromSide(side);
    }
}
