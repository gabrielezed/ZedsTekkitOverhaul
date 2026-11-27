package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelBiped;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.ModelSkeleton;
import net.minecraft.src.ModelZombie;
import net.minecraft.src.Render;
import net.minecraft.src.RenderArrow;
import net.minecraft.src.RenderBiped;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.exploration.abyssal.guardian.DiverArrow;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityDiverSkeleton;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityDiverZombie;

/** Classic bodies with a cosmetic equipment pass in the same animated limb transforms. */
public final class RenderDiver extends RenderBiped {
    private static final String TEXTURE = "/zed/textures/abyssal/diver_equipment.png";
    private final DiveGear gear = new DiveGear();

    private RenderDiver(ModelBiped model) { super(model, 0.40F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityDiverZombie.class, new RenderDiver(new ModelZombie() {
            @Override public void setRotationAngles(float stride, float swing, float age, float yaw, float pitch, float scale) {
                super.setRotationAngles(stride, swing, age, yaw, pitch, scale);
                kick(this, age);
            }
        }));
        renderers.put(EntityDiverSkeleton.class, new RenderDiver(new SwimmingSkeleton()));
        renderers.put(DiverArrow.class, new RenderArrow());
    }

    private static void kick(ModelBiped model, float age) {
        float paddle = MathHelper.sin(age * 0.31F) * 0.31F;
        model.bipedRightLeg.rotateAngleX = 0.15F + paddle;
        model.bipedLeftLeg.rotateAngleX = 0.15F - paddle;
    }

    @Override protected void rotateCorpse(EntityLiving entity, float age, float yaw, float partial) {
        super.rotateCorpse(entity, age, yaw, partial);
        if (entity.isInWater() && entity.isEntityAlive())
            GL11.glRotatef(21 + entity.rotationPitch * 0.22F, 1, 0, 0);
    }

    @Override protected void renderModel(EntityLiving entity, float stride, float swing, float age,
            float yaw, float pitch, float scale) {
        super.renderModel(entity, stride, swing, age, yaw, pitch, scale);
        loadTexture(TEXTURE);
        gear.render(modelBipedMain, scale);
    }

    private static final class SwimmingSkeleton extends ModelSkeleton {
        private boolean drawing;

        @Override public void render(Entity actor, float stride, float swing, float age, float yaw, float pitch, float scale) {
            drawing = ((EntityDiverSkeleton) actor).aiming();
            super.render(actor, stride, swing, age, yaw, pitch, scale);
        }

        @Override public void setRotationAngles(float stride, float swing, float age, float yaw, float pitch, float scale) {
            super.setRotationAngles(stride, swing, age, yaw, pitch, scale);
            kick(this, age);
            bipedRightArm.rotateAngleX = drawing ? -1.24F + pitch * 0.017453292F : -0.55F;
            bipedLeftArm.rotateAngleX = drawing ? -1.05F : -0.25F;
        }
    }

    private static final class DiveGear extends ModelBase {
        private final ModelRenderer goggles, lensLeft, lensRight, mouth, hose, tank, straps, fins;

        DiveGear() {
            textureWidth = textureHeight = 64;
            goggles = box(0, 0, -4, -6, -5, 8, 3, 2);
            lensLeft = box(24, 0, -3.5F, -5.6F, -5.4F, 3, 2, 1);
            lensRight = box(24, 0, 0.5F, -5.6F, -5.4F, 3, 2, 1);
            mouth = box(0, 8, -1.5F, -2.2F, -5.2F, 3, 2, 2);
            hose = box(12, 8, 1.5F, -1.8F, -5.0F, 1, 6, 1);
            tank = box(0, 16, -2.5F, 1, 2, 5, 9, 4);
            tank.setTextureOffset(42, 16).addBox(-1, -0.5F, 3.2F, 2, 2, 2);
            straps = box(22, 16, -3, 0, -2.45F, 1, 9, 5);
            straps.addBox(2, 0, -2.45F, 1, 9, 5);
            fins = box(0, 34, -2, 11, -5, 4, 1, 6);
        }

        private ModelRenderer box(int u, int v, float x, float y, float z, int width, int height, int depth) {
            return new ModelRenderer(this, u, v).addBox(x, y, z, width, height, depth);
        }

        void render(ModelBiped model, float scale) {
            renderPart(model.bipedHead, scale, goggles, lensLeft, lensRight, mouth, hose);
            renderPart(model.bipedBody, scale, tank, straps);
            renderPart(model.bipedRightLeg, scale, fins);
            renderPart(model.bipedLeftLeg, scale, fins);
        }

        private void renderPart(ModelRenderer limb, float scale, ModelRenderer... parts) {
            GL11.glPushMatrix();
            limb.postRender(scale);
            for (ModelRenderer part : parts) part.render(scale);
            GL11.glPopMatrix();
        }
    }
}
