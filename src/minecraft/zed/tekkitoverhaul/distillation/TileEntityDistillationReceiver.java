package zed.tekkitoverhaul.distillation;

import buildcraft.api.ILiquidContainer;
import buildcraft.api.Orientations;
import net.minecraft.src.TileEntity;
import zed.tekkitoverhaul.fluids.PetroleumFluids;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.integration.LoadedTile;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.SabotageTarget;

/** Extraction-only BuildCraft endpoint for the boiler's petroleum output. */
public final class TileEntityDistillationReceiver extends TileEntityDistillationPart
        implements ILiquidContainer, SabotageTarget {
    private EntitySaboteur disruptionOwner;
    private TileEntity leasedPipe;
    private int disruptionTicks;
    private TileEntity lastExtractionPipe;
    private long lastExtraction;
    private int blockedOffers;

    private TileEntityDistillationBoiler boiler() {
        return DistillationStructure.boilerFor(worldObj, xCoord, yCoord, zCoord, DistillationPartType.RECEIVER);
    }

    public int fill(Orientations from, int quantity, int liquidId, boolean commit) { return 0; }

    public int empty(int quantity, boolean commit) {
        if (quantity <= 0 || worldObj == null) return 0;
        TileEntityDistillationBoiler boiler = boiler();
        if (boiler == null) return 0;
        int accepted = Math.min(quantity, boiler.getProcess().getDistillateMB());
        if (accepted == 0) return 0;
        if (isDisrupted()) {
            if (!commit && blockedOffers < Integer.MAX_VALUE) {
                blockedOffers++;
                if (blockedOffers == 1 || disruptionTicks % 8 == 0)
                    worldObj.spawnParticle("portal", xCoord + 0.5D, yCoord + 1.05D, zCoord + 0.5D, 0, -0.15D, 0);
            }
            return 0;
        }
        if (!commit) return accepted;
        if (worldObj.isRemote) return 0;
        int moved = boiler.takeDistillate(accepted);
        if (moved > 0) {
            lastExtractionPipe = woodenExtractionPipe();
            lastExtraction = worldObj.getWorldTime();
        }
        return moved;
    }

    /** Exact 2.2.14 wood-liquid source connection. Multiple extractors leave this endpoint ineligible. */
    private TileEntity woodenExtractionPipe() {
        TileEntity result = null;
        for (int side = 0; side < 6; side++) {
            int x = xCoord + TileHopper.DX[side], y = yCoord + TileHopper.DY[side], z = zCoord + TileHopper.DZ[side];
            TileEntity neighboring = LoadedTile.at(worldObj, x, y, z);
            if (neighboring == null || !neighboring.getClass().getName().equals("buildcraft.transport.TileGenericPipe")
                    || worldObj.getBlockMetadata(x, y, z) != (side ^ 1)) continue;
            try {
                Object pipe = neighboring.getClass().getField("pipe").get(neighboring);
                if (pipe == null || !pipe.getClass().getName().equals("buildcraft.transport.pipes.PipeLiquidsWood"))
                    continue;
            } catch (Exception unsupported) {
                return null;
            }
            if (result != null) return null;
            result = neighboring;
        }
        return result;
    }

    public TileEntity activeExtractionPipe() {
        if (lastExtractionPipe == null) return null;
        long age = worldObj.getWorldTime() - lastExtraction;
        return age >= 0 && age <= 64 && woodenExtractionPipe() == lastExtractionPipe
                && getLiquidQuantity() > 0 ? lastExtractionPipe : null;
    }

    public String activeIndustrialOperation() {
        return activeExtractionPipe() == null ? null : "BUILDCRAFT_RECEIVER";
    }

    public boolean beginDisruption(EntitySaboteur owner) {
        if (!owner.canStartDisruption(this) || !owner.canMaintainDisruption(worldObj)
                || isDisrupted() || LoadedTile.at(worldObj, xCoord, yCoord, zCoord) != this) return false;
        TileEntity pipe = activeExtractionPipe();
        if (pipe == null || pipe != owner.disruptionConnection()) return false;
        disruptionOwner = owner;
        leasedPipe = pipe;
        disruptionTicks = 80;
        blockedOffers = 0;
        return true;
    }

    public boolean isDisrupted() {
        return disruptionTicks > 0 && !isInvalid() && disruptionOwner.canMaintainDisruption(worldObj)
                && LoadedTile.at(worldObj, xCoord, yCoord, zCoord) == this && woodenExtractionPipe() == leasedPipe;
    }

    public int disruptionTicksRemaining() { return isDisrupted() ? disruptionTicks : 0; }
    public int blockedOffers() { return blockedOffers; }

    @Override
    public void updateEntity() {
        if (disruptionTicks > 0 && (!isDisrupted() || --disruptionTicks == 0)) clearDisruption();
    }

    private void clearDisruption() {
        disruptionTicks = 0;
        disruptionOwner = null;
        leasedPipe = null;
    }

    @Override
    public void onChunkUnload() {
        clearDisruption();
        lastExtractionPipe = null;
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        clearDisruption();
        lastExtractionPipe = null;
        super.invalidate();
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
