package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.GuiContainer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import org.lwjgl.opengl.GL11;

final class GuiAirCompressor extends GuiContainer {
    private static final int TEXT_COLOR = 0x30363A;
    private final TileAirCompressor machine;

    GuiAirCompressor(InventoryPlayer player, TileAirCompressor machine) {
        super(new ContainerAirCompressor(player, machine));
        this.machine = machine;
        ySize = 169;
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        ItemStack tank = machine.getStackInSlot(0);
        int charge = OxygenCharge.get(tank);
        fontRenderer.drawString(machine.getInvName(), 8, 6, TEXT_COLOR);
        fontRenderer.drawString("Oxygen " + charge + "/" + UnderwaterBalance.OXYGEN_CAPACITY + " ticks", 42, 21, TEXT_COLOR);
        fontRenderer.drawString("Buffer " + machine.storedEU() + "/" + UnderwaterBalance.EU_CAPACITY + " EU", 42, 44, TEXT_COLOR);
        String status = tank == null ? "Insert an Oxygen Tank" : charge == UnderwaterBalance.OXYGEN_CAPACITY
                ? "Tank full" : machine.canCharge() ? "Charging" : "Connect an LV EU supply";
        fontRenderer.drawString(status, 8, 59, TEXT_COLOR);
        fontRenderer.drawString("Inventory", 8, 76, TEXT_COLOR);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.renderEngine.bindTexture(mc.renderEngine.getTexture("/zed/gui/air_compressor.png"));
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        int charge = OxygenCharge.get(machine.getStackInSlot(0));
        int width = 120 * charge / UnderwaterBalance.OXYGEN_CAPACITY;
        if (width > 0) drawTexturedModalRect(guiLeft + 42, guiTop + 33, 0, 176, width, 6);
    }
}
