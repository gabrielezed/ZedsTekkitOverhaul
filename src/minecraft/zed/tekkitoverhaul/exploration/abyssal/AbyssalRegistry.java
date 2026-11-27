package zed.tekkitoverhaul.exploration.abyssal;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.src.forge.MinecraftForgeClient;
import zed.tekkitoverhaul.config.ZtoConfig;

public final class AbyssalRegistry {
    public enum MaterialType {
        STONE("abyssalStone", "Abyssal Stone", 0, 1.8F),
        SILT("abyssalSilt", "Abyssal Silt", 1, 0.5F),
        SHELLSTONE("shellstone", "Shellstone", 2, 1.4F),
        PETRIFIED_CORAL("petrifiedCoral", "Petrified Coral", 3, 1.3F),
        BRICKS("abyssalBricks", "Abyssal Bricks", 4, 2.0F),
        ERODED_BRICKS("erodedAbyssalBricks", "Eroded Abyssal Bricks", 5, 1.8F),
        LAMP("abyssalLamp", "Abyssal Lamp", 6, 1.0F),
        STAIRS("abyssalStairs", "Abyssal Brick Stairs", 4, 2.0F),
        ERODED_STAIRS("erodedAbyssalStairs", "Eroded Abyssal Brick Stairs", 5, 1.8F),
        SLAB("abyssalSlab", "Abyssal Brick Slab", 4, 2.0F),
        PILLAR("abyssalPillar", "Abyssal Pillar", 16, 2.0F),
        CARVED("abyssalCarved", "Carved Abyssal Bricks", 18, 2.0F),
        FRIEZE("abyssalFrieze", "Abyssal Frieze", 19, 2.0F),
        GRATE("abyssalGrate", "Abyssal Grate", 20, 2.0F),
        PEDESTAL("abyssalPedestal", "Abyssal Pedestal", 21, 2.0F);

        final String key, displayName;
        final int texture;
        final float hardness;
        private Block block;

        MaterialType(String key, String displayName, int texture, float hardness) {
            this.key = key;
            this.displayName = displayName;
            this.texture = texture;
            this.hardness = hardness;
        }

        public Block block() {
            return block;
        }
    }

    static final String ITEMS_TEXTURE = "/zed/textures/abyssal/items.png";

    public static boolean isMarineOrganism(Block block) {
        return block instanceof BlockMarineOrganism;
    }

    private AbyssalRegistry() {}

    public static void register(ZtoConfig config, int marineModel) {
        Map<Integer, String> ids = new HashMap<Integer, String>();
        for (MaterialType type : MaterialType.values()) {
            checkId(ids, config.abyssalId("block." + type.key), "block." + type.key);
        }
        for (MarineType type : MarineType.values()) {
            checkId(ids, config.abyssalId("block." + type.key), "block." + type.key);
            checkId(ids, config.abyssalId("item." + type.itemKey) + 256, "item." + type.itemKey);
        }
        checkId(ids, config.abyssalId("item.abyssalShell") + 256, "item.abyssalShell");

        MinecraftForgeClient.preloadTexture(BlockAbyssalMaterial.TEXTURE);
        for (MaterialType type : MaterialType.values()) {
            boolean sediment = type == MaterialType.SILT;
            int id = config.abyssalId("block." + type.key);
            switch (type) {
                case STAIRS: type.block = new AbyssalArchitecture.Stairs(id, MaterialType.BRICKS.block, type.key); break;
                case ERODED_STAIRS: type.block = new AbyssalArchitecture.Stairs(id, MaterialType.ERODED_BRICKS.block, type.key); break;
                case SLAB: case GRATE: case PEDESTAL: type.block = new AbyssalArchitecture.Partial(id, type); break;
                default:
                    type.block = new BlockAbyssalMaterial(id, type.texture, type.key, type.hardness, sediment,
                            type == MaterialType.LAMP ? 5 : 0);
            }
            ModLoader.registerBlock(type.block);
            ModLoader.addName(type.block, type.displayName);
            MinecraftForge.setBlockHarvestLevel(type.block, sediment ? "shovel" : "pickaxe", 0);
        }

        MinecraftForgeClient.preloadTexture(ITEMS_TEXTURE);
        for (MarineType type : MarineType.values()) {
            int itemId = config.abyssalId("item." + type.itemKey);
            type.organism = new BlockMarineOrganism(config.abyssalId("block." + type.key),
                    itemId + 256, marineModel, type);
            ModLoader.registerBlock(type.organism, ItemMarineOrganism.class);
            ModLoader.addName(type.organism, type.displayName);
            type.propagule = new ItemMarineOrganism.Propagule(itemId, type.organism);
            ModLoader.addName(type.propagule, type.itemName);
        }

        Item shell = new Item(config.abyssalId("item.abyssalShell")) {};
        shell.setItemName("zto.abyssalShell");
        shell.setTextureFile(ITEMS_TEXTURE);
        shell.setIconIndex(5);
        ModLoader.addName(shell, "Abyssal Shell");
        ModLoader.addRecipe(new ItemStack(MaterialType.BRICKS.block, 4), "SS", "SS",
                'S', MaterialType.STONE.block);
        ModLoader.addShapelessRecipe(new ItemStack(MaterialType.ERODED_BRICKS.block),
                MaterialType.BRICKS.block, Block.sand);
        ModLoader.addShapelessRecipe(new ItemStack(MaterialType.LAMP.block),
                MaterialType.STONE.block, MarineType.ANEMONE.propagule);
        AbyssalArchitecture.recipes();
        ModLoader.addShapelessRecipe(new ItemStack(shell), MaterialType.SHELLSTONE.block);
        ModLoader.addShapelessRecipe(new ItemStack(MaterialType.SHELLSTONE.block), shell,
                MaterialType.STONE.block);
    }

    private static void checkId(Map<Integer, String> ids, int id, String key) {
        String previous = ids.put(id, key);
        if (previous != null) {
            throw new IllegalStateException("ZTO registry ID " + id + " is shared by " + previous + " and " + key);
        }
        if (Item.itemsList[id] != null || (id < Block.blocksList.length && Block.blocksList[id] != null)) {
            throw new IllegalStateException("ZTO registry ID " + id + " is occupied: " + key);
        }
    }
}
