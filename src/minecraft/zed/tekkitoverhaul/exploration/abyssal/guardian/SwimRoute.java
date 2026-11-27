package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.Material;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Loaded water only. Direct sweeps handle open water; a short-lived route handles corners. */
final class SwimRoute {
    private static final int[][] NEIGHBORS = {{1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}, {0,1,0}, {0,-1,0}};
    private static final int MAX_NODES = 192;
    private final ArrayDeque<Node> route = new ArrayDeque<Node>();
    private int refresh;

    static boolean clear(EntityLiving actor, double x, double y, double z) {
        World world = actor.worldObj;
        AxisAlignedBB box = actor.boundingBox;
        double half = Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.5 + 0.02;
        int minX = floor(x - half), maxX = floor(x + half);
        int minZ = floor(z - half), maxZ = floor(z + half);
        int minY = floor(y), maxY = floor(y + box.maxY - box.minY + 0.03);
        if (minY < 1 || maxY > 125 || !world.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) return false;
        for (int bx = minX; bx <= maxX; bx++) for (int bz = minZ; bz <= maxZ; bz++)
            for (int by = minY; by <= maxY; by++)
                if (world.getBlockMaterial(bx, by, bz) != Material.water) return false;
        return true;
    }

    static boolean swept(EntityLiving actor, double x, double y, double z, double dx, double dy, double dz) {
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.5));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            if (!clear(actor, x + dx * t, y + dy * t, z + dz * t)) return false;
        }
        return true;
    }

    static boolean canInsert(EntityLiving actor) {
        World world = actor.worldObj;
        AxisAlignedBB box = actor.boundingBox;
        return clear(actor, actor.posX, box.minY, actor.posZ)
                && world.getCollidingBoundingBoxes(actor, box).isEmpty() && world.checkIfAABBIsClear(box);
    }

    void reset() { route.clear(); refresh = 0; }

    boolean steer(EntityLiving actor, double x, double y, double z, double speed) {
        double dx = x - actor.posX, dy = y - actor.posY, dz = z - actor.posZ;
        double range = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (range < 0.25) return true;
        double look = Math.min(1, 16 / range);
        if (swept(actor, actor.posX, actor.posY, actor.posZ, dx * look, dy * look, dz * look)) {
            if (!route.isEmpty()) reset();
            thrust(actor, dx, dy, dz, speed);
            return true;
        }
        Node next = route.peekFirst();
        if (next != null && actor.getDistanceSq(next.x + 0.5, next.y + 0.08, next.z + 0.5) < 0.13) {
            route.removeFirst();
            next = route.peekFirst();
        }
        if (next != null && !swept(actor, actor.posX, actor.posY, actor.posZ,
                next.x + 0.5 - actor.posX, next.y + 0.08 - actor.posY, next.z + 0.5 - actor.posZ)) {
            route.clear();
            next = null;
        }
        if (refresh > 0) refresh--;
        if (next == null && refresh == 0) {
            refresh = 16 + (actor.entityId & 7);
            search(actor, floor(x), floor(y), floor(z));
            next = route.peekFirst();
        }
        if (next != null) {
            thrust(actor, next.x + 0.5 - actor.posX, next.y + 0.08 - actor.posY, next.z + 0.5 - actor.posZ, speed);
            return true;
        }
        return feeler(actor, dx / range, dy / range, dz / range, speed);
    }

    private boolean feeler(EntityLiving actor, double dx, double dy, double dz, double speed) {
        for (int side = 0; side < 4; side++) {
            int sign = (side & 1) == 0 ? 1 : -1;
            double x = side < 2 ? dx * 0.55 - dz * sign : dx * 0.55;
            double z = side < 2 ? dz * 0.55 + dx * sign : dz * 0.55;
            double y = side < 2 ? dy * 0.35 : sign * 0.8;
            if (swept(actor, actor.posX, actor.posY, actor.posZ, x, y, z)) {
                thrust(actor, x, y, z, speed * 0.75);
                return false;
            }
        }
        return false;
    }

    private void search(EntityLiving actor, int tx, int ty, int tz) {
        route.clear();
        PriorityQueue<Node> open = new PriorityQueue<Node>();
        Set<Long> visited = new HashSet<Long>();
        Node start = new Node(floor(actor.posX), floor(actor.posY), floor(actor.posZ), null, tx, ty, tz);
        if (!clear(actor, start.x + 0.5, start.y + 0.08, start.z + 0.5)) return;
        open.add(start);
        visited.add(start.key());
        Node best = start;
        int minY = Math.min(start.y, ty) - 2, maxY = Math.max(start.y, ty) + 2;
        for (int n = 0; n < MAX_NODES && !open.isEmpty(); n++) {
            Node node = open.poll();
            if (node.heuristic < best.heuristic) best = node;
            if (node.heuristic == 0 || node.depth >= 18) {
                if (node.heuristic == 0) break;
                continue;
            }
            for (int[] direction : NEIGHBORS) {
                Node child = new Node(node.x + direction[0], node.y + direction[1], node.z + direction[2],
                        node, tx, ty, tz);
                if (child.y < minY || child.y > maxY) continue;
                if (visited.add(child.key()) && clear(actor, child.x + 0.5, child.y + 0.08, child.z + 0.5))
                    open.add(child);
            }
        }
        if (best == start) return;
        do {
            route.addFirst(best);
            best = best.parent;
        } while (best != start);
        if (actor.getDistanceSq(start.x + 0.5, start.y + 0.08, start.z + 0.5) > 0.12)
            route.addFirst(start);
    }

    static void thrust(EntityLiving actor, double x, double y, double z, double speed) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 0.001) return;
        double force = Math.min(speed, length) / length;
        actor.motionX += (x * force - actor.motionX) * 0.72;
        actor.motionY += (y * force - actor.motionY) * 0.72;
        actor.motionZ += (z * force - actor.motionZ) * 0.72;
        face(actor, x, y, z);
    }

    static void face(EntityLiving actor, double x, double y, double z) {
        float desired = (float) (Math.atan2(-x, z) * 180 / Math.PI);
        float turn = desired - actor.rotationYaw;
        turn -= 360 * Math.floor((turn + 180) / 360);
        actor.rotationYaw += Math.max(-30, Math.min(30, turn));
        float pitch = (float) (-Math.atan2(y, Math.hypot(x, z)) * 180 / Math.PI);
        actor.rotationPitch += (Math.max(-60, Math.min(60, pitch)) - actor.rotationPitch) * 0.35F;
    }

    static void move(EntityLiving actor, double drag) {
        if (swept(actor, actor.posX, actor.posY, actor.posZ, actor.motionX, actor.motionY, actor.motionZ)) {
            actor.moveEntity(actor.motionX, actor.motionY, actor.motionZ);
        } else if (swept(actor, actor.posX, actor.posY, actor.posZ, 0, actor.motionY, 0)) {
            actor.moveEntity(0, actor.motionY, 0);
            actor.motionX *= 0.2;
            actor.motionZ *= 0.2;
        } else {
            actor.motionX *= 0.2;
            actor.motionY *= 0.2;
            actor.motionZ *= 0.2;
        }
        actor.motionX *= drag;
        actor.motionY *= drag;
        actor.motionZ *= drag;
    }

    static int floor(double n) { return MathHelper.floor_double(n); }

    private static final class Node implements Comparable<Node> {
        final int x, y, z, depth, cost, heuristic;
        final Node parent;

        Node(int x, int y, int z, Node parent, int tx, int ty, int tz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.parent = parent;
            depth = parent == null ? 0 : parent.depth + 1;
            cost = parent == null ? 0 : parent.cost + (y == parent.y ? 10 : 17);
            heuristic = Math.abs(tx - x) + Math.abs(ty - y) + Math.abs(tz - z);
        }

        long key() { return ((x & 0x3ffffffL) << 33) | ((z & 0x3ffffffL) << 7) | (y & 127); }

        @Override public int compareTo(Node other) {
            int result = Integer.compare(cost + 15 * heuristic, other.cost + 15 * other.heuristic);
            return result == 0 ? Long.compare(key(), other.key()) : result;
        }
    }
}
