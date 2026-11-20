package zed.tekkitoverhaul.settlement;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;

/** Append-only identities with bounded local parts. */
final class SiteRegistry {
    static final int CAPACITY = 256, PART_LIMIT = 8;
    private static final int FRAGMENT_LIMIT = 64, MAX_MASK = (SiteSurvey.VOLUME + 7) / 8;
    private static final int ENVELOPE_WIDTH = 64, ENVELOPE_HEIGHT = 24;
    private static final int MAGIC = 0x5A544F53, COMPOUND = 2, FRAGMENTS = 3, HEADER = 10, SITE_ROW = 13, PART_ROW = 39;
    static final int MAX_FILE = HEADER + CAPACITY * (SITE_ROW + FRAGMENT_LIMIT * (PART_ROW + 1 + MAX_MASK));
    private final Path path;
    final List<Site> sites = new ArrayList<Site>();
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

    Site byId(int id) { return id > 0 && id <= size() ? sites.get(id - 1) : null; }

    boolean writable() { return "READY".equals(status) || "WRITE_FAILED".equals(status); }

    Match match(Bounds bounds, int x, int y, int z) {
        Match match = new Match();
        for (Site site : sites) {
            if (!site.bounds.contains(x, y, z) && (bounds == null || site.bounds.overlap(bounds) == 0)) continue;
            for (Part part : site.parts) {
                if (!part.bounds.contains(x, y, z) && !substantial(part.bounds, bounds)) continue;
                if (match.site != null && match.site != site) {
                    match.ambiguous = true;
                    return match;
                }
                int overlap = bounds == null ? 0 : part.bounds.overlap(bounds);
                if (match.part == null || overlap > match.overlap) {
                    match.part = part;
                    match.overlap = overlap;
                }
                match.site = site;
            }
        }
        return match;
    }

    private static boolean substantial(Bounds a, Bounds b) {
        return b != null && a.overlap(b) * 3 >= Math.min(a.volume(), b.volume());
    }

    Match connected(SiteSurvey survey, SiteSurvey.Evidence evidence) {
        Match match = new Match();
        for (Site site : sites) {
            if (site.bounds.overlap(evidence.bounds) == 0) continue;
            for (Part part : site.parts) {
                int shared = survey.sharedConstruction(evidence, part);
                if (shared < 8) continue;
                if (match.site != null && match.site != site) {
                    match.ambiguous = true;
                    return match;
                }
                match.site = site;
                if (shared > match.overlap) {
                    match.part = part;
                    match.overlap = shared;
                }
            }
        }
        return match;
    }

    boolean fragmentOverlap(Bounds bounds) {
        for (Site site : sites) {
            if (!site.fragmented || site.bounds.overlap(bounds) == 0) continue;
            for (Part part : site.parts)
                if (part.construction != null && part.bounds.overlap(bounds) > 0) return true;
        }
        return false;
    }

    Site promote(Bounds bounds, long clock, int manual, int inferred, int uses) {
        if (!writable() || size() == CAPACITY) return null;
        for (Site site : sites) {
            if (!site.fragmented || site.bounds.overlap(bounds) == 0) continue;
            for (Part part : site.parts) if (part.bounds.overlap(bounds) > 0) return null;
        }
        Site site = new Site(size() + 1, clock);
        sites.add(site);
        add(site, new Part(bounds, clock, manual >= 8, manual, inferred, uses));
        dirty = true;
        return site;
    }

    Site promoteFragment(Part fragment) {
        if (!writable() || size() == CAPACITY || intersectsOther(fragment.bounds, null)) return null;
        Site site = new Site(size() + 1, fragment.lastSeen);
        sites.add(site);
        add(site, fragment);
        dirty = true;
        return site;
    }

    boolean attachFragment(Site site, Part fragment) {
        Bounds extent = site.bounds.union(fragment.bounds);
        if (!writable() || site.parts.size() >= FRAGMENT_LIMIT || !validEnvelope(extent, true)
                || intersectsOther(fragment.bounds, null)) return false;
        add(site, fragment);
        dirty = true;
        return true;
    }

    boolean completeFragment(Site site, Part part, Bounds bounds, long clock, int manual, int inferred, int uses) {
        if (site.parts.size() != 1 || part.construction == null || !bounds.covers(part.bounds)
                || intersectsOther(bounds, part)) return false;
        part.construction = null;
        part.bounds = bounds;
        reobserve(site, part, clock, manual, inferred, uses);
        refreshEnvelope(site);
        dirty = true;
        return true;
    }

