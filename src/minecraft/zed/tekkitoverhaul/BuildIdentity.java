package zed.tekkitoverhaul;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Display identity derived once from the code source that loaded ZTO. */
public final class BuildIdentity {
    private static final String METADATA = "zto-build.properties";
    private static final String LABEL = readLabel();

    private BuildIdentity() { }

    public static String label() { return LABEL; }

    private static String readLabel() {
        Properties metadata = new Properties();
        String archiveHash = null;
        try {
            CodeSource source = BuildVersion.class.getProtectionDomain().getCodeSource();
            if (source != null && "file".equals(source.getLocation().getProtocol())) {
                File location = new File(source.getLocation().toURI());
                if (location.isFile()) {
                    archiveHash = archiveHash(location);
                    JarFile archive = new JarFile(location);
                    try {
                        JarEntry entry = archive.getJarEntry(METADATA);
                        if (entry != null) loadMetadata(metadata, archive.getInputStream(entry));
                    } finally {
                        archive.close();
                    }
                } else if (location.isDirectory()) {
                    File properties = new File(location, METADATA);
                    if (properties.isFile()) loadMetadata(metadata, new FileInputStream(properties));
                }
            }
        } catch (IOException ignored) {
            // Provenance is optional when running loose classes or under a different loader.
        } catch (URISyntaxException ignored) {
        } catch (IllegalArgumentException ignored) {
        } catch (SecurityException ignored) {
        }
        String commit = metadata.getProperty("commit", "");
        String identifier = archiveHash != null ? "b" + archiveHash
                : commit.matches("[0-9a-f]{40}") ? "g" + commit.substring(0, 8) : "";
        String dirty = Boolean.parseBoolean(metadata.getProperty("sourceDirty")) ? " *" : "";
        String label = "ZTO " + BuildVersion.VERSION;
        return (identifier.isEmpty() ? label : label + " \u00b7 " + identifier) + dirty;
    }

    private static void loadMetadata(Properties metadata, InputStream input) throws IOException {
        if (input == null) return;
        try {
            metadata.load(input);
        } finally {
            input.close();
        }
    }

    private static String archiveHash(File location) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException unavailable) {
            return null;
        }
        byte[] buffer = new byte[16384];
        InputStream input = new FileInputStream(location);
        try {
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        } finally {
            input.close();
        }
        byte[] hash = digest.digest();
        char[] hex = new char[8];
        for (int i = 0; i < 4; i++) {
            hex[i * 2] = Character.forDigit((hash[i] & 255) >>> 4, 16);
            hex[i * 2 + 1] = Character.forDigit(hash[i] & 15, 16);
        }
        return new String(hex);
    }
}
