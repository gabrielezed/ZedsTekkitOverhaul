package zed.tekkitoverhaul.assault;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-wave accounting in simulation ticks. Carry-over transfers existing budget. */
final class AssaultWave {
    static final int COUNT = 3;
    static final int FIRST_START = 100;
    static final int WINDOW = 280;
    static final int RETRY_INTERVAL = 20;
    static final int CANDIDATES_PER_RETRY = 8;
    static final int FINAL_ENGAGEMENT = 900;

    final int number;
    final int planned;
    final Map<String, Integer> rejections = new LinkedHashMap<String, Integer>();
    int startedAt = -1;
    int closedAt = -1;
    int attempts;
    int inserted;
    int active;
    int carriedIn;
    int carriedOut;
    int localHostiles = -1;
    int peakActive;
    String outcome;

    AssaultWave(int number, int planned) {
        this.number = number;
        this.planned = planned;
    }

    static AssaultWave[] plan(int budget) {
        int first = (budget * 4 + 7) / 15;
        int second = (budget * 9 + 7) / 15;
        return new AssaultWave[] {
            new AssaultWave(1, first), new AssaultWave(2, second - first),
            new AssaultWave(3, budget - second)
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
