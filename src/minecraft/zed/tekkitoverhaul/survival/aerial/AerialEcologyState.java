package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.WorldSavedData;

/** The quiet period belongs to the save and advances only in active Overworld play. */
public final class AerialEcologyState extends WorldSavedData {
    private static final String KEY = "zto_aerial_ecology";
    int cooldownSeconds;

    public AerialEcologyState(String name) { super(name); }

    static AerialEcologyState load(World world) {
        AerialEcologyState state = (AerialEcologyState) world.loadItemData(AerialEcologyState.class, KEY);
        if (state == null) {
            state = new AerialEcologyState(KEY);
            world.setItemData(KEY, state);
        }
        return state;
    }

    void reserve(int seconds) {
        cooldownSeconds = Math.max(cooldownSeconds, seconds);
        markDirty();
    }

    void elapseSecond() {
        if (cooldownSeconds > 0) {
            --cooldownSeconds;
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        cooldownSeconds = Math.max(0, Math.min(600, tag.getInteger("CooldownSeconds")));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) { tag.setInteger("CooldownSeconds", cooldownSeconds); }
}
