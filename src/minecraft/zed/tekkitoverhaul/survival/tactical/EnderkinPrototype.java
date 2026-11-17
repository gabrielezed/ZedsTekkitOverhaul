package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.World;

/** Short walks and ordinary melee for inspecting Creative-spawned prototypes. */
abstract class EnderkinPrototype extends TacticalHostile {
    private final float speed;
    private int nextRoute;

    EnderkinPrototype(World world, float width, float height, String skin, float speed) {
        super(world, width, height, skin);
        this.speed = speed;
    }

    @Override
    protected final void idle() {
        if (!getNavigator().noPath() || ticksExisted < nextRoute) return;
        nextRoute = ticksExisted + 45 + rand.nextInt(40);
        getNavigator().setPath(plan(posX + rand.nextInt(9) - 4, boundingBox.minY,
                posZ + rand.nextInt(9) - 4), speed);
    }

    @Override
    protected final void fight(double distanceSq, boolean visible) {
        double reach = 1.5D + width * 0.5D;
        if (visible && distanceSq < reach * reach && target.boundingBox.maxY > boundingBox.minY
                && target.boundingBox.minY < boundingBox.maxY) {
            getNavigator().clearPathEntity();
            if (attackTime == 0) {
                attackTime = 20;
                swing();
                attackEntityAsMob(target);
            }
        } else if (ticksExisted >= nextRoute) {
            nextRoute = ticksExisted + 20;
            getNavigator().setPath(plan(target.posX, target.boundingBox.minY, target.posZ), speed);
        }
    }

    @Override
    protected final int unseenLimit() { return 60; }

    @Override
    protected final void resetTactics() { nextRoute = ticksExisted; }
}
