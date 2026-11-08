package ic2.api;

import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/**
 * MCP-only test double. No EU is actually transferred in Eclipse.
 * Never include this placeholder class in the release mod JAR.
 */
public final class EnergyNet {
    private static final EnergyNet EMPTY = new EnergyNet();

    private EnergyNet() { }

    public static EnergyNet getForWorld(World world) {
        return EMPTY;
    }

    public void addTileEntity(TileEntity tile) {
        // Deliberately no-op: real IC2 1.97 supplies the production EnergyNet.
    }

    public void removeTileEntity(TileEntity tile) {
        // Deliberately no-op.
    }

    public int emitEnergyFrom(IEnergySource source, int amount) {
        return amount;
    }
}
