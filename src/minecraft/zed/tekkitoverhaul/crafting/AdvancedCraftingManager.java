package zed.tekkitoverhaul.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.Block;
import net.minecraft.src.InventoryCrafting;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;

/** Dedicated 5x5 recipes; does not modify vanilla CraftingManager. */
public final class AdvancedCraftingManager {
    private static final List<Recipe> RECIPES = new ArrayList<Recipe>();

    private AdvancedCraftingManager() {}

    public static void register(ItemStack output, String[] rows, Object... mappings) {
        if (rows == null || rows.length != 5 || mappings.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid 5x5 pattern or ingredient mapping");
        }
        ItemStack[] ingredients = new ItemStack[25];
        for (int y = 0; y < 5; y++) {
            if (rows[y] == null || rows[y].length() != 5) {
                throw new IllegalArgumentException("Row " + y + " must contain exactly 5 characters");
            }
            for (int x = 0; x < 5; x++) {
                char symbol = rows[y].charAt(x);
                if (symbol == ' ') continue;
                Object obj = null;
                for (int k = 0; k < mappings.length; k += 2) {
                    if (((Character)mappings[k]).charValue() == symbol) {
                        obj = mappings[k + 1];
                        break;
                    }
                }
                if (obj == null) throw new IllegalArgumentException("No mapping for '" + symbol + "'");
                ingredients[x + y * 5] = toStack(obj);
            }
        }
        RECIPES.add(new Recipe(ingredients, output.copy()));
    }

    private static ItemStack toStack(Object obj) {
        if (obj instanceof ItemStack) return ((ItemStack)obj).copy();
        if (obj instanceof Block) return new ItemStack((Block)obj, 1);
        if (obj instanceof Item) return new ItemStack((Item)obj, 1);
        throw new IllegalArgumentException("Unsupported ingredient: " + obj);
    }

    public static ItemStack findMatchingRecipe(InventoryCrafting inv) {
        if (inv.getSizeInventory() != 25) return null;
        for (Recipe recipe : RECIPES) {
            boolean matches = true;
            for (int slot = 0; slot < 25; slot++) {
                ItemStack required = recipe.inputs[slot];
                ItemStack supplied = inv.getStackInSlot(slot);
                if (required == null) {
                    if (supplied != null) { matches = false; break; }
                } else if (supplied == null || supplied.itemID != required.itemID
                        || supplied.getItemDamage() != required.getItemDamage()) {
                    matches = false;
                    break;
                }
            }
            if (matches) return recipe.output.copy();
        }
        return null;
    }

    public static List<Recipe> getRecipes() {
        return new ArrayList<Recipe>(RECIPES);
    }

    public static final class Recipe {
        final ItemStack[] inputs;
        final ItemStack output;
        Recipe(ItemStack[] inputs, ItemStack output) {
            this.inputs = inputs;
            this.output = output;
        }

        public ItemStack[] getInputs() {
            ItemStack[] copy = new ItemStack[inputs.length];
            for (int index = 0; index < inputs.length; index++) {
                if (inputs[index] != null) copy[index] = inputs[index].copy();
            }
            return copy;
        }

        public ItemStack getOutput() { return output.copy(); }
    }
}
