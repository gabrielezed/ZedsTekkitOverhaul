package zed.tekkitoverhaul.distillation;

/** Result of the last authoritative boiler tick. */
public enum DistillationState {
    INCOMPLETE("Structure incomplete", "Place the missing components."),
    IDLE("Ready", "Ready for the next processing tick."),
    ACTIVE("Processing", "Batch advancing at 8 EU/t."),
    CRUDE_REQUIRED("Crude oil required", "Load a BuildCraft oil bucket or connect an oil pipe."),
    COOLANT_REQUIRED("Cooling water required", "Fill the condenser with water."),
    POWER_REQUIRED("Waiting for LV power", "Supply the heater with at least 8 EU."),
    OUTPUT_FULL("Receiver is full", "Extract distillate to free room for the next batch."),
    OUTPUT_MISMATCH("Receiver contains another fraction", "Drain the receiver or select the stored fraction.");

    public final String label;
    public final String action;

    DistillationState(String label, String action) {
        this.label = label;
        this.action = action;
    }

    public boolean blocksOutput() {
        return this == OUTPUT_FULL || this == OUTPUT_MISMATCH;
    }
}
