package zed.tekkitoverhaul.fluids.client;

import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Material;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;

/** Renders the custom liquid without the vanilla water/lava-only flow-vector lookup. */
public final class RenderGasoline {
    private static final double TILE = 1D / 16D;
    private static final double TILE_END = 15.99D / 256D;

    private RenderGasoline() { }

    public static boolean render(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
        Material material = block.blockMaterial;
        double nw = y + renderer.getFluidHeight(x, y, z, material);
        double sw = y + renderer.getFluidHeight(x, y, z + 1, material);
        double se = y + renderer.getFluidHeight(x + 1, y, z + 1, material);
        double ne = y + renderer.getFluidHeight(x + 1, y, z, material);
        int east = x + 1;
        int south = z + 1;

        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        boolean rendered = false;
        if (block.shouldSideBeRendered(world, x, y + 1, z, 1)) {
            tessellator.setColorOpaque_F(1F, 1F, 1F);
            quad(tessellator, 0D, x, nw, z, x, sw, south, east, se, south, east, ne, z);
            rendered = true;
        }
        if (world.getBlockMaterial(x, y - 1, z) != material
                && !world.isBlockOpaqueCube(x, y - 1, z)) {
            tessellator.setColorOpaque_F(0.5F, 0.5F, 0.5F);
            quad(tessellator, 0D, x, y, south, x, y, z, east, y, z, east, y, south);
            rendered = true;
        }
        tessellator.setColorOpaque_F(0.8F, 0.8F, 0.8F);
        if (block.shouldSideBeRendered(world, x, y, z - 1, 2)) {
            quad(tessellator, TILE, east, ne, z, east, y, z, x, y, z, x, nw, z);
            rendered = true;
        }
        if (block.shouldSideBeRendered(world, x, y, south, 3)) {
            quad(tessellator, TILE, x, sw, south, x, y, south, east, y, south, east, se, south);
            rendered = true;
        }
        tessellator.setColorOpaque_F(0.6F, 0.6F, 0.6F);
        if (block.shouldSideBeRendered(world, x - 1, y, z, 4)) {
            quad(tessellator, TILE, x, nw, z, x, y, z, x, y, south, x, sw, south);
            rendered = true;
        }
        if (block.shouldSideBeRendered(world, east, y, z, 5)) {
            quad(tessellator, TILE, east, se, south, east, y, south, east, y, z, east, ne, z);
            rendered = true;
        }
        return rendered;
    }

    private static void quad(Tessellator tessellator, double u,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        tessellator.addVertexWithUV(ax, ay, az, u, 0D);
        tessellator.addVertexWithUV(bx, by, bz, u, TILE_END);
        tessellator.addVertexWithUV(cx, cy, cz, u + TILE_END, TILE_END);
        tessellator.addVertexWithUV(dx, dy, dz, u + TILE_END, 0D);
    }
}
