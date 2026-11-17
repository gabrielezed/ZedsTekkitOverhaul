package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Intercepts only the ordinary spawning pass in the pinned Tekkit client. */
final class NaturalSpawnHook implements Opcodes {
    static final String TARGET = "vf";
    static final String SHA256 = "59c1b4c3f6c2aa036aba1defe1c9987d83d552e0cc17e119eafdbf8822d32dec";
    private static final int CREATURE_TYPE_SLOT = 8;

    private NaturalSpawnHook() {}

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] sites = new int[4];
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, desc, signature, exceptions);
                if (!name.equals("a") || !desc.equals("(Lxd;ZZ)I")) return method;
                return new MethodVisitor(ASM9, method) {
                    private boolean capDivision;

                    @Override
                    public void visitInsn(int opcode) {
                        capDivision = opcode == IDIV;
                        super.visitInsn(opcode);
                    }

                    private void context() {
                        super.visitVarInsn(ALOAD, 0);
                        super.visitVarInsn(ALOAD, CREATURE_TYPE_SLOT);
                    }

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(ALOAD, 0);
                        super.visitVarInsn(ILOAD, 1);
                        super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "beginNaturalSpawning",
                                "(Ljava/lang/Object;Z)V", false);
                        sites[0]++;
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        if (opcode == IF_ICMPGT && capDivision) {
                            context();
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "aboveNaturalCap",
                                    "(IILjava/lang/Object;Ljava/lang/Object;)Z", false);
                            opcode = IFNE;
                            sites[1]++;
                        }
                        capDivision = false;
                        super.visitJumpInsn(opcode, label);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        if (owner.equals("java/util/Iterator") && name.equals("hasNext")
                                || owner.equals("acq") && name.equals("i") && desc.equals("()Z")) {
                            context();
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "allowNaturalCandidate",
                                    "(ZLjava/lang/Object;Ljava/lang/Object;)Z", false);
                            sites[2]++;
                        }
                        if (owner.equals("xd") && name.equals("a") && desc.equals("(Lnn;)Z")) {
                            super.visitInsn(DUP);
                            context();
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "recordNaturalSpawn",
                                    "(ZLjava/lang/Object;Ljava/lang/Object;)V", false);
                            sites[3]++;
                        }
                    }
                };
            }
        }, 0);
        if (sites[0] != 1 || sites[1] != 1 || sites[2] != 2 || sites[3] != 1)
            throw new IllegalStateException("unexpected natural spawning call sites");
        return writer.toByteArray();
    }
}
