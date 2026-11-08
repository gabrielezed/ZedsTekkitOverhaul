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
        super(id, DistillationPartType.BOILER);
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
        if (held != null && (held.stackSize != 1 || !DistillationRegistry.isBuildCraftOilBucket(held))) return false;
        if (!world.isRemote) {
            String result = null;
            if (held != null) {
                result = "Not enough space for an oil bucket.";
                if (boiler.getProcess().getCrudeMB() <= FractionationProcess.TANK_CAPACITY_MB - 1000
                        && boiler.addCrude(1000) == 1000) {
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Item.bucketEmpty));
                    result = "Loaded 1000 mB of crude oil.";
                }
            }
            boiler.refreshStructure();
            ModLoader.openGUI(player, new GuiDistillationPanel(DistillationPartType.BOILER, boiler, boiler, result));
        }
        return true;
    }
}
