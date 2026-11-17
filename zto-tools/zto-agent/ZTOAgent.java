package zed.tekkitoverhaul.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

/** Java startup entry point and bridge. Its signatures never link game classes. */
public final class ZTOAgent {
    static final String BRIDGE = "zed/tekkitoverhaul/agent/ZTOAgent";
    private static final Map<ClassLoader, Map<String, Boolean>> STATUS = new WeakHashMap<>();
    private static boolean started;
    private static volatile WeakReference<Predicate<Object>> mouse = new WeakReference<>(null);
    private static volatile WeakReference<ToIntBiFunction<Object, Integer>> moon = new WeakReference<>(null);
    private static volatile WeakReference<Predicate<Object>> industry = new WeakReference<>(null);

    private ZTOAgent() {}

    public static synchronized void premain(String args, Instrumentation instrumentation) {
        if (started) return;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> previous,
                                    ProtectionDomain protection, byte[] bytes) {
                if (loader == null || previous != null || name == null) return null;
                try {
                    return patch(loader, name, bytes);
                } catch (Exception | LinkageError unsupported) {
                    record(loader, name, false);
                    System.err.println("[ZTO] Compatibility hook inactive for " + name + ": " + unsupported);
                    return null;
                }
            }
        });
        started = true;
    }

    private static byte[] patch(ClassLoader loader, String name, byte[] bytes) throws Exception {
        IndustrialHooks.Hook industrial = IndustrialHooks.find(name);
        boolean inventory = InventoryHook.TARGET.equals(name);
        boolean lunar = LunarHook.matches(name);
        if (!inventory && !lunar && industrial == null) return null;
        byte[] transformed = patchKnownClass(name, bytes, industrial, inventory, lunar);
        record(loader, name, true);
        return transformed;
    }

    static byte[] patchKnownClass(String name, byte[] bytes, IndustrialHooks.Hook industrial,
                                  boolean inventory, boolean lunar) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte part : MessageDigest.getInstance("SHA-256").digest(bytes))
            hex.append(Integer.toHexString((part & 255) | 256).substring(1));
        String hash = hex.toString();
        if (inventory && InventoryHook.SHA256.equals(hash)) return InventoryHook.patch(bytes);
        if (lunar && LunarHook.accepts(name, hash)) return LunarHook.patch(bytes, LunarHook.MCP_RENDERER.equals(name));
        if (industrial != null && industrial.hash.equals(hash)) return IndustrialHooks.patch(industrial, bytes);
        throw new IllegalArgumentException("unsupported class fingerprint");
    }

    private static synchronized void record(ClassLoader loader, String name, boolean applied) {
        STATUS.computeIfAbsent(loader, ignored -> new HashMap<>()).put(name, applied);
    }

    private static synchronized boolean installed(ClassLoader loader, String name) {
        Map<String, Boolean> hooks = STATUS.get(loader);
        return hooks != null && Boolean.TRUE.equals(hooks.get(name));
    }

    public static void registerMouse(Predicate<Object> callback) { mouse = new WeakReference<>(callback); }
    public static void registerMoon(ToIntBiFunction<Object, Integer> callback) { moon = new WeakReference<>(callback); }
    public static void registerIndustry(Predicate<Object> callback) { industry = new WeakReference<>(callback); }

    public static boolean mouseActive(Class<?> type) {
        return mouse.get() != null && installed(type.getClassLoader(), InventoryHook.TARGET);
    }

    public static synchronized String moonStatus(ClassLoader loader) {
        if (moon.get() == null) return "Moon callback unavailable; vanilla phase";
        if (installed(loader, "l") || installed(loader, LunarHook.MCP_RENDERER)) return "Native moon phase hook active";
        Map<String, Boolean> hooks = STATUS.get(loader);
        return hooks != null && (hooks.containsKey("l") || hooks.containsKey(LunarHook.MCP_RENDERER))
                ? "Unsupported renderer; vanilla phase" : "Waiting for RenderGlobal";
    }

    public static boolean industrialActive(Class<?> type) {
        return industry.get() != null && installed(type.getClassLoader(), type.getName().replace('.', '/'));
    }

    public static boolean handleMouse(Object screen) {
        Predicate<Object> callback = mouse.get();
        return callback != null && callback.test(screen);
    }

    public static int selectMoon(Object world, float partialTick, int original) {
        ToIntBiFunction<Object, Integer> callback = moon.get();
        return callback == null ? original : callback.applyAsInt(world, original);
    }

    public static boolean paused(Object target) {
        Predicate<Object> callback = industry.get();
        return callback != null && callback.test(target);
    }
}
