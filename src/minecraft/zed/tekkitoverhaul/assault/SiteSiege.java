package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.EnumSkyBlock;
import net.minecraft.src.MathHelper;
import net.minecraft.src.PathPoint;
import net.minecraft.src.SpawnerAnimals;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.settlement.ProtectedInterior;
import zed.tekkitoverhaul.settlement.SettlementRecognition;
import zed.tekkitoverhaul.settlement.SiteQueries;
import zed.tekkitoverhaul.settlement.SiteSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;

/** Owns one bounded site objective. The director owns the lunar boundary and the major encounter decision. */
public final class SiteSiege {
    enum Admission { WAIT, FALLBACK, COMMITTED }
    private static final int OPTIONS = 12, MAX_PARTS = 2, PLANNING_TICKS = 40;
    private static final int GROUP = 3, SPAWN_INTERVAL = 20, MAX_INSERTION_ATTEMPTS = 7;
    private static final int DEADLINE = 600, STALL = 120, PRESENCE = 48;
    private static final int LIGHT_WAIT = 900, MAX_LIGHT_ROLLS = 20;
    private final boolean enabled;
    private final SiteQueries sites;
    private final List<Attacker> attackers = new ArrayList<Attacker>();
    private final List<Candidate> candidates = new ArrayList<Candidate>();
    private World world;
    private AssaultWorldState storage;
    private SiteSnapshot selected;
    private SiteSnapshot observed;
    private SiteApproach approach;
    private final List<SiteApproach> frontages = new ArrayList<SiteApproach>();
    private Bounds part;
    private EntityPlayer observer;
    private long boundary = Long.MIN_VALUE, lunarSession;
    private int optionsTried, frames, attempts, inserted, peak, elapsed, arrivals, steps, peakColumns, peakSkyReads;
    private int lastVerified, lightRolls;
    private long nextCheck;
    private long columns, skyReads, heightReads, protectionReads, planningNanos, peakPlanningNanos;
    private double progress;
    private String phase = "IDLE", status, rejection = "NONE", outcome = "NONE";
    private boolean opportunity;

    SiteSiege(boolean enabled, SiteQueries sites) {
        this.enabled = enabled;
        this.sites = sites;
        status = enabled ? "WAITING_FOR_NATURAL_FULL_MOON" : "DISABLED";
    }

    void bind(World world, AssaultWorldState storage) {
        this.world = world;
        this.storage = storage;
        resetPlan();
        selected = observed = null;
        part = null;
        approach = null;
        boundary = Long.MIN_VALUE;
        opportunity = false;
        resetCounters();
        phase = "IDLE";
        status = !enabled ? "DISABLED" : storage == null ? "WORLD_UNAVAILABLE" : "WAITING_FOR_NATURAL_FULL_MOON";
        outcome = storage == null ? "NONE" : storage.siteOutcome;
    }

    boolean active() { return "ACTIVE".equals(phase); }
    boolean planning() { return "PLANNING".equals(phase); }

