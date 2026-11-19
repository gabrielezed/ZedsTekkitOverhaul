package zed.tekkitoverhaul.settlement;

import net.minecraft.src.Block;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;

/** One local survey, one loaded slice per update, then one bounded component pass. */
final class SiteSurvey {
    static final int RADIUS = 6, WIDTH = 2 * RADIUS + 1, LAYERS = 9;
    static final int SLICE = WIDTH * WIDTH, VOLUME = LAYERS * SLICE;
    private static final int SOLID = 1, MANUAL = 2, INFERRED = 4, VISITED = 8;
    private static final int CONSTRUCTION = MANUAL | INFERRED;
    static final int WEST = 1, EAST = 2, BELOW = 4, ABOVE = 8, NORTH = 16, SOUTH = 32;
    private static final String[] FACE_NAMES = {"west", "east", "below", "above", "north", "south"};
    private final byte[] blocks = new byte[VOLUME];
    private final byte[] devices = new byte[VOLUME];
    private final int[] membership = new int[VOLUME];
    private final int[] queue = new int[VOLUME];
    int x, y, z, minY, layer, reads, focusX, focusY, focusZ;
    boolean followUp;
    boolean active, changed;

    void start(int x, int y, int z) {
        focusX = x;
        focusY = y;
        focusZ = z;
        followUp = false;
        center(x, y, z);
    }

    private void center(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        minY = Math.max(0, Math.min(256 - LAYERS, y - 2));
        layer = 0;
        changed = false;
        active = true;
    }

    void observeMutation(int px, int py, int pz) {
        if (active && py >= minY && py < minY + layer
                && Math.abs(px - x) <= RADIUS && Math.abs(pz - z) <= RADIUS) changed = true;
    }

    boolean near(int px, int py, int pz, int horizontal, int vertical) {
        return Math.abs(focusX - px) <= horizontal && Math.abs(focusY - py) <= vertical
                && Math.abs(focusZ - pz) <= horizontal;
    }

    boolean recenter(Evidence evidence) {
        if (followUp || !evidence.spread() || (evidence.manual < 6 && evidence.inferred < 12)) return false;
        Bounds box = evidence.bounds;
        int nx = shift(x, box.minX, box.maxX, evidence.faces, WEST, EAST, 3, WIDTH);
        int ny = shift(y, box.minY, box.maxY, evidence.faces, BELOW, ABOVE, 2, LAYERS);
        int nz = shift(z, box.minZ, box.maxZ, evidence.faces, NORTH, SOUTH, 3, WIDTH);
        if (nx == x && ny == y && nz == z) return false;
        if (!ProvenanceStore.valid(nx - RADIUS, ny, nz - RADIUS)
                || !ProvenanceStore.valid(nx + RADIUS, ny, nz + RADIUS)) return false;
        followUp = true;
        center(nx, ny, nz);
        return true;
    }

    private static int shift(int axis, int min, int max, int faces, int low, int high, int limit, int span) {
        int clipped = faces & (low | high);
        if (clipped == 0 || clipped == (low | high) || max - min + 1 > span - 2) return axis;
        // The observed midpoint safely moves the window toward the clipped face.
        int delta = min + (max - min) / 2 - axis;
        if (clipped == low) return axis + Math.min(-1, Math.max(-limit, delta));
        return axis + Math.max(1, Math.min(limit, delta));
    }

    Bounds extent() {
        return new Bounds(x - RADIUS, minY, z - RADIUS, x + RADIUS, minY + LAYERS - 1, z + RADIUS);
    }

    boolean completelyCovers(Bounds box) {
        return box.minX > x - RADIUS && box.maxX < x + RADIUS && box.minZ > z - RADIUS && box.maxZ < z + RADIUS
                && box.minY > minY && box.maxY < minY + LAYERS - 1;
    }

    boolean constructionRemains(Bounds box) {
        for (int py = box.minY; py <= box.maxY; py++) {
            for (int pz = box.minZ; pz <= box.maxZ; pz++) {
                int index = (py - minY) * SLICE + (pz - z + RADIUS) * WIDTH + box.minX - x + RADIUS;
                for (int px = box.minX; px <= box.maxX; px++) {
                    if ((blocks[index++] & CONSTRUCTION) != 0) return true;
                }
            }
        }
        return false;
    }

