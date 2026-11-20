package zed.tekkitoverhaul.settlement;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.IInventory;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/** Registry properties are cached per block. Live capabilities come from the loaded chunk. */
final class BlockEvidence {
    static final int TILE = 1, FULL = 2, SOLID_MATERIAL = 4, TILE_VARIANT = 8;
    private static final Block[] KNOWN = new Block[Block.blocksList.length];
    private static final byte[] FLAGS = new byte[KNOWN.length];
    private static final ClassValue<Boolean> OVERRIDES_TILE = new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                // Forge keeps this name in both MCP and the pinned runtime. An override may depend on metadata.
                return type.getMethod("hasTileEntity", Integer.TYPE).getDeclaringClass() != Block.class;
            } catch (NoSuchMethodException unavailable) {
                return true;
            }
        }
    };

    static int flags(int id) {
        if (id <= 0 || id >= KNOWN.length) return 0;
        Block block = Block.blocksList[id];
        if (KNOWN[id] != block) {
            int flags = 0;
            if (block != null) {
                if (OVERRIDES_TILE.get(block.getClass())) flags |= TILE | TILE_VARIANT;
                else if (block.hasTileEntity(0)) flags |= TILE;
                if (block.blockMaterial.isSolid()) {
                    flags |= SOLID_MATERIAL;
                    if (block.renderAsNormalBlock()) flags |= FULL;
                }
            }
            KNOWN[id] = block;
            FLAGS[id] = (byte) flags;
        }
        return FLAGS[id];
    }

    static TileEntity loadedTile(World world, int x, int y, int z) {
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        // The native tile getter creates missing tiles. The loaded map has no such side effect.
        TileEntity tile = (TileEntity) chunk.chunkTileEntityMap.get(new ChunkPosition(x & 15, y, z & 15));
        return valid(tile, world, x, y, z) ? tile : null;
    }

    static boolean hasTile(int id, int metadata) {
        try {
            return Block.blocksList[id].hasTileEntity(metadata);
        } catch (RuntimeException unreadable) {
            return true;
        }
    }

    static boolean inventory(TileEntity tile) {
        try {
            // A transport facade can implement IInventory with zero addressable slots.
            return ((IInventory) tile).getSizeInventory() > 0;
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    private static boolean valid(TileEntity tile, World world, int x, int y, int z) {
        return tile != null && !tile.isInvalid() && tile.worldObj == world
                && tile.xCoord == x && tile.yCoord == y && tile.zCoord == z;
    }

    private BlockEvidence() {}
}
