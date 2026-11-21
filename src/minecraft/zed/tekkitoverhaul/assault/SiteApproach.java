package zed.tekkitoverhaul.assault;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.PathPoint;
import net.minecraft.src.World;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Bounds;

/** Verified site objectives and loaded standing surfaces. */
final class SiteApproach {
    enum Objective { PRESSURE_PERIMETER, LIMITED_BREACH, PRESSURE_ENTRANCE, APPROACH_ENTRANCE }
    static final int MARGIN = 2, MAX_COLUMNS = 28;
    private static final int DEPTH = 14;
    private static final int[] DX = {-1, 1, 0, 0}, DZ = {0, 0, -1, 1};

    final PathPoint[] route;
    final Objective objective;
    final SiegeBarrier barrier;
    final boolean connected;

    SiteApproach(List<PathPoint> route, Objective objective, SiegeBarrier barrier) {
        this(route, objective, barrier, false);
    }

    SiteApproach(List<PathPoint> route, Objective objective, SiegeBarrier barrier, boolean connected) {
        this.route = route.toArray(new PathPoint[route.size()]);
        this.connected = connected;
        this.objective = objective;
        this.barrier = barrier;
    }

    PathPoint start() { return route[0]; }
    PathPoint goal() { return route[route.length - 1]; }

    /** Standoff routes share the frontage axis but stop outside the engagement lane. */
    SiteApproach staging(int lateral, int setback) {
        if (connected) lateral = 0;
        PathPoint first = start(), second = route[1];
        int dx = (second.zCoord - first.zCoord) * lateral;
        int dz = (first.xCoord - second.xCoord) * lateral;
        int length = Math.min(route.length, Math.max(3, route.length - setback));
        List<PathPoint> staging = new ArrayList<PathPoint>(length);
        for (int i = 0; i < length; i++) {
            PathPoint point = route[i];
            staging.add(new PathPoint(point.xCoord + dx, point.yCoord, point.zCoord + dz));
        }
        return new SiteApproach(staging, Objective.PRESSURE_PERIMETER, null, connected);
    }

    SiteApproach opened(PathPoint[] passage) {
        List<PathPoint> combined = new ArrayList<PathPoint>(route.length + passage.length);
        for (PathPoint point : route) combined.add(point);
        for (PathPoint point : passage) combined.add(point);
        return new SiteApproach(combined, Objective.APPROACH_ENTRANCE, null, connected);
    }

    String signature() {
        StringBuilder key = new StringBuilder();
        for (PathPoint point : route) key.append(point.xCoord).append(',').append(point.yCoord)
                .append(',').append(point.zCoord).append('/');
        return key.toString();
    }

    static boolean exterior(Bounds b, int x, int z) {
        return x < b.minX - MARGIN || x > b.maxX + MARGIN
                || z < b.minZ - MARGIN || z > b.maxZ + MARGIN;
    }

    static final class Sampler {
        final World world;
        int columns, heightReads;
        int breachChecks;
        String rejection, breachRejection;

        Sampler(World world) { this.world = world; }

        SiteApproach plan(Bounds part, Bounds envelope, int option, SiteSiege.Profile profile, boolean destructive) {
            int direction = option % 4, offset = option / 4;
            int dx = DX[direction], dz = DZ[direction];
            int x = dx < 0 ? part.minX : dx > 0 ? part.maxX : lane(part.minX, part.maxX, offset);
            int z = dz < 0 ? part.minZ : dz > 0 ? part.maxZ : lane(part.minZ, part.maxZ, offset);
            int sx = x + dx * DEPTH, sz = z + dz * DEPTH;
            if (!exterior(envelope, sx, sz)) {
                rejection = "INSIDE_EXCLUSION";
                return null;
            }
            if (!world.blockExists(sx, 64, sz)) {
                rejection = "APPROACH_UNLOADED";
                return null;
            }
            int y = -1;
            for (int offsetY : new int[] {1, 0, 2, -1, 3}) {
                if (!standing(sx, part.minY + offsetY, sz)) continue;
                y = part.minY + offsetY;
                break;
            }
            if (y < 0) return null;
            List<PathPoint> route = new ArrayList<PathPoint>();
            for (int i = DEPTH; i >= 0; i--) {
                int px = x + dx * i, pz = z + dz * i;
                if (!standing(px, y, pz)) {
                    if ("APPROACH_UNLOADED".equals(rejection) || route.size() < 7 || i > 4) return null;
                    if (i == 0 && profile != SiteSiege.Profile.PERIMETER) {
                        SiteApproach tactic = specialist(route, x, y, z, dx, dz, destructive);
                        if (tactic != null) return tactic;
                    }
                    return new SiteApproach(route, Objective.PRESSURE_PERIMETER, null);
                }
                route.add(new PathPoint(px, y, pz));
            }
            // An open doorway can lead one step into this part. The spawn remains outside the compound.
            boolean crosses = standing(x - dx, y, z - dz);
            if (crosses) route.add(new PathPoint(x - dx, y, z - dz));
            int id = world.getBlockId(x, y, z);
            boolean entrance = crosses && (id == Block.doorWood.blockID || id == Block.doorSteel.blockID
                    || id == Block.fenceGate.blockID || framed(x, y, z, dx, dz));
            return new SiteApproach(route, entrance ? Objective.APPROACH_ENTRANCE : Objective.PRESSURE_PERIMETER, null);
        }

