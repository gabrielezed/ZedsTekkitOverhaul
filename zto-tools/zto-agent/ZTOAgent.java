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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.function.ToIntBiFunction;

/** Java startup entry point and bridge. Its signatures never link game classes. */
public final class ZTOAgent {
    static final String BRIDGE = "zed/tekkitoverhaul/agent/ZTOAgent";
    private static final Map<ClassLoader, Map<String, Boolean>> STATUS = new WeakHashMap<>();
    private static boolean started;
    private static volatile WeakReference<Predicate<Object>> mouse = new WeakReference<>(null);
    private static volatile WeakReference<BiPredicate<Object, int[]>> furnaceFuel = new WeakReference<>(null);
    private static volatile WeakReference<ToIntBiFunction<Object, Integer>> moon = new WeakReference<>(null);
    private static volatile WeakReference<Predicate<Object>> industry = new WeakReference<>(null);
    private static volatile WeakReference<BiConsumer<Object, Boolean>> spawnStart = new WeakReference<>(null);
    private static volatile WeakReference<BiFunction<Object, int[], Boolean>> spawnCap = new WeakReference<>(null);
    private static volatile WeakReference<Predicate<Object>> spawnCandidate = new WeakReference<>(null);
    private static volatile WeakReference<Consumer<Object>> spawnInserted = new WeakReference<>(null);
    private static volatile WeakReference<UnaryOperator<Object>> spawnReplacement = new WeakReference<>(null);

    private static volatile WeakReference<ObjDoubleConsumer<Object>> jetpack = new WeakReference<>(null);
    private static volatile WeakReference<Predicate<Object>> jetpackGuard = new WeakReference<>(null);
    private static volatile WeakReference<BiConsumer<Object[], int[]>> provenance = new WeakReference<>(null);

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
        if (!inventory && !lunar && industrial == null && !NaturalSpawnHook.TARGET.equals(name)
                && !JetpackHook.TARGET.equals(name) && !FurnaceTransferHook.TARGET.equals(name)
                && ProvenanceHook.find(name) == null) return null;
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
        ProvenanceHook mutation = ProvenanceHook.find(name);
        if (mutation != null && mutation.hash.equals(hash)) {
            byte[] observed = mutation.patch(bytes);
            return mutation == ProvenanceHook.REMOVE ? FurnaceTransferHook.patch(observed) : observed;
        }
        if (JetpackHook.TARGET.equals(name) && JetpackHook.SHA256.equals(hash)) return JetpackHook.patch(bytes);
        if (NaturalSpawnHook.TARGET.equals(name) && NaturalSpawnHook.SHA256.equals(hash))
            return NaturalSpawnHook.patch(bytes);
        if (inventory && InventoryHook.SHA256.equals(hash)) return InventoryHook.patch(bytes);
        if (FurnaceTransferHook.TARGET.equals(name) && FurnaceTransferHook.SHA256.equals(hash))
            return FurnaceTransferHook.patch(bytes);
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

    public static void registerProvenance(BiConsumer<Object[], int[]> callback) {
        provenance = new WeakReference<>(callback);
    }

    public static int provenanceHooks(ClassLoader loader) {
        int active = 0;
        if (provenance.get() != null) {
            for (ProvenanceHook hook : ProvenanceHook.values()) {
                if (installed(loader, hook.target)) active |= 1 << hook.ordinal();
            }
        }
        return active;
    }

    public static void observeMutation(boolean success, Object world, Object actor, int x, int y, int z,
                                       int block, int source) {
        BiConsumer<Object[], int[]> callback = provenance.get();
        if (success && callback != null) callback.accept(new Object[] {world, actor}, new int[] {x, y, z, block, source});
    }

    public static void registerJetpack(ObjDoubleConsumer<Object> callback, Predicate<Object> guard) {
        jetpack = new WeakReference<>(callback);
        jetpackGuard = new WeakReference<>(guard);
    }

    public static boolean jetpackActive(ClassLoader loader) {
        return jetpack.get() != null && jetpackGuard.get() != null && installed(loader, JetpackHook.TARGET);
    }

    public static boolean jetpackBlocked(Object item, Object player) {
        Predicate<Object> guard = jetpackGuard.get();
        return guard != null && electricJetpack(item) && guard.test(player);
    }

    public static void jetpackThrust(Object item, Object player, double previousY) {
        ObjDoubleConsumer<Object> callback = jetpack.get();
        if (callback != null && electricJetpack(item)) callback.accept(player, previousY);
    }

    private static boolean electricJetpack(Object item) {
        return "ic2.common.ItemArmorJetpackElectric".equals(item.getClass().getName());
    }

    public static void registerMouse(Predicate<Object> callback) { mouse = new WeakReference<>(callback); }
    public static void registerFurnaceFuel(BiPredicate<Object, int[]> callback) { furnaceFuel = new WeakReference<>(callback); }

    public static boolean transferFurnaceFuel(Object player, int window, int slot) {
        BiPredicate<Object, int[]> callback = furnaceFuel.get();
        return callback != null && callback.test(player, new int[] {window, slot});
    }

    public static void registerMoon(ToIntBiFunction<Object, Integer> callback) { moon = new WeakReference<>(callback); }
    public static void registerIndustry(Predicate<Object> callback) { industry = new WeakReference<>(callback); }

    public static void registerNaturalSpawning(BiConsumer<Object, Boolean> start,
            BiFunction<Object, int[], Boolean> cap, Predicate<Object> candidate, Consumer<Object> inserted) {
        spawnStart = new WeakReference<>(start);
        spawnCap = new WeakReference<>(cap);
        spawnCandidate = new WeakReference<>(candidate);
        spawnInserted = new WeakReference<>(inserted);
    }

    public static boolean naturalSpawningActive(ClassLoader loader) {
        return spawnCap.get() != null && installed(loader, NaturalSpawnHook.TARGET);
    }

    public static void registerNaturalSpawnReplacement(UnaryOperator<Object> callback) {
        spawnReplacement = new WeakReference<>(callback);
    }

    public static Object naturalSpawnReplacement(Object entity, Object type) {
        UnaryOperator<Object> callback = spawnReplacement.get();
        return callback != null && hostile(type) ? callback.apply(entity) : entity;
    }

    public static void beginNaturalSpawning(Object world, boolean hostileEnabled) {
        BiConsumer<Object, Boolean> callback = spawnStart.get();
        if (callback != null) callback.accept(world, hostileEnabled);
    }

    public static boolean aboveNaturalCap(int count, int cap, Object world, Object type) {
        BiFunction<Object, int[], Boolean> callback = spawnCap.get();
        return callback != null && hostile(type) ? callback.apply(world, new int[] {count, cap}) : count > cap;
    }

    public static boolean allowNaturalCandidate(boolean vanilla, Object world, Object type) {
        if (!vanilla || !hostile(type)) return vanilla;
        Predicate<Object> callback = spawnCandidate.get();
        return callback == null || callback.test(world);
    }

    public static void recordNaturalSpawn(boolean inserted, Object world, Object type) {
        if (!inserted || !hostile(type)) return;
        Consumer<Object> callback = spawnInserted.get();
        if (callback != null) callback.accept(world);
    }

    private static boolean hostile(Object type) {
        // Pinned acf declares monster as its first constant.
        return ((Enum<?>) type).ordinal() == 0;
    }

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
