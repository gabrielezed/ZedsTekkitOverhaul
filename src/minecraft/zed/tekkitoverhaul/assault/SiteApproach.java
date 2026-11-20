package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.PathPoint;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;

/** Samples cardinal, standing-height approaches. Each call has a fixed column and sky budget. */
final class SiteApproach {
    enum Objective { PRESSURE_PERIMETER, LIMITED_BREACH, PRESSURE_ENTRANCE, APPROACH_ENTRANCE }
    static final int MARGIN = 2, MAX_COLUMNS = 28, MAX_SKY_READS = 255;
    private static final int DEPTH = 14;
    private static final int[] DX = {-1, 1, 0, 0}, DZ = {0, 0, -1, 1};

    final PathPoint[] route;
    final Objective objective;
    final SiegeBarrier barrier;

    private SiteApproach(List<PathPoint> route, Objective objective, SiegeBarrier barrier) {
        this.route = route.toArray(new PathPoint[route.size()]);
        this.objective = objective;
        this.barrier = barrier;
    }

    PathPoint start() { return route[0]; }
    PathPoint goal() { return route[route.length - 1]; }

    static boolean exterior(Bounds b, int x, int z) {
        return x < b.minX - MARGIN || x > b.maxX + MARGIN
                || z < b.minZ - MARGIN || z > b.maxZ + MARGIN;
    }

    static final class Sampler {
        final World world;
        int columns, skyReads, heightReads;
        int breachChecks;
        String rejection, breachRejection;

        Sampler(World world) { this.world = world; }

        SiteApproach plan(Bounds part, Bounds envelope, int option, SiteSiege.Profile profile, boolean destructive) {
            int direction = option % 4, offset = option / 4;
            int dx = DX[direction], dz = DZ[direction];
            int x = dx < 0 ? part.minX : dx > 0 ? part.maxX : lane(part.minX, part.maxX, offset);
            int z = dz < 0 ? part.minZ : dz > 0 ? part.maxZ : lane(part.minZ, part.maxZ, offset);
            int sx = x + dx * DEPTH, sz = z + dz * DEPTH;
            if (!exterior(envelope, sx, sz)) {
                rejection = "INSIDE_EXCLUSION";
                return null;
            }
            if (!world.blockExists(sx, 64, sz)) {
                rejection = "APPROACH_UNLOADED";
                return null;
            }
            heightReads++;
            int y = world.getHeightValue(sx, sz);
            if (Math.abs(y - part.minY) > 1) {
                rejection = "SURFACE_OUT_OF_REACH";
                return null;
            }
            List<PathPoint> route = new ArrayList<PathPoint>();
            for (int i = DEPTH; i >= 0; i--) {
                int px = x + dx * i, pz = z + dz * i;
                if (!standing(px, y, pz)) {
                    if ("APPROACH_UNLOADED".equals(rejection) || route.size() < 7 || i > 4) return null;
                    if (i == 0 && profile != SiteSiege.Profile.PERIMETER) {
                        SiteApproach tactic = specialist(route, x, y, z, dx, dz, destructive);
                        if (tactic != null) return tactic;
                    }
                    return new SiteApproach(route, Objective.PRESSURE_PERIMETER, null);
                }
                route.add(new PathPoint(px, y, pz));
            }
            // An open doorway can lead one step into this part. The spawn remains outside the compound.
            boolean crosses = standing(x - dx, y, z - dz);
            if (crosses) route.add(new PathPoint(x - dx, y, z - dz));
            int id = world.getBlockId(x, y, z);
            boolean entrance = crosses && (id == Block.doorWood.blockID || id == Block.doorSteel.blockID
                    || id == Block.fenceGate.blockID || framed(x, y, z, dx, dz));
            return new SiteApproach(route, entrance ? Objective.APPROACH_ENTRANCE : Objective.PRESSURE_PERIMETER, null);
        }

        private boolean framed(int x, int y, int z, int dx, int dz) {
            return world.isBlockNormalCube(x - dz, y, z - dx) && world.isBlockNormalCube(x + dz, y, z + dx);
        }

        private SiteApproach specialist(List<PathPoint> route, int x, int y, int z, int dx, int dz,
                                        boolean destructive) {
            breachChecks++;
            SiegeBarrier entrance = SiegeBarrier.entrance(world, x, y, z, dx, dz);
            if (entrance != null) {
                if (world.difficultySetting < 2) breachRejection = "ENTRANCE_REQUIRES_NORMAL";
                else if (standing(x - dx, y, z - dz)) return new SiteApproach(route, Objective.PRESSURE_ENTRANCE, entrance);
                else breachRejection = rejection;
                return null;
            }
            SiegeBarrier barrier = SiegeBarrier.structure(world, x, y, z, dx, dz);
            if (barrier == null) return null;
            if (!destructive) breachRejection = "STRUCTURAL_BREACH_DISABLED";
            else if (world.difficultySetting < 3) breachRejection = "STRUCTURAL_BREACH_REQUIRES_HARD";
            else {
                List<PathPoint> staging = route.subList(0, route.size() - 1);
                for (PathPoint point : staging) {
                    if (standing(point, true)) continue;
                    breachRejection = rejection;
                    return null;
                }
                return new SiteApproach(staging, Objective.LIMITED_BREACH, barrier);
            }
            return null;
        }

