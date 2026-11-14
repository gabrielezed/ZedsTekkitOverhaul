package zed.tekkitoverhaul.crafting;

import net.minecraft.src.Block;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.render.TileEntityFoundryVisual;
import zed.tekkitoverhaul.render.client.RenderFoundryVisual;

/** Registers blocks and recipes without changing vanilla classes. */
public final class AdvancedCraftingRegistry {
    public static Block advancedTable;
    public static Block foundry;
    private static boolean initialized;

    private AdvancedCraftingRegistry() {}

    public static void register(ZtoConfig config) {
        if (initialized) return;
        int tableId = config.advancedCraftingTable;
        int foundryId = config.foundry;
        if (tableId == foundryId || tableId < 1 || foundryId < 1
                || tableId >= Block.blocksList.length || foundryId >= Block.blocksList.length
                || Block.blocksList[tableId] != null || Block.blocksList[foundryId] != null) {
            throw new IllegalStateException("ZTO: Block IDs are already used or out of range: "
                    + tableId + ", " + foundryId + ". Check config/ZedsTekkitOverhaul.cfg");
        }

        advancedTable = new BlockAdvancedCraftingTable(tableId);
        foundry = new BlockFoundry(foundryId);
        ModLoader.registerBlock(advancedTable);
        ModLoader.registerBlock(foundry);
        ModLoader.addName(advancedTable, "Advanced Crafting Table");
        ModLoader.addName(foundry, "Foundry");
        // This renderer is a client-only prototype; multiplayer support is not implemented.
        ModLoader.registerTileEntity(TileEntityFoundryVisual.class, "ZTOFoundryVisual", new RenderFoundryVisual());

        // Two rows of two crafting tables (four total); vanilla recipe offsets are accepted.
        ModLoader.addRecipe(new ItemStack(advancedTable, 1), new Object[] {
            "TT", "TT", Character.valueOf('T'), Block.workbench
        });

        // Dedicated 5x5 recipe: 16 smooth Stone blocks around the entire perimeter.
        AdvancedCraftingManager.register(new ItemStack(foundry, 1), new String[] {
            "SSSSS",
            "S   S",
            "S   S",
            "S   S",
            "SSSSS"
        }, Character.valueOf('S'), new ItemStack(Block.stone, 1, 0));
        initialized = true;
        System.out.println("[ZTO] Advanced Crafting Table ID=" + tableId
                + ", Foundry ID=" + foundryId + ", 5x5 recipes=1");
    }
}
