import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;

/** Each invocation loads the HUD in a fresh process using the same disposable profile. */
public final class SurvivalOverlayProbe {
    public static void main(String[] args) throws Exception {
        Minecraft game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja failure) { throw new AssertionError(failure); }
        };
        game.F = new File(args[0]);
        Class<?> local = Class.forName("zed.tekkitoverhaul.lunar.client.LocalLunarCalendar");
        Class<?> hud = Class.forName("zed.tekkitoverhaul.lunar.client.SurvivalDebugHud");
        Constructor<?> constructor = hud.getDeclaredConstructor(local, Minecraft.class,
                Class.forName("BaseMod"), int.class, int.class);
        constructor.setAccessible(true);
        Object overlay = constructor.newInstance(null, game, null, 0, 0);
        Method visible = hud.getDeclaredMethod("overlayVisible");
        visible.setAccessible(true);
        boolean expected = Boolean.parseBoolean(args[1]);
        if (!visible.invoke(overlay).equals(expected)) throw new AssertionError("Expected persisted visibility " + expected);
        if (args.length > 2) {
            Method toggle = hud.getDeclaredMethod("toggleOverlay");
            toggle.setAccessible(true);
            toggle.invoke(overlay);
            if (!visible.invoke(overlay).equals(!expected)) throw new AssertionError("Overlay failed to toggle");
        }
        System.out.println("PASS Survival Debug overlay, fresh JVM reads " + (expected ? "ON" : "OFF"));
    }
}
