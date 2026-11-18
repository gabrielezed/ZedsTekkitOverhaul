package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.PathEntity;
import net.minecraft.src.PathPoint;
import net.minecraft.src.World;

/** Short legacy navigation samples toward a saved trail, refreshed only on request. */
public final class EnderTrail {
    private static final int RANGE = 12;
    private static final int HALO = RANGE + 8;
    private static final int MARKERS = 12;
    private static final int DURATION = 85;
    private static final double[] HEADINGS = {0, 0.62D, -0.62D};
    private PathPoint[] points;
    private int expires, lastEmission;

    boolean start(World world, EntityPlayer player, ChunkCoordinates target) {
        clear();
        points = route(world, player, target);
        expires = player.ticksExisted + DURATION;
        lastEmission = player.ticksExisted - 5;
        return points != null;
    }

    void clear() { points = null; }

    void emit(World world, int tick) {
        if (tick >= expires) clear();
        if (points == null || tick - lastEmission < 5) return;
        lastEmission = tick;
        // A traveling highlight makes direction readable without showing a permanent waypoint.
        int lead = (points.length - 1) * ((tick / 5) % 7) / 6;
        for (int i = 0; i < points.length; i++) {
            PathPoint point = points[i];
            if (!world.blockExists(point.xCoord, point.yCoord, point.zCoord)) continue;
            if (expires - tick <= 20 && (i & 1) != 0) continue;
            world.spawnParticle("reddust", point.xCoord + 0.5D, point.yCoord + (i == lead ? 0.56D : 0.18D),
                    point.zCoord + 0.5D, i == lead ? 0.82D : 0.46D, 0.15D, 0.73D);
        }
    }

    static PathPoint[] route(World world, EntityPlayer player, ChunkCoordinates target) {
        int x = MathHelper.floor_double(player.posX), z = MathHelper.floor_double(player.posZ);
        int y = MathHelper.floor_double(player.boundingBox.minY + 0.5D);
        // World#getEntityPathToXYZ constructs a ChunkCache for the full range plus eight.
        if (target == null || !world.checkChunksExist(x - HALO, 0, z - HALO, x + HALO, 0, z + HALO)) return null;
        double dx = target.posX + 0.5D - player.posX, dz = target.posZ + 0.5D - player.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double heading = Math.atan2(dz, dx);
        double step = Math.min(8.0D, distance);
        double bestScore = 0.5D;
        PathEntity best = null;
        for (double turn : HEADINGS) {
            int endX = MathHelper.floor_double(player.posX + Math.cos(heading + turn) * step);
            int endZ = MathHelper.floor_double(player.posZ + Math.sin(heading + turn) * step);
            int footing = footing(world, endX, y, endZ, target.posY, distance <= 8 && turn == 0);
            if (footing < 0) continue;
            PathEntity path = world.getEntityPathToXYZ(player, endX, footing, endZ, RANGE, false, false, true, false);
            if (path == null || path.getCurrentPathLength() < 2) continue;
            PathPoint end = path.getPathPointFromIndex(Math.min(MARKERS, path.getCurrentPathLength()) - 1);
            double score = separation(x, y, z, target) - separation(end.xCoord, end.yCoord, end.zCoord, target);
            if (score > bestScore) {
                best = path;
                bestScore = score;
            }
        }
        if (best == null) return null;
        PathPoint[] result = new PathPoint[Math.min(MARKERS, best.getCurrentPathLength())];
        for (int i = 0; i < result.length; i++) result[i] = best.getPathPointFromIndex(i);
        return result;
    }

    private static double separation(int x, int y, int z, ChunkCoordinates target) {
        double dx = (double) x - target.posX, dy = (double) y - target.posY, dz = (double) z - target.posZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static int footing(World world, int x, int start, int z, int targetY, boolean destination) {
        if (destination && Math.abs(targetY - start) <= 6 && open(world, x, targetY, z)) return targetY;
        for (int n = 0; n <= 5; n++) {
            if (open(world, x, start + n, z)) return start + n;
            if (n > 0 && open(world, x, start - n, z)) return start - n;
        }
        return -1;
    }

    private static boolean open(World world, int x, int y, int z) {
        return y > 0 && y + 1 < world.getHeight() && world.isBlockNormalCube(x, y - 1, z)
                && !world.isBlockNormalCube(x, y, z) && !world.isBlockNormalCube(x, y + 1, z)
                && !world.getBlockMaterial(x, y, z).isLiquid();
    }
}
