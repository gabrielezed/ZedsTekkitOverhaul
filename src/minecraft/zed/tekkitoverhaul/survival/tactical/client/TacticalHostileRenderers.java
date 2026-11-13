package zed.tekkitoverhaul.survival.tactical.client;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.Render;
import net.minecraft.src.RenderLiving;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;

public final class TacticalHostileRenderers {
    private TacticalHostileRenderers() {}

    public static void register(Map<Class<? extends Entity>, Render> renderers) {
        renderers.put(EntitySkirmisher.class, new TacticalRenderer(new SkirmisherModel(), 0.30F, 2.76F));
        renderers.put(EntityBreacher.class, new TacticalRenderer(new BreacherModel(), 0.58F, 2.43F));
    }

    private static final class TacticalRenderer extends RenderLiving {
        private final float visualEnvelope;
        private final ArrayList<AxisAlignedBB> ceilingHits = new ArrayList<AxisAlignedBB>();

        TacticalRenderer(EnderkinModel model, float shadow, float visualEnvelope) {
            super(model, shadow);
            this.visualEnvelope = visualEnvelope;
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
            double top = feet + visualEnvelope;
            // Probe only above the movement hull and only in already loaded columns.
            double half = mob.width * 0.5D;
            AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(x - half, feet + mob.height, z - half,
                    x + half, top, z + half);
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
            for (AxisAlignedBB hit : ceilingHits) {
                if (hit.minY >= probe.minY - 0.001D) top = Math.min(top, hit.minY - 0.025D);
            }
            ceilingHits.clear();
            return Math.min(1, (float) ((top - feet) / visualEnvelope));
        }
    }
}
