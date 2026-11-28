package zed.tekkitoverhaul.exploration.abyssal.worldgen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.minecraft.src.AnvilChunkLoader;
import net.minecraft.src.AnvilSaveHandler;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkProvider;
import net.minecraft.src.ChunkProviderGenerate;
import net.minecraft.src.ExtendedBlockStorage;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.forge.ISaveEventHandler;
import net.minecraft.src.forge.MinecraftForge;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry;

/** The world owner is the only writer. Records are independent of the asynchronous Anvil queue. */
public final class ObservatorySites {
    private static final int MAGIC = 0x5a544f53;
    private static final int GRID = 512;
    private static final int MIN_FLOOR = 17, MAX_FLOOR = 32;
    private static final int FOUNDATION = 7;
    private static final long SALT = 0x41F399AB647B58ECL;
    private static final Map<World, Map<Long, Site>> CACHE = new WeakHashMap<World, Map<Long, Site>>();
    private static final Site UNAVAILABLE = new Site(null, null);
    private static final String STAMP = "ZTOObservatory1";
    private static final Map<Chunk, Long> STAMPS = new WeakHashMap<Chunk, Long>();
    private static boolean active;
    private static final BiConsumer<Object, Object> WRITE_STAMP = new BiConsumer<Object, Object>() {
        @Override public void accept(Object tag, Object chunk) {
            Long id = STAMPS.get(chunk);
            if (id != null) ((NBTTagCompound) tag).setLong(STAMP, id);
        }
    };

    private ObservatorySites() {}

    public static void register() {
        try {
            Class<?> loader = AnvilChunkLoader.class;
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            active = (Boolean) agent.getMethod("observatorySaveActive", ClassLoader.class).invoke(null, loader.getClassLoader());
            if (active) agent.getMethod("registerObservatoryStamp", BiConsumer.class).invoke(null, WRITE_STAMP);
        } catch (ReflectiveOperationException unavailable) {
            active = false;
        }
        if (!active) {
            System.err.println("[ZTO] Observatory inactive. Install the matching agent and pinned Anvil loader.");
            return;
        }
        MinecraftForge.registerSaveHandler(new ISaveEventHandler() {
            @Override public void onWorldLoad(World world) {}
            @Override public void onWorldSave(World world) {}
            @Override public void onChunkLoad(World world, Chunk chunk) {}
            @Override public void onChunkUnload(World world, Chunk chunk) {}

            @Override public void onChunkSaveData(World world, Chunk chunk, NBTTagCompound data) {}

            @Override public void onChunkLoadData(World world, Chunk chunk, NBTTagCompound data) {
                if (data.hasKey(STAMP)) STAMPS.put(chunk, data.getLong(STAMP));
            }
        });
    }

    public static boolean supported(World world) {
        return active && world != null && !world.isRemote && AbyssalWorldgen.supported(world)
                && world.getSaveHandler().getClass() == AnvilSaveHandler.class
                && world.getChunkProvider() != null && world.getChunkProvider().getClass() == ChunkProvider.class;
    }

