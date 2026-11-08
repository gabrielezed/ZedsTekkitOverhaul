package zed.tekkitoverhaul.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.TileEntity;

/** Shared live status panel for local machine controls. */
public abstract class GuiMachinePanel extends GuiScreen {
    protected static final int PANEL_WIDTH = 246;
    private static final int PANEL_HEIGHT = 206;
    private static final int CLOSE = 99;
    private final String title;
    protected final TileEntity tile;
    protected int left;
    protected int top;

    protected GuiMachinePanel(String title, TileEntity tile) {
        this.title = title;
        this.tile = tile;
    }

    @Override
    public void initGui() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        controlList.clear();
        controlList.add(new GuiButton(CLOSE, left + 165, top + 177, 67, 20, "Close"));
    }

    @Override
    public void updateScreen() {
        if (tile.isInvalid() || mc.theWorld == null || mc.theWorld != tile.worldObj || mc.thePlayer == null
                || mc.thePlayer.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) > 64
                || mc.theWorld.getBlockTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == CLOSE) mc.displayGuiScreen(null);
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF556570);
        drawRect(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF202B32);
        drawRect(left, top, left + PANEL_WIDTH, top + 29, 0xFF344653);
        drawCenteredString(fontRenderer, title, width / 2, top + 10, 0xFFF4F7FA);
        drawPanel();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected abstract void drawPanel();

    protected final void status(int y, String text, int color) {
        fontRenderer.drawSplitString(text, left + 14, top + y, PANEL_WIDTH - 28, color);
    }

    protected final void meter(int y, String title, int amount, int max, String unit, int color) {
        meter(y, title, amount, max, color, amount + " / " + max + " " + unit);
    }

    protected final void meter(int y, String title, int amount, int max, int color, String value) {
        int start = left + 14;
        int end = left + PANEL_WIDTH - 14;
        int barY = top + y + 12;
        drawString(fontRenderer, title, start, top + y, 0xFFF4F7FA);
        drawString(fontRenderer, value, end - fontRenderer.getStringWidth(value), top + y, 0xFFD8E4ED);
        drawRect(start, barY, end, barY + 5, 0xFF12191D);
        int fill = Math.min(max, Math.max(0, amount)) * (end - start) / max;
        if (fill > 0) drawRect(start, barY, start + fill, barY + 5, color);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
