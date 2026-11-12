package zed.tekkitoverhaul.equipment;

import java.util.ArrayList;
import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.forge.adaptors.EntityLivingHandlerAdaptor;
import zed.tekkitoverhaul.equipment.MobEquipment.Slot;

public final class MobEquipmentHooks extends EntityLivingHandlerAdaptor {
    private static final Slot[] SLOTS = Slot.values();

    @Override
    public int onEntityLivingHurt(EntityLiving victim, DamageSource source, int damage) {
        if (victim.worldObj.isRemote || damage <= 0) return damage;

        Entity attacker = source.getEntity();
        if (attacker != null && attacker.getClass() == EntityZombie.class
                && attacker.worldObj == victim.worldObj && "mob".equals(source.getDamageType())) {
            ItemStack sword = MobEquipment.get((EntityLiving) attacker, Slot.MAIN_HAND);
            if (sword != null) damage += sword.getDamageVsEntity(victim) - 4;
        }

        if (source.isUnblockable() || !MobEquipment.hasEquipment(victim)) return damage;
        int armor = 0;
        for (Slot slot : SLOTS) {
            if (slot == Slot.MAIN_HAND) continue;
            ItemStack piece = MobEquipment.get(victim, slot);
            if (piece != null) armor += ((ItemArmor) piece.getItem()).damageReduceAmount;
        }
        // This hook runs before the entity's native armor and potion reductions.
        return damage - (int) ((long) damage * armor / 25);
    }

    @Override
    public void onEntityLivingDrops(EntityLiving mob, DamageSource source, ArrayList<EntityItem> drops,
                                    int looting, boolean recentlyHit, int specialDrop) {
        if (!MobEquipment.hasEquipment(mob)) return;
        for (Slot slot : SLOTS) {
            boolean dropsOnDeath = MobEquipment.dropsOnDeath(mob, slot);
            ItemStack stack = MobEquipment.remove(mob, slot);
            if (stack == null || !dropsOnDeath) continue;
            EntityItem drop = new EntityItem(mob.worldObj, mob.posX, mob.posY, mob.posZ, stack);
            drop.delayBeforeCanPickup = 10;
            drops.add(drop);
        }
    }
}
