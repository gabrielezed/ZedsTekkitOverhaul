package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Composes room shells, linking passages and detail independently of module order. */
final class ObservatoryPlanTwo extends ObservatoryPlan {
    private static final int FIRST = -5, LOW = -2, HIGH = 11;
    private final List<Module> modules = new ArrayList<Module>();
    private final List<Room> openings = new ArrayList<Room>();
    private final int[][] homes;
    private final int hallU, hallV, arenaU, entranceEnd, lowerV, side;
    private final Room chamber;
    private byte[] voxels;

    ObservatoryPlanTwo(long id, int x, int z, int y) {
        super(id, x, z, y);
        Random geometry = new Random(id ^ 0x1476C83F520BA927L);
        entranceEnd = 9 + geometry.nextInt(4);
        hallV = 23 + geometry.nextInt(4);
        side = geometry.nextBoolean() ? 1 : -1;
        hallU = side * geometry.nextInt(5);
        arenaU = hallU - side * (1 + geometry.nextInt(3));
        lowerV = hallV + 18;
        int hallFloor = geometry.nextInt(3) - 1;
        int arenaFloor = geometry.nextBoolean() ? -1 : 1;
        int wing = geometry.nextBoolean() ? 1 : -1;
        int branchU = wing * (width - 4);
        int returnU = -wing * (width - 4);
        room(0, 7, 0, entranceEnd, 0, 8, false);
        corridor(0, hallU, entranceEnd - 2, hallV - 3, 0, hallFloor);
        room(hallU, 8, hallV - 7, hallV + 8, hallFloor, 10, geometry.nextBoolean());
        int arenaFront = length - 17;
        chamber = room(arenaU, 9, arenaFront, length, arenaFloor, 10, false);
        corridor(hallU, arenaU, hallV + 5, arenaFront + 2, hallFloor, arenaFloor);

        int wingFloor = geometry.nextBoolean() ? -2 : 0;
        room(branchU, 4, entranceEnd - 5, entranceEnd + 4, wingFloor, 5, geometry.nextBoolean());
        cross(0, branchU, entranceEnd - 2, 0, wingFloor);
        corridor(branchU, branchU, entranceEnd + 2, hallV + 1, wingFloor, hallFloor);
        cross(branchU, hallU, hallV, wingFloor, hallFloor);

        int terrace = hallFloor == 1 ? 0 : 2;
        room(returnU, 4, hallV + 4, lowerV + 3, terrace, 7, geometry.nextBoolean());
        cross(hallU, returnU, hallV + 5, hallFloor, terrace);
        corridor(returnU, returnU, lowerV + 1, arenaFront + 6, terrace, arenaFloor);
        cross(returnU, arenaU, arenaFront + 5, terrace, arenaFloor);
        if (geometry.nextBoolean()) {
            room(-branchU, 4, 3, 12, -1, 5, true);
            cross(0, -branchU, 6, 0, -1);
        }
        if (geometry.nextBoolean()) {
            room(branchU, 4, hallV + 5, lowerV + 5, 0, 6, true);
            cross(hallU, branchU, hallV + 6, hallFloor, 0);
        }
        homes = new int[][] {{-5, 8, -4}, {6, 8, -4}, {-4, 2, 4}, {4, 2, 5},
            {0, 3, entranceEnd - 2}, {0, 3, entranceEnd + 4}, {hallU, 3, hallV - 8},
            {branchU, wingFloor + 2, entranceEnd}, {branchU, wingFloor + 2, hallV - 4},
            {hallU - 3, hallFloor + 2, hallV - 1}, {hallU + 3, hallFloor + 3, hallV + 3},
            {hallU, hallFloor + 4, hallV + 5}, {returnU, terrace + 2, hallV + 8},
            {returnU, terrace + 2, lowerV}, {arenaU, 3, arenaFront - 3}, {0, 3, 7},
            {branchU, wingFloor + 3, entranceEnd + 2}, {hallU - 3, hallFloor + 3, hallV + 5},
            {returnU, 4, arenaFront + 2}};
    }

