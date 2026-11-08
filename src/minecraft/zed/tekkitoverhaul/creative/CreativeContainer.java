package zed.tekkitoverhaul.creative;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryBasic;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

final class CreativeContainer extends Container {
    static final int COLUMNS = 9;
    static final int ROWS = 5;
    private final InventoryBasic catalog = new InventoryBasic("Creative catalog", COLUMNS * ROWS);
    private final InventoryBasic discard = new InventoryBasic("Delete item", 1);
    private final List<Slot> catalogSlots = new ArrayList<Slot>();
    private final List<Slot> playerSlots = new ArrayList<Slot>();

    CreativeContainer(final EntityPlayer player) {
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            addSlot(new Slot(catalog, i, 9 + i % COLUMNS * 18, 18 + i / COLUMNS * 18));
        }
        for (int i = 0; i < 9; i++) addSlot(new Slot(player.inventory, i, 9 + i * 18, 112));
        for (Object slot : inventorySlots) catalogSlots.add((Slot) slot);

        inventorySlots.clear();
        inventoryItemStacks.clear();
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(player.inventory, 39 - i, 9 + i / 2 * 54, 6 + i % 2 * 27) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return player.inventorySlots.getSlot(slotNumber + 5).isItemValid(stack);
                }

                @Override
                public int getSlotStackLimit() { return 1; }
            });
        }
        for (int i = 9; i < 36; i++) {
            addSlot(new Slot(player.inventory, i, 9 + i % 9 * 18, 54 + (i / 9 - 1) * 18));
        }
        for (int i = 0; i < 9; i++) addSlot(new Slot(player.inventory, i, 9 + i * 18, 112));
        addSlot(new Slot(discard, 0, 173, 112));
        for (Object slot : inventorySlots) playerSlots.add((Slot) slot);
        showInventory(false);
    }

    void showInventory(boolean showPlayer) {
        inventorySlots.clear();
        inventoryItemStacks.clear();
        for (Slot slot : showPlayer ? playerSlots : catalogSlots) addSlot(slot);
    }

    void fill(List<CreativeCatalog.Entry> entries, int row) {
        for (int slot = 0; slot < catalog.getSizeInventory(); slot++) {
            int index = row * COLUMNS + slot;
            catalog.setInventorySlotContents(slot, index < entries.size() ? entries.get(index).stack.copy() : null);
        }
    }

    boolean isCatalog(Slot slot) { return slot.inventory == catalog; }
    boolean isDiscard(Slot slot) { return slot.inventory == discard; }

    @Override
    public ItemStack slotClick(int index, int button, boolean shift, EntityPlayer player) {
        if (!canInteractWith(player) || (index < 0 && index != -999) || index >= inventorySlots.size()) return null;
        if (index >= 0) {
            Slot slot = getSlot(index);
            if (isCatalog(slot) || isDiscard(slot)) return null;
        }
        return super.slotClick(index, button, shift, player);
    }

    @Override
    public ItemStack transferStackInSlot(int index) {
        // NEI can call the container directly. Vanilla's default returns the unchanged stack and retries.
        return null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) { return player.capabilities.isCreativeMode; }
}
