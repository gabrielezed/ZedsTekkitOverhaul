package zed.tekkitoverhaul.crafting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.ItemStack;

/** Optional bridge to the unmodified NEI 1.2.2.4 recipe screen. */
final class LegacyNei {
    private final Class<?> screen = Class.forName("codechicken.nei.GuiRecipe");
    private final Class<?> handler = Class.forName("codechicken.nei.IRecipeHandler");
    private final Class<?> positioned = Class.forName("codechicken.nei.PositionedStack");
    private final Method enabled = Class.forName("codechicken.nei.NEIConfig").getMethod("isEnabled");
    private final Method hidden = enabled.getDeclaringClass().getMethod("isHidden");
    private final Method count = handler.getMethod("getNumRecipes");
    private final Method ingredients = handler.getMethod("getIngredientStacks", int.class);
    private final Method result = handler.getMethod("getResultStack", int.class);
    private final Method name = handler.getMethod("getRecipeName");
    private final Method currentHandlers = screen.getMethod("getCurrentRecipeHandlers");
    private final Field query = screen.getField("key");
    private final Field handlers = screen.getField("currenthandlers");
    private final Field category = screen.getField("recipetype");
    private final Field page = screen.getField("page");
    private final Field first = screen.getField("firstGui");
    private final Field previous = screen.getField("prevGui");
    private final Field x = positioned.getField("relx");
    private final Field y = positioned.getField("rely");
    private final Field item = positioned.getField("item");

    LegacyNei() throws ReflectiveOperationException { }

    boolean handles(GuiContainer gui) {
        return screen.isInstance(gui);
    }

    boolean visible() throws ReflectiveOperationException {
        return (Boolean) enabled.invoke(null) && !(Boolean) hidden.invoke(null);
    }

    GuiContainer destination(GuiContainer gui) throws ReflectiveOperationException {
        return (GuiContainer) first.get(gui);
    }

    GuiContainer previous(GuiContainer gui) throws ReflectiveOperationException {
        return (GuiContainer) previous.get(gui);
    }

    RecipeBookmarks.Entry bookmark(GuiContainer gui) throws ReflectiveOperationException {
        List<?> categories = (List<?>) handlers.get(gui);
        Object active = categories.get(category.getInt(gui));
        return new RecipeBookmarks.Entry((ItemStack) query.get(gui),
                gui.getClass().getName().equals("codechicken.nei.GuiUsageRecipe"),
                (String) name.invoke(active), page.getInt(gui));
    }

    boolean openBookmark(Minecraft game, GuiContainer from, RecipeBookmarks.Entry bookmark)
            throws ReflectiveOperationException {
        Class<?> view = Class.forName(bookmark.usage ? "codechicken.nei.GuiUsageRecipe"
                : "codechicken.nei.GuiCraftingRecipe");
        GuiContainer next = (GuiContainer) view.getConstructor(ItemStack.class, GuiContainer.class)
                .newInstance(bookmark.item.copy(), from);
        List<?> categories = (List<?>) currentHandlers.invoke(next);
        if (categories.isEmpty()) return false;
        for (int index = 0; index < categories.size(); index++) {
            Object candidate = categories.get(index);
            if (!bookmark.category.equals(name.invoke(candidate))) continue;
            category.setInt(next, index);
            page.setInt(next, Math.min(bookmark.page, ((Integer) count.invoke(candidate) - 1) / 2));
            break;
        }
        // Same preservation path as NEI's recipe key when opening from a real inventory.
        if (!handles(from)) game.currentScreen = null;
        game.displayGuiScreen(next);
        return true;
    }

