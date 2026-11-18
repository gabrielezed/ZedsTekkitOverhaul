package zed.tekkitoverhaul.survival.aerial.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.ModelBase;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntitySkelebird;

public final class RenderAerialPredator extends RenderLiving {
    private RenderAerialPredator(ModelBase model) { super(model, 0.42F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityAerialPredator.class, new RenderAerialPredator(new ModelAerialPredator()));
        renderers.put(EntitySkelebird.class, new RenderAerialPredator(new ModelSkelebird()));
    }

    @Override
    protected void rotateCorpse(EntityLiving mob, float age, float renderYaw, float partialTick) {
        // Follow flight heading even when the body helper lags behind a turn.
        float turn = (mob.rotationYaw - mob.prevRotationYaw) % 360;
        if (turn > 180) turn -= 360;
        if (turn < -180) turn += 360;
        super.rotateCorpse(mob, age, mob.prevRotationYaw + turn * partialTick, partialTick);
        GL11.glTranslatef(0, 0.30F, 0);
        float pitch = mob.prevRotationPitch + (mob.rotationPitch - mob.prevRotationPitch) * partialTick;
        if (mob.getHealth() <= 0) pitch = Math.min(70, pitch + mob.deathTime * 3);
        // The model faces -Z before the vanilla yaw transform. Negative X rotation pitches its beak down.
        GL11.glRotatef(modelPitch(pitch), 1, 0, 0);
        GL11.glRotatef(Math.max(-25, Math.min(25, turn * 2.5F)), 0, 0, 1);
        GL11.glTranslatef(0, -0.30F, 0);
    }

    static float modelPitch(float flightPitch) { return -flightPitch; }

    @Override
    protected float getDeathMaxRotation(EntityLiving mob) { return 48; }
}
