package zed.tekkitoverhaul.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.ItemStack;

/** Resolves vanilla metadata wildcards against owned stacks with bounded backtracking. */
final class VariantFill {
    private final CraftingFill fill;
    private final LegacyNei.Selection recipe;
    private final List<ItemStack> stock = new ArrayList<ItemStack>();
    private final List<List<ItemStack>> choices = new ArrayList<List<ItemStack>>();
    private final List<Integer> positions = new ArrayList<Integer>();
    private int nodes = 4096;

    VariantFill(CraftingFill fill, LegacyNei.Selection recipe) {
        this.fill = fill;
        this.recipe = recipe;
        for (int index = 0; index < fill.slots.length; index++) {
            ItemStack stack = fill.slots[index].getStack();
            if (stack == null || stack.hasTagCompound()) continue;
            ItemStack total = find(stock, stack);
            if (total == null) stock.add(stack.copy());
            else total.stackSize += stack.stackSize;
        }
        for (int cell = 0; cell < recipe.pattern.length; cell++) {
            ItemStack shown = recipe.pattern[cell];
            if (shown == null) continue;
            List<ItemStack> variants = new ArrayList<ItemStack>();
            variants.add(shown);
            if (recipe.wildcards[cell] && !shown.hasTagCompound()) {
                for (ItemStack stack : stock) {
                    if (stack.itemID != shown.itemID || InventoryPlan.sameType(stack, shown)) continue;
                    ItemStack variant = stack.copy();
                    variant.stackSize = shown.stackSize;
                    variants.add(variant);
                }
            }
            choices.add(variants);
            positions.add(cell);
        }
    }

    CraftingFill.Result prepare(CraftingFill.Result exact, boolean batch) {
        if (!batch && exact.plan != null) return exact;
        boolean alternatives = false;
        int upper = 64;
        for (List<ItemStack> variants : choices) {
            alternatives |= variants.size() > 1;
            int largest = 0;
            for (ItemStack variant : variants) {
                ItemStack owned = find(stock, variant);
                if (owned != null) largest = Math.max(largest, owned.stackSize / variant.stackSize);
            }
            upper = Math.min(upper, largest);
        }
        if (!alternatives) return exact;
        int minimum = exact.plan == null ? 1 : exact.sets + 1;
        for (int sets = batch ? upper : 1; sets >= minimum && nodes > 0; sets--) {
            CraftingFill.Result candidate = search(recipe.pattern.clone(), 0, sets);
            if (candidate != null) {
                candidate.sets = sets;
                candidate.alternatives = true;
                return candidate;
            }
        }
        return exact;
    }

    private CraftingFill.Result search(ItemStack[] assigned, int position, int sets) {
        if (nodes-- <= 0) return null;
        if (position == positions.size()) {
            CraftingFill.Result candidate = fill.prepare(assigned, recipe.output, sets, false);
            return candidate.plan == null ? null : candidate;
        }
        int cell = positions.get(position);
        for (ItemStack variant : choices.get(position)) {
            ItemStack owned = find(stock, variant);
            int needed = sets * variant.stackSize;
            if (owned == null || owned.stackSize < needed) continue;
            assigned[cell] = variant;
            owned.stackSize -= needed;
            CraftingFill.Result candidate = search(assigned, position + 1, sets);
            owned.stackSize += needed;
            if (candidate != null) return candidate;
        }
        return null;
    }

    private static ItemStack find(List<ItemStack> stacks, ItemStack query) {
        for (ItemStack stack : stacks) {
            if (InventoryPlan.sameType(stack, query)) return stack;
        }
        return null;
    }
}
