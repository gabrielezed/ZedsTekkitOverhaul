package zed.tekkitoverhaul.exploration.abyssal.worldgen;

/** A shared portal frame adapts to either a shelf or a rock shoulder above the buried galleries. */
final class ObservatoryPlanFour extends ObservatoryPlanThree {
    final int sill;
    final boolean recessed;
    private final int jamb;
    private final int damagedSide;

    ObservatoryPlanFour(long id, int x, int z, int y, int sill, boolean recessed) {
        super(id, x, z, y);
        this.sill = sill;
        this.recessed = recessed;
        jamb = (id & 8) == 0 ? 2 : 3;
        damagedSide = (id & 16) == 0 ? -1 : 1;
    }

    @Override int revision() { return 4; }

    @Override boolean coversColumn(int wx, int wz) {
        int v = along(wx, wz);
        return v >= -5 && v <= 5 && Math.abs(across(wx, wz)) <= 6 || super.coversColumn(wx, wz);
    }

    @Override int coverRoof(int u, int v) {
        if (v >= -5 && v <= 5 && Math.abs(u) <= 6) return 0;
        return super.coverRoof(u, v);
    }

    @Override int cell(int u, int h, int v, int surface) {
        int side = Math.abs(u);
        int rise = h - sill;
        if (v >= -5 && v <= 5 && side <= 6) {
            if (v >= -3 && v <= 0 && side < jamb && rise >= 1 && rise <= 4) return WATER;
            if (v < 0 && side < jamb && rise > 4 && h <= surface) return WATER;
            if (v == -3 && side == jamb + 2 && h == surface + 1) return PEDESTAL;
            if (v == -1 && side <= jamb + 1 && rise == 0) return ERODED_STAIRS;
            if (v == 0 && side <= jamb + 2 && rise == 0) return ERODED;
            if (v >= 0 && v <= 2 && side == jamb && rise >= 1 && rise <= 5) {
                if (rise == 2 && v == 1) return LAMP;
                return rise == 1 ? PEDESTAL : rise == 5 ? CARVED : PILLAR;
            }
            if (v == (recessed ? -1 : 0) && side == jamb + 2 && rise >= 1 && rise <= 4
                    && (u * damagedSide < 0 || rise <= 2)) return rise == 1 && !recessed ? PEDESTAL : ERODED;
            if (v >= 0 && v <= 2 && side <= jamb + 1 && rise == 5) return FRIEZE;
            if (v >= 0 && v <= 2 && side <= jamb && rise == 6) return recessed ? CARVED : SLAB;
            if (v >= 1 && v <= 4 && side < jamb && h >= 5 && rise <= 4) return WATER;
            if (v >= 2 && v <= 4 && side == jamb && rise == 0) return BRICK;
        }
        int original = interior(u, h, v);
        if (original == ERODED || original == BRICK) {
            double patch = TrenchRegion.field(id ^ (h / 3), u, v, 7);
            return patch > (h <= 2 ? 0.55 : 0.72) ? ERODED : BRICK;
        }
        if (original != WATER) return original;
        if (v >= 7 && v <= 12 && side >= 4 && (v == 8 || v == 11)) {
            if (h == 3) return PEDESTAL;
            if (h >= 4 && h <= 9) return h == 9 ? CARVED : PILLAR;
        }
        if (v >= 22 && v <= 31 && v % 4 == 2 && h == 9 && side >= 4) return SLAB;
        if (v >= length - 14 && v <= length - 4 && side == 8 && v % 5 == 0) {
            if (h == 0) return PEDESTAL;
            if (h >= 1 && h <= 10) return h == 10 ? CARVED : PILLAR;
        }
        if (v == length - 1 && side <= 2 && h >= 3 && h <= 7)
            return h == 3 || h == 7 ? FRIEZE : CARVED;
        return WATER;
    }
}
