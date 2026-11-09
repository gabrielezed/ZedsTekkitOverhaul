package zed.tekkitoverhaul.crafting;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Gui;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Adds ordinary screen controls without replacing NEI or the container click handler. */
public final class CraftingUx {
    private static final int RETURN = 2;
    private static final int RELOAD = 3;
    private static final int STORAGE = 4;
    private static final int BACK = 7;
    private static final int FORWARD = 8;
    private static final int MACHINE_INFO = 9;
    private static final int BOOKMARK = 10;
    private static final int OPEN_BOOKMARK = 11;
    private static final int TOTALS = 12;
    private final ArrayDeque<GuiContainer> forward = new ArrayDeque<GuiContainer>();
    private final List<ActionButton> added = new ArrayList<ActionButton>();
    private final List<String> tooltip = new ArrayList<String>();
    private Field controls;
    private Field xSize;
    private Field ySize;
    private Field buttonWidth;
    private Field buttonHeight;
    private LegacyNei nei;
    private RecipeBookmarks bookmarks;
    private boolean initialized;
    private boolean broken;
    private GuiContainer screen;
    private Pending pending;
    private LegacyNei.Selection lastRecipe;
    private EntityPlayer player;
    private long keyboardEvent;
    private long previewUntil;
    private long nextVisibilityRefresh;
    private ActionButton hovered;
    private String status = "";
    private long statusUntil;
    private boolean partialEnabled;
    private boolean showTotals;
    private int activeChest;

    public void tick(Minecraft game, GuiScreen visible, boolean partialEnabled, int shortcut) {
        if (broken) return;
        this.partialEnabled = partialEnabled;
        GuiScreen rendered = visible;
        if (player != game.thePlayer) {
            player = game.thePlayer;
            lastRecipe = null;
            pending = null;
            status = "";
            showTotals = false;
        }
        try {
            initialize();
            if (pending != null) {
                Pending request = pending;
                pending = null;
                if (game.currentScreen == request.screen && request.player == game.thePlayer
                        && request.screen.inventorySlots == game.thePlayer.craftingInventory
                        && request.window == request.screen.inventorySlots.windowId) {
                    execute(game, request, partialEnabled);
                }
                visible = game.currentScreen;
            }
            if (!(visible instanceof GuiContainer) || player == null) {
                screen = null;
                forward.clear();
                added.clear();
                hovered = null;
                return;
            }
            GuiContainer container = (GuiContainer) visible;
            List<?> list = (List<?>) controls.get(container);
            if (screen != container || !list.containsAll(added)) {
                if (screen != container) observeNavigation(container);
                screen = container;
                added.clear();
                hovered = null;
                addControls(container, list);
                nextVisibilityRefresh = 0;
            }
            long now = System.currentTimeMillis();
            if (now >= nextVisibilityRefresh) {
                nextVisibilityRefresh = now + 150L;
                refreshVisibility(container);
            }
            int mouseX = Mouse.getX() * container.width / game.displayWidth;
            int mouseY = container.height - Mouse.getY() * container.height / game.displayHeight - 1;
            ActionButton underMouse = null;
            for (ActionButton button : added) {
                if (button.drawButton && button.contains(mouseX, mouseY)) {
                    underMouse = button;
                    break;
                }
            }
            long event = Keyboard.getEventNanoseconds();
            if (event != keyboardEvent) {
                keyboardEvent = event;
                if (shortcut > 0 && Keyboard.getEventKey() == shortcut && Keyboard.getEventKeyState()
                        && !Keyboard.isRepeatEvent() && underMouse != null) {
                    queue(game, underMouse.action);
                }
            }
            if (rendered == container) drawFeedback(game, underMouse, mouseY, now);
        } catch (Exception failure) {
            disable(failure);
        } catch (LinkageError failure) {
            disable(failure);
        }
    }

