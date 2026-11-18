package zed.tekkitoverhaul.survival.aerial.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie;

public final class RenderMagpie extends RenderLiving {
    private RenderMagpie() { super(new MagpieModel(), 0.20F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityMagpie.class, new RenderMagpie());
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
            EntityMagpie.Pose pose = ((EntityMagpie) entity).pose();
            boolean flying = pose == EntityMagpie.Pose.FLY;
            float flap = flying ? MathHelper.sin(age * 1.25F) * 0.65F : 1.44F;
            leftWing.rotateAngleZ = -flap;
            rightWing.rotateAngleZ = flap;
            head.rotateAngleY = pose == EntityMagpie.Pose.WATCH || pose == EntityMagpie.Pose.PERCH ? MathHelper.sin(age * 0.11F) * 0.48F : 0;
            head.rotateAngleX = pose == EntityMagpie.Pose.PECK ? -0.48F - 0.40F * MathHelper.sin(age * 0.8F) : 0;
            tail.rotateAngleX = flying ? 0.12F : 0.30F;
            float step = pose == EntityMagpie.Pose.WALK ? MathHelper.sin(stride * 1.2F) * swing : 0;
            leftFoot.rotateAngleX = flying ? 1 : step;
            rightFoot.rotateAngleX = flying ? 1 : -step;
            body.render(scale);
        }
    }
}
