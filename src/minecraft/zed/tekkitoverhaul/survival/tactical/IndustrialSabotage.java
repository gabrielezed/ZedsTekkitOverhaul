package zed.tekkitoverhaul.survival.tactical;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.src.Chunk;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.forge.ISaveEventHandler;
import net.minecraft.src.forge.MinecraftForge;
import zed.tekkitoverhaul.integration.LoadedTile;

/** Discovers local native endpoints and owns their transient, game-thread-only interruption leases. */
final class IndustrialSabotage implements SabotageTarget {
    private static final String PIPE_TILE = "buildcraft.transport.TileGenericPipe";
    private static final String ITEM_PIPE = "buildcraft.transport.pipes.PipeItemsWood";
    private static final String LIQUID_PIPE = "buildcraft.transport.pipes.PipeLiquidsWood";
    private static final String ENGINE_TILE = "buildcraft.energy.TileEngine";
    private static final String ELECTRIC_BLOCK = "ic2.common.TileEntityElectricBlock";
    private static final String SOLAR_ARRAY = "cpw.mods.compactsolars.TileEntityCompactSolar";
    private static final String RELAY_TILE = "eloraam.machine.TileRelay";
    private static final int[] DX = {0, 0, 0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, -1, 1, 0, 0};
    private static final int DURATION = 80;
    private static final Map<Object, Lease> LEASES = new WeakHashMap<Object, Lease>();
    private static final Map<TileEntity, WeakReference<Object>> LINE_CLAIMS =
            new WeakHashMap<TileEntity, WeakReference<Object>>();
    private static final Predicate<Object> PAUSE_GATE = new Predicate<Object>() {
        public boolean test(Object target) {
            Lease lease = activeLease(target);
            if (lease == null) return false;
            lease.showEffect();
            return true;
        }
    };
    private static Method installed;

    static void register() {
        if (installed == null) installed = connectAgent();
    }

    enum Operation {
        BUILDCRAFT_ITEM_EXTRACTION(2),
        BUILDCRAFT_LIQUID_EXTRACTION(2),
        BUILDCRAFT_REDSTONE_ENGINE(1),
        IC2_STORAGE_OUTPUT(2),
        COMPACT_SOLARS_OUTPUT(2),
        REDPOWER_RELAY_EXTRACTION(2),
        EE2_COLLECTOR(2);

        final int priority;

        Operation(int priority) { this.priority = priority; }
    }

    final Operation operation;
    private final TileEntity tile;
    private final Object key;
    private final TileEntity source;
    private final int side;

    private IndustrialSabotage(Operation operation, TileEntity tile, Object key, TileEntity source, int side) {
        this.operation = operation;
        this.tile = tile;
        this.key = key;
        this.source = source;
        this.side = side;
    }

    static IndustrialSabotage find(TileEntity tile) {
        if (installed == null || tile == null || tile.worldObj == null || tile.worldObj.isRemote) return null;
        try {
            Class<?> type = tile.getClass();
            String name = type.getName();
            if (PIPE_TILE.equals(name)) return extraction(tile);
            if (ENGINE_TILE.equals(name) && installed(type) && poweredRedstoneEngine(tile)) {
                int side = intField(tile, "orientation");
                TileEntity receiver = neighbor(tile, side);
                if (isolatedEngineOutput(tile, side, receiver))
                    return new IndustrialSabotage(Operation.BUILDCRAFT_REDSTONE_ENGINE, tile, tile, receiver, side);
            }
            if (name.equals("ic2.common.TileEntityElectricBatBox") || name.equals("ic2.common.TileEntityElectricMFE")
                    || name.equals("ic2.common.TileEntityElectricMFSU")) {
                Class<?> base = type.getSuperclass();
                if (ELECTRIC_BLOCK.equals(base.getName()) && installed(base) && supplyingEU(tile))
                    return new IndustrialSabotage(Operation.IC2_STORAGE_OUTPUT, tile, tile, null, -1);
            }
            if (name.equals(SOLAR_ARRAY) || name.equals(SOLAR_ARRAY + "MV") || name.equals(SOLAR_ARRAY + "HV")) {
                Class<?> base = name.equals(SOLAR_ARRAY) ? type : type.getSuperclass();
                if (SOLAR_ARRAY.equals(base.getName()) && installed(base) && solarOutput(tile))
                    return new IndustrialSabotage(Operation.COMPACT_SOLARS_OUTPUT, tile, tile, null, -1);
            }
            if (RELAY_TILE.equals(name) && installed(type) && hasItems(tile, 9) && !booleanField(tile, "Powered")
                    && neighbor(tile, intField(tile, "Rotation")) != null)
                return new IndustrialSabotage(Operation.REDPOWER_RELAY_EXTRACTION, tile, tile, null, -1);
            if ((name.equals("ee.TileCollector") || name.equals("ee.TileCollector2") || name.equals("ee.TileCollector3"))
                    && installed(type) && collectorOperating(tile) && !feedsConverter(tile))
                return new IndustrialSabotage(Operation.EE2_COLLECTOR, tile, tile, null, -1);
        } catch (ReflectiveOperationException incompatible) {
            // The optional ABI can be missing in another pack. Leave that endpoint native.
        } catch (LinkageError incompatible) {
            // A partial installation can resolve the tile but fail to link an optional API.
        }
        return null;
    }

