package zed.tekkitoverhaul.integration.inventory;

import net.minecraft.src.Container;
import net.minecraft.src.ContainerChest;
import net.minecraft.src.ContainerDispenser;
import net.minecraft.src.ContainerFurnace;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.ContainerWorkbench;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.Slot;
import zed.tekkitoverhaul.crafting.ContainerAdvancedCraftingTable;
import zed.tekkitoverhaul.crafting.CraftingDestination;
import zed.tekkitoverhaul.hopper.ContainerHopper;

/** Recognizes ordinary destinations without treating every plain Slot as a machine input. */
final class DragSlotPolicy {
    private static final boolean RELEASE = Container.class.getName().equals("dd");
    private static final ClassValue<Boolean> STANDARD_GUI = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type; current != GuiContainer.class; current = current.getSuperclass()) {
                if (declares(current, RELEASE ? "a" : "mouseClicked", int.class, int.class, int.class)
                        || declares(current, RELEASE ? "a" : "handleMouseClick",
                                    Slot.class, int.class, int.class, boolean.class)
                        || declares(current, "clickSlot", Slot.class, int.class, int.class, boolean.class)) return false;
            }
            return true;
        }
    };
    private static final ClassValue<Boolean> STANDARD_CONTAINER = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                Class<?> owner = type.getMethod(RELEASE ? "a" : "slotClick",
                        int.class, int.class, boolean.class, EntityPlayer.class).getDeclaringClass();
                return owner == Container.class || owner.getName().equals("ic2.common.ContainerIC2")
                        || owner.getName().equals("buildcraft.factory.ContainerAutoWorkbench")
                            && type == owner;
            } catch (NoSuchMethodException unavailable) {
                return false;
            }
        }
    };

    private DragSlotPolicy() { }

    static boolean canPlace(GuiContainer gui, Slot slot, EntityPlayer player) {
        if (slot == null || slot.getClass() != Slot.class || !STANDARD_GUI.get(gui.getClass())) return false;
        Container container = gui.inventorySlots;
        int index = slot.slotNumber;
        int count = container.inventorySlots.size();
        if (index < 0 || index >= count || container.getSlot(index) != slot
                || !STANDARD_CONTAINER.get(container.getClass())) return false;
        if (slot.inventory == player.inventory) return true;

        Class<?> type = container.getClass();
        if (type == ContainerChest.class) return index < CraftingDestination.chestSize(container);
        if (type == ContainerPlayer.class) return count == 45 && index >= 1 && index <= 4;
        if (type == ContainerWorkbench.class) return count == 46 && index >= 1 && index <= 9;
        if (type == ContainerAdvancedCraftingTable.class) return count == 62 && index >= 1 && index <= 25;
        if (type == ContainerHopper.class) return count == 41 && index < 5;
        if (type == ContainerDispenser.class) return count == 45 && index < 9;
        if (type == ContainerFurnace.class) return count == 39 && index < 2;
        return isModInput(type.getName(), index, count, slot.inventory.getSizeInventory());
    }

    static boolean isModInput(String container, int index, int count, int inventorySize) {
        if (index < 0 || index >= count) return false;
        if (container.equals("cpw.mods.ironchest.ContainerIronChestBase")
                || container.equals("codechicken.enderstorage.ContainerEnderStorage")
                || container.equals("ee.ContainerAlchChest")) {
            return count == inventorySize + 36 && index < inventorySize;
        }
        if (container.equals("ee.ContainerCondenser")) return count == 128 && index >= 1 && index <= 91;
        if (container.equals("ee.ContainerPortableCrafting")
                || container.equals("buildcraft.factory.ContainerAutoWorkbench")) {
            return count == 46 && index >= 1 && index <= 9;
        }
        if (container.equals("eloraam.base.ContainerAlloyFurnace")) return count == 47 && index < 9;
        if (container.equals("eloraam.base.ContainerAdvBench")) {
            // The refill result sits between the crafting matrix and the storage rows.
            return count == 64 && index < 28 && index != 9;
        }
        if (container.equals("eloraam.machine.ContainerBufferChest")) return count == 56 && index < 20;
        if (container.equals("eloraam.machine.ContainerDeploy") || container.equals("eloraam.machine.ContainerEject")) {
            return count == 45 && index < 9;
        }
        if (container.equals("eloraam.machine.ContainerBlueFurnace")) return count == 38 && index == 0;
        if (container.equals("eloraam.machine.ContainerBlueAlloyFurnace")) return count == 46 && index < 9;
        return container.equals("ic2.common.ContainerElectricMachine") && count == 43 && index == 0;
    }

    private static boolean declares(Class<?> type, String name, Class<?>... parameters) {
        try {
            type.getDeclaredMethod(name, parameters);
            return true;
        } catch (NoSuchMethodException inherited) {
            return false;
        }
    }
}
