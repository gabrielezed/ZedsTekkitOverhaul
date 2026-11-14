package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.distillation.client.RenderDistillationComponent;
import zed.tekkitoverhaul.fluids.PetroleumFluids;

/**
 * Registers the provisional apparatus, four bottled liquids and their textures.
 * Numeric IDs are configurable; never silently change existing world mappings.
 */
public final class DistillationRegistry {
    private static final Block[] BLOCKS = new Block[DistillationPartType.values().length];
    private static final Item[] BUCKETS = new Item[DistillateType.values().length];
    private static final int[] BUCKET_ICONS = new int[BUCKETS.length];
    private static boolean registered;
    private static int oilBucketIdOverride = -1;

    private DistillationRegistry() { }

    public static int blockId(DistillationPartType type) {
        Block block = BLOCKS[type.index];
        return block == null ? -1 : block.blockID;
    }

    public static DistillationPartType typeForBlockId(int id) {
        for (DistillationPartType type : DistillationPartType.values()) {
            Block block = BLOCKS[type.index];
            if (block != null && block.blockID == id) return type;
        }
        return null;
    }

    public static Item bucketFor(int typeId) {
        return typeId < 0 || typeId >= BUCKETS.length ? null : BUCKETS[typeId];
    }

    public static int bucketIcon(int typeId) {
        return BUCKET_ICONS[typeId];
    }

    public static boolean isBuildCraftOilBucket(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        if (item == null || item instanceof ItemDistillateBucket) return false;
        if (oilBucketIdOverride >= 0 && stack.itemID == oilBucketIdOverride) return true;
        return item == BuildCraftOilBucket.ITEM;
    }

    private static final class BuildCraftOilBucket {
        private static final Item ITEM = find();

        private static Item find() {
            try {
                Object bucket = Class.forName("BuildCraftEnergy").getField("bucketOil").get(null);
                return bucket instanceof Item ? (Item) bucket : null;
            } catch (ReflectiveOperationException unavailable) {
                return null;
            } catch (LinkageError unavailable) {
                return null;
            }
        }
    }

    public static void register(ZtoConfig config) {
        if (registered) return;
        int[] blockIds = new int[BLOCKS.length];
        int[] itemIds = new int[BUCKETS.length];
        int[] flowingIds = new int[BUCKETS.length];
        for (DistillationPartType type : DistillationPartType.values()) {
            blockIds[type.index] = config.partId(type);
        }
        for (DistillateType type : DistillateType.values()) {
            itemIds[type.id] = config.bucketId(type);
            flowingIds[type.id] = config.flowingId(type);
        }
        oilBucketIdOverride = config.buildcraftOilBucketItemId;

        for (int i = 0; i < blockIds.length; i++) {
            int id = blockIds[i];
            if (id < 1 || id >= Block.blocksList.length || Block.blocksList[id] != null)
                throw new IllegalStateException("ZTO: Invalid or occupied distillation block ID " + id);
            for (int j = 0; j < i; j++)
                if (id == blockIds[j])
                    throw new IllegalStateException("ZTO: Duplicate distillation block ID " + id);
        }
        for (int i = 0; i < itemIds.length; i++) {
            int shifted = itemIds[i] + 256;
            if (itemIds[i] < 0 || shifted >= Item.itemsList.length || Item.itemsList[shifted] != null)
                throw new IllegalStateException("ZTO: Invalid or occupied distillate item ID " + shifted);
            for (int flowingId : flowingIds) {
                if (shifted == flowingId || shifted - 1 == flowingId)
                    throw new IllegalStateException("ZTO: Distillate item ID overlaps petroleum block ID " + shifted);
            }
            for (int j = 0; j < i; j++)
                if (itemIds[i] == itemIds[j])
                    throw new IllegalStateException("ZTO: Duplicate distillate item ID " + shifted);
        }

        ModLoader.registerTileEntity(TileEntityDistillationBoiler.class,
                "ZTODistillationBoiler", new RenderDistillationComponent());
        ModLoader.registerTileEntity(TileEntityDistillationPart.class,
                "ZTODistillationPart", new RenderDistillationComponent());
        ModLoader.registerTileEntity(TileEntityDistillationHeater.class,
                "ZTODistillationLVHeater", new RenderDistillationComponent());
        ModLoader.registerTileEntity(TileEntityDistillationReceiver.class,
                "ZTODistillationReceiver", new RenderDistillationComponent());
        ModLoader.registerTileEntity(TileEntityDistillationCondenser.class,
                "ZTODistillationCondenser", new RenderDistillationComponent());

        for (DistillationPartType type : DistillationPartType.values()) {
            Block block = type == DistillationPartType.BOILER ?
                    new BlockDistillationBoiler(blockIds[type.index]) :
                    new BlockDistillationPart(blockIds[type.index], type);
            BLOCKS[type.index] = block;
            ModLoader.registerBlock(block);
            ModLoader.addName(block, type.displayName);
        }
        for (DistillateType type : DistillateType.values()) {
            PetroleumFluids.registerBlocks(type, flowingIds[type.id]);
            Item item = new ItemDistillateBucket(itemIds[type.id], type);
            BUCKET_ICONS[type.id] = ModLoader.getUniqueSpriteIndex("/gui/items.png");
            item.setIconIndex(Item.bucketEmpty.getIconFromDamage(0));
            BUCKETS[type.id] = item;
            ModLoader.addName(item, type.displayName + " Bucket");
            PetroleumFluids.registerBucket(type, item);
        }
        registerTemporaryRecipes();
        registered = true;
        System.out.println("[ZTO] Registered six distillation component types and four petroleum fluids");
    }

