package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Java 8 launch agent that leaves the original moon quad and atlas lookup in place. */
final class LunarHook {
    private static final String TEKKIT = "23f7dfa8ee3e8c2707090c6c7dfb8f1843fef9692fd34a0d9588c79c2c8df312";
    private static final String VANILLA = "339a99bfa23e5b9329ceb2af33a84f1e130c96f8061aefd071078fe6d3fc5675";
    private static final String MCP = "9a46dbed02eea4fa9dbae9552ce066db33bb3151383ffa53d237962e81cefa4a";
    static final String MCP_RENDERER = "net/minecraft/src/RenderGlobal";
    static boolean matches(String name) {
        return "l".equals(name) || MCP_RENDERER.equals(name);
    }

    static boolean accepts(String name, String fingerprint) {
        return MCP_RENDERER.equals(name) ? MCP.equals(fingerprint)
                : TEKKIT.equals(fingerprint) || VANILLA.equals(fingerprint);
    }

    static byte[] patch(byte[] bytes, final boolean mcp) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] calls = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals(mcp ? "renderSky" : "a") || !descriptor.equals("(F)V")) return target;
                return new MethodVisitor(Opcodes.ASM9, target) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        boolean phase = opcode == Opcodes.INVOKEVIRTUAL && desc.equals("(F)I")
                                && owner.equals(mcp ? "net/minecraft/src/World" : "xd")
                                && method.equals(mcp ? "getMoonPhase" : "d");
                        if (phase) visitInsn(Opcodes.DUP2);
                        super.visitMethodInsn(opcode, owner, method, desc, itf);
                        if (phase) {
                            calls[0]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, ZTOAgent.BRIDGE, "selectMoon", "(Ljava/lang/Object;FI)I", false);
                        }
                    }
                };
            }
        }, 0);
        if (calls[0] != 1) throw new IllegalStateException("Expected one native moon phase call");
        return writer.toByteArray();
    }
}
