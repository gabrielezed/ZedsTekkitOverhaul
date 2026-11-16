package zed.tekkitoverhaul.territory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.src.NBTBase;
import java.util.Map;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.NBTTagLong;
import net.minecraft.src.World;
import net.minecraft.src.WorldSavedData;

public final class TerritorialWorldState extends WorldSavedData {
    static final String KEY = "zto_territorial_activity";
    private static final int VERSION = 1;
    private static final int CHECKPOINT = 1200;
    private static final int MAX_FILE_BYTES = 65536;
    final TerritorialActivity activity = new TerritorialActivity();
    private long checkpoint;
    String status = "UNREADABLE";

    public TerritorialWorldState(String name) { super(name); }

    static TerritorialWorldState load(World world) {
        File file = world.getSaveHandler().getMapFileFromName(KEY);
        TerritorialWorldState state = new TerritorialWorldState(KEY);
        if (file == null) return state;
        Path path = file.toPath();
        if (Files.notExists(path)) {
            state.status = "READY";
        } else {
            try {
                if (Files.size(path) > MAX_FILE_BYTES) return state;
                InputStream input = Files.newInputStream(path);
                try {
                    NBTBase data = TerritorialNbtInput.read(input).getTag("data");
                    if (data instanceof NBTTagCompound) state.readFromNBT((NBTTagCompound) data);
                } finally {
                    input.close();
                }
            } catch (IOException unreadable) {
                return state;
            }
        }
        if ("READY".equals(state.status)) state.attach(world);
        return state;
    }

    void attach(World world) { world.setItemData(KEY, this); }

    void checkpoint(boolean changed) {
        if (changed || (!activity.records.isEmpty() && activity.clock - checkpoint >= CHECKPOINT)) {
            markDirty();
            checkpoint = activity.clock;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        status = "UNREADABLE";
        if (!(tag.getTag("Schema") instanceof NBTTagInt)) return;
        if (tag.getInteger("Schema") != VERSION) {
            status = "UNSUPPORTED_SCHEMA";
            return;
        }
        if (!(tag.getTag("Grid") instanceof NBTTagInt) || tag.getInteger("Grid") != VERSION
                || !(tag.getTag("Dimension") instanceof NBTTagInt) || tag.getInteger("Dimension") != 0
                || !(tag.getTag("Clock") instanceof NBTTagLong)
                || !(tag.getTag("Regions") instanceof NBTTagList)) return;
        long clock = tag.getLong("Clock");
        NBTTagList list = tag.getTagList("Regions");
        if (clock < 0 || list.tagCount() > TerritorialActivity.CAPACITY) return;

        TerritorialActivity decoded = new TerritorialActivity();
        long previousSeen = -1;
        for (int index = 0; index < list.tagCount(); index++) {
            if (!(list.tagAt(index) instanceof NBTTagCompound)) return;
            NBTTagCompound row = (NBTTagCompound) list.tagAt(index);
            if (!(row.getTag("X") instanceof NBTTagInt) || !(row.getTag("Z") instanceof NBTTagInt)
                    || !(row.getTag("Presence") instanceof NBTTagInt) || !(row.getTag("Returns") instanceof NBTTagInt)
                    || !(row.getTag("Seen") instanceof NBTTagLong)) return;
            int x = row.getInteger("X"), z = row.getInteger("Z");
            int min = TerritorialActivity.cell(-TerritorialActivity.WORLD_LIMIT);
            int max = TerritorialActivity.cell(TerritorialActivity.WORLD_LIMIT - 1);
            if (x < min || x > max || z < min || z > max) return;
            TerritorialActivity.Record record = new TerritorialActivity.Record();
            record.presence = row.getInteger("Presence");
            record.returns = row.getInteger("Returns");
            record.seen = row.getLong("Seen");
            if (record.presence < 1 || record.presence > TerritorialActivity.PRESENCE_MAX
                    || record.returns < 0 || record.returns > TerritorialActivity.RETURN_MAX
                    || record.seen < 0 || record.seen < previousSeen || record.seen > clock
                    || decoded.records.put(TerritorialActivity.key(x, z), record) != null) return;
            previousSeen = record.seen;
        }
        activity.records.clear();
        activity.records.putAll(decoded.records);
        activity.clock = checkpoint = clock;
        activity.clearVisit();
        status = "READY";
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Schema", VERSION);
        tag.setInteger("Grid", VERSION);
        tag.setInteger("Dimension", 0);
        tag.setLong("Clock", activity.clock);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Long, TerritorialActivity.Record> entry : activity.records.entrySet()) {
            TerritorialActivity.Record record = entry.getValue();
            NBTTagCompound row = new NBTTagCompound();
            row.setInteger("X", TerritorialActivity.x(entry.getKey()));
            row.setInteger("Z", TerritorialActivity.z(entry.getKey()));
            row.setInteger("Presence", record.presence);
            row.setInteger("Returns", record.returns);
            row.setLong("Seen", record.seen);
            list.appendTag(row);
        }
        tag.setTag("Regions", list);
    }
}