    private static void registerTemporaryRecipes() {
        Block boiler = BLOCKS[DistillationPartType.BOILER.index];
        Block heater = BLOCKS[DistillationPartType.HEATER.index];
        Block column = BLOCKS[DistillationPartType.COLUMN.index];
        Block condenser = BLOCKS[DistillationPartType.CONDENSER.index];
        Block receiver = BLOCKS[DistillationPartType.RECEIVER.index];
        Block thermometer = BLOCKS[DistillationPartType.THERMOMETER.index];

        ModLoader.addRecipe(new ItemStack(boiler), new Object[]{
                "IGI", "IFI", "III",
                Character.valueOf('I'), Item.ingotIron,
                Character.valueOf('G'), Block.glass,
                Character.valueOf('F'), Block.stoneOvenIdle
        });
        ModLoader.addRecipe(new ItemStack(heater), new Object[]{
                "IRI", "RFR", "III",
                Character.valueOf('I'), Item.ingotIron,
                Character.valueOf('R'), Item.redstone,
                Character.valueOf('F'), Block.stoneOvenIdle
        });
        ModLoader.addRecipe(new ItemStack(column, 2), new Object[]{
                "IGI", "IGI", "IGI",
                Character.valueOf('I'), Item.ingotIron,
                Character.valueOf('G'), Block.glass
        });
        ModLoader.addRecipe(new ItemStack(condenser), new Object[]{
                "III", "GWG", "III",
                Character.valueOf('I'), Item.ingotIron,
                Character.valueOf('G'), Block.glass,
                Character.valueOf('W'), Item.bucketWater
        });
        ModLoader.addRecipe(new ItemStack(receiver), new Object[]{
                "GGG", "G G", "III",
                Character.valueOf('G'), Block.glass,
                Character.valueOf('I'), Item.ingotIron
        });
        ModLoader.addRecipe(new ItemStack(thermometer), new Object[]{
                "IGI", "RGR", "III",
                Character.valueOf('I'), Item.ingotIron,
                Character.valueOf('G'), Block.glass,
                Character.valueOf('R'), Item.redstone
        });
    }
}
