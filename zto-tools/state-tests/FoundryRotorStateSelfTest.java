import zed.tekkitoverhaul.machine.FoundryRotorState;

/** Tests pause/resume, angle continuity and fields used in NBT persistence. */
public final class FoundryRotorStateSelfTest {
    private static int count;

    private static void eq(boolean actual, boolean expected) {
        count++;
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static void eq(float actual, float expected) {
        count++;
        if (Math.abs(actual - expected) > 0.01F) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void eq(long actual, long expected) {
        count++;
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        FoundryRotorState state = new FoundryRotorState();
        eq(state.isRunning(), true);
        eq(state.angleAt(0, 0), 0F);
        eq(state.angleAt(1, 0.5F), 12F);
        eq(state.angleAt(46, 0), 8F);
        eq(state.setRunning(false, 46), true);
        eq(state.isRunning(), false);
        eq(state.angleAt(1000, 0), 8F);
        eq(state.setRunning(false, 2000), false);
        eq(state.setRunning(true, 1000), true);
        eq(state.angleAt(1001, 0.5F), 20F);

        FoundryRotorState restored = new FoundryRotorState();
        restored.load(state.isRunning(), state.getStoppedAngle(), state.getReferenceTick());
        eq(restored.angleAt(1001, 0.5F), 20F);
        eq(restored.getReferenceTick(), 1000);
        restored.load(false, Float.NaN, 5);
        eq(restored.angleAt(500, 0), 0F);
        restored.load(true, 725F, 50);
        eq(restored.angleAt(50, 0), 5F);
        eq(restored.angleAt(48, 0), 349F);
        System.out.println("PASS: " + count + " Foundry rotor state checks");
    }
}
