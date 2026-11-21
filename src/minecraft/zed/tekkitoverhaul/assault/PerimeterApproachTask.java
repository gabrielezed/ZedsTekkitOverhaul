package zed.tekkitoverhaul.assault;

import java.lang.reflect.Field;
import net.minecraft.src.EntityAIBase;
import net.minecraft.src.EntityAITasks;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityMob;
import net.minecraft.src.MathHelper;
import net.minecraft.src.PathPoint;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;

/** A transient native action task with a finite, prevalidated set of waypoints. */
final class PerimeterApproachTask extends EntityAIBase {
    private static final Field ACTIONS = actionField();
    // The pinned native navigator uses a 16-block search and a 16-block entity path cache margin.
    private static final int COMBAT_CACHE_RADIUS = 32;
    private final EntityMob mob;
    private final boolean infantry;
    private PathPoint[] route;
    private boolean engaged = true, hold;
    boolean nativeDespawn;
    private int next = 1;

    private PerimeterApproachTask(EntityMob mob, SiteApproach approach, boolean hold) {
        this.mob = mob;
        infantry = !(mob instanceof TacticalHostile);
        route = approach.route;
        this.hold = hold;
        setMutexBits(7);
    }

    static boolean available() { return ACTIONS != null; }

    static PerimeterApproachTask attach(EntityMob mob, SiteApproach approach, boolean hold) {
        if (!available()) return null;
        try {
            EntityAITasks tasks = (EntityAITasks) ACTIONS.get(mob);
            if (tasks == null) return null;
            PerimeterApproachTask task = new PerimeterApproachTask(mob, approach, hold);
            tasks.addTask(0, task);
            if (mob instanceof SiegeZombie) ((SiegeZombie) mob).approach = task;
            if (mob instanceof DebugSiegeSkeleton) ((DebugSiegeSkeleton) mob).approach = task;
            return task;
        } catch (IllegalAccessException failure) {
            return null;
        }
    }

    private static Field actionField() {
        try {
            // MCP 6.2 and pinned client 1.2.5. Target tasks retain their native authority.
            String name = "acq".equals(EntityLiving.class.getName()) ? "bT" : "tasks";
            Field field = EntityLiving.class.getDeclaredField(name);
            if (field.getType() != EntityAITasks.class) return null;
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException failure) {
            return null;
        } catch (SecurityException failure) {
            return null;
        }
    }

    @Override public boolean shouldExecute() { return engaged && (!arrived() || hold); }

    boolean combatDeferred() {
        return infantry && arrived() && !loadedCombatCache();
    }

    private boolean loadedCombatCache() {
        int x = MathHelper.floor_double(mob.posX), z = MathHelper.floor_double(mob.posZ);
        return mob.worldObj.checkChunksExist(x - COMBAT_CACHE_RADIUS, 0, z - COMBAT_CACHE_RADIUS,
                x + COMBAT_CACHE_RADIUS, 0, z + COMBAT_CACHE_RADIUS);
    }

    boolean updateWithoutCombat() {
        if (!engaged || loadedCombatCache()) return false;
        // Direct waypoints only use the locally validated lane. Skip native searches until their
        // full cache is available, and keep the movement and look helpers ticking.
        updateTask();
        mob.getMoveHelper().onUpdateMoveHelper();
        mob.getLookHelper().onUpdateLook();
        mob.getJumpHelper().doJump();
        return true;
    }

    @Override public void updateTask() {
        // The native scheduler selects tasks before running its update pass.
        if (!engaged) return;
        mob.getNavigator().clearPathEntity();
        if (infantry) mob.setAttackTarget(null);
        if (arrived()) {
            if (hold) {
                PathPoint goal = route[route.length - 1];
                if (horizontalDistanceSq(goal) >= 0.025) move(goal);
            }
            return;
        }
        PathPoint point = route[next];
        if (horizontalDistanceSq(point) < 0.16 && Math.abs(mob.boundingBox.minY - point.yCoord) < 0.75) {
            next++;
            if (arrived()) return;
            point = route[next];
        }
        move(point);
    }

    private double horizontalDistanceSq(PathPoint point) {
        double dx = mob.posX - point.xCoord - 0.5, dz = mob.posZ - point.zCoord - 0.5;
        return dx * dx + dz * dz;
    }

    private void move(PathPoint point) {
        mob.getMoveHelper().setMoveTo(point.xCoord + 0.5, point.yCoord, point.zCoord + 0.5,
                mob instanceof EntityTank ? 0.18F : 0.25F);
    }

    double progress() {
        if (arrived()) return steps();
        PathPoint last = route[next - 1], point = route[next];
        double dx = mob.posX - last.xCoord - 0.5, dz = mob.posZ - last.zCoord - 0.5;
        double forward = dx * (point.xCoord - last.xCoord) + dz * (point.zCoord - last.zCoord);
        return steps() + Math.max(0, Math.min(1, forward));
    }

    int steps() { return next - 1; }
    boolean arrived() { return next == route.length; }
    PathPoint[] waypoints() { return route; }

    boolean retreat() {
        if (!engaged || !arrived() || route.length < 2 || horizontalDistanceSq(route[route.length - 1]) > 1)
            return false;
        PathPoint[] reverse = new PathPoint[route.length];
        for (int i = 0; i < route.length; i++) reverse[i] = route[route.length - 1 - i];
        route = reverse;
        next = 1;
        hold = false;
        return true;
    }

    void advance(PathPoint[] passage) {
        PathPoint[] extended = new PathPoint[route.length + passage.length];
        System.arraycopy(route, 0, extended, 0, route.length);
        System.arraycopy(passage, 0, extended, route.length, passage.length);
        route = extended;
        hold = false;
    }

    void release() {
        engaged = false;
        if (mob instanceof SiegeZombie) ((SiegeZombie) mob).approach = null;
        if (mob instanceof DebugSiegeSkeleton) ((DebugSiegeSkeleton) mob).approach = null;
    }
}
