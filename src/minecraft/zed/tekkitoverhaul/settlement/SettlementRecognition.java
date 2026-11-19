package zed.tekkitoverhaul.settlement;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteRegistry.Match;
import zed.tekkitoverhaul.settlement.SiteRegistry.Part;
import zed.tekkitoverhaul.settlement.SiteRegistry.Site;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;
import zed.tekkitoverhaul.settlement.SiteSurvey.Evidence;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Evaluates the current Survival player's work area independently of encounters. */
public final class SettlementRecognition implements SiteQueries {
    private static final String KEY = "zto_settlement_sites";
    private static final int INTERVAL = 100, USE_MEMORY = 1200, RECENT_LIMIT = 16;
    private static final int LINK_TIME = 400, MAX_GAP = 8, MAX_ROUTE = 24, DORMANT_AFTER = 24000;
    public static final int MAX_QUERY_RADIUS = 24, MAX_QUERY_SITES = 16;
    private final Thread owner = Thread.currentThread();
    private final TerritorialActivityService territory;
    private final WorldProvenance provenance;
    private final SiteSurvey survey = new SiteSurvey();
    private final ProtectedInterior interior = new ProtectedInterior();
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
    private long tick, worldSession, routeEpoch, routeLength;
    private int routeX, routeY, routeZ;
    private boolean routeStarted;
    private int anchorX, anchorY, anchorZ, walked, walkIndex, useIndex, delay, peakReads;
    private int resultX, resultY, resultZ;
    private Decision latest;
    private ProtectedInterior.Assessment localProtection;
    private boolean protectionSlot;

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
        worldSession++;
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
        protectionSlot = false;
        localProtection = null;
        routeStarted = false;
        routeLength = 0;
        routeEpoch++;
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
        protectionSlot = true;
        localProtection = null;
        survey.reads = 0;
        if (player == null) return;
        tick++;
        int x = MathHelper.floor_double(player.posX), y = MathHelper.floor_double(player.boundingBox.minY);
        int z = MathHelper.floor_double(player.posZ);
        rememberRoute(x, y, z);
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
            // A doorway can be outside the stored bounds even when the last window matched the site.
            Match match = registry.match(null, survey.focusX, survey.focusY, survey.focusZ);
            if (!match.ambiguous) {
                unavailable.site = match.site;
                unavailable.part = match.part;
            }
            if (unavailable.site == null && !match.ambiguous && latest != null
                    && resultX == survey.focusX && resultY == survey.focusY && resultZ == survey.focusZ) {
                unavailable.site = latest.site;
                unavailable.part = latest.part;
            }
            publish(unavailable);
            return;
        }
        peakReads = Math.max(peakReads, survey.reads);
        if (survey.layer == SiteSurvey.LAYERS) evaluate(survey.finish());
    }

    private void rememberRoute(int x, int y, int z) {
        if (routeStarted) {
            int dx = Math.abs(x - routeX), dy = Math.abs(y - routeY), dz = Math.abs(z - routeZ);
            if (dx > 1 || dy > 1 || dz > 1) {
                routeEpoch++;
                routeLength = 0;
            } else {
                routeLength += dx + dy + dz;
            }
        }
        routeStarted = true;
        routeX = x;
        routeY = y;
        routeZ = z;
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
        boolean nearby = routeStarted && Math.abs(target[0] - routeX) + Math.abs(target[2] - routeZ) <= 4
                && Math.abs(target[1] - routeY) <= 1;
        recentUse[useIndex] = new Use(target[0], target[1], target[2], SiteSurvey.utilityType(target[3]), tick,
                nearby ? routeEpoch : -1, routeLength);
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
                    if (use.epoch == routeEpoch) {
                        if (result.firstUse == null || use.when < result.firstUse.when) result.firstUse = use;
                        result.localOpenings++;
                    }
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
        Match match = registry.match(bounds, survey.focusX, survey.focusY, survey.focusZ);
        if (match.ambiguous) {
            result.state = "Ambiguous";
            result.reason = "Survey overlaps multiple known sites";
            survey.recenter(evidence);
            return;
        }
        Site known = match.site;
        result.site = known;
        result.part = match.part;
        if (survey.recenter(evidence)) {
            result.state = known == null ? "Candidate" : "Recognized";
            result.reason = "Partial footprint. Sampling closer to the clipped edge.";
            return;
        }
        if (known != null) {
            result.state = "Recognized";
            Part part = result.part;
            boolean fullPart = survey.completelyCovers(part.bounds);
            if (evidence.faces == 0 && evidence.spread() && (evidence.manual >= 4 || evidence.inferred >= 8)
                    && (steps >= 2 || uses > 0) && clock >= 0 && match.overlap > 0) {
                registry.reobserve(known, part, clock, evidence.manual, evidence.inferred, uses);
                confirmUses(result);
                result.confirmed = true;
                if (observed && (shelter || uses > 0) && registry.expand(known, part, bounds)) {
                    result.reason = "Existing site footprint expanded";
                } else if (fullPart && observed && part.bounds.covers(bounds) && part.bounds.volume() > bounds.volume()) {
                    if (registry.contract(known, part, bounds)) result.reason = "Existing site footprint narrowed";
                    else {
                        result.confirmed = false;
                        result.reason = "Smaller footprint awaiting repeated confirmation";
                    }
                } else {
                    if (fullPart) part.resetContraction();
                    result.reason = "Existing site re-observed";
                }
            } else if (evidence.faces == 0 && fullPart && !survey.constructionRemains(part.bounds)) {
                result.reason = registry.contract(known, part, null)
                        ? "Removed annex retired from footprint" : "Missing part awaiting repeated confirmation";
                if (!known.parts.contains(part)) result.part = null;
            } else {
                if (evidence.faces == 0 && fullPart) part.resetContraction();
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
                Match link = observed ? relatedPart(result) : new Match();
                if (link.ambiguous) {
                    result.state = "Ambiguous";
                    result.reason = "Local use connects more than one known site";
                    return;
                }
                if (link.site != null) {
                    if (result.localOpenings < 2) {
                        result.reason = "Related work area needs another utility opening";
                        return;
                    }
                    result.part = registry.attach(link.site, bounds, clock, evidence.manual, evidence.inferred, uses);
                    if (result.part == null) {
                        result.reason = "Related part overlaps known construction or reaches capacity";
                        return;
                    }
                    result.site = link.site;
                    result.reason = "Work area linked by observed construction and utility use";
                } else {
                    result.site = registry.promote(bounds, clock, evidence.manual, evidence.inferred, uses);
                    result.reason = result.site == null ? "Registry capacity reached"
                            : observed ? "Construction and local use corroborated" : "Structure and repeated utility use corroborated";
                    if (result.site != null) result.part = result.site.parts.get(0);
                }
                result.state = result.site == null ? "Candidate" : "Recognized";
                result.confirmed = result.part != null;
                if (result.confirmed) confirmUses(result);
            }
        }
    }

    private void confirmUses(Decision result) {
        for (Use use : recentUse) {
            if (use != null && use.epoch == routeEpoch && tick - use.when <= LINK_TIME
                    && survey.associatedUtility(result.evidence, use.x, use.y, use.z, use.type)) {
                use.site = result.site;
                use.part = result.part;
            }
        }
    }

    private Match relatedPart(Decision result) {
        Match match = new Match();
        Use first = result.firstUse;
        if (first == null || tick - first.when > LINK_TIME) return match;
        Bounds target = result.evidence.bounds;
        for (Use use : recentUse) {
            if (use == null || use.site == null || use.epoch != first.epoch || use.when >= first.when
                    || first.when - use.when > LINK_TIME || first.distance - use.distance < 3
                    || first.distance - use.distance > MAX_ROUTE || !use.site.parts.contains(use.part)) continue;
            Part part = use.part;
            Bounds origin = part.bounds;
            if (!part.observed || origin.horizontalGap(target) > MAX_GAP || origin.overlap(target) != 0
                    || Math.abs(origin.minY - target.minY) > 2 || Math.abs(use.y - first.y) > 2) continue;
            int corroboration = 0;
            for (Use earlier : recentUse) {
                if (earlier != null && earlier.part == part && earlier.epoch == first.epoch
                        && earlier.when <= use.when && first.when - earlier.when <= LINK_TIME) corroboration++;
            }
            if (corroboration < 2) continue;
            if (match.site != null && match.site != use.site) {
                match.ambiguous = true;
                return match;
            }
            match.site = use.site;
        }
        return match;
    }

    private void publish(Decision decision) {
        latest = decision;
        resultX = survey.focusX;
        resultY = survey.focusY;
        resultZ = survey.focusZ;
        if (decision.part != null) {
            decision.part.session = worldSession;
            decision.part.checkedAt = tick;
            decision.part.coverage = decision.evidence == null && "Terrain unavailable".equals(decision.reason)
                    ? Coverage.UNAVAILABLE : decision.confirmed ? Coverage.COMPLETE : Coverage.PARTIAL;
        }
    }

    private boolean available(World context) {
        return Thread.currentThread() == owner && context != null && context == world && registry != null
                && context.worldProvider.worldType == 0;
    }

    /** Resolve a persisted identity, including when the last confirmed parts are unavailable. */
    @Override public SiteSnapshot resolve(World context, int id) {
        if (!available(context)) return null;
        Site site = registry.byId(id);
        return site == null ? null : describe(site);
    }

    /** Up to sixteen recognized sites in ID order. At least one intersecting part must be loaded. */
    @Override public List<SiteSnapshot> nearby(World context, int x, int y, int z, int radius) {
        if (!available(context) || !ProvenanceStore.valid(x, y, z) || radius < 0 || radius > MAX_QUERY_RADIUS)
            return Collections.emptyList();
        Bounds area = new Bounds(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<SiteSnapshot> result = new ArrayList<SiteSnapshot>();
        for (Site site : registry.sites) {
            if (site.bounds.overlap(area) == 0) continue;
            for (Part part : site.parts) {
                if (part.bounds.overlap(area) > 0 && loaded(part.bounds)) {
                    result.add(describe(site));
                    break;
                }
            }
            if (result.size() == MAX_QUERY_SITES) break;
        }
        return Collections.unmodifiableList(result);
    }

    @Override public Point locate(World context, int x, int y, int z) {
        if (!available(context) || !ProvenanceStore.valid(x, y, z))
            return new Point(Recognition.UNKNOWN, null, "World or position unavailable");
        Match match = registry.match(null, x, y, z);
        boolean local = latest != null && tick - latest.tick <= INTERVAL * 2 && latest.evidence != null
                && latest.evidence.bounds != null && latest.evidence.bounds.contains(x, y, z);
        if (match.ambiguous || (local && "Ambiguous".equals(latest.state)))
            return new Point(Recognition.AMBIGUOUS, null, "Observation has competing site evidence");
        if (match.site != null) return new Point(Recognition.RECOGNIZED, describe(match.site), "Within a known part");
        if (local && "Candidate".equals(latest.state))
            return new Point(Recognition.CANDIDATE, null, latest.reason);
        return new Point(Recognition.UNKNOWN, null, "No known part at this point");
    }

    private SiteSnapshot describe(Site site) {
        long clock = territory.observationClock(world);
        boolean dormant = stale(clock, site.lastSeen);
        int unavailable = 0, complete = 0;
        List<SiteSnapshot.Part> parts = new ArrayList<SiteSnapshot.Part>(site.parts.size());
        for (Part part : site.parts) {
            Coverage coverage;
            boolean recent = part.session == worldSession && tick - part.checkedAt <= INTERVAL * 2;
            if (!loaded(part.bounds) || (recent && part.coverage == Coverage.UNAVAILABLE)) {
                coverage = Coverage.UNAVAILABLE;
                unavailable++;
            } else if (stale(clock, part.lastSeen)) {
                coverage = Coverage.STALE;
            } else {
                coverage = recent ? part.coverage : Coverage.PARTIAL;
                if (coverage == Coverage.COMPLETE) complete++;
            }
            parts.add(new SiteSnapshot.Part(part, coverage));
        }
        Coverage overall = unavailable == parts.size() ? Coverage.UNAVAILABLE : dormant ? Coverage.STALE
                : complete == parts.size() ? Coverage.COMPLETE : Coverage.PARTIAL;
        return new SiteSnapshot(worldSession, world.worldProvider.worldType, site, parts, overall, dormant);
    }

    private boolean loaded(Bounds bounds) {
        for (int x = bounds.minX >> 4; x <= bounds.maxX >> 4; x++) {
            for (int z = bounds.minZ >> 4; z <= bounds.maxZ >> 4; z++) {
                if (!world.blockExists(x << 4, bounds.minY, z << 4)) return false;
            }
        }
        return true;
    }

    private static boolean stale(long current, long confirmed) {
        return current < confirmed || current - confirmed > DORMANT_AFTER;
    }

    /**
     * Query after the recognition tick. One independent geometry computation can use the idle
     * tick's read budget. The same point can reuse that tick's immutable result.
     */
    @Override public ProtectedInterior.Assessment protection(World context, int x, int y, int z) {
        if (!available(context) || !ProvenanceStore.valid(x, y, z))
            return ProtectedInterior.unknown(x, y, z, "World or position unavailable");
        if (localProtection != null && localProtection.x == x && localProtection.y == y && localProtection.z == z)
            return localProtection;
        if (!protectionSlot || survey.reads != 0)
            return ProtectedInterior.unknown(x, y, z, "Protection budget reserved until an idle survey tick");
        protectionSlot = false;
        localProtection = interior.assess(world, x, y, z);
        return localProtection;
    }

    public Snapshot snapshot(World context, int x, int y, int z) {
        if (Thread.currentThread() != owner || context == null || context != world || registry == null) return null;
        boolean overworld = context.worldProvider.worldType == 0;
        boolean nearResult = Math.abs(x - resultX) <= 3 && Math.abs(y - resultY) <= 2 && Math.abs(z - resultZ) <= 3;
        Decision local = overworld && latest != null && (nearResult || latest.evidence != null
                && latest.evidence.bounds != null && latest.evidence.bounds.contains(x, y, z, 0)) ? latest : null;
        Match atPoint = overworld ? registry.match(null, x, y, z) : new Match();
        Site site = atPoint.site;
        if (atPoint.ambiguous || (local != null && "Ambiguous".equals(local.state))) site = null;
        if (site == null && local != null) site = local.site;
        boolean useActive = false;
        try {
            if (useHook != null) useActive = (Boolean) useHook.invoke(null, World.class.getClassLoader());
        } catch (ReflectiveOperationException unavailable) {
            useHook = null;
        }
        boolean pending = overworld && survey.active && survey.near(x, y, z, 3, 2);
        return new Snapshot(overworld ? registry.status : "OUTSIDE_OVERWORLD", registry.size(), site, local,
                useActive, pending, survey.reads, peakReads, tick, pending ? survey.extent() : null, atPoint.ambiguous);
    }

    private static final class Use {
        final int x, y, z, type;
        final long when, epoch, distance;
        Site site;
        Part part;

        Use(int x, int y, int z, int type, long when, long epoch, long distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.when = when;
            this.epoch = epoch;
            this.distance = distance;
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
        Part part;
        Use firstUse;
        int localOpenings;
        boolean confirmed;

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
        public final int anchorX, anchorY, anchorZ, footY, sampledLayers, knownParts;
        public final long observedTick, assessedTick, assessmentAge;
        public final boolean useHook, queued;
        public final UtilitySnapshot workbench, furnace, chest;

        private Snapshot(String registry, int count, Site site, Decision decision, boolean useHook, boolean queued,
                         int reads, int peakReads, long tick, Bounds pendingExtent, boolean ambiguous) {
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
            knownParts = site == null ? 0 : site.parts.size();
            state = ambiguous ? "Ambiguous" : site != null ? "Recognized" : evidence == null ? "Unknown" : decision.state;
            confidence = site != null ? (site.observed ? "Observed construction" : "Inferred. Authorship unknown.")
                    : "Insufficient for recognition";
            reason = ambiguous ? "Point overlaps multiple known parts" : decision == null ? "Awaiting local survey" : decision.reason;
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
