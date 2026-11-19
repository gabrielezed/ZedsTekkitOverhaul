package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Guards propulsion and observes successful thrust in the pinned IC2 1.97 method. */
final class JetpackHook implements Opcodes {
    static final String TARGET = "ic2/common/ItemArmorJetpack";
    static final String SHA256 = "60a24a25b2702e54c96b011f46f7366bb18a923ee4d83de57e9585e8112a01d6";

    private static final int NATIVE_PRE_THRUST_Y = 11;

    private JetpackHook() {}

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] returns = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"useJetpack".equals(name) || !"(Lyw;Z)Z".equals(descriptor)) return method;
                return new MethodVisitor(ASM9, method) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label nativeThrust = new Label();
                        super.visitVarInsn(ALOAD, 0);
                        super.visitVarInsn(ALOAD, 1);
                        super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "jetpackBlocked",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                        super.visitJumpInsn(IFEQ, nativeThrust);
                        super.visitInsn(ICONST_0);
                        super.visitInsn(IRETURN);
                        super.visitLabel(nativeThrust);
                        super.visitFrame(F_SAME, 0, null, 0, null);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        // The first return is IC2 rejecting empty equipment. Slot 11 is live on success.
                        if (opcode == IRETURN && ++returns[0] == 2) {
                            super.visitVarInsn(ALOAD, 0);
                            super.visitVarInsn(ALOAD, 1);
                            super.visitVarInsn(DLOAD, NATIVE_PRE_THRUST_Y);
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "jetpackThrust",
                                    "(Ljava/lang/Object;Ljava/lang/Object;D)V", false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);
        if (returns[0] != 2) throw new IllegalArgumentException("unexpected IC2 jetpack returns");
        return writer.toByteArray();
    }
}
