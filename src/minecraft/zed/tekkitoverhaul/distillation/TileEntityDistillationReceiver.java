package zed.tekkitoverhaul.distillation;

import buildcraft.api.ILiquidContainer;
import buildcraft.api.Orientations;
import zed.tekkitoverhaul.fluids.PetroleumFluids;

/** Extraction-only BuildCraft endpoint for the boiler's petroleum output. */
public final class TileEntityDistillationReceiver extends TileEntityDistillationPart implements ILiquidContainer {
    private TileEntityDistillationBoiler boiler() {
        return DistillationStructure.boilerFor(worldObj, xCoord, yCoord, zCoord, DistillationPartType.RECEIVER);
    }

    public int fill(Orientations from, int quantity, int liquidId, boolean commit) { return 0; }

    public int empty(int quantity, boolean commit) {
        if (quantity <= 0 || worldObj == null) return 0;
        TileEntityDistillationBoiler boiler = boiler();
        if (boiler == null) return 0;
        int accepted = Math.min(quantity, boiler.getProcess().getDistillateMB());
        if (accepted == 0 || !commit) return accepted;
        return worldObj.isRemote ? 0 : boiler.takeDistillate(accepted);
    }

    public int getLiquidQuantity() {
        TileEntityDistillationBoiler boiler = boiler();
        return boiler == null ? 0 : boiler.getProcess().getDistillateMB();
    }

    public int getCapacity() { return FractionationProcess.TANK_CAPACITY_MB; }

    public int getLiquidId() {
        TileEntityDistillationBoiler boiler = boiler();
        if (boiler == null) return 0;
        DistillateType type = DistillateType.fromId(boiler.getProcess().getOutputTypeId());
        return type == null ? 0 : PetroleumFluids.stillId(type);
    }
}
