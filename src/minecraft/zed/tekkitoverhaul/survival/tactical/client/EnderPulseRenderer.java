package zed.tekkitoverhaul.survival.tactical.client;

import net.minecraft.src.Entity;
import net.minecraft.src.Render;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

/** A faceted core and two advancing, broken contours aligned to local +Z. */
final class EnderPulseRenderer extends Render {
    @Override
    public void doRender(Entity pulse, double x, double y, double z, float yaw, float partial) {
        double horizontal = Math.sqrt(pulse.motionX * pulse.motionX + pulse.motionZ * pulse.motionZ);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotated(Math.toDegrees(Math.atan2(pulse.motionX, pulse.motionZ)), 0, 1, 0);
            GL11.glRotated(-Math.toDegrees(Math.atan2(pulse.motionY, horizontal)), 1, 0, 0);
            draw(pulse.ticksExisted + partial, 1.0F);
        } finally {
            GL11.glPopMatrix();
        }
    }

    static void draw(float age, float size) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        try {
            GL11.glScalef(size, size, size);
            GL11.glRotatef(age * 17.0F, 0, 0, 1);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            Tessellator mesh = Tessellator.instance;
            mesh.startDrawing(GL11.GL_TRIANGLES);
            for (int side = 0; side < 6; side++) {
                double a = side * Math.PI / 3, b = (side + 1) * Math.PI / 3;
                mesh.setColorOpaque_I(side % 2 == 0 ? 0x492567 : 0x643382);
                mesh.addVertex(0, 0, 0.22D);
                vertex(mesh, a, 0.105D, 0);
                vertex(mesh, b, 0.105D, 0);
                mesh.setColorOpaque_I(0x301844);
                mesh.addVertex(0, 0, -0.23D);
                vertex(mesh, b, 0.105D, 0);
                vertex(mesh, a, 0.105D, 0);
            }
            mesh.draw();
            mesh.startDrawingQuads();
            for (int ring = 0; ring < 2; ring++) {
                double phase = age * 0.34D + ring * 2.2D;
                double radius = 0.147D + 0.016D * Math.sin(phase);
                double z = 0.075D - ring * 0.22D + 0.035D * Math.sin(phase);
                mesh.setColorOpaque_I(ring == 0 ? 0xDC79F5 : 0x9C4ACD);
                for (int segment = 0; segment < 6; segment++) {
                    if ((segment + ring) % 3 == 2) continue;
                    double a = segment * Math.PI / 3 + ring * 0.5D;
                    double b = a + Math.PI / 3;
                    vertex(mesh, a, radius, z);
                    vertex(mesh, b, radius, z);
                    vertex(mesh, b, radius - 0.041D, z + 0.034D);
                    vertex(mesh, a, radius - 0.041D, z + 0.034D);
                }
            }
            mesh.draw();
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void vertex(Tessellator mesh, double angle, double radius, double z) {
        mesh.addVertex(Math.cos(angle) * radius, Math.sin(angle) * radius, z);
    }
}
