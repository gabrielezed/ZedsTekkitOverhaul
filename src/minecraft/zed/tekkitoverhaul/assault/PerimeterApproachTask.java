package zed.tekkitoverhaul.assault;

import java.lang.reflect.Field;
import net.minecraft.src.EntityAIBase;
import net.minecraft.src.EntityAITasks;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.PathPoint;

/** A transient task on a stock skeleton. Once released, only native tasks execute. */
final class PerimeterApproachTask extends EntityAIBase {
    private final EntitySkeleton mob;
    private SiteApproach approach;
    private final int length;
    private int next = 1;
    private boolean released;

    private PerimeterApproachTask(EntitySkeleton mob, SiteApproach approach) {
        this.mob = mob;
        this.approach = approach;
        length = approach.route.length;
        setMutexBits(7);
    }

    static PerimeterApproachTask attach(EntitySkeleton mob, SiteApproach approach) {
        try {
            // MCP 6.2 and the pinned client 1.2.5 action-task field. Target tasks remain native.
            String name = "acq".equals(EntityLiving.class.getName()) ? "bT" : "tasks";
            Field field = EntityLiving.class.getDeclaredField(name);
            if (field.getType() != EntityAITasks.class) return null;
            field.setAccessible(true);
            PerimeterApproachTask task = new PerimeterApproachTask(mob, approach);
            ((EntityAITasks) field.get(mob)).addTask(0, task);
            return task;
        } catch (ReflectiveOperationException failure) {
            return null;
        } catch (SecurityException failure) {
            return null;
        }
    }

    @Override public boolean shouldExecute() { return !released; }

    @Override public void updateTask() {
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
    void release() {
        released = true;
        approach = null;
    }
}
