package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.lunar.LunarPhase;

public final class EnderkinSelectionSelfTest {
    public static void main(String[] args) {
        int patrols = 0, skirmishers = 0, breachers = 0, ordinaryAssaults = 0;
        for (long seed = 0; seed < 100; seed++) {
            for (long cycle = 0; cycle < 100; cycle++) {
                long day = cycle * 8;
                check(EnderkinRole.select(false, seed, day + 4, LunarPhase.FULL_MOON, 2) == null);
                check(EnderkinRole.select(true, seed, day + 2, LunarPhase.FIRST_QUARTER, 0) == null);
                check(EnderkinRole.select(true, seed, day + 4, LunarPhase.FULL_MOON, 1) == null);
                EnderkinRole patrol = EnderkinRole.select(true, seed, day + 3, LunarPhase.WAXING_GIBBOUS, 0);
                EnderkinRole second = EnderkinRole.select(true, seed, day + 4, LunarPhase.FULL_MOON, 2);
                EnderkinRole third = EnderkinRole.select(true, seed, day + 4, LunarPhase.FULL_MOON, 3);
                check(patrol == null || patrol == EnderkinRole.SKIRMISHER);
                check(second == null || second == EnderkinRole.SKIRMISHER);
                check(third == null || third == EnderkinRole.BREACHER);
                if (patrol != null) patrols++;
                if (second != null) skirmishers++;
                if (third != null) breachers++;
                if (second == null && third == null) ordinaryAssaults++;
            }
        }
        check(patrols > 2100 && patrols < 2900 && skirmishers > 4600 && skirmishers < 5400
                && breachers > 3600 && breachers < 4400 && ordinaryAssaults > 0);
        EnderkinSlot slot = new EnderkinSlot();
        slot.reset(EnderkinRole.BREACHER);
        for (int i = 0; i < 4; i++) {
            check(slot.pending() == EnderkinRole.BREACHER);
            slot.record("ENDERKIN_CLEARANCE");
        }
        check(slot.pending() == null && !slot.inserted && slot.rejections.get("ENDERKIN_CLEARANCE") == 4);
        slot.reset(EnderkinRole.SKIRMISHER);
        slot.record(null);
        check(slot.pending() == null && slot.inserted && slot.attempts == 1 && slot.rejections.isEmpty());
        System.out.println("Enderkin selection and fallback passed across 10000 cycles/seeds");
    }

    private static void check(boolean result) {
        if (!result) throw new AssertionError("Enderkin composition invariant failed");
    }
}
