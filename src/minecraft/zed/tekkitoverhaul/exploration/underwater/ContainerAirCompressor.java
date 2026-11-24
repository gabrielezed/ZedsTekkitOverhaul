package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

final class ContainerAirCompressor extends Container {
    private final TileAirCompressor machine;

    ContainerAirCompressor(InventoryPlayer player, TileAirCompressor machine) {
        this.machine = machine;
        addSlot(new Slot(machine, 0, 16, 26) {
            @Override
            public boolean isItemValid(ItemStack stack) { return OxygenCharge.isTank(stack); }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, 9 + row * 9 + column, 8 + column * 18, 87 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, 8 + column * 18, 145));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) { return machine.isUseableByPlayer(player); }

    @Override
    public ItemStack transferStackInSlot(int index) {
        Slot source = getSlot(index);
        ItemStack tank = source.getStack();
        if (!OxygenCharge.isTank(tank)) return null;
        if (index != 0) return move(source, getSlot(0), tank);
        for (int destination = inventorySlots.size() - 1; destination > 0; destination--) {
            ItemStack moved = move(source, getSlot(destination), tank);
            if (moved != null) return moved;
        }
        return null;
    }

    private ItemStack move(Slot source, Slot destination, ItemStack tank) {
        if (destination.getHasStack() || !destination.isItemValid(tank)) return null;
        destination.putStack(tank);
        source.putStack(null);
        source.onPickupFromSlot(tank);
        return tank.copy();
    }
}
