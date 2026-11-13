package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;

final class BreacherModel extends EnderkinModel {
    private final ModelRenderer chest, head;
    private final ModelRenderer[] arms = new ModelRenderer[2];
    private final ModelRenderer[] forearms = new ModelRenderer[2];
    private final ModelRenderer[] fists = new ModelRenderer[2];
    private final ModelRenderer[] thighs = new ModelRenderer[2];
    private final ModelRenderer[] shins = new ModelRenderer[2];
    private final ModelRenderer[] feet = new ModelRenderer[2];

    BreacherModel() {
        super(128);
        body = new ModelRenderer(this);
        body.setRotationPoint(0, 6, 0);
        chest = box(body, 0, 0, -6, -14, -3.5F, 12, 11, 7, 0, 0, 0);
        box(chest, 0, 18, -4, -5, -3, 8, 6, 6, 0, 0, 0);
        head = box(chest, 52, 30, -3, -5, -4, 6, 6, 6, 0, -12, -2.5F);
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            arms[i] = box(chest, 38 + i * 28, 0, -3.5F, -3, -3.5F,
                    7, 8, 7, side * 8.25F, -11.25F, 0);
            forearms[i] = box(arms[i], i == 0 ? 94 : 38, i * 15, -4, -1, -3,
                    8, 9, 6, side * 0.4F, 4.5F, 0);
            fists[i] = box(forearms[i], 76, 30 + i * 11, -4, -0.5F, -4,
                    8, 4, 7, 0, 7.5F, 0);
            thighs[i] = box(body, 66 + i * 22, 15, -2.5F, -1, -3,
                    5, 9, 6, side * 3.25F, 0, 0);
            shins[i] = box(thighs[i], 110, 15 + i * 12, -2, -0.5F, -2.5F,
                    4, 7, 5, 0, 8, 0);
            feet[i] = box(shins[i], i * 26, 30, -2.5F, 0, -5,
                    5, 4, 8, 0, 6, 0);
        }
    }

    @Override
    void pose(Entity mob, float stride, float speed, float age, float yaw, float pitch) {
        float gait = stride * 0.52F;
        float step = (float) Math.cos(gait) * speed;
        float phase = onGround;
        float lift = phase == 0 ? 0 : ease(phase * 4) * (1 - ease((phase - 0.25F) * 6));
        float impact = ease((phase - 0.24F) * 5.5F) * (1 - ease((phase - 0.44F) * 1.8F));
        float breathing = (float) Math.sin(age * 0.09F);
        body.rotationPointY = 6 + Math.abs(step) * 0.62F + impact * 0.9F;
        chest.rotateAngleX = 0.12F + speed * 0.12F - lift * 0.27F + impact * 0.29F + breathing * 0.025F;
        chest.rotateAngleY = -step * 0.13F + lift * 0.16F - impact * 0.13F;
        chest.rotateAngleZ = step * 0.11F - lift * 0.06F + impact * 0.07F;
        head.rotateAngleY = clamp(yaw - chest.rotateAngleY, -0.55F, 0.55F);
        head.rotateAngleX = clamp(pitch * 0.6F - chest.rotateAngleX, -0.42F, 0.5F);
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            float leg = side * step;
            thighs[i].rotateAngleX = leg * 0.56F - impact * 0.17F;
            thighs[i].rotateAngleZ = -side * 0.10F;
            shins[i].rotateAngleX = Math.max(0, -leg) * 0.47F + impact * 0.27F;
            feet[i].rotateAngleX = -(thighs[i].rotateAngleX + shins[i].rotateAngleX) * 0.7F;
            arms[i].rotateAngleX = -0.08F - leg * 0.53F - lift * (i == 0 ? 2.40F : 1.85F) - impact * 0.68F;
            arms[i].rotateAngleZ = -side * (0.19F + lift * 0.24F + impact * 0.07F);
            forearms[i].rotateAngleX = -0.31F - lift * 0.39F + impact * 0.27F + leg * 0.18F;
            fists[i].rotateAngleX = -0.10F + impact * 0.33F;
        }
    }
}
