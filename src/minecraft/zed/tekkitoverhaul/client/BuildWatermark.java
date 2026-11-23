package zed.tekkitoverhaul.client;

import java.util.EnumSet;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.ScaledResolution;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.BuildIdentity;

/** Draws one label after the native title screen or HUD has finished rendering. */
public final class BuildWatermark implements ITickHandler {
    private final Minecraft game;
    private final String label;
    private int brandingLines = -1;

    public BuildWatermark(Minecraft game) {
        this.game = game;
        label = BuildIdentity.label();
    }

    public void tickStart(EnumSet<TickType> ticks, Object... data) { }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (!ticks.contains(TickType.RENDER)) return;
        boolean title = game.currentScreen instanceof GuiMainMenu;
        if (!title && (game.currentScreen != null || game.theWorld == null || game.thePlayer == null
                || game.gameSettings.hideGUI || game.gameSettings.showDebugInfo)) return;
        FontRenderer font = game.fontRenderer;
        ScaledResolution scale = new ScaledResolution(game.gameSettings, game.displayWidth, game.displayHeight);
        int width = scale.getScaledWidth();
        String text = font.trimStringToWidth(label, width - 6);
        int y = 3;
        if (title) {
            if (brandingLines < 0) {
                brandingLines = FMLCommonHandler.instance().getBrandingStrings("Minecraft 1.2.5").length;
            }
            y = titleY(width, scale.getScaledHeight(), brandingLines, font.FONT_HEIGHT, font.getStringWidth(text));
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            font.drawStringWithShadow(text, 3, y, 0xDDDDDD);
        } finally {
            GL11.glPopAttrib();
        }
    }

    static int titleY(int width, int height, int lines, int fontHeight, int textWidth) {
        // FML draws its stack upward from height - 10. The last row has no room below.
        int y = height - 10 - lines * (fontHeight + 1);
        // The pinned title screen's last row includes the 24-pixel language button.
        boolean overlapsButtons = y + fontHeight > height / 4 + 48 && y < height / 4 + 152
                && 3 + textWidth >= width / 2 - 124;
        return y < 3 || overlapsButtons ? 3 : y;
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }

    public String getLabel() { return "ZTO Build Watermark"; }
}
