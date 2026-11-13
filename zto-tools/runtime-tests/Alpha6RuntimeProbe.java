import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.mod_ZedsTekkitOverhaul;

/** Exercises the packaged classes on the game thread in a marked copy of the pack. */
public final class Alpha6RuntimeProbe implements ITickHandler {
    private static final int X = 34, Y = 121, Z = 215;
    private static String save, mode;
    private final boolean bootstrap;
    private boolean done;
    private int stage, frozen;
    private long pauseUntil, reopenAt;
    private int cleared;
    private LocalLunarCalendar lunar;

    private Alpha6RuntimeProbe(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    public static void main(String[] args) throws Exception {
        File game = new File(args[0]).getCanonicalFile();
        save = args[1];
        mode = args[2];
        if (!"survival-hud-runtime".equals(game.getParentFile().getParentFile().getName())
                || !save.matches("ZTO-Test-[a-f0-9]{32}")
                || !new File(game, "saves/" + save + "/.zto-disposable-test").isFile()) {
            throw new IllegalArgumentException("Use a marked save in the isolated Tekkit Dev copy");
        }
        field(Minecraft.class, "aj").set(null, game);
        FMLCommonHandler.instance().registerTickHandler(new Alpha6RuntimeProbe(true));
        Minecraft.main(new String[] {"ZTOTest"});
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    public String getLabel() { return "Alpha 6 runtime acceptance"; }
    public void tickStart(EnumSet<TickType> ticks, Object... data) { }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        if (game == null) return;
        try {
            if (bootstrap) {
                if (!(game.s instanceof xt)) return;
                FMLCommonHandler.instance().registerTickHandler(new Alpha6RuntimeProbe(false));
                done = true;
                return;
            }
            if (stage == 0) {
                check(!mod_ZedsTekkitOverhaul.survivalDebugHud && mod_ZedsTekkitOverhaul.lunarDebugKey == 0
                        && !mod_ZedsTekkitOverhaul.lunarVerboseLogging
                        && !Boolean.getBoolean("zto.devBridge.enabled"), "diagnostics disabled");
                check(mod_ZedsTekkitOverhaul.lunarOutdoorLightPressure != "disabled".equals(mode), "loaded configuration");
                System.out.println("ALPHA6 JAR " + mod_ZedsTekkitOverhaul.class.getProtectionDomain()
                        .getCodeSource().getLocation() + " Java " + System.getProperty("java.version"));
                lunar = (LocalLunarCalendar) field(mod_ZedsTekkitOverhaul.class, "LUNAR").get(null);
                open(game);
                arena(game);
                stage++;
                return;
            }
            Snapshot view = lunar.assaultSnapshot(game.f);
            if (view == null) return;
            if (stage == 1) {
                if (game.f.w() < 10820) return;
                if ("first".equals(mode) || "disabled".equals(mode)) geometry(game);
                game.f.a(12980L);
                stage++;
                return;
            }
            if (stage == 3) {
                check(view.patrol.elapsed == frozen && view.patrol.attempts == 0, "paused patrol is frozen");
                if (System.nanoTime() >= pauseUntil) {
                    game.a((vp) null);
                    stage = 4;
                }
                return;
            }
            if (stage == 5) {
                check(view.starts == 0 && view.spawns.isEmpty() && view.patrol.attempts == 0, "same night replay");
                if ("full".equals(mode)) check("INTERRUPTED_ON_REOPEN".equals(view.reason), "interrupted assault state");
                if ("waves".equals(mode)) check("NIGHT_CONSUMED".equals(view.reason), "resolved assault state");
                if (game.f.w() >= reopenAt + 100) {
                    saveQuit(game);
                    System.out.println("ALPHA6 PASS " + mode);
                    done = true;
                    game.f();
                }
                return;
            }
            if ("first".equals(mode) && stage == 2 && view.patrol.elapsed >= 10) {
                frozen = view.patrol.elapsed;
                pauseUntil = System.nanoTime() + 1500000000L;
                game.i();
                stage++;
                return;
            }
            if (!verifyEncounter(game, view)) return;
            saveQuit(game);
            check(game.f == null && game.s instanceof xt, "Save and Quit returned to title");
            open(game);
            reopenAt = game.f.w();
            stage = 5;
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean verifyEncounter(Minecraft game, Snapshot view) {
        if ("full".equals(mode) || "waves".equals(mode)) {
            if (view.spawns.isEmpty()) return false;
            check(view.starts == 1 && view.patrol.spawned == 0 && view.budget == 15, "full moon assault only");
            SiegeDirector.WaveSnapshot first = (SiegeDirector.WaveSnapshot) view.waves.get(0);
            check(first.planned == 4 && ((SiegeDirector.WaveSnapshot) view.waves.get(1)).planned == 5
                    && ((SiegeDirector.WaveSnapshot) view.waves.get(2)).planned == 6, "wave allocations");
            check(view.peakActive <= view.activeCap
                    && view.loadouts.equipped <= 5 && view.loadouts.failures == 0, "outdoor assault insertion and caps");
            while (cleared < view.spawns.size()) {
                SiegeDirector.Spawn spawn = (SiegeDirector.Spawn) view.spawns.get(cleared++);
                check(spawn.outdoorLight && spawn.horizontalSquared >= 144 && spawn.horizontalSquared <= 256,
                        "outdoor assault placement and distance");
                check(Math.max(Math.abs(spawn.x - X - .5), Math.abs(spawn.z - Z - .5)) > 13, "assault spawned outside wall");
                if ("waves".equals(mode)) clearHostiles(game);
            }
            if ("waves".equals(mode) && view.state != SiegeDirector.State.RESOLVED) return false;
            for (Object item : view.waves) {
                SiegeDirector.WaveSnapshot wave = (SiegeDirector.WaveSnapshot) item;
                check(wave.equipped <= wave.equipmentCap, "wave equipment cap");
                if ("waves".equals(mode)) check(wave.outdoorLight > 0, "outdoor insertion in each wave");
                System.out.println("ALPHA6 wave=" + wave.number + " inserted=" + wave.inserted + " outdoor="
                        + wave.outdoorLight + " equipped=" + wave.equipped + " attempts=" + wave.attempts
                        + " rejections=" + wave.rejections);
            }
        } else if ("quiet".equals(mode)) {
            if (game.f.w() < 14045) return false;
            check(view.starts == 0 && view.patrol.attempts == 0, "quiet new moon");
        } else {
            if (view.patrol.state != SiegeDirector.State.RESOLVED) return false;
            int budget = "gibbous".equals(mode) ? 3 : 2;
            int expected = "disabled".equals(mode) ? 0 : budget;
            check(view.patrol.budget == budget && view.patrol.spawned == expected
                    && view.patrol.outdoorLight == expected && view.patrol.attempts <= 48 && view.starts == 0,
                    "patrol outdoor insertions, schedule and budget");
            check(view.patrol.loadouts.equipped <= 1 && view.patrol.loadouts.failures == 0, "patrol equipment cap");
            System.out.println("ALPHA6 patrol inserted=" + view.patrol.spawned + " equipped="
                    + view.patrol.loadouts.equipped + " attempts=" + view.patrol.attempts + " rejections="
                    + view.patrol.rejections);
        }
        return true;
    }

    private static void arena(Minecraft game) {
        game.f.q = 2;
        for (int dx = -42; dx <= 42; dx++) for (int dz = -42; dz <= 42; dz++) {
            game.f.g(X + dx, Y - 1, Z + dz, 1);
            // A four-block torch grid leaves the supporting stone surface continuous.
            if (dx % 4 == 0 && dz % 4 == 0) game.f.b(X + dx, Y, Z + dz, 50, 5);
        }
        if ("full".equals(mode) || "waves".equals(mode)) wall(game, 13);
        game.h.c(X + .5, Y, Z + .5, 0, 0);
        game.h.r = game.h.s = game.h.t = 0;
        game.h.aT.a = true;
    }

    private static void clearHostiles(Minecraft game) {
        for (Object entity : game.f.b) {
            if (entity instanceof yy && Math.abs(((yy) entity).p - Y) < 3) game.f.b((yy) entity);
        }
    }

    private static void wall(Minecraft game, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
            for (int dy = 0; dy < 3; dy++) game.f.g(X + dx, Y + dy, Z + dz, 1);
        }
    }

    private static void geometry(Minecraft game) throws Exception {
        Class<?> type = Class.forName("zed.tekkitoverhaul.assault.SpawnController");
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(int.class, boolean.class);
        constructor.setAccessible(true);
        Object controller = constructor.newInstance(28, mod_ZedsTekkitOverhaul.lunarOutdoorLightPressure);
        ((java.util.Random) field(type, "random").get(controller)).setSeed(6001);
        Method place = type.getDeclaredMethod("tryCandidate", xd.class, yw.class, boolean.class, int.class, int.class);
        place.setAccessible(true);
        int x = X + 12, z = Z + 2;
        System.out.println("ALPHA6 fixture player=" + game.h.o + "," + game.h.p + "," + game.h.q
                + " surface=" + game.f.e(x, z) + " center=" + game.f.e(X, Z));
        check(game.f.b(wl.b, x, Y, z) > 7, "real artificial light above 7");
        yy vanilla = new ajg(game.f);
        vanilla.c(x + .5, Y, z + .5, 0, 0);
        check(!vanilla.i(), "ordinary zombie rejects the lit candidate");
        if ("disabled".equals(mode)) {
            probe(game, controller, place, "disabled field", x, z, "VANILLA_LIGHT");
            return;
        }
        probe(game, controller, place, "field", x, z, null);
        house(game, x, z, 20);
        probe(game, controller, place, "glass-roofed house", x, z, "NOT_OPEN_TO_SKY");
        house(game, x, z, 0);
        game.f.g(x, Y + 12, z, 1);
        probe(game, controller, place, "covered cell below raised stone surface", x, z, "SURFACE_OUT_OF_REACH");
        game.f.g(x, Y + 12, z, 0);
        game.f.g(x, Y, z, 9);
        check(game.f.e(x, z) == Y + 1, "water fixture height");
        probe(game, controller, place, "water", x, z, "UNSUITABLE_GROUND");
        game.f.g(x, Y, z, 0);
        wall(game, 14);
        probe(game, controller, place, "courtyard inside", x, z, "NO_OPEN_APPROACHES");
        probe(game, controller, place, "courtyard outside", X + 17, z, null);
        for (int dy = 0; dy < 3; dy++) game.f.g(X + 14, Y + dy, z, 0);
        probe(game, controller, place, "open entrance inside", x, z, "NO_OPEN_APPROACHES");
        probe(game, controller, place, "open entrance outside", X + 17, z, null);
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 14) continue;
            for (int dy = 0; dy < 3; dy++) game.f.g(X + dx, Y + dy, Z + dz, 0);
        }
    }

