package zed.tekkitoverhaul.integration.inventory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Container;
import net.minecraft.src.ContainerChest;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.ContainerWorkbench;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Gui;
import net.minecraft.src.GuiChest;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.GuiCrafting;
import net.minecraft.src.GuiInventory;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.PlayerControllerMP;
import net.minecraft.src.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.crafting.ContainerAdvancedCraftingTable;
import zed.tekkitoverhaul.crafting.CraftingDestination;
import zed.tekkitoverhaul.crafting.GuiAdvancedCraftingTable;
import zed.tekkitoverhaul.crafting.InventoryPlan;

/** Collects a held-stack gesture before the legacy dispatcher clicks its first slot. */
public final class DragDistributionIntegration {
    private static Session session;
    private static Method slotAt;
    private static Method originalClick;
    private static Method widgetContains;
    private static Field neiDelete;
    private static Field infinite;
    private static Field left;
    private static Field top;

    private DragDistributionIntegration() { }

    public static void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.inputagent.MouseAgent", false,
                    ClassLoader.getSystemClassLoader());
            slotAt = findMethod(GuiContainer.class, "getSlotAtPosition", "a", int.class, int.class);
            originalClick = findMethod(GuiScreen.class, "mouseClicked", "a", int.class, int.class, int.class);
            left = findField(GuiContainer.class, "guiLeft", "e");
            top = findField(GuiContainer.class, "guiTop", "f");
            ClassLoader gameLoader = GuiContainer.class.getClassLoader();
            Class<?> nei = Class.forName("codechicken.nei.NEIController", false, gameLoader);
            neiDelete = nei.getField("deleteMode");
            infinite = findField(nei, "heldStackInfinite", "heldStackInfinite");
            widgetContains = Class.forName("codechicken.nei.LayoutManager", false, gameLoader)
                    .getMethod("widgetContains", int.class, int.class);
            boolean attached = (Boolean) agent.getMethod("register", Predicate.class).invoke(null, new Predicate<Object>() {
                @Override
                public boolean test(Object screen) {
                    return mouseEvent(screen);
                }
            });
            if (attached) ModLoader.getLogger().info("[ZTO] Inventory drag connected to the legacy mouse dispatcher");
        } catch (ClassNotFoundException absent) {
            // The release drag is opt-in through the launch agent.
        } catch (ReflectiveOperationException failure) {
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] Inventory drag is unavailable", failure);
        } catch (LinkageError failure) {
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] Inventory drag is unavailable", failure);
        }
    }

    private static Method findMethod(Class<?> type, String mcp, String release, Class<?>... args)
            throws NoSuchMethodException {
        Method method;
        try {
            method = type.getDeclaredMethod(mcp, args);
        } catch (NoSuchMethodException production) {
            method = type.getDeclaredMethod(release, args);
        }
        method.setAccessible(true);
        return method;
    }

    private static Field findField(Class<?> type, String mcp, String release) throws NoSuchFieldException {
        Field field;
        try {
            field = type.getDeclaredField(mcp);
        } catch (NoSuchFieldException production) {
            field = type.getDeclaredField(release);
        }
        field.setAccessible(true);
        return field;
    }

    private static boolean mouseEvent(Object screen) {
        if (!(screen instanceof GuiContainer)) {
            session = null;
            return false;
        }
        GuiContainer gui = (GuiContainer) screen;
        Minecraft game = ModLoader.getMinecraftInstance();
        if (!canUse(game, gui) || Mouse.getEventDWheel() != 0) {
            session = null;
            return false;
        }
        int button = Mouse.getEventButton();
        boolean pressed = Mouse.getEventButtonState();
        int x = Mouse.getEventX() * gui.width / game.displayWidth;
        int y = gui.height - Mouse.getEventY() * gui.height / game.displayHeight - 1;
        boolean ownsEvent = session != null && session.gui == gui && (button == -1 || button == session.button);
        try {
            if (neiDelete.getBoolean(null) || infinite.get(null) != null) {
                session = null;
                return ownsEvent && !pressed && button != -1;
            }
            boolean overWidget = (Boolean) widgetContains.invoke(null, x, y);
            Slot slot = overWidget ? null : (Slot) slotAt.invoke(gui, x, y);
            if (pressed) return begin(game, gui, slot, button, x, y);
            return advance(game, gui, slot, button);
        } catch (Exception failure) {
            session = null;
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] Inventory drag event rejected", failure);
            return ownsEvent;
        }
    }

    // Retain the local MCP source-hook entry points for the existing development workspace.
    public static boolean onMousePressed(GuiContainer gui, Slot slot, int button) {
        return begin(ModLoader.getMinecraftInstance(), gui, slot, button, -1, -1);
    }

    public static boolean onMouseMovedOrUp(GuiContainer gui, Slot slot, int button) {
        return advance(ModLoader.getMinecraftInstance(), gui, slot, button);
    }

    private static boolean begin(Minecraft game, GuiContainer gui, Slot slot, int button, int x, int y) {
        session = null;
        if ((button != 0 && button != 1) || !canUse(game, gui) || modified(game)
                || Mouse.isButtonDown(1 - button)) return false;
        ItemStack held = game.thePlayer.inventory.getItemStack();
        if (held == null || held.stackSize <= 1 || held.getMaxStackSize() <= 1
                || !validSlot(gui, slot) || room(slot, held) == 0) return false;
        session = new Session(game.thePlayer, gui, button, x, y, held, slot);
        return true;
    }

    private static boolean advance(Minecraft game, GuiContainer gui, Slot slot, int button) {
        Session drag = session;
        if (drag == null) return false;
        if (!current(game, drag) || drag.gui != gui || modified(game)) {
            session = null;
            return false;
        }
        if (button != -1 && button != drag.button) return false;
        if (drag.slots.size() < drag.initial.stackSize && validSlot(gui, slot)
                && room(slot, drag.initial) > 0 && !drag.slots.containsKey(slot)) {
            drag.slots.put(slot, copy(slot.getStack()));
        }
        if (button == -1) return true;
        session = null;
        finish(game, drag);
        return true;
    }

    private static void finish(Minecraft game, Session drag) {
        Slot[] slots = drag.slots.keySet().toArray(new Slot[drag.slots.size()]);
        int[] capacities = new int[slots.length];
        for (int index = 0; index < slots.length; index++) {
            Slot slot = slots[index];
            if (!validSlot(drag.gui, slot) || !ItemStack.areItemStacksEqual(drag.slots.get(slot), slot.getStack())) return;
            capacities[index] = room(slot, drag.initial);
            if (capacities[index] == 0) return;
        }
        if (slots.length == 1) {
            if (drag.x < 0) {
                click(game, drag, slots[0], drag.button);
            } else {
                try {
                    originalClick.invoke(drag.gui, drag.x, drag.y, drag.button);
                } catch (ReflectiveOperationException failure) {
                    ModLoader.getLogger().log(Level.WARNING, "[ZTO] Could not replay inventory click", failure);
                }
            }
            return;
        }
        int[] amounts = DistributionPlanner.drag(drag.initial.stackSize, capacities, drag.button == 1);
        int remaining = drag.initial.stackSize;
        for (int index = 0; index < slots.length; index++) {
            Slot slot = slots[index];
            ItemStack expected = drag.slots.get(slot);
            for (int count = 0; count < amounts[index]; count++) {
                ItemStack held = game.thePlayer.inventory.getItemStack();
                if (!canUse(game, drag.gui) || drag.gui.inventorySlots.windowId != drag.window
                        || game.thePlayer != drag.player || !InventoryPlan.sameType(held, drag.initial)
                        || held.stackSize != remaining || !validSlot(drag.gui, slot)
                        || !ItemStack.areItemStacksEqual(expected, slot.getStack()) || room(slot, held) == 0) return;
                click(game, drag, slot, 1);
                remaining--;
                int size = expected == null ? 0 : expected.stackSize;
                expected = drag.initial.copy();
                expected.stackSize = size + 1;
                ItemStack cursor = drag.player.inventory.getItemStack();
                if (!ItemStack.areItemStacksEqual(expected, slot.getStack())
                        || (remaining == 0 ? cursor != null
                            : !InventoryPlan.sameType(cursor, drag.initial) || cursor.stackSize != remaining)) return;
            }
        }
    }

    public static void renderPreview(Minecraft game, GuiScreen screen) {
        Session drag = session;
        if (drag == null) return;
        if (screen != drag.gui || !current(game, drag)) {
            session = null;
            return;
        }
        if (left == null || drag.slots.size() < 2) return;
        try {
            int x = left.getInt(drag.gui);
            int y = top.getInt(drag.gui);
            Slot[] slots = drag.slots.keySet().toArray(new Slot[drag.slots.size()]);
            int[] limits = new int[slots.length];
            for (int index = 0; index < slots.length; index++) limits[index] = room(slots[index], drag.initial);
            int[] amounts = DistributionPlanner.drag(drag.initial.stackSize, limits, drag.button == 1);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_LIGHTING);
                for (int index = 0; index < slots.length; index++) {
                    if (amounts[index] == 0) continue;
                    int sx = x + slots[index].xDisplayPosition;
                    int sy = y + slots[index].yDisplayPosition;
                    Gui.drawRect(sx, sy, sx + 16, sy + 16, 0x60FFFFFF);
                    String count = "+" + amounts[index];
                    game.fontRenderer.drawStringWithShadow(count, sx + 17 - game.fontRenderer.getStringWidth(count),
                            sy + 9, 0xFFFFA0);
                }
            } finally {
                GL11.glPopAttrib();
            }
        } catch (IllegalAccessException unavailable) {
            session = null;
        }
    }

    private static boolean modified(Minecraft game) {
        int drop = game.gameSettings.keyBindDrop.keyCode;
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)
                || (drop > 0 && drop < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(drop));
    }

    private static boolean current(Minecraft game, Session drag) {
        return canUse(game, drag.gui) && game.thePlayer == drag.player && drag.gui.inventorySlots.windowId == drag.window
                && ItemStack.areItemStacksEqual(drag.initial, drag.player.inventory.getItemStack());
    }

    private static boolean canUse(Minecraft game, GuiContainer gui) {
        return game != null && game.currentScreen == gui && game.theWorld != null && !game.theWorld.isRemote
                && game.thePlayer != null && game.thePlayer.isEntityAlive() && game.playerController != null
                && !(game.playerController instanceof PlayerControllerMP) && !game.playerController.isInCreativeMode()
                && game.thePlayer.craftingInventory == gui.inventorySlots
                && supported(gui) && gui.inventorySlots.canInteractWith(game.thePlayer);
    }

    private static boolean supported(GuiContainer gui) {
        Class<?> screen = gui.getClass();
        Class<?> container = gui.inventorySlots.getClass();
        return screen == GuiInventory.class && container == ContainerPlayer.class
                || screen == GuiCrafting.class && container == ContainerWorkbench.class
                || screen == GuiAdvancedCraftingTable.class && container == ContainerAdvancedCraftingTable.class
                || screen == GuiChest.class && container == ContainerChest.class;
    }

    private static boolean validSlot(GuiContainer gui, Slot slot) {
        if (slot == null || slot.getClass() != Slot.class) return false;
        Container container = gui.inventorySlots;
        int index = slot.slotNumber;
        if (index < 0 || index >= container.inventorySlots.size() || container.getSlot(index) != slot) return false;
        if (gui.getClass() == GuiChest.class) {
            return CraftingDestination.chestSize(container) > 0;
        }
        return index != 0 && (gui.getClass() != GuiInventory.class || index <= 4 || index >= 9);
    }

    private static int room(Slot slot, ItemStack stack) {
        if (!slot.isItemValid(stack)) return 0;
        ItemStack present = slot.getStack();
        if (present != null && !InventoryPlan.sameType(present, stack)) return 0;
        return Math.max(0, Math.min(slot.getSlotStackLimit(), stack.getMaxStackSize())
                - (present == null ? 0 : present.stackSize));
    }

    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }

    private static void click(Minecraft game, Session drag, Slot slot, int button) {
        game.playerController.windowClick(drag.window, slot.slotNumber, button, false, drag.player);
    }

    private static final class Session {
        final EntityPlayer player;
        final GuiContainer gui;
        final int window;
        final int button;
        final int x;
        final int y;
        final ItemStack initial;
        final Map<Slot, ItemStack> slots = new LinkedHashMap<Slot, ItemStack>();

        Session(EntityPlayer player, GuiContainer gui, int button, int x, int y, ItemStack initial, Slot first) {
            this.player = player;
            this.gui = gui;
            this.window = gui.inventorySlots.windowId;
            this.button = button;
            this.x = x;
            this.y = y;
            this.initial = initial.copy();
            slots.put(first, copy(first.getStack()));
        }
    }
}
