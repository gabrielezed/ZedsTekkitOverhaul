package zed.tekkitoverhaul.distillation.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.src.ModLoader;
import net.minecraft.src.ModTextureStatic;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;

/** Recolors the installed BuildCraft 2.2.14 fuel bucket at texture registration. */
public final class DistillateBucketTextures {
    private static final String SOURCE = "/net/minecraft/src/buildcraft/core/gui/item_textures.png";
    private static final int SIZE = 16;
    private static final int TILE_X = 48;
    private static final int FLUID = 0xFFC4C00E;

    private DistillateBucketTextures() { }

    public static void register() {
        try {
            BufferedImage atlas = readAtlas();
            if (atlas == null || atlas.getWidth() != 256 || atlas.getHeight() != 256
                    || atlas.getRGB(55, 4) != FLUID) {
                System.err.println("[ZTO] Expected BuildCraft 2.2.14 bucket atlas at " + SOURCE);
                return;
            }
            for (DistillateType type : DistillateType.values()) {
                int index = DistillationRegistry.bucketIcon(type.id);
                ModLoader.addAnimation(new ModTextureStatic(index, 1, recolor(atlas, fuelColor(type))));
                DistillationRegistry.bucketFor(type.id).setIconIndex(index);
            }
        } catch (IOException exception) {
            System.err.println("[ZTO] Cannot read BuildCraft bucket texture: " + exception);
        }
    }

    private static BufferedImage readAtlas() throws IOException {
        InputStream stream = DistillateBucketTextures.class.getResourceAsStream(SOURCE);
        if (stream == null) return null;
        try {
            return ImageIO.read(stream);
        } finally {
            stream.close();
        }
    }

    static BufferedImage recolor(BufferedImage atlas, int color) {
        BufferedImage sprite = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int pixel = atlas.getRGB(TILE_X + x, y);
                // The atlas grid is translucent padding, outside the opaque bucket sprite.
                if ((pixel >>> 24) != 255) continue;
                sprite.setRGB(x, y, pixel == FLUID ? color : pixel);
            }
        }
        return sprite;
    }

    private static int fuelColor(DistillateType type) {
        switch (type) {
            case GASOLINE: return 0xFFD7C578;
            case KEROSENE: return 0xFF95C2B9;
            case DIESEL: return 0xFFBC8E50;
            case HEAVY_GAS_OIL: return 0xFF78594F;
            default: throw new AssertionError(type);
        }
    }
}
