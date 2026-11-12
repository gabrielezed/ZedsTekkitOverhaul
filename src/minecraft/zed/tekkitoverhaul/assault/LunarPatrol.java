package zed.tekkitoverhaul.assault;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.assault.SiegeDirector.State;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** A single dusk patrol in the local player's vicinity. */
final class LunarPatrol {
    private static final int SPAWN_DELAY = 240;
    private static final int MAX_ATTEMPTS = 24;
    final boolean enabled;
    private final SpawnController spawns = new SpawnController(16);
    private EntityPlayer target;
    private int difficulty, elapsed;
    State state = State.IDLE;
    String reason;
    int budget, spawned;

    LunarPatrol(boolean enabled) {
        this.enabled = enabled;
        reset();
    }

    void reset() {
        state = State.IDLE;
        reason = enabled ? "WAITING_FOR_NATURAL_DUSK" : "DISABLED";
        target = null;
        budget = spawned = 0;
    }

    void onEvent(LunarEvent event, AssaultDiagnostics diagnostics) {
        if (event.kind == LunarEvent.Kind.RESYNC || event.kind == LunarEvent.Kind.NIGHT_ENDED) {
            if (state == State.ACTIVE) {
                finish(event.after.rawTime, State.ABORTED,
                        event.kind == LunarEvent.Kind.NIGHT_ENDED ? "NIGHT_ENDED" : event.cause.name(),
                        diagnostics, "");
            }
        } else if (PatrolSchedule.canStart(event) && state != State.ACTIVE) {
            state = State.ACTIVE;
            reason = "NATURAL_DUSK";
            budget = PatrolSchedule.budget(event.after.phase);
            spawned = elapsed = 0;
        }
    }

    void tick(World world, LunarSnapshot moon, EntityPlayer player, boolean paused, AssaultDiagnostics diagnostics) {
        if (state != State.ACTIVE) return;
        if (paused || !SiegeDirector.available(world, player) || (target != null && player != target)) {
            finish(moon.rawTime, State.ABORTED, paused ? "PAUSED" : "PLAYER_UNAVAILABLE", diagnostics, "");
        } else if (world.difficultySetting <= 0 || (target != null && world.difficultySetting != difficulty)) {
            finish(moon.rawTime, State.ABORTED, world.difficultySetting <= 0 ? "PEACEFUL" : "DIFFICULTY_CHANGED",
                    diagnostics, "");
        } else if (target == null) {
            target = player;
            difficulty = world.difficultySetting;
        } else if (++elapsed >= SPAWN_DELAY) {
            spawn(world, moon, player, diagnostics);
        }
    }

    private void spawn(World world, LunarSnapshot moon, EntityPlayer player, AssaultDiagnostics diagnostics) {
        int localHostiles = spawns.localHostiles(world, player);
        int attempts = 0;
        Map<String, Integer> rejected = new LinkedHashMap<String, Integer>();
        while (attempts < MAX_ATTEMPTS && spawned < budget && localHostiles < SpawnController.LOCAL_CAP) {
            attempts++;
            // Vanilla Hard zombies can break wooden doors.
            if (spawns.trySpawn(world, player, difficulty == 3 || spawned % 2 == 1) != null) {
                spawned++;
                localHostiles++;
            } else {
                String rejection = spawns.rejection;
                Integer count = rejected.get(rejection);
                rejected.put(rejection, count == null ? 1 : count + 1);
            }
        }
        String outcome = spawned == budget ? "COMPLETED"
                : localHostiles >= SpawnController.LOCAL_CAP ? "LOCAL_MOB_CAP" : "ATTEMPT_LIMIT";
        finish(moon.rawTime, State.RESOLVED, outcome, diagnostics,
                " | Attempts " + attempts + (rejected.isEmpty() ? "" : " | " + AssaultDiagnostics.counts(rejected)));
    }

    private void finish(long time, State terminal, String cause, AssaultDiagnostics diagnostics, String details) {
        state = terminal;
        reason = cause;
        target = null;
        String message = terminal + " | " + cause + " | Spawned " + spawned + "/" + budget + details;
        ModLoader.getLogger().info("[ZTO] PATROL [" + time + "] " + message);
        if (diagnostics != null) diagnostics.record(time, "PATROL", message);
    }
}
