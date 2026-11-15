package zed.tekkitoverhaul.territory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import net.minecraft.src.CompressedStreamTools;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;

public final class TerritorialSelfTest {
    private static volatile int blackhole;

    public static void main(String[] args) throws Exception {
        regions();
        observations();
        smoothing();
        persistence();
        measure();
        System.out.println("PASS territorial activity, threat policy and legacy NBT");
    }

    private static void regions() {
        expect(TerritorialActivity.cell(-65) == -2 && TerritorialActivity.cell(-64) == -1
                && TerritorialActivity.cell(-1) == -1 && TerritorialActivity.cell(64) == 1, "signed cells");
        for (int size : new int[] {16, 64, 128}) {
            Set<Long> walk = new HashSet<Long>(), work = new HashSet<Long>(), outposts = new HashSet<Long>();
            for (int i = 0; i < 1000; i++) walk.add(cell(i * 21, 0, size));
            for (int i = 0; i < 720; i++) work.add(cell(62 + i % 7, 70 + i % 9, size));
            for (int i = 0; i < 960; i++) outposts.add(cell(i % 240 < 120 ? 4 : 100, 4, size));
            System.out.printf("grid %d: walk=%d work=%d outposts=%d%n", size, walk.size(), work.size(), outposts.size());
        }
    }

    private static long cell(int x, int z, int size) {
        return TerritorialActivity.key(Math.floorDiv(x, size), Math.floorDiv(z, size));
    }

    private static void observations() {
        TerritorialActivity activity = new TerritorialActivity();
        sample(activity, 0, 0, 2);
        expect(activity.score(0, 0) == 2, "brief passage");
        sample(activity, 0, 0, 1000);
        expect(activity.score(0, 0) == 24, "saturation");
        sample(activity, 256, 0, 38);
        sample(activity, 0, 0, 2);
        expect(activity.records.get(cell(0, 0, 64)).returns == 0, "return must remain three samples");
        sample(activity, 0, 0, 1);
        expect(activity.records.get(cell(0, 0, 64)).returns == 2 && activity.score(4, 0) == 24, "spaced return and independent outpost");
        for (int i = 0; i < 1000; i++) sample(activity, i % 2 == 0 ? 64 : 63, 0, 1);
        expect(activity.records.get(cell(0, 0, 64)).returns == 2 && activity.records.get(cell(64, 0, 64)).returns == 0,
                "boundary hopping supplies no return contribution");
        activity.clock += 48_000;
        expect(activity.score(0, 0) == 0 && activity.score(4, 0) == 0, "complete lazy decay");
        activity.clock = Long.MAX_VALUE;
        activity.advance();
        expect(activity.clock == Long.MAX_VALUE && activity.score(0, 0) == 0, "clock saturation");
        TerritorialActivity exploration = new TerritorialActivity();
        for (int i = 0; i <= TerritorialActivity.CAPACITY; i++) sample(exploration, 64 * i, 0, 1);
        expect(exploration.records.size() == 512 && exploration.evicted && !exploration.records.containsKey(0L), "oldest eviction");
        sample(exploration, 0, 0, 1);
        expect(exploration.score(0, 0) == 1 && exploration.score(1, 0) == 0, "neutral revisit and predictable next eviction");
    }

    private static void smoothing() {
        TerritorialActivity activity = new TerritorialActivity();
        sample(activity, 32, 32, 24);
        expect(activity.blended(32, 32) == 24 && activity.blended(160, 160) == 0, "locality");
        expect(Math.abs(activity.blended(63, 32) - activity.blended(64, 32)) <= 1, "horizontal edge");
        expect(Math.abs(activity.blended(63, 63) - activity.blended(64, 64)) <= 1, "corner");
        sample(activity, -32, -32, 24);
        expect(Math.abs(activity.blended(-1, -1) - activity.blended(0, 0)) <= 1, "signed corner");
        ThreatAssessment baseline = assessment(0, ThreatAssessment.Environment.BASELINE);
        ThreatAssessment swamp = assessment(0, ThreatAssessment.Environment.SWAMPLAND);
        ThreatAssessment mature = assessment(100, ThreatAssessment.Environment.SWAMPLAND);
        expect(baseline.threat == 12 && swamp.threat == 18 && !swamp.observed && mature.threat == ThreatAssessment.MAX,
                "independent environmental threat and cap");
        for (int budget = 1; budget <= 24; budget++) {
            expect(baseline.assaultBudget(budget) == Math.max(1, budget - 1)
                    && swamp.assaultBudget(budget) == budget && mature.assaultBudget(budget) == budget, "budget envelope");
        }
    }

    private static ThreatAssessment assessment(int activity, ThreatAssessment.Environment environment) {
        return new ThreatAssessment(0, 0, 0, activity, environment, activity > 0);
    }

