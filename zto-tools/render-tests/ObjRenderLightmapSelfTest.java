import java.io.ByteArrayInputStream;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.render.client.ObjTessellatorRenderer;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Verifies that brightness is configured before vertices for every OBJ part. */
public final class ObjRenderLightmapSelfTest {
    private static int assertions;

    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        String raw = "v 0 0 0\nv 1 0 0\nv 0 0 1\n"
                + "g base\nf 1 2 3\ng rotor\nf 1 3 2\n";
        ObjModel model = ObjLoader.read(new ByteArrayInputStream(raw.getBytes("UTF-8")));
        Tessellator tessellator = Tessellator.instance;

        ObjTessellatorRenderer.render(model.getPart("base"), 0x00F000F0);
        check(tessellator.mode == GL11.GL_TRIANGLES, "Expected triangle mode");
        check(tessellator.brightness == 0x00F000F0, "Expected full-bright lightmap");
        check(tessellator.vertexCount == 3, "Expected three base vertices");
        check(tessellator.drawings == 1, "Expected one completed base draw");

        ObjTessellatorRenderer.render(model.getPart("rotor"), 0x0000A000);
        check(tessellator.brightness == 0x0000A000, "Expected per-part lightmap");
        check(tessellator.vertexCount == 3, "Expected three rotor vertices");
        check(tessellator.drawings == 2, "Expected two completed draw calls");

        ObjTessellatorRenderer.render(null, 0x00F000F0);
        check(tessellator.drawings == 2, "Null part must not trigger a draw");
        System.out.println("PASS: " + assertions + " legacy lightmap renderer assertions");
    }
}
