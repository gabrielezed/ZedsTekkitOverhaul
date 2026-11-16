package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.PathEntity;
import net.minecraft.src.World;

/** Small Enderkin available through its spawn egg. */
public final class EntitySaboteur extends TacticalHostile {
    public static final byte IDLE = 0, APPROACH = 1, WINDUP = 2, RECOVER = 3, CANCELLED = 4;
    private static final int POSE = 20;
    private static final float SPEED = 0.34F;
    private static final int ROUTE_INTERVAL = 20;
    private static final int BLINK_INTERVAL = 48;
    private static final double[] BLINK_ANGLES = {0, Math.PI / 5, -Math.PI / 5};
    private int nextRoute, nextBlink, nextMelee;
    private int stalledRoutes;
    private double progressX, progressZ;
    private int pathPlans, blinkAttempts, blinkChecks, blinks;
    private SaboteurBlink.Result blinkResult = SaboteurBlink.Result.NOT_GROUNDED;

    public EntitySaboteur(World world) {
        super(world, 0.45F, 1.1F, "saboteur");
        attackStrength = 1;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(POSE, Byte.valueOf(IDLE));
    }

    public byte pose() { return dataWatcher.getWatchableObjectByte(POSE); }

    private void pose(byte value) { dataWatcher.updateObject(POSE, Byte.valueOf(value)); }

    @Override
    public int getMaxHealth() { return 12; }

    @Override
    protected int unseenLimit() { return 60; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        pose(APPROACH);
        if (defend(distanceSq, visible)) return;
        pursue(target.posX, target.boundingBox.minY, target.posZ, visible);
    }

    private boolean defend(double distanceSq, boolean visible) {
        if (!visible || distanceSq > 1.69D || target.boundingBox.minY >= boundingBox.maxY
                || target.boundingBox.maxY <= boundingBox.minY) return false;
        getNavigator().clearPathEntity();
        stalledRoutes = 0;
        if (ticksExisted >= nextMelee) {
            nextMelee = ticksExisted + 34;
            swing();
            attackEntityAsMob(target);
        }
        return true;
    }

    private void pursue(double x, double y, double z, boolean allowBlink) {
        if (allowBlink && tryBlink(x, z)) return;
        if (ticksExisted < nextRoute || !onGround) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;
        double dx = posX - progressX;
        double dz = posZ - progressZ;
        progressX = posX;
        progressZ = posZ;
        stalledRoutes = dx * dx + dz * dz < 0.16D ? stalledRoutes + 1 : 0;
        if (stalledRoutes >= 5) {
            disengage();
            return;
        }
        pathPlans++;
        PathEntity path = plan(x, y, z);
        if (reaches(path, x, y, z)) getNavigator().setPath(path, SPEED);
    }

    private boolean tryBlink(double x, double z) {
        double dx = x - posX;
        double dz = z - posZ;
        double distanceSq = dx * dx + dz * dz;
        if (ticksExisted < nextBlink || !onGround || hurtTime > 0 || distanceSq < 16 || distanceSq > 64)
            return false;
        nextBlink = ticksExisted + BLINK_INTERVAL;
        blinkAttempts++;
        double direction = Math.atan2(dz, dx);
        for (double angle : BLINK_ANGLES) {
            double shiftX = Math.cos(direction + angle) * 2.75D;
            double shiftZ = Math.sin(direction + angle) * 2.75D;
            blinkChecks++;
            blinkResult = SaboteurBlink.check(this, shiftX, shiftZ);
            if (blinkResult != SaboteurBlink.Result.CLEAR) continue;
            burst(posX, posY + 0.45D, posZ, "portal");
            setPosition(posX + shiftX, posY, posZ + shiftZ);
            prevPosX = lastTickPosX = posX;
            prevPosZ = lastTickPosZ = posZ;
            motionX = motionZ = 0;
            getNavigator().clearPathEntity();
            burst(posX, posY + 0.45D, posZ, "portal");
            blinks++;
            return true;
        }
        return false;
    }

    private void burst(double x, double y, double z, String particle) {
        for (int i = 0; i < 4; i++) {
            worldObj.spawnParticle(particle, x, y, z,
                    (rand.nextFloat() - 0.5F) * 0.6D, rand.nextFloat() * 0.3D,
                    (rand.nextFloat() - 0.5F) * 0.6D);
        }
    }

    @Override
    protected void resetTactics() {
        stalledRoutes = 0;
        pose(IDLE);
    }
}
