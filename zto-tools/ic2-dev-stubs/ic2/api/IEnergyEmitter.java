package ic2.api;

import net.minecraft.src.TileEntity;

/** Compile-only MCP boundary for a legacy IC2 emitter. */
public interface IEnergyEmitter extends IEnergyTile {
    boolean emitsEnergyTo(TileEntity receiver, Direction side);
}
