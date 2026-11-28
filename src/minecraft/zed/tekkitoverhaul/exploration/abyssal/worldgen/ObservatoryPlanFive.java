package zed.tekkitoverhaul.exploration.abyssal.worldgen;

/** An eroded threshold above a buried cistern, gallery and inner vault. */
final class ObservatoryPlanFive extends ObservatoryPlanThree {
    static final int APPROACH = -12;
    static final int FRAMES = 0, CLEFT = 1, TERRACE = 2;
    final int sill, profile, shoulder;
    private final int hallWall;

    ObservatoryPlanFive(long id, int x, int z, int y, int sill, int profile, int shoulder) {
        super(id, x, z, y, APPROACH, 4);
        this.sill = sill;
        this.profile = profile;
        this.shoulder = shoulder;
        hallWall = 7 + (int) (id & 1);
    }

    @Override int revision() { return 5; }
    @Override int[] routeStart() { return new int[] {0, 6, 11}; }

    private int approachWidth(int v) {
        return v < -6 ? 7 : v < 5 ? 11 : 6;
    }

    @Override boolean coversColumn(int wx, int wz) {
        int u = across(wx, wz), v = along(wx, wz);
        return v >= APPROACH && v <= 12 && Math.abs(u) <= approachWidth(v)
                || super.coversColumn(wx, wz);
    }

    @Override int coverRoof(int u, int v) {
        if (v >= APPROACH && v <= 12 && Math.abs(u) <= approachWidth(v)) return -1;
        if (v > length && v <= length + 4 && Math.abs(u) <= 6) return 10;
        if (v >= 21 && v <= 32 && Math.abs(u) <= hallWall + 2) return 10;
        return super.coverRoof(u, v);
    }

    private int invert(int v) {
        if (v < 0) return sill - Math.min(3, (1 - v) / 3);
        return v <= 3 ? sill - v / 2 : Math.max(3, sill - 1 - (sill - 4) * (v - 3) / 8);
    }

    private int offset(int v) {
        return profile == CLEFT && v >= 2 && v <= 8 ? shoulder : 0;
    }

    private int span(int v) {
        int entrance = profile == TERRACE ? 5 : profile == CLEFT ? 3 : 4;
        return v <= 1 ? entrance : v < 6 ? entrance - 1 : 3;
    }

    private int arch(int v, int side) {
        int crown = profile == CLEFT ? 9 : profile == TERRACE ? 6 : 8;
        crown = v <= 1 ? crown : v < 6 ? crown - 1 : 6;
        return crown - Math.max(0, side + 3 - span(v));
    }

    private int entrance(int u, int h, int v, int surface) {
        int side = Math.abs(u - offset(v)), width = span(v), base = invert(v), rise = h - base;
        if (v >= -8 && v <= 11 && side < width) {
            if (rise > 0 && rise <= (v < 0 ? 10 : arch(v, side))) return WATER;
            if (rise == 0) return v < 0 || invert(v - 1) > base ? ERODED_STAIRS : BRICK;
            if (rise < 0 && h > surface && v <= 8) return rise >= -2 ? ERODED : STONE;
        }
        // The terrace is the most exposed approach. Its uphill rib ends in the rock.
        if (v == -1 || v == 0 || v == 3 || v == 4 || v == 7 || v == 8) {
            int spring = arch(v, Math.min(side, width - 1));
            boolean outer = v <= 0;
            int buttress = outer ? 2 : 1;
            // Two overlapping courses keep the stepped ribs connected along full faces.
            if (side < width && rise > spring && rise <= spring + 2)
                return rise == spring + 1 ? FRIEZE : ERODED;
            if (side >= width && side <= width + buttress) {
                int crown = spring + 1 - (side - width);
                if (outer && u * shoulder < 0 && side > width) crown -= 2;
                if (rise <= 0 && h >= Math.min(base, surface)) return ERODED;
                if (rise > 0 && rise <= crown) {
                    if (side == width && rise == 2 && (v == 0 || v == 4 || v == 8)) return LAMP;
                    if (rise == crown) return CARVED;
                    return side == width ? PILLAR : ERODED;
                }
            }
        }
        if (v >= -10 && v <= 6 && side >= width + 1 && side <= width + 5) {
            int crest = u * shoulder > 0 ? 5 : 2;
            if (profile == CLEFT && u * shoulder > 0) crest += 3;
            int wing = crest - Math.abs(v + 1) / 2 - (side - width - 1);
            if (h > surface && rise <= wing && wing >= 0) {
                if (v >= -5 && rise == wing && side == width + 2 && (v & 1) == 0) return SHELL;
                return STONE;
            }
        }
        if (v >= 1 && v <= 11 && side < width && rise == arch(v, side) + 1 && h <= surface) return STONE;
        if (v >= 9 && v <= 12 && Math.abs(u) <= 2 && h >= 4 && h <= 8) return WATER;
        return UNTOUCHED;
    }