    Selection selection(GuiContainer gui, int row) throws ReflectiveOperationException {
        GuiContainer destination = destination(gui);
        List<?> categories = (List<?>) handlers.get(gui);
        int type = category.getInt(gui);
        if (destination == null || type < 0 || type >= categories.size()) return null;
        Object active = categories.get(type);
        int index = page.getInt(gui) * 2 + row;
        if (index < 0 || index >= (Integer) count.invoke(active)) return null;
        int width = CraftingDestination.gridWidth(destination.inventorySlots);
        String name = active.getClass().getName();
        boolean machine = width == 0 && MaceratorFill.supports(destination)
                && name.equals("ic2.neiIntegration11x.platform.MaceratorRecipeHandler");
        boolean advanced = width == 5 && active instanceof java.lang.reflect.Proxy
                && java.lang.reflect.Proxy.getInvocationHandler(active) instanceof AdvancedNeiRecipes;
        boolean shapeless = name.equals("codechicken.nei.ShapelessRecipeHandler")
                || name.equals("ic2.neiIntegration11x.platform.AdvShapelessRecipeHandler");
        boolean shaped = name.equals("codechicken.nei.ShapedRecipeHandler")
                || name.equals("ic2.neiIntegration11x.platform.AdvRecipeHandler");
        if (!machine && !advanced && (!(shaped || shapeless) || (width != 2 && width != 3))) return null;
        Object output = result.invoke(active, index);
        if (output == null) return null;
        ItemStack[] pattern = new ItemStack[machine ? 1 : width * width];
        List<Integer> cells = new ArrayList<Integer>();
        int used = 0;
        for (Object entry : (List<?>) ingredients.invoke(active, index)) {
            int column = x.getInt(entry) - (advanced ? 8 : 25);
            int line = y.getInt(entry) - (advanced ? 4 : 6);
            int cell = machine || shapeless ? used : column / 18 + line / 18 * width;
            if (used >= pattern.length || cell < 0 || cell >= pattern.length || pattern[cell] != null
                    || (!machine && !shapeless && (column < 0 || line < 0 || column % 18 != 0
                            || line % 18 != 0 || column / 18 >= width))) return null;
            ItemStack stack = (ItemStack) item.get(entry);
            if (stack == null || stack.getItemDamage() < 0 || stack.stackSize <= 0) return null;
            pattern[cell] = stack.copy();
            cells.add(cell);
            if (!machine) pattern[cell].stackSize = 1;
            used++;
        }
        if (used == 0) return null;
        boolean[] wildcards = new boolean[pattern.length];
        if (name.equals("codechicken.nei.ShapedRecipeHandler") || name.equals("codechicken.nei.ShapelessRecipeHandler")) {
            readWildcards(active, index, cells, wildcards);
        }
        return new Selection(destination, active, index, pattern, ((ItemStack) item.get(output)).copy(), machine,
                shapeless, wildcards);
    }

    private void readWildcards(Object active, int index, List<Integer> cells, boolean[] wildcards) {
        try {
            List<?> recipes = (List<?>) active.getClass().getField("arecipes").get(active);
            Object cached = recipes.get(index);
            Field raw = cached.getClass().getDeclaredField("ingredients");
            raw.setAccessible(true);
            List<?> stacks = (List<?>) raw.get(cached);
            if (stacks.size() != cells.size()) return;
            for (int ingredient = 0; ingredient < stacks.size(); ingredient++) {
                ItemStack input = (ItemStack) item.get(stacks.get(ingredient));
                wildcards[cells.get(ingredient)] = input.getItemDamage() == -1;
            }
        } catch (ReflectiveOperationException unavailable) {
            // The displayed ingredient remains usable when no wildcard declaration is available.
        }
    }

    static final class Selection {
        final GuiContainer destination;
        final Object handler;
        final int recipe;
        final ItemStack[] pattern;
        final ItemStack output;
        final boolean machine;
        final boolean shapeless;
        final boolean[] wildcards;

        Selection(GuiContainer destination, Object handler, int recipe, ItemStack[] pattern,
                  ItemStack output, boolean machine, boolean shapeless, boolean[] wildcards) {
            this.destination = destination;
            this.handler = handler;
            this.recipe = recipe;
            this.pattern = pattern;
            this.output = output;
            this.machine = machine;
            this.shapeless = shapeless;
            this.wildcards = wildcards;
        }

        Selection forGrid(int width) {
            if (machine || width == 0) return null;
            if (width * width == pattern.length) return this;
            if (width == 5 || pattern.length == 25) return null;
            int previousWidth = pattern.length == 9 ? 3 : 2;
            ItemStack[] resized = new ItemStack[width * width];
            boolean[] flags = new boolean[resized.length];
            int used = 0;
            for (int cell = 0; cell < pattern.length; cell++) {
                if (pattern[cell] == null) continue;
                int x = cell % previousWidth;
                int y = cell / previousWidth;
                if (!shapeless && (x >= width || y >= width)) return null;
                int target = shapeless ? used++ : x + y * width;
                if (target >= resized.length) return null;
                resized[target] = pattern[cell];
                flags[target] = wildcards[cell];
            }
            return new Selection(destination, handler, recipe, resized, output, false, shapeless, flags);
        }
    }
}
