package zed.tekkitoverhaul.lunar.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SurvivalOverlayPreference {
    private final Path file;
    private final Logger log;
    private boolean visible;

    SurvivalOverlayPreference(Path config, Logger log) {
        file = config.resolve("ZtoSurvivalOverlay.txt");
        this.log = log;
        try {
            visible = "true".equals(new String(Files.readAllBytes(file), StandardCharsets.US_ASCII).trim());
        } catch (NoSuchFileException ignored) {
            // A fresh client starts with the overlay hidden.
        } catch (IOException error) {
            log.log(Level.WARNING, "[ZTO] Could not read Survival Debug overlay preference", error);
        }
    }

    boolean visible() { return visible; }

    void toggle() {
        visible = !visible;
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "ZtoSurvivalOverlay", ".tmp");
            try {
                Files.write(temporary, Boolean.toString(visible).getBytes(StandardCharsets.US_ASCII));
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException error) {
            log.log(Level.WARNING, "[ZTO] Could not save Survival Debug overlay preference", error);
        }
    }
}
