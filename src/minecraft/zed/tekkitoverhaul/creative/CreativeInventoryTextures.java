package zed.tekkitoverhaul.creative;

final class CreativeInventoryTextures {
    private static final String ROOT = "/zed/creative-1.4.7/";
    static final String TABS = ROOT + "allitems.png";
    static final String LIST = ROOT + "list_items.png";
    static final String SEARCH = ROOT + "search.png";
    static final String INVENTORY = ROOT + "survival_inv.png";

    private CreativeInventoryTextures() { }

    static boolean available() {
        for (String path : new String[] {TABS, LIST, SEARCH, INVENTORY}) {
            if (CreativeInventoryTextures.class.getResource(path) == null) return false;
        }
        return true;
    }
}
