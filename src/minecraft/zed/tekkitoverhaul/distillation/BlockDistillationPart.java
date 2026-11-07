package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/** Boiler auxiliaries: electric heater, column, condenser, receiver and thermometer. */
public final class BlockDistillationPart extends BlockContainer {
    private final DistillationPartType part;

    public BlockDistillationPart(int id, DistillationPartType part) {
        super(id, Block.blockSteel.blockIndexInTexture, Material.iron);
        this.part = part;
        setHardness(3.0F);
        setResistance(8.0F);
        setStepSound(soundMetalFootstep);
        setBlockName("ztoDistill" + part.modelName);
    }

    @Override
    public TileEntity getBlockEntity() {
        return part == DistillationPartType.HEATER ?
                new TileEntityDistillationHeater() : new TileEntityDistillationPart();
    }

    @Override
    public int getRenderType() { return -1; }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    private TileEntityDistillationBoiler getBoiler(World world, int x, int y, int z) {
        int bx = x, by = y, bz = z;
        if (part == DistillationPartType.THERMOMETER) bx = x - 1;
        else if (part == DistillationPartType.CONDENSER) { bx = x - 1; by = y - 2; }
        else if (part == DistillationPartType.RECEIVER) { bx = x - 1; by = y - 1; }
        else if (part == DistillationPartType.HEATER) by = y + 1;
        else if (part == DistillationPartType.COLUMN) {
            TileEntity upper = world.getBlockTileEntity(x, y + 1, z);
            if (upper instanceof TileEntityDistillationBoiler) by = y + 1;
            else {
                TileEntity lower = world.getBlockTileEntity(x, y - 1, z);
                if (lower instanceof TileEntityDistillationBoiler) by = y - 1;
                else by = y - 2;
            }
        }
        TileEntity tile = world.getBlockTileEntity(bx, by, bz);
        return tile instanceof TileEntityDistillationBoiler ? (TileEntityDistillationBoiler)tile : null;
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (part == DistillationPartType.COLUMN) return false;
        TileEntityDistillationBoiler boiler = getBoiler(world, x, y, z);
        if (part == DistillationPartType.HEATER) {
            TileEntity te = world.getBlockTileEntity(x, y, z);
            if (!world.isRemote && te instanceof TileEntityDistillationHeater)
                player.addChatMessage("[ZTO] LV heater: " +
                        ((TileEntityDistillationHeater)te).getStoredEnergy() + " / 4000 EU");
            return true;
        }
        if (boiler == null) {
            if (!world.isRemote) player.addChatMessage("[ZTO] No matching distillation boiler");
            return true;
        }
        ItemStack held = player.inventory.getCurrentItem();
        if (part == DistillationPartType.CONDENSER) {
            if (held != null && held.stackSize == 1 && held.itemID == Item.bucketWater.shiftedIndex) {
                if (!world.isRemote && boiler.getProcess().getCoolantMB() <=
                        FractionationProcess.TANK_CAPACITY_MB - 1000 && boiler.addCoolingWater(1000) == 1000) {
                    player.inventory.setInventorySlotContents(player.inventory.currentItem,
                            new ItemStack(Item.bucketEmpty));
                    player.addChatMessage("[ZTO] Condenser: 1000 mB cooling water loaded");
                }
            } else if (!world.isRemote) {
                player.addChatMessage("[ZTO] Cooling water: " + boiler.getProcess().getCoolantMB() + " mB");
            }
            return true;
        }
        if (part == DistillationPartType.RECEIVER) {
            if (held != null && held.stackSize == 1 && held.itemID == Item.bucketEmpty.shiftedIndex) {
                int outId = boiler.getProcess().getOutputTypeId();
                if (outId >= 0 && boiler.getProcess().getDistillateMB() >= 1000 && !world.isRemote) {
                    Item product = DistillationRegistry.bucketFor(outId);
                    if (product != null && boiler.takeDistillate(1000) == 1000) {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem,
                                new ItemStack(product));
                        player.addChatMessage("[ZTO] Collected 1000 mB " +
                                DistillateType.fromId(outId).displayName);
                    }
                }
            } else if (!world.isRemote) {
                player.addChatMessage("[ZTO] Receiver: " +
                        boiler.getProcess().getDistillateMB() + " / 8000 mB");
            }
            return true;
        }
        if (part == DistillationPartType.THERMOMETER) {
            ModLoader.openGUI(player, new GuiDistillationThermometer(boiler));
            return true;
        }
        return false;
    }
}
