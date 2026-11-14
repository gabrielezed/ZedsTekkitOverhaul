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
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.mod_ZedsTekkitOverhaul;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;

/** Exercises the packaged classes on the game thread in a marked copy of the pack. */
public final class Alpha6RuntimeProbe implements ITickHandler {
    private static final int X = 34, Y = 121, Z = 215;
    private static String save, mode;
    private static Field lastFocusTime;
    private boolean done;
    private int stage, frozen;
    private long pauseUntil, reopenAt;
    private int cleared;
    private LocalLunarCalendar lunar;
    private AssaultRecurrenceProbe recurrence;
    private EntitySkirmisher caster;
    private EntityBreacher breacher;
    private boolean sawPulse, sawPressure;
    private long combatStart;
    private boolean saturated;
    private boolean liftedRoof;
    private int doorX, doorZ;
    private long lastCombatFrame = Long.MIN_VALUE;
    private File directory;

    public static final class Bootstrap implements ITickHandler {
        private boolean ready;

        public static void main(String[] args) throws Exception {
            File game = new File(args[0]).getCanonicalFile();
            String world = args[1];
            if (!"survival-hud-runtime".equals(game.getParentFile().getParentFile().getName())
                    || !world.matches("ZTO-Test-[a-f0-9]{32}")
                    || !new File(game, "saves/" + world + "/.zto-disposable-test").isFile()) {
                throw new IllegalArgumentException("Use a marked save in the isolated Tekkit Dev copy");
            }
            Field folder = Minecraft.class.getDeclaredField("aj");
            folder.setAccessible(true);
            folder.set(null, game);
            System.setProperty("zto.alpha6.save", world);
            System.setProperty("zto.alpha6.mode", args[2]);
            FMLCommonHandler.instance().registerTickHandler(new Bootstrap());
            Minecraft.main(new String[] {"ZTOTest"});
        }

        public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
        public String getLabel() { return "Lunar encounter probe bootstrap"; }
        public void tickStart(EnumSet<TickType> ticks, Object... data) {}
        public void tickEnd(EnumSet<TickType> ticks, Object... data) {
            if (ready) return;
            Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
            if (game == null || !(game.s instanceof xt)) return;
            FMLCommonHandler.instance().registerTickHandler(new Alpha6RuntimeProbe());
            ready = true;
        }
    }

    private static void initialize() throws Exception {
        save = System.getProperty("zto.alpha6.save");
        mode = System.getProperty("zto.alpha6.mode");
        lastFocusTime = field(lr.class, "Y");
    }

