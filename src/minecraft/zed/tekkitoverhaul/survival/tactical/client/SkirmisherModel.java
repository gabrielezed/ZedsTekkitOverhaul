package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
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
        float aim = ease(mob.getAimPose(partialTick));
        float recoil = onGround == 0 ? 0 : (1 - ease(onGround)) * (1 - aim);
        float gait = stride * 0.6662F;
        float step = (float) Math.cos(gait) * speed;
        float breath = (float) Math.sin(age * 0.12F);
        float look = mob.prevRotationYawHead + (mob.rotationYawHead - mob.prevRotationYawHead) * partialTick;
        float lateral = clamp((float) ((mob.posX - mob.prevPosX) * Math.cos(look * RADIANS)
                + (mob.posZ - mob.prevPosZ) * Math.sin(look * RADIANS)) * 4, -1, 1) * speed;
        chest.rotateAngleX = 0.08F + speed * 0.17F + aim * 0.10F - recoil * 0.24F + breath * 0.018F;
        chest.rotateAngleY = clamp(yaw, -0.8F, 0.8F) * (aim * 0.76F + recoil * 0.5F) + step * 0.16F;
        chest.rotateAngleZ = lateral * 0.23F;
        head.rotateAngleX = clamp(pitch - chest.rotateAngleX, -0.6F, 0.6F);
        head.rotateAngleY = clamp(yaw - chest.rotateAngleY, -0.96F, 0.96F);
        head.rotateAngleZ = -chest.rotateAngleZ * 0.8F;
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            float leg = side * step;
            thighs[i].rotateAngleX = leg * 0.92F - speed * 0.09F;
            thighs[i].rotateAngleZ = -side * (0.035F + Math.abs(lateral) * 0.16F) + lateral * 0.12F;
            shins[i].rotateAngleX = Math.max(0, -leg) * 0.8F;
            feet[i].rotateAngleX = -(thighs[i].rotateAngleX + shins[i].rotateAngleX) * 0.6F;
            arms[i].rotateAngleX = 0.12F - leg * 0.34F - aim * (i == 0 ? 1.33F : 0.93F) - recoil * 0.63F;
            arms[i].rotateAngleZ = -side * (0.13F + aim * 0.22F + recoil * 0.43F);
            forearms[i].rotateAngleX = -0.24F - aim * (i == 0 ? 0.56F : 1.1F) + recoil * 0.16F;
            forearms[i].rotateAngleY = side * aim * 0.24F;
        }
    }
}
