import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Validates the distillation models and their atlas against the ZTO OBJ parser. */
public final class DistillationModelsSelfTest {
    private static final String[] PARTS = {
            "boiler", "heater", "column", "condenser", "receiver", "thermometer"
    };
    // OBJ row zero is the bottom PNG row after ObjLoader flips V.
    private static final List<String> MATERIALS = Arrays.asList(
            "steel", "darksteel", "copper", "copperBright", "glass", "insulation", "brass", "heat",
            "black", "water", "oil", "glassHighlight", "red", "green", "ceramic", "wornCopper");
    private static int checks;

    private static void check(boolean good, String reason) {
        ++checks;
        if (!good) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        BufferedImage atlas = ImageIO.read(new File("src/minecraft/zed/textures/distillation_atlas.png"));
        check(atlas != null && atlas.getWidth() == 64 && atlas.getHeight() == 32,
                "Expected a 64 by 32 distillation atlas");
        for (int y = 0; y < atlas.getHeight(); y++) {
            for (int x = 0; x < atlas.getWidth(); x++) {
                check(atlas.getRGB(x, y) >>> 24 == 255, "Non-opaque texel at " + x + ", " + y);
            }
        }
        for (String part : PARTS) {
            String file = "src/minecraft/zed/models/distillation_" + part + ".obj";
            ObjModel model = ObjLoader.read(new FileInputStream(file));
            check(!model.getParts().isEmpty(), "No groups in " + part);
            int faces = 0;
            for (ObjModel.Part group : model.getParts().values()) {
                check(group.vertices.size() % 3 == 0, "Invalid triangles in " + part);
                int slot = MATERIALS.indexOf(group.name);
                check(slot >= 0, "Unknown material: " + group.name);
                checkTriangles(atlas, group, slot);
                faces += group.vertices.size() / 3;
            }
            check(faces >= 20, "Model unexpectedly empty: " + part);
            System.out.println("[ZTO] " + part + " OBJ: " + faces + " triangles");
        }
        ObjModel connectors = ObjLoader.read(new FileInputStream(
                "src/minecraft/zed/models/distillation_connections.obj"));
        String[] requiredGroups = {
                "boiler", "heater", "column_lower", "column_upper",
                "condenser", "receiver", "thermometer"
        };
        for (String group : requiredGroups) {
            ObjModel.Part part = connectors.getPart(group);
            check(part != null && part.vertices.size() >= 6,
                    "Missing or empty connector group: " + group);
            checkTriangles(atlas, part, -1);
        }
        System.out.println("[ZTO] Connector groups: " + requiredGroups.length);
        System.out.println("PASS: " + checks + " OBJ model assertions");
    }

    private static void checkTriangles(BufferedImage atlas, ObjModel.Part part, int expectedSlot) {
        for (int i = 0; i < part.vertices.size(); i += 3) {
            int slot = atlasSlot(atlas, part.vertices.get(i));
            check(slot == atlasSlot(atlas, part.vertices.get(i + 1))
                            && slot == atlasSlot(atlas, part.vertices.get(i + 2)),
                    "Triangle crosses material slots: " + part.name);
            check(expectedSlot < 0 || slot == expectedSlot, "Wrong atlas slot: " + part.name);
        }
    }

    private static int atlasSlot(BufferedImage atlas, ObjModel.Vertex vertex) {
        check(vertex.u >= 0F && vertex.u < 1F && vertex.v >= 0.5F && vertex.v < 1F,
                "UV outside the populated bottom half of the atlas");
        int x = (int) (vertex.u * atlas.getWidth());
        int y = (int) (vertex.v * atlas.getHeight());
        check((atlas.getRGB(x, y) & 0xFFFFFF) != 0, "UV samples a black texel at " + x + ", " + y);
        return (3 - y / 8) * 8 + x / 8;
    }
}
