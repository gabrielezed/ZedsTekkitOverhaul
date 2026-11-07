package zed.tekkitoverhaul.render.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable, triangulated OBJ mesh with named parts for rigid animation. */
public final class ObjModel {
    private final Map<String, Part> parts;

    ObjModel(Map<String, List<Vertex>> faces) {
        Map<String, Part> built = new LinkedHashMap<String, Part>();
        for (Map.Entry<String, List<Vertex>> entry : faces.entrySet()) {
            built.put(entry.getKey(), new Part(entry.getKey(), entry.getValue()));
        }
        parts = Collections.unmodifiableMap(built);
    }

    public Part getPart(String name) {
        return parts.get(name);
    }

    public Map<String, Part> getParts() {
        return parts;
    }

    public static final class Part {
        public final String name;
        public final List<Vertex> vertices;

        private Part(String name, List<Vertex> vertices) {
            this.name = name;
            this.vertices = Collections.unmodifiableList(new ArrayList<Vertex>(vertices));
        }
    }

    public static final class Vertex {
        public final float x, y, z;
        public final float u, v;
        public final float nx, ny, nz;

        Vertex(float x, float y, float z, float u, float v,
               float nx, float ny, float nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }
}
