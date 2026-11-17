import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/** Build-only relocation of ASM core and the agent's references to it. */
final class PackageAgent {
    private static final String ASM = "org/objectweb/asm/";
    private static final Remapper RELOCATE = new Remapper() {
        @Override
        public String map(String name) {
            return name.startsWith(ASM) ? "zed/tekkitoverhaul/agent/internal/asm/" + name.substring(ASM.length()) : name;
        }
    };

    public static void main(String[] args) throws Exception {
        Path classes = Paths.get(args[0]);
        Path resources = Paths.get(args[2]);
        Manifest manifest;
        try (InputStream stream = Files.newInputStream(resources.resolve("manifest.mf"))) {
            manifest = new Manifest(stream);
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(Paths.get(args[3])), manifest);
             ZipFile asm = new ZipFile(args[1]);
             Stream<Path> files = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".class"))::iterator)
                addClass(jar, new ClassReader(Files.readAllBytes(file)));
            Enumeration<? extends ZipEntry> entries = asm.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith(ASM) && entry.getName().endsWith(".class")) {
                    try (InputStream stream = asm.getInputStream(entry)) {
                        addClass(jar, new ClassReader(stream));
                    }
                }
            }
            jar.putNextEntry(new JarEntry("META-INF/LICENSE-ASM.txt"));
            Files.copy(resources.resolve("LICENSE-ASM.txt"), jar);
            jar.closeEntry();
        }
    }

    private static void addClass(JarOutputStream jar, ClassReader reader) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, RELOCATE), 0);
        jar.putNextEntry(new JarEntry(RELOCATE.map(reader.getClassName()) + ".class"));
        jar.write(writer.toByteArray());
        jar.closeEntry();
    }
}
