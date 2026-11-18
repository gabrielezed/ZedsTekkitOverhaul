package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
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
    public enum Pose { WATCH, WALK, HOP, FLY, PECK, PERCH }
    private int decisionDelay, flightTicks, lifeTicks;
    private EntityItem curiosity;
    private boolean startled;

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

    @Override
    protected void updateEntityActionState() {
        if (++lifeTicks >= MAX_AGE) {
            setDead();
            return;
        }
        ++entityAge;
        despawnEntity();
        if (isDead) return;

        if (flightTicks > 0) {
            --flightTicks;
            pose(Pose.FLY);
            return;
        }
        if (!onGround) return;
        if (startled) {
            startled = false;
            curiosity = null;
            launch();
            return;
        }
        if (--decisionDelay <= 0) chooseActivity();
        if (curiosity != null) approachItem();
        if (pose() == Pose.WALK || pose() == Pose.HOP) {
            double angle = rotationYaw * Math.PI / 180;
            double speed = pose() == Pose.WALK ? 0.075 : 0.12;
            motionX = -Math.sin(angle) * speed;
            motionZ = Math.cos(angle) * speed;
            if (pose() == Pose.HOP && decisionDelay % 9 == 0) motionY = 0.18;
        } else {
            motionX = motionZ = 0;
        }
    }

    private void chooseActivity() {
        EntityPlayer observer = worldObj.getClosestPlayerToEntity(this, 4.0);
        if (observer != null && !observer.isSneaking() && !observer.capabilities.isCreativeMode
                && AerialTerrain.loaded(worldObj, posX, posZ, observer.posX, observer.posZ)
                && canEntityBeSeen(observer)) {
            face(posX - observer.posX, posZ - observer.posZ);
            curiosity = null;
            launch();
            return;
        }
        int roll = rand.nextInt(10);
        curiosity = roll < 4 ? nearbyItem() : null;
        if (curiosity != null) {
            decisionDelay = 36;
            return;
        }
        rotationYaw += rand.nextInt(141) - 70;
        decisionDelay = 18 + rand.nextInt(28);
        if (roll == 9) launch();
        else pose(roll < 4 ? restingPose() : roll < 8 ? Pose.WALK : Pose.HOP);
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
                    && AerialTerrain.loaded(worldObj, posX, posZ, item.posX, item.posZ)
                    && canEntityBeSeen(item)) return item;
        }
        return null;
    }

    private void approachItem() {
        if (curiosity.isDead || getDistanceSqToEntity(curiosity) > 25
                || !AerialTerrain.loaded(worldObj, posX, posZ, curiosity.posX, curiosity.posZ)
                || !canEntityBeSeen(curiosity)) {
            curiosity = null;
            pose(Pose.WATCH);
        } else {
            face(curiosity.posX - posX, curiosity.posZ - posZ);
            pose(getDistanceSqToEntity(curiosity) < 0.56 ? Pose.PECK : Pose.WALK);
        }
    }

    private void face(double x, double z) { rotationYaw = (float) Math.toDegrees(Math.atan2(-x, z)); }

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
        if (flightTicks > 0) {
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
        super.moveEntityWithHeading(0, 0);
        if (onGround && pose() == Pose.FLY) {
            Pose landed = restingPose();
            pose(landed);
            if (landed == Pose.PERCH) decisionDelay = 60;
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
