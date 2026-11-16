package zed.tekkitoverhaul.assault;

import java.util.LinkedHashMap;
import java.util.Map;
import zed.tekkitoverhaul.territory.ThreatAssessment;

/** Per-wave accounting in simulation ticks. Carry-over transfers existing budget. */
final class AssaultWave {
    enum Profile {
        DISABLED, ASSESSMENT_UNAVAILABLE, QUIET, ORDINARY, ELEVATED;

        static Profile select(boolean enabled, ThreatAssessment assessment) {
            if (!enabled) return DISABLED;
            if (assessment == null) return ASSESSMENT_UNAVAILABLE;
            return forThreat(assessment.threat);
        }

        static Profile forThreat(int threat) {
            if (threat < 16) return QUIET;
            return threat < 24 ? ORDINARY : ELEVATED;
        }

        int budget(int configured) {
            return this == QUIET ? Math.max(1, configured - 1) : configured;
        }
    }

    final EnderkinSlot special = new EnderkinSlot();
    int equipped;
    static final int COUNT = 3;
    static final int FIRST_START = 100;
    static final int WINDOW = 280;
    static final int RETRY_INTERVAL = 20;
    static final int CANDIDATES_PER_RETRY = 8;
    static final int FINAL_ENGAGEMENT = 900;

    final int number;
    final int planned;
    final AssaultComposition composition;
    final Map<String, Integer> rejections = new LinkedHashMap<String, Integer>();
    int startedAt = -1;
    int closedAt = -1;
    int attempts;
    int inserted;
    int outdoorLight;
    int active;
    int carriedIn;
    int carriedOut;
    int localHostiles = -1;
    int peakActive;
    String outcome;

    private AssaultWave(int number, int planned, Profile profile, int difficulty) {
        this.number = number;
        this.planned = planned;
        composition = AssaultComposition.select(profile, difficulty, number);
    }

    static AssaultWave[] plan(int budget) {
        return plan(budget, Profile.DISABLED);
    }

    static AssaultWave[] plan(int budget, Profile profile) {
        return plan(budget, profile, 2);
    }

    static AssaultWave[] plan(int budget, Profile profile, int difficulty) {
        int first = (budget * 4 + 7) / 15;
        int throughSecond = (budget * 9 + 7) / 15;
        int middle = throughSecond - first;
        int last = budget - throughSecond;
        if (profile == Profile.QUIET && middle > 0 && first > 1) {
            int shift = Math.min(2, first - 1);
            first -= shift;
            middle += shift;
        } else if (profile == Profile.ELEVATED && first > 0 && last > 1) {
            int shift = Math.min(2, last - 1);
            first += shift;
            last -= shift;
        }
        return new AssaultWave[] {
            new AssaultWave(1, first, profile, difficulty),
            new AssaultWave(2, middle, profile, difficulty),
            new AssaultWave(3, last, profile, difficulty)
        };
    }

    int allocation() { return planned + carriedIn; }
    int unspent() { return allocation() - inserted - carriedOut; }
    int skipped() { return closedAt < 0 ? 0 : unspent(); }
    int deferred() { return closedAt < 0 ? unspent() : 0; }

    int nextStart(int interval, int activeAssault) {
        if (closedAt < 0) return startedAt + interval;
        int spacing = activeAssault == 0 ? Math.max(360, interval / 2) : interval;
        return Math.max(startedAt + spacing, closedAt + 80);
    }

    String state() {
        if (closedAt < 0) return startedAt < 0 ? "PENDING" : "SPAWNING";
        if (inserted == 0) return "SKIPPED";
        return active > 0 ? "ENGAGED" : "COMPLETED";
    }

    void reject(String reason) {
        Integer count = rejections.get(reason);
        rejections.put(reason, count == null ? 1 : count + 1);
    }
}
