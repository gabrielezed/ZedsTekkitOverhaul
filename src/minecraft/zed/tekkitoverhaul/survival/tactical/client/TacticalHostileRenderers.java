package zed.tekkitoverhaul.survival.tactical.client;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.OpenGlHelper;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;

public final class TacticalHostileRenderers {
    private TacticalHostileRenderers() {}

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntitySkirmisher.class, new TacticalRenderer(new SkirmisherModel(), 0.30F, 2.76F, "skirmisher"));
        renderers.put(EntityBreacher.class, new TacticalRenderer(new BreacherModel(), 0.58F, 2.43F, "breacher"));
        renderers.put(EntityEnderPulse.class, new EnderPulseRenderer());
    }

    private static final class TacticalRenderer extends RenderLiving {
        private final float visualEnvelope;
        private final String eyes;
        private final ArrayList<AxisAlignedBB> ceilingHits = new ArrayList<AxisAlignedBB>();

        TacticalRenderer(EnderkinModel model, float shadow, float visualEnvelope, String skin) {
            super(model, shadow);
            this.visualEnvelope = visualEnvelope;
            eyes = "/zed/textures/" + skin + "_eyes.png";
        }

        @Override
        protected void renderModel(EntityLiving mob, float stride, float speed, float age,
                float yaw, float pitch, float scale) {
            super.renderModel(mob, stride, speed, age, yaw, pitch, scale);
            EnderkinModel model = (EnderkinModel) mainModel;
            int brightness = mob.getBrightnessForRender(model.partialTick);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT);
            try {
                loadTexture(eyes);
                GL11.glDisable(GL11.GL_LIGHTING);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                model.body.render(scale);
                if (model instanceof SkirmisherModel && mob.deathTime == 0)
                    ((SkirmisherModel) model).renderCharge((EntitySkirmisher) mob, age, yaw, scale);
            } finally {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness & 65535, brightness >> 16);
                GL11.glPopAttrib();
            }
        }

        @Override
        protected void preRenderCallback(EntityLiving mob, float partialTick) {
            if (mob.deathTime > 0) return;
            float scale = clearanceScale(mob, partialTick);
            if (scale < 1) GL11.glScalef(1, scale, 1);
        }

        private float clearanceScale(EntityLiving mob, float partialTick) {
            double x = mob.lastTickPosX + (mob.posX - mob.lastTickPosX) * partialTick;
            double z = mob.lastTickPosZ + (mob.posZ - mob.lastTickPosZ) * partialTick;
            double feet = mob.boundingBox.minY + (mob.lastTickPosY - mob.posY) * (1 - partialTick);
            // Start the compression before the collision hull reaches a roof edge.
            // This spatial transition works even when the mob first enters the frustum under a roof.
            double margin = 0.35D;
            double half = mob.width * 0.5D;
            double reach = half + margin;
            AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(x - reach, feet + mob.height, z - reach,
                    x + reach, feet + visualEnvelope, z + reach);
            ceilingHits.clear();
            for (int bx = MathHelper.floor_double(probe.minX); bx <= MathHelper.floor_double(probe.maxX); bx++) {
                for (int bz = MathHelper.floor_double(probe.minZ); bz <= MathHelper.floor_double(probe.maxZ); bz++) {
                    for (int by = MathHelper.floor_double(probe.minY); by <= MathHelper.floor_double(probe.maxY); by++) {
                        if (!mob.worldObj.blockExists(bx, by, bz)) continue;
                        Block block = Block.blocksList[mob.worldObj.getBlockId(bx, by, bz)];
                        if (block != null) block.getCollidingBoundingBoxes(mob.worldObj, bx, by, bz, probe, ceilingHits);
                    }
                }
            }
            float scale = 1;
            for (AxisAlignedBB hit : ceilingHits) {
                if (hit.minY < probe.minY - 0.001D) continue;
                double dx = Math.max(hit.minX - x, x - hit.maxX);
                double dz = Math.max(hit.minZ - z, z - hit.maxZ);
                float blend = EnderkinModel.ease((float) ((reach - Math.max(dx, dz)) / margin));
                float required = (float) ((hit.minY - feet - 0.025D) / visualEnvelope);
                scale = Math.min(scale, 1 - (1 - required) * blend);
            }
            ceilingHits.clear();
            return scale;
        }
    }
}
