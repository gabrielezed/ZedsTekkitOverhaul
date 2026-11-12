package zed.tekkitoverhaul.assault;

import java.util.Random;
import zed.tekkitoverhaul.assault.LoadoutPolicy.Profile;
import zed.tekkitoverhaul.lunar.LunarPhase;

public final class LoadoutPolicySelfTest {
    public static void main(String[] args) {
        for (LunarPhase phase : LunarPhase.values()) {
            for (boolean skeleton : new boolean[] {false, true}) {
                int waveCount = phase == LunarPhase.FULL_MOON ? 3 : 1;
                for (int wave = 1; wave <= waveCount; wave++) {
                    int[] counts = new int[Profile.values().length];
                    for (int roll = 0; roll < 100; roll++) {
                        Profile profile = LoadoutPolicy.select(phase, wave, skeleton, 0, 0, roll);
                        counts[profile.ordinal()]++;
                        check(LoadoutPolicy.select(phase, wave, skeleton, LoadoutPolicy.cap(phase), 0, roll)
                                == Profile.VANILLA, "Encounter cap exceeded");
                    }
                    int protectedCount = phase == LunarPhase.WAXING_GIBBOUS ? (skeleton ? 30 : 10)
                            : phase == LunarPhase.FULL_MOON ? (skeleton ? 5 + 15 * wave : 10 * (wave - 1)) : 0;
                    int light = skeleton ? 0 : phase == LunarPhase.FIRST_QUARTER || phase == LunarPhase.WAXING_GIBBOUS
                            ? (phase == LunarPhase.FIRST_QUARTER ? 15 : 20)
                            : phase == LunarPhase.FULL_MOON ? 15 + 5 * wave : 0;
                    check(counts[Profile.LIGHT_ZOMBIE.ordinal()] == light, "Light profile weight");
                    check(counts[(skeleton ? Profile.PROTECTED_ZOMBIE : Profile.PROTECTED_SKELETON).ordinal()] == 0,
                            "Profile crossed species");
                    check(counts[(skeleton ? Profile.PROTECTED_SKELETON : Profile.PROTECTED_ZOMBIE).ordinal()]
                            == protectedCount, "Protected profile weight");
                }
            }
        }
        Random random = new Random(5L);
        int sawCap = 0;
        for (int encounter = 0; encounter < 10000; encounter++) {
            int equipped = 0;
            for (int wave = 1; wave <= 3; wave++) {
                int waveEquipped = 0;
                for (int mob = 0; mob < wave + 3; mob++) {
                    if (LoadoutPolicy.select(LunarPhase.FULL_MOON, wave, mob % 2 == 1,
                            equipped, waveEquipped, random.nextInt(100)) != Profile.VANILLA) {
                        equipped++;
                        waveEquipped++;
                    }
                }
                check(waveEquipped <= (wave == 1 ? 1 : 2), "Wave cap exceeded");
            }
            check(equipped <= 5, "Assault cap exceeded");
            if (equipped == 5) sawCap++;
        }
        check(sawCap > 0 && sawCap < 10000, "Expected variation across assaults");
        System.out.println("Loadout weights and caps passed across all rolls and 10,000 fixed-seed assaults");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
