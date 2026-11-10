import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zed.tekkitoverhaul.lunar.*;

public final class LunarCalendarSelfTest {
    private static final long DAY = LunarSnapshot.DAY;
    private final List<LunarEvent> events = new ArrayList<LunarEvent>();
    private int writes;
    private int failures;
    private final LunarCalendar calendar = new LunarCalendar(1, 0, 0, Long.MIN_VALUE,
            () -> writes++, failure -> failures++);

    public static void main(String[] args) throws Exception {
        new LunarCalendarSelfTest().run();
        System.out.println("Lunar calendar checks passed");
    }

    private void run() throws Exception {
        LunarService service = calendar.view();
        LunarService.Listener collector = events::add;
        service.subscribe(collector);
        service.subscribe(collector);
        calendar.rebaseline(0, 0, LunarEvent.Cause.INITIALIZED);
        check(service.snapshot().isNewMoon(), "New world alignment");
        check(service.snapshot().daysUntil(LunarPhase.FULL_MOON) == 4, "Full moon countdown");
        check(service.snapshot().ticksToNextPhase == DAY, "Tick countdown");
        check(events.size() == 1, "Subscription is idempotent");

        calendar.observe(4 * DAY + 12999);
        check(last().cause == LunarEvent.Cause.TIME_SKIP && last().nightStartsCrossed == 4, "Bounded forward resync");
        check(service.snapshot().isFullMoon(), "Full moon on day four");
        int saved = writes;
        LunarSnapshot before = service.snapshot();
        LunarEvent preview = calendar.preview(DAY);
        check(preview.cause == LunarEvent.Cause.TIME_SKIP && preview.after.phase == LunarPhase.WANING_GIBBOUS,
                "Detached forward preview");
        check(service.snapshot() == before && writes == saved && events.size() == 2, "Preview is detached");
        LunarEventWindow full = new LunarEventWindow(13000, 23000, LunarPhase.FULL_MOON);
        calendar.observe(4 * DAY + 13000);
        check(last().kind == LunarEvent.Kind.NIGHT_STARTED && full.matches(service.snapshot()), "Full moon night");
        calendar.observe(4 * DAY + 13000);
        check(events.size() == 3, "Repeated tick");
        calendar.observe(4 * DAY + 12999);
        check(last().cause == LunarEvent.Cause.TIME_REWIND, "Rewind classification");
        calendar.observe(4 * DAY + 13000);
        check(last().cause == LunarEvent.Cause.REPLAY_SUPPRESSED, "Replay guard");
        calendar.observe(4 * DAY + 22999);
        calendar.observe(4 * DAY + 23000);
        check(last().kind == LunarEvent.Kind.NIGHT_ENDED && !full.matches(service.snapshot()), "Exclusive night end");

        calendar.observe(8 * DAY - 1);
        events.clear();
        LunarService.Listener reentrant = event -> calendar.advance(1);
        service.subscribe(reentrant);
        service.subscribe(event -> check(service.snapshot() == event.after, "State published before callbacks"));
        calendar.observe(8 * DAY);
        check(Arrays.equals(events.stream().map(e -> e.kind).toArray(), new Object[] {
                LunarEvent.Kind.DAY_STARTED, LunarEvent.Kind.PHASE_CHANGED, LunarEvent.Kind.CYCLE_COMPLETED
        }), "Dawn event order and cycle wrap");
        check(failures == 3, "Listener failure isolation and reentrant rejection");
        service.unsubscribe(reentrant);
        check(service.snapshot().completedCycles == 1, "Completed cycles");

        LunarEventWindow midnight = new LunarEventWindow(23000, 1000, null);
        check(midnight.matches(service.snapshot()), "Window wraps at midnight");
        calendar.observe(8 * DAY + 1000);
        check(!midnight.matches(service.snapshot()), "Exclusive wrapped endpoint");
        calendar.rebaseline(-1, 8 * DAY + 12999, LunarEvent.Cause.DIMENSION_CHANGED);
        calendar.observe(8 * DAY + 13000);
        check(last().cause == LunarEvent.Cause.OUTSIDE_OVERWORLD && !full.matches(service.snapshot()),
                "Off-dimension suppression");
        calendar.setPhase(LunarPhase.FULL_MOON);
        check(service.snapshot().logicalDay == 12 && service.snapshot().rawTime == 8 * DAY + 13000, "Set phase");
        calendar.advance(8);
        check(service.snapshot().logicalDay == 20 && last().cause == LunarEvent.Cause.ADMIN_COMMAND, "Advance cycle");
        calendar.reset();
        check(service.snapshot().logicalDay == 8 && last().cause == LunarEvent.Cause.CALENDAR_RESET, "Reset");
        service.unsubscribe(collector);
        events.clear();
        calendar.observe(-1);
        check(events.isEmpty() && service.snapshot().logicalDay == -1
                && service.snapshot().timeOfDay == 23999, "Unsubscribe and signed time");
        calendar.rebaseline(0, Long.MAX_VALUE, LunarEvent.Cause.DIMENSION_CHANGED);
        expectFailure(() -> calendar.preview(1), ArithmeticException.class);
        expectFailure(() -> calendar.advance(9), IllegalArgumentException.class);
        expectFailure(() -> new LunarEventWindow(1, 1, null), IllegalArgumentException.class);
        calendar.detach();
        check(service.snapshot() == null, "Detached view");

        for (int day = 0; day < 8; day++) {
            LunarCalendar legacy = new LunarCalendar(day, 4, 4, day * DAY + 13000, () -> {}, failure -> {});
            legacy.rebaseline(0, day * DAY, LunarEvent.Cause.LOADED);
            check((legacy.view().snapshot().phase.id + 4) % 8 == day, "Legacy atlas alignment");
            legacy.view().subscribe(collector);
            legacy.observe(day * DAY + 12999);
            legacy.observe(day * DAY + 13000);
            check(last().cause == LunarEvent.Cause.REPLAY_SUPPRESSED, "Persisted watermark on older clock");
        }
    }

    private LunarEvent last() { return events.get(events.size() - 1); }

    private static void expectFailure(Runnable action, Class<? extends RuntimeException> type) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            if (type.isInstance(failure)) return;
            throw failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
