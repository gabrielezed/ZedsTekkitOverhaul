import java.util.HashMap;
import java.util.Map;
import zed.tekkitoverhaul.multiblock.FoundryStructureValidator;
import zed.tekkitoverhaul.multiblock.StructurePattern;

/** Pure Java tests; no Minecraft libraries required. */
public final class StructurePatternSelfTest {
    private static int assertions;
    private static void test(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static final class Grid implements StructurePattern.BlockView {
        final Map<String, int[]> values = new HashMap<String, int[]>();
        void set(int x, int y, int z, int id, int metadata) {
            values.put(x + ":" + y + ":" + z, new int[]{id, metadata});
        }
        public int getBlockId(int x, int y, int z) {
            int[] value = values.get(x + ":" + y + ":" + z);
            return value == null ? 0 : value[0];
        }
        public int getBlockMetadata(int x, int y, int z) {
            int[] value = values.get(x + ":" + y + ":" + z);
            return value == null ? 0 : value[1];
        }
        void ring(int x, int y, int z, int id) {
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dz != 0) set(x + dx, y, z + dz, id, 0);
            }
        }
    }
    public static void main(String[] args) {
        StructurePattern ring = FoundryStructureValidator.createStoneRing(1);
        Grid grid = new Grid();
        StructurePattern.Result result = ring.check(grid, 20, 64, 40);
        test(!result.formed, "Empty ring was reported formed");
        test(result.mismatches == 8, "Empty ring must miss eight positions");
        test(result.checkedBlocks == 8, "Ring must require eight blocks");
        grid.ring(20, 64, 40, 1);
        test(ring.check(grid, 20, 64, 40).formed, "Completed ring not recognized");
        grid.set(19, 64, 39, 0, 0);
        test(ring.check(grid, 20, 64, 40).mismatches == 1, "Missing diagonal not detected");
        grid.set(19, 64, 39, 1, 0);
        grid.set(19, 64, 40, 1, 1);
        test(!ring.check(grid, 20, 64, 40).formed, "Wrong Stone metadata accepted");
        grid.set(19, 64, 40, 1, 0);
        grid.set(20, 64, 40, 99, 0);
        test(ring.check(grid, 20, 64, 40).formed, "Controller should not be part of ring check");
        grid.ring(40, 64, 60, 1);
        test(ring.check(grid, 40, 64, 60).formed, "Independent second structure not recognized");
        grid.set(39, 64, 59, 2, 0);
        test(!ring.check(grid, 40, 64, 60).formed, "Modified second structure not detected");
        test(ring.check(grid, 20, 64, 40).formed, "Second structure affected first structure");
        grid.set(20, 65, 40, 1, 0);
        test(ring.check(grid, 20, 64, 40).formed, "Extra block above controller should be ignored");
        boolean dup = false;
        try { new StructurePattern.Builder().require(1, 0, 0, 1, 0).require(1, 0, 0, 1, 0); }
        catch (IllegalArgumentException expected) { dup = true; }
        test(dup, "Duplicate offset allowed");
        StructurePattern wildcard = new StructurePattern.Builder().require(1, 0, 0, 4, StructurePattern.ANY_METADATA).build();
        grid.set(1, 0, 0, 4, 15);
        test(wildcard.check(grid, 0, 0, 0).formed, "Metadata wildcard rejected");
        System.out.println("PASS: " + assertions + " structure validation assertions");
    }
}
