package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.EntityAISwimming;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.PathEntity;
import net.minecraft.src.PathPoint;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import net.minecraft.src.DamageSource;
import zed.tekkitoverhaul.assault.SiegeCensus;
import zed.tekkitoverhaul.survival.Shoreline;

/** Shared local-world lifecycle and bounded access to legacy navigation. */
public abstract class TacticalHostile extends EntityMob implements SiegeCensus.Participant {
    private final SiegeCensus.Actor siegeActor = new SiegeCensus.Actor();

    @Override public final SiegeCensus.Actor siegeActor() { return siegeActor; }

    @Override public void onUpdate() {
        super.onUpdate();
        if (siegeSite > 0) siegeActor.observe(this, siegeSite);
    }

    @Override public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (dead && siegeSite > 0) siegeActor.died(this, siegeSite);
    }
    private static final float PATH_RANGE = 16.0F;
    private static final int PATH_CACHE_RADIUS = (int) PATH_RANGE + 8;
    private static final double TARGET_RANGE = 16.0D;

    protected EntityPlayer target;
    private int nextTargetSearch;
    private int unseenTicks;
    private int swingTicks;
    private int previousSwingTicks;
    private boolean siteSiegeControlled;
    private int siegeSite;
    private EntityPlayer siteDefender;
    private boolean siteEngaging;
    private double siteX, siteZ;
    private long sitePathNanos, peakSitePathNanos;

    public final long sitePathNanos() { return sitePathNanos; }
    public final long peakSitePathNanos() { return peakSitePathNanos; }

    protected TacticalHostile(World world, float width, float height, String skin) {
        super(world);
        setSize(width, height);
        texture = "/zed/textures/" + skin + ".png";
        tasks.addTask(0, new EntityAISwimming(this));
        getNavigator().setAvoidsWater(true);
    }

    @Override
    public final boolean getCanSpawnHere() {
        return false;
    }

    /** Called only after the encounter director has selected this species. */
    public final boolean canSpawnForEncounter() {
        return super.getCanSpawnHere();
    }

    public final boolean canEngageEncounterPlayer(EntityPlayer player) {
        return valid(player) && loadedBetween(player);
    }

    public final void engageEncounterPlayer(EntityPlayer player) {
        if (canEngageEncounterPlayer(player)) target = player;
    }

    /** The encounter owns navigation and physical actions while this transient role is assigned. */
    public final void claimSiteSiege(EntityPlayer defender, PathPoint staging, int siteId) {
        siteSiegeControlled = true;
        siegeSite = siteId;
        siteDefender = defender;
        siteX = staging.xCoord + 0.5;
        siteZ = staging.zCoord + 0.5;
    }

    private boolean nativeSiteDespawn;
    public final boolean nativeSiteDespawn() { return nativeSiteDespawn; }

    @Override protected void despawnEntity() {
        boolean owned = siteSiegeControlled && !isDead;
        super.despawnEntity();
        if (owned && isDead) nativeSiteDespawn = true;
    }

    @Override protected boolean canDespawn() { return siegeSite == 0 && super.canDespawn(); }

    public final void releaseSiteSiege() {
        siteSiegeControlled = siteEngaging = false;
        siteDefender = null;
        disengage();
    }

    public final void holdSiteMovement() { siteEngaging = false; }

    public final void beginSiteCombat() { siteEngaging = true; }
    protected final boolean nearSiteStaging(double x, double z) {
        double dx = x - siteX, dz = z - siteZ;
        return dx * dx + dz * dz <= siteCombatRadius() * siteCombatRadius();
    }
    protected final boolean holdingSiteSiege() { return siteSiegeControlled; }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (siegeSite > 0) tag.setInteger("ZTOSiegeSite", siegeSite);
        siegeActor.write(tag);
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        siegeSite = tag.getTag("ZTOSiegeSite") instanceof NBTTagInt
                ? Math.max(0, tag.getInteger("ZTOSiegeSite")) : 0;
        siegeActor.read(tag, siegeSite);
    }

    @Override
    protected final boolean isAIEnabled() {
        return true;
    }

    @Override
    protected final void updateAITasks() {
        if (worldObj.isRemote || worldObj.difficultySetting == 0) return;
        previousSwingTicks = swingTicks;
        if (swingTicks > 0) swingTicks--;
        if (siteSiegeControlled) {
            if (siteEngaging && !independentTactics() && !siteObjective(siteDefender)) fightAtSite();
            super.updateAITasks();
            updateAttackPose();
            return;
        }
        if (independentTactics()) {
            super.updateAITasks();
            updateAttackPose();
            return;
        }
        if (target == null && ticksExisted >= nextTargetSearch) {
            nextTargetSearch = ticksExisted + 20;
            EntityPlayer player = worldObj.getClosestVulnerablePlayerToEntity(this, TARGET_RANGE);
            if (valid(player) && loadedBetween(player) && (canEntityBeSeen(player) || canAcquireUnseen(player)))
                target = player;
        }

        if (target != null) {
            if (valid(target) && loadedBetween(target)) {
                boolean visible = canEntityBeSeen(target);
                unseenTicks = visible ? 0 : unseenTicks + 1;
                if (unseenTicks < unseenLimit()) {
                    getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
                    fight(getDistanceSqToEntity(target), visible);
                } else {
                    disengage();
                }
            } else {
                disengage();
            }
        }
        if (target == null) idle();
        super.updateAITasks();
        updateAttackPose();
    }

    private void fightAtSite() {
        if (valid(siteDefender) && nearSiteStaging(posX, posZ)
                && nearSiteStaging(siteDefender.posX, siteDefender.posZ)
                && loadedBetween(siteDefender) && canEntityBeSeen(siteDefender)) {
            target = siteDefender;
            getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            fight(getDistanceSqToEntity(target), true);
        } else if (target != null) {
            disengage();
        }
    }

    private boolean valid(EntityPlayer player) {
        return player != null && player.worldObj == worldObj && player.isEntityAlive()
                && !player.capabilities.disableDamage && getDistanceSqToEntity(player) <= TARGET_RANGE * TARGET_RANGE;
    }

    private boolean loadedBetween(EntityPlayer player) {
        return worldObj.checkChunksExist(MathHelper.floor_double(Math.min(posX, player.posX)), 0,
                MathHelper.floor_double(Math.min(posZ, player.posZ)),
                MathHelper.floor_double(Math.max(posX, player.posX)), 0,
                MathHelper.floor_double(Math.max(posZ, player.posZ)));
    }

    protected final PathEntity plan(double x, double y, double z) {
        if (!onGround || y < 1 || y >= 255) return null;
        int sx = MathHelper.floor_double(posX);
        int sz = MathHelper.floor_double(posZ);
        // World#getEntityPathToXYZ constructs a ChunkCache covering this entire square.
        if (!worldObj.checkChunksExist(sx - PATH_CACHE_RADIUS, 0, sz - PATH_CACHE_RADIUS,
                sx + PATH_CACHE_RADIUS, 0, sz + PATH_CACHE_RADIUS)) return null;
        long started = siteSiegeControlled ? System.nanoTime() : 0;
        PathEntity path = worldObj.getEntityPathToXYZ(this, MathHelper.floor_double(x), MathHelper.floor_double(y),
                MathHelper.floor_double(z), PATH_RANGE, true, false, true, false);
        if (siteSiegeControlled) {
            long nanos = System.nanoTime() - started;
            sitePathNanos += nanos;
            peakSitePathNanos = Math.max(peakSitePathNanos, nanos);
        }
        return path;
    }

    @Override
    public final void moveEntityWithHeading(float strafe, float forward) {
        if (!worldObj.isRemote && onGround && !isInWater() && !getNavigator().noPath()
                && (strafe != 0 || forward != 0)) {
            double bearing = Math.toRadians(rotationYaw) + Math.atan2(-strafe, forward);
            double dx = motionX - Math.sin(bearing) * 0.45D;
            double dz = motionZ + Math.cos(bearing) * 0.45D;
            if (!Shoreline.mayStep(this, dx, dz)) {
                getNavigator().clearPathEntity();
                if (hurtTime == 0) motionX = motionZ = 0;
                super.moveEntityWithHeading(0, 0);
                return;
            }
        }
        super.moveEntityWithHeading(strafe, forward);
    }

    protected final boolean reaches(PathEntity path, double x, double y, double z) {
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        return end != null && end.xCoord == MathHelper.floor_double(x)
                && end.yCoord == MathHelper.floor_double(y) && end.zCoord == MathHelper.floor_double(z);
    }

    protected final void disengage() {
        target = null;
        unseenTicks = 0;
        nextTargetSearch = ticksExisted + 100;
        getNavigator().clearPathEntity();
        resetTactics();
    }

    protected final void swing() {
        swingTicks = 10;
        previousSwingTicks = 10;
    }

    @Override
    public final float getSwingProgress(float partialTick) {
        if (previousSwingTicks == 0) return 0;
        return (10 - previousSwingTicks + (previousSwingTicks - swingTicks) * partialTick) / 10;
    }

    protected void idle() {}

    protected boolean canAcquireUnseen(EntityPlayer player) { return false; }

    protected double siteCombatRadius() { return 8.0D; }

    protected boolean carriesSiteCargo() { return false; }
    protected boolean siteObjective(EntityPlayer defender) { return false; }

    protected boolean independentTactics() { return false; }

    protected void updateAttackPose() {}

    protected final Vec3D eyes() {
        return Vec3D.createVector(posX, posY + getEyeHeight(), posZ);
    }

    protected abstract int unseenLimit();
    protected abstract void fight(double distanceSq, boolean visible);
    protected abstract void resetTactics();
}
