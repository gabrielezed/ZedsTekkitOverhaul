package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.src.Block;
import net.minecraft.src.PathPoint;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;

/** Owner-thread search from known frontage out to connected staging ground. */
final class TerrainApproachSearch {
    private static final int FRONTAGES = 20;
    private static final int MAX_DEPTH = 22, MAX_NODES = 144, MIN_DEPLOYMENT = 12;
    private static final int[] DX = {-1, 1, 0, 0}, DZ = {0, 0, -1, 1};
    private final Bounds part, envelope;
    private final SiteSiege.Profile profile;
    private final boolean wide;
    private final List<Seed> seeds = new ArrayList<Seed>();
    private final PriorityQueue<Node> frontier = new PriorityQueue<Node>();
    private final Set<PathPoint> visited = new HashSet<PathPoint>();
    final List<SiteApproach> routes = new ArrayList<SiteApproach>();
    private int samples, nextSeed, sequence;
    private Seed seed;
    private Node current;
    private int direction;
    boolean finished;
    String rejection = "NO_CONNECTED_STAGING";

    TerrainApproachSearch(Bounds part, Bounds envelope, SiteSiege.Profile profile) {
        this.part = part;
        this.envelope = envelope;
        this.profile = profile;
        this.wide = false;
    }

    TerrainApproachSearch(Bounds part, Bounds envelope, SiteApproach approach) {
        this.part = part;
        this.envelope = envelope;
        profile = SiteSiege.Profile.PERIMETER;
        wide = true;
        samples = FRONTAGES;
        PathPoint[] path = approach.route;
        int end = Math.max(1, path.length - 4);
        PathPoint base = path[end], towardSite = path[end + 1];
        int dx = base.xCoord - towardSite.xCoord, dz = base.zCoord - towardSite.zCoord;
        for (int offset : new int[] {0, -1, 1}) {
            PathPoint point = new PathPoint(base.xCoord + dz * offset, base.yCoord, base.zCoord - dx * offset);
            seeds.add(new Seed(point, dx, dz, 1, null, null));
        }
    }

    void advance(SiteApproach.Sampler sampler) {
        if (samples < FRONTAGES) {
            Seed found = seed(sampler, samples++);
            if (found != null) seeds.add(found);
            if (samples == FRONTAGES) Collections.sort(seeds, new Comparator<Seed>() {
                @Override public int compare(Seed a, Seed b) { return b.rank - a.rank; }
            });
            return;
        }
        for (int edge = 0; edge < (wide ? 6 : 24) && sampler.columns <= SiteApproach.MAX_COLUMNS - 4; edge++) {
            if (current == null && frontier.isEmpty()) {
                if (!routes.isEmpty() || nextSeed >= Math.min(4, seeds.size())) {
                    finished = true;
                    return;
                }
                seed = seeds.get(nextSeed++);
                visited.clear();
                if (wide && !sampler.standing(seed.outside, true)) {
                    rejection = sampler.rejection;
                    continue;
                }
                enqueue(seed.outside, null);
            }
            if (current == null) {
                current = frontier.remove();
                direction = 0;
                if (stage(sampler, current)) {
                    if (routes.size() >= (wide ? 2 : 3)) {
                        finished = true;
                        return;
                    }
                }
            }
            if (current.depth < MAX_DEPTH && visited.size() < MAX_NODES) expand(sampler, direction);
            else if (routes.isEmpty()) rejection = current.depth >= MAX_DEPTH ? "PATH_TOO_LONG" : "TERRAIN_SEARCH_LIMIT";
            if (++direction == 4) current = null;
        }
    }

    private Seed seed(SiteApproach.Sampler sampler, int option) {
        int side = option % 4, dx = DX[side], dz = DZ[side], offset = option / 4;
        int x = dx < 0 ? part.minX : dx > 0 ? part.maxX : frontage(part.minX, part.maxX, offset);
        int z = dz < 0 ? part.minZ : dz > 0 ? part.maxZ : frontage(part.minZ, part.maxZ, offset);
        Seed frontage = null;
        for (int delta : new int[] {1, 0, 2, -1, 3}) {
            int y = part.minY + delta;
            if (profile != SiteSiege.Profile.PERIMETER && sampler.world.difficultySetting >= 2) {
                Seed door = entrance(sampler, x, y, z, dx, dz);
                if (door != null) return door;
            }
            if (profile == SiteSiege.Profile.BREACH || frontage != null) continue;
            if (!sampler.standing(x + dx, y, z + dz)) {
                remember(sampler.rejection);
                continue;
            }
            PathPoint outside = new PathPoint(x + dx, y, z + dz);
            if (sampler.standing(x, y, z) && sampler.standing(x - dx, y, z - dz)) {
                int id = sampler.world.getBlockId(x, y, z);
                if (id == Block.doorWood.blockID || id == Block.doorSteel.blockID || id == Block.fenceGate.blockID
                        || sampler.framed(x, y, z, dx, dz)) {
                    return new Seed(outside, dx, dz, 3, null,
                            new PathPoint[] {new PathPoint(x, y, z), new PathPoint(x - dx, y, z - dz)});
                }
            }
            frontage = new Seed(outside, dx, dz, 1, null, null);
        }
        return frontage;
    }

