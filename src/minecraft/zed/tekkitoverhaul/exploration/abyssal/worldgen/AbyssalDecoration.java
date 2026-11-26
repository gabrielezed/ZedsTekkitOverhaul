package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ExtendedBlockStorage;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry.MaterialType;
import zed.tekkitoverhaul.exploration.abyssal.MarineType;

final class AbyssalDecoration {
    private AbyssalDecoration() { }

    static void decorate(Chunk chunk, AbyssalWorldgen.Plan plan, int biomeId) {
        int startX = chunk.xPosition << 4, startZ = chunk.zPosition << 4;
        long seed = plan.seed;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (z << 4) | x, y = plan.floor[i];
                if (y == 0) continue;
                chunk.getBiomeArray()[i] = (byte) biomeId;
                if (!AbyssalWorldgen.geology(chunk.getBlockID(x, y, z)) || !water(chunk, x, y + 1, z)) {
                    plan.floor[i] = 0;
                    continue;
                }
                int wx = startX + x, wz = startZ + z;
                double shell = 0.55 * TrenchRegion.field(seed ^ 27, wx, wz, 12)
                        + 0.45 * TrenchRegion.field(seed ^ 217, wx, wz, 97);
                double sediment = 0.65 * TrenchRegion.field(seed ^ 51, wx, wz, 24)
                        + 0.35 * TrenchRegion.field(seed ^ 317, wx, wz, 68);
                double influence = plan.influence[i];
                if (influence < 0.18 && shell < 0.57) continue;
                boolean level = slope(plan, x, z, y) <= 2;
                MaterialType surface = level && sediment > (y < 28 ? 0.49 : 0.57) ? MaterialType.SILT
                        : shell > 0.60 ? MaterialType.SHELLSTONE : MaterialType.STONE;
                int thickness = 2 + (int) (3 * sediment);
                for (int layer = 0; layer < thickness && y - layer > 7; layer++) {
                    if (!AbyssalWorldgen.geology(chunk.getBlockID(x, y - layer, z))) break;
                    MaterialType material = layer == 0 || (layer == 1 && shell > 0.72) ? surface : MaterialType.STONE;
                    put(chunk, x, y - layer, z, material);
                }
            }
        }
        coatCliffs(chunk, plan, startX, startZ);
        formation(chunk, plan, seed);
        for (int z = 1; z < 15; z++) {
            for (int x = 1; x < 15; x++) {
                int y = plan.floor[(z << 4) | x];
                if (y == 0) continue;
                int wx = startX + x, wz = startZ + z;
                long roll = TrenchRegion.hash(seed ^ 7781, wx, wz);
                double ticket = TrenchRegion.unit(roll);
                double forest = 0.64 * TrenchRegion.field(seed ^ 764, wx, wz, 34)
                        + 0.36 * TrenchRegion.field(seed ^ 813, wx, wz, 118);
                double colony = TrenchRegion.field(seed ^ 523, wx, wz, 9);
                boolean rocky = chunk.getBlockID(x, y, z) != MaterialType.SILT.block().blockID;
                if (y >= 23 && y <= 43 && forest > 0.54 && ticket < 0.30 && slope(plan, x, z, y) <= 3) {
                    double height = TrenchRegion.unit(TrenchRegion.hash(seed ^ 711, wx, wz));
                    MarineType.KELP.placeInNewChunk(chunk, x, y + 1, z,
                            height < 0.20 ? 1 : height < 0.55 ? 2 : height < 0.85 ? 3 : 4, 0);
                } else if (y >= 20 && colony > 0.56 && ticket < (rocky ? 0.24 : 0.16)) {
                    MarineType type = ticket < (rocky ? 0.08 : 0.12) ? MarineType.ALGAE
                            : rocky && ticket < 0.18 ? MarineType.FAN_CORAL : MarineType.SPONGE;
                    type.placeInNewChunk(chunk, x, y + 1, z, 1, (int) roll);
                }
            }
        }
        for (int z = 3; z <= 12; z++) for (int x = 3; x <= 12; x++) {
            int y = plan.floor[(z << 4) | x];
            if (y == 0 || y > 24) continue;
            long roll = TrenchRegion.hash(seed ^ 5719, startX + x, startZ + z);
            if ((roll & 2047) == 3 && MarineType.ANEMONE.placeInNewChunk(chunk, x, y + 1, z, 1, 0))
                light(chunk, x, y + 1, z, 3);
            else if ((roll & 8191) == 12 && chunk.getBlockID(x, y, z) == MaterialType.STONE.block().blockID) {
                put(chunk, x, y, z, MaterialType.LAMP);
                light(chunk, x, y, z, 5);
            }
        }
    }

    private static void coatCliffs(Chunk chunk, AbyssalWorldgen.Plan plan, int startX, int startZ) {
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int top = plan.seabedAt(x, z);
            int bottom = top;
            for (int side = 0; side < 4; side++) {
                int xx = x + (side == 0 ? -1 : side == 1 ? 1 : 0);
                int zz = z + (side == 2 ? -1 : side == 3 ? 1 : 0);
                int next = plan.seabedAt(xx, zz);
                if ((plan.loweredAt(x, z) || plan.loweredAt(xx, zz)) && next < top - 1 && (xx < 0 || xx > 15 || zz < 0 || zz > 15
                        || water(chunk, xx, Math.min(top, 61), zz))) bottom = Math.min(bottom, next);
            }
            if (top - bottom < 2 || !water(chunk, x, top + 1, z)) continue;
            double shell = TrenchRegion.field(plan.seed ^ 217, startX + x, startZ + z, 37);
            for (int y = Math.min(top, 61); y > Math.max(bottom, 8); y--) {
                if (AbyssalWorldgen.geology(chunk.getBlockID(x, y, z)))
                    put(chunk, x, y, z, shell > 0.67 ? MaterialType.SHELLSTONE : MaterialType.STONE);
            }
        }
    }

    private static int slope(AbyssalWorldgen.Plan plan, int x, int z, int y) {
        int steepest = 0;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            if (x + dx < 0 || x + dx > 15 || z + dz < 0 || z + dz > 15) continue;
            int neighbor = plan.floor[((z + dz) << 4) | (x + dx)];
            if (neighbor != 0) steepest = Math.max(steepest, Math.abs(neighbor - y));
        }
        return steepest;
    }

    private static void formation(Chunk chunk, AbyssalWorldgen.Plan plan, long seed) {
        int startX = chunk.xPosition << 4, startZ = chunk.zPosition << 4;
        int spacing = 23;
        for (int gz = Math.floorDiv(startZ - 6, spacing); gz <= Math.floorDiv(startZ + 21, spacing); gz++) {
            for (int gx = Math.floorDiv(startX - 6, spacing); gx <= Math.floorDiv(startX + 21, spacing); gx++) {
                long roll = TrenchRegion.hash(seed ^ 9133, gx, gz);
                int wx = gx * spacing + (int) ((roll >>> 8) % spacing);
                int wz = gz * spacing + (int) ((roll >>> 17) % spacing);
                double fossils = TrenchRegion.field(seed ^ 8821, wx, wz, 109);
                int ticket = (int) (roll & 127);
                if (ticket > 11 + fossils * 38) continue;
                boolean masonry = ticket < 3;
                int anchorX = wx - startX, anchorZ = wz - startZ;
                if (masonry && (anchorX < 3 || anchorX > 12 || anchorZ < 3 || anchorZ > 12)) continue;
                double angle = TrenchRegion.unit(TrenchRegion.hash(roll, 0, 0)) * Math.PI;
                double sin = Math.sin(angle), cos = Math.cos(angle);
                int extent = 2 + (int) ((roll >>> 28) & 3);
                int peak = 2 + (int) ((roll >>> 35) & 7);
                for (int z = Math.max(0, anchorZ - 6); z <= Math.min(15, anchorZ + 6); z++) {
                    for (int x = Math.max(0, anchorX - 6); x <= Math.min(15, anchorX + 6); x++) {
                        int y = plan.floor[(z << 4) | x];
                        if (y < 15 || y > 44 || !coated(chunk.getBlockID(x, y, z))) continue;
                        int dx = x - anchorX, dz = z - anchorZ;
                        double along = dx * cos + dz * sin, across = dz * cos - dx * sin;
                        int height;
                        if (masonry) {
                            if (Math.abs(across) >= 0.65 || Math.abs(along) >= 3) continue;
                            height = Math.abs(along) > 1.6 ? 3 : 1;
                        } else {
                            double branch = Math.abs(across - Math.sin(along) * (fossils + 0.35));
                            double falloff = Math.abs(along) / extent + branch / (1.3 + fossils * 1.8);
                            double rock = TrenchRegion.field(roll, startX + x, startZ + z, 3);
                            height = (int) Math.round(peak * (1 - falloff) + (rock - 0.5) * 2.5);
                        }
                        long variant = TrenchRegion.hash(roll, dx, dz);
                        for (int dy = 1; dy <= height && water(chunk, x, y + dy, z); dy++) {
                            MaterialType material = masonry ? (((dy + variant) & 3) == 0 ? MaterialType.BRICKS
                                    : MaterialType.ERODED_BRICKS) : MaterialType.PETRIFIED_CORAL;
                            put(chunk, x, y + dy, z, material);
                        }
                    }
                }
            }
        }
    }

    private static boolean coated(int id) {
        return id == MaterialType.STONE.block().blockID || id == MaterialType.SILT.block().blockID
                || id == MaterialType.SHELLSTONE.block().blockID;
    }

    private static boolean water(Chunk chunk, int x, int y, int z) {
        return AbyssalWorldgen.isWater(chunk.getBlockID(x, y, z)) && chunk.getBlockMetadata(x, y, z) == 0;
    }

    private static void put(Chunk chunk, int x, int y, int z, MaterialType material) {
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        // Each write replaces a non-air water or geological cell in an allocated section.
        section.setExtBlockID(x, y & 15, z, material.block().blockID);
        section.setExtBlockMetadata(x, y & 15, z, 0);
    }

    private static void light(Chunk chunk, int x, int y, int z, int level) {
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        if (section == null || section.getExtBlocklightValue(x, y & 15, z) >= level) return;
        section.setExtBlocklightValue(x, y & 15, z, level);
        if (level <= 1) return;
        for (int side = 0; side < 6; side++) {
            int xx = x + (side == 0 ? -1 : side == 1 ? 1 : 0);
            int yy = y + (side == 2 ? -1 : side == 3 ? 1 : 0);
            int zz = z + (side == 4 ? -1 : side == 5 ? 1 : 0);
            if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
            int attenuation = Math.max(1, chunk.getBlockLightOpacity(xx, yy, zz));
            if (level > attenuation) light(chunk, xx, yy, zz, level - attenuation);
        }
    }
}