        boolean framed(int x, int y, int z, int dx, int dz) {
            return world.isBlockNormalCube(x - dz, y, z - dx) && world.isBlockNormalCube(x + dz, y, z + dx);
        }

        SiteApproach specialist(List<PathPoint> route, int x, int y, int z, int dx, int dz,
                                        boolean destructive) {
            breachChecks++;
            SiegeBarrier entrance = SiegeBarrier.entrance(world, x, y, z, dx, dz);
            if (entrance != null) {
                if (world.difficultySetting < 2) breachRejection = "ENTRANCE_REQUIRES_NORMAL";
                else if (standing(x - dx, y, z - dz)) return new SiteApproach(route, Objective.PRESSURE_ENTRANCE, entrance);
                else breachRejection = rejection;
                return null;
            }
            SiegeBarrier barrier = SiegeBarrier.structure(world, x, y, z, dx, dz);
            if (barrier == null) return null;
            if (!destructive) breachRejection = "STRUCTURAL_BREACH_DISABLED";
            else if (world.difficultySetting < 3) breachRejection = "STRUCTURAL_BREACH_REQUIRES_HARD";
            else {
                List<PathPoint> staging = route.subList(0, route.size() - 1);
                for (PathPoint point : staging) {
                    if (standing(point, true)) continue;
                    breachRejection = rejection;
                    return null;
                }
                return new SiteApproach(staging, Objective.LIMITED_BREACH, barrier);
            }
            return null;
        }

        static int lane(int min, int max, int offset) {
            int middle = min + (max - min) / 2;
            return offset == 0 ? middle : offset == 1 ? Math.max(min, middle - 2) : Math.min(max, middle + 2);
        }

        boolean validate(SiteApproach approach, boolean wide) {
            PathPoint previous = null;
            for (PathPoint point : approach.route) {
                if (!standing(point, wide) || (previous != null && !transition(previous, point, wide))) return false;
                previous = point;
            }
            return true;
        }

        boolean transition(PathPoint from, PathPoint to, boolean wide) {
            int rise = Math.abs(from.yCoord - to.yCoord);
            if (rise == 0) return true;
            if (rise > 1 || wide) {
                rejection = wide ? "TANK_HEIGHT_TRANSITION" : "HEIGHT_TRANSITION_UNSUPPORTED";
                return false;
            }
            PathPoint low = from.yCoord < to.yCoord ? from : to;
            int high = low.yCoord + 1;
            if (body(low.xCoord, high, low.zCoord, 0.30, 1.95)) return true;
            rejection = "HEIGHT_TRANSITION_UNSUPPORTED";
            return false;
        }

        /** Covered insertion needs a supported, surface-connected witness along its observed route. */
        boolean surfaceAccess(SiteApproach route, Bounds envelope) {
            for (PathPoint point : route.route) {
                int x = point.xCoord, y = point.yCoord, z = point.zCoord;
                if (!exterior(envelope, x, z)) continue;
                heightReads++;
                if (world.getHeightValue(x, z) <= y) return true;
                for (int i = 0; i < DX.length; i++) {
                    int nx = x + DX[i], nz = z + DZ[i];
                    if (!exterior(envelope, nx, nz)) continue;
                    heightReads++;
                    if (world.getHeightValue(nx, nz) <= y && supported(nx, y, nz)
                            && body(nx, y, nz, 0.36, 1.95)) return true;
                }
            }
            rejection = "NO_VERIFIED_EXTERIOR_ACCESS";
            return false;
        }

