package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.PathPoint;
import net.minecraft.src.World;
import zed.tekkitoverhaul.assault.SiteSiegeForcePlan.Role;
import zed.tekkitoverhaul.assault.SiteSiegeForcePlan.Wave;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.settlement.ProtectedInterior;
import zed.tekkitoverhaul.settlement.SettlementRecognition;
import zed.tekkitoverhaul.settlement.SiteQueries;
import zed.tekkitoverhaul.settlement.SiteSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.EntityThief;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.territory.ThreatAssessment;

/** Owns one bounded site objective. The director owns the lunar boundary and the major encounter decision. */
public final class SiteSiege {
    public enum Origin { NATURAL, MANUAL_DEBUG }
    public enum Profile { AUTO, PERIMETER, BREACH }
    public enum Intensity { NATURAL, LOW, MODERATE, HIGH, MAXIMUM }

    private enum TacticalPhase { IDLE, PLANNING, APPROACHING, PRESSURING, BREACHING, ENGAGING, BATTLEFIELD_ACTIVE, RESOLVED, ABORTED }

    enum Admission { WAIT, FALLBACK, COMMITTED }
    private static final int OPTIONS = 12, MAX_PARTS = 2, PLANNING_TICKS = 88;
    private static final int STALL = 140, PRESENCE = 48, LIGHT_WAIT = 900;
    private static final int WARNING = 8;
    private static final int[] OFFSETS = { 0, -2, 2, -4, 4 };
    private SiteSiegeForcePlan force;
    private Intensity requestedIntensity;
    private int waveIndex, combatStart, admittedTicks, defeated, withdrawn, abandoned, routeCursor, maxLifetime;
    private long peakTickNanos, retiredPathNanos, peakPathNanos;
    private final Map<Role, Integer> insertedRoles = new EnumMap<Role, Integer>(Role.class);
    private final Map<String, Integer> failures = new LinkedHashMap<String, Integer>();
    private final boolean enabled, destructive;
    private final SiteQueries sites;
    private final List<Attacker> attackers = new ArrayList<Attacker>();
    private final List<Candidate> candidates = new ArrayList<Candidate>();
    private World world;
    private AssaultWorldState storage;
    private SiteSnapshot selected;
    private SiteSnapshot observed;
    private SiteApproach approach, openedApproach;
    private final List<SiteApproach> frontages = new ArrayList<SiteApproach>();
    private final List<SiteApproach> connectedAlternates = new ArrayList<SiteApproach>();
    private final List<SiteApproach> sectors = new ArrayList<SiteApproach>();
    private final Set<String> deployedSectors = new HashSet<String>();
    private final Set<PathPoint> insertionPositions = new HashSet<PathPoint>();
    private final Set<String> failedLanes = new HashSet<String>();
    private TerrainApproachSearch terrainSearch, heavySearch;
    private final List<SiteApproach> heavyRoutes = new ArrayList<SiteApproach>();
    private int terrainPart, validRoutes, failedRoutes, planningColumns;
    private Bounds part;
    private EntityPlayer observer;
    private long boundary = Long.MIN_VALUE, lunarSession;
    private int optionsTried, frames, attempts, inserted, peak, elapsed, arrivals, steps, peakColumns;
    private int lastVerified;
    private long columns, heightReads, protectionReads, planningNanos, peakPlanningNanos;
    private double progress;
    private String phase = "IDLE", status, rejection = "NONE", outcome = "NONE";
    private boolean opportunity;
    private Origin origin = Origin.NATURAL;
    private Profile profile = Profile.AUTO;
    private TacticalPhase tacticalPhase = TacticalPhase.IDLE;
    private Attacker specialist;
    private int impactTick, windupHealth, firstContact, breachChecks, structuralChanges, entranceChanges;
    private String objectiveOutcome = "NONE", tacticalRejection = "NO_ELIGIBLE_BREACH";
    private int nativeDespawns;
    private String insertionFailure, industrial = "NONE";
    private boolean triedSpecialist;
    private PathPoint destination;
    private PathPoint[] passageAfterOpen;

    String admissionStatus() { return status; }
    String resolution() { return outcome; }
    boolean hasInserted() { return inserted > 0; }

    boolean manual() { return origin == Origin.MANUAL_DEBUG; }

    SiteSiege(boolean enabled, boolean destructive, SiteQueries sites) {
        this.enabled = enabled;
        this.destructive = destructive;
        this.sites = sites;
        status = enabled ? "WAITING_FOR_NATURAL_FULL_MOON" : "DISABLED";
    }

    void bind(World world, AssaultWorldState storage) {
        this.world = world;
        this.storage = storage;
        resetPlan();
        origin = Origin.NATURAL;
        profile = Profile.AUTO;
        selected = observed = null;
        part = null;
        approach = null;
        boundary = Long.MIN_VALUE;
        opportunity = false;
        resetCounters();
        phase = "IDLE";
        tacticalPhase = TacticalPhase.IDLE;
        status = !enabled ? "DISABLED" : storage == null ? "WORLD_UNAVAILABLE" : "WAITING_FOR_NATURAL_FULL_MOON";
        outcome = storage == null ? "NONE" : storage.siteOutcome;
    }

    boolean active() { return "ACTIVE".equals(phase); }
    boolean planning() { return "PLANNING".equals(phase); }

    Admission admit(EntityPlayer player, LunarSnapshot event, TerritorialActivityService territory, int cap) {
        if (!enabled) return Admission.FALLBACK;
        if (!planning()) {
            beginPlanning(player, Origin.NATURAL, Profile.AUTO, Intensity.NATURAL, event.rawTime, event.sessionId);
            opportunity = SiteSiegePolicy.opportunity(world.getSeed(), event.cycleIndex);
            if (!opportunity) return fallback("RARITY");
            discoverSites(player);
            if (candidates.isEmpty()) return fallback("NO_ELIGIBLE_SITE");
        }
        if (world.getWorldTime() - boundary >= AssaultWave.FIRST_START - 1) return fallback("PLANNING_DEADLINE");
        return plan(player, territory, cap);
    }

    Admission admitManual(EntityPlayer player, Profile requested, Intensity intensity,
                          TerritorialActivityService territory, int cap) {
        if (!planning()) {
            beginPlanning(player, Origin.MANUAL_DEBUG, requested, intensity, world.getWorldTime(), 0);
            discoverSites(player);
            if (candidates.isEmpty()) return fallback("NO_RECOGNIZED_SITE");
            if (selected.perimeterUnresolved && profile == Profile.BREACH) return fallback("SITE_FOOTPRINT_UNRESOLVED");
        }
        if (observer != player || !SiegeDirector.available(world, player)) return fallback("PLAYER_UNAVAILABLE");
        return plan(player, territory, cap);
    }

