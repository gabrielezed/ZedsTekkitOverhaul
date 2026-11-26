import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.AbyssalWorldgen;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.TrenchRegion;

/** Native provider and Anvil fixture. Direct provider calls isolate ZTO from later population. */
public final class AbyssalWorldgenProbe {
    private static final long SEED = 32021L;
    private static final int RADIUS = 13;
    private static final int SIDE = (RADIUS * 2 + 1) * 16;

    public static void main(String[] args) {
        try {
            run(new File(args[0]), new File(args[1]));
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File root, File previews) throws Exception {
        UnderwaterEquipmentProbe.startFixture(root, "setup");
        ZtoConfig config = ZtoConfigLoader.load(new File(root, "config").toPath(), false, Logger.getLogger("trench"));
        AbyssalRegistry.register(config, 100);
        xd world = world(root, "trench", SEED);
        aly baseline = new aly(world, SEED, true);
        AbyssalWorldgen.register(config.abyssalId("biome.abyssalTrench"));
        TrenchRegion region = AbyssalWorldgen.locate(world, 0, 0);
        check(region != null, "locator found a planned trench");
        TrenchRegion again = AbyssalWorldgen.locate(world, 0, 0);
        check(again.identity == region.identity, "same seed and search yield same planned identity");
        xd otherWorld = world(root, "other", SEED + 1);
        TrenchRegion other = AbyssalWorldgen.locate(otherWorld, 0, 0);
        check(other == null || other.identity != region.identity, "other seed generates different geography");
        sampleFrequency(world, otherWorld, world(root, "third", -SEED));
        int cx = region.x >> 4, cz = region.z >> 4;
        int originX = (cx - RADIUS) << 4, originZ = (cz - RADIUS) << 4;
        int biome = config.abyssalId("biome.abyssalTrench");
        aiv disk = new aiv(new File(root, "trench"));
        // RAW native chunks generated with the callback temporarily unavailable.
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(null);
        ack nativeCenter = baseline.b(cx, cz);
        ack oldCenter = baseline.b(cx - 1, cz);
        oldCenter.a(8, 61, 8, 54, 0);
        hb chest = new hb();
        chest.a(0, new aan(264, 3, 0));
        oldCenter.a(8, 61, 8, chest);
        oldCenter.a(9, 61, 8, 3032, 0);
        disk.a(world, oldCenter);
        agp.a();
        java.lang.reflect.Field callback = AbyssalWorldgen.class.getDeclaredField("CALLBACK");
        callback.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Object[], int[], Object> generator =
                (java.util.function.BiFunction<Object[], int[], Object>) callback.get(null);
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(generator);

        System.out.printf(Locale.ROOT, "seed=%d center=%d,%d footprint=%.0fx%.0f bearing=%.1f origin=%d,%d%n",
                SEED, region.x, region.z, region.length, region.width, Math.toDegrees(region.angle), originX, originZ);
        BufferedImage depths = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        BufferedImage habitats = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        aly provider = new aly(world, SEED, true);
        aly reverse = new aly(world, SEED, true);
        Map<Long, Long> signatures = new HashMap<>();
        int[] counts = new int[12], levels = new int[64];
        int changed = 0, ordinary = 0, land = 0, deep = 0, dryCaveColumns = 0;
        long ordinarySample = 0, landSample = 0;
        int ordinaryDx = 0, ordinaryDz = 0, landDx = 0, landDz = 0;
        long normalTime = 0, trenchTime = 0;
        int normalChunks = 0, trenchChunks = 0;
        for (int dz = -RADIUS; dz <= RADIUS; dz++) for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            long started = System.nanoTime();
            ack chunk = provider.b(cx + dx, cz + dz);
            long elapsed = System.nanoTime() - started;
            signatures.put(key(dx, dz), signature(chunk));
            boolean affected = false, hasOcean = false, hasLand = false;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                boolean trench = (chunk.m()[z * 16 + x] & 255) == biome;
                int columnBiome = chunk.m()[z * 16 + x] & 255;
                int floor = 62;
                while (floor > 0 && transparentWater(chunk.a(x, floor, z))) floor--;
                int surface = chunk.a(x, floor, z);
                int color = columnBiome == 0 || trench
                        ? ((11 + floor) << 16) | ((22 + floor * 2) << 8) | (42 + floor * 3) : 0x6f7551;
                depths.setRGB((dx + RADIUS) * 16 + x, (dz + RADIUS) * 16 + z, color);
                int feature = 0;
                if (trench) {
                    affected = true;
                    changed++;
                    check(chunk.a(x, 0, z) == 7, "bedrock at Y0");
                    check(chunk.a(x, 62, z) == 9 || chunk.a(x, 62, z) == 8, "native ocean surface Y62");
                    check(chunk.a(x, 63, z) == 0, "air above the native water surface");
                    check(chunk.a(x, z, world.i()).M == biome, "native chunk biome query");
                    if (floor <= 20) deep++;
                    levels[floor]++;
                    boolean dry = false;
                    for (int y = floor + 1; y <= 62; y++) if (chunk.a(x, y, z) == 0) dry = true;
                    if (dry) dryCaveColumns++;
                    for (int y = 1; y < 62; y++) {
                        int id = chunk.a(x, y, z);
                        check(id == 0 || pb.m[id] != null, "block ID registered");
                        if (id < 3032 || id > 3043) continue;
                        counts[id - 3032]++;
                        if (id >= 3039 || y == floor) feature = id;
                        int data = chunk.c(x, y, z);
                        if (id >= 3039) {
                            check(data == 8 || id == 3041 && data == 9, "occupied water metadata");
                            check(transparentWater(chunk.a(x, y + 1, z)), "submerged marine organism");
                            int bottom = y;
                            while (chunk.a(x, bottom - 1, z) == 3039) bottom--;
                            check(y - bottom < 4 && chunk.a(x, bottom - 1, z) >= 3032 && chunk.a(x, bottom - 1, z) <= 3038, "rooted marine column");
                        } else {
                            check(data == 0, "solid metadata id=" + id + " data=" + data + " chunk=" + chunk.g + "," + chunk.h + " local=" + x + "," + y + "," + z);
                        }
                    }
                } else if (columnBiome == 0) {
                    ordinary++;
                    hasOcean = true;
                } else {
                    land++;
                    hasLand = true;
                }
                int[] colors = {0x496779, 0xa3977b, 0xbab39a, 0xd0b7ac, 0x936f5b, 0x78574b,
                        0x71ffd7, 0x368244, 0x64a747, 0xcc8064, 0xd0c489, 0x8ee8ed};
                habitats.setRGB((dx + RADIUS) * 16 + x, (dz + RADIUS) * 16 + z,
                        feature == 0 ? (trench ? 0x263a40 : color) : colors[feature - 3032]);
            }
            if (affected) {
                trenchChunks++;
                trenchTime += elapsed;
            } else {
                normalChunks++;
                normalTime += elapsed;
                if (hasLand && landSample == 0) {
                    landSample = signatures.get(key(dx, dz));
                    landDx = dx;
                    landDz = dz;
                }
                if (hasOcean && !hasLand && ordinarySample == 0) {
                    ordinarySample = signatures.get(key(dx, dz));
                    ordinaryDx = dx;
                    ordinaryDz = dz;
                }
            }
        }
        for (int dz = RADIUS; dz >= -RADIUS; dz--) for (int dx = RADIUS; dx >= -RADIUS; dx--)
            check(signatures.get(key(dx, dz)) == signature(reverse.b(cx + dx, cz + dz)), "reverse provider order");
        ack generated = provider.b(cx, cz);
        check(signature(nativeCenter) != signature(generated), "center differs from ordinary terrain");
        disk.a(world, generated);
        agp.a();
        check(signature(generated) == signature(disk.a(world, cx, cz)), "native Anvil round trip including extended IDs");
        ack retained = disk.a(world, cx - 1, cz);
        check(signature(oldCenter) == signature(retained) && retained.a(8, 61, 8) == 54
                && retained.a(9, 61, 8) == 3032, "pre-generation chunk preserved beside new terrain");
        hb retainedChest = (hb) retained.e(8, 61, 8);
        check(retainedChest != null && retainedChest.k_(0).c == 264 && retainedChest.k_(0).a == 3,
                "old TileEntity and inventory survive");
        verifyPopulation(root, region, biome);
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(null);
        check(ordinarySample != 0 && ordinarySample == signature(baseline.b(cx + ordinaryDx, cz + ordinaryDz)),
                "ordinary ocean matches unmodified provider");
        check(landSample != 0 && landSample == signature(baseline.b(cx + landDx, cz + landDz)),
                "land matches unmodified provider");
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(generator);
        check(dryCaveColumns < changed / 100, "continuous water above sampled seabed");
        check(changed > 2000 && deep > 500 && ordinary > 1000, "deep valley and ordinary ocean");
        for (int id = 3039; id <= 3043; id++) check(counts[id - 3032] > 0, "naturally generated marine ID " + id);
        for (int id : new int[] {3032, 3033, 3034, 3035}) check(counts[id - 3032] > 0, "naturally generated geology ID " + id);
        previews.mkdirs();
        ImageIO.write(depths, "png", new File(previews, "depth.png"));
        ImageIO.write(habitats, "png", new File(previews, "habitats.png"));
        System.out.printf(Locale.ROOT, "trenchColumns=%d deepColumns=%d ordinaryOceanColumns=%d otherBiomeColumns=%d%n", changed, deep, ordinary, land);
        System.out.println("dry interruptions above seabed " + dryCaveColumns);
        System.out.println("counts by id 3032-3043 " + Arrays.toString(counts));
        System.out.println("seabed heights " + Arrays.toString(levels));
        System.out.printf(Locale.ROOT, "mean provideChunk trench=%.3f ms (%d), background=%.3f ms (%d)%n",
                trenchTime / (trenchChunks * 1e6), trenchChunks, normalTime / (normalChunks * 1e6), normalChunks);
        System.out.println("PASS native provider, reverse order, high IDs, metadata, Anvil and prior chunk preservation");
    }

    private static void verifyPopulation(File root, TrenchRegion region, int biome) {
        BuildCraftCore.initialize();
        mod_BuildCraftEnergy energy = new mod_BuildCraftEnergy();
        BuildCraftEnergy.load();
        BuildCraftEnergy.initialize();
        mod_RedPowerWorld.initialize();
        cpw.mods.fml.common.FMLCommonHandler handler = cpw.mods.fml.common.FMLCommonHandler.instance();
        handler.registerWorldGenerator(mod_IC2.getInstance());
        handler.registerWorldGenerator(energy);
        handler.registerWorldGenerator(new mod_RedPowerWorld());
        xd populated = world(root, "populated", SEED);
        int cx = region.x >> 4, cz = region.z >> 4;
        for (int dz = -3; dz <= 3; dz++) for (int dx = -3; dx <= 3; dx++) populated.d(cx + dx, cz + dz);
        // BuildCraft's natural medium-deposit ticket is rare. Select a reproducible
        // native random stream just for this isolated intersection check.
        int oilX = cx << 4, oilZ = cz << 4;
        int oilSeed = 0;
        while (new java.util.Random(oilSeed).nextDouble() > 0.0015D) oilSeed++;
        buildcraft.energy.OilPopulate.rand = new java.util.Random(oilSeed);
        energy.generateSurface(populated, new java.util.Random(0), oilX, oilZ);
        int copper = ic2.api.Items.getItem("copperOre").c;
        int ic2 = 0, redPower = 0, oil = 0, coated = 0;
        for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
            ack chunk = populated.d(cx + dx, cz + dz);
            for (int y = 1; y < 64; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int id = chunk.a(x, y, z);
                if (id == copper) ic2++;
                if (id == RedPowerWorld.blockOres.bO) redPower++;
                if (id == BuildCraftEnergy.oilStill.bO || id == BuildCraftEnergy.oilMoving.bO) oil++;
                if (id == 3032 && (chunk.m()[z * 16 + x] & 255) == biome) coated++;
            }
        }
        check(coated > 0 && ic2 > 0 && redPower > 0 && oil > 0, "native population and targeted oil intersect generated Trench");
        System.out.printf(Locale.ROOT, "post-population abyssal stone=%d IC2 copper=%d RedPower ore=%d BuildCraft oil=%d%n",
                coated, ic2, redPower, oil);
    }

    private static void sampleFrequency(xd... worlds) throws Exception {
        java.lang.reflect.Method candidate = TrenchRegion.class.getDeclaredMethod("candidate", long.class, int.class, int.class);
        java.lang.reflect.Method eligible = AbyssalWorldgen.class.getDeclaredMethod("eligible", rs.class, TrenchRegion.class);
        candidate.setAccessible(true);
        eligible.setAccessible(true);
        for (xd world : worlds) {
            int admitted = 0, oceanic = 0;
            for (int z = -5; z <= 5; z++) for (int x = -5; x <= 5; x++) {
                TrenchRegion region = (TrenchRegion) candidate.invoke(null, world.v(), x, z);
                if (region == null) continue;
                admitted++;
                if ((Boolean) eligible.invoke(null, world.i(), region)) oceanic++;
            }
            System.out.printf(Locale.ROOT, "seed=%d admitted=%d/121 eligible=%d/121%n", world.v(), admitted, oceanic);
        }
    }

    private static xd world(File root, String name, long seed) {
        return new xd(new aia(root, name, false), name, new fj(seed, 0, true, false, vx.b));
    }

    private static boolean transparentWater(int id) { return id == 8 || id == 9 || id >= 3039 && id <= 3043; }
    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    private static long signature(ack chunk) {
        long result = Arrays.hashCode(chunk.m());
        for (int y = 0; y < 128; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
            result = result * 1103515245 + 16 * chunk.a(x, y, z) + chunk.c(x, y, z);
        return result;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
