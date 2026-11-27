package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Intercepts the six ordered visibility decisions in the pinned OptiFine liquid renderer. */
final class AbyssalCeilingHook implements Opcodes {
    private static final int[] SIDES = {1, 0, 2, 3, 4, 5};
    static final String TARGET = "vl";
    static final String SHA256 = "e26df3cd6daac4cb6946ef20a6754458a7d62fd3f34be5e770644b58fbe38a9a";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] calls = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"n".equals(name) || !"(Lpb;III)Z".equals(descriptor)) return method;
                return new MethodVisitor(ASM9, method) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        if (opcode != INVOKEVIRTUAL || !"pb".equals(owner) || !"a".equals(name)
                                || !"(Lali;IIII)Z".equals(desc)) return;
                        if (calls[0] >= SIDES.length) throw new IllegalArgumentException("unexpected liquid visibility call");
                        int side = SIDES[calls[0]++];
                        super.visitVarInsn(ALOAD, 1);
                        super.visitVarInsn(ALOAD, 0);
                        super.visitFieldInsn(GETFIELD, TARGET, "a", "Lali;");
                        super.visitVarInsn(ILOAD, 2);
                        super.visitVarInsn(ILOAD, 3);
                        super.visitVarInsn(ILOAD, 4);
                        super.visitInsn(ICONST_0 + side);
                        super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "waterVisibleFace",
                                "(ZLjava/lang/Object;Ljava/lang/Object;IIII)Z", false);
                    }
                };
            }
        }, 0);
        if (calls[0] != 6) throw new IllegalArgumentException("unexpected liquid visibility calls");
        return writer.toByteArray();
    }
}
