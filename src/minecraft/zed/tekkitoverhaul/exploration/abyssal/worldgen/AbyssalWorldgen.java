package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.BiomeGenOcean;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkProviderGenerate;
import net.minecraft.src.World;
import net.minecraft.src.WorldChunkManager;
import net.minecraft.src.WorldProviderSurface;
import net.minecraft.src.WorldType;

/** The native biome layers are the unmodified ocean mask. New chunks own the generated identity. */
public final class AbyssalWorldgen {
    private static final int SEA_SURFACE = 62;
    // A floor is at most 50 blocks below the water line. A 16-block halo
    // contains the complete reach of the 4-rise-per-block envelope.
    private static final int HALO = 16;
    private static final int SIDE = 16 + HALO * 2;
    private static final int ORTHOGONAL_RISE = 4;
    private static final int DIAGONAL_RISE = 6;
    private static final int LOCATOR_STEP = 64;
    private static final int LOCATOR_RADIUS = 48;
    private static final int COAST_QUARTS = 8;
    private static final BiomeGenBase[] OPEN_OCEAN = new BiomeGenBase[0];
    private static final int COAST_SIDE = 4 + 2 * COAST_QUARTS;
    private static final Map<World, Map<Long, byte[]>> TERRAIN = new WeakHashMap<World, Map<Long, byte[]>>();

