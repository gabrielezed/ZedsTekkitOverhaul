package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.tactical.EntityTank;

final class TankModel extends EnderkinModel {
    private final ModelRenderer chest, head, rightArm, leftArm, rightFist, leftFist;
    private final ModelRenderer rightLeg, leftLeg, rightShin, leftShin;

    TankModel() {
        super(128);
        body = box(null, 0, 0, -7, -5, -4, 14, 6, 8, 0, 8, 0);
        chest = box(body, 0, 14, -9, -22, -5, 18, 23, 10, 0, -6, 0);
        box(chest, 0, 48, -10, -24, -4, 20, 6, 8, 0, 0, 1);
        head = box(chest, 84, 0, -3.5F, -4, -5, 7, 7, 6, 0, -21, -4);
        rightArm = box(chest, 56, 14, -4, -3, -4, 8, 15, 8, -11, -20, 0);
        leftArm = box(chest, 56, 14, -4, -3, -4, 8, 15, 8, 11, -20, 0);
        rightFist = box(rightArm, 88, 14, -4.5F, -1, -5, 9, 13, 9, 0, 11, 0);
        leftFist = box(leftArm, 88, 14, -4.5F, -1, -5, 9, 13, 9, 0, 11, 0);
        rightLeg = box(body, 56, 38, -3.5F, -1, -4, 7, 10, 8, -4, 0, 0);
        leftLeg = box(body, 56, 38, -3.5F, -1, -4, 7, 10, 8, 4, 0, 0);
        rightShin = box(rightLeg, 88, 38, -3, -1, -5, 6, 9, 9, 0, 8, 0);
        leftShin = box(leftLeg, 88, 38, -3, -1, -5, 6, 9, 9, 0, 8, 0);
    }

    @Override
    void pose(Entity mob, float stride, float speed, float age, float yaw, float pitch) {
        float walk = clamp(speed * 2.7F, 0, 1);
        float phase = stride * 0.78F;
        float step = wave(phase) * walk;
        float transfer = wave(phase - 1.2F) * walk;
        float breath = wave(age * 0.13F) * (1 - walk);
        float swing = ((EntityTank) mob).getSwingProgress(partialTick);
        float load = pulse(swing, 0.26F, 0.48F);
        float strike = pulse(swing, 0.55F, 1.0F);
        body.rotationPointY = 8 + (0.22F - Math.abs(step) * 0.8F) * walk;
        body.rotateAngleZ = transfer * 0.044F;
        chest.rotateAngleX = 0.17F + breath * 0.022F - load * 0.16F + strike * 0.20F;
        chest.rotateAngleY = step * 0.086F - load * 0.28F + strike * 0.34F;
        head.rotateAngleX = clamp(pitch * 0.55F, -0.27F, 0.30F) - chest.rotateAngleX * 0.48F;
        head.rotateAngleY = clamp(yaw * 0.62F, -0.46F, 0.46F) - chest.rotateAngleY * 0.45F;
        rightLeg.rotateAngleX = step * 0.42F;
        leftLeg.rotateAngleX = -step * 0.42F;
        rightShin.rotateAngleX = Math.max(0, -transfer) * 0.27F;
        leftShin.rotateAngleX = Math.max(0, transfer) * 0.27F;
        float armLag = wave(phase - 0.43F) * walk;
        rightArm.rotateAngleX = -0.16F - armLag * 0.26F + load * 0.50F - strike * 1.19F;
        leftArm.rotateAngleX = -0.14F + armLag * 0.25F - strike * 0.16F;
        rightFist.rotateAngleX = -0.15F + breath * 0.034F - load * 0.57F + strike * 0.10F;
        leftFist.rotateAngleX = -0.19F + breath * 0.031F;
    }
}
