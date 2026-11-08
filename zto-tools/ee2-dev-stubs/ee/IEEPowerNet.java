package ee;

/** Compile-only boundary for EE2 1.4.6.5 adjacent power packets. */
public interface IEEPowerNet {
    boolean receiveEnergy(int amount, byte side, boolean commit);
    boolean sendEnergy(int amount, byte side, boolean commit);
    boolean passEnergy(int amount, byte side, boolean commit);
    void sendAllPackets(int amount);
    int relayBonus();
}
