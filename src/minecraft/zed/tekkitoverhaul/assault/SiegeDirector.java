package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarService;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;

/** Local player-vicinity encounters. All methods run on the owning simulation thread. */
public final class SiegeDirector implements LunarService.Listener {
    public enum State { IDLE, ACTIVE, RESOLVED, ABORTED }
    public enum Eligibility {
        UNAVAILABLE("Unavailable"), ACTIVE("Encounter active"), PEACEFUL("Peaceful difficulty"),
        PLAYER("Requires an awake Survival player"), PHASE("Waiting for full moon"),
        NIGHT("Requires a natural dusk"), CONSUMED("Night already consumed"), DUSK("Eligible at natural dusk");

        public final String label;
        Eligibility(String label) { this.label = label; }
    }

    private final boolean enabled;
    private final boolean experimentalEnderkin;
    private final int budget;
    private final int interval;
    private final int activeCap;
    private final int outerRadius;
    private final SpawnController spawns;
    private final LunarPatrol patrol;
    private final EncounterLoadouts loadouts = new EncounterLoadouts();
    private final List<Spawn> evidence = new ArrayList<Spawn>();
    private final List<TrackedMob> tracked = new ArrayList<TrackedMob>();
    private AssaultWave[] waves = AssaultWave.plan(0);
    private World world;
    private LunarService lunar;
    private AssaultDiagnostics diagnostics;
    private AssaultWorldState storage;
    private LunarSnapshot pending;
    private EntityPlayer target;
    private State state = State.IDLE;
    private String reason = "NO_WORLD";
    private int elapsed;
    private int waveIndex = -1;
    private int localHostiles = -1;
    private int peakActive;
    private int observedDeaths;
    private int removed;
    private int unloaded;
    private int starts;
    private int difficulty;
    public SiegeDirector(boolean enabled, int budget, int interval, int activeCap, int outerRadius,
                         boolean patrolEnabled, boolean outdoorPressure, boolean experimentalEnderkin) {
        this.enabled = enabled;
        this.experimentalEnderkin = experimentalEnderkin;
        this.budget = Math.max(1, Math.min(24, budget));
        this.interval = Math.max(400, Math.min(1200, interval));
        this.activeCap = Math.max(2, Math.min(12, activeCap));
        this.outerRadius = Math.max(14, Math.min(28, outerRadius));
        spawns = new SpawnController(this.outerRadius, outdoorPressure);
        patrol = new LunarPatrol(patrolEnabled, outdoorPressure, experimentalEnderkin);
    }

    public void bind(World incoming, LunarService service) {
        if (world != null) {
            String cause = incoming != null && world.worldProvider.worldType != incoming.worldProvider.worldType
                    ? "DIMENSION_CHANGED" : "WORLD_UNLOADED";
            patrol.interrupt(world.getWorldTime(), cause, diagnostics);
        }
        if (diagnostics != null) {
            if (state == State.ACTIVE) diagnostics.assault(world.getWorldTime(), "Interrupted by world change");
        }
        if (lunar != null) lunar.unsubscribe(this);
        world = incoming;
        lunar = service;
        patrol.reset();
        storage = null;
        pending = null;
        target = null;
        evidence.clear();
        loadouts.reset(LunarPhase.NEW_MOON);
        tracked.clear();
        waves = AssaultWave.plan(0);
        state = State.IDLE;
        elapsed = starts = peakActive = observedDeaths = removed = unloaded = 0;
        waveIndex = localHostiles = -1;
        spawns.rejection = null;
        reason = !enabled ? "DISABLED" : "NO_OVERWORLD";
        if (world == null || lunar == null || world.isRemote || world.worldProvider.worldType != 0) return;
        if (patrol.enabled) lunar.subscribe(this);
        if (!enabled) return;
        storage = AssaultWorldState.load(world);
        if (storage == null) {
            reason = "STORAGE_UNAVAILABLE";
            return;
        }
        if (storage.interrupted) {
            state = State.ABORTED;
            reason = "INTERRUPTED_ON_REOPEN";
            storage.close();
            if (diagnostics != null) diagnostics.assault(world.getWorldTime(), "Interrupted on reopen");
        } else if (storage.consumed != Long.MIN_VALUE) {
            state = State.RESOLVED;
            reason = "NIGHT_CONSUMED";
        } else {
            reason = "WAITING_FOR_FULL_MOON";
        }
        lunar.subscribe(this);
    }

