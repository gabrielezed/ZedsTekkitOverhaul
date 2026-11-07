package zed.tekkitoverhaul.distillation;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;

/** Boiler controller: stores tanks, selected fraction and manufacturing progress. */
public final class TileEntityDistillationBoiler extends TileEntityDistillationComponent {
    private static final int STRUCTURE_CHECK_INTERVAL = 20;
    private final FractionationProcess process = new FractionationProcess();
    private int checkCountdown;
    private boolean formed;
    private int missingParts = 6;
    private DistillationLayout.Match lastStructure;
    private long lastSavedTick;

    public FractionationProcess getProcess() { return process; }
    public boolean isFormed() { return formed; }
    public int getMissingParts() { return missingParts; }
    private long lastVisualCheck = Long.MIN_VALUE;

    public DistillationLayout.Facing getVisualFacing() {
        if (worldObj != null && (lastStructure == null || worldObj.isRemote)) {
            long now = worldObj.getWorldTime();
            if (lastVisualCheck == Long.MIN_VALUE || now < lastVisualCheck ||
                    now - lastVisualCheck >= STRUCTURE_CHECK_INTERVAL) {
                lastVisualCheck = now;
                refreshStructure();
            }
        }
        return lastStructure == null ? getPreferredFacing() : lastStructure.facing;
    }

    @Override
    protected boolean canTurnTo(DistillationLayout.Facing facing) {
        return DistillationStructure.canFace(worldObj, xCoord, yCoord, zCoord, facing);
    }
    public String getStructureDetails() {
        return lastStructure == null ? "No boiler structure" : lastStructure.describeMissing();
    }

    public void select(int id) {
        DistillateType next = DistillateType.fromId(id);
        if (next != null) {
            process.select(next);
            onInventoryChanged();
        }
    }

    public int addCrude(int mb) {
        int added = process.fillCrude(mb);
        if (added > 0) onInventoryChanged();
        return added;
    }

    public int addCoolingWater(int mb) {
        int added = process.fillCoolant(mb);
        if (added > 0) onInventoryChanged();
        return added;
    }

    public int takeDistillate(int mb) {
        int taken = process.drain(mb);
        if (taken > 0) onInventoryChanged();
        return taken;
    }

    /**
     * Explain a stalled processing cycle using the current controller state.
     * This is diagnostic feedback, not a networking or process simulation.
     */
    public String getProcessStatus() {
        if (!formed) return "Structure incomplete";
        DistillateType selected = process.selectedType();
        if (selected == null) return "No fraction selected";
        if (process.getCrudeMB() < FractionationProcess.BATCH_INPUT_MB)
            return "Crude oil required";
        if (process.getCoolantMB() < selected.coolantPerBatchMB)
            return "Cooling water required";
        if (process.getDistillateMB() > 0 &&
                process.getOutputTypeId() != selected.id)
            return "Receiver contains another fraction";
        if (process.getDistillateMB() >
                FractionationProcess.TANK_CAPACITY_MB - FractionationProcess.BATCH_OUTPUT_MB)
            return "Receiver is full";
        if (worldObj == null) return "World not available";
        TileEntity below = worldObj.getBlockTileEntity(xCoord, yCoord - 1, zCoord);
        if (!(below instanceof TileEntityDistillationHeater))
            return "LV heater missing";
        if (((TileEntityDistillationHeater)below).getStoredEnergy() <
                FractionationProcess.EU_PER_TICK)
            return "Waiting for LV power";
        return "Processing";
    }

    public void refreshStructure() {
        lastStructure = DistillationStructure.check(worldObj, xCoord, yCoord, zCoord);
        formed = lastStructure != null && lastStructure.isFormed();
        missingParts = lastStructure == null ? 6 : lastStructure.missingParts;
        checkCountdown = STRUCTURE_CHECK_INTERVAL;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (--checkCountdown <= 0) refreshStructure();
        TileEntity heaterTile = worldObj.getBlockTileEntity(xCoord, yCoord - 1, zCoord);
        TileEntityDistillationHeater heater = heaterTile instanceof TileEntityDistillationHeater
                ? (TileEntityDistillationHeater)heaterTile : null;
        int available = heater == null ? 0 : heater.getStoredEnergy();
        int eu = process.tick(formed && heater != null, available);
        if (eu > 0) {
            // The process only asks for EU already present in this heater.
            if (!heater.extractEU(eu)) throw new IllegalStateException("Distillation EU accounting error");
            if (worldObj.getWorldTime() - lastSavedTick >= 20L || process.getProgressTicks() == 0) {
                lastSavedTick = worldObj.getWorldTime();
                onInventoryChanged();
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        process.restore(tag.getInteger("ZTO_DistillCrude"),
                tag.getInteger("ZTO_DistillWater"),
                tag.getInteger("ZTO_DistillOutput"),
                tag.getInteger("ZTO_DistillProduct"),
                tag.getInteger("ZTO_DistillSelected"),
                tag.getInteger("ZTO_DistillProgress"));
        formed = false;
        missingParts = 6;
        lastStructure = null;
        checkCountdown = 0;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("ZTO_DistillCrude", process.getCrudeMB());
        tag.setInteger("ZTO_DistillWater", process.getCoolantMB());
        tag.setInteger("ZTO_DistillOutput", process.getDistillateMB());
        tag.setInteger("ZTO_DistillProduct", process.getOutputTypeId());
        tag.setInteger("ZTO_DistillSelected", process.getSelectedTypeId());
        tag.setInteger("ZTO_DistillProgress", process.getProgressTicks());
    }
}
