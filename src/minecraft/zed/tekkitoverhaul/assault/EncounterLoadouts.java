package zed.tekkitoverhaul.assault;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import zed.tekkitoverhaul.assault.LoadoutPolicy.Profile;
import zed.tekkitoverhaul.equipment.MobEquipment;
import zed.tekkitoverhaul.equipment.MobEquipment.Slot;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;

/** Records the outcome of the single assignment opportunity after each insertion. */
final class EncounterLoadouts {
    private final Random random = new Random();
    final Map<String, Integer> selected = new LinkedHashMap<String, Integer>();
    LunarPhase phase = LunarPhase.NEW_MOON;
    int equipped, failures;

    void reset(LunarPhase phase) {
        this.phase = phase;
        equipped = failures = 0;
        selected.clear();
    }

    Profile apply(EntityMob mob, int wave, int waveEquipped) {
        if (mob instanceof TacticalHostile) return Profile.VANILLA;
        Profile profile = LoadoutPolicy.select(phase, wave, mob instanceof EntitySkeleton,
                equipped, waveEquipped, random.nextInt(100));
        Integer previous = selected.get(profile.name());
        selected.put(profile.name(), previous == null ? 1 : previous + 1);
        if (profile != Profile.VANILLA) {
            if (assign(mob, profile)) equipped++;
            else failures++;
        }
        return profile;
    }

    static boolean assign(EntityMob mob, Profile profile) {
        switch (profile) {
            case LIGHT_ZOMBIE:
                return MobEquipment.assignWithoutDrop(mob, Slot.MAIN_HAND, new ItemStack(Item.swordStone));
            case PROTECTED_ZOMBIE:
                return MobEquipment.assignWithoutDrop(mob, Slot.CHEST, new ItemStack(Item.plateLeather));
            case PROTECTED_SKELETON:
                return MobEquipment.assignWithoutDrop(mob, Slot.CHEST, new ItemStack(Item.plateChain));
            default:
                return false;
        }
    }
}
