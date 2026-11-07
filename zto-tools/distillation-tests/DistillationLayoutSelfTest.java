import java.util.HashMap;
import java.util.Map;
import zed.tekkitoverhaul.distillation.DistillationLayout;
import zed.tekkitoverhaul.distillation.DistillationPartType;

/** Pure Java regression tests for rotated, incomplete and ambiguous layouts. */
public final class DistillationLayoutSelfTest {
    private static int assertions;

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static final class Grid implements DistillationLayout.BlockAccess {
        final Map<String, DistillationPartType> placed =
                new HashMap<String, DistillationPartType>();

        private String key(int x, int y, int z) { return x + ":" + y + ":" + z; }

        void put(int x, int y, int z, DistillationPartType part) {
            placed.put(key(x, y, z), part);
        }

        void remove(int x, int y, int z) { placed.remove(key(x, y, z)); }

        public boolean hasPart(int x, int y, int z, DistillationPartType part) {
            return part == placed.get(key(x, y, z));
        }

        void build(int bx, int by, int bz, DistillationLayout.Facing facing) {
            put(bx, by, bz, DistillationPartType.BOILER);
            for (int i = 0; i < 6; i++) {
                DistillationLayout.Placement p =
                        DistillationLayout.placement(bx, by, bz, facing, i);
                put(p.x, p.y, p.z, p.part);
            }
        }
    }

    public static void main(String[] args) {
        for (DistillationLayout.Facing facing : DistillationLayout.Facing.values()) {
            Grid grid = new Grid();
            int bx = 50, by = 65, bz = 90;
            check(DistillationLayout.Facing.fromFrontSide(facing.frontSide) == facing,
                    "Front side must round-trip");
            check(DistillationLayout.inspectBest(grid, bx, by, bz, facing).facing == facing,
                    "An empty layout must respect the placement front");
            grid.build(bx, by, bz, facing);
            DistillationLayout.Facing opposite = DistillationLayout.Facing.values()[(facing.ordinal() + 2) % 4];
            DistillationLayout.Match complete = DistillationLayout.inspectBest(grid, bx, by, bz, opposite);
            check(complete.isFormed(), facing + " must form");
            check(complete.facing == facing, facing + " orientation mismatch");
            DistillationLayout.Placement thermometer =
                    DistillationLayout.placement(bx, by, bz, facing, 5);
            DistillationLayout.Match resolved = DistillationLayout.resolve(
                    grid, thermometer.x, thermometer.y, thermometer.z,
                    DistillationPartType.THERMOMETER);
            check(resolved != null, facing + " thermometer not linked");
            check(resolved.boilerX == bx && resolved.boilerY == by &&
                    resolved.boilerZ == bz, facing + " linked to the wrong boiler");
            DistillationLayout.Placement condenser =
                    DistillationLayout.placement(bx, by, bz, facing, 3);
            resolved = DistillationLayout.resolve(grid, condenser.x,
                    condenser.y, condenser.z, DistillationPartType.CONDENSER);
            check(resolved != null && resolved.isFormed(), facing + " condenser not linked");
            grid.remove(condenser.x, condenser.y, condenser.z);
            DistillationLayout.Match broken = DistillationLayout.inspectBest(grid, bx, by, bz);
            check(!broken.isFormed() && broken.missingParts == 1,
                    facing + " missing condenser not detected");
            check(broken.describeMissing().contains("Condenser"),
                    facing + " missing-part diagnostic not useful");
            // An incomplete machine must still allow access to the thermometer.
            check(DistillationLayout.resolve(grid, thermometer.x, thermometer.y,
                    thermometer.z, DistillationPartType.THERMOMETER) != null,
                    facing + " incomplete structure became inaccessible");
        }
        Grid separate = new Grid();
        separate.build(10, 64, 10, DistillationLayout.Facing.EAST);
        separate.build(20, 64, 10, DistillationLayout.Facing.WEST);
        DistillationLayout.Match link = DistillationLayout.resolve(separate, 19, 64, 10,
                DistillationPartType.THERMOMETER);
        check(link != null && link.boilerX == 20, "Boiler cross-link");
        check(DistillationLayout.resolve(separate, 70, 70, 70,
                DistillationPartType.THERMOMETER) == null, "Unrelated part was linked");
        System.out.println("PASS: " + assertions + " distillation layout assertions");
    }
}
