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
    private SoundPool installed;

    public static void register(SoundManager manager) {
        AerialSoundRegistration sounds = new AerialSoundRegistration();
        MinecraftForgeClient.registerSoundHandler(sounds);
        sounds.onLoadSoundSettings(manager);
    }

    @Override
    public void onLoadSoundSettings(SoundManager manager) {
        SoundPool pool = manager.getSoundsPool();
        if (installed == pool) return;
        for (String clip : CLIPS) {
            URL resource = AerialSoundRegistration.class.getResource("/zed/sounds/aerial/" + clip + ".wav");
            if (resource == null) {
                ModLoader.getLogger().warning("[ZTO] Missing aerial predator sound: " + clip);
                continue;
            }
            pool.addSound("zto/aerial/" + clip + ".wav", resource);
        }
        installed = pool;
    }
}
