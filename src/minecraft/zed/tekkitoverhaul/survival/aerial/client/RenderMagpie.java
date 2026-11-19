package zed.tekkitoverhaul.survival.aerial.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie;

public final class RenderMagpie extends RenderLiving {
    private RenderMagpie() { super(new MagpieModel(), 0.20F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityMagpie.class, new RenderMagpie());
    }

    @Override
    protected void renderModel(EntityLiving bird, float stride, float swing, float age, float yaw, float pitch, float scale) {
        GL11.glPushAttrib(GL11.GL_CURRENT_BIT);
        try {
            GL11.glColor4f(0.88F, 0.89F, 0.92F, 1.0F);
            super.renderModel(bird, stride, swing, age, yaw, pitch, scale);
        } finally {
            GL11.glPopAttrib();
        }
    }

    @Override
    protected void renderEquippedItems(EntityLiving entity, float partialTick) {
        ItemStack item = ((EntityMagpie) entity).carriedItem();
        if (item == null) return;
        MagpieModel model = (MagpieModel) mainModel;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        try {
            model.body.postRender(0.0625F);
            model.head.postRender(0.0625F);
            GL11.glTranslatef(0, 0.016F, -0.25F);
            GL11.glRotatef(90, 0, 1, 0);
            GL11.glScalef(0.115F, 0.115F, 0.115F);
            // Center the native equipped-item quad on the beak tip.
            GL11.glTranslatef(0.24F, -0.57F, -0.24F);
            renderManager.itemRenderer.renderItem(entity, item, 0);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static final class MagpieModel extends ModelBase {
        private final ModelRenderer body, head, tail, leftWing, rightWing, leftFoot, rightFoot;

        MagpieModel() {
            textureWidth = 32;
            textureHeight = 32;
            body = box(0, 0, 0, 21, 0, -2, -2, -3, 4, 3, 6);
            head = box(0, 10, 0, -2, -2, -1.5F, -2, -2, 3, 3, 3);
            head.addChild(box(12, 11, 0, 0, -2, -0.5F, -0.25F, -2, 1, 1, 2));
            body.addChild(head);
            tail = box(0, 18, 0, 0, 3, -1, -0.5F, 0, 2, 1, 6);
            body.addChild(tail);
            leftWing = box(16, 16, -1.8F, -1, -1, -4, 0, -1, 4, 1, 4);
            rightWing = box(16, 16, 1.8F, -1, -1, 0, 0, -1, 4, 1, 4);
            body.addChild(leftWing);
            body.addChild(rightWing);
            leftFoot = box(22, 2, -1, 1, 0, -0.5F, 0, -1, 1, 2, 2);
            rightFoot = box(22, 2, 1, 1, 0, -0.5F, 0, -1, 1, 2, 2);
            body.addChild(leftFoot);
            body.addChild(rightFoot);
        }

        private ModelRenderer box(int u, int v, float pivotX, float pivotY, float pivotZ,
                float x, float y, float z, int w, int h, int d) {
            ModelRenderer part = new ModelRenderer(this, u, v);
            part.setRotationPoint(pivotX, pivotY, pivotZ);
            return part.addBox(x, y, z, w, h, d);
        }

        @Override
        public void render(Entity entity, float stride, float swing, float age, float yaw, float pitch, float scale) {
            EntityMagpie bird = (EntityMagpie) entity;
            EntityMagpie.Pose pose = bird.pose();
            float partial = age - bird.ticksExisted;
            boolean flying = pose == EntityMagpie.Pose.FLY || pose == EntityMagpie.Pose.CLIMB;
            boolean hopping = pose == EntityMagpie.Pose.HOP;
            boolean landing = pose == EntityMagpie.Pose.LAND;
            boolean perched = pose == EntityMagpie.Pose.PERCH;
            float flap = 1.44F;
            if (flying) flap = 0.18F + MathHelper.sin(age * 1.45F) * 0.65F;
            else if (hopping) flap = 1.10F;
            else if (landing) flap = 0.82F;
            leftWing.rotateAngleZ = -flap;
            rightWing.rotateAngleZ = flap;
            head.rotateAngleY = (yaw + bird.headYaw(partial)) * (float) (Math.PI / 180);
            head.rotateAngleX = bird.headPitch(partial) * (float) (Math.PI / 180);
            if (pose == EntityMagpie.Pose.PECK) head.rotateAngleX += 0.28F + 0.23F * MathHelper.sin(age * 0.8F);
            float step = MathHelper.sin(bird.gait(partial)) * bird.stride(partial);
            body.rotateAngleX = flying || hopping ? (float) (-bird.motionY * 0.9) : landing ? 0.16F : 0;
            body.rotationPointY = 21 + (landing || perched ? 0.55F : -0.20F * Math.abs(step));
            tail.rotateAngleX = flying ? 0.12F : perched ? 0.52F : 0.30F;
            float tuck = flying ? 0.95F : hopping ? 0.60F : 0;
            leftFoot.rotateAngleX = tuck + step * 0.86F;
            rightFoot.rotateAngleX = tuck - step * 0.86F;
            leftFoot.rotationPointY = 1 - Math.max(0, step) * 0.7F;
            rightFoot.rotationPointY = 1 - Math.max(0, -step) * 0.7F;
            body.render(scale);
        }
    }
}
