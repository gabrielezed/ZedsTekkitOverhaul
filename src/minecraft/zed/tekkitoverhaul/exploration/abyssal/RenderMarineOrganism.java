package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;

/** Cutout plants and small sponge cuboids in the ordinary Forge chunk pass. */
public final class RenderMarineOrganism {
    private RenderMarineOrganism() {}

    public static boolean render(RenderBlocks renderer, IBlockAccess world, Block block, int x, int y, int z) {
        BlockMarineOrganism organism = (BlockMarineOrganism) block;
        MarineType type = organism.type;
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1F, 1F, 1F);
        int override = renderer.overrideBlockTexture;
        int tile = override >= 0 ? override : type.tile;
        switch (type) {
            case KELP:
                if (override < 0 && world.getBlockId(x, y + 1, z) == block.blockID) tile = 7;
                plane(tess, tile, x, y, z, 1, 1, 15, 15, 16);
                plane(tess, tile, x, y, z, 15, 1, 1, 15, 16);
                break;
            case FAN_CORAL:
                if ((world.getBlockMetadata(x, y, z) & 1) == 0)
                    plane(tess, tile, x, y, z, 1, 8, 15, 8, type.height);
                else
                    plane(tess, tile, x, y, z, 8, 1, 8, 15, type.height);
                break;
            case SPONGE:
                int side = override >= 0 ? override : 11;
                int cap = override >= 0 ? override : 12;
                tube(tess, side, cap, x, y, z, 5, 5, 9, 9, 11);
                tube(tess, side, cap, x, y, z, 9, 6, 12, 9, 7);
                tube(tess, side, cap, x, y, z, 6, 9, 10, 13, 5);
                break;
            default:
                plane(tess, tile, x, y, z, 2, 2, 14, 14, type.height);
                plane(tess, tile, x, y, z, 14, 2, 2, 14, type.height);
                break;
        }
        return true;
    }

    private static void plane(Tessellator t, int tile, int x, int y, int z,
                              int x1, int z1, int x2, int z2, int height) {
        double a = x + x1 / 16.0, b = x + x2 / 16.0;
        double c = z + z1 / 16.0, d = z + z2 / 16.0, top = y + height / 16.0;
        quad(t, tile, a, top, c, a, y, c, b, y, d, b, top, d);
        quad(t, tile, b, top, d, b, y, d, a, y, c, a, top, c);
    }

    private static void tube(Tessellator t, int side, int cap, int x, int y, int z,
                             int x1, int z1, int x2, int z2, int height) {
        double a = x + x1 / 16.0, b = x + x2 / 16.0;
        double c = z + z1 / 16.0, d = z + z2 / 16.0, top = y + height / 16.0;
        t.setColorOpaque_F(0.78F, 0.78F, 0.78F);
        quad(t, side, b, top, c, b, y, c, a, y, c, a, top, c);
        quad(t, side, a, top, d, a, y, d, b, y, d, b, top, d);
        t.setColorOpaque_F(0.64F, 0.64F, 0.64F);
        quad(t, side, b, top, d, b, y, d, b, y, c, b, top, c);
        quad(t, side, a, top, c, a, y, c, a, y, d, a, top, d);
        t.setColorOpaque_F(1F, 1F, 1F);
        quad(t, cap, b, top, d, b, top, c, a, top, c, a, top, d);
    }

    private static void quad(Tessellator t, int tile,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        double u = (tile % 16) / 16.0, v = (tile / 16) / 16.0;
        double end = 15.99 / 256.0;
        t.addVertexWithUV(ax, ay, az, u, v);
        t.addVertexWithUV(bx, by, bz, u, v + end);
        t.addVertexWithUV(cx, cy, cz, u + end, v + end);
        t.addVertexWithUV(dx, dy, dz, u + end, v);
    }
}
