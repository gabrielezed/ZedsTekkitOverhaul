package zed.tekkitoverhaul.exploration.motorboat.client;

import net.minecraft.src.Entity;
import net.minecraft.src.ModelBoat;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.RenderBoat;

final class RenderMotorboat extends RenderBoat {
    private int motorTexture = -1;

    RenderMotorboat() {
        // RenderBoat applies the native heading, damage roll and axis flips once to both models.
        modelBoat = new ModelBoat() {
            private final ModelRenderer[] motor = {
                    // +X is the stern of the vanilla boat. Its end wall spans X=10 to X=12.
                    box(0, 0, 12, -7, -3, 6, 5, 6),       // Engine housing
                    box(0, 13, 12, -8, -2, 5, 1, 4),      // Cap
                    box(24, 0, 10, -3, -2, 5, 2, 4),      // Transom clamp
                    box(44, 0, 15, -2, -1, 2, 8, 2),      // Shaft
                    box(24, 8, 14, 6, -1, 4, 2, 2),       // Lower gear case
                    box(36, 10, 18, 5, -1, 1, 4, 2),      // Propeller blades
                    box(44, 12, 18, 6, -2, 1, 2, 4)       // Propeller cross
            };

            private ModelRenderer box(int u, int v, int x, int y, int z, int w, int h, int d) {
                ModelRenderer part = new ModelRenderer(this, u, v);
                part.addBox(x, y, z, w, h, d);
                return part;
            }

            @Override
            public void render(Entity entity, float a, float b, float c, float yaw, float pitch, float scale) {
                super.render(entity, a, b, c, yaw, pitch, scale);
                if (motorTexture < 0) {
                    motorTexture = renderManager.renderEngine.allocateAndSetupTexture(MotorboatArtwork.engine());
                }
                renderManager.renderEngine.bindTexture(motorTexture);
                for (ModelRenderer part : motor) part.render(scale);
            }
        };
    }
}