    /** Check each intersecting chunk before a block read. A partial survey yields uncertainty. */
    boolean sample(World world, WorldProvenance provenance) {
        reads = 0;
        int left = x - RADIUS, front = z - RADIUS;
        int height = minY + layer;
        for (int cx = left >> 4; cx <= (x + RADIUS) >> 4; cx++) {
            for (int cz = front >> 4; cz <= (z + RADIUS) >> 4; cz++) {
                if (!world.blockExists(cx << 4, height, cz << 4)) return false;
            }
        }
        int index = layer * SLICE;
        for (int pz = front; pz < front + WIDTH; pz++) {
            for (int px = left; px < left + WIDTH; px++) {
                int id = world.getBlockId(px, height, pz);
                reads++;
                devices[index] = (byte) utilityType(id);
                int flags = 0;
                Block block = id > 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
                if (block != null && block.blockMaterial.isSolid()) {
                    flags |= SOLID;
                    if (provenance.observation(world, px, height, pz) == id) flags |= MANUAL;
                    else if (builtMaterial(id)) flags |= INFERRED;
                }
                blocks[index++] = (byte) flags;
            }
        }
        layer++;
        return true;
    }

    int cover(Evidence evidence, int px, int feetY, int pz) {
        int columns = 0;
        int dx = px - x + RADIUS, dz = pz - z + RADIUS;
        for (int row = Math.max(0, dz - 1); row <= Math.min(WIDTH - 1, dz + 1); row++) {
            for (int column = Math.max(0, dx - 1); column <= Math.min(WIDTH - 1, dx + 1); column++) {
                for (int h = Math.max(0, feetY + 2 - minY); h < LAYERS; h++) {
                    int index = h * SLICE + row * WIDTH + column;
                    if ((blocks[index] & SOLID) != 0) {
                        // The first solid hit can be terrain or the selected structure.
                        if ((blocks[index] & CONSTRUCTION) == 0 || membership[index] == evidence.component)
                            columns++;
                        break;
                    }
                }
            }
        }
        return columns;
    }

    Evidence finish() {
        active = false;
        Evidence best = new Evidence();
        for (int i = 0; i < VOLUME; i++) {
            if ((blocks[i] & CONSTRUCTION) == 0 || (blocks[i] & VISITED) != 0) continue;
            Evidence component = component(i);
            // Match the place being used. Remote components cannot borrow the player's activity.
            if (!component.bounds.contains(focusX, focusY, focusZ, 1)) continue;
            if (component.rank() > best.rank()) best = component;
        }
        for (int i = 0; i < VOLUME; i++) {
            int type = devices[i];
            if (type == 0) continue;
            best.found[type]++;
            if (associated(best, i)) best.associated[type]++;
        }
        best.anchorCover = cover(best, x, focusY, z);
        return best;
    }

    boolean associatedUtility(Evidence evidence, int px, int py, int pz, int type) {
        int dx = px - x + RADIUS, dz = pz - z + RADIUS, h = py - minY;
        if (dx < 0 || dx >= WIDTH || dz < 0 || dz >= WIDTH || h < 0 || h >= LAYERS) return false;
        int index = h * SLICE + dz * WIDTH + dx;
        return devices[index] == type && associated(evidence, index);
    }

    boolean walkable(int px, int py, int pz) {
        int dx = px - x + RADIUS, dz = pz - z + RADIUS, h = py - minY;
        if (dx < 0 || dx >= WIDTH || dz < 0 || dz >= WIDTH || h <= 0 || h + 1 >= LAYERS) return false;
        int index = h * SLICE + dz * WIDTH + dx;
        return (blocks[index - SLICE] & SOLID) != 0 && ((blocks[index] | blocks[index + SLICE]) & SOLID) == 0;
    }

    private boolean associated(Evidence evidence, int index) {
        if (evidence.component == 0 || Math.abs(minY + index / SLICE - focusY) > 1) return false;
        if ((blocks[index] & CONSTRUCTION) != 0 && membership[index] == evidence.component) return true;
        return index >= SLICE && (blocks[index - SLICE] & CONSTRUCTION) != 0
                && membership[index - SLICE] == evidence.component;
    }

