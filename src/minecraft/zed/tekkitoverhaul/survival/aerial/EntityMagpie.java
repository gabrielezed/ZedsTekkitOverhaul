package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

/** Small forest wildlife with short flights and no combat target. */
public final class EntityMagpie extends EntityLiving {
    public static final String TEXTURE = "/zed/textures/magpie.png";
    private static final int POSE_WATCHER = 20;
    private static final int MAX_AGE = 6000;
    private static final Pose[] POSES = Pose.values();
    private static final double WALK_SPEED = 0.105;
    public enum Pose { WATCH, WALK, HOP, FLY, PECK, PERCH, CLIMB, LAND }
    private int decisionDelay, flightTicks, lifeTicks, stepTick;
    private EntityItem curiosity;
    private boolean startled;
    private Step step;
    private float gait, lastGait, stride, lastStride;
    private float headYaw, lastHeadYaw, headPitch, lastHeadPitch;

    public EntityMagpie(World world) {
        super(world);
        texture = TEXTURE;
        setSize(0.38F, 0.42F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(POSE_WATCHER, Byte.valueOf((byte) Pose.WATCH.ordinal()));
    }

    @Override
    public int getMaxHealth() { return 4; }

    public Pose pose() { return POSES[dataWatcher.getWatchableObjectByte(POSE_WATCHER)]; }

    private void pose(Pose pose) { dataWatcher.updateObject(POSE_WATCHER, Byte.valueOf((byte) pose.ordinal())); }

    public float gait(float partial) { return lastGait + (gait - lastGait) * partial; }
    public float stride(float partial) { return lastStride + (stride - lastStride) * partial; }
    public float headYaw(float partial) { return lastHeadYaw + (headYaw - lastHeadYaw) * partial; }
    public float headPitch(float partial) { return lastHeadPitch + (headPitch - lastHeadPitch) * partial; }

    @Override
    protected void updateEntityActionState() {
        if (++lifeTicks >= MAX_AGE) {
            setDead();
            return;
        }
        ++entityAge;
        despawnEntity();
        if (isDead) return;

        if (flightTicks > 0 || step != null || !onGround) {
            lookAt(null);
            return;
        }
        EntityPlayer observer = worldObj.getClosestPlayerToEntity(this, 6.0);
        if (!visible(observer) || !observer.isEntityAlive()) observer = null;
        if (observer != null && rushing(observer)) {
            face(posX - observer.posX, posZ - observer.posZ);
            startled = true;
        }
        if (startled) {
            startled = false;
            curiosity = null;
            lookAt(null);
            launch();
            return;
        }
        if (--decisionDelay <= 0) chooseActivity();
        if (curiosity != null) approachItem();
        lookAt(curiosity != null ? curiosity : observer != null && attentiveTo(observer) ? observer : null);
        motionX = motionZ = 0;
        if (pose() == Pose.WALK) walk();
    }

    private void chooseActivity() {
        curiosity = null;
        if (pose() == Pose.LAND || pose() == Pose.WALK || pose() == Pose.PECK) {
            rest();
            return;
        }
        int roll = rand.nextInt(10);
        curiosity = roll < 4 ? nearbyItem() : null;
        if (curiosity != null) {
            decisionDelay = 28;
            return;
        }
        rotationYaw += rand.nextInt(111) - 55;
        if (roll == 9) launch();
        else if (roll < 3) rest();
        else {
            pose(Pose.WALK);
            decisionDelay = 14 + rand.nextInt(14);
        }
    }

    private void rest() {
        Pose resting = restingPose();
        pose(resting);
        decisionDelay = resting == Pose.PERCH ? 60 : 8 + rand.nextInt(11);
    }

    private boolean visible(Entity target) {
        return target != null && AerialTerrain.loaded(worldObj, posX, posZ, target.posX, target.posZ)
                && canEntityBeSeen(target);
    }

    private boolean rushing(EntityPlayer player) {
        if (player.isSneaking() || player.capabilities.isCreativeMode || getDistanceSqToEntity(player) >= 16) return false;
        double dx = player.posX - player.prevPosX, dz = player.posZ - player.prevPosZ;
        return dx * dx + dz * dz > 0.09 * 0.09
                && dx * (posX - player.posX) + dz * (posZ - player.posZ) > 0.04;
    }

    private static boolean attentiveTo(EntityPlayer player) {
        double dx = player.posX - player.prevPosX, dz = player.posZ - player.prevPosZ;
        return player.isSneaking() || dx * dx + dz * dz < 0.045 * 0.045;
    }

    private void lookAt(Entity target) {
        lastHeadYaw = headYaw;
        lastHeadPitch = headPitch;
        float yaw = 0, pitch = 0;
        if (target != null) {
            double dx = target.posX - posX, dz = target.posZ - posZ;
            double dy = target.posY + target.getEyeHeight() - posY - getEyeHeight();
            float angle = heading(dx, dz) - rotationYaw;
            yaw = angle - 360 * MathHelper.floor_float((angle + 180) / 360);
            pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
        headYaw += limit(limit(yaw, 82) - headYaw, 13);
        headPitch += limit(limit(pitch, 40) - headPitch, 8);
    }

    private static float limit(float value, float maximum) {
        return Math.max(-maximum, Math.min(maximum, value));
    }

    private Pose restingPose() {
        int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(boundingBox.minY - 0.05);
        int z = MathHelper.floor_double(posZ);
        return worldObj.blockExists(x, y, z) && worldObj.getBlockId(x, y, z) == Block.leaves.blockID
                ? Pose.PERCH : Pose.WATCH;
    }

    private EntityItem nearbyItem() {
        for (Object entry : worldObj.getEntitiesWithinAABB(EntityItem.class, boundingBox.expand(4, 1, 4))) {
            EntityItem item = (EntityItem) entry;
            if (!item.isDead && item.item != null && (item.item.itemID == Item.seeds.shiftedIndex
                    || item.item.itemID == Item.feather.shiftedIndex)
                    && visible(item)) return item;
        }
        return null;
    }

    private void approachItem() {
        if (curiosity.isDead || getDistanceSqToEntity(curiosity) > 25 || !visible(curiosity)) {
            curiosity = null;
            rest();
        } else {
            boolean close = getDistanceSqToEntity(curiosity) < 0.56;
            if (!close) face(curiosity.posX - posX, curiosity.posZ - posZ);
            pose(close ? Pose.PECK : Pose.WALK);
        }
    }

    private static float heading(double x, double z) { return (float) Math.toDegrees(Math.atan2(-x, z)); }

    private void face(double x, double z) { rotationYaw = heading(x, z); }

    private void walk() {
        if (clearAhead(rotationYaw, 0.37)) {
            motionX = -Math.sin(Math.toRadians(rotationYaw)) * WALK_SPEED;
            motionZ = Math.cos(Math.toRadians(rotationYaw)) * WALK_SPEED;
        } else if (!findStep()) {
            redirect();
        }
    }

    private boolean clearAhead(float heading, double distance) {
        double angle = Math.toRadians(heading);
        return AerialTerrain.clear(this, boundingBox.addCoord(-Math.sin(angle) * distance, 0, Math.cos(angle) * distance));
    }

    private void redirect() {
        curiosity = null;
        motionX = motionZ = 0;
        int turn = rand.nextBoolean() ? 1 : -1;
        for (int bend : new int[] {65 * turn, -65 * turn, 120 * turn, -120 * turn, 180}) {
            if (!clearAhead(rotationYaw + bend, 0.55)) continue;
            rotationYaw += bend;
            decisionDelay = 15;
            pose(Pose.WALK);
            return;
        }
        rotationYaw += 110 * turn;
        rest();
    }

    private boolean findStep() {
        double angle = Math.toRadians(rotationYaw);
        int x = MathHelper.floor_double(posX - Math.sin(angle) * 0.75);
        int z = MathHelper.floor_double(posZ + Math.cos(angle) * 0.75);
        int feet = MathHelper.floor_double(boundingBox.minY + 0.05);
        if (!worldObj.blockExists(x, feet + 2, z)) return false;
        for (int rise = 1; rise <= 2; ++rise) {
            // Require a full support block and clearance for the entire takeoff and landing.
            if (!worldObj.isBlockNormalCube(x, feet + rise - 1, z)) continue;
            Step candidate = new Step(x + 0.5 - posX, feet + rise - boundingBox.minY, z + 0.5 - posZ, rise);
            if (!candidate.clear()) continue;
            step = candidate;
            stepTick = 0;
            curiosity = null;
            face(step.dx, step.dz);
            pose(rise == 1 ? Pose.HOP : Pose.CLIMB);
            return true;
        }
        return false;
    }

    private final class Step {
        final double dx, dy, dz;
        final int ticks;

        Step(double dx, double dy, double dz, int rise) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            ticks = rise == 1 ? 11 : 16;
        }

        double forward(int tick) {
            // Clear the face before crossing, then slow over the center of the landing block.
            double progress = Math.max(0, Math.min(1, (tick / (double) ticks - 0.36) / 0.45));
            return progress * progress * (3 - 2 * progress);
        }

        double height(int tick) {
            // Finish just above support so native gravity makes the final contact.
            double t = tick / (double) ticks;
            double up = Math.min(1, t / 0.36);
            double down = Math.max(0, (t - 0.81) / 0.19);
            return (dy + 0.18) * Math.sin(up * Math.PI / 2) - down * 0.15;
        }

        boolean clear() {
            double along = 0, up = 0;
            for (int tick = 1; tick <= ticks; ++tick) {
                double next = forward(tick), above = height(tick);
                AxisAlignedBB start = boundingBox.getOffsetBoundingBox(dx * along, up, dz * along);
                if (!AerialTerrain.clear(EntityMagpie.this,
                        start.addCoord(dx * (next - along), above - up, dz * (next - along)))) return false;
                along = next;
                up = above;
            }
            return true;
        }
    }

    private void launch() {
        flightTicks = 24 + rand.nextInt(12);
        decisionDelay = 24;
        motionX = motionZ = 0;
        motionY = 0.25;
        pose(Pose.FLY);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        if (hit && source.getEntity() != null) {
            face(posX - source.getEntity().posX, posZ - source.getEntity().posZ);
            startled = true;
        }
        return hit;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        lastGait = gait;
        lastStride = stride;
        stride = 0;
        if (step != null) {
            double along = step.forward(stepTick + 1) - step.forward(stepTick);
            double dy = step.height(stepTick + 1) - step.height(stepTick);
            double dx = step.dx * along, dz = step.dz * along;
            if (AerialTerrain.clear(this, boundingBox.addCoord(dx, dy, dz))) {
                moveEntity(dx, dy, dz);
                motionX = motionZ = 0;
                motionY = dy;
                if (++stepTick == step.ticks) step = null;
                return;
            }
            step = null;
            motionX = motionY = motionZ = 0;
        }
        if (flightTicks > 0) {
            --flightTicks;
            double angle = rotationYaw * Math.PI / 180;
            double dx = -Math.sin(angle) * 0.23, dz = Math.cos(angle) * 0.23;
            double dy = flightTicks > 22 ? 0.18 : flightTicks > 12 ? 0.025 : -0.15;
            AxisAlignedBB path = boundingBox.addCoord(dx, dy, dz);
            if (AerialTerrain.clear(this, path)) {
                moveEntity(dx, dy, dz);
                fallDistance = 0;
                motionX = dx;
                motionY = dy;
                motionZ = dz;
                return;
            }
            flightTicks = 0;
            motionX = motionY = motionZ = 0;
        }
        if (!AerialTerrain.loaded(worldObj, boundingBox.addCoord(motionX, motionY, motionZ))) {
            motionX = motionY = motionZ = 0;
            return;
        }
        double x = posX, z = posZ;
        boolean walking = onGround && pose() == Pose.WALK;
        super.moveEntityWithHeading(0, 0);
        if (!onGround) return;
        if (pose() == Pose.FLY || pose() == Pose.HOP || pose() == Pose.CLIMB) {
            pose(Pose.LAND);
            decisionDelay = 5;
        } else if (walking) {
            double dx = posX - x, dz = posZ - z;
            double travel = Math.sqrt(dx * dx + dz * dz);
            gait += (float) (travel * 11);
            stride = (float) Math.min(1, travel / WALK_SPEED);
            if (isCollidedHorizontally) redirect();
        }
    }

    @Override
    protected void fall(float distance) {}

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("ZtoMagpieAge", lifeTicks);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        lifeTicks = MathHelper.clamp_int(tag.getInteger("ZtoMagpieAge"), 0, MAX_AGE);
    }
}
