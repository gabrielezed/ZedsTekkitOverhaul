package zed.tekkitoverhaul.distillation;

import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/** Minecraft-to-layout adapter; supports all four horizontal orientations. */
public final class DistillationStructure {
    private DistillationStructure() { }

    private static final class WorldAccess implements DistillationLayout.BlockAccess {
        private final World world;

        WorldAccess(World world) { this.world = world; }

        public boolean hasPart(int x, int y, int z, DistillationPartType type) {
            return world.getBlockId(x, y, z) == DistillationRegistry.blockId(type);
        }
    }

    public static DistillationLayout.Match check(World world, int x, int y, int z) {
        if (world == null || world.getBlockId(x, y, z) !=
                DistillationRegistry.blockId(DistillationPartType.BOILER)) return null;
        return DistillationLayout.inspectBest(new WorldAccess(world), x, y, z,
                DistillationLayout.Facing.fromFrontSide(world.getBlockMetadata(x, y, z)));
    }

    static boolean canFace(World world, int x, int y, int z, DistillationLayout.Facing facing) {
        DistillationLayout.Match best = check(world, x, y, z);
        return best != null && DistillationLayout.inspect(new WorldAccess(world), x, y, z, facing)
                .missingParts == best.missingParts;
    }

    /** Locates the correct boiler without requiring full structure formation. */
    public static DistillationLayout.Match resolve(World world, int x, int y, int z,
                                                    DistillationPartType clicked) {
        if (world == null) return null;
        return DistillationLayout.resolve(new WorldAccess(world), x, y, z, clicked);
    }

    public static TileEntityDistillationBoiler boilerFor(World world, int x, int y, int z,
                                                         DistillationPartType clicked) {
        DistillationLayout.Match match = resolve(world, x, y, z, clicked);
        if (match == null) return null;
        TileEntity te = world.getBlockTileEntity(match.boilerX, match.boilerY, match.boilerZ);
        return te instanceof TileEntityDistillationBoiler ? (TileEntityDistillationBoiler)te : null;
    }
}
