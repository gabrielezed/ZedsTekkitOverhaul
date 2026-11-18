import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.imageio.ImageIO;
import zed.tekkitoverhaul.survival.aerial.AerialPredators;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator.Flight;
import zed.tekkitoverhaul.survival.aerial.client.RenderAerialPredator;

/** Runs the reobfuscated predator in a disposable flat world with the installed Tekkit classes. */
public final class AerialPredatorProbe {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        xd world = new xd(new aia(root, "aerial-predator", false), "aerial-predator", NaturalSpawnProbe.SETTINGS) {
            @Override public void a(nn entity, String sound, float volume, float pitch) {}
            @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        };
        java.nio.file.Files.createFile(new File(root, "aerial-predator/.zto-disposable").toPath());
        world.q = 2;
        world.a(18000);
        world.q();
        world.r.setSeed(3865L);
        for (int x = 14; x <= 22; x++) {
            for (int z = 14; z <= 22; z++) world.d(x, z);
        }
        yw player = NaturalSpawnProbe.join(world);
        player.aA = "Pilot";
        registryAndEgg(world, player);

        player.c(288.5, 20, 288.5, 0, 0);
        EntityAerialPredator hunter = place(world, 299, 28, 288.5, 0);
        boundsAndWindup(world, hunter, player);
        ady saved = new ady();
        check(hunter.c(saved), "named entity saves through vanilla");
        EntityAerialPredator reloaded = (EntityAerialPredator) aao.a(saved, world);
        check(reloaded != null && reloaded.bb() == hunter.bb() && reloaded.flight() == Flight.CRUISE
                && "-".equals(reloaded.targetName()) && Math.abs(reloaded.o - hunter.o) < 0.001,
                "vanilla reload restores identity, health and position and clears a committed hunt");

        int health = player.bb();
        waitFor(world, hunter, Flight.RECOVER, 45);
        check(player.bb() < health && Math.abs(player.s) < 0.001, "committed contact damages without lifting the pilot");
        double low = hunter.y.b;
        for (int i = 0; i < 36; i++) {
            world.a(hunter, true);
            check(hunter.flight() == Flight.RECOVER, "holds recovery interval");
        }
        check(hunter.y.b > low + 3, "recovery climbs");
        hunter.A();

        player.c(288.5, 20, 288.5, 0, 0);
        player.r = player.s = player.t = 0;
        EntityAerialPredator evaded = place(world, 299, 28, 288.5, 0);
        waitFor(world, evaded, Flight.DIVE, 225);
        double targetX = evaded.destinationX(), targetZ = evaded.destinationZ();
        player.c(288.5, 20, 294.5, 0, 0);
        health = player.bb();
        for (int i = 0; i < 40 && evaded.flight() == Flight.DIVE; i++) {
            check(evaded.destinationX() == targetX && evaded.destinationZ() == targetZ,
                    "commitment keeps its original endpoint");
            world.a(evaded, true);
        }
        check(evaded.flight() == Flight.RECOVER && player.bb() == health && "dive missed".equals(evaded.lastFailure()),
                "a lateral dodge defeats the fixed trajectory");
        for (int x = 287; x <= 290; x++) {
            for (int z = 293; z <= 296; z++) world.g(x, 25, z, 1);
        }
        waitFor(world, evaded, Flight.DISENGAGE, 10);
        check(evaded.lastExit().contains("cover") && player.bb() == health, "roof breaks the hunt during recovery");
        evaded.A();