    public void setDiagnostics(AssaultDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public void onLunarEvent(LunarEvent event) {
        if (patrol.enabled) patrol.onEvent(world, event, diagnostics);
        if (storage == null) return;
        if (event.kind == LunarEvent.Kind.RESYNC) {
            pending = null;
            if (state == State.ACTIVE) end(State.ABORTED, event.cause.name());
            if (diagnostics != null && AssaultSchedule.WINDOW.matches(event.after)) {
                diagnostics.assault(event.after.rawTime, "No assault boundary: " + event.cause
                        + " | consumed " + storage.consumed + " | starts " + starts);
            }
        } else if (event.kind == LunarEvent.Kind.NIGHT_ENDED && state == State.ACTIVE) {
            end(State.RESOLVED, "NIGHT_ENDED");
        } else if (state != State.ACTIVE && AssaultSchedule.canStart(event, storage.consumed)) {
            pending = event.after;
        } else if (diagnostics != null && event.kind == LunarEvent.Kind.NIGHT_STARTED
                && AssaultSchedule.WINDOW.matches(event.after)) {
            String rejection = event.cause != LunarEvent.Cause.NATURAL ? event.cause.name()
                    : state == State.ACTIVE ? "ACTIVE" : "CONSUMED";
            diagnostics.assault(event.after.rawTime, "Eligibility rejected: " + rejection
                    + " | consumed " + storage.consumed + " | starts " + starts);
        }
    }

    public void tick(EntityPlayer player, boolean paused) {
        if (patrol.state == State.ACTIVE) patrol.tick(world, lunar.snapshot(), player, paused, diagnostics);
        if (storage == null) return;
        LunarSnapshot boundary = pending;
        pending = null;
        if (paused) {
            if (diagnostics != null && boundary != null) diagnostics.assault(boundary.rawTime, "Eligibility rejected: paused");
            return;
        }
        if (boundary != null) {
            if (world.difficultySetting <= 0 || !available(world, player)) {
                reason = world.difficultySetting <= 0 ? "PEACEFUL" : "PLAYER_UNAVAILABLE";
                if (diagnostics != null) diagnostics.assault(boundary.rawTime, "Eligibility rejected: " + AssaultDiagnostics.label(reason));
                return;
            }
            storage.start(boundary.rawTime);
            state = State.ACTIVE;
            reason = "NATURAL_FULL_MOON";
            target = player;
            difficulty = world.difficultySetting;
            elapsed = observedDeaths = removed = unloaded = 0;
            waveIndex = -1;
            localHostiles = -1;
            peakActive = tracked.size();
            waves = AssaultWave.plan(budget);
            for (AssaultWave wave : waves) {
                if (wave.planned > 0) wave.special.reset(EnderkinRole.select(experimentalEnderkin,
                        world.getSeed(), boundary.logicalDay, boundary.phase, wave.number));
            }
            loadouts.reset(boundary.phase);
            starts++;
            evidence.clear();
            spawns.rejection = null;
            if (diagnostics != null) {
                diagnostics.assault(boundary.rawTime, "Eligibility accepted: natural full moon");
                diagnostics.assault(boundary.rawTime, "Player assault started");
            }
            world.playSoundEffect(player.posX, player.posY, player.posZ, "ambient.cave.cave", 0.5F, 0.85F);
            return;
        }
        if (state != State.ACTIVE) {
            if (!tracked.isEmpty() && world.getWorldTime() % AssaultWave.RETRY_INTERVAL == 0) refreshPopulation(player);
            return;
        }
        if (!available(world, player) || player != target) {
            end(State.ABORTED, "PLAYER_UNAVAILABLE");
        } else if (world.difficultySetting != difficulty) {
            end(State.ABORTED, world.difficultySetting == 0 ? "PEACEFUL" : "DIFFICULTY_CHANGED");
        } else if (!AssaultSchedule.WINDOW.matches(lunar.snapshot())) {
            end(State.RESOLVED, "NIGHT_ENDED");
        } else {
            advanceWaves(player);
        }
    }

    private void advanceWaves(EntityPlayer player) {
        elapsed++;
        if (elapsed % AssaultWave.RETRY_INTERVAL == 0) refreshPopulation(player);
        if (waveIndex + 1 < waves.length && elapsed >= nextWaveAt()) {
            AssaultWave wave = waves[++waveIndex];
            wave.startedAt = elapsed;
            if (diagnostics != null) {
                diagnostics.beginWave(world.getWorldTime(), wave);
                diagnostics.selected(world.getWorldTime(), "Wave " + wave.number, wave.special);
            }
        }
        if (waveIndex < 0) return;
        AssaultWave wave = waves[waveIndex];
        if (wave.closedAt < 0) {
            int age = elapsed - wave.startedAt;
            if (age >= AssaultWave.WINDOW) {
                closeWave(wave, "SPAWN_WINDOW_EXPIRED", true);
            } else if (wave.unspent() > 0 && age % AssaultWave.RETRY_INTERVAL == 0) {
                if (elapsed % AssaultWave.RETRY_INTERVAL != 0) refreshPopulation(player);
                retry(wave, player);
            }
            if (wave.closedAt < 0 && wave.unspent() == 0) closeWave(wave, "ALLOCATION_EXHAUSTED", false);
        }
        if (wave.closedAt < 0) return;
        for (int i = waveIndex + 1; i < waves.length; i++) {
            if (waves[i].unspent() > 0) return;
        }
        if (tracked.isEmpty()) end(State.RESOLVED, "WAVES_COMPLETE");
        else if (elapsed >= wave.closedAt + AssaultWave.FINAL_ENGAGEMENT) end(State.RESOLVED, "ENCOUNTER_TIMEOUT");
    }

    private int nextWaveAt() {
        if (waveIndex < 0) return AssaultWave.FIRST_START;
        AssaultWave current = waves[waveIndex];
        if (current.closedAt < 0) return current.startedAt + interval;
        for (int i = waveIndex + 1; i < waves.length; i++) {
            if (waves[i].unspent() > 0) return current.nextStart(interval, tracked.size());
        }
        return Integer.MAX_VALUE;
    }

    private void retry(AssaultWave wave, EntityPlayer player) {
        wave.localHostiles = localHostiles;
        wave.peakActive = Math.max(wave.peakActive, tracked.size());
        if (tracked.size() >= activeCap || localHostiles >= SpawnController.LOCAL_CAP) {
            spawns.rejection = tracked.size() >= activeCap ? "ASSAULT_CAP" : "LOCAL_MOB_CAP";
            wave.reject(spawns.rejection);
            return;
        }
        for (int tries = 0; tries < AssaultWave.CANDIDATES_PER_RETRY; tries++) {
            wave.attempts++;
            // Vanilla Hard zombies can break wooden doors. Keep vanilla AI and use skeletons there.
            boolean skeleton = difficulty == 3 || (wave.number == 3 ? wave.inserted % 2 == 0 : wave.inserted % 3 == 2);
            EnderkinRole role = wave.special.pending();
            EntityMob mob = spawns.trySpawn(world, player, skeleton, role);
            if (role != null) {
                wave.special.record(spawns.rejection);
                if (diagnostics != null) diagnostics.specialAttempt(world.getWorldTime(), "Wave " + wave.number, wave.special);
            }
            if (mob != null) {
                int equippedBefore = loadouts.equipped;
                String profile = loadouts.apply(mob, wave.number, wave.equipped).name();
                wave.equipped += loadouts.equipped - equippedBefore;
                Spawn spawn = new Spawn(mob, player, wave.number, elapsed, profile, spawns.outdoorLight);
                evidence.add(spawn);
                tracked.add(new TrackedMob(mob, wave));
                wave.inserted++;
                if (spawn.outdoorLight) wave.outdoorLight++;
                wave.active++;
                wave.localHostiles = ++localHostiles;
                wave.peakActive = Math.max(wave.peakActive, tracked.size());
                peakActive = Math.max(peakActive, tracked.size());
                if (diagnostics != null) diagnostics.spawned(world.getWorldTime(), spawn);
                return;
            }
            wave.reject(spawns.rejection);
        }
    }

    private void closeWave(AssaultWave wave, String outcome, boolean carry) {
        if (carry && wave.number < waves.length && wave.unspent() > 0) {
            wave.carriedOut = 1;
            waves[wave.number].carriedIn = 1;
        }
        wave.closedAt = elapsed;
        wave.outcome = outcome;
        if (diagnostics != null) diagnostics.endWave(world.getWorldTime(), wave);
    }

    private void refreshPopulation(EntityPlayer player) {
        Iterator<TrackedMob> mobs = tracked.iterator();
        while (mobs.hasNext()) {
            TrackedMob entry = mobs.next();
            EntityMob mob = entry.mob;
            if (mob.getHealth() <= 0) observedDeaths++;
            else if (mob.isDead || mob.worldObj != world) removed++;
            else if (!world.blockExists((int) Math.floor(mob.posX), 64, (int) Math.floor(mob.posZ))) unloaded++;
            else if (world.getEntitiesWithinAABB(EntityMob.class, mob.boundingBox).contains(mob)) continue;
            else removed++;
            entry.wave.active--;
            mobs.remove();
        }
        localHostiles = player == null || player.worldObj != world ? -1 : spawns.localHostiles(world, player);
    }

    static boolean available(World world, EntityPlayer player) {
        return player != null && player.worldObj == world && player.isEntityAlive()
                && !player.isPlayerSleeping() && !player.capabilities.isCreativeMode;
    }

    private void end(State terminal, String cause) {
        for (AssaultWave wave : waves) {
            if (wave.closedAt < 0) closeWave(wave, wave.unspent() == 0 ? "NO_ALLOCATION" : cause, false);
        }
        state = terminal;
        reason = cause;
        target = null;
        storage.close();
        if (diagnostics != null) diagnostics.assault(world.getWorldTime(),
                (terminal == State.ABORTED ? "Interrupted: " : "Completed: ") + AssaultDiagnostics.label(cause));
    }

    public Snapshot snapshot(World context) {
        if (context == null || context != world) return null;
        return new Snapshot(this);
    }

    /** Current start conditions, independent of the recorded outcome of the previous encounter. */
    public Eligibility eligibility(World context, EntityPlayer player) {
        if (context == null || context != world || storage == null) return Eligibility.UNAVAILABLE;
        if (state == State.ACTIVE) return Eligibility.ACTIVE;
        if (world.difficultySetting <= 0) return Eligibility.PEACEFUL;
        if (!available(world, player)) return Eligibility.PLAYER;
        LunarSnapshot view = lunar.snapshot();
        if (!view.isFullMoon()) return Eligibility.PHASE;
        if (view.timeOfDay >= LunarSnapshot.NIGHT_START) return Eligibility.NIGHT;
        long untilDusk = LunarSnapshot.NIGHT_START - view.timeOfDay;
        if (view.rawTime > Long.MAX_VALUE - untilDusk) return Eligibility.UNAVAILABLE;
        return view.rawTime + untilDusk <= storage.consumed ? Eligibility.CONSUMED : Eligibility.DUSK;
    }

    public static final class Spawn {
        public final String mob, profile;
        public final double x, y, z, horizontalSquared;
        public final int wave, tick;
        public final boolean outdoorLight;

        private Spawn(EntityMob entity, EntityPlayer target, int wave, int tick, String profile, boolean outdoorLight) {
            this.wave = wave;
            this.tick = tick;
            mob = entity instanceof EntitySkirmisher ? "SKIRMISHER" : entity instanceof EntityBreacher ? "BREACHER"
                    : entity instanceof EntitySkeleton ? "SKELETON" : "ZOMBIE";
            this.profile = profile;
            this.outdoorLight = outdoorLight;
            x = entity.posX;
            y = entity.posY;
            z = entity.posZ;
            double dx = x - target.posX, dz = z - target.posZ;
            horizontalSquared = dx * dx + dz * dz;
        }
    }

    private static final class TrackedMob {
        final EntityMob mob;
        final AssaultWave wave;

        TrackedMob(EntityMob mob, AssaultWave wave) {
            this.mob = mob;
            this.wave = wave;
        }
    }

    public static final class WaveSnapshot {
        public final EnderkinSnapshot enderkin;
        public final int equipped, equipmentCap;
        public final int outdoorLight;
        public final int number, planned, allocation, inserted, attempts, deferred, skipped, active;
        public final int startedAt, closedAt, carriedIn, carriedOut, localHostiles, peakActive;
        public final String state, outcome;
        public final Map<String, Integer> rejections;

        private WaveSnapshot(AssaultWave wave) {
            enderkin = new EnderkinSnapshot(wave.special);
            equipped = wave.equipped;
            equipmentCap = LoadoutPolicy.assaultWaveCap(wave.number);
            number = wave.number;
            planned = wave.planned;
            allocation = wave.allocation();
            inserted = wave.inserted;
            outdoorLight = wave.outdoorLight;
            attempts = wave.attempts;
            deferred = wave.deferred();
            skipped = wave.skipped();
            active = wave.active;
            startedAt = wave.startedAt;
            closedAt = wave.closedAt;
            carriedIn = wave.carriedIn;
            carriedOut = wave.carriedOut;
            localHostiles = wave.localHostiles;
            peakActive = wave.peakActive;
            state = wave.state();
            outcome = wave.outcome;
            rejections = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(wave.rejections));
        }
    }

