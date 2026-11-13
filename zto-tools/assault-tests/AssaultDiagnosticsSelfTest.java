package zed.tekkitoverhaul.assault;

import java.util.List;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Checks wave timing and accounting, plus HUD history with real calendar events. */
public final class AssaultDiagnosticsSelfTest {
    public static void main(String[] args) {
        long dusk = 4L * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
        AssaultDiagnostics log = new AssaultDiagnostics(20);
        LunarCalendar calendar = new LunarCalendar(1, 0, 0, Long.MIN_VALUE, () -> { }, failure -> {
            throw new AssertionError(failure);
        });
        calendar.view().subscribe(log);
        calendar.rebaseline(0, dusk - 1, LunarEvent.Cause.LOADED);
        calendar.observe(dusk);
        log.assault(dusk, "Player assault started");
        AssaultWave[] waves = AssaultWave.plan(15);
        AssaultWave first = waves[0];
        AssaultWave second = waves[1];
        check(first.planned == 4 && second.planned == 5 && waves[2].planned == 6, "Incorrect baseline allocation");
        check(first.state().equals("PENDING"), "Wave must wait before starting");
        first.startedAt = AssaultWave.FIRST_START;
        check(first.state().equals("SPAWNING"), "Wave must begin spawning");
        log.beginWave(dusk + first.startedAt, first);
        for (int i = 0; i < 24; i++) first.reject("VANILLA_LIGHT");
        first.attempts = 24;
        check(first.deferred() == 4, "Failed candidates spent allocation");
        first.carriedOut = 1;
        second.carriedIn = 1;
        first.closedAt = first.startedAt + AssaultWave.WINDOW;
        first.outcome = "SPAWN_WINDOW_EXPIRED";
        check(first.state().equals("SKIPPED") && first.skipped() == 3 && second.deferred() == 6, "Carry must transfer budget");
        log.endWave(dusk + first.closedAt, first);
        check(first.nextStart(900, 0) == 550, "Cleared field should advance the next wave");
        check(first.nextStart(900, 1) == 1000, "Active mobs should preserve recovery time");
        second.startedAt = first.nextStart(900, 0);
        log.beginWave(dusk + second.startedAt, second);
        second.reject("LOCAL_MOB_CAP");
        second.closedAt = second.startedAt + AssaultWave.WINDOW;
        second.outcome = "SPAWN_WINDOW_EXPIRED";
        log.endWave(dusk + second.closedAt, second);
        for (int budget = 1; budget <= 24; budget++) {
            AssaultWave[] plan = AssaultWave.plan(budget);
            check(plan[0].planned + plan[1].planned + plan[2].planned == budget, "Plan must preserve budget");
        }

        List<String> before = log.history();
        check(before.size() == 7, "Rejections must produce one summary per wave");
        check(before.get(1).contains("Full moon window opened"), "Natural window event missing");
        check(before.get(2).contains("Player assault started"), "Assault must follow the window");
        check(before.get(4).contains("Inserted 0/4 in 24 attempts") && before.get(4).contains("24 Vanilla light"),
                "Incorrect wave summary");
        check(before.get(6).contains("1 Local mob cap") && !before.get(6).contains("24 Vanilla light"),
                "Wave rejections leaked into the next wave");
        for (int i = 0; i <= 20; i++) log.record(dusk + 500 + i, "WORLD", "Entry " + i);
        check(log.history().size() == 20 && log.history().get(0).endsWith("Entry 1"), "Oldest events were not evicted");
        check(before.size() == 7, "Published history changed");
        AssaultDiagnostics nextSave = new AssaultDiagnostics(20);
        check(nextSave.history().isEmpty(), "History leaked to a new save");
        System.out.println("Assault diagnostics checks passed");
    }

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
