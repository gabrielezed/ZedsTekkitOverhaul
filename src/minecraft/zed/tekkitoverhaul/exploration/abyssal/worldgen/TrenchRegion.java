package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.util.Random;

/** Coordinate fields shared by terrain, decoration and the development locator. */
public final class TrenchRegion {
    public static final int SPACING = 768;
    public final long identity;
    public final int x, z;
    public final double length, width, angle;
    private final double directionX, directionZ, bend;

    private TrenchRegion(long seed, int gridX, int gridZ) {
        identity = hash(seed, gridX, gridZ);
        Random random = new Random(identity);
        x = gridX * SPACING + 230 + random.nextInt(308);
        z = gridZ * SPACING + 230 + random.nextInt(308);
        length = 240 + random.nextInt(181);
        width = 100 + random.nextInt(81);
        angle = random.nextDouble() * Math.PI;
        directionX = Math.cos(angle);
        directionZ = Math.sin(angle);
        bend = random.nextDouble() * 30 - 15;
    }

    static TrenchRegion candidate(long seed, int gridX, int gridZ) {
        return (hash(seed ^ 41723, gridX, gridZ) & Long.MAX_VALUE) % 3 == 0
                ? new TrenchRegion(seed, gridX, gridZ) : null;
    }

    public double radius(int blockX, int blockZ) {
        int dx = blockX - x, dz = blockZ - z;
        double along = (dx * directionX + dz * directionZ) / (length * 0.5);
        double across = -dx * directionZ + dz * directionX - bend * Math.sin(along * Math.PI);
        double halfWidth = width * (0.44 + 0.12 * field(identity ^ 63, blockX, blockZ, 42));
        return Math.sqrt(along * along + across * across / (halfWidth * halfWidth));
    }

    public int seabed(int x, int z, int nativeFloor, double radius) {
        double broad = field(identity ^ 271, x, z, 29);
        double detail = field(identity ^ 1681, x, z, 8);
        double deepest = 12 + broad * 6 + detail * 2;
        double side = smooth((radius - 0.24) / 0.76);
        double shelf = 0.16 * Math.sin(side * Math.PI * 4) * broad;
        double fraction = Math.max(0, Math.min(1, side + shelf * side * (1 - side) * 4));
        return Math.min(nativeFloor, (int) Math.round(deepest + (nativeFloor - deepest) * fraction));
    }

    static long hash(long seed, int x, int z) {
        long value = seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    static double field(long seed, int x, int z, int wavelength) {
        int gx = Math.floorDiv(x, wavelength), gz = Math.floorDiv(z, wavelength);
        double dx = smooth(Math.floorMod(x, wavelength) / (double) wavelength);
        double dz = smooth(Math.floorMod(z, wavelength) / (double) wavelength);
        double a = lerp(unit(hash(seed, gx, gz)), unit(hash(seed, gx + 1, gz)), dx);
        double b = lerp(unit(hash(seed, gx, gz + 1)), unit(hash(seed, gx + 1, gz + 1)), dx);
        return lerp(a, b, dz);
    }

    static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
