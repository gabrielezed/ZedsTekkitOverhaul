import java.io.FileInputStream;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/** Validates the six real OBJ resources against the exact ZTO OBJ parser. */
public final class DistillationModelsSelfTest {
    private static final String[] PARTS = {
            "boiler", "heater", "column", "condenser", "receiver", "thermometer"
    };
    private static int checks;

    private static void check(boolean good, String reason) {
        ++checks;
        if (!good) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        for (String part : PARTS) {
            String file = "src/minecraft/zed/models/distillation_" + part + ".obj";
            ObjModel model = ObjLoader.read(new FileInputStream(file));
            check(!model.getParts().isEmpty(), "No groups in " + part);
            int faces = 0;
            for (ObjModel.Part group : model.getParts().values()) {
                check(group.vertices.size() % 3 == 0, "Invalid triangles in " + part);
                for (ObjModel.Vertex vertex : group.vertices) {
                    check(!Float.isNaN(vertex.u) && !Float.isNaN(vertex.v),
                            "Invalid texture coordinate in " + part);
                }
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
            for (ObjModel.Vertex vertex : part.vertices) {
                check(vertex.u >= 0F && vertex.u <= 1F &&
                      vertex.v >= 0F && vertex.v <= 1F,
                      "Connector UV out of range: " + group);
            }
        }
        System.out.println("[ZTO] Connector groups: " + requiredGroups.length);
        System.out.println("PASS: " + checks + " OBJ model assertions");
    }
}
