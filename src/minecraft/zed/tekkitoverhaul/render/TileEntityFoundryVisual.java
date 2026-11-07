package zed.tekkitoverhaul.render;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import zed.tekkitoverhaul.machine.FoundryRotorState;

/** Visual-only tile entity with a persistent rotor enable flag and rotation phase. */
public final class TileEntityFoundryVisual extends TileEntity {
    private static final String NBT_RUNNING = "ZTO_RotorRunning";
    private static final String NBT_ANGLE = "ZTO_RotorAngle";
    private static final String NBT_REFERENCE = "ZTO_RotorReferenceTick";

    private final FoundryRotorState rotor = new FoundryRotorState();

    public boolean isRotorRunning() {
        return rotor.isRunning();
    }

    public float getRotorAngle(float partialTicks) {
        long currentTick = worldObj == null ? 0L : worldObj.getWorldTime();
        return rotor.angleAt(currentTick, partialTicks);
    }

    /** Called by the block after a server-side right click. */
    public void toggleRotor() {
        if (worldObj != null && rotor.setRunning(!rotor.isRunning(), worldObj.getWorldTime())) {
            onInventoryChanged(); // Marks the containing chunk for saving in this legacy API.
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey(NBT_RUNNING)) {
            rotor.load(tag.getBoolean(NBT_RUNNING), tag.getFloat(NBT_ANGLE),
                    tag.getLong(NBT_REFERENCE));
        } else {
            // Older Foundry TileEntities had no custom data and defaulted to running.
            rotor.load(true, 0F, 0L);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean(NBT_RUNNING, rotor.isRunning());
        tag.setFloat(NBT_ANGLE, rotor.getStoppedAngle());
        tag.setLong(NBT_REFERENCE, rotor.getReferenceTick());
    }
}