    Admission admit(EntityPlayer player, LunarSnapshot event) {
        if (!enabled) return Admission.FALLBACK;
        if (!planning()) {
            resetPlan();
            resetCounters();
            selected = observed = null;
            approach = null;
            part = null;
            boundary = event.rawTime;
            lunarSession = event.sessionId;
            opportunity = SiteSiegePolicy.opportunity(world.getSeed(), event.cycleIndex);
            if (!opportunity) return fallback("RARITY");
            long discovery = System.nanoTime();
            discover(player);
            accountTime(discovery);
            if (candidates.isEmpty()) return fallback("NO_ELIGIBLE_SITE");
            phase = "PLANNING";
            status = "VERIFYING_EXTERIOR";
        }
        if (world.getWorldTime() - boundary >= AssaultWave.FIRST_START - 1) return fallback("PLANNING_DEADLINE");
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            frames++;
            if (frames > PLANNING_TICKS) return fallback("PLANNING_DEADLINE");
            if (approach == null) nextApproach(sampler);
            if (approach == null) return plansExhausted() ? fallback("NO_VALID_APPROACH") : Admission.WAIT;
            // A second sample is always required, with a fresh budget, before the first insertion.
            if (sampler.columns != 0) return Admission.WAIT;
            EntitySkeleton mob = validateInsertion(player, sampler);
            if (mob == null) {
                if ("PROTECTION_DEFERRED".equals(rejection)) return Admission.WAIT;
                approach = null;
                return plansExhausted() ? fallback("NO_VALID_APPROACH") : Admission.WAIT;
            }
            String blocked = storage.siegeEligibility(selected.id, boundary);
            if (blocked != null) return fallback(blocked);
            if (!PerimeterApproachTask.available()) return fallback("NATIVE_TASK_UNAVAILABLE");
            storage.startSite(selected.id, boundary);
            observer = player;
            phase = "ACTIVE";
            status = "WAITING_FOR_DARKNESS";
            resetPlan();
            return Admission.COMMITTED;
        } finally {
            account(sampler, start);
        }
    }

    private void discover(final EntityPlayer player) {
        int x = MathHelper.floor_double(player.posX), y = MathHelper.floor_double(player.boundingBox.minY);
        int z = MathHelper.floor_double(player.posZ);
        for (SiteSnapshot site : sites.nearby(world, x, y, z, SettlementRecognition.MAX_QUERY_RADIUS)) {
            String reason = unavailable(site);
            if (reason == null) reason = storage.siegeEligibility(site.id, boundary);
            if (reason != null) {
                if (selected == null) selected = observed = site;
                rejection = reason;
                continue;
            }
            for (SiteSnapshot.Part candidate : site.parts) {
                if (usable(candidate.coverage)) candidates.add(new Candidate(site, candidate.bounds));
            }
        }
        Collections.sort(candidates, new Comparator<Candidate>() {
            @Override public int compare(Candidate a, Candidate b) {
                int order = Double.compare(Candidate.distance(a.part, player), Candidate.distance(b.part, player));
                return order != 0 ? order : Integer.compare(a.site.id, b.site.id);
            }
        });
        if (candidates.size() > MAX_PARTS) candidates.subList(MAX_PARTS, candidates.size()).clear();
    }

    private boolean plansExhausted() {
        return optionsTried >= candidates.size() * OPTIONS && frontages.isEmpty();
    }

    private void nextApproach(SiteApproach.Sampler sampler) {
        if (optionsTried % OPTIONS == 0 && !frontages.isEmpty()) {
            approach = frontages.remove(0);
            return;
        }
        if (plansExhausted()) return;
        Candidate candidate = candidates.get(optionsTried / OPTIONS);
        int option = optionsTried++ % OPTIONS;
        selected = candidate.site;
        part = candidate.part;
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null) return;
        SiteApproach plan = sampler.plan(part, observed.envelope, option);
        if (plan == null || !sampler.openSky(plan.start())) {
            rejection = sampler.rejection;
            return;
        }
        if (plan.entrance) approach = plan;
        else frontages.add(plan);
    }

    private EntitySkeleton validateInsertion(EntityPlayer player, SiteApproach.Sampler sampler) {
        PathPoint start = approach.start();
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null) return null;
        if (!nearSite(player)) return reject("PLAYER_ABSENT");
        for (SiteSnapshot neighbor : sites.nearby(world, start.xCoord, start.yCoord, start.zCoord,
                SettlementRecognition.MAX_QUERY_RADIUS)) {
            if (!SiteApproach.exterior(neighbor.envelope, start.xCoord, start.zCoord))
                return reject("INSIDE_SITE_EXCLUSION");
        }
        if (!SiteApproach.exterior(observed.envelope, start.xCoord, start.zCoord))
            return reject("INSIDE_SITE_EXCLUSION");
        if (!sampler.validate(approach) || !sampler.openSky(start)) return reject(sampler.rejection);
        lastVerified = elapsed;
        ProtectedInterior.Assessment protection = sites.protection(world, start.xCoord, start.yCoord, start.zCoord);
        protectionReads += protection.reads;
        if (protection.state == ProtectedInterior.State.PROTECTED) return reject("PROTECTED_INSERTION");
        if (protection.state == ProtectedInterior.State.UNKNOWN)
            return reject(protection.reads == 0 ? "PROTECTION_DEFERRED" : "UNCERTAIN_PROTECTION");
        double x = start.xCoord + 0.5, y = start.yCoord, z = start.zCoord + 0.5;
        if (world.getClosestPlayer(x, y, z, SpawnController.INNER_RADIUS) != null)
            return reject("PLAYER_TOO_CLOSE");
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(x - 32, y - 24, z - 32, x + 32, y + 24, z + 32);
        if (SpawnController.localHostiles(world, area) >= SpawnController.LOCAL_CAP)
            return reject("POPULATION_CAP");
        if (!SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world,
                start.xCoord, start.yCoord, start.zCoord)) return reject("UNSUITABLE_GROUND");
        EntitySkeleton mob = new EntitySkeleton(world);
        mob.setLocationAndAngles(x, y, z, 0, 0);
        String collision = SpawnController.placementCollision(world, mob);
        if (collision != null) return reject(collision);
        if (world.getSavedLightValue(EnumSkyBlock.Block, start.xCoord, start.yCoord, start.zCoord) > 7)
            return reject("INSERTION_TOO_BRIGHT");
        return mob;
    }

    void tick(EntityPlayer player, LunarSnapshot now, int activeCap) {
        if (now.sessionId != lunarSession || !AssaultSchedule.WINDOW.matches(now)) {
            finish("LUNAR_CONTEXT_LOST");
            return;
        }
        if (world.difficultySetting <= 0) {
            finish("PEACEFUL");
            return;
        }
        if (!SiegeDirector.available(world, player) || player != observer || !nearSite(player)) {
            finish("PLAYER_ABSENT");
            return;
        }
        if (attempts == 0) {
            if (now.rawTime % 5 == 0 && !refreshSite()) return;
            beginApproach(player, now.rawTime);
            return;
        }
        elapsed++;
        if (elapsed >= DEADLINE) {
            finish("DEADLINE");
            return;
        }
        if (elapsed % 5 == 0 && !refreshSite()) return;
        refreshAttackers();
        if (!active()) return;
        boolean reinforcing = elapsed % SPAWN_INTERVAL == 0 && attempts < MAX_INSERTION_ATTEMPTS
                && inserted < Math.min(GROUP, activeCap) && attackers.size() < activeCap;
        if (!reinforcing && elapsed % 5 == 0 && !observeRoute()) return;
        if (inserted > 0 && attackers.isEmpty()) {
            finish("ATTACKERS_GONE");
        } else if (reinforcing) {
            reinforce(player);
        } else if (attackers.isEmpty() && attempts >= MAX_INSERTION_ATTEMPTS) {
            finish("ALLOCATION_EXHAUSTED");
        } else if (elapsed >= 240 && arrivals == inserted && inserted > 0) {
            finish("FRONTAGE_REACHED");
        }
    }

    private boolean refreshSite() {
        observed = sites.resolve(world, selected.id);
        if (currentPart() != null) return true;
        finish(rejection);
        return false;
    }

    private void beginApproach(EntityPlayer player, long time) {
        if (time - boundary >= LIGHT_WAIT + 100) {
            finish("FIRST_INSERTION_TIMEOUT");
            return;
        }
        if (time < nextCheck) return;
        if (world.skylightSubtracted < 11 && time - boundary < LIGHT_WAIT) {
            nextCheck = time + 20;
            return;
        }
        status = "WAITING_FOR_NATIVE_SPAWN";
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            EntitySkeleton mob = validateInsertion(player, sampler);
            if (mob == null) {
                if (!"PROTECTION_DEFERRED".equals(rejection)) finish(rejection);
                return;
            }
            if (!mob.getCanSpawnHere()) {
                rejection = "VANILLA_SPAWN_RULE";
                if (++lightRolls >= MAX_LIGHT_ROLLS) finish("FIRST_INSERTION_EXHAUSTED");
                else nextCheck = time + 4;
                return;
            }
            PerimeterApproachTask task = PerimeterApproachTask.attach(mob, approach);
            if (task == null) {
                finish("NATIVE_TASK_UNAVAILABLE");
                return;
            }
            attempts++;
            status = "PERIMETER_APPROACH";
            insert(mob, task);
        } finally {
            account(sampler, start);
        }
    }

    private boolean observeRoute() {
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            for (Attacker attacker : attackers) {
                if (attacker.task.arrived() || !attacker.mob.isEntityAlive()) continue;
                int index = attacker.task.steps() + 1;
                for (int i = Math.max(0, index - 1); i < Math.min(index + 2, approach.route.length); i++) {
                    PathPoint point = approach.route[i];
                    if (!sampler.standing(point.xCoord, point.yCoord, point.zCoord)) {
                        rejection = sampler.rejection;
                        finish("ROUTE_LOST");
                        return false;
                    }
                }
            }
            if (sampler.columns > 0) lastVerified = elapsed;
            return true;
        } finally {
            account(sampler, start);
        }
    }

    private void reinforce(EntityPlayer player) {
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            attempts++;
            EntitySkeleton mob = validateInsertion(player, sampler);
            if (mob == null) return;
            if (!mob.getCanSpawnHere()) {
                rejection = "VANILLA_SPAWN_RULE";
                return;
            }
            PerimeterApproachTask task = PerimeterApproachTask.attach(mob, approach);
            if (task != null) insert(mob, task);
            else rejection = "NATIVE_TASK_UNAVAILABLE";
        } finally {
            account(sampler, start);
        }
    }

    private void insert(EntitySkeleton mob, PerimeterApproachTask task) {
        if (!world.spawnEntityInWorld(mob)) {
            task.release();
            rejection = "INSERTION_REJECTED";
            return;
        }
        attackers.add(new Attacker(mob, task, elapsed));
        inserted++;
        peak = Math.max(peak, attackers.size());
    }

    private void refreshAttackers() {
        Iterator<Attacker> iterator = attackers.iterator();
        while (iterator.hasNext()) {
            Attacker attacker = iterator.next();
            EntitySkeleton mob = attacker.mob;
            int x = MathHelper.floor_double(mob.posX), z = MathHelper.floor_double(mob.posZ);
            if (!mob.isEntityAlive() || mob.worldObj != world || !world.blockExists(x, 64, z)
                    || !world.getEntitiesWithinAABB(EntitySkeleton.class, mob.boundingBox).contains(mob)) {
                attacker.task.release();
                iterator.remove();
                continue;
            }
            PathPoint first = approach.start(), goal = approach.goal();
            double dx = mob.posX - first.xCoord - 0.5, dz = mob.posZ - first.zCoord - 0.5;
            double length = Math.abs(goal.xCoord - first.xCoord) + Math.abs(goal.zCoord - first.zCoord);
            double advance = (dx * (goal.xCoord - first.xCoord) + dz * (goal.zCoord - first.zCoord)) / length;
            if (attacker.task.arrived()) advance = Math.min(advance, length);
            progress = Math.max(progress, advance);
            int walked = attacker.task.steps();
            steps = Math.max(steps, walked);
            if (walked > attacker.steps) {
                attacker.lastProgress = elapsed;
                attacker.steps = walked;
                if (attacker.task.arrived()) arrivals++;
            } else if (!attacker.task.arrived() && elapsed - attacker.lastProgress >= STALL) {
                finish("APPROACH_STALLED");
                return;
            }
        }
    }

    void interrupt(String cause) {
        if (active()) finish(cause);
        else if (planning()) fallback(cause);
    }

    private void finish(String cause) {
        for (Attacker attacker : attackers) attacker.task.release();
        attackers.clear();
        observer = null;
        phase = "TERMINAL";
        outcome = cause;
        status = "RESOLVED";
        storage.closeSite(cause);
    }

    private Admission fallback(String cause) {
        phase = "IDLE";
        status = cause;
        resetPlan();
        return Admission.FALLBACK;
    }

    private void resetPlan() {
        candidates.clear();
        frontages.clear();
    }

    private void resetCounters() {
        optionsTried = frames = attempts = inserted = peak = elapsed = arrivals = steps = peakColumns = peakSkyReads = 0;
        lastVerified = -1;
        lightRolls = 0;
        nextCheck = Long.MIN_VALUE;
        columns = heightReads = skyReads = protectionReads = planningNanos = peakPlanningNanos = 0;
        progress = 0;
        rejection = "NONE";
    }

    private void account(SiteApproach.Sampler sampler, long start) {
        columns += sampler.columns;
        heightReads += sampler.heightReads;
        skyReads += sampler.skyReads;
        peakColumns = Math.max(peakColumns, sampler.columns);
        peakSkyReads = Math.max(peakSkyReads, sampler.skyReads);
        accountTime(start);
    }

    private void accountTime(long start) {
        long nanos = System.nanoTime() - start;
        planningNanos += nanos;
        peakPlanningNanos = Math.max(peakPlanningNanos, nanos);
    }

    private boolean nearSite(EntityPlayer player) {
        return Candidate.distance(part, player) <= PRESENCE * PRESENCE;
    }

    private Bounds currentPart() {
        String unavailable = unavailable(observed);
        if (unavailable != null) {
            rejection = unavailable;
            return null;
        }
        if (observed.id != selected.id || observed.worldSession != selected.worldSession
                || observed.dimension != selected.dimension) {
            rejection = "SITE_IDENTITY_CHANGED";
            return null;
        }
        for (SiteSnapshot.Part current : observed.parts) {
            Bounds b = current.bounds;
            if (usable(current.coverage) && b.minX == part.minX && b.maxX == part.maxX && b.minY == part.minY
                    && b.maxY == part.maxY && b.minZ == part.minZ && b.maxZ == part.maxZ) return b;
        }
        rejection = "PART_CHANGED_OR_UNAVAILABLE";
        return null;
    }

    private static String unavailable(SiteSnapshot site) {
        if (site == null || site.id <= 0 || site.dimension != 0) return "SITE_UNAVAILABLE";
        if (site.dormant) return "SITE_DORMANT";
        return usable(site.coverage) ? null : "SITE_" + site.coverage;
    }

    private static boolean usable(Coverage coverage) {
        return coverage == Coverage.COMPLETE || coverage == Coverage.PARTIAL;
    }

    private EntitySkeleton reject(String cause) {
        rejection = cause;
        return null;
    }

    private static final class Candidate {
        final SiteSnapshot site;
        final Bounds part;

        Candidate(SiteSnapshot site, Bounds part) { this.site = site; this.part = part; }

        static double distance(Bounds part, EntityPlayer player) {
            double dx = Math.max(0, Math.max(part.minX - player.posX, player.posX - part.maxX - 1));
            double dy = Math.max(0, Math.max(part.minY - player.posY, player.posY - part.maxY - 1));
            double dz = Math.max(0, Math.max(part.minZ - player.posZ, player.posZ - part.maxZ - 1));
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private static final class Attacker {
        final EntitySkeleton mob;
        final PerimeterApproachTask task;
        int steps, lastProgress;

        Attacker(EntitySkeleton mob, PerimeterApproachTask task, int elapsed) {
            this.mob = mob;
            this.task = task;
            lastProgress = elapsed;
        }
    }

    public static final class Snapshot {
        public final SiegeDirector.EncounterKind kind;
        public final String eligibility, phase, rejection, outcome, coverage, approach, objective, identity;
        public final boolean opportunity, dormant;
        public final int site, dimension, active, inserted, peak, attempts, remaining, elapsed, steps, arrivals;
        public final int candidates, peakColumns, peakSkyReads, geometryAge;
        public final long boundary, nextOpportunityCycle, cooldownUntil, observation, siteSession;
        public final long columns, skyReads, heightReads, protectionReads;
        public final long planningNanos, peakPlanningNanos;
        public final double progress;

        Snapshot(SiteSiege siege, SiegeDirector.EncounterKind kind, int activeCap, LunarSnapshot lunar) {
            this.kind = kind;
            eligibility = siege.status;
            phase = siege.phase;
            rejection = siege.rejection;
            outcome = siege.outcome;
            SiteSnapshot siteView = siege.observed;
            site = siege.selected != null ? siege.selected.id : siege.storage != null ? siege.storage.lastSite : 0;
            coverage = siteView == null ? "UNAVAILABLE" : siteView.coverage.name();
            dimension = siteView == null ? Integer.MIN_VALUE : siteView.dimension;
            siteSession = siteView == null ? 0 : siteView.worldSession;
            dormant = siteView != null && siteView.dormant;
            identity = siteView == null ? "UNAVAILABLE" : "RECOGNIZED";
            observation = siteView == null ? Long.MIN_VALUE : siteView.lastConfirmed;
            approach = siege.approach == null ? "NONE" : point(siege.approach.start()) + " > " + point(siege.approach.goal());
            objective = siege.approach == null ? "NONE" : siege.approach.entrance ? "OPENING" : "FRONTAGE";
            opportunity = siege.opportunity;
            nextOpportunityCycle = siege.enabled && siege.storage != null && lunar != null
                    ? SiteSiegePolicy.nextOpportunity(siege.world.getSeed(), lunar) : Long.MIN_VALUE;
            boundary = siege.boundary != Long.MIN_VALUE ? siege.boundary
                    : siege.storage != null ? siege.storage.lastSiteBoundary : Long.MIN_VALUE;
            cooldownUntil = siege.storage == null ? Long.MIN_VALUE : siege.storage.cooldownUntil(site);
            active = siege.attackers.size();
            inserted = siege.inserted;
            peak = siege.peak;
            attempts = siege.attempts;
            remaining = siege.active() && attempts < MAX_INSERTION_ATTEMPTS
                    ? Math.max(0, Math.min(GROUP, activeCap) - inserted) : 0;
            elapsed = siege.elapsed;
            steps = siege.steps;
            arrivals = siege.arrivals;
            progress = siege.progress;
            candidates = siege.optionsTried;
            columns = siege.columns;
            geometryAge = siege.lastVerified < 0 ? -1 : siege.elapsed - siege.lastVerified;
            skyReads = siege.skyReads;
            heightReads = siege.heightReads;
            protectionReads = siege.protectionReads;
            peakColumns = siege.peakColumns;
            peakSkyReads = siege.peakSkyReads;
            planningNanos = siege.planningNanos;
            peakPlanningNanos = siege.peakPlanningNanos;
        }

        private static String point(PathPoint p) { return p.xCoord + "," + p.yCoord + "," + p.zCoord; }
    }
}
