package zed.tekkitoverhaul.exploration.underwater;

import ic2.api.Direction;
import ic2.api.EnergyNet;
import ic2.api.IEnergySink;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.TileEntity;
import net.minecraft.src.forge.ISidedInventory;

public final class TileAirCompressor extends TileEntity implements IEnergySink, ISidedInventory {
    private static final String ENERGY = "ZTO_CompressorEU";
    private static final String TANK = "ZTO_CompressorTank";
    private int storedEU;
    private boolean connected;
    private ItemStack tank;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || isInvalid()) return;
        if (!connected) {
            EnergyNet.getForWorld(worldObj).addTileEntity(this);
            connected = true;
        }
        if (canCharge()) {
            OxygenCharge.set(tank, OxygenCharge.get(tank) + UnderwaterBalance.OXYGEN_PER_CYCLE);
            storedEU -= UnderwaterBalance.EU_PER_CYCLE;
            onInventoryChanged();
        }
    }

    boolean canCharge() {
        return OxygenCharge.isTank(tank) && OxygenCharge.get(tank) < UnderwaterBalance.OXYGEN_CAPACITY
                && storedEU >= UnderwaterBalance.EU_PER_CYCLE;
    }

    int storedEU() { return storedEU; }

    @Override
    public boolean isAddedToEnergyNet() { return connected; }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction side) { return true; }

    @Override
    public boolean demandsEnergy() {
        return connected && !isInvalid() && storedEU < UnderwaterBalance.EU_CAPACITY;
    }

    @Override
    public int injectEnergy(Direction side, int packet) {
        if (!demandsEnergy() || packet <= 0 || packet > UnderwaterBalance.MAX_PACKET_EU) return packet;
        int accepted = Math.min(packet, UnderwaterBalance.EU_CAPACITY - storedEU);
        storedEU += accepted;
        onInventoryChanged();
        return packet - accepted;
    }

    @Override
    public void invalidate() {
        disconnect();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        disconnect();
        super.onChunkUnload();
    }

    private void disconnect() {
        if (connected) {
            EnergyNet.getForWorld(worldObj).removeTileEntity(this);
            connected = false;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        storedEU = tag.getTag(ENERGY) instanceof NBTTagInt
                ? Math.max(0, Math.min(tag.getInteger(ENERGY), UnderwaterBalance.EU_CAPACITY)) : 0;
        ItemStack loaded = tag.getTag(TANK) instanceof NBTTagCompound
                ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag(TANK)) : null;
        tank = OxygenCharge.isTank(loaded) ? loaded : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger(ENERGY, storedEU);
        if (tank != null) tag.setCompoundTag(TANK, tank.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public int getSizeInventory() { return 1; }

    @Override
    public ItemStack getStackInSlot(int slot) { return slot == 0 ? tank : null; }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        return count > 0 ? getStackInSlotOnClosing(slot) : null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack removed = getStackInSlot(slot);
        if (removed != null) setInventorySlotContents(slot, null);
        return removed;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0 && (stack == null || OxygenCharge.isTank(stack))) {
            tank = stack;
            onInventoryChanged();
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && !isInvalid() && worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
                && player.getDistanceSq(xCoord + .5D, yCoord + .5D, zCoord + .5D) <= 64D;
    }

    @Override
    public int getInventoryStackLimit() { return 1; }

    @Override
    public String getInvName() { return "Air Compressor"; }

    @Override
    public void openChest() {}

    @Override
    public void closeChest() {}

    // Legacy item pipes cannot ask IInventory whether a stack is valid.
    @Override
    public int getStartInventorySide(int side) { return 0; }

    @Override
    public int getSizeInventorySide(int side) { return 0; }
}
