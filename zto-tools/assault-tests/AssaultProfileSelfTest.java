package zed.tekkitoverhaul.assault;

import zed.tekkitoverhaul.assault.AssaultWave.Profile;

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
        System.out.println("PASS contextual assault profiles for budgets 1–24");
    }

    private static void plan(Profile profile, int first, int middle, int last) {
        AssaultWave[] waves = AssaultWave.plan(profile.budget(15), profile);
        expect(waves[0].planned == first && waves[1].planned == middle && waves[2].planned == last, profile.name());
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
