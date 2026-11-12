package zed.tekkitoverhaul.assault;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityMob;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.assault.SiegeDirector.State;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** A single dusk patrol in the local player's vicinity. */
final class LunarPatrol {
    final boolean enabled;
    private final SpawnController spawns = new SpawnController(16);
    final EncounterLoadouts loadouts = new EncounterLoadouts();
    final Map<String, Integer> rejections = new LinkedHashMap<String, Integer>();
    private EntityPlayer target;
    private int difficulty;
    State state = State.IDLE;
    String reason;
    int budget, spawned, attempts, elapsed;
    boolean paused;

    LunarPatrol(boolean enabled) {
        this.enabled = enabled;
        reset();
    }

    void reset() {
        state = State.IDLE;
        reason = enabled ? "WAITING_FOR_NATURAL_DUSK" : "DISABLED";
        target = null;
        budget = 0;
        loadouts.reset(LunarPhase.NEW_MOON);
        clearProgress();
    }

    private void clearProgress() {
        spawned = attempts = elapsed = 0;
        paused = false;
        rejections.clear();
    }

    void onEvent(LunarEvent event, AssaultDiagnostics diagnostics) {
        if (event.kind == LunarEvent.Kind.RESYNC || event.kind == LunarEvent.Kind.NIGHT_ENDED) {
            if (state == State.ACTIVE) {
                finish(event.after.rawTime, State.ABORTED,
                        event.kind == LunarEvent.Kind.NIGHT_ENDED ? "NIGHT_ENDED" : event.cause.name(),
                        diagnostics);
            }
        } else if (PatrolSchedule.canStart(event) && state != State.ACTIVE) {
            state = State.ACTIVE;
            reason = "NATURAL_DUSK";
            budget = PatrolSchedule.budget(event.after.phase);
            loadouts.reset(event.after.phase);
            clearProgress();
        }
    }

    void tick(World world, LunarSnapshot moon, EntityPlayer player, boolean paused, AssaultDiagnostics diagnostics) {
        if (state != State.ACTIVE) return;
        if (!SiegeDirector.available(world, player) || (target != null && player != target)) {
            finish(moon.rawTime, State.ABORTED, "PLAYER_UNAVAILABLE", diagnostics);
            return;
        }
        if (world.difficultySetting <= 0 || (target != null && world.difficultySetting != difficulty)) {
            finish(moon.rawTime, State.ABORTED, world.difficultySetting <= 0 ? "PEACEFUL" : "DIFFICULTY_CHANGED",
                    diagnostics);
            return;
        }
        this.paused = paused;
        if (target == null) {
            target = player;
            difficulty = world.difficultySetting;
            return;
        }
        elapsed = PatrolSchedule.advance(elapsed, paused);
        if (paused) return;
        if (PatrolSchedule.expired(elapsed)) {
            finish(moon.rawTime, State.RESOLVED, "SPAWN_WINDOW_EXPIRED", diagnostics);
        } else if (PatrolSchedule.retryDue(elapsed)) {
            spawn(world, moon, player, diagnostics);
        }
    }

    void interrupt(long time, String cause, AssaultDiagnostics diagnostics) {
        if (state == State.ACTIVE) finish(time, State.ABORTED, cause, diagnostics);
    }

    private void spawn(World world, LunarSnapshot moon, EntityPlayer player, AssaultDiagnostics diagnostics) {
        int localHostiles = spawns.localHostiles(world, player);
        for (int tried = 0; tried < PatrolSchedule.CANDIDATES_PER_RETRY
                && spawned < budget && localHostiles < SpawnController.LOCAL_CAP; tried++) {
            attempts++;
            // Vanilla Hard zombies can break wooden doors.
            EntityMob mob = spawns.trySpawn(world, player, difficulty == 3 || spawned % 2 == 1);
            if (mob != null) {
                loadouts.apply(mob, 0, 0);
                spawned++;
                localHostiles++;
            } else {
                String rejection = spawns.rejection;
                Integer count = rejections.get(rejection);
                rejections.put(rejection, count == null ? 1 : count + 1);
            }
        }
        if (spawned == budget) {
            finish(moon.rawTime, State.RESOLVED, "COMPLETED", diagnostics);
        } else if (localHostiles >= SpawnController.LOCAL_CAP) {
            finish(moon.rawTime, State.RESOLVED, "LOCAL_MOB_CAP", diagnostics);
        }
    }

    private void finish(long time, State terminal, String cause, AssaultDiagnostics diagnostics) {
        state = terminal;
        reason = cause;
        target = null;
        paused = false;
        String message = terminal + " | " + cause + " | Spawned " + spawned + "/" + budget
                + " | Attempts " + attempts + " | Skipped " + (budget - spawned) + " | Elapsed " + elapsed
                + (rejections.isEmpty() ? "" : " | " + AssaultDiagnostics.counts(rejections));
        ModLoader.getLogger().info("[ZTO] PATROL [" + time + "] " + message);
        if (diagnostics != null) diagnostics.record(time, "PATROL",
                message + " | " + new SiegeDirector.LoadoutSnapshot(loadouts, spawned).summary());
    }
}
