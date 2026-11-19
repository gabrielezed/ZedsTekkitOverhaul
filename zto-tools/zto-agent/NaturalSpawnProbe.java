import java.io.File;
import zed.tekkitoverhaul.agent.ZTOAgent;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Headless comparison using the pinned Tekkit World, entity checks and ordinary spawning pass. */
public final class NaturalSpawnProbe {
    static final fj SETTINGS = new fj(32021L, 0, false, false, vx.c);

    public static void main(String[] args) {
        File root = new File(args[0]);
        mod_MinecraftForge.SPAWNER_MAKE_MORE_RANDOM = false;
        TerritorialActivityService territory = new TerritorialActivityService();
        NaturalSpawnPressure pressure = new NaturalSpawnPressure(territory, 70, true);
        pressure.register();
        xd quiet = open(root, "quiet", 18000);
        yw visitor = join(quiet);
        territory.bind(quiet);
        pressure.bind(quiet);
        NaturalSpawnPressure.Snapshot baseline = populate(quiet, pressure);
        int lowCount = quiet.b(xf.class);
        check(baseline.hookActive && baseline.increasePercent == 0 && baseline.limitedPasses > 0,
                "quiet world reaches the vanilla cap: " + baseline.status + " " + lowCount + "/" + baseline.vanillaCap
                        + " (inserted " + baseline.successfulSpawns + ", skylight subtraction " + quiet.f + ")");
        check(territory.snapshot(quiet, visitor).localActivity == 0, "quiet region has no history");

        xd occupied = open(root, "occupied", 18000);
        yw resident = join(occupied);
        territory.bind(occupied);
        pressure.bind(occupied);
        for (int tick = 0; tick < 4800; tick++) territory.tick(resident, false);
        check(territory.snapshot(occupied, resident).presence == 24, "qualified presence saturates");
        occupied.z.a(); // MapStorage saves the production ledger through the native save handler.
        NaturalSpawnPressure.Snapshot elevated = populate(occupied, pressure);
        int highCount = occupied.b(xf.class);
        check(elevated.increasePercent == 70 && elevated.limitedPasses > 0, "occupied world reaches boosted cap");
        check(highCount >= lowCount * 3 / 2 && highCount <= elevated.effectiveCap + 4,
                "actual hostile density is substantially higher and bounded");
        check(elevated.successfulSpawns >= lowCount * 3 / 2, "primary vanilla insertions explain the difference");
        System.out.printf("PASS natural spawning at night: quiet %d (cap %d), occupied %d (cap %d)%n",
                lowCount, baseline.vanillaCap, highCount, elevated.effectiveCap);

        xd reopened = open(root, "occupied", 6000);
        yw returned = join(reopened);
        territory.bind(reopened);
        pressure.bind(reopened);
        check(territory.snapshot(reopened, returned).presence == 24, "native ledger reload");
        check(populate(reopened, pressure).effectiveCap == elevated.effectiveCap && reopened.b(yy.class) == 0,
                "daylight blocks light-sensitive hostiles at high activity");
        vf.a(reopened, false, false);
        check(pressure.snapshot(reopened).count < 0, "world disables hostile spawning");
        returned.c(800, 4, 800, 0, 0);
        for (int tick = 0; tick < 3600; tick++) territory.tick(returned, false);
        returned.c(288, 4, 288, 0, 0);
        check(territory.snapshot(reopened, returned).presence == 21, "absence decay after reload");
        for (int tick = 0; tick < 400; tick++) territory.tick(returned, false);
        check(territory.snapshot(reopened, returned).returns == 2, "qualified return contribution");
        returned.c(800, 4, 800, 0, 0);
        for (int tick = 0; tick < 12000; tick++) territory.tick(returned, false);
        returned.c(288, 4, 288, 0, 0);
        vf.a(reopened, true, false);
        NaturalSpawnPressure.Snapshot decayed = pressure.snapshot(reopened);
        check(decayed.effectiveCap < elevated.effectiveCap && decayed.effectiveCap > decayed.vanillaCap,
                "absence lowers the actual cap on return");

        NaturalSpawnPressure disabled = new NaturalSpawnPressure(territory, 0);
        disabled.register();
        disabled.bind(reopened);
        vf.a(reopened, true, false);
        check(disabled.snapshot(reopened).effectiveCap == baseline.vanillaCap, "zero bonus restores the vanilla cap");
        System.out.println("PASS native light restrictions, disabled spawning, territorial reload, decay and return");
    }

    static xd open(File root, String name, long time) {
        xd world = new xd(new aia(root, name, false), name, SETTINGS);
        world.q = 2; // Normal difficulty.
        world.a(time);
        world.q(); // Refresh native skylight after setting the fixture time.
        world.r.setSeed(3981L);
        return world;
    }

    @SuppressWarnings("unchecked")
    static yw join(xd world) {
        yw player = new yw(world) {
            @Override
            public void ab() {}
        };
        player.c(288, 4, 288, 0, 0);
        world.d.add(player);
        return player;
    }

    static NaturalSpawnPressure.Snapshot populate(xd world, NaturalSpawnPressure pressure) {
        // The graphical client normally prepares the player's surrounding chunks.
        for (int x = 9; x <= 27; x++) {
            for (int z = 9; z <= 27; z++) world.d(x, z);
        }
        for (int pass = 0; pass < 5000; pass++) vf.a(world, true, false);
        return pressure.snapshot(world);
    }

    private static void check(boolean expected, String description) {
        if (!expected) throw new AssertionError(description);
    }
}
