package zed.tekkitoverhaul.exploration.abyssal.guardian;

/** Shared server and model dimensions and timing, in blocks and ticks. */
public final class CustodianBalance {
    public static final int HEALTH = 80, SECOND_PHASE = 40;
    public static final float WIDTH = 1.55F, HEIGHT = 4.45F;
    public static final double SHOULDER = 0.6875, SHOULDER_Y = 3.375;
    public static final double DETECTION = 17, STRIKE_REACH = 4.85, STRIKE_THICKNESS = 0.17, PRESSURE_RADIUS = 6;
    public static final int STRIKE_DAMAGE = 5, PRESSURE_DAMAGE = 2;
    public static final double PATROL = 0.09, CRUISE = 0.13, REPOSITION = 0.17, PRESSURE_FORCE = 0.46;
    public static final int BLINK_MIN = 4, BLINK_MAX = 9, BLINK_CANDIDATES = 16;
    public static final int BLINK_COOLDOWN = 132, BLINK_COOLDOWN_TWO = 92, PRESSURE_COOLDOWN = 108;
    public static final int RESONANCE_COOLDOWN = 220, RESONANCE_LIMIT = 2, RESONANCE_SIGNAL = 48;
    public static final int AWAKEN = 24, TRANSITION = 28;
    public static final double EXTENDED_HEIGHT = 5.9;
    public static final float COMPRESSED_SCALE = 0.38F;
    public static final int COMPRESSION_EXPAND = 24, COMPRESSION_COOLDOWN = 280, COMPRESSION_COOLDOWN_TWO = 205;

    public enum Attack {
        STRIKE(22, 6, 34), PRESSURE(34, 1, 42), BLINK(18, 1, 22), RESONANCE(26, 1, 32), COMPRESSION(26, 48, 48);

        public final int windup, active, recover;

        Attack(int windup, int active, int recover) {
            this.windup = windup;
            this.active = active;
            this.recover = recover;
        }

        public int end() { return windup + active + recover; }
    }

    public static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private CustodianBalance() {}
}
