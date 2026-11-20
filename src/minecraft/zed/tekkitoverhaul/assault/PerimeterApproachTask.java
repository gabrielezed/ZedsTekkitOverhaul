package zed.tekkitoverhaul.assault;

import java.lang.reflect.Field;
import net.minecraft.src.EntityAIBase;
import net.minecraft.src.EntityAITasks;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityMob;
import net.minecraft.src.PathPoint;
import zed.tekkitoverhaul.survival.tactical.EntityTank;

/** A transient native action task with a finite, prevalidated set of waypoints. */
final class PerimeterApproachTask extends EntityAIBase {
    private static final Field ACTIONS = actionField();
    private final EntityMob mob;
    private PathPoint[] route;
    private boolean engaged = true, hold;
    private int next = 1;

    private PerimeterApproachTask(EntityMob mob, SiteApproach approach, boolean hold) {
        this.mob = mob;
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

    @Override public boolean shouldExecute() { return engaged; }

    @Override public void updateTask() {
        // The native scheduler selects tasks before running its update pass.
        if (!engaged) return;
        mob.getNavigator().clearPathEntity();
        if (arrived()) {
            if (hold) {
                PathPoint goal = route[route.length - 1];
                if (horizontalDistanceSq(goal) >= 0.16) move(goal);
            }
            return;
        }
        PathPoint point = route[next];
        if (horizontalDistanceSq(point) < 0.16 && Math.abs(mob.boundingBox.minY - point.yCoord) < 0.75) {
            next++;
            if (arrived()) {
                if (!hold) release();
                return;
            }
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

    int steps() { return next - 1; }
    boolean arrived() { return next == route.length; }
    PathPoint[] waypoints() { return route; }

    void advance(PathPoint[] passage) {
        PathPoint[] extended = new PathPoint[route.length + passage.length];
        System.arraycopy(route, 0, extended, 0, route.length);
        System.arraycopy(passage, 0, extended, route.length, passage.length);
        route = extended;
        hold = false;
    }

    void release() { engaged = false; }
}
