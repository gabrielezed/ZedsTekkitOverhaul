import java.io.File;
import java.util.ArrayList;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.EntityTank;
import zed.tekkitoverhaul.survival.tactical.EntityThief;
import zed.tekkitoverhaul.survival.tactical.ThiefTrail;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.territory.TerritorialWorldState;

/** Executes the actual staged force, with fixed saves and seeded territorial records. */
public final class SiteSiegeEscalationProbe extends TacticalSiteSiegeProbe {
    SiteSiegeEscalationProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            SiteSiegeEscalationProbe probe = new SiteSiegeEscalationProbe(new File(args[0]));
            probe.natural("normal-low", 2, false, 0, 6, 4, 2, SiteSiege.Intensity.LOW);
            probe.natural("hard-moderate", 3, true, 0, 10, 6, 3, SiteSiege.Intensity.MODERATE);
            probe.natural("hard-high", 3, true, 6, 14, 7, 3, SiteSiege.Intensity.HIGH);
            probe.natural("hard-maximum", 3, true, 32, 18, 8, 3, SiteSiege.Intensity.MAXIMUM);
            probe.manualMaximum(false);
            probe.manualMaximum(true);
            probe.narrowTankLane();
            probe.reinforcementPopulation();
            probe.unloadedCombatCache();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private TerritorialWorldState territory(boolean swamp, int score) {
        world.t.c = new fm(swamp ? abn.h : abn.b, 0.8F, 0.8F);
        siegeTerritory = new TerritorialActivityService();
        siegeTerritory.bind(world);
        TerritorialWorldState ledger = (TerritorialWorldState) world.a(TerritorialWorldState.class,
                "zto_territorial_activity");
        ady tag = new ady();
        tag.a("Schema", 1);
        tag.a("Grid", 1);
        tag.a("Dimension", 0);
        tag.a("Clock", 5000L);
        no regions = new no();
        if (score > 0) {
            for (int x = 4; x <= 5; x++) {
                for (int z = 4; z <= 5; z++) {
                    ady region = new ady();
                    region.a("X", x);
                    region.a("Z", z);
                    region.a("Presence", Math.min(24, score));
                    region.a("Returns", Math.max(0, score - 24));
                    region.a("Seen", 5000L);
                    regions.a(region);
                }
            }
        }
        tag.a("Regions", regions);
        ledger.a(tag);
        return ledger;
    }

