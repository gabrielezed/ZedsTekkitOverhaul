package zed.tekkitoverhaul.creative;

import java.util.List;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.GuiInventory;
import net.minecraft.src.GuiTextField;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.RenderHelper;
import net.minecraft.src.RenderManager;
import net.minecraft.src.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

final class GuiCreativeBackport extends GuiContainer {
    private static final int SCROLL_X = 175;
    private static final int SCROLL_Y = 18;
    private static final int SCROLL_TRAVEL = 95;
    private final CreativeContainer container;
    private final CreativeCatalog catalog;
    private final CreativeInventoryHook hook;
    private List<CreativeCatalog.Entry> entries;
    private GuiTextField search;
    private String query = "";
    private int tab;
    private int row;
    private boolean dragging;

    GuiCreativeBackport(EntityPlayer player, CreativeCatalog catalog, CreativeInventoryHook hook) {
        super(new CreativeContainer(player));
        container = (CreativeContainer) inventorySlots;
        this.catalog = catalog;
        this.hook = hook;
        xSize = 195;
        ySize = 136;
        refreshEntries();
    }

    @Override
    public void initGui() {
        super.initGui();
        search = new GuiTextField(fontRenderer, guiLeft + 82, guiTop + 6, 89, 9);
        search.setMaxStringLength(32);
        search.setEnableBackgroundDrawing(false);
        search.setText(query);
        search.setFocused(tab == CreativeCategories.SEARCH);
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void updateScreen() {
        if (!CreativeInventoryHook.canUse(mc)) {
            if (mc.thePlayer != null && mc.thePlayer.isEntityAlive() && mc.theWorld != null) {
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            } else {
                super.updateScreen();
            }
            return;
        }
        search.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        dragging = false;
        super.onGuiClosed();
        if (mc.thePlayer != null && mc.thePlayer.craftingInventory == inventorySlots) {
            mc.thePlayer.craftingInventory = mc.thePlayer.inventorySlots;
        }
    }

    private void refreshEntries() {
        entries = catalog.entries(tab, query);
        row = 0;
        container.fill(entries, row);
    }

    private void selectTab(int next) {
        if (next == tab) return;
        tab = next;
        dragging = false;
        container.showInventory(tab == CreativeCategories.INVENTORY);
        search.setFocused(tab == CreativeCategories.SEARCH);
        refreshEntries();
    }

    private int lastRow() {
        return Math.max(0, (entries.size() + CreativeContainer.COLUMNS - 1)
                / CreativeContainer.COLUMNS - CreativeContainer.ROWS);
    }

    private void scrollTo(int next) {
        next = Math.max(0, Math.min(lastRow(), next));
        if (next != row) {
            row = next;
            container.fill(entries, row);
        }
    }

    private void scrollToPointer(int y) {
        float fraction = (y - SCROLL_Y - 7) / (float) SCROLL_TRAVEL;
        scrollTo(Math.round(fraction * lastRow()));
    }

    @Override
    public void handleMouseInput() {
        if (!CreativeInventoryHook.canUse(mc)) return;
        int x = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        if (dragging) {
            scrollToPointer(y);
            if (Mouse.getEventButton() == 0 && !Mouse.getEventButtonState()) dragging = false;
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && tab != CreativeCategories.INVENTORY && inside(x, y, 8, 17, 180, 92)) {
            scrollTo(row - Integer.signum(wheel));
            return;
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        if (!CreativeInventoryHook.canUse(mc)) return;
        int left = x - guiLeft;
        int top = y - guiTop;
        int hovered = tabAt(left, top);
        if (hovered >= 0) {
            if (button == 0) selectTab(hovered);
            return;
        }
        if (classicAt(left, top)) {
            if (button == 0 && mc.thePlayer.inventory.getItemStack() == null) hook.useClassic(mc);
            return;
        }
        if (tab != CreativeCategories.INVENTORY && inside(left, top, SCROLL_X, SCROLL_Y, 12, 112)) {
            dragging = button == 0 && lastRow() > 0;
            if (dragging) scrollToPointer(top);
            return;
        }
        if (tab == CreativeCategories.SEARCH) search.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (!CreativeInventoryHook.canUse(mc)) return;
        if (tab == CreativeCategories.SEARCH && key == Keyboard.KEY_TAB) {
            search.setFocused(!search.getIsFocused());
            return;
        }
        if (key != Keyboard.KEY_ESCAPE && tab == CreativeCategories.SEARCH && search.getIsFocused()
                && search.textboxKeyTyped(character, key)) {
            if (!query.equals(search.getText())) {
                query = search.getText();
                refreshEntries();
            }
            return;
        }
        int hotbar = key - Keyboard.KEY_1;
        if (hotbar >= 0 && hotbar < 9 && mc.thePlayer.inventory.getItemStack() == null) {
            int x = Mouse.getX() * width / mc.displayWidth - guiLeft;
            int y = height - Mouse.getY() * height / mc.displayHeight - 1 - guiTop;
            for (Object candidate : inventorySlots.inventorySlots) {
                Slot slot = (Slot) candidate;
                if (inside(x, y, slot.xDisplayPosition - 1, slot.yDisplayPosition - 1, 18, 18)
                        && !container.isDiscard(slot)) {
                    moveToHotbar(slot, hotbar);
                    return;
                }
            }
        }
        super.keyTyped(character, key);
    }

    private void moveToHotbar(Slot slot, int hotbar) {
        InventoryPlayer player = mc.thePlayer.inventory;
        ItemStack stack = slot.getStack();
        if (container.isCatalog(slot)) {
            if (stack == null) return;
            ItemStack copy = stack.copy();
            copy.stackSize = copy.getMaxStackSize();
            player.setInventorySlotContents(hotbar, copy);
        } else {
            ItemStack previous = player.getStackInSlot(hotbar);
            if (previous != null && (!slot.isItemValid(previous)
                    || previous.stackSize > slot.getSlotStackLimit())) return;
            slot.putStack(previous);
            player.setInventorySlotContents(hotbar, stack);
        }
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int button, boolean shift) {
        if (!CreativeInventoryHook.canUse(mc) || button < 0 || button > 1) return;
        InventoryPlayer player = mc.thePlayer.inventory;
        if (slot == null) {
            if (slotId == -999) container.slotClick(slotId, button, false, mc.thePlayer);
        } else if (container.isDiscard(slot)) {
            player.setItemStack(null);
        } else if (container.isCatalog(slot)) {
            ItemStack source = slot.getStack();
            ItemStack held = player.getItemStack();
            if (source == null || held != null && (!held.isItemEqual(source)
                    || !ItemStack.func_46154_a(held, source))) {
                player.setItemStack(null);
            } else if (held == null) {
                ItemStack copy = source.copy();
                if (shift) copy.stackSize = copy.getMaxStackSize();
                player.setItemStack(copy);
            } else if (button == 1) {
                if (--held.stackSize == 0) player.setItemStack(null);
            } else {
                held.stackSize = shift ? held.getMaxStackSize() : Math.min(held.stackSize + 1, held.getMaxStackSize());
            }
        } else {
            if (shift && tab == CreativeCategories.INVENTORY && slot.slotNumber >= 4) {
                mc.thePlayer.inventorySlots.slotClick(slot.slotNumber + 5, button, true, mc.thePlayer);
            } else {
                container.slotClick(slot.slotNumber, button, false, mc.thePlayer);
            }
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTick) {
        if (!Mouse.isButtonDown(0)) dragging = false;
        super.drawScreen(x, y, partialTick);
        GL11.glDisable(GL11.GL_LIGHTING);
        int hovered = tabAt(x - guiLeft, y - guiTop);
        if (hovered >= 0) {
            drawHint(catalog.tabs[hovered].title, x, y);
        } else if (classicAt(x - guiLeft, y - guiTop)) {
            drawHint(mc.thePlayer.inventory.getItemStack() == null
                    ? "Use classic inventory (" + hook.restoreKeyName() + " to restore)"
                    : "Put down the held item first", x, y);
        } else if (tab == CreativeCategories.INVENTORY && inside(x - guiLeft, y - guiTop, 173, 112, 16, 16)) {
            drawHint("Delete item held by cursor", x, y);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        if (tab != CreativeCategories.INVENTORY) {
            fontRenderer.drawString(catalog.tabs[tab].title, 8, 6, 0x404040);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        for (int i = 0; i < catalog.tabs.length; i++) {
            if (catalog.tabs[i] != null && i != tab) drawTab(i);
        }
        drawClassicTab();
        bind(tab == CreativeCategories.SEARCH ? CreativeInventoryTextures.SEARCH
                : tab == CreativeCategories.INVENTORY ? CreativeInventoryTextures.INVENTORY
                : CreativeInventoryTextures.LIST);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        if (tab == CreativeCategories.SEARCH) search.drawTextBox();
        if (tab == CreativeCategories.INVENTORY) {
            drawPlayer(mouseX, mouseY);
        } else {
            bind(CreativeInventoryTextures.TABS);
            int offset = lastRow() == 0 ? 0 : Math.round(SCROLL_TRAVEL * row / (float) lastRow());
            drawTexturedModalRect(guiLeft + SCROLL_X, guiTop + SCROLL_Y + offset,
                    lastRow() == 0 ? 244 : 232, 0, 12, 15);
        }
        drawTab(tab);
    }

    private void drawClassicTab() {
        int x = guiLeft + tabX(CreativeCategories.CLASSIC);
        int y = guiTop + tabY(CreativeCategories.CLASSIC);
        bind(CreativeInventoryTextures.TABS);
        drawTexturedModalRect(x, y, 4 * 28, 64, 28, 32);
        fontRenderer.drawString("1.2.5", x + 2, y + 10, 0x404040);
    }

    private void drawTab(int index) {
        int x = guiLeft + tabX(index);
        int y = guiTop + tabY(index);
        boolean lower = index >= 6;
        bind(CreativeInventoryTextures.TABS);
        drawTexturedModalRect(x, y, index % 6 * 28, (lower ? 64 : 0) + (index == tab ? 32 : 0), 28, 32);
        RenderHelper.enableGUIStandardItemLighting();
        itemRenderer.renderItemIntoGUI(fontRenderer, mc.renderEngine, catalog.tabs[index].icon, x + 6, y + (lower ? 7 : 9));
        RenderHelper.disableStandardItemLighting();
    }

    private void drawPlayer(int mouseX, int mouseY) {
        EntityPlayer player = mc.thePlayer;
        float yaw = player.rotationYaw;
        float head = player.rotationYawHead;
        float body = player.renderYawOffset;
        float pitch = player.rotationPitch;
        float view = RenderManager.instance.playerViewY;
        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        try {
            GL11.glTranslatef(guiLeft + 43, guiTop + 45, 50);
            GL11.glScalef(-20, 20, 20);
            GL11.glRotatef(180, 0, 0, 1);
            RenderHelper.enableGUIStandardItemLighting();
            float horizontal = (float) Math.atan((guiLeft + 43 - mouseX) / 40.0F);
            float vertical = (float) Math.atan((guiTop + 15 - mouseY) / 40.0F);
            GL11.glRotatef(-vertical * 20, 1, 0, 0);
            player.rotationYaw = horizontal * 40;
            player.rotationYawHead = player.rotationYaw;
            player.renderYawOffset = horizontal * 20;
            player.rotationPitch = -vertical * 20;
            GL11.glTranslatef(0, player.yOffset, 0);
            RenderManager.instance.playerViewY = 180;
            RenderManager.instance.renderEntityWithPosYaw(player, 0, 0, 0, 0, 1);
        } finally {
            player.rotationYaw = yaw;
            player.rotationYawHead = head;
            player.renderYawOffset = body;
            player.rotationPitch = pitch;
            RenderManager.instance.playerViewY = view;
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            GL11.glPopMatrix();
        }
    }

    private void bind(String path) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.renderEngine.bindTexture(mc.renderEngine.getTexture(path));
    }

    private void drawHint(String label, int mouseX, int mouseY) {
        int length = fontRenderer.getStringWidth(label);
        int x = Math.min(mouseX + 10, width - length - 5);
        int y = Math.max(3, mouseY - 12);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(x - 3, y - 3, x + length + 3, y + 11, 0xF0100010);
        fontRenderer.drawStringWithShadow(label, x, y, 0xFFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private int tabAt(int x, int y) {
        for (int i = 0; i < catalog.tabs.length; i++) {
            if (catalog.tabs[i] == null) continue;
            int top = i < 6 ? -28 : ySize;
            if (inside(x, y, tabX(i), top, 28, 28)) return i;
        }
        return -1;
    }

    private int tabX(int index) {
        int column = index % 6;
        return column == 5 ? xSize - 28 : 29 * column;
    }

    private int tabY(int index) { return index < 6 ? -28 : ySize - 4; }

    private boolean classicAt(int x, int y) { return inside(x, y, tabX(CreativeCategories.CLASSIC), ySize, 28, 28); }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
