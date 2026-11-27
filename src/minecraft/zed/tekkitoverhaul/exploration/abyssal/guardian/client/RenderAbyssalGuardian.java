package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalGuardian;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalGuardian.State;

public final class RenderAbyssalGuardian extends RenderLiving {
    private RenderAbyssalGuardian() { super(new GuardianModel(), 0.25F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityAbyssalGuardian.class, new RenderAbyssalGuardian());
    }

    private static final class GuardianModel extends ModelBase {
        private final ModelRenderer body, brow, jaw, peduncle, tail, dorsal, port, starboard;

        GuardianModel() {
            textureWidth = 64;
            textureHeight = 64;
            body = box(0, 0, 0, 17, 0, -5, -3, -6, 10, 6, 12);
            brow = box(0, 20, 0, -1, -5, -4, -3, -5, 8, 5, 6);
            jaw = box(0, 32, 0, 2, -6, -3, -0.5F, -5, 6, 2, 6);
            peduncle = box(34, 20, 0, -0.5F, 5, -2.5F, -2, 0, 5, 4, 7);
            tail = box(42, 0, 0, 0, 6, -0.5F, -4, 0, 1, 8, 7);
            dorsal = box(48, 31, 0, -2.5F, -2, -0.5F, -4, 0, 1, 5, 6);
            port = box(0, 42, -4.5F, 1.5F, -3, -7, -0.5F, -1, 7, 1, 6);
            starboard = box(29, 42, 4.5F, 1.5F, -3, 0, -0.5F, -1, 7, 1, 6);
            body.addChild(brow);
            brow.addChild(jaw);
            body.addChild(peduncle);
            peduncle.addChild(tail);
            body.addChild(dorsal);
            body.addChild(port);
            body.addChild(starboard);
        }

        private ModelRenderer box(int u, int v, float pivotX, float pivotY, float pivotZ,
                float x, float y, float z, int width, int height, int length) {
            ModelRenderer part = new ModelRenderer(this, u, v);
            part.setRotationPoint(pivotX, pivotY, pivotZ);
            return part.addBox(x, y, z, width, height, length);
        }

        @Override public void render(Entity entity, float stride, float swing, float age, float yaw, float pitch, float scale) {
            EntityAbyssalGuardian guardian = (EntityAbyssalGuardian) entity;
            State state = guardian.state();
            float time = age * (state == State.LUNGE ? 1.28F : state == State.PURSUE ? 0.80F : 0.40F);
            float kick = state == State.WIND_UP ? 0.06F : state == State.RECOVER ? 0.14F : 0.36F;
            float wave = MathHelper.sin(time);
            body.rotateAngleX = pitch * (float) (Math.PI / 180);
            body.rotateAngleZ = MathHelper.sin(time * 0.8F) * kick * 0.15F;
            body.rotationPointY = 17 + MathHelper.sin(time * 0.6F) * kick;
            peduncle.rotateAngleY = wave * kick;
            tail.rotateAngleY = MathHelper.sin(time - 0.8F) * kick * 1.25F;
            dorsal.rotateAngleZ = -body.rotateAngleZ;
            port.rotateAngleZ = -0.25F + wave * kick * 0.6F;
            starboard.rotateAngleZ = 0.25F - wave * kick * 0.6F;
            port.rotateAngleY = state == State.WIND_UP ? 0.42F : state == State.LUNGE ? -0.7F : -0.24F;
            starboard.rotateAngleY = -port.rotateAngleY;
            jaw.rotateAngleX = state == State.WIND_UP ? 0.28F : state == State.LUNGE ? 0.58F : 0.03F;
            body.render(scale);
        }
    }
}
