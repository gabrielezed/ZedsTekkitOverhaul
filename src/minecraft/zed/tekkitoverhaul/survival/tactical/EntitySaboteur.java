package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.World;

/** Small Enderkin available through its spawn egg. */
public final class EntitySaboteur extends TacticalHostile {
    public static final byte IDLE = 0, APPROACH = 1, WINDUP = 2, RECOVER = 3, CANCELLED = 4;
    private static final int POSE = 20;

    public EntitySaboteur(World world) {
        super(world, 0.45F, 1.1F, "saboteur");
        attackStrength = 1;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(POSE, Byte.valueOf(IDLE));
    }

    public byte pose() { return dataWatcher.getWatchableObjectByte(POSE); }

    private void pose(byte value) { dataWatcher.updateObject(POSE, Byte.valueOf(value)); }

    @Override
    public int getMaxHealth() { return 12; }

    @Override
    protected int unseenLimit() { return 60; }

    @Override
    protected void fight(double distanceSq, boolean visible) {}

    @Override
    protected void resetTactics() { pose(IDLE); }
}
