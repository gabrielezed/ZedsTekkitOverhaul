package zed.tekkitoverhaul.equipment;

import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.Item;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagByte;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.NBTTagShort;

/** Entity-owned equipment for the authoritative local world. Call on the game thread. */
public final class MobEquipment {
    public enum Slot {
        MAIN_HAND, HEAD, CHEST, LEGS, FEET
    }

    private static final String KEY = "zed.tekkitoverhaul.equipment";
    private static final String NO_DROP = "NoDrop";
    private static final int VERSION = 1;

    private MobEquipment() {}

    public static boolean supports(EntityLiving mob) {
        return mob != null && mob.worldObj != null && !mob.worldObj.isRemote
                && (mob.getClass() == EntityZombie.class || mob.getClass() == EntitySkeleton.class);
    }

    /** Assigns one stack, copying its metadata and tags. A successful assignment replaces the old slot. */
    public static boolean assign(EntityLiving mob, Slot slot, ItemStack stack) {
        return assign(mob, slot, stack, false);
    }

    /** Assigns encounter equipment with a drop policy persisted alongside that stack. */
    public static boolean assignWithoutDrop(EntityLiving mob, Slot slot, ItemStack stack) {
        return assign(mob, slot, stack, true);
    }

    private static boolean assign(EntityLiving mob, Slot slot, ItemStack stack, boolean noDrop) {
        if (!supports(mob) || !mob.isEntityAlive() || !valid(mob, slot, stack)) return false;
        NBTTagCompound data = mob.getEntityData();
        NBTTagCompound slots = slots(data);
        if (slots == null) {
            if (data.hasKey(KEY)) return false; // Leave unknown formats intact.
            slots = new NBTTagCompound();
            slots.setInteger("Version", VERSION);
            data.setCompoundTag(KEY, slots);
        }
        NBTTagCompound tag = stack.copy().writeToNBT(new NBTTagCompound());
        if (noDrop) tag.setBoolean(NO_DROP, true);
        slots.setCompoundTag(slot.name(), tag);
        return true;
    }

    static boolean dropsOnDeath(EntityLiving mob, Slot slot) {
        NBTTagCompound slots = slots(mob);
        if (slots == null || !(slots.getTag(slot.name()) instanceof NBTTagCompound)) return true;
        NBTTagCompound stack = slots.getCompoundTag(slot.name());
        return !(stack.getTag(NO_DROP) instanceof NBTTagByte) || !stack.getBoolean(NO_DROP);
    }

    /** Returns a detached snapshot of owned equipment. The skeleton's native bow is implicit. */
    public static ItemStack get(EntityLiving mob, Slot slot) {
        if (slot == null) return null;
        NBTTagCompound slots = slots(mob);
        if (slots == null || !(slots.getTag(slot.name()) instanceof NBTTagCompound)) return null;
        NBTTagCompound tag = slots.getCompoundTag(slot.name());
        if (!(tag.getTag("id") instanceof NBTTagShort) || !(tag.getTag("Count") instanceof NBTTagByte)
                || !(tag.getTag("Damage") instanceof NBTTagShort)
                || (tag.hasKey("tag") && !(tag.getTag("tag") instanceof NBTTagCompound))) return null;
        int id = tag.getShort("id");
        if (id < 0 || id >= Item.itemsList.length) return null;
        ItemStack stack = ItemStack.loadItemStackFromNBT((NBTTagCompound) tag.copy());
        return valid(mob, slot, stack) ? stack : null;
    }

    /** Clears the slot and returns the detached stack, if one was present. */
    public static ItemStack remove(EntityLiving mob, Slot slot) {
        ItemStack previous = get(mob, slot);
        if (previous != null) {
            NBTTagCompound data = mob.getEntityData();
            NBTTagCompound slots = slots(data);
            slots.getTags().remove(slots.getTag(slot.name()));
            if (slots.getTags().size() == 1) data.getTags().remove(slots);
        }
        return previous;
    }

    static boolean hasEquipment(EntityLiving mob) {
        return slots(mob) != null;
    }

    private static NBTTagCompound slots(EntityLiving mob) {
        return supports(mob) ? slots(mob.getEntityData()) : null;
    }

    private static NBTTagCompound slots(NBTTagCompound data) {
        NBTBase tag = data.getTag(KEY);
        if (!(tag instanceof NBTTagCompound)) return null;
        NBTTagCompound slots = (NBTTagCompound) tag;
        return slots.getTag("Version") instanceof NBTTagInt && slots.getInteger("Version") == VERSION
                ? slots : null;
    }

    private static boolean valid(EntityLiving mob, Slot slot, ItemStack stack) {
        if (slot == null || stack == null || stack.stackSize != 1
                || stack.itemID < 0 || stack.itemID >= Item.itemsList.length) return false;
        Item item = stack.getItem();
        if (item == null || stack.getItemDamage() < 0 || stack.getItemDamage() > item.getMaxDamage()) return false;
        if (slot == Slot.MAIN_HAND) {
            if (mob.getClass() == EntitySkeleton.class) return item == Item.bow;
            return item == Item.swordStone || item == Item.swordSteel;
        }
        // The twenty stock armor IDs form a contiguous range in 1.2.5.
        return item instanceof ItemArmor && stack.itemID >= Item.helmetLeather.shiftedIndex
                && stack.itemID <= Item.bootsGold.shiftedIndex && ((ItemArmor) item).armorType == slot.ordinal() - 1;
    }
}
