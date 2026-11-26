package zed.tekkitoverhaul.exploration.motorboat.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.src.Item;
import net.minecraft.src.ModLoader;
import net.minecraft.src.ModTextureStatic;
import zed.tekkitoverhaul.exploration.motorboat.MotorboatRegistry;

/** Combines owned pixel layouts with the installed legacy textures at client startup. */
final class MotorboatArtwork {
    private static final String ITEMS = "/gui/items.png";
    private static final String ART = "/zed/textures/motorboat/";
    private static final String COMBUSTION = "/net/minecraft/src/buildcraft/energy/gui/base_iron.png";
    private static final String IC2 = "/ic2/sprites/block_machine.png";
    private static final int[] GUIDE = {0xFFE3E2DE, 0xFFC4C6C6, 0xFFA1AAAE, 0xFF858D92, 0xFF464B4E, 0xFF302E2A};
    private static final int[][] SAMPLES = {{20, 4}, {0, 17}, {94, 56}, {95, 62}, {82, 58}, {21, 12}};

    private static int motorSlot;
    private static int boatSlot;
    private static int[] sampledMetal;

    private MotorboatArtwork() {}

    static void reserveSlots() {
        motorSlot = ModLoader.getUniqueSpriteIndex(ITEMS);
        boatSlot = ModLoader.getUniqueSpriteIndex(ITEMS);
    }

    static void registerIcons() {
        ModLoader.addAnimation(new ModTextureStatic(motorSlot, 1, metalSurface("outboard.png")));
        ModLoader.addAnimation(new ModTextureStatic(boatSlot, 1, motorboatIcon()));
        MotorboatRegistry.outboardItem().setIconIndex(motorSlot);
        MotorboatRegistry.motorboatItem().setIconIndex(boatSlot);
    }

    static BufferedImage motorboatIcon() {
        BufferedImage atlas = read(ITEMS);
        if (atlas == null || atlas.getWidth() != 256 || atlas.getHeight() != 256) {
            throw new IllegalStateException("Missing Minecraft 1.2.5 inventory atlas " + ITEMS);
        }
        int vanillaSlot = Item.boat.getIconFromDamage(0);
        int x = vanillaSlot % 16 * 16;
        int y = vanillaSlot / 16 * 16;
        BufferedImage overlay = metalSurface("motorboat_overlay.png");
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                if ((overlay.getRGB(col, row) >>> 24) == 0) {
                    overlay.setRGB(col, row, atlas.getRGB(x + col, y + row));
                }
            }
        }
        return overlay;
    }

    static BufferedImage engine() { return metalSurface("engine.png"); }

    private static BufferedImage metalSurface(String name) {
        BufferedImage image = read(ART + name);
        if (image == null) throw new IllegalStateException("Missing Motorboat artwork " + name);
        if (sampledMetal == null) {
            BufferedImage combustion = read(COMBUSTION);
            BufferedImage ic2 = read(IC2);
            sampledMetal = GUIDE.clone();
            for (int i = 0; i < SAMPLES.length; i++) {
                BufferedImage source = i < 2 || i == 5 ? combustion : ic2;
                int x = SAMPLES[i][0];
                int y = SAMPLES[i][1];
                if (source != null && source.getWidth() > x && source.getHeight() > y) {
                    int pixel = source.getRGB(x, y);
                    if ((pixel >>> 24) == 255) sampledMetal[i] = pixel;
                }
            }
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                for (int i = 0; i < GUIDE.length; i++) {
                    if (pixel == GUIDE[i]) {
                        image.setRGB(x, y, sampledMetal[i]);
                        break;
                    }
                }
            }
        }
        return image;
    }

    private static BufferedImage read(String path) {
        InputStream stream = MotorboatArtwork.class.getResourceAsStream(path);
        if (stream == null) return null;
        try {
            try {
                return ImageIO.read(stream);
            } finally {
                stream.close();
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read Motorboat texture " + path, failure);
        }
    }
}
