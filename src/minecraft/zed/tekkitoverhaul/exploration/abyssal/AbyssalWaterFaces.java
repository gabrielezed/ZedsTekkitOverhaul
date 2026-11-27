package zed.tekkitoverhaul.exploration.abyssal;

import java.util.function.BiPredicate;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Material;

/** The legacy renderer emits a water top quad even when a full masonry cube is directly above it. */
public final class AbyssalWaterFaces {
    private static final BiPredicate<Object, int[]> OPEN_FACE = new BiPredicate<Object, int[]>() {
        @Override public boolean test(Object access, int[] xyz) {
            return allowsTopFace((IBlockAccess) access, xyz[0], xyz[1], xyz[2]);
        }
    };

    private AbyssalWaterFaces() {}

    public static void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            agent.getMethod("registerWaterTopFace", Object.class, Object.class, BiPredicate.class)
                    .invoke(null, Block.waterStill, Block.waterMoving, OPEN_FACE);
        } catch (ReflectiveOperationException unavailable) {
            System.err.println("[ZTO] Abyssal ceiling culling requires the matching agent.");
        }
    }

    public static boolean allowsTopFace(IBlockAccess access, int x, int y, int z) {
        int id = access.getBlockId(x, y + 1, z);
        Block ceiling = id == 0 ? null : Block.blocksList[id];
        return !(ceiling instanceof BlockAbyssalMaterial) || !ceiling.isOpaqueCube()
                || ceiling.blockMaterial != Material.rock;
    }
}
