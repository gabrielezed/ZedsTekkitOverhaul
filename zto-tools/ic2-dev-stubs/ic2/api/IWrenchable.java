package ic2.api;

import net.minecraft.src.EntityPlayer;

/** Local development ABI placeholder for IC2 1.97. */
public interface IWrenchable {
    boolean wrenchCanSetFacing(EntityPlayer player, int side);
    short getFacing();
    void setFacing(short side);
    boolean wrenchCanRemove(EntityPlayer player);
    float getWrenchDropRate();
}
