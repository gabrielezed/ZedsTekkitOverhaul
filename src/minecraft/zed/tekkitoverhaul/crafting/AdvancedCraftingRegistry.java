package zed.tekkitoverhaul.crafting;

import java.io.File;
import net.minecraft.src.Block;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.forge.Configuration;

/** Registrazione blocchi e ricette, senza modificare classi vanilla. */
public final class AdvancedCraftingRegistry {
    public static Block advancedTable;
    public static Block foundry;
    private static boolean initialized;

    private AdvancedCraftingRegistry() {}

    public static void register() {
        if (initialized) return;
        // La config viene salvata dopo il primo avvio. Non cambiare ID in un mondo esistente.
        Configuration config = new Configuration(new File("config/ZedsTekkitOverhaul.cfg"));
        config.load();
        int tableId = config.getOrCreateBlockIdProperty("advancedCraftingTable", 3000).getInt();
        int foundryId = config.getOrCreateBlockIdProperty("foundry", 3001).getInt();
        config.save();
        if (tableId == foundryId || tableId < 1 || foundryId < 1
                || tableId >= Block.blocksList.length || foundryId >= Block.blocksList.length
                || Block.blocksList[tableId] != null || Block.blocksList[foundryId] != null) {
            throw new IllegalStateException("ZTO: ID blocchi occupati o fuori range: "
                    + tableId + ", " + foundryId + ". Controlla config/ZedsTekkitOverhaul.cfg");
        }

        advancedTable = new BlockAdvancedCraftingTable(tableId);
        foundry = new BlockFoundry(foundryId);
        ModLoader.registerBlock(advancedTable);
        ModLoader.registerBlock(foundry);
        ModLoader.addName(advancedTable, "Advanced Crafting Table");
        ModLoader.addName(foundry, "Foundry");

        // Due righe da due crafting table (4 in tutto), accetta anche l'offset vanilla.
        ModLoader.addRecipe(new ItemStack(advancedTable, 1), new Object[] {
            "TT", "TT", Character.valueOf('T'), Block.workbench
        });

        // Ricetta esclusiva della 5x5: 16 blocchi di smooth stone, perimetro completo.
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
