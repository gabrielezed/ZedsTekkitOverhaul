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
        super(id, part);
        this.part = part;
        setHardness(3.0F);
        setResistance(8.0F);
        setStepSound(soundMetalFootstep);
        setBlockName("ztoDistill" + part.modelName);
    }

    @Override
    public TileEntity getBlockEntity() {
        if (part == DistillationPartType.HEATER) return new TileEntityDistillationHeater();
        if (part == DistillationPartType.RECEIVER) return new TileEntityDistillationReceiver();
        if (part == DistillationPartType.CONDENSER) return new TileEntityDistillationCondenser();
        return new TileEntityDistillationPart();
    }

    @Override
    public int getRenderType() { return -1; }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        ItemStack held = player.inventory.getCurrentItem();
        if (held != null && !(held.stackSize == 1 &&
                ((part == DistillationPartType.CONDENSER && held.itemID == Item.bucketWater.shiftedIndex) ||
                 (part == DistillationPartType.RECEIVER && held.itemID == Item.bucketEmpty.shiftedIndex)))) {
            return false;
        }
        if (world.isRemote) return true;
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (!(tile instanceof TileEntityDistillationPart)) return false;
        TileEntityDistillationBoiler boiler = DistillationStructure.boilerFor(world, x, y, z, part);
        String result = null;
        if (boiler != null && held != null) {
            result = part == DistillationPartType.CONDENSER
                    ? loadCoolingWater(boiler, player) : collectDistillate(boiler, player);
        }
        ModLoader.openGUI(player, new GuiDistillationPanel(part, tile, boiler, result));
        return true;
    }

    private String loadCoolingWater(TileEntityDistillationBoiler boiler, EntityPlayer player) {
        if (boiler.getProcess().getCoolantMB() > FractionationProcess.TANK_CAPACITY_MB - 1000
                || boiler.addCoolingWater(1000) != 1000) {
            return "Not enough space for a water bucket.";
        }
        player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Item.bucketEmpty));
        return "Loaded 1000 mB of cooling water.";
    }

    private String collectDistillate(TileEntityDistillationBoiler boiler, EntityPlayer player) {
        int outId = boiler.getProcess().getOutputTypeId();
        if (outId < 0 || boiler.getProcess().getDistillateMB() < 1000) {
            return "At least 1000 mB is required to fill a bucket.";
        }
        Item product = DistillationRegistry.bucketFor(outId);
        if (product == null || boiler.takeDistillate(1000) != 1000) {
            return "The stored fraction could not be collected.";
        }
        player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(product));
        return "Collected 1000 mB of " + DistillateType.fromId(outId).displayName + ".";
    }
}
