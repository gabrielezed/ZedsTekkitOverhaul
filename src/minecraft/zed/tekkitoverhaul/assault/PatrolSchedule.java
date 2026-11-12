package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class PatrolSchedule {
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
}
