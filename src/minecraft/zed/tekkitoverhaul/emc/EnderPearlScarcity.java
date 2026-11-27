package zed.tekkitoverhaul.emc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityEnderman;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.Item;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.src.forge.adaptors.EntityLivingHandlerAdaptor;

/** Native loot and EE2 exchange policy for the two vanilla progression items. */
public final class EnderPearlScarcity extends EntityLivingHandlerAdaptor {
    public static void register() {
        MinecraftForge.registerEntityLivingHandler(new EnderPearlScarcity());
    }

    @Override public void onEntityLivingDrops(EntityLiving mob, DamageSource source, ArrayList<EntityItem> drops,
                                               int looting, boolean recentlyHit, int specialDrop) {
        if (mob.getClass() != EntityEnderman.class) return;
        for (Iterator<EntityItem> it = drops.iterator(); it.hasNext();) {
            EntityItem drop = it.next();
            if (drop.item != null && drop.item.itemID == Item.enderPearl.shiftedIndex) it.remove();
        }
    }

    static boolean protectedItem(int item) {
        return item == Item.enderPearl.shiftedIndex || item == Item.eyeOfEnder.shiftedIndex;
    }

    static void protect(Map<Integer, Map<Integer, Integer>> values) {
        values.remove(Item.enderPearl.shiftedIndex);
        values.remove(Item.eyeOfEnder.shiftedIndex);
    }
}