    public static final class EnderkinSnapshot {
        public final String selected;
        public final int attempts;
        public final boolean inserted;
        public final Map<String, Integer> rejections;

        EnderkinSnapshot(EnderkinSlot slot) {
            selected = slot.selected == null ? null : slot.selected.name();
            attempts = slot.attempts;
            inserted = slot.inserted;
            rejections = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(slot.rejections));
        }

        public String summary() {
            return AssaultDiagnostics.label(selected) + " | Attempts " + attempts + " | Inserted " + (inserted ? 1 : 0)
                    + (rejections.isEmpty() ? "" : " | " + AssaultDiagnostics.counts(rejections));
        }
    }

    public static final class LoadoutSnapshot {
        public final LunarPhase phase;
        public final int equipped, unmodified, failures, cap;
        public final Map<String, Integer> selected;

        LoadoutSnapshot(EncounterLoadouts loadouts, int inserted) {
            phase = loadouts.phase;
            equipped = loadouts.equipped;
            unmodified = inserted - equipped;
            failures = loadouts.failures;
            cap = LoadoutPolicy.cap(phase);
            selected = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(loadouts.selected));
        }

        public String summary() {
            return phase + " | Equipped " + equipped + "/" + cap + " | Unmodified " + unmodified
                    + " | Assignment failures " + failures
                    + (selected.isEmpty() ? "" : " | " + AssaultDiagnostics.counts(selected));
        }
    }

    public static final class Snapshot {
        public final State state;
        public final String reason, lastRejection;
        public final long boundary, lunarSession;
        public final int dimension, starts, currentWave, nextWaveTicks, attempts, elapsed, budget, deferred, skipped;
        public final int active, peakActive, localHostiles, observedDeaths, removed, unloaded;
        public final int activeCap, localCap, innerRadius, outerRadius;
        public final String trigger;
        public final List<Spawn> spawns;
        public final List<WaveSnapshot> waves;
        public final PatrolSnapshot patrol;
        public final LoadoutSnapshot loadouts;
        public final boolean experimentalEnderkin;
        public final int activeEnderkin;

        private Snapshot(SiegeDirector director) {
            state = director.state;
            reason = director.reason;
            lastRejection = director.spawns.rejection;
            boundary = director.storage == null ? Long.MIN_VALUE : director.storage.consumed;
            lunarSession = director.lunar == null ? 0 : director.lunar.snapshot().sessionId;
            dimension = director.world.worldProvider.worldType;
            starts = director.starts;
            currentWave = director.waveIndex + 1;
            int next = currentWave == AssaultWave.COUNT || state != State.ACTIVE ? Integer.MAX_VALUE : director.nextWaveAt();
            nextWaveTicks = next == Integer.MAX_VALUE ? -1 : next - director.elapsed;
            List<WaveSnapshot> summaries = new ArrayList<WaveSnapshot>(AssaultWave.COUNT);
            int tried = 0, waiting = 0, lost = 0;
            for (AssaultWave wave : director.waves) {
                summaries.add(new WaveSnapshot(wave));
                tried += wave.attempts;
                waiting += wave.deferred();
                lost += wave.skipped();
            }
            waves = Collections.unmodifiableList(summaries);
            attempts = tried;
            deferred = waiting;
            skipped = lost;
            active = director.tracked.size();
            experimentalEnderkin = director.experimentalEnderkin;
            int tactical = 0;
            for (TrackedMob entry : director.tracked) if (entry.mob instanceof TacticalHostile) tactical++;
            activeEnderkin = tactical;
            peakActive = director.peakActive;
            localHostiles = director.localHostiles;
            observedDeaths = director.observedDeaths;
            removed = director.removed;
            unloaded = director.unloaded;
            activeCap = director.activeCap;
            localCap = SpawnController.LOCAL_CAP;
            innerRadius = SpawnController.INNER_RADIUS;
            outerRadius = director.outerRadius;
            elapsed = director.elapsed;
            budget = director.budget;
            trigger = starts == 0 ? null : "NATURAL_FULL_MOON";
            spawns = Collections.unmodifiableList(new ArrayList<Spawn>(director.evidence));
            patrol = new PatrolSnapshot(director);
            loadouts = new LoadoutSnapshot(director.loadouts, spawns.size());
        }
    }

    public static final class PatrolSnapshot {
        public final EnderkinSnapshot enderkin;
        public final boolean eligibleNight, paused;
        public final State state;
        public final String reason;
        public final String stage;
        public final int spawned, outdoorLight, budget, attempts, elapsed, remaining, skipped;
        public final Map<String, Integer> rejections;
        public final LoadoutSnapshot loadouts;

        private PatrolSnapshot(SiegeDirector director) {
            LunarPatrol patrol = director.patrol;
            enderkin = new EnderkinSnapshot(patrol.special);
            eligibleNight = patrol.enabled && director.lunar != null
                    && PatrolSchedule.eligibleNight(director.lunar.snapshot());
            state = patrol.state;
            reason = patrol.reason;
            paused = patrol.paused;
            stage = state != State.ACTIVE ? state.name()
                    : patrol.elapsed < PatrolSchedule.SPAWN_DELAY ? "PENDING" : "SPAWNING";
            spawned = patrol.spawned;
            outdoorLight = patrol.outdoorLight;
            budget = patrol.budget;
            attempts = patrol.attempts;
            elapsed = patrol.elapsed;
            remaining = state == State.ACTIVE ? budget - spawned : 0;
            skipped = state == State.ACTIVE ? 0 : budget - spawned;
            rejections = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(patrol.rejections));
            loadouts = new LoadoutSnapshot(patrol.loadouts, spawned);
        }
    }
}
