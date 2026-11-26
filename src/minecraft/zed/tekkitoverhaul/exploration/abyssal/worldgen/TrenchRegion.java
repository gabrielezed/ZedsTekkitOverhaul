package zed.tekkitoverhaul.exploration.abyssal.worldgen;

/** A planned expedition anchor and the continuous seed fields used to find it. */
public final class TrenchRegion {
    public final long identity;
    public final int x, z;

    TrenchRegion(long seed, int x, int z) {
        identity = hash(seed ^ 89109, x, z);
        this.x = x;
        this.z = z;
    }

    static double influence(long seed, int x, int z) {
        double wx = x + 124 * (field(seed ^ 311, x, z, 184) - 0.5);
        double wz = z + 124 * (field(seed ^ 997, x, z, 157) - 0.5);
        double geography = 0.67 * field(seed ^ 281, wx, wz, 576)
                + 0.23 * field(seed ^ 1709, wx, wz, 229)
                + 0.10 * field(seed ^ 3429, wx, wz, 112);
        return smooth((geography - 0.64) / 0.14);
    }

    static double relief(long seed, int x, int z) {
        double channels = Math.abs(2 * field(seed ^ 2837, x, z, 138) - 1);
        double ridges = field(seed ^ 1789, x, z, 91);
        double basins = field(seed ^ 4421, x, z, 364);
        double shelves = field(seed ^ 2957, x, z, 43);
        return 15 + 15 * smooth(channels * 1.6) + 8 * ridges + 5 * basins
                + 4 * (shelves - 0.5) + 2 * (field(seed ^ 1181, x, z, 13) - 0.5);
    }

    static int seabed(double relief, int nativeFloor, double influence) {
        return Math.min(nativeFloor, (int) Math.round(nativeFloor + (relief - nativeFloor) * influence));
    }

    static long hash(long seed, int x, int z) {
        long value = seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    static double field(long seed, double x, double z, int wavelength) {
        int gx = (int) Math.floor(x / wavelength), gz = (int) Math.floor(z / wavelength);
        double dx = smooth(x / wavelength - gx);
        double dz = smooth(z / wavelength - gz);
        double a = lerp(unit(hash(seed, gx, gz)), unit(hash(seed, gx + 1, gz)), dx);
        double b = lerp(unit(hash(seed, gx, gz + 1)), unit(hash(seed, gx + 1, gz + 1)), dx);
        return lerp(a, b, dz);
    }

    static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
