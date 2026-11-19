package zed.tekkitoverhaul.survival.tactical;

import java.util.Random;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.World;

/** Legacy Overworld habitat families. Weights apply only after an ordinary Enderkin slot is rolled. */
public enum BiomeEcology {
    OCEAN("Ocean", 42, 32, 20, 6, 0, 10),
    PLAINS("Plains", 62, 22, 13, 3, 1),
    DESERT("Desert", 72, 15, 10, 3, 2, 17),
    MOUNTAINS("Mountains", 49, 29, 12, 10, 3, 20),
    FOREST("Temperate Forest", 19, 42, 35, 4, 4, 18),
    TAIGA("Taiga", 20, 52, 22, 6, 5, 19),
    SWAMP("Swamp", 12, 36, 50, 2, 6),
    WATER_EDGE("Rivers and Coasts", 41, 32, 22, 5, 7, 16),
    SNOW("Snow and Ice", 48, 35, 13, 4, 11, 12, 13),
    MUSHROOM("Mushroom Island", 0, 0, 0, 0, 14, 15),
    JUNGLE("Jungle", 10, 48, 38, 4, 21, 22),
    OUTSIDE_OVERWORLD("Outside Overworld ecology", 0, 0, 0, 0, 8, 9),
    UNCLASSIFIED("Unclassified", 44, 32, 18, 6);

    private static final BiomeEcology[] LEGACY = new BiomeEcology[23];
    private static final int[] SAMPLE_X = {-16, 16, 0, 0};
    private static final int[] SAMPLE_Z = {0, 0, -16, 16};

    static {
        for (BiomeEcology family : values()) {
            for (int id : family.ids) LEGACY[id] = family;
        }
    }

    public final String label;
    private final int[] weights, ids;

    BiomeEcology(String label, int skirmisher, int breacher, int thief, int tank, int... ids) {
        this.label = label;
        this.weights = new int[] {skirmisher, breacher, thief, tank};
        this.ids = ids;
    }

    public static BiomeEcology family(int id) {
        return id >= 0 && id < LEGACY.length ? LEGACY[id] : UNCLASSIFIED;
    }

    public static Profile sample(World world, int x, int z) {
        BiomeGenBase biome = loadedBiome(world, x, z);
        return biome == null ? null : new Profile(world, x, z, biome);
    }

    static BiomeGenBase loadedBiome(World world, int x, int z) {
        return world.getChunkProvider().chunkExists(x >> 4, z >> 4) ? world.getBiomeGenForCoords(x, z) : null;
    }

    public static final class Profile {
        public final int biomeId;
        public final String biomeName;
        public final BiomeEcology family;
        private final int[] weights = new int[4];
        private int total;

        Profile(World world, int x, int z, BiomeGenBase biome) {
            biomeId = biome.biomeID;
            biomeName = biome.biomeName;
            family = world.worldProvider.worldType == 0 ? family(biomeId) : OUTSIDE_OVERWORLD;
            if (family == MUSHROOM || family == OUTSIDE_OVERWORLD) return;
            add(family, 4);
            for (int i = 0; i < SAMPLE_X.length; ++i) {
                BiomeGenBase neighbor = loadedBiome(world, x + SAMPLE_X[i], z + SAMPLE_Z[i]);
                if (neighbor == null) continue;
                BiomeEcology adjacent = family(neighbor.biomeID);
                if (adjacent != MUSHROOM && adjacent != OUTSIDE_OVERWORLD) add(adjacent, 1);
            }
        }

        private void add(BiomeEcology sample, int influence) {
            for (int i = 0; i < weights.length; ++i) {
                int weight = sample.weights[i] * influence;
                weights[i] += weight;
                total += weight;
            }
        }

        public boolean allowsEnderkin() { return total > 0; }

        int choose(Random random) {
            int ticket = random.nextInt(total);
            for (int i = 0; i < weights.length; ++i) {
                ticket -= weights[i];
                if (ticket < 0) return i;
            }
            throw new IllegalArgumentException("Enderkin roll outside ordinary budget");
        }

        public String weights() {
            return weights[0] + " / " + weights[1] + " / " + weights[2] + " / " + weights[3];
        }
    }
}
