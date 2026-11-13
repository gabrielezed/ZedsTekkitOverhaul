package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.EntityAISwimming;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.PathEntity;
import net.minecraft.src.PathPoint;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

/** Shared local-world lifecycle and bounded access to legacy navigation. */
public abstract class TacticalHostile extends EntityMob {
    private static final float PATH_RANGE = 16.0F;
    private static final int PATH_CACHE_RADIUS = (int) PATH_RANGE + 8;
    private static final double TARGET_RANGE = 16.0D;

    protected EntityPlayer target;
    private int nextTargetSearch;
    private int unseenTicks;
    private int swingTicks;
    private int previousSwingTicks;

    protected TacticalHostile(World world, float width, float height, String skin) {
        super(world);
        setSize(width, height);
        texture = "/zed/textures/" + skin + ".png";
        tasks.addTask(0, new EntityAISwimming(this));
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

    @Override
    protected final boolean isAIEnabled() {
        return true;
    }

    @Override
    protected final void updateAITasks() {
        if (worldObj.isRemote || worldObj.difficultySetting == 0) return;
        previousSwingTicks = swingTicks;
        if (swingTicks > 0) swingTicks--;
        if (target == null && ticksExisted >= nextTargetSearch) {
            nextTargetSearch = ticksExisted + 20;
            EntityPlayer player = worldObj.getClosestVulnerablePlayerToEntity(this, TARGET_RANGE);
            if (valid(player) && loadedBetween(player) && canEntityBeSeen(player)) target = player;
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
        super.updateAITasks();
        updateAttackPose();
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
        return worldObj.getEntityPathToXYZ(this, MathHelper.floor_double(x), MathHelper.floor_double(y),
                MathHelper.floor_double(z), PATH_RANGE, true, false, true, false);
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

    protected void updateAttackPose() {}

    protected final Vec3D eyes() {
        return Vec3D.createVector(posX, posY + getEyeHeight(), posZ);
    }

    protected abstract int unseenLimit();
    protected abstract void fight(double distanceSq, boolean visible);
    protected abstract void resetTactics();
}
