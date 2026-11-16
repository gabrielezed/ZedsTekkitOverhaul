package zed.tekkitoverhaul.integration;

import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.ChunkProvider;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/** A loaded local tile lookup which cannot instantiate missing tiles. */
public final class LoadedTile {
    private LoadedTile() {}

    public static TileEntity at(World world, int x, int y, int z) {
        if (world == null || !world.blockExists(x, y, z)) return null;
        IChunkProvider provider = world.getChunkProvider();
        if (provider.getClass() != ChunkProvider.class) return null;
        Chunk chunk = provider.provideChunk(x >> 4, z >> 4);
        TileEntity tile = (TileEntity) chunk.chunkTileEntityMap.get(new ChunkPosition(x & 15, y, z & 15));
        return tile == null || tile.isInvalid() ? null : tile;
    }
}
