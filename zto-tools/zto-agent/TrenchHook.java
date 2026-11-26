package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds generation-only callbacks to the pinned 1.2.5 Overworld provider. */
final class TrenchHook implements Opcodes {
    static final String TARGET = "aly";
    static final String SHA256 = "a20110d90358ed60ef1c1b345415e174fb93edd3be687a5ebdda12e6386f2e29";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] sites = new int[2];
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"b".equals(name) || !"(II)Lack;".equals(descriptor)) return method;
                return new MethodVisitor(ASM9, method) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == INVOKEVIRTUAL && "ack".equals(owner) && "b".equals(name) && "()V".equals(desc)) {
                            super.visitInsn(DUP);
                            super.visitVarInsn(ALOAD, 7);
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "finishTrench",
                                    "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                            sites[1]++;
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        if (opcode == INVOKEVIRTUAL && TARGET.equals(owner) && "a".equals(name) && "(II[B[Labn;)V".equals(desc)) {
                            super.visitVarInsn(ALOAD, 0);
                            super.visitFieldInsn(GETFIELD, TARGET, "p", "Lxd;");
                            super.visitVarInsn(ALOAD, 3);
                            super.visitVarInsn(ALOAD, 0);
                            super.visitFieldInsn(GETFIELD, TARGET, "y", "[Labn;");
                            super.visitVarInsn(ILOAD, 1);
                            super.visitVarInsn(ILOAD, 2);
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "shapeTrench",
                                    "(Ljava/lang/Object;[BLjava/lang/Object;II)Ljava/lang/Object;", false);
                            super.visitVarInsn(ASTORE, 7);
                            sites[0]++;
                        }
                    }
                };
            }
        }, 0);
        if (sites[0] != 1 || sites[1] != 1) throw new IllegalArgumentException("unexpected trench generation sites");
        return writer.toByteArray();
    }
}
