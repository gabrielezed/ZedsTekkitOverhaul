package zed.tekkitoverhaul.exploration.abyssal.worldgen;

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
    private static final int LOCATOR_STEP = 64;
    private static final int LOCATOR_RADIUS = 48;
    private static final int COAST_QUARTS = 8;
    private static final BiomeGenBase[] OPEN_OCEAN = new BiomeGenBase[0];
    private static final int COAST_SIDE = 4 + 2 * COAST_QUARTS;
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

    private static boolean supported(World world) {
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
        if (coordinates != null)
            return shape((World) objects[0], (byte[]) objects[1], (BiomeGenBase[]) objects[2],
                    coordinates[0], coordinates[1]);
        AbyssalDecoration.decorate((Chunk) objects[0], (Plan) objects[1], biome.biomeID);
        return null;
    }

    private static Plan shape(World world, byte[] terrain, BiomeGenBase[] biomes, int chunkX, int chunkZ) {
        if (!supported(world)) return null;
        int startX = chunkX << 4, startZ = chunkZ << 4;
        long seed = world.getSeed();
        BiomeGenBase[] coast = null;
        Plan plan = null;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int column = (z << 4) | x;
                if (biomes[column] != BiomeGenBase.ocean) continue;
                int blockX = startX + x, blockZ = startZ + z;
                double influence = TrenchRegion.influence(seed, blockX, blockZ);
                if (influence < 0.015) continue;
                if (coast == null) coast = coast(world.getWorldChunkManager(), startX, startZ);
                influence *= coastWeight(coast, x, z);
                if (influence < 0.015) continue;
                int offset = (x << 11) | (z << 7);
                if (terrain[offset + SEA_SURFACE + 1] != 0) continue;
                int floor = SEA_SURFACE;
                while (floor > 8 && isWater(terrain[offset + floor] & 255)) floor--;
                if (floor > SEA_SURFACE - 4 || !geology(terrain[offset + floor] & 255)) continue;
                int target = TrenchRegion.seabed(TrenchRegion.relief(seed, blockX, blockZ), floor, influence);
                if (target == floor || !geology(terrain[offset + target] & 255)) continue;
                if (plan == null) plan = new Plan(seed);
                plan.floor[column] = target;
                plan.influence[column] = influence;
                for (int y = target + 1; y <= floor; y++)
                    terrain[offset + y] = (byte) Block.waterStill.blockID;
            }
        }
        return plan;
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

        Plan(long seed) { this.seed = seed; }
    }
}
