package zed.tekkitoverhaul.emc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Applies staged rules to a detached copy of the EE2 data. */
final class EmcRules {
    private final Map<Integer, Map<Integer, Integer>> values;
    private final Set<Long> seen = new HashSet<Long>();
    private final Set<Integer> wholeItems = new HashSet<Integer>();

    EmcRules(Map<Integer, Map<Integer, Integer>> values) {
        this.values = values;
    }

    void defaultIfMissing(int id, int value) {
        if (get(id, 0) <= 0) put(id, 0, value);
    }

    void stage(EmcProfile.Rule rule, int id, boolean collectorFuel) {
        long key = ((long) id << 32) | (rule.meta & 0xffffffffL);
        if (!seen.add(key)) throw new IllegalArgumentException("duplicate resolved item and metadata");
        if (rule.meta == -1) wholeItems.add(id);
        if (collectorFuel && rule.action == EmcProfile.Action.DISABLE) {
            throw new IllegalArgumentException("DISABLE cannot suppress the EE2 collector fuel ladder");
        }
        if (rule.action == EmcProfile.Action.SET) put(id, rule.meta, rule.value);
        if (rule.action == EmcProfile.Action.DISABLE) {
            if (rule.meta == -1) values.remove(id);
            else if (values.containsKey(id)) values.get(id).remove(rule.meta);
        }
    }

    void validate(EmcProfile.Rule rule, int id) {
        if (rule.meta >= 0 && wholeItems.contains(id)) {
            throw new IllegalArgumentException("rule overlaps whole-item DISABLE");
        }
        if (rule.action == EmcProfile.Action.DISABLE && rule.meta > 0 && get(id, 0) != 0) {
            throw new IllegalArgumentException("metadata 0 still supplies fallback EMC; disable the whole item or remove its base");
        }
    }

    private int get(int id, int meta) {
        Map<Integer, Integer> variants = values.get(id);
        Integer value = variants == null ? null : variants.get(meta);
        return value == null ? 0 : value;
    }

    private void put(int id, int meta, int value) {
        Map<Integer, Integer> variants = values.get(id);
        if (variants == null) {
            variants = new HashMap<Integer, Integer>();
            values.put(id, variants);
        }
        variants.put(meta, value);
    }
}
