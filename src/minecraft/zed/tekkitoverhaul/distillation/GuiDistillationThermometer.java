package zed.tekkitoverhaul.distillation;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import org.lwjgl.opengl.GL11;

/**
 * Singleplayer-only temperature control. Dedicated client/server networking is
 * required before this GUI is usable on multiplayer servers.
 */
public final class GuiDistillationThermometer extends GuiScreen {
    private final TileEntityDistillationBoiler boiler;

    public GuiDistillationThermometer(TileEntityDistillationBoiler boiler) {
        this.boiler = boiler;
    }

    @Override
    public void initGui() {
        controlList.clear();
        int cx = width / 2;
        int top = height / 2 - 66;
        for (DistillateType type : DistillateType.values()) {
            controlList.add(new GuiButton(type.id, cx - 93, top + type.id * 25,
                    186, 21, type.displayName + "  |  " + type.temperatureCelsius + " C"));
        }
        controlList.add(new GuiButton(99, cx - 93, top + 106, 186, 20, "Close"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 99) {
            mc.displayGuiScreen(null);
            return;
        }
        DistillateType selected = DistillateType.fromId(button.id);
        if (selected != null) boiler.select(selected.id);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int top = height / 2 - 96;
        drawCenteredString(fontRenderer, "Fractional Distillation Thermometer", cx, top, 0xFFFFFF);
        drawCenteredString(fontRenderer, "Selected: " +
                boiler.getProcess().selectedType().displayName, cx, top + 13, 0xD7CF9A);
        super.drawScreen(mouseX, mouseY, partialTicks);
        int y = height / 2 + 75;
        drawCenteredString(fontRenderer, "Crude " + boiler.getProcess().getCrudeMB()
                + " mB  |  Water " + boiler.getProcess().getCoolantMB() + " mB", cx, y, 0xFFFFFF);
        drawCenteredString(fontRenderer, "Output " + boiler.getProcess().getDistillateMB() +
                " mB  |  Progress " + boiler.getProcess().getProgressTicks() + " ticks", cx, y + 12, 0xFFFFFF);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