    private static synchronized Map<Long, byte[]> terrainCache(World world) {
        Map<Long, byte[]> result = TERRAIN.get(world);
        if (result == null) {
            result = new LinkedHashMap<Long, byte[]>(64, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
                    return size() > 64;
                }
            };
            TERRAIN.put(world, result);
        }
        return result;
    }
    private static final BiFunction<Object[], int[], Object> CALLBACK = new BiFunction<Object[], int[], Object>() {
        @Override public Object apply(Object[] objects, int[] coordinates) {
            return generate(objects, coordinates);
        }
    };
    private static BiomeGenBase biome;
    private static boolean active;

    private AbyssalWorldgen() { }

    public static void register(int id) {
        if (id < 0 || id >= 255 || BiomeGenBase.biomeList[id] != null)
            throw new IllegalArgumentException("[ZTO] biome.abyssalTrench=" + id
                    + " is unavailable. Restore the saved biome ID or choose a free ID for a fresh world.");
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            ClassLoader loader = ChunkProviderGenerate.class.getClassLoader();
            active = (Boolean) agent.getMethod("trenchActive", ClassLoader.class).invoke(null, loader);
            if (active) agent.getMethod("registerTrench", BiFunction.class).invoke(null, CALLBACK);
        } catch (ClassNotFoundException missing) {
            active = false;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("[ZTO] Install the matching ZTOAgent for Abyssal Trench generation.", failure);
        }
        if (!active) {
            System.err.println("[ZTO] Abyssal Trench generation inactive. Install the matching pinned Tekkit agent and client.");
            return;
        }
        biome = new BiomeGenOcean(id);
        biome.biomeName = "Abyssal Trench";
        biome.minHeight = BiomeGenBase.ocean.minHeight;
        biome.maxHeight = BiomeGenBase.ocean.maxHeight;
        biome.temperature = BiomeGenBase.ocean.temperature;
        biome.rainfall = BiomeGenBase.ocean.rainfall;
        biome.waterColorMultiplier = BiomeGenBase.ocean.waterColorMultiplier;
        biome.color = BiomeGenBase.ocean.color;
        biome.biomeDecorator = BiomeGenBase.ocean.biomeDecorator;
    }

    public static boolean isTrench(int id) { return biome != null && biome.biomeID == id; }

    static boolean supported(World world) {
        return active && world.worldProvider.getClass() == WorldProviderSurface.class
                && world.worldProvider.worldType == 0 && world.getWorldInfo().getTerrainType() == WorldType.DEFAULT;
    }

    /** Bounded planned geography only. Call on the thread owning the world. */
    public static TrenchRegion locate(World world, int x, int z) {
        if (!supported(world)) return null;
        int gx = Math.floorDiv(x, LOCATOR_STEP), gz = Math.floorDiv(z, LOCATOR_STEP);
        long seed = world.getSeed();
        WorldChunkManager manager = world.getWorldChunkManager();
        TrenchRegion best = null;
        long nearest = Long.MAX_VALUE;
        BiomeGenBase[] sample = null;
        for (int dz = -LOCATOR_RADIUS; dz <= LOCATOR_RADIUS; dz++) {
            for (int dx = -LOCATOR_RADIUS; dx <= LOCATOR_RADIUS; dx++) {
                int xx = (gx + dx) * LOCATOR_STEP + 8, zz = (gz + dz) * LOCATOR_STEP + 8;
                long distance = (long) (xx - x) * (xx - x) + (long) (zz - z) * (zz - z);
                if (distance >= nearest || TrenchRegion.influence(seed, xx, zz) < 0.82
                        || TrenchRegion.relief(seed, xx, zz) > 25) continue;
                sample = manager.getBiomeGenAt(sample, xx, zz, 1, 1, false);
                if (sample[0] != BiomeGenBase.ocean) continue;
                if (coastWeight(coast(manager, xx & ~15, zz & ~15), xx & 15, zz & 15) < 0.99) continue;
                best = new TrenchRegion(seed, xx, zz);
                nearest = distance;
            }
        }
        return best;
    }

    private static BiomeGenBase[] coast(WorldChunkManager manager, int x, int z) {
        BiomeGenBase[] samples = manager.getBiomesForGeneration(null, (x >> 2) - COAST_QUARTS,
                (z >> 2) - COAST_QUARTS, COAST_SIDE, COAST_SIDE);
        for (int i = 0; i < COAST_SIDE * COAST_SIDE; i++)
            if (samples[i] != BiomeGenBase.ocean) return samples;
        return OPEN_OCEAN;
    }

    private static double coastWeight(BiomeGenBase[] coast, int x, int z) {
        if (coast == OPEN_OCEAN) return 1;
        int nearest = 28 * 28;
        for (int dz = 0; dz < COAST_SIDE; dz++) {
            int zz = 4 * (dz - COAST_QUARTS) - z;
            if (zz * zz >= nearest) continue;
            for (int dx = 0; dx < COAST_SIDE; dx++) {
                int xx = 4 * (dx - COAST_QUARTS) - x;
                if (coast[dx + dz * COAST_SIDE] != BiomeGenBase.ocean)
                    nearest = Math.min(nearest, xx * xx + zz * zz);
            }
        }
        return TrenchRegion.smooth((Math.sqrt(nearest) - 8) / 20);
    }

    private static Object generate(Object[] objects, int[] coordinates) {
        if (coordinates != null) {
            World world = (World) objects[0];
            Plan plan = shape(world, (byte[]) objects[1], (BiomeGenBase[]) objects[2], coordinates[0], coordinates[1]);
            if (plan != null) plan.observatory = ObservatorySites.prepare(world, coordinates[0], coordinates[1]);
            return plan;
        }
        Chunk chunk = (Chunk) objects[0];
        Plan plan = (Plan) objects[1];
        AbyssalDecoration.decorate(chunk, plan, biome.biomeID);
        if (plan != null && plan.observatory != null) ObservatorySites.build(chunk, plan, plan.observatory);
        return null;
    }

    /** Samples exactly the same trench envelope without requesting a chunk from the world. */
    static Plan sample(World world, ChunkProviderGenerate provider, int cx, int cz) {
        byte[] raw = new byte[32768];
        provider.generateTerrain(cx, cz, raw);
        BiomeGenBase[] biomes = world.getWorldChunkManager().getBiomeGenAt(null, cx << 4, cz << 4, 16, 16, false);
        return shape(world, raw, biomes, cx, cz);
    }

    private static Plan shape(World world, byte[] terrain, BiomeGenBase[] biomes, int chunkX, int chunkZ) {
        if (!supported(world)) return null;
        int startX = chunkX << 4, startZ = chunkZ << 4;
        long seed = world.getSeed();
        boolean candidate = false;
        for (int z = 0; z < 16 && !candidate; z++) for (int x = 0; x < 16 && !candidate; x++)
            candidate = biomes[x + z * 16] == BiomeGenBase.ocean
                    && TrenchRegion.influence(seed, startX + x, startZ + z) >= 0.015;
        if (!candidate) return null;

        WorldChunkManager manager = world.getWorldChunkManager();
        BiomeGenBase[] ocean = manager.getBiomeGenAt(null, startX - HALO, startZ - HALO, SIDE, SIDE, false);
        int[] nativeFloor = new int[SIDE * SIDE];
        int[] desired = new int[SIDE * SIDE];
        double[] strength = new double[256];
        Map<Long, byte[]> density = terrainCache(world);
        ChunkProviderGenerate sampler = null;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int wx = startX + dx * 16, wz = startZ + dz * 16;
            BiomeGenBase[] shore = coast(manager, wx, wz);
            // Raw density sampling only. This never requests or populates a chunk.
            long key = ((long) (chunkX + dx) << 32) | ((chunkZ + dz) & 0xffffffffL);
            byte[] raw = density.get(key);
            if (raw == null) {
                if (sampler == null) sampler = new ChunkProviderGenerate(world, seed, false);
                raw = new byte[32768];
                sampler.generateTerrain(chunkX + dx, chunkZ + dz, raw);
                density.put(key, raw);
            }
            boolean owned = dx == 0 && dz == 0;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int index = (z + (dz + 1) * 16) * SIDE + x + (dx + 1) * 16;
                int offset = (x << 11) | (z << 7);
                BiomeGenBase nativeBiome = owned ? biomes[x + z * 16] : ocean[index];
                int floor = seabed(raw, offset);
                nativeFloor[index] = floor;
                double influence = nativeBiome == BiomeGenBase.ocean && raw[offset + SEA_SURFACE + 1] == 0
                        && isWater(raw[offset + SEA_SURFACE] & 255)
                        ? TrenchRegion.influence(seed, wx + x, wz + z) * coastWeight(shore, x, z) : 0;
                if (owned) strength[x + z * 16] = influence;
                int target = influence < 0.015 ? floor : Math.max(12, TrenchRegion.seabed(
                        TrenchRegion.relief(seed, wx + x, wz + z), floor, influence));
                // Keep every cut cell in continuous native geological substrate.
                int safe = floor;
                while (safe > target && geology(raw[offset + safe] & 255)
                        && geology(raw[offset + safe - 1] & 255)
                        && (!owned || (geology(terrain[offset + safe] & 255)
                        && geology(terrain[offset + safe - 1] & 255)))) safe--;
                desired[index] = safe;
            }
        }
        limitExcavation(desired);
        Plan plan = null;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int index = (z + HALO) * SIDE + x + HALO;
            int floor = nativeFloor[index], target = desired[index];
            if (target >= floor) continue;
            int offset = (x << 11) | (z << 7);
            if (plan == null) plan = new Plan(seed, desired, nativeFloor);
            int column = x + z * 16;
            plan.floor[column] = target;
            plan.influence[column] = strength[column];
            for (int y = target + 1; y <= floor; y++) terrain[offset + y] = (byte) Block.waterStill.blockID;
        }
        return plan;
    }

    private static int seabed(byte[] terrain, int offset) {
        int floor = SEA_SURFACE;
        while (floor > 8 && isWater(terrain[offset + floor] & 255)) floor--;
        return floor;
    }

    private static void limitExcavation(int[] heights) {
        // Two opposite sweeps compute the maximum of surrounding floors minus chamfer distance.
        for (int step = 1; step >= -1; step -= 2) {
            int start = step == 1 ? 0 : SIDE - 1;
            for (int z = start; z >= 0 && z < SIDE; z += step) {
                for (int x = start; x >= 0 && x < SIDE; x += step) {
                    int i = z * SIDE + x, xx = x - step, zz = z - step;
                    if (xx >= 0 && xx < SIDE)
                        heights[i] = Math.max(heights[i], heights[z * SIDE + xx] - ORTHOGONAL_RISE);
                    if (zz < 0 || zz >= SIDE) continue;
                    heights[i] = Math.max(heights[i], heights[zz * SIDE + x] - ORTHOGONAL_RISE);
                    if (x > 0) heights[i] = Math.max(heights[i], heights[zz * SIDE + x - 1] - DIAGONAL_RISE);
                    if (x < SIDE - 1) heights[i] = Math.max(heights[i], heights[zz * SIDE + x + 1] - DIAGONAL_RISE);
                }
            }
        }
    }

    static boolean isWater(int id) {
        return id == Block.waterStill.blockID || id == Block.waterMoving.blockID;
    }

    static boolean geology(int id) {
        return id == Block.stone.blockID || id == Block.dirt.blockID || id == Block.grass.blockID
                || id == Block.sand.blockID || id == Block.gravel.blockID || id == Block.sandStone.blockID;
    }

    static final class Plan {
        final long seed;
        final int[] floor = new int[256];
        final double[] influence = new double[256];
        ObservatorySites.Site observatory;

        private final int[] envelope, nativeFloor;

        Plan(long seed, int[] envelope, int[] nativeFloor) {
            this.seed = seed;
            this.envelope = envelope;
            this.nativeFloor = nativeFloor;
        }

        boolean loweredAt(int x, int z) {
            int i = (z + HALO) * SIDE + x + HALO;
            return envelope[i] < nativeFloor[i];
        }

        int seabedAt(int x, int z) {
            int i = (z + HALO) * SIDE + x + HALO;
            return Math.min(envelope[i], nativeFloor[i]);
        }
    }
}
