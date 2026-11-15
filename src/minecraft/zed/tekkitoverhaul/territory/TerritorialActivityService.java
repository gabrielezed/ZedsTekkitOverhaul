package zed.tekkitoverhaul.territory;

import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.World;

/** Local simulation adapter. The save handler identifies one save across portal transitions. */
public final class TerritorialActivityService {
    private World world;
    private ISaveHandler save;
    private TerritorialWorldState state;
    private EntityPlayer observer;
    private int ticks;

    public void bind(World incoming) {
        if (incoming == world) return;
        if (incoming != null && incoming.isRemote) incoming = null;
        ISaveHandler handler = incoming == null ? null : incoming.getSaveHandler();
        if (state != null) state.activity.clearVisit();
        if (handler != save) state = null;
        world = incoming;
        save = handler;
        observer = null;
        ticks = 0;
        if (overworld()) {
            if (state == null) state = TerritorialWorldState.load(world);
            else state.attach(world);
        }
    }

    public void tick(EntityPlayer player, boolean paused) {
        if (paused || !ready()) return;
        TerritorialActivity activity = state.activity;
        activity.advance();
        EntityPlayer current = eligible(player) ? player : null;
        if (current != observer) {
            activity.clearVisit();
            ticks = 0;
            observer = current;
        }
        boolean changed = false;
        if (observer != null && ++ticks == TerritorialActivity.SAMPLE_TICKS) {
            ticks = 0;
            if (validPosition(player)) changed = activity.observe(block(player.posX), block(player.posZ));
            else activity.clearVisit();
        }
        state.checkpoint(changed);
    }

    public ThreatAssessment assess(World context, EntityPlayer player) {
        if (context != world || !ready() || !eligible(player) || !validPosition(player)) return null;
        return assess(block(player.posX), block(player.posZ));
    }

    private ThreatAssessment assess(int x, int z) {
        // The legacy biome manager consults its biome cache and generation layers, with no chunk lookup.
        BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(x, z);
        ThreatAssessment.Environment environment = biome == BiomeGenBase.swampland
                ? ThreatAssessment.Environment.SWAMPLAND : ThreatAssessment.Environment.BASELINE;
        int regionX = TerritorialActivity.cell(x), regionZ = TerritorialActivity.cell(z);
        TerritorialActivity activity = state.activity;
        return new ThreatAssessment(0, regionX, regionZ, activity.blended(x, z), environment,
                activity.records.containsKey(TerritorialActivity.key(regionX, regionZ)));
    }

    public Snapshot snapshot(World context, EntityPlayer player) {
        if (context == null || context != world) return null;
        ThreatAssessment threat = assess(context, player);
        String status = !overworld() ? "OUTSIDE_OVERWORLD" : state == null ? "UNREADABLE" : state.status;
        return new Snapshot(status, world.worldProvider.worldType, threat, ready() ? state.activity : null);
    }

    private boolean overworld() { return world != null && world.worldProvider.worldType == 0; }
    private boolean ready() { return overworld() && state != null && "READY".equals(state.status); }
    private static int block(double position) { return (int) Math.floor(position); }

    private boolean eligible(EntityPlayer player) {
        return player != null && player.worldObj == world && player.isEntityAlive()
                && !player.isPlayerSleeping() && !player.capabilities.isCreativeMode;
    }

    private static boolean validPosition(EntityPlayer player) {
        return TerritorialActivity.inBounds(player.posX) && TerritorialActivity.inBounds(player.posZ);
    }

    public static final class Snapshot {
        public final String status;
        public final int dimension, count, capacity, presence, returns, localActivity;
        public final long clock, observationAge;
        public final String lastEviction;
        public final ThreatAssessment threat;

        private Snapshot(String status, int dimension, ThreatAssessment threat, TerritorialActivity activity) {
            this.status = status;
            this.dimension = dimension;
            this.threat = threat;
            capacity = TerritorialActivity.CAPACITY;
            count = activity == null ? 0 : activity.records.size();
            clock = activity == null ? 0 : activity.clock;
            lastEviction = activity != null && activity.evicted ? "OLDEST_OBSERVATION" : "NONE";
            TerritorialActivity.Record record = threat == null ? null
                    : activity.records.get(TerritorialActivity.key(threat.regionX, threat.regionZ));
            presence = record == null ? 0 : record.presenceAt(clock);
            returns = record == null ? 0 : record.returnsAt(clock);
            localActivity = presence + returns;
            observationAge = record == null ? -1 : clock - record.seen;
        }
    }
}
