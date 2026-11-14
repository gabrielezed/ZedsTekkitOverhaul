import codechicken.nei.NEIUtils;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarService;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.mod_ZedsTekkitOverhaul;

/** Runs on the game thread in the alpha.6 runner's marked disposable world. */
final class AssaultRecurrenceProbe implements LunarService.Listener {
    private static final long CYCLE = 8L * LunarSnapshot.DAY;
    private static final int LEAD = 160;
    private final LocalLunarCalendar lunar;
    private final Method eligibility;
    private LunarService service;
    private LunarEvent lastEvent;
    private xd overworld;
    private long boundary, expected, jump;
    private int stage, completed;

    AssaultRecurrenceProbe(LocalLunarCalendar lunar) throws ReflectiveOperationException {
        this.lunar = lunar;
        eligibility = LocalLunarCalendar.class.getDeclaredMethod("assaultEligibility", xd.class);
        eligibility.setAccessible(true);
    }

    void trace(Minecraft game, Snapshot view) throws ReflectiveOperationException {
        LunarSnapshot now = service == null ? null : service.snapshot();
        if (now == null) return;
        System.out.println("ALPHA6 recurrence stage=" + stage + " raw=" + now.rawTime + " phase=" + now.phase
                + " event=" + (lastEvent == null ? "none" : lastEvent.kind + "/" + lastEvent.cause)
                + " consumed=" + view.boundary + " starts=" + view.starts + " state=" + view.state
                + " reason=" + view.reason + " eligibility=" + eligibility.invoke(lunar, game.f)
                + " waves=" + view.currentWave + " spawns=" + view.spawns.size()
                + " placement=" + view.lastRejection);
    }

    public void onLunarEvent(LunarEvent event) { lastEvent = event; }

