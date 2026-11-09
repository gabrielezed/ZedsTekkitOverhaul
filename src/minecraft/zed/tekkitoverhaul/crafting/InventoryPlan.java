package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Container;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

/** A single-use, local inventory transaction over explicitly selected slots. */
public final class InventoryPlan {
    private final Slot[] slots;
    private final ItemStack[] before;
    private final ItemStack[] after;
    private boolean used;

    public InventoryPlan(Slot[] slots) {
        this.slots = slots.clone();
        before = new ItemStack[slots.length];
        after = new ItemStack[slots.length];
        for (int index = 0; index < slots.length; index++) {
            before[index] = copy(slots[index].getStack());
            after[index] = copy(before[index]);
        }
    }

    public ItemStack get(int index) {
        return copy(after[index]);
    }

    public void set(int index, ItemStack stack) {
        after[index] = copy(stack);
    }

    /** Inserts into existing stacks before using empty slots. Returns the quantity left over. */
    public int insert(ItemStack stack, int start, int end) {
        int left = stack.stackSize;
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int index = start; index < end && left > 0; index++) {
                ItemStack present = after[index];
                if (pass == 0 ? !sameType(stack, present) : present != null) continue;
                if (!slots[index].isItemValid(stack)) continue;
                int count = present == null ? 0 : present.stackSize;
                int moved = Math.min(left, limit(index, stack) - count);
                if (moved <= 0) continue;
                ItemStack updated = stack.copy();
                updated.stackSize = count + moved;
                after[index] = updated;
                left -= moved;
            }
        }
        return left;
    }

    public int limit(int index, ItemStack stack) {
        return Math.min(slots[index].getSlotStackLimit(), stack.getMaxStackSize());
    }

    public int size() {
        return slots.length;
    }

    /** Call on the game thread after checking the current screen, player and container. */
    public boolean commit(Container container) {
        if (used) return false;
        used = true;
        for (int index = 0; index < slots.length; index++) {
            Slot slot = slots[index];
            if (slot.slotNumber < 0 || slot.slotNumber >= container.inventorySlots.size()
                    || container.getSlot(slot.slotNumber) != slot
                    || !ItemStack.areItemStacksEqual(before[index], slot.getStack())) return false;
            ItemStack next = after[index];
            if (!ItemStack.areItemStacksEqual(before[index], next) && next != null
                    && (next.stackSize <= 0 || next.stackSize > limit(index, next) || !slot.isItemValid(next))) {
                return false;
            }
        }
        for (int index = 0; index < slots.length; index++) {
            if (!ItemStack.areItemStacksEqual(before[index], after[index])) {
                slots[index].putStack(copy(after[index]));
            }
        }
        container.updateCraftingResults();
        return true;
    }

    public static boolean sameType(ItemStack first, ItemStack second) {
        if (first == null || second == null || !first.isItemEqual(second)) return false;
        return first.stackTagCompound == null ? second.stackTagCompound == null
                : first.stackTagCompound.equals(second.stackTagCompound);
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }
}
