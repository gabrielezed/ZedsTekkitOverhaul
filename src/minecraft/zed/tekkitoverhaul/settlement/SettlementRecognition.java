package zed.tekkitoverhaul.settlement;

import java.io.File;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteRegistry.Bounds;
import zed.tekkitoverhaul.settlement.SiteRegistry.Site;
import zed.tekkitoverhaul.settlement.SiteSurvey.Evidence;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Evaluates the current Survival player's work area independently of encounters. */
public final class SettlementRecognition {
    private static final String KEY = "zto_settlement_sites";
    private static final int INTERVAL = 100, USE_MEMORY = 1200, RECENT_LIMIT = 16;
    private final Thread owner = Thread.currentThread();
    private final TerritorialActivityService territory;
    private final WorldProvenance provenance;
    private final SiteSurvey survey = new SiteSurvey();
    private final BiConsumer<Object[], int[]> useListener = new BiConsumer<Object[], int[]>() {
        @Override public void accept(Object[] context, int[] change) { recordUse(context, change); }
    };
    private final int[] walkedX = new int[RECENT_LIMIT], walkedY = new int[RECENT_LIMIT],
            walkedZ = new int[RECENT_LIMIT];
    private final Use[] recentUse = new Use[RECENT_LIMIT];
    private World world;
    private ISaveHandler handler;
    private EntityPlayer player;
    private SiteRegistry registry;
    private Method useHook;
    private long tick;
    private int anchorX, anchorY, anchorZ, walked, useIndex, delay, peakReads;
    private int resultX, resultY, resultZ;
    private Decision latest;

    public SettlementRecognition(TerritorialActivityService territory, WorldProvenance provenance) {
        this.territory = territory;
        this.provenance = provenance;
    }

