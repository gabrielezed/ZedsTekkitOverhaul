package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Stamps Observatory slices before Forge's asynchronous Anvil queue sees the tag. */
final class ObservatorySaveHook implements Opcodes {
    static final String TARGET = "aiv";
    static final String SHA256 = "07178c250f035729d362fab6016c69f8545a0425eb82345840835cb709035913";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] sites = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name) || !"(Lxd;Lack;)V".equals(descriptor)) return visitor;
                return new MethodVisitor(ASM9, visitor) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == INVOKEVIRTUAL && TARGET.equals(owner) && "a".equals(name)
                                && "(Lsj;Lady;)V".equals(desc)) {
                            super.visitInsn(DUP);
                            super.visitVarInsn(ALOAD, 2);
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "beforeAnvilQueue",
                                    "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                            sites[0]++;
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            }
        }, 0);
        if (sites[0] != 1) throw new IllegalArgumentException("unexpected Anvil queue call site");
        return writer.toByteArray();
    }
}
