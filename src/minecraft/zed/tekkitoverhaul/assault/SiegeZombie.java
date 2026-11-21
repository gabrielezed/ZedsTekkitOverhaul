package zed.tekkitoverhaul.assault;

import net.minecraft.src.EntityZombie;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

/** Keeps native infantry combat and leaves entrance changes to the encounter objective. */
final class SiegeZombie extends EntityZombie {
    private final boolean daylightTest;
    private boolean checkingSun;
    PerimeterApproachTask approach;

    SiegeZombie(World world, boolean daylightTest) {
        super(world);
        this.daylightTest = daylightTest;
        getNavigator().setBreakDoors(false);
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
