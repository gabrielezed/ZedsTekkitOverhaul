package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.config.ZtoConfig;

public final class UnderwaterRegistry {
    static final String TEXTURES = "/zed/textures/underwater/";
    static DivingArmor mask, flippers;

    private UnderwaterRegistry() {}

    public static void register(ZtoConfig config) {
        int block = config.airCompressor;
        if (block >= Block.blocksList.length || Block.blocksList[block] != null || Item.itemsList[block] != null) {
            throw new IllegalStateException("ZTO Air Compressor block ID collision: " + block);
        }
        int[] items = {config.underwaterMask, config.oxygenTank, config.flippers};
        for (int i = 0; i < items.length; i++) {
            int id = items[i] + 256;
            if (id == block || Item.itemsList[id] != null
                    || (id < Block.blocksList.length && Block.blocksList[id] != null)) {
                throw new IllegalStateException("ZTO underwater item stack ID collision: " + id);
            }
            for (int previous = 0; previous < i; previous++) {
                if (items[previous] == items[i]) {
                    throw new IllegalStateException("ZTO underwater item stack ID reused: " + id);
                }
            }
        }

        mask = new DivingArmor(config.underwaterMask, 0, "mask");
        Item tank = new ItemOxygenTank(config.oxygenTank);
        flippers = new DivingArmor(config.flippers, 3, "flippers");
        Block machine = new BlockAirCompressor(block);
        ModLoader.registerBlock(machine);
        ModLoader.registerTileEntity(TileAirCompressor.class, "ZTOAirCompressor");
        ModLoader.addName(mask, "Underwater Mask");
        ModLoader.addName(tank, "Oxygen Tank");
        ModLoader.addName(flippers, "Flippers");
        ModLoader.addName(machine, "Air Compressor");
        UnderwaterRecipes.register(mask, tank, flippers, machine);
        UnderwaterHooks.register();
    }

    static int sprite(String atlas, String name) {
        String path = TEXTURES + name + ".png";
        if (UnderwaterRegistry.class.getResource(path) == null) {
            throw new IllegalStateException("Missing underwater texture " + path);
        }
        return ModLoader.addOverride(atlas, path);
    }
}
