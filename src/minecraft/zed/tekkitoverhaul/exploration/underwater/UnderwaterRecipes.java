package zed.tekkitoverhaul.exploration.underwater;

import java.lang.reflect.Method;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;

final class UnderwaterRecipes {
    private UnderwaterRecipes() {}

    static void register(Item mask, Item tank, Item flippers, Block machine) {
        try {
            Method getItem = Class.forName("ic2.api.Items").getMethod("getItem", String.class);
            ItemStack rubber = ingredient(getItem, "rubber", 0);
            ItemStack cell = ingredient(getItem, "cell", 0);
            ItemStack alloy = ingredient(getItem, "advancedAlloy", 0);
            ItemStack boots = ingredient(getItem, "rubberBoots", 0);
            ItemStack carbon = ingredient(getItem, "carbonPlate", 0);
            ItemStack compressor = ingredient(getItem, "compressor", 5);
            Item pipe = (Item) Class.forName("BuildCraftTransport").getField("pipeItemsDiamond").get(null);
            if (pipe == null) throw new IllegalStateException("Missing BuildCraft Diamond Transport Pipe");
            ModLoader.addRecipe(new ItemStack(mask), " G ", "R R", " L ",
                    'G', new ItemStack(Block.thinGlass), 'R', rubber, 'L', Item.leather);
            ModLoader.addRecipe(new ItemStack(tank), "ACA", "ALA", "   ",
                    'A', alloy, 'C', cell, 'L', new ItemStack(Item.plateLeather));
            ModLoader.addRecipe(new ItemStack(flippers), "CBC", "C C", "   ",
                    'C', carbon, 'B', boots);
            ModLoader.addRecipe(new ItemStack(machine), "P P", "CMC", "C C",
                    'P', new ItemStack(pipe), 'C', cell, 'M', compressor);
        } catch (ReflectiveOperationException failure) {
            // MCP carries API stubs and has no production ingredient registry.
            ModLoader.getLogger().warning("[ZTO] Underwater recipes require IC2 1.97 and BuildCraft 2.2.14: " + failure);
        }
    }

    private static ItemStack ingredient(Method lookup, String name, int metadata) throws ReflectiveOperationException {
        ItemStack stack = (ItemStack) lookup.invoke(null, name);
        if (stack == null || stack.getItem() == null || stack.getItemDamage() != metadata) {
            throw new IllegalStateException("Missing IC2 ingredient " + name + " with metadata " + metadata);
        }
        ItemStack ingredient = stack.copy();
        ingredient.stackSize = 1;
        return ingredient;
    }
}