    private Room room(int center, int half, int first, int last, int floor, int roof, boolean open) {
        Room room = new Room(center - half, center + half, first, last, floor, roof);
        modules.add(new Module(room, open));
        return room;
    }

    private void corridor(int a, int b, int first, int last, int aFloor, int bFloor) {
        int bend = (first + last) / 2;
        int floor = Math.min(aFloor, bFloor);
        int roof = Math.max(aFloor, bFloor) + 6;
        if (a == b) {
            link(new Room(a - 3, a + 3, first, last, floor, roof));
            return;
        }
        link(new Room(a - 3, a + 3, first, bend + 2, floor, roof));
        link(new Room(Math.min(a, b) - 3, Math.max(a, b) + 3, bend - 2, bend + 2, floor, roof));
        link(new Room(b - 3, b + 3, bend - 2, last, floor, roof));
    }

    private void cross(int a, int b, int z, int aFloor, int bFloor) {
        link(new Room(Math.min(a, b) - 1, Math.max(a, b) + 1, z - 2, z + 2,
                Math.min(aFloor, bFloor), Math.max(aFloor, bFloor) + 5));
    }

    private void link(Room room) {
        modules.add(new Module(room, false));
        openings.add(room);
    }

    @Override int revision() { return 2; }

    @Override int[] station(int slot) {
        int[] h = homes[slot];
        return position(h[0], h[1], h[2]);
    }

    @Override int[] arenaCorner(boolean far) {
        return far ? position(chamber.right - 1, chamber.roof - 1, chamber.last - 1)
                : position(chamber.left + 1, chamber.floor + 1, chamber.first + 1);
    }

    @Override int[] arenaEntry(boolean alternate) {
        if (!alternate) return position(arenaU, chamber.floor + 2, chamber.first);
        int edge = sideOfAnnex() < 0 ? chamber.left : chamber.right;
        return position(edge, chamber.floor + 2, chamber.first + 5);
    }

    private int sideOfAnnex() { return Integer.signum(homes[13][0] - arenaU); }

    @Override int[] arenaCenter() { return position(arenaU, chamber.floor + 3, chamber.first + 8); }

    @Override boolean coversColumn(int wx, int wz) {
        int u = across(wx, wz), v = along(wx, wz);
        if (v < 0) return v >= FIRST && Math.abs(u) <= 8;
        for (Module module : modules) if (module.bounds.covers(u, v)) return true;
        return false;
    }

    @Override int cell(int u, int h, int v, int floor) {
        if (v < FIRST || v > length || Math.abs(u) > width) return UNTOUCHED;
        if (h < LOW) {
            if (v < 0 || h <= floor) return UNTOUCHED;
            for (Module module : modules) if (module.bounds.covers(u, v)) return STONE;
            return UNTOUCHED;
        }
        if (h > HIGH) return UNTOUCHED;
        compose();
        int cell = voxels[index(u, h, v)];
        if (cell == UNTOUCHED && h > floor && h < 2 && v >= 0) {
            for (Module module : modules)
                if (module.bounds.covers(u, v) && h < module.bounds.floor) return STONE;
        }
        return cell;
    }

    private int index(int u, int h, int v) {
        return ((h - LOW) * (length - FIRST + 1) + v - FIRST) * (width * 2 + 1) + u + width;
    }

    private void compose() {
        if (voxels != null) return;
        voxels = new byte[(HIGH - LOW + 1) * (length - FIRST + 1) * (width * 2 + 1)];
        for (int h = LOW; h <= HIGH; h++) for (int v = FIRST; v <= length; v++)
            for (int u = -width; u <= width; u++) voxels[index(u, h, v)] = (byte) composeCell(u, h, v);
    }

