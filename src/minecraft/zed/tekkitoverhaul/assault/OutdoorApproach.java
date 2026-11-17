package zed.tekkitoverhaul.assault;

import java.util.Arrays;

/** Bounded, distinct walkable corridors extending away from the encounter target. */
final class OutdoorApproach {
    static final int RANGE = 14;
    private static final int SIDE = 3;
    private static final int WIDTH = SIDE * 2 + 1;
    static final int COLUMN_BUDGET = (RANGE + 1) * WIDTH;
    private static final int[] FORWARD = {1, 0, 0, -1};
    private static final int[] ACROSS = {0, 1, -1, 0};

    interface Surface {
        int feet(int x, int z);
        boolean clear(int x, int y, int z);
    }

    static boolean hasOpenApproaches(Surface surface, int x, int y, int z, int targetX, int targetZ) {
        return openAxis(surface, x, y, z, x - targetX, 1, 0)
                && openAxis(surface, x, y, z, z - targetZ, 0, 1);
    }

    private static boolean openAxis(Surface surface, int x, int y, int z, int offset, int dx, int dz) {
        return (offset >= 0 && openRoute(surface, x, y, z, dx, dz))
                || (offset <= 0 && openRoute(surface, x, y, z, -dx, -dz));
    }

    private static boolean openRoute(Surface surface, int x, int y, int z, int dx, int dz) {
        int[] height = new int[COLUMN_BUDGET];
        Arrays.fill(height, -2);
        boolean[] reached = new boolean[COLUMN_BUDGET];
        int[] pending = new int[COLUMN_BUDGET];
        height[SIDE] = y;
        reached[SIDE] = true;
        pending[0] = SIDE;
        int tail = 1;
        for (int head = 0; head < tail; head++) {
            int cell = pending[head];
            int forward = cell / WIDTH, side = cell % WIDTH;
            if (forward == RANGE) return true;
            int currentX = x + forward * dx + (side - SIDE) * dz;
            int currentZ = z + forward * dz + (side - SIDE) * dx;
            for (int move = 0; move < FORWARD.length; move++) {
                int nextForward = forward + FORWARD[move], nextSide = side + ACROSS[move];
                if (nextForward < 0 || nextSide < 0 || nextSide >= WIDTH) continue;
                int next = nextForward * WIDTH + nextSide;
                if (reached[next]) continue;
                int nextX = x + nextForward * dx + (nextSide - SIDE) * dz;
                int nextZ = z + nextForward * dz + (nextSide - SIDE) * dx;
                if (height[next] == -2) {
                    int feet = surface.feet(nextX, nextZ);
                    height[next] = feet >= 1 && surface.clear(nextX, feet, nextZ)
                            && surface.clear(nextX, feet + 1, nextZ) ? feet : -1;
                }
                int nextY = height[next], currentY = height[cell];
                if (nextY < 1 || Math.abs(nextY - currentY) > 1
                        || nextY > currentY && !surface.clear(currentX, currentY + 2, currentZ)
                        || nextY < currentY && !surface.clear(nextX, nextY + 2, nextZ)) continue;
                reached[next] = true;
                pending[tail++] = next;
            }
        }
        return false;
    }
}
