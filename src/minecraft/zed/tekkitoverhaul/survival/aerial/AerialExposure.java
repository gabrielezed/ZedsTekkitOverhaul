package zed.tekkitoverhaul.survival.aerial;

import java.util.Arrays;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** One-second samples of open sky and terrain clearance in the active player session. */
final class AerialExposure {
    enum Band { COVERED, GROUND, BUILDING, LOW_AIR, EXPOSED, HIGH_SKY }

    private final int[] terrain = new int[9];
    double height = -1, exposure;
    int groundSeconds;
    Band band = Band.COVERED;
    String restriction = "Waiting for terrain sample";

    void sample(EntityPlayer player) {
        World world = player.worldObj;
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.boundingBox.minY + player.height * 0.5);
        int z = MathHelper.floor_double(player.posZ);
        if (!world.blockExists(x, y, z) || !AerialTerrain.loaded(world, x - 6, z - 6, x + 6, z + 6)) {
            covered("Terrain sample reaches unloaded space");
            return;
        }
        int center = world.getPrecipitationHeight(x, z);
        if (center > y || !world.canBlockSeeTheSky(x, y, z) || player.isInWater()) {
            covered("Under cover or in water");
            return;
        }

        int n = 0;
        for (int dx = -6; dx <= 6; dx += 6) {
            for (int dz = -6; dz <= 6; dz += 6) {
                terrain[n++] = dx == 0 && dz == 0 ? center : world.getPrecipitationHeight(x + dx, z + dz);
            }
        }
        Arrays.sort(terrain);
        height = Math.max(0, player.boundingBox.minY - Math.max(center, terrain[6]));
        boolean wasAirborne = band == Band.LOW_AIR || band == Band.EXPOSED || band == Band.HIGH_SKY;
        if (player.onGround || height < (wasAirborne ? 6 : 7)) {
            exposure = Math.max(0, exposure - (height < 3 || player.onGround ? 8 : 4));
            groundSeconds = height < 3 ? Math.min(30, groundSeconds + 1) : 0;
            band = height < 3 ? Band.GROUND : Band.BUILDING;
            restriction = band == Band.GROUND ? "Waiting for sustained ground exposure" : "Below open flight band";
            return;
        }
        groundSeconds = 0;
        // Gain ramps from the canopy band to high sky. Residual exposure still requires current clearance.
        exposure = Math.min(100, exposure + 0.8 + Math.min(2.2, Math.max(0, height - 7) * 0.073));
        double highThreshold = band == Band.HIGH_SKY ? 32 : 36;
        double exposedThreshold = band == Band.EXPOSED || band == Band.HIGH_SKY ? 15 : 18;
        if (height >= highThreshold && exposure >= 70) band = Band.HIGH_SKY;
        else if (height >= exposedThreshold && exposure >= 60) band = Band.EXPOSED;
        else if (exposure >= 32) band = Band.LOW_AIR;
        else band = Band.BUILDING;
        restriction = "Building open flight exposure";
    }

    boolean ready() {
        return band == Band.GROUND ? groundSeconds >= 30
                : band == Band.LOW_AIR || band == Band.EXPOSED || band == Band.HIGH_SKY;
    }

    private void covered(String reason) {
        groundSeconds = 0;
        height = -1;
        exposure = Math.max(0, exposure - 12);
        band = Band.COVERED;
        restriction = reason;
    }
}
