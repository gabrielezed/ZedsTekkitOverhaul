package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.integration.LoadedTile;

/** One exposed column of the obstruction in the Tank's pursuit lane. */
final class TankBarrier {
    private static final double[] LANES = {0, 0.80D, -0.80D};
    private static final double REACH = 2.8D;
    private final int x, y, z;
    private final int[] blocks = new int[4];

    private TankBarrier(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static TankBarrier find(EntityTank tank, EntityPlayer target) {
        World world = tank.worldObj;
        double feet = tank.boundingBox.minY;
        double dx = target.posX - tank.posX, dz = target.posZ - tank.posZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        if (range < 1.25D || range > 7.0D || Math.abs(feet - target.boundingBox.minY) > 1.1D) return null;
        int sx = MathHelper.floor_double(tank.posX), sz = MathHelper.floor_double(tank.posZ);
        int base = MathHelper.floor_double(feet + 0.01D);
        if (base < 1 || base + 4 >= world.getHeight()
                || !world.checkChunksExist(sx - 5, 0, sz - 5, sx + 5, 0, sz + 5)) return null;
        double sideX = -dz / range, sideZ = dx / range;
        double fraction = Math.min(1, REACH / range);
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
            Block block = Block.blocksList[intended >> 4];
            if (world.setBlockWithNotify(x, y + level, z, 0)) {
                block.dropBlockAsItem(world, x, y + level, z, intended & 15, 0);
                broken = true;
            }
        }
        return broken;
    }

    private static int state(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        Block block = Block.blocksList[id];
        if (block != Block.planks && block != Block.wood && block != Block.cobblestone && block != Block.glass
                && block != Block.brick) return 0;
        int metadata = world.getBlockMetadata(x, y, z);
        return block.hasTileEntity(metadata) || LoadedTile.at(world, x, y, z) != null ? 0 : (id << 4) | metadata;
    }
}