    private static void persistence() throws Exception {
        TerritorialWorldState source = new TerritorialWorldState("test");
        TerritorialWorldState loaded = new TerritorialWorldState("test");
        for (int count : new int[] {1, 32, 128, 512}) {
            for (int i = source.activity.records.size(); i < count; i++) sample(source.activity, 64 * i, -64, 1);
            NBTTagCompound tag = encode(source);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NBTTagCompound envelope = new NBTTagCompound();
            envelope.setCompoundTag("data", tag);
            CompressedStreamTools.writeCompressed(envelope, bytes);
            loaded.readFromNBT(TerritorialNbtInput.read(new ByteArrayInputStream(bytes.toByteArray())).getCompoundTag("data"));
            expect("READY".equals(loaded.status) && loaded.activity.records.size() == count
                    && source.activity.clock == loaded.activity.clock, "round trip and repeated save");
            System.out.printf("NBT %d records: %d compressed bytes%n", count, bytes.size());
        }
        sample(loaded.activity, -64, 0, 1);
        expect(loaded.activity.records.size() == 512 && !loaded.activity.records.containsKey(cell(0, -64, 64)),
                "eviction order survives reopen");
        NBTTagCompound invalid = encode(source);
        invalid.setInteger("Schema", 2);
        loaded.readFromNBT(invalid);
        expect("UNSUPPORTED_SCHEMA".equals(loaded.status) && !loaded.isDirty(), "future schema stays read-only");
        invalid = encode(source);
        NBTTagList records = invalid.getTagList("Regions");
        ((NBTTagCompound) records.tagAt(0)).setInteger("Presence", 99);
        loaded.readFromNBT(invalid);
        expect("UNREADABLE".equals(loaded.status) && !loaded.isDirty(), "bad record stays read-only");
        invalid = encode(source);
        records = invalid.getTagList("Regions");
        records.appendTag(records.tagAt(0));
        loaded.readFromNBT(invalid);
        expect("UNREADABLE".equals(loaded.status), "oversized list rejected");
        oversizedFrame(9, "Regions");
        oversizedFrame(7, "UnexpectedArray");
        TerritorialWorldState dirty = new TerritorialWorldState("test");
        int marks = 0;
        for (int n = 0; n < 12000; n++) {
            dirty.activity.advance();
            boolean changed = n % 100 == 99 && dirty.activity.observe(0, 0);
            dirty.checkpoint(changed);
            if (dirty.isDirty()) {
                marks++;
                dirty.setDirty(false);
            }
        }
        expect(marks == 32, "saturated sampling uses bounded checkpoints");
        System.out.println("Dirty marks in 12,000 stationary simulation ticks: " + marks);
    }

    private static void oversizedFrame(int type, String name) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(10);
            out.writeUTF("");
            out.writeByte(10);
            out.writeUTF("data");
            out.writeByte(type);
            out.writeUTF(name);
            if (type == 9) out.writeByte(10);
            out.writeInt(Integer.MAX_VALUE);
        }
        try {
            TerritorialNbtInput.read(new ByteArrayInputStream(bytes.toByteArray()));
            throw new AssertionError("Oversized NBT declaration was accepted");
        } catch (IOException expected) {
            expect("Invalid territorial NBT framing".equals(expected.getMessage()), "reject declaration before allocation");
        }
    }

    private static NBTTagCompound encode(TerritorialWorldState state) {
        NBTTagCompound tag = new NBTTagCompound();
        state.writeToNBT(tag);
        return tag;
    }

    private static void measure() {
        final int samples = 100000;
        for (int run = 0; run < 6; run++) {
            TerritorialActivity activity = new TerritorialActivity();
            for (int i = 0; i < 512; i++) sample(activity, i * 64, 0, 1);
            int checksum = 0;
            long begin = System.nanoTime();
            for (int i = 0; i < samples; i++) checksum += TerritorialActivity.cell((int) Math.floor(i * 1.3));
            long positioned = System.nanoTime();
            for (int i = 0; i < samples; i++) sample(activity, (i & 511) * 64, 0, 1);
            long updated = System.nanoTime();
            for (int i = 0; i < samples; i++) checksum += activity.score(i & 511, 0);
            long lookedUp = System.nanoTime();
            for (int i = 0; i < samples; i++) checksum += activity.blended(i & 32767, 0);
            long blended = System.nanoTime();
            for (int i = 0; i < samples; i++) sample(activity, (i + 512) * 64, 0, 1);
            long evicted = System.nanoTime();
            blackhole = checksum;
            if (run >= 3) System.out.printf("ns/op position=%d update=%d lookup=%d blend=%d eviction=%d%n",
                    (positioned - begin) / samples, (updated - positioned) / samples, (lookedUp - updated) / samples,
                    (blended - lookedUp) / samples, (evicted - blended) / samples);
        }
    }

    private static void sample(TerritorialActivity activity, int x, int z, int count) {
        for (int i = 0; i < count; i++) {
            activity.clock += TerritorialActivity.SAMPLE_TICKS;
            activity.observe(x, z);
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
