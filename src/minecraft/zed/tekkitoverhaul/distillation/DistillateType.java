package zed.tekkitoverhaul.distillation;

/** Stable fluid identifiers; do not reorder or renumber without an NBT migration. */
public enum DistillateType {
    GASOLINE(0, "Gasoline", 125, 160, 40),
    KEROSENE(1, "Kerosene", 205, 200, 50),
    DIESEL(2, "Diesel", 300, 260, 60),
    HEAVY_GAS_OIL(3, "Heavy Gas Oil", 380, 320, 80);

    public final int id;
    public final String displayName;
    public final int temperatureCelsius;
    public final int durationTicks;
    public final int coolantPerBatchMB;

    DistillateType(int id, String name, int temperatureCelsius, int durationTicks, int coolantPerBatchMB) {
        this.id = id;
        this.displayName = name;
        this.temperatureCelsius = temperatureCelsius;
        this.durationTicks = durationTicks;
        this.coolantPerBatchMB = coolantPerBatchMB;
    }

    public static DistillateType fromId(int id) {
        for (DistillateType kind : values()) {
            if (kind.id == id) return kind;
        }
        return null;
    }
}
