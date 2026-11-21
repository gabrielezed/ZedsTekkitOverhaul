package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.integration.LoadedTile;

/** One exposed column of the obstruction in the Tank's pursuit lane. */
public final class TankBarrier {
    private static final double[] LANES = {0, 0.80D, -0.80D};
    private static final double REACH = 2.8D;
    private static final double TARGET_RANGE = 12.0D;
    private final int x, y, z;
    private final int[] blocks = new int[4];

    private TankBarrier(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static TankBarrier find(EntityTank tank, EntityPlayer target) {
        return find(tank, target, REACH);
    }

    static boolean detect(EntityTank tank, EntityPlayer target) {
        return find(tank, target, TARGET_RANGE) != null;
    }

    private static TankBarrier find(EntityTank tank, EntityPlayer target, double reach) {
        World world = tank.worldObj;
        double feet = tank.boundingBox.minY;
        double dx = target.posX - tank.posX, dz = target.posZ - tank.posZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        if (range < 1.25D || range > TARGET_RANGE || Math.abs(feet - target.boundingBox.minY) > 2.25D) return null;
        int sx = MathHelper.floor_double(tank.posX), sz = MathHelper.floor_double(tank.posZ);
        int base = MathHelper.floor_double(feet + 0.01D);
        int radius = (int) Math.ceil(reach) + 1;
        if (base < 1 || base + 4 >= world.getHeight()
                || !world.checkChunksExist(sx - radius, 0, sz - radius, sx + radius, 0, sz + radius)) return null;
        double sideX = -dz / range, sideZ = dx / range;
        double fraction = Math.min(1, reach / range);
        TankBarrier result = null;
        for (double lane : LANES) {
            double x = tank.posX + lane * sideX, z = tank.posZ + lane * sideZ;
            for (int level = 0; level < 4; level++) {
                double height = base + level + (level == 3 ? 0.08D : 0.5D);
                MovingObjectPosition hit = world.rayTraceBlocks(Vec3D.createVector(x, height, z),
                        Vec3D.createVector(x + dx * fraction, height, z + dz * fraction));
                if (hit == null || hit.blockY != base + level) continue;
                int state = state(world, hit.blockX, hit.blockY, hit.blockZ);
                if (state == 0) continue;
                if (result == null) result = new TankBarrier(hit.blockX, base, hit.blockZ);
                if (result.x == hit.blockX && result.z == hit.blockZ) result.blocks[level] = state;
            }
        }
        return result;
    }

    boolean strike(EntityTank tank, EntityPlayer target) {
        TankBarrier current = find(tank, target);
        if (current == null || current.x != x || current.y != y || current.z != z) return false;
        World world = tank.worldObj;
        boolean broken = false;
        for (int level = blocks.length - 1; level >= 0; level--) {
            int intended = blocks[level];
            if (intended == 0 || intended != current.blocks[level] || intended != state(world, x, y + level, z)) continue;
            if (remove(world, x, y + level, z, intended)) broken = true;
        }
        return broken;
    }

    public static boolean remove(World world, int x, int y, int z, int expected) {
        if (expected == 0 || expected != state(world, x, y, z) || !world.setBlockWithNotify(x, y, z, 0)) return false;
        Block.blocksList[expected >> 4].dropBlockAsItem(world, x, y, z, expected & 15, 0);
        return true;
    }

    public static int state(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return 0;
        int id = world.getBlockId(x, y, z);
        Block block = Block.blocksList[id];
        if (block != Block.planks && block != Block.wood && block != Block.cobblestone && block != Block.glass
                && block != Block.brick) return 0;
        int metadata = world.getBlockMetadata(x, y, z);
        return block.hasTileEntity(metadata) || LoadedTile.at(world, x, y, z) != null ? 0 : (id << 4) | metadata;
    }
}
