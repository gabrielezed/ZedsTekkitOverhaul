package zed.tekkitoverhaul.survival;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Loaded-terrain checks for voluntary movement near water. */
public final class Shoreline {
    private Shoreline() {}

    public static boolean mayStep(EntityLiving mob, double dx, double dz) {
        AxisAlignedBB feet = mob.boundingBox.getOffsetBoundingBox(dx, 0, dz);
        return dryFooting(mob.worldObj, feet, false);
    }

    public static boolean mayLand(World world, AxisAlignedBB feet) {
        return dryFooting(world, feet, true);
    }

    private static boolean dryFooting(World world, AxisAlignedBB feet, boolean surface) {
        int minX = MathHelper.floor_double(feet.minX + 0.001D), maxX = MathHelper.floor_double(feet.maxX - 0.001D);
        int minZ = MathHelper.floor_double(feet.minZ + 0.001D), maxZ = MathHelper.floor_double(feet.maxZ - 0.001D);
        if (!world.checkChunksExist(minX, 0, minZ, maxX, 0, maxZ)) return false;
        int y = Math.min(world.getHeight() - 1, MathHelper.floor_double(feet.minY + 0.01D));
        int bottom = surface ? 0 : Math.max(0, y - 4);
        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                // Only the first open drop matters. Dry bridges and banks still support movement.
                int top = surface ? Math.min(y, world.getPrecipitationHeight(x, z) - 1) : y;
                for (int level = top; level >= bottom; --level) {
                    Material material = world.getBlockMaterial(x, level, z);
                    if (material == Material.water) return false;
                    if (material.blocksMovement()) break;
                }
            }
        }
        return true;
    }

    /** Vanilla shrinks the water probe by 0.4 on both ends, which inverts a small bird's box. */
    public static boolean birdWaterContact(EntityLiving bird) {
        return bird.worldObj.handleMaterialAcceleration(bird.boundingBox.contract(0.001D, 0.001D, 0.001D),
                Material.water, bird);
    }
}
