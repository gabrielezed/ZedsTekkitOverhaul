package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** A flat, conservative sweep of the entire body from departure to arrival. */
final class SaboteurBlink {
    enum Result { CLEAR, NOT_GROUNDED, RANGE, HEIGHT, CHUNK_UNLOADED, FLOOR, OBSTRUCTED }

    private SaboteurBlink() {}

    static Result check(EntitySaboteur mob, double dx, double dz) {
        if (!mob.onGround || Math.abs(mob.motionY) > 0.1D) return Result.NOT_GROUNDED;
        double distanceSq = dx * dx + dz * dz;
        if (!(distanceSq >= 1 && distanceSq <= 16)) return Result.RANGE;
        AxisAlignedBB hull = mob.boundingBox.addCoord(dx, 0, dz);
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
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.isBlockNormalCube(x, feet - 1, z)) return Result.FLOOR;
                for (int y = feet; y <= MathHelper.floor_double(hull.maxY); y++) {
                    int id = world.getBlockId(x, y, z);
                    if (id == Block.fire.blockID || id == Block.portal.blockID)
                        return Result.OBSTRUCTED;
                }
            }
        }
        if (!world.getCollidingBoundingBoxes(mob, hull).isEmpty() || world.isAnyLiquid(hull)
                || !world.checkIfAABBIsClear(mob.boundingBox.getOffsetBoundingBox(dx, 0, dz)))
            return Result.OBSTRUCTED;
        return Result.CLEAR;
    }
}
