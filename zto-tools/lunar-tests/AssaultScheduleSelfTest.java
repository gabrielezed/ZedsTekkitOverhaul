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
        AssaultScheduleSelfTest test = new AssaultScheduleSelfTest();
        test.run();
        test.patrols();
        patrolRetryWindow();
        System.out.println("Assault and patrol schedule checks passed");
    }

    private static void patrolRetryWindow() {
        int elapsed = 0;
        int retries = 0;
        while (!PatrolSchedule.expired(elapsed)) {
            if (elapsed == 9 || PatrolSchedule.retryDue(elapsed)) {
                int beforePause = elapsed;
                for (int pausedTick = 0; pausedTick < 50; pausedTick++) {
                    elapsed = PatrolSchedule.advance(elapsed, true);
                }
                check(elapsed == beforePause, "Pause advanced the patrol clock");
            }
            elapsed = PatrolSchedule.advance(elapsed, false);
            if (PatrolSchedule.retryDue(elapsed)) {
                check(elapsed == 800 + retries * 20, "Unexpected patrol retry tick");
                retries++;
            }
        }
        check(elapsed == 1040, "Patrol window expired at the wrong tick");
        check(retries == 12 && retries * PatrolSchedule.CANDIDATES_PER_RETRY == 48,
                "Unexpected patrol retry budget");
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

    private void patrols() {
        int[] budgets = {0, 0, 2, 3, 0, 0, 0, 0};
        for (int day = 0; day < budgets.length; day++) {
            long dusk = (long) day * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
            LunarCalendar calendar = calendar(0, dusk - 1, Long.MIN_VALUE);
            calendar.observe(dusk);
            check(PatrolSchedule.budget(last().after.phase) == budgets[day], "Patrol budget on day " + day);
            check(PatrolSchedule.canStart(last()) == (budgets[day] > 0), "Patrol gate on day " + day);
            check(AssaultSchedule.canStart(last(), Long.MIN_VALUE) == (day == 4), "Assault gate on day " + day);
        }

        long dusk = 2L * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
        LunarCalendar calendar = calendar(0, dusk - 1, Long.MIN_VALUE);
        calendar.observe(dusk);
        check(PatrolSchedule.canStart(last()), "Natural first quarter did not open");
        long savedWatermark = calendar.watermark();
        calendar = calendar(0, dusk + 100, savedWatermark);
        check(!PatrolSchedule.canStart(last()), "Reload triggered a patrol");
        calendar.observe(dusk - 1);
        calendar.observe(dusk);
        check(last().cause == LunarEvent.Cause.REPLAY_SUPPRESSED && !PatrolSchedule.canStart(last()),
                "Rewind replayed a saved patrol boundary");
        calendar.setPhase(LunarPhase.WAXING_GIBBOUS);
        check(!PatrolSchedule.canStart(last()), "Administrative alignment triggered a patrol");

        calendar = calendar(0, dusk - 2, Long.MIN_VALUE);
        calendar.observe(dusk);
        check(!PatrolSchedule.canStart(last()), "Time jump to dusk triggered a patrol");
        calendar = calendar(0, dusk - 1, Long.MIN_VALUE);
        calendar.observe(3L * LunarSnapshot.DAY);
        check(!PatrolSchedule.canStart(last()), "Sleep-like jump across the night triggered a patrol");
        calendar = calendar(-1, dusk - 1, Long.MIN_VALUE);
        calendar.observe(dusk);
        check(!PatrolSchedule.canStart(last()), "A patrol started outside the Overworld");
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
