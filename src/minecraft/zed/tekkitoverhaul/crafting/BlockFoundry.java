package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Block;
import net.minecraft.src.Material;

/** Test block with no TileEntity or machine behavior. */
public final class BlockFoundry extends Block {
    public BlockFoundry(int id) {
        super(id, Block.stoneOvenIdle.blockIndexInTexture, Material.rock);
        setHardness(3.5F);
        setResistance(8.0F);
        setStepSound(soundStoneFootstep);
        setBlockName("ztoFoundry");
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        return Block.stoneOvenIdle.getBlockTextureFromSide(side);
    }
}
