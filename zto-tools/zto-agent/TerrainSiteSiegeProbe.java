import java.io.File;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.survival.tactical.EntityTank;

/** Native terrain, not an injected planner result. The headless runner owns all fixture worlds. */
public final class TerrainSiteSiegeProbe extends TacticalSiteSiegeProbe {
    private static final int DECK = 6, TURN_X = X - 6, TURN_Z = Z - 6, SHORE = X - 12;

    private boolean baseline;

    TerrainSiteSiegeProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            TerrainSiteSiegeProbe probe = new TerrainSiteSiegeProbe(new File(args[0]));
            probe.baseline = args.length == 2 && "baseline".equals(args[1]);
            if (probe.baseline) {
                probe.road("lit", true, 0);
                ModdedSettlementProbe.register(true);
                probe.waterfront("narrow", 0, true);
                System.exit(0);
            }
            probe.road("lit", true, 0);
            probe.road("dark", true, 0);
            probe.road("daylight", false, 0);
            probe.road("walkway", false, 5);
            probe.road("night-walkway", true, 5);
            probe.road("leaves", false, 18);
            probe.road("tunnel", false, 1);
            ModdedSettlementProbe.register(true);
            probe.waterfront("narrow", 0, true);
            probe.waterfront("wide", 1, false);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private void road(String name, boolean natural, int cover) throws Exception {
        int site = tacticalHouse("terrain-road-" + name, 64, false);
        toggle(X, Y, Z + 2);
        boolean lit = !"dark".equals(name);
        if (lit) world.g(X - 14, Y, Z + 2, 50);
        if (cover > 0) {
            for (int x = X - 16; x <= X - 11; x++) {
                world.g(x, Y + 2, Z + 2, cover);
                if (cover == 1) {
                    for (int side : new int[] {-1, 1}) {
                        world.g(x, Y, Z + 2 + side, 1);
                        world.g(x, Y + 1, Z + 2 + side, 1);
                        world.g(x, Y + 2, Z + 2 + side, 1);
                    }
                }
            }
        }
        long time = natural ? siegeDusk(0) : 6000;
        LunarCalendar calendar = siegeCalendar(140, time - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(natural, calendar);
        if (!natural) director.requestTestSiege(world, player, SiteSiege.Profile.AUTO);
        long start = waitForSiege(director, calendar, time);
        SiteSiege.Snapshot live = director.siteSiegeSnapshot(world);
        if (baseline) {
            check(!live.approach.startsWith((X - 14) + "," + Y + "," + (Z + 2)),
                    "archived SITE_SIEGE rejects the lit primary staging point", here());
            System.out.println("BASELINE illuminated staging rejected | " + siegeDetails(live));
            director.bind(null, null);
            return;
        }
        if (lit && cover == 0) {
            String[] point = live.approach.split(" > ")[0].split(",");
            int x = Integer.parseInt(point[0]), y = Integer.parseInt(point[1]), z = Integer.parseInt(point[2]);
            check(world.b(wl.b, x, y, z) > 7, "the actual siege staging point is torch-lit", here());
            xr ordinary = new xr(world);
            ordinary.c(x + 0.5, y, z + 0.5, 0, 0);
            for (int i = 0; i < 10; i++) check(!ordinary.i(), "ordinary vanilla spawn retains light rejection", here());
        }
        if (cover > 0) check(live.approach.startsWith((X - 14) + "," + Y + "," + (Z + 2)),
                "covered exterior remains the selected staging point | " + siegeDetails(live), here());
        siegeTicks(director, calendar, start, 202);
        SiteSiege.Snapshot moved = director.siteSiegeSnapshot(world);
        check(moved.site == site && moved.inserted >= 2 && moved.progress > 8 && world.chunkMisses == 0,
                "native movement on " + name + " road " + siegeDetails(moved), here());
        if (!natural) check(world.w() == start + 201, "debug deployment follows fixture clock", here());
        director.bind(null, null);
        System.out.println("PASS " + name + " road | " + siegeDetails(moved));
    }

    private void waterfront(String name, int halfWidth, boolean step) throws Exception {
        open("terrain-waterfront-" + name, false);
        prepare(X, Z);
        for (int x = SHORE + 1; x <= X + 16; x++)
            for (int z = Z - 18; z <= Z + 18; z++) world.g(x, Y, z, 9);
        for (int x = SHORE - 6; x <= SHORE; x++) {
            for (int z = Z - 11; z <= Z + 6; z++) {
                world.g(x, DECK - 2, z, 1);
                if (!step) world.g(x, DECK - 1, z, 1);
            }
        }
        room(X, DECK, Z, true);
        position(X + 2, DECK, Z + 2);
        walk();
        ticks(560);
        int site = recognized("waterfront " + name, true).id;
        bridge(halfWidth, true);
        ModdedSettlementProbe.installMacerator(world, X + 3, DECK, Z + 3);
        ady machineBefore = new ady();
        world.b(X + 3, DECK, Z + 3).b(machineBefore);
        siegeTerritory = new TerritorialActivityService();
        siegeTerritory.bind(world);
        world.d.add(player);
        world.auditChunks = true;
        for (int z = TURN_Z - 2; z <= TURN_Z + 2; z += 2)
            world.g(SHORE - 1, step ? DECK - 1 : DECK, z, 50);
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(150, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        beginSamples();
        boolean shoreSpawn = false, stage = false, crossTurn = false, tank = false;
        for (int tick = 0; tick < 1770; tick++) {
            player.l(20);
            siegeTicks(director, calendar, time++, 1);
            SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
            stage |= "CONNECTED_TERRAIN_ROUTE".equals(view.eligibility);
            for (Object entity : world.b) {
                if (!(entity instanceof yy) || ((yy) entity).G) continue;
                yy mob = (yy) entity;
                if (mob.V <= 1) {
                    check(mob.o < SHORE + 1 && mob.y.b >= DECK - 1 && mob.y.b <= DECK + 1,
                            "all actor insertions are directly above solid shore", here());
                    shoreSpawn = true;
                }
                crossTurn |= mob.o >= TURN_X && mob.q > TURN_Z + 4;
                tank |= mob instanceof EntityTank && mob.o >= TURN_X - 1 && mob.q >= TURN_Z + 3;
            }
            if (view.request == SiegeDirector.DebugState.REJECTED || view.request == SiegeDirector.DebugState.RESOLVED) break;
        }
        SiteSiege.Snapshot done = director.siteSiegeSnapshot(world);
        if (baseline) {
            check(done.inserted == 0, "archived layout cannot deploy on the same turned road", here());
            endSamples("waterfront-baseline");
            System.out.println("BASELINE waterfront | " + siegeDetails(done));
            director.bind(null, null);
            return;
        }
        ady machineAfter = new ady();
        world.b(X + 3, DECK, Z + 3).b(machineAfter);
        check(machineBefore.equals(machineAfter), "native machinery NBT remains identical", here());
        check(shoreSpawn && stage && crossTurn && done.site == site && done.wave == 3 && done.inserted >= 12
                        && done.planned == 18 && done.peak <= 8 && done.active == 0 && done.steps >= 15
                        && done.insertedRoles.containsKey("SKIRMISHER") && done.insertedRoles.containsKey("BREACHER")
                        && done.entranceChanges == 1 && world.chunkMisses == 0,
                "real waterfront force reaches its door through a turn on " + name + " bridge | " + siegeDetails(done), here());
        check(halfWidth == 0 ? !tank && done.failures.containsKey("TANK/TANK_CLEARANCE")
                        : tank && done.insertedRoles.containsKey("TANK"),
                "heavy deployment follows bridge width | " + siegeDetails(done), here());
        endSamples("waterfront-" + name);
        System.out.println("PASS waterfront " + name + " | " + siegeDetails(done) + " | " + done.deployment
                + " | route samples " + reading(done, "planningColumns") + ", valid " + reading(done, "validRoutes")
                + ", rejected " + reading(done, "failedRoutes") + " | planned " + done.plannedRoles
                + " | wave failures " + reading(done, "waveFailures"));
        if (halfWidth > 0) {
            director.bind(null, null);
            return;
        }

        toggle(X, DECK, Z + 2);
        bridge(halfWidth, false);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        siegeTicks(director, calendar, time, 160);
        time += 160;
        SiteSiege.Snapshot broken = director.siteSiegeSnapshot(world);
        check(broken.inserted == 0 && broken.request == SiegeDirector.DebugState.REJECTED,
                "broken bridge defeats the observed connection | " + siegeDetails(broken), here());
        bridge(halfWidth, true);
        world.hideChunk((X - 16) >> 4, (Z - 16) >> 4);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        siegeTicks(director, calendar, time, 160);
        time += 160;
        check(director.siteSiegeSnapshot(world).inserted == 0 && world.chunkMisses == 0,
                "missing bridge chunk defers or rejects without any unloaded reads", here());
        world.hideChunk(Integer.MIN_VALUE, Integer.MIN_VALUE);
        walk();
        ticks(100);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        siegeTicks(director, calendar, time, 250);
        SiteSiege.Snapshot restored = director.siteSiegeSnapshot(world);
        check(restored.inserted > 3, "restored loaded bridge permits deployment again | " + siegeDetails(restored)
                + " request=" + restored.requestReason, here());
        director.bind(null, null);
        System.out.println("PASS broken and unloaded bridge deny insertion, restored bridge reopens deployment");
    }

    private static Object reading(SiteSiege.Snapshot view, String field) throws Exception {
        return SiteSiege.Snapshot.class.getField(field).get(view);
    }

    private void bridge(int halfWidth, boolean intact) {
        for (int offset = -halfWidth; offset <= halfWidth; offset++) {
            for (int x = SHORE + 1; x <= TURN_X + halfWidth; x++)
                world.g(x, DECK - 1, TURN_Z + offset, intact || x != SHORE + 3 ? 1 : 0);
            for (int z = TURN_Z - halfWidth; z <= Z + 2; z++)
                world.g(TURN_X + offset, DECK - 1, z, 1);
            for (int x = TURN_X - halfWidth; x < X; x++)
                world.g(x, DECK - 1, Z + 2 + offset, 1);
        }
    }
}
