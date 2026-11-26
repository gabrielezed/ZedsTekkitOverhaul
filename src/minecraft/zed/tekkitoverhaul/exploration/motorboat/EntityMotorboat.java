package zed.tekkitoverhaul.exploration.motorboat;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityBoat;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryBasic;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.World;

public final class EntityMotorboat extends EntityBoat {
    public static final int CAPACITY = 4000;
    public static final int BUCKET_VOLUME = 1000;
    static final int INPUT = 0;
    static final int RETURN = 1;
    private static final int TICKS_PER_MB = 2;
    private static final int PROPELLER_TICKS_PER_TURN = 12;
    private static final double MAX_SPEED = 1D;
    private static final double REVERSE_SPEED = 0.28D;
    private static final double ACCELERATION = 0.075D;
    private static final float STEERING = 7F;
    private static final double COAST_DRAG = 0.94D;

    // The local client supplies ordinary movement input and opens the native container screen.
    public interface Controls {
        float throttle(EntityPlayer rider);
        float steering(EntityPlayer rider);
        void openFueling(EntityPlayer player, EntityMotorboat boat);
    }

    private static Controls controls;
    final InventoryBasic buckets = new InventoryBasic("Motorboat", 2) {
        @Override
        public int getInventoryStackLimit() { return 1; }
    };
    private int fuel;
    private int poweredTicks;
    private int propulsionDirection;
    private int propellerTick;
    private int ticksUntilSound;

    public EntityMotorboat(World world) {
        super(world);
    }

    EntityMotorboat(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public static void setControls(Controls input) { controls = input; }
    public int fuel() { return fuel; }
    public boolean isPropelling() { return propulsionDirection != 0; }
    public boolean hasRoomForBucket() { return CAPACITY - fuel >= BUCKET_VOLUME; }

    public float propellerTurns(float partialTick) {
        // Interpolate over the most recent signed step, even across a full turn.
        return (propellerTick + (partialTick - 1F) * propulsionDirection) / PROPELLER_TICKS_PER_TURN;
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (!canUse(player)) return true;
        if (!player.isSneaking()) return super.interact(player);
        if (!worldObj.isRemote && controls != null) controls.openFueling(player, this);
        return true;
    }

    boolean canUse(EntityPlayer player) {
        return !isDead && player.isEntityAlive() && player.worldObj == worldObj
                && player.getDistanceSqToEntity(this) < 64D
                && (riddenByEntity == null || riddenByEntity == player);
    }

    @Override
    public void onUpdate() {
        if (worldObj.isRemote) {
            super.onUpdate();
            return;
        }
        onEntityUpdate();
        if (isDead) return;
        if (getTimeSinceHit() > 0) setTimeSinceHit(getTimeSinceHit() - 1);
        if (getDamageTaken() > 0) setDamageTaken(getDamageTaken() - 1);
        if (riddenByEntity != null && riddenByEntity.isDead) riddenByEntity = null;
        transferBucket();

        double immersion = immersion();
        if (immersion < 1D) motionY += 0.04D * (2D * immersion - 1D);
        else motionY = Math.max(motionY, motionY * 0.5D) + 0.007D;
        motionY = Math.max(-0.3D, Math.min(0.2D, motionY));

        EntityPlayer rider = riddenByEntity instanceof EntityPlayer ? (EntityPlayer) riddenByEntity : null;
        float throttle = controls != null && rider != null && immersion > 0D && fuel > 0
                ? controls.throttle(rider) : 0F;
        if (throttle != 0F) {
            rotationYaw -= controls.steering(rider) * STEERING;
            if (rotationYaw >= 360F || rotationYaw <= -360F) {
                float wrapped = rotationYaw % 360F;
                prevRotationYaw += wrapped - rotationYaw;
                rotationYaw = wrapped;
            }
            accelerate(throttle);
        } else {
            double drag = onGround ? 0.5D : COAST_DRAG;
            motionX *= drag;
            motionZ *= drag;
        }
        limitSpeed();
        moveEntity(motionX, motionY, motionZ);
        motionY *= 0.95D;
        if (isCollidedHorizontally) {
            motionX *= 0.35D;
            motionZ *= 0.35D;
        }

        double heading = Math.toRadians(rotationYaw);
        double advance = -(posX - prevPosX) * Math.cos(heading) - (posZ - prevPosZ) * Math.sin(heading);
        int thrustDirection = (int) Math.signum(throttle);
        propulsionDirection = advance * thrustDirection > 0.01D ? thrustDirection : 0;
        if (ticksUntilSound > 0) --ticksUntilSound;
        if (isPropelling()) {
            propellerTick = (propellerTick + propulsionDirection) % PROPELLER_TICKS_PER_TURN;
            if (++poweredTicks >= TICKS_PER_MB) {
                fuel--;
                poweredTicks = 0;
            }
            poweredFeedback(heading);
        }
        for (Object candidate : worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(0.2D, 0D, 0.2D))) {
            if (candidate instanceof EntityBoat && ((EntityBoat) candidate).canBePushed()) {
                ((EntityBoat) candidate).applyEntityCollision(this);
            }
        }
    }

