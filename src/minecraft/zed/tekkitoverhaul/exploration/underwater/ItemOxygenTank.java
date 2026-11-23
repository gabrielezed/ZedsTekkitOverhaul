package zed.tekkitoverhaul.exploration.underwater;

import java.util.List;
import net.minecraft.src.ItemStack;

final class ItemOxygenTank extends DivingArmor {
    ItemOxygenTank(int id) { super(id, 1, "oxygen_tank"); }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, List lines) {
        lines.add("Oxygen: " + OxygenCharge.get(stack) + " / " + UnderwaterBalance.OXYGEN_CAPACITY + " ticks");
        lines.add("20 ticks = 1 second");
    }
}
