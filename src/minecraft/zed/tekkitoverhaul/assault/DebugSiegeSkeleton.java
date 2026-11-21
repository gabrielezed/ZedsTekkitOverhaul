package zed.tekkitoverhaul.assault;

import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

/** An encounter-owned skeleton with an optional daytime test exemption. Its visible brightness and native combat remain intact. */
final class DebugSiegeSkeleton extends EntitySkeleton {
    private final boolean daylightTest;
    private boolean checkingSun;
    PerimeterApproachTask approach;

    DebugSiegeSkeleton(World world, boolean daylightTest) {
        super(world);
        this.daylightTest = daylightTest;
    }

    @Override protected void despawnEntity() {
        boolean owned = approach != null && !isDead;
        super.despawnEntity();
        if (owned && isDead) approach.nativeDespawn = true;
    }

    @Override protected boolean canDespawn() { return approach == null; }

    @Override protected void updateAITasks() {
        if (approach == null || !approach.updateWithoutCombat()) super.updateAITasks();
    }

    @Override public void onLivingUpdate() {
        checkingSun = daylightTest;
        try {
            super.onLivingUpdate();
        } finally {
            checkingSun = false;
        }
    }

    @Override public float getBrightness(float partialTick) {
        return checkingSun ? 0.0F : super.getBrightness(partialTick);
    }

    @Override public boolean addEntityID(NBTTagCompound tag) { return false; }
}
