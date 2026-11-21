package zed.tekkitoverhaul.assault;

import net.minecraft.src.Block;
import net.minecraft.src.EntityMob;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.PathPoint;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.integration.LoadedTile;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.TankBarrier;

/** One exact frontage target and its verified outside face. */
final class SiegeBarrier {
    final PathPoint target, inside;
    final int id;
    private final int lower, upper, side;

    private SiegeBarrier(int x, int y, int z, int dx, int dz, int id, int lower, int upper) {
        target = new PathPoint(x, y, z);
        inside = new PathPoint(x - dx, y, z - dz);
        side = dx < 0 ? 4 : dx > 0 ? 5 : dz < 0 ? 2 : 3;
        this.id = id;
        this.lower = lower;
        this.upper = upper;
    }

    boolean structural() { return id != Block.doorWood.blockID && id != Block.fenceGate.blockID; }

    static SiegeBarrier entrance(World world, int x, int y, int z, int dx, int dz) {
        int id = world.getBlockId(x, y, z);
        if (id != Block.doorWood.blockID && id != Block.fenceGate.blockID) return null;
        int lower = world.getBlockMetadata(x, y, z);
        if ((lower & 12) != 0 || LoadedTile.at(world, x, y, z) != null) return null;
        int upper = 0;
        if (id == Block.doorWood.blockID) {
            if (world.getBlockId(x, y + 1, z) != id || LoadedTile.at(world, x, y + 1, z) != null) return null;
            upper = world.getBlockMetadata(x, y + 1, z);
            if ((upper & 8) == 0 || ((lower & 1) != 0) != (dz != 0)) return null;
        } else if (((lower & 1) != 0) != (dx != 0) || world.getBlockId(x, y + 1, z) != 0) {
            return null;
        }
        return new SiegeBarrier(x, y, z, dx, dz, id, lower, upper);
    }

    static SiegeBarrier structure(World world, int x, int y, int z, int dx, int dz) {
        int lower = TankBarrier.state(world, x, y, z);
        if (lower == 0 || !safeNeighbors(world, x, y, z)) return null;
        int upper = TankBarrier.state(world, x, y + 1, z);
        if (upper != 0 && !safeNeighbors(world, x, y + 1, z)) upper = 0;
        return new SiegeBarrier(x, y, z, dx, dz, lower >> 4, lower, upper);
    }

    boolean loaded(World world) {
        return world.blockExists(target.xCoord, target.yCoord, target.zCoord)
                && world.checkChunksExist(target.xCoord - 2, target.yCoord - 1, target.zCoord - 2,
                target.xCoord + 2, target.yCoord + 2, target.zCoord + 2);
    }

    boolean unchanged(World world) {
        int x = target.xCoord, y = target.yCoord, z = target.zCoord;
        if (!loaded(world)) return false;
        if (structural()) {
            return world.difficultySetting == 3 && structuralCell(world, x, y, z, lower)
                    && (upper == 0 || structuralCell(world, x, y + 1, z, upper));
        }
        if (world.difficultySetting < 2 || world.getBlockId(x, y, z) != id
                || world.getBlockMetadata(x, y, z) != lower || LoadedTile.at(world, x, y, z) != null) return false;
        return id == Block.fenceGate.blockID ? world.getBlockId(x, y + 1, z) == 0
                : world.getBlockId(x, y + 1, z) == id && world.getBlockMetadata(x, y + 1, z) == upper
                    && LoadedTile.at(world, x, y + 1, z) == null;
    }

    boolean inReach(EntityMob mob, int level) {
        int x = target.xCoord, y = target.yCoord + level, z = target.zCoord;
        double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;
        double reach = structural() ? 2.8 : 1.8;
        if (!mob.onGround || Math.abs(mob.boundingBox.minY - target.yCoord) > 0.25
                || mob.getDistanceSq(cx, cy, cz) > reach * reach) return false;
        int mx = MathHelper.floor_double(mob.posX), mz = MathHelper.floor_double(mob.posZ);
        if (!mob.worldObj.checkChunksExist(Math.min(x, mx), y, Math.min(z, mz),
                Math.max(x, mx), y, Math.max(z, mz))) return false;
        MovingObjectPosition hit = mob.worldObj.rayTraceBlocks(
                Vec3D.createVector(mob.posX, cy, mob.posZ),
                Vec3D.createVector(cx + (inside.xCoord - x) * 0.49, cy, cz + (inside.zCoord - z) * 0.49));
        return hit != null && hit.blockX == x && hit.blockY == y && hit.blockZ == z && hit.sideHit == side;
    }

    boolean readyForImpact(EntityMob mob) {
        return inReach(mob, 0) && (!structural() || upper == 0 || inReach(mob, 1));
    }

    int impact(EntityMob mob) {
        World world = mob.worldObj;
        if (!unchanged(world) || !inReach(mob, 0)) return 0;
        int x = target.xCoord, y = target.yCoord, z = target.zCoord;
        if (!structural()) {
            EntityBreacher.openWoodenEntrance(world, x, y, z, id == Block.doorWood.blockID, lower);
            return world.getBlockId(x, y, z) == id && (world.getBlockMetadata(x, y, z) & 4) != 0 ? 1 : 0;
        }
        // Check both cells before the first edit, then revalidate the remaining cell after notifications.
        if (upper != 0 && !inReach(mob, 1)) return 0;
        int removed = upper != 0 && TankBarrier.remove(world, x, y + 1, z, upper) ? 1 : 0;
        if ((upper == 0 || removed == 1) && loaded(world) && structuralCell(world, x, y, z, lower)
                && inReach(mob, 0) && TankBarrier.remove(world, x, y, z, lower)) removed++;
        return removed;
    }

    private static boolean structuralCell(World world, int x, int y, int z, int expected) {
        return TankBarrier.state(world, x, y, z) == expected && safeNeighbors(world, x, y, z);
    }

    private static boolean safeNeighbors(World world, int x, int y, int z) {
        // Conservative guard against dropping attached functional blocks or releasing fluids by notification.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                    int id = world.getBlockId(x + dx, y + dy, z + dz);
                    if (id == 0) continue;
                    Block block = Block.blocksList[id];
                    if (block == Block.stone || block == Block.dirt || block == Block.grass) {
                        if (block.hasTileEntity(world.getBlockMetadata(x + dx, y + dy, z + dz))
                                || LoadedTile.at(world, x + dx, y + dy, z + dz) != null) return false;
                    } else if (TankBarrier.state(world, x + dx, y + dy, z + dz) == 0) return false;
                }
            }
        }
        return true;
    }
}