    private double immersion() {
        int wet = 0;
        for (int slice = 0; slice < 5; slice++) {
            double depth = (boundingBox.maxY - boundingBox.minY) / 5D;
            double bottom = boundingBox.minY + slice * depth - 0.125D;
            if (worldObj.isAABBInMaterial(AxisAlignedBB.getBoundingBoxFromPool(
                    boundingBox.minX, bottom, boundingBox.minZ,
                    boundingBox.maxX, bottom + depth, boundingBox.maxZ), Material.water)) wet++;
        }
        return wet / 5D;
    }

    private void accelerate(float throttle) {
        double yaw = Math.toRadians(rotationYaw);
        double target = throttle > 0 ? MAX_SPEED : -REVERSE_SPEED;
        double changeX = -Math.cos(yaw) * target - motionX;
        double changeZ = -Math.sin(yaw) * target - motionZ;
        double change = Math.sqrt(changeX * changeX + changeZ * changeZ);
        double step = change > ACCELERATION ? ACCELERATION / change : 1D;
        motionX += changeX * step;
        motionZ += changeZ * step;
    }

    private void limitSpeed() {
        double speed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed > MAX_SPEED) {
            motionX *= MAX_SPEED / speed;
            motionZ *= MAX_SPEED / speed;
        }
    }

    private void poweredFeedback(double heading) {
        if (ticksUntilSound == 0) {
            worldObj.playSoundEffect(posX, posY, posZ, "zto.motorboat.running", 0.22F, 1F);
            ticksUntilSound = 8;
        }
        double sternX = Math.cos(heading);
        double sternZ = Math.sin(heading);
        for (int side = -1; side <= 1; side += 2) {
            double spread = side * (0.2D + rand.nextDouble() * 0.18D);
            worldObj.spawnParticle("splash", posX + sternX - spread * sternZ, boundingBox.minY + 0.12D,
                    posZ + sternZ + spread * sternX, sternX * 0.13D, 0.01D, sternZ * 0.13D);
        }
    }

    private void transferBucket() {
        if (!hasRoomForBucket() || buckets.getStackInSlot(RETURN) != null
                || !MotorboatRegistry.isGasoline(buckets.getStackInSlot(INPUT))) return;
        buckets.decrStackSize(INPUT, 1);
        fuel += BUCKET_VOLUME;
        buckets.setInventorySlotContents(RETURN, new ItemStack(Item.bucketEmpty));
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        if (worldObj.isRemote || isDead || damage <= 0) return true;
        setForwardDirection(-getForwardDirection());
        setTimeSinceHit(10);
        setDamageTaken(getDamageTaken() + Math.min(damage, 5) * 10);
        setBeenAttacked();
        Entity attacker = source.getEntity();
        if (getDamageTaken() > 40
                || (attacker instanceof EntityPlayer && ((EntityPlayer) attacker).capabilities.isCreativeMode)) {
            if (riddenByEntity != null) riddenByEntity.mountEntity(this);
            setDead();
            ItemStack recovered = new ItemStack(MotorboatRegistry.motorboatItem());
            if (fuel > 0) {
                NBTTagCompound tank = new NBTTagCompound();
                writeTank(tank);
                recovered.setTagCompound(tank);
            }
            entityDropItem(recovered, 0F);
            for (int slot = 0; slot < buckets.getSizeInventory(); slot++) {
                ItemStack bucket = buckets.getStackInSlotOnClosing(slot);
                if (bucket != null) entityDropItem(bucket, 0F);
            }
        }
        return true;
    }

    void readTank(NBTTagCompound tag) {
        fuel = Math.min(CAPACITY, Math.max(0, readInt(tag, "ZTO_MotorboatFuel")));
        poweredTicks = fuel > 0 ? Math.min(TICKS_PER_MB - 1, Math.max(0, readInt(tag, "ZTO_MotorboatTicks"))) : 0;
    }

    private void writeTank(NBTTagCompound tag) {
        tag.setInteger("ZTO_MotorboatFuel", fuel);
        tag.setInteger("ZTO_MotorboatTicks", poweredTicks);
    }

    private static int readInt(NBTTagCompound tag, String key) {
        return tag != null && tag.getTag(key) instanceof NBTTagInt ? tag.getInteger(key) : 0;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        writeTank(tag);
        for (int slot = 0; slot < buckets.getSizeInventory(); slot++) {
            ItemStack bucket = buckets.getStackInSlot(slot);
            if (bucket != null) tag.setCompoundTag("Bucket" + slot, bucket.writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        readTank(tag);
        for (int slot = 0; slot < buckets.getSizeInventory(); slot++) {
            ItemStack bucket = tag.getTag("Bucket" + slot) instanceof NBTTagCompound
                    ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Bucket" + slot)) : null;
            boolean valid = slot == INPUT ? MotorboatRegistry.isGasoline(bucket)
                    : bucket != null && bucket.getItem() == Item.bucketEmpty && bucket.stackSize > 0;
            buckets.setInventorySlotContents(slot, valid ? bucket : null);
        }
    }
}
