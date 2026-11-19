package zed.tekkitoverhaul.survival.tactical;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import zed.tekkitoverhaul.integration.LoadedTile;

/** Read-only BuildCraft ABI checks followed by the native block removal path. */
final class BuildCraftPipeTheft {
    private static final String STOCK = "buildcraft.transport.pipes.";
    private static final String ADDITIONAL = "buildcraft.additionalpipes.pipes.";
    private static final Collection<String> ITEM_PIPES = Arrays.asList("PipeItemsWood", "PipeItemsCobblestone",
            "PipeItemsStone", "PipeItemsIron", "PipeItemsGold", "PipeItemsDiamond", "PipeItemsObsidian");
    private static final Collection<String> LIQUID_PIPES = Arrays.asList("PipeLiquidsWood", "PipeLiquidsCobblestone",
            "PipeLiquidsStone", "PipeLiquidsIron", "PipeLiquidsGold");
    private static final Collection<String> POWER_PIPES = Arrays.asList("PipePowerWood", "PipePowerStone", "PipePowerGold");
    private static final Collection<String> ADDITIONAL_ITEM_PIPES = Arrays.asList("PipeItemsAdvancedWood",
            "PipeItemsAdvancedInsertion", "PipeItemsDistributor", "PipeItemsRedstone");
    private static final int[] NO_REQUESTS = new int[6];
    private static final double[] NO_POWER = new double[6];
    private static final int[] DEFAULT_DISTRIBUTION = {1, 1, 1, 1, 1, 1};
    private final TileEntity tile;
    private final Object pipe;
    private final Method nativeLookup;
    private final int blockId, metadata, itemId;

    private BuildCraftPipeTheft(TileEntity tile, Object pipe, Method nativeLookup, int blockId, int metadata, int itemId) {
        this.tile = tile;
        this.pipe = pipe;
        this.nativeLookup = nativeLookup;
        this.blockId = blockId;
        this.metadata = metadata;
        this.itemId = itemId;
    }

    static String family(TileEntity tile) throws ReflectiveOperationException {
        if (!"buildcraft.transport.TileGenericPipe".equals(tile.getClass().getName())) return null;
        Object pipe = field(tile, "pipe");
        if (pipe == null) return null;
        String name = pipe.getClass().getName();
        if (name.startsWith(STOCK)) {
            String variant = name.substring(STOCK.length());
            if (ITEM_PIPES.contains(variant)) return "BUILDCRAFT_ITEM";
            if (LIQUID_PIPES.contains(variant)) return "BUILDCRAFT_LIQUID";
            if (POWER_PIPES.contains(variant)) return "BUILDCRAFT_POWER";
        }
        if (name.startsWith(ADDITIONAL)) {
            String variant = name.substring(ADDITIONAL.length());
            if (ADDITIONAL_ITEM_PIPES.contains(variant)) return "ADDITIONAL_PIPES_ITEM";
            if ("PipeLiquidsRedstone".equals(variant)) return "ADDITIONAL_PIPES_LIQUID";
        }
        return null;
    }

