package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.NBTTagLong;
import net.minecraft.src.WorldSavedData;
import zed.tekkitoverhaul.lunar.LunarCalendar;

public final class LunarWorldState extends WorldSavedData {
    static final String KEY = "zto_lunar";
    private static final int SCHEMA = 1;

    long baseline;
    long adjustment;
    long watermark;
    boolean valid;
    boolean initializedHere;
    String status = "Unreadable lunar data";

    public LunarWorldState(String name) {
        super(name);
    }

    static LunarWorldState create(long alignment, long time, String status) {
        LunarWorldState state = new LunarWorldState(KEY);
        state.baseline = alignment;
        state.adjustment = alignment;
        state.watermark = time;
        state.status = status;
        state.valid = true;
        state.initializedHere = true;
        state.markDirty();
        return state;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if (!(tag.getTag("Schema") instanceof NBTTagInt)) return;
        int version = tag.getInteger("Schema");
        if (version > SCHEMA) {
            status = "Newer lunar schema " + version;
            return;
        }
        if (version < 0 || !(tag.getTag("Baseline") instanceof NBTTagLong)
                || !(tag.getTag("Adjustment") instanceof NBTTagLong)
                || (version == SCHEMA && !(tag.getTag("Watermark") instanceof NBTTagLong))) return;
        baseline = tag.getLong("Baseline");
        adjustment = tag.getLong("Adjustment");
        if (!LunarCalendar.validAdjustment(baseline) || !LunarCalendar.validAdjustment(adjustment)) return;
        // Schema 0 stored the two adjustments only. Binding consumes through the loaded clock.
        watermark = version == 0 ? Long.MIN_VALUE : tag.getLong("Watermark");
        status = version == 0 ? "Migrated lunar schema 0" : "Loaded lunar schema 1";
        valid = true;
        if (version == 0) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Schema", SCHEMA);
        tag.setLong("Baseline", baseline);
        tag.setLong("Adjustment", adjustment);
        tag.setLong("Watermark", watermark);
    }
}
