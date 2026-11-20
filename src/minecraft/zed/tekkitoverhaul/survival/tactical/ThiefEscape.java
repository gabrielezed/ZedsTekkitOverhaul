package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.PathEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

/** Short native paths compose into a chase as the player's loaded area moves. */
final class ThiefEscape {
    private static final float RUN_SPEED = 0.41F;
    private static final double[] ANGLES = {0, 0.65D, -0.65D, 1.25D, -1.25D};
    private final EntityThief thief;
    private int runUntil, nextRoute, nextBlink, nextCue;
    private int stalled;
    private boolean fleeing;
    private double lastX, lastZ, away;

    ThiefEscape(EntityThief thief) { this.thief = thief; }

    void start(EntityPlayer player) {
        fleeing = true;
        away = Math.atan2(thief.posZ - player.posZ, thief.posX - player.posX);
        runUntil = thief.ticksExisted + 130;
        nextBlink = thief.ticksExisted + 28;
        nextRoute = thief.ticksExisted;
        nextCue = thief.ticksExisted + 35;
        lastX = thief.posX;
        lastZ = thief.posZ;
    }

    void tick() {
        int now = thief.ticksExisted;
        EntityPlayer pursuer = thief.worldObj.getClosestVulnerablePlayerToEntity(thief, 54);
        double rangeSq = pursuer == null ? Double.POSITIVE_INFINITY : thief.getDistanceSqToEntity(pursuer);
        if (!fleeing) {
            if (rangeSq > 18 * 18) return;
            start(pursuer);
        }
        if (now >= runUntil && (rangeSq > 42 * 42 || (rangeSq > 30 * 30 && stalled >= 3))) {
            fleeing = false;
            thief.getNavigator().clearPathEntity();
            return;
        }
        if (now >= nextCue) {
            nextCue = now + 50;
            thief.trailParticles(3);
        }
        if (now < nextRoute || !thief.onGround) return;
        nextRoute = now + 12;
        double progressX = thief.posX - lastX, progressZ = thief.posZ - lastZ;
        stalled = progressX * progressX + progressZ * progressZ < 0.30D ? stalled + 1 : 0;
        lastX = thief.posX;
        lastZ = thief.posZ;
        if (pursuer != null) away = Math.atan2(thief.posZ - pursuer.posZ, thief.posX - pursuer.posX);
        int side = ((now / 48 + thief.entityId + stalled) & 1) == 0 ? 1 : -1;

        if (!thief.holdingSiteSiege() && (stalled >= 2 || rangeSq < 8 * 8) && now >= nextBlink) {
            boolean moved = EnderkinBlink.shift(thief, away + side * 0.58D, 3.75D, 7, 1.40F)
                    || EnderkinBlink.shift(thief, away - side * 0.82D, 3.3D, 7, 1.40F);
            nextBlink = now + (moved ? 62 : 16);
            if (moved) {
                stalled = 0;
                return;
            }
        }

        if (!thief.getNavigator().noPath() && stalled == 0 && rangeSq >= 16 * 16) return;
        route(pursuer, side);
    }

    private void route(EntityPlayer pursuer, int side) {
        World world = thief.worldObj;
        double score = Double.NEGATIVE_INFINITY;
        PathEntity best = null;
        int planned = 0;
        for (int i = 0; i < ANGLES.length && planned < 3; i++) {
            double angle = away + side * ANGLES[(i + (stalled > 1 ? 1 : 0)) % ANGLES.length];
            int x = MathHelper.floor_double(thief.posX + Math.cos(angle) * (i < 3 ? 11 : 7));
            int z = MathHelper.floor_double(thief.posZ + Math.sin(angle) * (i < 3 ? 11 : 7));
            int y = footing(world, x, z);
            if (y < 0) continue;
            PathEntity path = thief.plan(x, y, z);
            planned++;
            if (!thief.reaches(path, x, y, z)) continue;
            double value = pursuer == null ? 12 - i : Math.sqrt(pursuer.getDistanceSq(x + 0.5, y, z + 0.5));
            if (pursuer != null && world.checkChunksExist(MathHelper.floor_double(Math.min(x, pursuer.posX)), 0,
                    MathHelper.floor_double(Math.min(z, pursuer.posZ)), MathHelper.floor_double(Math.max(x, pursuer.posX)),
                    0, MathHelper.floor_double(Math.max(z, pursuer.posZ)))
                    && world.rayTraceBlocks(Vec3D.createVector(x + 0.5, y + 1.4, z + 0.5),
                            Vec3D.createVector(pursuer.posX, pursuer.posY + pursuer.getEyeHeight(), pursuer.posZ)) != null) {
                value += 6;
            }
            if (value > score) {
                score = value;
                best = path;
            }
        }
        if (best != null) thief.getNavigator().setPath(best, RUN_SPEED);
    }

    private int footing(World world, int x, int z) {
        int feet = MathHelper.floor_double(thief.boundingBox.minY + 0.01);
        for (int y = feet + 2; y >= feet - 4; y--) {
            if (y < 1 || y + 1 >= world.getHeight() || !world.blockExists(x, y, z)) continue;
            if (world.isBlockNormalCube(x, y - 1, z) && walkThrough(world, x, y, z)
                    && walkThrough(world, x, y + 1, z)) return y;
        }
        return -1;
    }

    private static boolean walkThrough(World world, int x, int y, int z) {
        Block block = Block.blocksList[world.getBlockId(x, y, z)];
        return block == null || (!block.blockMaterial.isSolid() && !block.blockMaterial.isLiquid()
                && block != Block.fire && block != Block.portal && block != Block.cactus);
    }
}
