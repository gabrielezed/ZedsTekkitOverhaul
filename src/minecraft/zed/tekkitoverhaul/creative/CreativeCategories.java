package zed.tekkitoverhaul.creative;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;

/** Category registration for client mods during load or modsLoaded. */
public final class CreativeCategories {
    public interface Filter {
        boolean includes(ItemStack stack);
    }

    static final int SEARCH = 5;
    static final int CLASSIC = 10;
    static final int INVENTORY = 11;
    private static final int[] EXTRA_TABS = {3, 4, 6, 7, 8, 9};
    private static final Map<String, Tab> EXTRAS = new TreeMap<String, Tab>();

    private CreativeCategories() { }

    public static void register(String key, String title, ItemStack icon, Filter filter) {
        if (key == null || title == null || icon == null || filter == null
                || EXTRAS.containsKey(key) || EXTRAS.size() == EXTRA_TABS.length) {
            throw new IllegalArgumentException("Invalid, duplicate or excess creative category");
        }
        EXTRAS.put(key, new Tab(title, icon.copy(), filter));
    }

    static Tab[] tabs() {
        Tab[] tabs = new Tab[12];
        tabs[0] = new Tab("Building Blocks", new ItemStack(Block.brick), new Filter() {
            public boolean includes(ItemStack stack) { return stack.getItem() instanceof ItemBlock; }
        });
        tabs[1] = new Tab("Items", new ItemStack(Item.swordGold), new Filter() {
            public boolean includes(ItemStack stack) { return !(stack.getItem() instanceof ItemBlock); }
        });
        tabs[2] = new Tab("ZedsTekkitOverhaul", new ItemStack(AdvancedCraftingRegistry.advancedTable), new Filter() {
            public boolean includes(ItemStack stack) {
                return isZto(stack.getItem() instanceof ItemBlock && stack.itemID < Block.blocksList.length
                        ? Block.blocksList[stack.itemID] : stack.getItem());
            }
        });
        tabs[SEARCH] = new Tab("Search", new ItemStack(Item.compass), null);
        tabs[INVENTORY] = new Tab("Survival Inventory", new ItemStack(Block.chest), null);
        int index = 0;
        for (Tab tab : EXTRAS.values()) tabs[EXTRA_TABS[index++]] = tab;
        return tabs;
    }

    static boolean isZto(Object registered) {
        return registered != null && registered.getClass().getName().startsWith("zed.tekkitoverhaul.");
    }

    static final class Tab {
        final String title;
        final ItemStack icon;
        final Filter filter;
        final List<CreativeCatalog.Entry> entries = new ArrayList<CreativeCatalog.Entry>();

        Tab(String title, ItemStack icon, Filter filter) {
            this.title = title;
            this.icon = icon;
            this.filter = filter;
        }
    }
}
