package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import net.minecraft.src.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin;

public final class RenderAbyssalEnderkin extends RenderLiving {
    private RenderAbyssalEnderkin() {
        super(new CasterModel(), 0.27F);
        setRenderPassModel(new CasterModel());
    }

    @Override protected int shouldRenderPass(EntityLiving actor, int pass, float partial) {
        if (pass != 0) return -1;
        loadTexture("/zed/textures/abyssal/enderkin_eyes.png");
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GL11.glColor4f(1, 1, 1, ((EntityAbyssalEnderkin) actor).casting() > 0 ? 0.86F : 0.62F);
        return 1;
    }

    @Override protected int inheritRenderPass(EntityLiving actor, int pass, float partial) { return -1; }

    @Override protected void renderEquippedItems(EntityLiving actor, float partial) {
        GL11.glDepthMask(true);
        GL11.glColor4f(1, 1, 1, 1);
        int light = actor.getBrightnessForRender(partial);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
    }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityAbyssalEnderkin.class, new RenderAbyssalEnderkin());
    }

    @Override protected void rotateCorpse(EntityLiving entity, float age, float yaw, float partial) {
        super.rotateCorpse(entity, age, entity.rotationYaw, partial);
    }

    private static final class CasterModel extends ModelBase {
        private final ModelRenderer head, torso, right, left, elbow, rightLeg, leftLeg, gillLeft, gillRight;
        private float partial;

        @Override public void setLivingAnimations(EntityLiving actor, float stride, float swing, float partial) {
            this.partial = partial;
        }

        CasterModel() {
            textureWidth = textureHeight = 64;
            head = box(0, 0, 0, 9, 0, -3.5F, -6, -3, 7, 6, 6);
            gillLeft = box(28, 0, -3.5F, -1, 0, -1, -2, -2, 1, 3, 4);
            gillRight = box(40, 0, 3.5F, -1, 0, 0, -2, -2, 1, 3, 4);
            head.addChild(gillLeft);
            head.addChild(gillRight);
            torso = box(0, 13, 0, 9, 0, -3, 0, -2, 6, 7, 4);
            right = box(22, 13, -4, 9, 0, -1, 0, -1, 2, 4, 2);
            elbow = box(31, 13, 0, 4, 0, -0.85F, 0, -1, 2, 3, 2);
            right.addChild(elbow);
            left = box(41, 13, 4, 9, 0, -1, 0, -1, 2, 7, 2);
            rightLeg = box(0, 28, -1.6F, 16, 0, -1, 0, -1, 2, 7, 2);
            leftLeg = box(12, 28, 1.6F, 16, 0, -1, 0, -1, 2, 7, 2);
            rightLeg.addChild(box(24, 28, 0, 6, 0, -1.5F, 0, 0, 3, 1, 3));
            leftLeg.addChild(box(38, 28, 0, 6, 0, -1.5F, 0, 0, 3, 1, 3));
        }

        private ModelRenderer box(int u, int v, float px, float py, float pz, float x, float y, float z,
                int width, int height, int depth) {
            ModelRenderer part = new ModelRenderer(this, u, v);
            part.setRotationPoint(px, py, pz);
            return part.addBox(x, y, z, width, height, depth);
        }

        @Override public void render(Entity actor, float stride, float swing, float age, float yaw, float pitch, float scale) {
            EntityAbyssalEnderkin caster = (EntityAbyssalEnderkin) actor;
            int phase = caster.casting();
            float travel = (float) Math.sqrt(caster.motionX * caster.motionX + caster.motionZ * caster.motionZ);
            float stroke = Math.min(1, 0.25F + travel * 5);
            float kick = MathHelper.sin(age * 0.30F) * stroke;
            float glide = MathHelper.sin(age * 0.15F);
            float rise = phase > 0 ? Math.min(1, phase / 8.0F) : 0;
            float flinch = AquaticHurtPose.recoil(caster, partial, 0);
            float legs = AquaticHurtPose.recoil(caster, partial, 2);
            float lateral = AquaticHurtPose.lateral(caster);
            float backward = AquaticHurtPose.backward(caster);
            head.rotateAngleX = pitch * 0.017453292F - (0.16F + 0.37F * backward) * flinch;
            head.rotateAngleY = lateral * flinch * 0.35F;
            head.rotateAngleZ = -lateral * flinch * 0.26F;
            torso.rotateAngleX = (-0.10F - 0.16F * stroke) * (1 - rise) + backward * flinch * 0.21F;
            torso.rotateAngleZ = lateral * flinch * 0.21F;
            gillLeft.rotateAngleY = -0.17F + 0.09F * kick - 0.27F * flinch;
            gillRight.rotateAngleY = 0.17F - 0.09F * kick + 0.27F * flinch;
            // Keep the casting shoulder and hand aligned with the authoritative pulse origin.
            right.rotateAngleX = phase == 0 ? -0.2F + kick * 0.23F + flinch * 0.65F
                    : EntityAbyssalEnderkin.armPitch(caster.rotationPitch, phase);
            right.rotateAngleZ = phase == 0 ? -0.28F * flinch : 0;
            elbow.rotateAngleX = phase > 0 ? -0.19F * (1 - Math.min(phase, 12) / 12.0F)
                    : phase == 0 ? -0.76F * flinch : 0;
            left.rotateAngleX = -0.30F - kick * 0.28F - rise * 0.28F - flinch * 0.66F;
            left.rotateAngleZ = 0.19F + rise * 0.22F + glide * 0.05F + flinch * 0.39F;
            rightLeg.rotateAngleX = 0.3F + 0.35F * kick + 0.46F * legs;
            leftLeg.rotateAngleX = 0.3F - 0.35F * kick + 0.26F * legs;
            rightLeg.rotateAngleZ = -0.22F * legs;
            leftLeg.rotateAngleZ = 0.21F * legs;
            head.render(scale);
            torso.render(scale);
            right.render(scale);
            left.render(scale);
            rightLeg.render(scale);
            leftLeg.render(scale);
        }
    }
}
