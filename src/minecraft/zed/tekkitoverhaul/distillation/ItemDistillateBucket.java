package zed.tekkitoverhaul.distillation;

import net.minecraft.src.Item;

/** A sealed 1000 mB sample container for one legacy internal distillate type. */
public final class ItemDistillateBucket extends Item {
    private final DistillateType fluid;

    public ItemDistillateBucket(int id, DistillateType fluid) {
        super(id);
        this.fluid = fluid;
        setMaxStackSize(1);
        setItemName("zto" + fluid.name().replace("_", "") + "Bucket");
    }

    public DistillateType getFluid() {
        return fluid;
    }
}
