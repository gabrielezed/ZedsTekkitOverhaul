package zed.tekkitoverhaul.integration.inventory;

import java.lang.reflect.Method;
import java.util.function.BiPredicate;
import net.minecraft.src.Container;
import net.minecraft.src.ContainerFurnace;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;
import net.minecraft.src.TileEntityFurnace;

/** Local quick moves into the verified fuel slot of each installed furnace. */
public final class FurnaceFuelTransfer {
    private static final BiPredicate<Object, int[]> CALLBACK = new BiPredicate<Object, int[]>() {
        @Override public boolean test(Object player, int[] click) {
            return player instanceof EntityPlayer && transfer((EntityPlayer) player, click[0], click[1]);
        }
    };

    private FurnaceFuelTransfer() {}

    public static void register() {
        try {
            Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader())
                    .getMethod("registerFurnaceFuel", BiPredicate.class).invoke(null, CALLBACK);
        } catch (ReflectiveOperationException ignored) {
            // Plain mod installs keep the native quick-move behavior.
        }
    }

    private static boolean transfer(EntityPlayer player, int window, int index) {
        Container container = player.craftingInventory;
        if (player.worldObj == null || player.worldObj.isRemote || container == null || container.windowId != window
                || index < 0 || index >= container.inventorySlots.size()) return false;
        Slot source = container.getSlot(index);
        ItemStack stack = source.getStack();
        if (source.inventory != player.inventory || stack == null || stack.getItem() == null) return false;

        String type = container.getClass().getName();
        int fuelIndex;
        if (container.getClass() == ContainerFurnace.class || "ic2.common.ContainerIronFurnace".equals(type)
                || "railcraft.common.utility.ContainerBlastFurnace".equals(type)) fuelIndex = 1;
        else if ("eloraam.base.ContainerAlloyFurnace".equals(type)) fuelIndex = 9;
        else if ("ee.ContainerDMFurnace".equals(type) || "ee.ContainerRMFurnace".equals(type)) fuelIndex = 0;
        else return false;

        Slot fuel = container.getSlot(fuelIndex);
        if (fuel == null || fuel.inventory == player.inventory || !fuel.isItemValid(stack)
                || !accepted(container, fuel, stack)) return false;
        ItemStack existing = fuel.getStack();
        if (existing != null && (!existing.isItemEqual(stack)
                || !ItemStack.func_46154_a(existing, stack))) return true;
        int capacity = Math.min(stack.getMaxStackSize(), fuel.getSlotStackLimit());
        int amount = Math.min(stack.stackSize, capacity - (existing == null ? 0 : existing.stackSize));
        if (amount <= 0) return true;
        ItemStack merged = existing == null ? stack.copy() : existing.copy();
        merged.stackSize = amount + (existing == null ? 0 : existing.stackSize);
        stack.stackSize -= amount;
        fuel.putStack(merged);
        if (stack.stackSize == 0) source.putStack(null);
        else source.onSlotChanged();
        source.onPickupFromSlot(stack);
        return true;
    }

    private static boolean accepted(Container container, Slot slot, ItemStack stack) {
        String owner = slot.inventory.getClass().getName();
        if (container.getClass() == ContainerFurnace.class || "eloraam.base.TileAlloyFurnace".equals(owner))
            return TileEntityFurnace.isItemFuel(stack);
        if ("railcraft.common.utility.SlotBlastFurnaceFuel".equals(slot.getClass().getName())) return true;
        try {
            Class<?> tile = slot.inventory.getClass();
            // Several native fuel predicates initialize or consume their input. Probe an isolated copy.
            ItemStack probe = stack.copy();
            Method predicate;
            Object result;
            if ("ic2.common.TileEntityIronFurnace".equals(owner)) {
                predicate = tile.getMethod("getFuelValueFor", ItemStack.class);
                result = predicate.invoke(null, probe);
            } else if ("ee.TileDMFurnace".equals(owner)) {
                predicate = tile.getMethod("getItemBurnTime", ItemStack.class, Boolean.TYPE);
                result = predicate.invoke(slot.inventory, probe, true);
            } else if ("ee.TileRMFurnace".equals(owner)) {
                predicate = tile.getMethod("getItemBurnTime", ItemStack.class);
                result = predicate.invoke(slot.inventory, probe);
            } else return false;
            return ((Number) result).intValue() > 0;
        } catch (ReflectiveOperationException unavailable) {
            return false;
        } catch (LinkageError unavailable) {
            return false;
        }
    }
}
