package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/** Pins the native decrement, first (water) thrust call and OptiFine water fog in Tekkit Classic. */
final class UnderwaterHook implements Opcodes {
    static final String LIVING = "acq";
    static final String RENDERER = "lr";
    private static final String LIVING_SHA = "3711d2b407c6ff0a4860094492c10c3a81c49e5032dd5226b4a547f3b945b24b";
    private static final String RENDERER_SHA = "029d4557456bd198178c82958392db88c2ba9b7e16c478470e724184027f81a5";
    private static final int GL_FOG_DENSITY = 2914;

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
        int[] calls = new int[5];
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor out = super.visitMethod(access, name, desc, signature, exceptions);
                if (living && name.equals("B") && desc.equals("()V")) return breath(out, calls);
                if (living && name.equals("a_") && desc.equals("(FF)V")) return swim(out, calls);
                if (!living && name.equals("a") && desc.equals("(IF)V")) return fog(out, calls);
                if (!living && name.equals("e") && desc.equals("()V")) return lightmapUpload(out, calls);
                return out;
            }
        }, 0);
        if (living ? calls[0] != 1 || calls[1] != 3 : calls[2] != 1 || calls[3] != 1 || calls[4] != 2) {
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

    private static MethodVisitor lightmapUpload(MethodVisitor out, int[] calls) {
        return new MethodVisitor(ASM9, out) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                if (opcode == INVOKEVIRTUAL && owner.equals("aaw") && name.equals("a") && desc.equals("([IIII)V")) {
                    super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "underwaterNativeLightmapUploaded", "()V", false);
                    calls[4]++;
                }
            }
        };
    }

    private static MethodVisitor fog(MethodVisitor out, int[] calls) {
        return new MethodVisitor(ASM9, out) {
            private boolean waterMaterial, clearWater, densityParameter, densityValue;
            private Label densityJoin;

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (opcode == GETSTATIC && owner.equals("acn") && name.equals("g") && desc.equals("Lacn;"))
                    waterMaterial = true;
                super.visitFieldInsn(opcode, owner, name, desc);
            }

            @Override
            public void visitJumpInsn(int opcode, Label label) {
                if (clearWater && opcode == IFEQ) densityJoin = label;
                clearWater = false;
                super.visitJumpInsn(opcode, label);
            }

            @Override
            public void visitLabel(Label label) {
                densityParameter = false;
                densityValue = label == densityJoin;
                super.visitLabel(label);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                densityParameter = densityValue && opcode == SIPUSH && operand == GL_FOG_DENSITY;
                densityValue = false;
                super.visitIntInsn(opcode, operand);
            }

            @Override
            public void visitVarInsn(int opcode, int var) {
                densityValue = densityParameter && opcode == FLOAD && var == 6;
                densityParameter = false;
                super.visitVarInsn(opcode, var);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (densityValue && opcode == INVOKESTATIC && owner.equals("org/lwjgl/opengl/GL11")
                        && name.equals("glFogf") && desc.equals("(IF)V")) {
                    super.visitVarInsn(ALOAD, 3);
                    super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "underwaterFog", "(FLjava/lang/Object;)F", false);
                    calls[2]++;
                }
                clearWater = waterMaterial && opcode == INVOKESTATIC && owner.equals("Config")
                        && name.equals("isClearWater") && desc.equals("()Z");
                densityValue = false;
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                if (opcode == INVOKESTATIC && owner.equals("aau") && name.equals("a")
                        && desc.equals("(Lxd;Lacq;F)I")) {
                    // Keep the native camera block ID on the stack for the renderer.
                    super.visitInsn(DUP);
                    super.visitVarInsn(ALOAD, 3);
                    super.visitVarInsn(ALOAD, 0);
                    super.visitFieldInsn(GETFIELD, RENDERER, "r", "Lnet/minecraft/client/Minecraft;");
                    super.visitFieldInsn(GETFIELD, "net/minecraft/client/Minecraft", "p", "Laaw;");
                    super.visitVarInsn(ALOAD, 0);
                    super.visitFieldInsn(GETFIELD, RENDERER, "Q", "[I");
                    super.visitVarInsn(ALOAD, 0);
                    super.visitFieldInsn(GETFIELD, RENDERER, "d", "I");
                    super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "underwaterLightmap",
                            "(ILjava/lang/Object;Ljava/lang/Object;[II)V", false);
                    calls[3]++;
                }
            }
        };
    }
}