    private void drawFeedback(Minecraft game, ActionButton underMouse, int mouseY, long now)
            throws IllegalAccessException {
        if (underMouse != null) {
            if (hovered != underMouse || now >= previewUntil) {
                hovered = underMouse;
                previewUntil = now + 300L;
                try {
                    describe(game, underMouse);
                } catch (Exception unavailable) {
                    tooltip.clear();
                    tooltip.add("Inventory details are unavailable");
                }
            }
            drawLines(game, tooltip, mouseY + 14);
        } else {
            hovered = null;
            if (showTotals) drawTotals(game, mouseY);
        }
        if (now < statusUntil && !added.isEmpty()) {
            int top = (screen.height - ySize.getInt(screen)) / 2;
            drawLines(game, Arrays.asList(status), Math.max(3, top - 13));
        }
    }

    private void drawTotals(Minecraft game, int mouseY) throws IllegalAccessException {
        Container container = screen.inventorySlots;
        int chest = activeChest;
        if (chest == 0) return;
        int x = Mouse.getX() * screen.width / game.displayWidth - (screen.width - xSize.getInt(screen)) / 2;
        int y = mouseY - (screen.height - ySize.getInt(screen)) / 2;
        for (int index = 0; index < chest + 36; index++) {
            Slot slot = container.getSlot(index);
            ItemStack stack = slot.getStack();
            if (stack == null || x < slot.xDisplayPosition || x >= slot.xDisplayPosition + 16
                    || y < slot.yDisplayPosition || y >= slot.yDisplayPosition + 16) continue;
            drawLines(game, Arrays.asList("Exact stacks: chest " + CraftingDestination.exactCount(container, stack, 0, chest)
                    + " / bag " + CraftingDestination.exactCount(container, stack, chest, chest + 27)
                    + " / hotbar " + CraftingDestination.exactCount(container, stack, chest + 27, chest + 36)), mouseY + 24);
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private void drawLines(Minecraft game, List<String> lines, int y) {
        if (lines.isEmpty()) return;
        List<String> fitted = new ArrayList<String>();
        for (String line : lines) {
            fitted.addAll(game.fontRenderer.listFormattedStringToWidth(line, Math.max(16, screen.width - 16)));
        }
        int wide = 0;
        for (String line : fitted) wide = Math.max(wide, game.fontRenderer.getStringWidth(line));
        int x = Math.max(3, (screen.width - wide) / 2);
        y = Math.max(4, Math.min(y, screen.height - fitted.size() * 10 - 4));
        // The ModLoader GUI callback runs after the container and NEI have rendered.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            int bottom = y + fitted.size() * 10;
            Gui.drawRect(x - 4, y - 4, x + wide + 4, bottom + 3, 0xF0100010);
            Gui.drawRect(x - 3, y - 3, x + wide + 3, y - 2, 0x505000FF);
            Gui.drawRect(x - 3, bottom + 1, x + wide + 3, bottom + 2, 0x5028007F);
            Gui.drawRect(x - 3, y - 2, x - 2, bottom + 1, 0x503800BF);
            Gui.drawRect(x + wide + 2, y - 2, x + wide + 3, bottom + 1, 0x503800BF);
            for (String line : fitted) {
                game.fontRenderer.drawStringWithShadow(line, x, y, 0xFFFFFF);
                y += 10;
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void refreshVisibility(GuiContainer container) throws ReflectiveOperationException {
        boolean showNei = nei != null && nei.handles(container) && nei.visible();
        for (ActionButton button : added) {
            if (button.action < RETURN) {
                button.drawButton = showNei && nei.selection(container, button.action) != null;
            } else if (button.action == BACK) {
                button.drawButton = showNei && nei.previous(container) != nei.destination(container);
            } else if (button.action == FORWARD) {
                button.drawButton = showNei && !forward.isEmpty();
            } else if (button.action == BOOKMARK) {
                button.drawButton = showNei;
            }
        }
    }

    private void observeNavigation(GuiContainer next) throws ReflectiveOperationException {
        if (nei != null && screen != null && nei.handles(screen) && nei.handles(next)
                && nei.previous(screen) == next) {
            if (forward.size() == 16) forward.removeLast();
            forward.addFirst(screen);
        } else if (forward.peekFirst() != next) {
            forward.clear();
        } else {
            forward.removeFirst();
        }
    }

    private void disable(Throwable failure) {
        broken = true;
        for (ActionButton button : added) button.drawButton = false;
        ModLoader.getLogger().log(Level.WARNING, "[ZTO] Crafting UX integration disabled", failure);
    }

    private void initialize() throws ReflectiveOperationException {
        if (initialized) return;
        initialized = true;
        controls = field(GuiScreen.class, "controlList", "s");
        xSize = field(GuiContainer.class, "xSize", "b");
        ySize = field(GuiContainer.class, "ySize", "c");
        buttonWidth = field(GuiButton.class, "width", "a");
        buttonHeight = field(GuiButton.class, "height", "b");
        try {
            nei = new LegacyNei();
        } catch (ClassNotFoundException absent) {
            return;
        }
        AdvancedNeiRecipes.register();
    }

    @SuppressWarnings("unchecked")
    private void addControls(GuiContainer gui, List<?> existing) throws ReflectiveOperationException {
        activeChest = CraftingDestination.chestSize(gui.inventorySlots);
        int width = xSize.getInt(gui);
        int left = (gui.width - width) / 2;
        int top = (gui.height - ySize.getInt(gui)) / 2;
        if (nei != null && nei.handles(gui)) {
            for (int row = 0; row < 2; row++) {
                GuiButton overlay = (GuiButton) Class.forName("codechicken.nei.GuiRecipe")
                        .getField(row == 0 ? "overlay1" : "overlay2").get(gui);
                if (overlay != null) add(existing, row, overlay.xPosition - 14, overlay.yPosition, 12, "+");
            }
            int footer = top + ySize.getInt(gui) - 18;
            add(existing, BACK, left + 3, footer, 13, "<");
            add(existing, FORWARD, left + width - 16, footer, 13, ">");
            add(existing, BOOKMARK, left + 36, footer, 12, "B");
        } else if (CraftingDestination.gridWidth(gui.inventorySlots) > 0) {
            int grid = CraftingDestination.gridWidth(gui.inventorySlots);
            int start = grid == 2 ? 88 : grid == 3 ? 102 : width - 53;
            int controlsY = top + (grid == 2 ? 67 : 4);
            add(existing, RETURN, left + start, controlsY, 38, "Return");
            add(existing, RELOAD, left + start + 40, controlsY, 12, "R");
            if (nei != null && grid != 5) {
                add(existing, OPEN_BOOKMARK, left + (grid == 2 ? 142 : 5), controlsY, 12, "B");
            }
        } else if (activeChest > 0) {
            add(existing, TOTALS, left + width - 92, top + 4, 14, "#");
            add(existing, STORAGE, left + width - 76, top + 4, 20, "DM");
            add(existing, STORAGE + 1, left + width - 55, top + 4, 20, "DA");
            add(existing, STORAGE + 2, left + width - 34, top + 4, 20, "TA");
        } else if (MaceratorFill.supports(gui)) {
            add(existing, MACHINE_INFO, left + 121, top + 4, 15, "i");
        }
        ((List<Object>) existing).addAll(added);
    }

    private void add(List<?> existing, int action, int x, int y, int width, String title)
            throws IllegalAccessException {
        if (x < 0 || y < 0 || x + width > screen.width || y + 12 > screen.height) return;
        for (Object control : existing) {
            if (!(control instanceof GuiButton)) continue;
            GuiButton button = (GuiButton) control;
            // The recipe overlay and + touch edges but have disjoint hitboxes.
            if (x < button.xPosition + buttonWidth.getInt(button) && button.xPosition < x + width
                    && y < button.yPosition + buttonHeight.getInt(button) && button.yPosition < y + 12) return;
        }
        added.add(new ActionButton(action, x, y, width, title));
    }

    private static Field field(Class<?> owner, String mcp, String release) throws NoSuchFieldException {
        Field field;
        try {
            field = owner.getDeclaredField(mcp);
        } catch (NoSuchFieldException production) {
            field = owner.getDeclaredField(release);
        }
        field.setAccessible(true);
        return field;
    }

    private void queue(Minecraft game, int action) {
        if (pending != null || game.currentScreen != screen || player == null) return;
        try {
            LegacyNei.Selection selection = action < RETURN ? nei.selection(screen, action) : null;
            boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
            boolean pin = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            pending = new Pending(screen, player, action, selection, shift, alt, pin);
        } catch (ReflectiveOperationException failure) {
            feedback("Recipe details are unavailable");
        }
    }

    private void execute(Minecraft game, Pending request, boolean partialEnabled) throws ReflectiveOperationException {
        if (request.action == MACHINE_INFO) return;
        if (request.action == TOTALS) {
            showTotals = !showTotals;
            feedback(showTotals ? "Exact chest totals on" : "Exact chest totals off");
            return;
        }
        if (request.action == BOOKMARK || request.action == OPEN_BOOKMARK) {
            useBookmark(game, request);
            return;
        }
        if (request.action == BACK || request.action == FORWARD) {
            if (game.thePlayer.inventory.getItemStack() != null) return;
            GuiContainer next = request.action == BACK ? nei.previous(screen) : forward.peekFirst();
            if (next != null && nei.handles(next) && nei.destination(next) == nei.destination(screen)) {
                game.displayGuiScreen(next);
            }
            return;
        }
        LegacyNei.Selection selection = request.selection;
        if (request.action < RETURN) {
            LegacyNei.Selection current = nei.selection(screen, request.action);
            if (!sameSelection(selection, current)) {
                feedback("Recipe changed. Try again.");
                return;
            }
        } else if (request.action == RELOAD) {
            int width = CraftingDestination.gridWidth(screen.inventorySlots);
            selection = lastRecipe == null ? null : lastRecipe.forGrid(width);
            if (selection == null) {
                feedback("No prepared recipe for this grid");
                return;
            }
        }
        GuiContainer destination = request.action < RETURN ? selection.destination : screen;
        if (!CraftingDestination.canUse(game, destination)) {
            feedback(blockedReason(game));
            return;
        }
        CraftingFill.Result planned;
        try {
            if (request.action >= STORAGE) {
                feedback(CraftingDestination.storage(game, destination.inventorySlots, request.action - STORAGE));
                return;
            }
            if (request.action == RETURN) {
                planned = CraftingDestination.crafting(game, destination).returnInputs();
            } else {
                planned = plan(game, destination, selection, request.shift, partialEnabled && request.alt, request.pin);
            }
        } catch (IllegalStateException unavailable) {
            feedback(unavailable.getMessage());
            return;
        } catch (ReflectiveOperationException unavailable) {
            feedback("The installed inventory adapter is unavailable");
            return;
        }
        if (planned.plan == null) {
            feedback(planned.status);
            return;
        }
        if (destination != screen) game.displayGuiScreen(destination);
        if (game.currentScreen != destination || game.thePlayer.craftingInventory != destination.inventorySlots
                || !CraftingDestination.canUse(game, destination) || !planned.plan.commit(destination.inventorySlots)) {
            feedback("Inventory changed. Try again.");
            return;
        }
        if (selection != null && !selection.machine && planned.status.equals("Ready")) lastRecipe = selection;
        feedback(planned.alternatives ? "Prepared " + planned.sets + " sets using alternatives"
                : selection != null && request.shift && planned.sets > 1
                ? "Prepared " + planned.sets + " sets" : planned.status);
    }

    private void useBookmark(Minecraft game, Pending request) {
        if (!CraftingDestination.local(game) || game.thePlayer.inventory.getItemStack() != null) {
            feedback(blockedReason(game));
            return;
        }
        try {
            if (!nei.visible()) {
                feedback("Enable NEI to use bookmarks");
                return;
            }
            if (bookmarks == null) bookmarks = new RecipeBookmarks(game.mcDataDir);
            if (request.action == BOOKMARK && !request.shift) {
                feedback(bookmarks.toggle(nei.bookmark(screen)));
                return;
            }
            RecipeBookmarks.Entry entry = bookmarks.advance();
            if (entry == null) feedback("No saved recipes");
            else if (!nei.openBookmark(game, screen, entry)) feedback("Bookmarked recipe is unavailable");
        } catch (IOException unavailable) {
            feedback("Could not read or save recipe bookmarks");
        } catch (ReflectiveOperationException unavailable) {
            feedback("Recipe bookmark is unavailable");
        }
    }

    private static CraftingFill.Result plan(Minecraft game, GuiContainer destination, LegacyNei.Selection selection,
                                            boolean batch, boolean partial, boolean pin) throws ReflectiveOperationException {
        if (selection.machine) return MaceratorFill.prepare(game, selection, batch);
        CraftingFill fill = CraftingDestination.crafting(game, destination);
        CraftingFill.Result result = batch ? fill.maximum(selection.pattern, selection.output)
                : fill.prepare(selection.pattern, selection.output, 1, false);
        if (!pin) {
            for (boolean wildcard : selection.wildcards) {
                if (!wildcard) continue;
                result = new VariantFill(fill, selection).prepare(result, batch);
                break;
            }
        }
        return result.plan == null && partial && !batch
                ? fill.prepare(selection.pattern, selection.output, 1, true) : result;
    }

    private static boolean sameSelection(LegacyNei.Selection first, LegacyNei.Selection second) {
        if (first == null || second == null || first.handler != second.handler || first.recipe != second.recipe
                || first.destination != second.destination || !ItemStack.areItemStacksEqual(first.output, second.output)
                || first.pattern.length != second.pattern.length) return false;
        for (int index = 0; index < first.pattern.length; index++) {
            if (!ItemStack.areItemStacksEqual(first.pattern[index], second.pattern[index])) return false;
        }
        return true;
    }

    private void feedback(String message) {
        status = message;
        statusUntil = System.currentTimeMillis() + 4000L;
        previewUntil = 0;
    }

    private static String blockedReason(Minecraft game) {
        if (!CraftingDestination.local(game)) return "Requires a local survival world";
        if (game.thePlayer.inventory.getItemStack() != null) return "Empty the cursor first";
        return "Container is no longer accessible";
    }

    private void describe(Minecraft game, ActionButton button) throws ReflectiveOperationException {
        tooltip.clear();
        int action = button.action;
        if (action < RETURN) {
            tooltip.add("Prepare recipe");
            LegacyNei.Selection selection = nei.selection(screen, action);
            if (selection == null) return;
            tooltip.add(selection.machine ? "Shift: maximum" : "Shift: maximum / Ctrl: pin variant");
            if (partialEnabled && !selection.machine) tooltip.add("Alt: allow partial fill");
            if (!CraftingDestination.canUse(game, selection.destination)) {
                tooltip.add(blockedReason(game));
                return;
            }
            CraftingFill.Result one = plan(game, selection.destination, selection, false, false, false);
            CraftingFill.Result maximum = plan(game, selection.destination, selection, true, false, false);
            tooltip.add(one.alternatives ? "Ready using compatible alternatives" : one.status);
            tooltip.add("Loadable sets: " + (maximum.plan == null ? 0 : maximum.sets));
            if (!selection.machine) {
                int stock = CraftingDestination.crafting(game, selection.destination).stockSets(selection.pattern);
                tooltip.add("Stock sets for shown variants: " + stock);
            }
            for (int index = 0; index < selection.pattern.length; index++) {
                ItemStack ingredient = selection.pattern[index];
                if (ingredient != null && one.available[index] < ingredient.stackSize) {
                    tooltip.add((one.available[index] == 0 ? "Missing " : "Partial ")
                            + (index + 1) + ": " + ingredient.getItem().getItemDisplayName(ingredient)
                            + " " + one.available[index] + "/" + ingredient.stackSize);
                    if (tooltip.size() == 8) {
                        tooltip.add("More ingredients are missing");
                        break;
                    }
                }
            }
        } else if (action == MACHINE_INFO) {
            MaceratorFill.describe(game, screen, tooltip);
        } else if (action == BOOKMARK || action == OPEN_BOOKMARK) {
            tooltip.add(action == BOOKMARK ? "Toggle recipe bookmark" : "Open next saved recipe");
            if (action == BOOKMARK) tooltip.add("Shift: open next saved recipe");
        } else if (action == TOTALS) {
            tooltip.add("Toggle exact chest, bag and hotbar counts");
            tooltip.add("Counts include protected slots");
        } else {
            String[] names = {"Return ingredients", "Reload last prepared recipe", "Deposit Matching",
                              "Deposit All", "Take All", "Back to previous recipe", "Forward to next recipe"};
            tooltip.add(names[action - RETURN]);
            if (!CraftingDestination.canUse(game, screen)) tooltip.add(blockedReason(game));
            else if (action >= STORAGE && action < BACK) tooltip.add("Hotbar and protected slots are skipped");
        }
    }

    private final class ActionButton extends GuiButton {
        final int action;

        ActionButton(int action, int x, int y, int width, String title) {
            super(-18200 - action, x, y, width, 12, title);
            this.action = action;
        }

        boolean contains(int x, int y) {
            return x >= xPosition && x < xPosition + width && y >= yPosition && y < yPosition + height;
        }

        @Override
        public boolean mousePressed(Minecraft game, int x, int y) {
            if (!super.mousePressed(game, x, y)) return false;
            queue(game, action);
            return true;
        }

        @Override
        public void drawButton(Minecraft game, int x, int y) {
            if (!drawButton) return;
            boolean over = contains(x, y);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, game.renderEngine.getTexture("/gui/gui.png"));
            GL11.glColor4f(1F, 1F, 1F, 1F);
            int v = 46 + getHoverState(over) * 20;
            int left = width / 2;
            int right = width - left;
            int top = height / 2;
            int bottom = height - top;
            drawTexturedModalRect(xPosition, yPosition, 0, v, left, top);
            drawTexturedModalRect(xPosition + left, yPosition, 200 - right, v, right, top);
            drawTexturedModalRect(xPosition, yPosition + top, 0, v + 20 - bottom, left, bottom);
            drawTexturedModalRect(xPosition + left, yPosition + top, 200 - right, v + 20 - bottom, right, bottom);
            drawCenteredString(game.fontRenderer, displayString, xPosition + width / 2,
                    yPosition + (height - 8) / 2, !enabled ? 0xA0A0A0 : over ? 0xFFFFA0 : 0xE0E0E0);
        }

    }

    private static final class Pending {
        final GuiContainer screen;
        final EntityPlayer player;
        final int action;
        final LegacyNei.Selection selection;
        final int window;
        final boolean shift;
        final boolean alt;
        final boolean pin;

        Pending(GuiContainer screen, EntityPlayer player, int action, LegacyNei.Selection selection,
                boolean shift, boolean alt, boolean pin) {
            this.screen = screen;
            this.player = player;
            this.action = action;
            this.selection = selection;
            this.window = screen.inventorySlots.windowId;
            this.shift = shift;
            this.alt = alt;
            this.pin = pin;
        }
    }
}
