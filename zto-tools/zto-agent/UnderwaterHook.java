package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Pins the native decrement, first (water) thrust call and OptiFine water fog in Tekkit Classic. */
final class UnderwaterHook implements Opcodes {
    static final String LIVING = "acq";
    static final String RENDERER = "lr";
    private static final String LIVING_SHA = "3711d2b407c6ff0a4860094492c10c3a81c49e5032dd5226b4a547f3b945b24b";
    private static final String RENDERER_SHA = "029d4557456bd198178c82958392db88c2ba9b7e16c478470e724184027f81a5";
    private static final int WATER_FOG_CALL = 9;

    private UnderwaterHook() {}

    static boolean matches(String name) {
        return LIVING.equals(name) || RENDERER.equals(name);
    }

    static boolean accepts(String name, String hash) {
        return LIVING.equals(name) ? LIVING_SHA.equals(hash) : RENDERER.equals(name) && RENDERER_SHA.equals(hash);
    }

    static byte[] patch(String target, byte[] bytes) {
        boolean living = LIVING.equals(target);
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] calls = new int[3];
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor out = super.visitMethod(access, name, desc, signature, exceptions);
                if (living && name.equals("B") && desc.equals("()V")) return breath(out, calls);
                if (living && name.equals("a_") && desc.equals("(FF)V")) return swim(out, calls);
                if (!living && name.equals("a") && desc.equals("(IF)V")) return fog(out, calls);
                return out;
            }
        }, 0);
        if (living ? calls[0] != 1 || calls[1] != 3 : calls[2] != 12) {
            throw new IllegalStateException("Unexpected underwater call sites in " + target);
        }
        return writer.toByteArray();
    }

    private static MethodVisitor breath(MethodVisitor out, int[] calls) {
        return new MethodVisitor(ASM9, out) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                boolean decrement = opcode == INVOKEVIRTUAL && owner.equals(LIVING)
                        && name.equals("b") && desc.equals("(I)I");
                if (decrement) super.visitInsn(DUP2);
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                if (decrement) {
                    calls[0]++;
                    super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "underwaterAir", "(Ljava/lang/Object;II)I", false);
                }
            }
        };
    }

    private static MethodVisitor swim(MethodVisitor out, int[] calls) {
        return new MethodVisitor(ASM9, out) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == INVOKEVIRTUAL && owner.equals(LIVING) && name.equals("a") && desc.equals("(FFF)V")
                        && ++calls[1] == 1) {
                    super.visitVarInsn(ALOAD, 0);
                    super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "underwaterThrust", "(FLjava/lang/Object;)F", false);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };
    }

    private static MethodVisitor fog(MethodVisitor out, int[] calls) {
        return new MethodVisitor(ASM9, out) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == INVOKESTATIC && owner.equals("org/lwjgl/opengl/GL11")
                        && name.equals("glFogf") && desc.equals("(IF)V") && ++calls[2] == WATER_FOG_CALL) {
                    super.visitVarInsn(ALOAD, 3); // The native render-view entity.
                    super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "underwaterFog", "(FLjava/lang/Object;)F", false);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };
    }
}
