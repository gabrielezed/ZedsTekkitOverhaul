package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry.MaterialType;

/** Revision-one architecture. Coordinates are relative to the entrance, facing inward. */
final class ObservatoryPlan {
    static final int REVISION = 1;
    static final int UNTOUCHED = 0, WATER = 1, BRICK = 2, ERODED = 3, STONE = 4, SILT = 5, LAMP = 6, SHELL = 7, CORAL = 8;
    private static final MaterialType[] MATERIALS = {null, null, MaterialType.BRICKS,
        MaterialType.ERODED_BRICKS, MaterialType.STONE, MaterialType.SILT, MaterialType.LAMP,
        MaterialType.SHELLSTONE, MaterialType.PETRIFIED_CORAL};

    final long id;
    final int x, z, y, rotation, length, width;
    final int minX, maxX, minZ, maxZ;
    final Room arena;
    final int[][] stations;
    private final List<Room> rooms = new ArrayList<Room>();
    private final int hallStart, hallEnd, atriumEnd, branch;

    ObservatoryPlan(long id, int x, int z, int y) {
        this.id = id;
        this.x = x;
        this.z = z;
        this.y = y;
        Random layout = new Random(id ^ 0x150874379bL);
        rotation = layout.nextInt(4);
        width = 17 + layout.nextInt(3);
        atriumEnd = 9 + layout.nextInt(4);
        hallStart = atriumEnd + 7 + layout.nextInt(4);
        hallEnd = hallStart + 15;
        length = hallEnd + 23;
        branch = layout.nextBoolean() ? 1 : -1;
        rooms.add(new Room(-7, 7, 0, atriumEnd, 0, 7));
        rooms.add(new Room(-3, 3, atriumEnd, hallStart, 0, 6));
        rooms.add(new Room(-7, 7, hallStart, hallEnd, 0, 8));
        rooms.add(new Room(-3, 3, hallEnd, hallEnd + 5, 0, 6));
        arena = new Room(-9, 9, hallEnd + 5, length, 0, 9);
        rooms.add(arena);
        rooms.add(new Room(3, width, atriumEnd - 5, atriumEnd + 1, 0, 5));
        rooms.add(new Room(width - 6, width, atriumEnd, hallStart + 5, 0, 5));
        rooms.add(new Room(5, width, hallStart + 1, hallStart + 7, 0, 5));
        rooms.add(new Room(-width, -5, hallStart + 8, hallEnd - 1, -1, 5));
        rooms.add(new Room(-width, -width + 5, hallEnd - 3, hallEnd + 11, -1, 5));
        rooms.add(new Room(-width, -7, hallEnd + 7, hallEnd + 12, 0, 5));
        if (layout.nextBoolean()) rooms.add(new Room(-width, -5, 3, 10, 0, 5));
        stations = new int[][] {{-8, 9, -4}, {8, 9, -4}, {0, 7, -3},
            {-4, 2, 4}, {3, 4, 6}, {0, 3, atriumEnd + 3}, {1, 4, hallStart - 2},
            {width - 3, 2, atriumEnd + 3}, {-width + 3, 2, hallEnd - 5},
            {0, 3, hallStart + 4}, {3, 4, hallStart + 10}, {-5, 3, hallStart + 7},
            {1, 3, hallEnd - 3}, {1, 3, hallEnd + 2}, {-width + 2, 2, hallEnd + 3},
            {4, 3, atriumEnd - 2}, {6, 3, -3}, {-1, 2, hallStart - 3},
            {-3, 5, hallEnd - 3}};
        int[] a = position(-width, 0, -5), b = position(width, 0, length);
        minX = Math.min(a[0], b[0]);
        maxX = Math.max(a[0], b[0]);
        minZ = Math.min(a[2], b[2]);
        maxZ = Math.max(a[2], b[2]);
    }

    int[] position(int u, int height, int v) {
        u *= branch;
        switch (rotation) {
            case 0: return new int[] {x + u, y + height, z + v};
            case 1: return new int[] {x - v, y + height, z + u};
            case 2: return new int[] {x - u, y + height, z - v};
            default: return new int[] {x + v, y + height, z - u};
        }
    }

    int across(int wx, int wz) {
        int offset = (rotation & 1) == 0 ? wx - x : wz - z;
        return branch * (rotation < 2 ? offset : -offset);
    }

    int along(int wx, int wz) {
        int offset = (rotation & 1) == 0 ? wz - z : x - wx;
        return rotation < 2 ? offset : -offset;
    }

    int slice(int cx, int cz) {
        return cx - (minX >> 4) + (cz - (minZ >> 4)) * ((maxX >> 4) - (minX >> 4) + 1);
    }

    long allSlices() {
        int count = ((maxX >> 4) - (minX >> 4) + 1) * ((maxZ >> 4) - (minZ >> 4) + 1);
        return (1L << count) - 1;
    }

    boolean intersects(int cx, int cz) {
        return cx >= minX >> 4 && cx <= maxX >> 4 && cz >= minZ >> 4 && cz <= maxZ >> 4;
    }

