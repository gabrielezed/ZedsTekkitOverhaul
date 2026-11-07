package zed.tekkitoverhaul.distillation;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/** Boiler auxiliaries: electric heater, column, condenser, receiver and thermometer. */
public final class BlockDistillationPart extends BlockDistillationComponent {
    private final DistillationPartType part;

    public BlockDistillationPart(int id, DistillationPartType part) {
        super(id);
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
        return DistillationStructure.boilerFor(world, x, y, z, part);
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (part == DistillationPartType.COLUMN) return false;
        ItemStack held = player.inventory.getCurrentItem();
        if (held != null && !(held.stackSize == 1 &&
                ((part == DistillationPartType.CONDENSER && held.itemID == Item.bucketWater.shiftedIndex) ||
                 (part == DistillationPartType.RECEIVER && held.itemID == Item.bucketEmpty.shiftedIndex)))) {
            return false;
        }
        TileEntityDistillationBoiler boiler = getBoiler(world, x, y, z);
        if (part == DistillationPartType.HEATER) {
            TileEntity te = world.getBlockTileEntity(x, y, z);
            if (!world.isRemote && te instanceof TileEntityDistillationHeater)
                player.addChatMessage("[ZTO] LV heater: " +
                        ((TileEntityDistillationHeater)te).getStoredEnergy() + " / 4000 EU");
            return true;
        }
        if (boiler == null) {
            if (!world.isRemote) player.addChatMessage("[ZTO] No boiler linked to this " + part.displayName
                        + " at (" + x + ", " + y + ", " + z + "). Check assembly positions.");
            return true;
        }
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
