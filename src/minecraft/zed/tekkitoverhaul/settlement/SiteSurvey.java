package zed.tekkitoverhaul.settlement;

import net.minecraft.src.Block;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteRegistry.Bounds;

/** One local survey, one loaded slice per update, then one bounded component pass. */
final class SiteSurvey {
    static final int RADIUS = 6, WIDTH = 2 * RADIUS + 1, LAYERS = 9;
    static final int SLICE = WIDTH * WIDTH, VOLUME = LAYERS * SLICE;
    private static final int SOLID = 1, MANUAL = 2, INFERRED = 4, UTILITY = 8, VISITED = 16;
    private static final int CONSTRUCTION = MANUAL | INFERRED;
    private final byte[] blocks = new byte[VOLUME];
    private final int[] queue = new int[VOLUME];
    int x, y, z, minY, layer, reads;
    boolean active;

    void start(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        minY = Math.max(0, Math.min(256 - LAYERS, y - 2));
        layer = 0;
        active = true;
    }

    boolean near(int px, int py, int pz, int horizontal, int vertical) {
        return Math.abs(x - px) <= horizontal && Math.abs(y - py) <= vertical && Math.abs(z - pz) <= horizontal;
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
                int flags = utility(id) ? UTILITY : 0;
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

    private int cover() {
        int columns = 0;
        for (int dz = RADIUS - 1; dz <= RADIUS + 1; dz++) {
            for (int dx = RADIUS - 1; dx <= RADIUS + 1; dx++) {
                for (int h = y + 2 - minY; h < LAYERS; h++) {
                    if ((blocks[h * SLICE + dz * WIDTH + dx] & SOLID) != 0) {
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
        best.cover = cover();
        for (int i = 0; i < VOLUME; i++) {
            if ((blocks[i] & CONSTRUCTION) == 0 || (blocks[i] & VISITED) != 0) continue;
            Evidence component = component(i, best.cover);
            // Match the place being used. Remote components cannot borrow the player's activity.
            if (!component.bounds.contains(x, y, z, 1)) continue;
            if (component.rank() > best.rank()) best = component;
        }
        return best;
    }

    boolean sampledUtility(int px, int py, int pz) {
        int dx = px - x + RADIUS, dz = pz - z + RADIUS, h = py - minY;
        return dx >= 0 && dx < WIDTH && dz >= 0 && dz < WIDTH && h >= 0 && h < LAYERS
                && (blocks[h * SLICE + dz * WIDTH + dx] & UTILITY) != 0;
    }

    private Evidence component(int start, int cover) {
        Evidence evidence = new Evidence();
        evidence.cover = cover;
        int head = 0, tail = enqueue(start, 0);
        int minX = WIDTH, minZ = WIDTH, minH = LAYERS, maxX = 0, maxH = 0, maxZ = 0;
        while (head < tail) {
            int index = queue[head++];
            int dx = index % WIDTH, dz = index / WIDTH % WIDTH, h = index / SLICE;
            minX = Math.min(minX, dx);
            minH = Math.min(minH, h);
            minZ = Math.min(minZ, dz);
            maxX = Math.max(maxX, dx);
            maxH = Math.max(maxH, h);
            maxZ = Math.max(maxZ, dz);
            if ((blocks[index] & MANUAL) != 0) evidence.manual++;
            if ((blocks[index] & INFERRED) != 0) evidence.inferred++;
            if ((blocks[index] & UTILITY) != 0) evidence.utility++;
            if (dx > 0) tail = enqueue(index - 1, tail);
            if (dx < WIDTH - 1) tail = enqueue(index + 1, tail);
            if (dz > 0) tail = enqueue(index - WIDTH, tail);
            if (dz < WIDTH - 1) tail = enqueue(index + WIDTH, tail);
            if (h > 0) tail = enqueue(index - SLICE, tail);
            if (h < LAYERS - 1) tail = enqueue(index + SLICE, tail);
        }
        evidence.clipped = minX == 0 || maxX == WIDTH - 1 || minZ == 0 || maxZ == WIDTH - 1
                || (minH == 0 && minY > 0) || (maxH == LAYERS - 1 && minY + LAYERS < 256);
        int bottom = minY + minH, top = minY + maxH;
        // Include the occupied layer immediately above a floor, or below a low roof.
        if (y == top + 1) top = y;
        if (y == bottom - 1) bottom = y;
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
        return id == Block.workbench.blockID || id == Block.chest.blockID
                || id == Block.stoneOvenIdle.blockID || id == Block.stoneOvenActive.blockID;
    }

    private static boolean builtMaterial(int id) {
        return id == Block.planks.blockID || id == Block.cobblestone.blockID || id == Block.brick.blockID
                || id == Block.stoneBrick.blockID || id == Block.glass.blockID
                || id == Block.stairDouble.blockID || id == Block.stairSingle.blockID || utility(id);
    }

    static final class Evidence {
        Bounds bounds;
        int manual, inferred, utility, cover;
        boolean clipped;

        boolean spread() {
            return bounds != null && bounds.maxX - bounds.minX >= 2 && bounds.maxZ - bounds.minZ >= 2;
        }

        int rank() {
            int blocks = manual + inferred;
            return spread() ? VOLUME + Math.min(manual, 24) * VOLUME + blocks : blocks;
        }
    }
}
