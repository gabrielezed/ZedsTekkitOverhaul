package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.Chunk;
import net.minecraft.src.ExtendedBlockStorage;
import net.minecraft.src.World;

public enum MarineType {
    KELP("abyssalKelp", "Abyssal Kelp", "kelpFrond", "Kelp Frond", 8, 0, 16),
    ALGAE("ribbonAlgae", "Ribbon Algae", "ribbonAlgaeCutting", "Ribbon Algae Cutting", 9, 1, 9),
    FAN_CORAL("deepwaterFanCoral", "Deepwater Fan Coral", "fanCoralFragment", "Fan Coral Fragment", 10, 2, 14),
    SPONGE("tubeSponge", "Tube Sponge", "spongeCutting", "Sponge Cutting", 14, 3, 11),
    ANEMONE("ghostAnemone", "Ghost Anemone", "luminousPolyp", "Luminous Polyp", 13, 4, 8);

    final String key, displayName, itemKey, itemName;
    final int tile, itemTile, height;
    BlockMarineOrganism organism;
    Item propagule;

    MarineType(String key, String displayName, String itemKey, String itemName, int tile, int itemTile, int height) {
        this.key = key;
        this.displayName = displayName;
        this.itemKey = itemKey;
        this.itemName = itemName;
        this.tile = tile;
        this.itemTile = itemTile;
        this.height = height;
    }

    public Block block() {
        return organism;
    }

    /** Generation-only placement. The caller supplies an owned, unlit native chunk. */
    public boolean placeInNewChunk(Chunk chunk, int x, int y, int z, int height, int orientation) {
        if (x < 1 || x > 14 || z < 1 || z > 14 || y < 1 || y + height > 62
                || height < 1 || height > (this == KELP ? 4 : 1)) return false;
        Block support = Block.blocksList[chunk.getBlockID(x, y - 1, z)];
        if (!(support instanceof BlockAbyssalMaterial) || !support.isOpaqueCube()
                || !source(chunk, x, y + height, z)) return false;
        for (int level = y; level < y + height; level++) {
            if (chunk.getBlockMetadata(x, level, z) != 0 || !source(chunk, x, level, z) || !source(chunk, x - 1, level, z)
                    || !source(chunk, x + 1, level, z) || !source(chunk, x, level, z - 1)
                    || !source(chunk, x, level, z + 1)) return false;
        }
        for (int level = y; level < y + height; level++) {
            ExtendedBlockStorage storage = chunk.getBlockStorageArray()[level >> 4];
            storage.setExtBlockID(x, level & 15, z, organism.blockID);
            storage.setExtBlockMetadata(x, level & 15, z, organism.occupiedMetadata(orientation));
        }
        return true;
    }

    private static boolean source(Chunk chunk, int x, int y, int z) {
        int id = chunk.getBlockID(x, y, z);
        Block block = Block.blocksList[id];
        return ((block == Block.waterStill || block == Block.waterMoving) && chunk.getBlockMetadata(x, y, z) == 0)
                || block instanceof BlockMarineOrganism;
    }

    public boolean place(World world, int x, int y, int z, int orientation) {
        return organism.place(world, x, y, z, orientation);
    }
}
