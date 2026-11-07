package ic2.api;

import net.minecraft.src.TileEntity;

/** Local development ABI placeholder for IC2 1.97. */
public interface IEnergyAcceptor extends IEnergyTile {
    boolean acceptsEnergyFrom(TileEntity emitter, Direction direction);
}
