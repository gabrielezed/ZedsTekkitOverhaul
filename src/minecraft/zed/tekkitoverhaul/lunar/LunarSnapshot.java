package zed.tekkitoverhaul.lunar;

/** An immutable view containing no game or save-handler references. */
public final class LunarSnapshot {
    public static final int DAY = 24000;
    public static final int NIGHT_START = 13000;
    public static final int NIGHT_END = 23000;

    public final long sessionId;
    public final int dimension;
    public final long rawTime;
    public final int timeOfDay;
    public final long logicalDay;
    public final int dayInCycle;
    public final long cycleIndex;
    public final long completedCycles;
    public final LunarPhase phase;
    public final boolean night;
    public final int ticksToNextPhase;
    public final boolean schedulingEligible;

    LunarSnapshot(long sessionId, int dimension, long rawTime, long adjustment) {
        this.sessionId = sessionId;
        this.dimension = dimension;
        this.rawTime = rawTime;
        timeOfDay = (int) Math.floorMod(rawTime, (long) DAY);
        logicalDay = Math.floorDiv(rawTime, (long) DAY) + adjustment;
        phase = LunarPhase.onDay(logicalDay);
        dayInCycle = phase.id;
        cycleIndex = Math.floorDiv(logicalDay, 8L);
        completedCycles = Math.max(0, cycleIndex);
        night = timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
        ticksToNextPhase = DAY - timeOfDay;
        schedulingEligible = dimension == 0;
    }

    public int daysUntil(LunarPhase target) {
        return Math.floorMod(target.id - phase.id, 8);
    }

    public boolean isFullMoon() { return phase == LunarPhase.FULL_MOON; }
    public boolean isNewMoon() { return phase == LunarPhase.NEW_MOON; }
}
