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
    private static java.util.function.BiFunction<Object[], int[], Object> capture;
    private static long SEED = 32021L;
    private static final int RADIUS = 24;
    private static final int SIDE = (RADIUS * 2 + 1) * 16;

    public static void main(String[] args) {
        try {
            if (args.length > 2) SEED = Long.parseLong(args[2]);
            run(new File(args[0]), new File(args[1]), args.length > 3 ? new File(args[3]) : null);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File root, File previews, File reference) throws Exception {
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

        final byte[][] terrain = new byte[2][];
        capture = (objects, coordinates) -> {
            if (coordinates == null) return generator.apply(objects, null);
            terrain[0] = ((byte[]) objects[1]).clone();
            Object plan = generator.apply(objects, coordinates);
            terrain[1] = ((byte[]) objects[1]).clone();
            return plan;
        };
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(capture);
        byte[] nativeFloors = new byte[SIDE * SIDE], shapedFloors = new byte[SIDE * SIDE];
        byte[] finalFloors = new byte[SIDE * SIDE], rejection = new byte[SIDE * SIDE];
        BufferedImage fieldPlan = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        java.lang.reflect.Method field = TrenchRegion.class.getDeclaredMethod("influence", long.class, int.class, int.class);
        java.lang.reflect.Method relief = TrenchRegion.class.getDeclaredMethod("relief", long.class, int.class, int.class);
        java.lang.reflect.Method coast = AbyssalWorldgen.class.getDeclaredMethod("coast", rs.class, int.class, int.class);
        java.lang.reflect.Method coastalWeight = AbyssalWorldgen.class.getDeclaredMethod("coastWeight", abn[].class, int.class, int.class);
        for (java.lang.reflect.Method method : new java.lang.reflect.Method[] {field, relief, coast, coastalWeight})
            method.setAccessible(true);
        System.out.printf(Locale.ROOT, "seed=%d anchor=%d,%d origin=%d,%d side=%d%n",
                SEED, region.x, region.z, originX, originZ, SIDE);
        BufferedImage depths = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        BufferedImage habitats = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        BufferedImage membership = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        BufferedImage eastWest = new BufferedImage(SIDE, 72, BufferedImage.TYPE_INT_RGB);
        BufferedImage northSouth = new BufferedImage(SIDE, 72, BufferedImage.TYPE_INT_RGB);
        aly provider = new aly(world, SEED, true);
        aly reverse = new aly(world, SEED, true);
        Map<Long, Long> signatures = new HashMap<>();
        int[] counts = new int[12], levels = new int[64];
        int changed = 0, ordinary = 0, land = 0, deep = 0, dryCaveColumns = 0;
        long ordinarySample = 0, landSample = 0;
        int ordinaryDx = 0, ordinaryDz = 0, landDx = 0, landDz = 0;
        long normalTime = 0, trenchTime = 0;
        int normalChunks = 0, trenchChunks = 0;
        String[] sceneNames = {"kelp", "fossils", "anemones", "relief", "quiet"};
        int[] bestScores = new int[sceneNames.length], bestX = new int[sceneNames.length];
        int[] bestZ = new int[sceneNames.length];
        for (int dz = -RADIUS; dz <= RADIUS; dz++) for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            long started = System.nanoTime();
            ack chunk = provider.b(cx + dx, cz + dz);
            long elapsed = System.nanoTime() - started;
            signatures.put(key(dx, dz), signature(chunk));
            boolean affected = false, hasOcean = false, hasLand = false;
            int kelp = 0, fossils = 0, anemones = 0, organisms = 0, columns = 0, low = 62, high = 0;
            Object shore = coast.invoke(null, world.i(), (cx + dx) << 4, (cz + dz) << 4);
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                boolean trench = (chunk.m()[z * 16 + x] & 255) == biome;
                int columnBiome = chunk.m()[z * 16 + x] & 255;
                int floor = 62;
                while (floor > 0 && transparentWater(chunk.a(x, floor, z))) floor--;
                int px = (dx + RADIUS) * 16 + x, pz = (dz + RADIUS) * 16 + z;
                int index = pz * SIDE + px, offset = (x << 11) | (z << 7);
                int before = rawFloor(terrain[0], offset), shaped = rawFloor(terrain[1], offset);
                nativeFloors[index] = (byte) before;
                shapedFloors[index] = (byte) shaped;
                finalFloors[index] = (byte) floor;
                rejection[index] = (byte) (trench ? 1 : columnBiome != 0 ? 2
                        : terrain[0][offset + 63] != 0 ? 3 : before > 58 ? 4 : 5);
                if (trench || columnBiome == 0) {
                    double influence = (Double) field.invoke(null, SEED, originX + px, originZ + pz);
                    double coastWeight = (Double) coastalWeight.invoke(null, shore, x, z);
                    double targetRelief = (Double) relief.invoke(null, SEED, originX + px, originZ + pz);
                    int intended = Math.min(before, (int) Math.round(before + (targetRelief - before) * influence * coastWeight));
                    fieldPlan.setRGB(px, pz, ((int) (255 * influence) << 16)
                            | ((int) (255 * coastWeight) << 8) | (intended * 4));
                }
                membership.setRGB(px, pz, trench ? 0x12b9b4 : columnBiome == 0 ? 0x17324c : 0x707955);
                if (pz == region.z - originZ) section(eastWest, px, chunk, x, z);
                if (px == region.x - originX) section(northSouth, pz, chunk, x, z);
                int color = columnBiome == 0 || trench
                        ? ((11 + floor) << 16) | ((22 + floor * 2) << 8) | (42 + floor * 3) : 0x6f7551;
                depths.setRGB((dx + RADIUS) * 16 + x, (dz + RADIUS) * 16 + z, color);
                int feature = 0;
                if (trench) {
                    affected = true;
                    columns++;
                    low = Math.min(low, floor);
                    high = Math.max(high, floor);
                    changed++;
                    check(chunk.a(x, 0, z) == 7, "bedrock at Y0");
                    check(chunk.a(x, 62, z) == 9 || chunk.a(x, 62, z) == 8, "native ocean surface Y62");
                    check(chunk.a(x, 63, z) == 0, "air above the native water surface");
                    check(chunk.a(x, z, world.i()).M == biome, "native chunk biome query");
                    if (floor <= 25) deep++;
                    levels[floor]++;
                    boolean dry = false;
                    for (int y = floor + 1; y <= 62; y++) if (chunk.a(x, y, z) == 0) dry = true;
                    if (dry) dryCaveColumns++;
                    for (int y = 1; y < 62; y++) {
                        int id = chunk.a(x, y, z);
                        check(id == 0 || pb.m[id] != null, "block ID registered");
                        if (id < 3032 || id > 3043) continue;
                        counts[id - 3032]++;
                        if (id == 3039) kelp++;
                        if (id == 3035) fossils++;
                        if (id == 3043) anemones++;
                        if (id >= 3039) organisms++;
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
                if (columns >= 192) {
                    int[] scores = {kelp, fossils, anemones, high - low,
                            organisms <= 4 && fossils <= 4 && high - low <= 3 ? 64 - high : 0};
                    for (int scene = 0; scene < scores.length; scene++) {
                        if (scores[scene] <= bestScores[scene]) continue;
                        bestScores[scene] = scores[scene];
                        bestX[scene] = cx + dx;
                        bestZ[scene] = cz + dz;
                    }
                }
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
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(generator);
        continuity(previews, originX, originZ, nativeFloors, shapedFloors, finalFloors, rejection);
        for (int dz = 13; dz >= -13; dz--) for (int dx = 13; dx >= -13; dx--)
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
        check(landSample == 0 || landSample == signature(baseline.b(cx + landDx, cz + landDz)),
                "land matches unmodified provider");
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(generator);
        check(dryCaveColumns < changed / 100, "continuous water above sampled seabed");
        check(changed > 2000 && deep > 100 && ordinary > 1000, "deep valley and ordinary ocean");
        check(counts[7] + counts[8] + counts[9] + counts[10] + counts[11] > 0, "naturally generated marine colonies");
        for (int id : new int[] {3032, 3033, 3034, 3035}) check(counts[id - 3032] > 0, "naturally generated geology ID " + id);
        previews.mkdirs();
        ImageIO.write(fieldPlan, "png", new File(previews, "field-plan.png"));
        ImageIO.write(membership, "png", new File(previews, "membership.png"));
        ImageIO.write(eastWest, "png", new File(previews, "cross-east-west.png"));
        ImageIO.write(northSouth, "png", new File(previews, "cross-north-south.png"));
        ImageIO.write(depths, "png", new File(previews, "depth.png"));
        ImageIO.write(habitats, "png", new File(previews, "habitats.png"));
        for (int scene = 0; scene < sceneNames.length; scene++) {
            String name = sceneNames[scene];
            File snapshot = new File(previews, name + ".zvox");
            if (reference != null) {
                File fixed = new File(reference, snapshot.getName());
                if (!fixed.isFile()) continue;
                int[] view = AbyssalSceneSnapshot.view(fixed);
                AbyssalSceneSnapshot.write(provider::b, snapshot, Arrays.copyOfRange(view, 0, 3), Arrays.copyOfRange(view, 3, 6));
            } else if (bestScores[scene] > 0) {
                int cxScene = bestX[scene], czScene = bestZ[scene];
                ack chunk = provider.b(cxScene, czScene);
                int x = (cxScene << 4) + 8, z = (czScene << 4) + 8;
                int y = surface(chunk, x, z);
                int[] target = {x, y + (scene == 0 ? 2 : 1), z};
                int[] camera = approach(provider, target, scene);
                AbyssalSceneSnapshot.write(provider::b, snapshot, camera, target);
                terrainProjection(chunk, new File(previews, name + "-projection.png"));
                System.out.printf("Scene %s seed=%d camera=%s target=%s score=%d%n",
                        name, SEED, Arrays.toString(camera), Arrays.toString(target), bestScores[scene]);
            }
        }
        System.out.printf(Locale.ROOT, "trenchColumns=%d deepColumns=%d ordinaryOceanColumns=%d otherBiomeColumns=%d%n", changed, deep, ordinary, land);
        System.out.printf(Locale.ROOT, "abyssal ocean coverage=%.2f%%%n", 100.0 * changed / (ordinary + changed));
        System.out.println("dry interruptions above seabed " + dryCaveColumns);
        System.out.println("counts by id 3032-3043 " + Arrays.toString(counts));
        System.out.println("seabed heights " + Arrays.toString(levels));
        System.out.printf(Locale.ROOT, "mean provideChunk trench=%.3f ms (%d), background=%.3f ms (%d)%n",
                trenchTime / (trenchChunks * 1e6), trenchChunks, normalTime / (normalChunks * 1e6), normalChunks);
        System.out.println("PASS native provider, reverse order, high IDs, metadata, Anvil and prior chunk preservation");
    }

    private static int surface(ack chunk, int x, int z) {
        int floor = 62;
        while (floor > 0 && transparentWater(chunk.a(x & 15, floor, z & 15))) floor--;
        return floor;
    }

    private static int[] approach(aly provider, int[] target, int scene) {
        int[] best = null;
        int bestScore = Integer.MIN_VALUE;
        int[][] views = {{4,-14}, {-4,14}, {-14,-5}, {14,5}, {11,-10}, {-11,10}};
        Map<Long, ack> chunks = new HashMap<>();
        for (int[] offset : views) {
            int dx = offset[0], dz = offset[1];
            if (scene == 2) { dx /= 2; dz /= 2; }
            int x = target[0] + dx, z = target[2] + dz;
            int floor = surface(chunks.computeIfAbsent(key(x >> 4, z >> 4), ignored -> provider.b(x >> 4, z >> 4)), x, z);
            int y = Math.min(60, Math.max(floor + 3, target[1] + (scene == 1 || scene == 3 ? -3 : 1)));
            int blocked = 0;
            for (int i = 1; i <= 8; i++) {
                int px = x - dx * i / 10, pz = z - dz * i / 10, py = y + (target[1] - y) * i / 10;
                ack chunk = chunks.computeIfAbsent(key(px >> 4, pz >> 4), ignored -> provider.b(px >> 4, pz >> 4));
                if (!transparentWater(chunk.a(px & 15, py, pz & 15))) blocked++;
            }
            int score = -blocked * 12 - Math.abs(y - target[1]) - (floor >= 60 ? 60 : 0);
            if (scene == 0 || scene == 2) {
                ack foreground = chunks.get(key(x >> 4, z >> 4));
                if (foreground.a(x & 15, y, z & 15) >= 3039) score -= 8;
            }
            if (score > bestScore) {
                bestScore = score;
                best = new int[] {x, y, z};
            }
        }
        return best;
    }

    private static int rawFloor(byte[] terrain, int offset) {
        int floor = 62;
        while (floor > 0 && (terrain[offset + floor] == 8 || terrain[offset + floor] == 9)) floor--;
        return floor;
    }

    private static void terrainProjection(ack chunk, File output) throws Exception {
        BufferedImage image = new BufferedImage(360, 380, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D draw = image.createGraphics();
        draw.setColor(new java.awt.Color(16, 35, 44));
        draw.fillRect(0, 0, image.getWidth(), image.getHeight());
        int[] palette = {0x496779, 0x777160, 0x929388, 0xb59686, 0x776b65, 0x635e56,
                0x9cd1b0, 0x64764a, 0x748553, 0xa07064, 0xbca982, 0x9ec4c4};
        for (int depth = 30; depth >= 0; depth--) for (int z = 0; z < 16; z++) {
            int x = depth - z;
            if (x < 0 || x >= 16) continue;
            for (int y = 12; y < 62; y++) {
                int id = chunk.a(x, y, z);
                if (id == 0 || id == 8 || id == 9) continue;
                int px = 180 + (x - z) * 10, py = 391 - depth * 4 - y * 5;
                int rgb = id >= 3032 && id <= 3043 ? palette[id - 3032] : 0x354952;
                java.awt.Color color = new java.awt.Color(rgb);
                if (id >= 3039 && id <= 3043) {
                    int width = id == 3039 ? 2 : 5;
                    draw.setColor(color);
                    draw.drawLine(px - width, py + 3, px + width, py - 3);
                    draw.drawLine(px, py + 5, px, py - 6);
                    continue;
                }
                draw.setColor(color.darker());
                draw.fillPolygon(new int[] {px - 10, px, px, px - 10},
                        new int[] {py, py + 4, py + 9, py + 5}, 4);
                draw.setColor(color);
                draw.fillPolygon(new int[] {px, px + 10, px + 10, px},
                        new int[] {py + 4, py, py + 5, py + 9}, 4);
                draw.setColor(color.brighter());
                draw.fillPolygon(new int[] {px - 10, px, px + 10, px},
                        new int[] {py, py - 4, py, py + 4}, 4);
            }
        }
        draw.dispose();
        ImageIO.write(image, "png", output);
    }

    private static void continuity(File previews, int originX, int originZ, byte[] nativeFloor, byte[] shaped,
                                   byte[] finished, byte[] reason) throws Exception {
        BufferedImage details = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        BufferedImage heights = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        BufferedImage heightDelta = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        int jumps = 0, seamJumps = 0, pillars = 0, maxExtra = 0, changedAfter = 0;
        int[] protectedEdges = new int[6];
        for (int z = 1; z < SIDE - 1; z++) for (int x = 1; x < SIDE - 1; x++) {
            int i = x + z * SIDE;
            int relief = shaped[i] - Math.max(Math.max(shaped[i - 1], shaped[i + 1]),
                    Math.max(shaped[i - SIDE], shaped[i + SIDE]));
            if (relief >= 12 && nativeFloor[i] - shaped[i] < 2) pillars++;
            if (finished[i] != shaped[i]) changedAfter++;
            int[] offsets = {1, SIDE};
            for (int offset : offsets) {
                int other = i + offset;
                int actual = Math.abs(shaped[i] - shaped[other]);
                int extra = actual - Math.abs(nativeFloor[i] - nativeFloor[other]);
                if ((reason[i] == 1 || reason[other] == 1) && extra > maxExtra) {
                    maxExtra = extra;
                    System.out.printf("jump sample x=%d z=%d native=%d/%d shaped=%d/%d final=%d/%d classes=%d/%d%n",
                            originX + x, originZ + z, nativeFloor[i], nativeFloor[other],
                            shaped[i], shaped[other], finished[i], finished[other], reason[i], reason[other]);
                }
                if (actual >= 12 && extra >= 8 && (reason[i] == 1 || reason[other] == 1)) {
                    jumps++;
                    if (offset == 1 ? (x & 15) == 15 : (z & 15) == 15) seamJumps++;
                    protectedEdges[reason[shaped[i] > shaped[other] ? i : other]]++;
                }
            }
            int[] colors = {0, 0x154e79, 0x777a4e, 0xef664c, 0xe8b848, 0x516672};
            details.setRGB(x, z, relief >= 12 ? 0xff00ff : colors[reason[i]]);
            int cut = Math.min(255, 6 * Math.max(0, nativeFloor[i] - shaped[i]));
            int later = Math.min(255, 15 * Math.abs(finished[i] - shaped[i]));
            heights.setRGB(x, z, (nativeFloor[i] * 4 << 16) | (shaped[i] * 4 << 8) | (finished[i] * 4));
            heightDelta.setRGB(x, z, later << 16 | cut << 8 | cut);
        }
        previews.mkdirs();
        ImageIO.write(heights, "png", new File(previews, "height-stages.png"));
        ImageIO.write(details, "png", new File(previews, "eligibility.png"));
        ImageIO.write(heightDelta, "png", new File(previews, "height-deltas.png"));
        System.out.printf("continuity jumps=%d chunkEdges=%d pillars=%d maxAddedStep=%d finalDiff=%d classes=%s%n",
                jumps, seamJumps, pillars, maxExtra, changedAfter, Arrays.toString(protectedEdges));
        boolean hasEnvelope = Arrays.stream(AbyssalWorldgen.class.getDeclaredMethods())
                .anyMatch(method -> "limitExcavation".equals(method.getName()));
        if (hasEnvelope) check(jumps == 0 && pillars == 0, "no pathological excavation discontinuities");
    }

    private static void section(BufferedImage image, int column, ack chunk, int x, int z) {
        for (int y = 0; y < image.getHeight(); y++) {
            int id = chunk.a(x, y, z);
            int color = id == 0 ? 0xaac3ce : transparentWater(id) ? 0x163652
                    : id >= 3032 && id <= 3038 ? 0x927e70 : id == 7 ? 0x241e25 : 0x514951;
            if (id >= 3039 && id <= 3043) color = 0x49a77d;
            image.setRGB(column, image.getHeight() - y - 1, color);
        }
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
