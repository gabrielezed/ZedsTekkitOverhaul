package zed.tekkitoverhaul.survival.aerial.client;

import java.net.URL;
import net.minecraft.src.ModLoader;
import net.minecraft.src.SoundManager;
import net.minecraft.src.SoundPool;
import net.minecraft.src.forge.MinecraftForgeClient;
import net.minecraft.src.forge.adaptors.SoundHandlerAdaptor;

/** Registers packaged WAV URLs in the local Forge sound pool, including after a sound reload. */
public final class AerialSoundRegistration extends SoundHandlerAdaptor {
    private static final String[] CLIPS = {"ambient1", "ambient2", "warning", "dive", "hurt", "death"};
    private static final String[] SKELEBIRD = {"ambient1", "ambient2", "warning", "slash", "hurt", "death"};
    private static final String[] JETPACK = {"warning", "fault", "recovery"};
    private static final String[] DISTILLATION = {"start", "running", "complete"};
    private static final String[] MOTORBOAT = {"running"};
    private static final String[] GUARDIAN = {"ambient", "warning", "lunge", "hurt", "death"};
    private static final String[] ABYSSAL_ENDERKIN = {"ambient", "charge", "release", "hurt", "death"};
    private static final AerialSoundRegistration HANDLER = new AerialSoundRegistration();
    private static boolean registered;
    private SoundPool installed;

    public static void register(SoundManager manager) {
        if (!registered) {
            MinecraftForgeClient.registerSoundHandler(HANDLER);
            registered = true;
        }
        HANDLER.onLoadSoundSettings(manager);
    }

    @Override
    public void onLoadSoundSettings(SoundManager manager) {
        SoundPool pool = manager.getSoundsPool();
        if (installed == pool) return;
        addClips(pool, "aerial", CLIPS);
        addClips(pool, "skelebird", SKELEBIRD);
        addClips(pool, "jetpack", JETPACK);
        addClips(pool, "distillation", DISTILLATION);
        addClips(pool, "motorboat", MOTORBOAT);
        addClips(pool, "guardian", GUARDIAN);
        addClips(pool, "abyssalenderkin", ABYSSAL_ENDERKIN);
        installed = pool;
    }

    private static void addClips(SoundPool pool, String family, String[] clips) {
        for (String clip : clips) {
            URL resource = AerialSoundRegistration.class.getResource("/zed/sounds/" + family + "/" + clip + ".wav");
            if (resource == null) {
                ModLoader.getLogger().warning("[ZTO] Missing sound " + family + "/" + clip);
                continue;
            }
            pool.addSound("zto/" + family + "/" + clip + ".wav", resource);
        }
    }
}
