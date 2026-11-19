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
    private int anchorX, anchorY, anchorZ, walked, walkIndex, useIndex, delay, peakReads;
    private int resultX, resultY, resultZ;
    private Decision latest;

    public SettlementRecognition(TerritorialActivityService territory, WorldProvenance provenance) {
        this.territory = territory;
        this.provenance = provenance;
        provenance.trackSurvey(survey);
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
        walkIndex = 0;
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
        // Keep the original cycle deadline after an edit. A busy build cannot force extra slices.
        if (survey.changed || !survey.sample(world, provenance)) {
            survey.active = false;
            Decision unavailable = new Decision(survey, tick, null, -1, -1);
            unavailable.reason = survey.changed ? "Terrain changed" : "Terrain unavailable";
            publish(unavailable);
            return;
        }
        peakReads = Math.max(peakReads, survey.reads);
        if (survey.layer == SiteSurvey.LAYERS) evaluate(survey.finish());
    }

    private void rememberWalk(int x, int y, int z) {
        if (walked == 0 || Math.abs(anchorX - x) > 6 || Math.abs(anchorY - y) > 2 || Math.abs(anchorZ - z) > 6) {
            walked = 0;
            walkIndex = 0;
            anchorX = x;
            anchorY = y;
            anchorZ = z;
        }
        for (int i = 0; i < walked; i++) if (walkedX[i] == x && walkedZ[i] == z) return;
        walkedX[walkIndex] = x;
        walkedY[walkIndex] = y;
        walkedZ[walkIndex] = z;
        walkIndex = (walkIndex + 1) % RECENT_LIMIT;
        walked = Math.min(RECENT_LIMIT, walked + 1);
    }

    private void recordUse(Object[] context, int[] target) {
        if (Thread.currentThread() != owner || context[0] != world || context[1] != player || !eligible(player)
                || !SiteSurvey.utility(target[3])) return;
        Use previous = recentUse[(useIndex + RECENT_LIMIT - 1) % RECENT_LIMIT];
        if (previous != null && tick - previous.when < 20) return;
        recentUse[useIndex] = new Use(target[0], target[1], target[2], SiteSurvey.utilityType(target[3]), tick);
        useIndex = (useIndex + 1) % RECENT_LIMIT;
    }

    private void evaluate(Evidence evidence) {
        int activity = territory.activityAt(world, survey.focusX, survey.focusZ);
        long clock = territory.observationClock(world);
        Bounds bounds = evidence.bounds;
        Decision result = new Decision(survey, tick, evidence, activity, clock);
        result.cover = evidence.anchorCover;
        if (bounds != null) {
            for (int i = 0; i < walked; i++) {
                int x = walkedX[i], y = walkedY[i], z = walkedZ[i];
                if (!bounds.contains(x, y, z, 0) || Math.abs(y - survey.focusY) > 1) continue;
                result.walked++;
                if (result.cover < 9 && survey.walkable(x, y, z))
                    result.cover = Math.max(result.cover, survey.cover(evidence, x, y, z));
            }
            for (Use use : recentUse) {
                if (use == null) continue;
                if (!survey.associatedUtility(evidence, use.x, use.y, use.z, use.type)) result.outside[use.type]++;
                else if (tick - use.when > USE_MEMORY) result.expired[use.type]++;
                else {
                    result.opened[use.type]++;
                    result.uses++;
                }
            }
        }
        classify(result);
        publish(result);
    }

    private void classify(Decision result) {
        Evidence evidence = result.evidence;
        Bounds bounds = evidence.bounds;
        int activity = result.activity, steps = result.walked, uses = result.uses;
        long clock = result.clock;
        boolean observed = evidence.manual >= 8;
        boolean familiar = evidence.inferred >= 16 && uses >= 2;
        boolean shelter = result.cover >= 4;
        Site known = bounds == null ? null : registry.matching(bounds);
        if (known == null) known = registry.containing(survey.focusX, survey.focusY, survey.focusZ);
        result.site = known;
        if (survey.recenter(evidence)) {
            result.state = known == null ? "Candidate" : "Recognized";
            result.reason = "Partial footprint. Sampling closer to the clipped edge.";
            return;
        }
        if (known != null) {
            result.state = "Recognized";
            if (evidence.faces == 0 && evidence.spread() && (evidence.manual >= 4 || evidence.inferred >= 8)
                    && (steps >= 2 || uses > 0) && clock >= 0) {
                registry.reobserve(known, clock, observed);
                result.reason = observed && (shelter || uses > 0) && registry.expand(known, bounds)
                        ? "Existing site footprint expanded" : "Existing site re-observed";
            } else {
                result.reason = "Identity retained. Current footprint is partial or evidence is weak.";
            }
        } else if (!registry.writable()) {
            result.reason = "Registry unavailable";
        } else if (!evidence.spread() || (evidence.manual < 6 && evidence.inferred < 12)) {
            result.reason = "Insufficient connected construction";
        } else if (evidence.faces != 0) {
            result.state = "Candidate";
            result.reason = "Construction reaches the survey edge";
        } else if (!shelter && uses == 0) {
            result.reason = "Little cover in this sample. Open a local utility to corroborate use.";
        } else if (activity < 1 && uses < 2) {
            result.reason = "Insufficient local activity";
        } else {
            result.state = "Candidate";
            if (!observed && !familiar) {
                result.reason = "Unknown authorship. Needs repeated utility use.";
            } else if (steps < 4 || (activity < 2 && uses < 2) || clock < 0) {
                result.reason = "Structure found. Needs corroborating local use.";
            } else {
                result.site = registry.promote(bounds, clock, observed);
                result.state = result.site == null ? "Candidate" : "Recognized";
                result.reason = result.site == null ? "Registry capacity reached"
                        : observed ? "Construction and local use corroborated" : "Structure and repeated utility use corroborated";
            }
        }
    }

    private void publish(Decision decision) {
        latest = decision;
        resultX = survey.focusX;
        resultY = survey.focusY;
        resultZ = survey.focusZ;
    }

    public Snapshot snapshot(World context, int x, int y, int z) {
        if (Thread.currentThread() != owner || context == null || context != world || registry == null) return null;
        boolean overworld = context.worldProvider.worldType == 0;
        boolean nearResult = Math.abs(x - resultX) <= 3 && Math.abs(y - resultY) <= 2 && Math.abs(z - resultZ) <= 3;
        Decision local = overworld && latest != null && (nearResult || latest.evidence != null
                && latest.evidence.bounds != null && latest.evidence.bounds.contains(x, y, z, 0)) ? latest : null;
        Site site = overworld ? registry.containing(x, y, z) : null;
        if (site == null && local != null) site = local.site;
        boolean useActive = false;
        try {
            if (useHook != null) useActive = (Boolean) useHook.invoke(null, World.class.getClassLoader());
        } catch (ReflectiveOperationException unavailable) {
            useHook = null;
        }
        boolean pending = overworld && survey.active && survey.near(x, y, z, 3, 2);
        return new Snapshot(overworld ? registry.status : "OUTSIDE_OVERWORLD", registry.size(), site, local,
                useActive, pending, survey.reads, peakReads, tick, pending ? survey.extent() : null);
    }

    private static final class Use {
        final int x, y, z, type;
        final long when;

        Use(int x, int y, int z, int type, long when) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.when = when;
        }
    }

    private static final class Decision {
        final Evidence evidence;
        final int activity, anchorX, anchorY, anchorZ, footY, layers;
        final long clock, tick;
        final Bounds extent;
        final int[] opened = new int[4], expired = new int[4], outside = new int[4];
        int walked, uses, cover;
        String state = "No site", reason;
        Site site;

        Decision(SiteSurvey survey, long tick, Evidence evidence, int activity, long clock) {
            anchorX = survey.x;
            anchorY = survey.y;
            anchorZ = survey.z;
            footY = survey.focusY;
            layers = survey.layer;
            extent = survey.extent();
            this.tick = tick;
            this.evidence = evidence;
            this.activity = activity;
            this.clock = clock;
        }
    }

    public static final class Snapshot {
        public final String registry, state, confidence, reason, boundary, component, assessment, extent, pendingExtent, clipping;
        public final int id, siteCount, placed, inferred, utilities, cover, anchorCover, clipFaces, activity, walked, uses, reads, peakReads;
        public final int anchorX, anchorY, anchorZ, footY, sampledLayers;
        public final long observedTick, assessedTick, assessmentAge;
        public final boolean useHook, queued;
        public final UtilitySnapshot workbench, furnace, chest;

        private Snapshot(String registry, int count, Site site, Decision decision, boolean useHook, boolean queued,
                         int reads, int peakReads, long tick, Bounds pendingExtent) {
            this.registry = registry;
            this.useHook = useHook;
            this.queued = queued;
            this.reads = reads;
            this.peakReads = peakReads;
            this.pendingExtent = pendingExtent == null ? "-" : pendingExtent.toString();
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
            component = evidence == null || evidence.bounds == null ? "-" : evidence.bounds.toString();
            assessment = decision == null ? "Awaiting survey" : evidence == null ? decision.reason
                    : evidence.faces != 0 ? "Partial footprint" : "Complete";
            extent = decision == null ? "-" : decision.extent.toString();
            anchorX = decision == null ? 0 : decision.anchorX;
            anchorY = decision == null ? 0 : decision.anchorY;
            anchorZ = decision == null ? 0 : decision.anchorZ;
            footY = decision == null ? 0 : decision.footY;
            sampledLayers = decision == null ? 0 : decision.layers;
            assessedTick = decision == null ? -1 : decision.tick;
            assessmentAge = decision == null ? -1 : tick - decision.tick;
            placed = evidence == null ? -1 : evidence.manual;
            inferred = evidence == null ? -1 : evidence.inferred;
            workbench = new UtilitySnapshot(decision, 1);
            furnace = new UtilitySnapshot(decision, 2);
            chest = new UtilitySnapshot(decision, 3);
            utilities = evidence == null ? -1 : workbench.associated + furnace.associated + chest.associated;
            cover = evidence == null ? -1 : decision.cover;
            anchorCover = evidence == null ? -1 : evidence.anchorCover;
            clipFaces = evidence == null ? 0 : evidence.faces;
            clipping = SiteSurvey.clipping(clipFaces);
            activity = decision == null ? -1 : decision.activity;
            walked = decision == null ? -1 : decision.walked;
            uses = decision == null ? -1 : decision.uses;
            observedTick = site == null ? -1 : site.lastSeen;
        }
    }

    public static final class UtilitySnapshot {
        public final int found, associated, opened, expired, outside;

        private UtilitySnapshot(Decision decision, int type) {
            Evidence evidence = decision == null ? null : decision.evidence;
            found = evidence == null ? -1 : evidence.found[type];
            associated = evidence == null ? -1 : evidence.associated[type];
            opened = evidence == null ? -1 : decision.opened[type];
            expired = evidence == null ? -1 : decision.expired[type];
            outside = evidence == null ? -1 : decision.outside[type];
        }
    }
}
