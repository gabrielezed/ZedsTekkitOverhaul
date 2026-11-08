package zed.tekkitoverhaul.creative;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.src.Block;
import net.minecraft.src.Container;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;

final class CreativeCatalog {
    final CreativeCategories.Tab[] tabs = CreativeCategories.tabs();

    static List<?> readLegacyEntries(Container container) throws ReflectiveOperationException {
        // MCP's ContainerCreative is package-private. Its single public List is itemList (os.a).
        for (Field field : container.getClass().getFields()) {
            if (field.getDeclaringClass() == container.getClass() && field.getType() == List.class) {
                field.setAccessible(true);
                return (List<?>) field.get(container);
            }
        }
        throw new NoSuchFieldException("ContainerCreative.itemList");
    }

    CreativeCatalog(List<?> legacyEntries) {
        Map<Integer, List<Entry>> seen = new HashMap<Integer, List<Entry>>();
        addEntries(legacyEntries, seen);
        // Forge's default Block.addCreativeItems is empty. Include our registered machine blocks.
        for (Block block : Block.blocksList) {
            if (CreativeCategories.isZto(block) && !block.blockMaterial.isLiquid()) {
                addEntries(Collections.singletonList(new ItemStack(block)), seen);
            }
        }
        addEntries(readNeiEntries(), seen);
    }

    private static List<?> readNeiEntries() {
        try {
            // NEI 1.2.2.4 populates this unfiltered catalog when the legacy screen initializes.
            Class<?> controller = Class.forName("codechicken.nei.NEIController");
            return (List<?>) controller.getField("items").get(null);
        } catch (ClassNotFoundException absent) {
            return Collections.emptyList();
        } catch (ReflectiveOperationException incompatible) {
            ModLoader.getLogger().fine("[ZTO] NEI creative catalog unavailable: " + incompatible);
            return Collections.emptyList();
        }
    }

    private void addEntries(List<?> candidates, Map<Integer, List<Entry>> seen) {
        for (Object candidate : candidates) {
            if (!(candidate instanceof ItemStack)) continue;
            ItemStack stack = (ItemStack) candidate;
            if (stack.itemID <= 0 || stack.itemID >= Item.itemsList.length || stack.getItem() == null) continue;
            List<Entry> variants = seen.get(stack.itemID);
            if (variants == null) {
                variants = new ArrayList<Entry>();
                seen.put(stack.itemID, variants);
            }
            if (contains(variants, stack)) continue;

            Entry entry = new Entry(stack);
            variants.add(entry);
            tabs[CreativeCategories.SEARCH].entries.add(entry);
            for (CreativeCategories.Tab tab : tabs) {
                if (tab != null && tab.filter != null && tab.filter.includes(stack)) tab.entries.add(entry);
            }
        }
    }

    private static boolean contains(List<Entry> variants, ItemStack stack) {
        for (Entry entry : variants) {
            if (entry.stack.isItemEqual(stack) && ItemStack.func_46154_a(entry.stack, stack)) return true;
        }
        return false;
    }

    List<Entry> entries(int tab, String query) {
        List<Entry> source = tabs[tab].entries;
        String term = query.trim().toLowerCase(Locale.ROOT);
        if (tab != CreativeCategories.SEARCH || term.isEmpty()) return source;

        List<Entry> results = new ArrayList<Entry>();
        for (Entry entry : source) {
            if (entry.searchText.contains(term)) results.add(entry);
        }
        return results;
    }

    static final class Entry {
        final ItemStack stack;
        final String searchText;

        Entry(ItemStack source) {
            stack = source.copy();
            stack.stackSize = 1;
            String name = "";
            try {
                name = stack.getItem().getItemDisplayName(stack);
            } catch (RuntimeException failure) {
                ModLoader.getLogger().fine("[ZTO] Creative name unavailable for " + stack.itemID + ": " + failure);
            }
            searchText = (name + " " + stack.itemID + ":" + stack.getItemDamage()).toLowerCase(Locale.ROOT);
        }
    }
}
