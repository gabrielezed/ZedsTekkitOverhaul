package zed.tekkitoverhaul.distillation;

/**
 * Deterministic, Minecraft-independent prototype of batch oil fractionation.
 * Internal liquid amounts are measured in mB; output is single-fluid until drained.
 */
public final class FractionationProcess {
    public static final int TANK_CAPACITY_MB = 8000;
    public static final int BATCH_INPUT_MB = 250;
    public static final int BATCH_OUTPUT_MB = 250;
    public static final int EU_PER_TICK = 8;

    private int crudeMB;
    private int coolantMB;
    private int distillateMB;
    private int outputTypeId = -1;
    private int selectedTypeId = 0;
    private int progressTicks;

    public int getCrudeMB() { return crudeMB; }
    public int getCoolantMB() { return coolantMB; }
    public int getDistillateMB() { return distillateMB; }
    public int getOutputTypeId() { return outputTypeId; }
    public int getSelectedTypeId() { return selectedTypeId; }
    public int getProgressTicks() { return progressTicks; }

    public DistillateType selectedType() {
        return DistillateType.fromId(selectedTypeId);
    }

    public int fillCrude(int requestedMB) {
        int accepted = accept(crudeMB, requestedMB);
        crudeMB += accepted;
        return accepted;
    }

    public int fillCoolant(int requestedMB) {
        int accepted = accept(coolantMB, requestedMB);
        coolantMB += accepted;
        return accepted;
    }

    public void select(DistillateType type) {
        if (type == null) throw new IllegalArgumentException("Distillate type is required");
        if (selectedTypeId != type.id) {
            selectedTypeId = type.id;
            progressTicks = 0; // Prevent switching a nearly completed batch to another product.
        }
    }

    /** Returns true if this output can be drained without mixing different products. */
    public boolean canDrain(DistillateType type, int amountMB) {
        return type != null && outputTypeId == type.id && amountMB > 0 && distillateMB >= amountMB;
    }

    public int drain(int requestedMB) {
        if (requestedMB <= 0) return 0;
        int taken = Math.min(requestedMB, distillateMB);
        distillateMB -= taken;
        if (distillateMB == 0) outputTypeId = -1;
        return taken;
    }

    /**
     * Advances exactly one tick if the structure is formed and energy is available.
     * Returns EU to deduct from the external heater; returns zero when stalled.
     */
    public int tick(boolean formed, int availableEu) {
        DistillateType type = selectedType();
        if (type == null || !formed || availableEu < EU_PER_TICK ||
                crudeMB < BATCH_INPUT_MB || coolantMB < type.coolantPerBatchMB ||
                distillateMB > TANK_CAPACITY_MB - BATCH_OUTPUT_MB ||
                distillateMB > 0 && outputTypeId != type.id) {
            return 0;
        }
        ++progressTicks;
        if (progressTicks >= type.durationTicks) {
            crudeMB -= BATCH_INPUT_MB;
            coolantMB -= type.coolantPerBatchMB;
            if (distillateMB == 0) outputTypeId = type.id;
            distillateMB += BATCH_OUTPUT_MB;
            progressTicks = 0;
        }
        return EU_PER_TICK;
    }

    /** Used only by the TileEntity NBT adapter; all fields are range-checked. */
    public void restore(int crude, int coolant, int output, int outputType,
                        int selectedType, int progress) {
        crudeMB = clamp(crude);
        coolantMB = clamp(coolant);
        distillateMB = clamp(output);
        DistillateType actual = DistillateType.fromId(outputType);
        outputTypeId = distillateMB == 0 || actual == null ? -1 : actual.id;
        if (outputTypeId == -1) distillateMB = 0;
        DistillateType selected = DistillateType.fromId(selectedType);
        selectedTypeId = selected == null ? 0 : selected.id;
        progressTicks = Math.max(0, Math.min(progress, selectedType().durationTicks - 1));
    }

    private static int accept(int current, int requested) {
        if (requested <= 0) return 0;
        return Math.min(requested, TANK_CAPACITY_MB - current);
    }

    private static int clamp(int n) {
        return Math.max(0, Math.min(TANK_CAPACITY_MB, n));
    }
}
