package zed.tekkitoverhaul.crafting;

import net.minecraft.src.GuiContainer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.World;
import org.lwjgl.opengl.GL11;

/** UI client per Minecraft 1.2.5. Texture game-ready 256x256 in resources. */
public final class GuiAdvancedCraftingTable extends GuiContainer {
    private static final String TEXTURE = "/zed/gui/advanced_table.png";

    public GuiAdvancedCraftingTable(InventoryPlayer inventory, World world, int x, int y, int z) {
        super(new ContainerAdvancedCraftingTable(inventory, world, x, y, z));
        xSize = 212;
        ySize = 236;
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString("Advanced Crafting Table", 12, 7, 0x404040);
        fontRenderer.drawString("Inventory", 25, 128, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float ticks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.renderEngine.bindTexture(mc.renderEngine.getTexture(TEXTURE));
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
    }
}
