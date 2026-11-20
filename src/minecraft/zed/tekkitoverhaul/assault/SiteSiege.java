package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityMob;
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
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;

/** Owns one bounded site objective. The director owns the lunar boundary and the major encounter decision. */
public final class SiteSiege {
    public enum Origin { NATURAL, MANUAL_DEBUG }
    public enum Profile { AUTO, PERIMETER, BREACH }

    private enum TacticalPhase { IDLE, PLANNING, APPROACHING, PRESSURING, BREACHING, ENGAGING, RESOLVED, ABORTED }

    enum Admission { WAIT, FALLBACK, COMMITTED }
    private static final int OPTIONS = 12, MAX_PARTS = 2, PLANNING_TICKS = 40;
    private static final int GROUP = 3, SPAWN_INTERVAL = 20, MAX_INSERTION_ATTEMPTS = 7;
    private static final int DEADLINE = 600, STALL = 120, PRESENCE = 48;
    private static final int LIGHT_WAIT = 900, MAX_LIGHT_ROLLS = 20;
    private final boolean enabled, destructive;
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
    private Origin origin = Origin.NATURAL;
    private Profile profile = Profile.AUTO;
    private TacticalPhase tacticalPhase = TacticalPhase.IDLE;
    private Attacker specialist;
    private int impactTick, windupHealth, breachChecks, structuralChanges, entranceChanges;
    private String objectiveOutcome = "NONE", tacticalRejection = "NO_ELIGIBLE_BREACH";
    private String insertionFailure;
    private boolean triedSpecialist;
    private PathPoint destination;

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

    Admission admit(EntityPlayer player, LunarSnapshot event) {
        if (!enabled) return Admission.FALLBACK;
        if (!planning()) {
            beginPlanning(player, Origin.NATURAL, Profile.AUTO, event.rawTime, event.sessionId);
            opportunity = SiteSiegePolicy.opportunity(world.getSeed(), event.cycleIndex);
            if (!opportunity) return fallback("RARITY");
            discoverSites(player);
            if (candidates.isEmpty()) return fallback("NO_ELIGIBLE_SITE");
        }
        if (world.getWorldTime() - boundary >= AssaultWave.FIRST_START - 1) return fallback("PLANNING_DEADLINE");
        return plan(player);
    }

    Admission admitManual(EntityPlayer player, Profile requested) {
        if (!planning()) {
            beginPlanning(player, Origin.MANUAL_DEBUG, requested, world.getWorldTime(), 0);
            discoverSites(player);
            if (candidates.isEmpty()) return fallback("NO_RECOGNIZED_SITE");
            if (selected.perimeterUnresolved && profile == Profile.BREACH) return fallback("SITE_FOOTPRINT_UNRESOLVED");
        }
        if (observer != player || !SiegeDirector.available(world, player)) return fallback("PLAYER_UNAVAILABLE");
        return plan(player);
    }

    private void beginPlanning(EntityPlayer player, Origin source, Profile requested, long time, long session) {
        resetPlan();
        resetCounters();
        selected = observed = null;
        approach = null;
        part = null;
        origin = source;
        profile = requested;
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

    private Admission plan(EntityPlayer player) {
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            frames++;
            if (frames > PLANNING_TICKS) return fallback("PLANNING_DEADLINE");
            if (approach == null) nextApproach(sampler);
            if (approach == null) return plansExhausted() ? noApproach() : Admission.WAIT;
            // A second sample is always required, with a fresh budget, before the first insertion.
            if (sampler.columns != 0) return Admission.WAIT;
            EntityMob mob = validateInsertion(player, sampler);
            if (mob == null) {
                if ("PROTECTION_DEFERRED".equals(rejection)) return Admission.WAIT;
                if (insertionFailure == null) insertionFailure = rejection;
                approach = null;
                return plansExhausted() ? noApproach() : Admission.WAIT;
            }
            if (!PerimeterApproachTask.available()) return fallback("NATIVE_TASK_UNAVAILABLE");
            if (!manual()) {
                String blocked = storage.siegeEligibility(selected.id, boundary);
                if (blocked != null) return fallback(blocked);
                storage.startSite(selected.id, boundary);
            }
            observer = player;
            phase = "ACTIVE";
            tacticalPhase = TacticalPhase.APPROACHING;
            destination = approach.goal();
            status = manual() ? "ADMITTED" : "WAITING_FOR_DARKNESS";
            resetPlan();
            return Admission.COMMITTED;
        } finally {
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
        return optionsTried >= candidates.size() * OPTIONS && frontages.isEmpty();
    }

    private void nextApproach(SiteApproach.Sampler sampler) {
        if (optionsTried % OPTIONS == 0 && !frontages.isEmpty()) {
            approach = frontages.remove(0);
            triedSpecialist |= approach.barrier != null;
            return;
        }
        if (plansExhausted()) return;
        Candidate candidate = candidates.get(optionsTried / OPTIONS);
        int option = optionsTried++ % OPTIONS;
        selected = candidate.site;
        part = candidate.part;
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null) return;
        Profile allowed = observed.perimeterUnresolved ? Profile.PERIMETER : profile;
        SiteApproach plan = sampler.plan(part, observed.envelope, option, allowed, destructive);
        if (sampler.breachRejection != null) tacticalRejection = sampler.breachRejection;
        if (plan == null || !sampler.openSky(plan.start()) || (observed.perimeterUnresolved && !sampler.localExterior(plan))) {
            rejection = sampler.rejection;
            if (plan != null && plan.barrier != null) tacticalRejection = rejection;
            return;
        }
        if (profile == Profile.BREACH && plan.barrier == null) return;
        if (plan.objective == SiteApproach.Objective.APPROACH_ENTRANCE
                || (profile == Profile.BREACH && plan.objective == SiteApproach.Objective.PRESSURE_ENTRANCE)) {
            approach = plan;
            triedSpecialist |= plan.barrier != null;
        } else {
            int index = 0;
            while (index < frontages.size() && frontages.get(index).objective.ordinal() >= plan.objective.ordinal())
                index++;
            frontages.add(index, plan);
        }
    }

