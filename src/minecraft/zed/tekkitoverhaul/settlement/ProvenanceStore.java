package zed.tekkitoverhaul.settlement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** One latest observation per position, grouped into 8-block cubes. Write-recency eviction is O(1). */
final class ProvenanceStore {
    static final int CAPACITY = 1024, VOLUME = 512, LIMIT = 30000000;
    static final short UNKNOWN = 0, MANUAL_REMOVAL = -1, QUARRY_REMOVAL = -2;
    private static final int VERSION = 1, MAX_FILE = 3 * 1024 * 1024;
    private static final long AXIS_MASK = (1L << 23) - 1;
    private final LinkedHashMap<Long, Section> sections = new LinkedHashMap<Long, Section>();
    private boolean dirty;
    String status = "READY";
    boolean evicted;

    static boolean valid(int x, int y, int z) {
        return x >= -LIMIT && x < LIMIT && z >= -LIMIT && z < LIMIT && y >= 0 && y < 256;
    }

    static long key(int x, int y, int z) {
        return ((long) (x >> 3) & AXIS_MASK) << 28 | ((long) (z >> 3) & AXIS_MASK) << 5 | (y >> 3);
    }

    private static int axis(long bits) { return ((int) (bits & AXIS_MASK) << 9) >> 9; }
    static int sectionX(long key) { return axis(key >>> 28) << 3; }
    static int sectionY(long key) { return ((int) key & 31) << 3; }
    static int sectionZ(long key) { return axis(key >>> 5) << 3; }
    static int offset(int x, int y, int z) { return (x & 7) << 6 | (y & 7) << 3 | (z & 7); }

    Section section(long key) { return sections.get(key); }
    int size() { return sections.size(); }

    void record(int x, int y, int z, short observation) {
        long key = key(x, y, z);
        int offset = offset(x, y, z);
        Section section = sections.get(key);
        if (section != null && section.blocks[offset] == observation) return;
        if (section == null) {
            if (sections.size() == CAPACITY) {
                sections.remove(sections.keySet().iterator().next());
                evicted = true;
            }
            section = new Section();
        } else {
            sections.remove(key);
        }
        section.set(offset, observation);
        sections.put(key, section);
        dirty = true;
    }

    static ProvenanceStore load(Path path) {
        ProvenanceStore store = new ProvenanceStore();
        if (path == null) {
            store.status = "UNAVAILABLE";
            return store;
        }
        if (Files.notExists(path)) return store;
        try {
            if (Files.size(path) > MAX_FILE) throw invalid();
            InputStream file = Files.newInputStream(path);
            try {
                DataInputStream input = new DataInputStream(new GZIPInputStream(file));
                try { store.read(input); }
                finally { input.close(); }
            } finally {
                file.close();
            }
        } catch (IOException failure) {
            store.sections.clear();
            if ("READY".equals(store.status)) store.status = "UNREADABLE";
        }
        return store;
    }

    // Fixed, bounded NBT envelope. Checking each field before allocation avoids legacy NBT's
    // unbounded byte-array and list allocation on corrupt data.
    private void read(DataInputStream input) throws IOException {
        expect(input, 10, "");
        expect(input, 10, "data");
        expect(input, 3, "Schema");
        if (input.readInt() != VERSION) {
            status = "UNSUPPORTED_SCHEMA";
            throw invalid();
        }
        expect(input, 7, "Sections");
        int remaining = input.readInt() - 2;
        int size = input.readUnsignedShort();
        if (size > CAPACITY || remaining < 0 || remaining > CAPACITY * (10 + VOLUME * 4)) throw invalid();
        for (int i = 0; i < size; i++) {
            long key = input.readLong();
            int count = input.readUnsignedShort();
            remaining -= 10 + count * 4;
            if (key >>> 51 != 0 || !valid(sectionX(key), sectionY(key), sectionZ(key))
                    || count < 1 || count > VOLUME || remaining < 0 || sections.containsKey(key)) throw invalid();
            Section section = new Section();
            int previous = -1;
            for (int j = 0; j < count; j++) {
                int offset = input.readUnsignedShort();
                short observation = input.readShort();
                if (offset <= previous || offset >= VOLUME || observation == UNKNOWN
                        || observation < QUARRY_REMOVAL || observation > 4095) throw invalid();
                section.set(offset, observation);
                previous = offset;
            }
            sections.put(key, section);
        }
        if (remaining != 0 || input.readUnsignedByte() != 0 || input.readUnsignedByte() != 0
                || input.read() != -1) throw invalid();
    }

    void save(Path path) {
        if (!dirty || path == null || !("READY".equals(status) || "WRITE_FAILED".equals(status))) return;
        Path pending = null;
        try {
            Files.createDirectories(path.getParent());
            pending = Files.createTempFile(path.getParent(), "zto_provenance_", ".tmp");
            OutputStream file = Files.newOutputStream(pending);
            try {
                DataOutputStream output = new DataOutputStream(new GZIPOutputStream(file));
                try { write(output); }
                finally { output.close(); }
            } finally {
                file.close();
            }
            Files.move(pending, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
            status = "READY";
        } catch (IOException failure) {
            status = "WRITE_FAILED";
        } finally {
            if (pending != null) {
                try { Files.deleteIfExists(pending); }
                catch (IOException ignored) { status = "WRITE_FAILED"; }
            }
        }
    }

    private void write(DataOutputStream output) throws IOException {
        header(output, 10, "");
        header(output, 10, "data");
        header(output, 3, "Schema");
        output.writeInt(VERSION);
        header(output, 7, "Sections");
        int length = 2;
        for (Section section : sections.values()) length += 10 + section.count * 4;
        output.writeInt(length);
        output.writeShort(sections.size());
        for (Map.Entry<Long, Section> entry : sections.entrySet()) {
            Section section = entry.getValue();
            output.writeLong(entry.getKey());
            output.writeShort(section.count);
            for (int i = 0; i < VOLUME; i++) {
                if (section.blocks[i] == UNKNOWN) continue;
                output.writeShort(i);
                output.writeShort(section.blocks[i]);
            }
        }
        output.writeShort(0);
    }

    private static void expect(DataInputStream input, int type, String name) throws IOException {
        if (input.readUnsignedByte() != type || input.readUnsignedShort() != name.length()) throw invalid();
        for (int i = 0; i < name.length(); i++) if (input.readUnsignedByte() != name.charAt(i)) throw invalid();
    }

    private static void header(DataOutputStream output, int type, String name) throws IOException {
        output.writeByte(type);
        output.writeUTF(name);
    }

    private static IOException invalid() { return new IOException("Invalid provenance NBT"); }

    static final class Section {
        final short[] blocks = new short[VOLUME];
        int count, placed, manualRemoval, quarryRemoval;

        void set(int i, short value) {
            adjust(blocks[i], -1);
            blocks[i] = value;
            adjust(value, 1);
        }

        private void adjust(short value, int delta) {
            if (value == UNKNOWN) return;
            count += delta;
            if (value > 0) placed += delta;
            else if (value == MANUAL_REMOVAL) manualRemoval += delta;
            else quarryRemoval += delta;
        }
    }
}