    private Evidence component(int start) {
        Evidence evidence = new Evidence();
        evidence.component = start + 1;
        int head = 0, tail = enqueue(start, 0);
        int minX = WIDTH, minZ = WIDTH, minH = LAYERS, maxX = 0, maxH = 0, maxZ = 0;
        while (head < tail) {
            int index = queue[head++];
            membership[index] = evidence.component;
            int dx = index % WIDTH, dz = index / WIDTH % WIDTH, h = index / SLICE;
            minX = Math.min(minX, dx);
            minH = Math.min(minH, h);
            minZ = Math.min(minZ, dz);
            maxX = Math.max(maxX, dx);
            maxH = Math.max(maxH, h);
            maxZ = Math.max(maxZ, dz);
            if ((blocks[index] & MANUAL) != 0) evidence.manual++;
            if ((blocks[index] & INFERRED) != 0) evidence.inferred++;
            if (dx > 0) tail = enqueue(index - 1, tail);
            if (dx < WIDTH - 1) tail = enqueue(index + 1, tail);
            if (dz > 0) tail = enqueue(index - WIDTH, tail);
            if (dz < WIDTH - 1) tail = enqueue(index + WIDTH, tail);
            if (h > 0) tail = enqueue(index - SLICE, tail);
            if (h < LAYERS - 1) tail = enqueue(index + SLICE, tail);
        }
        if (minX == 0) evidence.faces |= WEST;
        if (maxX == WIDTH - 1) evidence.faces |= EAST;
        if (minZ == 0) evidence.faces |= NORTH;
        if (maxZ == WIDTH - 1) evidence.faces |= SOUTH;
        if (minH == 0 && minY > 0) evidence.faces |= BELOW;
        if (maxH == LAYERS - 1 && minY + LAYERS < 256) evidence.faces |= ABOVE;
        int bottom = minY + minH, top = minY + maxH;
        // Include the occupied layer immediately above a floor, or below a low roof.
        if (focusY == top + 1) top = focusY;
        if (focusY == bottom - 1) bottom = focusY;
        evidence.bounds = new Bounds(x - RADIUS + minX, bottom, z - RADIUS + minZ,
                x - RADIUS + maxX, top, z - RADIUS + maxZ);
        return evidence;
    }

    private int enqueue(int index, int tail) {
        if ((blocks[index] & CONSTRUCTION) == 0 || (blocks[index] & VISITED) != 0) return tail;
        blocks[index] |= VISITED;
        queue[tail] = index;
        return tail + 1;
    }

    static boolean utility(int id) {
        return utilityType(id) != 0;
    }

    static int utilityType(int id) {
        if (id == Block.workbench.blockID) return 1;
        if (id == Block.stoneOvenIdle.blockID || id == Block.stoneOvenActive.blockID) return 2;
        return id == Block.chest.blockID ? 3 : 0;
    }

    static String clipping(int faces) {
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < FACE_NAMES.length; i++) {
            if ((faces & (1 << i)) == 0) continue;
            if (label.length() != 0) label.append(", ");
            label.append(FACE_NAMES[i]);
        }
        return label.length() == 0 ? "None" : label.toString();
    }

    private static boolean builtMaterial(int id) {
        return id == Block.planks.blockID || id == Block.cobblestone.blockID || id == Block.brick.blockID
                || id == Block.stoneBrick.blockID || id == Block.glass.blockID
                || id == Block.stairDouble.blockID || id == Block.stairSingle.blockID || utility(id);
    }

    static final class Evidence {
        Bounds bounds;
        int manual, inferred, anchorCover, faces, component;
        final int[] found = new int[4], associated = new int[4];

        boolean spread() {
            return bounds != null && bounds.maxX - bounds.minX >= 2 && bounds.maxZ - bounds.minZ >= 2;
        }

        int rank() {
            int blocks = manual + inferred;
            return spread() ? VOLUME + Math.min(manual, 24) * VOLUME + blocks : blocks;
        }
    }
}
