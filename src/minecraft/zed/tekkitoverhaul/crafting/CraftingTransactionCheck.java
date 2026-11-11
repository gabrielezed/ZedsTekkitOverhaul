package zed.tekkitoverhaul.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.src.Block;
import net.minecraft.src.Container;
import net.minecraft.src.ContainerWorkbench;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;
import net.minecraft.src.World;

/** Called only by the disposable-world bridge command. Uses the live recipe and protection configuration. */
public final class CraftingTransactionCheck {
    private CraftingTransactionCheck() {}

    public static String run(EntityPlayer player, World world, int x, int y, int z) throws ReflectiveOperationException {
        ItemStack[] saved = player.inventory.mainInventory;
        player.inventory.mainInventory = new ItemStack[saved.length];
        try {
            ContainerWorkbench workbench = new ContainerWorkbench(player.inventory, world, x, y, z);
            ItemStack[] vanilla = new ItemStack[9];
            for (int cell : new int[] {0, 1, 3, 4}) vanilla[cell] = new ItemStack(Block.workbench);
            ItemStack table = new ItemStack(AdvancedCraftingRegistry.advancedTable);
            boolean craft3 = craft(workbench, player, vanilla, table, 4);

            player.inventory.setItemStack(null);
            Arrays.fill(player.inventory.mainInventory, null);
            ContainerAdvancedCraftingTable advanced = new ContainerAdvancedCraftingTable(player.inventory, world, x, y, z);
            ItemStack[] perimeter = new ItemStack[25];
            for (int cell = 0; cell < perimeter.length; cell++) {
                if (cell < 5 || cell >= 20 || cell % 5 == 0 || cell % 5 == 4) perimeter[cell] = new ItemStack(Block.stone);
            }
            boolean craft5 = craft(advanced, player, perimeter, new ItemStack(AdvancedCraftingRegistry.foundry), 16);
            Arrays.fill(player.inventory.mainInventory, null);
            ContainerWorkbench protection = new ContainerWorkbench(player.inventory, world, x, y, z);
            return ",\"craft3\":" + craft3 + ",\"craft5\":" + craft5
                    + ",\"hardProtection\":" + hardProtection(player, protection, vanilla, table)
                    + ",\"hotbarLock\":" + hotbarLock(player, protection, vanilla, table);
        } finally {
            player.inventory.setItemStack(null);
            player.inventory.mainInventory = saved;
        }
    }

    private static String hardProtection(EntityPlayer player, Container workbench, ItemStack[] recipe, ItemStack output)
            throws ReflectiveOperationException {
        List<Slot> recipeOnly = new ArrayList<Slot>();
        List<Slot> eligible = ProtectedSlots.playerSlots(workbench, player, 10, true, recipeOnly);
        for (int i = 10; i < 46; i++) {
            Slot slot = workbench.getSlot(i);
            if (eligible.contains(slot) || recipeOnly.contains(slot)) continue;
            ItemStack protectedStack = new ItemStack(Block.workbench, 4);
            slot.putStack(protectedStack.copy());
            boolean preserved = CraftingDestination.crafting(workbench, player).prepare(recipe, output, 1, false).plan == null
                    && ItemStack.areItemStacksEqual(protectedStack, slot.getStack()) && gridEmpty(workbench, recipe.length);
            slot.putStack(null);
            return Boolean.toString(preserved);
        }
        return "null";
    }

    private static boolean craft(Container container, EntityPlayer player, ItemStack[] recipe, ItemStack output, int count)
            throws ReflectiveOperationException {
        int start = recipe.length + 1;
        List<Slot> eligible = ProtectedSlots.playerSlots(container, player, start, true);
        if (eligible.size() < 2) throw new IllegalStateException("The test requires two unprotected inventory slots");
        Slot source = eligible.get(0);
        Slot destination = eligible.get(1);
        CraftingFill fill = CraftingDestination.crafting(container, player);
        ItemStack ingredient = recipe[0].copy();
        ingredient.stackSize = count - 1;
        source.putStack(ingredient.copy());
        if (fill.prepare(recipe, output, 1, false).plan != null || !gridEmpty(container, recipe.length)
                || !ItemStack.areItemStacksEqual(source.getStack(), ingredient)) return false;

        ingredient.stackSize = count + 1;
        source.putStack(ingredient);
        CraftingFill.Result prepared = fill.prepare(recipe, output, 1, false);
        if (prepared.plan == null || !prepared.plan.commit(container)
                || !ItemStack.areItemStacksEqual(output, container.getSlot(0).getStack())) return false;
        container.slotClick(0, 0, false, player);
        if (!ItemStack.areItemStacksEqual(output, player.inventory.getItemStack())) return false;
        container.slotClick(destination.slotNumber, 0, false, player);
        ingredient = recipe[0].copy();
        return gridEmpty(container, recipe.length) && player.inventory.getItemStack() == null
                && ItemStack.areItemStacksEqual(ingredient, source.getStack())
                && ItemStack.areItemStacksEqual(output, destination.getStack());
    }

    private static String hotbarLock(EntityPlayer player, Container workbench, ItemStack[] recipe, ItemStack output)
            throws ReflectiveOperationException {
        List<Slot> recipeOnly = new ArrayList<Slot>();
        List<Slot> eligible = ProtectedSlots.playerSlots(workbench, player, 10, true, recipeOnly);
        if (recipeOnly.isEmpty()) return "null";
        Slot locked = recipeOnly.get(0);
        locked.putStack(new ItemStack(Block.workbench, 4));
        CraftingFill fill = CraftingDestination.crafting(workbench, player);
        CraftingFill.Result prepared = fill.prepare(recipe, output, 1, false);
        if (eligible.contains(locked) || prepared.plan == null || !prepared.plan.commit(workbench)
                || locked.getStack() != null) return "false";
        for (Slot slot : eligible) slot.putStack(new ItemStack(Block.dirt, 64));
        CraftingFill.Result returned = fill.returnInputs();
        return Boolean.toString(returned.plan.commit(workbench) && locked.getStack() == null
                && workbench.getSlot(1).getStack() != null);
    }

    private static boolean gridEmpty(Container container, int cells) {
        for (int slot = 0; slot <= cells; slot++) if (container.getSlot(slot).getStack() != null) return false;
        return true;
    }
}
