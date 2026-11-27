package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;

public final class EntityAbyssalEnderkin extends EntityMob {
    public static final String NAME = "ZTO.AbyssalEnderkin";
    public static final String TEXTURE = "/zed/textures/abyssal/enderkin.png";
    private static final int WATCH_CAST = 20;
    private static final int AIM = 18, COOL = 58;
    private static final String SOUND = "zto.abyssalenderkin.";
    private final AquaticPursuit movement = new AquaticPursuit();
    private int x, y, z, slot = -1, cooldown, aim;
    private long identity;
    private EntityPlayer target, resonanceTarget;
    private int resonanceTicks;

    public EntityAbyssalEnderkin(World world) {
        super(world);
        setSize(0.65F, 1.10F);
        texture = TEXTURE;
        experienceValue = 3;
    }

    @Override protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WATCH_CAST, Byte.valueOf((byte) 0));
    }

    @Override public int getMaxHealth() { return 12; }
    @Override public float getEyeHeight() { return 0.96F; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected String getLivingSound() { return SOUND + "ambient"; }
    @Override protected String getHurtSound() { return SOUND + "hurt"; }
    @Override protected String getDeathSound() { return SOUND + "death"; }
    @Override protected float getSoundVolume() { return 0.45F; }
    @Override public int getTalkInterval() { return 180; }

    void home(int[] station, Site site, int slot) {
        x = station[0];
        y = station[1];
        z = station[2];
        identity = site.identity();
        this.slot = slot;
    }

    void manualHome(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long siteIdentity() { return identity; }
    public int casting() { return dataWatcher.getWatchableObjectByte(WATCH_CAST); }
    boolean engages(EntityPlayer player) { return target == player && !isDead; }

    boolean canResonate(long identity, EntityPlayer player) {
        return this.identity == identity && player != null && !AquaticDevelopment.manual(this)
                && isEntityAlive() && isInWater() && aim == 0 && resonanceTicks == 0
                && player.getDistanceSq(x + 0.5, y + 0.08, z + 0.5) <= 27 * 27 && site() != null;
    }

    void resonate(EntityPlayer player) {
        resonanceTarget = player;
        resonanceTicks = CustodianBalance.RESONANCE_SIGNAL;
    }

    private Site site() {
        Site site = slot < 0 ? null : ObservatorySites.nearby(worldObj, x, z);
        return site != null && site.revision() == 2 && site.identity() == identity && slot < site.count() ? site : null;
    }

    @Override public void onUpdate() {
        if (!worldObj.isRemote && isEntityAlive() && !AquaticDevelopment.manual(this)
                && (worldObj.difficultySetting == 0 || (ticksExisted & 31) == 0)) {
            Site site = site();
            if (site == null || !site.consumed(slot) || site.terminal(slot) || duplicate()) {
                setDead();
                return;
            }
            if (worldObj.difficultySetting == 0) {
                site.finish(slot, false);
                setDead();
                return;
            }
        }
        super.onUpdate();
    }

    private boolean duplicate() {
        AxisAlignedBB area = AxisAlignedBB.getBoundingBoxFromPool(x - 34, y - 34, z - 34, x + 35, y + 35, z + 35);
        for (Object object : worldObj.getEntitiesWithinAABB(EntityAbyssalEnderkin.class, area)) {
            EntityAbyssalEnderkin other = (EntityAbyssalEnderkin) object;
            if (other.isEntityAlive() && other.identity == identity && other.slot == slot && other.entityId < entityId) return true;
        }
        return false;
    }

    @Override public void onDeath(DamageSource damage) {
        if (!worldObj.isRemote) {
            Site site = site();
            if (site != null) site.finish(slot, true);
        }
        super.onDeath(damage);
    }

    @Override protected void updateEntityActionState() {
        if (worldObj.isRemote || worldObj.difficultySetting == 0) return;
        if (cooldown > 0) cooldown--;
        if (resonanceTicks > 0 && --resonanceTicks == 0) resonanceTarget = null;
        if (casting() < 0) pose(casting() + 1);
        if (!isInWater()) {
            movement.lose();
            cancel();
            return;
        }
        EntityPlayer prospective = movement.target(this, 16, 27, x + 0.5, y + 0.08, z + 0.5);
        if (prospective != null && prospective != target && GuardianEncounters.casters(prospective, false) >= 6) {
            movement.lose();
            prospective = null;
        }
        target = prospective;
        if (target == null && resonanceTarget != null && resonanceTarget.isEntityAlive()
                && resonanceTarget.isInWater() && !resonanceTarget.capabilities.disableDamage
                && GuardianEncounters.casters(resonanceTarget, false) < 6
                && resonanceTarget.getDistanceSq(x + 0.5, y + 0.08, z + 0.5) <= 27 * 27) {
            cancel();
            movement.approach(this, resonanceTarget, 0.17);
            return;
        }
        if (target == null) {
            cancel();
            movement.patrol(this, x + 0.5, y + 0.08, z + 0.5, 0.12);
            return;
        }

        double distance = getDistanceSqToEntity(target);
        if (!AquaticPursuit.visible(this, target) || distance > 144) {
            cancel();
            movement.approach(this, target, 0.17);
            return;
        }
        if (distance < 23) {
            cancel();
            movement.reposition(this, target, 0.17);
            return;
        }
        SwimRoute.face(this, target.posX - posX, target.posY + target.getEyeHeight() * 0.75 - posY - 0.9, target.posZ - posZ);
        if (cooldown > 0) {
            if (cooldown < COOL - 10) movement.reposition(this, target, 0.11);
            return;
        }
        if (aim == 0) {
            if (GuardianEncounters.casters(target, true) >= 2 || !clearShot(hand())) {
                movement.reposition(this, target, 0.11);
                return;
            }
            worldObj.playSoundAtEntity(this, SOUND + "charge", 0.60F, 1);
        }
        pose(++aim);
        Vec3D origin = hand();
        if (aim % 4 == 0)
            worldObj.spawnParticle("reddust", origin.xCoord, origin.yCoord, origin.zCoord, 0.63, 0.18, 0.89);
        if (aim < AIM) return;
        if (clearShot(origin)) {
            worldObj.spawnEntityInWorld(new EntityEnderPulse(this, origin.xCoord, origin.yCoord, origin.zCoord,
                    target.posX, target.boundingBox.minY + 0.95, target.posZ));
            worldObj.playSoundAtEntity(this, SOUND + "release", 0.60F, 1);
            cooldown = COOL;
            pose(-8);
        } else {
            pose(0);
            cooldown = 12;
        }
        aim = 0;
    }

    private void cancel() {
        if (aim == 0) return;
        aim = 0;
        pose(0);
        cooldown = 12;
    }

    private void pose(int phase) { dataWatcher.updateObject(WATCH_CAST, Byte.valueOf((byte) phase)); }

    public static float armPitch(float pitch, int phase) {
        if (phase < 0) return -1.45F + pitch * 0.017453292F + phase * 0.025F;
        float lift = Math.min(1, phase / 8.0F);
        return lift * (-1.5707963F + pitch * 0.017453292F);
    }

    private Vec3D hand() {
        double yaw = Math.toRadians(rotationYaw);
        double pitch = armPitch(rotationPitch, casting());
        double reach = -0.40 * Math.sin(pitch);
        return Vec3D.createVector(posX - 0.25 * Math.cos(yaw) - reach * Math.sin(yaw),
                posY + 0.94 - 0.40 * Math.cos(pitch), posZ - 0.25 * Math.sin(yaw) + reach * Math.cos(yaw));
    }

    private boolean clearShot(Vec3D hand) {
        Vec3D end = Vec3D.createVector(target.posX, target.boundingBox.minY + 0.95, target.posZ);
        if (worldObj.getBlockMaterial(SwimRoute.floor(hand.xCoord), SwimRoute.floor(hand.yCoord),
                SwimRoute.floor(hand.zCoord)) != Material.water || worldObj.rayTraceBlocks_do_do(
                        Vec3D.createVector(hand.xCoord, hand.yCoord, hand.zCoord), end, false, true) != null) return false;
        AxisAlignedBB area = boundingBox.addCoord(end.xCoord - posX, end.yCoord - posY, end.zCoord - posZ)
                .expand(0.5, 0.5, 0.5);
        for (Object object : worldObj.getEntitiesWithinAABB(EntityAbyssalEnderkin.class, area)) {
            EntityAbyssalEnderkin other = (EntityAbyssalEnderkin) object;
            if (other != this && other.isEntityAlive()
                    && other.boundingBox.expand(0.12, 0.12, 0.12).calculateIntercept(hand, end) != null) return false;
        }
        return true;
    }

    @Override public void moveEntityWithHeading(float strafe, float forward) {
        if (isInWater()) SwimRoute.move(this, casting() > 0 ? 0.68 : 0.84);
        else super.moveEntityWithHeading(0, 0);
    }

    @Override public boolean addEntityID(NBTTagCompound tag) {
        return !AquaticDevelopment.manual(this) && super.addEntityID(tag);
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("ZtoAbyssalSlot", slot);
        tag.setLong("ZtoObservatory", identity);
        tag.setInteger("ZtoHomeX", x);
        tag.setInteger("ZtoHomeY", y);
        tag.setInteger("ZtoHomeZ", z);
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        slot = tag.hasKey("ZtoAbyssalSlot") ? tag.getInteger("ZtoAbyssalSlot") : -1;
        identity = tag.getLong("ZtoObservatory");
        x = tag.getInteger("ZtoHomeX");
        y = tag.getInteger("ZtoHomeY");
        z = tag.getInteger("ZtoHomeZ");
    }
}
