package zed.tekkitoverhaul.hopper;

import net.minecraft.src.*;

public final class ContainerHopper extends Container {
    private final TileHopper hopper;
    public ContainerHopper(InventoryPlayer player, TileHopper tile) {
        hopper = tile;
        for (int i=0;i<5;i++) addSlot(new Slot(tile,i,44+i*18,20));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++)
            addSlot(new Slot(player,col+row*9+9,8+col*18,51+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(player,col,8+col*18,109));
    }
    public boolean canInteractWith(EntityPlayer player) { return hopper.isUseableByPlayer(player); }
    public ItemStack transferStackInSlot(int index) {
        if (index < 0 || index >= inventorySlots.size()) return null;
        Slot source=(Slot)inventorySlots.get(index);
        ItemStack stack=source.getStack();
        if (stack == null) return null;
        ItemStack before=stack.copy();
        // Container.mergeItemStack in this release does not compare NBT.
        int start=index<5?5:0, end=index<5?inventorySlots.size():5;
        for (int i=start;i<end && stack.stackSize>0;i++) {
            Slot target=(Slot)inventorySlots.get(i);
            ItemStack existing=target.getStack();
            if (!target.isItemValid(stack) || (existing!=null && !InventoryAccess.same(existing,stack))) continue;
            int amount=Math.min(stack.stackSize,Math.min(target.getSlotStackLimit(),stack.getMaxStackSize())
                -(existing==null?0:existing.stackSize));
            if(amount<=0) continue;
            ItemStack updated=existing==null?stack.copy():existing.copy();
            updated.stackSize=(existing==null?0:existing.stackSize)+amount;
            target.putStack(updated); stack.stackSize-=amount;
        }
        if(stack.stackSize==before.stackSize) return null;
        if(stack.stackSize==0) source.putStack(null); else source.onSlotChanged();
        return before;
    }
}
