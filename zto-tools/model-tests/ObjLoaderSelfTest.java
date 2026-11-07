import java.io.ByteArrayInputStream;
import java.io.IOException;
import zed.tekkitoverhaul.render.model.ObjLoader;
import zed.tekkitoverhaul.render.model.ObjModel;

/** No dependencies beyond the JDK; run against the exact parser sources. */
public final class ObjLoaderSelfTest {
    private static int checks;

    private static ObjModel parse(String input) throws Exception {
        return ObjLoader.read(new ByteArrayInputStream(input.getBytes("UTF-8")));
    }

    private static void expect(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejected(String input) throws Exception {
        boolean rejected = false;
        try {
            parse(input);
        } catch (IOException expected) {
            rejected = true;
        }
        expect(rejected, "Invalid OBJ unexpectedly accepted");
    }

    public static void main(String[] args) throws Exception {
        String triangle = "# triangle\n" +
                "v 0 0 0\nv 1 0 0\nv 0 0 1\n" +
                "vt 0 0\nvt 1 0\nvt 0 1\n" +
                "vn 0 1 0\ng rotor\n" +
                "f 1/1/1 2/2/1 3/3/1 # comment\n";
        ObjModel m = parse(triangle);
        expect(m.getPart("rotor") != null, "Group not found");
        expect(m.getPart("rotor").vertices.size() == 3, "Triangle count incorrect");
        expect(m.getPart("rotor").vertices.get(2).v == 0F, "OBJ V coordinate not flipped");
        expect(m.getPart("rotor").vertices.get(0).ny == 1F, "Explicit normal lost");
        String quad = "v 0 0 0\nv 1 0 0\nv 1 0 1\nv 0 0 1\n" +
                "o base\nf -4 -3 -2 -1\n";
        ObjModel q = parse(quad);
        expect(q.getPart("base").vertices.size() == 6, "Quad was not triangulated");
        expect(q.getPart("base").vertices.get(0).y == 0F, "Position corrupted");
        expect(q.getPart("base").vertices.get(0).ny != 0F, "Fallback normal is missing");
        rejected("v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 9\n");
        rejected("v NaN 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n");
        rejected("v 0 0 0\nv 1 0 0\nv 0 1 0\nf 0 2 3\n");
        rejected("v 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 1\nv 1 1 1\nf 1 2 3 4 5\n");
        rejected("# empty model\n");
        expect(m.getParts().size() == 1, "Unexpected model parts");
        System.out.println("PASS: " + checks + " OBJ loader checks");
    }
}
