package zed.tekkitoverhaul.assault;

import net.minecraft.src.Block;
import net.minecraft.src.World;

/** Adapts a single placement's local, already loaded world geometry. */
final class LoadedSurface implements OutdoorApproach.Surface {
    private final World world;

    LoadedSurface(World world) {
        this.world = world;
    }

    @Override
    public int feet(int x, int z) {
        if (x < -29999999 || x >= 29999999 || z < -29999999 || z >= 29999999
                || !world.checkChunksExist(x - 1, 0, z - 1, x + 1, 255, z + 1)) return -1;
        int y = world.getHeightValue(x, z);
        return y >= 1 && y <= 253 && world.isBlockSolidOnSide(x, y - 1, z, 1) ? y : -1;
    }

    @Override
    public boolean clear(int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        if (id == 0) return true;
        Block block = Block.blocksList[id];
        return block != null && !block.blockMaterial.isLiquid() && block.getBlocksMovement(world, x, y, z);
    }
}