    private static void probe(Minecraft game, Object controller, Method place, String label, int x, int z,
                              String expectedRejection) throws Exception {
        Field rejection = field(controller.getClass(), "rejection");
        Field outdoor = field(controller.getClass(), "outdoorLight");
        Map<String, Integer> results = new LinkedHashMap<String, Integer>();
        long total = 0, worst = 0;
        int inserted = 0;
        for (int i = 0; i < 24; i++) {
            long sample = System.nanoTime();
            yy mob = (yy) place.invoke(controller, game.f, game.h, i % 2 == 0, x, z);
            long duration = System.nanoTime() - sample;
            total += duration;
            worst = Math.max(worst, duration);
            String result = (String) rejection.get(controller);
            if (mob != null) {
                check(expectedRejection == null && outdoor.getBoolean(controller), label + " insertion path");
                inserted++;
                game.f.b(mob);
                game.f.n(); // Remove the dead fixture before reusing this coordinate.
                result = "INSERTED";
            } else {
                check(expectedRejection == null ? "SKY_LIGHT_ROLL".equals(result) : expectedRejection.equals(result),
                        label + " rejection " + result);
            }
            Integer count = results.get(result);
            results.put(result, count == null ? 1 : count + 1);
        }
        check((inserted > 0) == (expectedRejection == null), label + " placement outcome");
        System.out.println("ALPHA6 " + label + " " + results + " mean-us=" + total / 24000
                + " max-us=" + worst / 1000);
    }

    private static void house(Minecraft game, int x, int z, int roofId) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            game.f.g(x + dx, Y + 3, z + dz, roofId);
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
            for (int dy = 0; dy < 3; dy++) game.f.g(x + dx, Y + dy, z + dz, roofId == 0 ? 0 : 1);
            if (roofId == 0 && (x + dx - X) % 4 == 0 && (z + dz - Z) % 4 == 0) {
                game.f.b(x + dx, Y, z + dz, 50, 5);
            }
        }
    }

    private static void open(Minecraft game) throws Exception {
        field(Minecraft.class, "c").set(game, new aes(game));
        game.a(save, save, (fj) null);
        game.a((vp) null);
        game.f.q = 2;
    }

    private static void saveQuit(Minecraft game) throws Exception {
        lb menu = new lb();
        game.a(menu);
        Method action = lb.class.getDeclaredMethod("a", abp.class);
        action.setAccessible(true);
        action.invoke(menu, new abp(1, 0, 0, "Save and Quit"));
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