    private void natural(String name, int difficulty, boolean swamp, int score, int total, int cap, int waves,
                         SiteSiege.Intensity intensity) throws Exception {
        int site = tacticalHouse("force-" + name, 64, false);
        toggle(X, Y, Z + 2);
        territory(swamp, score);
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(100, dusk - 1, Long.MIN_VALUE);
        world.q = difficulty;
        SiegeDirector director = siegeDirector(true, calendar);
        beginSamples();
        long time = waitForSiege(director, calendar, dusk);
        SiteSiege.Snapshot first = director.siteSiegeSnapshot(world);
        check(first.site == site && first.intensity == intensity && first.threat == (swamp ? 18 : 12) + score
                        && first.planned == total && first.activeCap == cap && first.waves == waves
                        && first.origin == SiteSiege.Origin.NATURAL && !first.overridden && first.difficulty == difficulty,
                "real site threat selects " + name + " | " + siegeDetails(first), here());
        int previousWave = 0, previousInserted = 0, wavesWithInsertions = 0;
        boolean sustained = false;
        boolean insertedThisWave = false, pulse = false, heavySwing = false;
        for (int age = 0; age < 1800; age++) {
            SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
            if (view.kind == SiegeDirector.EncounterKind.NONE) break;
            sustained |= view.peakLifetime > 560 && view.active >= 3;
            // A defended site makes room under the real cap for subsequent reserves.
            if (age > 580 && age % 24 == 0 && view.active >= cap - 1) {
                for (Object entity : new ArrayList<Object>(world.b)) {
                    if (entity instanceof yy && (entity.getClass().getSimpleName().equals("SiegeZombie")
                            || entity.getClass().getSimpleName().equals("DebugSiegeSkeleton")) && ((yy) entity).bb() > 0) {
                        ((yy) entity).a(md.a(player), 100);
                        break;
                    }
                }
            }
            if (view.wave > previousWave) {
                previousWave = view.wave;
                insertedThisWave = false;
            }
            if (view.inserted > previousInserted) {
                if (!insertedThisWave) wavesWithInsertions++;
                insertedThisWave = true;
                previousInserted = view.inserted;
            }
            if (intensity == SiteSiege.Intensity.MAXIMUM && age % 6 == 0) {
                EntityTank tank = live(EntityTank.class);
                EntitySkirmisher skirmisher = live(EntitySkirmisher.class);
                if (tank != null && tank.o >= X - 5 && !heavySwing)
                    player.c(tank.o + 2.5D, Y, tank.q + 0.25D, 0, 0);
                else if (skirmisher != null && skirmisher.o >= X - 7 && !pulse)
                    player.c(skirmisher.o + 1.0D, Y, skirmisher.q + 8.5D, 0, 0);
                else position(X + 2, Y, Z + 2);
            }
            player.l(20);
            siegeTicks(director, calendar, time + age, 1);
            pulse |= world.b.stream().anyMatch(entity -> entity instanceof EntityEnderPulse);
            EntityTank tank = live(EntityTank.class);
            heavySwing |= tank != null && tank.heavyPose(0) >= EntityTank.WINDUP;
        }
        SiteSiege.Snapshot done = director.siteSiegeSnapshot(world);
        int minimum = intensity == SiteSiege.Intensity.MAXIMUM ? 14 : Math.max(4, total - 4);
        check(done.kind == SiegeDirector.EncounterKind.NONE && done.wave == waves && wavesWithInsertions == waves
                        && done.inserted >= minimum && done.peak <= cap && done.progress > 8 && done.steps > 6
                        && done.active == 0 && world.chunkMisses == 0
                        && done.insertedRoles.containsKey("ZOMBIE") && done.insertedRoles.containsKey("SKELETON"),
                "deployed every wave with real native movement " + name + " | " + siegeDetails(done), here());
        if (intensity == SiteSiege.Intensity.MAXIMUM)
            check(sustained && done.sectorCount >= 2 && done.nativeDespawns == 0 && Integer.valueOf(1).equals(done.plannedRoles.get("SABOTEUR")) && pulse && heavySwing && done.insertedRoles.containsKey("TANK") && done.structuralChanges == 0,
                    "native pulse, heavy sweep, and safe combat Tank at maximum | pulse=" + pulse
                            + " swing=" + heavySwing + " | " + siegeDetails(done), here());
        endSamples(name);
        System.out.println("PASS " + name + " | " + siegeDetails(done) + " | " + done.deployment);
        director.bind(null, null);
    }

    private <T extends yy> T live(Class<T> type) {
        for (Object entity : world.b) {
            if (type.isInstance(entity) && ((yy) entity).bb() > 0 && !((yy) entity).G) return type.cast(entity);
        }
        return null;
    }