    private static IndustrialSabotage extraction(TileEntity tile) throws ReflectiveOperationException {
        Object pipe = field(tile, "pipe");
        if (pipe == null || !installed(pipe.getClass())) return null;
        int side = tile.worldObj.getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
        TileEntity source = neighbor(tile, side);
        if (source == null) return null;
        Operation operation;
        String name = pipe.getClass().getName();
        if (name.equals(ITEM_PIPE) && source.getClass() == TileEntityChest.class && hasItems(source, 27)) {
            operation = Operation.BUILDCRAFT_ITEM_EXTRACTION;
        } else if (name.equals(LIQUID_PIPE)
                && source.getClass().getName().equals("buildcraft.factory.TileTank")
                && ((Number) source.getClass().getMethod("getLiquidQuantity").invoke(source)).intValue() > 0) {
            operation = Operation.BUILDCRAFT_LIQUID_EXTRACTION;
        } else {
            return null;
        }
        for (int i = 0; i < 6; i++) {
            TileEntity engine = neighbor(tile, i);
            if (engine != null && ENGINE_TILE.equals(engine.getClass().getName())
                    && intField(engine, "orientation") == (i ^ 1) && poweredRedstoneEngine(engine)
                    && !paused(engine))
                return new IndustrialSabotage(operation, tile, pipe, source, side);
        }
        return null;
    }

    private static boolean poweredRedstoneEngine(TileEntity tile) throws ReflectiveOperationException {
        Object engine = field(tile, "engine");
        return engine != null && engine.getClass().getName().equals("buildcraft.energy.EngineWood") && powered(tile);
    }

    private static boolean isolatedEngineOutput(TileEntity tile, int side, TileEntity output)
            throws ReflectiveOperationException {
        if (output == null || !PIPE_TILE.equals(output.getClass().getName()) || !powerAreaLoaded(tile)) return false;
        Object pipe = field(output, "pipe");
        if (pipe == null) return false;
        String name = pipe.getClass().getName();
        if (!ITEM_PIPE.equals(name) && !LIQUID_PIPE.equals(name)) return false;
        TileEntity input = neighbor(output, tile.worldObj.getBlockMetadata(output.xCoord, output.yCoord, output.zCoord));
        if (input == null || input instanceof SabotageTarget) return false;
        // A wood extractor can receive a stroke. Disallow other adjacent tiles that could feed the paused engine.
        for (int i = 0; i < 6; i++) if (i != side && neighbor(tile, i) != null) return false;
        return true;
    }

