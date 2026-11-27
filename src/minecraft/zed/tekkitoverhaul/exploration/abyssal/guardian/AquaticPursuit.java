package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.World;
import net.minecraft.src.Vec3D;

/** Targeting and navigation shared by the unarmored divers and the Observatory caster. */
final class AquaticPursuit {
    private final SwimRoute route = new SwimRoute();
    private EntityPlayer target;
    private int wait, blocked;
    private double x, y, z;

    EntityPlayer target(EntityLiving actor, int detection, int leash, double hx, double hy, double hz) {
        if (target != null && (!wet(target) || target.getDistanceSq(hx, hy, hz) > leash * leash
                || actor.getDistanceSq(hx, hy, hz) > leash * leash)) lose();
        if (target == null && (actor.ticksExisted + actor.entityId) % 8 == 0
                && actor.getDistanceSq(hx, hy, hz) <= leash * leash) {
            EntityPlayer nearby = actor.worldObj.getClosestVulnerablePlayerToEntity(actor, detection);
            if (wet(nearby) && nearby.getDistanceSq(hx, hy, hz) <= leash * leash
                    && visible(actor, nearby)) target = nearby;
        }
        return target;
    }

    static boolean wet(EntityPlayer player) {
        return player != null && player.isEntityAlive() && !player.capabilities.disableDamage
                && !player.isPlayerSleeping() && (player.isInWater() || player.isInsideOfMaterial(Material.water));
    }

    static boolean visible(EntityLiving actor, EntityLiving target) {
        World world = actor.worldObj;
        return target.worldObj == world && world.checkChunksExist(
                SwimRoute.floor(Math.min(actor.posX, target.posX)), 0, SwimRoute.floor(Math.min(actor.posZ, target.posZ)),
                SwimRoute.floor(Math.max(actor.posX, target.posX)), 127, SwimRoute.floor(Math.max(actor.posZ, target.posZ)))
                && world.rayTraceBlocks_do_do(Vec3D.createVector(actor.posX, actor.posY + actor.getEyeHeight(), actor.posZ),
                        Vec3D.createVector(target.posX, target.posY + target.getEyeHeight(), target.posZ),
                        false, true) == null;
    }

    void lose() {
        if (target == null) return;
        target = null;
        blocked = 0;
        wait = 0;
        route.reset();
    }

    void patrol(EntityLiving actor, double hx, double hy, double hz, double speed) {
        if (actor.getDistanceSq(hx, hy, hz) > 12 * 12) {
            route.steer(actor, hx, hy, hz, speed);
            return;
        }
        if (--wait <= 0 || actor.getDistanceSq(x, y, z) < 0.5) {
            int roll = actor.entityId + actor.ticksExisted / 9;
            x = hx + (roll % 13) - 6;
            y = hy + ((roll >> 2) % 7) - 3;
            z = hz + ((roll * 7) % 13) - 6;
            if (!SwimRoute.clear(actor, x, y, z)) {
                x = hx;
                y = hy;
                z = hz;
            }
            wait = 32;
            route.reset();
        }
        route.steer(actor, x, y, z, speed);
    }

    void approach(EntityLiving actor, EntityPlayer player, double speed) {
        if (route.steer(actor, player.posX, player.boundingBox.minY, player.posZ, speed)) blocked = 0;
        else if (++blocked > 48) lose();
    }

    void reposition(EntityLiving actor, EntityPlayer player, double speed) {
        double x = actor.posX - player.posX, z = actor.posZ - player.posZ;
        double length = Math.max(0.01, Math.hypot(x, z));
        for (int attempt = 0; attempt < 4; attempt++) {
            int sign = ((actor.entityId + attempt) & 1) == 0 ? 1 : -1;
            double dx = (x * 1.4 - sign * z * 2) / length;
            double dz = (z * 1.4 + sign * x * 2) / length;
            double dy = attempt < 2 ? sign * 1.2 : 0;
            if (SwimRoute.swept(actor, actor.posX, actor.posY, actor.posZ, dx, dy, dz)) {
                SwimRoute.thrust(actor, dx, dy, dz, speed);
                return;
            }
        }
        if (++blocked > 48) lose();
    }
}