    private static ZtoConfig settings() {
        for (BaseMod mod : ModLoader.getLoadedMods()) {
            if (mod instanceof mod_ZedsTekkitOverhaul) return ((mod_ZedsTekkitOverhaul) mod).settings();
        }
        throw new AssertionError("ZTO was not loaded");
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    public String getLabel() { return "Alpha 6 runtime acceptance"; }
    public void tickStart(EnumSet<TickType> ticks, Object... data) {
        if (lastFocusTime == null || done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        try {
            // The hidden test client must only pause through the explicit menu in this probe.
            lastFocusTime.setLong(game.u, System.currentTimeMillis());
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        if (game == null) return;
        try {
            SilentRuntimeAudio.verify(game);
            if (stage == 0) {
                ZtoConfig settings = settings();
                initialize();
                directory = (File) field(Minecraft.class, "aj").get(null);
                check(!settings.survivalDebugHud && settings.lunarDebugKey == 0
                        && !settings.lunarVerboseLogging
                        && !Boolean.getBoolean("zto.devBridge.enabled"), "diagnostics disabled");
                check(settings.lunarOutdoorLightPressure != "disabled".equals(mode), "loaded configuration");
                check(settings.experimentalEnderkinEncounters == mode.startsWith("ender-"),
                        "experimental setting loaded from startup configuration");
                System.out.println("ALPHA6 JAR " + mod_ZedsTekkitOverhaul.class.getProtectionDomain()
                        .getCodeSource().getLocation() + " Java " + System.getProperty("java.version"));
                lunar = (LocalLunarCalendar) field(mod_ZedsTekkitOverhaul.class, "LUNAR").get(null);
                open(game);
                arena(game);
                if ("assault-recurrence".equals(mode)) {
                    recurrence = new AssaultRecurrenceProbe(lunar);
                    game.h.aT.a = false;
                    field(aft.class, "d").setFloat(field(Minecraft.class, "X").get(game), 6.0F);
                }
                if (mode.startsWith("ender-")) seedPlacement();
                stage++;
                return;
            }
            Snapshot view = lunar.assaultSnapshot(game.f);
            if (view == null) return;
            if (recurrence != null) {
                try {
                    if (recurrence.tick(game, view)) {
                        saveQuit(game);
                        System.out.println("ALPHA6 PASS " + mode);
                        done = true;
                        game.f();
                    }
                } catch (Throwable failure) {
                    recurrence.trace(game, view);
                    throw failure;
                }
                return;
            }
            if (stage == 1) {
                if (game.f.w() < 10820) return;
                if ("first".equals(mode) || "disabled".equals(mode)) geometry(game);
                if ("ender-first".equals(mode)) enderkinGeometry(game);
                if ("ender-fallback".equals(mode)) roofRing(game, 1);
                game.f.a("ender-waves".equals(mode) ? 8 * 24000L + 12980 : 12980L);
                stage++;
                return;
            }
            if (stage == 3) {
                check(view.patrol.elapsed == frozen && view.patrol.attempts == 0, "paused patrol is frozen");
                if (System.nanoTime() >= pauseUntil) {
                    check(game.s instanceof lb, "native pause menu is open");
                    game.a((vp) null);
                    stage = 4;
                }
                return;
            }
            if (stage == 5) {
                check(view.starts == 0 && view.spawns.isEmpty() && view.patrol.attempts == 0, "same night replay");
                if ("full".equals(mode)) check("INTERRUPTED_ON_REOPEN".equals(view.reason), "interrupted assault state");
                if ("waves".equals(mode) || "ender-waves".equals(mode))
                    check("NIGHT_CONSUMED".equals(view.reason), "resolved assault state");
                if (game.f.w() >= reopenAt + 100) {
                    System.out.println("ALPHA6 reopen mode=" + mode + " reason=" + view.reason
                            + " through=" + game.f.w() + " starts=" + view.starts + " patrolAttempts=" + view.patrol.attempts);
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
            if (!(mode.startsWith("ender-") ? verifyEnderkin(game, view) : verifyEncounter(game, view))) return;
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

    private void seedPlacement() throws Exception {
        Object director = field(LocalLunarCalendar.class, "assault").get(lunar);
        Object owner = director;
        if (!"ender-waves".equals(mode)) owner = field(director.getClass(), "patrol").get(director);
        Object spawns = field(owner.getClass(), "spawns").get(owner);
        ((java.util.Random) field(spawns.getClass(), "random").get(spawns)).setSeed(6001L);
    }

    private boolean verifyEnderkin(Minecraft game, Snapshot view) throws Exception {
        check(view.experimentalEnderkin, "snapshot exposes opt-in");
        if ("ender-waves".equals(mode)) return verifyMixedAssault(game, view);
        SiegeDirector.PatrolSnapshot patrol = view.patrol;
        if ("ender-peaceful".equals(mode) && patrol.state == SiegeDirector.State.ACTIVE && patrol.elapsed > 10)
            game.A.C = game.f.q = 0;
        if ("ender-cap".equals(mode) && !saturated && patrol.elapsed >= 790 && patrol.state == SiegeDirector.State.ACTIVE) {
            saturate(game);
            saturated = true;
        }
        if ("ender-fallback".equals(mode) && patrol.enderkin.attempts == 4 && !liftedRoof) {
            roofRing(game, 0);
            liftedRoof = true;
        }
        if (patrol.state != SiegeDirector.State.RESOLVED && patrol.state != SiegeDirector.State.ABORTED) return false;
        check(view.starts == 0, "patrol never starts an assault");
        check(patrol.budget == ("ender-first".equals(mode) ? 2 : 3), "unchanged patrol budget");
        if ("ender-first".equals(mode)) {
            check(patrol.spawned == 2 && patrol.enderkin.selected == null && patrol.enderkin.attempts == 0,
                    "first quarter stays ordinary when enabled");
        } else {
            check("SKIRMISHER".equals(patrol.enderkin.selected) && patrol.enderkin.attempts <= 4, "seeded patrol selection");
            if ("ender-fallback".equals(mode)) {
                check(patrol.enderkin.attempts == 4 && !patrol.enderkin.inserted && patrol.spawned == 3,
                        "four rejected candidates fall back to ordinary slots");
                check(Integer.valueOf(4).equals(patrol.enderkin.rejections.get("SURFACE_OUT_OF_REACH")),
                        "roof rejects every special candidate");
            } else if ("ender-cap".equals(mode)) {
                check("LOCAL_MOB_CAP".equals(patrol.reason) && patrol.enderkin.attempts == 0 && patrol.spawned == 0,
                        "nearby hostiles stop attempts before allocating an Enderkin");
            } else if ("ender-peaceful".equals(mode)) {
                check("PEACEFUL".equals(patrol.reason) && patrol.spawned == 0, "Peaceful cancels eligible patrol");
                game.A.C = game.f.q = 2;
            } else {
                check(patrol.spawned == 3 && patrol.enderkin.inserted && patrol.outdoorLight == 3,
                        "one Skirmisher substitutes inside a three-mob lit patrol");
            }
        }
        System.out.println("ALPHA6 " + mode + " budget=" + patrol.budget + " inserted=" + patrol.spawned
                + " outcome=" + patrol.reason + " " + patrol.enderkin.summary());
        return true;
    }

    private boolean verifyMixedAssault(Minecraft game, Snapshot view) throws Exception {
        if (view.starts == 0 || lastCombatFrame == game.f.w()) return false;
        lastCombatFrame = game.f.w();
        check(view.budget == 15 && view.spawns.size() <= 15 && view.peakActive <= view.activeCap, "same assault limits");
        while (cleared < view.spawns.size()) {
            SiegeDirector.Spawn entry = (SiegeDirector.Spawn) view.spawns.get(cleared++);
            check(entry.outdoorLight && entry.horizontalSquared >= 144 && entry.horizontalSquared <= 256,
                    "lit surface in legacy annulus");
        }
        for (Object entity : game.f.b) {
            if (caster == null && entity instanceof EntitySkirmisher) {
                caster = (EntitySkirmisher) entity;
                faceFrom(game, caster, 8);
                combatStart = game.f.w();
            }
            if (breacher == null && entity instanceof EntityBreacher) {
                breacher = (EntityBreacher) entity;
                faceFrom(game, breacher, 8);
                combatStart = game.f.w();
                doorX = (int) Math.floor(breacher.o) - 3;
                doorZ = (int) Math.floor(breacher.q);
                // Align the combat fixture with the same flat doorway used by the alpha.7 probe.
                breacher.c(breacher.o, Y, doorZ + .5D, 90, 0);
                faceFrom(game, breacher, 8);
                for (int dx = -10; dx <= 5; dx++) for (int dz = -3; dz <= 3; dz++) {
                    game.f.g(doorX + dx, Y - 1, doorZ + dz, 1);
                    for (int dy = 0; dy <= 2; dy++) game.f.g(doorX + dx, Y + dy, doorZ + dz, 0);
                }
                for (int dz = -16; dz <= 16; dz++)
                    for (int dy = 0; dy < 3; dy++) game.f.g(doorX, Y + dy, doorZ + dz, 1);
                game.f.b(doorX, Y + 1, doorZ, 64, 8);
                game.f.d(doorX, Y, doorZ, 64, 2);
                System.out.println("ALPHA6 mixed doorway at " + doorX + "," + doorZ);
            }
            if (!sawPulse && entity instanceof EntityEnderPulse && ((EntityEnderPulse) entity).getOwner() == caster) {
                sawPulse = true;
                capture(game, "mixed-pulse");
            }
        }
        if (breacher != null && !sawPressure && breacher.isPressureSwing()) {
            sawPressure = true;
            capture(game, "mixed-pressure");
            check(game.f.a(doorX, Y, doorZ) == 64 && game.f.e(doorX, Y, doorZ) == 2,
                    "mixed assault Breacher leaves the door closed and intact");
            for (int dz = -16; dz <= 16; dz++)
                for (int dy = 0; dy < 3; dy++) game.f.g(doorX, Y + dy, doorZ + dz, 0);
        }
        boolean waitForCast = caster != null && !caster.G && !sawPulse;
        boolean waitForPressure = breacher != null && !breacher.G && !sawPressure;
        if ((waitForCast || waitForPressure) && game.f.w() - combatStart < 240) {
            game.h.l(20);
            game.h.r = game.h.s = game.h.t = 0;
            return false;
        }
        if (game.f.w() % 20 == 0) {
            clearHostiles(game);
            game.h.c(X + .5, Y, Z + .5, -90, 0);
            game.h.r = game.h.s = game.h.t = 0;
        }
        if (view.state != SiegeDirector.State.RESOLVED) return false;
        int[] expected = {4, 5, 6};
        for (Object item : view.waves) {
            SiegeDirector.WaveSnapshot wave = (SiegeDirector.WaveSnapshot) item;
            check(wave.inserted == expected[wave.number - 1], "4/5/6 budget preserved");
            System.out.println("ALPHA6 mixed wave=" + wave.number + " inserted=" + wave.inserted
                    + " peak=" + wave.peakActive + " " + wave.enderkin.summary());
        }
        check(((SiegeDirector.WaveSnapshot) view.waves.get(0)).enderkin.selected == null
                && ((SiegeDirector.WaveSnapshot) view.waves.get(1)).enderkin.inserted
                && ((SiegeDirector.WaveSnapshot) view.waves.get(2)).enderkin.inserted && sawPulse && sawPressure,
                "both substituted roles use their tactical attacks");
        return true;
    }

    private static void faceFrom(Minecraft game, nn mob, double distance) {
        game.h.c(mob.o - distance, Y, mob.q, -90, 0);
        game.h.r = game.h.s = game.h.t = 0;
    }

    private void capture(Minecraft game, String name) {
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "renderer before capture");
        System.out.println("ALPHA6 " + name + " " + zh.a(directory, save + "-" + name + ".png", game.d, game.e));
    }

    private static void roofRing(Minecraft game, int block) {
        for (int dx = -16; dx <= 16; dx++) for (int dz = -16; dz <= 16; dz++) {
            int square = dx * dx + dz * dz;
            if (square >= 144 && square <= 256) game.f.g(X + dx, Y + 10, Z + dz, block);
        }
    }

    private static void enderkinGeometry(Minecraft game) throws Exception {
        Class<?> controllerClass = Class.forName("zed.tekkitoverhaul.assault.SpawnController");
        Class<?> roleClass = Class.forName("zed.tekkitoverhaul.assault.EnderkinRole");
        java.lang.reflect.Constructor<?> constructor = controllerClass.getDeclaredConstructor(int.class, boolean.class);
        constructor.setAccessible(true);
        Object controller = constructor.newInstance(16, true);
        Method place = controllerClass.getDeclaredMethod("tryCandidate", xd.class, yw.class, boolean.class,
                int.class, int.class, roleClass);
        place.setAccessible(true);
        Object role = roleClass.getEnumConstants()[1];
        int x = X + 12, z = Z + 2;
        game.h.aT.a = false;
        game.f.g(x, Y + 2, z, 20);
        geometryRejection(game, controller, place, role, x, z, "NOT_OPEN_TO_SKY");
        game.f.g(x, Y + 2, z, 0);
        game.f.g(x, Y, z, 9);
        geometryRejection(game, controller, place, role, x, z, "UNSUITABLE_GROUND");
        game.f.g(x, Y, z, 0);
        game.f.g(x, Y + 1, z + 1, 1);
        geometryRejection(game, controller, place, role, x, z, "ENDERKIN_CLEARANCE");
        game.f.g(x, Y + 1, z + 1, 0);
        wall(game, 14);
        geometryRejection(game, controller, place, role, x, z, "NO_OPEN_APPROACHES");
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 14) continue;
            for (int dy = 0; dy < 3; dy++) game.f.g(X + dx, Y + dy, Z + dz, 0);
        }
        game.f.g(x, Y - 2, z, 1);
        game.f.g(x, Y - 1, z, 0);
        // A two-block drop along an outward approach rules out this narrow trench.
        game.f.g(x + 1, Y - 3, z, 1);
        game.f.g(x + 1, Y - 2, z, 0);
        game.f.g(x + 1, Y - 1, z, 0);
        game.f.g(x + 2, Y - 1, z, 1);
        geometryRejection(game, controller, place, role, x, z, "ENDERKIN_CLEARANCE");
        game.f.g(x, Y - 1, z, 1);
        game.f.g(x + 1, Y - 1, z, 1);
        geometryRejection(game, controller, place, role, X + 28, z, "OUTSIDE_SPAWN_RANGE");
        unloadedBoundary(game, controller, place, role);
        barrierMatrix(game);
        game.h.aT.a = true;
    }

    private static void unloadedBoundary(Minecraft game, Object controller, Method place, Object role)
            throws Exception {
        int x = X + 4096;
        double playerX = game.h.o, playerY = game.h.p, playerZ = game.h.q;
        check(!game.f.b(x - 2, 0, Z - 2, x + 2, 255, Z + 2), "fixture starts outside loaded chunks");
        // Move the query origin temporarily without ticking or changing the chunk provider.
        game.h.c(x - 11.5D, Y, Z + .5D, 0, 0);
        try {
            geometryRejection(game, controller, place, role, x, Z, "UNLOADED_CHUNK");
            check(!game.f.b(x - 2, 0, Z - 2, x + 2, 255, Z + 2), "placement did not load the boundary");
        } finally {
            game.h.c(playerX, playerY, playerZ, 0, 0);
        }
    }

    private static void barrierMatrix(Minecraft game) throws Exception {
        int x = X + 4, z = Z;
        game.h.c(x - 1.5, Y, z + .5, -90, 0);
        EntityBreacher mob = new EntityBreacher(game.f);
        mob.c(x + 1.35, Y, z + .5, 90, 0);
        Class<?> tactical = mob.getClass().getSuperclass();
        field(tactical, "target").set(mob, game.h);
        Method pressure = EntityBreacher.class.getDeclaredMethod("pressure");
        pressure.setAccessible(true);
        Field impacts = field(EntityBreacher.class, "impacts");
        int machine = ((pb) Class.forName("RedPowerMachine").getField("blockMachine").get(null)).bO;
        // The pack's fence ray misses the isolated post in this approach. Native collision still blocks movement.
        int[][] cases = {{64, 0, 1}, {71, 0, 1}, {85, 0, 0}, {107, 1, 1}, {1, 0, 1},
                {64, 4, 0}, {107, 5, 0}, {54, 0, 0}, {machine, 1, 0}};
        for (int[] entry : cases) {
            int id = entry[0], data = entry[1];
            game.f.g(x, Y, z, 0);
            game.f.g(x, Y + 1, z, 0);
            if (id == 64 || id == 71) game.f.b(x, Y + 1, z, id, 8);
            game.f.d(x, Y, z, id, data);
            kw tile = game.f.b(x, Y, z);
            int initialMetadata = game.f.e(x, Y, z);
            if (id == machine || id == 54) check(tile != null, "fixture has live tile " + id);
            if (id == 54) ((hb) tile).a(0, new aan(265, 17, 0));
            ady before = new ady();
            if (tile != null) tile.b(before);
            impacts.setInt(mob, 0);
            boolean struck = (Boolean) pressure.invoke(mob);
            check(struck == (entry[2] == 1), "barrier gate behavior id=" + id + ":" + data + " struck=" + struck);
            check(game.f.a(x, Y, z) == id && game.f.e(x, Y, z) == initialMetadata && game.f.b(x, Y, z) == tile,
                    "barrier and tile unchanged id=" + id);
            if (tile != null) {
                ady after = new ady();
                tile.b(after);
                check(before.equals(after), "barrier tile NBT unchanged id=" + id);
            }
            System.out.println("ALPHA6 Breacher barrier id=" + id + ":" + data + " pressure=" + struck);
        }
        game.f.g(x, Y, z, 0);
        game.h.c(X + .5, Y, Z + .5, 0, 0);
        game.h.r = game.h.s = game.h.t = 0;
    }

    private static void geometryRejection(Minecraft game, Object controller, Method place, Object role,
                                          int x, int z, String expected) throws Exception {
        long start = System.nanoTime();
        Object result = place.invoke(controller, game.f, game.h, false, x, z, role);
        String rejection = (String) field(controller.getClass(), "rejection").get(controller);
        check(result == null && expected.equals(rejection), "special placement: expected " + expected + ", got " + rejection);
        System.out.println("ALPHA6 Breacher placement " + rejection + " us=" + (System.nanoTime() - start) / 1000);
    }

    private static void saturate(Minecraft game) {
        for (int i = 0; i < 28; i++) {
            ajg zombie = new ajg(game.f);
            zombie.c(X + 7.5 + i % 7, Y, Z + 6.5 + i / 7, 0, 0);
            check(game.f.a(zombie), "population fixture");
        }
        game.h.aT.a = true;
    }

    private boolean verifyEncounter(Minecraft game, Snapshot view) {
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
        game.h.aT.a = !("ender-waves".equals(mode) || "ender-gibbous".equals(mode));
    }

    static void clearHostiles(Minecraft game) {
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
        Object controller = constructor.newInstance(28, settings().lunarOutdoorLightPressure);
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

    static void open(Minecraft game) throws Exception {
        field(Minecraft.class, "c").set(game, new aes(game));
        game.a(save, save, (fj) null);
        game.a((vp) null);
        game.f.q = 2;
    }

    static void saveQuit(Minecraft game) throws Exception {
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
