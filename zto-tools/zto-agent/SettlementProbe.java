import buildcraft.api.PowerFramework;
import buildcraft.energy.PneumaticPowerFramework;
import buildcraft.factory.TileQuarry;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import forge.ISaveEventHandler;
import forge.MinecraftForge;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.settlement.SettlementRecognition;
import zed.tekkitoverhaul.settlement.SettlementRecognition.Snapshot;
import zed.tekkitoverhaul.settlement.WorldProvenance;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Exercises native placement, movement, utility activation and Save & Quit persistence without a window. */
public final class SettlementProbe implements ISaveEventHandler {
    private static final int X = 320, Y = 4, Z = 320, PLANK = 5, BENCH = 58, CHEST = 54, FURNACE = 61;
    private final File root;
    private final Minecraft game;
    private final aes controller;
    private final WorldProvenance provenance = new WorldProvenance();
    private final TerritorialActivityService territory = new TerritorialActivityService();
    private final SettlementRecognition settlements = new SettlementRecognition(territory, provenance);
    private Fixture world;
    private vq player;
    private int utilityOpens;

    public static void main(String[] args) {
        try {
            SettlementProbe probe = new SettlementProbe(new File(args[0]));
            probe.sites();
            probe.negatives();
            probe.village();
            probe.stress();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private SettlementProbe(File root) throws Exception {
        this.root = root;
        Class.forName("pb"); // Match a local world starting the vanilla block registry before FML.
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja error) { throw new AssertionError(error); }
            @Override public void a(vp screen) {
                if (!(screen instanceof gb)) throw new AssertionError("Expected a utility container");
                h.ar = ((gb) screen).d; // Native GuiContainer handoff with no LWJGL window.
                utilityOpens++;
            }
        };
        game.R = true;
        game.c = controller = new aes(game);
        Field client = FMLClientHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(FMLClientHandler.instance(), game);
        provenance.register();
        settlements.register();
        MinecraftForge.registerSaveHandler(this);
    }

    private void open(String name, boolean structures) throws Exception {
        territory.bind(null);
        provenance.bind(null);
        settlements.bind(null);
        Path marker = new File(root, name + "/.zto-disposable").toPath();
        Fixture incoming = new Fixture(new aia(root, name, false), name, structures);
        if (!Files.exists(marker)) Files.createFile(marker);
        bind(incoming);
    }

    private void bind(Fixture incoming) {
        world = incoming;
        game.f = world;
        player = new vq(game, world, new fc("SettlementProbe", ""), 0) {
            @Override public void a(ajw statistic, int count) {}
        };
        game.h = player;
        territory.bind(world);
        provenance.bind(world);
        provenance.bindPlayer(player);
        settlements.bind(world);
    }