    private void manualMaximum(boolean reopen) throws Exception {
        String saveName = reopen ? "force-thief-reopen" : "force-manual-maximum";
        tacticalHouse(saveName, 64, false);
        TerritorialWorldState ledger = territory(false, 0);
        ady before = new ady();
        ledger.b(before);
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(101, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        SiteSiege.Snapshot plan = null;
        for (int seed = 0; seed < 24; seed++) {
            director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
            for (int tick = 0; tick < 42; tick++) {
                siegeTicks(director, calendar, time++, 1);
                plan = director.siteSiegeSnapshot(world);
                if (plan.planned > 0 || plan.request == SiegeDirector.DebugState.REJECTED) break;
            }
            if (plan.plannedRoles.containsKey("THIEF")) break;
            director.cancelTestSiege(world);
            siegeTicks(director, calendar, time++, 1);
        }
        check(plan != null && plan.overridden && plan.intensity == SiteSiege.Intensity.MAXIMUM
                        && plan.threat == 12 && plan.planned == 18 && plan.plannedRoles.containsKey("THIEF")
                        && plan.plannedRoles.containsKey("BREACHER") && plan.structuralBudget == 0,
                "manual override uses closed entrance and can reserve a single Thief | " + siegeDetails(plan), here());
        ady after = new ady();
        ledger.b(after);
        check(before.equals(after), "debug override preserves actual territorial records", here());
        ady payload = new ady();
        payload.a("Fixture", 7357);
        player.ap.a[20] = new aan(35, 3, 14);
        player.ap.a[20].d = payload;
        EntityThief thief = null;
        for (int tick = 0; tick < 540; tick++) {
            player.l(20);
            siegeTicks(director, calendar, time++, 1);
            thief = live(EntityThief.class);
            if (thief != null && director.siteSiegeSnapshot(world).entranceChanges > 0) break;
        }
        SiteSiege.Snapshot assault = director.siteSiegeSnapshot(world);
        check(thief != null && !thief.carryingLoot() && assault.entranceChanges == 1 && player.ap.a[20].a == 3,
                "Thief cannot take from behind cover, while Breacher can physically open the selected door | "
                        + siegeDetails(assault), here());
        for (int tick = 0; tick < 220 && !thief.carryingLoot(); tick++) {
            // The defender clears other roles first, then exposes a flank outside the building.
            for (Object entity : new ArrayList<Object>(world.b)) {
                if (entity != thief && entity instanceof yy && ((yy) entity).bb() > 0)
                    ((yy) entity).a(md.a(player), 100);
            }
            if (thief.o >= X - 7) player.c(thief.o, Y, thief.q + 1.65D, 0, 0);
            player.l(20);
            siegeTicks(director, calendar, time++, 1);
        }
        check(thief.carryingLoot() && player.ap.a[20].a == 2, "exposed defender permits one native theft | "
                + siegeDetails(director.siteSiegeSnapshot(world)), here());
        if (reopen) {
            java.lang.reflect.Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
            register.setAccessible(true);
            register.invoke(null, EntityThief.class, "ZTO.Thief", 218);
            save();
            director.bind(null, null);
            open(saveName, false);
            prepare(X, Z);
            player.a(world.B().h());
            world.d.add(player);
            thief = live(EntityThief.class);
            check(thief != null && player.ap.a[20].a == 2,
                    "save keeps the carrier and native inventory debit", here());
        } else {
            director.cancelTestSiege(world);
            siegeTicks(director, calendar, time, 1);
            check("DEBUG_CANCELLED".equals(director.siteSiegeSnapshot(world).outcome)
                            && director.siteSiegeSnapshot(world).active == 0, "manual cancellation ends ownership", here());
        }
        check(thief.bb() > 0 && !thief.G, "carrier survives tactical release", here());
        thief.a(md.a(player), 100);
        uh trail = ThiefTrail.inspect(world, player);
        if (reopen && trail != null) {
            // Follow the saved compass point and stand on the exterior surface before recovery.
            position(trail.a, Y, trail.c);
            trail = ThiefTrail.inspect(world, player);
        }
        check(trail == null && ThiefTrail.inspect(world, player) == null, "native recovery clears the trail once", here());
        int recovered = 0;
        for (Object entity : world.b) {
            if (entity instanceof fq) {
                aan item = ((fq) entity).a;
                if (item.c == 35 && item.i() == 14 && payload.equals(item.d)) recovered += item.a;
            }
        }
        check(recovered == 1, "one recovered item preserves ID, metadata and NBT", here());
        director.bind(null, null);
        System.out.println("PASS MAXIMUM + AUTO closed door, low real threat, physical theft and "
                + (reopen ? "save/reopen" : "cancellation") + " recovery | " + siegeDetails(assault));
    }

    private void narrowTankLane() throws Exception {
        tacticalHouse("force-narrow", 64, false);
        toggle(X, Y, Z + 2);
        // Leave the primary lane intact. Remove support under every alternate Tank footprint.
        for (int x = X - 17; x <= X - 7; x++) {
            for (int z = Z - 4; z <= Z + 8; z++) if (z != Z + 2) world.g(x, Y - 1, z, 0);
        }
        // Apply the same narrow footing to the other three exterior sectors.
        for (int forward = 7; forward <= 17; forward++) {
            for (int lateral = -5; lateral <= 9; lateral++) {
                if (lateral == 2) continue;
                world.g(X + 4 + forward, Y - 1, Z + lateral, 0);
                world.g(X + lateral, Y - 1, Z - forward, 0);
                world.g(X + lateral, Y - 1, Z + 4 + forward, 0);
            }
        }
        territory(false, 0);
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(102, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        for (int age = 0; age < 1500; age++) {
            player.l(20);
            siegeTicks(director, calendar, time + age, 1);
        }
        SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
        check(view.plannedRoles.containsKey("TANK") && !view.insertedRoles.containsKey("TANK")
                        && view.failures.containsKey("TANK/TANK_CLEARANCE") && view.inserted > 3 && view.wave == 3,
                "wide clearance prevents the Tank but allows native infantry reinforcements: " + siegeDetails(view), here());
        director.bind(null, null);
        System.out.println("PASS narrow Tank approach rejected | " + siegeDetails(view));
    }

    private void unloadedCombatCache() throws Exception {
        tacticalHouse("force-unloaded-combat-cache", 64, false);
        toggle(X, Y, Z + 2);
        territory(false, 0);
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(104, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        world.hideChunk((X + 32) >> 4, Z >> 4);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO);
        forceEntityTick = true;
        try {
            siegeTicks(director, calendar, time, 300);
        } finally {
            forceEntityTick = false;
        }
        SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
        check(view.progress > 13 && view.arrivals > 0 && world.chunkMisses == 0
                        && view.failures.keySet().stream().anyMatch(reason -> reason.toString().contains("COMBAT_CACHE_UNLOADED")),
                "loaded approach still moves while a native path cache is unavailable, misses=" + world.chunkMisses + " | " + siegeDetails(view), here());
        world.hideChunk(Integer.MIN_VALUE, Integer.MIN_VALUE);
        director.bind(null, null);
        System.out.println("PASS missing native combat cache defers pathfinding without loading chunks");
    }

    private void reinforcementPopulation() throws Exception {
        tacticalHouse("force-near-cap", 64, false);
        toggle(X, Y, Z + 2);
        territory(false, 0);
        long time = 14000;
        LunarCalendar calendar = siegeCalendar(103, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        siegeTicks(director, calendar, time, 120);
        time += 120;
        int inserted = director.siteSiegeSnapshot(world).inserted;
        check(inserted > 3, "vanguard before the population cap rises", here());
        // Keep the native population within every sampled insertion's local box.
        for (int x = X + 9; x <= X + 17; x++) {
            for (int z = Z + 6; z <= Z + 11; z++) {
                if (x != X + 9 && x != X + 17 && z != Z + 6 && z != Z + 11) continue;
                for (int y = Y; y < Y + 3; y++) world.g(x, y, z, 4);
            }
        }
        ArrayList<ajg> ordinary = new ArrayList<ajg>();
        for (int i = 0; i < 28; i++) {
            ajg mob = new ajg(world);
            mob.c(X + 10.5 + i % 7, Y, Z + 7.5 + i / 7, 0, 0);
            check(world.a(mob), "native ordinary hostile spawn", here());
            ordinary.add(mob);
        }
        for (int tick = 0; tick < 620; tick++) {
            player.l(20);
            siegeTicks(director, calendar, time++, 1);
        }
        SiteSiege.Snapshot capped = director.siteSiegeSnapshot(world);
        check(capped.inserted == inserted && capped.wave == 3 && capped.failures.keySet().stream()
                        .anyMatch(reason -> reason.toString().contains("POPULATION_CAP")),
                "local population can block a whole reinforcement wave | " + siegeDetails(capped), here());
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, time, 1);
        check(ordinary.stream().noneMatch(mob -> mob.G), "cleanup leaves unrelated ordinary hostiles alive", here());
        director.bind(null, null);
        System.out.println("PASS cap during reinforcement and unrelated hostile cleanup | " + siegeDetails(capped));
    }
}