    private void beginPlanning(EntityPlayer player, Origin source, Profile requested, Intensity intensity,
                               long time, long session) {
        resetPlan();
        resetCounters();
        selected = observed = null;
        approach = null;
        part = null;
        origin = source;
        profile = requested;
        requestedIntensity = intensity;
        boundary = time;
        lunarSession = session;
        observer = player;
        opportunity = false;
        outcome = "NONE";
        phase = "PLANNING";
        tacticalPhase = TacticalPhase.PLANNING;
        status = "VERIFYING_EXTERIOR";
    }

    private void discoverSites(EntityPlayer player) {
        long start = System.nanoTime();
        discover(player);
        accountTime(start);
    }

    private Admission plan(EntityPlayer player, TerritorialActivityService territory, int cap) {
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            frames++;
            if (frames > PLANNING_TICKS) {
                count(failures, "PLAN/TERRAIN_PLANNING_LIMIT");
                return fallback(insertionFailure == null ? "TERRAIN_PLANNING_LIMIT" : insertionFailure);
            }
            if (approach == null) nextApproach(sampler);
            if (approach == null) return plansExhausted() ? noApproach() : Admission.WAIT;
            // A second sample is always required, with a fresh budget, before the first insertion.
            if (sampler.columns != 0) return Admission.WAIT;
            Role scout = approach.barrier != null && approach.barrier.structural() ? Role.TANK : Role.ZOMBIE;
            EntityMob mob = validateInsertion(player, sampler, approach, scout, true);
            if (mob == null) {
                if ("PROTECTION_DEFERRED".equals(rejection)) return Admission.WAIT;
                if (insertionFailure == null) insertionFailure = rejection;
                count(failures, "ADMISSION/" + rejection);
                approach = null;
                return plansExhausted() ? noApproach() : Admission.WAIT;
            }
            if (!PerimeterApproachTask.available()) return fallback("NATIVE_TASK_UNAVAILABLE");
            if (!manual()) {
                String blocked = storage.siegeEligibility(selected.id, boundary);
                if (blocked != null) return fallback(blocked);
            }
            int x = part.minX + (part.maxX - part.minX) / 2;
            int z = part.minZ + (part.maxZ - part.minZ) / 2;
            ThreatAssessment threat = territory == null ? null : territory.assessSite(world, x, part.minY, z);
            force = new SiteSiegeForcePlan(selected.id, world.difficultySetting, threat, requestedIntensity, cap,
                    approach, world.getSeed() ^ boundary);
            if (approach.connected && force.reserves(Role.TANK))
                heavySearch = new TerrainApproachSearch(part, observed.envelope, approach);
            if (!manual()) storage.startSite(force.siteId, boundary);
            observer = player;
            phase = "ACTIVE";
            tacticalPhase = TacticalPhase.APPROACHING;
            destination = approach.goal();
            status = manual() ? "WARNING" : "WAITING_FOR_DARKNESS";
            world.playSoundEffect(approach.goal().xCoord + 0.5, approach.goal().yCoord,
                    approach.goal().zCoord + 0.5, "ambient.cave.cave", 0.55F, 0.85F);
            if (approach.connected) sectors.clear();
            for (Iterator<SiteApproach> it = sectors.iterator(); it.hasNext();) {
                if (it.next().sector().equals(approach.sector())) it.remove();
            }
            sectors.add(0, approach);
            for (SiteApproach other : frontages) {
                boolean represented = false;
                for (SiteApproach sector : sectors) represented |= sector.sector().equals(other.sector());
                if (!represented) sectors.add(other);
            }
            resetPlan();
            return Admission.COMMITTED;
        } finally {
            planningColumns += sampler.columns;
            account(sampler, start);
        }
    }

    private void discover(final EntityPlayer player) {
        int x = MathHelper.floor_double(player.posX), y = MathHelper.floor_double(player.boundingBox.minY);
        int z = MathHelper.floor_double(player.posZ);
        SiteQueries.Point location = manual() ? sites.locate(world, x, y, z) : null;
        final int associated = location != null && location.state == SiteQueries.Recognition.RECOGNIZED
                ? location.site.id : 0;
        for (SiteSnapshot site : sites.nearby(world, x, y, z, SettlementRecognition.MAX_QUERY_RADIUS)) {
            String reason = unavailable(site);
            if (reason == null && !manual()) reason = storage.siegeEligibility(site.id, boundary);
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
                if (associated != 0 && (a.site.id == associated) != (b.site.id == associated))
                    return a.site.id == associated ? -1 : 1;
                int order = Double.compare(Candidate.distance(a.part, player), Candidate.distance(b.part, player));
                return order != 0 ? order : Integer.compare(a.site.id, b.site.id);
            }
        });
        if (manual() && !candidates.isEmpty()) {
            selected = observed = candidates.get(0).site;
            Iterator<Candidate> parts = candidates.iterator();
            while (parts.hasNext()) {
                if (parts.next().site.id != selected.id) parts.remove();
            }
        }
        if (candidates.size() > MAX_PARTS) candidates.subList(MAX_PARTS, candidates.size()).clear();
    }

    private Admission noApproach() {
        if (profile == Profile.BREACH && !triedSpecialist) return fallback(tacticalRejection);
        if (manual() && insertionFailure != null) return fallback(insertionFailure);
        return fallback(manual() && !"NONE".equals(rejection) ? rejection : "NO_VALID_APPROACH");
    }

    private boolean plansExhausted() {
        return optionsTried >= candidates.size() * OPTIONS && frontages.isEmpty()
                && connectedAlternates.isEmpty() && terrainPart >= candidates.size();
    }

    private void nextApproach(SiteApproach.Sampler sampler) {
        if (optionsTried % OPTIONS == 0 && !frontages.isEmpty()) {
            approach = frontages.remove(0);
            triedSpecialist |= approach.barrier != null;
            return;
        }
        if (optionsTried >= candidates.size() * OPTIONS) {
            nextConnectedApproach(sampler);
            return;
        }
        if (optionsTried % OPTIONS == 0) sectors.clear();
        Candidate candidate = candidates.get(optionsTried / OPTIONS);
        int option = optionsTried++ % OPTIONS;
        selected = candidate.site;
        part = candidate.part;
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null) return;
        Profile allowed = observed.perimeterUnresolved ? Profile.PERIMETER : profile;
        SiteApproach plan = sampler.plan(part, observed.envelope, option, allowed, destructive);
        if (sampler.breachRejection != null) tacticalRejection = sampler.breachRejection;
        if (plan == null || (observed.perimeterUnresolved && !sampler.localExterior(plan))) {
            rejection = sampler.rejection;
            failedRoutes++;
            if (rejection != null) count(failures, "PLAN/" + rejection);
            if (plan != null && plan.barrier != null) tacticalRejection = rejection;
            return;
        }
        validRoutes++;
        if (profile == Profile.BREACH && plan.barrier == null) {
            boolean represented = false;
            for (SiteApproach sector : sectors) represented |= sector.sector().equals(plan.sector());
            if (!represented) sectors.add(plan);
            return;
        }
        int index = 0;
        while (index < frontages.size() && frontages.get(index).priority() >= plan.priority()) index++;
        frontages.add(index, plan);
    }

    private void nextConnectedApproach(SiteApproach.Sampler sampler) {
        if (!connectedAlternates.isEmpty()) {
            approach = connectedAlternates.remove(0);
            triedSpecialist |= approach.barrier != null;
            return;
        }
        if (terrainPart >= candidates.size()) return;
        Candidate candidate = candidates.get(terrainPart);
        selected = candidate.site;
        part = candidate.part;
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null || observed.perimeterUnresolved) {
            terrainPart++;
            terrainSearch = null;
            return;
        }
        if (terrainSearch == null)
            terrainSearch = new TerrainApproachSearch(part, observed.envelope, profile);
        terrainSearch.advance(sampler);
        if (!terrainSearch.finished && (terrainSearch.routes.isEmpty() || frames < PLANNING_TICKS - 5)) return;
        terrainPart++;
        if (terrainSearch.routes.isEmpty()) {
            rejection = terrainSearch.rejection;
            failedRoutes++;
            count(failures, "PLAN/NO_CONNECTED_STAGING");
            if (!"NO_CONNECTED_STAGING".equals(rejection)) count(failures, "PLAN/" + rejection);
        } else {
            validRoutes += terrainSearch.routes.size();
            connectedAlternates.addAll(terrainSearch.routes);
        }
        terrainSearch = null;
    }

    private EntityMob validateInsertion(EntityPlayer player, SiteApproach.Sampler sampler,
                                        SiteApproach lane, Role role, boolean admission) {
        PathPoint start = lane.start();
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null) return null;
        if (!nearSite(player)) return reject("PLAYER_ABSENT");
        if (!sites.insertionExterior(world, start.xCoord, start.zCoord, SiteApproach.MARGIN))
            return reject("INSIDE_SITE_EXCLUSION");
        if (!sampler.validate(lane, role == Role.TANK)) return reject(sampler.rejection);
        if (role == Role.THIEF) {
            for (PathPoint point : lane.route) if (!sampler.thiefClear(point)) return reject(sampler.rejection);
        }
        if (observed.perimeterUnresolved && (lane.barrier != null || !sampler.localExterior(lane)))
            return reject(lane.barrier == null ? sampler.rejection : "SITE_FOOTPRINT_UNRESOLVED");
        if (lane.barrier != null && (admission || isObjectiveRole(role))) {
            sampler.breachChecks++;
            if (!lane.barrier.unchanged(world)) return reject("TARGET_CHANGED");
        }
        lastVerified = elapsed;
        ProtectedInterior.Assessment protection = sites.protection(world, start.xCoord, start.yCoord, start.zCoord);
        protectionReads += protection.reads;
        if (protection.state == ProtectedInterior.State.PROTECTED) return reject("PROTECTED_INSERTION");
        if (protection.state == ProtectedInterior.State.UNKNOWN) {
            if (protection.reads == 0) return reject("PROTECTION_DEFERRED");
            if (!"Covered route leaves local search window".equals(protection.reason)
                    && !"Ambiguous collision geometry".equals(protection.reason))
                return reject("UNCERTAIN_PROTECTION");
        }
        if (!sampler.surfaceAccess(lane, observed.envelope)) return reject(sampler.rejection);
        double x = start.xCoord + 0.5, y = start.yCoord, z = start.zCoord + 0.5;
        if (world.getClosestPlayer(x, y, z, SpawnController.INNER_RADIUS) != null)
            return reject("PLAYER_TOO_CLOSE");
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(x - 32, y - 24, z - 32, x + 32, y + 24, z + 32);
        int padding = (int) Math.ceil(World.MAX_ENTITY_RADIUS);
        if (!world.checkChunksExist(MathHelper.floor_double(area.minX) - padding, 0,
                MathHelper.floor_double(area.minZ) - padding, MathHelper.floor_double(area.maxX) + padding, 0,
                MathHelper.floor_double(area.maxZ) + padding)) return reject("POPULATION_AREA_UNLOADED");
        if (SpawnController.localHostiles(world, area) >= SpawnController.LOCAL_CAP)
            return reject("POPULATION_CAP");
        EntityMob mob = createAttacker(role);
        if ((role == Role.SABOTEUR || role == Role.THIEF || role == Role.TANK || role == Role.BREACHER)
                && !world.getEntitiesWithinAABB(mob.getClass(), area).isEmpty()) return reject("SPECIALIST_POPULATION");

        mob.setLocationAndAngles(x, y, z, 0, 0);
        String collision = SpawnController.placementCollision(world, mob);
        if (collision != null) return reject(collision);
        if (role == Role.SABOTEUR) {
            EntitySaboteur scout = (EntitySaboteur) mob;
            PathPoint entry = lane.goal();
            scout.setPosition(entry.xCoord + 0.5, entry.yCoord, entry.zCoord + 0.5);
            boolean supported = scout.assignSiteMachine(part);
            scout.setLocationAndAngles(x, y, z, 0, 0);
            industrial = scout.targetType() + " / " + scout.lastOutcome();
            if (!supported) return reject("NO_ACCESSIBLE_MACHINE");
        }
        return mob;
    }

    private EntityMob createAttacker(Role role) {
        switch (role) {
            case SKELETON: return new DebugSiegeSkeleton(world, manual(), selected.id);
            case ZOMBIE: return new SiegeZombie(world, manual(), selected.id);
            case BREACHER: return new EntityBreacher(world);
            case TANK: return new EntityTank(world);
            case SKIRMISHER: return new EntitySkirmisher(world);
            case SABOTEUR: return new EntitySaboteur(world);
            case THIEF: return new EntityThief(world);
            default: throw new IllegalArgumentException("Unassigned site siege role " + role);
        }
    }

    void tick(EntityPlayer player, LunarSnapshot now) {
        long start = System.nanoTime();
        try {
            tickForce(player, now);
        } finally {
            peakTickNanos = Math.max(peakTickNanos, System.nanoTime() - start);
        }
    }

    private void tickForce(EntityPlayer player, LunarSnapshot now) {
        admittedTicks++;
        if (!manual() && (now == null || now.sessionId != lunarSession || !AssaultSchedule.WINDOW.matches(now))) {
            finish("LUNAR_CONTEXT_LOST");
            return;
        }
        if (world.difficultySetting != force.difficulty) {
            finish(world.difficultySetting <= 0 ? "PEACEFUL" : "DIFFICULTY_CHANGED");
            return;
        }
        if (!SiegeDirector.available(world, player) || player != observer || !nearSite(player)) {
            finish("PLAYER_ABSENT");
            return;
        }
        if (approach.barrier != null && objectivePending() && !approach.barrier.loaded(world)) {
            finish("TARGET_UNLOADED");
            return;
        }
        if (admittedTicks % 5 == 0 && !refreshSite()) return;
        if (combatStart < 0) {
            if (admittedTicks < WARNING || (!manual() && world.skylightSubtracted < 11 && admittedTicks < LIGHT_WAIT))
                return;
            combatStart = admittedTicks;
            status = approach.connected ? "CONNECTED_TERRAIN_ROUTE" : approach.objective.name();
        }
        elapsed = admittedTicks - combatStart;
        refreshAttackers();
        if (elapsed >= force.tacticsEnd) {
            finish(defeated == inserted ? (inserted == 0 ? "DEPLOYMENT_FAILED" : "ATTACKERS_GONE")
                    : "REINFORCEMENTS_EXHAUSTED");
            return;
        }
        if (specialist != null && objectivePending()) executeObjective();
        if (!active()) return;

        if (approach.barrier == null && arrivals > 0 && objectivePending()) {
            tacticalPhase = TacticalPhase.ENGAGING;
            objectiveOutcome = "DESTINATION_REACHED";
        }
        if (waveIndex + 1 < force.waves.length && elapsed >= force.waves[waveIndex + 1].start) {
            waveIndex++;
            failedLanes.clear();
        }
        Wave wave = force.waves[waveIndex];
        if (elapsed < wave.end && wave.remaining() > 0
                && (elapsed - wave.start) % SiteSiegeForcePlan.RETRY_INTERVAL == 0) {
            if (attackers.size() >= Math.max(1, force.activeCap - (force.waves.length - waveIndex - 1))) {
                wave.capDeferrals++;
                count(wave.rejections, "ACTIVE_CAP");
                count(failures, "ACTIVE_CAP");
            } else reinforce(player, wave);
        } else if (heavySearch != null && elapsed % 3 == 1) {
            sampleHeavyRoutes();
        } else if (!attackers.isEmpty()) {
            observeRoute();
        }
        if (!active()) return;
        boolean finalWave = waveIndex == force.waves.length - 1;
        if (attackers.isEmpty() && finalWave && (elapsed >= wave.end || wave.remaining() == 0))
            finish(inserted == 0 ? "DEPLOYMENT_FAILED" : defeated == inserted ? "ATTACKERS_GONE" : "WAVES_COMPLETE");
    }

    private void sampleHeavyRoutes() {
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            heavySearch.advance(sampler);
            if (!heavySearch.finished && elapsed < 600) return;
            heavyRoutes.addAll(heavySearch.routes);
            validRoutes += heavyRoutes.size();
            if (heavyRoutes.isEmpty()) {
                failedRoutes++;
                count(failures, "PLAN/TANK/" + heavySearch.rejection);
            }
            heavySearch = null;
        } finally {
            planningColumns += sampler.columns;
            account(sampler, start);
        }
    }

    private boolean objectivePending() {
        return tacticalPhase == TacticalPhase.APPROACHING || tacticalPhase == TacticalPhase.BREACHING
                || tacticalPhase == TacticalPhase.PRESSURING;
    }

    private boolean isObjectiveRole(Role role) {
        return approach.barrier != null && role == (approach.barrier.structural() ? Role.TANK : Role.BREACHER);
    }

    private boolean refreshSite() {
        observed = sites.resolve(world, force.siteId);
        if (currentPart() != null) return true;
        finish(rejection);
        return false;
    }

    private void observeRoute() {
        Attacker attacker = attackers.get(routeCursor++ % attackers.size());
        if (attacker.task.arrived()) return;
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            int index = attacker.task.steps() + 1;
            PathPoint[] route = attacker.task.waypoints();
            for (int i = Math.max(0, index - 1); i < Math.min(index + 2, route.length); i++) {
                boolean wide = attacker.role == Role.TANK;
                if (sampler.standing(route[i], wide) && (i == 0 || sampler.transition(route[i - 1], route[i], wide))
                        && (attacker.role != Role.THIEF || sampler.thiefClear(route[i]))) continue;
                count(failures, sampler.rejection);
                rejectWave(attacker.role, sampler.rejection);
                if (i == index && recoverRoute(attacker, sampler)) return;
                withdraw(attacker, "ROUTE_LOST");
                attackers.remove(attacker);
                return;
            }
            lastVerified = elapsed;
        } finally {
            account(sampler, start);
        }
    }

    private boolean recoverRoute(Attacker attacker, SiteApproach.Sampler sampler) {
        if (attacker.recoveryTried || attacker.retreating
                || (attacker.role != Role.ZOMBIE && attacker.role != Role.SKELETON)) return false;
        attacker.recoveryTried = true;
        if (!attacker.task.tryExteriorBypass(sampler, observed.envelope)) return false;
        attacker.lastProgress = elapsed;
        attacker.stalled = false;
        count(failures, attacker.role + "/ROUTE_BYPASSED");
        return true;
    }

    private SiteApproach laneFor(Role role, int slot, int attempt) {
        if (role == Role.SABOTEUR) return openedApproach == null ? approach : openedApproach;
        SiteApproach primary = approach;
        if (approach.connected && !connectedAlternates.isEmpty()) {
            int choice = attempt % (connectedAlternates.size() + 1);
            if (choice > 0) primary = connectedAlternates.get(choice - 1);
        }
        if (isObjectiveRole(role)) return approach;
        if ((role == Role.SKELETON || role == Role.ZOMBIE) && attempt == 0 && slot < 2) {
            if (openedApproach != null) return openedApproach;
            if (approach.objective == SiteApproach.Objective.APPROACH_ENTRANCE) return approach;
        }
        if (!approach.connected && sectors.size() > 1) {
            SiteApproach sector = sectors.get((slot + attempt + waveIndex) % sectors.size());
            if (sector != approach) return sector.staging(0, role == Role.SKIRMISHER ? 5 : 2);
        }
        if (approach.connected) {
            if (role == Role.TANK) return heavyRoutes.isEmpty() ? primary.staging(0, 3)
                    : heavyRoutes.get(attempt % heavyRoutes.size());
            if (role == Role.SKIRMISHER) return primary.staging(0, 5);
            if (role == Role.THIEF) return primary.staging(0, 3);
            if (openedApproach != null) return primary.opened(passageAfterOpen);
            return approach.barrier == null ? primary : primary.staging(0, 2);
        }
        int offset = OFFSETS[(slot + attempt) % OFFSETS.length];
        if (role == Role.TANK) return approach.staging(offset, 5);
        if (role == Role.SKIRMISHER)
            return approach.staging(offset == 0 && attempt == 0 ? 3 : offset, 6);
        if (role == Role.THIEF) return approach.staging(offset == 0 && attempt == 0 ? -3 : offset, 3);
        if (approach.barrier != null && objectivePending())
            return approach.staging(offset == 0 && attempt == 0 ? 2 : offset, approach.barrier.structural() ? 5 : 2);
        SiteApproach entrance = openedApproach != null ? openedApproach
                : approach.objective == SiteApproach.Objective.APPROACH_ENTRANCE ? approach : null;
        if (entrance != null && attempt % 3 != 2) return entrance;
        if (approach.barrier == null && attempt % 2 == 0) return approach;
        int flank = OFFSETS[(slot + attempt / 2) % OFFSETS.length];
        // Keep the threshold lane free of infantry assigned to a shorter holding position.
        return approach.staging(entrance != null && flank == 0 ? 2 : flank, 2);
    }

    private void reinforce(EntityPlayer player, Wave wave) {
        int slot = wave.nextSlot(force.activeCap - attackers.size());
        if (slot < 0) return;
        Role role = wave.infantryFallback[slot] ? Role.ZOMBIE : wave.roles[slot];
        if (role == Role.SABOTEUR && (inserted == 0 || approach.barrier != null && openedApproach == null)) {
            rejectWave(role, "WAITING_FOR_ACCESS");
            if (objectivePending() && elapsed < (wave.start + wave.end) / 2) return;
            wave.infantryFallback[slot] = true;
            return;
        }
        boolean objectiveRole = isObjectiveRole(role);
        if (objectiveRole && !objectivePending()) {
            wave.placementFailures[slot] = SiteSiegeForcePlan.ATTEMPTS_PER_SLOT;
            rejectWave(role, "OBJECTIVE_CLOSED");
            return;
        }
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            SiteApproach lane = null;
            String key = null;
            for (int choice = 0; choice < 15; choice++) {
                lane = laneFor(role, slot, wave.attempts[slot]++);
                key = role + "/" + lane.signature();
                if (!failedLanes.contains(key)) break;
                lane = null;
            }
            if (lane == null) {
                wave.placementFailures[slot] = SiteSiegeForcePlan.ATTEMPTS_PER_SLOT;
                rejectWave(role, "ROUTE_CANDIDATES_EXHAUSTED");
                return;
            }
            attempts++;
            wave.tried++;
            EntityMob mob = validateInsertion(player, sampler, lane, role, false);
            if (mob == null) {
                if (role == Role.SABOTEUR && "NO_ACCESSIBLE_MACHINE".equals(rejection)) {
                    wave.infantryFallback[slot] = true;
                    rejectWave(role, rejection);
                    return;
                }
                if ("POPULATION_CAP".equals(rejection)) wave.capDeferrals++;
                else if (permanentPlacementFailure(rejection)) {
                    failedLanes.add(key);
                    wave.placementFailures[slot]++;
                }
                rejectWave(role, rejection);
                if (objectiveRole && "TARGET_CHANGED".equals(rejection)) {
                    PathPoint[] open = approach.barrier.structural() ? null : passage(sampler, false);
                    if (open == null) endObjective("TARGET_CHANGED");
                    else completePassage(open, "ENTRANCE_ALREADY_OPEN");
                }
                return;
            }
            PerimeterApproachTask task = PerimeterApproachTask.attach(mob, lane, objectiveRole);
            if (task == null) {
                rejectWave(role, "NATIVE_TASK_UNAVAILABLE");
                finish("NATIVE_TASK_UNAVAILABLE");
                return;
            }
            if (mob instanceof TacticalHostile) {
                ((TacticalHostile) mob).claimSiteSiege(player, lane.goal(), force.siteId);
            } else {
                PathPoint goal = lane.goal();
                mob.setHomeArea(goal.xCoord, goal.yCoord, goal.zCoord, 10);
            }
            if (!world.spawnEntityInWorld(mob)) {
                task.release();
                rejection = "INSERTION_REJECTED";
                wave.placementFailures[slot]++;
                rejectWave(role, rejection);
                return;
            }
            insertionPositions.add(lane.start());
            deployedSectors.add(lane.sector());
            wave.inserted[slot] = true;
            wave.deployed++;
            if (waveIndex > 0 && wave.deployed == 1)
                world.playSoundEffect(mob.posX, mob.posY, mob.posZ, "ambient.cave.cave", 0.45F, 0.9F);
            Attacker attacker = new Attacker(mob, task, role, elapsed);
            attackers.add(attacker);
            if (objectiveRole) specialist = attacker;
            inserted++;
            count(insertedRoles, role);
            peak = Math.max(peak, attackers.size());
        } finally {
            account(sampler, start);
        }
    }

    private void rejectWave(Role role, String cause) {
        count(failures, role + "/" + cause);
        count(force.waves[waveIndex].rejections, role + "/" + cause);
    }

    private static boolean permanentPlacementFailure(String reason) {
        return !"PROTECTION_DEFERRED".equals(reason) && !"PLAYER_TOO_CLOSE".equals(reason)
                && !"OCCUPIED".equals(reason) && !"SPECIALIST_POPULATION".equals(reason)
                && !"APPROACH_UNLOADED".equals(reason);
    }

    private void refreshAttackers() {
        Iterator<Attacker> iterator = attackers.iterator();
        while (iterator.hasNext()) {
            Attacker attacker = iterator.next();
            EntityMob mob = attacker.mob;
            int x = MathHelper.floor_double(mob.posX), z = MathHelper.floor_double(mob.posZ);
            if (mob instanceof EntitySaboteur && elapsed % 4 == 0) {
                EntitySaboteur scout = (EntitySaboteur) mob;
                industrial = scout.targetType() + " / " + scout.lastOutcome() + " / pose " + scout.pose();
            }
            boolean alive = mob.isEntityAlive();
            boolean loaded = alive && mob.worldObj == world && world.blockExists(x, 64, z)
                    && mob.addedToChunk;
            if (!loaded) {
                if (!alive && mob.getHealth() <= 0) defeated++;
                boolean despawn = mob instanceof TacticalHostile ? ((TacticalHostile) mob).nativeSiteDespawn()
                        : attacker.task.nativeDespawn;
                if (despawn) nativeDespawns++;
                withdraw(attacker, mob.getHealth() <= 0 ? "DEATH" : despawn ? "NATIVE_DESPAWN"
                        : mob.isDead ? "EXTERNAL_REMOVAL" : "ENTITY_UNLOADED");
                iterator.remove();
                continue;
            }
            progress = Math.max(progress, attacker.task.progress());
            int walked = attacker.task.steps();
            steps = Math.max(steps, walked);
            if (walked > attacker.steps) {
                attacker.stalled = false;
                attacker.lastProgress = elapsed;
                attacker.steps = walked;
            } else if (!attacker.task.arrived() && elapsed - attacker.lastProgress >= STALL) {
                if (!attacker.stalled) count(failures, attacker.role + "/MOVEMENT_STALL");
                attacker.stalled = true;
            }
            if (attacker.retreating && attacker.task.arrived()) {
                withdraw(attacker, "WITHDREW_TO_STAGING");
                iterator.remove();
                continue;
            }
            if (!attacker.retreating && attacker.counted && attacker != specialist
                    && elapsed >= force.tacticsEnd - 110 + attacker.mob.entityId % 32
                    && !(mob instanceof EntitySaboteur) && !(mob instanceof EntityThief)
                    && attacker.task.retreat()) {
                attacker.retreating = true;
                if (mob instanceof TacticalHostile) ((TacticalHostile) mob).holdSiteMovement();
            }
            if (attacker.task.arrived() && !attacker.counted) {
                attacker.counted = true;
                arrivals++;
                if (attacker.task.combatDeferred()) count(failures, attacker.role + "/COMBAT_CACHE_UNLOADED");
                if (attacker != specialist && mob instanceof TacticalHostile) ((TacticalHostile) mob).beginSiteCombat();
            }
        }
    }

    private void withdraw(Attacker attacker, String cause) {
        if (attacker.mob.getHealth() > 0) withdrawn++;
        if (attacker == specialist) {
            if (objectivePending()) endObjective(cause);
            specialist = null;
        }
        release(attacker, cause);
    }

    private static <K> void count(Map<K, Integer> counts, K key) {
        Integer previous = counts.get(key);
        counts.put(key, previous == null ? 1 : previous + 1);
    }

    private void executeObjective() {
        if (!specialist.task.arrived()) return;
        if (firstContact < 0) firstContact = elapsed;
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            SiegeBarrier barrier = approach.barrier;
            EntityMob mob = specialist.mob;
            if (tacticalPhase != TacticalPhase.APPROACHING && mob.getHealth() < windupHealth) {
                endObjective("WINDUP_INTERRUPTED");
                return;
            }
            sampler.breachChecks++;
            if (!barrier.unchanged(world)) {
                if (!barrier.structural() && passage(sampler, false) != null)
                    advanceObjective(sampler, "ENTRANCE_ALREADY_OPEN");
                else endObjective("TARGET_CHANGED");
                return;
            }
            if (!sampler.standing(approach.goal(), mob instanceof EntityTank)) {
                endObjective("TARGET_OUT_OF_REACH");
                return;
            }
            if (!barrier.readyForImpact(mob)) {
                // A heavy actor can regain its footing after incidental contact. Damage, block
                // changes and loss of the loaded target still interrupt immediately.
                int contactDeadline = tacticalPhase == TacticalPhase.APPROACHING ? firstContact + 24
                        : mob instanceof EntityTank ? impactTick + 8 : elapsed;
                if (elapsed >= contactDeadline) endObjective("TARGET_OUT_OF_REACH");
                return;
            }
            if (tacticalPhase == TacticalPhase.APPROACHING) {
                windupHealth = mob.getHealth();
                if (mob instanceof EntityTank) {
                    tacticalPhase = TacticalPhase.BREACHING;
                    impactTick = elapsed + EntityTank.WINDUP;
                    ((EntityTank) mob).signalSiteWindup(barrier.target.xCoord + 0.5, barrier.target.zCoord + 0.5);
                } else {
                    tacticalPhase = TacticalPhase.PRESSURING;
                    impactTick = elapsed + EntityBreacher.FORCE_TICKS;
                    mob.getLookHelper().setLookPosition(barrier.target.xCoord + 0.5,
                            barrier.target.yCoord + 0.8, barrier.target.zCoord + 0.5, 30, 30);
                    world.playSoundAtEntity(mob, "step.wood", 0.7F, 0.7F);
                }
                return;
            }
            if (elapsed < impactTick || !refreshSite()) return;
            if (!barrier.structural() && !sampler.standing(barrier.inside, false)) {
                endObjective("PASSAGE_BLOCKED");
                return;
            }
            sampler.breachChecks++;
            int changed = barrier.structural() && structuralChanges >= force.structuralBudget ? 0 : barrier.impact(mob);
            if (mob instanceof EntityTank) {
                structuralChanges += changed;
                ((EntityTank) mob).endSiteWindup(changed > 0);
            } else {
                entranceChanges += changed;
                if (changed > 0) ((EntityBreacher) mob).signalSiteImpact();
            }
            if (changed == 0) endObjective("IMPACT_REJECTED");
            else advanceObjective(sampler, barrier.structural() ? "STRUCTURE_BREACHED" : "ENTRANCE_OPENED");
        } finally {
            account(sampler, start);
        }
    }

    private PathPoint[] passage(SiteApproach.Sampler sampler, boolean wide) {
        SiegeBarrier barrier = approach.barrier;
        PathPoint target = barrier.target, inside = barrier.inside;
        if (barrier.structural()) {
            PathPoint outside = new PathPoint(2 * target.xCoord - inside.xCoord, target.yCoord,
                    2 * target.zCoord - inside.zCoord);
            if (!sampler.standing(outside, wide) || !sampler.standing(target, wide) || !sampler.standing(inside, wide))
                return null;
            return new PathPoint[] { outside, target, inside };
        }
        return sampler.standing(target, wide) && sampler.standing(inside, wide)
                ? new PathPoint[] { target, inside } : null;
    }

    private void advanceObjective(SiteApproach.Sampler sampler, String reason) {
        PathPoint[] narrow = passage(sampler, false);
        if (narrow == null) {
            endObjective("PASSAGE_BLOCKED");
            return;
        }
        completePassage(narrow, reason);
        PathPoint[] route = specialist.role == Role.TANK ? passage(sampler, true) : narrow;
        if (route == null) {
            specialist.task.release();
            ((TacticalHostile) specialist.mob).beginSiteCombat();
        } else {
            specialist.task.advance(route);
            specialist.counted = false;
        }
        specialist = null;
    }

    private void completePassage(PathPoint[] passage, String reason) {
        passageAfterOpen = passage;
        openedApproach = approach.opened(passage);
        destination = approach.barrier.inside;
        tacticalPhase = TacticalPhase.ENGAGING;
        objectiveOutcome = reason;
    }

    private void endObjective(String reason) {
        objectiveOutcome = reason;
        tacticalPhase = TacticalPhase.ENGAGING;
        if (specialist != null) {
            TacticalHostile mob = (TacticalHostile) specialist.mob;
            if (mob instanceof EntityTank) ((EntityTank) mob).endSiteWindup(false);
            specialist.task.release();
            mob.beginSiteCombat();
        }
    }

    void interrupt(String cause) {
        if (active()) finish(cause);
        else if (planning()) fallback(cause);
    }

    private void finish(String cause) {
        terrainSearch = heavySearch = null;
        if (force != null) abandoned = force.total - inserted;
        for (Attacker attacker : attackers) {
            if (attacker.mob.getHealth() <= 0) defeated++;
            else withdrawn++;
            release(attacker, cause);
        }
        attackers.clear();
        specialist = null;
        if ("ATTACKERS_GONE".equals(cause)) tacticalPhase = TacticalPhase.RESOLVED;
        else if ("REINFORCEMENTS_EXHAUSTED".equals(cause) || "WAVES_COMPLETE".equals(cause))
            tacticalPhase = TacticalPhase.BATTLEFIELD_ACTIVE;
        else tacticalPhase = TacticalPhase.ABORTED;
        if ("NONE".equals(objectiveOutcome)) objectiveOutcome = cause;
        observer = null;
        phase = "TERMINAL";
        outcome = cause;
        status = inserted > defeated ? "BATTLEFIELD_SURVIVORS_POSSIBLE" : "RESOLVED";
        if (!manual()) storage.closeSite(cause);
    }

    private void release(Attacker attacker, String cause) {
        count(failures, "TACTICAL_RELEASE/" + cause + (attacker.mob.isEntityAlive() ? "/ALIVE" : "/DEAD"));
        attacker.task.release();
        maxLifetime = Math.max(maxLifetime, elapsed - attacker.insertedAt);
        if (attacker.mob instanceof TacticalHostile) {
            TacticalHostile mob = (TacticalHostile) attacker.mob;
            retiredPathNanos += mob.sitePathNanos();
            peakPathNanos = Math.max(peakPathNanos, mob.peakSitePathNanos());
        }
        if (attacker.mob instanceof TacticalHostile) ((TacticalHostile) attacker.mob).releaseSiteSiege();
        if (attacker.mob instanceof EntitySaboteur) {
            EntitySaboteur scout = (EntitySaboteur) attacker.mob;
            industrial = scout.targetType() + " / " + scout.lastOutcome() + " / " + cause;
        }
    }

    private Admission fallback(String cause) {
        phase = "IDLE";
        tacticalPhase = TacticalPhase.ABORTED;
        objectiveOutcome = cause;
        observer = null;
        status = cause;
        if (manual()) rejection = cause;
        resetPlan();
        return Admission.FALLBACK;
    }

    private void resetPlan() {
        candidates.clear();
        frontages.clear();
    }

    private void resetCounters() {
        optionsTried = frames = attempts = inserted = peak = elapsed = arrivals = steps = peakColumns = 0;
        lastVerified = -1;
        force = null;
        openedApproach = null;
        passageAfterOpen = null;
        sectors.clear();
        deployedSectors.clear();
        insertionPositions.clear();
        connectedAlternates.clear();
        failedLanes.clear();
        terrainSearch = heavySearch = null;
        heavyRoutes.clear();
        terrainPart = validRoutes = failedRoutes = planningColumns = 0;
        combatStart = waveIndex = -1;
        admittedTicks = defeated = withdrawn = abandoned = routeCursor = maxLifetime = 0;
        peakTickNanos = retiredPathNanos = peakPathNanos = 0;
        insertedRoles.clear();
        failures.clear();
        columns = heightReads = protectionReads = planningNanos = peakPlanningNanos = 0;
        progress = 0;
        destination = null;
        specialist = null;
        breachChecks = structuralChanges = entranceChanges = 0;
        firstContact = -1;
        triedSpecialist = false;
        objectiveOutcome = "NONE";
        tacticalRejection = "NO_ELIGIBLE_BREACH";
        insertionFailure = null;
        industrial = "NONE";
        nativeDespawns = 0;
        rejection = "NONE";
    }

    private void account(SiteApproach.Sampler sampler, long start) {
        columns += sampler.columns;
        heightReads += sampler.heightReads;
        breachChecks += sampler.breachChecks;
        peakColumns = Math.max(peakColumns, sampler.columns);
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

    private String unavailable(SiteSnapshot site) {
        if (site == null || site.id <= 0 || site.dimension != 0) return "SITE_UNAVAILABLE";
        if (site.dormant) return "SITE_DORMANT";
        if (site.perimeterUnresolved && !manual()) return "SITE_FOOTPRINT_UNRESOLVED";
        return usable(site.coverage) ? null : "SITE_" + site.coverage;
    }

    private static boolean usable(Coverage coverage) {
        return coverage == Coverage.COMPLETE || coverage == Coverage.PARTIAL;
    }

    private EntityMob reject(String cause) {
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
        final EntityMob mob;
        final PerimeterApproachTask task;
        final Role role;
        final int insertedAt;
        int steps, lastProgress;
        boolean counted, stalled, retreating, recoveryTried;

        Attacker(EntityMob mob, PerimeterApproachTask task, Role role, int elapsed) {
            this.mob = mob;
            this.task = task;
            this.role = role;
            insertedAt = lastProgress = elapsed;
        }
    }

    public static final class Snapshot {
        public final SiegeDirector.EncounterKind kind;
        public final Origin origin;
        public final Profile profile;
        public final SiegeDirector.DebugState request;
        public final String requestReason, callBlock;
        public final boolean callAvailable, cancelAvailable;
        public final String eligibility, phase, rejection, outcome, coverage, approach, objective, identity;
        public final int nativeDespawns, sectorCount;
        public final String sectors, industry;
        public final String tacticalPhase, objectiveOutcome, specialist, objectiveTarget;
        public final boolean opportunity, dormant, destructive;
        public final int breachChecks, structuralChanges, entranceChanges, structuralBudget;
        public final Intensity intensity;
        public final boolean overridden;
        public final int threat, difficulty, planned, activeCap, waves, wave, defeated, withdrawn, deferred, abandoned;
        public final int duration, tacticsEnd, peakLifetime;
        public final long pathNanos, peakPathNanos, peakTickNanos;
        public final List<String> allocations, deployment;
        public final Map<String, Integer> plannedRoles, insertedRoles, activeRoles, failures, waveFailures;
        public final int site, dimension, active, inserted, peak, attempts, remaining, elapsed, steps, arrivals;
        public final int candidates, validRoutes, failedRoutes, planningColumns, peakColumns, peakSkyReads, geometryAge;
        public final long boundary, nextOpportunityCycle, cooldownUntil, observation, siteSession;
        public final long columns, skyReads, heightReads, protectionReads;
        public final long planningNanos, peakPlanningNanos;
        public final double progress;

        Snapshot(SiteSiege siege, SiegeDirector.EncounterKind kind, LunarSnapshot lunar,
                 SiegeDirector.DebugState request, String requestReason, String callBlock, boolean cancelAvailable) {
            this.kind = kind;
            this.request = request;
            this.requestReason = requestReason;
            this.callBlock = callBlock;
            callAvailable = callBlock == null;
            this.cancelAvailable = cancelAvailable;
            origin = siege.origin;
            profile = siege.profile;
            eligibility = siege.status;
            phase = siege.phase;
            rejection = siege.rejection;
            outcome = siege.outcome;
            SiteSnapshot siteView = siege.observed;
            site = siege.selected != null ? siege.selected.id : !siege.manual() && siege.storage != null ? siege.storage.lastSite : 0;
            coverage = siteView == null ? "UNAVAILABLE" : siteView.coverage.name();
            dimension = siteView == null ? Integer.MIN_VALUE : siteView.dimension;
            siteSession = siteView == null ? 0 : siteView.worldSession;
            dormant = siteView != null && siteView.dormant;
            identity = siteView == null ? "UNAVAILABLE" : "RECOGNIZED";
            observation = siteView == null ? Long.MIN_VALUE : siteView.lastConfirmed;
            SiteApproach lane = siege.force == null ? siege.approach : siege.force.objective;
            approach = lane == null ? "NONE" : point(lane.start()) + " > "
                    + point(siege.destination == null ? lane.goal() : siege.destination);
            objective = lane == null ? "NONE" : lane.objective.name();
            objectiveTarget = lane == null || lane.barrier == null ? "NONE" : point(lane.barrier.target);
            nativeDespawns = siege.nativeDespawns;
            sectorCount = siege.deployedSectors.size();
            sectors = siege.sectors.size() + " available | " + siege.deployedSectors + " | "
                    + siege.insertionPositions.size() + " starts";
            industry = siege.industrial;
            tacticalPhase = siege.tacticalPhase.name();
            objectiveOutcome = siege.objectiveOutcome;
            specialist = siege.specialist == null ? "NONE"
                    : siege.specialist.mob instanceof EntityTank ? "TANK" : "BREACHER";
            destructive = siege.destructive;
            breachChecks = siege.breachChecks;
            structuralChanges = siege.structuralChanges;
            entranceChanges = siege.entranceChanges;
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
            SiteSiegeForcePlan plan = siege.force;
            threat = plan == null || plan.threat == null ? -1 : plan.threat.threat;
            difficulty = plan == null ? 0 : plan.difficulty;
            intensity = plan == null ? Intensity.NATURAL : plan.intensity;
            overridden = plan != null && plan.overridden;
            planned = plan == null ? 0 : plan.total;
            activeCap = plan == null ? 0 : plan.activeCap;
            structuralBudget = plan == null ? 0 : plan.structuralBudget;
            waves = plan == null ? 0 : plan.waves.length;
            wave = siege.waveIndex + 1;
            int pending = 0, reserve = 0;
            Map<String, Integer> waveReasons = new LinkedHashMap<String, Integer>();
            List<String> rosters = new ArrayList<String>(), results = new ArrayList<String>();
            Map<String, Integer> plannedByRole = new LinkedHashMap<String, Integer>();
            for (int i = 0; i < waves; i++) {
                Wave allocation = plan.waves[i];
                rosters.add(allocation.toString());
                results.add(allocation.deployed + "/" + allocation.roles.length + " inserted | " + allocation.tried
                        + " tries | " + allocation.capDeferrals + " cap delays");
                for (Role role : allocation.roles) count(plannedByRole, role.name());
                for (Map.Entry<String, Integer> reason : allocation.rejections.entrySet())
                    waveReasons.put("W" + (i + 1) + " " + reason.getKey(), reason.getValue());
                if (!siege.active()) continue;
                if (i >= wave) reserve += allocation.remaining();
                else if (i == wave - 1 && siege.elapsed < allocation.end) pending = allocation.remaining();
            }
            remaining = reserve;
            deferred = pending;
            defeated = siege.defeated;
            withdrawn = siege.withdrawn;
            abandoned = siege.active() ? planned - inserted - reserve - pending : siege.abandoned;
            duration = siege.admittedTicks;
            tacticsEnd = plan == null ? 0 : plan.tacticsEnd;
            Map<String, Integer> insertedByRole = new LinkedHashMap<String, Integer>();
            for (Map.Entry<Role, Integer> role : siege.insertedRoles.entrySet())
                insertedByRole.put(role.getKey().name(), role.getValue());
            Map<String, Integer> activeByRole = new LinkedHashMap<String, Integer>();
            long pathTime = siege.retiredPathNanos, peakPath = siege.peakPathNanos;
            int longest = siege.maxLifetime;
            for (Attacker attacker : siege.attackers) {
                count(activeByRole, attacker.role.name());
                longest = Math.max(longest, siege.elapsed - attacker.insertedAt);
                if (attacker.mob instanceof TacticalHostile) {
                    TacticalHostile mob = (TacticalHostile) attacker.mob;
                    pathTime += mob.sitePathNanos();
                    peakPath = Math.max(peakPath, mob.peakSitePathNanos());
                }
            }
            peakLifetime = longest;
            pathNanos = pathTime;
            peakPathNanos = peakPath;
            peakTickNanos = siege.peakTickNanos;
            allocations = Collections.unmodifiableList(rosters);
            deployment = Collections.unmodifiableList(results);
            plannedRoles = Collections.unmodifiableMap(plannedByRole);
            insertedRoles = Collections.unmodifiableMap(insertedByRole);
            activeRoles = Collections.unmodifiableMap(activeByRole);
            waveFailures = Collections.unmodifiableMap(waveReasons);
            failures = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(siege.failures));
            elapsed = siege.elapsed;
            steps = siege.steps;
            arrivals = siege.arrivals;
            progress = siege.progress;
            candidates = siege.optionsTried;
            validRoutes = siege.validRoutes;
            failedRoutes = siege.failedRoutes;
            planningColumns = siege.planningColumns;
            columns = siege.columns;
            geometryAge = siege.lastVerified < 0 ? -1 : siege.elapsed - siege.lastVerified;
            skyReads = 0;
            heightReads = siege.heightReads;
            protectionReads = siege.protectionReads;
            peakColumns = siege.peakColumns;
            peakSkyReads = 0;
            planningNanos = siege.planningNanos;
            peakPlanningNanos = siege.peakPlanningNanos;
        }

        private static String point(PathPoint p) { return p.xCoord + "," + p.yCoord + "," + p.zCoord; }
    }
}
