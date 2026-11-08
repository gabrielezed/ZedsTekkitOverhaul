package zed.tekkitoverhaul.emc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.src.ItemStack;

/** Reflection boundary for the installed EE2 1.4.6.5 client. */
final class Ee2EmcAdapter {
    private final Field ready = Class.forName("ee.EEBase").getField("externalModsInitialized");
    private final Class<?> maps = Class.forName("ee.EEMaps");
    private final Field values = maps.getField("alchemicalValues");
    private final Method stackEmc = maps.getMethod("getEMC", ItemStack.class);
    private final Field fuels = maps.getField("fuelItemRegistry");

    Ee2EmcAdapter() throws ReflectiveOperationException {}

    boolean isReady() throws IllegalAccessException {
        return ready.getBoolean(null);
    }

    int stackValue(ItemStack stack) throws ReflectiveOperationException {
        return ((Integer) stackEmc.invoke(null, stack)).intValue();
    }

    boolean isCollectorFuel(int id) throws IllegalAccessException {
        return ((Map<?, ?>) fuels.get(null)).containsKey(id);
    }

    @SuppressWarnings("unchecked")
    Map<Integer, Map<Integer, Integer>> snapshot() throws IllegalAccessException {
        Map<Integer, Map<Integer, Integer>> source = (Map<Integer, Map<Integer, Integer>>) values.get(null);
        Map<Integer, Map<Integer, Integer>> copy = new HashMap<Integer, Map<Integer, Integer>>();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new HashMap<Integer, Integer>(entry.getValue()));
        }
        return copy;
    }

    void publish(Map<Integer, Map<Integer, Integer>> candidate) throws IllegalAccessException {
        // One assignment, after validating all rules. EE2 reads the public field on lookup.
        values.set(null, candidate);
    }
}
