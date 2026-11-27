package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.EntityArrow;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

/** Native arrow flight and damage with a local lifetime and loaded-chunk guard. */
public final class DiverArrow extends EntityArrow {
    public DiverArrow(World world) { super(world); }

    DiverArrow(World world, EntityLiving shooter, EntityLiving target) {
        super(world, shooter, target, 2.15F, 4);
        setDamage(1.3);
    }

    @Override public void onUpdate() {
        if (ticksExisted >= 30 || !worldObj.checkChunksExist(
                SwimRoute.floor(Math.min(posX, posX + motionX) - 1), 0,
                SwimRoute.floor(Math.min(posZ, posZ + motionZ) - 1),
                SwimRoute.floor(Math.max(posX, posX + motionX) + 1), 127,
                SwimRoute.floor(Math.max(posZ, posZ + motionZ) + 1))) {
            setDead();
            return;
        }
        super.onUpdate();
    }

    @Override public boolean addEntityID(NBTTagCompound tag) { return false; }
    @Override public void readEntityFromNBT(NBTTagCompound tag) { setDead(); }
}
