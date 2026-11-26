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
    private static final int LOCATOR_RADIUS = 4;
    private static final Map<World, Map<Long, TrenchRegion>> REGIONS = new WeakHashMap<World, Map<Long, TrenchRegion>>();
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
        int gx = Math.floorDiv(x, TrenchRegion.SPACING), gz = Math.floorDiv(z, TrenchRegion.SPACING);
        TrenchRegion best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int dz = -LOCATOR_RADIUS; dz <= LOCATOR_RADIUS; dz++) {
            for (int dx = -LOCATOR_RADIUS; dx <= LOCATOR_RADIUS; dx++) {
                TrenchRegion region = region(world, gx + dx, gz + dz);
                if (region == null) continue;
                double candidate = Math.hypot((double) x - region.x, (double) z - region.z);
                if (candidate < distance) {
                    best = region;
                    distance = candidate;
                }
            }
        }
        return best;
    }

    static boolean eligible(WorldChunkManager manager, TrenchRegion region) {
        BiomeGenBase[] sample = null;
        for (int dz = -224; dz <= 224; dz += 16) {
            for (int dx = -224; dx <= 224; dx += 16) {
                int x = region.x + dx, z = region.z + dz;
                if (region.radius(x, z) > 1.12) continue;
                sample = manager.getBiomeGenAt(sample, x, z, 1, 1, false);
                if (sample[0] != BiomeGenBase.ocean) return false;
            }
        }
        return true;
    }

    private static synchronized TrenchRegion region(World world, int gx, int gz) {
        Map<Long, TrenchRegion> cache = REGIONS.get(world);
        if (cache == null) {
            cache = new LinkedHashMap<Long, TrenchRegion>(64, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, TrenchRegion> entry) {
                    return size() > 128;
                }
            };
            REGIONS.put(world, cache);
        }
        long key = ((long) gx << 32) | (gz & 0xffffffffL);
        if (cache.containsKey(key)) return cache.get(key);
        TrenchRegion region = TrenchRegion.candidate(world.getSeed(), gx, gz);
        if (region != null && !eligible(world.getWorldChunkManager(), region)) region = null;
        cache.put(key, region);
        return region;
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
        TrenchRegion region = region(world, Math.floorDiv(startX, TrenchRegion.SPACING),
                Math.floorDiv(startZ, TrenchRegion.SPACING));
        if (region == null || Math.abs(region.x - startX - 8) > 233 || Math.abs(region.z - startZ - 8) > 233)
            return null;

        Plan plan = null;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int column = (z << 4) | x;
                if (biomes[column] != BiomeGenBase.ocean) continue;
                int blockX = startX + x, blockZ = startZ + z;
                double radius = region.radius(blockX, blockZ);
                if (radius >= 1) continue;
                int offset = (x << 11) | (z << 7);
                int floor = SEA_SURFACE;
                while (floor > 8 && isWater(terrain[offset + floor] & 255)) floor--;
                if (floor > SEA_SURFACE - 4 || !geology(terrain[offset + floor] & 255)) continue;
                int target = region.seabed(blockX, blockZ, floor, radius);
                if (target == floor || !geology(terrain[offset + target] & 255)) continue;
                if (plan == null) plan = new Plan(region);
                plan.floor[column] = target;
                plan.radius[column] = radius;
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
        final TrenchRegion region;
        final int[] floor = new int[256];
        final double[] radius = new double[256];

        Plan(TrenchRegion region) { this.region = region; }
    }
}
