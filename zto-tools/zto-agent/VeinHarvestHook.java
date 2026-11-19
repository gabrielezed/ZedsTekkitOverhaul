package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Observes successful native Survival harvests and serializes competing held input. */
final class VeinHarvestHook implements Opcodes {
    static final String TARGET = "aes";
    static final String SHA256 = "9bad8a2a6ec65f96084b1ce2d2de483be2b00c3de4f3e8504cf79cfef85e88c0";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] count = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                boolean harvest = "b".equals(name) && "(IIII)Z".equals(descriptor);
                boolean input = ("a".equals(name) || "c".equals(name)) && "(IIII)V".equals(descriptor);
                if (!harvest && !input) return method;
                count[0]++;
                return new MethodVisitor(ASM9, method) {
                    @Override public void visitCode() {
                        super.visitCode();
                        Label original = new Label();
                        visitVarInsn(ALOAD, 0);
                        visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "veinOwnsMiningInput", "(Ljava/lang/Object;)Z", false);
                        visitJumpInsn(IFEQ, original);
                        if (harvest) {
                            visitInsn(ICONST_0);
                            super.visitInsn(IRETURN);
                        } else visitInsn(RETURN);
                        visitLabel(original);
                        if (harvest) event(false);
                    }

                    @Override public void visitInsn(int opcode) {
                        if (harvest && opcode == IRETURN) {
                            Label original = new Label();
                            visitInsn(DUP);
                            visitJumpInsn(IFEQ, original);
                            event(true);
                            visitLabel(original);
                        }
                        super.visitInsn(opcode);
                    }

                    private void event(boolean success) {
                        visitVarInsn(ALOAD, 0);
                        visitVarInsn(ILOAD, 1);
                        visitVarInsn(ILOAD, 2);
                        visitVarInsn(ILOAD, 3);
                        visitInsn(success ? ICONST_1 : ICONST_0);
                        visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "veinHarvest", "(Ljava/lang/Object;IIIZ)V", false);
                    }
                };
            }
        }, 0);
        if (count[0] != 3) throw new IllegalStateException("Expected three local Survival mining methods");
        return writer.toByteArray();
    }
}