    boolean tick(Minecraft game, Snapshot view) throws Exception {
        LunarService current = mod_ZedsTekkitOverhaul.lunarService(game.f);
        if (current != service) {
            check(service == null || service.snapshot() == null, "old save session detached");
            service = current;
            service.subscribe(this);
            lastEvent = null;
        }
        LunarSnapshot now = service.snapshot();
        switch (stage) {
            case 0:
                if (now.timeOfDay < 10820) return false;
                check(now.isFullMoon() && eligibility.invoke(lunar, game.f) == SiegeDirector.Eligibility.DUSK,
                        "initial full moon eligible");
                expected = now.rawTime - now.timeOfDay + LunarSnapshot.NIGHT_START;
                stage = 1;
                return false;
            case 1:
                if (now.rawTime < expected) return false;
                check(view.starts == completed + 1 && view.state == SiegeDirector.State.ACTIVE
                        && view.boundary == expected && expected > boundary && now.isFullMoon(), "fresh full-moon admission");
                event(LunarEvent.Kind.NIGHT_STARTED, LunarEvent.Cause.NATURAL, expected);
                check(view.waves.size() == 3 && ((SiegeDirector.WaveSnapshot) view.waves.get(0)).planned == 4,
                        "wave schedule initialized at admission");
                trace(game, view);
                boundary = view.boundary;
                stage = 2;
                return false;
            case 2:
                if (view.state == SiegeDirector.State.ACTIVE) {
                    Alpha6RuntimeProbe.clearHostiles(game);
                    return false;
                }
                check(view.state == SiegeDirector.State.RESOLVED && "WAVES_COMPLETE".equals(view.reason)
                        && view.currentWave == 3 && !view.spawns.isEmpty(), "waves placed and resolved");
                trace(game, view);
                completed++;
                if (completed == 1) {
                    game.f.a(boundary - LEAD);
                    stage = 3;
                } else if (completed == 2) {
                    prepareNext(game);
                } else {
                    Alpha6RuntimeProbe.saveQuit(game);
                    check(game.f == null && game.s instanceof xt, "native save and quit reached title");
                    Alpha6RuntimeProbe.open(game);
                    stage = 4;
                }
                return false;
            case 3:
                if (now.rawTime < boundary) return false;
                event(LunarEvent.Kind.RESYNC, LunarEvent.Cause.REPLAY_SUPPRESSED, boundary);
                check(view.starts == 1 && view.boundary == boundary, "rewind did not duplicate consumed dusk");
                trace(game, view);
                prepareNext(game);
                return false;
            case 4:
                check(now.night && view.starts == 0 && view.boundary == boundary
                        && "NIGHT_CONSUMED".equals(view.reason), "save/reopen preserved completed boundary");
                trace(game, view);
                // Reproduce the installed NEI local absolute clock write across the next full-moon dusk.
                jump = boundary + CYCLE + 1000;
                NEIUtils.setTime(jump);
                stage = 5;
                return false;
            case 5:
                if (now.rawTime < jump) return false;
                check(lastEvent != null && lastEvent.kind == LunarEvent.Kind.RESYNC
                        && lastEvent.cause == LunarEvent.Cause.TIME_SKIP && lastEvent.nightStartsCrossed > 0
                        && now.isFullMoon() && now.night && view.starts == 0 && view.boundary == boundary,
                        "NEI skip across dusk left encounter suppressed");
                trace(game, view);
                expected = jump - Math.floorMod(jump, (long) LunarSnapshot.DAY)
                        + LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
                game.f.a(expected - LEAD);
                stage = 6;
                return false;
            case 6:
                if (now.rawTime < expected - LEAD) return false;
                check(!now.isFullMoon(), "phase alignment fixture starts before a non-full-moon dusk");
                check(lunar.alignBeforeNight(game.f, LunarPhase.FULL_MOON, LunarSnapshot.NIGHT_START - 30),
                        "calendar debug alignment accepted");
                check(service.snapshot().rawTime == now.rawTime && game.f.w() == now.rawTime && view.starts == 0,
                        "phase alignment left vanilla clock and starts unchanged");
                event(LunarEvent.Kind.RESYNC, LunarEvent.Cause.ADMIN_COMMAND, now.rawTime);
                trace(game, view);
                stage = 7;
                return false;
            case 7:
                if (now.rawTime < expected) return false;
                event(LunarEvent.Kind.NIGHT_STARTED, LunarEvent.Cause.NATURAL, expected);
                check(view.starts == 1 && view.boundary == expected && view.state == SiegeDirector.State.ACTIVE,
                        "natural aligned dusk started after reopen");
                trace(game, view);
                boundary = view.boundary;
                Alpha6RuntimeProbe.saveQuit(game);
                Alpha6RuntimeProbe.open(game);
                stage = 8;
                return false;
            case 8:
                check(view.starts == 0 && view.boundary == boundary && view.state == SiegeDirector.State.ABORTED
                        && "INTERRUPTED_ON_REOPEN".equals(view.reason), "interrupted boundary persisted without replay");
                trace(game, view);
                expected = boundary + CYCLE;
                game.f.a(expected - LEAD);
                game.A.C = game.f.q = 0;
                stage = 9;
                return false;
            case 9:
                if (now.rawTime < expected) return false;
                event(LunarEvent.Kind.NIGHT_STARTED, LunarEvent.Cause.NATURAL, expected);
                check(view.starts == 0 && view.boundary == boundary && "PEACEFUL".equals(view.reason),
                        "Peaceful dusk did not consume an assault");
                trace(game, view);
                expected += CYCLE;
                game.f.a(expected - LEAD);
                game.A.C = game.f.q = 2;
                game.h.aT.d = true;
                stage = 10;
                return false;
            case 10:
                if (now.rawTime < expected) return false;
                event(LunarEvent.Kind.NIGHT_STARTED, LunarEvent.Cause.NATURAL, expected);
                check(view.starts == 0 && view.boundary == boundary && "PLAYER_UNAVAILABLE".equals(view.reason),
                        "non-Survival player did not consume an assault");
                trace(game, view);
                expected += CYCLE;
                game.f.a(expected - LEAD);
                game.h.aT.d = false;
                stage = 11;
                return false;
            case 11:
                if (now.rawTime < expected) return false;
                event(LunarEvent.Kind.NIGHT_STARTED, LunarEvent.Cause.NATURAL, expected);
                check(view.starts == 1 && view.boundary == expected && view.state == SiegeDirector.State.ACTIVE,
                        "a later eligible dusk can start from ABORTED");
                trace(game, view);
                overworld = game.f;
                game.a(-1);
                stage = 12;
                return false;
            case 12:
                check(now.dimension == -1 && view.starts == 0
                        && lunar.assaultSnapshot(overworld) == null, "dimension change detached Overworld director");
                event(LunarEvent.Kind.RESYNC, LunarEvent.Cause.DIMENSION_CHANGED, now.rawTime);
                trace(game, view);
                game.a(0);
                stage = 13;
                return false;
            case 13:
                check(now.dimension == 0 && view.starts == 0 && view.boundary == expected
                        && view.state == SiegeDirector.State.ABORTED, "dimension return preserved interrupted boundary");
                trace(game, view);
                return true;
            default: throw new AssertionError("Unknown recurrence stage " + stage);
        }
    }

    private void prepareNext(Minecraft game) {
        expected = boundary + CYCLE;
        game.f.a(expected - LEAD);
        stage = 1;
    }

    private void event(LunarEvent.Kind kind, LunarEvent.Cause cause, long at) {
        check(lastEvent != null && lastEvent.kind == kind && lastEvent.cause == cause
                && lastEvent.after.rawTime == at, "event " + kind + "/" + cause + " at " + at);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
