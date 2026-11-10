package zed.tekkitoverhaul.lunar;

/** A half-open time window. A null phase accepts all phases. */
public final class LunarEventWindow {
    public final int start;
    public final int end;
    public final LunarPhase phase;

    public LunarEventWindow(int start, int end, LunarPhase phase) {
        if (start < 0 || start >= LunarSnapshot.DAY || end < 0 || end > LunarSnapshot.DAY || start == end) {
            throw new IllegalArgumentException("Invalid lunar window bounds");
        }
        this.start = start;
        this.end = end;
        this.phase = phase;
    }

    public boolean matches(LunarSnapshot view) {
        if (view == null || !view.schedulingEligible || (phase != null && view.phase != phase)) return false;
        int time = view.timeOfDay;
        return start < end ? time >= start && time < end : time >= start || time < end;
    }
}
