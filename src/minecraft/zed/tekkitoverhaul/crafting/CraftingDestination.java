package zed.tekkitoverhaul.crafting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Container;
import net.minecraft.src.ContainerChest;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.ContainerWorkbench;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.PlayerControllerMP;
import net.minecraft.src.Slot;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.InventoryLargeChest;

public final class CraftingDestination {
    static boolean local(Minecraft game) {
        return game.theWorld != null && !game.theWorld.isRemote && game.thePlayer != null
                && game.thePlayer.isEntityAlive() && game.playerController != null
                && !(game.playerController instanceof PlayerControllerMP) && !game.playerController.isInCreativeMode();
    }

    static boolean canUse(Minecraft game, GuiContainer screen) {
        return local(game) && game.thePlayer.inventory.getItemStack() == null
                && screen.inventorySlots.canInteractWith(game.thePlayer);
    }

    static int gridWidth(Container container) {
        if (container.getClass() == ContainerWorkbench.class) return 3;
        if (container.getClass() == ContainerPlayer.class) return 2;
        if (container.getClass() == ContainerAdvancedCraftingTable.class) return 5;
        return 0;
    }

    static CraftingFill crafting(Minecraft game, GuiContainer screen) throws ReflectiveOperationException {
        return crafting(screen.inventorySlots, game.thePlayer);
    }

    static CraftingFill crafting(Container container, EntityPlayer player) throws ReflectiveOperationException {
        int width = gridWidth(container);
        if (width == 0) return null;
        int cells = width * width;
        List<Slot> slots = new ArrayList<Slot>();
        for (int index = 1; index <= cells; index++) slots.add(container.getSlot(index));
        List<Slot> recipeOnly = new ArrayList<Slot>();
        slots.addAll(ProtectedSlots.playerSlots(container, player, width == 2 ? 9 : cells + 1, true, recipeOnly));
        int returnEnd = slots.size();
        slots.addAll(recipeOnly);
        return new CraftingFill(slots.toArray(new Slot[slots.size()]), width, returnEnd);
    }

    public static int chestSize(Container container) {
        if (container.getClass() != ContainerChest.class) return 0;
        int size = container.inventorySlots.size() - 36;
        if (size != 27 && size != 54) return 0;
        Object inventory = container.getSlot(0).inventory;
        if (size == 27 ? inventory.getClass() != TileEntityChest.class : !isVanillaDoubleChest(inventory)) return 0;
        for (int index = 0; index < size; index++) {
            Slot slot = container.getSlot(index);
            if (slot.getClass() != Slot.class || slot.inventory != inventory) return 0;
        }
        return size;
    }

    private static boolean isVanillaDoubleChest(Object inventory) {
        if (inventory.getClass() != InventoryLargeChest.class) return false;
        Object first = null;
        int halves = 0;
        try {
            for (Field field : InventoryLargeChest.class.getDeclaredFields()) {
                if (field.getType() != IInventory.class) continue;
                field.setAccessible(true);
                Object half = field.get(inventory);
                if (half == null || half.getClass() != TileEntityChest.class || half == first) return false;
                first = half;
                halves++;
            }
        } catch (IllegalAccessException unavailable) {
            return false;
        }
        return halves == 2;
    }

    static String storage(Minecraft game, Container container, int action) throws ReflectiveOperationException {
        int size = chestSize(container);
        if (size == 0) return "Unsupported storage";
        List<Slot> slots = new ArrayList<Slot>();
        for (int index = 0; index < size; index++) slots.add(container.getSlot(index));
        slots.addAll(ProtectedSlots.playerSlots(container, game.thePlayer, size, false));
        InventoryPlan plan = new InventoryPlan(slots.toArray(new Slot[slots.size()]));
        int moved = planStorage(plan, size, action);
        if (!plan.commit(container)) return "Inventory changed";
        return moved == 0 ? "No eligible items fit" : "Moved " + moved + " items";
    }

    static int planStorage(InventoryPlan plan, int size, int action) {
        List<ItemStack> stored = new ArrayList<ItemStack>();
        for (int index = 0; index < size; index++) stored.add(plan.get(index));
        boolean take = action == 2;
        int moved = 0;
        for (int index = take ? 0 : size; index < (take ? size : plan.size()); index++) {
            ItemStack stack = plan.get(index);
            if (stack == null || (action == 0 && !contains(stored, stack))) continue;
            int left = plan.insert(stack, take ? size : 0, take ? plan.size() : size);
            moved += stack.stackSize - left;
            stack.stackSize = left;
            plan.set(index, left == 0 ? null : stack);
        }
        return moved;
    }

    static int exactCount(Container container, ItemStack query, int from, int to) {
        int count = 0;
        for (int index = from; index < to; index++) {
            ItemStack stack = container.getSlot(index).getStack();
            if (InventoryPlan.sameType(stack, query)) count += stack.stackSize;
        }
        return count;
    }

    private static boolean contains(List<ItemStack> stored, ItemStack stack) {
        for (ItemStack entry : stored) {
            if (InventoryPlan.sameType(stack, entry)) return true;
        }
        return false;
    }
}
