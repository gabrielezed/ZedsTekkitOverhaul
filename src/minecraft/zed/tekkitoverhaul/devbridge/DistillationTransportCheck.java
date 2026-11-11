package zed.tekkitoverhaul.devbridge;

import buildcraft.api.ILiquidContainer;
import buildcraft.api.Orientations;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.FractionationProcess;
import zed.tekkitoverhaul.fluids.PetroleumFluids;

/** Fixed physical connections inside the existing arena, left in the disposable copy for diagnosis. */
final class DistillationTransportCheck {
    private final Block tank, engine;
    private final Item pipe;
    private final ItemStack generator;
    private final int oil;

    DistillationTransportCheck() throws ReflectiveOperationException {
        tank = (Block) field("BuildCraftFactory", "tankBlock");
        engine = (Block) field("BuildCraftEnergy", "engineBlock");
        pipe = (Item) field("BuildCraftTransport", "pipeLiquidsWood");
        generator = (ItemStack) field("ic2.common.Ic2Items", "generator");
        oil = ((Block) field("BuildCraftEnergy", "oilStill")).blockID;
    }

    String place(World world, Chunk chunk, EntityPlayer player, int x, int y, int z)
            throws ReflectiveOperationException, BridgeProtocol.Error {
        // Tank -> powered wooden pipe -> boiler.
        TileEntity inlet = place(world, chunk, x - 2, y, z, tank.blockID, 0);
        placePipe(world, player, x - 2, y, z, Orientations.XPos, Orientations.XNeg);
        placeEngine(world, chunk, x - 1, y + 1, z, Orientations.YNeg);
        torch(world, chunk, x - 1, y + 1, z - 1);

        // Receiver -> powered wooden pipe -> tank.
        place(world, chunk, x + 1, y + 1, z + 2, tank.blockID, 0);
        placePipe(world, player, x + 1, y + 1, z, Orientations.ZPos, Orientations.ZNeg);
        placeEngine(world, chunk, x + 2, y + 1, z + 1, Orientations.XNeg);
        torch(world, chunk, x + 2, y + 1, z + 2);

        TileEntity source = place(world, chunk, x - 1, y - 1, z, generator.itemID, generator.getItemDamage());
        TileEntity condenser = BridgeSnapshots.tile(chunk, x + 1, y + 2, z);
        if (!source.getClass().getName().equals("ic2.common.TileEntityGenerator")
                || !(inlet instanceof ILiquidContainer) || !(condenser instanceof ILiquidContainer)) throw setupError();
        int crude = ((ILiquidContainer) inlet).fill(Orientations.YPos, FractionationProcess.BATCH_INPUT_MB, oil, true);
        int coolant = ((ILiquidContainer) condenser).fill(Orientations.YPos, DistillateType.GASOLINE.coolantPerBatchMB,
                Block.waterStill.blockID, true);
        if (crude != FractionationProcess.BATCH_INPUT_MB || coolant != DistillateType.GASOLINE.coolantPerBatchMB) {
            throw setupError();
        }
        ((IInventory) source).setInventorySlotContents(1, new ItemStack(Item.coal));
        return ",\"oilMB\":" + crude + ",\"coolantMB\":" + coolant
                + ",\"gasolineId\":" + PetroleumFluids.stillId(DistillateType.GASOLINE);
    }

    private void placePipe(World world, EntityPlayer player, int x, int y, int z,
                           Orientations side, Orientations intake) throws BridgeProtocol.Error {
        if (!pipe.onItemUse(new ItemStack(pipe), player, world, x, y, z, side.ordinal())) throw setupError();
        world.setBlockMetadataWithNotify(x + (side == Orientations.XPos ? 1 : 0), y,
                z + (side == Orientations.ZPos ? 1 : 0), intake.ordinal());
    }

    private void placeEngine(World world, Chunk chunk, int x, int y, int z, Orientations facing)
            throws ReflectiveOperationException, BridgeProtocol.Error {
        TileEntity tile = place(world, chunk, x, y, z, engine.blockID, 0);
        tile.getClass().getField("orientation").setInt(tile, facing.ordinal());
    }

    private static void torch(World world, Chunk chunk, int x, int y, int z) throws BridgeProtocol.Error {
        place(world, chunk, x, y - 1, z, Block.stone.blockID, 0);
        place(world, chunk, x, y, z, Block.torchRedstoneActive.blockID, 5);
    }

    private static TileEntity place(World world, Chunk chunk, int x, int y, int z, int id, int meta)
            throws BridgeProtocol.Error {
        if (!world.setBlockAndMetadataWithNotify(x, y, z, id, meta)) throw setupError();
        TileEntity tile = BridgeSnapshots.tile(chunk, x, y, z);
        if (Block.blocksList[id] instanceof net.minecraft.src.BlockContainer && tile == null) throw setupError();
        return tile;
    }

    private static Object field(String owner, String name) throws ReflectiveOperationException {
        Object value = Class.forName(owner).getField(name).get(null);
        if (value == null) throw new NoSuchFieldException(name);
        return value;
    }

    private static BridgeProtocol.Error setupError() {
        return new BridgeProtocol.Error(409, "FIXTURE_SETUP_FAILED", "Physical distillation fixture placement or seeding failed");
    }
}
