package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.tactical.EntityThief;

final class ThiefModel extends EnderkinModel {
    private final ModelRenderer chest, head, rightArm, leftArm, rightHand, leftHand;
    private final ModelRenderer rightLeg, leftLeg, rightShin, leftShin;

    ThiefModel() {
        super(64);
        body = box(null, 0, 0, -2, -3, -2, 4, 4, 4, 0, 10, 0);
        chest = box(body, 16, 0, -3, -9, -2.5F, 6, 9, 5, 0, -2, 0);
        head = box(chest, 38, 0, -3, -5, -4, 6, 6, 5, 0, -8, -2);
        rightArm = box(chest, 0, 16, -1, -1, -1, 2, 8, 2, -3.4F, -7, 0);
        leftArm = box(chest, 0, 16, -1, -1, -1, 2, 8, 2, 3.4F, -7, 0);
        rightHand = box(rightArm, 8, 16, -1.5F, -1, -1.5F, 3, 9, 3, 0, 6, 0);
        leftHand = box(leftArm, 8, 16, -1.5F, -1, -1.5F, 3, 9, 3, 0, 6, 0);
        box(rightHand, 20, 16, -1.4F, 7.5F, -1.6F, 1, 3, 2, 0, 0, 0);
        box(rightHand, 20, 16, 0.4F, 7.5F, -1.6F, 1, 2, 2, 0, 0, 0);
        box(leftHand, 20, 16, -1.4F, 7.5F, -1.6F, 1, 2, 2, 0, 0, 0);
        box(leftHand, 20, 16, 0.4F, 7.5F, -1.6F, 1, 3, 2, 0, 0, 0);
        rightLeg = box(body, 0, 32, -1, -1, -1.5F, 2, 9, 3, -1.35F, 0, 0);
        leftLeg = box(body, 0, 32, -1, -1, -1.5F, 2, 9, 3, 1.35F, 0, 0);
        rightShin = box(rightLeg, 10, 32, -1, -1, -2, 2, 7, 3, 0, 8, 0);
        leftShin = box(leftLeg, 10, 32, -1, -1, -2, 2, 7, 3, 0, 8, 0);
    }

    @Override
    void pose(Entity mob, float stride, float speed, float age, float yaw, float pitch) {
        float walk = clamp(speed * 2.1F, 0, 1);
        float phase = stride * 1.16F;
        float step = wave(phase) * walk;
        float lift = wave(phase - 1.25F) * walk;
        float scan = wave(age * 0.19F + wave(age * 0.082F)) * (1 - walk);
        float reach = pulse(((EntityThief) mob).getSwingProgress(partialTick), 0.24F, 0.92F);
        body.rotationPointY = 10 + walk * 0.38F - Math.abs(step) * 0.57F;
        body.rotateAngleZ = 0.028F + step * 0.049F;
        chest.rotateAngleX = 0.34F + walk * 0.15F + reach * 0.14F;
        chest.rotateAngleY = -0.12F + step * 0.20F + reach * 0.32F;
        head.rotateAngleX = pitch * 0.72F - chest.rotateAngleX + wave(age * 0.38F) * 0.045F;
        head.rotateAngleY = clamp(yaw * 0.75F + scan * 0.32F - chest.rotateAngleY * 0.7F, -0.75F, 0.75F);
        rightLeg.rotateAngleX = step * 0.73F - 0.08F;
        leftLeg.rotateAngleX = -step * 0.73F - 0.08F;
        rightShin.rotateAngleX = 0.09F + Math.max(0, -lift) * 0.67F;
        leftShin.rotateAngleX = 0.09F + Math.max(0, lift) * 0.67F;
        rightArm.rotateAngleX = -0.37F - step * 0.57F - reach * 1.22F;
        leftArm.rotateAngleX = -0.10F + step * 0.63F + reach * 0.34F;
        rightHand.rotateAngleX = -0.38F + scan * 0.095F + reach * 0.29F;
        leftHand.rotateAngleX = -0.49F - scan * 0.06F;
    }
}
