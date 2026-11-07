package zed.tekkitoverhaul.machine;

/**
 * Minimal persistent rotation state for a visual test machine.
 * Independent from Minecraft and suitable for unit testing.
 */
public final class FoundryRotorState {
    public static final float DEGREES_PER_TICK = 8.0F;

    private boolean running = true;
    private float stoppedAngle;
    private long referenceTick;

    public boolean isRunning() {
        return running;
    }

    public float getStoppedAngle() {
        return stoppedAngle;
    }

    public long getReferenceTick() {
        return referenceTick;
    }

    public float angleAt(long tick, float partialTick) {
        double degrees = stoppedAngle;
        if (running) {
            degrees += ((double)tick - referenceTick + partialTick) * DEGREES_PER_TICK;
        }
        return normalize(degrees);
    }

    public boolean setRunning(boolean next, long tick) {
        if (running == next) return false;
        stoppedAngle = angleAt(tick, 0.0F);
        referenceTick = tick;
        running = next;
        return true;
    }

    public void load(boolean enabled, float angle, long reference) {
        running = enabled;
        stoppedAngle = normalize(angle);
        referenceTick = reference;
    }

    private static float normalize(double angle) {
        if (Double.isNaN(angle) || Double.isInfinite(angle)) return 0.0F;
        double result = angle % 360.0D;
        if (result < 0) result += 360.0D;
        return (float)result;
    }
}
