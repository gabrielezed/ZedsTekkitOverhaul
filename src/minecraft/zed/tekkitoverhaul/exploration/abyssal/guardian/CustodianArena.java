package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.World;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;

final class CustodianArena {
    final double x, y, z;
    final AxisAlignedBB bounds;

    CustodianArena(Site site) {
        int[] center = site.arenaCenter(), a = site.arenaCorner(false), b = site.arenaCorner(true);
        x = center[0] + 0.5;
        y = center[1] + 0.08;
        z = center[2] + 0.5;
        bounds = bounds(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[0], b[0]) + 1, Math.max(a[1], b[1]) + 1, Math.max(a[2], b[2]) + 1);
    }

    CustodianArena(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        bounds = bounds(x - 9, y - 4, z - 9, x + 9, y + 7, z + 9);
    }

    boolean contains(double x, double y, double z, double height) {
        double half = CustodianBalance.WIDTH * 0.5;
        return x - half >= bounds.minX && x + half <= bounds.maxX && y >= bounds.minY
                && y + height <= bounds.maxY && z - half >= bounds.minZ && z + half <= bounds.maxZ;
    }

    double xWithin(double point) { return clamp(point, bounds.minX + 0.7, bounds.maxX - 0.7); }
    double yWithin(double point) { return clamp(point, bounds.minY + 0.08, bounds.maxY - CustodianBalance.HEIGHT - 0.08); }
    double zWithin(double point) { return clamp(point, bounds.minZ + 0.7, bounds.maxZ - 0.7); }

    boolean landing(EntityAbyssalCustodian boss, double x, double y, double z) {
        if (!contains(x, y, z, 3.1) || !SwimRoute.clear(boss, x, y, z)) return false;
        if (!SwimRoute.clear(boss, x, y + 3.1 - CustodianBalance.HEIGHT, z)) return false;
        World world = boss.worldObj;
        double half = CustodianBalance.WIDTH * 0.5;
        AxisAlignedBB volume = AxisAlignedBB.getBoundingBoxFromPool(x - half, y, z - half, x + half, y + 3.1, z + half);
        return world.getEntitiesWithinAABBExcludingEntity(boss, volume).isEmpty()
                && world.getCollidingBoundingBoxes(boss, volume).isEmpty();
    }

    private static AxisAlignedBB bounds(double a, double b, double c, double x, double y, double z) {
        return AxisAlignedBB.getBoundingBox(a, b, c, x, y, z);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
