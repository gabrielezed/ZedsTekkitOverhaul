package zed.tekkitoverhaul.exploration.abyssal;

import java.util.function.BiPredicate;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Facing;
import net.minecraft.src.Material;

/** Masks legacy water quads facing submerged Abyssal masonry. */
public final class AbyssalWaterFaces {
    private static final BiPredicate<Object, int[]> OPEN_FACE = new BiPredicate<Object, int[]>() {
        @Override public boolean test(Object access, int[] xyz) {
            return allowsFace((IBlockAccess) access, xyz[0], xyz[1], xyz[2], xyz[3]);
        }
    };

    private AbyssalWaterFaces() {}

    public static void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            agent.getMethod("registerWaterFaces", Object.class, Object.class, BiPredicate.class)
                    .invoke(null, Block.waterStill, Block.waterMoving, OPEN_FACE);
        } catch (ReflectiveOperationException unavailable) {
            System.err.println("[ZTO] Abyssal water face culling requires the matching agent.");
        }
    }

    public static boolean allowsFace(IBlockAccess access, int x, int y, int z, int side) {
        int nx = x + Facing.offsetsXForSide[side];
        int ny = y + Facing.offsetsYForSide[side];
        int nz = z + Facing.offsetsZForSide[side];
        Block neighbor = Block.blocksList[access.getBlockId(nx, ny, nz)];
        if (side == 1 && solidMasonry(neighbor)) return false;
        if (!(neighbor instanceof AbyssalArchitecture.Partial || neighbor instanceof AbyssalArchitecture.Stairs))
            return true;

        if (side >= 2) {
            boolean floodedOverNeighbor = wet(access, nx, ny + 1, nz);
            boolean alongside = wetAlongside(access, nx, ny, nz, side ^ 1);
            if (wet(access, x, y + 1, z)) return !(floodedOverNeighbor || alongside);
            // Keep an exposed shallow pool's side. A ceiling can also enclose the uppermost source.
            return !(alongside && wet(access, x, y - 1, z) && (floodedOverNeighbor
                    || solidMasonry(Block.blocksList[access.getBlockId(x, y + 1, z)])));
        }
        // The volume above a low block is visually continuous only if the nearby cells are flooded.
        if (wet(access, nx, ny + (side == 1 ? 1 : -1), nz)) return false;
        if (side == 0 && !wet(access, x, y + 1, z)) return true;
        return !wetAlongside(access, nx, ny, nz, -1);
    }

    private static boolean wetAlongside(IBlockAccess access, int x, int y, int z, int exclude) {
        for (int side = 2; side < 6; side++) {
            if (side != exclude && wet(access, x + Facing.offsetsXForSide[side], y, z + Facing.offsetsZForSide[side]))
                return true;
        }
        return false;
    }

    private static boolean solidMasonry(Block block) {
        return block instanceof BlockAbyssalMaterial && block.isOpaqueCube() && block.blockMaterial == Material.rock;
    }

    private static boolean wet(IBlockAccess access, int x, int y, int z) {
        return access.getBlockMaterial(x, y, z) == Material.water;
    }
}
