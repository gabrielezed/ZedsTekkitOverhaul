package zed.tekkitoverhaul.crafting;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.FractionationProcess;

/** Adapts the existing 5x5 and fractionation registries to legacy NEI's display API. */
final class AdvancedNeiRecipes implements InvocationHandler {
    private final Class<?> crafting;
    private final Class<?> usage;
    private final Constructor<?> positioned;
    private final Constructor<?> renderer;
    private final Method text;
    private final Method bindTexture;
    private final Method blit;
    private final List<Entry> entries;
    private final boolean grid;

    static void register() throws ReflectiveOperationException {
        Class<?> api = Class.forName("codechicken.nei.API");
        api.getMethod("registerGuiOverlay", Class.class, String.class, int.class, int.class)
                .invoke(null, GuiAdvancedCraftingTable.class, "zto.advanced", 6, 20);
        // A full 5x5 needs more than the 65 pixels allocated to one of two recipes.
        // Register each fixed pattern separately so it owns a full category page.
        for (AdvancedCraftingManager.Recipe recipe : AdvancedCraftingManager.getRecipes()) {
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(new Entry(recipe.getInputs(), recipe.getOutput(), null));
            new AdvancedNeiRecipes(entries, true).registerWith(api);
        }
        List<Entry> processes = new ArrayList<Entry>();
        int boiler = DistillationRegistry.blockId(DistillationPartType.BOILER);
        if (boiler < 0) return;
        for (DistillateType type : DistillateType.values()) {
            Item bucket = DistillationRegistry.bucketFor(type.id);
            if (bucket != null) {
                processes.add(new Entry(new ItemStack[] {new ItemStack(boiler, 1, 0), new ItemStack(Item.bucketWater)},
                        new ItemStack(bucket), type));
            }
        }
        new AdvancedNeiRecipes(processes, false).registerWith(api);
    }

    private AdvancedNeiRecipes(List<Entry> entries, boolean grid) throws ReflectiveOperationException {
        this.entries = entries;
        this.grid = grid;
        crafting = Class.forName("codechicken.nei.ICraftingHandler");
        usage = Class.forName("codechicken.nei.IUsageHandler");
        positioned = Class.forName("codechicken.nei.PositionedStack").getConstructor(ItemStack.class, int.class, int.class);
        renderer = Class.forName("codechicken.nei.DefaultOverlayRenderer")
                .getConstructor(ArrayList.class, GuiContainer.class);
        Class<?> gui = Class.forName("codechicken.nei.GuiManager");
        text = gui.getMethod("drawText", int.class, int.class, String.class, int.class);
        bindTexture = gui.getMethod("bindTextureByName", String.class);
        blit = gui.getMethod("drawTexturedModalRect", int.class, int.class, int.class, int.class, int.class, int.class);
    }

    private Object proxy() {
        return Proxy.newProxyInstance(crafting.getClassLoader(), new Class<?>[] {crafting, usage}, this);
    }

    private void registerWith(Class<?> api) throws ReflectiveOperationException {
        Object adapter = proxy();
        api.getMethod("registerRecipeHandler", crafting).invoke(null, adapter);
        api.getMethod("registerUsageHandler", usage).invoke(null, adapter);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws ReflectiveOperationException {
        String name = method.getName();
        if (name.equals("equals")) return proxy == args[0];
        if (name.equals("hashCode")) return System.identityHashCode(proxy);
        if (name.equals("getRecipeName") || name.equals("toString")) {
            return grid ? "Advanced Crafting" : "Oil Fractionation";
        }
        if (name.equals("getRecipeHandler") || name.equals("getUsageHandler")) {
            List<Entry> matches = new ArrayList<Entry>();
            ItemStack query = (ItemStack) args[0];
            for (Entry entry : entries) {
                if (name.equals("getRecipeHandler") ? InventoryPlan.sameType(query, entry.output) : entry.uses(query)) {
                    matches.add(entry);
                }
            }
            return new AdvancedNeiRecipes(matches, grid).proxy();
        }
        if (name.equals("getNumRecipes")) return entries.size();
        if (name.equals("onUpdate")) return null;
        if (name.equals("hasOverlay")) return grid && args[0].getClass() == GuiAdvancedCraftingTable.class;
        if (name.equals("getOtherStacks")) return new ArrayList<Object>();
        if (name.equals("getOverlayRenderer")) {
            return grid && args[0].getClass() == GuiAdvancedCraftingTable.class
                    ? renderer.newInstance(ingredients(entries.get((Integer) args[1])), args[0]) : null;
        }
        Entry entry = entries.get((Integer) args[args.length - 1]);
        if (name.equals("getIngredientStacks")) return ingredients(entry);
        if (name.equals("getResultStack")) return positioned.newInstance(entry.output, grid ? 124 : 119, grid ? 26 : 0);
        if (name.equals("drawBackground") && grid) {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            Object gui = args[0];
            bindTexture.invoke(gui, "/gui/crafting.png");
            for (int cell = 0; cell < 25; cell++) {
                int x = 7 + cell % 5 * 18;
                int y = 3 + cell / 5 * 18;
                blit.invoke(gui, x, y, 29, 16, 18, 18);
            }
            blit.invoke(gui, 97, 25, 90, 35, 22, 15);
            blit.invoke(gui, 119, 21, 119, 30, 26, 26);
        }
        if (name.equals("drawForeground") && !grid) drawProcess(args[0], entry.type);
        return null;
    }

    private ArrayList<Object> ingredients(Entry entry) throws ReflectiveOperationException {
        ArrayList<Object> stacks = new ArrayList<Object>();
        for (int index = 0; index < entry.inputs.length; index++) {
            if (entry.inputs[index] == null) continue;
            int x = grid ? 8 + index % 5 * 18 : 8 + index * 18;
            int y = grid ? 4 + index / 5 * 18 : 0;
            stacks.add(positioned.newInstance(entry.inputs[index], x, y));
        }
        return stacks;
    }

    private void drawProcess(Object gui, DistillateType type) throws ReflectiveOperationException {
        String[] lines = {
            FractionationProcess.BATCH_INPUT_MB + " mB oil + " + type.coolantPerBatchMB + " mB water",
            FractionationProcess.BATCH_OUTPUT_MB + " mB " + type.displayName,
            type.temperatureCelsius + " C / " + type.durationTicks + " ticks / " + FractionationProcess.EU_PER_TICK + " EU/t",
            (type.durationTicks * FractionationProcess.EU_PER_TICK) + " EU per batch"
        };
        for (int line = 0; line < lines.length; line++) {
            text.invoke(gui, 8, 19 + line * 10, lines[line], 0x404040);
        }
    }

    private static final class Entry {
        final ItemStack[] inputs;
        final ItemStack output;
        final DistillateType type;

        Entry(ItemStack[] inputs, ItemStack output, DistillateType type) {
            this.inputs = inputs;
            this.output = output;
            this.type = type;
        }

        boolean uses(ItemStack item) {
            for (ItemStack input : inputs) {
                if (InventoryPlan.sameType(input, item)) return true;
            }
            return type != null && DistillationRegistry.isBuildCraftOilBucket(item);
        }
    }
}
