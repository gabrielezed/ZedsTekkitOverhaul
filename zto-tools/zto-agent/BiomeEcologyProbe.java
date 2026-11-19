import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import javax.imageio.ImageIO;
import zed.tekkitoverhaul.agent.ZTOAgent;
import zed.tekkitoverhaul.survival.aerial.AerialPredators;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie.Pose;
import zed.tekkitoverhaul.survival.aerial.ForestWildlife;
import zed.tekkitoverhaul.survival.tactical.BiomeEcology;
import zed.tekkitoverhaul.survival.tactical.EnderkinEcology;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.EntityThief;

/** Exercises the pack's native biome layers, chunk biome bytes, substitution and magpie ticks. */
public final class BiomeEcologyProbe {
    private static final int[] BIOMES = {4, 1, 2, 6, 3, 5, 12, 21, 7, 16, 0, 14, 15};

    public static void main(String[] args) throws Exception {
        auditBiomes();
        File root = new File(args[0]);
        xd world = new xd(new aia(root, "biome-ecology", false), "biome-ecology", NaturalSpawnProbe.SETTINGS) {
            @Override public void a(nn entity, String sound, float volume, float pitch) {}
            @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        };
        world.q = 2;
        world.a(18000);
        world.q();
        Files.createFile(new File(root, "biome-ecology/.zto-disposable").toPath());
        for (int x = 14; x <= 22; ++x) for (int z = 14; z <= 22; ++z) world.d(x, z);
        compareDistributions(world);
        verifyUndergroundGeometry(world);
        compareNativeHabitats(world);
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        verifyWildlife(world);
    }

    private static void verifyUndergroundGeometry(xd world) {
        check(caveBonus(world, 360, 4, 1, true), "deep natural mine");
        check(caveBonus(world, 361, 76, 1, true), "high mountain tunnel");
        check(!caveBonus(world, 362, 4, 0, true), "sky-exposed ravine floor");
        check(!caveBonus(world, 363, 76, 0, true), "exposed cliff ledge");
        check(!caveBonus(world, 364, 4, 5, true), "built roof beneath natural terrain");
        check(!caveBonus(world, 365, 4, 1, false), "built floor under natural cover");
        xd nether = new xd(world, new akf());
        nether.d(22, 18);
        check(!caveBonus(nether, 360, 40, 1, true), "non-Overworld candidate retains native species");
        System.out.println("PASS deep mine, mountain tunnel, open ravine and ledge, built-interior geometry and other dimension");
    }

    private static boolean caveBonus(xd world, int x, int feet, int roof, boolean naturalFloor) {
        int z = 288;
        world.g(x, feet - 1, z, naturalFloor ? 1 : 5);
        if (roof != 0) {
            world.g(x, feet + 3, z, roof);
            for (int y = feet + 6; y <= feet + 8; ++y) world.g(x, y, z, 1);
        }
        ajg candidate = new ajg(world);
        candidate.c(x + 0.5, feet, z + 0.5, 0, 0);
        world.r = new Random(79) {
            @Override public int nextInt(int bound) { return bound == 1000 ? 185 : super.nextInt(bound); }
        };
        return EnderkinEcology.select(candidate, 0) != candidate;
    }

