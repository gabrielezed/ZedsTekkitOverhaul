package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Container;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryCrafting;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

/** Builds copied crafting grids. Output is always collected through SlotCrafting. */
final class CraftingFill {
    final Slot[] slots;
    final int cells;
    final int width;
    private final int returnEnd;

    CraftingFill(Slot[] slots, int width) {
        this(slots, width, slots.length);
    }

    CraftingFill(Slot[] slots, int width, int returnEnd) {
        this.slots = slots;
        this.width = width;
        this.returnEnd = returnEnd;
        cells = width * width;
    }

    Result prepare(ItemStack[] pattern, ItemStack output, int sets, boolean partial) {
        InventoryPlan plan = new InventoryPlan(slots);
        ItemStack[] displaced = new ItemStack[cells];
        int[] available = new int[cells];
        boolean changed = false;
        boolean complete = true;
        for (int cell = 0; cell < cells; cell++) {
            ItemStack existing = plan.get(cell);
            if (existing != null && !InventoryPlan.sameType(existing, pattern[cell])) {
                if (partial) {
                    complete = false;
                    continue;
                }
                displaced[cell] = existing;
                plan.set(cell, null);
            }
        }
        for (int cell = 0; cell < cells; cell++) {
            ItemStack ingredient = pattern[cell];
            if (ingredient == null) continue;
            ItemStack existing = plan.get(cell);
            if (existing != null && !InventoryPlan.sameType(existing, ingredient)) {
                complete = false;
                continue;
            }
            int count = existing == null ? 0 : existing.stackSize;
            int target = sets * ingredient.stackSize;
            int limit = plan.limit(cell, ingredient);
            if (target > limit || !slots[cell].isItemValid(ingredient)) {
                return new Result(null, available, "Ingredient exceeds the grid slot limit");
            }
            int supplied = takeDisplaced(displaced, ingredient, Math.max(0, target - count));
            for (int index = cells; index < plan.size() && count + supplied < target; index++) {
                ItemStack source = plan.get(index);
                if (!InventoryPlan.sameType(ingredient, source)) continue;
                int taken = Math.min(source.stackSize, target - count - supplied);
                source.stackSize -= taken;
                plan.set(index, source.stackSize == 0 ? null : source);
                supplied += taken;
            }
            available[cell] = Math.min(target, count + supplied);
            complete &= available[cell] == target;
            if (supplied > 0) {
                ItemStack updated = ingredient.copy();
                updated.stackSize = count + supplied;
                plan.set(cell, updated);
                changed = true;
            }
        }
        if (!complete && (!partial || !changed)) return new Result(null, available, "Missing ingredients");
        for (ItemStack stack : displaced) {
            if (stack != null && stack.stackSize > 0 && plan.insert(stack, cells, returnEnd) != 0) {
                return new Result(null, available, "Make room for displaced ingredients");
            }
        }
        if (complete && !matches(plan, output)) return new Result(null, available, "Grid does not match this recipe");
        return new Result(plan, available, complete ? "Ready" : "Partial fill");
    }

    Result maximum(ItemStack[] pattern, ItemStack output) {
        Result best = prepare(pattern, output, 1, false);
        int upper = Math.min(64, stockSets(pattern));
        InventoryPlan inventory = new InventoryPlan(slots);
        for (int index = 0; index < cells; index++) {
            ItemStack ingredient = pattern[index];
            if (ingredient != null) upper = Math.min(upper, inventory.limit(index, ingredient) / ingredient.stackSize);
        }
        // Returning displaced stacks can become possible only after a larger fill frees a slot.
        for (int sets = upper; sets > 1; sets--) {
            Result candidate = prepare(pattern, output, sets, false);
            if (candidate.plan != null) {
                candidate.sets = sets;
                return candidate;
            }
        }
        return best;
    }

    int stockSets(ItemStack[] pattern) {
        InventoryPlan inventory = new InventoryPlan(slots);
        int upper = Integer.MAX_VALUE;
        for (int index = 0; index < cells; index++) {
            ItemStack ingredient = pattern[index];
            if (ingredient == null) continue;
            int needed = 0;
            int owned = 0;
            for (ItemStack input : pattern) {
                if (InventoryPlan.sameType(ingredient, input)) needed += input.stackSize;
            }
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                if (InventoryPlan.sameType(ingredient, stack)) owned += stack.stackSize;
            }
            upper = Math.min(upper, owned / needed);
        }
        return upper == Integer.MAX_VALUE ? 0 : upper;
    }

    Result returnInputs() {
        InventoryPlan plan = new InventoryPlan(slots);
        int left = 0;
        for (int cell = 0; cell < cells; cell++) {
            ItemStack stack = plan.get(cell);
            if (stack == null) continue;
            stack.stackSize = plan.insert(stack, cells, returnEnd);
            left += stack.stackSize;
            plan.set(cell, stack.stackSize == 0 ? null : stack);
        }
        return new Result(plan, new int[cells], left == 0 ? "Grid cleared" : left + " items still in grid");
    }

    private boolean matches(InventoryPlan plan, ItemStack output) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) { return false; }
        }, width, width);
        for (int cell = 0; cell < cells; cell++) grid.setInventorySlotContents(cell, plan.get(cell));
        ItemStack result = width == 5 ? AdvancedCraftingManager.findMatchingRecipe(grid)
                : CraftingManager.getInstance().findMatchingRecipe(grid);
        return ItemStack.areItemStacksEqual(output, result);
    }

    private static int takeDisplaced(ItemStack[] displaced, ItemStack ingredient, int needed) {
        int supplied = 0;
        for (ItemStack stack : displaced) {
            if (needed == supplied) break;
            if (!InventoryPlan.sameType(ingredient, stack)) continue;
            int taken = Math.min(stack.stackSize, needed - supplied);
            stack.stackSize -= taken;
            supplied += taken;
        }
        return supplied;
    }

    static final class Result {
        final InventoryPlan plan;
        final int[] available;
        final String status;
        int sets = 1;
        boolean alternatives;

        Result(InventoryPlan plan, int[] available, String status) {
            this.plan = plan;
            this.available = available;
            this.status = status;
        }
    }
}
