package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;

/** Cuboids in texel units, with the vanilla ground plane at model Y=24. */
abstract class EnderkinModel extends ModelBase {
    static final float RADIANS = (float) Math.PI / 180.0F;
    ModelRenderer body;
    float partialTick;

    EnderkinModel(int atlasWidth) {
        textureWidth = atlasWidth;
        textureHeight = 64;
    }

    final ModelRenderer box(ModelRenderer parent, int u, int v, float x, float y, float z,
            int width, int height, int depth, float pivotX, float pivotY, float pivotZ) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, width, height, depth);
        part.setRotationPoint(pivotX, pivotY, pivotZ);
        if (parent != null) parent.addChild(part);
        return part;
    }

    @Override
    public final void setLivingAnimations(EntityLiving mob, float stride, float speed, float partial) {
        partialTick = partial;
    }

    @Override
    public final void render(Entity mob, float stride, float speed, float age, float yaw, float pitch, float scale) {
        pose(mob, stride, speed, age, yaw * RADIANS, pitch * RADIANS);
        body.render(scale);
    }

    abstract void pose(Entity mob, float stride, float speed, float age, float yaw, float pitch);

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static float ease(float value) {
        float t = clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    static float wave(float phase) { return (float) Math.cos(phase); }

    static float pulse(float time, float peak, float end) {
        return ease(time / peak) * (1 - ease((time - peak) / (end - peak)));
    }
}
