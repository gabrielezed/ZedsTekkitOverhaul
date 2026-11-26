package zed.tekkitoverhaul.agent;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.lang.management.ManagementFactory;
import java.util.zip.ZipFile;
import zed.tekkitoverhaul.agent.internal.asm.ClassReader;
import zed.tekkitoverhaul.agent.internal.asm.ClassVisitor;
import zed.tekkitoverhaul.agent.internal.asm.MethodVisitor;
import zed.tekkitoverhaul.agent.internal.asm.Opcodes;

/** Loads and verifies the installed pack classes without starting Minecraft or LWJGL input. */
public final class AgentProbe {
    public static void main(String[] args) throws Exception {
        long agents = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(option -> option.startsWith("-javaagent:")).count();
        check(agents == 1, "exactly one startup agent");
        check(!ZTOAgent.paused(new Object()) && ZTOAgent.selectMoon(null, 0, 6) == 6,
                "unregistered hooks delegate");
        check(ZTOAgent.underwaterAir(null, 12, 11) == 11
                && ZTOAgent.underwaterThrust(.02F, null) == .02F && ZTOAgent.underwaterFog(.1F, null) == .1F,
                "underwater hooks delegate without callbacks");
        if ("archive".equals(args[0])) {
            try (ZipFile game = new ZipFile(args[1])) {
                byte[] title = bytes(game.getInputStream(game.getEntry("xt.class")));
                check(ZTOAgent.patchKnownClass("xt", title, null, false, false) != null, "pinned native title patch");
                rejectFingerprint("xt", title, null, false);
                String[] branding = {"44 mods loaded", "Optifine", "Forge", "Minecraft"};
                check(ZTOAgent.titleBranding(branding) == branding, "native branding without registration");
                ZTOAgent.registerTitleBuild("ZTO test");
                check(java.util.Arrays.equals(ZTOAgent.titleBranding(branding),
                        new String[] {"44 mods loaded", "ZTO test", "Optifine", "Forge", "Minecraft"}),
                        "ZTO follows OptiFine and precedes the mod count in draw order");
                ZTOAgent.registerTitleBuild(null);
                byte[] renderer = bytes(game.getInputStream(game.getEntry("l.class")));
                checkMoonPatch(renderer);
                rejectFingerprint("l", renderer, null, true);
                byte[] gui = bytes(game.getInputStream(game.getEntry("vp.class")));
                check(ZTOAgent.patchKnownClass("vp", gui, null, true, false) != null, "pinned mouse patch");
                byte[] trench = bytes(game.getInputStream(game.getEntry("aly.class")));
                check(ZTOAgent.patchKnownClass("aly", trench, null, false, false) != null, "pinned terrain provider");
                rejectFingerprint("aly", trench, null, false);
                byte[] spawner = bytes(game.getInputStream(game.getEntry("vf.class")));
                check(ZTOAgent.patchKnownClass("vf", spawner, null, false, false) != null, "pinned natural spawner");
                rejectFingerprint("vf", spawner, null, false);
                for (String target : new String[] {UnderwaterHook.LIVING, UnderwaterHook.RENDERER}) {
                    byte[] underwater = bytes(game.getInputStream(game.getEntry(target + ".class")));
                    check(ZTOAgent.patchKnownClass(target, underwater, null, false, false) != null,
                            "pinned underwater transformation for " + target);
                    rejectFingerprint(target, underwater, null, false);
                }
            }
            System.out.println("PASS standalone startup and pinned bytecode transformation on Java " + System.getProperty("java.version"));
            return;
        }
        ClassLoader loader = AgentProbe.class.getClassLoader();
        Class<?> gui = Class.forName("vp");
        Object screen = gui.newInstance();
        AtomicInteger calls = new AtomicInteger();
        Predicate<Object> probeMouse = value -> value == screen && calls.incrementAndGet() == 1;
        ZTOAgent.registerMouse(probeMouse);
        check(ZTOAgent.mouseActive(gui), "pinned GuiScreen and callback");
        gui.getMethod("f").invoke(screen);
        check(calls.get() == 1, "one transformed dispatcher invocation");
        check(Class.forName("gb", false, loader).getDeclaredMethod("f") != null, "NEI retains its mouse override");
        ZTOAgent.registerMouse(null);
        Class.forName("zed.tekkitoverhaul.integration.inventory.DragDistributionIntegration")
                .getMethod("register").invoke(null);
        check(ZTOAgent.mouseActive(gui) && !ZTOAgent.handleMouse(screen), "production callback and non-container passthrough");

        Class.forName("l", false, loader);
        Class<?> lunar = Class.forName("zed.tekkitoverhaul.lunar.client.LunarMoonRenderer");
        Class<?> calendar = Class.forName("zed.tekkitoverhaul.lunar.client.LocalLunarCalendar");
        Method register = lunar.getDeclaredMethod("register", calendar);
        register.setAccessible(true);
        register.invoke(null, calendar.newInstance());
        check(ZTOAgent.moonStatus(loader).contains("hook active"), "pinned renderer and production callback");
        byte[] renderer = bytes(loader.getResourceAsStream("l.class"));
        checkMoonPatch(renderer);
        Object world = new Object();
        ToIntBiFunction<Object, Integer> probeMoon = (candidate, original) -> candidate == world ? 4 : original;
        ZTOAgent.registerMoon(probeMoon);
        check(ZTOAgent.selectMoon(world, 0, 6) == 4 && ZTOAgent.selectMoon(null, 0, 6) == 6,
                "selected phase and fallback");

        Method registerIndustry = Class.forName("zed.tekkitoverhaul.survival.tactical.IndustrialSabotage")
                .getDeclaredMethod("register");
        registerIndustry.setAccessible(true);
        registerIndustry.invoke(null);
        boolean reduced = "reduced".equals(args[0]);
        int loaded = 0;
        for (IndustrialHooks.Hook hook : IndustrialHooks.Hook.values()) {
            if (reduced && hook != IndustrialHooks.Hook.EU) {
                check(loader.getResource(hook.name + ".class") == null, "optional class unexpectedly present");
                continue;
            }
            Class<?> target = Class.forName(hook.name.replace('/', '.'), false, loader);
            target.getDeclaredMethods();
            check(ZTOAgent.industrialActive(target), hook.name + " transformation and production callback");
            rejectFingerprint(hook.name, bytes(loader.getResourceAsStream(hook.name + ".class")), hook, false);
            loaded++;
        }
        byte[] jetpack = bytes(loader.getResourceAsStream(JetpackHook.TARGET + ".class"));
        check(ZTOAgent.patchKnownClass(JetpackHook.TARGET, jetpack, null, false, false) != null,
                "pinned IC2 jetpack guard and observer");
        rejectFingerprint(JetpackHook.TARGET, jetpack, null, false);
        rejectFingerprint("l", renderer, null, true);
        check(ZTOAgent.mouseActive(gui) && ZTOAgent.moonStatus(loader).contains("hook active"),
                "independent module state");
        System.out.println("PASS " + (reduced ? "reduced" : "full") + " pack linkage, production callbacks, " + loaded
                + " direct hook classes and conservative fingerprint rejection on Java " + System.getProperty("java.version"));
    }

    private static void checkMoonPatch(byte[] renderer) throws Exception {
        byte[] patched = ZTOAgent.patchKnownClass("l", renderer, null, false, true);
        AtomicInteger phaseCalls = new AtomicInteger();
        new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (owner.equals(ZTOAgent.BRIDGE) && name.equals("selectMoon")) phaseCalls.incrementAndGet();
                    }
                };
            }
        }, 0);
        check(phaseCalls.get() == 1, "one moon atlas selection");
    }

    private static void rejectFingerprint(String name, byte[] bytes, IndustrialHooks.Hook hook, boolean lunar) throws Exception {
        bytes[bytes.length - 1] ^= 1;
        try {
            ZTOAgent.patchKnownClass(name, bytes, hook, false, lunar);
            throw new AssertionError("accepted unknown fingerprint for " + name);
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().equals("unsupported class fingerprint"), "fingerprint guard");
        }
    }

    private static byte[] bytes(InputStream source) throws Exception {
        try (InputStream stream = source;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (stream == null) throw new AssertionError("Missing class resource");
            byte[] buffer = new byte[8192];
            for (int count; (count = stream.read(buffer)) != -1;) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
