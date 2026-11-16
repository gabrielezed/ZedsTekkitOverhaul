package zed.tekkitoverhaul.territory;

/** Immutable local inputs and decision, with no dependency on the game or diagnostics. */
public final class ThreatAssessment {
    public static final int MAX = 50;

    public enum Environment {
        BASELINE(12), EXPOSED(16), SWAMPLAND(18);

        final int contribution;
        Environment(int contribution) { this.contribution = contribution; }
    }

    public final int dimension, regionX, regionZ, activity, environmental, threat;
    public final Environment environment;
    public final boolean observed;

    ThreatAssessment(int dimension, int regionX, int regionZ, int activity, Environment environment,
            int environmental, boolean observed) {
        this.dimension = dimension;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.activity = Math.max(0, Math.min(TerritorialActivity.PRESENCE_MAX + TerritorialActivity.RETURN_MAX, activity));
        this.environment = environment;
        this.environmental = environmental;
        this.threat = this.activity + this.environmental;
        this.observed = observed;
    }
}
