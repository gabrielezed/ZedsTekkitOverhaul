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
        return contains(x, y, z, CustodianBalance.WIDTH, height);
    }

    boolean contains(double x, double y, double z, double width, double height) {
        double half = width * 0.5;
        return x - half >= bounds.minX && x + half <= bounds.maxX && y >= bounds.minY
                && y + height <= bounds.maxY && z - half >= bounds.minZ && z + half <= bounds.maxZ;
    }

    double xWithin(double point, double width) { return clamp(point, bounds.minX + width * 0.5 + 0.1, bounds.maxX - width * 0.5 - 0.1); }
    double yWithin(double point, double height) { return clamp(point, bounds.minY + 0.08, bounds.maxY - height - 0.08); }
    double zWithin(double point, double width) { return clamp(point, bounds.minZ + width * 0.5 + 0.1, bounds.maxZ - width * 0.5 - 0.1); }

    boolean landing(EntityAbyssalCustodian boss, double x, double y, double z) {
        return free(boss, x, y, z, CustodianBalance.WIDTH, CustodianBalance.HEIGHT + 0.3);
    }

    boolean free(EntityAbyssalCustodian boss, double x, double y, double z, double width, double height) {
        if (!contains(x, y, z, width, height) || !SwimRoute.clear(boss, x, y, z, width, height)) return false;
        World world = boss.worldObj;
        double half = width * 0.5;
        AxisAlignedBB volume = AxisAlignedBB.getBoundingBoxFromPool(x - half, y, z - half, x + half, y + height, z + half);
        if (!world.getCollidingBoundingBoxes(boss, volume).isEmpty()) return false;
        for (Object object : world.getEntitiesWithinAABBExcludingEntity(boss, volume)) {
            net.minecraft.src.Entity entity = (net.minecraft.src.Entity) object;
            if (!entity.isDead && (entity.preventEntitySpawning || entity instanceof net.minecraft.src.EntityPlayer)) return false;
        }
        return true;
    }

    private static AxisAlignedBB bounds(double a, double b, double c, double x, double y, double z) {
        return AxisAlignedBB.getBoundingBox(a, b, c, x, y, z);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