    private static boolean supplyingEU(TileEntity tile) throws ReflectiveOperationException {
        int energy = intField(tile, "energy");
        if (energy < intField(tile, "output") || !booleanField(tile, "addedToEnergyNet")) return false;
        int mode = intField(tile, "redstoneMode");
        if ((mode == 4 || mode == 5 && energy < intField(tile, "maxStorage"))
                && (!powerAreaLoaded(tile) || tile.worldObj.isBlockGettingPowered(tile.xCoord, tile.yCoord, tile.zCoord)))
            return false;
        int side = ((Number) tile.getClass().getMethod("getFacing").invoke(tile)).intValue();
        return energyAcceptor(tile).isInstance(neighbor(tile, side));
    }

    private static boolean solarOutput(TileEntity tile) throws ReflectiveOperationException {
        if (!booleanField(tile, "theSunIsVisible")) return false;
        Class<?> acceptor = energyAcceptor(tile);
        for (int side = 0; side < 6; side++) if (acceptor.isInstance(neighbor(tile, side))) return true;
        return false;
    }

    private static Class<?> energyAcceptor(TileEntity tile) throws ClassNotFoundException {
        return Class.forName("ic2.api.IEnergyAcceptor", false, tile.getClass().getClassLoader());
    }

    private static boolean collectorOperating(TileEntity tile) throws ReflectiveOperationException {
        World world = tile.worldObj;
        if (tile.yCoord + 1 >= world.getHeight() || !powerAreaLoaded(tile)) return false;
        return ((Number) tile.getClass().getMethod("getProduction").invoke(tile)).intValue() > 1;
    }

    private static boolean feedsConverter(TileEntity collector) {
        for (int side = 0; side < 6; side++) {
            TileEntity adjacent = neighbor(collector, side);
            if (adjacent instanceof SabotageTarget && "zed.tekkitoverhaul.power.TileAlchemicalConverter"
                    .equals(adjacent.getClass().getName())) return true;
        }
        return false;
    }