    private int composeCell(int u, int h, int v) {
        int abs = Math.abs(u);
        if (v < 0) {
            if (abs <= 3) return h == 0 ? SHELL : h > 0 && h <= 8 ? WATER : UNTOUCHED;
            if ((abs == 5 || abs == 7) && (v == -2 || v == FIRST) && h >= 0 && h <= (u < 0 ? 5 : 7) + v / 2)
                return h == 0 ? SHELL : h == 3 && v == -2 ? LAMP : brick(u, h, v);
            return UNTOUCHED;
        }
        if (v == 0 && abs <= (h < 5 ? 3 : h < 7 ? 2 : 1) && h >= 1 && h <= 7) return WATER;

        for (Room door : openings)
            if (door.inside(u, v) && h > door.floor && h < door.roof) return WATER;

        boolean interior = false, shell = false, roof = false, floor = false;
        for (Module module : modules) {
            Room room = module.bounds;
            if (!room.covers(u, v) || h < room.floor || h > room.roof) continue;
            if (h > room.floor && h < room.roof && room.inside(u, v)) interior = true;
            else {
                shell = true;
                floor |= h == room.floor;
                if (h == room.roof) {
                    roof = true;
                    if (module.open && room.inside(u, v)
                            && TrenchRegion.field(id ^ 3517, u, v, 8) > 0.46) interior = true;
                }
            }
        }
        if (interior) {
            int hallOffset = Math.abs(u - hallU);
            if ((hallOffset == 6 || hallOffset == 7 && h <= 2) && v > hallV - 6 && v < hallV + 7
                    && (v - hallV + 6) % 4 == 0 && h >= 1 && h <= 9)
                return h == 3 ? LAMP : h == 1 ? SHELL : brick(u, h, v);
            if (v >= length - 5 && abs <= Math.abs(arenaU) + 4 && Math.abs(u - arenaU) <= 4 && h == chamber.floor + 1)
                return STONE;
            if ((v == 3 && abs == 6 || v == chamber.first + 2 && Math.abs(u - arenaU) == 8
                    || v == length - 4 && Math.abs(u - arenaU) == 7) && h == 3) return LAMP;
            return WATER;
        }
        if (!shell) {
            if (v == 0 && abs >= 4 && abs <= 7 && h >= 8 && h <= 11) return brick(u, h, v);
            return UNTOUCHED;
        }
        if (floor) {
            if ((u - v) % 7 == 0) return SHELL;
            return abs > 12 && TrenchRegion.field(id ^ 149, u, v, 7) > 0.65 ? SILT : STONE;
        }
        if (roof && v > 2 && v < hallV + 7 && abs > 4 && TrenchRegion.field(id ^ 418, u, v, 6) > 0.80)
            return WATER;
        if (h == 3 && (v == entranceEnd || v == chamber.first) && abs == 5) return LAMP;
        if (h >= 4 && h <= 6 && abs == 7 && v < entranceEnd && v % 4 == 2) return WATER;
        if (v == length && Math.abs(u - arenaU) <= 2 && h >= 3 && h <= 7) return ERODED;
        return brick(u, h, v);
    }

    private int brick(int u, int h, int v) {
        return TrenchRegion.field(id ^ 8253, u + 0.4 * h, v, 7) > 0.60 ? ERODED : BRICK;
    }

    @Override boolean connected(boolean alternate) {
        compose();
        boolean[] seen = new boolean[voxels.length];
        ArrayDeque<int[]> open = new ArrayDeque<int[]>();
        open.add(new int[] {0, 2, -3});
        seen[index(0, 2, -3)] = true;
        boolean reachesHall = false, reachesArena = false;
        int[][] steps = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        while (!open.isEmpty()) {
            int[] p = open.remove();
            reachesHall |= p[0] == hallU && p[2] == hallV;
            reachesArena |= p[0] == arenaU && p[2] == chamber.first + 8;
            if (reachesHall && reachesArena) return true;
            for (int[] step : steps) {
                int u = p[0] + step[0], h = p[1] + step[1], v = p[2] + step[2];
                if (Math.abs(u) > width || h < LOW || h >= HIGH || v < FIRST || v > length
                        || alternate && v == chamber.first && Math.abs(u - arenaU) < 7) continue;
                int i = index(u, h, v);
                if (seen[i] || voxels[i] != WATER || voxels[index(u, h + 1, v)] != WATER) continue;
                seen[i] = true;
                open.add(new int[] {u, h, v});
            }
        }
        return false;
    }

    private static final class Module {
        final Room bounds;
        final boolean open;

        Module(Room bounds, boolean open) {
            this.bounds = bounds;
            this.open = open;
        }
    }
}
