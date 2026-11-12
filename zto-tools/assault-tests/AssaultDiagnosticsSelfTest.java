package zed.tekkitoverhaul.assault;

import java.util.List;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Checks the HUD recorder against real calendar events and aggregated pulse outcomes. */
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
        log.beginPulse(dusk + 100, 1);
        for (int i = 0; i < 24; i++) log.reject("LIGHT_OR_COLLISION");
        log.endPulse(dusk + 100, 1, 0, 24);
        log.beginPulse(dusk + 260, 2);
        log.reject("LOCAL_MOB_CAP");
        log.endPulse(dusk + 260, 2, 0, 0);

        List<String> before = log.history();
        check(before.size() == 7, "Rejections must produce one summary per pulse");
        check(before.get(1).contains("Full moon window opened"), "Natural window event missing");
        check(before.get(2).contains("Player assault started"), "Assault must follow the window");
        check(before.get(4).contains("+0 assault mobs in 24 attempts / 24 Light or collision"), "Incorrect pulse summary");
        check(!before.get(6).contains("Light or collision"), "Pulse rejections leaked into the next pulse");
        check(log.rejectionSummary().equals("24 Light or collision, 1 Local mob cap"), "Incorrect encounter totals");
        for (int i = 0; i <= 20; i++) log.record(dusk + 500 + i, "WORLD", "Entry " + i);
        check(log.history().size() == 20 && log.history().get(0).endsWith("Entry 1"), "Oldest events were not evicted");
        check(before.size() == 7, "Published history changed");
        log.resetEncounter();
        check(log.rejectionSummary().equals("None") && log.history().size() == 20, "Encounter reset cleared the history");
        AssaultDiagnostics nextSave = new AssaultDiagnostics(20);
        check(nextSave.history().isEmpty(), "History leaked to a new save");
        System.out.println("Assault diagnostics checks passed");
    }

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
