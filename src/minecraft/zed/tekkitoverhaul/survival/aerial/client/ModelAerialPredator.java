package zed.tekkitoverhaul.survival.aerial.client;

import net.minecraft.src.Entity;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;

final class ModelAerialPredator extends ModelBase {
    private final ModelRenderer body, head, jaw, tail;
    private final ModelRenderer[] wings = new ModelRenderer[2];
    private final ModelRenderer[] wrists = new ModelRenderer[2];
    private final ModelRenderer[] feet = new ModelRenderer[2];

    ModelAerialPredator() {
        textureWidth = 64;
        textureHeight = 64;
        body = box(0, 0, -5, -4, -6, 10, 7, 13);
        body.setRotationPoint(0, 19, 0);
        head = box(0, 20, -3, -3, -5, 6, 6, 6);
        head.setRotationPoint(0, -2, -5);
        body.addChild(head);
        head.addChild(box(24, 20, -2, -1, -9, 4, 3, 4));
        head.addChild(box(40, 20, -1.5F, 1, -9, 3, 3, 2));
        jaw = box(24, 27, -1.5F, 0, -6, 3, 2, 6);
        jaw.setRotationPoint(0, 2, -3);
        head.addChild(jaw);
        tail = box(42, 27, -1.5F, -1, 0, 3, 2, 7);
        tail.setRotationPoint(0, 0, 6);
        body.addChild(tail);
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            ModelRenderer fan = box(0, 55, side < 0 ? -3 : 0, -0.5F, 0, 3, 1, 7);
            fan.setRotationPoint(0, 0, 4);
            fan.rotateAngleY = side * 0.28F;
            tail.addChild(fan);

            wings[i] = box(0, 36, side < 0 ? -12 : 0, -1, -3, 12, 2, 9);
            wings[i].setRotationPoint(side * 4, -2, -2);
            body.addChild(wings[i]);
            wrists[i] = box(0, 47, side < 0 ? -10 : 0, -0.5F, -2, 10, 1, 6);
            wrists[i].setRotationPoint(side * 11, 0, 0);
            wings[i].addChild(wrists[i]);
            ModelRenderer tip = box(32, 47, side < 0 ? -5 : 0, -0.5F, -1, 5, 1, 3);
            tip.setRotationPoint(side * 9, 0, 1);
            tip.rotateAngleY = side * -0.32F;
            wrists[i].addChild(tip);

            feet[i] = box(50, 20, -1, 0, -1, 2, 3, 3);
            feet[i].setRotationPoint(side * 2.5F, 2, 3);
            feet[i].addChild(box(50, 37, -1, 2, -3, 2, 1, 3));
            body.addChild(feet[i]);
        }
    }

    private ModelRenderer box(int u, int v, float x, float y, float z, int width, int height, int depth) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        return part.addBox(x, y, z, width, height, depth);
    }

    @Override
    public void render(Entity entity, float stride, float swing, float age, float yaw, float pitch, float scale) {
        EntityAerialPredator.Flight flight = ((EntityAerialPredator) entity).flight();
        float stroke = MathHelper.sin(age * 0.53F);
        float sweep = 0.03F;
        float wing = 0.16F + stroke * 0.43F;
        float wrist = -0.10F + MathHelper.sin(age * 0.53F - 0.8F) * 0.25F;
        float talons = 1.05F;
        head.rotateAngleX = -0.05F;
        jaw.rotateAngleX = 0;
        tail.rotateAngleX = 0.06F;
        switch (flight) {
            case GLIDE:
                wing = -0.09F + stroke * 0.035F;
                wrist = -0.08F;
                break;
            case CIRCLE:
                wing += 0.13F;
                break;
            case PREPARE:
                wing = 0.65F;
                wrist = 0.34F;
                sweep = -0.10F;
                tail.rotateAngleX = -0.28F;
                talons = 0.55F;
                jaw.rotateAngleX = 0.58F;
                head.rotateAngleX = 0.22F;
                break;
            case DIVE:
                sweep = -0.68F;
                wing = 0.18F;
                wrist = -0.31F;
                talons = -0.44F;
                jaw.rotateAngleX = 0.31F;
                tail.rotateAngleX = -0.15F;
                break;
            case RECOVER:
                wing = 0.25F + stroke * 0.62F;
                wrist = -0.05F + MathHelper.sin(age * 0.53F - 0.9F) * 0.41F;
                tail.rotateAngleX = 0.25F;
                break;
            case FALL:
                wing = 0.68F + stroke * 0.09F;
                sweep = -0.54F;
                wrist = -0.36F;
                head.rotateAngleX = 0.43F;
                talons = 0.37F;
                break;
            default:
                break;
        }
        tail.rotateAngleY = flight == EntityAerialPredator.Flight.CIRCLE ? 0.15F : 0;
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            wings[i].rotateAngleY = side * sweep;
            wings[i].rotateAngleZ = side * wing;
            wrists[i].rotateAngleZ = side * wrist;
            feet[i].rotateAngleX = talons;
        }
        body.render(scale);
    }
}
