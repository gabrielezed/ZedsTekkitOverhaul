package zed.tekkitoverhaul.power;

import java.lang.reflect.Method;
import java.util.logging.Level;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.config.ZtoConfig;

/** Optional registration boundary for the EE2 to IC2 converter. */
public final class AlchemicalPowerRegistry {
    private AlchemicalPowerRegistry() { }

    public static void register(ZtoConfig config) {
        Class<?> ic2Items;
        try {
            ClassLoader loader = AlchemicalPowerRegistry.class.getClassLoader();
            Class.forName("ee.EEBase", false, loader);
            Class.forName("ee.IEEPowerNet", false, loader);
            Class.forName("ic2.api.IEnergySource", false, loader);
            ic2Items = Class.forName("ic2.api.Items", false, loader);
        } catch (ClassNotFoundException absent) {
            return;
        } catch (LinkageError incompatible) {
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] Alchemical power dependencies unavailable", incompatible);
            return;
        }

        try {
            Method getItem = ic2Items.getMethod("getItem", String.class);
            ItemStack machine = (ItemStack) getItem.invoke(null, "machine");
            ItemStack circuit = (ItemStack) getItem.invoke(null, "advancedCircuit");
            ItemStack cable = (ItemStack) getItem.invoke(null, "insulatedCopperCableItem");
            if (machine == null || circuit == null || cable == null) {
                ModLoader.getLogger().warning("[ZTO] Alchemical converter requires the IC2 machine, advanced circuit and copper cable");
                return;
            }
            registerBlock(config.alchemicalConverter, machine, circuit, cable);
        } catch (ReflectiveOperationException failure) {
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] Alchemical converter recipe lookup failed", failure);
        }
    }

    private static void registerBlock(int id, ItemStack machine, ItemStack circuit, ItemStack cable) {
        if (id < 1 || id >= Block.blocksList.length || Block.blocksList[id] != null || Item.itemsList[id] != null) {
            throw new IllegalStateException("ZTO: Invalid or occupied alchemical converter block ID " + id);
        }

        Block block = new BlockAlchemicalConverter(id);
        ModLoader.registerBlock(block);
        ModLoader.registerTileEntity(TileAlchemicalConverter.class, "ZTOAlchemicalConverter");
        ModLoader.addName(block, "Alchemical Converter");
        ModLoader.addRecipe(new ItemStack(block), "GGG", "CMC", " W ",
                'G', Block.glowStone, 'C', circuit, 'M', machine, 'W', cable);
        ModLoader.getLogger().info("[ZTO] Alchemical Converter ID=" + id);
    }
}