    private static boolean hasItems(TileEntity tile, int limit) {
        if (!(tile instanceof IInventory)) return false;
        IInventory inventory = (IInventory) tile;
        for (int slot = 0; slot < Math.min(limit, inventory.getSizeInventory()); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.stackSize > 0) return true;
        }
        return false;
    }

    private static TileEntity neighbor(TileEntity tile, int side) {
        if (side < 0 || side >= 6) return null;
        return LoadedTile.at(tile.worldObj, tile.xCoord + DX[side], tile.yCoord + DY[side], tile.zCoord + DZ[side]);
    }

    private static boolean powered(TileEntity tile) {
        return powerAreaLoaded(tile) && tile.worldObj.isBlockIndirectlyGettingPowered(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    private static boolean powerAreaLoaded(TileEntity tile) {
        return tile.worldObj.checkChunksExist(tile.xCoord - 2, tile.yCoord, tile.zCoord - 2,
                tile.xCoord + 2, tile.yCoord, tile.zCoord + 2);
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getField(name).get(target);
    }

    private static int intField(Object target, String name) throws ReflectiveOperationException {
        return ((Number) field(target, name)).intValue();
    }

    private static boolean booleanField(Object target, String name) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(field(target, name));
    }

    private static Method connectAgent() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", true,
                    ClassLoader.getSystemClassLoader());
            Method installed = agent.getMethod("industrialActive", Class.class);
            agent.getMethod("registerIndustry", Predicate.class).invoke(null, PAUSE_GATE);
            MinecraftForge.registerSaveHandler(new UnloadObserver());
            return installed;
        } catch (ReflectiveOperationException unavailable) {
            return null;
        } catch (LinkageError unavailable) {
            return null;
        }
    }

    private static boolean installed(Class<?> type) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(installed.invoke(null, type));
    }

    private static boolean paused(Object key) { return activeLease(key) != null; }

    private static Lease activeLease(Object key) {
        if (LEASES.isEmpty()) return null;
        Lease lease = LEASES.get(key);
        if (lease != null && !lease.isLive(key)) {
            LEASES.remove(key);
            return null;
        }
        return lease;
    }

    boolean stillBound() {
        return bindingMatches(tile, key, side, source);
    }

    public String activeIndustrialOperation() { return operation.name(); }

    private static boolean bindingMatches(TileEntity tile, Object key, int side, TileEntity source) {
        World world = tile.worldObj;
        if (LoadedTile.at(world, tile.xCoord, tile.yCoord, tile.zCoord) != tile) return false;
        if (side < 0) return !tile.getClass().getName().startsWith("ee.") || !feedsConverter(tile);
        if (source == null || neighbor(tile, side) != source) return false;
        try {
            if (key == tile) return intField(tile, "orientation") == side && isolatedEngineOutput(tile, side, source);
            return world.getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord) == side && field(tile, "pipe") == key;
        } catch (ReflectiveOperationException incompatible) {
            return false;
        }
    }

    private TileEntity line() {
        return operation == Operation.BUILDCRAFT_REDSTONE_ENGINE ? source : tile;
    }

    public boolean isDisrupted() {
        if (paused(key)) return true;
        TileEntity line = line();
        WeakReference<Object> claim = LINE_CLAIMS.get(line);
        if (claim == null) return false;
        if (paused(claim.get())) return true;
        LINE_CLAIMS.remove(line);
        return false;
    }

    public boolean beginDisruption(EntitySaboteur owner) {
        if (!owner.canStartDisruption(tile) || !owner.canMaintainDisruption(tile.worldObj) || isDisrupted()) return false;
        IndustrialSabotage current = find(tile);
        if (current == null || current.key != key || current.side != side || current.source != source) return false;
        LEASES.put(key, new Lease(this, owner));
        LINE_CLAIMS.put(line(), new WeakReference<Object>(key));
        return true;
    }

    private static final class Lease {
        final WeakReference<EntitySaboteur> owner;
        final WeakReference<TileEntity> tile, source;
        final int side;
        final long start;
        long lastParticle = Long.MIN_VALUE;

        Lease(IndustrialSabotage target, EntitySaboteur actor) {
            owner = new WeakReference<EntitySaboteur>(actor);
            tile = new WeakReference<TileEntity>(target.tile);
            source = new WeakReference<TileEntity>(target.source);
            side = target.side;
            start = target.tile.worldObj.getWorldTime();
        }

        boolean isLive(Object key) {
            EntitySaboteur actor = owner.get();
            TileEntity target = tile.get();
            if (actor == null || target == null || !actor.canMaintainDisruption(target.worldObj)) return false;
            long now = target.worldObj.getWorldTime();
            return now >= start && now - start < DURATION && bindingMatches(target, key, side, source.get());
        }

        void showEffect() {
            TileEntity target = tile.get();
            if (target == null) return;
            long now = target.worldObj.getWorldTime();
            if (lastParticle != Long.MIN_VALUE && now - lastParticle < 8) return;
            lastParticle = now;
            target.worldObj.spawnParticle("portal", target.xCoord + 0.5D, target.yCoord + 0.95D,
                    target.zCoord + 0.5D, 0, -0.15D, 0);
        }
    }

    private static final class UnloadObserver implements ISaveEventHandler {
        public void onWorldLoad(World world) {
            if (!world.isRemote) {
                LEASES.clear();
                LINE_CLAIMS.clear();
            }
        }
        public void onWorldSave(World world) {}
        public void onChunkLoad(World world, Chunk chunk) {}
        public void onChunkSaveData(World world, Chunk chunk, NBTTagCompound data) {}
        public void onChunkLoadData(World world, Chunk chunk, NBTTagCompound data) {}

        public void onChunkUnload(World world, Chunk chunk) {
            if (LEASES.isEmpty()) return;
            Iterator<Lease> leases = LEASES.values().iterator();
            while (leases.hasNext()) {
                Lease lease = leases.next();
                TileEntity tile = lease.tile.get();
                TileEntity endpoint = lease.source.get();
                EntitySaboteur actor = lease.owner.get();
                if (tile == null || actor == null || tile.worldObj == world
                        && (inside(tile, chunk) || inside(endpoint, chunk)
                        || actor.chunkCoordX == chunk.xPosition && actor.chunkCoordZ == chunk.zPosition))
                    leases.remove();
            }
        }

        private boolean inside(TileEntity tile, Chunk chunk) {
            return tile != null && (tile.xCoord >> 4) == chunk.xPosition && (tile.zCoord >> 4) == chunk.zPosition;
        }
    }
}
