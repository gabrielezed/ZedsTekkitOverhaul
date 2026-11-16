package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.assault.AssaultWave.Profile;
import zed.tekkitoverhaul.lunar.LunarPhase;

public final class AssaultProfileSelfTest {
    public static void main(String[] args) {
        expect(Profile.select(false, null) == Profile.DISABLED, "disabled");
        expect(Profile.select(true, null) == Profile.ASSESSMENT_UNAVAILABLE, "unavailable");
        expect(Profile.forThreat(15) == Profile.QUIET && Profile.forThreat(16) == Profile.ORDINARY
                && Profile.forThreat(23) == Profile.ORDINARY && Profile.forThreat(24) == Profile.ELEVATED,
                "thresholds");
        plan(Profile.QUIET, 2, 6, 6);
        plan(Profile.ORDINARY, 4, 5, 6);
        plan(Profile.ELEVATED, 6, 5, 4);
        for (Profile profile : Profile.values()) {
            for (int maximum = 1; maximum <= 24; maximum++) {
                int selected = profile.budget(maximum);
                int sum = 0;
                AssaultWave[] legacy = AssaultWave.plan(selected);
                AssaultWave[] contextual = AssaultWave.plan(selected, profile);
                for (int i = 0; i < contextual.length; i++) {
                    int planned = contextual[i].planned;
                    expect((planned > 0) == (legacy[i].planned > 0), "original wave availability");
                    expect(Math.abs(planned - legacy[i].planned) <= 2, "bounded redistribution");
                    sum += planned;
                }
                expect(contextual.length == 3 && sum == selected && sum > 0 && sum <= maximum, "budget");
            }
        }
        composition(Profile.QUIET, "ZZ", "ZZZSZZ", "ZZZSZZ");
        composition(Profile.ORDINARY, "ZZSZ", "ZZSZZ", "SZSZSZ");
        composition(Profile.ELEVATED, "SZSZSZ", "ZZSZZ", "SZSZ");
        for (Profile profile : Profile.values()) {
            for (int difficulty = 1; difficulty <= 3; difficulty++) {
                for (int budget = 1; budget <= 24; budget++) {
                    verifySlots(profile, difficulty, profile.budget(budget));
                }
            }
        }
        System.out.println("PASS contextual waves, vanilla slots and special fallback for budgets 1–24");
    }

    private static void composition(Profile profile, String... expected) {
        for (int difficulty = 1; difficulty <= 2; difficulty++) {
            for (AssaultWave wave : AssaultWave.plan(profile.budget(15), profile, difficulty)) {
                StringBuilder actual = new StringBuilder();
                while (wave.unspent() > 0) {
                    actual.append(wave.composition.skeleton(wave.inserted++) ? 'S' : 'Z');
                }
                expect(expected[wave.number - 1].contentEquals(actual), "default slot order " + profile);
            }
        }
    }

    private static void verifySlots(Profile profile, int difficulty, int budget) {
        AssaultWave[] plan = AssaultWave.plan(budget, profile, difficulty);
        for (AssaultWave wave : plan) {
            // Check the carry slot as well as the admission allocation.
            for (int slot = 0; slot <= wave.planned; slot++) {
                boolean skeleton = wave.composition.skeleton(slot);
                if (difficulty == 3) expect(skeleton, "Hard cannot select a vanilla zombie");
                if (difficulty < 3 && profile == Profile.QUIET) expect(skeleton == (slot % 4 == 3), "quiet ranged bound");
                if (difficulty < 3 && profile == Profile.ELEVATED && wave.number == 1) {
                    expect(skeleton == (slot % 2 == 0), "elevated alternating opening");
                }
                if (profile != Profile.QUIET && (profile != Profile.ELEVATED || wave.number != 1)) {
                    boolean legacy = difficulty == 3 || (wave.number == 3 ? slot % 2 == 0 : slot % 3 == 2);
                    expect(skeleton == legacy, "legacy order including carry");
                }
            }
            for (boolean enderkin : new boolean[] {false, true}) {
                EnderkinRole role = EnderkinRole.select(enderkin, -7184827518270584220L, 8, LunarPhase.FULL_MOON, wave.number);
                EnderkinRole expected = !enderkin || wave.number == 1 ? null
                        : wave.number == 2 ? EnderkinRole.SKIRMISHER : EnderkinRole.BREACHER;
                expect(role == expected && role == EnderkinRole.select(enderkin, -7184827518270584220L, 8,
                        LunarPhase.FULL_MOON, wave.number), "independent same-seed special role");
                if (wave.planned == 0) continue;
                wave.special.reset(role);
                boolean first = wave.composition.skeleton(wave.inserted);
                for (int i = 0; i < 4; i++) {
                    wave.reject("NOT_OPEN_TO_SKY");
                    if (wave.special.pending() != null) wave.special.record("NOT_OPEN_TO_SKY");
                    expect(wave.composition.skeleton(wave.inserted) == first, "failed candidate keeps current vanilla slot");
                }
                expect(wave.special.pending() == null, "ordinary fallback after rejected special");
                expect(plan[2].composition == AssaultWave.plan(budget, profile, difficulty)[2].composition,
                        "rejections leave later composition frozen");
            }
        }
    }

    private static void plan(Profile profile, int first, int middle, int last) {
        AssaultWave[] waves = AssaultWave.plan(profile.budget(15), profile);
        expect(waves[0].planned == first && waves[1].planned == middle && waves[2].planned == last, profile.name());
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
