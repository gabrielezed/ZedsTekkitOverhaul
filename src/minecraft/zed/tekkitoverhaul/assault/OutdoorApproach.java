package zed.tekkitoverhaul.assault;

/** Conservative surface routes extending away from the encounter target. */
final class OutdoorApproach {
    static final int RANGE = 24;

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
        for (int step = 0; step < RANGE; step++) {
            int nextX = x + dx;
            int nextZ = z + dz;
            int nextY = surface.feet(nextX, nextZ);
            if (nextY < 1 || Math.abs(nextY - y) > 1
                    || !surface.clear(nextX, nextY, nextZ) || !surface.clear(nextX, nextY + 1, nextZ)) {
                return false;
            }
            // A step needs headroom on the lower side of the transition.
            if (nextY > y && !surface.clear(x, y + 2, z)) return false;
            if (nextY < y && !surface.clear(nextX, nextY + 2, nextZ)) return false;
            x = nextX;
            y = nextY;
            z = nextZ;
        }
        return true;
    }
}
