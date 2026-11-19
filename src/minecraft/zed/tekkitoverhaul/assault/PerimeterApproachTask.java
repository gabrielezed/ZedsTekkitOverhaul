package zed.tekkitoverhaul.assault;

import java.lang.reflect.Field;
import net.minecraft.src.EntityAIBase;
import net.minecraft.src.EntityAITasks;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.PathPoint;

/** A transient task on a stock skeleton. Once released, only native tasks execute. */
final class PerimeterApproachTask extends EntityAIBase {
    private static final Field ACTIONS = actionField();
    private final EntitySkeleton mob;
    private SiteApproach approach;
    private final int length;
    private int next = 1;

    private PerimeterApproachTask(EntitySkeleton mob, SiteApproach approach) {
        this.mob = mob;
        this.approach = approach;
        length = approach.route.length;
        setMutexBits(7);
    }

    static boolean available() { return ACTIONS != null; }

    static PerimeterApproachTask attach(EntitySkeleton mob, SiteApproach approach) {
        if (!available()) return null;
        try {
            EntityAITasks tasks = (EntityAITasks) ACTIONS.get(mob);
            if (tasks == null) return null;
            PerimeterApproachTask task = new PerimeterApproachTask(mob, approach);
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

    @Override public boolean shouldExecute() { return approach != null; }

    @Override public void updateTask() {
        // The native scheduler selects tasks before running its update pass.
        if (approach == null) return;
        mob.getNavigator().clearPathEntity();
        PathPoint point = approach.route[next];
        double dx = mob.posX - point.xCoord - 0.5, dz = mob.posZ - point.zCoord - 0.5;
        if (dx * dx + dz * dz < 0.16 && Math.abs(mob.boundingBox.minY - point.yCoord) < 0.75) {
            next++;
            if (arrived()) {
                release();
                return;
            }
            point = approach.route[next];
        }
        mob.getMoveHelper().setMoveTo(point.xCoord + 0.5, point.yCoord, point.zCoord + 0.5, 0.25F);
    }

    int steps() { return next - 1; }
    boolean arrived() { return next == length; }
    void release() { approach = null; }
}
