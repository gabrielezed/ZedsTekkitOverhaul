package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.EntityZombie;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

public final class EntityDiverZombie extends EntityZombie {
    public static final String NAME = "ZTO.DiverZombie";
    private final DiverHunt hunt = new DiverHunt();

    public EntityDiverZombie(World world) {
        super(world);
        attackStrength = 3;
    }

    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected boolean isAIEnabled() { return false; }
    @Override public void setFire(int seconds) { if (!isInWater()) super.setFire(seconds); }
    @Override protected void updateEntityActionState() { ++entityAge; despawnEntity(); if (!isDead) hunt.tick(this, false); }

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