        groundAndTerrain(world, player);
        EntityAerialPredator falling = place(world, 292, 24, 288.5, 0);
        check(falling.a(md.a(player), 20) && falling.flight() == Flight.FALL, "ordinary player damage kills the predator");
        double deathY = falling.p;
        for (int i = 0; i < 12; i++) world.a(falling, true);
        check(falling.p < deathY - 1, "a dead predator loses lift (vertical displacement " + (falling.p - deathY) + ")");
        check(world.a(reloaded), "reload can be inserted");
        world.q = 0;
        world.a(reloaded, true);
        check(reloaded.G, "Peaceful removes the predator");
        System.out.println("PASS aerial predator egg, renderer construction, atlas, flight, ground and airborne acquisition, "
                + "windup, committed impact, dodge, climb, shelter, obstacle avoidance, death fall and named NBT reload");
    }

    private static void registryAndEgg(xd world, yw player) throws Exception {
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        AerialPredators.registerEgg(22009);
        yr egg = yr.e[22265];
        aan stack = new aan(egg);
        Properties names = new Properties();
        FMLCommonHandler.instance().handleLanguageLoad(names, "en_US");
        check(names.containsValue("Aerial Predator Spawn Egg") && egg.c(), "localized two-pass egg");
        player.aT.d = true;
        check(egg.a(stack, player, world, 290, 3, 288, 1) && stack.a == 1, "Creative egg spawns without spending");
        check(!egg.a(stack, player, world, 290, 3, 288, 1), "egg rejects occupied space");
        EntityAerialPredator spawned = (EntityAerialPredator) world.a(EntityAerialPredator.class,
                wu.b(286, 4, 286, 294, 8, 292)).get(0);
        check(AerialPredators.ENTITY_NAME.equals(aao.b(spawned)), "native entity name");
        for (int tick = 0; tick < 32; tick++) world.a(spawned, true);
        check(spawned.y.b > 7, "egg hatch climbs from ground level");
        spawned.A();
        player.aT.d = false;

        Map<Class<? extends nn>, um> renderers = new HashMap<Class<? extends nn>, um>();
        RenderAerialPredator.register(renderers);
        check(renderers.get(EntityAerialPredator.class) instanceof RenderAerialPredator, "native renderer mapping");
        BufferedImage texture = ImageIO.read(EntityAerialPredator.class.getResource(EntityAerialPredator.TEXTURE));
        check(texture.getWidth() == 64 && texture.getHeight() == 64
                && texture.getRGB(13, 5) != texture.getRGB(10, 40), "readable model atlas");
    }

    private static void groundAndTerrain(xd world, yw player) throws Exception {
        player.c(288.5, 4, 288.5, 0, 0);
        player.z = true;
        EntityAerialPredator ground = place(world, 299, 16, 288.5, 0);
        // Force the low-probability night roll without altering detection or the attack state.
        seed(ground, new Random(17L) {
            @Override public int nextInt(int bound) { return bound == 9 ? 0 : super.nextInt(bound); }
        });
        waitFor(world, ground, Flight.PREPARE, 220);
        waitFor(world, ground, Flight.DISENGAGE, 110);
        check("attack budget".equals(ground.lastExit()), "ground hunt spends one attack opportunity");
        ground.A();

        player.aT.a = true;
        for (int z = 275; z <= 305; z++) {
            for (int y = 4; y < 21; y++) world.g(305, y, z, y >= 17 ? 18 : 1);
        }
        EntityAerialPredator flyer = place(world, 303.4, 15, 288.5, -90);
        double start = flyer.q;
        double previousYaw = flyer.u;
        int contacts = 0;
        for (int tick = 0; tick < 100; tick++) {
            world.a(flyer, true);
            if (flyer.A) contacts++;
            double turn = Math.IEEEremainder(flyer.u - previousYaw, 360);
            check(Math.abs(turn) <= 11.1, "steering bounds each turn");
            previousYaw = flyer.u;
        }
        check(contacts <= 2 && Math.abs(start - flyer.q) > 4, "cliff and canopy do not trap or repeatedly clip the flyer");
        flyer.A();
    }

    private static void boundsAndWindup(xd world, EntityAerialPredator mob, yw player) {
        int health = player.bb();
        waitFor(world, mob, Flight.PREPARE, 220);
        for (int i = 0; i < 17; i++) {
            world.a(mob, true);
            check(mob.flight() == Flight.PREPARE && player.bb() == health, "full preparation precedes damage");
        }
        waitFor(world, mob, Flight.DIVE, 2);
        check(player.bb() == health && mob.s < 0, "dive begins with downward movement and no remote damage");
    }

    private static EntityAerialPredator place(xd world, double x, double y, double z, float yaw) throws Exception {
        EntityAerialPredator mob = new EntityAerialPredator(world);
        seed(mob, new Random(3865L));
        mob.c(x, y, z, yaw, 0);
        check(world.a(mob), "loaded fixture spawn");
        return mob;
    }

    private static void seed(EntityAerialPredator mob, Random seed) throws Exception {
        Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(mob, seed);
    }

    private static void waitFor(xd world, EntityAerialPredator mob, Flight phase, int ticks) {
        for (int i = 0; i < ticks && mob.flight() != phase; i++) world.a(mob, true);
        check(mob.flight() == phase, "expected " + phase + ", saw " + mob.flight()
                + " [" + mob.lastFailure() + ", " + mob.lastExit() + "]");
    }

    private static void check(boolean expected, String description) {
        if (!expected) throw new AssertionError(description);
    }
}
