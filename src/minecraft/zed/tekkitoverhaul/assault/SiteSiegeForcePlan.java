package zed.tekkitoverhaul.assault;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import zed.tekkitoverhaul.territory.ThreatAssessment;

/** Frozen admission and roster. A slot has at most one successful insertion. */
final class SiteSiegeForcePlan {
    enum Role { ZOMBIE, SKELETON, SKIRMISHER, BREACHER, TANK, THIEF, SABOTEUR }

    static final int RETRY_INTERVAL = 8, ATTEMPTS_PER_SLOT = 9, FINAL_COMBAT_TICKS = 560;
    final int siteId, difficulty, total, activeCap, deadline, structuralBudget;
    final ThreatAssessment threat;
    final SiteSiege.Intensity intensity;
    final boolean overridden;
    final SiteApproach objective;
    final Wave[] waves;

    SiteSiegeForcePlan(int siteId, int difficulty, ThreatAssessment threat, SiteSiege.Intensity requested,
                       int configuredCap, SiteApproach objective, long seed) {
        this.siteId = siteId;
        this.difficulty = difficulty;
        this.threat = threat;
        overridden = requested != SiteSiege.Intensity.NATURAL;
        intensity = overridden ? requested : classify(threat);
        this.objective = objective;
        boolean hard = difficulty >= 3;
        int tier = intensity.ordinal() - SiteSiege.Intensity.LOW.ordinal();
        int[] totals = hard ? new int[] {8, 10, 14, 18}
                : difficulty == 2 ? new int[] {6, 8, 11, 15} : new int[] {4, 5, 7, 10};
        int[] caps = hard ? new int[] {5, 6, 7, 9}
                : difficulty == 2 ? new int[] {4, 5, 6, 7} : new int[] {3, 4, 4, 5};
        total = totals[tier];
        activeCap = Math.min(configuredCap, caps[tier]);
        boolean heavy = objective.barrier != null && objective.barrier.structural();
        structuralBudget = heavy ? 2 : 0;
        boolean third = tier >= 2 || (hard && tier == 1) || heavy;
        int first = third ? (total + 1) / 3 : total / 2;
        int second = third ? (total + 2) / 3 : total - first;
        waves = third
                ? new Wave[] { new Wave(first, 0, 320), new Wave(second, 320, 690),
                               new Wave(total - first - second, 690, 1130) }
                : new Wave[] { new Wave(first, 0, 320), new Wave(second, 320, 750) };
        deadline = waves[waves.length - 1].end + FINAL_COMBAT_TICKS;
        Random random = new Random(seed ^ (siteId * 0x9e3779b97f4a7c15L));
        boolean ranged = random.nextBoolean();
        for (Wave wave : waves) {
            for (int i = 0; i < wave.roles.length; i++) {
                wave.roles[i] = ranged ? Role.SKELETON : Role.ZOMBIE;
                ranged = !ranged;
            }
        }
        if (tier >= 1 && difficulty >= 2) waves[0].roles[0] = Role.SKIRMISHER;
        if (objective.barrier != null && !heavy) waves[1].roles[0] = Role.BREACHER;
        if (heavy || (hard && tier == 3)) waves[waves.length - 1].roles[0] = Role.TANK;
        if (hard && tier == 3) waves[2].roles[1] = Role.SKIRMISHER;
        if (tier >= 1 && difficulty >= 2 && waves[1].roles.length >= 3 && random.nextInt(3) == 0)
            waves[1].roles[1] = Role.THIEF;
    }

    boolean reserves(Role role) {
        for (Wave wave : waves) for (Role slot : wave.roles) if (slot == role) return true;
        return false;
    }

    private static SiteSiege.Intensity classify(ThreatAssessment assessment) {
        int threat = assessment == null ? 0 : assessment.threat;
        if (threat >= ThreatAssessment.MAX - 5) return SiteSiege.Intensity.MAXIMUM;
        if (threat >= 24) return SiteSiege.Intensity.HIGH;
        return threat >= 16 ? SiteSiege.Intensity.MODERATE : SiteSiege.Intensity.LOW;
    }

    static final class Wave {
        final int start, end;
        final Role[] roles;
        final int[] attempts, placementFailures;
        final boolean[] inserted;
        int cursor, tried, deployed, capDeferrals;
        final Map<String, Integer> rejections = new LinkedHashMap<String, Integer>();

        Wave(int count, int start, int end) {
            roles = new Role[count];
            attempts = new int[count];
            placementFailures = new int[count];
            inserted = new boolean[count];
            this.start = start;
            this.end = end;
        }

        int nextSlot(int headroom) {
            // A temporary cap opening must give the objective or heavy reserve a deployment chance.
            if (headroom == 1) {
                for (int slot = 0; slot < roles.length; slot++) {
                    Role role = roles[slot];
                    if ((role == Role.TANK || role == Role.BREACHER) && pending(slot)) return slot;
                }
            }
            for (int i = 0; i < roles.length; i++) {
                int slot = cursor++ % roles.length;
                if (pending(slot)) return slot;
            }
            return -1;
        }

        private boolean pending(int slot) {
            return !inserted[slot] && placementFailures[slot] < ATTEMPTS_PER_SLOT;
        }

        int remaining() {
            int left = 0;
            for (int i = 0; i < roles.length; i++)
                if (pending(i)) left++;
            return left;
        }

        @Override public String toString() { return Arrays.toString(roles); }
    }
}
