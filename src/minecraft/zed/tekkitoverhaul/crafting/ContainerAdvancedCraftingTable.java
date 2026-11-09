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
    private final EntityPlayer owner;
    private final int x, y, z;

    public ContainerAdvancedCraftingTable(InventoryPlayer inventory, World world, int x, int y, int z) {
        this.world = world;
        this.owner = inventory.player;
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

    @Override
    public ItemStack transferStackInSlot(int slotIndex) {
        if (slotIndex != 0 || world.isRemote || owner.craftingInventory != this || !canInteractWith(owner)
                || owner.inventory.getItemStack() != null) return null;
        Slot result = getSlot(0);
        ItemStack output = result.getStack();
        if (output == null) return null;
        Slot[] destinations = new Slot[36];
        for (int index = 0; index < destinations.length; index++) {
            destinations[index] = getSlot(61 - index);
        }
        InventoryPlan plan = new InventoryPlan(destinations);
        if (plan.insert(output, 0, destinations.length) != 0 || !plan.commit(this)) return null;
        ItemStack crafted = result.decrStackSize(output.stackSize);
        result.onPickupFromSlot(crafted);
        return crafted;
    }

    @Override
    protected void retrySlotClick(int slotIndex, int mouseButton, boolean shift, EntityPlayer player) {
        if (slotIndex != 0 || player.craftingInventory != this || !canInteractWith(player)) return;
        for (int attempt = 1; attempt < 64; attempt++) {
            int before = ingredientCount();
            if (transferStackInSlot(0) == null || ingredientCount() >= before) return;
        }
    }

    private int ingredientCount() {
        int total = 0;
        for (int index = 0; index < craftMatrix.getSizeInventory(); index++) {
            ItemStack stack = craftMatrix.getStackInSlot(index);
            if (stack != null) total += stack.stackSize;
        }
        return total;
    }
}
