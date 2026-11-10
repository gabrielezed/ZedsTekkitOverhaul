package zed.tekkitoverhaul.lunaragent;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipFile;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

public final class MoonAgentProbe {
    public static void main(String[] args) throws Exception {
        String mcp = "net/minecraft/src/RenderGlobal";
        verify(mcp, Files.readAllBytes(Paths.get(args[0])));
        for (int index = 1; index < args.length; index++) {
            try (ZipFile jar = new ZipFile(args[index])) {
                byte[] bytes = new byte[(int) jar.getEntry("l.class").getSize()];
                try (java.io.DataInputStream input = new java.io.DataInputStream(jar.getInputStream(jar.getEntry("l.class")))) {
                    input.readFully(bytes);
                }
                verify("l", bytes);
            }
        }
        if (MoonAgent.select(null, 0, 6) != 6) throw new AssertionError("Absent callback fallback");
        Object world = new Object();
        MoonAgent.register((candidate, original) -> candidate == world ? 4 : original);
        if (MoonAgent.select(world, 0, 6) != 4 || MoonAgent.select(null, 0, 6) != 6) {
            throw new AssertionError("World-specific bridge");
        }
        System.out.println("Moon renderer hook checks passed");
    }

    private static void verify(String name, byte[] input) {
        byte[] patched = MoonAgent.transformRenderer(name, input);
        if (patched == null) throw new AssertionError(MoonAgent.status());
        int[] count = {0};
        new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        if (owner.endsWith("/MoonAgent") && method.equals("select")) count[0]++;
                    }
                };
            }
        }, 0);
        if (count[0] != 1) throw new AssertionError("Expected one bridge call");
        if (MoonAgent.transformRenderer(name, patched) != null) throw new AssertionError("Unknown bytecode was patched");
    }
}
