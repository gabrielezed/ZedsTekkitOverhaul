import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Random;
import zed.tekkitoverhaul.survival.aerial.AerialPredators;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie;
import zed.tekkitoverhaul.survival.aerial.EntitySkelebird;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.EntityThief;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;

/** Checks intentional shoreline movement and native drowning in a loaded pool. */
public final class WaterBehaviorProbe {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Fixture world = new Fixture(root);
        Files.createFile(new File(root, "mob-water/.zto-disposable").toPath());
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        yw player = NaturalSpawnProbe.join(world);
        shoreline(world, player);
        drowning(world, player);
    }

    private static void shoreline(Fixture world, yw player) throws Exception {
        player.c(294.5, 4, 288.5, 0, 0);
        for (int x = 288; x <= 298; ++x) {
            for (int z = 278; z <= 298; ++z) {
                world.g(x, 3, z, 9);
            }
        }
        for (boolean flight : new boolean[] {false, true}) {
            EntityMagpie bird = new EntityMagpie(world);
            bird.c(flight ? 287.35 : 285.5, 4, 288.5, -90, 0);
            Field rng = nn.class.getDeclaredField("U");
            rng.setAccessible(true);
            rng.set(bird, new Random(73L) {
                @Override public int nextInt(int bound) {
                    if (bound == 10) return flight ? 9 : 5;
                    if (bound == 111) return 55;
                    return super.nextInt(bound);
                }
            });
            check(world.a(bird), "shoreline magpie inserted");
            double start = bird.o, furthest = bird.o;
            boolean tookFlight = false;
            for (int t = 0; t < 80; ++t) {
                tick(world, bird);
                check(!bird.H() && bird.o < 288, "magpie stays on the dry side of the lower water surface");
                tookFlight |= bird.pose() == EntityMagpie.Pose.FLY;
                furthest = Math.max(furthest, bird.o);
            }
            check(flight ? tookFlight : furthest > start + 0.75,
                    "the bird actually tried the shoreline route (" + flight + ", " + furthest + ")");
            bird.A();
            world.n();
        }

        for (TacticalHostile mob : enderkin(world)) {
            mob.c(287.4, 4, 288.5, -90, 0);
            mob.z = true;
            check(world.a(mob), "shoreline Enderkin inserted");
            check(mob.aM().a(new vu(new e[] {new e(289, 4, 288)}), 0.75F), "fixture injects a water-side waypoint");
            mob.a_(0, 1);
            check(mob.o == 287.4 && mob.aM().e(), "navigator cancels a step off the bank");
            mob.engageEncounterPlayer(player);
            for (int t = 0; t < 80; ++t) {
                player.l(20);
                tick(world, mob);
                check(!mob.H() && mob.o < 288, "the dry bank prevents a voluntary crossing");
            }
            mob.A();
            world.n();
        }

        world.g(288, 4, 288, 9);
        for (EntityAerialPredator bird : new EntityAerialPredator[] {new EntityAerialPredator(world), new EntitySkelebird(world)}) {
            bird.c(287.3, 4.1, 288.5, -90, 0);
            bird.r = 0.7;
            bird.a_(0, 0);
            check(bird.o == 287.3 && bird.lastFailure().contains("water"), "flight movement refuses a newly wet corridor");
        }
        System.out.println("PASS dry shoreline and wet flight corridor for magpie, both predators and five Enderkin");
    }

    private static void drowning(Fixture world, yw player) {
        player.c(296, 4, 304, 0, 0);
        for (int x = 301; x <= 307; ++x) {
            for (int z = 301; z <= 307; ++z) {
                for (int y = 4; y <= 10; ++y) {
                    boolean wall = x == 301 || x == 307 || z == 301 || z == 307;
                    world.g(x, y, z, wall || y == 10 ? 20 : 9);
                }
            }
        }
        EntityMagpie magpie = new EntityMagpie(world);
        magpie.c(304.5, 5, 304.5, 0, 0);
        ady saved = new ady();
        check(magpie.c(saved), "native magpie save");
        saved.a("ZtoMagpieBeak", new aan(295, 1, 0).b(new ady()));
        saved.a("ZtoMagpieAge", 5999);
        magpie = (EntityMagpie) aao.a(saved, world);
        check(magpie != null && magpie.carriedItem() != null, "fixture loads one carried seed");
        verifyDrowning(world, magpie);
        int seeds = 0;
        for (Object entry : world.b) {
            if (entry instanceof fq && !((fq) entry).G && ((fq) entry).a.c == 295) {
                seeds += ((fq) entry).a.a;
            }
        }
        check(seeds == 1 && magpie.carriedItem() == null, "the submerged death returns the beak stack once");
        for (EntityAerialPredator bird : new EntityAerialPredator[] {new EntityAerialPredator(world), new EntitySkelebird(world)}) {
            ady wildSave = new ady();
            check(bird.c(wildSave), "native predator save");
            wildSave.a("ZtoAerialWildTicks", 8);
            verifyDrowning(world, (EntityAerialPredator) aao.a(wildSave, world));
        }
        for (TacticalHostile mob : enderkin(world)) verifyDrowning(world, mob);

        EntityMagpie wading = new EntityMagpie(world);
        world.b(300, 4, 303, 8, 7); // Shallow flow reaches the feet, with the head above the liquid surface.
        wading.c(300.5, 4, 303.5, 0, 0);
        for (int t = 0; t < 340; ++t) wading.B();
        check(wading.H() && wading.Y() == 300 && wading.bb() == 4, "shallow contact leaves breathing available");
    }

    private static void verifyDrowning(Fixture world, acq mob) {
        mob.c(304.5, 5, 304.5, 0, 0);
        check(world.a(mob), "fixture mob inserted underwater");
        int health = mob.bb(), firstDamage = -1;
        for (int t = 1; t <= 1120 && !mob.G; ++t) {
            tick(world, mob);
            if (firstDamage < 0 && mob.bb() < health) firstDamage = t;
            if (t == 1) check(mob.H() && mob.Y() == 299, "water contact and native air decrement");
        }
        check(firstDamage == 320 && mob.bb() <= 0 && mob.G,
                mob.getClass().getSimpleName() + " must take native drowning damage and die (first damage " + firstDamage
                        + ", air " + mob.Y() + ", health " + mob.bb() + ")");
        System.out.println("PASS " + mob.getClass().getSimpleName() + " first drowning damage tick " + firstDamage + ", died");
        world.n();
    }

    private static TacticalHostile[] enderkin(xd world) {
        return new TacticalHostile[] {new EntitySkirmisher(world), new EntityBreacher(world), new EntityThief(world),
                new EntitySaboteur(world), new EntityTank(world)};
    }

    private static void tick(xd world, acq mob) {
        wu.b();
        bo.b();
        world.a(mob, true);
    }

    private static final class Fixture extends xd {
        Fixture(File root) {
            super(new aia(root, "mob-water", false), "mob-water", NaturalSpawnProbe.SETTINGS);
            q = 2;
            a(18000L);
            q();
            for (int x = 14; x <= 22; ++x) for (int z = 14; z <= 22; ++z) d(x, z);
        }

        @Override public void a(nn entity, String sound, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
