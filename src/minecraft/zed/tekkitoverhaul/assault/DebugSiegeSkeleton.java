package zed.tekkitoverhaul.assault;

import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

/** An encounter-owned skeleton. Its visible brightness and native combat remain intact. */
final class DebugSiegeSkeleton extends EntitySkeleton {
    private boolean checkingSun;

    DebugSiegeSkeleton(World world) { super(world); }

    @Override public void onLivingUpdate() {
        checkingSun = true;
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
