package zed.tekkitoverhaul.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Owns the canonical settings file and the one-time import of older formats. */
public final class ZtoConfigLoader {
    public static final String FILE_NAME = "ZedsTekkitOverhaul.cfg";
    private static final String MLPROP = "mod_ZedsTekkitOverhaul.cfg";
    private static final String VERSION = "configVersion";
    private static final Pattern CATEGORY = Pattern.compile("([a-zA-Z0-9_]+)\\s*\\{");
    private static final Charset ENCODING = StandardCharsets.ISO_8859_1;

    private ZtoConfigLoader() { }

    public static ZtoConfig load(Path directory, boolean hopperAssets, Logger log) {
        Path canonical = directory.resolve(FILE_NAME);
        Path backup = directory.resolve(FILE_NAME + ".legacy");
        Properties primary = new Properties();
        Properties legacy = new Properties();
        Map<String, String> entries = new LinkedHashMap<String, String>();
        try {
            String previous = read(canonical, ENCODING);
            boolean forge = previous != null && isForge(previous);
            if (previous != null && !forge) loadProperties(primary, previous);
            boolean migrate = !primary.containsKey(VERSION);
            if (migrate) {
                importForge(legacy, directory.resolve("ZedsTekkitOverhaul-AlchemicalPower.cfg"));
                importForge(legacy, directory.resolve("ZedsTekkitOverhaul-Tactical.cfg"));
                importForge(legacy, directory.resolve("ZedsTekkitOverhaul-Distillation.cfg"));
                if (forge) {
                    loadForge(legacy, previous);
                } else {
                    importForge(legacy, backup);
                }
                // FML preInit renames unused MLProp files before BaseMod.load().
                importProperties(legacy, directory.resolve(MLPROP + ".bak"));
                importProperties(legacy, directory.resolve(MLPROP));
                if (!primary.containsKey("emc.profile")) {
                    String emc = read(directory.resolve("ZedsTekkitOverhaul-emc.txt"), StandardCharsets.UTF_8);
                    if (emc != null) legacy.setProperty("emc.profile", emc);
                }
            }

            ZtoConfig config = new ZtoConfig(primary, legacy, hopperAssets, log, entries);
            boolean addHopper = !migrate && hopperAssets && !primary.containsKey("block.hopper");
            if (migrate || addHopper) {
                try {
                    Files.createDirectories(directory);
                    if (forge) {
                        // Keep exactly one copy of the original before replacing the file.
                        if (Files.notExists(backup)) {
                            Path temp = Files.createTempFile(directory, "zto-legacy-", ".tmp");
                            try {
                                Files.copy(canonical, temp, StandardCopyOption.REPLACE_EXISTING);
                                move(temp, backup, false);
                            } finally {
                                Files.deleteIfExists(temp);
                            }
                        }
                        String saved = read(backup, ENCODING);
                        if (!previous.equals(saved)) throw new IOException("Conflicting legacy backup " + backup);
                    }
                    Map<String, String> additions = entries;
                    if (addHopper) {
                        additions = new LinkedHashMap<String, String>();
                        additions.put("block.hopper", entries.get("block.hopper"));
                    }
                    write(canonical, forge ? null : previous, migrate, primary, additions);
                    log.info("[ZTO] Configuration ready at " + canonical);
                } catch (IOException failure) {
                    log.log(Level.WARNING, "[ZTO] Could not save " + canonical + ". Using loaded settings for this session.", failure);
                }
            }
            return config;
        } catch (IOException failure) {
            throw unreadable(directory, failure);
        } catch (IllegalArgumentException failure) {
            throw unreadable(directory, failure);
        }
    }

    private static IllegalStateException unreadable(Path directory, Exception cause) {
        return new IllegalStateException("[ZTO] Cannot read settings in " + directory
                + ". Restore the affected file before opening an existing world.", cause);
    }

    private static String read(Path file, Charset charset) throws IOException {
        try {
            byte[] contents = Files.readAllBytes(file);
            return charset.newDecoder().decode(ByteBuffer.wrap(contents)).toString();
        } catch (NoSuchFileException missing) {
            return null;
        }
    }

    private static void loadProperties(Properties values, String text) throws IOException {
        values.load(new StringReader(text));
    }

    private static void importProperties(Properties legacy, Path file) throws IOException {
        String content = read(file, Charset.defaultCharset());
        if (content == null) return;
        Properties values = new Properties();
        loadProperties(values, content);
        // MLProp keys were flat. A backup cannot supply registry or internal settings.
        for (String key : values.stringPropertyNames()) {
            if (key.indexOf('.') < 0) legacy.setProperty(key, values.getProperty(key));
        }
    }

    private static void importForge(Properties legacy, Path file) throws IOException {
        String content = read(file, ENCODING);
        if (content != null) loadForge(legacy, content);
    }

    private static boolean isForge(String text) {
        for (String line : text.split("\\r\\n|\\r|\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
            return CATEGORY.matcher(trimmed).matches();
        }
        return false;
    }

    private static void loadForge(Properties legacy, String text) {
        String category = null;
        for (String line : text.split("\\r\\n|\\r|\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            Matcher section = CATEGORY.matcher(line);
            if (section.matches()) {
                category = section.group(1);
            } else if (line.equals("}")) {
                category = null;
            } else if (category != null) {
                int equals = line.indexOf('=');
                if (equals > 0) {
                    String name = line.substring(0, equals).trim().replace("_", "");
                    legacy.setProperty(category + "." + name, line.substring(equals + 1).trim());
                }
            }
        }
    }

    private static void write(Path canonical, String previous, boolean migrate, Properties primary,
                              Map<String, String> entries) throws IOException {
        Path temp = Files.createTempFile(canonical.getParent(), "zto-settings-", ".tmp");
        try {
            BufferedWriter writer = Files.newBufferedWriter(temp, ENCODING);
            try {
                if (previous == null || previous.isEmpty()) {
                    writer.write("# ZedsTekkitOverhaul settings. Edit while Minecraft is closed.\n");
                } else {
                    writer.write(previous);
                    writer.write("\n\n");
                }
                if (migrate) writer.write(VERSION + "=1\n");
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("#")) {
                        writer.write("\n" + key + "\n");
                    } else if (!primary.containsKey(key)) {
                        writer.write(key + "=" + escape(entry.getValue()) + "\n");
                    }
                }
            } finally {
                writer.close();
            }
            move(temp, canonical, true);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void move(Path temp, Path destination, boolean replace) throws IOException {
        if (!replace) {
            Files.move(temp, destination);
            return;
        }
        try {
            Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String escape(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': encoded.append("\\\\"); break;
                case '\n': encoded.append("\\n"); break;
                case '\r': encoded.append("\\r"); break;
                case '\t': encoded.append("\\t"); break;
                case ' ': encoded.append(i == 0 ? "\\ " : " "); break;
                default:
                    if (c < 32 || c > 126) encoded.append(String.format("\\u%04x", (int) c));
                    else encoded.append(c);
            }
        }
        return encoded.toString();
    }
}
