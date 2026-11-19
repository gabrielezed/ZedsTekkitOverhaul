package zed.tekkitoverhaul.settlement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable recognized-site observation. IDs are scoped to one world save. */
public final class SiteSnapshot {
    public enum Coverage { COMPLETE, PARTIAL, STALE, UNAVAILABLE }

    public final long worldSession, lastConfirmed;
    public final int dimension, id;
    public final boolean observedConstruction, dormant;
    public final Coverage coverage;
    public final Bounds envelope;
    public final List<Part> parts;

    SiteSnapshot(long worldSession, int dimension, SiteRegistry.Site site, List<Part> parts, Coverage coverage,
                 boolean dormant) {
        this.worldSession = worldSession;
        this.dimension = dimension;
        id = site.id;
        lastConfirmed = site.lastSeen;
        observedConstruction = site.observed;
        this.dormant = dormant;
        this.coverage = coverage;
        envelope = site.bounds;
        this.parts = Collections.unmodifiableList(new ArrayList<Part>(parts));
    }

    /** Tests approximate known parts. Gaps in the envelope confer no membership. */
    public boolean contains(int x, int y, int z) {
        for (Part part : parts) if (part.bounds.contains(x, y, z, 0)) return true;
        return false;
    }

    public static final class Part {
        public final Bounds bounds;
        public final long lastConfirmed;
        public final boolean observedConstruction;
        public final int matchingPlacements, inferredBlocks, utilityOpenings;
        public final Coverage coverage;

        Part(SiteRegistry.Part part, Coverage coverage) {
            bounds = part.bounds;
            lastConfirmed = part.lastSeen;
            observedConstruction = part.observed;
            matchingPlacements = part.manual;
            inferredBlocks = part.inferred;
            utilityOpenings = part.uses;
            this.coverage = coverage;
        }
    }

    public static final class Bounds {
        public final int minX, minY, minZ, maxX, maxY, maxZ;

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
                    && maxX - minX < SiteSurvey.WIDTH && maxY - minY < SiteSurvey.LAYERS
                    && maxZ - minZ < SiteSurvey.WIDTH;
        }

        public boolean contains(int x, int y, int z) { return contains(x, y, z, 0); }

        boolean contains(int x, int y, int z, int margin) {
            return x >= minX - margin && x <= maxX + margin && y >= minY - margin && y <= maxY + margin
                    && z >= minZ - margin && z <= maxZ + margin;
        }

        boolean covers(Bounds other) {
            return contains(other.minX, other.minY, other.minZ) && contains(other.maxX, other.maxY, other.maxZ);
        }

        Bounds union(Bounds other) {
            return new Bounds(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                    Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ));
        }

        int volume() { return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1); }

        int overlap(Bounds other) {
            return span(minX, maxX, other.minX, other.maxX) * span(minY, maxY, other.minY, other.maxY)
                    * span(minZ, maxZ, other.minZ, other.maxZ);
        }

        int horizontalGap(Bounds other) {
            return gap(minX, maxX, other.minX, other.maxX) + gap(minZ, maxZ, other.minZ, other.maxZ);
        }

        private static int gap(int min, int max, int otherMin, int otherMax) {
            return Math.max(0, Math.max(min, otherMin) - Math.min(max, otherMax) - 1);
        }

        private static int span(int min, int max, int otherMin, int otherMax) {
            return Math.max(0, Math.min(max, otherMax) - Math.max(min, otherMin) + 1);
        }

        @Override public String toString() {
            return "(" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ")";
        }
    }
}
