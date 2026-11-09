package zed.tekkitoverhaul.hopper;

import java.util.List;
import net.minecraft.src.*;
import net.minecraft.src.forge.ISidedInventory;

/** Five persistent slots; only the authoritative world performs transfers. */
public final class TileHopper extends TileEntity implements ISidedInventory {
    private ItemStack[] items = new ItemStack[5];
    private int cooldown;
    private int outputMode;
    public String outputModeName() {
        return outputMode == 1 ? "Input" : outputMode == 2 ? "Fuel" : "Auto";
    }
    public void cycleOutputMode() {
        outputMode = (outputMode + 1) % 3;
        onInventoryChanged();
    }
    public static final int[] DX = {0,0,0,0,-1,1};
    public static final int[] DY = {-1,1,0,0,0,0};
    public static final int[] DZ = {0,0,-1,1,0,0};

    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (cooldown > 0) cooldown--;
        if (cooldown > 0 || isLocked()) return;
        int face = facing();
        boolean moved = InventoryAccess.moveOne(this, -1,
            InventoryAccess.at(worldObj, xCoord + DX[face], yCoord + DY[face], zCoord + DZ[face]), face ^ 1, outputMode);
        IInventory above = InventoryAccess.at(worldObj, xCoord, yCoord + 1, zCoord);
        if (above != null) moved = InventoryAccess.moveOne(above, 0, this, -1) | moved;
        else moved = collect() | moved;
        if (moved) { cooldown = 8; onInventoryChanged(); }
    }

    private boolean collect() {
        if (!worldObj.blockExists(xCoord, yCoord + 1, zCoord)) return false;
        List entities = worldObj.getEntitiesWithinAABB(EntityItem.class,
            AxisAlignedBB.getBoundingBoxFromPool(xCoord, yCoord + .625, zCoord, xCoord + 1, yCoord + 2, zCoord + 1));
        for (Object candidate : entities) {
            EntityItem entity = (EntityItem) candidate;
            if (entity.isDead || entity.item == null || entity.item.stackSize <= 0) continue;
            int accepted = InventoryAccess.insert(this, entity.item, -1, true);
            entity.item.stackSize -= accepted;
            if (entity.item.stackSize <= 0) entity.setDead();
            if (accepted > 0) return true;
        }
        return false;
    }

    public int facing() {
        int face = getBlockMetadata() & 7;
        return face == 1 || face > 5 ? 0 : face;
    }
    public boolean isLocked() { return worldObj != null && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord); }
    public void setTransferCooldown(int ticks) { cooldown = ticks; }
    public int getSizeInventory() { return 5; }
    public String getInvName() { return "Hopper"; }
    public int getInventoryStackLimit() { return 64; }
    public ItemStack getStackInSlot(int slot) { return items[slot]; }
    public void setInventorySlotContents(int slot, ItemStack stack) { items[slot] = stack; onInventoryChanged(); }
    public ItemStack decrStackSize(int slot, int count) {
        if (items[slot] == null || count <= 0) return null;
        ItemStack result = items[slot].splitStack(Math.min(count, items[slot].stackSize));
        if (items[slot].stackSize == 0) items[slot] = null;
        onInventoryChanged();
        return result;
    }
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = items[slot]; items[slot] = null; onInventoryChanged(); return stack;
    }
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + .5, yCoord + .5, zCoord + .5) <= 64;
    }
    public void openChest() { }
    public void closeChest() { }
    public int getStartInventorySide(int side) { return 0; }
    public int getSizeInventorySide(int side) { return 5; }

    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        items = new ItemStack[5];
        outputMode = Math.max(0, Math.min(2, tag.getInteger("OutputMode")));
        cooldown = Math.max(0, Math.min(8, tag.getInteger("TransferCooldown")));
        NBTTagList list = tag.getTagList("Items");
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = (NBTTagCompound) list.tagAt(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < 5) items[slot] = ItemStack.loadItemStackFromNBT(entry);
        }
    }
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("OutputMode", outputMode);
        tag.setInteger("TransferCooldown", cooldown);
        tag.setInteger("ZTOHopperVersion", 1);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 5; i++) if (items[i] != null) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte("Slot", (byte) i);
            items[i].writeToNBT(entry);
            list.appendTag(entry);
        }
        tag.setTag("Items", list);
    }
}
