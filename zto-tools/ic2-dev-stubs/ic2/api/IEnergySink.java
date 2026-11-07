package ic2.api;

/** Local development ABI placeholder for the legacy energy sink contract. */
public interface IEnergySink extends IEnergyAcceptor {
    boolean demandsEnergy();
    int injectEnergy(Direction direction, int amount);
}
