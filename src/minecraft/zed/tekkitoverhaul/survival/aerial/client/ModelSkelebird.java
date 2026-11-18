package zed.tekkitoverhaul.survival.aerial.client;

import net.minecraft.src.Entity;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator.Flight;

/** Open ribs and jointed spars give the silhouette its gaps even at a distance. */
final class ModelSkelebird extends ModelBase {
    private final ModelRenderer spine, skull, jaw, tail;
    private final ModelRenderer[] shoulders = new ModelRenderer[2];
    private final ModelRenderer[] wrists = new ModelRenderer[2];
    private final ModelRenderer[] talons = new ModelRenderer[2];

    ModelSkelebird() {
        textureWidth = textureHeight = 64;
        spine = box(0, 0, -1, -1, -9, 2, 2, 19);
        spine.setRotationPoint(0, 19, 0);
        spine.addChild(box(40, 0, -1, 2, -4, 2, 1, 10));
        for (int i = 0; i < 2; ++i) {
            int side = i == 0 ? -1 : 1;
            for (int rib = 0; rib < 4; ++rib) {
                ModelRenderer arch = box(44, 13, side < 0 ? -4 : 0, 0, 0, 4, 1, 1);
                arch.setRotationPoint(side * 0.6F, -1, -5 + rib * 3);
                arch.rotateAngleZ = side * (0.55F + rib * 0.06F);
                spine.addChild(arch);
                ModelRenderer lowerRib = box(55, 13, 0, 0, 0, 1, 4, 1);
                lowerRib.setRotationPoint(side < 0 ? -4 : 3, 0, 0);
                lowerRib.rotateAngleZ = side * 0.32F;
                arch.addChild(lowerRib);
            }
            shoulders[i] = box(0, 23, side < 0 ? -9 : 0, -1, -1, 9, 2, 2);
            shoulders[i].setRotationPoint(side * 2, -1, -4);
            spine.addChild(shoulders[i]);
            addSpar(shoulders[i], side, 1, 1, 9, 0.45F, 0, 29);
            wrists[i] = box(0, 33, side < 0 ? -10 : 0, -1, -1, 10, 2, 2);
            wrists[i].setRotationPoint(side * 9, 0, 0);
            shoulders[i].addChild(wrists[i]);
            addSpar(wrists[i], side, 0, 1, 14, 0.22F, 0, 39);
            addSpar(wrists[i], side, 0, 1, 12, 0.62F, 0, 43);
            addSpar(wrists[i], side, 0, 1, 9, 1.09F, 0, 29);
            talons[i] = box(55, 13, side < 0 ? -1 : 0, 0, 0, 1, 4, 1);
            talons[i].setRotationPoint(side * 1.4F, 1, 6);
            talons[i].addChild(box(35, 49, -1, 3, -2, 2, 1, 2));
            spine.addChild(talons[i]);
        }
        skull = box(0, 48, -2, -2, -5, 4, 4, 5);
        skull.setRotationPoint(0, -0.5F, -8);
        skull.addChild(box(19, 48, -1, -0.5F, -9, 2, 2, 4));
        jaw = box(0, 58, -1.5F, 0, -8, 3, 1, 5);
        jaw.setRotationPoint(0, 1.5F, 0);
        skull.addChild(jaw);
        spine.addChild(skull);
        tail = box(44, 20, -1, -1, 0, 2, 2, 4);
        tail.setRotationPoint(0, 0, 9);
        tail.addChild(box(44, 27, -0.5F, -0.5F, 4, 1, 1, 5));
        tail.addChild(box(44, 34, -1, -0.5F, 8, 2, 1, 4));
        spine.addChild(tail);
    }

    private ModelRenderer box(int u, int v, float x, float y, float z, int w, int h, int d) {
        return new ModelRenderer(this, u, v).addBox(x, y, z, w, h, d);
    }

    private void addSpar(ModelRenderer parent, int side, float x, float z, int length, float sweep, int u, int v) {
        ModelRenderer spar = box(u, v, side < 0 ? -length : 0, 0, 0, length, 1, 1);
        spar.setRotationPoint(side * x, 0, z);
        spar.rotateAngleY = -side * sweep;
        parent.addChild(spar);
    }

    @Override
    public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        EntityAerialPredator bird = (EntityAerialPredator) entity;
        pose(bird.flight(), age, bird.phaseTicks());
        spine.render(scale);
    }

    private void pose(Flight flight, float age, int phaseTicks) {
        float beat = MathHelper.sin(age * 0.44F);
        float flap = beat * 0.45F;
        float sweep = -0.14F, wrist = -0.08F - beat * 0.10F, asymmetry = 0;
        skull.rotateAngleX = 0.045F * beat;
        jaw.rotateAngleX = 0.09F;
        tail.rotateAngleY = 0;
        switch (flight) {
            case GLIDE:
                flap = -0.06F + beat * 0.055F;
                wrist = -0.10F;
                break;
            case CIRCLE:
                asymmetry = 0.13F;
                tail.rotateAngleY = 0.17F;
                break;
            case PREPARE:
                flap = -0.85F + beat * 0.055F;
                sweep = -0.42F;
                wrist = 0.40F;
                jaw.rotateAngleX = 0.47F;
                break;
            case DIVE:
                flap = -0.07F;
                sweep = -0.67F;
                wrist = -0.27F;
                skull.rotateAngleX = -0.10F;
                jaw.rotateAngleX = 0.24F;
                break;
            case RECOVER:
                float fold = Math.max(0, 1 - phaseTicks / 12.0F);
                sweep -= fold * 0.53F;
                flap = flap * (1 - fold) - 0.07F * fold;
                break;
            case FALL:
                flap = 0.82F + beat * 0.12F;
                sweep = -0.72F;
                wrist = 0.44F;
                asymmetry = 0.28F;
                jaw.rotateAngleX = 0.43F;
                break;
            default:
                break;
        }
        for (int i = 0; i < 2; ++i) {
            int side = i == 0 ? -1 : 1;
            shoulders[i].rotateAngleZ = side * flap + asymmetry;
            shoulders[i].rotateAngleY = side * sweep;
            wrists[i].rotateAngleZ = side * wrist;
            talons[i].rotateAngleX = flight == Flight.DIVE ? -0.80F : -0.17F;
        }
    }
}
