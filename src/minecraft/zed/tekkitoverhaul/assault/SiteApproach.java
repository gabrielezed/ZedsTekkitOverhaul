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
    static final int MARGIN = 2, MAX_COLUMNS = 28, MAX_SKY_READS = 255;
    private static final int DEPTH = 14;
    private static final int[] DX = {-1, 1, 0, 0}, DZ = {0, 0, -1, 1};

    final PathPoint[] route;
    final boolean entrance;

    private SiteApproach(List<PathPoint> route, boolean entrance) {
        this.route = route.toArray(new PathPoint[route.size()]);
        this.entrance = entrance;
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
        String rejection;

        Sampler(World world) { this.world = world; }

        SiteApproach plan(Bounds part, Bounds envelope, int option) {
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
                    return new SiteApproach(route, false);
                }
                route.add(new PathPoint(px, y, pz));
            }
            // An open doorway can lead one step into this part. The spawn remains outside the compound.
            boolean entrance = standing(x - dx, y, z - dz);
            if (entrance) route.add(new PathPoint(x - dx, y, z - dz));
            return new SiteApproach(route, entrance);
        }

        private static int lane(int min, int max, int offset) {
            int middle = min + (max - min) / 2;
            return offset == 0 ? middle : offset == 1 ? Math.max(min, middle - 2) : Math.min(max, middle + 2);
        }

        boolean validate(SiteApproach approach) {
            for (PathPoint point : approach.route) {
                if (!standing(point.xCoord, point.yCoord, point.zCoord)) return false;
            }
            return true;
        }

        boolean standing(int x, int y, int z) {
            if (columns >= MAX_COLUMNS) {
                rejection = "PLANNER_BUDGET";
                return false;
            }
            columns++;
            if (y < 1 || y > 253 || x < -29999997 || x > 29999997 || z < -29999997 || z > 29999997
                    || !world.checkChunksExist(x - 2, y - 1, z - 2, x + 2, y + 1, z + 2)) {
                rejection = "APPROACH_UNLOADED";
                return false;
            }
            if (!world.isBlockSolidOnSide(x, y - 1, z, 1)
                    || !clear(x, y, z, y) || !clear(x, y + 1, z, y)) {
                rejection = "ROUTE_OBSTRUCTED";
                return false;
            }
            return true;
        }

        private boolean clear(int x, int y, int z, int feet) {
            int id = world.getBlockId(x, y, z);
            if (id == 0) return true;
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
