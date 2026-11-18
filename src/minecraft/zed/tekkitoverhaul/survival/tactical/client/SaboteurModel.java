package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;

final class SaboteurModel extends EnderkinModel {
    private final ModelRenderer head, rightArm, leftArm, rightHand, leftHand, rightLeg, leftLeg;

    SaboteurModel() {
        super(64);
        body = box(null, 0, 0, -2.5F, -4, -2, 5, 8, 4, 0, 15, 0);
        head = box(body, 20, 0, -3, -4, -4, 6, 5, 5, 0, -4, -1);
        box(head, 44, 0, -2, 0, -4, 4, 2, 3, 0, 0, 0);
        rightArm = box(body, 0, 16, -1, -1, -1, 2, 7, 2, -3, -3, -1);
        leftArm = box(body, 8, 16, -1, -1, -1, 2, 7, 2, 3, -3, -1);
        rightHand = box(rightArm, 16, 16, -1, 0, -1, 2, 2, 2, 0, 5, 0);
        leftHand = box(leftArm, 24, 16, -1, 0, -1, 2, 2, 2, 0, 5, 0);
        box(rightHand, 32, 16, -1.1F, 1, -1, 1, 4, 1, 0, 0, 0);
        box(rightHand, 36, 16, 0.2F, 1, -0.5F, 1, 3, 1, 0, 0, 0);
        box(leftHand, 40, 16, 0.1F, 1, -1, 1, 4, 1, 0, 0, 0);
        box(leftHand, 44, 16, -1.2F, 1, -0.5F, 1, 3, 1, 0, 0, 0);
        rightLeg = box(body, 0, 32, -1, 0, -1, 2, 6, 2, -1.5F, 3, 1);
        leftLeg = box(body, 8, 32, -1, 0, -1, 2, 6, 2, 1.5F, 3, 1);
    }

    @Override
    void pose(Entity mob, float stride, float speed, float age, float yaw, float pitch) {
        byte pose = ((EntitySaboteur) mob).pose();
        float gait = wave(stride * 1.45F + wave(stride * 0.53F) * 0.26F) * clamp(speed * 2, 0, 1);
        body.rotationPointY = 15 - Math.abs(gait) * 0.5F;
        body.rotateAngleX = 0.40F + Math.abs(gait) * 0.19F;
        body.rotateAngleZ = gait * 0.08F;
        head.rotateAngleX = pitch - body.rotateAngleX;
        head.rotateAngleY = yaw * 0.6F;
        rightLeg.rotateAngleX = gait * 0.9F - 0.22F;
        leftLeg.rotateAngleX = -gait * 0.9F - 0.22F;
        rightArm.rotateAngleX = -0.22F - gait * 0.8F;
        leftArm.rotateAngleX = -0.10F + gait * 0.7F;
        rightArm.rotateAngleZ = 0.08F;
        leftArm.rotateAngleZ = -0.08F;
        rightHand.rotateAngleX = -0.22F + wave(age * 0.73F) * 0.13F;
        leftHand.rotateAngleX = -0.29F + wave(age * 0.97F) * 0.17F;
        if (pose == EntitySaboteur.WINDUP) {
            float tremor = wave(age * 1.7F) * 0.11F;
            rightArm.rotateAngleX = -1.78F + tremor;
            leftArm.rotateAngleX = -1.39F - tremor;
            rightHand.rotateAngleX = -0.71F;
            leftHand.rotateAngleX = -0.48F;
            body.rotationPointY = 16.2F;
        } else if (pose == EntitySaboteur.DISMANTLE) {
            float turn = wave(age * 0.9F) * 0.24F;
            body.rotateAngleX = 0.56F;
            rightArm.rotateAngleX = -1.49F + turn;
            leftArm.rotateAngleX = -1.49F - turn;
            rightHand.rotateAngleX = -0.62F - turn;
            leftHand.rotateAngleX = -0.62F + turn;
        } else if (pose == EntitySaboteur.CARRY) {
            body.rotateAngleX += 0.18F;
            leftArm.rotateAngleX = -1.06F;
            leftHand.rotateAngleX = -0.80F;
        } else if (pose == EntitySaboteur.DECOY) {
            rightArm.rotateAngleX = -1.85F + wave(age * 1.4F) * 0.17F;
            leftArm.rotateAngleX = 0.55F;
        } else if (pose == EntitySaboteur.EVADE) {
            body.rotationPointY = 17;
            body.rotateAngleX = 0.86F;
            head.rotateAngleX = -0.63F;
            rightLeg.rotateAngleX = -0.82F;
            leftLeg.rotateAngleX = 0.37F;
            rightArm.rotateAngleX = 0.83F;
            leftArm.rotateAngleX = 0.63F;
        } else if (pose == EntitySaboteur.RECOVER || pose == EntitySaboteur.CANCELLED) {
            body.rotateAngleX = pose == EntitySaboteur.RECOVER ? 0.42F : -0.08F;
            rightArm.rotateAngleZ = 0.50F;
            leftArm.rotateAngleZ = -0.50F;
        } else {
            rightArm.rotateAngleX -= ((EntitySaboteur) mob).getSwingProgress(partialTick) * 1.3F;
        }
    }
}
