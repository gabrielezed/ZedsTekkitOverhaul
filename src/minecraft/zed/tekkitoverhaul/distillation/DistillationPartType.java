package zed.tekkitoverhaul.distillation;

/** Fixed block component identities. Values are not interchangeable with item IDs. */
public enum DistillationPartType {
    BOILER(0, "Distillation Boiler", "boiler"),
    HEATER(1, "LV Distillation Heater", "heater"),
    COLUMN(2, "Fractionating Column", "column"),
    CONDENSER(3, "Water-Cooled Condenser", "condenser"),
    RECEIVER(4, "Distillate Receiver", "receiver"),
    THERMOMETER(5, "Distillation Thermometer", "thermometer");

    public final int index;
    public final String displayName;
    public final String modelName;

    DistillationPartType(int index, String name, String modelName) {
        this.index = index;
        this.displayName = name;
        this.modelName = modelName;
    }

    public static DistillationPartType fromIndex(int index) {
        for (DistillationPartType type : values()) {
            if (type.index == index) return type;
        }
        return null;
    }
}
