package zed.tekkitoverhaul.settlement;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.src.Block;
import net.minecraft.src.IInventory;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;
import zed.tekkitoverhaul.settlement.SiteRegistry.Part;

/** One local survey, one loaded slice per update, then one bounded component pass. */
final class SiteSurvey {
    static final int RADIUS = 6, WIDTH = 2 * RADIUS + 1, LAYERS = 9;
    static final int SLICE = WIDTH * WIDTH, VOLUME = LAYERS * SLICE;
    private static final int SOLID = 1, MANUAL = 2, INFERRED = 4, VISITED = 8, PLACED = 16, OCCUPIED = 64;
    private static final int LEGACY_PLACED_SOLID = 128;
    private static final byte SUPPORT = 1, ENDPOINT = 2, INVENTORY = 3;
    private static final int CONSTRUCTION = MANUAL | INFERRED;
    static final int WEST = 1, EAST = 2, BELOW = 4, ABOVE = 8, NORTH = 16, SOUTH = 32;
    private static final String[] FACE_NAMES = {"west", "east", "below", "above", "north", "south"};
    private final byte[] blocks = new byte[VOLUME];
    private final byte[] devices = new byte[VOLUME];
    private final short[] ids = new short[VOLUME];
    private final int[] membership = new int[VOLUME];
    private final int[] queue = new int[VOLUME];
    private final TileEntity[] tiles = new TileEntity[VOLUME];
    private final byte[] industrial = new byte[VOLUME], faces = new byte[VOLUME];
    private final byte[] functional = new byte[VOLUME];
    private int cursor, cableCount, unsupported;
    private int genericReads, ic2Reads;
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
        cursor = 0;
        cableCount = 0;
        unsupported = 0;
        genericReads = 0;
        ic2Reads = 0;
        Arrays.fill(tiles, null);
        changed = false;
        active = true;
    }

    void cancel() {
        active = false;
        Arrays.fill(tiles, null);
    }

    void observeMutation(int px, int py, int pz) {
        if (active && py >= minY && py < minY + layer + (cursor > 0 ? 1 : 0)
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
                    if ((blocks[index++] & (CONSTRUCTION | LEGACY_PLACED_SOLID)) != 0) return true;
                }
            }
        }
        return false;
    }

    boolean legacyConstructionOutside(Bounds stored, Bounds current) {
        for (int py = stored.minY; py <= stored.maxY; py++) {
            for (int pz = stored.minZ; pz <= stored.maxZ; pz++) {
                int index = (py - minY) * SLICE + (pz - z + RADIUS) * WIDTH + stored.minX - x + RADIUS;
                for (int px = stored.minX; px <= stored.maxX; px++) {
                    if ((blocks[index++] & LEGACY_PLACED_SOLID) != 0 && !current.contains(px, py, pz)) return true;
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
        Ic2SiteAdapter adapter = Ic2SiteAdapter.get();
        while (cursor < SLICE && reads < SLICE) {
            int index = layer * SLICE + cursor;
            int px = left + cursor % WIDTH, pz = front + cursor / WIDTH;
            int id = world.getBlockId(px, height, pz);
            reads++;
            int flags = BlockEvidence.flags(id);
            int utility = utilityType(id);
            boolean enrich = adapter.inspect(id);
            boolean probe = enrich || utility == 0 && (flags & BlockEvidence.TILE) != 0;
            boolean variant = (flags & BlockEvidence.TILE_VARIANT) != 0;
            boolean metadata = enrich || probe && variant;
            if (probe && reads + 3 + (metadata ? 1 : 0) > SLICE) return true;
            cursor++;
            ids[index] = (short) id;
            devices[index] = (byte) utility;
            industrial[index] = 0;
            blocks[index] = 0;
            functional[index] = 0;
            if (id == 0) continue;
            int meta = 0;
            if (metadata) {
                meta = world.getBlockMetadata(px, height, pz);
                reads++;
                if (variant) genericReads++;
                else ic2Reads++;
            }
            boolean equipmentShape = probe && (!variant || BlockEvidence.hasTile(id, meta));
            TileEntity tile = null;
            if (equipmentShape) {
                tile = BlockEvidence.loadedTile(world, px, height, pz);
                reads += 2;
                genericReads += 2;
                tiles[index] = tile;
            }
            boolean full = (flags & BlockEvidence.FULL) != 0;
            boolean verified = provenance.observation(world, px, height, pz) == id;
            if (verified) blocks[index] |= PLACED;
            if (tile != null) {
                boolean inventory = false;
                if (tile instanceof IInventory) {
                    inventory = BlockEvidence.inventory(tile);
                    reads++;
                    genericReads++;
                }
                functional[index] = inventory ? INVENTORY : full ? ENDPOINT : SUPPORT;
            }
            if (adapter.family(id)) {
                if (enrich) {
                    int type = adapter.type(id, meta, tile);
                    if (type == Ic2SiteAdapter.CABLE) {
                        int color = adapter.cableColor(meta, tile);
                        if (color >= 0) {
                            industrial[index] = (byte) (color + 1);
                            cableCount++;
                        }
                    } else if (type > 0) {
                        industrial[index] = (byte) -type;
                        faces[index] = (byte) adapter.outputFace(type, tile);
                    }
                }
                if (industrial[index] == 0) unsupported++;
            }
            boolean built = builtMaterial(id, utility);
            // Equipment contributes separately. Its registered shape can still prevent counting a walked column.
            if (full && equipmentShape && !built) blocks[index] |= OCCUPIED;
            if (built || (full && !equipmentShape)) {
                blocks[index] |= SOLID;
                if (verified) blocks[index] |= MANUAL;
                else if (built) blocks[index] |= INFERRED;
            } else if (verified && !adapter.family(id) && (flags & BlockEvidence.SOLID_MATERIAL) != 0) {
                // Prior releases could include these shapes as manual construction. Respect stored bounds.
                blocks[index] |= LEGACY_PLACED_SOLID;
            }
        }
        if (cursor == SLICE) {
            layer++;
            cursor = 0;
        }
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
        if (!best.spread()) {
            Evidence platform = contextualPlatform();
            if (platform != null) best = platform;
        }
        for (int i = 0; i < VOLUME; i++) {
            int type = devices[i];
            if (tiles[i] != null && tiles[i].isInvalid()) {
                if (industrial[i] > 0) cableCount--;
                industrial[i] = 0;
                functional[i] = 0;
                tiles[i] = null;
            }
            if ((blocks[i] & PLACED) != 0) best.verified++;
            if ((blocks[i] & (SOLID | CONSTRUCTION)) == SOLID) best.unattributed++;
            if (functional[i] != 0) best.found[GENERIC]++;
            if (industrial[i] < 0) {
                best.found[IC2]++;
                if (associated(best, i)) {
                    best.associated[IC2]++;
                    best.machineTypes |= 1 << -industrial[i];
                    if ((blocks[i] & PLACED) != 0) best.placedMachines++;
                }
            }
            if (type == 0) continue;
            best.found[type]++;
            if (associated(best, i)) best.associated[type]++;
        }
        if (best.found[GENERIC] > 0) observeFunction(best);
        best.genericReads = genericReads;
        best.ic2Reads = ic2Reads;
        best.cables = cableCount;
        best.unsupported = unsupported;
        if (cableCount > 0 && best.associated[IC2] >= 2) observeNetworks(best);
        best.anchorCover = cover(best, x, focusY, z);
        return best;
    }

    boolean associatedUtility(Evidence evidence, int px, int py, int pz, int type, int id, TileEntity tile) {
        int dx = px - x + RADIUS, dz = pz - z + RADIUS, h = py - minY;
        if (dx < 0 || dx >= WIDTH || dz < 0 || dz >= WIDTH || h < 0 || h >= LAYERS) return false;
        int index = h * SLICE + dz * WIDTH + dx;
        if (type == GENERIC) {
            return ids[index] == id && tiles[index] == tile && (tile == null || !tile.isInvalid())
                    && (direct(evidence, index) || nearEndpoint(evidence, index));
        }
        return (type == IC2 ? industrial[index] < 0 && tile != null && tiles[index] == tile && !tile.isInvalid()
                : devices[index] == type) && associated(evidence, index);
    }

    boolean walkable(int px, int py, int pz) {
        int dx = px - x + RADIUS, dz = pz - z + RADIUS, h = py - minY;
        if (dx < 0 || dx >= WIDTH || dz < 0 || dz >= WIDTH || h <= 0 || h + 1 >= LAYERS) return false;
        int index = h * SLICE + dz * WIDTH + dx;
        return (blocks[index - SLICE] & SOLID) != 0
                && ((blocks[index] | blocks[index + SLICE]) & (SOLID | OCCUPIED)) == 0
                && industrial[index] >= 0 && industrial[index + SLICE] >= 0;
    }

    boolean walkedOn(Evidence evidence, int px, int py, int pz) {
        return member(evidence, index(px, py - 1, pz));
    }

    private int index(int px, int py, int pz) {
        return (py - minY) * SLICE + (pz - z + RADIUS) * WIDTH + px - x + RADIUS;
    }

    private static int offset(Bounds box, int px, int py, int pz) {
        int width = box.maxX - box.minX + 1, depth = box.maxZ - box.minZ + 1;
        return ((py - box.minY) * depth + pz - box.minZ) * width + px - box.minX;
    }

    /** A continuation needs common construction cells in the same current face-connected component. */
    int sharedConstruction(Evidence evidence, Part part) {
        Bounds box = part.bounds;
        Bounds overlap = intersection(evidence.bounds, box);
        if (overlap == null) return 0;
        int shared = 0;
        for (int py = overlap.minY; py <= overlap.maxY; py++)
            for (int pz = overlap.minZ; pz <= overlap.maxZ; pz++)
                for (int px = overlap.minX; px <= overlap.maxX; px++)
                    if (member(evidence, index(px, py, pz)) && (part.construction == null
                            || part.construction.get(offset(box, px, py, pz)))) shared++;
        return shared;
    }

    private static Bounds intersection(Bounds a, Bounds b) {
        return a.overlap(b) == 0 ? null : new Bounds(Math.max(a.minX, b.minX), Math.max(a.minY, b.minY),
                Math.max(a.minZ, b.minZ), Math.min(a.maxX, b.maxX), Math.min(a.maxY, b.maxY), Math.min(a.maxZ, b.maxZ));
    }

    Part fragment(Evidence evidence, Bounds box, long clock, int uses) {
        BitSet cells = new BitSet(box.volume());
        int manual = 0, inferred = 0;
        for (int py = box.minY; py <= box.maxY; py++) {
            for (int pz = box.minZ; pz <= box.maxZ; pz++) {
                for (int px = box.minX; px <= box.maxX; px++) {
                    int i = index(px, py, pz);
                    if (!member(evidence, i)) continue;
                    cells.set(offset(box, px, py, pz));
                    if ((blocks[i] & MANUAL) != 0) manual++;
                    else inferred++;
                }
            }
        }
        Part part = new Part(box, clock, manual >= 8, manual, inferred, uses);
        part.construction = cells;
        return part;
    }

    /** Consider slices outside each face of the anchored part. Store at most one per survey. */
    boolean extend(SiteRegistry registry, SiteRegistry.Site site, Part anchor, Evidence evidence, long clock, int uses) {
        Bounds a = anchor.bounds, b = evidence.bounds;
        Bounds[] strips = {
            new Bounds(b.minX, b.minY, b.minZ, a.minX - 1, b.maxY, b.maxZ),
            new Bounds(a.maxX + 1, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ),
            new Bounds(b.minX, b.minY, b.minZ, b.maxX, a.minY - 1, b.maxZ),
            new Bounds(b.minX, a.maxY + 1, b.minZ, b.maxX, b.maxY, b.maxZ),
            new Bounds(b.minX, b.minY, b.minZ, b.maxX, b.maxY, a.minZ - 1),
            new Bounds(b.minX, b.minY, a.maxZ + 1, b.maxX, b.maxY, b.maxZ)
        };
        for (Bounds strip : strips) {
            if (!strip.valid() || !b.covers(strip) || strip.maxX == strip.minX || strip.maxZ == strip.minZ) continue;
            Part fragment = fragment(evidence, strip, clock, uses);
            if (fragment.manual >= 4 && fragment.manual + fragment.inferred >= 12
                    && registry.attachFragment(site, fragment)) return true;
        }
        return false;
    }

    private boolean associated(Evidence evidence, int index) {
        if (evidence.component == 0 || Math.abs(minY + index / SLICE - focusY) > 1) return false;
        if ((blocks[index] & CONSTRUCTION) != 0 && membership[index] == evidence.component) return true;
        return index >= SLICE && (blocks[index - SLICE] & CONSTRUCTION) != 0
                && membership[index - SLICE] == evidence.component;
    }

    private boolean direct(Evidence evidence, int index) {
        if (evidence.component == 0 || Math.abs(minY + index / SLICE - focusY) > 2) return false;
        if (member(evidence, index)) return true;
        for (int face = 0; face < 6; face++) {
            if (member(evidence, neighbor(index, face))) return true;
        }
        return false;
    }

    private boolean member(Evidence evidence, int index) {
        return index >= 0 && (blocks[index] & CONSTRUCTION) != 0 && membership[index] == evidence.component;
    }

    /** A single horizontal hop inside the built envelope, ending at a directly supported endpoint. */
    private boolean nearEndpoint(Evidence evidence, int index) {
        if (evidence.bounds == null || Math.abs(minY + index / SLICE - focusY) > 2) return false;
        int dx = index % WIDTH, dz = index / WIDTH % WIDTH;
        Bounds bounds = evidence.bounds;
        int px = x - RADIUS + dx, pz = z - RADIUS + dz;
        if (px < bounds.minX || px > bounds.maxX || pz < bounds.minZ || pz > bounds.maxZ) return false;
        for (int row = Math.max(0, dz - 2); row <= Math.min(WIDTH - 1, dz + 2); row++) {
            int remaining = 2 - Math.abs(row - dz);
            for (int column = Math.max(0, dx - remaining); column <= Math.min(WIDTH - 1, dx + remaining); column++) {
                int other = index / SLICE * SLICE + row * WIDTH + column;
                if (other != index && functional[other] >= ENDPOINT && industrial[other] <= 0
                        && direct(evidence, other)) return true;
            }
        }
        return false;
    }

    private void observeFunction(Evidence evidence) {
        Set<Class<?>> types = new HashSet<Class<?>>(), endpoints = new HashSet<Class<?>>();
        for (int i = 0; i < VOLUME; i++) {
            if (functional[i] == 0) continue;
            boolean adjacent = direct(evidence, i);
            boolean near = nearEndpoint(evidence, i);
            if (!near && (!adjacent || functional[i] == SUPPORT || industrial[i] > 0)) continue;
            evidence.associated[GENERIC]++;
            if (near) evidence.localAssociations++;
            if (functional[i] == SUPPORT || industrial[i] > 0) {
                evidence.supports++;
            } else {
                if (functional[i] == INVENTORY) evidence.inventories++;
                if (adjacent) endpoints.add(tiles[i].getClass());
            }
            types.add(tiles[i].getClass());
        }
        evidence.genericTypes = types.size();
        evidence.endpointTypes = endpoints.size();
    }

    /** Only a bounded, predominantly filled platform can supply inferred unfamiliar material on its own. */
    private Evidence contextualPlatform() {
        int h = focusY - minY - 1;
        if (h <= 0 || h >= LAYERS - 1) return null;
        int start = h * SLICE + (focusZ - z + RADIUS) * WIDTH + focusX - x + RADIUS;
        int id = ids[start];
        if (id <= 0 || (blocks[start] & (SOLID | CONSTRUCTION)) != SOLID) return null;
        int head = 0, tail = 1;
        int minX = WIDTH, minZ = WIDTH, maxX = 0, maxZ = 0;
        queue[0] = start;
        blocks[start] |= VISITED;
        boolean bounded = true;
        while (head < tail) {
            int index = queue[head++];
            int dx = index % WIDTH, dz = index / WIDTH % WIDTH;
            minX = Math.min(minX, dx);
            minZ = Math.min(minZ, dz);
            maxX = Math.max(maxX, dx);
            maxZ = Math.max(maxZ, dz);
            if (ids[index - SLICE] == id) bounded = false;
            for (int face = 2; face < 6; face++) {
                int next = neighbor(index, face);
                if (next < 0) bounded = false;
                else if (ids[next] == id && (blocks[next] & (SOLID | VISITED)) == SOLID) {
                    blocks[next] |= VISITED;
                    queue[tail++] = next;
                }
            }
        }
        int width = maxX - minX + 1, depth = maxZ - minZ + 1;
        if (!bounded || tail < 16 || width < 3 || depth < 3 || tail * 4 < width * depth * 3) return null;
        for (int i = 0; i < tail; i++) blocks[queue[i]] = (byte) ((blocks[queue[i]] & ~VISITED) | INFERRED);
        Evidence result = component(start);
        result.contextual = tail;
        return result;
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

    static final int IC2 = 4, GENERIC = 5, UTILITY_TYPES = 6;
    private static final byte NETWORK_VISITED = 32;
    private static final int MAX_LOCAL_CABLES = 12;

    /** Components use only sampled cable faces. Machines terminate paths and must share the selected floor. */
    private void observeNetworks(Evidence evidence) {
        byte[] endpoints = new byte[VOLUME];
        int[] touched = new int[VOLUME];
        for (int root = 0; root < VOLUME; root++) {
            if (industrial[root] <= 0 || (blocks[root] & NETWORK_VISITED) != 0) continue;
            int head = 0, tail = 1, count = 0;
            boolean clipped = false;
            queue[0] = root;
            blocks[root] |= NETWORK_VISITED;
            while (head < tail) {
                int cell = queue[head++];
                for (int face = 0; face < 6; face++) {
                    int next = neighbor(cell, face);
                    if (next < 0) {
                        clipped = true;
                    } else if (industrial[next] > 0 && (blocks[next] & NETWORK_VISITED) == 0
                            && (industrial[cell] == 1 || industrial[next] == 1 || industrial[cell] == industrial[next])) {
                        blocks[next] |= NETWORK_VISITED;
                        queue[tail++] = next;
                    } else if (industrial[next] < 0 && associated(evidence, next)) {
                        int type = -industrial[next];
                        int role = type == Ic2SiteAdapter.GENERATOR ? 1 : type < Ic2SiteAdapter.STORAGE ? 2
                                : faces[next] < 0 ? 0 : faces[next] == (face ^ 1) ? 1 : 2;
                        if (role == 0) continue;
                        if (endpoints[next] == 0) touched[count++] = next;
                        endpoints[next] |= role;
                    }
                }
            }
            int sources = 0, sinks = 0, both = 0;
            for (int i = 0; i < count; i++) {
                int roles = endpoints[touched[i]];
                if ((roles & 1) != 0) sources++;
                if ((roles & 2) != 0) sinks++;
                if (roles == 3) both++;
                endpoints[touched[i]] = 0;
            }
            int pairs = sources * sinks - both;
            if (clipped || tail > MAX_LOCAL_CABLES) {
                evidence.limitedNetworks++;
            } else if (pairs > 0) {
                evidence.clusters++;
                evidence.connections += pairs;
            }
        }
    }

    private static int neighbor(int cell, int face) {
        switch (face) {
            case 0: return cell < SLICE ? -1 : cell - SLICE;
            case 1: return cell >= VOLUME - SLICE ? -1 : cell + SLICE;
            case 2: return cell / WIDTH % WIDTH == 0 ? -1 : cell - WIDTH;
            case 3: return cell / WIDTH % WIDTH == WIDTH - 1 ? -1 : cell + WIDTH;
            case 4: return cell % WIDTH == 0 ? -1 : cell - 1;
            default: return cell % WIDTH == WIDTH - 1 ? -1 : cell + 1;
        }
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

    private static boolean builtMaterial(int id, int utility) {
        return id == Block.planks.blockID || id == Block.cobblestone.blockID || id == Block.brick.blockID
                || id == Block.stoneBrick.blockID || id == Block.glass.blockID
                || id == Block.stairDouble.blockID || id == Block.stairSingle.blockID || utility != 0;
    }

    static final class Evidence {
        Bounds bounds;
        int manual, inferred, anchorCover, faces, component;
        int cables, unsupported, limitedNetworks, clusters, connections, machineTypes, placedMachines;
        int verified, contextual, unattributed, genericTypes, endpointTypes, inventories, supports, localAssociations;
        int genericReads, ic2Reads;
        final int[] found = new int[UTILITY_TYPES], associated = new int[UTILITY_TYPES];

        boolean spread() {
            return bounds != null && bounds.maxX - bounds.minX >= 2 && bounds.maxZ - bounds.minZ >= 2;
        }

        int rank() {
            int blocks = manual + inferred;
            return spread() ? VOLUME + Math.min(manual, 24) * VOLUME + blocks : blocks;
        }
    }
}
