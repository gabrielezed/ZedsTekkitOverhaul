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
                int floor = y;
                while (y < 58 && coated(chunk.getBlockID(x, y + 1, z))) y++;
                double bend = (TrenchRegion.field(seed ^ 719, wx, wz, 63) - 0.5) * 28;
                double forest = 0.68 * TrenchRegion.field(seed ^ 764, wx + (int) bend, wz, 51)
                        + 0.32 * TrenchRegion.field(seed ^ 813, wx, wz, 121);
                double lead = Math.abs(TrenchRegion.field(seed ^ 523, wx, wz, 24) - 0.50);
                double reef = TrenchRegion.field(seed ^ 5719, wx, wz, 32);
                double patches = TrenchRegion.field(seed ^ 3203, wx, wz, 8);
                int incline = slope(plan, x, z, floor);
                boolean rocky = chunk.getBlockID(x, y, z) != MaterialType.SILT.block().blockID;
                if (y <= 34 && rocky && reef > 0.73 && incline <= 4 && patches > 0.48) {
                    // Short bright pockets sit below the rim or among low formations.
                    if (ticket < 0.25 && MarineType.ANEMONE.placeInNewChunk(chunk, x, y + 1, z, 1, 0))
                        light(chunk, x, y + 1, z, 3);
                    else if (ticket >= 0.25 && ticket < 0.58)
                        MarineType.SPONGE.placeInNewChunk(chunk, x, y + 1, z, 1, 0);
                } else if (floor >= 22 && floor <= 44 && y - floor <= 2 && forest > 0.52 && incline <= 3) {
                    double canopy = Math.min(1, (forest - 0.52) * 7);
                    double shoots = TrenchRegion.field(seed ^ 619, wx, wz, 6);
                    if (lead > 0.080 + 0.045 * patches && shoots > 0.34
                            && ticket < 0.08 + canopy * (0.22 + 0.34 * shoots)) {
                        double frond = TrenchRegion.unit(TrenchRegion.hash(roll, 7, 0));
                        int height = Math.min(4, 1 + (int) (canopy * 0.9 + shoots * 1.7 + frond * 1.5));
                        MarineType.KELP.placeInNewChunk(chunk, x, y + 1, z, height, 0);
                    } else if (ticket > 0.83 && lead < 0.13) {
                        MarineType.ALGAE.placeInNewChunk(chunk, x, y + 1, z, 1, 0);
                    }
                } else if (y >= 19 && patches > 0.75 && ticket < 0.22) {
                    MarineType type = rocky && ticket > 0.07 ? MarineType.FAN_CORAL : MarineType.ALGAE;
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
        int spacing = 37, reach = 15;
        for (int gz = Math.floorDiv(startZ - reach, spacing); gz <= Math.floorDiv(startZ + 15 + reach, spacing); gz++) {
            for (int gx = Math.floorDiv(startX - reach, spacing); gx <= Math.floorDiv(startX + 15 + reach, spacing); gx++) {
                long roll = TrenchRegion.hash(seed ^ 9133, gx, gz);
                int wx = gx * spacing + (int) ((roll >>> 8) % spacing);
                int wz = gz * spacing + (int) ((roll >>> 17) % spacing);
                int anchorX = wx - startX, anchorZ = wz - startZ;
                if (anchorX < -reach || anchorX > 15 + reach || anchorZ < -reach || anchorZ > 15 + reach
                        || !plan.loweredAt(anchorX, anchorZ)) continue;
                int footing = plan.seabedAt(anchorX, anchorZ);
                if (footing < 15 || footing > 43) continue;
                double fossils = TrenchRegion.field(seed ^ 8821, wx, wz, 109);
                int ticket = (int) (roll & 127);
                if (ticket > 28 + fossils * 42) continue;
                boolean escarpment = ticket < 19;
                double angle = TrenchRegion.unit(TrenchRegion.hash(roll, 0, 0)) * Math.PI;
                double sin = Math.sin(angle), cos = Math.cos(angle);
                int length = 10 + (int) ((roll >>> 28) & 3);
                int relief = (escarpment ? 12 : 5) + (int) ((roll >>> 35) & 3);
                int dip = (roll & 256) == 0 ? -1 : 1;
                for (int z = Math.max(0, anchorZ - reach); z <= Math.min(15, anchorZ + reach); z++) {
                    for (int x = Math.max(0, anchorX - reach); x <= Math.min(15, anchorX + reach); x++) {
                        if (plan.observatory != null && plan.observatory.coversColumn(startX + x, startZ + z)) continue;
                        int y = plan.floor[(z << 4) | x];
                        if (y < 14 || y > 47 || !coated(chunk.getBlockID(x, y, z))) continue;
                        int dx = x - anchorX, dz = z - anchorZ;
                        double along = dx * cos + dz * sin, across = (dz * cos - dx * sin) * dip;
                        double taper = Math.abs(along) / length;
                        if (taper >= 1) continue;
                        double course = 1.7 * Math.sin(along / 4.5) + along * along / (length * 9.0);
                        double flank = across - course;
                        double body = Math.abs(flank) / (flank > 0 ? 3.1 : 6.8) + taper * taper;
                        if (!escarpment) {
                            double fork = Math.abs(flank - 0.65 * (along + 3)) / 2.7
                                    + Math.abs(along - 1) / (length * 0.9) + 0.12;
                            body = Math.min(body, fork);
                        }
                        if (body >= 1) continue;
                        double wear = TrenchRegion.field(roll, startX + x, startZ + z, 4);
                        int strata = (int) Math.floor((along + 1) / 4);
                        int crest = (int) (footing + relief * (1 - body) + strata * 0.6 + wear * 2);
                        // Each cell builds straight up from actual support, even on a chunk edge.
                        // The sampled crest is an absolute elevation shared by every owned slice.
                        int top = Math.min(Math.min(crest, 55), y + (escarpment ? 15 : 9));
                        for (int yy = y + 1; yy <= top && water(chunk, x, yy, z); yy++) {
                            int seam = Math.floorMod(yy + strata, 5);
                            MaterialType material = seam == 0 ? MaterialType.SHELLSTONE : MaterialType.STONE;
                            if (!escarpment && (seam == 1 || yy == top && wear > 0.60))
                                material = MaterialType.PETRIFIED_CORAL;
                            put(chunk, x, yy, z, material);
                        }
                    }
                }
            }
        }
    }

    private static boolean coated(int id) {
        return id == MaterialType.STONE.block().blockID || id == MaterialType.SILT.block().blockID
                || id == MaterialType.SHELLSTONE.block().blockID
                || id == MaterialType.PETRIFIED_CORAL.block().blockID;
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
