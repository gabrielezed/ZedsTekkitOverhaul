package buildcraft.api;

public interface ILiquidContainer {
    int fill(Orientations from, int quantity, int liquidId, boolean commit);
    int empty(int quantity, boolean commit);
    int getLiquidQuantity();
    int getCapacity();
    int getLiquidId();
}
