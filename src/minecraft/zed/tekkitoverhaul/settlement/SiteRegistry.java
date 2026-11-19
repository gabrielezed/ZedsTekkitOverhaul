package zed.tekkitoverhaul.settlement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Append-only identities for the local Overworld. No eviction or ID reuse in alpha.2. */
final class SiteRegistry {
    static final int CAPACITY = 256;
    private static final int MAGIC = 0x5A544F53, VERSION = 1, HEADER = 10, ROW = 37;
    private final Path path;
    private final List<Site> sites = new ArrayList<Site>();
    String status = "READY";
    private boolean dirty;

    SiteRegistry(Path path) {
        this.path = path;
        if (path == null) {
            status = "UNAVAILABLE";
        } else if (!Files.notExists(path)) {
            try {
                read();
            } catch (IOException invalid) {
                sites.clear();
                if ("READY".equals(status)) status = "UNREADABLE";
            }
        }
    }

    int size() { return sites.size(); }

    boolean writable() { return "READY".equals(status) || "WRITE_FAILED".equals(status); }

    Site containing(int x, int y, int z) {
        for (Site site : sites) if (site.bounds.contains(x, y, z, 0)) return site;
        return null;
    }

    Site matching(Bounds bounds) {
        Site best = null;
        int overlap = 0;
        for (Site site : sites) {
            int shared = site.bounds.overlap(bounds);
            if (shared <= overlap || shared * 3 < Math.min(bounds.volume(), site.bounds.volume())) continue;
            best = site;
            overlap = shared;
        }
        return best;
    }

    Site promote(Bounds bounds, long clock, boolean observed) {
        if (!writable() || size() == CAPACITY) return null;
        Site site = new Site(size() + 1, bounds, clock, observed);
        sites.add(site);
        dirty = true;
        return site;
    }

    void reobserve(Site site, long clock, boolean observed) {
        long next = Math.max(site.lastSeen, clock);
        if (next == site.lastSeen && (!observed || site.observed)) return;
        site.lastSeen = next;
        site.observed |= observed;
        dirty = true;
    }

    private void read() throws IOException {
        long length = Files.size(path);
        if (length < 8) throw invalid();
        DataInputStream input = new DataInputStream(Files.newInputStream(path));
        try {
            if (input.readInt() != MAGIC) throw invalid();
            if (input.readInt() != VERSION) {
                status = "UNSUPPORTED_SCHEMA";
                throw invalid();
            }
            int count = input.readUnsignedShort();
            if (count > CAPACITY || length != HEADER + count * ROW) throw invalid();
            for (int i = 1; i <= count; i++) {
                int id = input.readInt();
                Bounds bounds = new Bounds(input.readInt(), input.readInt(), input.readInt(),
                        input.readInt(), input.readInt(), input.readInt());
                long seen = input.readLong();
                int evidence = input.readUnsignedByte();
                if (id != i || !bounds.valid() || seen < 0 || evidence > 1) throw invalid();
                if (matching(bounds) != null) throw invalid();
                sites.add(new Site(id, bounds, seen, evidence == 1));
            }
            if (input.read() != -1) throw invalid();
        } finally {
            input.close();
        }
    }

    void save() {
        if (!dirty || !writable()) return;
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "zto_sites_", ".tmp");
            DataOutputStream output = new DataOutputStream(Files.newOutputStream(temporary));
            try {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeShort(size());
                for (Site site : sites) {
                    Bounds box = site.bounds;
                    output.writeInt(site.id);
                    output.writeInt(box.minX);
                    output.writeInt(box.minY);
                    output.writeInt(box.minZ);
                    output.writeInt(box.maxX);
                    output.writeInt(box.maxY);
                    output.writeInt(box.maxZ);
                    output.writeLong(site.lastSeen);
                    output.writeBoolean(site.observed);
                }
            } finally {
                output.close();
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
            status = "READY";
        } catch (IOException failure) {
            status = "WRITE_FAILED";
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { status = "WRITE_FAILED"; }
            }
        }
    }

    private static IOException invalid() { return new IOException("Invalid settlement registry"); }

    static final class Site {
        final int id;
        final Bounds bounds;
        long lastSeen;
        boolean observed;

        Site(int id, Bounds bounds, long lastSeen, boolean observed) {
            this.id = id;
            this.bounds = bounds;
            this.lastSeen = lastSeen;
            this.observed = observed;
        }
    }

    static final class Bounds {
        final int minX, minY, minZ, maxX, maxY, maxZ;

        Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean valid() {
            return ProvenanceStore.valid(minX, minY, minZ) && ProvenanceStore.valid(maxX, maxY, maxZ)
                    && maxX >= minX && maxY >= minY && maxZ >= minZ
                    && maxX - minX < 13 && maxY - minY < 9 && maxZ - minZ < 13;
        }

        boolean contains(int x, int y, int z, int margin) {
            return x >= minX - margin && x <= maxX + margin && y >= minY - margin && y <= maxY + margin
                    && z >= minZ - margin && z <= maxZ + margin;
        }

        int volume() { return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1); }

        int overlap(Bounds other) {
            return span(minX, maxX, other.minX, other.maxX) * span(minY, maxY, other.minY, other.maxY)
                    * span(minZ, maxZ, other.minZ, other.maxZ);
        }

        private static int span(int min, int max, int otherMin, int otherMax) {
            return Math.max(0, Math.min(max, otherMax) - Math.max(min, otherMin) + 1);
        }

        @Override
        public String toString() {
            return "(" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ")";
        }
    }
}
