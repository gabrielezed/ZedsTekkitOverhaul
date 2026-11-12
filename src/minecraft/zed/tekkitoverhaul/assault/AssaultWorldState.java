package zed.tekkitoverhaul.assault;

import java.io.File;
import net.minecraft.src.NBTTagByte;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagLong;
import net.minecraft.src.World;
import net.minecraft.src.WorldSavedData;

/** One high-water mark keyed by the vanilla night boundary, independent of phase adjustments. */
public final class AssaultWorldState extends WorldSavedData {
    private static final String KEY = "zto_player_assault";
    long consumed = Long.MIN_VALUE;
    boolean interrupted;
    private boolean valid;

    public AssaultWorldState(String name) { super(name); }

    static AssaultWorldState load(World world) {
        AssaultWorldState state = (AssaultWorldState) world.loadItemData(AssaultWorldState.class, KEY);
        if (state != null) return state.valid ? state : null;
        File file = world.getSaveHandler().getMapFileFromName(KEY);
        if (file == null || file.exists()) return null;
        state = new AssaultWorldState(KEY);
        state.valid = true;
        world.setItemData(KEY, state);
        return state;
    }

    void start(long boundary) {
        consumed = boundary;
        interrupted = true;
        markDirty();
    }

    void close() {
        interrupted = false;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        valid = tag.getTag("Boundary") instanceof NBTTagLong && tag.getTag("Open") instanceof NBTTagByte;
        if (valid) {
            consumed = tag.getLong("Boundary");
            interrupted = tag.getBoolean("Open");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setLong("Boundary", consumed);
        tag.setBoolean("Open", interrupted);
    }
}
