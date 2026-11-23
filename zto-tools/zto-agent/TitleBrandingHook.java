package zed.tekkitoverhaul.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Inserts the build into the native title's branding array on the pinned Tekkit overlay. */
final class TitleBrandingHook implements Opcodes {
    static final String TARGET = "xt";
    static final String SHA256 = "a666d03e3a953afedf35da6c9119ad8e81d2a4332c99a96c1b265825a73c17c4";

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        int[] sites = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name) || !"(IIF)V".equals(descriptor)) return method;
                return new MethodVisitor(ASM9, method) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
                        super.visitMethodInsn(opcode, owner, name, descriptor, itf);
                        if (opcode == INVOKEVIRTUAL && "cpw/mods/fml/common/FMLCommonHandler".equals(owner)
                                && "getBrandingStrings".equals(name)
                                && "(Ljava/lang/String;)[Ljava/lang/String;".equals(descriptor)) {
                            super.visitMethodInsn(INVOKESTATIC, ZTOAgent.BRIDGE, "titleBranding",
                                    "([Ljava/lang/String;)[Ljava/lang/String;", false);
                            sites[0]++;
                        }
                    }
                };
            }
        }, 0);
        if (sites[0] != 1) throw new IllegalArgumentException("unexpected title branding call sites");
        return writer.toByteArray();
    }
}
