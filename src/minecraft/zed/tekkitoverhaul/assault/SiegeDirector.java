package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarService;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** One local player encounter. All methods run on the owning simulation thread. */
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
    private final int budget;
    private final int interval;
    private final SpawnController spawns = new SpawnController();
    private final List<Spawn> evidence = new ArrayList<Spawn>();
    private World world;
    private LunarService lunar;
    private AssaultDiagnostics diagnostics;
    private AssaultWorldState storage;
    private LunarSnapshot pending;
    private EntityPlayer target;
    private State state = State.IDLE;
    private String reason = "NO_WORLD";
    private int elapsed;
    private int pulses;
    private int attempts;
    private int starts;
    private int difficulty;
    private static final int FIRST_PULSE = 100;
    private static final int MAX_PULSES = 3;

    public SiegeDirector(boolean enabled, int budget, int interval) {
        this.enabled = enabled;
        this.budget = Math.max(1, Math.min(6, budget));
        this.interval = Math.max(80, Math.min(400, interval));
    }

    public void bind(World incoming, LunarService service) {
        if (diagnostics != null) {
            if (state == State.ACTIVE) diagnostics.assault(world.getWorldTime(), "Interrupted by world change");
            diagnostics.resetEncounter();
        }
        if (lunar != null) lunar.unsubscribe(this);
        world = incoming;
        lunar = service;
        storage = null;
        pending = null;
        target = null;
        evidence.clear();
        state = State.IDLE;
        elapsed = pulses = attempts = starts = 0;
        spawns.rejection = null;
        reason = !enabled ? "DISABLED" : "NO_OVERWORLD";
        if (!enabled || world == null || lunar == null || world.isRemote || world.worldProvider.worldType != 0) return;
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
        if (event.kind == LunarEvent.Kind.RESYNC) {
            pending = null;
            if (state == State.ACTIVE) end(State.ABORTED, event.cause.name());
        } else if (event.kind == LunarEvent.Kind.NIGHT_ENDED && state == State.ACTIVE) {
            end(State.RESOLVED, "NIGHT_ENDED");
        } else if (state != State.ACTIVE && AssaultSchedule.canStart(event, storage.consumed)) {
            pending = event.after;
        } else if (diagnostics != null && event.kind == LunarEvent.Kind.NIGHT_STARTED
                && AssaultSchedule.WINDOW.matches(event.after)) {
            diagnostics.assault(event.after.rawTime, "Eligibility rejected: night already consumed or active");
        }
    }

    public void tick(EntityPlayer player, boolean paused) {
        if (storage == null) return;
        LunarSnapshot boundary = pending;
        pending = null;
        if (paused) {
            if (diagnostics != null && boundary != null) diagnostics.assault(boundary.rawTime, "Eligibility rejected: paused");
            return;
        }
        if (boundary != null) {
            if (world.difficultySetting <= 0 || !available(player)) {
                reason = world.difficultySetting <= 0 ? "PEACEFUL" : "PLAYER_UNAVAILABLE";
                if (diagnostics != null) diagnostics.assault(boundary.rawTime, "Eligibility rejected: " + AssaultDiagnostics.label(reason));
                return;
            }
            storage.start(boundary.rawTime);
            state = State.ACTIVE;
            reason = "NATURAL_FULL_MOON";
            target = player;
            difficulty = world.difficultySetting;
            elapsed = attempts = pulses = 0;
            starts++;
            evidence.clear();
            spawns.rejection = null;
            if (diagnostics != null) {
                diagnostics.resetEncounter();
                diagnostics.assault(boundary.rawTime, "Eligibility accepted: natural full moon");
                diagnostics.assault(boundary.rawTime, "Player assault started");
            }
            world.playSoundEffect(player.posX, player.posY, player.posZ, "ambient.cave.cave", 0.5F, 0.85F);
            return;
        }
        if (state != State.ACTIVE) return;
        if (!available(player) || player != target) {
            end(State.ABORTED, "PLAYER_UNAVAILABLE");
        } else if (world.difficultySetting != difficulty) {
            end(State.ABORTED, world.difficultySetting == 0 ? "PEACEFUL" : "DIFFICULTY_CHANGED");
        } else if (!AssaultSchedule.WINDOW.matches(lunar.snapshot())) {
            end(State.RESOLVED, "NIGHT_ENDED");
        } else if (++elapsed >= FIRST_PULSE && (elapsed - FIRST_PULSE) % interval == 0) {
            pulse(player);
            if (evidence.size() == budget) end(State.RESOLVED, "BUDGET_EXHAUSTED");
            else if (pulses == MAX_PULSES) end(State.RESOLVED, evidence.isEmpty() ? "NO_VALID_POSITION" : "PULSES_COMPLETE");
        }
    }

    private void pulse(EntityPlayer player) {
        pulses++;
        int placed = 0;
        int previousAttempts = attempts;
        long time = diagnostics == null ? 0 : world.getWorldTime();
        if (diagnostics != null) diagnostics.beginPulse(time, pulses);
        for (int tries = 0; placed < 2 && tries < SpawnController.ATTEMPTS && evidence.size() < budget; tries++) {
            if (!spawns.belowLocalCap(world, player)) {
                spawns.rejection = "LOCAL_MOB_CAP";
                if (diagnostics != null) diagnostics.reject(spawns.rejection);
                break;
            }
            attempts++;
            // Vanilla Hard zombies can break wooden doors. Keep vanilla AI and use skeletons there.
            EntityMob mob = spawns.trySpawn(world, player, difficulty == 3 || evidence.size() % 3 == 2);
            if (mob != null) {
                Spawn spawn = new Spawn(mob, player);
                evidence.add(spawn);
                if (diagnostics != null) diagnostics.spawned(time, pulses, spawn);
                placed++;
            } else if (diagnostics != null) {
                diagnostics.reject(spawns.rejection);
            }
        }
        if (diagnostics != null) diagnostics.endPulse(time, pulses, placed, attempts - previousAttempts);
    }

    private boolean available(EntityPlayer player) {
        return player != null && player.worldObj == world && player.isEntityAlive()
                && !player.isPlayerSleeping() && !player.capabilities.isCreativeMode;
    }

    private void end(State terminal, String cause) {
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
        if (!available(player)) return Eligibility.PLAYER;
        LunarSnapshot view = lunar.snapshot();
        if (!view.isFullMoon()) return Eligibility.PHASE;
        if (view.timeOfDay >= LunarSnapshot.NIGHT_START) return Eligibility.NIGHT;
        long untilDusk = LunarSnapshot.NIGHT_START - view.timeOfDay;
        if (view.rawTime > Long.MAX_VALUE - untilDusk) return Eligibility.UNAVAILABLE;
        return view.rawTime + untilDusk <= storage.consumed ? Eligibility.CONSUMED : Eligibility.DUSK;
    }

    public static final class Spawn {
        public final String mob;
        public final double x, y, z, horizontalSquared;

        private Spawn(EntityMob entity, EntityPlayer target) {
            mob = entity instanceof EntitySkeleton ? "SKELETON" : "ZOMBIE";
            x = entity.posX;
            y = entity.posY;
            z = entity.posZ;
            double dx = x - target.posX, dz = z - target.posZ;
            horizontalSquared = dx * dx + dz * dz;
        }
    }

    public static final class Snapshot {
        public final State state;
        public final String reason, lastRejection;
        public final long boundary, lunarSession;
        public final int dimension, starts, pulses, attempts, elapsed, budget;
        public final int scheduledPulses = MAX_PULSES;
        public final String trigger;
        public final List<Spawn> spawns;

        private Snapshot(SiegeDirector director) {
            state = director.state;
            reason = director.reason;
            lastRejection = director.spawns.rejection;
            boundary = director.storage == null ? Long.MIN_VALUE : director.storage.consumed;
            lunarSession = director.lunar == null ? 0 : director.lunar.snapshot().sessionId;
            dimension = director.world.worldProvider.worldType;
            starts = director.starts;
            pulses = director.pulses;
            attempts = director.attempts;
            elapsed = director.elapsed;
            budget = director.budget;
            trigger = starts == 0 ? null : "NATURAL_FULL_MOON";
            spawns = Collections.unmodifiableList(new ArrayList<Spawn>(director.evidence));
        }
    }
}
