package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Observes the pinned call sites after the native write, preserving its return value. */
enum ProvenanceHook implements Opcodes {
    PLACE("vd", "f0030f5471c757cd7915dbf5b401774f6b1cf1f0a8f77c0cc179415baccd2439",
            "a", "(Laan;Lyw;Lxd;IIII)Z", "pb", "a", "(Lxd;IIILacq;)V"),
    REMOVE("ki", "fb011f152e3f00f3b1b641264e1182351fdc08d0b3bece01d46347cf33d243c6",
            "b", "(IIII)Z", "pb", "removeBlockByPlayer", "(Lxd;Lyw;III)Z"),
    QUARRY("buildcraft/factory/TileQuarry", "48cb3acd69af64a40d161462a679a963df2fa13b9cdf73f589f1b34f1ce3d11a",
            "positionReached", "(Lbuildcraft/factory/EntityMechanicalArm;)V", "xd", "g", "(IIII)Z");

    final String target, hash;
    private final String method, signature, callOwner, call, callSignature;

    ProvenanceHook(String target, String hash, String method, String signature,
                   String callOwner, String call, String callSignature) {
        this.target = target;
        this.hash = hash;
        this.method = method;
        this.signature = signature;
        this.callOwner = callOwner;
        this.call = call;
        this.callSignature = callSignature;
    }

    static ProvenanceHook find(String name) {
        if (PLACE.target.equals(name)) return PLACE;
        if (REMOVE.target.equals(name)) return REMOVE;
        if (QUARRY.target.equals(name)) return QUARRY;
        return null;
    }

    byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] sites = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                MethodVisitor output = super.visitMethod(access, name, desc, sig, exceptions);
                if (!method.equals(name) || !signature.equals(desc)) return output;
                return new MethodVisitor(ASM9, output) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        if (opcode != INVOKEVIRTUAL || !callOwner.equals(owner)
                                || !call.equals(name) || !callSignature.equals(desc)) return;
                        sites[0]++;
                        if (ProvenanceHook.this == PLACE) {
                            visitInsn(ICONST_1);
                            visitVarInsn(ALOAD, 3);
                            visitVarInsn(ALOAD, 2);
                            coordinates(4);
                            visitVarInsn(ALOAD, 0);
                            visitFieldInsn(GETFIELD, "vd", "a", "I");
                        } else if (ProvenanceHook.this == REMOVE) {
                            visitInsn(DUP);
                            visitVarInsn(ALOAD, 5);
                            visitVarInsn(ALOAD, 0);
                            visitFieldInsn(GETFIELD, "ki", "a", "Lnet/minecraft/client/Minecraft;");
                            visitFieldInsn(GETFIELD, "net/minecraft/client/Minecraft", "h", "Lvq;");
                            coordinates(1);
                            visitVarInsn(ALOAD, 6);
                            visitFieldInsn(GETFIELD, "pb", "bO", "I");
                        } else {
                            visitInsn(DUP);
                            visitVarInsn(ALOAD, 0);
                            visitFieldInsn(GETFIELD, QUARRY.target, "i", "Lxd;");
                            visitInsn(ACONST_NULL);
                            coordinates(2);
                            visitVarInsn(ILOAD, 5);
                        }
                        visitInsn(ICONST_0 + ordinal());
                        super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "observeMutation",
                                "(ZLjava/lang/Object;Ljava/lang/Object;IIIII)V", false);
                    }

                    private void coordinates(int first) {
                        for (int local = first; local < first + 3; local++) visitVarInsn(ILOAD, local);
                    }
                };
            }
        }, 0);
        if (sites[0] != 1) throw new IllegalStateException("Expected one provenance observation in " + target);
        return writer.toByteArray();
    }
}
