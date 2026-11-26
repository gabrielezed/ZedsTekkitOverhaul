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
        long seed = plan.region.identity;
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
                double margin = plan.radius[i];
                double shell = TrenchRegion.field(seed ^ 27, wx, wz, 9);
                double sediment = TrenchRegion.field(seed ^ 51, wx, wz, 24);
                if (margin > 0.87 && shell < 0.52) continue;
                int east = plan.region.seabed(wx + 2, wz, 47, plan.region.radius(wx + 2, wz));
                int south = plan.region.seabed(wx, wz + 2, 47, plan.region.radius(wx, wz + 2));
                int local = plan.region.seabed(wx, wz, 47, margin);
                boolean level = Math.abs(east - local) <= 2 && Math.abs(south - local) <= 2;
                MaterialType surface = level && sediment > 0.53 && y <= 38 ? MaterialType.SILT
                        : shell > 0.63 ? MaterialType.SHELLSTONE : MaterialType.STONE;
                int thickness = 2 + (int) (3 * sediment);
                for (int layer = 0; layer < thickness && y - layer > 7; layer++) {
                    if (!AbyssalWorldgen.geology(chunk.getBlockID(x, y - layer, z))) break;
                    MaterialType material = layer == 0 || (layer == 1 && shell > 0.72) ? surface : MaterialType.STONE;
                    put(chunk, x, y - layer, z, material);
                }
            }
        }
        formation(chunk, plan, seed);
        for (int z = 1; z < 15; z++) {
            for (int x = 1; x < 15; x++) {
                int y = plan.floor[(z << 4) | x];
                if (y == 0) continue;
                int wx = startX + x, wz = startZ + z;
                long roll = TrenchRegion.hash(seed ^ 7781, wx, wz);
                double ticket = TrenchRegion.unit(roll);
                double forest = TrenchRegion.field(seed ^ 764, wx, wz, 19);
                double colony = TrenchRegion.field(seed ^ 523, wx, wz, 9);
                if (y >= 23 && y <= 41 && forest > 0.56 && ticket < 0.30) {
                    double height = TrenchRegion.unit(TrenchRegion.hash(seed ^ 711, wx, wz));
                    MarineType.KELP.placeInNewChunk(chunk, x, y + 1, z,
                            height < 0.20 ? 1 : height < 0.55 ? 2 : height < 0.85 ? 3 : 4, 0);
                } else if (y >= 20 && colony > 0.59 && ticket < 0.22) {
                    MarineType type = ticket < 0.12 ? MarineType.ALGAE
                            : ticket < 0.18 ? MarineType.FAN_CORAL : MarineType.SPONGE;
                    type.placeInNewChunk(chunk, x, y + 1, z, 1, (int) roll);
                }
            }
        }
        int x = 3 + (int) ((seed ^ (startZ * 31L)) & 7);
        int z = 3 + (int) ((seed ^ (startX * 31L)) & 7);
        long light = TrenchRegion.hash(seed ^ 5719, chunk.xPosition, chunk.zPosition);
        int y = plan.floor[(z << 4) | x];
        if (y != 0 && y <= 20) {
            if ((light & 7) < 2 && MarineType.ANEMONE.placeInNewChunk(chunk, x, y + 1, z, 1, 0))
                light(chunk, x, y + 1, z, 3);
            else if ((light & 63) == 12 && chunk.getBlockID(x, y, z) == MaterialType.STONE.block().blockID) {
                put(chunk, x, y, z, MaterialType.LAMP);
                light(chunk, x, y, z, 5);
            }
        }
    }

    private static void formation(Chunk chunk, AbyssalWorldgen.Plan plan, long seed) {
        long roll = TrenchRegion.hash(seed ^ 9133, chunk.xPosition, chunk.zPosition);
        int ticket = (int) (roll & 127);
        if (ticket > 18) return;
        int x = 5 + (int) ((roll >>> 9) & 3), z = 5 + (int) ((roll >>> 12) & 3);
        int floor = plan.floor[(z << 4) | x];
        if (floor < 16 || floor > 40) return;
        boolean masonry = ticket < 2;
        boolean alongX = (roll & 256) != 0;
        for (int along = -2; along <= 2; along++) {
            for (int across = -1; across <= 1; across++) {
                int xx = x + (alongX ? along : across), zz = z + (alongX ? across : along);
                int y = plan.floor[(zz << 4) | xx];
                if (y == 0 || Math.abs(y - floor) > 2 || !coated(chunk.getBlockID(xx, y, zz))) continue;
                long variation = TrenchRegion.hash(roll, along, across);
                int height;
                if (masonry) {
                    if (across != 0 && (along != -2 || across < 0)) continue;
                    height = along == -2 ? 3 : ((variation & 3) == 0 ? 0 : 1);
                } else {
                    height = along == 0 && across == 0 ? 3 + (int) ((roll >>> 16) & 3)
                            : Math.max(0, 3 - Math.abs(along) - Math.abs(across) - (int) (variation & 1));
                }
                for (int dy = 1; dy <= height && water(chunk, xx, y + dy, zz); dy++) {
                    MaterialType material = masonry ? ((dy + variation & 3) == 0 ? MaterialType.BRICKS
                            : MaterialType.ERODED_BRICKS) : MaterialType.PETRIFIED_CORAL;
                    put(chunk, xx, y + dy, zz, material);
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
