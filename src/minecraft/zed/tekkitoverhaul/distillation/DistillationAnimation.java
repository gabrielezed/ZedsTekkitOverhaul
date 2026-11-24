package zed.tekkitoverhaul.distillation;

/** Frame indices for the 16-pixel cells of the refinery indicator atlas. */
final class DistillationAnimation {
    private DistillationAnimation() { }

    static int motionFrame(long worldTick) { return (int)(worldTick >> 2) & 3; }

    static int receiverFrame(int output, boolean blocked, int completionTicks) {
        if (completionTicks > 0) return 10;
        if (blocked) return 9;
        return Math.min(8, (output + 999) / 1000);
    }
}
