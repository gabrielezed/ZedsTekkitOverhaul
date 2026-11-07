package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import zed.tekkitoverhaul.render.TileEntityFoundryVisual;

/** Foundry prototype: rotor control and provisional 3x3 Stone ring detection. */
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

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
        super.onNeighborBlockChange(world, x, y, z, neighborId);
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (tile instanceof TileEntityFoundryVisual) {
            ((TileEntityFoundryVisual)tile).invalidateStructure();
        }
        // Diagonal block changes are handled by the tile's periodic recheck.
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (!world.isRemote) {
            TileEntity tile = world.getBlockTileEntity(x, y, z);
            if (tile instanceof TileEntityFoundryVisual) {
                TileEntityFoundryVisual foundry = (TileEntityFoundryVisual)tile;
                foundry.checkStructureNow();
                foundry.toggleRotor();
                if (foundry.isStructureFormed()) {
                    player.addChatMessage("[ZTO] Foundry structure: FORMED (8/8 Stone)");
                } else {
                    player.addChatMessage("[ZTO] Foundry structure: INCOMPLETE ("
                            + foundry.getMissingStoneCount() + " Stone missing)");
                }
            }
        }
        return true;
    }
}