    private static void compareNativeHabitats(xd world) {
        yw player = NaturalSpawnProbe.join(world);
        for (int x = 9; x <= 27; ++x) for (int z = 9; z <= 27; ++z) paintChunk(world.d(x, z), 4);
        for (int x = 217; x < 273; ++x) for (int z = 256; z < 320; ++z)
            for (int y = 8; y <= 10; ++y) world.g(x, y, z, 1);
        world.g(240, 4, 280, 50);
        world.r = new Random(4134);
        int[] eligible = new int[2], selected = new int[2], inserted = new int[2];
        java.util.function.UnaryOperator<Object> observation = value -> {
            if (!(value.getClass() == ajg.class || value.getClass() == xr.class
                    || value.getClass() == yd.class || value.getClass() == jg.class)) return value;
            acq mob = (acq) value;
            check(mob.p != 4 || Math.abs(mob.o - 240.5) > 2 || Math.abs(mob.q - 280.5) > 2,
                    "native light gate rejects the lit underground interior before substitution");
            int habitat = cave(mob) ? 1 : 0;
            ++eligible[habitat];
            Object replacement = EnderkinEcology.select(mob, 0);
            if (replacement != mob) ++selected[habitat];
            return replacement;
        };
        try {
            ZTOAgent.registerNaturalSpawnReplacement(observation);
            for (int pass = 0; pass < 1800; ++pass) {
                vf.a(world, true, false);
                for (Object entity : world.b) if (entity instanceof zed.tekkitoverhaul.survival.tactical.TacticalHostile)
                    ++inserted[cave((acq) entity) ? 1 : 0];
                world.b(new ArrayList(world.b));
                world.n();
            }
            double surface = inserted[0] / (double) eligible[0], cave = inserted[1] / (double) eligible[1];
            System.out.printf("Native eligible surface %d, selected %d, inserted %d (%.2f%%); cave %d, selected %d, inserted %d (%.2f%%)%n",
                    eligible[0], selected[0], inserted[0], surface * 100, eligible[1], selected[1], inserted[1], cave * 100);
            check(eligible[0] >= 1000 && eligible[1] >= 500 && cave > surface * 2 && cave < 0.18,
                    "native eligible cave slots have substantially more successful substitutions");
        } finally {
            ZTOAgent.registerNaturalSpawnReplacement(null);
            world.d.remove(player);
            world.g(240, 4, 280, 0);
            for (int x = 217; x < 273; ++x) for (int z = 256; z < 320; ++z)
                for (int y = 8; y <= 10; ++y) world.g(x, y, z, 0);
        }
    }

    private static boolean cave(acq mob) {
        return mob.o >= 217 && mob.o < 273 && mob.q >= 256 && mob.q < 320 && mob.p < 8;
    }

    private static void auditBiomes() {
        String[] names = {"Ocean", "Plains", "Desert", "Extreme Hills", "Forest", "Taiga", "Swampland", "River",
            "Hell", "Sky", "FrozenOcean", "FrozenRiver", "Ice Plains", "Ice Mountains", "MushroomIsland",
            "MushroomIslandShore", "Beach", "DesertHills", "ForestHills", "TaigaHills", "Extreme Hills Edge",
            "Jungle", "JungleHills"};
        for (int id = 0; id < names.length; ++id) {
            check(abn.a[id].M == id && names[id].equals(abn.a[id].y), "registry slot " + id);
            check(BiomeEcology.family(id) != BiomeEcology.UNCLASSIFIED, "classified ID " + id);
        }
        check(BiomeEcology.family(23) == BiomeEcology.UNCLASSIFIED, "unknown IDs use a fallback");
        wp biomes = wp.a(32021L, vx.b)[0]; // RiverMix, before the final Voronoi refinement.
        boolean[] generated = new boolean[names.length];
        for (int x = -2048; x < 2048; x += 512) {
            for (int z = -2048; z < 2048; z += 512) {
                ad.a();
                int[] ids = biomes.a(x, z, 512, 512);
                for (int i = 0; i < 512 * 512; ++i) generated[ids[i]] = true;
            }
        }
        for (int id = 0; id < generated.length; ++id) {
            check(generated[id] == (id != 8 && id != 9), "default generation reachability for " + names[id]);
        }
        check(abn.a[14].a(acf.a).isEmpty() && abn.a[15].a(acf.a).isEmpty(), "native mushroom hostile lists empty");
        System.out.println("PASS 23 registered biomes, all 21 Overworld IDs sampled in the pinned default generator");
    }

