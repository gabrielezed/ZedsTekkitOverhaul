package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.World;

public final class EntityThief extends EnderkinPrototype {
    private int nextBlink;

    public EntityThief(World world) {
        super(world, 0.72F, 1.95F, "thief", 0.32F);
    }

    @Override
    public int getMaxHealth() { return 16; }

    @Override
    public float getEyeHeight() { return 1.66F; }

    @Override
    protected boolean reposition(double distanceSq, boolean visible) {
        if (!visible || distanceSq > 36 || ticksExisted < nextBlink || (hurtTime == 0 && attackTime < 12))
            return false;
        double away = Math.atan2(posZ - target.posZ, posX - target.posX);
        double flank = (entityId & 1) == 0 ? 0.90D : -0.90D;
        boolean escaped = EnderkinBlink.shift(this, away + flank, 3.8D, 4, 1.55F)
                || EnderkinBlink.shift(this, away - flank, 3.4D, 4, 1.55F);
        nextBlink = ticksExisted + (escaped ? 68 : 10);
        return escaped;
    }
}
