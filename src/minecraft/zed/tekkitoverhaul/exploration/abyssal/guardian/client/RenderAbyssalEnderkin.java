package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin;

public final class RenderAbyssalEnderkin extends RenderLiving {
    private RenderAbyssalEnderkin() { super(new CasterModel(), 0.27F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityAbyssalEnderkin.class, new RenderAbyssalEnderkin());
    }

    @Override protected void rotateCorpse(EntityLiving entity, float age, float yaw, float partial) {
        super.rotateCorpse(entity, age, entity.rotationYaw, partial);
    }

    private static final class CasterModel extends ModelBase {
        private final ModelRenderer head, torso, right, left, elbow, rightLeg, leftLeg, gillLeft, gillRight;

        CasterModel() {
            textureWidth = textureHeight = 64;
            head = box(0, 0, 0, 9, 0, -3.5F, -5, -3, 7, 5, 6);
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
            float kick = MathHelper.sin(age * 0.39F);
            head.rotateAngleX = pitch * 0.017453292F;
            head.rotateAngleY = 0;
            torso.rotateAngleX = phase == 0 ? -0.13F : 0;
            gillLeft.rotateAngleY = -0.17F + 0.09F * kick;
            gillRight.rotateAngleY = 0.17F - 0.09F * kick;
            right.rotateAngleX = phase == 0 ? -0.2F + kick * 0.23F
                    : EntityAbyssalEnderkin.armPitch(caster.rotationPitch, phase);
            elbow.rotateAngleX = phase > 0 ? -0.19F * (1 - Math.min(phase, 12) / 12.0F) : 0;
            left.rotateAngleX = -0.35F - kick * 0.24F;
            left.rotateAngleZ = 0.23F;
            rightLeg.rotateAngleX = 0.3F + 0.35F * kick;
            leftLeg.rotateAngleX = 0.3F - 0.35F * kick;
            head.render(scale);
            torso.render(scale);
            right.render(scale);
            left.render(scale);
            rightLeg.render(scale);
            leftLeg.render(scale);
        }
    }
}
