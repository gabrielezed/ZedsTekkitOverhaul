package zed.tekkitoverhaul.devbridge;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.ISaveFormat;
import net.minecraft.src.ModLoader;
import net.minecraft.src.PlayerControllerCreative;
import net.minecraft.src.PlayerControllerSP;
import net.minecraft.src.ThreadedFileIOBase;
import net.minecraft.src.World;
import net.minecraft.src.WorldInfo;

/** One title-screen load, gated by all bridge test flags and a local launch request. */
final class DisposableWorldBootstrap {
    private static final Pattern REQUEST = Pattern.compile("([a-f0-9]{32}) (ZTO-Test-[a-f0-9]{32}) ([a-f0-9]{64})\n");
    private static final int REQUEST_LENGTH = 32 + 1 + 41 + 1 + 64 + 1;
    private final Path discovery;
    private final HopperTestFixture fixture;
    private boolean attempted;
    private String nonce;
    private World opened;
    private long nextStopCheck;

    DisposableWorldBootstrap(Path discovery, HopperTestFixture fixture) {
        this.discovery = discovery;
        this.fixture = fixture;
    }

    void tick(Minecraft game) {
        if (opened != null) {
            if (game.theWorld != opened) opened = null;
            else checkStop(game);
            return;
        }
        if (attempted || game.theWorld != null || !(game.currentScreen instanceof GuiMainMenu)) return;
        attempted = true;
        Path request = game.mcDataDir.toPath().resolve("zto-dev-autoload.request");
        if (!Files.isRegularFile(request, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            Matcher fields = REQUEST.matcher(readExact(request, REQUEST_LENGTH));
            if (!fields.matches()) return;
            Files.delete(request);
            String runtime = ManagementFactory.getRuntimeMXBean().getName();
            String pid = runtime.split("@", 2)[0];
            if (!pid.matches("[0-9]+")) throw new IOException("Process ID unavailable");
            String audit = "[ZTO] Autoload " + fields.group(1) + " ";
            ModLoader.getLogger().info(audit + "PID " + pid + " Discovery: " + discovery);
            if (!fixture.matchesBuild(fields.group(3))) {
                ModLoader.getLogger().warning(audit + "BUILD_MISMATCH");
                return;
            }

            String save = fields.group(2);
            Path dir = HopperTestFixture.saveDirectory(game.mcDataDir, save);
            if (!Files.isRegularFile(dir.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS)) {
                ModLoader.getLogger().warning(audit + "SAVE_NOT_FOUND");
                return;
            }
            Path backup = dir.resolve("level.dat_old");
            if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS)) {
                ModLoader.getLogger().warning(audit + "INVALID_SAVE_BACKUP");
                return;
            }
            ISaveFormat saves = game.getSaveLoader();
            WorldInfo info = saves.getWorldInfo(save);
            if (info == null || saves.isOldMapFormat(save)
                    || !HopperTestFixture.markedSave(game.mcDataDir, save, info.getSeed())) {
                ModLoader.getLogger().warning(audit + "TEST_WORLD_REQUIRED");
                return;
            }
            game.playerController = info.getGameType() == 0 ? new PlayerControllerSP(game) : new PlayerControllerCreative(game);
            game.startWorld(save, info.getWorldName(), null);
            // The legacy renderer opens the pause menu when an unfocused game has no screen.
            game.displayGuiScreen(new GuiScreen() {
                @Override
                public boolean doesGuiPauseGame() { return false; }
            });
            opened = game.theWorld;
            nonce = fields.group(1);
            ModLoader.getLogger().info(audit + "WORLD_OPENED " + save);
        } catch (IOException failure) {
            ModLoader.getLogger().warning("[ZTO] Disposable world autoload refused: " + failure.getMessage());
        } catch (RuntimeException failure) {
            ModLoader.getLogger().warning("[ZTO] Disposable world autoload failed: " + failure.getClass().getSimpleName());
        }
    }

    private void checkStop(Minecraft game) {
        long now = System.nanoTime();
        if (now - nextStopCheck < 0) return;
        nextStopCheck = now + 250000000L;
        Path request = game.mcDataDir.toPath().resolve("zto-dev-autoload.request");
        if (!Files.isRegularFile(request, LinkOption.NOFOLLOW_LINKS)) return;
        String stop = nonce + " stop\n";
        try {
            if (!stop.equals(readExact(request, stop.length()))) return;
            Files.delete(request);
            // Save and Quit unloads while running. shutdown() alone can abort the loading screen before saving.
            opened = null;
            game.changeWorld1(null);
            ThreadedFileIOBase.threadedIOInstance.waitForFinish();
            ModLoader.getLogger().info("[ZTO] Autoload " + nonce + " STOP_REQUESTED");
            game.shutdown();
        } catch (IOException failure) {
            ModLoader.getLogger().warning("[ZTO] Disposable world stop request could not be read");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            ModLoader.getLogger().warning("[ZTO] Disposable world save was interrupted");
        }
    }

    private static String readExact(Path path, int length) throws IOException {
        byte[] bytes = new byte[length];
        InputStream in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS);
        try {
            for (int i = 0; i < bytes.length; i++) {
                int value = in.read();
                if (value < 0) return "";
                bytes[i] = (byte) value;
            }
            return in.read() == -1 ? new String(bytes, StandardCharsets.US_ASCII) : "";
        } finally {
            in.close();
        }
    }
}
