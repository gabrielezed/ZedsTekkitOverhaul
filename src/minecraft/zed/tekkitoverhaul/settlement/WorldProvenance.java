package zed.tekkitoverhaul.settlement;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.World;

/** Owns provenance for the local Overworld and follows its save across portal transitions. */
public final class WorldProvenance {
    private static final String KEY = "zto_world_provenance";
    private static final int PLACE = 0, REMOVE = 1, QUARRY = 2;
    private final Thread owner = Thread.currentThread();
    private final BiConsumer<Object[], int[]> listener = new BiConsumer<Object[], int[]>() {
        @Override
        public void accept(Object[] context, int[] change) { observe(context, change); }
    };
    private World world;
    private EntityPlayer player;
    private ISaveHandler handler;
    private Path path;
    private ProvenanceStore store;
    private Method hooks;

    public void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false,
                    ClassLoader.getSystemClassLoader());
            agent.getMethod("registerProvenance", BiConsumer.class).invoke(null, listener);
            hooks = agent.getMethod("provenanceHooks", ClassLoader.class);
        } catch (ReflectiveOperationException unavailable) {
            hooks = null;
        } catch (LinkageError unavailable) {
            hooks = null;
        }
    }

    public void bind(World incoming) {
        if (Thread.currentThread() != owner) return;
        if (incoming != null && incoming.isRemote) incoming = null;
        if (world == incoming) return;
        world = incoming;
        player = null;
        ISaveHandler next = world == null ? null : world.getSaveHandler();
        if (handler == next) return;
        handler = next;
        File file = handler == null ? null : handler.getMapFileFromName(KEY);
        path = file == null ? null : file.toPath();
        store = handler == null ? null : ProvenanceStore.load(path);
    }

    public void bindPlayer(EntityPlayer current) {
        player = current != null && current.worldObj == world ? current : null;
    }

    public void save(World context) {
        if (Thread.currentThread() == owner && context != null && !context.isRemote && store != null
                && context.getSaveHandler() == handler) store.save(path);
    }

    private void observe(Object[] context, int[] change) {
        if (Thread.currentThread() != owner || world == null || context[0] != world
                || world.worldProvider.worldType != 0 || store == null
                || !("READY".equals(store.status) || "WRITE_FAILED".equals(store.status))) return;
        int x = change[0], y = change[1], z = change[2], block = change[3], source = change[4];
        if (source != QUARRY && (player == null || context[1] != player || !player.isEntityAlive()
                || player.capabilities.isCreativeMode)) return;
        if (!ProvenanceStore.valid(x, y, z) || !world.blockExists(x, y, z)) return;
        int current = world.getBlockId(x, y, z);
        if (source == PLACE && current == block && block > 0 && block <= 4095) {
            store.record(x, y, z, (short) block);
        } else if (source == REMOVE && current != block) {
            store.record(x, y, z, ProvenanceStore.MANUAL_REMOVAL);
        } else if (source == QUARRY && block > 0 && current == 0) {
            store.record(x, y, z, ProvenanceStore.QUARRY_REMOVAL);
        }
    }

    public Snapshot snapshot(World context, int x, int y, int z) {
        if (Thread.currentThread() != owner || context == null || context != world) return null;
        int dimension = context.worldProvider.worldType;
        String status = dimension != 0 ? "OUTSIDE_OVERWORLD" : store == null ? "UNAVAILABLE" : store.status;
        int active = 0;
        try {
            if (hooks != null) active = (Integer) hooks.invoke(null, World.class.getClassLoader());
        } catch (ReflectiveOperationException unavailable) {
            hooks = null;
        }
        ProvenanceStore.Section section = dimension == 0 && store != null && ProvenanceStore.valid(x, y, z)
                ? store.section(ProvenanceStore.key(x, y, z)) : null;
        return new Snapshot(status, active, dimension, x, y, z, section, store);
    }

    public static final class Snapshot {
        public final String status, position;
        public final int hooks, dimension, placed, manualRemoval, quarryRemoval, sections, capacity;
        public final boolean evicted;

        private Snapshot(String status, int hooks, int dimension, int x, int y, int z,
                         ProvenanceStore.Section local, ProvenanceStore store) {
            this.status = status;
            this.hooks = hooks;
            this.dimension = dimension;
            placed = local == null ? 0 : local.placed;
            manualRemoval = local == null ? 0 : local.manualRemoval;
            quarryRemoval = local == null ? 0 : local.quarryRemoval;
            sections = store == null ? 0 : store.size();
            capacity = ProvenanceStore.CAPACITY;
            evicted = store != null && store.evicted;
            short observation = local == null ? 0 : local.blocks[ProvenanceStore.offset(x, y, z)];
            if (observation > 0) position = "Observed manual placement (block " + observation + ")";
            else if (observation == ProvenanceStore.MANUAL_REMOVAL) position = "Observed manual removal";
            else if (observation == ProvenanceStore.QUARRY_REMOVAL) position = "Observed quarry removal";
            else position = "Unknown history";
        }
    }
}
