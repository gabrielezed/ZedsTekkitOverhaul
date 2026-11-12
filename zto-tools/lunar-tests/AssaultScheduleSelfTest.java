package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.List;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Exercise the assault gate with actual LunarCalendar events, including resyncs. */
public final class AssaultScheduleSelfTest {
    private static final long FULL = 4L * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
    private final List<LunarEvent> events = new ArrayList<LunarEvent>();

    public static void main(String[] args) {
        new AssaultScheduleSelfTest().run();
        System.out.println("Assault schedule checks passed");
    }

    private void run() {
        LunarCalendar calendar = calendar(0, FULL - 1, Long.MIN_VALUE);
        calendar.observe(FULL);
        expect(LunarEvent.Cause.NATURAL, Long.MIN_VALUE, true);
        check(!AssaultSchedule.canStart(last(), FULL), "Consumed boundary was accepted");

        calendar.observe(FULL - 1);
        expect(LunarEvent.Cause.TIME_REWIND, FULL, false);
        calendar.observe(FULL);
        expect(LunarEvent.Cause.REPLAY_SUPPRESSED, FULL, false);
        calendar.setPhase(LunarPhase.FULL_MOON);
        expect(LunarEvent.Cause.ADMIN_COMMAND, Long.MIN_VALUE, false);

        calendar = calendar(0, FULL - 2, Long.MIN_VALUE);
        calendar.observe(FULL + 40);
        expect(LunarEvent.Cause.TIME_SKIP, Long.MIN_VALUE, false);
        calendar = calendar(0, FULL + 40, FULL);
        expect(LunarEvent.Cause.LOADED, FULL, false);
        int count = events.size();
        calendar.observe(FULL + 41);
        check(events.size() == count, "Reload replayed a night event");
        calendar.rebaseline(-1, FULL - 1, LunarEvent.Cause.DIMENSION_CHANGED);
        expect(LunarEvent.Cause.DIMENSION_CHANGED, FULL, false);
        calendar.observe(FULL);
        expect(LunarEvent.Cause.OUTSIDE_OVERWORLD, Long.MIN_VALUE, false);

        long ordinary = FULL + LunarSnapshot.DAY;
        calendar = calendar(0, ordinary - 1, FULL);
        calendar.observe(ordinary);
        expect(LunarEvent.Cause.NATURAL, FULL, false);
        long nextFull = FULL + 8L * LunarSnapshot.DAY;
        calendar = calendar(0, nextFull - 1, FULL);
        calendar.observe(nextFull);
        expect(LunarEvent.Cause.NATURAL, FULL, true);
    }

    private LunarCalendar calendar(int dimension, long rawTime, long watermark) {
        LunarCalendar calendar = new LunarCalendar(1, 0, 0, watermark, () -> { }, failure -> {
            throw new AssertionError(failure);
        });
        calendar.view().subscribe(events::add);
        calendar.rebaseline(dimension, rawTime, LunarEvent.Cause.LOADED);
        return calendar;
    }

    private LunarEvent last() { return events.get(events.size() - 1); }

    private void expect(LunarEvent.Cause cause, long consumed, boolean accepted) {
        check(last().cause == cause, "Expected lunar cause " + cause + ", got " + last().cause);
        check(AssaultSchedule.canStart(last(), consumed) == accepted, "Unexpected eligibility after " + cause);
    }

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
