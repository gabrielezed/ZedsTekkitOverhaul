package zed.tekkitoverhaul.exploration.motorboat;

import net.minecraft.src.Block;
import net.minecraft.src.EntityList;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;

public final class MotorboatRegistry {
    public static final String ENTITY_NAME = "ZTO.Motorboat";
    private static Item motorboat;
    private static Item outboard;

    private MotorboatRegistry() {}

    public static void register(ZtoConfig config) {
        checkItemId(config.outboardMotor);
        checkItemId(config.motorboat);
        if (config.outboardMotor == config.motorboat) {
            throw new IllegalStateException("ZTO Outboard Motor and Motorboat need distinct item IDs.");
        }
        if (EntityList.getStringFromID(config.motorboatEntity) != null) {
            throw new IllegalStateException("ZTO Motorboat entity ID collision: " + config.motorboatEntity);
        }
        ModLoader.registerEntityID(EntityMotorboat.class, ENTITY_NAME, config.motorboatEntity);
        outboard = new Item(config.outboardMotor) {}.setItemName("ztoOutboardMotor");
        motorboat = new ItemMotorboat(config.motorboat);
        ModLoader.addName(outboard, "Outboard Motor");
        ModLoader.addName(motorboat, "Motorboat");
        ModLoader.addRecipe(new ItemStack(motorboat), "B", "M", 'B', Item.boat, 'M', outboard);
        registerOutboardRecipe();
    }

    private static void checkItemId(int id) {
        int stackId = id + 256;
        if (Item.itemsList[stackId] != null
                || (stackId < Block.blocksList.length && Block.blocksList[stackId] != null)) {
            throw new IllegalStateException("ZTO Motorboat item stack ID collision: " + stackId);
        }
    }

    private static void registerOutboardRecipe() {
        try {
            ItemStack iron = (ItemStack) Class.forName("ic2.api.Items").getMethod("getItem", String.class)
                    .invoke(null, "refinedIronIngot");
            Item gear = (Item) Class.forName("BuildCraftCore").getField("ironGearItem").get(null);
            Block engine = (Block) Class.forName("BuildCraftEnergy").getField("engineBlock").get(null);
            if (iron == null || iron.getItem() == null || gear == null || engine == null) {
                throw new IllegalStateException("An Outboard Motor ingredient was not registered.");
            }
            ModLoader.addRecipe(new ItemStack(outboard), "RGR", "RER", " G ",
                    'R', iron.copy(), 'G', new ItemStack(gear), 'E', new ItemStack(engine, 1, 2));
        } catch (ReflectiveOperationException failure) {
            // The deobfuscated development profile does not install production mods.
            ModLoader.getLogger().warning("[ZTO] Outboard Motor recipe requires IC2 1.97 and BuildCraft 2.2.14: " + failure);
        }
    }

    public static Item motorboatItem() { return motorboat; }
    public static Item outboardItem() { return outboard; }

    static boolean isGasoline(ItemStack stack) {
        return stack != null && stack.getItem() != null
                && stack.getItem() == DistillationRegistry.bucketFor(DistillateType.GASOLINE.id)
                && stack.getItemDamage() == 0 && stack.stackSize > 0;
    }
}
