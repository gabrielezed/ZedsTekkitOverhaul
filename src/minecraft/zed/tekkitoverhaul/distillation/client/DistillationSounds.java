package zed.tekkitoverhaul.distillation.client;

import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.src.World;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;

/** Short positional cues in the local client's existing sound engine. */
public final class DistillationSounds implements TileEntityDistillationBoiler.StepListener {
    private final Minecraft minecraft;
    private WeakReference<World> soundWorld = new WeakReference<World>(null);
    private long lastPulse = Long.MIN_VALUE;

    private DistillationSounds(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public static void register(Minecraft minecraft) {
        TileEntityDistillationBoiler.setStepListener(new DistillationSounds(minecraft));
    }

    @Override
    public void onStep(TileEntityDistillationBoiler boiler, boolean started, boolean completed) {
        World world = boiler.worldObj;
        if (world != minecraft.theWorld || minecraft.thePlayer == null) return;
        long tick = world.getWorldTime();
        boolean pulse = (tick + boiler.xCoord * 11L + boiler.zCoord * 17L) % 79L == 0;
        if ((!started && !completed && !pulse) || !boiler.presentationAvailable()) return;

        double x = boiler.xCoord + 0.5D;
        double y = boiler.yCoord + 0.5D;
        double z = boiler.zCoord + 0.5D;
        if (minecraft.thePlayer.getDistanceSq(x, y, z) >= 16D * 16D) return;

        if (started) world.playSoundEffect(x, y, z, "zto.distillation.start", 0.27F, 1F);
        if (completed) world.playSoundEffect(x, y, z, "zto.distillation.complete", 0.32F, 1F);
        if (!started && !completed) {
            boolean worldChanged = world != soundWorld.get();
            if (worldChanged || tick < lastPulse || lastPulse == Long.MIN_VALUE || tick - lastPulse >= 10) {
                if (worldChanged) soundWorld = new WeakReference<World>(world);
                lastPulse = tick;
                world.playSoundEffect(x, y, z, "zto.distillation.running", 0.19F, 1F);
            }
        }
    }
}
