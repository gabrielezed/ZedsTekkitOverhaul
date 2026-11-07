package zed.tekkitoverhaul.distillation;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

/**
 * Controller and crude intake: hold a BuildCraft oil bucket and right-click,
 * or right-click empty-handed to inspect the apparatus.
 */
public final class BlockDistillationBoiler extends BlockDistillationComponent {
    public BlockDistillationBoiler(int id) {
        super(id);
        setHardness(4.0F);
        setResistance(10.0F);
        setStepSound(soundMetalFootstep);
        setBlockName("ztoDistillationBoiler");
    }

    @Override
    public TileEntity getBlockEntity() {
        return new TileEntityDistillationBoiler();
    }

    @Override
    public int getRenderType() { return -1; }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (!(te instanceof TileEntityDistillationBoiler)) return false;
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler)te;
        ItemStack held = player.inventory.getCurrentItem();
        if (held != null && held.stackSize == 1 && DistillationRegistry.isBuildCraftOilBucket(held)) {
            if (!world.isRemote && boiler.getProcess().getCrudeMB() <=
                    FractionationProcess.TANK_CAPACITY_MB - 1000 && boiler.addCrude(1000) == 1000) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem,
                        new ItemStack(Item.bucketEmpty));
                player.addChatMessage("[ZTO] Boiler: 1000 mB crude oil loaded");
            }
            return true;
        }
        if (held != null) return false;
        if (!world.isRemote) {
            boiler.refreshStructure();
            player.addChatMessage("[ZTO] Distiller " + (boiler.isFormed() ?
                    "FORMED" : "INCOMPLETE (" + boiler.getMissingParts() + " parts missing)")
                    + " facing " + boiler.getVisualFacing().name());
            if (!boiler.isFormed()) {
                player.addChatMessage("[ZTO] Missing: " + boiler.getStructureDetails());
            }
            player.addChatMessage("[ZTO] Crude: " + boiler.getProcess().getCrudeMB()
                    + " mB; Cooling water: " + boiler.getProcess().getCoolantMB() + " mB");
            player.addChatMessage("[ZTO] Process: " + boiler.getProcessStatus()
                    + " (setpoint " + boiler.getProcess().selectedType().temperatureCelsius
                    + " C; progress " + boiler.getProcess().getProgressTicks() + " ticks)");
        }
        return true;
    }
}
