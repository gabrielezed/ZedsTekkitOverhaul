import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Native AI ticks and full-moon encounter dispatch against the pinned Tekkit world. */
public final class SurvivalIntegrationProbe {
    private static final long SEED = 32021L;
    private static final long DUSK = 4 * 24000 + 13000;
    private static final int CENTER = 288;
    private static final List<String> noEncounter = new ArrayList<>();
    private static boolean baseline;
    private static int wetlandInsertions;

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        baseline = args.length > 1 && "--baseline".equals(args[1]);
        tank(root, "open", 0, 0, false, false);
        tank(root, "hidden-cobble", 4, 0, false, true);
        tank(root, "retreat-behind-planks", 5, 0, true, true);
        tank(root, "raised-cobble", 4, 2, false, true);
        tank(root, "reachable-around-wall", 5, 0, false, false);
        tank(root, "protected-obsidian", 49, 0, false, false);
        rangedAndDoor(root);
        assaults(root);
        assaultReload(root);
        check(noEncounter.isEmpty(), "weak encounters on " + noEncounter);
        if (!baseline) check(wetlandInsertions >= 5, "both swamp sites yield a meaningful combined improvement");
    }

    private static void rangedAndDoor(File root) {
        TestWorld world = new TestWorld(root, "ranged-and-door", vx.c);
        world.q = 3;
        load(world, CENTER, CENTER, 3);
        yw player = NaturalSpawnProbe.join(world);
        player.c(296.5, 4, 288.5, 0, 0);
        EntitySkirmisher ranged = new EntitySkirmisher(world);
        ranged.c(288.5, 4, 288.5, 0, 0);
        check(world.a(ranged), "Skirmisher inserted");
        boolean aim = false, shot = false;
        double lateral = 0;
        for (int tick = 0; tick < 100; tick++) {
            world.n();
            aim |= ranged.isAiming();
            for (Object entity : world.b) if (entity instanceof EntityEnderPulse) shot = true;
            lateral = Math.max(lateral, Math.abs(ranged.q - 288.5));
        }
        check(aim && shot && lateral >= 2.0D, "Skirmisher repositions and fires a native projectile after aiming");
        world.e(ranged);
        for (int x = 287; x <= 296; x++)
            for (int y = 4; y < 7; y++) {
                world.g(x, y, 291, 1);
                world.g(x, y, 287, 1);
            }
        world.d(292, 4, 289, 64, 0);
        world.d(292, 5, 289, 64, 8);
        for (int y = 4; y < 7; y++) {
            world.g(292, y, 288, 1);
            world.g(292, y, 290, 1);
        }
        player.c(295.5, 4, 289.5, 0, 0);
        EntityBreacher melee = new EntityBreacher(world);
        melee.c(290.5, 4, 289.5, 0, 0);
        melee.engageEncounterPlayer(player);
        check(world.a(melee), "Breacher inserted");
        boolean pressure = false;
        for (int tick = 0; tick < 135; tick++) {
            world.n();
            pressure |= melee.isPressureSwing();
        }
        check(pressure && world.a(292, 4, 289) == 64 && world.a(292, 5, 289) == 64,
                "Breacher pressures a closed wooden door without removing its halves");
        System.out.println("ENDERKIN Skirmisher aimed, repositioned and fired; Breacher pressured an intact door");
    }

    private static void tank(File root, String name, int material, int rise, boolean retreat, boolean breach)
            throws Exception {
        TestWorld world = new TestWorld(root, "tank-" + name, vx.c);
        world.q = 3;
        load(world, CENTER, CENTER, 3);
        int wallX = CENTER + 3;
        int half = "reachable-around-wall".equals(name) ? 2 : 19;
        if (material != 0) {
            for (int z = CENTER - half; z <= CENTER + half; z++)
                for (int y = 4; y < 8; y++) world.g(wallX, y, z, material);
        }
        if (rise > 0) {
            for (int x = wallX + 1; x < wallX + 6; x++)
                for (int z = CENTER - 4; z <= CENTER + 4; z++)
                    for (int y = 4; y < 4 + rise; y++) world.g(x, y, z, 1);
        }
        yw player = NaturalSpawnProbe.join(world);
        player.c(retreat ? CENTER + 1.5 : wallX + 3.5, 4 + rise, retreat ? CENTER + 6.5 : CENTER + 0.5, 0, 0);
        EntityTank tank = new EntityTank(world);
        tank.c(CENTER - 1.5, 4, CENTER + 0.5, 0, 0);
        check(world.a(tank), "Tank inserted");
        Field target = TacticalHostile.class.getDeclaredField("target");
        target.setAccessible(true);
        int acquired = -1, windup = -1, struck = -1, crossed = -1, strikes = 0;
        int firstStrikeBlocks = 0;
        for (int tick = 0; tick < 480; tick++) {
            if (retreat && tick == 30) player.c(wallX + 3.5, 4, CENTER + 0.5, 0, 0);
            world.n();
            if (acquired < 0 && target.get(tank) == player) acquired = tick;
            if (windup < 0 && tank.heavyPose(0) >= 0) windup = tick;
            ady data = new ady();
            tank.b(data);
            strikes = data.f("ZtoBreachStrikes");
            if (strikes > 0 && struck < 0) {
                struck = tick;
                firstStrikeBlocks = removedWall(world, wallX, half);
            }
            if (material != 0 && tank.o > wallX + 1.95D) {
                crossed = tick;
                break;
            }
        }
        int removed = material == 0 ? 0 : removedWall(world, wallX, half);
        System.out.printf("TANK %-22s acquired=%d pose=%d breach=%d strikes=%d blocks=%d crossed=%d playerHp=%d%n",
                name, acquired, windup, struck, strikes, removed, crossed, player.bb());
        if (!baseline && breach) check(struck >= 0 && firstStrikeBlocks > 0 && firstStrikeBlocks <= 4
                && removed <= 12 && struck - windup >= EntityTank.WINDUP, name + " requires a telegraphed local breach");
        if (!breach) check(removed == 0, name + " should not break its surroundings");
        if (!baseline && "open".equals(name)) check(windup >= 0, "can engage on open ground");
        if (!baseline && breach && rise == 0) check(player.bb() < 20, "Tank lands a melee strike through the breach");
        if (!baseline && "reachable-around-wall".equals(name))
            check(crossed >= 0, "uses the available route around a short wall");
    }

    private static int removedWall(TestWorld world, int wallX, int half) {
        int removed = 0;
        for (int z = CENTER - half; z <= CENTER + half; z++)
            for (int y = 4; y < 8; y++) if (world.a(wallX, y, z) == 0) removed++;
        return removed;
    }

    private static void assaults(File root) throws Exception {
        String[] names = {"plains", "swamp", "forest", "hills", "swamp-second"};
        int[] biomes = {1, 6, 4, 3, 6};
        for (int i = 0; i < names.length; i++) {
            TestWorld world = new TestWorld(root, names[i], vx.b);
            int[] site = site(world, biomes[i], i == 4);
            encounter(world, names[i], site[0], site[1], false, false, false);
        }
        TestWorld base = new TestWorld(root, "base", vx.c);
        load(base, CENTER, CENTER, 3);
        // Closed flat-roofed shelter in one sector, with an open field around it.
        for (int x = CENTER + 10; x <= CENTER + 17; x++) {
            for (int z = CENTER - 7; z <= CENTER + 7; z++) {
                base.g(x, 7, z, 20);
                if (x == CENTER + 10 || x == CENTER + 17 || Math.abs(z - CENTER) == 7)
                    for (int y = 4; y < 7; y++) base.g(x, y, z, 1);
            }
        }
        encounter(base, "base", CENTER, CENTER, true, true, false);
        TestWorld roofed = new TestWorld(root, "roofed", vx.c);
        load(roofed, CENTER, CENTER, 3);
        for (int x = CENTER - 17; x <= CENTER + 17; x++)
            for (int z = CENTER - 17; z <= CENTER + 17; z++) roofed.g(x, 7, z, 20);
        encounter(roofed, "roofed", CENTER, CENTER, false, false, false);

        TestWorld populated = new TestWorld(root, "territory-and-assault", vx.b);
        int[] site = site(populated, 1, false);
        encounter(populated, "occupied", site[0], site[1], false, true, true);

        TestWorld crowded = new TestWorld(root, "local-cap-control", vx.c);
        load(crowded, CENTER, CENTER, 3);
        for (int i = 0; i < 28; i++) {
            ajg resident = new ajg(crowded);
            resident.c(CENTER + 19 + i % 7, 4, CENTER + i / 7, 0, 0);
            check(crowded.a(resident), "insert nearby hostile for local cap control");
        }
        encounter(crowded, "crowded", CENTER, CENTER, false, true, false);
    }

    private static int[] site(TestWorld world, int biome, boolean distant) {
        for (int radius = 128; radius <= 8192; radius += 128) {
            if (distant && radius < 768) continue;
            for (int i = -radius; i <= radius; i += 128) {
                for (int[] candidate : new int[][] {{i, radius}, {i, -radius}, {radius, i}, {-radius, i}}) {
                    int x = candidate[0], z = candidate[1];
                    if (world.i().a(x, z).M != biome || world.i().a(x + 20, z + 20).M != biome
                            || world.i().a(x - 20, z - 20).M != biome) continue;
                    load(world, x, z, 3);
                    // A real exposed place to stand. Leave the sampled annulus unmodified.
                    for (int dz = -8; dz <= 8; dz++) {
                        for (int dx = -8; dx <= 8; dx++) {
                            int feet = world.e(x + dx, z + dz);
                            int ground = world.a(x + dx, feet - 1, z + dz);
                            if (ground == 2 || ground == 3 || ground == 1) return new int[] {x + dx, z + dz};
                        }
                    }
                }
            }
        }
        throw new AssertionError("No exposed sample for biome " + biome);
    }

    private static void assaultReload(File root) {
        TestWorld first = new TestWorld(root, "assault-reload", vx.c);
        first.q = 2;
        yw player = NaturalSpawnProbe.join(first);
        LunarCalendar moon = calendar(DUSK - 1);
        SiegeDirector live = new SiegeDirector(true, 15, 900, 8, 16, false, true, true);
        live.bind(first, moon.view());
        first.a(DUSK);
        moon.observe(DUSK);
        live.tick(player, false, null);
        check(live.snapshot(first).state == SiegeDirector.State.ACTIVE, "full moon begins before saving");
        first.z.a();

        TestWorld reopened = new TestWorld(root, "assault-reload", vx.c);
        reopened.q = 2;
        yw returned = NaturalSpawnProbe.join(reopened);
        LunarCalendar sameNight = calendar(DUSK);
        SiegeDirector restored = new SiegeDirector(true, 15, 900, 8, 16, false, true, true);
        restored.bind(reopened, sameNight.view());
        sameNight.observe(DUSK + 1);
        restored.tick(returned, false, null);
        SiegeDirector.Snapshot result = restored.snapshot(reopened);
        check(result.state == SiegeDirector.State.ABORTED && "INTERRUPTED_ON_REOPEN".equals(result.reason)
                && result.starts == 0 && result.spawns.isEmpty(), "reopening consumes the interrupted night without replay");
        System.out.println("ASSAULT reload       interrupted encounter is consumed without replay");
    }

    private static LunarCalendar calendar(long time) {
        LunarCalendar moon = new LunarCalendar(1, 0, 0, Long.MIN_VALUE, () -> {}, error -> {
            throw (RuntimeException) error;
        });
        moon.rebaseline(0, time, LunarEvent.Cause.LOADED);
        return moon;
    }

    @SuppressWarnings("unchecked")
    private static void encounter(TestWorld world, String name, int x, int z, boolean protectedRoom, boolean occupied,
                                  boolean populate) throws Exception {
        load(world, x, z, 4);
        world.q = 2;
        world.a(DUSK - 1);
        world.q();
        yw player = NaturalSpawnProbe.join(world);
        player.c(x + 0.5, world.e(x, z), z + 0.5, 0, 0);
        TerritorialActivityService territory = new TerritorialActivityService();
        if (occupied) {
            territory.bind(world);
            for (int tick = 0; tick < 4800; tick++) territory.tick(player, false);
        }
        int ambient = world.b(xf.class);
        if (populate) {
            world.a(DUSK + 5000);
            world.q();
            mod_MinecraftForge.SPAWNER_MAKE_MORE_RANDOM = false;
            NaturalSpawnPressure pressure = new NaturalSpawnPressure(territory, 70, true);
            pressure.bind(world);
            pressure.register();
            load(world, x, z, 9);
            for (int pass = 0; pass < 5000; pass++) vf.a(world, true, false);
            NaturalSpawnPressure.Snapshot natural = pressure.snapshot(world);
            ambient = world.b(xf.class);
            check(natural.hookActive && natural.effectiveCap > natural.vanillaCap
                    && ambient > natural.vanillaCap && ambient <= natural.effectiveCap + 4,
                    "generated terrain reaches a bounded boosted cap before the assault");
        }
        SiegeDirector director = new SiegeDirector(true, 15, 900, 8, 16, false, true, true);
        LunarCalendar moon = calendar(DUSK - 1);
        director.bind(world, moon.view());
        Field spawns = SiegeDirector.class.getDeclaredField("spawns");
        spawns.setAccessible(true);
        Object placer = spawns.get(director);
        Field random = placer.getClass().getDeclaredField("random");
        random.setAccessible(true);
        ((Random) random.get(placer)).setSeed(32021L);
        // Advance the real calendar one tick at a time. Keep placed mobs in the world so the
        // real director's population caps apply, while holding combat and unrelated world ticks.
        for (int tick = 0; tick <= 3300; tick++) {
            long time = DUSK + tick;
            world.a(time);
            if (tick % 20 == 0) world.q();
            moon.observe(time);
            director.tick(player, false, occupied ? territory : null);
        }
        SiegeDirector.Snapshot result = director.snapshot(world);
        check(result.starts == 1 && result.peakActive <= result.activeCap, name + " real natural admission and cap");
        Map<String, Integer> rejected = new TreeMap<>();
        for (Object value : result.waves) {
            SiegeDirector.WaveSnapshot wave = (SiegeDirector.WaveSnapshot) value;
            wave.rejections.forEach((rule, count) -> rejected.merge((String) rule, (Integer) count, Integer::sum));
        }
        int nativeMobCount = world.b(xf.class);
        check(nativeMobCount == ambient + result.spawns.size(), name + " evidence matches actual live insertions");
        if (protectedRoom) {
            for (Object value : result.spawns) {
                SiegeDirector.Spawn spawn = (SiegeDirector.Spawn) value;
                int sx = (int) Math.floor(spawn.x), sz = (int) Math.floor(spawn.z);
                check(!(sx >= x + 10 && sx <= x + 17 && sz >= z - 7 && sz <= z + 7 && spawn.y < 8),
                        "closed glass roof protects the interior");
            }
        }
        System.out.printf("ASSAULT %-12s x=%d z=%d y=%.0f %d/%d ambient=%d local=%d peak=%d rejects=%s%n",
                name, x, z, player.y.b, result.spawns.size(), result.attempts,
                ambient, result.localHostiles, result.peakActive, rejected);
        if ("roofed".equals(name)) {
            check(result.spawns.isEmpty() && rejected.containsKey("NOT_OPEN_TO_SKY"),
                    "covered annulus cannot place a lunar mob");
        } else if ("crowded".equals(name)) {
            check(result.spawns.isEmpty() && result.attempts == 0 && rejected.containsKey("LOCAL_MOB_CAP"),
                    "existing local hostiles defer the assault before placement");
        } else if (!baseline && !populate) {
            boolean wetland = name.startsWith("swamp");
            if (wetland) wetlandInsertions += result.spawns.size();
            if (result.spawns.size() < (wetland ? 1 : 4)) noEncounter.add(name);
        } else if (populate) {
            check(result.admissionThreat != null && result.admissionThreat.threat == territory.assess(world, player).threat
                    && result.admissionThreat.threat > 0, "captures the actual occupied territory threat at dusk");
            check(result.spawns.size() >= 4 || rejected.containsKey("LOCAL_MOB_CAP"),
                    "existing population either leaves space for waves or explicitly defers them");
        }
        for (Object entity : world.b.toArray()) if (entity instanceof xf) world.e((nn) entity);
        world.d.remove(player);
    }

    private static void load(xd world, int x, int z, int radius) {
        for (int cx = (x >> 4) - radius; cx <= (x >> 4) + radius; cx++)
            for (int cz = (z >> 4) - radius; cz <= (z >> 4) + radius; cz++) world.d(cx, cz);
    }

    private static void check(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
    }

    private static final class TestWorld extends xd {
        TestWorld(File root, String name, vx terrain) {
            super(new aia(root, name, false), name, new fj(SEED, 0, false, false, terrain));
        }

        @Override public void a(nn entity, String sound, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
    }
}
