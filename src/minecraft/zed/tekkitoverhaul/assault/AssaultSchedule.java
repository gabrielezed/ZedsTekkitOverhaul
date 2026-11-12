package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarEventWindow;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class AssaultSchedule {
    static final LunarEventWindow WINDOW = new LunarEventWindow(
            LunarSnapshot.NIGHT_START, LunarSnapshot.NIGHT_END, LunarPhase.FULL_MOON);

    private AssaultSchedule() { }

    static boolean canStart(LunarEvent event, long consumedBoundary) {
        return event.kind == LunarEvent.Kind.NIGHT_STARTED && event.cause == LunarEvent.Cause.NATURAL
                && WINDOW.matches(event.after) && event.after.rawTime > consumedBoundary;
    }
}
