package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;
import static zed.tekkitoverhaul.distillation.DistillationGeometry.*;

/** Draws the inset vessels in the normal Forge chunk pass and as lit block items. */
public final class RenderDistillationBlock {
    private static final float[] SHADE = {0.5F, 1F, 0.8F, 0.8F, 0.6F, 0.6F};

    private RenderDistillationBlock() { }

    public static boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
        try {
            return render(renderer, world, x, y, z, block,
                    DistillationLayout.Facing.fromFrontSide(world.getBlockMetadata(x, y, z)).frontSide);
        } finally {
            block.setBlockBounds(0, 0, 0, 1, 1, 1);
        }
    }

    public static void renderItem(RenderBlocks renderer, Block block, int metadata) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            render(renderer, null, 0, 0, 0, block,
                    DistillationLayout.Facing.fromFrontSide(metadata == 0 ? 3 : metadata).frontSide);
        } finally {
            block.setBlockBounds(0, 0, 0, 1, 1, 1);
            GL11.glPopMatrix();
        }
    }

    private static boolean render(RenderBlocks renderer, IBlockAccess world, int x, int y, int z,
                                  Block block, int front) {
        DistillationPartType part = DistillationRegistry.typeForBlockId(block.blockID);
        switch (part) {
            case COLUMN:
                boolean below = neighbor(world, x, y - 1, z, part);
                boolean above = neighbor(world, x, y + 1, z, part);
                draw(renderer, world, x, y, z, block, part, COLUMN_BODY, front, -1);
                draw(renderer, world, x, y, z, block, part, COLUMN_BASE, front, below ? 0 : -1);
                draw(renderer, world, x, y, z, block, part, COLUMN_CAP, front, above ? 1 : -1);
                for (Box rail : COLUMN_RAILS) draw(renderer, world, x, y, z, block, part, rail, front, -1);
                ports(renderer, world, x, y, z, block, part, front, DistillationPartType.CONDENSER, COLUMN_PORTS);
                return true;
            case CONDENSER:
                draw(renderer, world, x, y, z, block, part, CONDENSER_BODY, front, -1);
                draw(renderer, world, x, y, z, block, part, CONDENSER_BASE, front, -1);
                draw(renderer, world, x, y, z, block, part, CONDENSER_CAP, front, -1);
                ports(renderer, world, x, y, z, block, part, front, DistillationPartType.COLUMN, CONDENSER_PORTS);
                return true;
            case RECEIVER:
                draw(renderer, world, x, y, z, block, part, RECEIVER_BODY, front, -1);
                draw(renderer, world, x, y, z, block, part, RECEIVER_BASE, front, -1);
                draw(renderer, world, x, y, z, block, part, RECEIVER_CAP, front, -1);
                draw(renderer, world, x, y, z, block, part, RECEIVER_OUTLETS[front ^ 1], front, -1);
                if (neighbor(world, x, y + 1, z, DistillationPartType.CONDENSER))
                    draw(renderer, world, x, y, z, block, part, RECEIVER_NECK, front, -1);
                return true;
            default:
                return false;
        }
    }

    private static void ports(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block,
                              DistillationPartType part, int front, DistillationPartType other, Box[] ports) {
        if (world == null) return;
        for (int side = 2; side < 6; side++) {
            if (neighbor(world, x + DX[side], y, z + DZ[side], other))
                draw(renderer, world, x, y, z, block, part, ports[side], front, -1);
        }
    }

    private static boolean neighbor(IBlockAccess world, int x, int y, int z, DistillationPartType type) {
        return world != null && y >= 0 && y < world.getHeight()
                && world.getBlockId(x, y, z) == DistillationRegistry.blockId(type);
    }

    private static void draw(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block,
                             DistillationPartType part, Box box, int front, int omit) {
        block.setBlockBounds(box.minX / 16F, box.minY / 16F, box.minZ / 16F,
                box.maxX / 16F, box.maxY / 16F, box.maxZ / 16F);
        Tessellator tess = Tessellator.instance;
        for (int side = 0; side < 6; side++) {
            if (side == omit || (box.faces & 1 << side) == 0) continue;
            int cell;
            if (box.trim) cell = DistillationTextures.cell(part, side < 2 ? side : 1, front, false);
            else if (world == null) cell = block.getBlockTextureFromSideAndMetadata(side, front);
            else cell = block.getBlockTexture(world, x, y, z, side);

            if (world == null) {
                tess.startDrawingQuads();
                tess.setNormal(DX[side], DY[side], DZ[side]);
            } else {
                boolean boundary = box.onBoundary(side);
                int sx = x + (boundary ? DX[side] : 0);
                int sy = y + (boundary ? DY[side] : 0);
                int sz = z + (boundary ? DZ[side] : 0);
                if (boundary && !renderer.renderAllFaces && world.isBlockOpaqueCube(sx, sy, sz)) continue;
                tess.setBrightness(block.getMixedBrightnessForBlock(world, sx, sy, sz));
                tess.setColorOpaque_F(SHADE[side], SHADE[side], SHADE[side]);
            }
            switch (side) {
                case 0: renderer.renderBottomFace(block, x, y, z, cell); break;
                case 1: renderer.renderTopFace(block, x, y, z, cell); break;
                case 2: renderer.renderEastFace(block, x, y, z, cell); break;
                case 3: renderer.renderWestFace(block, x, y, z, cell); break;
                case 4: renderer.renderNorthFace(block, x, y, z, cell); break;
                case 5: renderer.renderSouthFace(block, x, y, z, cell); break;
                default: break;
            }
            if (world == null) tess.draw();
        }
    }
}
