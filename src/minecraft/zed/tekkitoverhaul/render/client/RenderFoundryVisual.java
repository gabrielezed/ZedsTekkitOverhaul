package zed.tekkitoverhaul.render.client;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import zed.tekkitoverhaul.render.TileEntityFoundryVisual;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Draws the OBJ rotor above the vanilla-textured Foundry block. */
public final class RenderFoundryVisual extends TileEntitySpecialRenderer {
    // Diagnostic value: both lightmap channels at maximum (240, 240).
    // Replace with world-derived brightness when finalizing machine lighting.
    private static final int PREVIEW_LIGHTMAP = 0x00F000F0;

    private ObjModel model;
    private boolean attemptedLoad;

    private void loadModel() {
        if (attemptedLoad) return;
        attemptedLoad = true;
        InputStream stream = RenderFoundryVisual.class.getResourceAsStream("/zed/models/foundry_rotor.obj");
        if (stream == null) {
            System.err.println("[ZTO] Renderer prototype OBJ asset is missing");
            return;
        }
        try {
            model = ObjLoader.read(stream);
        } catch (IOException error) {
            System.err.println("[ZTO] Cannot parse Foundry OBJ prototype: " + error);
        }
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityFoundryVisual)) return;
        loadModel();
        if (model == null) return;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT);
        try {
            GL11.glTranslatef((float)x + 0.5F, (float)y + 1.015F, (float)z + 0.5F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            // This unlit full-bright preview isolates texture/UV correctness from world lighting.
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            bindTextureByName("/zed/textures/foundry_rotor.png");
            ObjTessellatorRenderer.render(model.getPart("base"), PREVIEW_LIGHTMAP);

            float angle = ((TileEntityFoundryVisual)tile).getRotorAngle(partialTicks);
            GL11.glRotatef(angle, 0F, 1F, 0F);
            ObjTessellatorRenderer.render(model.getPart("rotor"), PREVIEW_LIGHTMAP);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
