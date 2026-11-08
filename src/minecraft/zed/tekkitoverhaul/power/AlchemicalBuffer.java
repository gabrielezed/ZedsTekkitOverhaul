package zed.tekkitoverhaul.power;

import java.util.Arrays;

/** Integer accounting in EE2 packet units until EU is accepted by EnergyNet. */
final class AlchemicalBuffer {
    static final int UNITS_PER_EU = 20;
    static final int UNITS_PER_EMC = 80;
    static final int CAPACITY = 32 * UNITS_PER_EU;
    static final int MAX_PACKET_EU = 8;
    private int units;
    private long tick;
    private boolean seenTick;
    private final int[] received = new int[CollectorFace.PACKET_SIDES.length];
    private boolean offered;

    boolean receive(long worldTick, CollectorFace face, int amount, int limit, boolean commit) {
        beginTick(worldTick);
        int side = face.ordinal();
        if (amount <= 0 || amount > limit - received[side] || amount > CAPACITY - units) return false;
        if (commit) {
            units += amount;
            received[side] += amount;
        }
        return true;
    }

    int offer(long worldTick, int limit) {
        beginTick(worldTick);
        if (offered) return 0;
        offered = true;
        return Math.min(limit, units / UNITS_PER_EU);
    }

    void finishOffer(int sent, int remainder) {
        units -= (sent - Math.max(0, Math.min(sent, remainder))) * UNITS_PER_EU;
    }

    int storedUnits() { return units; }

    void restore(int stored) {
        units = Math.max(0, Math.min(CAPACITY, stored));
        seenTick = false;
    }

    private void beginTick(long worldTick) {
        if (!seenTick || tick != worldTick) {
            seenTick = true;
            tick = worldTick;
            Arrays.fill(received, 0);
            offered = false;
        }
    }
}
