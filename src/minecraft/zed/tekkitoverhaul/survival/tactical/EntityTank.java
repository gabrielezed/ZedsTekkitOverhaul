package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.World;

public final class EntityTank extends EnderkinPrototype {
    public EntityTank(World world) {
        super(world, 1.85F, 3.15F, "tank", 0.19F);
    }

    @Override
    public int getMaxHealth() { return 22; }

    @Override
    public float getEyeHeight() { return 2.69F; }
}
