package zed.tekkitoverhaul.survival.aerial.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;

public final class RenderAerialPredator extends RenderLiving {
    private RenderAerialPredator() { super(new ModelAerialPredator(), 0.42F); }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityAerialPredator.class, new RenderAerialPredator());
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
        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(Math.max(-25, Math.min(25, turn * 2.5F)), 0, 0, 1);
        GL11.glTranslatef(0, -0.30F, 0);
    }

    @Override
    protected float getDeathMaxRotation(EntityLiving mob) { return 48; }
}
