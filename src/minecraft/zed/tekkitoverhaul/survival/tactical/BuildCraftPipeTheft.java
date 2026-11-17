package zed.tekkitoverhaul.survival.tactical;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import zed.tekkitoverhaul.integration.LoadedTile;

/** Read-only BuildCraft ABI checks followed by the native block removal path. */
final class BuildCraftPipeTheft {
    private final TileEntity tile;
    private final Object pipe;
    private final int blockId, itemId;

    private BuildCraftPipeTheft(TileEntity tile, Object pipe, int blockId, int itemId) {
        this.tile = tile;
        this.pipe = pipe;
        this.blockId = blockId;
        this.itemId = itemId;
    }

    static BuildCraftPipeTheft find(TileEntity tile) {
        if (tile == null || !"buildcraft.transport.TileGenericPipe".equals(tile.getClass().getName())) return null;
        World world = tile.worldObj;
        int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
        if (!ThiefTrail.local(world) || !world.checkChunksExist(x - 1, y, z - 1, x + 1, y, z + 1)
                || LoadedTile.at(world, x, y, z) != tile) return null;
        int blockId = world.getBlockId(x, y, z);
        Block block = Block.blocksList[blockId];
        // Stock passive item pipes have block metadata zero and no logic inventory.
        if (block == null || !"buildcraft.transport.BlockGenericPipe".equals(block.getClass().getName())
                || world.getBlockMetadata(x, y, z) != 0) return null;
        try {
            Object pipe = field(tile, "pipe");
            if (pipe == null || field(pipe, "worldObj") != world || field(pipe, "container") != tile) return null;
            Class<?> type = pipe.getClass();
            String name = type.getName();
            if (!"buildcraft.transport.pipes.PipeItemsStone".equals(name)
                    && !"buildcraft.transport.pipes.PipeItemsCobblestone".equals(name)) return null;
            Object transport = field(pipe, "transport");
            if (transport == null || !"buildcraft.transport.PipeTransportItems".equals(transport.getClass().getName())
                    || !((Map<?, ?>) field(transport, "travelingEntities")).isEmpty()) return null;
            // BuildCraft restores NBT cargo into a separate queue until the next pipe update.
            Field pending = transport.getClass().getDeclaredField("entitiesToLoad");
            pending.setAccessible(true);
            if (!((Collection<?>) pending.get(transport)).isEmpty()) return null;
            int itemId = ((Number) field(pipe, "itemID")).intValue();
            if (itemId <= 0 || itemId >= Item.itemsList.length || Item.itemsList[itemId] == null
                    || !"buildcraft.transport.ItemPipe".equals(Item.itemsList[itemId].getClass().getName())
                    || ((Map<?, ?>) block.getClass().getField("pipes").get(null)).get(itemId) != type) return null;
            return new BuildCraftPipeTheft(tile, pipe, blockId, itemId);
        } catch (ReflectiveOperationException unsupported) {
            return null;
        } catch (LinkageError unsupported) {
            return null;
        }
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        return object.getClass().getField(name).get(object);
    }

    boolean stillEligible() {
        BuildCraftPipeTheft current = find(tile);
        return current != null && current.pipe == pipe && current.blockId == blockId && current.itemId == itemId;
    }

    ItemStack remove() {
        if (!stillEligible()) return null;
        // Chunk removal invokes BuildCraft's tile teardown and sends native neighbor notifications.
        // Do not also call the harvest/drop path, which would create a second pipe item.
        ItemStack component = new ItemStack(itemId, 1, 0);
        return tile.worldObj.setBlockWithNotify(tile.xCoord, tile.yCoord, tile.zCoord, 0) ? component : null;
    }
}
