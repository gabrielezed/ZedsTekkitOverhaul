package zed.tekkitoverhaul.exploration.motorboat.client;

import net.minecraft.src.GuiContainer;
import net.minecraft.src.InventoryPlayer;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.exploration.motorboat.ContainerMotorboat;
import zed.tekkitoverhaul.exploration.motorboat.EntityMotorboat;

final class GuiMotorboat extends GuiContainer {
    private static final int TEXT_COLOR = 0x404040;
    private final EntityMotorboat boat;
    private final ContainerMotorboat container;

    GuiMotorboat(InventoryPlayer inventory, EntityMotorboat boat) {
        this(new ContainerMotorboat(inventory, boat), boat);
    }

    private GuiMotorboat(ContainerMotorboat container, EntityMotorboat boat) {
        super(container);
        this.container = container;
        this.boat = boat;
        ySize = 169;
    }

    @Override
    public void updateScreen() {
        if (mc.thePlayer != null && !container.canInteractWith(mc.thePlayer)) mc.thePlayer.closeScreen();
        else super.updateScreen();
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        String title = "Motorboat";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, TEXT_COLOR);
        fontRenderer.drawString("Gasoline", 9, 17, TEXT_COLOR);
        fontRenderer.drawString("Empty", 52, 17, TEXT_COLOR);
        fontRenderer.drawString(boat.fuel() + "/" + EntityMotorboat.CAPACITY + " mB", 85, 23, TEXT_COLOR);
        fontRenderer.drawString(container.engineStatus(), 8, 59, TEXT_COLOR);
        fontRenderer.drawString("Inventory", 8, 76, TEXT_COLOR);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.renderEngine.bindTexture(mc.renderEngine.getTexture("/zed/gui/motorboat.png"));
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        int fill = 80 * boat.fuel() / EntityMotorboat.CAPACITY;
        if (fill > 0) drawTexturedModalRect(guiLeft + 86, guiTop + 35, 0, 176, fill, 6);
    }
}
