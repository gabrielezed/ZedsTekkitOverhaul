package zed.tekkitoverhaul.power;

public final class AlchemicalBufferSelfTest {
    public static void main(String[] args) {
        simulate(16, 1, 80);
        simulate(48, 3, 240);
        simulate(160, 8, 800);

        AlchemicalBuffer buffer = new AlchemicalBuffer();
        check(buffer.receive(1, CollectorFace.ABOVE, 16, 16, false), "simulate incoming packet");
        check(buffer.storedUnits() == 0, "simulation does not credit the buffer");
        check(buffer.receive(1, CollectorFace.ABOVE, 16, 16, true), "accept normal packet");
        check(!buffer.receive(1, CollectorFace.ABOVE, 1, 16, true), "reject duplicate input in the same tick");
        check(!buffer.receive(2, CollectorFace.ABOVE, 17, 16, true), "reject boosted packet without partial acceptance");
        check(!buffer.receive(2, CollectorFace.ABOVE, -1, 16, true), "reject negative packet");
        check(buffer.receive(2, CollectorFace.ABOVE, 16, 16, true), "accept new tick");
        int offer = buffer.offer(2, 1);
        check(offer == 1, "integer conversion retains fractions");
        buffer.finishOffer(offer, offer);
        check(buffer.storedUnits() == 32, "disconnected network returns the whole packet");
        check(buffer.offer(2, 1) == 0, "same-tick repeat cannot emit");

        buffer.restore(Integer.MAX_VALUE);
        check(buffer.storedUnits() == 640, "NBT is clamped to capacity");
        check(!buffer.receive(3, CollectorFace.EAST, 1, 160, true), "full buffer refuses input");
        offer = buffer.offer(3, 8);
        buffer.finishOffer(offer, 3);
        check(buffer.storedUnits() == 540, "partial network delivery debits accepted EU only");
        buffer.restore(-5);
        check(buffer.storedUnits() == 0, "invalid negative NBT is discarded");

        int[][] expectedOffsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, 1},
                {0, 0, -1}, {-1, 0, 0}, {1, 0, 0}};
        for (int side = 0; side < expectedOffsets.length; side++) {
            CollectorFace face = CollectorFace.PACKET_SIDES[side];
            int[] offset = expectedOffsets[side];
            check(face.dx == offset[0] && face.dy == offset[1] && face.dz == offset[2], "EE2 source face " + side);
            check(buffer.receive(4, face, 48, 48, true), "accept independent face " + side);
            check(!buffer.receive(4, face, 1, 48, true), "enforce face limit " + side);
        }
        check(buffer.storedUnits() == 288, "all faces feed one buffer");
        check(buffer.offer(4, 3) == 3, "multiple collectors share one output limit");
        check(buffer.offer(4, 3) == 0, "second output attempt in the same tick is refused");
        System.out.println("Alchemical converter self-test passed");
    }

    private static void simulate(int input, int maxOffer, int expected) {
        AlchemicalBuffer buffer = new AlchemicalBuffer();
        int delivered = 0;
        for (int tick = 0; tick < 100; tick++) {
            check(buffer.receive(tick, CollectorFace.ABOVE, input, input, true), "full-light collector input");
            int sent = buffer.offer(tick, maxOffer);
            buffer.finishOffer(sent, 0);
            delivered += sent;
        }
        check(delivered == expected, "expected full-light throughput by collector mark");
        check(buffer.storedUnits() == 0, "no fractional remainder after 100 ticks");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
