package zed.tekkitoverhaul.hopper;

import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import zed.tekkitoverhaul.integration.LoadedTile;

/** The legacy sided inventory ABI is consulted before either extractor removes a stack. */
final class NativeItemExtraction {
    final TileEntity peer;
    final int face;
    final String operation;
    private final Object mechanism;

    private NativeItemExtraction(TileEntity peer, int face, String operation, Object mechanism) {
        this.peer = peer;
        this.face = face;
        this.operation = operation;
        this.mechanism = mechanism;
    }

    static NativeItemExtraction at(TileHopper hopper, int face) {
        TileEntity peer = neighbor(hopper, face);
        if (peer == null) return null;
        try {
            Class<?> type = peer.getClass();
            if ("buildcraft.transport.TileGenericPipe".equals(type.getName())) {
                Object pipe = type.getField("pipe").get(peer);
                if (pipe != null && "buildcraft.transport.pipes.PipeItemsWood".equals(pipe.getClass().getName())
                        && peer.worldObj.getBlockMetadata(peer.xCoord, peer.yCoord, peer.zCoord) == (face ^ 1)
                        && dedicatedWoodenPipe(peer, face ^ 1))
                    return new NativeItemExtraction(peer, face, "BUILDCRAFT_HOPPER_EXTRACTION", pipe);
            } else if ("eloraam.machine.TileTranspose".equals(type.getName())
                    && type.getField("Rotation").getInt(peer) == face) {
                return new NativeItemExtraction(peer, face, "REDPOWER_HOPPER_EXTRACTION", peer);
            }
        } catch (ReflectiveOperationException incompatible) {
            // An optional endpoint with a different ABI keeps its ordinary inventory access.
        } catch (LinkageError incompatible) {
            // Some partial packs can load a tile before all its dependencies resolve.
        }
        return null;
    }

    boolean matches(TileHopper hopper) {
        NativeItemExtraction current = at(hopper, face);
        return current != null && current.peer == peer && current.mechanism == mechanism;
    }

    private static boolean dedicatedWoodenPipe(TileEntity pipe, int intake) throws ReflectiveOperationException {
        int x = pipe.xCoord, y = pipe.yCoord, z = pipe.zCoord;
        if (!pipe.worldObj.checkChunksExist(x - 1, y, z - 1, x + 1, y, z + 1)) return false;
        // The sided ABI also governs insertion. Exclude junctions and pipes with a second payload source.
        int chests = 0;
        for (int side = 0; side < 6; side++) {
            if (side == intake) continue;
            TileEntity tile = neighbor(pipe, side);
            if (tile instanceof TileEntityChest) chests++;
            else if (tile != null && !redstoneEngine(tile)) return false;
        }
        return chests == 1;
    }

    private static boolean redstoneEngine(TileEntity tile) throws ReflectiveOperationException {
        if (!"buildcraft.energy.TileEngine".equals(tile.getClass().getName())) return false;
        Object engine = tile.getClass().getMethod("getEngine").invoke(tile);
        return engine != null && "buildcraft.energy.EngineWood".equals(engine.getClass().getName());
    }

    private static TileEntity neighbor(TileEntity hopper, int face) {
        if (face < 0 || face >= 6) return null;
        return LoadedTile.at(hopper.worldObj, hopper.xCoord + TileHopper.DX[face],
                hopper.yCoord + TileHopper.DY[face], hopper.zCoord + TileHopper.DZ[face]);
    }
}
