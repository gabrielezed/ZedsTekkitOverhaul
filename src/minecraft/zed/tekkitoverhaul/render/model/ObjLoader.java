package zed.tekkitoverhaul.render.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal Wavefront OBJ parser with no Minecraft or third-party dependencies. */
public final class ObjLoader {
    private static final int MAX_TRIANGLES = 150000;

    private ObjLoader() { }

    public static ObjModel read(InputStream stream) throws IOException {
        if (stream == null) throw new IllegalArgumentException("OBJ stream is null");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        List<float[]> positions = new ArrayList<float[]>();
        List<float[]> uvs = new ArrayList<float[]>();
        List<float[]> normals = new ArrayList<float[]>();
        Map<String, List<ObjModel.Vertex>> parts = new LinkedHashMap<String, List<ObjModel.Vertex>>();
        String group = "default";
        int lineNumber = 0;
        int triangles = 0;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                int comment = line.indexOf('#');
                if (comment >= 0) line = line.substring(0, comment);
                line = line.trim();
                if (line.length() == 0) continue;
                String[] words = line.split("\\s+");
                try {
                    if ("v".equals(words[0])) {
                        require(words, 4);
                        positions.add(new float[]{number(words[1]), number(words[2]), number(words[3])});
                    } else if ("vt".equals(words[0])) {
                        require(words, 3);
                        uvs.add(new float[]{number(words[1]), number(words[2])});
                    } else if ("vn".equals(words[0])) {
                        require(words, 4);
                        normals.add(new float[]{number(words[1]), number(words[2]), number(words[3])});
                    } else if ("g".equals(words[0]) || "o".equals(words[0])) {
                        require(words, 2);
                        group = words[1];
                    } else if ("f".equals(words[0])) {
                        if (words.length < 4 || words.length > 5) {
                            throw new IllegalArgumentException("Only triangle and quad faces are supported");
                        }
                        List<ObjModel.Vertex> output = parts.get(group);
                        if (output == null) {
                            output = new ArrayList<ObjModel.Vertex>();
                            parts.put(group, output);
                        }
                        int[][] refs = new int[words.length - 1][];
                        for (int i = 1; i < words.length; i++) {
                            refs[i - 1] = faceRef(words[i], positions.size(), uvs.size(), normals.size());
                        }
                        for (int i = 1; i < refs.length - 1; i++) {
                            if (++triangles > MAX_TRIANGLES) {
                                throw new IllegalArgumentException("OBJ triangle limit exceeded");
                            }
                            addTriangle(output, refs[0], refs[i], refs[i + 1], positions, uvs, normals);
                        }
                    }
                    // Other OBJ directives (materials, smoothing, etc.) are not supported yet.
                } catch (RuntimeException invalid) {
                    throw new IOException("Invalid OBJ at line " + lineNumber + ": " + invalid.getMessage(), invalid);
                }
            }
        } finally {
            reader.close();
        }
        if (triangles == 0) throw new IOException("OBJ contains no triangle or quad faces");
        return new ObjModel(parts);
    }

    private static void require(String[] words, int count) {
        if (words.length < count) throw new IllegalArgumentException("Missing OBJ components");
    }

    private static float number(String word) {
        float result = Float.parseFloat(word);
        if (Float.isNaN(result) || Float.isInfinite(result)) {
            throw new IllegalArgumentException("Non-finite OBJ coordinate");
        }
        return result;
    }

    private static int index(String word, int size) {
        int n = Integer.parseInt(word);
        int result = n < 0 ? size + n : n - 1;
        if (n == 0 || result < 0 || result >= size) {
            throw new IllegalArgumentException("OBJ index out of range: " + word);
        }
        return result;
    }

    private static int[] faceRef(String word, int positions, int uvs, int normals) {
        String[] parts = word.split("/", -1);
        if (parts.length > 3 || parts[0].length() == 0) {
            throw new IllegalArgumentException("Unsupported OBJ face reference: " + word);
        }
        int position = index(parts[0], positions);
        int uv = parts.length > 1 && parts[1].length() > 0 ? index(parts[1], uvs) : -1;
        int normal = parts.length > 2 && parts[2].length() > 0 ? index(parts[2], normals) : -1;
        return new int[]{position, uv, normal};
    }

    private static void addTriangle(List<ObjModel.Vertex> out, int[] a, int[] b, int[] c,
                                    List<float[]> positions, List<float[]> uvs, List<float[]> normals) {
        float[] pa = positions.get(a[0]);
        float[] pb = positions.get(b[0]);
        float[] pc = positions.get(c[0]);
        float ux = pb[0] - pa[0], uy = pb[1] - pa[1], uz = pb[2] - pa[2];
        float vx = pc[0] - pa[0], vy = pc[1] - pa[1], vz = pc[2] - pa[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        } else {
            ny = 1F;
        }
        append(out, a, positions, uvs, normals, nx, ny, nz);
        append(out, b, positions, uvs, normals, nx, ny, nz);
        append(out, c, positions, uvs, normals, nx, ny, nz);
    }

    private static void append(List<ObjModel.Vertex> out, int[] ref,
                               List<float[]> positions, List<float[]> uvs, List<float[]> normals,
                               float nx, float ny, float nz) {
        float[] p = positions.get(ref[0]);
        float[] uv = ref[1] >= 0 ? uvs.get(ref[1]) : null;
        float[] normal = ref[2] >= 0 ? normals.get(ref[2]) : null;
        out.add(new ObjModel.Vertex(p[0], p[1], p[2], uv == null ? 0F : uv[0],
                uv == null ? 0F : 1F - uv[1], normal == null ? nx : normal[0],
                normal == null ? ny : normal[1], normal == null ? nz : normal[2]));
    }
}
