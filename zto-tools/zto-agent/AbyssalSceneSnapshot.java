import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.function.BiFunction;

/** Records a diver view of actual owned native sections for offline textured previews. */
final class AbyssalSceneSnapshot {
    private AbyssalSceneSnapshot() {}

    static int[] view(File file) throws Exception {
        try (DataInputStream data = new DataInputStream(Files.newInputStream(file.toPath()))) {
            for (int i = 0; i < 6; i++) data.readInt();
            int[] view = new int[6];
            for (int i = 0; i < view.length; i++) view[i] = (int) Math.floor(data.readDouble());
            return view;
        }
    }

    static void write(BiFunction<Integer, Integer, ack> provider, File file, int[] camera, int[] target) throws Exception {
        ack eye = provider.apply(camera[0] >> 4, camera[2] >> 4);
        for (int y = camera[1]; y <= Math.min(61, camera[1] + 12); y++) {
            int id = eye.a(camera[0] & 15, y, camera[2] & 15);
            if (id == 8 || id == 9 || id >= 3039 && id <= 3043) {
                camera[1] = y;
                break;
            }
        }
        int x0 = Math.min(camera[0], target[0]) - 19, x1 = Math.max(camera[0], target[0]) + 19;
        int z0 = Math.min(camera[2], target[2]) - 19, z1 = Math.max(camera[2], target[2]) + 19;
        int y0 = Math.max(1, Math.min(camera[1], target[1]) - 18), y1 = 62;
        int cx0 = x0 >> 4, cz0 = z0 >> 4;
        ack[][] chunks = new ack[(z1 >> 4) - cz0 + 1][(x1 >> 4) - cx0 + 1];
        for (int z = 0; z < chunks.length; z++) for (int x = 0; x < chunks[z].length; x++)
            chunks[z][x] = provider.apply(cx0 + x, cz0 + z);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            for (int n : new int[] {x0, y0, z0, x1-x0+1, y1-y0+1, z1-z0+1}) out.writeInt(n);
            for (int n : camera) out.writeDouble(n + 0.5);
            for (int n : target) out.writeDouble(n + 0.5);
            for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                ack chunk = chunks[(z >> 4) - cz0][(x >> 4) - cx0];
                int u = x & 15, v = z & 15;
                out.writeInt(chunk.a(u, y, v) | chunk.c(u, y, v) << 16 | chunk.a(wl.b, u, y, v) << 20);
            }
        }
    }
}