    private EntityMob validateInsertion(EntityPlayer player, SiteApproach.Sampler sampler) {
        PathPoint start = approach.start();
        observed = sites.resolve(world, selected.id);
        if (currentPart() == null) return null;
        if (!nearSite(player)) return reject("PLAYER_ABSENT");
        if (!sites.insertionExterior(world, start.xCoord, start.zCoord, SiteApproach.MARGIN))
            return reject("INSIDE_SITE_EXCLUSION");
        if (!sampler.validate(approach) || !sampler.openSky(start)) return reject(sampler.rejection);
        if (observed.perimeterUnresolved && (approach.barrier != null || !sampler.localExterior(approach)))
            return reject(approach.barrier == null ? sampler.rejection : "SITE_FOOTPRINT_UNRESOLVED");
        if (inserted == 0 && approach.barrier != null) {
            sampler.breachChecks++;
            if (!approach.barrier.unchanged(world)) return reject("TARGET_CHANGED");
        }
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
        EntityMob mob = createAttacker();
        if (mob instanceof TacticalHostile
                && !world.getEntitiesWithinAABB(mob.getClass(), area).isEmpty()) return reject("SPECIALIST_POPULATION");

        mob.setLocationAndAngles(x, y, z, 0, 0);
        String collision = SpawnController.placementCollision(world, mob);
        if (collision != null) return reject(collision);
        if (!manual() && world.getSavedLightValue(EnumSkyBlock.Block, start.xCoord, start.yCoord, start.zCoord) > 7)
            return reject("INSERTION_TOO_BRIGHT");
        return mob;
    }

    private EntityMob createAttacker() {
        if (inserted == 0 && approach.barrier != null) {
            TacticalHostile mob = approach.barrier.structural() ? new EntityTank(world) : new EntityBreacher(world);
            mob.claimSiteSiege();
            return mob;
        }
        return manual() ? new DebugSiegeSkeleton(world) : new EntitySkeleton(world);
    }

    private static boolean nativeSpawn(EntityMob mob) {
        return mob instanceof TacticalHostile ? ((TacticalHostile) mob).canSpawnForEncounter() : mob.getCanSpawnHere();
    }

