package zed.tekkitoverhaul.render;

import net.minecraft.src.Block;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import zed.tekkitoverhaul.machine.FoundryRotorState;
import zed.tekkitoverhaul.multiblock.FoundryStructureValidator;
import zed.tekkitoverhaul.multiblock.StructurePattern;
import zed.tekkitoverhaul.multiblock.WorldBlockView;

/** Persistent rotor state and a transient, periodically refreshed structure check. */
public final class TileEntityFoundryVisual extends TileEntity {
    private static final String NBT_RUNNING = "ZTO_RotorRunning";
    private static final String NBT_ANGLE = "ZTO_RotorAngle";
    private static final String NBT_REFERENCE = "ZTO_RotorReferenceTick";
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final StructurePattern STONE_RING =
            FoundryStructureValidator.createStoneRing(Block.stone.blockID);
    private final FoundryRotorState rotor = new FoundryRotorState();

    // Structural validity depends on live world blocks and must not be trusted from NBT.
    private boolean structureChecked;
    private boolean structureFormed;
    private int missingStone = 8;
    private int nextCheckInTicks;

    public boolean isRotorRunning() {
        return rotor.isRunning();
    }

    public float getRotorAngle(float partialTicks) {
        long currentTick = worldObj == null ? 0L : worldObj.getWorldTime();
        return rotor.angleAt(currentTick, partialTicks);
    }

    public void toggleRotor() {
        if (worldObj != null && rotor.setRunning(!rotor.isRunning(), worldObj.getWorldTime())) {
            onInventoryChanged(); // Persist only the rotation state, not derived formation status.
        }
    }

    /** Revalidate on demand, for example after player interaction. */
    public void checkStructureNow() {
        if (worldObj == null) {
            invalidateStructure();
            return;
        }
        StructurePattern.Result result = STONE_RING.check(new WorldBlockView(worldObj),
                xCoord, yCoord, zCoord);
        structureFormed = result.formed;
        missingStone = result.mismatches;
        structureChecked = true;
        nextCheckInTicks = CHECK_INTERVAL_TICKS;
    }

    public void invalidateStructure() {
        structureChecked = false;
        nextCheckInTicks = 0;
    }

    public boolean isStructureFormed() {
        if (!structureChecked) checkStructureNow();
        return structureFormed;
    }

    public int getMissingStoneCount() {
        if (!structureChecked) checkStructureNow();
        return missingStone;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (!structureChecked || --nextCheckInTicks <= 0) {
            checkStructureNow();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey(NBT_RUNNING)) {
            rotor.load(tag.getBoolean(NBT_RUNNING), tag.getFloat(NBT_ANGLE),
                    tag.getLong(NBT_REFERENCE));
        } else {
            rotor.load(true, 0F, 0L);
        }
        structureFormed = false;
        missingStone = 8;
        invalidateStructure();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean(NBT_RUNNING, rotor.isRunning());
        tag.setFloat(NBT_ANGLE, rotor.getStoppedAngle());
        tag.setLong(NBT_REFERENCE, rotor.getReferenceTick());
    }
}
