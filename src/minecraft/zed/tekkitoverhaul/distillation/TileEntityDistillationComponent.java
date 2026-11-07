package zed.tekkitoverhaul.distillation;

import ic2.api.IWrenchable;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.TileEntity;

/** Persists the preferred front in block metadata, using the IC2 1.97 side convention. */
abstract class TileEntityDistillationComponent extends TileEntity implements IWrenchable {
    protected DistillationLayout.Facing getPreferredFacing() {
        int side = worldObj == null ? 0 : worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        return DistillationLayout.Facing.fromFrontSide(side);
    }

    public abstract DistillationLayout.Facing getVisualFacing();

    protected abstract boolean canTurnTo(DistillationLayout.Facing facing);

    public boolean wrenchCanSetFacing(EntityPlayer player, int side) {
        return worldObj != null && side >= 2 && side <= 5 && side != getFacing()
                && canTurnTo(DistillationLayout.Facing.fromFrontSide(side));
    }

    public short getFacing() {
        return getVisualFacing().frontSide;
    }

    public void setFacing(short side) {
        if (wrenchCanSetFacing(null, side)) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, side);
        }
    }

    public boolean wrenchCanRemove(EntityPlayer player) { return false; }

    public float getWrenchDropRate() { return 1F; }
}
