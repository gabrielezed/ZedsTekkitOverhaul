package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Block;
import net.minecraft.src.World;
import zed.tekkitoverhaul.multiblock.StructurePattern;
import zed.tekkitoverhaul.multiblock.WorldBlockView;

/**
 * Temporary non-rotating 2x4x1 distillation apparatus.
 *
 * Y+2: column / condenser
 * Y+1: column / receiver
 * Y 0: boiler / thermometer
 * Y-1: heater / empty
 *
 * Columns are stacked. Auxiliary parts extend one block toward +X.
 */
public final class DistillationStructure {
    private static StructurePattern pattern;

    private DistillationStructure() { }

    public static StructurePattern build() {
        if (pattern == null) {
            pattern = new StructurePattern.Builder()
                    .require(0, -1, 0, DistillationRegistry.blockId(DistillationPartType.HEATER), 0)
                    .require(0, 1, 0, DistillationRegistry.blockId(DistillationPartType.COLUMN), 0)
                    .require(0, 2, 0, DistillationRegistry.blockId(DistillationPartType.COLUMN), 0)
                    .require(1, 2, 0, DistillationRegistry.blockId(DistillationPartType.CONDENSER), 0)
                    .require(1, 1, 0, DistillationRegistry.blockId(DistillationPartType.RECEIVER), 0)
                    .require(1, 0, 0, DistillationRegistry.blockId(DistillationPartType.THERMOMETER), 0)
                    .build();
        }
        return pattern;
    }

    public static StructurePattern.Result check(World world, int x, int y, int z) {
        if (world == null || world.getBlockId(x, y, z) !=
                DistillationRegistry.blockId(DistillationPartType.BOILER)) {
            return null;
        }
        return build().check(new WorldBlockView(world), x, y, z);
    }
}
