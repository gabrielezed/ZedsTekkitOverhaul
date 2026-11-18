import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.survival.aerial.AerialEcology;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntitySkelebird;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

public final class LunarAerialProbe {
    private static final long DUSK = 4 * 24000 + 13000;
    private static final long SEED = selectedSeed();

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Fixture normal = new Fixture(root, "moon-air", 2, false);
        normal.start();
        check(normal.director.snapshot(normal.world).aerialAllocation == 1, "Normal spends one lunar air slot");
        normal.assertBudget();
        for (int i = 1; i <= 350; ++i) normal.advance(DUSK + i);
        SiegeDirector.Snapshot first = normal.director.snapshot(normal.world);
        check(first.spawns.size() > 0, "ground waves still insert enemies: "
                + ((SiegeDirector.WaveSnapshot) first.waves.get(0)).rejections);
        normal.world.z.a();

        Fixture reload = new Fixture(root, "moon-air", 2, false);
        reload.calendar.rebaseline(0, DUSK + 350, LunarEvent.Cause.LOADED);
        reload.advance(DUSK + 351);
        check(reload.director.snapshot(reload.world).starts == 0 && reload.air.lunarMembers() == 0
                && reload.air.snapshot().cooldownSeconds > 0, "reopened full moon consumes the original group and keeps quiet period");

        normal.world.q = 3;
        normal.advance(DUSK + 351);
        check(normal.director.snapshot(normal.world).state == SiegeDirector.State.ABORTED
                && normal.air.lunarMembers() == 0, "difficulty change ends lunar air participation");

        Fixture hard = new Fixture(root, "moon-hard", 3, true);
        hard.start();
        check(hard.director.snapshot(hard.world).aerialAllocation == 2, "elevated Hard assault spends two slots");
        int skeletalMembers = 0;
        for (Object mob : hard.world.b) if (mob instanceof EntitySkelebird) ++skeletalMembers;
        check(skeletalMembers == 2,
                "a selected Hard moon can spend both existing slots on Skelebirds");
        hard.assertBudget();
        hard.player.c(347.5, 4, 288.5, 0, 0);
        for (int i = 1; i <= 20; ++i) hard.advance(DUSK + i);
        check(hard.air.lunarMembers() == 0 && hard.air.lunarStatus().contains("airspace"),
                "departure from the lunar airspace ends pursuit");

        Fixture death = new Fixture(root, "moon-death", 2, false);
        death.start();
        death.player.l(0);
        death.advance(DUSK + 1);
        check(death.air.lunarMembers() == 0 && death.director.snapshot(death.world).state == SiegeDirector.State.ABORTED,
                "player death ends the group with the assault");

        Fixture ended = new Fixture(root, "moon-end", 2, false);
        ended.start();
        ended.calendar.rebaseline(0, DUSK + 2, LunarEvent.Cause.DIMENSION_CHANGED);
        check(ended.air.lunarMembers() == 0, "calendar resync ends lunar participation");

        Fixture roof = new Fixture(root, "moon-roof", 3, true);
        roof.world.g(288, 7, 288, 20);
        roof.start();
        roof.assertBudget();
        check(roof.air.lunarMembers() == 0 && roof.air.lunarStatus().contains("cover"), "glass overhead prevents lunar air admission");

        Fixture easy = new Fixture(root, "moon-easy", 1, false);
        easy.start();
        easy.assertBudget();
        check(easy.air.lunarMembers() == 0, "Easy preserves the ground-only allocation");

        Fixture capped = new Fixture(root, "moon-cap", 3, true);
        for (int i = 0; i < 5; i++) {
            EntityAerialPredator existing = new EntityAerialPredator(capped.world);
            existing.c(283 + i, 16, 288, 0, 0);
            check(capped.world.a(existing), "loaded cap fixture");
        }
        capped.start();
        capped.assertBudget();
        check(capped.air.lunarMembers() == 0 && capped.air.lunarStatus().contains("cap"), "loaded aerial population blocks the air allocation");

        Fixture wildlife = new Fixture(root, "moon-wildlife", 2, false);
        wildlife.world.r = new Random(92L) {
            @Override public int nextInt(int bound) { return bound == 1000 ? 0 : super.nextInt(bound); }
        };
        for (int i = 0; i < 600; ++i) wildlife.air.tick(wildlife.player, false);
        check(wildlife.air.snapshot().activeMembers == 1, "natural encounter preceded the moon");
        wildlife.start();
        check(wildlife.air.snapshot().activeMembers == 1 && wildlife.air.lunarMembers() == 0
                && wildlife.director.snapshot(wildlife.world).aerialAllocation == 0,
                "active natural encounter and cooldown keep the lunar allocation on the ground");
        System.out.println("PASS lunar authority, Normal/Hard split, ground insertion, Easy, cover, cap, overlap, lifecycle and replay");
    }

    private static long selectedSeed() {
        for (long seed = 32021; seed < 34000; ++seed) {
            Random moon = new Random(seed ^ (4 * 341873128712L));
            if (moon.nextInt(100) < 34 && moon.nextInt(100) < 50) return seed;
        }
        throw new AssertionError("No selected fixture seed");
    }

    private static final class Fixture {
        final xd world;
        final yw player;
        final TerritorialActivityService territory = new TerritorialActivityService();
        final NaturalSpawnPressure spawning = new NaturalSpawnPressure(territory, 70, true);
        final AerialEcology air = new AerialEcology(true, true, 90, territory, spawning);
        final SiegeDirector director = new SiegeDirector(true, 15, 900, 8, 16, false, true, false);
        final LunarCalendar calendar = new LunarCalendar(1, 0, 0, Long.MIN_VALUE, () -> {}, error -> {
            throw (RuntimeException) error;
        });

        Fixture(File root, String name, int difficulty, boolean occupied) throws Exception {
            world = new xd(new aia(root, name, false), name, new fj(SEED, 0, false, false, vx.c)) {
                @Override public void a(nn entity, String sound, float volume, float pitch) {}
                @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
            };
            File marker = new File(root, name + "/.zto-disposable");
            if (!marker.exists()) Files.createFile(marker.toPath());
            world.q = difficulty;
            world.a(DUSK - 1);
            world.q();
            for (int x = 13; x <= 23; ++x) for (int z = 13; z <= 23; ++z) world.d(x, z);
            player = NaturalSpawnProbe.join(world);
            player.c(288.5, 4, 288.5, 0, 0);
            player.z = true;
            territory.bind(world);
            if (occupied) for (int i = 0; i < 4800; ++i) territory.tick(player, false);
            air.bind(world);
            spawning.bind(world);
            calendar.rebaseline(0, DUSK - 1, LunarEvent.Cause.LOADED);
            director.bind(world, calendar.view());
            director.attachAerialEcology(air);
        }

        void start() { advance(DUSK); }

        void advance(long time) {
            world.a(time);
            if (time % 20 == 0) world.q();
            calendar.observe(time);
            air.tick(player, false);
            director.tick(player, false, null);
        }

        void assertBudget() {
            SiegeDirector.Snapshot state = director.snapshot(world);
            int planned = state.aerialAllocation;
            for (Object wave : state.waves) planned += ((SiegeDirector.WaveSnapshot) wave).planned;
            check(planned == state.budget && state.starts == 1 && state.state == SiegeDirector.State.ACTIVE,
                    "ground plan plus air allocation equals the natural assault budget");
        }
    }

    private static void check(boolean expected, String description) {
        if (!expected) throw new AssertionError(description);
    }
}
