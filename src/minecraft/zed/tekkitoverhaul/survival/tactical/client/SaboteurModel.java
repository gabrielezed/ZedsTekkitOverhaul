package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;

final class SaboteurModel extends EnderkinModel {
    private final ModelRenderer head, rightArm, leftArm, rightHand, leftHand, rightLeg, leftLeg;

    SaboteurModel() {
        super(64);
        body = box(null, 0, 0, -3, -4, -2, 6, 8, 5, 0, 16, 0);
        head = box(body, 24, 0, -4, -5, -5, 8, 6, 6, 0, -3, 0);
        box(head, 0, 46, -4, -6, -2, 2, 2, 3, 0, 0, 0);
        box(head, 10, 46, 2, -6, -2, 2, 2, 3, 0, 0, 0);
        rightArm = box(body, 0, 16, -1, -1, -1, 2, 5, 2, -3, -2, 0);
        leftArm = box(body, 8, 16, -1, -1, -1, 2, 5, 2, 3, -2, 0);
        rightHand = box(rightArm, 20, 16, -2, 0, -2, 3, 4, 3, 0, 3, 0);
        leftHand = box(leftArm, 34, 16, -1, 0, -2, 3, 4, 3, 0, 3, 0);
        rightLeg = box(body, 0, 30, -1, 0, -2, 2, 5, 3, -2, 3, 1);
        leftLeg = box(body, 12, 30, -1, 0, -2, 2, 5, 3, 2, 3, 1);
    }

    @Override
    void pose(Entity mob, float stride, float speed, float age, float yaw, float pitch) {
        byte pose = ((EntitySaboteur) mob).pose();
        float gait = wave(stride * 1.15F) * clamp(speed * 2, 0, 1);
        body.rotationPointY = 16 - Math.abs(gait) * 0.7F;
        body.rotateAngleX = 0.24F + Math.abs(gait) * 0.22F;
        head.rotateAngleX = pitch - body.rotateAngleX;
        head.rotateAngleY = yaw * 0.6F;
        rightLeg.rotateAngleX = gait * 0.9F - 0.22F;
        leftLeg.rotateAngleX = -gait * 0.9F - 0.22F;
        rightArm.rotateAngleX = -0.4F - gait * 0.9F;
        leftArm.rotateAngleX = -0.4F + gait * 0.9F;
        rightArm.rotateAngleZ = 0.08F;
        leftArm.rotateAngleZ = -0.08F;
        rightHand.rotateAngleX = leftHand.rotateAngleX = -0.16F;
        if (pose == EntitySaboteur.WINDUP) {
            float tremor = wave(age * 1.7F) * 0.11F;
            rightArm.rotateAngleX = -1.48F + tremor;
            leftArm.rotateAngleX = -1.48F - tremor;
            rightHand.rotateAngleX = leftHand.rotateAngleX = -0.55F;
            body.rotationPointY = 17;
        } else if (pose == EntitySaboteur.RECOVER || pose == EntitySaboteur.CANCELLED) {
            body.rotateAngleX = pose == EntitySaboteur.RECOVER ? 0.42F : -0.08F;
            rightArm.rotateAngleZ = 0.50F;
            leftArm.rotateAngleZ = -0.50F;
        } else {
            rightArm.rotateAngleX -= ((EntitySaboteur) mob).getSwingProgress(partialTick) * 1.3F;
        }
    }
}
