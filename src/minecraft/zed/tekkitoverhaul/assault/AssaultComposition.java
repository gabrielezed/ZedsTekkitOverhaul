package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.assault.AssaultWave.Profile;

/** The repeating vanilla slot order for a wave of the natural full-moon assault. */
enum AssaultComposition {
    LEGACY_EARLY("ZZS"),
    LEGACY_FINAL("SZ"),
    QUIET_MELEE("ZZZS"),
    ELEVATED_OPENING("SZ"),
    HARD_RANGED("SSZ"),
    HARD_QUIET_OPENING("ZSS"),
    HARD_ELEVATED_OPENING("SZS");

    private final String order;

    AssaultComposition(String order) {
        this.order = order;
    }

    static AssaultComposition select(Profile profile, int difficulty, int wave) {
        if (difficulty == 3) {
            if (wave == 1 && profile == Profile.QUIET) return HARD_QUIET_OPENING;
            if (wave == 1 && profile == Profile.ELEVATED) return HARD_ELEVATED_OPENING;
            return HARD_RANGED;
        }
        if (profile == Profile.QUIET) return QUIET_MELEE;
        if (profile == Profile.ELEVATED && wave == 1) return ELEVATED_OPENING;
        return wave == AssaultWave.COUNT ? LEGACY_FINAL : LEGACY_EARLY;
    }

    boolean skeleton(int slot) {
        return order.charAt(slot % order.length()) == 'S';
    }
}