    private static void compareDistributions(xd world) {
        ajg candidate = new ajg(world);
        candidate.c(288.5, 4, 288.5, 0, 0);
        int[] strongest = {1, 0, 0, 2, 0, 1, 0, 1, 0, 0, 0, -1, -1};
        for (int index = 0; index < BIOMES.length; ++index) {
            int id = BIOMES[index];
            paint(world, id);
            world.r = new Random(9821);
            int[] count = new int[5];
            for (int roll = 0; roll < 20000; ++roll) {
                Object replacement = EnderkinEcology.select(candidate, 0);
                if (replacement instanceof EntitySkirmisher) ++count[0];
                else if (replacement instanceof EntityBreacher) ++count[1];
                else if (replacement instanceof EntityThief) ++count[2];
                else if (replacement instanceof EntityTank) ++count[3];
                else if (replacement instanceof EntitySaboteur) ++count[4];
                else check(replacement == candidate, "ordinary fallback preserves the validated entity");
            }
            int total = Arrays.stream(count).sum();
            System.out.printf("%s %s (%d total)%n", abn.a[id].y, Arrays.toString(count), total);
            if (strongest[index] < 0) check(total == 0, "mushroom candidate untouched");
            else {
                for (int role = 0; role < 4; ++role) {
                    if (role != strongest[index]) check(count[strongest[index]] > count[role], "dominant role in " + id);
                }
                check(total >= 680 && total <= 890 && count[3] <= 0.13 * total, "comparable budgets and rare Tank");
            }
        }
        paint(world, 4);
        String interior = BiomeEcology.sample(world, 288, 288).weights();
        paintChunk(world.d(19, 18), 2);
        BiomeEcology.Profile edge = BiomeEcology.sample(world, 288, 288);
        check(edge.family == BiomeEcology.FOREST && !edge.weights().equals(interior), "neighbor influences an edge");
        check(BiomeEcology.sample(world, 8000, 8000) == null && !world.b(8000, 0, 8000, 8000, 0, 8000),
                "unloaded samples don't load chunks");
    }

