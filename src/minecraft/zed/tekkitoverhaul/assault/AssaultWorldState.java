package zed.tekkitoverhaul.assault;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.NBTTagString;
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
    private static final int MAX_COOLDOWNS = 128;
    private final Map<Integer, Long> cooldowns = new TreeMap<Integer, Long>();
    private boolean valid, siegeDataValid = true;
    int lastSite;
    long lastSiteBoundary = Long.MIN_VALUE;
    String siteOutcome = "NONE";

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

    String siegeEligibility(int site, long boundary) {
        if (!siegeDataValid) return "SITE_STORAGE_UNAVAILABLE";
        if (boundary <= consumed) return "BOUNDARY_CONSUMED";
        if (cooldownUntil(site) > boundary) return "SITE_COOLDOWN";
        int unexpired = 0;
        for (long until : cooldowns.values()) if (until > boundary) unexpired++;
        return unexpired >= MAX_COOLDOWNS ? "COOLDOWN_STORAGE_FULL" : null;
    }

    long cooldownUntil(int site) {
        Long stored = cooldowns.get(site);
        return stored == null ? Long.MIN_VALUE : stored;
    }

    void startSite(int site, long boundary) {
        Iterator<Long> entries = cooldowns.values().iterator();
        while (entries.hasNext()) if (entries.next() <= boundary) entries.remove();
        cooldowns.put(site, SiteSiegePolicy.cooldownUntil(boundary));
        lastSite = site;
        lastSiteBoundary = boundary;
        siteOutcome = "OPEN";
        start(boundary);
    }

    void closeSite(String outcome) {
        siteOutcome = outcome;
        close();
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
            siegeDataValid = readSieges(tag);
        }
    }

    private boolean readSieges(NBTTagCompound tag) {
        if (!tag.hasKey("SiteSiege")) return true;
        if (!(tag.getTag("SiteSiege") instanceof NBTTagCompound)) return false;
        NBTTagCompound siege = tag.getCompoundTag("SiteSiege");
        if (!(siege.getTag("Version") instanceof NBTTagInt) || siege.getInteger("Version") != 1
                || !(siege.getTag("Cooldowns") instanceof NBTTagList)
                || !(siege.getTag("LastSite") instanceof NBTTagInt)
                || !(siege.getTag("LastBoundary") instanceof NBTTagLong)
                || !(siege.getTag("Outcome") instanceof NBTTagString)) return false;
        NBTTagList rows = siege.getTagList("Cooldowns");
        int site = siege.getInteger("LastSite");
        long boundary = siege.getLong("LastBoundary");
        String outcome = siege.getString("Outcome");
        if (rows.tagCount() < 1 || rows.tagCount() > MAX_COOLDOWNS || site <= 0
                || boundary > consumed || !outcome.matches("[A-Z_]{2,48}")
                || ("OPEN".equals(outcome) && (!interrupted || boundary != consumed))) return false;
        Map<Integer, Long> loaded = new TreeMap<Integer, Long>();
        for (int i = 0; i < rows.tagCount(); i++) {
            if (!(rows.tagAt(i) instanceof NBTTagCompound)) return false;
            NBTTagCompound row = (NBTTagCompound) rows.tagAt(i);
            if (!(row.getTag("Site") instanceof NBTTagInt) || !(row.getTag("Until") instanceof NBTTagLong))
                return false;
            int id = row.getInteger("Site");
            long until = row.getLong("Until");
            if (id <= 0 || until > SiteSiegePolicy.cooldownUntil(boundary) || loaded.put(id, until) != null)
                return false;
        }
        if (!Long.valueOf(SiteSiegePolicy.cooldownUntil(boundary)).equals(loaded.get(site))) return false;
        cooldowns.putAll(loaded);
        lastSite = site;
        lastSiteBoundary = boundary;
        siteOutcome = outcome;
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setLong("Boundary", consumed);
        tag.setBoolean("Open", interrupted);
        if (!siegeDataValid) {
            // Preserve fail-closed siege admission on future saves while keeping ordinary assault available.
            tag.setInteger("SiteSiege", -1);
        } else if (lastSite > 0) {
            NBTTagCompound siege = new NBTTagCompound();
            siege.setInteger("Version", 1);
            siege.setInteger("LastSite", lastSite);
            siege.setLong("LastBoundary", lastSiteBoundary);
            siege.setString("Outcome", siteOutcome);
            NBTTagList rows = new NBTTagList();
            for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
                NBTTagCompound row = new NBTTagCompound();
                row.setInteger("Site", entry.getKey());
                row.setLong("Until", entry.getValue());
                rows.appendTag(row);
            }
            siege.setTag("Cooldowns", rows);
            tag.setCompoundTag("SiteSiege", siege);
        }
    }
}
