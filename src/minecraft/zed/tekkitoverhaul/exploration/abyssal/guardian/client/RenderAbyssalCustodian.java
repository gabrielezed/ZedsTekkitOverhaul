package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import java.util.Map;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.OpenGlHelper;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance.Attack;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalCustodian;

public final class RenderAbyssalCustodian extends RenderLiving {
    private static final String EYES = "/zed/textures/abyssal/custodian_eyes.png";
    private final CustodianModel eyes = new CustodianModel(true);

    private RenderAbyssalCustodian() {
        super(new CustodianModel(false), 0.48F);
        setRenderPassModel(eyes);
    }

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntityAbyssalCustodian.class, new RenderAbyssalCustodian());
    }

    @Override protected void rotateCorpse(EntityLiving actor, float age, float yaw, float partial) {
        EntityAbyssalCustodian boss = (EntityAbyssalCustodian) actor;
        float direction = boss.pose() == EntityAbyssalCustodian.ATTACK ? boss.strikeYaw() : actor.rotationYaw;
        super.rotateCorpse(actor, age, direction, partial);
    }

    @Override protected int shouldRenderPass(EntityLiving actor, int pass, float partial) {
        if (pass != 0) return -1;
        EntityAbyssalCustodian boss = (EntityAbyssalCustodian) actor;
        boolean charge = boss.pose() >= EntityAbyssalCustodian.ATTACK
                && boss.poseTick() < Attack.values()[boss.pose() - EntityAbyssalCustodian.ATTACK].windup;
        float glow = boss.deathTime == 0 ? charge ? 0.90F : 0.61F : Math.max(0, 0.61F - boss.deathTime * 0.032F);
        loadTexture(EYES);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GL11.glColor4f(1, 1, 1, glow);
        return 1;
    }

    @Override protected int inheritRenderPass(EntityLiving actor, int pass, float partial) { return -1; }

    @Override protected void renderEquippedItems(EntityLiving actor, float partial) {
        GL11.glColor4f(1, 1, 1, 1);
        int light = actor.getBrightnessForRender(partial);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
    }
}
