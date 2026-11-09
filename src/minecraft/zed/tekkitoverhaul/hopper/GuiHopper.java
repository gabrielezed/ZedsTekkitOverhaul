package zed.tekkitoverhaul.hopper;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

public final class GuiHopper extends GuiContainer {
    private final TileHopper hopper;
    public GuiHopper(InventoryPlayer player, TileHopper tile) {
        super(new ContainerHopper(player,tile)); hopper=tile; xSize=176; ySize=133;
    }
    @Override
    public void initGui() {
        super.initGui();
        controlList.add(new GuiButton(90, guiLeft + 89, guiTop + 36, 79, 13, hopper.outputModeName()));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 90 && !mc.theWorld.isRemote && hopper.isUseableByPlayer(mc.thePlayer)) {
            hopper.cycleOutputMode();
            button.displayString = hopper.outputModeName();
        }
    }

    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString("Hopper",8,6,4210752);
        fontRenderer.drawString(hopper.isLocked()?"Locked":"Enabled",122,6,4210752);
        fontRenderer.drawString("Inventory",8,39,4210752);
    }
    public void drawScreen(int mouseX,int mouseY,float partialTick) {
        super.drawScreen(mouseX,mouseY,partialTick);
        int left=(width-xSize)/2, top=(height-ySize)/2;
        if(mouseX<left+8 || mouseX>=left+168 || mouseY<top+4 || mouseY>=top+17) return;
        String[] directions={"Down","Up","North","South","West","East"};
        String[] lines={"5 slots; up to 64 items per slot", "Transfer: 1 item / 8 ticks (2.5/s at 20 TPS)",
            "Output: "+directions[hopper.facing()],
            hopper.isLocked()?"Remove redstone power to enable transfers.":"Pulls from above; output needs space and a valid slot."};
        int w=0; for(int i=0;i<lines.length;i++) w=Math.max(w,fontRenderer.getStringWidth(lines[i]));
        int tx=Math.max(4,Math.min(width-w-8,mouseX+12));
        int ty=Math.max(4,Math.min(height-48,mouseY+12));
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(tx-4,ty-4,tx+w+4,ty+44,0xF0100010);
        for(int i=0;i<lines.length;i++) fontRenderer.drawStringWithShadow(lines[i],tx,ty+i*10,0xFFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
    protected void drawGuiContainerBackgroundLayer(float partialTick,int mouseX,int mouseY) {
        GL11.glColor4f(1,1,1,1);
        mc.renderEngine.bindTexture(mc.renderEngine.getTexture("/zed/hopper-1.5.2/gui.png"));
        drawTexturedModalRect((width-xSize)/2,(height-ySize)/2,0,0,xSize,ySize);
    }
}
