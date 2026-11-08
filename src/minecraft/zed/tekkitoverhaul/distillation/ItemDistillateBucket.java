package zed.tekkitoverhaul.distillation;

import net.minecraft.src.ItemBucket;
import zed.tekkitoverhaul.fluids.PetroleumFluids;

/** A placeable 1000 mB petroleum bucket. */
public final class ItemDistillateBucket extends ItemBucket {
    private final DistillateType fluid;

    public ItemDistillateBucket(int id, DistillateType fluid) {
        super(id, PetroleumFluids.flowingId(fluid));
        this.fluid = fluid;
        setMaxStackSize(1);
        setItemName("zto" + fluid.name().replace("_", "") + "Bucket");
    }
    public DistillateType getFluid() {
        return fluid;
    }
}
