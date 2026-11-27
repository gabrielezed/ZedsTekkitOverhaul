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
                if (plan.observatory != null && plan.observatory.coversColumn(wx, wz)) continue;
                long roll = TrenchRegion.hash(seed ^ 7781, wx, wz);
                double ticket = TrenchRegion.unit(roll);
                double forest = 0.72 * TrenchRegion.field(seed ^ 764, wx, wz, 42)
                        + 0.28 * TrenchRegion.field(seed ^ 813, wx, wz, 119);
                double opening = TrenchRegion.field(seed ^ 523, wx, wz, 12);
                double reef = TrenchRegion.field(seed ^ 5719, wx, wz, 19);
                int incline = slope(plan, x, z, y);
                boolean rocky = chunk.getBlockID(x, y, z) != MaterialType.SILT.block().blockID;
                if (y >= 23 && y <= 43 && forest > 0.55 && incline <= 3) {
                    double canopy = Math.min(1, (forest - 0.55) * 6);
                    if (opening > 0.31 && ticket < 0.12 + canopy * 0.35) {
                        int height = Math.min(4, 1 + (int) (canopy * 2 + opening * 2));
                        MarineType.KELP.placeInNewChunk(chunk, x, y + 1, z, height, 0);
                    } else if (ticket > 0.88 && opening < 0.53) {
                        MarineType.ALGAE.placeInNewChunk(chunk, x, y + 1, z, 1, 0);
                    }
                } else if (y <= 28 && rocky && reef > 0.75 && incline <= 4) {
                    if (ticket < 0.13 && MarineType.ANEMONE.placeInNewChunk(chunk, x, y + 1, z, 1, 0))
                        light(chunk, x, y + 1, z, 3);
                    else if (ticket >= 0.13 && ticket < 0.29)
                        MarineType.SPONGE.placeInNewChunk(chunk, x, y + 1, z, 1, 0);
                } else if (y >= 20 && opening > 0.64 && ticket < 0.19) {
                    MarineType type = rocky && ticket > 0.08 ? MarineType.FAN_CORAL : MarineType.ALGAE;
                    type.placeInNewChunk(chunk, x, y + 1, z, 1, (int) roll);
                }
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
            int neighbor = plan.seabedAt(x + dx, z + dz);
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
                boolean stack = !masonry && (roll & 768) == 256;
                int extent = 3 + (int) ((roll >>> 28) & 3);
                int peak = stack ? 9 + (int) ((roll >>> 35) & 3) : 2 + (int) ((roll >>> 35) & 7);
                for (int z = Math.max(0, anchorZ - 6); z <= Math.min(15, anchorZ + 6); z++) {
                    for (int x = Math.max(0, anchorX - 6); x <= Math.min(15, anchorX + 6); x++) {
                        if (plan.observatory != null && plan.observatory.coversColumn(startX + x, startZ + z)) continue;
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
                            double ridge = Math.abs(along) / extent + branch / (1.3 + fossils * 1.8);
                            double spur = Math.abs(along + 1.4) / 3.8 + Math.abs(across + 0.55 * along) / 2.4;
                            double falloff = stack ? Math.hypot(along * 0.37, across * 0.57)
                                    : Math.min(ridge, spur + 0.24);
                            double rock = TrenchRegion.field(roll, startX + x, startZ + z, 3);
                            height = (int) Math.round(peak * (1 - falloff) + (rock - 0.5) * 2.5);
                        }
                        long variant = TrenchRegion.hash(roll, dx, dz);
                        for (int dy = 1; dy <= height && water(chunk, x, y + dy, z); dy++) {
                            MaterialType material = masonry ? (((dy + variant) & 3) == 0 ? MaterialType.BRICKS
                                    : MaterialType.ERODED_BRICKS) : stack && dy <= height / 2 ? MaterialType.STONE
                                    : dy == 1 || (y + dy) % 5 == 0 ? MaterialType.SHELLSTONE : MaterialType.PETRIFIED_CORAL;
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

    static void light(Chunk chunk, int x, int y, int z, int level) {
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
