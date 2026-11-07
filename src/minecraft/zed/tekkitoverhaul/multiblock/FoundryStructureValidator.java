package zed.tekkitoverhaul.multiblock;

/** Temporary horizontal 3x3 ring around the Foundry controller. */
public final class FoundryStructureValidator {
    private FoundryStructureValidator() { }

    public static StructurePattern createStoneRing(int stoneId) {
        StructurePattern.Builder builder = new StructurePattern.Builder();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dz != 0) {
                    builder.require(dx, 0, dz, stoneId, 0);
                }
            }
        }
        return builder.build();
    }
}
