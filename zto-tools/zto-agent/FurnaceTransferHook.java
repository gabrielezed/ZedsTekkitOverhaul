package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** The pinned base controller also serves NEI and Inventory Tweaks window clicks in local play. */
final class FurnaceTransferHook implements Opcodes {
    static final String TARGET = "ki";
    static final String SHA256 = "fb011f152e3f00f3b1b641264e1182351fdc08d0b3bece01d46347cf33d243c6";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] count = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name) || !"(IIIZLyw;)Laan;".equals(descriptor)) return method;
                count[0]++;
                return new MethodVisitor(ASM9, method) {
                    @Override public void visitCode() {
                        super.visitCode();
                        Label original = new Label();
                        visitVarInsn(ILOAD, 4);
                        visitJumpInsn(IFEQ, original);
                        visitVarInsn(ALOAD, 5);
                        visitVarInsn(ILOAD, 1);
                        visitVarInsn(ILOAD, 2);
                        visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "transferFurnaceFuel", "(Ljava/lang/Object;II)Z", false);
                        visitJumpInsn(IFEQ, original);
                        visitInsn(ACONST_NULL);
                        visitInsn(ARETURN);
                        visitLabel(original);
                    }
                };
            }
        }, 0);
        if (count[0] != 1) throw new IllegalStateException("Expected one local window click method");
        return writer.toByteArray();
    }
}
