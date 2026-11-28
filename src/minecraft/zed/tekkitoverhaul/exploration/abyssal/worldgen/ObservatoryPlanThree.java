package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** A flooded shaft gives access to chambers cut below the existing seabed. */
class ObservatoryPlanThree extends ObservatoryPlan {
    // The chamber floor is one block below this datum. Native caves can fill through Y10 with lava.
    static final int MIN_BASE = 12, MAX_HEIGHT = 30;
    private final List<Room> rooms = new ArrayList<Room>();
    private final int wing, chamberFront, hallWidth, count;
    private final int[][] homes;
    private final Room chamber;

    ObservatoryPlanThree(long id, int x, int z, int y) {
        this(id, x, z, y, -5, 0);
    }

    ObservatoryPlanThree(long id, int x, int z, int y, int approachStart, int rearExtension) {
        super(id, x, z, y, approachStart, rearExtension);
        wing = (id & 4) == 0 ? -1 : 1;
        hallWidth = 7 + (int) (id & 1);
        chamberFront = length - 18;
        chamber = room(-9, 9, chamberFront, length, -1, 11);
        room(-4, 4, 0, 9, 3, 10);
        room(-6, 6, 6, 15, 2, 10);
        room(-3, 3, 13, 20, 0, 8);
        room(-hallWidth, hallWidth, 18, 34, 0, 10);
        room(-3, 3, 32, chamberFront + 2, -1, 8);
        int side = wing * 12;
        room(side - 3, side + 3, 23, chamberFront + 9, 1, 8);
        room(Math.min(5 * wing, side), Math.max(5 * wing, side), 24, 29, 1, 8);
        room(Math.min(6 * wing, side), Math.max(6 * wing, side), chamberFront + 4, chamberFront + 10, 0, 8);
        if (hallWidth == 8) room(Math.min(-13 * wing, -5 * wing), Math.max(-13 * wing, -5 * wing), 24, 31, 1, 7);
        homes = new int[][] {
            {-4,5,10}, {4,5,12}, {-5,3,22}, {5,5,31}, {side,4,25}, {side,3,chamberFront + 7},
            {-6,3,chamberFront + 12}, {5,4,chamberFront + 13}, {0,4,chamberFront - 2},
            {-10 * wing,4,27}, {side,5,32}, {3,6,27}
        };
        count = 8 + (width - 17) + (hallWidth == 8 ? 2 : 0);
    }

    private Room room(int a, int b, int first, int last, int floor, int roof) {
        Room room = new Room(a, b, first, last, floor, roof);
        rooms.add(room);
        return room;
    }

    @Override int revision() { return 3; }
    @Override int population() { return count; }

    int stationRoom(int slot) {
        if (slot < 2) return 0;
        if (slot < 4 || slot == 11) return 1;
        if (slot == 4 || slot == 9 && hallWidth == 7 || slot == 10) return 2;
        if (slot == 5 || slot == 8) return 3;
        if (slot < 8) return 4;
        return 5;
    }

    @Override int[] station(int slot) {
        int[] h = homes[slot];
        // Slot nine uses the optional annex only on wide sites.
        if (slot == 9 && hallWidth == 7) return position(wing * 12, 5, 32);
        return position(h[0], h[1], h[2]);
    }

    @Override int[] arenaCorner(boolean far) {
        return far ? position(8, 10, length - 1) : position(-8, 0, chamberFront + 1);
    }

    @Override int[] arenaCenter() { return position(0, 2, length - 8); }

    @Override int[] arenaEntry(boolean alternate) {
        return alternate ? position(wing * 9, 3, chamberFront + 7) : position(0, 2, chamberFront);
    }

    int coverRoof(int u, int v) {
        if (v <= 5) return 0;
        int top = -1;
        for (Room room : rooms) if (room.covers(u, v)) top = Math.max(top, room.roof);
        return top;
    }

    @Override boolean coversColumn(int wx, int wz) {
        int u = across(wx, wz), v = along(wx, wz);
        return v >= -3 && v <= 5 && Math.abs(u) <= 5 || coverRoof(u, v) >= 0;
    }

