package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Block;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import net.minecraft.src.World;
import org.lwjgl.opengl.GL11;

/** One boiler renderer draws the small, authoritative indicators for its apparatus. */
final class RenderDistillationIndicators extends TileEntitySpecialRenderer {
    static final String SHEET = "/zed/textures/distillation_indicators.png";

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler)tile;
        if (!boiler.presentationAvailable()) return;

        DistillationState state = boiler.visualState();
        boolean running = state == DistillationState.ACTIVE;
        FractionationProcess process = boiler.getProcess();
        int motion = DistillationAnimation.motionFrame(tile.worldObj.getWorldTime());
        DistillationLayout.Facing facing = boiler.getVisualFacing();
        int completion = boiler.completionTicks();
        int receiver = DistillationAnimation.receiverFrame(process.getDistillateMB(), state.blocksOutput(), completion);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            bindTextureByName(SHEET);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setColorOpaque_I(0xFFFFFF);
            if (running) {
                window(tess, tile, x, y, z, 0, -1, 0, 0, 5, 11, 5, 8, motion, 0);
                window(tess, tile, x, y, z, 0, 1, 0, 3, 6, 10, 3, 13, motion, 1);
                window(tess, tile, x, y, z, 0, 2, 0, 3, 6, 10, 3, 13, (motion + 2) & 3, 1);
                window(tess, tile, x, y, z, facing.dx, 2, facing.dz, 2, 5, 11, 4, 7, motion, 2);
            }
            if (receiver > 0)
                window(tess, tile, x, y, z, facing.dx, 1, facing.dz, 3, 6, 10, 4, 11, receiver, 3);

            if (state != DistillationState.IDLE) {
                int progress = running ? 4 * process.getProgressTicks() / process.selectedType().durationTicks : 5;
                if (completion > 0) progress = 4;
                window(tess, tile, x, y, z, facing.dx, 0, facing.dz, 0, 5, 11, 2, 4, progress, 4);
            }
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void window(Tessellator tess, TileEntity tile, double x, double y, double z,
                               int dx, int dy, int dz, int inset, int left, int right, int bottom, int top,
                               int frame, int row) {
        World world = tile.worldObj;
        int bx = tile.xCoord + dx, by = tile.yCoord + dy, bz = tile.zCoord + dz;
        int side = DistillationLayout.Facing.fromFrontSide(world.getBlockMetadata(bx, by, bz)).frontSide;
        int nx = DistillationGeometry.DX[side], nz = DistillationGeometry.DZ[side];
        Block block = Block.blocksList[world.getBlockId(bx, by, bz)];
        if (block == null || world.isBlockOpaqueCube(bx + nx, by, bz + nz)) return;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, bx + nx, by, bz + nz));

        double depth = (8 - inset) / 16D + 0.001D;
        double cx = x + dx + 0.5D + nx * depth;
        double cz = z + dz + 0.5D + nz * depth;
        double a = (left - 8) / 16D, b = (right - 8) / 16D;
        double lo = y + dy + bottom / 16D, hi = y + dy + top / 16D;
        double u = (frame * 16 + 0.01D) / 256D, uEnd = (frame * 16 + 15.99D) / 256D;
        double v = (row * 16 + 0.01D) / 256D, vEnd = (row * 16 + 15.99D) / 256D;
        tess.addVertexWithUV(cx + nz * a, hi, cz - nx * a, u, v);
        tess.addVertexWithUV(cx + nz * a, lo, cz - nx * a, u, vEnd);
        tess.addVertexWithUV(cx + nz * b, lo, cz - nx * b, uEnd, vEnd);
        tess.addVertexWithUV(cx + nz * b, hi, cz - nx * b, uEnd, v);
    }
}
