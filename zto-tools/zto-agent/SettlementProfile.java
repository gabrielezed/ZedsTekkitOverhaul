import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import zed.tekkitoverhaul.agent.ZTOAgent;
import zed.tekkitoverhaul.settlement.WorldProvenance;

/** Headless measurements against the pinned production classes. Timings are descriptive. */
final class SettlementProfile {
    static void component(String label, xd world, WorldProvenance provenance, int x, int y, int z) throws Exception {
        Class<?> type = Class.forName("zed.tekkitoverhaul.settlement.SiteSurvey");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object survey = constructor.newInstance();
        Method start = method(type, "start", int.class, int.class, int.class);
        Method sample = method(type, "sample", xd.class, WorldProvenance.class);
        Method finish = method(type, "finish");
        Samples component = new Samples();
        for (int pass = -512; pass < 144; pass++) {
            start.invoke(survey, x, y, z);
            for (int layer = 0; layer < 9; layer++) {
                if (!Boolean.TRUE.equals(sample.invoke(survey, world, provenance)))
                    throw new AssertionError("component window must be loaded");
            }
            long time = System.nanoTime();
            finish.invoke(survey);
            if (pass >= 0) component.add(System.nanoTime() - time);
        }
        component.report(label + " component pass alone");
    }

    static void mutations(xd world, yw player, int x, int y, int z) {
        Samples placement = new Samples(), removal = new Samples(), quarry = new Samples();
        for (int i = -1000; i < 3600; i++) {
            world.g(x, y, z, 5);
            long start = System.nanoTime();
            ZTOAgent.observeMutation(true, world, player, x, y, z, 5, 0);
            if (i >= 0) placement.add(System.nanoTime() - start);
            world.g(x, y, z, 0);
            start = System.nanoTime();
            ZTOAgent.observeMutation(true, world, player, x, y, z, 5, 1);
            if (i >= 0) removal.add(System.nanoTime() - start);
            start = System.nanoTime();
            ZTOAgent.observeMutation(true, world, null, x, y, z, 1, 2);
            if (i >= 0) quarry.add(System.nanoTime() - start);
        }
        placement.report("callback placement");
        removal.report("callback manual removal");
        quarry.report("callback remote quarry removal");
    }

    static void provenance(Path root) throws Exception {
        Class<?> type = Class.forName("zed.tekkitoverhaul.settlement.ProvenanceStore");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Method record = method(type, "record", int.class, int.class, int.class, short.class);
        Method size = method(type, "size");
        Method write = method(type, "write", DataOutputStream.class);
        Method save = method(type, "save", Path.class);
        Method load = method(type, "load", Path.class);
        Path dir = Files.createDirectory(root.resolve("profile-provenance"));
        for (int density : new int[] {1, 512}) {
            Object store = constructor.newInstance();
            for (int sx = 0; sx < 8; sx++) {
                for (int sz = 0; sz < 8; sz++) {
                    for (int sy = 0; sy < 16; sy++) {
                        for (int offset = 0; offset < density; offset++) {
                            short id = (short) (1 + ((sx + sy + sz + offset) % 123));
                            record.invoke(store, sx * 8 + (offset >> 6), sy * 8 + (offset >> 3 & 7),
                                    sz * 8 + (offset & 7), id);
                        }
                    }
                }
            }
            if (!Integer.valueOf(1024).equals(size.invoke(store))) throw new AssertionError("exact section cap required");
            Path target = dir.resolve("provenance-" + density + ".dat");
            Path copy = dir.resolve("write-" + density + ".dat");
            Samples update = new Samples(), serialization = new Samples(), writeOnly = new Samples();
            Samples dirty = new Samples(), clean = new Samples(), reopen = new Samples();
            for (int pass = -4; pass < 24; pass++) {
                // Mutate an existing position in every section before each dirty save.
                for (int section = 0; section < 1024; section++) {
                    long time = System.nanoTime();
                    record.invoke(store, (section >> 7) * 8, (section & 15) * 8, (section >> 4 & 7) * 8,
                            (short) ((pass & 1) == 0 ? -1 : -2));
                    if (pass >= 0) update.add(System.nanoTime() - time);
                }

                ByteArrayOutputStream memory = new ByteArrayOutputStream();
                long time = System.nanoTime();
                try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(memory))) {
                    write.invoke(store, output);
                }
                if (pass >= 0) serialization.add(System.nanoTime() - time);
                byte[] encoded = memory.toByteArray();
                time = System.nanoTime();
                Path pending = Files.createTempFile(dir, "profile_", ".tmp");
                try {
                    Files.write(pending, encoded);
                    Files.move(pending, copy, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(pending);
                }
                if (pass >= 0) writeOnly.add(System.nanoTime() - time);

                time = System.nanoTime();
                save.invoke(store, target);
                if (pass >= 0) dirty.add(System.nanoTime() - time);
                if (Files.size(target) != encoded.length || !Arrays.equals(encoded, Files.readAllBytes(target)))
                    throw new AssertionError("profile serialization must equal the real dirty file");
                time = System.nanoTime();
                save.invoke(store, target);
                if (pass >= 0) clean.add(System.nanoTime() - time);
                time = System.nanoTime();
                Object loaded = load.invoke(null, target);
                if (pass >= 0) reopen.add(System.nanoTime() - time);
                if (!Integer.valueOf(1024).equals(size.invoke(loaded))) throw new AssertionError("store lost sections on reopen");
            }
            String label = "storage " + density + " tracked positions/section";
            System.out.printf("%s: 1024 sections / %d compressed bytes%n", label, Files.size(target));
            update.report(label + " record");
            serialization.report(label + " gzip to memory");
            writeOnly.report(label + " bytes + atomic replace");
            dirty.report(label + " dirty save");
            clean.report(label + " clean save");
            reopen.report(label + " reopen");
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    static final class Samples {
        private long[] values = new long[64];
        private int size;

        void add(long elapsed) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = elapsed;
        }

        void report(String label) {
            if (size == 0) return;
            Arrays.sort(values, 0, size);
            System.out.printf(java.util.Locale.ROOT, "PROFILE %s: n=%d median=%s p95=%s p99=%s max=%s us%n",
                    label, size, quantile(0.50), size >= 20 ? quantile(0.95) : "-",
                    size >= 100 ? quantile(0.99) : "-", quantile(1.00));
        }

        private String quantile(double percentile) {
            int index = (int) Math.ceil(size * percentile) - 1;
            return String.format(java.util.Locale.ROOT, "%.3f", values[index] / 1000.0);
        }
    }
}
