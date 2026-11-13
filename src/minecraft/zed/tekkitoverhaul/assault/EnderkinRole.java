package zed.tekkitoverhaul.assault;

import java.util.Random;
import zed.tekkitoverhaul.lunar.LunarPhase;

/** Independent rolls preserve the ordinary encounter's random streams. */
enum EnderkinRole {
    SKIRMISHER, BREACHER;

    static EnderkinRole select(boolean enabled, long seed, long day, LunarPhase phase, int wave) {
        if (!enabled) return null;
        if (phase == LunarPhase.WAXING_GIBBOUS && wave == 0)
            return roll(seed, day, 0) < 25 ? SKIRMISHER : null;
        if (phase == LunarPhase.FULL_MOON && wave == 2)
            return roll(seed, day, 2) < 50 ? SKIRMISHER : null;
        if (phase == LunarPhase.FULL_MOON && wave == 3)
            return roll(seed, day, 3) < 40 ? BREACHER : null;
        return null;
    }

    private static int roll(long seed, long day, int wave) {
        return new Random(seed ^ (day * 0x9E3779B97F4A7C15L) ^ (wave * 0xC2B2AE3D27D4EB4FL)).nextInt(100);
    }
}