    boolean coversColumn(int wx, int wz) {
        if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) return false;
        int u = across(wx, wz), v = along(wx, wz);
        if (v < 0) return Math.abs(u) <= 2 && v >= -4 || Math.abs(u) >= 5 && Math.abs(u) <= 6 && (v == -2 || v == -5);
        for (Room room : rooms) if (room.covers(u, v)) return true;
        return false;
    }

    boolean connected(boolean bypass) {
        int stride = width * 2 + 1;
        boolean[] visited = new boolean[stride * (length + 5)];
        ArrayDeque<Integer> open = new ArrayDeque<Integer>();
        int start = width;
        open.add(start);
        visited[start] = true;
        while (!open.isEmpty()) {
            int index = open.remove();
            int u = index % stride - width, v = index / stride - 4;
            if (u == 0 && v == length - 9) return true;
            for (int side = 0; side < 4; side++) {
                int ux = u + (side == 0 ? -1 : side == 1 ? 1 : 0);
                int vz = v + (side == 2 ? -1 : side == 3 ? 1 : 0);
                if (Math.abs(ux) > width || vz < -4 || vz > length
                        || bypass && vz == arena.first && Math.abs(ux) <= 3) continue;
                int adjacent = width + ux + (vz + 4) * stride;
                if (visited[adjacent] || cell(ux, 2, vz, 0) != WATER || cell(ux, 3, vz, 0) != WATER) continue;
                visited[adjacent] = true;
                open.add(adjacent);
            }
        }
        return false;
    }

    private boolean portal(int u, int v, int h) {
        if (h < 1 || h > 5) return false;
        if (Math.abs(u) <= (h == 5 ? 1 : 2)
                && (v == 0 || v == atriumEnd || v == hallStart || v == hallEnd || v == arena.first)) return true;
        // Penetrations at overlapping walls keep the two bypasses open.
        return h <= 4 && ((u >= 3 && u <= 7 && v >= atriumEnd - 4 && v <= atriumEnd - 1)
                || (u >= 5 && u <= 7 && v >= hallStart + 2 && v <= hallStart + 6)
                || (u <= -5 && u >= -7 && v >= hallStart + 9 && v <= hallEnd - 2)
                || (u <= -7 && u >= -9 && v >= hallEnd + 8 && v <= hallEnd + 10)
                || (Math.abs(u - (width - 3)) <= 2 && v == atriumEnd)
                || (Math.abs(u + width - 3) <= 2 && v == hallEnd - 3)
                || (u == -7 && v >= 5 && v <= 8 && insideAnnex(u, v)));
    }

    private boolean insideAnnex(int u, int v) {
        for (int i = 1; i < rooms.size(); i++) if (rooms.get(i).covers(u, v)) return true;
        return false;
    }

    int cell(int u, int h, int v, int floor) {
        int abs = Math.abs(u);
        if (v < 0) {
            if (abs <= 2 && v >= -4) {
                if (h <= 0 && h >= -2) return STONE;
                if (h >= 1 && h <= 8) return WATER;
            }
            if ((abs == 5 || abs == 6) && (v == -2 || v == -5) && h >= -1
                    && h <= (u < 0 ? 7 : 10) + v) {
                if (h <= 0) return SHELL;
                if (h == 1 && v == -5 && abs == 6) return CORAL;
                return masonry(u, h, v);
            }
            return UNTOUCHED;
        }
        if (v == 0 && abs >= 5 && abs <= 7 && h >= 7 && h <= 11) return masonry(u, h, v);
        boolean shell = false, interior = false, roof = false, foundation = false;
        for (Room room : rooms) {
            if (!room.covers(u, v)) continue;
            if (h < room.floor) foundation |= h > floor || h >= -1;
            if (h < room.floor || h > room.roof) continue;
            if (h > room.floor && h < room.roof && room.inside(u, v)) interior = true;
            else {
                shell = true;
                roof |= h == room.roof;
            }
        }
        if (foundation) return STONE;
        if (interior || portal(u, v, h)) {
            if (v > hallStart + 1 && v < hallEnd && abs == 4
                    && ((v - hallStart) % 5 == 2) && h <= 7) return masonry(u, h, v);
            if (v >= length - 5 && abs <= 3 && h == 1) return STONE;
            if (v == length - 2 && abs == 4 && h <= 6) return masonry(u, h, v);
            if ((v == 3 && abs == 6 || v == hallStart + 3 && abs == 6
                    || v == length - 4 && abs == 8) && h == 3) return LAMP;
            return WATER;
        }
        if (!shell) return UNTOUCHED;
        if (h <= 0 && (abs >= 10 || TrenchRegion.field(id ^ 944, u, v, 7) > 0.80)) return SILT;
        if (roof && abs >= 3 && (v < 6 || v >= hallStart && v < hallEnd)
                && TrenchRegion.field(id ^ 449, u, v, 6) > 0.71) return WATER;
        if (!roof && h >= 3 && h <= 5 && abs == 7 && v > 3 && v < atriumEnd && v % 4 == 0) return WATER;
        if (v == length && abs <= 2 && h >= 2 && h <= 6) return ERODED;
        return masonry(u, h, v);
    }

    private int masonry(int u, int h, int v) {
        return TrenchRegion.field(id ^ 726, u + h * 0.4, v - h * 0.3, 7) > 0.58 ? ERODED : BRICK;
    }

    static int material(int cell) { return MATERIALS[cell].block().blockID; }

    static final class Room {
        final int left, right, first, last, floor, roof;

        Room(int left, int right, int first, int last, int floor, int roof) {
            this.left = left;
            this.right = right;
            this.first = first;
            this.last = last;
            this.floor = floor;
            this.roof = roof;
        }

        boolean covers(int u, int v) { return u >= left && u <= right && v >= first && v <= last; }
        boolean inside(int u, int v) { return u > left && u < right && v > first && v < last; }
    }
}