    private void sites() throws Exception {
        open("settlement-sites", false);
        prepare(X, Z);
        position(X - 3, Y, Z + 3);
        cabin(X, Y, Z, true, false);
        position(X + 2, Y, Z + 2);
        ticks(220);
        Snapshot candidate = here();
        check("Candidate".equals(candidate.state) && candidate.placed >= 8 && candidate.walked < 4,
                "cabin forms a candidate before corroboration", candidate);
        walk();
        ticks(340);
        Snapshot cabin = recognized("furniture-free cabin", true);
        check(cabin.uses == 0 && cabin.utilities == 0, "cabin has no utilities", cabin);
        check(controller.b(X, Y + 2, Z + 2, 4), "ordinary cabin wall edit", cabin);
        world.g(X + 1, Y + 3, Z + 2, 1); // Simulate one unobserved replacement by a different block ID.
        walk();
        ticks(120);
        check(here().id == cabin.id && here().placed == cabin.placed - 2,
                "wall edit retains identity and stale placement fails the live check", here());

        int remoteX = X + 512;
        save();
        // The pinned local provider does not evict on its ordinary unload poll.
        // Replace the native cache with the same save handler to exercise unavailable terrain.
        bind(new Fixture((aia) world.A(), "settlement-sites", false));
        prepare(remoteX, Z);
        check(!world.j(X + 2, Y, Z + 2), "previous site is unloaded", here());
        position(X + 2, Y, Z + 2);
        ticks(120);
        Snapshot missing = here();
        check(missing.id == cabin.id && !world.j(X + 2, Y, Z + 2)
                && "Terrain unavailable".equals(missing.reason), "survey retains unloaded site without loading", missing);
        position(remoteX, Y, Z);
        ticks(110);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(130);
        check(here().id == cabin.id, "return reuses identity", here());

        int caveX = X + 11;
        position(caveX - 2, Y, Z + 3);
        for (int x = caveX - 1; x <= caveX + 6; x++) {
            for (int z = Z - 1; z <= Z + 6; z++) {
                for (int y = Y + 3; y <= Y + 6; y++) world.g(x, y, z, 1);
            }
        }
        platform(caveX, Y, Z);
        position(caveX + 1, Y, Z + 1);
        walk();
        ticks(540);
        Snapshot cave = recognized("adapted underground workspace", true);
        check(cave.id != cabin.id && cave.cover >= 4, "nearby cabin and cave keep independent sites", cave);

        int elevated = 28;
        for (int x = X; x < X + 5; x++) {
            for (int z = Z; z < Z + 5; z++) world.g(x, elevated - 2, z, 1);
        }
        position(X - 2, elevated, Z + 2);
        platform(X, elevated, Z);
        place(BENCH, X + 4, elevated - 1, Z + 4, 1);
        place(CHEST, X + 4, elevated - 1, Z + 3, 1);
        place(FURNACE, X + 4, elevated - 1, Z + 2, 1);
        position(X + 1, elevated, Z + 1);
        walk();
        useUtility(X + 4, elevated, Z + 4);
        ticks(25);
        useUtility(X + 4, elevated, Z + 3);
        ticks(25);
        useUtility(X + 4, elevated, Z + 2);
        ticks(440);
        Snapshot workshop = recognized("open elevated workshop", true);
        check(workshop.id != cabin.id && workshop.id != cave.id && workshop.cover == 0 && workshop.uses == 3,
                "overlapping X/Z sites are distinct and open workshop has use evidence", workshop);

        int oldZ = Z + 17;
        prepare(X, oldZ);
        position(X - 2, Y, oldZ);
        cabin(X, Y, oldZ, false, true);
        world.g(X + 5, Y, oldZ + 4, CHEST);
        world.g(X + 5, Y + 1, oldZ + 4, 1);
        position(X + 2, Y, oldZ + 2);
        walk();
        int before = utilityOpens;
        check(controller.a(player, world, null, X + 5, Y, oldZ + 4, 1) && utilityOpens == before,
                "blocked chest consumes the click without opening", here());
        ticks(30);
        check(controller.a(player, world, null, X + 5, Y, oldZ + 4, 1) && utilityOpens == before,
                "repeated blocked activation opens no container", here());
        ticks(450);
        check(here().id == 0 && here().inferred > 16 && here().uses == 0, "old structure needs actual use", here());
        useUtility(X + 5, Y, oldZ + 5);
        ticks(30);
        useUtility(X + 5, Y, oldZ + 5);
        ticks(120);
        Snapshot old = recognized("old inhabited structure", false);
        check(old.placed == 0 && old.uses >= 2 && old.useHook, "unknown authorship is explicit", old);

        save();
        Path registry = new File(root, "settlement-sites/data/zto_settlement_sites.dat").toPath();
        check(Files.size(registry) == 158, "four bounded records serialized", here());
        open("settlement-sites", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        Snapshot reopened = here();
        check(reopened.id == cabin.id && reopened.boundary.equals(cabin.boundary)
                && world.a(X + 2, Y + 3, Z + 2) == PLANK, "native world reopened with stable cabin ID and bounds", reopened);
        walk();
        ticks(450);
        check(here().id == cabin.id && here().siteCount == 4, "re-observation does not duplicate an ID", here());

        corrupt(registry, new byte[] {1, 2, 3}, "UNREADABLE");
        corrupt(registry, ByteBuffer.allocate(8).putInt(0x5A544F53).putInt(999).array(), "UNSUPPORTED_SCHEMA");
        System.out.println("PASS four site identities, re-observation, native save/reopen, malformed and future files");
    }

    private void corrupt(Path registry, byte[] contents, String status) throws Exception {
        Files.write(registry, contents);
        open("settlement-sites", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(420);
        check(here().registry.equals(status) && here().siteCount == 0, "invalid registry quarantined", here());
        save();
        check(Arrays.equals(contents, Files.readAllBytes(registry)), "invalid registry preserved", here());
    }

    private void negatives() throws Exception {
        open("settlement-controls", false);
        prepare(X, Z);
        position(X, Y, Z);
        for (int visit = 0; visit < 3; visit++) {
            walk();
            ticks(800);
            check(here().id == 0 && here().activity >= 3, "untouched terrain receives activity without a site", here());
            position(X + 80, Y, Z);
            ticks(3700);
            position(X, Y, Z);
        }
        for (int z = Z; z < Z + 8; z++) {
            for (int y = Y; y < Y + 3; y++) place(PLANK, X + 3, y - 1, z, 1);
        }
        walk();
        ticks(500);
        check(here().siteCount == 0, "isolated wall cannot promote", here());
        for (int i = 0; i < 48; i++) {
            place(PLANK, X - 2, Y - 1, Z, 1);
            check(controller.b(X - 2, Y, Z, 1), "native replacement cycle", here());
        }
        walk();
        ticks(500);
        check(here().siteCount == 0, "replacement cycling cannot promote", here());

        PowerFramework.currentFramework = new PneumaticPowerFramework();
        TileQuarry quarry = new TileQuarry();
        quarry.i = world;
        quarry.j = X + 49;
        quarry.k = Y;
        quarry.l = Z;
        prepare(X + 45, Z);
        position(X + 45, Y, Z);
        int removed = 0;
        for (int x = X + 37; x < X + 53; x++) {
            for (int z = Z - 8; z < Z + 8; z++) {
                world.g(x, Y, z, 1);
                quarry.targetX = x;
                quarry.targetY = Y + 1;
                quarry.targetZ = z;
                quarry.positionReached(null);
                if (world.a(x, Y, z) == 0) removed++;
            }
        }
        walk();
        ticks(600);
        check(removed == 256 && here().siteCount == 0
                && provenance.snapshot(world, X + 47, Y, Z).quarryRemoval > 0, "quarry has removal evidence only", here());
        System.out.printf("PASS untouched revisits, isolated wall, replacement cycles and %d native Quarry removals%n", removed);
    }

    private void village() throws Exception {
        open("settlement-village", true);
        for (int regionX = 0; regionX < 4; regionX++) {
            Random region = world.A(regionX, 0, 10387312);
            int x = (regionX * 32 + region.nextInt(24)) << 4, z = region.nextInt(24) << 4;
            prepare(x, z);
            for (int bx = x - 32; bx < x + 40; bx++) {
                for (int bz = z - 32; bz < z + 40; bz++) {
                    int wall = world.a(bx, Y + 1, bz);
                    if (wall != PLANK && wall != 4 || world.a(bx, Y, bz + 1) != 0) continue;
                    position(bx, Y, bz + 1);
                    ticks(700);
                    Snapshot village = here();
                    if (village.inferred < 12) continue;
                    check(village.placed == 0 && village.id == 0 && village.activity >= 3,
                            "native generated village and visits stay unrecognized", village);
                    System.out.printf("PASS generated village control, %d inferred blocks and unknown authorship%n", village.inferred);
                    return;
                }
            }
        }
        throw new AssertionError("pinned flat-world generator did not yield a sampled village structure");
    }

    private void stress() throws Exception {
        open("settlement-dispersed", false);
        prepare(X + 96, Z);
        position(X + 93, Y, Z + 3);
        cabin(X + 96, Y, Z, true, false);
        position(X + 98, Y, Z + 2);
        walk();
        ticks(550);
        int original = recognized("cabin before provenance eviction", true).id;
        prepare(X + 30, Z + 30);
        position(X - 4, Y, Z);
        int placements = 0;
        for (int x = X; x < X + 64; x += 8) {
            for (int z = Z; z < Z + 64; z += 8) {
                for (int y = 8; y <= 136; y += 8) {
                    world.g(x, y - 1, z, 1);
                    place(PLANK, x, y - 1, z, 1);
                    placements++;
                }
            }
        }
        // Dense manufactured geometry drives the connected component pass as well as chunk reads.
        for (int x = X + 24; x <= X + 36; x++) {
            for (int z = Z + 24; z <= Z + 36; z++) {
                for (int y = 14; y <= 22; y++) {
                    if (x < X + 28 || x > X + 32 || z < Z + 28 || z > Z + 32 || y < 17 || y > 19)
                        world.g(x, y, z, PLANK);
                }
            }
        }
        position(X + 31, 17, Z + 31);
        ticks(100);
        int sampled = 0, queued = 0;
        long measured = 0, worst = 0;
        for (int i = 0; i < 3200; i++) {
            territory.tick(player, false);
            long start = System.nanoTime();
            settlements.tick(player, false);
            long elapsed = System.nanoTime() - start;
            Snapshot step = here();
            if (step.reads != 0) {
                sampled++;
                measured += elapsed;
                worst = Math.max(worst, elapsed);
            }
            if (step.queued) queued++;
        }
        WorldProvenance.Snapshot store = provenance.snapshot(world, X + 63, 136, Z + 63);
        check(here().peakReads == 169 && sampled == 288 && queued == 256
                && store.sections == 1024 && store.evicted && here().siteCount == 1, "measurement and capacity", here());
        save();
        long bytes = Files.size(new File(root, "settlement-dispersed/data/zto_world_provenance.dat").toPath());
        System.out.printf("PASS dispersed %d placements, %d sections, %d bytes, %d sampled / %d queued ticks, "
                        + "peak %d reads/tick, mean %.3f ms/sampled tick, max %.3f ms%n",
                placements, store.sections, bytes, sampled, queued, here().peakReads,
                measured / (1000000.0 * sampled), worst / 1000000.0);
        position(X + 98, Y, Z + 2);
        walk();
        ticks(450);
        check(here().id == original && here().placed == 0 && here().siteCount == 1,
                "eviction removes provenance evidence while site keeps its identity", here());
    }

    private void cabin(int x, int y, int z, boolean nativeBuild, boolean bench) {
        for (int dz = 0; dz < 7; dz++) {
            for (int dx = 0; dx < 7; dx++) {
                if (dx != 0 && dx != 6 && dz != 0 && dz != 6) continue;
                if (dx == 0 && dz == 3) continue;
                for (int dy = 0; dy < 3; dy++) {
                    if (nativeBuild) place(PLANK, x + dx, y + dy - 1, z + dz, 1);
                    else world.g(x + dx, y + dy, z + dz, PLANK);
                }
            }
        }
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 7; dz++) {
                if (!nativeBuild) world.g(x + dx, y + 3, z + dz, PLANK);
                else if (dx > 0) place(PLANK, x + dx - 1, y + 3, z + dz, 5);
                else if (dz == 3) place(PLANK, x, y + 3, z + dz - 1, 3);
                else place(PLANK, x, y + 2, z + dz, 1);
            }
        }
        if (bench) world.g(x + 5, y, z + 5, BENCH);
    }

