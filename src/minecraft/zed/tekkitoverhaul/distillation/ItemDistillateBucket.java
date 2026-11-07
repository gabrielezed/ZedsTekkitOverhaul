package zed.tekkitoverhaul.distillation;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemBucket;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import zed.tekkitoverhaul.fluids.GasolineFluid;

/** A 1000 mB container, placeable once its world liquid is registered. */
public final class ItemDistillateBucket extends ItemBucket {
    private final DistillateType fluid;

    public ItemDistillateBucket(int id, DistillateType fluid) {
        super(id, fluid == DistillateType.GASOLINE ? GasolineFluid.flowingId() : -1);
        this.fluid = fluid;
        setMaxStackSize(1);
        setItemName("zto" + fluid.name().replace("_", "") + "Bucket");
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return fluid == DistillateType.GASOLINE ? super.onItemRightClick(stack, world, player) : stack;
    }

    public DistillateType getFluid() {
        return fluid;
    }
}
