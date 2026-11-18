import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import javax.imageio.ImageIO;
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
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        verifyWildlife(world);
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
        java.lang.reflect.Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(bird, new Random(73123L));
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
        check(poses.containsAll(Arrays.asList(Pose.WATCH, Pose.WALK, Pose.HOP, Pose.PECK, Pose.FLY)) && landed,
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

        bird.c(290, 4, 288, 0, 0);
        bird.a(md.a(player), 1);
        for (int i = 0; i < 35 && bird.pose() != Pose.FLY; ++i) world.a(bird, true);
        check(bird.pose() == Pose.FLY, "damage causes evasive takeoff");
        player.c(900, 4, 900, 0, 0);
        world.a(bird, true);
        check(bird.G, "ordinary distance despawn");
        System.out.println("PASS forest-only daylight placement and loaded cap, magpie poses, non-consuming curiosity, flight, evasion, despawn and native NBT");
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
