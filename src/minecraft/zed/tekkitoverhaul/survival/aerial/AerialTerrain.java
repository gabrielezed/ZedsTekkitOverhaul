package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Local queries shared by flight and placement. Check the chunk envelope before reading blocks. */
final class AerialTerrain {
    private AerialTerrain() {}

    static boolean loaded(World world, AxisAlignedBB area) {
        return loaded(world, area.minX - 1, area.minZ - 1, area.maxX + 1, area.maxZ + 1);
    }

    static boolean loaded(World world, double x, double z, double endX, double endZ) {
        return world.checkChunksExist(MathHelper.floor_double(Math.min(x, endX)), 0,
                MathHelper.floor_double(Math.min(z, endZ)), MathHelper.floor_double(Math.max(x, endX)),
                0, MathHelper.floor_double(Math.max(z, endZ)));
    }

    static boolean clear(EntityLiving mob, AxisAlignedBB area) {
        World world = mob.worldObj;
        return area.minY >= 0 && area.maxY < world.getHeight() && loaded(world, area)
                && world.getCollidingBoundingBoxes(mob, area).isEmpty() && !world.isAnyLiquid(area);
    }

    static double skyHeight(EntityLiving mob) {
        World world = mob.worldObj;
        int x = MathHelper.floor_double(mob.posX);
        int y = MathHelper.floor_double(mob.boundingBox.minY + mob.height * 0.5);
        int z = MathHelper.floor_double(mob.posZ);
        if (!world.blockExists(x, y, z) || !world.canBlockSeeTheSky(x, y, z)) return -1;
        return Math.max(0, mob.boundingBox.minY - world.getHeightValue(x, z));
    }
}
