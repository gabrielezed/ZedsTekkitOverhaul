package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.lunar.LunarPhase;

/** Percent chances per successfully inserted mob, subject to encounter and wave caps. */
final class LoadoutPolicy {
    enum Profile { VANILLA, LIGHT_ZOMBIE, PROTECTED_ZOMBIE, PROTECTED_SKELETON }

    private static final int[] ASSAULT_CHANCES = {20, 35, 50};
    private static final int[] ASSAULT_WAVE_CAPS = {1, 2, 2};

    private LoadoutPolicy() { }

    static int cap(LunarPhase phase) {
        if (phase == LunarPhase.FULL_MOON) return 5;
        return phase == LunarPhase.FIRST_QUARTER || phase == LunarPhase.WAXING_GIBBOUS ? 1 : 0;
    }

    static int assaultWaveCap(int wave) {
        return ASSAULT_WAVE_CAPS[wave - 1];
    }

    static Profile select(LunarPhase phase, int wave, boolean skeleton, int equipped, int waveEquipped, int roll) {
        if (equipped >= cap(phase)) return Profile.VANILLA;
        switch (phase) {
            case FIRST_QUARTER:
                return !skeleton && roll < 15 ? Profile.LIGHT_ZOMBIE : Profile.VANILLA;
            case WAXING_GIBBOUS:
                if (roll >= 30) return Profile.VANILLA;
                if (skeleton) return Profile.PROTECTED_SKELETON;
                return roll < 10 ? Profile.PROTECTED_ZOMBIE : Profile.LIGHT_ZOMBIE;
            case FULL_MOON:
                int index = wave - 1;
                if (waveEquipped >= assaultWaveCap(wave) || roll >= ASSAULT_CHANCES[index]) {
                    return Profile.VANILLA;
                }
                if (skeleton) return Profile.PROTECTED_SKELETON;
                return roll < index * 10 ? Profile.PROTECTED_ZOMBIE : Profile.LIGHT_ZOMBIE;
            default:
                return Profile.VANILLA;
        }
    }
}
