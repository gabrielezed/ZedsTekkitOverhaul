package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;

final class OxygenCharge {
    static final String KEY = "ZTO_OxygenTicks";

    private OxygenCharge() {}

    static boolean isTank(ItemStack stack) {
        return stack != null && stack.stackSize == 1 && stack.getItem() instanceof ItemOxygenTank;
    }

    static int get(ItemStack stack) {
        if (!isTank(stack) || stack.stackTagCompound == null
                || !(stack.stackTagCompound.getTag(KEY) instanceof NBTTagInt)) return 0;
        return clamp(stack.stackTagCompound.getInteger(KEY));
    }

    static void set(ItemStack stack, int ticks) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger(KEY, clamp(ticks));
    }

    private static int clamp(int ticks) {
        return Math.max(0, Math.min(ticks, UnderwaterBalance.OXYGEN_CAPACITY));
    }
}
