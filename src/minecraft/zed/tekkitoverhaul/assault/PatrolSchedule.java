package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class PatrolSchedule {
    static final int SPAWN_DELAY = 800;
    static final int RETRY_INTERVAL = 20;
    static final int SPAWN_WINDOW = 240;
    static final int CANDIDATES_PER_RETRY = 4;

    private PatrolSchedule() { }

    static int budget(LunarPhase phase) {
        switch (phase) {
            case FIRST_QUARTER: return 2;
            case WAXING_GIBBOUS: return 3;
            default: return 0;
        }
    }

    static boolean eligibleNight(LunarSnapshot moon) {
        return moon != null && moon.schedulingEligible && moon.night && budget(moon.phase) > 0;
    }

    static boolean canStart(LunarEvent event) {
        return event.kind == LunarEvent.Kind.NIGHT_STARTED && event.cause == LunarEvent.Cause.NATURAL
                && eligibleNight(event.after);
    }

    static int advance(int elapsed, boolean paused) {
        return paused ? elapsed : elapsed + 1;
    }

    static boolean expired(int elapsed) {
        return elapsed >= SPAWN_DELAY + SPAWN_WINDOW;
    }

    static boolean retryDue(int elapsed) {
        return elapsed >= SPAWN_DELAY && !expired(elapsed)
                && (elapsed - SPAWN_DELAY) % RETRY_INTERVAL == 0;
    }
}
