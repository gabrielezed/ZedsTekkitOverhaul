package zed.tekkitoverhaul.distillation;

/** Pixel bounds shared by the chunk model and the overlay renderer. */
final class DistillationGeometry {
    static final int SIDES = 0x3c;
    static final int ALL = 0x3f;
    static final int[] DX = {0, 0, 0, 0, -1, 1};
    static final int[] DY = {-1, 1, 0, 0, 0, 0};
    static final int[] DZ = {0, 0, -1, 1, 0, 0};

    static final Box COLUMN_BODY = new Box(3, 2, 3, 13, 14, 13, SIDES, false);
    static final Box COLUMN_BASE = new Box(1, 0, 1, 15, 2, 15, ALL, true);
    static final Box COLUMN_CAP = new Box(1, 14, 1, 15, 16, 15, ALL, true);
    static final Box[] COLUMN_RAILS = {
        new Box(2, 2, 2, 4, 14, 4, SIDES, true), new Box(12, 2, 2, 14, 14, 4, SIDES, true),
        new Box(2, 2, 12, 4, 14, 14, SIDES, true), new Box(12, 2, 12, 14, 14, 14, SIDES, true)
    };
    static final Box CONDENSER_BODY = new Box(2, 2, 2, 14, 14, 14, SIDES, false);
    static final Box CONDENSER_BASE = new Box(1, 0, 1, 15, 2, 15, ALL, true);
    static final Box CONDENSER_CAP = new Box(1, 14, 1, 15, 16, 15, ALL, true);
    static final Box RECEIVER_BODY = new Box(3, 2, 3, 13, 13, 13, SIDES, false);
    static final Box RECEIVER_BASE = new Box(1, 0, 1, 15, 2, 15, ALL, true);
    static final Box RECEIVER_CAP = new Box(2, 13, 2, 14, 14, 14, ALL, true);
    static final Box RECEIVER_NECK = new Box(6, 14, 6, 10, 16, 10, SIDES, true);

    // Opposing collars share exactly one plane. Their touching faces are omitted.
    static final Box[] COLUMN_PORTS = {
        null, null, new Box(6, 7, 0, 10, 11, 3, ALL & ~(1 << 2), true),
        new Box(6, 7, 13, 10, 11, 16, ALL & ~(1 << 3), true),
        new Box(0, 7, 6, 3, 11, 10, ALL & ~(1 << 4), true),
        new Box(13, 7, 6, 16, 11, 10, ALL & ~(1 << 5), true)
    };
    static final Box[] CONDENSER_PORTS = {
        null, null, new Box(6, 7, 0, 10, 11, 2, ALL & ~(1 << 2), true),
        new Box(6, 7, 14, 10, 11, 16, ALL & ~(1 << 3), true),
        new Box(0, 7, 6, 2, 11, 10, ALL & ~(1 << 4), true),
        new Box(14, 7, 6, 16, 11, 10, ALL & ~(1 << 5), true)
    };
    static final Box[] RECEIVER_OUTLETS = {
        null, null, new Box(6, 4, 1, 10, 8, 3, ALL, true),
        new Box(6, 4, 13, 10, 8, 15, ALL, true),
        new Box(1, 4, 6, 3, 8, 10, ALL, true),
        new Box(13, 4, 6, 15, 8, 10, ALL, true)
    };

    private DistillationGeometry() { }

    static boolean shaped(DistillationPartType part) {
        return part == DistillationPartType.COLUMN || part == DistillationPartType.CONDENSER
                || part == DistillationPartType.RECEIVER;
    }

    static final class Box {
        final int minX, minY, minZ, maxX, maxY, maxZ, faces;
        final boolean trim;

        Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int faces, boolean trim) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.faces = faces;
            this.trim = trim;
        }

        boolean onBoundary(int side) {
            switch (side) {
                case 0: return minY == 0;
                case 1: return maxY == 16;
                case 2: return minZ == 0;
                case 3: return maxZ == 16;
                case 4: return minX == 0;
                case 5: return maxX == 16;
                default: throw new IllegalArgumentException("Invalid face");
            }
        }
    }
}