        /** A clipped site needs additional verified escape space beyond its insertion. */
        boolean localExterior(SiteApproach approach) {
            PathPoint start = approach.start(), next = approach.route[1];
            int dx = start.xCoord - next.xCoord, dz = start.zCoord - next.zCoord;
            for (int i = 1; i <= 4; i++) {
                if (!standing(start.xCoord + dx * i, start.yCoord, start.zCoord + dz * i)) return false;
            }
            return standing(start.xCoord - dz, start.yCoord, start.zCoord + dx)
                    && standing(start.xCoord + dz, start.yCoord, start.zCoord - dx);
        }

        boolean standing(PathPoint point, boolean wide) {
            int x = point.xCoord, y = point.yCoord, z = point.zCoord;
            if (!wide) return standing(x, y, z);
            if (y > 252 || !sampleColumn(x, y, z)) {
                if (y > 252) rejection = "TANK_CLEARANCE";
                return false;
            }
            // Keep the Tank's complete footprint supported even on a centered bridge.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (supported(x + dx, y, z + dz)) continue;
                    rejection = "TANK_CLEARANCE";
                    return false;
                }
            }
            if (body(x, y, z, 0.925, 3.15)) return true;
            rejection = "TANK_CLEARANCE";
            return false;
        }

        boolean standing(int x, int y, int z) {
            if (!sampleColumn(x, y, z)) return false;
            if (!supported(x, y, z)) {
                rejection = world.getBlockMaterial(x, y, z).isLiquid()
                        || world.getBlockMaterial(x, y - 1, z).isLiquid() ? "WATER_WITHOUT_SUPPORT" : "INVALID_SUPPORT";
                return false;
            }
            if (body(x, y, z, 0.30, 1.95)) return true;
            rejection = world.getBlockMaterial(x, y, z).isLiquid() ? "WATER_WITHOUT_SUPPORT" : "ROUTE_OBSTRUCTED";
            return false;
        }

        boolean thiefClear(PathPoint point) {
            if (body(point.xCoord, point.yCoord, point.zCoord, 0.36, 1.95)) return true;
            rejection = "THIEF_CLEARANCE";
            return false;
        }

        private boolean supported(int x, int y, int z) {
            return world.isBlockSolidOnSide(x, y - 1, z, 1);
        }

        private boolean sampleColumn(int x, int y, int z) {
            if (columns >= MAX_COLUMNS) {
                rejection = "PLANNER_BUDGET";
                return false;
            }
            columns++;
            if (y < 1 || y > 253 || x < -29999997 || x > 29999997 || z < -29999997 || z > 29999997
                    || !world.blockExists(x, y, z)
                    || !world.checkChunksExist(x - 2, y - 1, z - 2, x + 2, y + 1, z + 2)) {
                rejection = "APPROACH_UNLOADED";
                return false;
            }
            return true;
        }

        private boolean body(int x, int y, int z, double half, double height) {
            AxisAlignedBB body = AxisAlignedBB.getBoundingBox(x + 0.5 - half, y, z + 0.5 - half,
                    x + 0.5 + half, y + height, z + 0.5 + half);
            ArrayList<AxisAlignedBB> collisions = new ArrayList<AxisAlignedBB>();
            int radius = half > 0.5 ? 1 : 0;
            for (int bx = x - radius; bx <= x + radius; bx++) {
                for (int bz = z - radius; bz <= z + radius; bz++) {
                    for (int by = y - 1; by < y + height; by++) {
                        int id = world.getBlockId(bx, by, bz);
                        if (id == 0) continue;
                        Block block = Block.blocksList[id];
                        if (block == null || (by >= y && block.blockMaterial.isLiquid())) return false;
                        if (by < y && block != Block.fence && block != Block.netherFence && block != Block.fenceGate)
                            continue;
                        if ((block == Block.doorWood || block == Block.doorSteel)
                                && (world.getBlockMetadata(bx, y, bz) & 4) == 0) return false;
                        if (block == Block.fenceGate && (world.getBlockMetadata(bx, by, bz) & 4) == 0) return false;
                        block.getCollidingBoundingBoxes(world, bx, by, bz, body, collisions);
                        if (!collisions.isEmpty()) return false;
                    }
                }
            }
            return true;
        }
    }
}
