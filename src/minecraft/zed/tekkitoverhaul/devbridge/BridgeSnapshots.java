package zed.tekkitoverhaul.devbridge;

import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.ChunkProvider;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import zed.tekkitoverhaul.distillation.FractionationProcess;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;
import zed.tekkitoverhaul.distillation.TileEntityDistillationHeater;
import zed.tekkitoverhaul.hopper.TileHopper;

/** Copies allowlisted state on the game thread without creating tiles or loading chunks. */
final class BridgeSnapshots {
    private BridgeSnapshots() {}

    static Chunk loaded(World world, int x, int z) throws BridgeProtocol.Error {
        IChunkProvider provider = world.getChunkProvider();
        if (provider.getClass() != ChunkProvider.class) {
            throw new BridgeProtocol.Error(409, "UNSUPPORTED", "The active chunk provider has no loaded-only adapter");
        }
        if (!provider.chunkExists(x >> 4, z >> 4)) {
            throw new BridgeProtocol.Error(404, "CHUNK_NOT_LOADED", "The target chunk is not loaded");
        }
        return provider.provideChunk(x >> 4, z >> 4);
    }

    static TileEntity tile(Chunk chunk, int x, int y, int z) {
        TileEntity tile = (TileEntity) chunk.chunkTileEntityMap.get(new ChunkPosition(x & 15, y, z & 15));
        return tile == null || tile.isInvalid() ? null : tile;
    }

    static String player(InventoryPlayer inventory, long worldId) {
        return "{\"worldId\":" + worldId + ",\"selectedSlot\":" + inventory.currentItem
                + ",\"slots\":" + slots(inventory, 40) + ",\"cursor\":" + stack(inventory.getItemStack()) + "}";
    }

    static String inspect(World world, BridgeProtocol.Query query, long simulationTicks) throws BridgeProtocol.Error {
        Chunk chunk = loaded(world, query.x, query.z);
        TileEntity tile = tile(chunk, query.x, query.y, query.z);
        String prefix = "{\"worldId\":" + query.worldId + ",\"dimension\":" + query.dimension
                + ",\"x\":" + query.x + ",\"y\":" + query.y + ",\"z\":" + query.z
                + ",\"loaded\":true,\"simulationTicks\":" + simulationTicks;
        if (tile instanceof TileHopper) {
            TileHopper hopper = (TileHopper) tile;
            String state = "";
            if ("machine.inspect".equals(query.op)) {
                // Indirect power can read one block beyond each adjacent block.
                for (int x = query.x - 2; x <= query.x + 2; x += 4) {
                    for (int z = query.z - 2; z <= query.z + 2; z += 4) loaded(world, x, z);
                }
                int face = chunk.getBlockMetadata(query.x & 15, query.y, query.z & 15) & 7;
                state = ",\"facing\":" + (face == 1 || face > 5 ? 0 : face)
                        + ",\"locked\":" + hopper.isLocked() + ",\"outputMode\":\"" + hopper.outputModeName() + "\"";
            }
            return prefix + ",\"type\":\"hopper\",\"stackLimit\":64,\"slots\":" + slots(hopper, 5) + state + "}";
        }
        if ("inventory.container".equals(query.op) && tile != null && tile.getClass() == TileEntityChest.class) {
            return prefix + ",\"type\":\"chest\",\"stackLimit\":64,\"slots\":" + slots((IInventory) tile, 27) + "}";
        }
        if ("machine.inspect".equals(query.op) && tile != null) {
            // Guard by exact class name before linking any optional integration type.
            String className = tile.getClass().getName();
            if ("zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler".equals(className)) {
                return boiler(prefix, (TileEntityDistillationBoiler) tile);
            }
            if ("zed.tekkitoverhaul.distillation.TileEntityDistillationHeater".equals(className)) {
                TileEntityDistillationHeater heater = (TileEntityDistillationHeater) tile;
                return prefix + ",\"type\":\"distillation.heater\",\"storedEU\":" + heater.getStoredEnergy()
                        + ",\"energyNetJoined\":" + heater.isAddedToEnergyNet() + "}";
            }
        }
        throw new BridgeProtocol.Error(409, "UNSUPPORTED", "No snapshot adapter for the loaded tile");
    }

    private static String boiler(String prefix, TileEntityDistillationBoiler boiler) {
            FractionationProcess process = boiler.getProcess();
            return prefix + ",\"type\":\"distillation.boiler\",\"formed\":" + boiler.isFormed()
                    + ",\"missingParts\":" + boiler.getMissingParts() + ",\"crudeMB\":" + process.getCrudeMB()
                    + ",\"coolantMB\":" + process.getCoolantMB() + ",\"distillateMB\":" + process.getDistillateMB()
                    + ",\"tankCapacityMB\":" + FractionationProcess.TANK_CAPACITY_MB
                    + ",\"fractionId\":" + process.getSelectedTypeId() + ",\"outputTypeId\":" + process.getOutputTypeId()
                    + ",\"progressTicks\":" + process.getProgressTicks() + "}";
    }

    private static String slots(IInventory inventory, int limit) {
        StringBuilder result = new StringBuilder("[");
        for (int slot = 0; slot < Math.min(inventory.getSizeInventory(), limit); slot++) {
            if (slot > 0) result.append(',');
            result.append(stack(inventory.getStackInSlot(slot)));
        }
        return result.append(']').toString();
    }

    private static String stack(ItemStack stack) {
        if (stack == null) return "null";
        return "{\"itemId\":" + stack.itemID + ",\"damage\":" + stack.getItemDamage()
                + ",\"count\":" + stack.stackSize + ",\"tagged\":" + stack.hasTagCompound() + "}";
    }
}
