package zed.tekkitoverhaul.survival.tactical;

import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** A grounded, straight escape lane with a continuous body collision check. */
final class SaboteurBlink {
    enum Result { CLEAR, NOT_GROUNDED, RANGE, HEIGHT, CHUNK_UNLOADED, FLOOR, OBSTRUCTED }

    private SaboteurBlink() {}

    static Result check(EntitySaboteur mob, double dx, double dz) {
        if (!mob.onGround || Math.abs(mob.motionY) > 0.1D) return Result.NOT_GROUNDED;
        double distanceSq = dx * dx + dz * dz;
        if (!(distanceSq >= 1 && distanceSq <= 16)) return Result.RANGE;
        AxisAlignedBB body = mob.boundingBox;
        AxisAlignedBB hull = body.addCoord(dx, 0, dz);
        int feet = MathHelper.floor_double(hull.minY + 0.01D);
        World world = mob.worldObj;
        if (feet < 1 || hull.maxY >= world.getHeight()
                || Math.abs(hull.minY - feet) > 0.01D) return Result.HEIGHT;

        int minX = MathHelper.floor_double(hull.minX);
        int maxX = MathHelper.floor_double(hull.maxX);
        int minZ = MathHelper.floor_double(hull.minZ);
        int maxZ = MathHelper.floor_double(hull.maxZ);
        // Leave room for vanilla's block and nearby-entity queries and legacy block shapes.
        int halo = (int) Math.ceil(World.MAX_ENTITY_RADIUS) + 1;
        if (!world.checkChunksExist(minX - halo, 0, minZ - halo, maxX + halo, 0, maxZ + halo))
            return Result.CHUNK_UNLOADED;
        List collisions = world.getCollidingBoundingBoxes(mob, hull);
        for (Object collision : collisions) {
            AxisAlignedBB box = (AxisAlignedBB) collision;
            if (crosses(body, dx, dz, box.minX, box.maxX, box.minZ, box.maxZ))
                return Result.OBSTRUCTED;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!crosses(body, dx, dz, x, x + 1, z, z + 1)) continue;
                if (!world.isBlockNormalCube(x, feet - 1, z)) return Result.FLOOR;
                for (int y = feet; y <= MathHelper.floor_double(hull.maxY); y++) {
                    int id = world.getBlockId(x, y, z);
                    if (id == Block.fire.blockID || id == Block.portal.blockID
                            || id != 0 && Block.blocksList[id].blockMaterial.isLiquid())
                        return Result.OBSTRUCTED;
                }
            }
        }
        if (!world.checkIfAABBIsClear(body.getOffsetBoundingBox(dx, 0, dz)))
            return Result.OBSTRUCTED;
        return Result.CLEAR;
    }

    // Intersect the time intervals during which the translated body overlaps each axis.
    private static boolean crosses(AxisAlignedBB body, double dx, double dz,
                                   double minX, double maxX, double minZ, double maxZ) {
        double enter = 0, exit = 1;
        if (Math.abs(dx) < 1.0E-8D) {
            if (body.maxX <= minX || body.minX >= maxX) return false;
        } else {
            double a = (minX - body.maxX) / dx, b = (maxX - body.minX) / dx;
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        if (Math.abs(dz) < 1.0E-8D) {
            if (body.maxZ <= minZ || body.minZ >= maxZ) return false;
        } else {
            double a = (minZ - body.maxZ) / dz, b = (maxZ - body.minZ) / dz;
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        return enter < exit;
    }
}
