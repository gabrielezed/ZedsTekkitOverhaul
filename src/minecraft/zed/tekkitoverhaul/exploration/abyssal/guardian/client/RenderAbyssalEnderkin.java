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
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalCustodian;

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
        private final ModelRenderer[] shins = new ModelRenderer[2], fins = new ModelRenderer[2];
        private float partial;

        @Override public void setLivingAnimations(EntityLiving actor, float stride, float swing, float partial) {
            this.partial = partial;
        }

        CasterModel() {
            textureWidth = textureHeight = 64;
            head = box(0, 0, 0, 9, 0, -3.5F, -6, -4, 7, 6, 7);
            gillLeft = box(30, 0, -3.2F, -2, 0, -1, -2, -1, 1, 4, 5);
            gillRight = box(43, 0, 3.2F, -2, 0, 0, -2, -1, 1, 4, 5);
            head.addChild(gillLeft);
            head.addChild(gillRight);
            torso = box(0, 14, 0, 9, 0, -3, 0, -1.5F, 6, 8, 4);
            right = box(22, 14, -4, 9, 0, -1, 0, -1, 2, 4, 2);
            elbow = box(31, 14, 0, 3.8F, 0, -0.85F, 0, -1, 2, 3, 2);
            right.addChild(elbow);
            left = box(41, 14, 4, 9, 0, -1, 0, -1, 2, 7, 2);
            rightLeg = box(0, 29, -1.5F, 16, 0.5F, -1, 0, -1, 2, 4, 2);
            leftLeg = box(12, 29, 1.5F, 16, 0.5F, -1, 0, -1, 2, 4, 2);
            for (int i = 0; i < 2; i++) {
                shins[i] = box(24, 29, 0, 3.7F, 0, -0.75F, 0, -0.75F, 2, 3, 2);
                fins[i] = box(0, 40, 0, 2.8F, 0, -1.5F, -0.5F, 0, 3, 1, 5);
                shins[i].addChild(fins[i]);
            }
            rightLeg.addChild(shins[0]);
            leftLeg.addChild(shins[1]);
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
            float travel = (float) Math.hypot(caster.motionX, caster.motionZ);
            float effort = Math.min(1, travel * 6);
            float phaseTime = age * 0.31F;
            float vitality = 1 - CustodianBalance.ease(caster.deathTime / 17F);
            float stroke = (0.12F + effort * 0.69F) * vitality;
            float windup = CustodianBalance.ease(phase / 8.0F);
            float recoil = phase < 0 ? CustodianBalance.ease((-phase + 1 - partial) / 8) : 0;
            float casting = Math.max(windup, recoil);
            float flinch = AquaticHurtPose.recoil(caster, partial, 0);
            float tailFlinch = AquaticHurtPose.recoil(caster, partial, 2);
            float lateral = AquaticHurtPose.lateral(caster);
            float backward = AquaticHurtPose.backward(caster);
            float turn = EntityAbyssalCustodian.wrap(caster.rotationYaw - caster.prevRotationYaw);
            float bank = Math.max(-0.28F, Math.min(0.28F, turn * 0.043F)) * (1 - casting);
            float climb = (float) Math.max(-0.22, Math.min(0.22, caster.motionY * 1.7));
            float lean = (-0.16F - 0.76F * effort - climb) * (1 - casting);
            lean += backward * flinch * 0.19F;
            bank += lateral * flinch * 0.19F;
            head.rotateAngleX = pitch * 0.017453292F - lean * 0.63F - (0.16F + 0.37F * backward) * flinch;
            head.rotateAngleY = -bank * 0.58F + lateral * flinch * 0.2F;
            head.rotateAngleZ = -bank * 0.6F;
            float breath = 0.11F + 0.08F * MathHelper.sin(age * 0.19F) * vitality + 0.25F * flinch + 0.13F * casting;
            gillLeft.rotateAngleY = -breath;
            gillRight.rotateAngleY = breath;
            gillLeft.rotateAngleZ = -0.23F - breath * 0.45F;
            gillRight.rotateAngleZ = 0.23F + breath * 0.45F;
            float driftingArm = -0.16F + 0.65F * effort + flinch * 0.6F;
            right.rotateAngleX = phase > 0 ? EntityAbyssalEnderkin.armPitch(caster.rotationPitch, phase)
                    : driftingArm + (EntityAbyssalEnderkin.armPitch(caster.rotationPitch, phase) - driftingArm) * recoil;
            right.rotateAngleZ = -0.25F * flinch * (1 - casting);
            elbow.rotateAngleX = (-0.22F - 0.60F * flinch) * (1 - casting);
            left.rotateAngleX = driftingArm - casting * 0.63F - flinch;
            left.rotateAngleZ = 0.20F + casting * 0.32F + bank * 0.6F;
            for (int i = 0; i < 2; i++) {
                float sign = i == 0 ? -1 : 1;
                ModelRenderer thigh = i == 0 ? rightLeg : leftLeg;
                thigh.rotateAngleX = 0.25F + sign * stroke * MathHelper.sin(phaseTime) + tailFlinch * 0.34F;
                thigh.rotateAngleZ = sign * (0.08F + tailFlinch * 0.18F) - bank * 0.54F;
                shins[i].rotateAngleX = -0.19F + sign * stroke * MathHelper.sin(phaseTime - 0.75F) * 0.61F;
                fins[i].rotateAngleX = -0.29F + sign * stroke * MathHelper.sin(phaseTime - 1.4F) * 0.76F;
                fins[i].rotateAngleZ = sign * (0.13F + effort * 0.21F);
            }
            // The moving body follows the current. During a cast the hand uses the same
            // unbanked shoulder and pitch as the native projectile origin.
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 9 * scale, 0);
            GL11.glRotatef(bank * 57.29578F, 0, 0, 1);
            GL11.glRotatef(lean * 57.29578F, 1, 0, 0);
            GL11.glTranslatef(0, -9 * scale, 0);
            head.render(scale);
            torso.render(scale);
            left.render(scale);
            rightLeg.render(scale);
            leftLeg.render(scale);
            if (phase == 0) right.render(scale);
            GL11.glPopMatrix();
            if (phase != 0) {
                // Blend back through the body rig on the final recoil frames.
                GL11.glPushMatrix();
                GL11.glTranslatef(0, 9 * scale, 0);
                GL11.glRotatef(bank * (phase > 0 ? 0 : 1 - recoil) * 57.29578F, 0, 0, 1);
                GL11.glRotatef(lean * (phase > 0 ? 0 : 1 - recoil) * 57.29578F, 1, 0, 0);
                GL11.glTranslatef(0, -9 * scale, 0);
                right.render(scale);
                GL11.glPopMatrix();
            }
        }
    }
}
