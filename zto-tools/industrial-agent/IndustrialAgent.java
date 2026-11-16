package zed.tekkitoverhaul.industrialagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Launch-time gates for the exact Tekkit Classic client bytecode. No Minecraft dependency. */
public final class IndustrialAgent implements Opcodes {
    private static final String AGENT = "zed/tekkitoverhaul/industrialagent/IndustrialAgent";
    private static final String EMIT = "(Lic2/api/IEnergySource;I)I";
    private static final Hook[] HOOKS = Hook.values();
    private static final Map<ClassLoader, EnumSet<Hook>> INSTALLED = new WeakHashMap<ClassLoader, EnumSet<Hook>>();
    private static volatile Predicate<Object> gate;

    private enum Hook {
        ITEMS("buildcraft/transport/pipes/PipeItemsWood", "f5f084ba7c421caedf090f55fca3a09fda14585bff7574477f6d1516d0562d00", 1),
        LIQUIDS("buildcraft/transport/pipes/PipeLiquidsWood", "ccb7f5d7ce236aa71454b19201f5f5702d8bd7835acfe53775ecc3467a886ece", 2),
        ENGINE("buildcraft/energy/TileEngine", "20ec0a85265cd48684da97150ecc5a182b3a36f83ee0385830d88febbad5a518", 1),
        EU("ic2/common/TileEntityElectricBlock", "bffa7d7c41cfa8c3353f96a73eee4354afdfc98ac7dccec7f9694e9aaa21a150", 1),
        SOLAR("cpw/mods/compactsolars/TileEntityCompactSolar", "090cc73a3053d7ee20d0636167aed3ea6b8bde6c492c64b08b2702e21a0714fd", 1),
        REDPOWER("eloraam/machine/TileRelay", "80690f4f05c566c279d1ad586ae02b18e4a9eedc40721d2d6104a879c181ef57", 1),
        COLLECTOR("ee/TileCollector", "847e1203b683ce6facde3f862f898d23c21ac608a0396603797a655d6997c2bb", 1),
        COLLECTOR_2("ee/TileCollector2", "e72c40b343ff9cd6df5d267a0d1e32a199dc20616da38701e28d7b32c23d4d5e", 1),
        COLLECTOR_3("ee/TileCollector3", "003f83647e015a987dbf16c197ec4f7fe74623f686e8aef15d8d4081be91d0ab", 1);

        final String name, hash;
        final int sites;

        Hook(String name, String hash, int sites) {
            this.name = name;
            this.hash = hash;
            this.sites = sites;
        }
    }

