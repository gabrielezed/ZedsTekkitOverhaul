package zed.tekkitoverhaul.distillation;

/**
 * IC2 1.97 LV receiving rules, independently testable without IC2.
 * The inject method returns unaccepted EU, as required by IEnergySink.
 */
public final class LvHeaterBuffer {
    public static final int CAPACITY_EU = 4000;
    public static final int MAX_PACKET_EU = 32;
    private int storedEU;

    public int getStoredEU() { return storedEU; }

    public boolean needsEnergy() { return storedEU < CAPACITY_EU; }

    public int inject(int packetEU) {
        if (packetEU <= 0) return 0;
        if (packetEU > MAX_PACKET_EU) return packetEU; // Reject over-voltage; do not silently delete EU.
        int accepted = Math.min(packetEU, CAPACITY_EU - storedEU);
        storedEU += accepted;
        return packetEU - accepted;
    }

    public boolean consume(int eu) {
        if (eu <= 0 || eu > storedEU) return false;
        storedEU -= eu;
        return true;
    }

    public void restore(int eu) {
        storedEU = Math.max(0, Math.min(eu, CAPACITY_EU));
    }
}
