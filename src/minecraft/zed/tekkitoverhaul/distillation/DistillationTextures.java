package zed.tekkitoverhaul.distillation;

/** Cell values for the 16-column Forge block sheet. Shared by world and item rendering. */
final class DistillationTextures {
    static final String SHEET = "/zed/textures/distillation_blocks.png";
    private static final int STRIDE = 16;

    private DistillationTextures() { }

    static int cell(DistillationPartType part, int side, int metadata, boolean upper) {
        int front = DistillationLayout.Facing.fromFrontSide(metadata).frontSide;
        int row;
        if (side == 0) row = 3;
        else if (side == 1) row = 2;
        else if (side == front) row = upper ? 5 : 0;
        else if (side == (front ^ 1)) row = 4;
        else row = 1;
        return row * STRIDE + part.index;
    }
}