    private IndustrialAgent() {}

    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> previous,
                                    ProtectionDomain domain, byte[] bytes) {
                if (loader == null || previous != null || name == null) return null;
                for (Hook hook : HOOKS) {
                    if (name.equals(hook.name)) {
                        try {
                            byte[] patched = patch(hook, bytes);
                            synchronized (INSTALLED) {
                                EnumSet<Hook> hooks = INSTALLED.get(loader);
                                if (hooks == null) {
                                    hooks = EnumSet.noneOf(Hook.class);
                                    INSTALLED.put(loader, hooks);
                                }
                                hooks.add(hook);
                            }
                            return patched;
                        } catch (Exception failure) {
                            System.err.println("[ZTO] Industrial hook unavailable for " + name + ": " + failure.getMessage());
                        }
                        break;
                    }
                }
                return null;
            }
        });
    }

    public static void register(Predicate<Object> callback) { gate = callback; }

    public static boolean installed(Class<?> type) {
        synchronized (INSTALLED) {
            EnumSet<Hook> hooks = INSTALLED.get(type.getClassLoader());
            if (hooks == null) return false;
            String name = type.getName().replace('.', '/');
            for (Hook hook : hooks) if (name.equals(hook.name)) return true;
            return false;
        }
    }

    public static boolean paused(Object target) {
        Predicate<Object> callback = gate;
        return callback != null && callback.test(target);
    }

    private static byte[] patch(final Hook hook, byte[] bytes) throws Exception {
        StringBuilder fingerprint = new StringBuilder();
        for (byte part : MessageDigest.getInstance("SHA-256").digest(bytes))
            fingerprint.append(Integer.toHexString((part & 255) | 256).substring(1));
        if (!hook.hash.contentEquals(fingerprint)) throw new IllegalArgumentException("unexpected class fingerprint");
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] sites = {0};
        final String energyNet = hook == Hook.EU ? "ic2/common/EnergyNet"
                : hook == Hook.SOLAR ? "ic2/api/EnergyNet" : null;
        reader.accept(new ClassVisitor(ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor target = super.visitMethod(access, name, desc, signature, exceptions);
                if ((hook == Hook.ITEMS || hook == Hook.LIQUIDS) && name.equals("doWork") && desc.equals("()V"))
                    return entryGate(target, sites, RETURN);
                if (hook == Hook.REDPOWER && name.equals("handleExtract") && desc.equals("()Z"))
                    return entryGate(target, sites, IRETURN);
                if (hook.name.startsWith("ee/") && name.equals("n_") && desc.equals("()V"))
                    return entryGate(target, sites, RETURN);
                if (hook == Hook.LIQUIDS && name.equals("updateEntity") && desc.equals("()V"))
                    return afterSuper(target, sites, "buildcraft/transport/Pipe", "updateEntity");
                if (hook == Hook.ENGINE && name.equals("n_") && desc.equals("()V"))
                    return afterSuper(target, sites, "buildcraft/core/TileBuildCraft", "n_");
                if (energyNet != null && name.equals("n_") && desc.equals("()V")) {
                    return new MethodVisitor(ASM5, target) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                            if (opcode == INVOKEVIRTUAL && owner.equals(energyNet) && method.equals("emitEnergyFrom")
                                    && desc.equals(EMIT)) {
                                sites[0]++;
                                super.visitMethodInsn(INVOKESTATIC, hook.name, "zto$offerEnergy", offer(energyNet), false);
                            } else super.visitMethodInsn(opcode, owner, method, desc, itf);
                        }
                    };
                }
                return target;
            }

            @Override
            public void visitEnd() {
                if (energyNet != null) addEnergyOffer(cv, energyNet);
                super.visitEnd();
            }
        }, 0);
        if (sites[0] != hook.sites) throw new IllegalStateException("unexpected number of native call sites");
        return writer.toByteArray();
    }

    private static MethodVisitor entryGate(MethodVisitor target, final int[] sites, final int returnOpcode) {
        return new MethodVisitor(ASM5, target) {
            @Override
            public void visitCode() {
                super.visitCode();
                sites[0]++;
                guard(mv, returnOpcode);
            }
        };
    }

    private static MethodVisitor afterSuper(MethodVisitor target, final int[] sites,
                                            final String superClass, final String method) {
        return new MethodVisitor(ASM5, target) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                if (opcode == INVOKESPECIAL && owner.equals(superClass) && name.equals(method) && desc.equals("()V")) {
                    sites[0]++;
                    guard(mv, RETURN);
                }
            }
        };
    }

    private static void guard(MethodVisitor method, int returnOpcode) {
        Label resume = new Label();
        method.visitVarInsn(ALOAD, 0);
        method.visitMethodInsn(INVOKESTATIC, AGENT, "paused", "(Ljava/lang/Object;)Z", false);
        method.visitJumpInsn(IFEQ, resume);
        if (returnOpcode == IRETURN) method.visitInsn(ICONST_0);
        method.visitInsn(returnOpcode);
        method.visitLabel(resume);
        method.visitFrame(F_SAME, 0, null, 0, null);
    }

    /** EnergyNet returns the unaccepted remainder. A withheld offer spends exactly zero EU. */
    private static void addEnergyOffer(ClassVisitor owner, String energyNet) {
        MethodVisitor method = owner.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "zto$offerEnergy", offer(energyNet), null, null);
        method.visitCode();
        Label emit = new Label();
        method.visitVarInsn(ALOAD, 1);
        method.visitMethodInsn(INVOKESTATIC, AGENT, "paused", "(Ljava/lang/Object;)Z", false);
        method.visitJumpInsn(IFEQ, emit);
        method.visitVarInsn(ILOAD, 2);
        method.visitInsn(IRETURN);
        method.visitLabel(emit);
        method.visitFrame(F_SAME, 0, null, 0, null);
        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(ALOAD, 1);
        method.visitVarInsn(ILOAD, 2);
        method.visitMethodInsn(INVOKEVIRTUAL, energyNet, "emitEnergyFrom", EMIT, false);
        method.visitInsn(IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static String offer(String energyNet) {
        return "(L" + energyNet + ";" + EMIT.substring(1);
    }
}