    @Override int cell(int u, int h, int v, int surface) {
        if (v >= APPROACH && v <= 12 && Math.abs(u) <= approachWidth(v)) {
            int threshold = entrance(u, h, v, surface);
            if (threshold != UNTOUCHED) return threshold;
            // Above the waterway the native seabed supplies the intact rock cap.
            if (v < 6) return UNTOUCHED;
        }
        int original = interior(u, h, v);
        int side = Math.abs(u);
        if (v >= 16 && v <= 19 && side <= 3 && h == 8) return FRIEZE;
        if (v >= 21 && v <= 32 && side >= hallWall && side <= hallWall + 2 && h >= 1 && h <= 10) {
            boolean end = v == 21 || v == 32;
            if (side == hallWall + 2 || h == 10 || end && side > hallWall) {
                if (original == WATER) return WATER; // The original side return crosses this gallery.
                return h == 8 ? FRIEZE : ERODED;
            }
            if (h == 4 && (v < 24 || v > 29 || side == hallWall + 1)) return SLAB;
            if (side == hallWall && v % 5 == 0 && h >= 2) return h == 6 ? LAMP : PILLAR;
            return WATER;
        }
        if (v >= 19 && v <= 33 && side < hallWall) {
            int bay = (v - 20) % 5;
            // Central crossing over a flooded sediment basin. Side walks remain at the old datum.
            if (v >= 22 && v <= 30 && side >= 2 && side <= hallWall - 3) {
                if (h == -2) return SILT;
                if (h >= -1 && h <= 0 && (v != 26 || h == -1)) return WATER;
            }
            if (v >= 22 && v <= 30 && side == 1 && h == 0) return SLAB;
            if (side == hallWall - 1 && h == 4 && bay != 0) return SLAB;
            if (side == hallWall - 2 && bay == 0 && h >= 1 && h <= 9)
                return h == 4 && v != 25 ? LAMP : h == 1 || h == 9 ? CARVED : PILLAR;
            if ((side >= hallWall - 2 && h == 9) || (side >= hallWall - 3 && h == 10 && bay == 0))
                return FRIEZE;
            if (bay == 0 && h == 9 && side >= 2) return SLAB;
        }
        int rear = length - v;
        if (rear <= 0 && rear >= -4 && side <= 6 && h >= -1 && h <= 10) {
            int arch = 9 - Math.max(0, side - 2);
            if (rear == -4 || side == 6 || h <= 0 || h >= arch)
                return h == arch || (rear == -4 && h == 2) ? FRIEZE : ERODED;
            if (rear == 0 && (side == 5 || h == arch - 1))
                return side == 5 && h == 3 ? LAMP : CARVED;
            if (rear == -3 && side == 3 && h >= 2 && h <= 7)
                return h == 4 ? LAMP : PILLAR;
            if (rear == -3 && side <= 1 && h <= 7 && (side == 0 || h <= 2))
                return h == 1 ? PEDESTAL : h == 7 ? CARVED : h == 6 ? FRIEZE : PILLAR;
            return WATER;
        }
        if (rear > 0 && rear <= 17) {
            // Ribs and light wells stay against the shell, beyond the boss's usable water.
            if (side == 9 && (rear == 3 || rear == 8 || rear == 13) && h >= 0 && h <= 10)
                return h == 3 && rear != 8 ? LAMP : h == 0 || h == 10 ? CARVED : PILLAR;
            if (rear == 1 && side >= 5 && h <= 10 && h >= 0) {
                if (side == 7 && h >= 2 && h <= 8) return PILLAR;
                if (h == 10 || h == 0) return CARVED;
            }
            if (rear >= 2 && (rear - 3) % 5 == 0 && h == 11) return side <= 6 ? FRIEZE : CARVED;
            if (rear == 3 && side <= 2 && h == 0) return WATER;
        }
        if (original == BRICK || original == ERODED) {
            double erosion = TrenchRegion.field(id ^ (h / 3), u, v, 7);
            return erosion > (h <= 2 ? 0.56 : 0.73) ? ERODED : BRICK;
        }
        return original;
    }

    @Override int metadata(int cell, int u, int v) {
        if (cell == LAMP && v >= length) {
            return new int[] {2, 5, 3, 4}[rotation];
        }
        return super.metadata(cell, u, v);
    }
}
