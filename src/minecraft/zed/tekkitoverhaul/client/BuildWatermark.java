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
        int x = title ? 2 : 3;
        int y = 3;
        float textScale = 1F;
        if (title) {
            if (brandingLines < 0) {
                brandingLines = FMLCommonHandler.instance().getBrandingStrings("Minecraft 1.2.5").length;
            }
            int height = scale.getScaledHeight();
            y = titleY(height, brandingLines, font.FONT_HEIGHT);
            textScale = titleScale(width, height, y, font.FONT_HEIGHT, font.getStringWidth(text));
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT | GL11.GL_TRANSFORM_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glTranslatef(x, y, 0F);
            GL11.glScalef(textScale, textScale, 1F);
            font.drawStringWithShadow(text, 0, 0, 0xFFFFFF);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    static int titleY(int height, int lines, int fontHeight) {
        // FML draws its stack upward from height - 10.
        return Math.max(3, height - 10 - lines * (fontHeight + 1));
    }

    static float titleScale(int width, int height, int y, int fontHeight, int textWidth) {
        if (y + fontHeight <= height / 4 + 48 || y >= height / 4 + 152) return 1F;
        // Leave two pixels of padding on both sides of the left gutter.
        int gutter = Math.max(1, width / 2 - 124 - 4);
        return Math.min(1F, (float) gutter / (textWidth + 1));
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }

    public String getLabel() { return "ZTO Build Watermark"; }
}
