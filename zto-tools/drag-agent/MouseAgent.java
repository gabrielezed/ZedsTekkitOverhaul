package zed.tekkitoverhaul.inputagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.function.Predicate;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Java 8 launch agent for the verified Minecraft 1.2.5 GuiScreen mouse dispatcher. */
public final class MouseAgent {
    private static final String GUI_SHA256 = "91415976668ab43afabbb4257a6f2d295d448b0975b4da682069ba370d127ff3";
    private static volatile Predicate<Object> handler;
    private static volatile boolean installed;

    private MouseAgent() { }

    public static void premain(String arguments, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> previous,
                                    ProtectionDomain domain, byte[] bytes) {
                if (!"vp".equals(name) || previous != null) return null;
                try {
                    byte[] fingerprint = MessageDigest.getInstance("SHA-256").digest(bytes);
                    StringBuilder hex = new StringBuilder();
                    for (byte part : fingerprint) hex.append(Integer.toHexString((part & 255) | 256).substring(1));
                    if (!GUI_SHA256.contentEquals(hex)) {
                        System.err.println("[ZTO] Inventory drag skipped: unrecognized GuiScreen fingerprint.");
                        return null;
                    }
                    byte[] patched = hook(bytes);
                    installed = true;
                    return patched;
                } catch (Exception failure) {
                    System.err.println("[ZTO] Inventory drag skipped: " + failure);
                    return null;
                }
            }
        });
    }

    public static boolean register(Predicate<Object> callback) {
        if (!installed) return false;
        handler = callback;
        return true;
    }

    public static boolean handle(Object screen) {
        Predicate<Object> callback = handler;
        return callback != null && callback.test(screen);
    }

    private static byte[] hook(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        // The verified input is a Java 5 class without stack-map frames.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] patched = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals("f") || !descriptor.equals("()V")) return target;
                patched[0]++;
                return new MethodVisitor(Opcodes.ASM5, target) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label original = new Label();
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitMethodInsn(Opcodes.INVOKESTATIC, "zed/tekkitoverhaul/inputagent/MouseAgent",
                                "handle", "(Ljava/lang/Object;)Z", false);
                        visitJumpInsn(Opcodes.IFEQ, original);
                        visitInsn(Opcodes.RETURN);
                        visitLabel(original);
                    }
                };
            }
        }, 0);
        if (patched[0] != 1) throw new IllegalStateException("Expected one mouse dispatcher");
        return writer.toByteArray();
    }
}
