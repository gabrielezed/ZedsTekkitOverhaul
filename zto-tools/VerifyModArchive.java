import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;

/** Reads local ZIP headers in the same order as the legacy NEI mod scanner. */
public final class VerifyModArchive {
    public static void main(String[] args) throws Exception {
        try {
            verify(args[0]);
        } catch (Exception invalid) {
            if (args.length == 1) throw invalid;
            System.exit(1);
        }
    }

    private static void verify(String file) throws Exception {
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte[] buffer = new byte[8192];
            while (zip.getNextEntry() != null) {
                while (zip.read(buffer) != -1) { }
                entries++;
            }
        }
        if (entries == 0) throw new IllegalArgumentException("Empty mod archive");
        System.out.println("[ZTO] Verified " + entries + " entries with the streaming ZIP reader.");
    }
}
