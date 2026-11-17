package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.World;

public final class EntityThief extends EnderkinPrototype {
    public EntityThief(World world) {
        super(world, 0.72F, 1.95F, "thief", 0.32F);
    }

    @Override
    public int getMaxHealth() { return 16; }

    @Override
    public float getEyeHeight() { return 1.66F; }
}
