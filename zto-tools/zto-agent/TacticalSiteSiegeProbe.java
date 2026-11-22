import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import zed.tekkitoverhaul.assault.AssaultWorldState;
import zed.tekkitoverhaul.assault.SiegeCensus;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;
import zed.tekkitoverhaul.survival.aerial.AerialEcology;
import zed.tekkitoverhaul.survival.aerial.EntitySkelebird;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Exercises the exact local command mailbox and native tactical execution without rendering. */
public class TacticalSiteSiegeProbe extends SiteSiegeProbe {
    private final File root;

    TacticalSiteSiegeProbe(File root) throws Exception {
        super(root);
        this.root = root;
    }

    public static void main(String[] args) {
        try {
            TacticalSiteSiegeProbe probe = new TacticalSiteSiegeProbe(new File(args[0]));
            probe.manualSiege();
            probe.aerialSurvivor();
            probe.sideEntrance();
            probe.battlefieldHistory();
            if (args.length < 2) {
                probe.tacticalSieges();
                probe.naturalEntrance();
                probe.naturalArbitration();
                probe.undergroundAndPopulation();
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private void manualSiege() throws Exception {
        Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
        register.setAccessible(true);
        register.invoke(null, zed.tekkitoverhaul.assault.SiegeZombie.class, "ZTO.SiegeZombie", 215);
        register.invoke(null, zed.tekkitoverhaul.assault.DebugSiegeSkeleton.class, "ZTO.SiegeSkeleton", 216);
        register.invoke(null, zed.tekkitoverhaul.survival.tactical.EntityTank.class, "ZTO.Tank", 217);
        open("siege-debug-no-site", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        world.d.add(player);
        long day = 6000;
        LunarCalendar calendar = siegeCalendar(60, day, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO);
        siegeTicks(director, calendar, day, 1);
        check("NO_RECOGNIZED_SITE".equals(director.siteSiegeSnapshot(world).requestReason)
                        && world.b.isEmpty(), "no recognized site rejects without spawning", here());

        director.bind(null, null);
        open("siege-debug", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized("manual siege house", true).id;
        toggle(X, Y, Z + 2);
        world.d.add(player);
        calendar = siegeCalendar(60, day, Long.MIN_VALUE);
        director = siegeDirector(false, calendar);
        world.q = 0;
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        siegeTicks(director, calendar, day + 1, 1);
        check("PEACEFUL".equals(director.siteSiegeSnapshot(world).requestReason), "Peaceful rejection", here());
        world.q = 3;
        AssaultWorldState stored = (AssaultWorldState) world.a(AssaultWorldState.class, "zto_player_assault");
        Method siteStart = AssaultWorldState.class.getDeclaredMethod("startSite", int.class, long.class);
        Method siteClose = AssaultWorldState.class.getDeclaredMethod("closeSite", String.class);
        siteStart.setAccessible(true);
        siteClose.setAccessible(true);
        siteStart.invoke(stored, site, day - 1000);
        siteClose.invoke(stored, "FIXTURE_COOLDOWN");
        ady original = new ady();
        stored.b(original);
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        check(director.siteSiegeSnapshot(world).request == SiegeDirector.DebugState.REQUESTED
                        && director.siteSiegeSnapshot(world).inserted == 0, "button only queues", here());
        director.requestTestSiege(world, player, SiteSiege.Profile.BREACH);
        check("REQUEST_PENDING".equals(director.siteSiegeSnapshot(world).requestReason), "duplicate click is rejected", here());
        world.auditChunks = true;
        beginSamples();
        siegeTicks(director, calendar, day + 2, 185);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        check(world.w() == day + 186 && calendar.view().snapshot().phase.name().equals("NEW_MOON")
                        && active.origin == SiteSiege.Origin.MANUAL_DEBUG && active.request == SiegeDirector.DebugState.ACTIVE
                        && active.kind == SiegeDirector.EncounterKind.SITE_SIEGE && active.site == site
                        && active.profile == SiteSiege.Profile.PERIMETER && active.progress > 8
                        && active.inserted > 0 && active.inserted <= active.planned && active.active > 0
                        && world.chunkMisses == 0 && director.snapshot(world).starts == 0,
                "daytime New Moon siege with scheduling disabled and cooldown active: " + siegeDetails(active), here());
        int debugMobs = 0;
        for (Object entity : world.b) {
            if (!(entity instanceof yy) || !infantry(entity)) continue;
            yy mob = (yy) entity;
            ady saved = new ady();
            check(!mob.T() && mob.c(saved) && saved.f("ZTOSiegeSite") == site,
                    "debug infantry survives sun and saves its site", here());
            yy restored = (yy) aao.a(saved, world);
            check(restored != null && restored.getClass() == mob.getClass() && restored.bb() == mob.bb(),
                    "native registry restores infantry class and health", here());
            debugMobs++;
        }
        check(debugMobs == active.active, "actual owned hostile accounting", here());
        double px = player.o, py = player.p, pz = player.q;
        Method despawn = acq.class.getDeclaredMethod("w");
        despawn.setAccessible(true);
        player.c(px + 180, py, pz, 0, 0);
        for (Object actor : world.b) {
            if (!infantry(actor)) continue;
            despawn.invoke(actor);
            check(!((yy) actor).G, "owned infantry survives native distance despawn", here());
        }
        player.c(px, py, pz, 0, 0);
        endSamples("manual-daytime");

        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO);
        check("SITE_SIEGE_ACTIVE".equals(director.siteSiegeSnapshot(world).requestReason), "repeat during active siege", here());
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, day + 187, 5);
        check(director.siteSiegeSnapshot(world).request == SiegeDirector.DebugState.CANCELLED
                        && director.siteSiegeSnapshot(world).active == 0
                        && "DEBUG_CANCELLED".equals(director.siteSiegeSnapshot(world).outcome),
                "cancel releases ownership and resolves", here());
        for (Object entity : world.b) {
            if (infantry(entity))
                check(!((yy) entity).G, "cancel leaves infantry alive", here());
        }
        ady afterCancel = new ady();
        stored.b(afterCancel);
        check(original.equals(afterCancel), "cancel leaves the natural saved state identical", here());

        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        siegeTicks(director, calendar, day + 192, 35);
        int inserted = director.siteSiegeSnapshot(world).inserted;
        check(inserted > 0, "restart after cancel", here());
        position(X + 180, Y, Z + 2);
        siegeTicks(director, calendar, day + 227, 1);
        SiteSiege.Snapshot absent = director.siteSiegeSnapshot(world);
        check("PLAYER_ABSENT".equals(absent.outcome) && absent.inserted == inserted,
                "retreat stops further reinforcements", here());
        long surviving = world.b.stream().filter(TacticalSiteSiegeProbe::infantry)
                .filter(entity -> !((yy) entity).G).count();
        check(surviving > 0, "live siege infantry available for native chunk eviction", here());
        world.auditChunks = false;
        world.a(new uh(0, Y, 0));
        position(X + 512, Y, Z + 2);
        ko provider = (ko) world.z();
        java.util.Set<String> queued = new java.util.HashSet<String>();
        for (Object actor : world.b) {
            if (!infantry(actor)) continue;
            yy mob = (yy) actor;
            int cx = (int) Math.floor(mob.o) >> 4, cz = (int) Math.floor(mob.q) >> 4;
            if (queued.add(cx + "," + cz)) provider.d(cx, cz);
        }
        provider.a();
        world.n();
        check(world.b.stream().noneMatch(TacticalSiteSiegeProbe::infantry),
                "evicted infantry leave the loaded world list", here());
        position(X + 2, Y, Z + 2);
        prepare(X, Z);
        check(world.b.stream().filter(TacticalSiteSiegeProbe::infantry)
                        .filter(entity -> !((yy) entity).G).count() == surviving,
                "native chunk reload restores the infantry once", here());
        save();
        director.bind(null, null);
        open("siege-debug", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        world.d.add(player);
        long dusk = siegeDusk(0);
        calendar = siegeCalendar(61, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(false, calendar);
        ady reopened = new ady();
        ((AssaultWorldState) world.a(AssaultWorldState.class, "zto_player_assault")).b(reopened);
        check(original.equals(reopened) && director.siteSiegeSnapshot(world).kind == SiegeDirector.EncounterKind.NONE
                        && world.b.stream().filter(TacticalSiteSiegeProbe::infantry).count() == surviving
                        && settlements.resolve(world, site).coverage == Coverage.PARTIAL,
                "save/reopen restores infantry and preserves natural records | records match "
                        + original.equals(reopened) + " | " + siegeDetails(director.siteSiegeSnapshot(world))
                        + " | coverage " + settlements.resolve(world, site).coverage, here());
        siegeTicks(director, calendar, dusk, 1);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO);
        check("PLAYER_ASSAULT_ACTIVE".equals(director.siteSiegeSnapshot(world).requestReason)
                        && director.snapshot(world).starts == 1, "active player assault rejects a debug siege", here());
        world.q = 0;
        siegeTicks(director, calendar, dusk + 1, 2);
        check(world.b.stream().filter(TacticalSiteSiegeProbe::infantry).noneMatch(actor -> !((yy) actor).G),
                "Peaceful applies native hostile removal to restored siege infantry", here());
        System.out.println("PASS manual daytime, New Moon, cooldown, disabled toggle, duplicates, Peaceful, cancel, retreat and reopen "
                + siegeDetails(active));
    }

    private static boolean infantry(Object entity) {
        String type = entity.getClass().getSimpleName();
        return type.equals("DebugSiegeSkeleton") || type.equals("SiegeZombie");
    }

    private void aerialSurvivor() throws Exception {
        Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
        register.setAccessible(true);
        register.invoke(null, EntitySkelebird.class, "ZTO.Skelebird", 218);
        open("siege-aerial-survivor", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized("aerial siege cabin", true).id;
        toggle(X, Y, Z + 2);
        position(X - 1, Y, Z + 2);
        world.d.add(player);
        world.q = 3;
        long day = 6000;
        LunarCalendar calendar = siegeCalendar(73, day, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        TerritorialActivityService territory = new TerritorialActivityService();
        NaturalSpawnPressure spawning = new NaturalSpawnPressure(territory, 70, true);
        AerialEcology air = new AerialEcology(false, true, 90, territory, spawning);
        territory.bind(world);
        spawning.bind(world);
        air.bind(world);
        director.attachAerialEcology(air);
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER, SiteSiege.Intensity.MAXIMUM);
        long tick = day;
        while (tick < day + 720 && siteBird() == null) {
            siegeTicks(director, calendar, ++tick, 1);
            air.tick(player, false);
        }
        EntitySkelebird bird = siteBird();
        SiteSiege.Snapshot deployed = director.siteSiegeSnapshot(world);
        check(bird != null && bird.siegeSite() == site && deployed.activeAerial == 1
                && deployed.inserted <= deployed.planned && deployed.censusInserted == deployed.inserted
                && deployed.censusLoaded > 0, "siege spends one reserved air slot: "
                + deployed.aerialStatus + " | " + siegeDetails(deployed), here());
        beginSamples();
        siegeTicks(director, calendar, tick + 1, 96);
        tick += 96;
        endSamples("combined siege air and ground");
        bird.l(9);
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, ++tick, 1);
        check(director.siteSiegeSnapshot(world).activeAerial == 0
                && bird.encounterStatus().equals("Persistent siege survivor"), "release clears tactical control", here());
        double departureX = bird.o, departureZ = bird.q;
        siegeTicks(director, calendar, tick + 1, 1100);
        check(!bird.G && bird.bb() == 9 && (bird.o != departureX || bird.q != departureZ),
                "bird flies beyond the former wild lifetime", here());
        int cx = (int) Math.floor(bird.o) >> 4, cz = (int) Math.floor(bird.q) >> 4;
        int deathsBeforeEviction = director.siteSiegeSnapshot(world).censusDead;
        world.a(new uh(0, Y, 0));
        position(X + 512, Y, Z + 2);
        ko provider = (ko) world.z();
        provider.d(cx, cz);
        provider.a();
        world.n();
        SiteSiege.Snapshot evicted = director.siteSiegeSnapshot(world);
        check(siteBird() == null && evicted.censusUnloaded + evicted.censusUnknown > 0 && evicted.censusDead == deathsBeforeEviction,
                "native eviction removes the loaded observation without confirming death: bird still loaded " + (siteBird() != null)
                        + " | unloaded " + evicted.censusUnloaded + " | unknown " + evicted.censusUnknown
                        + " | dead " + deathsBeforeEviction + " -> " + evicted.censusDead, here());
        world.d(cx, cz);
        check(siteBird() != null && siteBird() != bird, "native chunk loading reconstructs the bird", here());
        save();
        director.bind(null, null);
        open("siege-aerial-survivor", false);
        world.d(cx, cz);
        bird = siteBird();
        check(bird != null && bird.siegeSite() == site && bird.bb() == 9
                && bird.encounterStatus().equals("Persistent siege survivor")
                && world.b.stream().filter(EntitySkelebird.class::isInstance).count() == 1,
                "Save/Quit reconstructs exactly one healthy, released siege bird", here());
        world.d.add(player);
        calendar = siegeCalendar(74, day, Long.MIN_VALUE);
        director = siegeDirector(false, calendar);
        SiteSiege.Snapshot reopened = director.siteSiegeSnapshot(world);
        check(reopened.censusSite == site && reopened.censusInserted == deployed.censusInserted
                && reopened.censusDead == deathsBeforeEviction && reopened.censusUnknown > 0 && reopened.censusReleased > 0,
                "recent insertions and releases persist across reopening before new observations", here());
        prepare((int) Math.floor(bird.o), (int) Math.floor(bird.q));
        bird.a(md.a(player), 100);
        siegeTicks(director, calendar, day, 2);
        check(bird.bb() <= 0 && director.siteSiegeSnapshot(world).censusDead == deathsBeforeEviction + 1,
                "native death after Save/Quit closes exactly one retained record", here());
        System.out.println("PASS native siege Skelebird slot, release, lifetime, flight, chunk eviction and saved identity");
    }

    private EntitySkelebird siteBird() {
        for (Object entity : world.b) if (entity instanceof EntitySkelebird) return (EntitySkelebird) entity;
        return null;
    }

    private void sideEntrance() throws Exception {
        tacticalHouse("siege-offset-east-door", 4, false);
        int x = X + 4, z = Z + 1;
        world.d(x, Y, z, 64, 0);
        world.d(x, Y + 1, z, 64, 8);
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(73, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        long windup = waitForPhase(director, calendar, time, "PRESSURING");
        check(director.siteSiegeSnapshot(world).objectiveTarget.equals(x + "," + Y + "," + z),
                "east door takes priority over western frontage", here());
        siegeTicks(director, calendar, windup, 70);
        check((world.e(x, Y, z) & 4) != 0, "Breacher opens the offset east door", here());
        world.d(x, Y, z, 64, 0);
        world.d(X, Y, Z + 2, 64, 4);
        world.d(X, Y + 1, Z + 2, 64, 8);
        siegeTicks(director, calendar, windup + 70, 75);
        SiteSiege.Snapshot after = director.siteSiegeSnapshot(world);
        check(after.reassessments > 0 && after.reassessments <= 8 && after.reassessment.startsWith("ALTERNATE_")
                && (world.e(x, Y, z) & 4) == 0 && (world.e(X, Y, Z + 2) & 4) != 0,
                "reclosed east door triggers bounded reassessment with the west door open: " + after.reassessment, here());
        System.out.println("PASS east entrance reclosed, alternate west approach reassessed in " + after.reassessments + " samples");
        director.bind(null, null);
    }

    private void battlefieldHistory() throws Exception {
        String name = "siege-battlefield-history";
        int outpost = X + 80;
        open(name, false);
        int[] sites = new int[2];
        int[] starts = {X, outpost};
        for (int i = 0; i < starts.length; ++i) {
            int x = starts[i];
            prepare(x, Z);
            room(x, Y, Z, true);
            world.g(x + 1, Y, Z + 1, 61);
            position(x + 2, Y, Z + 2);
            walk();
            ticks(560);
            sites[i] = recognized("battlefield history workshop " + (i + 1), true).id;
            toggle(x, Y, Z + 2);
        }
        check(sites[0] != sites[1], "history uses two independent workshops", here());
        world.d.add(player);
        long time = 14000;
        LunarCalendar calendar = siegeCalendar(81, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        profileHistory("no previous sieges", director, calendar, time);
        time += 320;
        int[] expected = new int[2];
        for (int run = 0; run < 4; ++run) {
            int index = run % sites.length;
            position(starts[index] + 2, Y, Z + 2);
            director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER, SiteSiege.Intensity.LOW);
            siegeTicks(director, calendar, time, 80);
            time += 80;
            SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
            expected[index] += active.inserted;
            check(active.site == sites[index] && active.inserted > 0 && active.censusInserted == expected[index]
                    && active.peak <= active.activeCap, "bounded independent site accounting: " + siegeDetails(active), here());
            director.cancelTestSiege(world);
            siegeTicks(director, calendar, time++, 1);
            SiteSiege.Snapshot released = director.siteSiegeSnapshot(world);
            check(released.active == 0 && released.censusReleased > 0 && released.censusLoaded > 0,
                    "history records actual surviving actors after tactical release", here());
        }
        int total = expected[0] + expected[1];
        profileHistory("four mobilizations across two sites", director, calendar, time);
        long saveStart = System.nanoTime();
        save();
        long saveMicros = (System.nanoTime() - saveStart) / 1000;
        long bytes = Files.size(new File(root, name + "/data/zto_siege_census.dat").toPath());
        check(bytes < 32768, "recent native census stays compact", here());
        director.bind(null, null);
        for (int round = 0; round < 2; ++round) {
            open(name, false);
            prepare(X, Z);
            prepare(outpost, Z);
            position(outpost + 2, Y, Z + 2);
            world.d.add(player);
            calendar = siegeCalendar(82 + round, time, Long.MIN_VALUE);
            director = siegeDirector(false, calendar);
            SiteSiege.Snapshot reopened = director.siteSiegeSnapshot(world);
            check(reopened.censusSite == sites[1] && reopened.censusInserted == expected[1]
                    && world.b.stream().filter(TacticalSiteSiegeProbe::infantry).count() == total,
                    "repeat native reopen preserves exact infantry and retained records", here());
            profileHistory("reopened " + (round + 1), director, calendar, time);
            save();
            director.bind(null, null);
        }
        check(Files.size(new File(root, name + "/data/zto_siege_census.dat").toPath()) == bytes,
                "unchanged census does not grow from reopening", here());
        SiegeCensus malformed = new SiegeCensus("fixture");
        ady bad = new ady();
        bad.a("Version", 1);
        bad.a("Records", 7);
        malformed.a(bad);
        malformed.b(new ady());
        System.out.printf(Locale.ROOT, "PASS recent history two sites, four mobilizations, two reopens, %d actors, census %d bytes, native save %d us%n",
                total, bytes, saveMicros);
    }

    private void profileHistory(String label, SiegeDirector director, LunarCalendar calendar, long time) {
        long[] nativeTicks = new long[256], snapshots = new long[256];
        siegeTicks(director, calendar, time, 64);
        for (int i = 0; i < nativeTicks.length; ++i) {
            long start = System.nanoTime();
            siegeTicks(director, calendar, time + i + 64, 1);
            nativeTicks[i] = System.nanoTime() - start;
            start = System.nanoTime();
            director.siteSiegeSnapshot(world);
            snapshots[i] = System.nanoTime() - start;
        }
        System.out.println("PROFILE history " + label + " | fixture " + percentiles(nativeTicks)
                + " | census and director snapshot " + percentiles(snapshots));
    }

    private static String percentiles(long[] samples) {
        Arrays.sort(samples);
        return String.format(Locale.ROOT, "n=%d us median=%.2f p95=%.2f p99=%.2f max=%.2f", samples.length,
                samples[127] / 1000.0, samples[243] / 1000.0, samples[253] / 1000.0, samples[255] / 1000.0);
    }

    private void tacticalSieges() throws Exception {
        long time = 6000;
        for (int entrance : new int[] {64, 107}) {
            int site = tacticalHouse("siege-entrance-" + entrance, entrance, false);
            LunarCalendar calendar = siegeCalendar(70, time, Long.MIN_VALUE);
            SiegeDirector director = siegeDirector(false, calendar);
            director.requestTestSiege(world, player, entrance == 64 ? SiteSiege.Profile.BREACH : SiteSiege.Profile.AUTO);
            beginSamples();
            long windup = waitForPhase(director, calendar, time, "PRESSURING");
            SiteSiege.Snapshot pressure = director.siteSiegeSnapshot(world);
            check(pressure.site == site && pressure.specialist.equals("BREACHER") && pressure.entranceChanges == 0
                            && world.a(X, Y, Z + 2) == entrance && (world.e(X, Y, Z + 2) & 4) == 0,
                    "physical closed entrance windup: " + siegeDetails(pressure), here());
            siegeTicks(director, calendar, windup, 75);
            SiteSiege.Snapshot opened = director.siteSiegeSnapshot(world);
            check(opened.entranceChanges == 1 && opened.structuralChanges == 0 && opened.arrivals > 0
                            && "ENTRANCE_OPENED".equals(opened.objectiveOutcome)
                            && (world.e(X, Y, Z + 2) & 4) != 0 && world.chunkMisses == 0,
                    "native entrance opened and party advances through it: " + siegeDetails(opened), here());
            endSamples("entrance-" + entrance);
            director.bind(null, null);
        }

        for (String fixture : new String[] {"blocked", "single", "double", "killed", "replacement", "reload", "combat", "unloaded"}) {
            int site = tacticalHouse("siege-cobble-" + fixture, 4, fixture.equals("double"));
            LunarCalendar calendar = siegeCalendar(71, time, Long.MIN_VALUE);
            SiegeDirector director = new SiegeDirector(true, 15, 900, 8, 16, false, false, false);
            director.attachSites(false, !fixture.equals("blocked"), settlements);
            director.bind(world, calendar.view());
            director.requestTestSiege(world, player, SiteSiege.Profile.BREACH);
            if (fixture.equals("blocked")) {
                siegeTicks(director, calendar, time, 55);
                check("STRUCTURAL_BREACH_DISABLED".equals(director.siteSiegeSnapshot(world).requestReason)
                                && world.a(X, Y, Z + 2) == 4 && director.siteSiegeSnapshot(world).inserted == 0,
                        "BREACH does not authorize structural edits", here());
                director.bind(null, null);
                continue;
            }
            beginSamples();
            long windup = waitForPhase(director, calendar, time, "BREACHING");
            SiteSiege.Snapshot pressure = director.siteSiegeSnapshot(world);
            check(pressure.site == site && pressure.specialist.equals("TANK") && pressure.structuralChanges == 0
                            && world.a(X, Y, Z + 2) == 4 && world.a(X, Y + 1, Z + 2) == 4,
                    "Tank reaches its assigned wall before the windup: " + siegeDetails(pressure), here());
            if (fixture.equals("killed") || fixture.equals("combat")) {
                for (Object entity : world.b) {
                    if (entity.getClass().getSimpleName().equals("EntityTank"))
                        ((yy) entity).a(md.a(player), fixture.equals("killed") ? 100 : 5);
                }
            } else if (fixture.equals("replacement")) {
                world.g(X, Y + 1, Z + 2, 42);
            } else if (fixture.equals("unloaded")) {
                world.hideChunk(X >> 4, Z >> 4);
                // Tick the authority without native physics touching the artificially hidden chunk.
                world.a(windup);
                calendar.observe(windup);
                director.tick(player, false, null);
                world.hideChunk(Integer.MIN_VALUE, Integer.MIN_VALUE);
                SiteSiege.Snapshot result = director.siteSiegeSnapshot(world);
                check(result.kind == SiegeDirector.EncounterKind.NONE && result.structuralChanges == 0
                                && world.chunkMisses == 0 && world.a(X, Y, Z + 2) == 4,
                        "unloaded target cannot be retrieved or edited: " + siegeDetails(result)
                                + " | " + result.outcome + " | chunk misses " + world.chunkMisses, here());
                endSamples("structural-unloaded");
                director.bind(null, null);
                continue;
            } else if (fixture.equals("reload")) {
                save();
                director.bind(null, null);
                open("siege-cobble-reload", false);
                prepare(X, Z);
                position(X + 2, Y, Z + 2);
                world.d.add(player);
                director = siegeDirector(false, calendar);
                siegeTicks(director, calendar, windup, 80);
                check(director.siteSiegeSnapshot(world).inserted == 0
                                && world.a(X, Y, Z + 2) == 4 && world.a(X, Y + 1, Z + 2) == 4
                                && world.b.stream().anyMatch(e -> e.getClass().getSimpleName().equals("EntityTank")),
                        "save/reopen during windup preserves the Tank without delayed impact", here());
                director.bind(null, null);
                endSamples("structural-reload");
                continue;
            }
            siegeTicks(director, calendar, windup, 70);
            SiteSiege.Snapshot result = director.siteSiegeSnapshot(world);
            if (fixture.equals("single") || fixture.equals("double")) {
                check(result.structuralChanges == 2 && result.entranceChanges == 0 && result.peak <= result.activeCap
                                && world.a(X, Y, Z + 2) == 0 && world.a(X, Y + 1, Z + 2) == 0
                                && (fixture.equals("double") ? "PASSAGE_BLOCKED" : "STRUCTURE_BREACHED").equals(result.objectiveOutcome),
                        "two native removals and no further damage: " + siegeDetails(result), here());
                if (fixture.equals("double"))
                    check("PASSAGE_BLOCKED".equals(result.objectiveOutcome) && world.a(X + 1, Y, Z + 2) == 4
                                    && world.a(X + 1, Y + 1, Z + 2) == 4,
                            "a second wall layer defeats the breach", here());
                else
                    check(result.arrivals > 0 && result.approach.endsWith("321,4,322"),
                            "skeletons can walk through a verified breach: " + siegeDetails(result), here());
            } else {
                String cause = fixture.equals("killed") ? "DEATH"
                        : fixture.equals("combat") ? "WINDUP_INTERRUPTED" : "TARGET_CHANGED";
                check(cause.equals(result.objectiveOutcome) && result.structuralChanges == 0 && world.a(X, Y, Z + 2) == 4
                                && world.a(X, Y + 1, Z + 2) == (fixture.equals("replacement") ? 42 : 4),
                        "interruption prevents impact: " + cause + " | " + siegeDetails(result), here());
            }
            check(world.chunkMisses == 0 && result.peakColumns <= 28 && result.breachChecks > 0,
                    "tactical planning and execution stay bounded", here());
            endSamples("structural-" + fixture);
            director.bind(null, null);
        }
        System.out.println("PASS tactical doors, gates, sealed and two-layer walls, opt-in, windup counterplay and reload");
    }

    @SuppressWarnings("unchecked")
    int tacticalHouse(String name, int front, boolean secondLayer) throws Exception {
        open(name, false);
        prepare(X, Z);
        room(X, Y, Z, front == 64);
        if (front == 107) {
            player.u = -90;
            place(107, X, Y - 1, Z + 2, 1);
        } else if (front == 4) {
            world.g(X, Y, Z + 2, 4);
            world.g(X, Y + 1, Z + 2, 4);
            if (secondLayer) {
                world.g(X + 1, Y, Z + 2, 4);
                world.g(X + 1, Y + 1, Z + 2, 4);
            }
        }
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized(name, true).id;
        world.d.add(player);
        world.auditChunks = true;
        return site;
    }

    long waitForPhase(SiegeDirector director, LunarCalendar calendar, long time, String phase) {
        for (long tick = time; tick < time + 1100; tick++) {
            siegeTicks(director, calendar, tick, 1);
            SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
            if (view.tacticalPhase.equals(phase)) return tick + 1;
            if (view.request == SiegeDirector.DebugState.RESOLVED || view.request == SiegeDirector.DebugState.REJECTED) break;
        }
        SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
        throw new AssertionError("missing " + phase + " | " + siegeDetails(view) + " | " + view.outcome
                + " | " + view.objective + " | target " + view.objectiveTarget + " | reason " + view.requestReason);
    }

    private void naturalEntrance() throws Exception {
        int site = tacticalHouse("siege-natural-entrance", 64, false);
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(83, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        long first = waitForSiege(director, calendar, dusk);
        long windup = waitForPhase(director, calendar, first, "PRESSURING");
        siegeTicks(director, calendar, windup, 70);
        SiteSiege.Snapshot opened = director.siteSiegeSnapshot(world);
        check(opened.origin == SiteSiege.Origin.NATURAL && opened.site == site && opened.entranceChanges == 1
                        && opened.structuralChanges == 0 && opened.arrivals > 0 && director.snapshot(world).starts == 0,
                "natural AUTO uses the same entrance tactic: " + siegeDetails(opened), here());
        director.bind(null, null);
        System.out.println("PASS natural AUTO shares the Breacher's physical approach and native entrance transition");
    }

    private void naturalArbitration() throws Exception {
        tacticalHouse("siege-natural-debug-arbitration", 64, false);
        toggle(X, Y, Z + 2);
        long dusk = siegeDusk(0), before = dusk - 65;
        LunarCalendar calendar = siegeCalendar(84, before, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        siegeTicks(director, calendar, before, 66);
        check(director.siteSiegeSnapshot(world).request == SiegeDirector.DebugState.ACTIVE
                        && director.snapshot(world).starts == 0 && calendar.watermark() == dusk,
                "natural opportunity passes during active manual siege", here());
        AssaultWorldState saved = (AssaultWorldState) world.a(AssaultWorldState.class, "zto_player_assault");
        ady natural = new ady();
        saved.b(natural);
        check(natural.g("Boundary") == Long.MIN_VALUE, "debug never writes the major boundary", here());
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, dusk + 1, 25);
        check(director.snapshot(world).starts == 0, "passed boundary cannot replay after cancel", here());

        long next = dusk + 8L * LunarSnapshot.DAY;
        director.bind(null, null);
        calendar = siegeCalendar(85, next - 1, dusk);
        director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        siegeTicks(director, calendar, next, 1);
        check(director.snapshot(world).starts == 1
                        && "MAJOR_ENCOUNTER_RESERVED".equals(director.siteSiegeSnapshot(world).requestReason),
                "natural dusk takes precedence over a queued request that has not begun planning", here());
        director.bind(null, null);
        System.out.println("PASS queued and active debug arbitration without changing lunar watermarks or replay");
    }

    private void undergroundAndPopulation() throws Exception {
        int site = tacticalHouse("siege-underground", 64, false);
        // Bury the site under the center of a supported, higher surface.
        for (int x = X - 16; x <= X + 20; x++) {
            for (int z = Z - 16; z <= Z + 20; z++) {
                world.g(x, Y + 4, z, 1);
                if (x == X - 16 || x == X + 20 || z == Z - 16 || z == Z + 20)
                    for (int y = Y; y < Y + 4; y++) world.g(x, y, z, 1);
            }
        }
        long day = 5500;
        LunarCalendar calendar = siegeCalendar(87, day, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        siegeTicks(director, calendar, day, 100);
        SiteSiege.Snapshot rejected = director.siteSiegeSnapshot(world);
        check(rejected.site == site && rejected.inserted == 0 && world.chunkMisses == 0
                        && rejected.request == SiegeDirector.DebugState.REJECTED,
                "an underground site cannot insert through an unsupported surface route: " + siegeDetails(rejected), here());
        director.bind(null, null);

        tacticalHouse("siege-local-population-cap", 64, false);
        toggle(X, Y, Z + 2);
        calendar = siegeCalendar(88, day, Long.MIN_VALUE);
        director = siegeDirector(false, calendar);
        world.auditChunks = false;
        for (int i = 0; i < 28; i++) {
            xr mob = new xr(world);
            mob.c(X + 2.5, Y + 8, Z + 2.5, 0, 0);
            check(world.a(mob), "native local hostile for cap check", here());
        }
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        for (int tick = 0; tick < 100; tick++) {
            world.a(day + tick);
            ticks(1);
            director.tick(player, false, null);
        }
        rejected = director.siteSiegeSnapshot(world);
        check(rejected.inserted == 0 && "POPULATION_CAP".equals(rejected.requestReason),
                "local population pressure blocks debug admission: " + siegeDetails(rejected), here());
        director.bind(null, null);
        System.out.println("PASS underground geometry and native local hostile cap reject manual insertion");
    }
}
