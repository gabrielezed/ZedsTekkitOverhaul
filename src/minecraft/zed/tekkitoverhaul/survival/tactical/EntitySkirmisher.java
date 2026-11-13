package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.EntityArrow;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.PathEntity;
import net.minecraft.src.World;

public final class EntitySkirmisher extends TacticalHostile {
    private static final float SPEED = 0.25F;
    private static final double RETREAT_SQ = 25.0D;
    private static final double RANGE_SQ = 144.0D;
    private static final int AIM_TICKS = 12;
    private static final int SHOT_COOLDOWN = 50;
    private static final int MOVE_INTERVAL = 20;
    private static final int MAX_MOVE_FAILURES = 4;

    private final ItemStack bow = new ItemStack(Item.bow);
    private int aim;
    private int nextShot;
    private int nextMove;
    private int moveFailures;
    private int side = 1;
    private float previousAimPose;
    private float aimPose;

    public EntitySkirmisher(World world) {
        super(world, 0.6F, 1.7F, "skirmisher");
        nextShot = 20;
    }

    @Override
    public int getMaxHealth() { return 16; }

    @Override
    public ItemStack getHeldItem() { return bow; }

    public boolean isAiming() { return aim > 0; }

    public float getAimPose(float partialTick) {
        return previousAimPose + (aimPose - previousAimPose) * partialTick;
    }

    @Override
    protected void updateAttackPose() {
        previousAimPose = aimPose;
        aimPose = aim > 0 ? Math.min(1, aimPose + 0.10F) : Math.max(0, aimPose - 0.18F);
    }

    @Override
    protected int unseenLimit() { return 100; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        boolean inRange = visible && distanceSq >= RETREAT_SQ && distanceSq <= RANGE_SQ;
        if (aim > 0) {
            if (!inRange) {
                aim = 0;
                nextShot = ticksExisted + AIM_TICKS;
            } else if (--aim == 0) {
                fire();
                nextShot = ticksExisted + SHOT_COOLDOWN;
                return;
            } else {
                return;
            }
        }
        if (inRange && ticksExisted >= nextShot) {
            getNavigator().clearPathEntity();
            aim = AIM_TICKS;
            return;
        }
        if (ticksExisted < nextMove) return;
        nextMove = ticksExisted + MOVE_INTERVAL;
        if (move(distanceSq, visible)) moveFailures = 0;
        else if (++moveFailures >= MAX_MOVE_FAILURES) disengage();
    }

    private boolean move(double distanceSq, boolean visible) {
        if (!visible || distanceSq > RANGE_SQ) {
            double y = target.boundingBox.minY;
            PathEntity path = plan(target.posX, y, target.posZ);
            return reaches(path, target.posX, y, target.posZ) && getNavigator().setPath(path, SPEED);
        }

        double dx = posX - target.posX;
        double dz = posZ - target.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.01D) {
            dx = 1.0D;
            length = 1.0D;
        }
        double outward = distanceSq < RETREAT_SQ ? 4.0D : 0.0D;
        double lateral = outward > 0.0D ? 1.5D : 3.0D;
        double x = posX + (outward * dx - side * lateral * dz) / length;
        double z = posZ + (outward * dz + side * lateral * dx) / length;
        double y = boundingBox.minY;
        PathEntity path = plan(x, y, z);
        side = -side;
        return reaches(path, x, y, z) && getNavigator().setPath(path, SPEED);
    }

    private void fire() {
        // Start inside the shooter's volume so collision checks include nearby cover.
        EntityArrow arrow = new EntityArrow(worldObj, this, 1.0F);
        arrow.setPosition(posX, posY + getEyeHeight() - 0.1D, posZ);
        double dx = target.posX - arrow.posX;
        double dz = target.posZ - arrow.posZ;
        double dy = target.boundingBox.minY + 0.9D - arrow.posY + Math.sqrt(dx * dx + dz * dz) * 0.12D;
        arrow.setArrowHeading(dx, dy, dz, 1.35F, 10.0F);
        arrow.setDamage(1.5D);
        if (worldObj.spawnEntityInWorld(arrow)) {
            swing();
            worldObj.playSoundAtEntity(this, "random.bow", 0.8F, 1.0F);
        }
    }

    @Override
    protected void resetTactics() {
        aim = 0;
        moveFailures = 0;
    }
}
