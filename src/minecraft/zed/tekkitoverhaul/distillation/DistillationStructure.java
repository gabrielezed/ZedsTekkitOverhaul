package zed.tekkitoverhaul.distillation;

import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/** Minecraft-to-layout adapter; supports all four horizontal orientations. */
public final class DistillationStructure {
    private DistillationStructure() { }

    private static final class WorldAccess implements DistillationLayout.BlockAccess, DistillationLayout.FacingLookup {
        private final World world;

        WorldAccess(World world) { this.world = world; }

        public boolean hasPart(int x, int y, int z, DistillationPartType type) {
            return world.blockExists(x, y, z) && world.getBlockId(x, y, z) == DistillationRegistry.blockId(type);
        }

        public DistillationLayout.Facing preferredFacing(int x, int y, int z) {
            return DistillationLayout.Facing.fromFrontSide(world.getBlockMetadata(x, y, z));
        }
    }

    public static DistillationLayout.Match check(World world, int x, int y, int z) {
        if (world == null) return null;
        WorldAccess access = new WorldAccess(world);
        if (!access.hasPart(x, y, z, DistillationPartType.BOILER)) return null;
        return DistillationLayout.inspectBest(access, x, y, z, access.preferredFacing(x, y, z));
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
        WorldAccess access = new WorldAccess(world);
        return DistillationLayout.resolve(access, x, y, z, clicked, access);
    }

    public static TileEntityDistillationBoiler boilerFor(World world, int x, int y, int z,
                                                         DistillationPartType clicked) {
        DistillationLayout.Match match = resolve(world, x, y, z, clicked);
        if (match == null) return null;
        TileEntity te = world.getBlockTileEntity(match.boilerX, match.boilerY, match.boilerZ);
        return te instanceof TileEntityDistillationBoiler ? (TileEntityDistillationBoiler)te : null;
    }
}
