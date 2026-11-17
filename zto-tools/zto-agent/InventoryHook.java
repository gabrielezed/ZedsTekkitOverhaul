package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Java 8 launch agent for the verified Minecraft 1.2.5 GuiScreen mouse dispatcher. */
final class InventoryHook {
    static final String TARGET = "vp";
    static final String SHA256 = "91415976668ab43afabbb4257a6f2d295d448b0975b4da682069ba370d127ff3";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        // The verified input is a Java 5 class without stack-map frames.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] patched = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals("f") || !descriptor.equals("()V")) return target;
                patched[0]++;
                return new MethodVisitor(Opcodes.ASM9, target) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label original = new Label();
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitMethodInsn(Opcodes.INVOKESTATIC, ZTOAgent.BRIDGE,
                                "handleMouse", "(Ljava/lang/Object;)Z", false);
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
