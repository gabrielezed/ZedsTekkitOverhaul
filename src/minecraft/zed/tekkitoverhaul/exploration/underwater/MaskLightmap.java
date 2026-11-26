package zed.tekkitoverhaul.exploration.underwater;

import java.util.function.BiConsumer;
import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Material;
import net.minecraft.src.RenderEngine;
import org.lwjgl.opengl.GL11;

/** Retains a separate upload buffer so the native and OptiFine colors stay authoritative. */
final class MaskLightmap {
    static final BiConsumer<Object[], int[]> CALLBACK = new BiConsumer<Object[], int[]>() {
        @Override public void accept(Object[] args, int[] info) {
            apply(args[0], (RenderEngine) args[1], (int[]) args[2], info[0], info[1], info[2]);
        }
    };
    private static final int[] enhanced = new int[256];
    private static int[] activeSource;
    private static int activeTexture, appliedSerial;

    private MaskLightmap() { }

    private static void apply(Object camera, RenderEngine engine, int[] source, int block, int texture, int serial) {
        if (engine == null || source == null || source.length != enhanced.length) return;
        boolean active = activeSource == source && activeTexture == texture;
        Block view = block > 0 && block < Block.blocksList.length ? Block.blocksList[block] : null;
        EntityPlayer local = ModLoader.getMinecraftInstance().thePlayer;
        boolean use = view != null && view.blockMaterial == Material.water && local != null && camera == local
                && UnderwaterHooks.wears(local, 3, UnderwaterRegistry.mask);
        if (!use) {
            if (active && appliedSerial == serial) upload(engine, source, texture);
            if (active) activeSource = null;
            return;
        }
        if (active && appliedSerial == serial) return;
        for (int i = 0; i < source.length; i++) {
            int argb = source[i];
            enhanced[i] = (argb & 0xff000000) | (lift(argb >>> 16 & 255) << 16)
                    | (lift(argb >>> 8 & 255) << 8) | lift(argb & 255);
        }
        upload(engine, enhanced, texture);
        activeSource = source;
        activeTexture = texture;
        appliedSerial = serial;
    }

    private static int lift(int value) {
        return value + Math.round(UnderwaterBalance.MASK_BRIGHTNESS * (255 - value));
    }

    private static void upload(RenderEngine engine, int[] colors, int texture) {
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            engine.createTextureFromBytes(colors, 16, 16, texture);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
        }
    }
}
