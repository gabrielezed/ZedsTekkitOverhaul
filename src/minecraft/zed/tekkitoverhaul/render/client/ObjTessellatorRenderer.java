package zed.tekkitoverhaul.render.client;

import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Renders one triangulated OBJ part with the Minecraft 1.2.5 Tessellator. */
public final class ObjTessellatorRenderer {
    private ObjTessellatorRenderer() { }

    public static void render(ObjModel.Part part) {
        if (part == null || part.vertices.isEmpty()) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLES);
        for (ObjModel.Vertex vertex : part.vertices) {
            tessellator.setNormal(vertex.nx, vertex.ny, vertex.nz);
            tessellator.addVertexWithUV(vertex.x, vertex.y, vertex.z, vertex.u, vertex.v);
        }
        tessellator.draw();
    }
}
