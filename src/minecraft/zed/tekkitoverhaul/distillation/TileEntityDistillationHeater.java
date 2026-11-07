package zed.tekkitoverhaul.distillation;

import ic2.api.Direction;
import ic2.api.EnergyNet;
import ic2.api.IEnergySink;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;

/**
 * Standalone IC2 1.97 low-voltage EU sink mounted below the boiler.
 * Network-facing code is confined to this class.
 */
public final class TileEntityDistillationHeater extends TileEntityDistillationPart implements IEnergySink {
    private final LvHeaterBuffer energy = new LvHeaterBuffer();
    private boolean registeredToNetwork;

    public int getStoredEnergy() { return energy.getStoredEU(); }

    public boolean extractEU(int eu) {
        boolean consumed = energy.consume(eu);
        if (consumed) onInventoryChanged();
        return consumed;
    }

    @Override
    public boolean isAddedToEnergyNet() {
        return registeredToNetwork;
    }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction side) {
        return side != Direction.YP; // The boiler occupies the upper face.
    }

    @Override
    public boolean demandsEnergy() {
        return energy.needsEnergy();
    }

    @Override
    public int injectEnergy(Direction side, int eu) {
        int remainder = energy.inject(eu);
        if (remainder != eu) onInventoryChanged();
        return remainder; // IC2 1.97 expects EU not accepted, not EU consumed.
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || registeredToNetwork) return;
        try {
            // Register only after world assignment to avoid a double EnergyNet insertion.
            EnergyNet.getForWorld(worldObj).addTileEntity(this);
            registeredToNetwork = true;
        } catch (RuntimeException failure) {
            System.err.println("[ZTO] Distillation heater EnergyNet registration failed: " + failure);
        }
    }

    @Override
    public void invalidate() {
        if (registeredToNetwork && worldObj != null && !worldObj.isRemote) {
            try {
                EnergyNet.getForWorld(worldObj).removeTileEntity(this);
            } catch (RuntimeException failure) {
                System.err.println("[ZTO] Distillation heater EnergyNet removal failed: " + failure);
            } finally {
                registeredToNetwork = false;
            }
        }
        super.invalidate();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy.restore(tag.getInteger("ZTO_DistillHeaterEU"));
        registeredToNetwork = false; // Reconnect after the chunk actually loads.
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("ZTO_DistillHeaterEU", energy.getStoredEU());
    }
}
