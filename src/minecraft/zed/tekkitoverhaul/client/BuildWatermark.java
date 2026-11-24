package zed.tekkitoverhaul.client;

import java.util.EnumSet;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.src.FontRenderer;

import net.minecraft.src.ScaledResolution;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.BuildIdentity;

/** Draws one label after the native HUD has finished rendering. */
public final class BuildWatermark implements ITickHandler {
    private final Minecraft game;
    private final String label;

    public BuildWatermark(Minecraft game) {
        this.game = game;
        label = BuildIdentity.label();
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            agent.getMethod("registerTitleBuild", String.class).invoke(null, label);
        } catch (ReflectiveOperationException unavailable) {
            // The standalone client retains its gameplay watermark.
        }
    }

    public void tickStart(EnumSet<TickType> ticks, Object... data) { }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (!ticks.contains(TickType.RENDER)) return;
        if (game.currentScreen != null || game.theWorld == null || game.thePlayer == null
                || game.gameSettings.hideGUI || game.gameSettings.showDebugInfo) return;
        FontRenderer font = game.fontRenderer;
        ScaledResolution scale = new ScaledResolution(game.gameSettings, game.displayWidth, game.displayHeight);
        int width = scale.getScaledWidth();
        String text = font.trimStringToWidth(label, width - 6);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT);
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
            font.drawStringWithShadow(text, 3, 3, 0xFFFFFF);
        } finally {
            GL11.glPopAttrib();
        }
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }

    public String getLabel() { return "ZTO Build Watermark"; }
}
