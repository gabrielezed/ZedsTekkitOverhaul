package zed.tekkitoverhaul.distillation;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-independent geometry for the seven-place distillation apparatus.
 * Boiler coordinates are the origin; the other parts rotate as a single unit.
 */
public final class DistillationLayout {
    public enum Facing {
        EAST(1, 0, 0), SOUTH(0, 1, -90), WEST(-1, 0, 180), NORTH(0, -1, 90);

        public final int dx;
        public final int dz;
        public final int rotationDegrees;

        Facing(int dx, int dz, int rotationDegrees) {
            this.dx = dx;
            this.dz = dz;
            this.rotationDegrees = rotationDegrees;
        }
    }

    public interface BlockAccess {
        boolean hasPart(int x, int y, int z, DistillationPartType type);
    }

    public static final class Placement {
        public final DistillationPartType part;
        public final int x, y, z;

        private Placement(DistillationPartType part, int x, int y, int z) {
            this.part = part;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class Match {
        public final int boilerX, boilerY, boilerZ;
        public final Facing facing;
        public final int missingParts;
        public final List<Placement> missing;

        private Match(int x, int y, int z, Facing facing, List<Placement> missing) {
            boilerX = x;
            boilerY = y;
            boilerZ = z;
            this.facing = facing;
            this.missing = java.util.Collections.unmodifiableList(missing);
            missingParts = missing.size();
        }

        public boolean isFormed() { return missingParts == 0; }

        /** Human-readable diagnostics with world-space coordinates. */
        public String describeMissing() {
            if (missing.isEmpty()) return "none";
            StringBuilder description = new StringBuilder();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) description.append("; ");
                Placement p = missing.get(i);
                description.append(p.part.displayName).append(" at (")
                        .append(p.x).append(", ").append(p.y).append(", ")
                        .append(p.z).append(")");
            }
            return description.toString();
        }
    }

    private static final DistillationPartType[] TYPES = {
        DistillationPartType.HEATER,
        DistillationPartType.COLUMN,
        DistillationPartType.COLUMN,
        DistillationPartType.CONDENSER,
        DistillationPartType.RECEIVER,
        DistillationPartType.THERMOMETER
    };
    // Offsets measured from the boiler facing EAST (+X).
    private static final int[][] OFFSETS = {
        {0, -1}, {0, 1}, {0, 2}, {1, 2}, {1, 1}, {1, 0}
    };

    private DistillationLayout() { }

    public static Placement placement(int boilerX, int boilerY, int boilerZ, Facing facing, int index) {
        if (facing == null || index < 0 || index >= TYPES.length)
            throw new IllegalArgumentException("Invalid distillation layout index or facing");
        int lateral = OFFSETS[index][0];
        return new Placement(TYPES[index], boilerX + lateral * facing.dx,
                boilerY + OFFSETS[index][1], boilerZ + lateral * facing.dz);
    }

    public static Match inspect(BlockAccess blocks, int x, int y, int z, Facing facing) {
        if (blocks == null) throw new IllegalArgumentException("BlockAccess is required");
        List<Placement> missing = new ArrayList<Placement>();
        for (int i = 0; i < TYPES.length; i++) {
            Placement p = placement(x, y, z, facing, i);
            if (!blocks.hasPart(p.x, p.y, p.z, p.part)) missing.add(p);
        }
        return new Match(x, y, z, facing, missing);
    }

    public static Match inspectBest(BlockAccess blocks, int x, int y, int z) {
        Match best = null;
        for (Facing facing : Facing.values()) {
            Match m = inspect(blocks, x, y, z, facing);
            if (best == null || m.missingParts < best.missingParts) best = m;
        }
        return best;
    }

    /**
     * Finds an actual boiler whose expected layout contains the clicked part.
     * Does not demand a completed structure: this permits filling tanks and
     * editing the setpoint before the assembly is complete.
     *
     * A tied score across different boilers is ambiguous; never bind blindly.
     */
    public static Match resolve(BlockAccess blocks, int x, int y, int z, DistillationPartType clicked) {
        if (clicked == null || clicked == DistillationPartType.BOILER) return null;
        Match best = null;
        boolean ambiguous = false;
        for (Facing facing : Facing.values()) {
            for (int i = 0; i < TYPES.length; i++) {
                if (TYPES[i] != clicked) continue;
                int lateral = OFFSETS[i][0];
                int bx = x - lateral * facing.dx;
                int by = y - OFFSETS[i][1];
                int bz = z - lateral * facing.dz;
                if (!blocks.hasPart(bx, by, bz, DistillationPartType.BOILER)) continue;
                Match candidate = inspect(blocks, bx, by, bz, facing);
                if (best == null || candidate.missingParts < best.missingParts) {
                    best = candidate;
                    ambiguous = false;
                } else if (candidate.missingParts == best.missingParts &&
                        (bx != best.boilerX || by != best.boilerY || bz != best.boilerZ)) {
                    ambiguous = true;
                }
            }
        }
        return ambiguous ? null : best;
    }
}
