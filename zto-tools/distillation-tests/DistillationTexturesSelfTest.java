package zed.tekkitoverhaul.distillation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

/** Verifies the static face and operating indicator atlas contracts. */
public final class DistillationTexturesSelfTest {
    public static void main(String[] args) throws Exception {
        BufferedImage atlas = ImageIO.read(new File("src/minecraft" + DistillationTextures.SHEET));
        check(atlas.getWidth() == 256 && atlas.getHeight() == 256, "Forge sheet dimensions");
        Set<Integer> signatures = new HashSet<Integer>();
        for (DistillationPartType part : DistillationPartType.values()) {
            for (DistillationLayout.Facing facing : DistillationLayout.Facing.values()) {
                for (int side = 0; side < 6; side++) {
                    int row = side == 0 ? 3 : side == 1 ? 2
                            : side == facing.frontSide ? 0 : side == (facing.frontSide ^ 1) ? 4 : 1;
                    int cell = DistillationTextures.cell(part, side, facing.frontSide, false);
                    check(cell / 16 == row && cell % 16 == part.index, "face map " + part + " " + facing + " side " + side);
                    signature(atlas, cell);
                }
            }
            check(signatures.add(signature(atlas, part.index)), "unique machine front for " + part);
        }
        int lower = DistillationTextures.cell(DistillationPartType.COLUMN, 2, 2, false);
        int upper = DistillationTextures.cell(DistillationPartType.COLUMN, 2, 2, true);
        check(signature(atlas, lower) != signature(atlas, upper), "column sections differ");
        System.out.println("PASS six distinct block fronts, all horizontal facings, caps and upper column cell");
        BufferedImage indicators = ImageIO.read(new File("src/minecraft/zed/textures/distillation_indicators.png"));
        check(indicators.getWidth() == 256 && indicators.getHeight() == 256, "indicator sheet dimensions");
        for (int row = 0; row < 3; row++) {
            signatures.clear();
            for (int tick = 0; tick < 16; tick++) {
                int frame = DistillationAnimation.motionFrame(tick);
                check(frame == tick / 4, "four world ticks per motion frame");
                if (tick % 4 == 0) check(signatures.add(signature(indicators, row * 16 + frame)),
                        "distinct animation frame in row " + row);
            }
        }
        check(DistillationAnimation.motionFrame(16) == 0, "motion wraps");
        check(DistillationAnimation.receiverFrame(0, false, 0) == 0, "empty receiver");
        check(DistillationAnimation.receiverFrame(1, false, 0) == 1, "a trace is visible");
        check(DistillationAnimation.receiverFrame(4000, false, 0) == 4, "half full receiver");
        check(DistillationAnimation.receiverFrame(8000, false, 0) == 8, "full receiver");
        check(DistillationAnimation.receiverFrame(8000, true, 0) == 9, "output warning");
        check(DistillationAnimation.receiverFrame(250, false, 10) == 10, "completion pulse");
        System.out.println("PASS indicator frame timing, distinct motion cells and truthful receiver bands");
    }

    private static int signature(BufferedImage atlas, int cell) {
        int sum = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pixel = atlas.getRGB(cell % 16 * 16 + x, cell / 16 * 16 + y);
                check((pixel >>> 24) == 255, "opaque mapped pixel in cell " + cell);
                sum = sum * 31 + pixel;
            }
        }
        return sum;
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
