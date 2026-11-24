package zed.tekkitoverhaul.exploration.motorboat;

import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

public final class ContainerMotorboat extends Container {
    private static final int PLAYER_START = 2;
    private final EntityPlayer player;
    private final EntityMotorboat boat;

    public ContainerMotorboat(InventoryPlayer inventory, EntityMotorboat boat) {
        this.player = inventory.player;
        this.boat = boat;
        addSlot(new Slot(boat.buckets, EntityMotorboat.INPUT, 19, 28) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return canInteractWith(player) && MotorboatRegistry.isGasoline(stack);
            }
        });
        addSlot(new Slot(boat.buckets, EntityMotorboat.RETURN, 57, 28) {
            @Override
            public boolean isItemValid(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 87 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 145));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player == this.player && !boat.worldObj.isRemote && boat.canUse(player);
    }

    @Override
    public ItemStack slotClick(int slot, int mouseButton, boolean shift, EntityPlayer player) {
        return canInteractWith(player) && slot < inventorySlots.size()
                ? super.slotClick(slot, mouseButton, shift, player) : null;
    }

    @Override
    public ItemStack transferStackInSlot(int index) {
        if (index < 0 || index >= inventorySlots.size() || !canInteractWith(player)) return null;
        Slot slot = getSlot(index);
        ItemStack stack = slot.getStack();
        if (stack == null) return null;
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!mergeItemStack(stack, PLAYER_START, inventorySlots.size(), true)) return null;
            if (stack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        } else {
            Slot input = getSlot(EntityMotorboat.INPUT);
            if (input.getHasStack() || !input.isItemValid(stack)) return null;
            input.putStack(slot.decrStackSize(1));
        }
        slot.onPickupFromSlot(original);
        return original;
    }

    public String engineStatus() {
        if (boat.isPropelling()) return "Engine running";
        if (getSlot(EntityMotorboat.INPUT).getHasStack()) {
            if (getSlot(EntityMotorboat.RETURN).getHasStack()) return "Take the empty bucket";
            if (!boat.hasRoomForBucket()) return "Needs 1000 mB of tank space";
        }
        if (boat.fuel() == 0) return "Insert a Gasoline Bucket";
        return boat.fuel() == EntityMotorboat.CAPACITY ? "Engine idle - tank full" : "Engine idle";
    }
}
