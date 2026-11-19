package zed.tekkitoverhaul.settlement;

import net.minecraft.src.Block;
import net.minecraft.src.World;

/** Bounded, current geometry for a standing-sized observer on the world thread. */
public final class ProtectedInterior {
    public enum State { PROTECTED, EXPOSED, UNKNOWN }
    public enum Evidence { YES, NO, UNKNOWN }

    public static final class Assessment {
        public final int x, y, z, reads;
        public final State state;
        public final Evidence overhead, enclosure, exteriorRoute;
        public final String reason;

        Assessment(int x, int y, int z, int reads, State state, Evidence overhead, Evidence enclosure,
                   Evidence exteriorRoute, String reason) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.reads = reads;
            this.state = state;
            this.overhead = overhead;
            this.enclosure = enclosure;
            this.exteriorRoute = exteriorRoute;
            this.reason = reason;
        }
    }

    private static final int RADIUS = 4, WIDTH = 9, HEIGHT = 9, LOW = -2, HIGH = 2;
    static final int READ_LIMIT = 169;
    private static final int AIR = 1, FULL = 2, TOP_SLAB = 3, LOW_SLAB = 4, LADDER = 5, WALL = 6,
            UNCERTAIN = 7, DOOR_TOP = 8, DOOR_BOTTOM = 9, GATE = 10;
    private static final int[] DX = {-1, 1, 0, 0}, DZ = {0, 0, -1, 1};
    private final short[] cells = new short[WIDTH * WIDTH * HEIGHT];
    private final boolean[] visited = new boolean[cells.length];
    private final int[] queue = new int[cells.length];
    private int x, y, z, reads, tail;
    private boolean uncertain, boundary;
    private World world;

    static Assessment unknown(int x, int y, int z, String reason) {
        return new Assessment(x, y, z, 0, State.UNKNOWN, Evidence.UNKNOWN, Evidence.UNKNOWN, Evidence.UNKNOWN, reason);
    }

    Assessment assess(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        reads = tail = 0;
        uncertain = boundary = false;
        java.util.Arrays.fill(cells, (short) 0);
        java.util.Arrays.fill(visited, false);
        Evidence overhead = Evidence.UNKNOWN;
        try {
            overhead = overhead(0, 0, 0);
            if (!occupiable(0, 0, 0, 0, 0)) {
                return result(State.UNKNOWN, overhead, Evidence.UNKNOWN, Evidence.UNKNOWN, "Point lacks clear standing space");
            }
            if (overhead == Evidence.NO) {
                return result(State.EXPOSED, overhead, Evidence.UNKNOWN, Evidence.UNKNOWN, "Open overhead within local window");
            }
            enqueue(0, 0, 0);
            for (int head = 0; head < tail; head++) {
                int i = queue[head], px = i % WIDTH - RADIUS, pz = i / WIDTH % WIDTH - RADIUS, py = i / (WIDTH * WIDTH) + LOW;
                if (overhead(px, py, pz) == Evidence.NO) {
                    return result(State.EXPOSED, overhead, Evidence.NO, Evidence.YES, "Passable route to uncovered space");
                }
                for (int direction = 0; direction < DX.length; direction++) {
                    int dx = DX[direction], dz = DZ[direction];
                    walk(px, py, pz, dx, dz);
                }
                climb(px, py, pz, -1);
                climb(px, py, pz, 1);
            }
            if (boundary || uncertain || overhead == Evidence.UNKNOWN) {
                return result(State.UNKNOWN, overhead, Evidence.UNKNOWN, Evidence.UNKNOWN,
                        boundary ? "Covered route leaves local search window" : "Ambiguous collision geometry");
            }
            return result(State.PROTECTED, overhead, Evidence.YES, Evidence.NO, "Cover and no passable local exterior route");
        } catch (Incomplete incomplete) {
            return result(State.UNKNOWN, overhead, Evidence.UNKNOWN, Evidence.UNKNOWN, incomplete.getMessage());
        } finally {
            this.world = null;
        }
    }

    private void walk(int x, int y, int z, int dx, int dz) {
        if (!passable(x, y, z, dx, dz)) return;
        int nx = x + dx, nz = z + dz;
        if (Math.abs(nx) > RADIUS || Math.abs(nz) > RADIUS) {
            boundary = true;
        } else if (occupiable(nx, y, nz, dx, dz)) {
            enqueue(nx, y, nz);
        } else if (supported(nx, y, nz) && passable(x, y + 2, z, dx, dz)) {
            if (occupiable(nx, y + 1, nz, dx, dz)) enqueue(nx, y + 1, nz);
        } else if (passable(nx, y, nz, dx, dz) && passable(nx, y + 1, nz, dx, dz)) {
            if (y == LOW) boundary = true;
            else if (occupiable(nx, y - 1, nz, dx, dz)) enqueue(nx, y - 1, nz);
            else if (passable(nx, y - 1, nz, dx, dz)) uncertain = true;
        }
    }

    private void climb(int x, int y, int z, int dy) {
        if (kind(cell(x, y, z)) != LADDER
                && (y + dy < LOW || y + dy > HIGH || kind(cell(x, y + dy, z)) != LADDER)) return;
        if (y + dy < LOW || y + dy > HIGH) {
            boundary = true;
        } else if (passable(x, y + dy, z, 0, 0) && passable(x, y + dy + 1, z, 0, 0)) {
            enqueue(x, y + dy, z);
        }
    }

    private void enqueue(int x, int y, int z) {
        if (y < LOW || y > HIGH) {
            boundary = true;
            return;
        }
        int i = index(x, y, z);
        if (!visited[i]) {
            visited[i] = true;
            queue[tail++] = i;
        }
    }

    private Evidence overhead(int x, int y, int z) {
        for (int dy = 2; dy <= 4; dy++) {
            int kind = kind(cell(x, y + dy, z));
            if (kind == FULL || kind == TOP_SLAB || kind == LOW_SLAB) return Evidence.YES;
            if (kind == UNCERTAIN || kind == WALL || kind == GATE || kind == DOOR_TOP || kind == DOOR_BOTTOM) {
                uncertain = true;
                return Evidence.UNKNOWN;
            }
        }
        return Evidence.NO;
    }

    private boolean occupiable(int x, int y, int z, int dx, int dz) {
        return passable(x, y, z, dx, dz) && passable(x, y + 1, z, dx, dz)
                && (kind(cell(x, y, z)) == LADDER || supported(x, y - 1, z));
    }

    private boolean supported(int x, int y, int z) {
        int kind = kind(cell(x, y, z));
        if (kind == LOW_SLAB || kind == UNCERTAIN) uncertain = true;
        return kind == FULL || kind == TOP_SLAB;
    }

    private boolean passable(int x, int y, int z, int dx, int dz) {
        int value = cell(x, y, z), kind = kind(value);
        if (kind == AIR || kind == LADDER) return true;
        if (kind == LOW_SLAB || kind == UNCERTAIN) uncertain = true;
        if (kind == GATE) return (value >> 4 & 4) != 0;
        if (kind != DOOR_TOP && kind != DOOR_BOTTOM) return false;
        int lower = kind == DOOR_TOP ? cell(x, y - 1, z) : value;
        int upper = kind == DOOR_BOTTOM ? cell(x, y + 1, z) : value;
        if (kind(lower) != DOOR_BOTTOM || kind(upper) != DOOR_TOP || ((lower ^ upper) >> 8) != 0) {
            uncertain = true;
            return false;
        }
        int metadata = lower >> 4;
        // In 1.2.5 an open panel sits against the side perpendicular to the closed panel.
        return (metadata & 4) != 0 && ((metadata & 1) == 0 ? dz == 0 : dx == 0);
    }

    private int cell(int x, int y, int z) {
        if (y < LOW || y >= LOW + HEIGHT) throw Incomplete.WINDOW;
        int index = index(x, y, z);
        if (cells[index] != 0) return cells[index];
        int wx = this.x + x, wy = this.y + y, wz = this.z + z;
        if (!ProvenanceStore.valid(wx, wy, wz) || !world.blockExists(wx, wy, wz)) throw Incomplete.TERRAIN;
        spend();
        int id = world.getBlockId(wx, wy, wz);
        int shape;
        if (id == 64 || id == 71 || id == 107 || id == 44) {
            spend();
            int meta = world.getBlockMetadata(wx, wy, wz);
            if (id == 64 || id == 71) {
                shape = ((id == 64 ? 1 : 2) << 8) | meta << 4 | ((meta & 8) == 0 ? DOOR_BOTTOM : DOOR_TOP);
            } else if (id == 107) {
                shape = meta << 4 | GATE;
            } else {
                shape = (meta & 8) != 0 ? TOP_SLAB : LOW_SLAB;
            }
        } else if (id == 0 || id == 50 || id == 75 || id == 76) {
            shape = AIR;
        } else if (id == 65) {
            shape = LADDER;
        } else if (id == 85 || id == 113) {
            shape = WALL;
        } else {
            Block block = id > 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
            shape = id == 20 || id == 43 || (block != null && block.isOpaqueCube() && block.renderAsNormalBlock())
                    ? FULL : UNCERTAIN;
        }
        cells[index] = (short) shape;
        return shape;
    }

    private void spend() {
        if (reads == READ_LIMIT) throw Incomplete.BUDGET;
        reads++;
    }

    private static int kind(int shape) { return shape & 15; }
    private static int index(int x, int y, int z) { return ((y - LOW) * WIDTH + z + RADIUS) * WIDTH + x + RADIUS; }

    private Assessment result(State state, Evidence overhead, Evidence enclosure, Evidence exteriorRoute, String reason) {
        return new Assessment(x, y, z, reads, state, overhead, enclosure, exteriorRoute, reason);
    }

    private static final class Incomplete extends RuntimeException {
        static final Incomplete TERRAIN = new Incomplete("Terrain unavailable");
        static final Incomplete BUDGET = new Incomplete("Protection read budget exhausted");
        static final Incomplete WINDOW = new Incomplete("Vertical route leaves local search window");

        private Incomplete(String message) { super(message, null, false, false); }
    }
}