    static BuildCraftPipeTheft find(TileEntity tile) {
        if (tile == null || !"buildcraft.transport.TileGenericPipe".equals(tile.getClass().getName())) return null;
        World world = tile.worldObj;
        int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
        if (!ThiefTrail.local(world) || !world.checkChunksExist(x - 1, y, z - 1, x + 1, y, z + 1)
                || LoadedTile.at(world, x, y, z) != tile) return null;
        int blockId = world.getBlockId(x, y, z);
        Block block = Block.blocksList[blockId];
        if (block == null || !"buildcraft.transport.BlockGenericPipe".equals(block.getClass().getName())) return null;
        try {
            String family = family(tile);
            if (family == null) return null;
            Object pipe = field(tile, "pipe");
            if (pipe == null || field(pipe, "worldObj") != world || field(pipe, "container") != tile) return null;
            Method lookup = block.getClass().getMethod("getPipe", IBlockAccess.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            if (lookup.invoke(null, world, x, y, z) != pipe) return null;
            Class<?> type = pipe.getClass();
            int itemId = ((Number) field(pipe, "itemID")).intValue();
            if (itemId <= 0 || itemId >= Item.itemsList.length || Item.itemsList[itemId] == null
                    || !"buildcraft.transport.ItemPipe".equals(Item.itemsList[itemId].getClass().getName())
                    || ((Map<?, ?>) block.getClass().getField("pipes").get(null)).get(itemId) != type) return null;
            if (!quiescent(pipe, family)) return null;
            return new BuildCraftPipeTheft(tile, pipe, lookup, blockId, world.getBlockMetadata(x, y, z), itemId);
        } catch (Exception unsupported) {
            return null;
        } catch (LinkageError unsupported) {
            return null;
        }
    }

    private static boolean quiescent(Object pipe, String family) throws ReflectiveOperationException {
        String variant = pipe.getClass().getSimpleName();
        if ("PipeItemsDiamond".equals(variant) || "PipeItemsAdvancedWood".equals(variant)) {
            Object logic = field(pipe, "logic");
            for (Object stack : (Object[]) declared(logic, "items")) if (stack != null) return false;
        }
        if ("PipeItemsDistributor".equals(variant)
                && !Arrays.equals((int[]) field(field(pipe, "logic"), "distData"), DEFAULT_DISTRIBUTION))
            return false;
        if (variant.endsWith("Wood") || "PipeItemsObsidian".equals(variant)) {
            Object provider = pipe.getClass().getMethod("getPowerProvider").invoke(pipe);
            if (provider == null) return false;
            // The native redstone provider uses a constant one as its activation sentinel.
            String providerType = provider.getClass().getName();
            int idle;
            if ("buildcraft.core.RedstonePowerProvider".equals(providerType)) idle = 1;
            else if ("buildcraft.energy.PneumaticPowerProvider".equals(providerType)) idle = 0;
            else return false;
            if (((Number) field(provider, "energyStored")).intValue() != idle) return false;
        }
        if ("PipeLiquidsWood".equals(variant) && ((Number) field(pipe, "liquidToExtract")).intValue() != 0)
            return false;
        Object transport = field(pipe, "transport");
        String name = transport.getClass().getName();
        if (family.endsWith("_ITEM")) {
            return "buildcraft.transport.PipeTransportItems".equals(name)
                    && ((Map<?, ?>) field(transport, "travelingEntities")).isEmpty()
                    && ((Collection<?>) declared(transport, "entitiesToLoad")).isEmpty()
                    && ((Collection<?>) declared(transport, "toRemove")).isEmpty();
        }
        if (family.endsWith("_LIQUID")) {
            if (!"buildcraft.transport.PipeTransportLiquids".equals(name)) return false;
            if (!emptyBuffer(field(transport, "center"))) return false;
            for (Object buffer : (Object[]) field(transport, "side")) if (!emptyBuffer(buffer)) return false;
            return true;
        }
        return "buildcraft.transport.PipeTransportPower".equals(name)
                && Arrays.equals((int[]) field(transport, "powerQuery"), NO_REQUESTS)
                && Arrays.equals((int[]) field(transport, "nextPowerQuery"), NO_REQUESTS)
                && Arrays.equals((double[]) field(transport, "internalPower"), NO_POWER)
                && Arrays.equals((double[]) field(transport, "internalNextPower"), NO_POWER);
    }

    private static boolean emptyBuffer(Object buffer) throws ReflectiveOperationException {
        if (((Number) declared(buffer, "qty")).intValue() != 0
                || ((Number) declared(buffer, "ready")).intValue() != 0) return false;
        for (short amount : (short[]) declared(buffer, "in")) if (amount != 0) return false;
        for (short amount : (short[]) declared(buffer, "out")) if (amount != 0) return false;
        return true;
    }

    private static Object declared(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        return object.getClass().getField(name).get(object);
    }

    boolean stillEligible() {
        BuildCraftPipeTheft current = find(tile);
        return current != null && current.pipe == pipe && current.blockId == blockId
                && current.metadata == metadata && current.itemId == itemId;
    }

    ItemStack remove() {
        if (!stillEligible()) return null;
        // Chunk removal invokes BuildCraft's tile teardown and sends native neighbor notifications.
        // Do not also call the harvest/drop path, which would create a second pipe item.
        ItemStack component = new ItemStack(itemId, 1, 0);
        World world = tile.worldObj;
        int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
        if (!world.setBlockWithNotify(x, y, z, 0) || world.getBlockId(x, y, z) != 0
                || LoadedTile.at(world, x, y, z) != null || !tile.isInvalid()) return null;
        try {
            return nativeLookup.invoke(null, world, x, y, z) == null ? component : null;
        } catch (ReflectiveOperationException incompatible) {
            return null;
        }
    }
}
