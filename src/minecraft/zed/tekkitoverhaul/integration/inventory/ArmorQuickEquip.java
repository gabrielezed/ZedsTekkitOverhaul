package zed.tekkitoverhaul.integration.inventory;

import net.minecraft.src.Container;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

/** The successful local player-inventory transfer handled by the mouse dispatcher. */
final class ArmorQuickEquip {
    private ArmorQuickEquip() { }

    static boolean transfer(EntityPlayer player, Container container, Slot source) {
        if (container != player.inventorySlots || container != player.craftingInventory
                || container.getClass() != ContainerPlayer.class || container.inventorySlots.size() != 45
                || source == null || source.inventory != player.inventory || source.slotNumber < 9
                || source.slotNumber >= 45 || container.getSlot(source.slotNumber) != source
                || player.inventory.getItemStack() != null) return false;
        ItemStack stack = source.getStack();
        if (stack == null || stack.stackSize < 1 || !(stack.getItem() instanceof ItemArmor)) return false;
        int type = ((ItemArmor) stack.getItem()).armorType;
        if (type < 0 || type >= 4) return false;
        Slot target = container.getSlot(5 + type);
        if (target.inventory != player.inventory || target.getHasStack()
                || target.getSlotStackLimit() != 1 || !target.isItemValid(stack)) return false;
        target.putStack(source.decrStackSize(1));
        source.onSlotChanged();
        return true;
    }
}
