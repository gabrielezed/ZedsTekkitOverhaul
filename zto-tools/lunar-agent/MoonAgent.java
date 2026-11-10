package zed.tekkitoverhaul.lunaragent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.function.ToIntBiFunction;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Java 8 launch agent that leaves the original moon quad and atlas lookup in place. */
public final class MoonAgent {
    private static final String TEKKIT = "23f7dfa8ee3e8c2707090c6c7dfb8f1843fef9692fd34a0d9588c79c2c8df312";
    private static final String VANILLA = "339a99bfa23e5b9329ceb2af33a84f1e130c96f8061aefd071078fe6d3fc5675";
    private static final String MCP = "9a46dbed02eea4fa9dbae9552ce066db33bb3151383ffa53d237962e81cefa4a";
    private static final String MCP_RENDERER = "net/minecraft/src/RenderGlobal";
    private static final String BRIDGE = "zed/tekkitoverhaul/lunaragent/MoonAgent";
    private static volatile String status = "Waiting for RenderGlobal";
    private static volatile ToIntBiFunction<Object, Integer> callback;

    private MoonAgent() { }

    public static void premain(String arguments, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> previous,
                                    ProtectionDomain domain, byte[] bytes) {
                if (previous != null || !("l".equals(name) || MCP_RENDERER.equals(name))) return null;
                return transformRenderer(name, bytes);
            }
        });
    }

    static byte[] transformRenderer(String name, byte[] bytes) {
        try {
            boolean mcp = MCP_RENDERER.equals(name);
            String hash = fingerprint(bytes);
            boolean supported = mcp ? MCP.equals(hash) : TEKKIT.equals(hash) || VANILLA.equals(hash);
            if (!supported) {
                status = "Unsupported renderer fingerprint; vanilla phase";
                return null;
            }
            byte[] patched = hook(bytes, mcp);
            status = "Native moon phase hook active";
            return patched;
        } catch (Exception failure) {
            status = "Renderer hook failed; vanilla phase";
            return null;
        }
    }

    public static void register(ToIntBiFunction<Object, Integer> selector) { callback = selector; }
    public static String status() { return status; }

    public static int select(Object world, float partialTick, int original) {
        ToIntBiFunction<Object, Integer> selector = callback;
        return selector == null ? original : selector.applyAsInt(world, original);
    }

    private static String fingerprint(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte part : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            hex.append(Integer.toHexString((part & 255) | 256).substring(1));
        }
        return hex.toString();
    }

    private static byte[] hook(byte[] bytes, final boolean mcp) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] calls = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals(mcp ? "renderSky" : "a") || !descriptor.equals("(F)V")) return target;
                return new MethodVisitor(Opcodes.ASM5, target) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        boolean phase = opcode == Opcodes.INVOKEVIRTUAL && desc.equals("(F)I")
                                && owner.equals(mcp ? "net/minecraft/src/World" : "xd")
                                && method.equals(mcp ? "getMoonPhase" : "d");
                        if (phase) visitInsn(Opcodes.DUP2);
                        super.visitMethodInsn(opcode, owner, method, desc, itf);
                        if (phase) {
                            calls[0]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "select", "(Ljava/lang/Object;FI)I", false);
                        }
                    }
                };
            }
        }, 0);
        if (calls[0] != 1) throw new IllegalStateException("Expected one native moon phase call");
        return writer.toByteArray();
    }
}
