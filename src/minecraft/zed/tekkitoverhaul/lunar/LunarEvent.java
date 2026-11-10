package zed.tekkitoverhaul.lunar;

public final class LunarEvent {
    public enum Kind { DAY_STARTED, PHASE_CHANGED, CYCLE_COMPLETED, NIGHT_STARTED, NIGHT_ENDED, RESYNC }
    public enum Cause {
        NATURAL, TIME_SKIP, TIME_REWIND, ADMIN_COMMAND, CALENDAR_RESET,
        INITIALIZED, LOADED, DIMENSION_CHANGED, OUTSIDE_OVERWORLD, REPLAY_SUPPRESSED
    }

    public final Kind kind;
    public final Cause cause;
    public final LunarSnapshot before;
    public final LunarSnapshot after;
    public final long dayDelta;
    public final long cycleDelta;
    public final long nightStartsCrossed;
    public final long nightEndsCrossed;

    LunarEvent(Kind kind, Cause cause, LunarSnapshot before, LunarSnapshot after) {
        this.kind = kind;
        this.cause = cause;
        this.before = before;
        this.after = after;
        dayDelta = before == null ? 0 : after.logicalDay - before.logicalDay;
        cycleDelta = before == null ? 0 : after.cycleIndex - before.cycleIndex;
        nightStartsCrossed = crossed(LunarSnapshot.NIGHT_START);
        nightEndsCrossed = crossed(LunarSnapshot.NIGHT_END);
    }

    private long crossed(int boundary) {
        return before == null ? 0 : boundaryIndex(after, boundary) - boundaryIndex(before, boundary);
    }

    private static long boundaryIndex(LunarSnapshot view, int boundary) {
        long rawDay = Math.floorDiv(view.rawTime, (long) LunarSnapshot.DAY);
        return view.timeOfDay < boundary ? rawDay - 1 : rawDay;
    }
}
