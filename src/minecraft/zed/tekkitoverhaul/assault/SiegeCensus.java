package zed.tekkitoverhaul.assault;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.src.Chunk;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.NBTTagByte;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.NBTTagLong;
import net.minecraft.src.World;
import net.minecraft.src.WorldSavedData;

/** Recent siege records. The native chunk save remains authoritative for entity existence. */
public final class SiegeCensus extends WorldSavedData {
    private static final String KEY = "zto_siege_census";
    private static final int LIMIT = 256, VERSION = 1;
    private final Map<UUID, Record> recent = new LinkedHashMap<UUID, Record>();
    private boolean truncated;

    public SiegeCensus(String name) { super(name); }

    static SiegeCensus load(World world) {
        if (world == null || world.isRemote || world.worldProvider.worldType != 0) return null;
        SiegeCensus data = (SiegeCensus) world.loadItemData(SiegeCensus.class, KEY);
        if (data != null) return data;
        data = new SiegeCensus(KEY);
        world.setItemData(KEY, data);
        return data;
    }

    void inserted(EntityLiving mob, int site, UUID mobilization, boolean manual) {
        Actor actor = ((Participant) mob).siegeActor();
        if (actor.id != null) return;
        do { actor.id = UUID.randomUUID(); } while (recent.containsKey(actor.id));
        Record row = new Record(site, mobilization, manual);
        row.live = new WeakReference<EntityLiving>(mob);
        actor.reconciled = true;
        if (recent.size() == LIMIT) {
            Iterator<UUID> records = recent.keySet().iterator();
            records.next();
            records.remove();
            truncated = true;
        }
        recent.put(actor.id, row);
        markDirty();
    }

    void released(EntityLiving mob) {
        Record row = recent.get(((Participant) mob).siegeActor().id);
        if (row != null && !row.dead && !row.released && row.observed() == mob) {
            row.released = true;
            markDirty();
        }
    }

    private Record claim(EntityLiving mob, int site, UUID id) {
        Record row = recent.get(id);
        if (row == null || row.site != site || row.dead) return null;
        EntityLiving previous = row.observed();
        if (previous != null && previous != mob && previous.worldObj == mob.worldObj
                && previous.isEntityAlive() && previous.addedToChunk && chunkAvailable(previous)
                && presentInAvailableChunk(previous)) {
            if (!truncated) {
                truncated = true;
                markDirty();
            }
            return null;
        }
        if (previous != mob) row.live = new WeakReference<EntityLiving>(mob);
        return row;
    }

    private static boolean chunkAvailable(EntityLiving mob) {
        return mob.worldObj.blockExists(mob.chunkCoordX << 4, 0, mob.chunkCoordZ << 4);
    }

    private static boolean presentInAvailableChunk(EntityLiving mob) {
        Chunk chunk = mob.worldObj.getChunkFromChunkCoords(mob.chunkCoordX, mob.chunkCoordZ);
        int section = mob.chunkCoordY;
        return chunk.isChunkLoaded && section >= 0 && section < chunk.entityLists.length
                && chunk.entityLists[section].contains(mob);
    }

    Snapshot snapshot(World world, int site) {
        Snapshot result = new Snapshot(truncated);
        for (Record row : recent.values()) {
            if (row.site != site) continue;
            ++result.inserted;
            if (row.released) ++result.released;
            if (row.dead) {
                ++result.dead;
                continue;
            }
            EntityLiving mob = row.observed();
            if (mob == null || mob.worldObj != world || !mob.isEntityAlive() || !mob.addedToChunk) {
                ++result.unknown;
            } else if (!chunkAvailable(mob)) {
                ++result.unloaded;
            } else if (presentInAvailableChunk(mob)) ++result.loaded;
            else ++result.unknown;
        }
        return result;
    }

