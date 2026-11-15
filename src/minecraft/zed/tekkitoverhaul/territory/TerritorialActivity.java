package zed.tekkitoverhaul.territory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Overworld cell history. Iteration order is oldest observation first. */
final class TerritorialActivity {
    static final int CELL_SIZE = 64;
    static final int CAPACITY = 512;
    static final int SAMPLE_TICKS = 100;
    static final int PRESENCE_MAX = 24;
    static final int RETURN_MAX = 8;
    static final int RETURN_AFTER = 3600;
    static final int RETURN_SAMPLES = 3;
    static final int PRESENCE_DECAY = 1200;
    static final int RETURN_DECAY = 6000;
    static final int WORLD_LIMIT = 30000000;

    final Map<Long, Record> records = new LinkedHashMap<Long, Record>();
    long clock;
    boolean evicted;
    private Long visit;
    private int visitSamples;
    private boolean spacedReturn;

    static boolean inBounds(double position) {
        return position >= -WORLD_LIMIT && position < WORLD_LIMIT;
    }

    static int cell(int block) { return Math.floorDiv(block, CELL_SIZE); }

    static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
    static int x(long key) { return (int) (key >> 32); }
    static int z(long key) { return (int) key; }

    void clearVisit() {
        visit = null;
        visitSamples = 0;
        spacedReturn = false;
    }

    void advance() {
        if (clock < Long.MAX_VALUE) clock++;
    }

    boolean observe(int blockX, int blockZ) {
        Long position = key(cell(blockX), cell(blockZ));
        Record record = records.remove(position);
        if (!position.equals(visit)) {
            visit = position;
            visitSamples = 0;
            spacedReturn = record != null && clock - record.seen >= RETURN_AFTER
                    && record.score(clock) > 0;
        }
        if (visitSamples < RETURN_SAMPLES) visitSamples++;
        int previousPresence = record == null ? 0 : record.presence;
        int previousReturns = record == null ? 0 : record.returns;
        if (record == null) {
            if (records.size() == CAPACITY) {
                Iterator<Long> oldest = records.keySet().iterator();
                oldest.next();
                oldest.remove();
                evicted = true;
            }
            record = new Record();
        }
        record.presence = Math.min(PRESENCE_MAX, record.presenceAt(clock) + 1);
        record.returns = record.returnsAt(clock);
        if (spacedReturn && visitSamples == RETURN_SAMPLES) {
            record.returns = Math.min(RETURN_MAX, record.returns + 2);
            spacedReturn = false;
        }
        record.seen = clock;
        records.put(position, record);
        return record.presence != previousPresence || record.returns != previousReturns;
    }

    int score(int x, int z) {
        Record record = records.get(key(x, z));
        return record == null ? 0 : record.score(clock);
    }

    /** Bilinear interpolation around cell centers using block-center coordinates. */
    int blended(int blockX, int blockZ) {
        int half = CELL_SIZE / 2;
        int left = cell(blockX - half), north = cell(blockZ - half);
        int scale = 2 * CELL_SIZE;
        int east = 2 * Math.floorMod(blockX - half, CELL_SIZE) + 1;
        int south = 2 * Math.floorMod(blockZ - half, CELL_SIZE) + 1;
        int weighted = score(left, north) * (scale - east) * (scale - south)
                + score(left + 1, north) * east * (scale - south)
                + score(left, north + 1) * (scale - east) * south
                + score(left + 1, north + 1) * east * south;
        return (weighted + scale * scale / 2) / (scale * scale);
    }

    static final class Record {
        int presence, returns;
        long seen;

        int presenceAt(long tick) { return decayed(presence, tick, PRESENCE_DECAY); }
        int returnsAt(long tick) { return decayed(returns, tick, RETURN_DECAY); }
        int score(long tick) { return presenceAt(tick) + returnsAt(tick); }

        private int decayed(int value, long tick, int period) {
            long elapsed = tick >= seen ? tick - seen : 0;
            return (int) Math.max(0L, value - elapsed / period);
        }
    }
}
