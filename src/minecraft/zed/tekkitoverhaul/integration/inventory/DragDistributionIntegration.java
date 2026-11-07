package zed.tekkitoverhaul.integration.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiChest;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.GuiCrafting;
import net.minecraft.src.GuiInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Minecraft 1.2.5: GUI event hook implementation, not a tick-based poller.
 * Called by a small, separate source patch in net.minecraft.src.GuiContainer.
 *
 * Deliberately conservative: only EMPTY ordinary slots, vanilla chest,
 * crafting table and player inventory. Uses regular synchronized windowClick.
 * The modified base class is necessary to suppress the first vanilla click.
 */
public final class DragDistributionIntegration {
    private static Session session;

    private DragDistributionIntegration() { }

    /** Called before the original GuiContainer.mouseClicked. */
    public static boolean onMousePressed(GuiContainer gui, Slot hovered, int button) {
        // A fresh click never inherits stale state from a former GUI.
        session = null;
        if (button != 0 && button != 1) return false;
        if (!isSupported(gui) || !validSlot(gui, hovered)) return false;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return false;
        if (Mouse.isButtonDown(1 - button)) return false;
        Minecraft mc = ModLoader.getMinecraftInstance();
        if (mc == null || mc.currentScreen != gui || mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.inventory.getItemStack();
        if (held == null || held.stackSize <= 1 || held.getMaxStackSize() <= 1) return false;
        if (!canPlace(hovered, held)) return false;
        session = new Session(gui, gui.inventorySlots.windowId, button, held.copy());
        session.slots.add(hovered);
        return true; // CANCEL vanilla's premature initial click.
    }

    /** Called on mouse move (button -1) or release (button 0/1). */
    public static boolean onMouseMovedOrUp(GuiContainer gui, Slot hovered, int button) {
        Session s = session;
        if (s == null) return false;
        Minecraft mc = ModLoader.getMinecraftInstance();
        if (mc == null || mc.currentScreen != gui || mc.thePlayer == null
                || s.gui != gui || s.windowId != gui.inventorySlots.windowId) {
            session = null;
            return false;
        }
        if (button == -1) {
            if (Mouse.isButtonDown(s.button) && validSlot(gui, hovered)
                    && canPlace(hovered, s.initial)) {
                addOnce(s, hovered);
            }
            return true;
        }
        if (button != s.button) return false;
        session = null;
        // Include the slot below the mouse upon release (rapid movement).
        if (validSlot(gui, hovered) && canPlace(hovered, s.initial)) {
            addOnce(s, hovered);
        }
        commit(mc, s);
        return true;
    }

    private static void commit(Minecraft mc, Session s) {
        ItemStack held = mc.thePlayer.inventory.getItemStack();
        if (held == null || !same(held, s.initial)
                || held.stackSize != s.initial.stackSize) return;
        List<Slot> slots = new ArrayList<Slot>();
        List<Integer> capacity = new ArrayList<Integer>();
        for (Slot slot : s.slots) {
            if (validSlot(s.gui, slot) && canPlace(slot, held)) {
                int limit = Math.min(slot.getSlotStackLimit(), held.getMaxStackSize());
                if (limit > 0) {
                    slots.add(slot);
                    capacity.add(Integer.valueOf(limit));
                }
            }
        }
        if (slots.isEmpty()) return;
        if (slots.size() == 1) {
            // A click without a drag is still a normal vanilla click.
            click(mc, s.gui, slots.get(0), s.button);
            return;
        }
        int[] amounts = new int[slots.size()];
        if (s.button == 1) {
            int remaining = held.stackSize;
            for (int i = 0; i < amounts.length && remaining > 0; i++, remaining--) {
                amounts[i] = 1;
            }
        } else {
            int[] caps = new int[capacity.size()];
            for (int i = 0; i < caps.length; i++) caps[i] = capacity.get(i).intValue();
            amounts = DistributionPlanner.distribute(held.stackSize, caps);
        }
        // Normal windowClick ensures container checks and networking are used.
        // This is an initial singleplayer-oriented implementation.
        for (int i = 0; i < slots.size(); i++) {
            for (int j = 0; j < amounts[i]; j++) {
                ItemStack current = mc.thePlayer.inventory.getItemStack();
                if (current == null || !same(current, s.initial)) return;
                int before = current.stackSize;
                click(mc, s.gui, slots.get(i), 1);
                ItemStack after = mc.thePlayer.inventory.getItemStack();
                if ((after == null ? 0 : after.stackSize) != before - 1) return;
            }
        }
    }

    private static void addOnce(Session s, Slot slot) {
        if (!s.slots.contains(slot)) s.slots.add(slot);
    }

    private static boolean isSupported(GuiContainer gui) {
        return gui instanceof GuiChest || gui instanceof GuiCrafting
                || gui instanceof GuiInventory;
    }

    private static boolean validSlot(GuiContainer gui, Slot slot) {
        if (slot == null || gui == null || gui.inventorySlots == null
                || !isSupported(gui)) return false;
        int index = gui.inventorySlots.inventorySlots.indexOf(slot);
        if (index < 0) return false;
        if (gui instanceof GuiCrafting) return index != 0; // Skip result slot.
        if (gui instanceof GuiInventory) {
            return index >= 1 && index <= 4 || index >= 9; // Skip result and armor.
        }
        return true; // Chest storage and player inventory slots.
    }

    private static boolean canPlace(Slot slot, ItemStack stack) {
        return slot != null && stack != null && slot.getStack() == null
                && slot.isItemValid(stack) && slot.getSlotStackLimit() > 0;
    }

    private static boolean same(ItemStack x, ItemStack y) {
        return x != null && y != null && x.itemID == y.itemID
                && x.getItemDamage() == y.getItemDamage();
    }

    private static void click(Minecraft mc, GuiContainer gui, Slot slot, int button) {
        mc.playerController.windowClick(gui.inventorySlots.windowId,
                slot.slotNumber, button, false, mc.thePlayer);
    }

    private static final class Session {
        final GuiContainer gui;
        final int windowId;
        final int button;
        final ItemStack initial;
        final List<Slot> slots = new ArrayList<Slot>();
        Session(GuiContainer gui, int windowId, int button, ItemStack initial) {
            this.gui = gui;
            this.windowId = windowId;
            this.button = button;
            this.initial = initial;
        }
    }
}
