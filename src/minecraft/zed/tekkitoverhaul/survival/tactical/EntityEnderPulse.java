package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

/** Short-lived local-world projectile with a fixed heading and one impact. */
public final class EntityEnderPulse extends Entity {
    public static final double SPEED = 0.82D;
    public static final int DAMAGE = 2;
    public static final int LIFETIME = 19;
    private static final double HIT_MARGIN = 0.10D;
    private EntitySkirmisher owner;

    public EntityEnderPulse(World world) {
        super(world);
        setSize(0.20F, 0.20F);
    }

    public EntityEnderPulse(EntitySkirmisher shooter, double x, double y, double z,
            double aimX, double aimY, double aimZ) {
        this(shooter.worldObj);
        owner = shooter;
        setLocationAndAngles(x, y, z, 0, 0);
        double dx = aimX - x, dy = aimY - y, dz = aimZ - z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 1.0E-5D) {
            motionX = dx * SPEED / length;
            motionY = dy * SPEED / length;
            motionZ = dz * SPEED / length;
        }
    }

    public EntitySkirmisher getOwner() { return owner; }

    @Override
    public void onUpdate() {
        if (++ticksExisted >= LIFETIME) {
            setDead();
            return;
        }
        if (worldObj.isRemote) return;
        if (owner == null || worldObj.difficultySetting == 0) {
            setDead();
            return;
        }

        double x = posX + motionX, y = posY + motionY, z = posZ + motionZ;
        AxisAlignedBB sweep = boundingBox.addCoord(motionX, motionY, motionZ).expand(HIT_MARGIN, HIT_MARGIN, HIT_MARGIN);
        if (y < 0 || y >= worldObj.getHeight() || !worldObj.checkChunksExist(
                MathHelper.floor_double(sweep.minX), 0, MathHelper.floor_double(sweep.minZ),
                MathHelper.floor_double(sweep.maxX), 0, MathHelper.floor_double(sweep.maxZ))) {
            setDead();
            return;
        }

        Vec3D start = Vec3D.createVector(posX, posY, posZ);
        Vec3D end = Vec3D.createVector(x, y, z);
        // The legacy world ray mutates its start vector.
        MovingObjectPosition wall = worldObj.rayTraceBlocks(Vec3D.createVector(posX, posY, posZ), end);
        Vec3D hit = wall == null ? null : wall.hitVec;
        double nearest = hit == null ? Double.POSITIVE_INFINITY : start.squareDistanceTo(hit);
        EntityLiving victim = null;
        for (Object candidate : worldObj.getEntitiesWithinAABBExcludingEntity(this, sweep)) {
            if (!(candidate instanceof EntityLiving) || candidate == owner) continue;
            EntityLiving living = (EntityLiving) candidate;
            if (!living.isEntityAlive() || !living.canBeCollidedWith()) continue;
            AxisAlignedBB box = living.boundingBox.expand(HIT_MARGIN, HIT_MARGIN, HIT_MARGIN);
            MovingObjectPosition contact = box.calculateIntercept(start, end);
            Vec3D point = box.isVecInside(start) ? start : contact == null ? null : contact.hitVec;
            if (point != null && start.squareDistanceTo(point) < nearest) {
                nearest = start.squareDistanceTo(point);
                victim = living;
                hit = point;
            }
        }
        if (hit != null) {
            if (victim != null) victim.attackEntityFrom(DamageSource.causeThrownDamage(this, owner), DAMAGE);
            dissipate(hit);
        } else {
            setPosition(x, y, z);
        }
    }

    @Override
    public int getBrightnessForRender(float partialTick) { return 15728880; }

    void dissipate(Vec3D point) {
        for (int i = 0; i < 4; i++) {
            worldObj.spawnParticle("reddust", point.xCoord + (rand.nextFloat() - 0.5F) * 0.24D,
                    point.yCoord + (rand.nextFloat() - 0.5F) * 0.24D,
                    point.zCoord + (rand.nextFloat() - 0.5F) * 0.24D, 0.72D, 0.08D, 0.96D);
        }
        setDead();
    }

    @Override
    public boolean addEntityID(NBTTagCompound tag) { return false; }

    @Override
    protected void entityInit() {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) { setDead(); }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}
}
