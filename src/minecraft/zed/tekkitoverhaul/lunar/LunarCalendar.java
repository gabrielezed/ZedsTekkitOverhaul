package zed.tekkitoverhaul.lunar;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static zed.tekkitoverhaul.lunar.LunarEvent.Cause;
import static zed.tekkitoverhaul.lunar.LunarEvent.Kind;

/** Game-thread authority. Adapters own lifecycle, persistence and publication context. */
public final class LunarCalendar {
    public static final long MAX_ADJUSTMENT = 1000000000000000L;
    private final Thread owner = Thread.currentThread();
    private final long sessionId;
    private final long baseline;
    private final Runnable dirty;
    private final Consumer<RuntimeException> report;
    private final Set<LunarService.Listener> listeners = new LinkedHashSet<LunarService.Listener>();
    private volatile LunarSnapshot snapshot;
    private long adjustment;
    private long watermark;
    private boolean dispatching;
    private boolean detached;

    private final LunarService view = new LunarService() {
        public LunarSnapshot snapshot() { return snapshot; }

        public void subscribe(Listener listener) {
            checkThread();
            if (!detached) listeners.add(Objects.requireNonNull(listener));
        }

        public void unsubscribe(Listener listener) {
            checkThread();
            listeners.remove(listener);
        }
    };

    public LunarCalendar(long sessionId, long baseline, long adjustment, long watermark,
                         Runnable dirty, Consumer<RuntimeException> report) {
        if (!validAdjustment(baseline) || !validAdjustment(adjustment)) {
            throw new IllegalArgumentException("Lunar adjustment outside bounds");
        }
        this.sessionId = sessionId;
        this.baseline = baseline;
        this.adjustment = adjustment;
        this.watermark = watermark;
        this.dirty = Objects.requireNonNull(dirty);
        this.report = Objects.requireNonNull(report);
    }

    public static boolean validAdjustment(long days) {
        return days >= -MAX_ADJUSTMENT && days <= MAX_ADJUSTMENT;
    }

    public LunarService view() { return view; }
    public long adjustment() { return adjustment; }
    public long watermark() { return watermark; }

    public void rebaseline(int dimension, long time, Cause cause) {
        checkMutation();
        LunarSnapshot before = snapshot;
        snapshot = new LunarSnapshot(sessionId, dimension, time, adjustment);
        consume(time);
        publish(Kind.RESYNC, cause, before);
    }

    public void observe(long time) {
        checkMutation();
        LunarSnapshot before = Objects.requireNonNull(snapshot);
        if (time == before.rawTime) return;
        snapshot = new LunarSnapshot(sessionId, before.dimension, time, adjustment);
        Cause cause = classify(before, snapshot, watermark);
        if (cause == null) return;
        consume(time);
        if (cause != Cause.NATURAL) {
            publish(Kind.RESYNC, cause, before);
        } else if (snapshot.timeOfDay == 0) {
            publish(Kind.DAY_STARTED, cause, before);
            publish(Kind.PHASE_CHANGED, cause, before);
            if (snapshot.dayInCycle == 0) publish(Kind.CYCLE_COMPLETED, cause, before);
        } else {
            Kind kind = snapshot.timeOfDay == LunarSnapshot.NIGHT_START ? Kind.NIGHT_STARTED : Kind.NIGHT_ENDED;
            publish(kind, cause, before);
        }
    }

    private static Cause classify(LunarSnapshot before, LunarSnapshot after, long watermark) {
        if (after.rawTime == before.rawTime) return null;
        if (after.rawTime < before.rawTime) return Cause.TIME_REWIND;
        if (after.rawTime != before.rawTime + 1) return Cause.TIME_SKIP;
        int tick = after.timeOfDay;
        if (tick != 0 && tick != LunarSnapshot.NIGHT_START && tick != LunarSnapshot.NIGHT_END) return null;
        if (!after.schedulingEligible) return Cause.OUTSIDE_OVERWORLD;
        return after.rawTime <= watermark ? Cause.REPLAY_SUPPRESSED : Cause.NATURAL;
    }

    public LunarEvent preview(long delta) {
        checkThread();
        LunarSnapshot before = Objects.requireNonNull(snapshot);
        if (delta == 0) return null;
        long destination = Math.addExact(before.rawTime, delta);
        LunarSnapshot after = new LunarSnapshot(sessionId, before.dimension, destination, adjustment);
        Cause cause = classify(before, after, watermark);
        Kind kind = Kind.RESYNC;
        if (cause == Cause.NATURAL) {
            if (after.timeOfDay == 0) kind = Kind.DAY_STARTED;
            else if (after.timeOfDay == LunarSnapshot.NIGHT_START) kind = Kind.NIGHT_STARTED;
            else kind = Kind.NIGHT_ENDED;
        }
        return cause == null ? null : new LunarEvent(kind, cause, before, after);
    }

    public void advance(int days) {
        checkMutation();
        if (days < 0 || days > 8) throw new IllegalArgumentException("Expected 0 to 8 days");
        align(Math.addExact(adjustment, days), Cause.ADMIN_COMMAND);
    }

    public void setPhase(LunarPhase phase) {
        checkMutation();
        advance(snapshot.daysUntil(phase));
    }

    public void reset() {
        checkMutation();
        align(baseline, Cause.CALENDAR_RESET);
    }

    private void align(long days, Cause cause) {
        if (!validAdjustment(days)) throw new IllegalArgumentException("Lunar adjustment outside bounds");
        LunarSnapshot before = Objects.requireNonNull(snapshot);
        boolean changed = adjustment != days;
        adjustment = days;
        snapshot = new LunarSnapshot(sessionId, before.dimension, before.rawTime, adjustment);
        if (changed) dirty.run();
        publish(Kind.RESYNC, cause, before);
    }

    public void detach() {
        checkMutation();
        detached = true;
        snapshot = null;
        listeners.clear();
    }

    private void consume(long time) {
        if (time > watermark) {
            watermark = time;
            dirty.run();
        }
    }

    private void publish(Kind kind, Cause cause, LunarSnapshot before) {
        if (listeners.isEmpty()) return;
        LunarEvent event = new LunarEvent(kind, cause, before, snapshot);
        dispatching = true;
        try {
            for (LunarService.Listener listener : listeners.toArray(new LunarService.Listener[0])) {
                try {
                    listener.onLunarEvent(event);
                } catch (RuntimeException failure) {
                    report.accept(failure);
                }
            }
        } finally {
            dispatching = false;
        }
    }

    private void checkThread() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Wrong lunar authority thread");
    }

    private void checkMutation() {
        checkThread();
        if (dispatching || detached) throw new IllegalStateException("Lunar authority unavailable for mutation");
    }
}
