package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
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

    public boolean place(World world, int x, int y, int z, int orientation) {
        return organism.place(world, x, y, z, orientation);
    }
}
