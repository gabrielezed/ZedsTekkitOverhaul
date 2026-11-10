package zed.tekkitoverhaul.lunar;

public enum LunarPhase {
    NEW_MOON(0, "New Moon"),
    WAXING_CRESCENT(1, "Waxing Crescent"),
    FIRST_QUARTER(2, "First Quarter"),
    WAXING_GIBBOUS(3, "Waxing Gibbous"),
    FULL_MOON(4, "Full Moon"),
    WANING_GIBBOUS(5, "Waning Gibbous"),
    LAST_QUARTER(6, "Last Quarter"),
    WANING_CRESCENT(7, "Waning Crescent");

    private static final LunarPhase[] BY_DAY = values();
    public final int id;
    public final String label;

    LunarPhase(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public static LunarPhase onDay(long day) {
        return BY_DAY[(int) Math.floorMod(day, 8L)];
    }
}
