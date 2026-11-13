package zed.tekkitoverhaul.assault;

final class OutdoorApproachSelfTest {
    public static void main(String[] args) {
        Surface surface = new Surface();
        check(surface, "illuminated field", true, 12, 2);
        check(surface, "aligned field", true, 12, 0);
        surface.mode = "courtyard";
        check(surface, "inside courtyard", false, 12, 2);
        check(surface, "outside courtyard", true, 18, 2);
        surface.mode = "opening";
        check(surface, "inside courtyard facing its opening", false, 12, 2);
        check(surface, "outside courtyard with opening", true, 18, 2);
        surface.mode = "wall";
        check(surface, "outside isolated wall", true, 18, 2);
        surface.mode = "slope";
        check(surface, "one-block slope", true, 12, 2);
        surface.mode = "trench";
        check(surface, "one-block trench", true, 12, 2);
        surface.mode = "overhang";
        check(surface, "insufficient step headroom", false, 12, 2);
        surface.mode = "missing";
        check(surface, "unknown column", false, 12, 2);
        surface.mode = "blocked";
        check(surface, "closed boundary in the upper body space", false, 12, 2);
        System.out.println("Outdoor approach checks passed");
    }

    private static void check(Surface surface, String label, boolean expected, int x, int z) {
        surface.samples = 0;
        if (OutdoorApproach.hasOpenApproaches(surface, x, 64, z, 0, 0) != expected) {
            throw new AssertionError(label);
        }
        if (surface.samples > 3 * OutdoorApproach.RANGE) throw new AssertionError("column budget");
    }

    private static final class Surface implements OutdoorApproach.Surface {
        String mode = "open";
        int samples;

        public int feet(int x, int z) {
            samples++;
            int radius = Math.max(Math.abs(x), Math.abs(z));
            if (radius == 16 && ("courtyard".equals(mode) || ("opening".equals(mode) && z != 2))) return 67;
            if (x == 16 && "wall".equals(mode)) return 67;
            if (x >= 16 && ("slope".equals(mode) || "overhang".equals(mode))) return 65;
            if (x == 16 && "trench".equals(mode)) return 63;
            if (x == 16 && "missing".equals(mode)) return -1;
            return 64;
        }

        public boolean clear(int x, int y, int z) {
            return !("overhang".equals(mode) && x == 15 && y == 66)
                    && !("blocked".equals(mode) && x == 16 && y == 65);
        }
    }
}
