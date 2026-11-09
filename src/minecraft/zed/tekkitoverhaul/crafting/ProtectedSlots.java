package zed.tekkitoverhaul.crafting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.BaseMod;
import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Slot;

/** Uses the active Inventory Tweaks 1.41b rules. Unknown protection state blocks bulk moves. */
final class ProtectedSlots {
    private static final int ROW_LOCK_PRIORITY = 2999999;

    static List<Slot> playerSlots(Container container, EntityPlayer player, int start, boolean hotbar)
            throws ReflectiveOperationException {
        return playerSlots(container, player, start, hotbar, null);
    }

    static List<Slot> playerSlots(Container container, EntityPlayer player, int start, boolean hotbar,
                                  List<Slot> recipeOnly)
            throws ReflectiveOperationException {
        int[] locks = new int[36];
        boolean[] frozen = new boolean[36];
        for (BaseMod mod : ModLoader.getLoadedMods()) {
            if (!mod.getClass().getName().equals("mod_InvTweaks")) continue;
            Object instance = field(mod, "instance");
            Object manager = field(instance, "cfgManager");
            Object config = manager.getClass().getMethod("getConfig").invoke(manager);
            if (config == null) throw new IllegalStateException("Inventory Tweaks rules are unavailable");
            locks = (int[]) config.getClass().getMethod("getLockPriorities").invoke(config);
            frozen = (boolean[]) config.getClass().getMethod("getFrozenSlots").invoke(config);
        }
        if (locks.length != 36 || frozen.length != 36 || container.inventorySlots.size() != start + 36) {
            throw new IllegalStateException("Unsupported player inventory layout");
        }
        InventoryPlayer inventory = player.inventory;
        List<Slot> result = new ArrayList<Slot>();
        for (int index = 0; index < 36; index++) {
            Slot slot = container.getSlot(start + index);
            if (slot.inventory != inventory || slot.getClass() != Slot.class) {
                throw new IllegalStateException("Unexpected player inventory slot");
            }
            if ((hotbar || index < 27) && locks[index] == 0 && !frozen[index]) result.add(slot);
            if (recipeOnly != null && isRecipeOnlySource(index, locks[index], frozen[index])) recipeOnly.add(slot);
        }
        return result;
    }

    static boolean isRecipeOnlySource(int index, int lockPriority, boolean frozen) {
        // The stock D LOCKED rule keeps the hotbar in place during sorting.
        return index >= 27 && index < 36 && lockPriority == ROW_LOCK_PRIORITY && !frozen;
    }

    private static Object field(Object instance, String name) throws ReflectiveOperationException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }
}
