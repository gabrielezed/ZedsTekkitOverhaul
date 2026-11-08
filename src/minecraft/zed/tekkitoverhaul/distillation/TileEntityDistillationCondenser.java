package zed.tekkitoverhaul.distillation;

import buildcraft.api.ILiquidContainer;
import buildcraft.api.Orientations;
import net.minecraft.src.Block;

/** Water-only BuildCraft inlet backed by the boiler's coolant tank. */
public final class TileEntityDistillationCondenser extends TileEntityDistillationPart implements ILiquidContainer {
    private TileEntityDistillationBoiler boiler() {
        return DistillationStructure.boilerFor(worldObj, xCoord, yCoord, zCoord, DistillationPartType.CONDENSER);
    }

    public int fill(Orientations from, int quantity, int liquidId, boolean commit) {
        if (liquidId != Block.waterStill.blockID || worldObj == null || commit && worldObj.isRemote) return 0;
        TileEntityDistillationBoiler boiler = boiler();
        return boiler == null ? 0 : boiler.acceptCoolingWater(quantity, commit);
    }

    public int empty(int quantity, boolean commit) { return 0; }

    public int getLiquidQuantity() {
        TileEntityDistillationBoiler boiler = boiler();
        return boiler == null ? 0 : boiler.getProcess().getCoolantMB();
    }

    public int getCapacity() { return FractionationProcess.TANK_CAPACITY_MB; }

    public int getLiquidId() { return getLiquidQuantity() > 0 ? Block.waterStill.blockID : 0; }
}
