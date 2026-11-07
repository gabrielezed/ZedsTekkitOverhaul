package zed.tekkitoverhaul.render.client;

import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Draws one triangulated OBJ part with explicit legacy lightmap brightness. */
public final class ObjTessellatorRenderer {
    private ObjTessellatorRenderer() { }

    public static void render(ObjModel.Part part, int packedBrightness) {
        if (part == null || part.vertices.isEmpty()) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLES);
        // Minecraft 1.2.5 applies a second texture unit for the lightmap.
        // GL_LIGHTING alone does not control this lightmap multiplier.
        tessellator.setBrightness(packedBrightness);
        for (ObjModel.Vertex vertex : part.vertices) {
            tessellator.setNormal(vertex.nx, vertex.ny, vertex.nz);
            tessellator.addVertexWithUV(vertex.x, vertex.y, vertex.z, vertex.u, vertex.v);
        }
        tessellator.draw();
    }
}