        private static int lane(int min, int max, int offset) {
            int middle = min + (max - min) / 2;
            return offset == 0 ? middle : offset == 1 ? Math.max(min, middle - 2) : Math.min(max, middle + 2);
        }

        boolean validate(SiteApproach approach) {
            boolean wide = approach.objective == Objective.LIMITED_BREACH;
            for (PathPoint point : approach.route) {
                if (!standing(point, wide)) return false;
            }
            return true;
        }

        /** A clipped site needs additional verified escape space beyond its insertion. */
        boolean localExterior(SiteApproach approach) {
            PathPoint start = approach.start(), next = approach.route[1];
            int dx = start.xCoord - next.xCoord, dz = start.zCoord - next.zCoord;
            for (int i = 1; i <= 4; i++) {
                if (!standing(start.xCoord + dx * i, start.yCoord, start.zCoord + dz * i)) return false;
            }
            return standing(start.xCoord - dz, start.yCoord, start.zCoord + dx)
                    && standing(start.xCoord + dz, start.yCoord, start.zCoord - dx);
        }

        boolean standing(PathPoint point, boolean wide) {
            int x = point.xCoord, y = point.yCoord, z = point.zCoord;
            if (!wide) return standing(x, y, z);
            if (y > 252 || !sampleColumn(x, y, z)) {
                if (y > 252) rejection = "TANK_CLEARANCE";
                return false;
            }
            // The Tank spans a 3 x 3 footprint and four body cells at lane centers.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!world.isBlockSolidOnSide(x + dx, y - 1, z + dz, 1)) {
                        rejection = "TANK_CLEARANCE";
                        return false;
                    }
                    for (int dy = 0; dy < 4; dy++) {
                        if (world.getBlockId(x + dx, y + dy, z + dz) == 0) continue;
                        rejection = "TANK_CLEARANCE";
                        return false;
                    }
                }
            }
            return true;
        }

        boolean standing(int x, int y, int z) {
            if (!sampleColumn(x, y, z)) return false;
            if (!world.isBlockSolidOnSide(x, y - 1, z, 1)
                    || !clear(x, y, z, y) || !clear(x, y + 1, z, y)) {
                rejection = "ROUTE_OBSTRUCTED";
                return false;
            }
            return true;
        }

        private boolean sampleColumn(int x, int y, int z) {
            if (columns >= MAX_COLUMNS) {
                rejection = "PLANNER_BUDGET";
                return false;
            }
            columns++;
            if (y < 1 || y > 253 || x < -29999997 || x > 29999997 || z < -29999997 || z > 29999997
                    || !world.blockExists(x, y, z)
                    || !world.checkChunksExist(x - 2, y - 1, z - 2, x + 2, y + 1, z + 2)) {
                rejection = "APPROACH_UNLOADED";
                return false;
            }
            return true;
        }

        private boolean clear(int x, int y, int z, int feet) {
            int id = world.getBlockId(x, y, z);
            if (id == 0) return true;
            if (id == Block.fenceGate.blockID) return (world.getBlockMetadata(x, y, z) & 4) != 0;
            if (id != Block.doorWood.blockID && id != Block.doorSteel.blockID) return false;
            ArrayList<AxisAlignedBB> collisions = new ArrayList<AxisAlignedBB>();
            AxisAlignedBB body = AxisAlignedBB.getBoundingBox(x + 0.19, feet, z + 0.19,
                    x + 0.81, feet + 1.9, z + 0.81);
            Block.blocksList[id].getCollidingBoundingBoxes(world, x, y, z, body, collisions);
            // Closed doors also have a side placement whose plane misses a centered AABB.
            return (world.getBlockMetadata(x, feet, z) & 4) != 0 && collisions.isEmpty();
        }

        boolean openSky(PathPoint point) {
            if (!world.blockExists(point.xCoord, point.yCoord, point.zCoord)) {
                rejection = "APPROACH_UNLOADED";
                return false;
            }
            Chunk chunk = world.getChunkFromBlockCoords(point.xCoord, point.zCoord);
            for (int y = point.yCoord; y < 256; y++) {
                if (skyReads >= MAX_SKY_READS) {
                    rejection = "PLANNER_BUDGET";
                    return false;
                }
                skyReads++;
                if (chunk.getBlockID(point.xCoord & 15, y, point.zCoord & 15) != 0) {
                    rejection = "NOT_OPEN_TO_SKY";
                    return false;
                }
            }
            return true;
        }
    }
}
