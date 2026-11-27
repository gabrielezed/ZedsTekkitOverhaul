package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.NBTTagCompound;

final class DiverHunt {
    private final AquaticPursuit pursuit = new AquaticPursuit();
    private double homeX, homeY, homeZ;
    private boolean hasHome;
    private int cooldown, windup;

    void tick(EntityMob diver, boolean bow) {
        if (diver.worldObj.isRemote || diver.worldObj.difficultySetting == 0) return;
        if (!hasHome) {
            homeX = diver.posX;
            homeY = diver.posY;
            homeZ = diver.posZ;
            hasHome = true;
        }
        if (!diver.isInWater()) {
            lower(diver, bow);
            pursuit.lose();
            return;
        }
        if (cooldown > 0) cooldown--;
        EntityPlayer player = pursuit.target(diver, 15, 28, homeX, homeY, homeZ);
        if (player == null) {
            lower(diver, bow);
            pursuit.patrol(diver, homeX, homeY, homeZ, 0.10);
            return;
        }
        double distance = diver.getDistanceSq(player.posX, player.boundingBox.minY, player.posZ);
        boolean sight = AquaticPursuit.visible(diver, player);
        if (bow && distance < 3.25 * 3.25) {
            lower(diver, true);
            pursuit.reposition(diver, player, 0.15);
        } else if (distance > (bow ? 6.5 * 6.5 : 1.7 * 1.7) || !sight) {
            lower(diver, bow);
            pursuit.approach(diver, player, bow ? 0.15 : 0.17);
        } else {
            SwimRoute.face(diver, player.posX - diver.posX,
                    player.posY + player.getEyeHeight() - diver.posY - diver.getEyeHeight(), player.posZ - diver.posZ);
            if (cooldown > 0) return;
            if (bow) {
                ((EntityDiverSkeleton) diver).aim(true);
                if (++windup < 10) return;
                lower(diver, true);
                DiverArrow arrow = new DiverArrow(diver.worldObj, diver, player);
                diver.worldObj.spawnEntityInWorld(arrow);
                diver.worldObj.playSoundAtEntity(diver, "random.bow", 0.8F, 0.9F);
                cooldown = 46;
            } else {
                diver.attackEntityAsMob(player);
                cooldown = 22;
            }
        }
    }

    private void lower(EntityMob diver, boolean bow) {
        if (!bow || windup == 0) return;
        windup = 0;
        ((EntityDiverSkeleton) diver).aim(false);
    }

    void write(NBTTagCompound tag) {
        if (!hasHome) return;
        tag.setDouble("ZtoDiveX", homeX);
        tag.setDouble("ZtoDiveY", homeY);
        tag.setDouble("ZtoDiveZ", homeZ);
    }

    void read(NBTTagCompound tag) {
        hasHome = tag.hasKey("ZtoDiveY");
        homeX = tag.getDouble("ZtoDiveX");
        homeY = tag.getDouble("ZtoDiveY");
        homeZ = tag.getDouble("ZtoDiveZ");
    }
}
