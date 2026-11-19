package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class SiteSiegePolicy {
    static final long COOLDOWN = 16L * LunarSnapshot.DAY;

    private SiteSiegePolicy() { }

    static boolean opportunity(long seed, long cycle) {
        // One fixed cycle residue per save. No independent clock or random state to reroll.
        return Math.floorMod(cycle, 3L) == Math.floorMod(seed ^ (seed >>> 32), 3L);
    }

    static long cooldownUntil(long boundary) {
        return boundary > Long.MAX_VALUE - COOLDOWN ? Long.MAX_VALUE : boundary + COOLDOWN;
    }
}