    private void platform(int x, int y, int z) {
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                world.g(x + dx, y - 2, z + dz, 1);
                world.g(x + dx, y - 1, z + dz, 0);
                place(PLANK, x + dx, y - 2, z + dz, 1);
            }
        }
    }

    private void place(int block, int x, int y, int z, int side) {
        check(controller.a(player, world, new aan(block, 1, 0), x, y, z, side),
                "native placement at " + x + "," + y + "," + z + "/" + side, null);
    }

    private void useUtility(int x, int y, int z) {
        int before = utilityOpens;
        check(controller.a(player, world, null, x, y, z, 1) && utilityOpens == before + 1,
                "normal utility activation handed off a container", here());
        player.ar = player.aq;
    }

    private void prepare(int x, int z) {
        int cx = x >> 4, cz = z >> 4;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) world.d(cx + dx, cz + dz);
    }

    private void position(int x, int y, int z) { player.c(x + 0.5, y, z + 0.5, 0, 0); }

    private void walk() {
        for (int segment = 0; segment < 4; segment++) {
            double dx = segment == 0 ? 0.25 : segment == 2 ? -0.25 : 0;
            double dz = segment == 1 ? 0.25 : segment == 3 ? -0.25 : 0;
            for (int step = 0; step < 4; step++) {
                player.b(dx, 0, dz);
                ticks(1);
            }
        }
    }

    private void ticks(int number) {
        for (int i = 0; i < number; i++) {
            territory.tick(player, false);
            settlements.tick(player, false);
        }
    }

    private Snapshot here() {
        return settlements.snapshot(world, (int) Math.floor(player.o),
                (int) Math.floor(player.y.b), (int) Math.floor(player.q));
    }

    private Snapshot recognized(String name, boolean observed) {
        Snapshot snapshot = here();
        check(snapshot.id > 0 && snapshot.confidence.startsWith(observed ? "Observed" : "Inferred"), name, snapshot);
        System.out.printf("PASS %s #%d at %s%n", name, snapshot.id, snapshot.boundary);
        return snapshot;
    }

    private void save() throws Exception {
        world.a(true, (rw) null);
        ce.a.a();
    }

    private static void check(boolean expected, String message, Snapshot details) {
        if (expected) return;
        throw new AssertionError(message + (details == null ? "" : " | " + details.state + " " + details.reason
                + " | placed=" + details.placed + " inferred=" + details.inferred + " cover=" + details.cover
                + " activity=" + details.activity + " steps=" + details.walked + " uses=" + details.uses
                + " registry=" + details.registry + " bounds=" + details.boundary));
    }

    @Override public void onWorldSave(xd context) {
        provenance.save(context);
        settlements.save(context);
    }
    @Override public void onWorldLoad(xd context) {}
    @Override public void onChunkLoad(xd context, ack chunk) {}
    @Override public void onChunkUnload(xd context, ack chunk) {}
    @Override public void onChunkSaveData(xd context, ack chunk, ady data) {}
    @Override public void onChunkLoadData(xd context, ack chunk, ady data) {}

    private static final class Fixture extends xd {
        Fixture(aia save, String name, boolean structures) {
            super(save, name, new fj(32021L, 0, structures, false, vx.c));
        }

        @Override public void a(nn entity, String sound, float gain, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float gain, float pitch) {}
    }
}
