package zed.tekkitoverhaul.exploration.underwater;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;

final class UnderwaterHooks {
    private static final ToIntBiFunction<Object, Integer> BREATH = new ToIntBiFunction<Object, Integer>() {
        @Override
        public int applyAsInt(Object entity, Integer air) { return holdAir(entity, air); }
    };
    private static final Predicate<Object> SWIM = new Predicate<Object>() {
        @Override
        public boolean test(Object entity) {
            if (!(entity instanceof EntityPlayer)) return false;
            EntityPlayer player = (EntityPlayer) entity;
            return !player.worldObj.isRemote && wears(player, 0, UnderwaterRegistry.flippers);
        }
    };
    private static final Predicate<Object> FOG = new Predicate<Object>() {
        @Override
        public boolean test(Object camera) {
            Minecraft client = ModLoader.getMinecraftInstance();
            return client.thePlayer != null && camera == client.thePlayer
                    && wears(client.thePlayer, 3, UnderwaterRegistry.mask);
        }
    };

    private UnderwaterHooks() {}

    static void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            agent.getMethod("registerUnderwater", ToIntBiFunction.class, Predicate.class, Predicate.class, float.class, float.class)
                    .invoke(null, BREATH, SWIM, FOG, UnderwaterBalance.SWIM_THRUST, UnderwaterBalance.FOG_DISTANCE);
            agent.getMethod("registerMaskLightmap", BiConsumer.class).invoke(null, MaskLightmap.CALLBACK);
        } catch (ReflectiveOperationException unavailable) {
            ModLoader.getLogger().warning("[ZTO] Underwater equipment effects require the matching ZTOAgent: " + unavailable);
        }
    }

    static boolean wears(EntityPlayer player, int slot, Item item) {
        ItemStack stack = player.inventory.armorItemInSlot(slot);
        return stack != null && stack.getItem() == item;
    }

    // Called only inside the native water-air depletion branch, after its Respiration calculation.
    static int holdAir(Object entity, int air) {
        if (!(entity instanceof EntityPlayer)) return -1;
        EntityPlayer player = (EntityPlayer) entity;
        if (player.worldObj.isRemote || player.capabilities.disableDamage || player.capabilities.isCreativeMode) return -1;
        ItemStack tank = player.inventory.armorItemInSlot(2);
        int charge = OxygenCharge.get(tank);
        if (charge == 0) return -1;
        OxygenCharge.set(tank, charge - 1);
        return Math.max(0, air);
    }
}
