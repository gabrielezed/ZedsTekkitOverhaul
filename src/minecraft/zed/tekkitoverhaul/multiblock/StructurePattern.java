package zed.tekkitoverhaul.multiblock;

import java.util.ArrayList;
import java.util.List;

/** A reusable set of block requirements relative to one controller position. */
public final class StructurePattern {
    public static final int ANY_METADATA = -1;

    /** Replace with a World adapter in the Minecraft-specific integration layer. */
    public interface BlockView {
        int getBlockId(int x, int y, int z);
        int getBlockMetadata(int x, int y, int z);
    }

    private static final class Requirement {
        final int dx, dy, dz, blockId, metadata;
        Requirement(int dx, int dy, int dz, int blockId, int metadata) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.blockId = blockId;
            this.metadata = metadata;
        }
    }

    public static final class Result {
        public final boolean formed;
        public final int mismatches;
        public final int checkedBlocks;

        private Result(int mismatches, int checkedBlocks) {
            this.formed = mismatches == 0;
            this.mismatches = mismatches;
            this.checkedBlocks = checkedBlocks;
        }
    }

    private final Requirement[] requirements;

    private StructurePattern(List<Requirement> definitions) {
        requirements = definitions.toArray(new Requirement[definitions.size()]);
    }

    public Result check(BlockView blocks, int x, int y, int z) {
        if (blocks == null) throw new IllegalArgumentException("BlockView cannot be null");
        int missing = 0;
        for (int i = 0; i < requirements.length; i++) {
            Requirement required = requirements[i];
            int worldX = x + required.dx;
            int worldY = y + required.dy;
            int worldZ = z + required.dz;
            int id = blocks.getBlockId(worldX, worldY, worldZ);
            if (id != required.blockId ||
                    required.metadata != ANY_METADATA &&
                    blocks.getBlockMetadata(worldX, worldY, worldZ) != required.metadata) {
                missing++;
            }
        }
        return new Result(missing, requirements.length);
    }

    /** Mutable during construction; the built StructurePattern is immutable. */
    public static final class Builder {
        private final List<Requirement> definitions = new ArrayList<Requirement>();
        public Builder require(int dx, int dy, int dz, int blockId, int metadata) {
            if (blockId < 0 || metadata < ANY_METADATA || metadata > 15) {
                throw new IllegalArgumentException("Invalid block ID or metadata");
            }
            for (int i = 0; i < definitions.size(); i++) {
                Requirement previous = definitions.get(i);
                if (previous.dx == dx && previous.dy == dy && previous.dz == dz) {
                    throw new IllegalArgumentException("Duplicate structure offset");
                }
            }
            definitions.add(new Requirement(dx, dy, dz, blockId, metadata));
            return this;
        }
        public StructurePattern build() {
            if (definitions.isEmpty()) throw new IllegalStateException("A structure must have requirements");
            return new StructurePattern(new ArrayList<Requirement>(definitions));
        }
    }
}