    private static void verifyWildlife(xd world) throws Exception {
        yw player = NaturalSpawnProbe.join(world);
        ForestWildlife wildlife = new ForestWildlife(true);
        wildlife.bind(world);
        paint(world, 4);
        for (int i = 0; i < 100; ++i) wildlife.tick(player, false);
        check(wildlife.loaded() == 0, "night admission rejected");
        world.a(6000);
        world.q();
        paint(world, 1);
        for (int i = 0; i < 200; ++i) wildlife.tick(player, false);
        check(wildlife.loaded() == 0, "plains admission rejected");
        paint(world, 4);
        for (int i = 0; i < 2400; ++i) wildlife.tick(player, false);
        check(wildlife.loaded() == ForestWildlife.LOADED_CAP, "forest admission reaches and obeys the loaded cap");

        EntityMagpie bird = null;
        for (Object entry : world.b) if (entry instanceof EntityMagpie) {
            bird = (EntityMagpie) entry;
            break;
        }
        check(bird != null, "magpie inserted by native world");
        Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(bird, new Random(73123L) {
            @Override public int nextInt(int bound) { return bound == 5 ? 4 : super.nextInt(bound); }
        });
        bird.c(297.5, 4, 288.5, 0, 0);
        fq seed = new fq(world, bird.o + 0.4, 4.1, bird.q, new aan(295, 3, 0));
        check(world.a(seed), "dropped seed stack inserted");
        HashSet<Pose> poses = new HashSet<Pose>();
        boolean landed = false;
        for (int i = 0; i < 700; ++i) {
            if (i < 120 && i % 20 == 0) seed.c(bird.o + 0.4, bird.p + 0.1, bird.q, 0, 0);
            if (i == 120) seed.c(350, 4.1, 350, 0, 0);
            world.a(bird, true);
            poses.add(bird.pose());
            landed |= poses.contains(Pose.FLY) && bird.z;
        }
        check(poses.containsAll(Arrays.asList(Pose.WATCH, Pose.WALK, Pose.PECK, Pose.FLY, Pose.LAND)) && landed,
                "observed ground movement, curiosity, flight and landing: " + poses);
        check(!seed.G && seed.a.a == 3, "pecking leaves ownership and stack intact");
        ady saved = new ady();
        check(bird.c(saved) && saved.f("ZtoMagpieAge") == 700, "native named save with age");
        EntityMagpie resumed = (EntityMagpie) aao.a(saved, world);
        check(resumed != null && resumed.bb() == bird.bb() && resumed.pose() == Pose.WATCH, "reload restores bird without transient target");
        for (int x = 269; x <= 273; ++x) for (int z = 285; z <= 289; ++z) world.g(x, 5, z, 18);
        resumed.c(271.5, 6, 287.5, 0, 0);
        random.set(resumed, new Random(17) {
            @Override public int nextInt(int bound) { return bound == 10 ? 0 : super.nextInt(bound); }
        });
        check(world.a(resumed), "reloaded bird can be inserted");
        for (int i = 0; i < 40 && resumed.pose() != Pose.PERCH; ++i) world.a(resumed, true);
        check(resumed.pose() == Pose.PERCH && resumed.z, "bird rests on a leaf perch");
        check(ImageIO.read(EntityMagpie.class.getResource(EntityMagpie.TEXTURE)).getWidth() == 32,
                "original texture atlas packaged");
        verifyTravel(world, random);
        verifyAttention(world, random, player);
        player.c(288, 4, 288, 0, 0);
        EntityMagpie curious = directedBird(world, random, 284.5, 282.5, 0);
        fq feather = new fq(world, 284.9, 4.1, 282.5, new aan(288, 2, 0));
        check(world.a(feather), "dropped feather stack inserted");
        for (int i = 0; i < 10; ++i) world.a(curious, true);
        check(curious.pose() == Pose.PECK && curious.headYaw(1) < -20 && !feather.G && feather.a.a == 2,
                "stationary bird looks toward the nearby feather without consuming it");
        curious.A();

        bird.c(290, 4, 288, 0, 0);
        bird.a(md.a(player), 1);
        for (int i = 0; i < 35 && bird.pose() != Pose.FLY; ++i) world.a(bird, true);
        check(bird.pose() == Pose.FLY, "damage causes evasive takeoff");
        player.c(900, 4, 900, 0, 0);
        world.a(bird, true);
        check(bird.G, "ordinary distance despawn");
        System.out.println("PASS forest-only daylight placement and loaded cap, physical movement, attention, non-consuming curiosity, flight, evasion, despawn and native NBT");
    }

    private static EntityMagpie directedBird(xd world, Field random, double x, double z, int roll) throws Exception {
        EntityMagpie bird = new EntityMagpie(world);
        bird.c(x, 4, z, 0, 0);
        random.set(bird, new Random(17) {
            @Override public int nextInt(int bound) {
                if (bound == 10) return roll;
                if (bound == 111) return 55;
                if (bound == 5) return 4;
                return super.nextInt(bound);
            }
        });
        check(world.a(bird), "directed fixture bird inserted");
        return bird;
    }

    private static void verifyTravel(xd world, Field random) throws Exception {
        EntityMagpie flat = directedBird(world, random, 281.5, 292.5, 5);
        boolean positive = false, negative = false, stopped = false;
        for (int i = 0; i < 40; ++i) {
            double before = flat.q;
            float gait = flat.gait(1);
            world.a(flat, true);
            double distance = flat.q - before;
            if (distance > 0.06) {
                check(flat.z && flat.stride(1) > 0.5 && flat.gait(1) > gait, "applied ground travel drives gait");
                double swing = Math.sin(flat.gait(1));
                positive |= swing > 0.5;
                negative |= swing < -0.5;
            } else if (positive && flat.pose() == Pose.WATCH) {
                check(Math.abs(distance) < 1.0e-9 && flat.gait(1) == gait && flat.stride(1) == 0, "feet settle during a pause");
                stopped = true;
            }
        }
        check(positive && negative && stopped, "flat ground has an alternating gait and a settled pause");
        flat.A();

        verifyStep(world, random, 300, 1, false, false);
        verifyStep(world, random, 306, 2, false, false);
        verifyStep(world, random, 312, 3, false, false);
        verifyStep(world, random, 318, 2, true, false);
        verifyStep(world, random, 324, 2, false, true);
    }