    public void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false,
                    ClassLoader.getSystemClassLoader());
            agent.getMethod("registerSiteUse", BiConsumer.class).invoke(null, useListener);
            useHook = agent.getMethod("siteUseActive", ClassLoader.class);
        } catch (ReflectiveOperationException unavailable) {
            useHook = null;
        } catch (LinkageError unavailable) {
            useHook = null;
        }
    }

    public void bind(World incoming) {
        if (Thread.currentThread() != owner) return;
        if (incoming != null && incoming.isRemote) incoming = null;
        if (incoming == world) return;
        world = incoming;
        ISaveHandler next = world == null ? null : world.getSaveHandler();
        if (next != handler) {
            handler = next;
            File file = handler == null ? null : handler.getMapFileFromName(KEY);
            registry = handler == null ? null : new SiteRegistry(file == null ? null : file.toPath());
            peakReads = 0;
        }
        resetPlayer(null);
    }

    private void resetPlayer(EntityPlayer next) {
        player = next;
        latest = null;
        survey.active = false;
        survey.reads = 0;
        walked = 0;
        delay = 0;
        for (int i = 0; i < recentUse.length; i++) recentUse[i] = null;
        useIndex = 0;
    }

    public void save(World context) {
        if (Thread.currentThread() == owner && context != null && !context.isRemote && registry != null
                && handler == context.getSaveHandler()) registry.save();
    }

    private boolean eligible(EntityPlayer observer) {
        return world != null && world.worldProvider.worldType == 0 && observer != null && observer.worldObj == world
                && observer.isEntityAlive() && !observer.capabilities.isCreativeMode && !observer.isPlayerSleeping();
    }

    public void tick(EntityPlayer observer, boolean paused) {
        if (Thread.currentThread() != owner || paused || registry == null) return;
        if (!eligible(observer)) observer = null;
        if (observer != player) resetPlayer(observer);
        if (player == null) return;
        tick++;
        int x = MathHelper.floor_double(player.posX), y = MathHelper.floor_double(player.boundingBox.minY);
        int z = MathHelper.floor_double(player.posZ);
        survey.reads = 0;
        if (!ProvenanceStore.valid(x - SiteSurvey.RADIUS, y, z - SiteSurvey.RADIUS)
                || !ProvenanceStore.valid(x + SiteSurvey.RADIUS, y, z + SiteSurvey.RADIUS)) {
            survey.active = false;
            return;
        }
        rememberWalk(x, y, z);
        if (delay > 0) delay--;
        if (survey.active && !survey.near(x, y, z, 3, 2)) survey.active = false;
        if (!survey.active) {
            if (delay > 0) return;
            survey.start(x, y, z);
            delay = INTERVAL;
        }
        if (!survey.sample(world, provenance)) {
            survey.active = false;
            publish(new Decision(null, "", "Terrain unavailable", -1, 0, 0, -1, null));
            return;
        }
        peakReads = Math.max(peakReads, survey.reads);
        if (survey.layer == SiteSurvey.LAYERS) evaluate(survey.finish());
    }

    private void rememberWalk(int x, int y, int z) {
        if (walked == 0 || Math.abs(anchorX - x) > 3 || Math.abs(anchorY - y) > 2 || Math.abs(anchorZ - z) > 3) {
            walked = 0;
            anchorX = x;
            anchorY = y;
            anchorZ = z;
        }
        if (walked == RECENT_LIMIT) return;
        for (int i = 0; i < walked; i++) if (walkedX[i] == x && walkedZ[i] == z) return;
        walkedX[walked] = x;
        walkedY[walked] = y;
        walkedZ[walked++] = z;
    }

    private void recordUse(Object[] context, int[] target) {
        if (Thread.currentThread() != owner || context[0] != world || context[1] != player || !eligible(player)
                || !SiteSurvey.utility(target[3])) return;
        Use previous = recentUse[(useIndex + RECENT_LIMIT - 1) % RECENT_LIMIT];
        if (previous != null && tick - previous.when < 20) return;
        recentUse[useIndex] = new Use(target[0], target[1], target[2], tick);
        useIndex = (useIndex + 1) % RECENT_LIMIT;
    }

    private void evaluate(Evidence evidence) {
        int activity = territory.activityAt(world, survey.x, survey.z);
        long clock = territory.observationClock(world);
        Bounds bounds = evidence.bounds;
        int steps = 0, uses = 0;
        if (bounds != null) {
            for (int i = 0; i < walked; i++) if (bounds.contains(walkedX[i], walkedY[i], walkedZ[i], 0)) steps++;
            for (Use use : recentUse) {
                if (use != null && tick - use.when <= USE_MEMORY && bounds.contains(use.x, use.y, use.z, 1)
                        && survey.sampledUtility(use.x, use.y, use.z)) uses++;
            }
        }
        boolean observed = evidence.manual >= 8;
        boolean familiar = evidence.inferred >= 16 && uses >= 2;
        boolean shelter = evidence.cover >= 4;
        Site known = bounds == null ? null : registry.matching(bounds);
        if (known == null) known = registry.containing(survey.x, survey.y, survey.z);
        String state = "No site";
        String reason;
        if (known != null) {
            state = "Recognized";
            if (evidence.spread() && (evidence.manual >= 4 || evidence.inferred >= 8)
                    && (steps >= 2 || uses > 0) && clock >= 0) {
                registry.reobserve(known, clock, observed);
                reason = "Existing site re-observed";
            } else {
                reason = "Identity retained. Current evidence is weak.";
            }
        } else if (!registry.writable()) {
            reason = "Registry unavailable";
        } else if (!evidence.spread() || (evidence.manual < 6 && evidence.inferred < 12)) {
            reason = "Insufficient connected construction";
        } else if (!shelter && uses == 0) {
            reason = "Exposed structure needs evidence of use";
        } else if (activity < 1 && uses < 2) {
            reason = "Insufficient local activity";
        } else {
            state = "Candidate";
            if (evidence.clipped) {
                reason = "Construction reaches the survey edge";
            } else if (!observed && !familiar) {
                reason = "Unknown authorship. Needs repeated utility use.";
            } else if (steps < 4 || (activity < 2 && uses < 2) || clock < 0) {
                reason = "Structure found. Needs corroborating local use.";
            } else {
                known = registry.promote(bounds, clock, observed);
                state = known == null ? "Candidate" : "Recognized";
                reason = known == null ? "Registry capacity reached"
                        : observed ? "Construction and local use corroborated" : "Structure and repeated utility use corroborated";
            }
        }
        publish(new Decision(evidence, state, reason, activity, steps, uses, clock, known));
    }

    private void publish(Decision decision) {
        latest = decision;
        resultX = survey.x;
        resultY = survey.y;
        resultZ = survey.z;
    }

    public Snapshot snapshot(World context, int x, int y, int z) {
        if (Thread.currentThread() != owner || context == null || context != world || registry == null) return null;
        boolean overworld = context.worldProvider.worldType == 0;
        boolean nearResult = Math.abs(x - resultX) <= 3 && Math.abs(y - resultY) <= 2 && Math.abs(z - resultZ) <= 3;
        Decision local = overworld && nearResult ? latest : null;
        Site site = overworld ? registry.containing(x, y, z) : null;
        if (site == null && local != null) site = local.site;
        boolean useActive = false;
        try {
            if (useHook != null) useActive = (Boolean) useHook.invoke(null, World.class.getClassLoader());
        } catch (ReflectiveOperationException unavailable) {
            useHook = null;
        }
        return new Snapshot(overworld ? registry.status : "OUTSIDE_OVERWORLD", registry.size(), site, local,
                useActive, survey.active, survey.reads, peakReads);
    }

    private static final class Use {
        final int x, y, z;
        final long when;

        Use(int x, int y, int z, long when) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.when = when;
        }
    }

    private static final class Decision {
        final Evidence evidence;
        final String state, reason;
        final int activity, walked, uses;
        final long clock;
        final Site site;

        Decision(Evidence evidence, String state, String reason, int activity, int walked, int uses,
                 long clock, Site site) {
            this.evidence = evidence;
            this.state = state;
            this.reason = reason;
            this.activity = activity;
            this.walked = walked;
            this.uses = uses;
            this.clock = clock;
            this.site = site;
        }
    }

    public static final class Snapshot {
        public final String registry, state, confidence, reason, boundary;
        public final int id, siteCount, placed, inferred, utilities, cover, activity, walked, uses, reads, peakReads;
        public final long observedTick;
        public final boolean useHook, queued;

        private Snapshot(String registry, int count, Site site, Decision decision, boolean useHook, boolean queued,
                         int reads, int peakReads) {
            this.registry = registry;
            this.useHook = useHook;
            this.queued = queued;
            this.reads = reads;
            this.peakReads = peakReads;
            // A cached re-evaluation may belong to a different nearby site.
            if (site != null && decision != null && decision.site != null && decision.site != site) decision = null;
            Evidence evidence = decision == null ? null : decision.evidence;
            Bounds box = site != null ? site.bounds : evidence == null ? null : evidence.bounds;
            siteCount = count;
            id = site == null ? 0 : site.id;
            state = site != null ? "Recognized" : evidence == null ? "Unknown" : decision.state;
            confidence = site != null ? (site.observed ? "Observed construction" : "Inferred. Authorship unknown.")
                    : "Insufficient for recognition";
            reason = decision == null ? "Awaiting local survey" : decision.reason;
            boundary = box == null ? "-" : box.toString();
            placed = evidence == null ? -1 : evidence.manual;
            inferred = evidence == null ? -1 : evidence.inferred;
            utilities = evidence == null ? -1 : evidence.utility;
            cover = evidence == null ? -1 : evidence.cover;
            activity = decision == null ? -1 : decision.activity;
            walked = decision == null ? -1 : decision.walked;
            uses = decision == null ? -1 : decision.uses;
            observedTick = site != null ? site.lastSeen : decision != null && "Candidate".equals(state) ? decision.clock : -1;
        }
    }
}
