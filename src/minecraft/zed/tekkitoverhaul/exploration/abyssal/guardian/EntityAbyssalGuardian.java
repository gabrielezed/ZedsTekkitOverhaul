package zed.tekkitoverhaul.exploration.abyssal.guardian;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;

/** Native hostile damage and persistence with a water-only movement controller. */
public final class EntityAbyssalGuardian extends EntityMob {
    public static final String NAME = "ZTO.AbyssalGuardian";
    public static final String TEXTURE = "/zed/textures/abyssal/guardian.png";
    private static final String SOUND = "zto.guardian.";
    private static final int WATCH_STATE = 20;
    private static final int DETECT = 18, LEASH = 28;
    private static final int WINDUP_TICKS = 10, LUNGE_TICKS = 8, RECOVERY_TICKS = 32;
    private static final double CRUISE = 0.14, CHASE = 0.23, BURST = 0.38;
    private static final double HIT_RANGE = 1.45;

    public enum State { PATROL, ALERT, PURSUE, WIND_UP, LUNGE, RECOVER, RETURN }
    private static final State[] STATES = State.values();
    private final SwimRoute route = new SwimRoute();
    private EntityPlayer target;
    private int homeX, homeY, homeZ, slot = -1;
    private long siteId;
    private boolean homeKnown, invalid, hit;
    private int phaseTicks, blocked, patrolTicks, dryTicks;
    private double goalX, goalY, goalZ, attackX, attackY, attackZ;

    public EntityAbyssalGuardian(World world) {
        super(world);
        setSize(0.68F, 0.70F);
        texture = TEXTURE;
        attackStrength = 4;
    }

