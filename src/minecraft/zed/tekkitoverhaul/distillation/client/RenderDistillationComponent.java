package zed.tekkitoverhaul.distillation.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.src.OpenGlHelper;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import zed.tekkitoverhaul.distillation.DistillationLayout;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;
import zed.tekkitoverhaul.distillation.TileEntityDistillationPart;
import zed.tekkitoverhaul.render.client.ObjTessellatorRenderer;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/**
 * Minecraft 1.2.5 OBJ renderer with an IC2/BuildCraft-compatible, opaque
 * pixel-art atlas and interlocking component connections.
 */
public final class RenderDistillationComponent extends TileEntitySpecialRenderer {
    private final EnumMap<DistillationPartType, ObjModel> models =
            new EnumMap<DistillationPartType, ObjModel>(DistillationPartType.class);
    private final Set<DistillationPartType> attempted =
            new HashSet<DistillationPartType>();
    private ObjModel connections;
    private boolean attemptedConnections;

    private ObjModel readModel(String path) {
        InputStream stream = RenderDistillationComponent.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("[ZTO] Distillation OBJ not found: " + path);
            return null;
        }
        try {
            return ObjLoader.read(stream);
        } catch (IOException exception) {
            System.err.println("[ZTO] Invalid distillation OBJ " + path + ": " + exception);
            return null;
        }
    }

    private ObjModel modelFor(DistillationPartType type) {
        if (attempted.contains(type)) return models.get(type);
        attempted.add(type);
        ObjModel model = readModel("/zed/models/distillation_" + type.modelName + ".obj");
        if (model != null) models.put(type, model);
        return model;
    }

    private ObjModel connections() {
        if (!attemptedConnections) {
            attemptedConnections = true;
            connections = readModel("/zed/models/distillation_connections.obj");
        }
        return connections;
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTick) {
        if (tile.worldObj == null) return;
        DistillationPartType type = DistillationRegistry.typeForBlockId(
                tile.worldObj.getBlockId(tile.xCoord, tile.yCoord, tile.zCoord));
        if (type == null) return;
        ObjModel mesh = modelFor(type);
        if (mesh == null) return;

        DistillationLayout.Facing facing = DistillationLayout.Facing.EAST;
        boolean upperColumn = false;
        if (tile instanceof TileEntityDistillationBoiler) {
            facing = ((TileEntityDistillationBoiler)tile).getVisualFacing();
        } else if (tile instanceof TileEntityDistillationPart) {
            TileEntityDistillationPart part = (TileEntityDistillationPart)tile;
            facing = part.getVisualFacing();
            upperColumn = type == DistillationPartType.COLUMN && part.isUpperColumn();
        }

        int brightness = tile.worldObj.getLightBrightnessForSkyBlocks(
                tile.xCoord, tile.yCoord, tile.zCoord, 0);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT |
                GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT |
                GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
            GL11.glRotatef(facing.rotationDegrees, 0F, 1F, 0F);
            // All atlas materials are opaque; translucent blending was causing
            // the preview's ghost-like overlay and incorrect saturation.
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(true);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            bindTextureByName("/zed/textures/distillation_atlas.png");

            for (ObjModel.Part part : mesh.getParts().values()) {
                ObjTessellatorRenderer.render(part, brightness);
            }
            ObjModel attachments = connections();
            if (attachments != null) {
                String groupName = type == DistillationPartType.COLUMN ?
                        (upperColumn ? "column_upper" : "column_lower") : type.modelName;
                ObjTessellatorRenderer.render(attachments.getPart(groupName), brightness);
            }
        } finally {
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
