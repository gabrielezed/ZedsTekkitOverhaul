package zed.tekkitoverhaul.distillation.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.render.client.ObjTessellatorRenderer;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Renders one independently placed OBJ component at Minecraft 1.2.5 block scale. */
public final class RenderDistillationComponent extends TileEntitySpecialRenderer {
    private static final int BRIGHTNESS = 0x00F000F0; // Diagnostic fullbright; world-lighting pass is future work.
    private final EnumMap<DistillationPartType, ObjModel> models =
            new EnumMap<DistillationPartType, ObjModel>(DistillationPartType.class);
    private final Set<DistillationPartType> attempted = new HashSet<DistillationPartType>();

    private ObjModel modelFor(DistillationPartType type) {
        if (attempted.contains(type)) return models.get(type);
        attempted.add(type);
        String path = "/zed/models/distillation_" + type.modelName + ".obj";
        InputStream stream = RenderDistillationComponent.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("[ZTO] Distillation model asset missing: " + path);
            return null;
        }
        try {
            ObjModel model = ObjLoader.read(stream);
            models.put(type, model);
            return model;
        } catch (IOException exception) {
            System.err.println("[ZTO] Cannot parse distillation model " + path + ": " + exception);
            return null;
        }
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTick) {
        if (tile.worldObj == null) return;
        DistillationPartType type = DistillationRegistry.typeForBlockId(
                tile.worldObj.getBlockId(tile.xCoord, tile.yCoord, tile.zCoord));
        if (type == null) return;
        ObjModel model = modelFor(type);
        if (model == null) return;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT
                | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            bindTextureByName("/zed/textures/distillation_atlas.png");
            for (ObjModel.Part part : model.getParts().values()) {
                ObjTessellatorRenderer.render(part, BRIGHTNESS);
            }
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
