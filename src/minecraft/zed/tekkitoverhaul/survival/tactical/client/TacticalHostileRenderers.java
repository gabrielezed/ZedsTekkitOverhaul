package zed.tekkitoverhaul.survival.tactical.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.ModelBiped;
import net.minecraft.src.Render;
import net.minecraft.src.RenderBiped;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;

public final class TacticalHostileRenderers {
    private TacticalHostileRenderers() {}

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntitySkirmisher.class, new TacticalRenderer(false));
        renderers.put(EntityBreacher.class, new TacticalRenderer(true));
    }

    private static final class TacticalRenderer extends RenderBiped {
        private final boolean breacher;

        TacticalRenderer(boolean breacher) {
            super(new ModelBiped(breacher ? 0.25F : 0.0F), breacher ? 0.48F : 0.32F);
            this.breacher = breacher;
            modelBipedMain.bipedHeadwear.isHidden = true;
            modelBipedMain.heldItemRight = breacher ? 0 : 1;
        }

        @Override
        protected void preRenderCallback(EntityLiving mob, float partialTick) {
            modelBipedMain.aimedBow = !breacher && ((EntitySkirmisher) mob).isAiming();
            // RenderLiving encloses this callback in its model-view matrix push/pop.
            if (breacher) GL11.glScalef(1.17F, 0.96F, 1.10F);
            else GL11.glScalef(0.87F, 0.87F, 0.87F);
        }
    }
}
