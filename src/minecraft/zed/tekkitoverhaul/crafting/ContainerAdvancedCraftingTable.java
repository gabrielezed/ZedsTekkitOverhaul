package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Block;
import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryCrafting;
import net.minecraft.src.InventoryCraftResult;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;
import net.minecraft.src.SlotCrafting;
import net.minecraft.src.World;

/** 25 crafting inputs, one result slot and 36 player inventory slots. */
public final class ContainerAdvancedCraftingTable extends Container {
    public final InventoryCrafting craftMatrix = new InventoryCrafting(this, 5, 5);
    public final IInventory craftResult = new InventoryCraftResult();
    private final World world;
    private final int x, y, z;

    public ContainerAdvancedCraftingTable(InventoryPlayer inventory, World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;

        // Vanilla SlotCrafting consumes ingredients across the crafting inventory.
        addSlot(new SlotCrafting(inventory.player, craftMatrix, craftResult, 0, 158, 66));
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                addSlot(new Slot(craftMatrix, col + row * 5, 14 + col * 18, 24 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + (row + 1) * 9, 25 + col * 18, 142 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 25 + col * 18, 208));
        }
        onCraftMatrixChanged(craftMatrix);
    }

    @Override
    public void onCraftMatrixChanged(IInventory changed) {
        craftResult.setInventorySlotContents(0, AdvancedCraftingManager.findMatchingRecipe(craftMatrix));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
    	return world.getBlockId(x, y, z) == AdvancedCraftingRegistry.advancedTable.blockID
            && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D;
    }


    @Override
    public void onCraftGuiClosed(EntityPlayer player) {
        super.onCraftGuiClosed(player);
        // Return remaining inputs when the GUI closes, as a vanilla crafting table does.
        for (int index = 0; index < craftMatrix.getSizeInventory(); index++) {
            ItemStack stack = craftMatrix.getStackInSlot(index);
            if (stack != null) {
                player.dropPlayerItem(stack);
                craftMatrix.setInventorySlotContents(index, null);
            }
        }
    }

    // Shift-click transfer remains unimplemented until the basic crafting flow is verified.
    @Override
    public ItemStack transferStackInSlot(int slotIndex) {
        return null;
    }
}
