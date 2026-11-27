package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

public final class EntityDiverSkeleton extends EntitySkeleton {
    public static final String NAME = "ZTO.DiverSkeleton";
    private final DiverHunt hunt = new DiverHunt();

    public EntityDiverSkeleton(World world) { super(world); }

    @Override protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(20, Byte.valueOf((byte) 0));
    }

    public boolean aiming() { return dataWatcher.getWatchableObjectByte(20) != 0; }

    void aim(boolean raised) { dataWatcher.updateObject(20, Byte.valueOf((byte) (raised ? 1 : 0))); }

    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isAIEnabled() { return false; }
    @Override public void setFire(int seconds) { if (!isInWater()) super.setFire(seconds); }
    @Override protected void updateEntityActionState() { ++entityAge; despawnEntity(); if (!isDead) hunt.tick(this, true); }

    @Override public void moveEntityWithHeading(float strafe, float forward) {
        if (isInWater()) SwimRoute.move(this, 0.84);
        else super.moveEntityWithHeading(0, 0);
    }

    @Override public boolean addEntityID(NBTTagCompound tag) {
        return !AquaticDevelopment.manual(this) && super.addEntityID(tag);
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) { super.writeEntityToNBT(tag); hunt.write(tag); }
    @Override public void readEntityFromNBT(NBTTagCompound tag) { super.readEntityFromNBT(tag); hunt.read(tag); }
}