    @Override int cell(int u, int h, int v, int surface) {
        int side = Math.abs(u);
        if (v >= -2 && v <= 4 && side <= 4 && h >= surface - 1) {
            if (v == 0 && side == 3 && h <= surface + (u < 0 ? 3 : 2))
                return h == surface + 1 ? PILLAR : ERODED;
            if (v == 0 && side <= 3 && (h == surface + 3 && side > 1 || h == surface + 4 && side <= 2)) return FRIEZE;
            if (v == 1 && side == 3 && h == surface + 1) return LAMP;
            if (v <= 1 && side <= 2 && h == surface - 1 && v < 0) return ERODED;
        }
        if (side <= 2 && v >= 0 && v <= 4 && h >= 5 && h <= surface + 1) {
            if (side == 2 && h <= 9 && v % 2 == 0) return SLAB;
            return WATER;
        }
        return interior(u, h, v);
    }

    final int interior(int u, int h, int v) {
        int side = Math.abs(u);
        boolean hollow = false, shell = false;
        for (Room room : rooms) if (room.covers(u, v) && h >= room.floor && h <= room.roof) {
            if (room.inside(u, v) && h > room.floor && h < room.roof) hollow = true;
            else shell = true;
        }
        if (hollow) {
            if (v >= 19 && v <= 32 && side == hallWidth - 2 && (v - 20) % 5 == 0)
                return h == 1 ? PEDESTAL : h == 4 ? LAMP : h == 8 ? CARVED : PILLAR;
            if (v >= 14 && v <= 17 && side <= 2 && h == (17 - v) / 2 + 1)
                return v % 2 == 0 ? STAIRS : ERODED_STAIRS;
            if (v == length - 3 && side <= 2 && h == 0) return PEDESTAL;
            if ((v == length - 5 || v == chamberFront + 6) && side == 7 && h == 3) return LAMP;
            if (side == hallWidth - 1 && v >= 20 && v <= 30 && h == 1) return SLAB;
            if (u == wing * 14 && v == 32 && h == 2) return GRATE;
            return WATER;
        }
        if (!shell) return UNTOUCHED;
        if (h == 6 && (side == hallWidth && v >= 19 && v < 34 || side == 9 && v >= chamberFront)) return FRIEZE;
        return (TrenchRegion.hash(id + h, u, v) & 15) < 3 ? ERODED : BRICK;
    }

    @Override int metadata(int cell, int u, int v) {
        if (cell == STAIRS || cell == ERODED_STAIRS) {
            // The high half faces the entrance, opposite the positive longitudinal axis.
            return new int[] {3, 0, 2, 1}[rotation];
        }
        if (cell == LAMP) {
            int[] at = position(u, 0, v), toward = position(u + (u < 0 ? 1 : -1), 0, v);
            return toward[0] > at[0] ? 5 : toward[0] < at[0] ? 4 : toward[2] > at[2] ? 3 : 2;
        }
        return 0;
    }

    @Override boolean connected(boolean bypass) {
        int span = width * 2 + 1, depth = length + 1;
        boolean[] seen = new boolean[span * depth * 13];
        ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        queue.add(routeStart());
        int[][] offsets = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!queue.isEmpty()) {
            int[] p = queue.removeFirst();
            if (p[0] == 0 && p[2] == length - 8) return true;
            for (int[] d : offsets) {
                int u = p[0]+d[0], h = p[1]+d[1], v = p[2]+d[2];
                if (Math.abs(u) > width || h < 0 || h > 11 || v < 0 || v > length
                        || bypass && v == chamberFront && Math.abs(u) <= 3) continue;
                int i = u + width + span * (v + depth*h);
                if (seen[i] || cell(u, h, v, 17) != WATER || cell(u, h+1, v, 17) != WATER) continue;
                seen[i] = true;
                queue.add(new int[] {u,h,v});
            }
        }
        return false;
    }

    int[] routeStart() { return new int[] {0, 9, 2}; }
}