    private static void verifyStep(xd world, Field random, int x, int rise, boolean canopy, boolean blockedLanding)
            throws Exception {
        for (int z = 294; z <= 296; ++z) {
            for (int column = x - 1; column <= x + 1; ++column) {
                for (int y = 4; y < 4 + rise; ++y) world.g(column, y, z, 3);
                if (blockedLanding) world.g(column, 4 + rise, z, 18);
            }
        }
        if (canopy) world.g(x, 5, 293, 18);
        EntityMagpie bird = directedBird(world, random, x + 0.5, 292.5, 5);
        boolean clear = rise <= 2 && !canopy && !blockedLanding;
        boolean hopped = false, climbed = false, redirected = false, landed = false;
        boolean settled = false;
        double peak = bird.p;
        int ticks = 0;
        while (ticks < 36 && !landed && !redirected) {
            ++ticks;
            world.a(bird, true);
            peak = Math.max(peak, bird.p);
            hopped |= bird.pose() == Pose.HOP;
            climbed |= bird.pose() == Pose.CLIMB;
            landed = bird.pose() == Pose.LAND && bird.z && bird.q > 294.25 && Math.abs(bird.p - (4 + rise)) < 0.02;
            settled |= Math.abs(bird.u) > 45 && bird.stride(1) == 0;
            redirected = settled && Math.abs(bird.o - (x + 0.5)) > 0.30 && bird.q < 294;
        }
        if (clear) check(landed && (rise == 1 ? hopped : climbed) && peak > 4 + rise,
                "bird crosses and lands on a " + rise + "-block step: " + bird.pose() + " @ " + bird.o + ", " + bird.p + ", " + bird.q);
        else check(redirected && !hopped && !climbed, "blocked path causes prompt ground redirection: "
                + rise + " / canopy " + canopy + " / landing " + blockedLanding + " @" + bird.o + ", " + bird.p + ", " + bird.q);
        check(clear || ticks <= 25, "ground redirection is prompt");
        System.out.printf("Magpie rise %d, canopy %b, blocked landing %b: %s at Y=%.2f (peak %.2f) in %d ticks%n",
                rise, canopy, blockedLanding, landed ? "landed" : "redirected", bird.p, peak, ticks);
        bird.A();
    }

    private static void verifyAttention(xd world, Field random, yw player) throws Exception {
        EntityMagpie bird = directedBird(world, random, 287.5, 278.5, 0);
        player.c(290.5, 4, 280.5, 0, 0);
        for (int i = 0; i < 9; ++i) world.a(bird, true);
        check(bird.pose() == Pose.WATCH && bird.headYaw(1) < -35 && Math.abs(bird.u) < 0.1,
                "stationary player draws the head independently of body heading");
        player.c(284.5, 4, 280.5, 0, 0);
        player.c(true);
        for (int i = 0; i < 14; ++i) {
            player.l = player.o;
            player.b(0.10, 0, 0);
            world.a(bird, true);
        }
        check(bird.pose() == Pose.WATCH && bird.headYaw(1) > 30 && Math.abs(bird.u) < 0.1,
                "sneaking approach draws the head without displacing or startling the bird");
        player.c(false);
        player.l = player.o;
        player.b(0.25, 0, 0);
        world.a(bird, true);
        check(bird.pose() == Pose.FLY, "quick close approach starts evasive flight");
        bird.A();
    }

    private static void paint(xd world, int biome) {
        for (int x = 15; x <= 21; ++x) for (int z = 15; z <= 21; ++z) paintChunk(world.d(x, z), biome);
    }

    private static void paintChunk(ack chunk, int biome) {
        byte[] ids = new byte[256];
        Arrays.fill(ids, (byte) biome);
        chunk.a(ids);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
