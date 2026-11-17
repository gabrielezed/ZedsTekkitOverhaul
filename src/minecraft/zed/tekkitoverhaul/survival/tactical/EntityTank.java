package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.PathEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

public final class EntityTank extends TacticalHostile {
    public static final int WINDUP = 24;
    private static final int RECOVERY = 26;
    private static final int ROUTE_INTERVAL = 16;
    private static final int BREACH_LIMIT = 3;
    private static final double REACH_SQ = 3.1D * 3.1D;
    private static final float SPEED = 0.18F;
    private enum Attack { NONE, SWEEP, BREACH, RELOCATE }

    private Attack attack = Attack.NONE;
    private int attackStart, recoverUntil, nextRoute, lastProgress, nextBlink, breachStrikes;
    private double forwardX, forwardZ, progressX, progressZ;
    private TankBarrier barrier;
    private boolean completedStrike;

    public EntityTank(World world) {
        super(world, 1.85F, 3.15F, "tank");
        attackStrength = 9;
    }

    @Override
    public int getMaxHealth() { return 64; }

    @Override
    public float getEyeHeight() { return 2.69F; }

    @Override
    protected int unseenLimit() { return 200; }

    @Override
    protected void idle() {
        if (ticksExisted < recoverUntil || ticksExisted < nextRoute || !getNavigator().noPath()) return;
        nextRoute = ticksExisted + 55 + rand.nextInt(40);
        getNavigator().setPath(plan(posX + rand.nextInt(9) - 4, boundingBox.minY,
                posZ + rand.nextInt(9) - 4), SPEED);
    }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        if (attack != Attack.NONE) {
            holdFacing();
            if (ticksExisted - attackStart >= WINDUP) impact(visible);
            return;
        }
        if (ticksExisted < recoverUntil) {
            holdFacing();
            return;
        }
        if (onGround && visible && inReach() && strikeLane()) {
            start(Attack.SWEEP);
            return;
        }
        if (ticksExisted < nextRoute) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;

        double movedX = posX - progressX, movedZ = posZ - progressZ;
        if (lastProgress == 0 || movedX * movedX + movedZ * movedZ >= 0.36D) markProgress();
        int stalled = ticksExisted - lastProgress;
        if (stalled >= 176) {
            disengage();
            return;
        }
        PathEntity path = plan(target.posX, target.boundingBox.minY, target.posZ);
        if (onGround && stalled >= 3 * ROUTE_INTERVAL) {
            if (worldObj.difficultySetting == 3 && breachStrikes < BREACH_LIMIT) {
                barrier = TankBarrier.find(this, target);
                if (barrier != null) {
                    start(Attack.BREACH);
                    return;
                }
            }
            if (stalled >= 5 * ROUTE_INTERVAL && visible && ticksExisted >= nextBlink
                    && EnderkinBlink.elevatedLanding(this, target, 3.0D, 4) != null) {
                start(Attack.RELOCATE);
                nextBlink = ticksExisted + 300;
                return;
            }
        }
        getNavigator().setPath(path, SPEED);
    }

    private boolean inReach() {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        return dx * dx + dz * dz <= REACH_SQ && target.boundingBox.maxY > boundingBox.minY
                && target.boundingBox.minY < boundingBox.maxY;
    }

    private void start(Attack next) {
        double angle = Math.atan2(target.posZ - posZ, target.posX - posX);
        forwardX = Math.cos(angle);
        forwardZ = Math.sin(angle);
        attack = next;
        completedStrike = false;
        attackStart = ticksExisted;
        holdFacing();
        worldObj.playSoundAtEntity(this, "mob.endermen.portal", 0.40F, 0.55F);
    }

    private void holdFacing() {
        getNavigator().clearPathEntity();
        setJumping(false);
        float yaw = (float) Math.toDegrees(Math.atan2(forwardZ, forwardX)) - 90F;
        rotationYaw = renderYawOffset = rotationYawHead = yaw;
        getLookHelper().setLookPosition(posX + forwardX, posY + getEyeHeight(), posZ + forwardZ, 30, 30);
    }

    private boolean inArc() {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        return length < 0.001D || (dx * forwardX + dz * forwardZ) >= length * 0.78D;
    }

    private boolean strikeLane() {
        return worldObj.rayTraceBlocks(Vec3D.createVector(posX, boundingBox.minY + 1.2D, posZ),
                Vec3D.createVector(target.posX, target.boundingBox.minY + 1.2D, target.posZ)) == null;
    }

    private void impact(boolean visible) {
        if (onGround && inArc()) {
            if (attack == Attack.SWEEP && visible && inReach() && strikeLane()) {
                if (attackEntityAsMob(target)) target.addVelocity(forwardX * 0.45D, 0.10D, forwardZ * 0.45D);
            } else if (attack == Attack.BREACH && worldObj.difficultySetting == 3 && barrier.strike(this, target)) {
                breachStrikes++;
            } else if (attack == Attack.RELOCATE && visible) {
                double[] landing = EnderkinBlink.elevatedLanding(this, target, 3.0D, 4);
                if (landing != null) EnderkinBlink.move(this, landing[0], landing[1], landing[2], 9, 0.55F);
            }
        }
        worldObj.playSoundAtEntity(this, "step.stone", 0.9F, 0.55F);
        attack = Attack.NONE;
        barrier = null;
        completedStrike = true;
        recoverUntil = ticksExisted + RECOVERY;
        markProgress();
    }

    private void markProgress() {
        progressX = posX;
        progressZ = posZ;
        lastProgress = ticksExisted;
    }

    /** The contact keyframe occurs on the same tick as the gameplay impact. */
    public float heavyPose(float partialTick) {
        if (attack != Attack.NONE) return Math.min(WINDUP, ticksExisted - attackStart + partialTick);
        return completedStrike && recoverUntil > ticksExisted
                ? WINDUP + RECOVERY - (recoverUntil - ticksExisted) + partialTick : -1;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        // Concentrated fire can interrupt a committed structural or relocation windup.
        if (hit && damage >= 5 && (attack == Attack.BREACH || attack == Attack.RELOCATE)) resetTactics();
        return hit;
    }

    @Override
    public void knockBack(Entity source, int damage, double x, double z) {
        double beforeX = motionX, beforeY = motionY, beforeZ = motionZ;
        super.knockBack(source, damage, x, z);
        motionX = beforeX + (motionX - beforeX) * 0.40D;
        motionY = beforeY + (motionY - beforeY) * 0.40D;
        motionZ = beforeZ + (motionZ - beforeZ) * 0.40D;
    }

    @Override
    protected void resetTactics() {
        if (attack != Attack.NONE) recoverUntil = ticksExisted + 16;
        completedStrike = false;
        attack = Attack.NONE;
        barrier = null;
        nextRoute = ticksExisted;
        lastProgress = 0;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("ZtoBreachStrikes", breachStrikes);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        breachStrikes = Math.max(0, Math.min(BREACH_LIMIT, tag.getInteger("ZtoBreachStrikes")));
    }
}