    int lastSite() {
        int site = 0;
        for (Record row : recent.values()) site = row.site;
        return site;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        recent.clear();
        truncated = tag.getTag("Truncated") instanceof NBTTagByte && tag.getBoolean("Truncated");
        if (!(tag.getTag("Version") instanceof NBTTagInt) || tag.getInteger("Version") != VERSION
                || !(tag.getTag("Records") instanceof NBTTagList)) {
            truncated = true;
            return;
        }
        NBTTagList rows = tag.getTagList("Records");
        truncated |= rows.tagCount() > LIMIT;
        for (int i = Math.max(0, rows.tagCount() - LIMIT); i < rows.tagCount(); ++i) {
            if (!(rows.tagAt(i) instanceof NBTTagCompound)) {
                truncated = true;
                continue;
            }
            NBTTagCompound saved = (NBTTagCompound) rows.tagAt(i);
            UUID id = readId(saved, "Actor"), mobilization = readId(saved, "Encounter");
            int site = saved.getTag("Site") instanceof NBTTagInt ? saved.getInteger("Site") : 0;
            if (id == null || mobilization == null || site <= 0 || recent.containsKey(id)
                    || !(saved.getTag("Manual") instanceof NBTTagByte) || !(saved.getTag("Dead") instanceof NBTTagByte)
                    || !(saved.getTag("Released") instanceof NBTTagByte)) {
                truncated = true;
                continue;
            }
            Record row = new Record(site, mobilization, saved.getBoolean("Manual"));
            row.dead = saved.getBoolean("Dead");
            row.released = saved.getBoolean("Released");
            recent.put(id, row);
        }
    }

    @Override public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Version", VERSION);
        tag.setBoolean("Truncated", truncated);
        NBTTagList rows = new NBTTagList();
        for (Map.Entry<UUID, Record> entry : recent.entrySet()) {
            Record row = entry.getValue();
            NBTTagCompound saved = new NBTTagCompound();
            writeId(saved, "Actor", entry.getKey());
            writeId(saved, "Encounter", row.mobilization);
            saved.setInteger("Site", row.site);
            saved.setBoolean("Manual", row.manual);
            saved.setBoolean("Dead", row.dead);
            saved.setBoolean("Released", row.released);
            rows.appendTag(saved);
        }
        tag.setTag("Records", rows);
    }

    private static void writeId(NBTTagCompound tag, String name, UUID id) {
        tag.setLong(name + "High", id.getMostSignificantBits());
        tag.setLong(name + "Low", id.getLeastSignificantBits());
    }

    private static UUID readId(NBTTagCompound tag, String name) {
        if (!(tag.getTag(name + "High") instanceof NBTTagLong) || !(tag.getTag(name + "Low") instanceof NBTTagLong))
            return null;
        return new UUID(tag.getLong(name + "High"), tag.getLong(name + "Low"));
    }

    private static final class Record {
        final int site;
        final UUID mobilization;
        final boolean manual;
        boolean dead, released;
        WeakReference<EntityLiving> live;

        EntityLiving observed() { return live == null ? null : live.get(); }

        Record(int site, UUID mobilization, boolean manual) {
            this.site = site;
            this.mobilization = mobilization;
            this.manual = manual;
        }
    }

    static final class Snapshot {
        final boolean truncated;
        int inserted, loaded, unloaded, unknown, dead, released;

        Snapshot(boolean truncated) { this.truncated = truncated; }
    }

    public interface Participant { Actor siegeActor(); }

    /** Optional versioned identity for ZTO-owned siege actors. Legacy site NBT remains independent. */
    public static final class Actor {
        private UUID id;
        private boolean reconciled, confirmedDeath;

        boolean confirmedDeath() { return confirmedDeath; }

        public void write(NBTTagCompound tag) {
            if (id == null) return;
            NBTTagCompound identity = new NBTTagCompound();
            identity.setInteger("Version", VERSION);
            writeId(identity, "Actor", id);
            tag.setTag("ZTOSiegeCensus", identity);
        }

        public void read(NBTTagCompound tag, int site) {
            id = null;
            if (site > 0 && tag.getTag("ZTOSiegeCensus") instanceof NBTTagCompound) {
                NBTTagCompound identity = tag.getCompoundTag("ZTOSiegeCensus");
                if (identity.getTag("Version") instanceof NBTTagInt && identity.getInteger("Version") == VERSION)
                    id = readId(identity, "Actor");
            }
            reconciled = false;
            confirmedDeath = false;
        }

        public void observe(EntityLiving mob, int site) {
            if (reconciled || id == null || mob.worldObj.isRemote) return;
            reconciled = true;
            SiegeCensus census = load(mob.worldObj);
            if (census != null) census.claim(mob, site, id);
        }

        /** Called after the native death callback has committed the death. */
        public void died(EntityLiving mob, int site) {
            if (confirmedDeath || mob.worldObj.isRemote) return;
            confirmedDeath = true;
            if (id == null) return;
            SiegeCensus census = load(mob.worldObj);
            Record row = census == null ? null : census.claim(mob, site, id);
            if (row != null) {
                row.dead = true;
                row.live = null;
                census.markDirty();
            }
        }
    }
}
