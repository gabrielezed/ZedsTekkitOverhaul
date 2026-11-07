package zed.tekkitoverhaul.distillation;

import buildcraft.api.ILiquidContainer;
import buildcraft.api.Orientations;
import zed.tekkitoverhaul.fluids.GasolineFluid;

/** Extraction-only BuildCraft endpoint for the boiler's gasoline output. */
public final class TileEntityDistillationReceiver extends TileEntityDistillationPart implements ILiquidContainer {
    private TileEntityDistillationBoiler boiler() {
        return DistillationStructure.boilerFor(worldObj, xCoord, yCoord, zCoord, DistillationPartType.RECEIVER);
    }

    private int gasolineQuantity(TileEntityDistillationBoiler boiler) {
        if (boiler == null) return 0;
        FractionationProcess process = boiler.getProcess();
        return process.getOutputTypeId() == DistillateType.GASOLINE.id ? process.getDistillateMB() : 0;
    }

    public int fill(Orientations from, int quantity, int liquidId, boolean commit) { return 0; }

    public int empty(int quantity, boolean commit) {
        if (quantity <= 0 || worldObj == null) return 0;
        TileEntityDistillationBoiler boiler = boiler();
        int accepted = Math.min(quantity, gasolineQuantity(boiler));
        if (accepted == 0 || !commit) return accepted;
        return worldObj.isRemote ? 0 : boiler.takeDistillate(accepted);
    }

    public int getLiquidQuantity() { return gasolineQuantity(boiler()); }

    public int getCapacity() { return FractionationProcess.TANK_CAPACITY_MB; }

    public int getLiquidId() { return getLiquidQuantity() > 0 ? GasolineFluid.stillId() : 0; }
}