    Part attach(Site site, Bounds bounds, long clock, int manual, int inferred, int uses) {
        if (!writable() || site.parts.size() >= PART_LIMIT || !validEnvelope(site.bounds.union(bounds), site.fragmented)
                || intersectsOther(bounds, null)) return null;
        Part part = new Part(bounds, clock, manual >= 8, manual, inferred, uses);
        add(site, part);
        dirty = true;
        return part;
    }

    void reobserve(Site site, Part part, long clock, int manual, int inferred, int uses) {
        long next = Math.max(part.lastSeen, clock);
        if (next == part.lastSeen && part.manual == manual && part.inferred == inferred && part.uses == uses) return;
        part.lastSeen = next;
        part.observed |= manual >= 8;
        part.manual = manual;
        part.inferred = inferred;
        part.uses = uses;
        site.lastSeen = Math.max(site.lastSeen, next);
        site.observed |= part.observed;
        dirty = true;
    }

    boolean expand(Site site, Part part, Bounds surveyed) {
        Bounds old = part.bounds;
        if (old.overlap(surveyed) * 3 < old.volume() * 2) return false;
        Bounds union = old.union(surveyed);
        if (!union.valid() || union.volume() == old.volume() || !validEnvelope(site.bounds.union(union), site.fragmented)
                || intersectsOther(union, part)) return false;
        part.bounds = union;
        part.resetContraction();
        refreshEnvelope(site);
        dirty = true;
        return true;
    }

    boolean contract(Site site, Part part, Bounds smaller) {
        if (part.construction != null) return false;
        boolean repeated = part.contractions > 0 && (smaller == null ? part.smaller == null
                : part.smaller != null && smaller.covers(part.smaller) && part.smaller.covers(smaller));
        part.smaller = smaller;
        part.contractions = repeated ? part.contractions + 1 : 1;
        if (part.contractions < 3 || (smaller == null && site.parts.size() == 1)) return false;
        if (smaller == null) site.parts.remove(part);
        else {
            part.bounds = smaller;
            part.resetContraction();
        }
        refreshEnvelope(site);
        dirty = true;
        return true;
    }

    private boolean intersectsOther(Bounds surveyed, Part own) {
        for (Site site : sites) {
            if (site.bounds.overlap(surveyed) == 0) continue;
            for (Part part : site.parts) {
                if (part != own && part.bounds.overlap(surveyed) > 0) return true;
            }
        }
        return false;
    }

    private static boolean validEnvelope(Bounds bounds, boolean fragmented) {
        int width = fragmented ? 512 : ENVELOPE_WIDTH;
        int height = fragmented ? 64 : ENVELOPE_HEIGHT;
        return bounds.maxX - bounds.minX < width && bounds.maxZ - bounds.minZ < width
                && bounds.maxY - bounds.minY < height;
    }

    private static void add(Site site, Part part) {
        site.parts.add(part);
        site.bounds = site.bounds == null ? part.bounds : site.bounds.union(part.bounds);
        site.lastSeen = Math.max(site.lastSeen, part.lastSeen);
        site.observed |= part.observed;
        site.fragmented |= part.construction != null;
    }

    private static void refreshEnvelope(Site site) {
        site.bounds = site.parts.get(0).bounds;
        site.observed = false;
        site.fragmented = false;
        for (Part part : site.parts) {
            site.bounds = site.bounds.union(part.bounds);
            site.observed |= part.observed;
            site.fragmented |= part.construction != null;
        }
    }

