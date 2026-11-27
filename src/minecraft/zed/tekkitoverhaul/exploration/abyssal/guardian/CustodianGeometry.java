package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.util.ArrayList;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

final class CustodianGeometry {
    private CustodianGeometry() {}

    static Vec3D shoulder(EntityAbyssalCustodian boss, int side, float yaw) {
        double angle = Math.toRadians(yaw);
        return Vec3D.createVector(boss.posX - side * CustodianBalance.SHOULDER * Math.cos(angle),
                boss.boundingBox.minY + CustodianBalance.SHOULDER_Y,
                boss.posZ - side * CustodianBalance.SHOULDER * Math.sin(angle));
    }

    static Vec3D point(Vec3D origin, float yaw, float pitch, double length) {
        double a = Math.toRadians(yaw), b = Math.toRadians(pitch);
        return origin.addVector(-Math.sin(a) * Math.cos(b) * length, -Math.sin(b) * length,
                Math.cos(a) * Math.cos(b) * length);
    }

    static double clearReach(World world, Vec3D origin, float yaw, float pitch) {
        double radius = CustodianBalance.STRIKE_THICKNESS;
        ArrayList<AxisAlignedBB> collisions = new ArrayList<AxisAlignedBB>();
        double clear = 0;
        int steps = (int) Math.ceil(CustodianBalance.STRIKE_REACH * 12);
        for (int step = 0; step <= steps; step++) {
            double length = CustodianBalance.STRIKE_REACH * step / steps;
            Vec3D at = point(origin, yaw, pitch, length);
            AxisAlignedBB box = AxisAlignedBB.getBoundingBoxFromPool(at.xCoord - radius, at.yCoord - radius,
                    at.zCoord - radius, at.xCoord + radius, at.yCoord + radius, at.zCoord + radius);
            int x = SwimRoute.floor(box.minX), y = SwimRoute.floor(box.minY), z = SwimRoute.floor(box.minZ);
            int xx = SwimRoute.floor(box.maxX), yy = SwimRoute.floor(box.maxY), zz = SwimRoute.floor(box.maxZ);
            if (!world.checkChunksExist(x, y, z, xx, yy, zz)) break;
            for (int bx = x; bx <= xx; bx++) for (int by = y; by <= yy; by++) for (int bz = z; bz <= zz; bz++) {
                Block block = Block.blocksList[world.getBlockId(bx, by, bz)];
                if (block != null) block.getCollidingBoundingBoxes(world, bx, by, bz, box, collisions);
            }
            if (!collisions.isEmpty()) break;
            clear = length;
        }
        return clear;
    }

    static boolean visible(World world, Vec3D from, Vec3D to) {
        if (!world.checkChunksExist(SwimRoute.floor(Math.min(from.xCoord, to.xCoord)), 0,
                SwimRoute.floor(Math.min(from.zCoord, to.zCoord)), SwimRoute.floor(Math.max(from.xCoord, to.xCoord)),
                0, SwimRoute.floor(Math.max(from.zCoord, to.zCoord)))) return false;
        return world.rayTraceBlocks_do_do(Vec3D.createVector(from.xCoord, from.yCoord, from.zCoord),
                Vec3D.createVector(to.xCoord, to.yCoord, to.zCoord), false, true) == null;
    }

    static Vec3D center(EntityPlayer player) {
        return Vec3D.createVector(player.posX, (player.boundingBox.minY + player.boundingBox.maxY) * 0.5, player.posZ);
    }

    static double hitDistance(EntityPlayer player, Vec3D origin, Vec3D end) {
        double r = CustodianBalance.STRIKE_THICKNESS;
        AxisAlignedBB box = player.boundingBox.expand(r, r, r);
        if (box.isVecInside(origin)) return 0;
        MovingObjectPosition hit = box.calculateIntercept(origin, end);
        return hit == null ? Double.POSITIVE_INFINITY : origin.squareDistanceTo(hit.hitVec);
    }
}
