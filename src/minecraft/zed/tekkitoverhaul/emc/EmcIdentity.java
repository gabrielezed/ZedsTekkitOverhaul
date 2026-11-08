package zed.tekkitoverhaul.emc;

import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;

/** Resolves exact local registry names, or explicitly configured runtime IDs. */
final class EmcIdentity {
    private EmcIdentity() {}

    static int resolve(EmcProfile.Rule rule) {
        if (rule.identity.startsWith("id:")) {
            int id = EmcProfile.unsigned(rule.identity.substring(3), Item.itemsList.length - 1);
            if (Item.itemsList[id] != null) return id;
            throw new IllegalArgumentException("unregistered item: " + rule.identity);
        }
        String name = rule.identity.substring(5);
        int resolved = -1;
        for (int id = 0; id < Item.itemsList.length; id++) {
            Item item = Item.itemsList[id];
            if (item == null || !matches(item, id, rule.meta, name)) continue;
            if (resolved != -1) {
                throw new IllegalArgumentException("ambiguous name " + name + " at IDs " + resolved + " and " + id);
            }
            resolved = id;
        }
        if (resolved == -1) throw new IllegalArgumentException("unknown item name: " + name);
        return resolved;
    }

    private static boolean matches(Item item, int id, int meta, String name) {
        if (name.equals(item.getItemName())) return true;
        try {
            return name.equals(item.getItemNameIS(new ItemStack(id, 1, Math.max(meta, 0))));
        } catch (RuntimeException unsupportedMetadata) {
            return false;
        }
    }
}