    private void read() throws IOException {
        long length = Files.size(path);
        if (length < 8) throw invalid();
        DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)));
        try {
            if (input.readInt() != MAGIC) throw invalid();
            int version = input.readInt();
            if (version != 1 && version != COMPOUND && version != FRAGMENTS) {
                status = "UNSUPPORTED_SCHEMA";
                throw invalid();
            }
            int count = input.readUnsignedShort();
            if (count > CAPACITY || length > MAX_FILE || (version == 1 && length != HEADER + count * 37)) throw invalid();
            for (int id = 1; id <= count; id++) {
                if (input.readInt() != id) throw invalid();
                Site site;
                if (version == 1) {
                    Part part = readPart(input, version);
                    site = new Site(id, part.lastSeen);
                    add(site, part);
                } else {
                    long clock = input.readLong();
                    int parts = input.readUnsignedByte();
                    if (clock < 0 || parts < 1 || parts > (version == FRAGMENTS ? FRAGMENT_LIMIT : PART_LIMIT)) throw invalid();
                    site = new Site(id, clock);
                    for (int i = 0; i < parts; i++) {
                        Part part = readPart(input, version);
                        if (part.lastSeen > clock) throw invalid();
                        for (Part known : site.parts) if (known.bounds.overlap(part.bounds) > 0) throw invalid();
                        add(site, part);
                    }
                }
                if ((!site.fragmented && site.parts.size() > PART_LIMIT) || !validEnvelope(site.bounds, site.fragmented)) throw invalid();
                for (Site existing : sites) {
                    if (existing.bounds.overlap(site.bounds) == 0) continue;
                    for (Part part : site.parts) for (Part known : existing.parts) {
                        if (part.construction != null || known.construction != null) {
                            if (part.bounds.overlap(known.bounds) > 0) throw invalid();
                        } else if (substantial(known.bounds, part.bounds)) throw invalid();
                    }
                }
                sites.add(site);
            }
            if (input.read() != -1) throw invalid();
            dirty = version == 1;
        } finally {
            input.close();
        }
    }

    private static Part readPart(DataInputStream input, int version) throws IOException {
        boolean extended = version >= COMPOUND;
        Bounds bounds = new Bounds(input.readInt(), input.readInt(), input.readInt(),
                input.readInt(), input.readInt(), input.readInt());
        long seen = input.readLong();
        int evidence = input.readUnsignedByte();
        int manual = extended ? input.readShort() : -1;
        int inferred = extended ? input.readShort() : -1;
        int uses = extended ? input.readShort() : -1;
        if (!bounds.valid() || seen < 0 || evidence > 1 || manual < -1 || inferred < -1
                || manual + inferred > SiteSurvey.VOLUME || uses < -1 || uses > 16
                || (manual == -1) != (inferred == -1) || (manual == -1) != (uses == -1)) throw invalid();
        Part part = new Part(bounds, seen, evidence == 1, manual, inferred, uses);
        if (version == FRAGMENTS) {
            int bytes = input.readUnsignedByte();
            if (bytes > MAX_MASK) throw invalid();
            if (bytes != 0) {
                byte[] mask = new byte[bytes];
                input.readFully(mask);
                part.construction = BitSet.valueOf(mask);
                if (part.construction.length() > bounds.volume() || part.construction.cardinality() != manual + inferred
                        || part.construction.cardinality() < 12) throw invalid();
            }
        }
        return part;
    }

    void save() {
        if (!dirty || !writable()) return;
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "zto_sites_", ".tmp");
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)));
            try {
                output.writeInt(MAGIC);
                boolean fragmented = false;
                for (Site site : sites) fragmented |= site.fragmented;
                output.writeInt(fragmented ? FRAGMENTS : COMPOUND);
                output.writeShort(size());
                for (Site site : sites) {
                    output.writeInt(site.id);
                    output.writeLong(site.lastSeen);
                    output.writeByte(site.parts.size());
                    for (Part part : site.parts) {
                        Bounds box = part.bounds;
                        output.writeInt(box.minX);
                        output.writeInt(box.minY);
                        output.writeInt(box.minZ);
                        output.writeInt(box.maxX);
                        output.writeInt(box.maxY);
                        output.writeInt(box.maxZ);
                        output.writeLong(part.lastSeen);
                        output.writeBoolean(part.observed);
                        output.writeShort(part.manual);
                        output.writeShort(part.inferred);
                        output.writeShort(part.uses);
                        if (fragmented) {
                            byte[] mask = part.construction == null ? new byte[0] : part.construction.toByteArray();
                            output.writeByte(mask.length);
                            output.write(mask);
                        }
                    }
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

    static final class Match {
        Site site;
        Part part;
        int overlap;
        boolean ambiguous;
    }

    static final class Site {
        final int id;
        final List<Part> parts = new ArrayList<Part>();
        Bounds bounds;
        long lastSeen;
        boolean observed, fragmented;

        Site(int id, long lastSeen) {
            this.id = id;
            this.lastSeen = lastSeen;
        }
    }

    static final class Part {
        Bounds bounds, smaller;
        long lastSeen;
        boolean observed;
        BitSet construction;
        int manual, inferred, uses, contractions;
        long checkedAt = -1, session = -1;
        SiteSnapshot.Coverage coverage = SiteSnapshot.Coverage.PARTIAL;

        Part(Bounds bounds, long lastSeen, boolean observed, int manual, int inferred, int uses) {
            this.bounds = bounds;
            this.lastSeen = lastSeen;
            this.observed = observed;
            this.manual = manual;
            this.inferred = inferred;
            this.uses = uses;
        }

        void resetContraction() {
            contractions = 0;
            smaller = null;
        }
    }
}
