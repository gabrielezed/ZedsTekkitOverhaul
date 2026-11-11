package zed.tekkitoverhaul.devbridge;

import buildcraft.api.Orientations;
import ic2.api.Direction;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationLayout;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.FractionationProcess;
import zed.tekkitoverhaul.distillation.LvHeaterBuffer;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;
import zed.tekkitoverhaul.distillation.TileEntityDistillationCondenser;
import zed.tekkitoverhaul.distillation.TileEntityDistillationHeater;
import zed.tekkitoverhaul.distillation.TileEntityDistillationReceiver;
import zed.tekkitoverhaul.fluids.PetroleumFluids;

/** One fixed gasoline batch using the normal tile endpoints and world ticks. */
final class DistillationTestFixture {
    private static final DistillationLayout.Facing FACING = DistillationLayout.Facing.EAST;
    private final int[][] cells = new int[7][4];
    private final TileEntity[] tiles = new TileEntity[cells.length];
    private boolean started;

    DistillationTestFixture(int x, int y, int z) throws BridgeProtocol.Error {
        setCell(0, x, y, z, DistillationPartType.BOILER);
        for (int i = 1; i < cells.length; i++) {
            DistillationLayout.Placement p = DistillationLayout.placement(x, y, z, FACING, i - 1);
            setCell(i, p.x, p.y, p.z, p.part);
        }
    }

    private void setCell(int i, int x, int y, int z, DistillationPartType part) throws BridgeProtocol.Error {
        int id = DistillationRegistry.blockId(part);
        if (id <= 0) throw error("UNSUPPORTED", "Distillation is unavailable");
        cells[i] = new int[] {x, y, z, id};
    }

    void place(World world, Chunk chunk) throws BridgeProtocol.Error {
        for (int i = 0; i < cells.length; i++) {
            int[] p = cells[i];
            if (!world.setBlockAndMetadataWithNotify(p[0], p[1], p[2], p[3], FACING.frontSide)) {
                throw error("FIXTURE_SETUP_FAILED", "A distillation component could not be placed");
            }
            tiles[i] = BridgeSnapshots.tile(chunk, p[0], p[1], p[2]);
            if (tiles[i] == null) throw error("FIXTURE_SETUP_FAILED", "A distillation tile was not created");
        }
    }

    void validate(Chunk chunk) throws BridgeProtocol.Error {
        for (int i = 0; i < cells.length; i++) {
            int[] p = cells[i];
            if (chunk.getBlockID(p[0] & 15, p[1], p[2] & 15) != p[3]
                    || chunk.getBlockMetadata(p[0] & 15, p[1], p[2] & 15) != FACING.frontSide
                    || BridgeSnapshots.tile(chunk, p[0], p[1], p[2]) != tiles[i]) {
                throw error("ARENA_CHANGED", "Distillation fixture ownership changed");
            }
        }
    }

    void remove(World world) throws BridgeProtocol.Error {
        for (int i = cells.length - 1; i >= 0; i--) {
            int[] p = cells[i];
            if (!world.setBlockWithNotify(p[0], p[1], p[2], 0)) {
                throw error("DIRTY_FIXTURE", "Distillation fixture removal did not complete");
            }
        }
    }

    String start(boolean partial) throws BridgeProtocol.Error {
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler) tiles[0];
        TileEntityDistillationHeater heater = (TileEntityDistillationHeater) tiles[1];
        TileEntityDistillationCondenser condenser = (TileEntityDistillationCondenser) tiles[4];
        if (started || !boiler.isFormed() || !heater.isAddedToEnergyNet()) {
            throw error("FIXTURE_NOT_READY", "A fresh formed fixture with an online heater is required");
        }
        int oilId;
        try {
            oilId = ((Block) Class.forName("BuildCraftEnergy").getField("oilStill").get(null)).blockID;
        } catch (ReflectiveOperationException failure) {
            throw error("UNSUPPORTED", "BuildCraft oil ID is unavailable");
        }
        started = true;
        int crude = boiler.fill(Orientations.XNeg, FractionationProcess.BATCH_INPUT_MB, oilId, true);
        int water = condenser.fill(Orientations.YPos, DistillateType.GASOLINE.coolantPerBatchMB, Block.waterStill.blockID, true);
        int ticks = DistillateType.GASOLINE.durationTicks;
        if (partial) ticks /= 2;
        int remaining = ticks * FractionationProcess.EU_PER_TICK;
        while (remaining > 0) {
            int packet = Math.min(remaining, LvHeaterBuffer.MAX_PACKET_EU);
            if (heater.injectEnergy(Direction.XN, packet) != 0) {
                throw error("INPUT_REJECTED", "The LV heater refused a batch packet");
            }
            remaining -= packet;
        }
        return ",\"crudeMB\":" + crude + ",\"coolantMB\":" + water + ",\"storedEU\":" + heater.getStoredEnergy()
                + ",\"poweredTicks\":" + ticks;
    }

    String collect() throws BridgeProtocol.Error {
        if (!started) throw error("FIXTURE_NOT_READY", "Start the distillation batch before collection");
        TileEntityDistillationReceiver receiver = (TileEntityDistillationReceiver) tiles[5];
        int liquid = receiver.getLiquidId();
        int drained = receiver.empty(FractionationProcess.BATCH_OUTPUT_MB, true);
        return ",\"liquidId\":" + liquid + ",\"gasolineId\":" + PetroleumFluids.stillId(DistillateType.GASOLINE)
                + ",\"drainedMB\":" + drained + ",\"remainingMB\":" + receiver.getLiquidQuantity();
    }

    private static BridgeProtocol.Error error(String code, String message) {
        return new BridgeProtocol.Error(409, code, message);
    }
}
