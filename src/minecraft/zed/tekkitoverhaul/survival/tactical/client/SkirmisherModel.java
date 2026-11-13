package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;

final class SkirmisherModel extends EnderkinModel {
    private final ModelRenderer chest, head;
    private final ModelRenderer[] arms = new ModelRenderer[2];
    private final ModelRenderer[] forearms = new ModelRenderer[2];
    private final ModelRenderer[] thighs = new ModelRenderer[2];
    private final ModelRenderer[] shins = new ModelRenderer[2];
    private final ModelRenderer[] feet = new ModelRenderer[2];

    SkirmisherModel() {
        super(64);
        body = new ModelRenderer(this);
        body.setRotationPoint(0, 1, 0);
        chest = box(body, 0, 0, -3.5F, -13, -2, 7, 11, 4, 0, 0, 0);
        box(chest, 32, 27, -2, -4, -1.5F, 4, 5, 3, 0, 0, 0.125F);
        head = box(chest, 32, 14, -3, -7.5F, -3, 6, 8, 5, 0, -12.5F, 0);
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            arms[i] = box(chest, i == 0 ? 54 : 0, 14 + i, -1, -1, -1.5F,
                    2, 10, 3, side * 4.25F, -11, 0);
            forearms[i] = box(arms[i], i == 0 ? 56 : 10, i * 15, -1, -1, -1,
                    2, 11, 2, 0, 8.5F, 0);
            box(forearms[i], i == 0 ? 16 : 46, i == 0 ? 28 : 34, -1, -0.5F, -2,
                    2, 4, 3, 0, 9.5F, 0);
            thighs[i] = box(body, 22 + i * 12, 0, -1.5F, -0.5F, -1.5F,
                    3, 11, 3, side * 1.7F, 0, 0);
            shins[i] = box(thighs[i], i == 0 ? 46 : 22, i * 14, -1, 0, -1.5F,
                    2, 11, 3, 0, 10, 0);
            feet[i] = box(shins[i], i == 0 ? 46 : 0, 27 + i, -1.5F, 0, -3.5F,
                    3, 2, 5, 0, 11, 0);
        }
    }

    @Override
    void pose(Entity entity, float stride, float speed, float age, float yaw, float pitch) {
        EntitySkirmisher mob = (EntitySkirmisher) entity;
        float draw = mob.getAimPose(partialTick);
        float gather = ease(draw * 1.45F);
        float aim = ease((draw - 0.15F) / 0.85F);
        float recoil = pulse(onGround, 0.18F, 0.88F);
        float gait = stride * 0.60F;
        float step = wave(gait) * speed;
        float breath = wave(age * 0.085F + mob.entityId);
        float look = mob.rotationYawHead * RADIANS;
        float lateral = clamp((float) ((mob.posX - mob.prevPosX) * Math.cos(look)
                + (mob.posZ - mob.prevPosZ) * Math.sin(look)) * 3, -1, 1) * speed;
        float walk = speed * (1 - 0.7F * gather);
        body.rotationPointY = 1 + walk * (0.30F - wave(gait * 2 - 0.35F) * 0.30F);
        chest.rotateAngleX = 0.07F + walk * 0.16F + gather * 0.08F - recoil * 0.15F + breath * 0.018F;
        chest.rotateAngleY = clamp(yaw, -0.8F, 0.8F) * gather * 0.68F + step * 0.17F - recoil * 0.11F;
        chest.rotateAngleZ = lateral * 0.24F + wave(gait - 0.55F) * speed * 0.05F + breath * 0.013F;
        head.rotateAngleX = clamp(pitch - chest.rotateAngleX * 0.84F + wave(age * 0.061F) * 0.022F, -0.6F, 0.6F);
        head.rotateAngleY = clamp(yaw - chest.rotateAngleY, -0.96F, 0.96F);
        head.rotateAngleZ = -chest.rotateAngleZ * 0.8F;
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            float phase = gait + (i == 0 ? 0 : 3.18F);
            float lift = ease((wave(phase - 2.08F) + 0.28F) / 1.28F) * speed;
            thighs[i].rotateAngleX = wave(phase) * speed * 0.70F - lift * 0.09F;
            thighs[i].rotateAngleZ = -side * (0.035F + Math.abs(lateral) * 0.16F) + lateral * 0.12F;
            shins[i].rotateAngleX = lift * 0.86F;
            feet[i].rotateAngleX = -(thighs[i].rotateAngleX + shins[i].rotateAngleX) * 0.78F + lift * 0.19F;
            arms[i].rotateAngleX = 0.12F - wave(phase - 0.52F) * walk * 0.35F
                    - gather * 0.40F - aim * (i == 0 ? 0.76F : 0.67F) - recoil * 0.46F;
            arms[i].rotateAngleZ = side * (-0.13F + aim * 0.27F - recoil * 0.30F) + breath * 0.019F;
            arms[i].rotateAngleY = side * (aim * 0.25F - recoil * 0.20F);
            forearms[i].rotateAngleX = -0.23F - wave(phase - 1.02F) * walk * 0.14F
                    - aim * (i == 0 ? 0.30F : 0.44F) - recoil * 0.18F;
        }
    }

    void renderCharge(EntitySkirmisher mob, float age, float yaw, float scale) {
        float draw = mob.getAimPose(partialTick);
        float charge = ease((draw - 0.32F) / 0.68F) * (1 - ease(mob.getSwingProgress(partialTick) * 10));
        if (charge <= 0) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 24 * scale, 0);
            GL11.glRotatef(yaw, 0, 1, 0);
            GL11.glTranslated(0, -EntitySkirmisher.CAST_HEIGHT + 0.12D * (1 - charge),
                    -EntitySkirmisher.CAST_REACH + 0.22D * (1 - charge));
            EnderPulseRenderer.draw(age, 0.24F + charge * 0.58F);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
