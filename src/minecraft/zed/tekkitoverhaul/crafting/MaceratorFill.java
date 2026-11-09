package zed.tekkitoverhaul.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Container;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

final class MaceratorFill {
    static boolean supports(GuiContainer gui) {
        Container container = gui.inventorySlots;
        return gui.getClass().getName().equals("ic2.platform.GuiMacerator")
                && container.getClass().getName().equals("ic2.common.ContainerElectricMachine")
                && container.inventorySlots.size() == 43
                && container.getSlot(0).getClass() == Slot.class
                && container.getSlot(0).inventory.getClass().getName().equals("ic2.common.TileEntityMacerator");
    }

    static CraftingFill.Result prepare(Minecraft game, LegacyNei.Selection selection, boolean batch)
            throws ReflectiveOperationException {
        if (!supports(selection.destination)) return fail("Unsupported machine input", 0);
        Container container = selection.destination.inventorySlots;
        List<Slot> slots = new ArrayList<Slot>();
        slots.add(container.getSlot(0));
        List<Slot> recipeOnly = new ArrayList<Slot>();
        slots.addAll(ProtectedSlots.playerSlots(container, game.thePlayer, 7, true, recipeOnly));
        slots.addAll(recipeOnly);
        InventoryPlan plan = new InventoryPlan(slots.toArray(new Slot[slots.size()]));
        ItemStack input = selection.pattern[0];
        ItemStack existing = plan.get(0);
        if (existing != null && !InventoryPlan.sameType(input, existing)) return fail("Clear the input slot", 0);
        if (!ItemStack.areItemStacksEqual(selection.output, result(input))) return fail("Invalid Macerator recipe", 0);
        int present = existing == null ? 0 : existing.stackSize;
        int limit = plan.limit(0, input);
        int available = present;
        for (int index = 1; index < plan.size(); index++) {
            ItemStack stack = plan.get(index);
            if (InventoryPlan.sameType(stack, input)) available += stack.stackSize;
        }
        int target = batch ? Math.min(available, limit) / input.stackSize * input.stackSize : input.stackSize;
        if (target < input.stackSize || target > available || target > limit || present > limit) {
            return fail("Missing ingredients or input capacity", Math.min(available, limit));
        }
        int remaining = Math.max(0, target - present);
        for (int index = 1; index < plan.size() && remaining > 0; index++) {
            ItemStack stack = plan.get(index);
            if (!InventoryPlan.sameType(stack, input)) continue;
            int taken = Math.min(stack.stackSize, remaining);
            remaining -= taken;
            stack.stackSize -= taken;
            plan.set(index, stack.stackSize == 0 ? null : stack);
        }
        plan.set(0, withCount(input, Math.max(present, target)));
        CraftingFill.Result prepared = new CraftingFill.Result(plan, new int[] {available}, "Macerator input ready");
        prepared.sets = target / input.stackSize;
        return prepared;
    }

    static void describe(Minecraft game, GuiContainer screen, List<String> lines)
            throws ReflectiveOperationException {
        lines.add("IndustrialCraft 2 / Macerator");
        lines.add("Input: top / Battery: lower left");
        lines.add("Output: right / Upgrades: far right");
        if (game.theWorld == null || game.theWorld.isRemote || !supports(screen)) return;
        Slot inputSlot = screen.inventorySlots.getSlot(0);
        Object machine = inputSlot.inventory;
        Class<?> type = machine.getClass();
        int energy = type.getField("energy").getInt(machine);
        int capacity = type.getField("maxEnergy").getInt(machine);
        int rate = type.getField("energyConsume").getInt(machine);
        int length = type.getField("operationLength").getInt(machine);
        lines.add(energy + "/" + capacity + " EU stored");
        lines.add(rate + " EU/t / " + length + " ticks per operation");
        ItemStack input = inputSlot.getStack();
        if (input == null) lines.add("Add a maceratable input");
        else if (result(input) == null) lines.add("Check the input recipe and amount");
        else if (!(Boolean) type.getMethod("canOperate").invoke(machine)) lines.add("Clear room in the output");
        else if (energy < rate) lines.add("Supply power or a charged battery");
    }

    private static ItemStack result(ItemStack input) throws ReflectiveOperationException {
        return (ItemStack) Class.forName("ic2.api.Ic2Recipes")
                .getMethod("getMaceratorOutputFor", ItemStack.class, boolean.class)
                .invoke(null, input.copy(), false);
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.stackSize = count;
        return copy;
    }

    private static CraftingFill.Result fail(String status, int available) {
        return new CraftingFill.Result(null, new int[] {available}, status);
    }
}