    private Seed entrance(SiteApproach.Sampler sampler, int x, int y, int z, int dx, int dz) {
        // A surveyed road may extend the envelope beyond the actual entrance.
        for (int inset = 0; inset <= 6; inset++) {
            int ex = x - dx * inset, ez = z - dz * inset;
            if (ex < part.minX || ex > part.maxX || ez < part.minZ || ez > part.maxZ) break;
            if (!sampler.world.checkChunksExist(ex - 2, y - 1, ez - 2, ex + 2, y + 2, ez + 2)) {
                remember("APPROACH_UNLOADED");
                continue;
            }
            SiegeBarrier door = SiegeBarrier.entrance(sampler.world, ex, y, ez, dx, dz);
            if (door != null && sampler.standing(ex + dx, y, ez + dz) && sampler.standing(ex - dx, y, ez - dz))
                return new Seed(new PathPoint(ex + dx, y, ez + dz), dx, dz, 2, door, null);
        }
        return null;
    }

    private static int frontage(int min, int max, int offset) {
        if (offset < 3) return SiteApproach.Sampler.lane(min, max, offset);
        int middle = min + (max - min) / 2;
        return Math.max(min, Math.min(max, middle + (offset == 3 ? -1 : 1)));
    }

    private void remember(String reason) {
        if ("APPROACH_UNLOADED".equals(reason) || "NO_CONNECTED_STAGING".equals(rejection)) rejection = reason;
    }

    private boolean stage(SiteApproach.Sampler sampler, Node node) {
        PathPoint at = node.point;
        int distance = Math.abs(at.xCoord - seed.outside.xCoord) + Math.abs(at.zCoord - seed.outside.zCoord);
        if (distance < MIN_DEPLOYMENT || !SiteApproach.exterior(envelope, at.xCoord, at.zCoord)) return false;
        // Bridge decks are travel surfaces. Prefer staging on the near shore or a dry platform.
        for (int y = Math.max(0, at.yCoord - 5); y < at.yCoord; y++) {
            if (sampler.world.getBlockMaterial(at.xCoord, y, at.zCoord).isLiquid()) return false;
        }
        List<PathPoint> path = new ArrayList<PathPoint>();
        for (Node next = node; next != null; next = next.parent) path.add(next.point);
        SiteApproach.Objective objective = SiteApproach.Objective.PRESSURE_PERIMETER;
        if (seed.threshold != null) {
            Collections.addAll(path, seed.threshold);
            objective = SiteApproach.Objective.APPROACH_ENTRANCE;
        } else if (seed.barrier != null) objective = SiteApproach.Objective.PRESSURE_ENTRANCE;
        routes.add(new SiteApproach(path, objective, seed.barrier, true));
        return true;
    }

    private void expand(SiteApproach.Sampler sampler, int side) {
        int x = current.point.xCoord + DX[side], z = current.point.zCoord + DZ[side];
        int rx = x - seed.outside.xCoord, rz = z - seed.outside.zCoord;
        if (rx * seed.dx + rz * seed.dz < 0 || Math.abs(rx * seed.dz - rz * seed.dx) > 12) return;
        for (int attempt = 0; attempt < (wide ? 1 : 3); attempt++) {
            int dy = attempt == 0 ? 0 : attempt == 1 ? -1 : 1;
            int y = current.point.yCoord + dy;
            if (Math.abs(y - seed.outside.yCoord) > 4) continue;
            PathPoint point = new PathPoint(x, y, z);
            if (visited.contains(point)) continue;
            if (!sampler.standing(point, wide) || !sampler.transition(current.point, point, wide)) {
                remember(sampler.rejection);
                continue;
            }
            enqueue(point, current);
            return;
        }
    }

    private void enqueue(PathPoint point, Node parent) {
        visited.add(point);
        frontier.add(new Node(point, parent, sequence++));
    }

    private static final class Seed {
        final PathPoint outside;
        final int dx, dz, rank;
        final SiegeBarrier barrier;
        final PathPoint[] threshold;

        Seed(PathPoint outside, int dx, int dz, int rank, SiegeBarrier barrier, PathPoint[] threshold) {
            this.outside = outside;
            this.dx = dx;
            this.dz = dz;
            this.rank = rank;
            this.barrier = barrier;
            this.threshold = threshold;
        }
    }

    private static final class Node implements Comparable<Node> {
        final PathPoint point;
        final Node parent;
        final int depth, order;

        Node(PathPoint point, Node parent, int order) {
            this.point = point;
            this.parent = parent;
            depth = parent == null ? 0 : parent.depth + 1;
            this.order = order;
        }

        @Override public int compareTo(Node other) {
            int difference = depth - other.depth;
            return difference == 0 ? order - other.order : difference;
        }
    }
}