    @Override protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WATCH_STATE, Byte.valueOf((byte) 0));
    }

    @Override public int getMaxHealth() { return 20; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean handleWaterMovement() {
        return worldObj.handleMaterialAcceleration(boundingBox.contract(0.01, 0.08, 0.01), Material.water, this);
    }
    @Override protected boolean canDespawn() { return slot < 0; }
    @Override protected String getLivingSound() { return SOUND + "ambient"; }
    @Override protected String getHurtSound() { return SOUND + "hurt"; }
    @Override protected String getDeathSound() { return SOUND + "death"; }
    @Override protected float getSoundVolume() { return 0.48F; }
    @Override public int getTalkInterval() { return 180; }

    public State state() { return STATES[dataWatcher.getWatchableObjectByte(WATCH_STATE)]; }
    public boolean siteOwned() { return slot >= 0; }
    public long siteIdentity() { return siteId; }

    void home(int[] position, Site site, int allocatedSlot) {
        homeX = position[0];
        homeY = position[1];
        homeZ = position[2];
        homeKnown = true;
        siteId = site == null ? 0 : site.identity();
        slot = allocatedSlot;
    }

    boolean engaged() {
        State state = state();
        return target != null && state != State.PATROL && state != State.RETURN;
    }

    private void state(State state, int ticks) {
        dataWatcher.updateObject(WATCH_STATE, Byte.valueOf((byte) state.ordinal()));
        phaseTicks = ticks;
    }

    private void release() {
        target = null;
        blocked = 0;
        route.reset();
        state(State.RETURN, 0);
    }

    private Site site() {
        Site result = ObservatorySites.nearby(worldObj, homeX, homeZ);
        return result != null && result.identity() == siteId && result.revision() == 1 && slot < result.count() ? result : null;
    }

    @Override public void onUpdate() {
        if (!worldObj.isRemote && slot >= 0 && isEntityAlive()) {
            if (worldObj.difficultySetting == 0) {
                Site site = site();
                if (site != null) site.finish(slot, false);
                setDead();
                return;
            }
            if ((ticksExisted & 31) == 0) {
                Site site = site();
                if (invalid || site == null || !site.consumed(slot) || site.terminal(slot) || duplicate()) {
                    setDead();
                    return;
                }
            }
        }
        super.onUpdate();
    }

    private boolean duplicate() {
        AxisAlignedBB area = AxisAlignedBB.getBoundingBoxFromPool(homeX - 33, homeY - 33, homeZ - 33,
                homeX + 34, homeY + 34, homeZ + 34);
        for (Object other : worldObj.getEntitiesWithinAABB(EntityAbyssalGuardian.class, area)) {
            EntityAbyssalGuardian actor = (EntityAbyssalGuardian) other;
            if (!actor.isDead && !actor.invalid && actor.siteId == siteId && actor.slot == slot && actor.entityId < entityId) {
                System.err.println("[ZTO] Suppressed duplicate Observatory Guardian " + Long.toHexString(siteId) + "/" + slot);
                return true;
            }
        }
        return false;
    }

    @Override public void onDeath(DamageSource source) {
        if (!worldObj.isRemote && siteOwned() && !invalid) {
            Site site = site();
            if (site != null) site.finish(slot, true);
        }
        super.onDeath(source);
    }

    @Override protected void updateEntityActionState() {
        if (worldObj.isRemote || worldObj.difficultySetting == 0) return;
        if (!homeKnown) home(new int[] {SwimRoute.floor(posX), SwimRoute.floor(posY), SwimRoute.floor(posZ)}, null, -1);
        ++entityAge;
        if (!siteOwned()) despawnEntity();
        if (isDead) return;
        if (!isInWater()) {
            release();
            if (++dryTicks > 160 && dryTicks % 20 == 0) attackEntityFrom(DamageSource.drown, 1);
            return;
        }
        dryTicks = 0;
        if (target != null && (!eligible(target) || homeDistance(target.posX, target.posY, target.posZ) > LEASH * LEASH
                || homeDistance(posX, posY, posZ) > (LEASH + 2) * (LEASH + 2))) release();
        switch (state()) {
            case PATROL:
                patrol();
                if ((ticksExisted + entityId) % 8 == 0) acquire();
                break;
            case ALERT:
                face(target.posX - posX, target.posY + 0.45 - posY, target.posZ - posZ);
                if (--phaseTicks <= 0) state(State.PURSUE, 0);
                break;
            case PURSUE:
                pursue();
                break;
            case WIND_UP:
                face(attackX, attackY, attackZ);
                if (--phaseTicks <= 0) {
                    hit = false;
                    worldObj.playSoundAtEntity(this, SOUND + "lunge", 0.55F, 1);
                    state(State.LUNGE, LUNGE_TICKS);
                }
                break;
            case LUNGE:
                lunge();
                break;
            case RECOVER:
                if (--phaseTicks <= 0) state(State.PURSUE, 0);
                break;
            case RETURN:
                if (homeDistance(posX, posY, posZ) <= 2.25) {
                    patrolTicks = 0;
                    state(State.PATROL, 0);
                } else if (!route.steer(this, homeX + 0.5, homeY + 0.08, homeZ + 0.5, CRUISE)) {
                    if (blocked++ > 100) {
                        route.reset();
                        blocked = 0;
                        patrolTicks = 0;
                        state(State.PATROL, 0);
                    }
                } else blocked = 0;
                break;
            default:
                break;
        }
    }

    private void acquire() {
        EntityPlayer candidate = worldObj.getClosestVulnerablePlayerToEntity(this, DETECT);
        if (!eligible(candidate) || homeDistance(candidate.posX, candidate.posY, candidate.posZ) > 24 * 24
                || !visible(candidate) || GuardianEncounters.engagements(candidate) >= 6) return;
        target = candidate;
        blocked = 0;
        route.reset();
        state(State.ALERT, 12);
    }

    private boolean eligible(EntityPlayer player) {
        return player != null && player.isEntityAlive() && !player.capabilities.disableDamage
                && player.worldObj == worldObj && !player.isPlayerSleeping()
                && (player.isInWater() || player.isInsideOfMaterial(Material.water));
    }

    private boolean visible(EntityPlayer player) {
        return worldObj.checkChunksExist(SwimRoute.floor(Math.min(posX, player.posX)), 0,
                SwimRoute.floor(Math.min(posZ, player.posZ)), SwimRoute.floor(Math.max(posX, player.posX)), 127,
                SwimRoute.floor(Math.max(posZ, player.posZ))) && canEntityBeSeen(player);
    }

    boolean engages(EntityPlayer player) { return target == player && engaged() && isEntityAlive(); }

    private void patrol() {
        if (--patrolTicks <= 0 || getDistanceSq(goalX, goalY, goalZ) < 1) {
            patrolTicks = 24 + rand.nextInt(32);
            int radius = 6 + rand.nextInt(5);
            goalX = homeX + 0.5 + rand.nextInt(radius * 2 + 1) - radius;
            goalY = homeY + 0.08 + rand.nextInt(9) - 4;
            goalZ = homeZ + 0.5 + rand.nextInt(radius * 2 + 1) - radius;
            if (!SwimRoute.clear(this, goalX, goalY, goalZ)) {
                goalX = homeX + 0.5;
                goalY = homeY + 0.08;
                goalZ = homeZ + 0.5;
            }
            route.reset();
        }
        route.steer(this, goalX, goalY, goalZ, CRUISE);
    }

    private void pursue() {
        double y = target.boundingBox.minY + 0.42;
        double dx = target.posX - posX, dy = y - posY, dz = target.posZ - posZ;
        double range = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (range > 0.02 && range < 3.1 && visible(target)
                && SwimRoute.swept(this, posX, posY, posZ, dx * 0.85, dy * 0.85, dz * 0.85)) {
            attackX = dx / range;
            attackY = dy / range;
            attackZ = dz / range;
            motionX *= 0.2;
            motionY *= 0.2;
            motionZ *= 0.2;
            worldObj.playSoundAtEntity(this, SOUND + "warning", 0.62F, 1);
            state(State.WIND_UP, WINDUP_TICKS);
        } else if (route.steer(this, target.posX, y, target.posZ, CHASE)) {
            blocked = 0;
        } else if (++blocked > 46) release();
    }

    private void lunge() {
        double dx = target.posX - posX, dy = target.boundingBox.minY + 0.42 - posY, dz = target.posZ - posZ;
        if (!hit && dx * dx + dy * dy + dz * dz <= HIT_RANGE * HIT_RANGE
                && dx * attackX + dy * attackY + dz * attackZ > -0.2 && visible(target)
                && SwimRoute.swept(this, posX, posY, posZ, dx * 0.8, dy * 0.8, dz * 0.8)) {
            hit = true;
            attackEntityAsMob(target);
            state(State.RECOVER, RECOVERY_TICKS);
        } else if (--phaseTicks <= 0 || !SwimRoute.swept(this, posX, posY, posZ,
                attackX * BURST, attackY * BURST, attackZ * BURST)) {
            state(State.RECOVER, RECOVERY_TICKS);
        } else thrust(attackX, attackY, attackZ, BURST);
    }

    private double homeDistance(double x, double y, double z) {
        double dx = x - homeX - 0.5, dy = y - homeY, dz = z - homeZ - 0.5;
        return dx * dx + dy * dy + dz * dz;
    }

    private void thrust(double x, double y, double z, double speed) {
        SwimRoute.thrust(this, x, y, z, speed);
    }

    private void face(double x, double y, double z) {
        SwimRoute.face(this, x, y, z);
    }

    @Override public void moveEntityWithHeading(float strafe, float forward) {
        if (!isInWater()) {
            super.moveEntityWithHeading(0, 0);
            return;
        }
        SwimRoute.move(this, state() == State.WIND_UP || state() == State.RECOVER ? 0.65 : 0.85);
    }

    @Override public boolean addEntityID(NBTTagCompound tag) {
        return !AquaticDevelopment.manual(this) && super.addEntityID(tag);
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("ZtoGuardianHome", homeKnown);
        tag.setInteger("ZtoHomeX", homeX);
        tag.setInteger("ZtoHomeY", homeY);
        tag.setInteger("ZtoHomeZ", homeZ);
        tag.setLong("ZtoObservatory", siteId);
        tag.setInteger("ZtoGuardianSlot", slot);
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (!tag.hasKey("ZtoGuardianSlot")) return;
        slot = tag.getInteger("ZtoGuardianSlot");
        siteId = tag.getLong("ZtoObservatory");
        homeKnown = tag.getBoolean("ZtoGuardianHome");
        homeX = tag.getInteger("ZtoHomeX");
        homeY = tag.getInteger("ZtoHomeY");
        homeZ = tag.getInteger("ZtoHomeZ");
        invalid = slot < -1 || slot >= 24 || slot >= 0 && (!homeKnown || homeY < 8 || homeY > 60);
        if (invalid && slot < 0) slot = 24;
    }
}
