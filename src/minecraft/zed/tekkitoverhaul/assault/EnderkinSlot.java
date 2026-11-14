package zed.tekkitoverhaul.assault;

import java.util.LinkedHashMap;
import java.util.Map;

/** One optional substitution at the start of an already allocated wave or patrol. */
final class EnderkinSlot {
    private static final int MAX_ATTEMPTS = 4;
    final Map<String, Integer> rejections = new LinkedHashMap<String, Integer>();
    EnderkinRole selected;
    int attempts;
    boolean inserted;

    void reset(EnderkinRole role) {
        selected = role;
        attempts = 0;
        inserted = false;
        rejections.clear();
    }

    EnderkinRole pending() {
        return !inserted && attempts < MAX_ATTEMPTS ? selected : null;
    }

    void record(String rejection) {
        attempts++;
        inserted = rejection == null;
        if (!inserted) {
            Integer count = rejections.get(rejection);
            rejections.put(rejection, count == null ? 1 : count + 1);
        }
    }
}
