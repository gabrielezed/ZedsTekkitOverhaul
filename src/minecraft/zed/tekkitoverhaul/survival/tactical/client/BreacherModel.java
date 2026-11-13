package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;

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
        EntityBreacher breacher = (EntityBreacher) mob;
        float gait = stride * 0.49F;
        float step = wave(gait) * speed;
        float phase = onGround;
        boolean pressure = breacher.isPressureSwing();
        float contact = EntityBreacher.PRESSURE_CONTACT_TICKS / 10.0F;
        float lift = pressure ? pulse(phase, 0.22F, contact + 0.02F) : 0;
        float impact = pressure ? pulse(phase, contact, 0.97F) : 0;
        float punch = pressure ? 0 : pulse(phase, 0.19F, 0.90F);
        float breathing = wave(age * 0.068F + mob.entityId);
        body.rotationPointY = 6 + speed * (0.28F - wave(gait * 2 - 0.4F) * 0.28F) + impact * 0.36F;
        chest.rotateAngleX = 0.12F + speed * 0.12F - lift * 0.20F + impact * 0.31F
                + punch * 0.19F + breathing * 0.023F;
        chest.rotateAngleY = -step * 0.13F + lift * 0.13F - impact * 0.10F + punch * 0.24F;
        chest.rotateAngleZ = wave(gait - 0.38F) * speed * 0.09F - lift * 0.05F + impact * 0.06F;
        head.rotateAngleY = clamp(yaw - chest.rotateAngleY, -0.55F, 0.55F);
        head.rotateAngleX = clamp(pitch * 0.6F - chest.rotateAngleX * 0.86F
                + wave(age * 0.048F) * 0.018F, -0.42F, 0.5F);
        for (int i = 0; i < 2; i++) {
            int side = 2 * i - 1;
            float footPhase = gait + (i == 0 ? 0 : 3.12F);
            float knee = ease((wave(footPhase - 2.15F) + 0.15F) / 1.15F) * speed;
            float drive = punch * (i == 0 ? 1 : 0.22F);
            thighs[i].rotateAngleX = wave(footPhase) * speed * 0.51F - impact * 0.13F;
            thighs[i].rotateAngleZ = -side * 0.10F;
            shins[i].rotateAngleX = knee * 0.49F + impact * 0.23F;
            feet[i].rotateAngleX = -(thighs[i].rotateAngleX + shins[i].rotateAngleX) * 0.83F;
            arms[i].rotateAngleX = -0.08F - wave(footPhase - 0.58F) * speed * 0.44F
                    - lift * (i == 0 ? 2.22F : 1.73F) - impact * 1.13F - drive * 1.04F;
            arms[i].rotateAngleZ = -side * (0.20F + lift * 0.21F + impact * 0.09F + breathing * side * 0.018F);
            forearms[i].rotateAngleX = -0.31F - lift * 0.41F + impact * 0.30F
                    + wave(footPhase - 1.1F) * speed * 0.15F - drive * 0.31F;
            fists[i].rotateAngleX = -0.10F + pulse(phase, 0.52F, 0.92F) * (pressure ? 0.33F : 0.14F);
        }
    }
}