    private static synchronized Map<Long, Site> cache(World world) {
        Map<Long, Site> map = CACHE.get(world);
        if (map == null) {
            map = new LinkedHashMap<Long, Site>(64, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, Site> eldest) {
                    return size() > 64;
                }
            };
            CACHE.put(world, map);
        }
        return map;
    }

    private static ObservatoryPlan candidate(long seed, int gx, int gz) {
        long id = TrenchRegion.hash(seed ^ SALT, gx, gz);
        if ((id & 7) >= 5) return null;
        int x = gx * GRID + 136 + (int) ((id >>> 8) % 241);
        int z = gz * GRID + 136 + (int) ((id >>> 24) % 241);
        if (TrenchRegion.influence(seed, x, z) < 0.77 || TrenchRegion.relief(seed, x, z) > 35) return null;
        return new ObservatoryPlanFive(id, x, z, 0, 12, ObservatoryPlanFive.FRAMES, 1);
    }

    static Site prepare(World world, int cx, int cz) {
        if (!supported(world)) return null;
        int gx = Math.floorDiv(cx << 4, GRID), gz = Math.floorDiv(cz << 4, GRID);
        ObservatoryPlan candidate = candidate(world.getSeed(), gx, gz);
        if (candidate == null || !candidate.intersects(cx, cz)) return null;
        Site existing = record(world, gx, gz, candidate);
        if (existing == UNAVAILABLE || existing != null && existing.failed) return null;
        if (existing != null) return existing.plan.intersects(cx, cz) ? existing : null;
        File file = file(world, gx, gz);
        try {
            if (!untouched(world, candidate, file.getParentFile().getParentFile().getParentFile())) {
                cache(world).put(key(gx, gz), UNAVAILABLE);
                return null;
            }
            int[] entrance = new int[3];
            int y = survey(world, candidate, entrance);
            if (y == 0) {
                cache(world).put(key(gx, gz), UNAVAILABLE);
                return null;
            }
            Site result = new Site(new ObservatoryPlanFive(candidate.id, candidate.x, candidate.z, y,
                    entrance[0], entrance[1], entrance[2]), file);
            result.save();
            cache(world).put(key(gx, gz), result);
            return result;
        } catch (IOException error) {
            cache(world).put(key(gx, gz), UNAVAILABLE);
            System.err.println("[ZTO] Observatory admission unavailable at " + gx + ", " + gz + ": " + error);
            return null;
        }
    }

    private static File file(World world, int gx, int gz) {
        File data = world.getSaveHandler().getMapFileFromName("ZTOObservatories").getParentFile();
        return new File(new File(data, "zto-observatories"), gx + "." + gz + ".dat");
    }

    private static long key(int gx, int gz) { return ((long) gx << 32) | (gz & 0xffffffffL); }

    private static Site record(World world, int gx, int gz, ObservatoryPlan candidate) {
        Map<Long, Site> records = cache(world);
        long key = key(gx, gz);
        if (records.containsKey(key)) return records.get(key);
        File file = file(world, gx, gz);
        Site found;
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(Files.newInputStream(file.toPath()));
            if (stream.readInt() != MAGIC) throw new IOException("Unrecognized site header");
            int revision = stream.readInt();
            if ((revision < 1 || revision > 5) || stream.readLong() != candidate.id
                    || stream.readInt() != candidate.x || stream.readInt() != candidate.z)
                throw new IOException("Unrecognized site revision or identity");
            int floor = stream.readInt();
            if (floor < (revision >= 3 ? ObservatoryPlanThree.MIN_BASE : MIN_FLOOR) || floor > MAX_FLOOR) throw new IOException("Invalid floor");
            ObservatoryPlan frozen = revision == 1 ? new ObservatoryPlan(candidate.id, candidate.x, candidate.z, floor)
                    : revision == 2 ? new ObservatoryPlanTwo(candidate.id, candidate.x, candidate.z, floor)
                    : revision == 3 ? new ObservatoryPlanThree(candidate.id, candidate.x, candidate.z, floor)
                    : readEntrance(stream, candidate, floor, revision);
            found = new Site(frozen, file);
            found.slices = stream.readLong();
            found.consumed = stream.readInt();
            found.dead = stream.readInt();
            found.retired = stream.readInt();
            found.failed = stream.readBoolean();
            int slotMask = (1 << found.count()) - 1;
            if ((found.slices & ~found.plan.allSlices()) != 0 || (found.consumed & ~slotMask) != 0
                    || ((found.dead | found.retired) & ~found.consumed) != 0
                    || (found.dead & found.retired) != 0 || stream.read() != -1)
                throw new IOException("Invalid site state");
        } catch (NoSuchFileException absent) {
            found = null;
        } catch (IOException error) {
            System.err.println("[ZTO] Observatory record unavailable at " + gx + ", " + gz + ": " + error);
            found = UNAVAILABLE;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException error) {
                    found = UNAVAILABLE;
                }
            }
        }
        records.put(key, found);
        return found;
    }

    private static boolean untouched(World world, ObservatoryPlan plan, File root) throws IOException {
        for (int cz = plan.minZ >> 4; cz <= plan.maxZ >> 4; cz++) {
            for (int cx = plan.minX >> 4; cx <= plan.maxX >> 4; cx++) {
                if (world.getChunkProvider().chunkExists(cx, cz) || stored(root, cx, cz)) return false;
            }
        }
        return true;
    }

    private static boolean stored(File root, int cx, int cz) throws IOException {
        File directory = new File(root, "region");
        String name = "r." + (cx >> 5) + "." + (cz >> 5);
        int index = (cx & 31) + (cz & 31) * 32;
        return hasSector(new File(directory, name + ".mca"), index)
                || hasSector(new File(directory, name + ".mcr"), index);
    }

    private static boolean hasSector(File file, int index) throws IOException {
        try {
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            if (!attributes.isRegularFile() || attributes.size() < 8192) return true;
        } catch (NoSuchFileException absent) {
            return false;
        }
        RandomAccessFile stream = new RandomAccessFile(file, "r");
        try {
            stream.seek(index * 4L);
            return stream.readInt() != 0;
        } finally {
            stream.close();
        }
    }

    private static ObservatoryPlan readEntrance(DataInputStream stream, ObservatoryPlan candidate, int floor, int revision)
            throws IOException {
        int sill = stream.readUnsignedByte();
        int profile = stream.readUnsignedByte();
        if (revision == 5) {
            int shoulder = stream.readByte();
            if (sill < 9 || sill > 21 || profile > ObservatoryPlanFive.TERRACE || Math.abs(shoulder) != 1)
                throw new IOException("Invalid Observatory approach");
            return new ObservatoryPlanFive(candidate.id, candidate.x, candidate.z, floor, sill, profile, shoulder);
        }
        if (sill < 9 || sill > 23 || profile > 1) throw new IOException("Invalid portal profile");
        return new ObservatoryPlanFour(candidate.id, candidate.x, candidate.z, floor, sill, profile == 1);
    }

    private static int survey(World world, ObservatoryPlan plan) {
        return survey(world, plan, null);
    }

    private static int survey(World world, ObservatoryPlan plan, int[] entrance) {
        if (!plan.connected(false) || !plan.connected(true)) return 0;
        ChunkProviderGenerate sampler = new ChunkProviderGenerate(world, world.getSeed(), false);
        int low = 127, high = 0, sum = 0, samples = 0, maxBase = MAX_FLOOR, entranceTop = 0;
        int approach = 0, shoulder = 0, left = 0, right = 0, forecourt = 0;
        boolean composed = plan instanceof ObservatoryPlanFive;
        ObservatoryPlanThree buried = plan instanceof ObservatoryPlanThree ? (ObservatoryPlanThree) plan : null;
        for (int cz = plan.minZ >> 4; cz <= plan.maxZ >> 4; cz++) {
            for (int cx = plan.minX >> 4; cx <= plan.maxX >> 4; cx++) {
                AbyssalWorldgen.Plan terrain = AbyssalWorldgen.sample(world, sampler, cx, cz);
                if (terrain == null) return 0;
                for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    int wx = (cx << 4) + x, wz = (cz << 4) + z;
                    if (wx < plan.minX || wx > plan.maxX || wz < plan.minZ || wz > plan.maxZ) continue;
                    int column = x + z * 16;
                    int floor = terrain.floor[column];
                    if (buried != null) {
                        int u = plan.across(wx, wz), v = plan.along(wx, wz), roof = buried.coverRoof(u, v);
                        if (roof >= 0) maxBase = Math.min(maxBase, floor - roof - 3);
                        if (Math.abs(u) <= 4 && v >= -2 && v <= 5) entranceTop = Math.max(entranceTop, floor);
                        if (u == 0 && v == -3) approach = floor;
                        if (Math.abs(u) == 4 && v == 1) shoulder = Math.max(shoulder, floor);
                        if (composed && v >= -2 && v <= 2) {
                            if (u == -6) left += floor;
                            if (u == 6) right += floor;
                        }
                        if (composed && u == 0 && v == -8) forecourt = floor;
                        if (plan.coversColumn(wx, wz) && terrain.influence[column] < 0.50) return 0;
                    } else if (floor < 12 || floor > 40 || terrain.influence[column] < 0.55) return 0;
                    low = Math.min(low, floor);
                    high = Math.max(high, floor);
                    sum += floor;
                    samples++;
                }
            }
        }
        if (buried != null) {
            if (maxBase < ObservatoryPlanThree.MIN_BASE + (composed ? 1 : 0) || entranceTop - maxBase > 25
                    || entranceTop - maxBase < 9) return 0;
            if (entrance != null) {
                if (composed) {
                    int bank = Math.abs(left - right);
                    entrance[0] = Math.max(9, Math.min(Math.min(21, 50 - maxBase), approach - maxBase));
                    entrance[1] = bank >= 12 ? ObservatoryPlanFive.CLEFT
                            : Math.abs(approach - forecourt) <= 2 && shoulder <= approach + 2
                                    && ((plan.id >>> 32) & 3) != 0
                            ? ObservatoryPlanFive.TERRACE : ObservatoryPlanFive.FRAMES;
                    entrance[2] = left == right ? ((plan.id & 16) == 0 ? -1 : 1) : left > right ? -1 : 1;
                } else {
                    entrance[0] = Math.max(9, Math.min(23, approach - maxBase));
                    entrance[1] = shoulder >= approach + 1 ? 1 : 0;
                }
            }
            return maxBase;
        }
        int floor = (sum + samples / 2) / samples;
        return floor >= MIN_FLOOR && floor <= MAX_FLOOR && floor - low <= FOUNDATION
                && high - floor <= 8 ? floor : 0;
    }

    static void build(Chunk chunk, AbyssalWorldgen.Plan terrain, Site site) {
        ObservatoryPlan plan = site.plan;
        long bit = 1L << plan.slice(chunk.xPosition, chunk.zPosition);
        if ((site.slices & bit) != 0 || site.failed) return;
        boolean buried = plan.revision() >= 3;
        int high = plan.revision() == 5 ? ObservatoryPlanFive.MAX_HEIGHT : buried ? ObservatoryPlanThree.MAX_HEIGHT : 11;
        int[] writes = new int[16 * 16 * (high + FOUNDATION + 1)];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int wx = (chunk.xPosition << 4) + x, wz = (chunk.zPosition << 4) + z;
            if (wx < plan.minX || wx > plan.maxX || wz < plan.minZ || wz > plan.maxZ) continue;
            int u = plan.across(wx, wz), v = plan.along(wx, wz);
            int floor = terrain.floor[x + z * 16];
            for (int h = -FOUNDATION; h <= high; h++) {
                int cell = plan.cell(u, h, v, floor - plan.y);
                if (cell == ObservatoryPlan.UNTOUCHED) continue;
                if ((!buried && (floor < plan.y - FOUNDATION || floor > plan.y + 8))
                        || !replaceable(chunk.getBlockID(x, plan.y + h, z))) {
                    System.err.println("[ZTO] Observatory " + Long.toHexString(plan.id) + " veto at "
                            + wx + "," + (plan.y + h) + "," + wz + " over block " + chunk.getBlockID(x, plan.y + h, z));
                    site.failed = true;
                    site.saveChange();
                    return;
                }
                writes[(h + FOUNDATION) * 256 + z * 16 + x] = cell | plan.metadata(cell, u, v) << 8;
            }
        }
        // Reserve before exposing blocks. A lost native slice after a crash is never replayed.
        site.slices |= bit;
        if (!site.saveChange()) return;
        for (int i = 0; i < writes.length; i++) {
            int cell = writes[i] & 255;
            if (cell == 0) continue;
            int x = i & 15, z = (i >> 4) & 15, y = plan.y + (i >> 8) - FOUNDATION;
            ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
            ExtendedBlockStorage section = storage[y >> 4];
            if (section == null) section = storage[y >> 4] = new ExtendedBlockStorage(y & ~15);
            section.setExtBlockID(x, y & 15, z, cell == ObservatoryPlan.WATER
                    ? Block.waterStill.blockID : ObservatoryPlan.material(cell));
            section.setExtBlockMetadata(x, y & 15, z, writes[i] >>> 8);
            section.setExtBlocklightValue(x, y & 15, z, 0);
        }
        int lamp = ObservatoryPlan.material(ObservatoryPlan.LAMP);
        int emission = Block.lightValue[lamp];
        for (int i = 0; i < writes.length; i++) {
            if ((writes[i] & 255) == ObservatoryPlan.LAMP)
                AbyssalDecoration.light(chunk, i & 15, plan.y + (i >> 8) - FOUNDATION, (i >> 4) & 15, emission);
        }
        if (plan.revision() >= 2) lightAcrossSeams(chunk, plan, emission);
        STAMPS.put(chunk, plan.id);
    }

    /** Native source water absorbs three light levels, so one submerged neighbor can receive lamp light. */
    private static void lightAcrossSeams(Chunk chunk, ObservatoryPlan plan, int emission) {
        int startX = chunk.xPosition << 4, startZ = chunk.zPosition << 4;
        for (int side = 0; side < 4; side++) for (int i = 0; i < 16; i++) {
            int x = side < 2 ? (side == 0 ? 0 : 15) : i;
            int z = side < 2 ? i : (side == 2 ? 0 : 15);
            int otherX = startX + x + (side == 0 ? -1 : side == 1 ? 1 : 0);
            int otherZ = startZ + z + (side == 2 ? -1 : side == 3 ? 1 : 0);
            int u = plan.across(otherX, otherZ), v = plan.along(otherX, otherZ);
            for (int h = 0; h <= (plan.revision() == 5 ? ObservatoryPlanFive.MAX_HEIGHT : 11); h++) {
                int y = plan.y + h;
                if (chunk.getBlockID(x, y, z) != Block.waterStill.blockID
                        || plan.cell(u, h, v, h) != ObservatoryPlan.LAMP) continue;
                int light = emission - Math.max(1, chunk.getBlockLightOpacity(x, y, z));
                if (light > 0) AbyssalDecoration.light(chunk, x, y, z, light);
            }
        }
    }

    private static boolean replaceable(int id) {
        if (id == 0 || AbyssalWorldgen.isWater(id) || AbyssalWorldgen.geology(id)
                || AbyssalRegistry.isMarineOrganism(Block.blocksList[id])) return true;
        for (AbyssalRegistry.MaterialType type : AbyssalRegistry.MaterialType.values())
            if (id == type.block().blockID) return true;
        return false;
    }

    public static Site nearby(World world, int x, int z) {
        if (!supported(world)) return null;
        int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
        ObservatoryPlan candidate = candidate(world.getSeed(), gx, gz);
        if (candidate == null || x < candidate.minX - 38 || x > candidate.maxX + 38
                || z < candidate.minZ - 38 || z > candidate.maxZ + 38) return null;
        Site result = record(world, gx, gz, candidate);
        return result == UNAVAILABLE ? null : result;
    }

    public static String locate(World world, final int x, final int z) {
        if (!supported(world)) return "Observatory generation unavailable";
        int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
        List<ObservatoryPlan> predictions = new ArrayList<ObservatoryPlan>();
        for (int dz = -6; dz <= 6; dz++) for (int dx = -6; dx <= 6; dx++) {
            ObservatoryPlan plan = candidate(world.getSeed(), gx + dx, gz + dz);
            if (plan != null) predictions.add(plan);
        }
        Collections.sort(predictions, new Comparator<ObservatoryPlan>() {
            @Override public int compare(ObservatoryPlan left, ObservatoryPlan right) {
                return Double.compare(Math.hypot((double) left.x - x, (double) left.z - z),
                        Math.hypot((double) right.x - x, (double) right.z - z));
            }
        });
        int surveys = 0;
        for (ObservatoryPlan plan : predictions) {
            Site site = record(world, Math.floorDiv(plan.x, GRID), Math.floorDiv(plan.z, GRID), plan);
            if (site == UNAVAILABLE || site != null && site.failed) continue;
            if (site == null && (++surveys > 12 || survey(world, plan) == 0)) continue;
            return describe(plan, x, z, site == null ? "Predicted" : "Admitted");
        }
        return "No suitable Observatory found in this survey";
    }

    private static String describe(ObservatoryPlan plan, int x, int z, String status) {
        return status + " " + plan.x + ", " + plan.z + " | "
                + Math.round(Math.hypot((double) plan.x - x, (double) plan.z - z)) + " m | "
                + (plan.width * 2 + 1) + "x" + (plan.length + (plan.revision() == 5 ? 17 : 6))
                + " | " + Long.toHexString(plan.id);
    }

    public static final class Site {
        private final ObservatoryPlan plan;
        private final File file;
        private long slices;
        private int consumed, dead, retired;
        private boolean failed;
        private CustodianRecord custodian;

        private Site(ObservatoryPlan plan, File file) {
            this.plan = plan;
            this.file = file;
        }

        public long identity() { return plan.id; }
        public int revision() { return plan.revision(); }
        public String description(int x, int z) { return describe(plan, x, z, "Site r" + revision()); }
        boolean coversColumn(int x, int z) { return !failed && plan.coversColumn(x, z); }
        public int count() { return plan.population(); }
        private long lastAdmission = Long.MIN_VALUE;
        public boolean roomCleared(int slot) {
            if (!(plan instanceof ObservatoryPlanThree)) return false;
            ObservatoryPlanThree buried = (ObservatoryPlanThree) plan;
            int room = buried.stationRoom(slot);
            for (int i = 0; i < count(); i++) if (buried.stationRoom(i) == room && terminal(i)) return true;
            return false;
        }

        public boolean canMaterialize(long tick) {
            return lastAdmission == Long.MIN_VALUE || tick < lastAdmission || tick - lastAdmission >= 160;
        }
        public void materialized(long tick) { lastAdmission = tick; }
        public boolean complete() { return !failed && slices == plan.allSlices(); }
        public boolean ready(World world) {
            if (!complete()) return false;
            for (int cz = plan.minZ >> 4; cz <= plan.maxZ >> 4; cz++) {
                for (int cx = plan.minX >> 4; cx <= plan.maxX >> 4; cx++) {
                    if (!world.getChunkProvider().chunkExists(cx, cz)) return false;
                    Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                    Long stamp = STAMPS.get(chunk);
                    if (stamp == null || stamp.longValue() != plan.id || !chunk.isTerrainPopulated) return false;
                }
            }
            return true;
        }
        public boolean consumed(int slot) { return (consumed & (1 << slot)) != 0; }
        public boolean terminal(int slot) { return ((dead | retired) & (1 << slot)) != 0; }
        public boolean reserve(int slot) {
            if (slot < 0 || slot >= count() || !complete() || consumed(slot)) return false;
            consumed |= 1 << slot;
            return saveChange();
        }

        public void finish(int slot, boolean killed) {
            if (slot < 0 || slot >= count() || !consumed(slot) || terminal(slot)) return;
            if (killed) dead |= 1 << slot;
            else retired |= 1 << slot;
            saveChange();
        }

        public int[] home(int slot) { return plan.station(slot); }
        public int[] arenaCorner(boolean far) { return plan.arenaCorner(far); }
        public int[] arenaEntry(boolean side) { return plan.arenaEntry(side); }
        public CustodianRecord custodian() {
            if (custodian == null) custodian = new CustodianRecord(file, plan.id);
            return custodian;
        }

        public int[] arenaCenter() { return plan.arenaCenter(); }

        public boolean excludesWild(int x, int z) {
            return x >= plan.minX - 12 && x <= plan.maxX + 12 && z >= plan.minZ - 12 && z <= plan.maxZ + 12;
        }

        public String status() {
            return (failed ? "Unavailable" : complete() ? "Slices reserved" : "Admitted")
                    + " | " + Long.bitCount(slices) + "/" + Long.bitCount(plan.allSlices())
                    + " slices | " + Integer.bitCount(consumed) + "/" + count() + " used | "
                    + Integer.bitCount(dead) + " dead | " + Integer.bitCount(retired) + " retired";
        }

        private boolean saveChange() {
            try {
                save();
                return true;
            } catch (IOException error) {
                failed = true;
                System.err.println("[ZTO] Observatory state write failed for " + Long.toHexString(plan.id) + ": " + error);
                return false;
            }
        }

        private void save() throws IOException {
            Files.createDirectories(file.getParentFile().toPath());
            File temporary = new File(file.getParentFile(), file.getName() + ".pending");
            FileOutputStream output = new FileOutputStream(temporary);
            DataOutputStream stream = new DataOutputStream(output);
            try {
                stream.writeInt(MAGIC);
                stream.writeInt(plan.revision());
                stream.writeLong(plan.id);
                stream.writeInt(plan.x);
                stream.writeInt(plan.z);
                stream.writeInt(plan.y);
                if (plan instanceof ObservatoryPlanFour) {
                    ObservatoryPlanFour portal = (ObservatoryPlanFour) plan;
                    stream.writeByte(portal.sill);
                    stream.writeByte(portal.recessed ? 1 : 0);
                } else if (plan instanceof ObservatoryPlanFive) {
                    ObservatoryPlanFive portal = (ObservatoryPlanFive) plan;
                    stream.writeByte(portal.sill);
                    stream.writeByte(portal.profile);
                    stream.writeByte(portal.shoulder);
                }
                stream.writeLong(slices);
                stream.writeInt(consumed);
                stream.writeInt(dead);
                stream.writeInt(retired);
                stream.writeBoolean(failed);
                stream.flush();
                output.getFD().sync();
            } finally {
                stream.close();
            }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
