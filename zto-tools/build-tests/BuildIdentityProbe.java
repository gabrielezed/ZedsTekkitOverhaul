import java.lang.reflect.Method;
import zed.tekkitoverhaul.BuildIdentity;

/** Run in a fresh JVM for each code source. */
public final class BuildIdentityProbe {
    public static void main(String[] args) throws Exception {
        String label = BuildIdentity.label();
        if (label != BuildIdentity.label() || !args[0].equals(label)) {
            throw new AssertionError("Expected [" + args[0] + "], got [" + label + "]");
        }
        System.out.println("PASS " + label);
        if (args.length > 1) verifyTitlePlacement();
    }

    private static void verifyTitlePlacement() throws Exception {
        Method titleY = Class.forName("zed.tekkitoverhaul.client.BuildWatermark")
                .getDeclaredMethod("titleY", int.class, int.class, int.class, int.class, int.class);
        titleY.setAccessible(true);
        int[][] profiles = {
            // Scaled width and height, FML line count, expected label y.
            {854, 480, 5, 420},
            {640, 360, 4, 310},
            {427, 240, 5, 3},
            {320, 240, 4, 3},
            {1280, 240, 5, 180}
        };
        for (int[] profile : profiles) {
            int y = (Integer) titleY.invoke(null, profile[0], profile[1], profile[2], 9, 154);
            if (y != profile[3]) throw new AssertionError("Title placement at " + profile[0] + "x" + profile[1]);
        }
        System.out.println("PASS title placement above the native stack or clear of the buttons at five scaled sizes");
    }
}
