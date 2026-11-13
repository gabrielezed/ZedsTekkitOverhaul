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
        float draw = mob.getAimPose(partialTick);
        float gather = ease(draw * 1.6F);
        float aim = ease((draw - 0.15F) / 0.85F);
        float recoil = pulse(onGround, 0.16F, 0.78F);
        float gait = stride * 0.60F;
        float step = wave(gait) * speed;
        float breath = wave(age * 0.085F + mob.entityId);
        // The render yaw is already wrapped and interpolated by RenderLiving.
        float look = mob.rotationYawHead * RADIANS;
        float lateral = clamp((float) ((mob.posX - mob.prevPosX) * Math.cos(look)
                + (mob.posZ - mob.prevPosZ) * Math.sin(look)) * 3, -1, 1) * speed;
        chest.rotateAngleX = 0.07F + speed * 0.16F + gather * 0.13F - recoil * 0.20F + breath * 0.018F;
        chest.rotateAngleY = clamp(yaw, -0.8F, 0.8F) * gather * 0.68F + step * 0.17F - recoil * 0.11F;
        chest.rotateAngleZ = lateral * 0.24F + wave(gait - 0.55F) * speed * 0.05F + breath * 0.013F;
        head.rotateAngleX = clamp(pitch - chest.rotateAngleX * 0.84F + wave(age * 0.061F) * 0.022F, -0.6F, 0.6F);
        head.rotateAngleY = clamp(yaw - chest.rotateAngleY, -0.96F, 0.96F);
        head.rotateAngleZ = -chest.rotateAngleZ * 0.8F;
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            float phase = gait + (i == 0 ? 0 : 3.18F);
            float lift = ease((wave(phase - 2.08F) + 0.28F) / 1.28F) * speed;
            thighs[i].rotateAngleX = wave(phase) * speed * 0.78F - lift * 0.09F;
            thighs[i].rotateAngleZ = -side * (0.035F + Math.abs(lateral) * 0.16F) + lateral * 0.12F;
            shins[i].rotateAngleX = lift * 0.86F;
            feet[i].rotateAngleX = -(thighs[i].rotateAngleX + shins[i].rotateAngleX) * 0.78F + lift * 0.19F;
            arms[i].rotateAngleX = 0.12F - wave(phase - 0.40F) * speed * 0.35F * (1 - aim)
                    - gather * 0.22F - aim * (i == 0 ? 1.11F : 0.71F) - recoil * 0.47F;
            arms[i].rotateAngleZ = -side * (0.13F + aim * 0.19F + recoil * 0.32F + breath * side * 0.019F);
            forearms[i].rotateAngleX = -0.23F - wave(phase - 0.9F) * speed * 0.14F * (1 - aim)
                    - aim * (i == 0 ? 0.55F : 1.02F) + recoil * 0.24F;
            forearms[i].rotateAngleY = side * aim * 0.24F;
        }
    }
}