    void tick(EntityPlayer player, LunarSnapshot now, int activeCap) {
        if (!manual() && (now == null || now.sessionId != lunarSession || !AssaultSchedule.WINDOW.matches(now))) {
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
            long time = world.getWorldTime();
            if (time % 5 == 0 && !refreshSite()) return;
            if (manual() && ++elapsed >= PLANNING_TICKS) {
                finish("FIRST_INSERTION_TIMEOUT");
                return;
            }
            beginApproach(player, time);
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
        boolean reinforcing = tacticalPhase == TacticalPhase.APPROACHING
                && elapsed % SPAWN_INTERVAL == 0 && attempts < MAX_INSERTION_ATTEMPTS
                && inserted < Math.min(GROUP, activeCap) && attackers.size() < activeCap;
        if (!reinforcing && elapsed % 5 == 0 && !observeRoute()) return;
        if (specialist != null && tacticalPhase != TacticalPhase.ENGAGING) executeObjective();
        if (!active()) return;
        if (approach.barrier == null && arrivals > 0 && tacticalPhase == TacticalPhase.APPROACHING) {
            tacticalPhase = TacticalPhase.ENGAGING;
            objectiveOutcome = "DESTINATION_REACHED";
        }
        if (inserted > 0 && attackers.isEmpty()) {
            finish("ATTACKERS_GONE");
        } else if (reinforcing && tacticalPhase == TacticalPhase.APPROACHING) {
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
        if (!manual() && time - boundary >= LIGHT_WAIT + 100) {
            finish("FIRST_INSERTION_TIMEOUT");
            return;
        }
        if (!manual() && time < nextCheck) return;
        if (!manual() && world.skylightSubtracted < 11 && time - boundary < LIGHT_WAIT) {
            nextCheck = time + 20;
            return;
        }
        status = "WAITING_FOR_NATIVE_SPAWN";
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            EntityMob mob = validateInsertion(player, sampler);
            if (mob == null) {
                if (!"PROTECTION_DEFERRED".equals(rejection)) finish(rejection);
                return;
            }
            if (!manual() && !nativeSpawn(mob)) {
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
            status = approach.objective.name();
            insert(mob, task);
            if (inserted == 0 && manual()) finish(rejection);
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
                PathPoint[] route = attacker.task.waypoints();
                for (int i = Math.max(0, index - 1); i < Math.min(index + 2, route.length); i++) {
                    if (!sampler.standing(route[i], attacker.mob instanceof EntityTank)) {
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
            EntityMob mob = validateInsertion(player, sampler);
            if (mob == null) return;
            if (!manual() && !nativeSpawn(mob)) {
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

    private void insert(EntityMob mob, PerimeterApproachTask task) {
        if (!world.spawnEntityInWorld(mob)) {
            task.release();
            rejection = "INSERTION_REJECTED";
            return;
        }
        Attacker attacker = new Attacker(mob, task, elapsed);
        attackers.add(attacker);
        if (mob instanceof TacticalHostile) specialist = attacker;
        inserted++;
        peak = Math.max(peak, attackers.size());
    }

    private void refreshAttackers() {
        Iterator<Attacker> iterator = attackers.iterator();
        while (iterator.hasNext()) {
            Attacker attacker = iterator.next();
            EntityMob mob = attacker.mob;
            int x = MathHelper.floor_double(mob.posX), z = MathHelper.floor_double(mob.posZ);
            if (!mob.isEntityAlive() || mob.worldObj != world || !world.blockExists(x, 64, z)
                    || !world.getEntitiesWithinAABB(EntityMob.class, mob.boundingBox).contains(mob)) {
                release(attacker);
                iterator.remove();
                if (attacker == specialist) {
                    specialist = null;
                    if (tacticalPhase != TacticalPhase.ENGAGING) {
                        finish("SPECIALIST_LOST");
                        return;
                    }
                }
                continue;
            }
            PathPoint first = approach.start(), goal = approach.goal();
            double dx = mob.posX - first.xCoord - 0.5, dz = mob.posZ - first.zCoord - 0.5;
            double length = Math.abs(goal.xCoord - first.xCoord) + Math.abs(goal.zCoord - first.zCoord);
            double advance = (dx * (goal.xCoord - first.xCoord) + dz * (goal.zCoord - first.zCoord)) / length;
            if (attacker.task.arrived() && destination == goal) advance = Math.min(advance, length);
            progress = Math.max(progress, advance);
            int walked = attacker.task.steps();
            steps = Math.max(steps, walked);
            if (walked > attacker.steps) {
                attacker.lastProgress = elapsed;
                attacker.steps = walked;
            } else if (!attacker.task.arrived() && elapsed - attacker.lastProgress >= STALL) {
                finish("APPROACH_STALLED");
                return;
            }
            if (attacker.task.arrived() && !attacker.counted
                    && (approach.barrier == null || tacticalPhase == TacticalPhase.ENGAGING)) {
                attacker.counted = true;
                arrivals++;
            }
        }
    }

    private void executeObjective() {
        if (!specialist.task.arrived()) return;
        long start = System.nanoTime();
        SiteApproach.Sampler sampler = new SiteApproach.Sampler(world);
        try {
            SiegeBarrier barrier = approach.barrier;
            EntityMob mob = specialist.mob;
            if (tacticalPhase != TacticalPhase.APPROACHING && mob.getHealth() < windupHealth) {
                finish("WINDUP_INTERRUPTED");
                return;
            }
            if (!barrier.loaded(world)) {
                finish("TARGET_UNLOADED");
                return;
            }
            sampler.breachChecks++;
            if (!barrier.unchanged(world)) {
                if (!barrier.structural() && passage(sampler, false) != null)
                    advanceObjective(sampler, "ENTRANCE_ALREADY_OPEN");
                else finish("TARGET_CHANGED");
                return;
            }
            if (!barrier.inReach(mob, 0) || !sampler.standing(approach.goal(), mob instanceof EntityTank)) {
                finish("TARGET_OUT_OF_REACH");
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
                finish("PASSAGE_BLOCKED");
                return;
            }
            sampler.breachChecks++;
            int changed = barrier.impact(mob);
            if (mob instanceof EntityTank) {
                structuralChanges += changed;
                ((EntityTank) mob).endSiteWindup(changed > 0);
            } else {
                entranceChanges += changed;
                if (changed > 0) ((EntityBreacher) mob).signalSiteImpact();
            }
            if (changed == 0) finish("IMPACT_REJECTED");
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
        tacticalPhase = TacticalPhase.ENGAGING;
        objectiveOutcome = reason;
        PathPoint[] narrow = passage(sampler, false);
        if (narrow == null) {
            finish("PASSAGE_BLOCKED");
            return;
        }
        destination = approach.barrier.inside;
        PathPoint[] wide = approach.barrier.structural() ? passage(sampler, true) : null;
        for (Attacker attacker : attackers) {
            PathPoint[] route = attacker.mob instanceof EntityTank ? wide : narrow;
            if (route == null) attacker.task.release();
            else attacker.task.advance(route);
        }
    }

    void interrupt(String cause) {
        if (active()) finish(cause);
        else if (planning()) fallback(cause);
    }

    private void finish(String cause) {
        for (Attacker attacker : attackers) release(attacker);
        attackers.clear();
        specialist = null;
        boolean reached = "FRONTAGE_REACHED".equals(cause);
        tacticalPhase = reached ? TacticalPhase.RESOLVED : TacticalPhase.ABORTED;
        if ("NONE".equals(objectiveOutcome)) objectiveOutcome = cause;
        observer = null;
        phase = "TERMINAL";
        outcome = cause;
        status = "RESOLVED";
        if (!manual()) storage.closeSite(cause);
    }

    private void release(Attacker attacker) {
        attacker.task.release();
        if ((manual() || attacker.mob instanceof TacticalHostile) && attacker.mob.isEntityAlive()) attacker.mob.setDead();
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
        optionsTried = frames = attempts = inserted = peak = elapsed = arrivals = steps = peakColumns = peakSkyReads = 0;
        lastVerified = -1;
        lightRolls = 0;
        nextCheck = Long.MIN_VALUE;
        columns = heightReads = skyReads = protectionReads = planningNanos = peakPlanningNanos = 0;
        progress = 0;
        destination = null;
        specialist = null;
        breachChecks = structuralChanges = entranceChanges = 0;
        triedSpecialist = false;
        objectiveOutcome = "NONE";
        tacticalRejection = "NO_ELIGIBLE_BREACH";
        insertionFailure = null;
        rejection = "NONE";
    }

    private void account(SiteApproach.Sampler sampler, long start) {
        columns += sampler.columns;
        heightReads += sampler.heightReads;
        breachChecks += sampler.breachChecks;
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
        int steps, lastProgress;
        boolean counted;

        Attacker(EntityMob mob, PerimeterApproachTask task, int elapsed) {
            this.mob = mob;
            this.task = task;
            lastProgress = elapsed;
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
        public final String tacticalPhase, objectiveOutcome, specialist, objectiveTarget;
        public final boolean opportunity, dormant, destructive;
        public final int breachChecks, structuralChanges, entranceChanges;
        public final int site, dimension, active, inserted, peak, attempts, remaining, elapsed, steps, arrivals;
        public final int candidates, peakColumns, peakSkyReads, geometryAge;
        public final long boundary, nextOpportunityCycle, cooldownUntil, observation, siteSession;
        public final long columns, skyReads, heightReads, protectionReads;
        public final long planningNanos, peakPlanningNanos;
        public final double progress;

        Snapshot(SiteSiege siege, SiegeDirector.EncounterKind kind, int activeCap, LunarSnapshot lunar,
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
            SiteApproach lane = siege.approach;
            approach = lane == null ? "NONE" : point(lane.start()) + " > "
                    + point(siege.destination == null ? lane.goal() : siege.destination);
            objective = lane == null ? "NONE" : lane.objective.name();
            objectiveTarget = lane == null || lane.barrier == null ? "NONE" : point(lane.barrier.target);
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
            remaining = siege.active() && siege.tacticalPhase == TacticalPhase.APPROACHING && attempts < MAX_INSERTION_ATTEMPTS
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
