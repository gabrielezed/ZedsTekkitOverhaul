import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;

/** Asserts the installed 1.2.5 client kept the process audio backend uninitialized. */
final class SilentRuntimeAudio {
    private static final Field BACKEND = soundBackend();

    static void verify(Minecraft game) throws IllegalAccessException {
        if (game.A.a != 0.0F || game.A.b != 0.0F || BACKEND.get(null) != null) {
            throw new AssertionError("Expected silent music and effects with no SoundSystem");
        }
    }

    private static Field soundBackend() {
        try {
            Field field = sd.class.getDeclaredField("a");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException error) {
            throw new AssertionError("Unsupported SoundManager layout", error);
        }
    }
}
