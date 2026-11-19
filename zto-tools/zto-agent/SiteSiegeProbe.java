import java.io.File;
import zed.tekkitoverhaul.assault.AssaultWorldState;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;

/** Native simulation using the same disposable world and recognition setup as the settlement suite. */
public final class SiteSiegeProbe extends SettlementProbe {
    private SiteSiegeProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            new SiteSiegeProbe(new File(args[0])).siteSiege();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    private void siteSiege() throws Exception {
        open("siege-no-site", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        world.d.add(player);
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(1, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, dusk, 1);
        check(director.snapshot(world).starts == 1 && director.siteSiegeSnapshot(world).kind == SiegeDirector.EncounterKind.PLAYER_ASSAULT,
                "no recognized site preserves the natural assault", here());

        open("siege-house", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int house = recognized("siege house", true).id;
        toggle(X, Y, Z + 2);
        world.d.add(player);
        calendar = siegeCalendar(2, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(false, calendar);
        siegeTicks(director, calendar, dusk, 1);
        check(director.snapshot(world).starts == 1 && "DISABLED".equals(director.siteSiegeSnapshot(world).eligibility),
                "disabled siege leaves a recognized house on the legacy path", here());

        // Advance the real lunar authority to a deterministic eligible residue in this marked fixture.
        dusk = siegeDusk(3);
        director.bind(null, null);
        calendar = siegeCalendar(3, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        world.auditChunks = true;
        long afterAdmission = waitForSiege(director, calendar, dusk);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        check(active.kind == SiegeDirector.EncounterKind.SITE_SIEGE && active.site == house && active.inserted > 0
                        && director.snapshot(world).starts == 0 && "OPENING".equals(active.objective),
                "open entrance admits a committed siege instead of assault: " + siegeDetails(active), here());
        String fixedApproach = active.approach;
        String[] start = fixedApproach.split(" > ")[0].split(",");
        SiteSnapshot.Bounds envelope = settlements.resolve(world, house).envelope;
        check(Integer.parseInt(start[0]) < envelope.minX - 2,
                "opening lane inserted from the west exterior: " + fixedApproach, here());
        position(X + 4, Y, Z + 1);
        siegeTicks(director, calendar, afterAdmission, 100);
        active = director.siteSiegeSnapshot(world);
        check(active.site == house && fixedApproach.equals(active.approach) && active.progress >= 4 && active.steps >= 4
                        && active.peak <= 3 && active.inserted <= 3 && active.peakColumns <= 28
                        && active.peakSkyReads <= 255 && here().peakReads <= 169 && world.chunkMisses == 0,
                "native movement stays site-directed and within work/population limits: " + siegeDetails(active), here());
        siegeTicks(director, calendar, afterAdmission + 100, 105);
        active = director.siteSiegeSnapshot(world);
        check(active.arrivals > 0 && active.progress >= 14.5,
                "native task crossed the open threshold: " + siegeDetails(active), here());
        long cooldown = active.cooldownUntil;
        check(cooldown == dusk + 16L * LunarSnapshot.DAY, "two-cycle cooldown", here());
        System.out.println("PASS native site movement " + siegeDetails(active) + " | chunk misses " + world.chunkMisses
                + " | recognition peak " + here().peakReads);
        save();

        open("siege-house", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        world.d.add(player);
        calendar = siegeCalendar(4, dusk - 1, dusk);
        director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, dusk, 4);
        SiteSiege.Snapshot interrupted = director.siteSiegeSnapshot(world);
        check("INTERRUPTED_ON_REOPEN".equals(interrupted.outcome) && interrupted.cooldownUntil == cooldown
                        && interrupted.site == house && director.snapshot(world).starts == 0 && interrupted.inserted == 0,
                "reopen preserves cooldown and admission without replay: " + siegeDetails(interrupted), here());

        // Force a natural-looking replay into the director with an independent fixture calendar.
        // The persisted major boundary must reject it even without the earlier lunar watermark.
        director.bind(null, null);
        calendar = siegeCalendar(5, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, dusk, 1);
        check(director.snapshot(world).starts == 0 && director.siteSiegeSnapshot(world).inserted == 0,
                "major watermark independently prevents replay", here());
        director.bind(null, null);
        long earlierDusk = dusk + LunarSnapshot.DAY;
        calendar = siegeCalendar(6, earlierDusk - 1, dusk, 23);
        director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, earlierDusk, 1);
        check("SITE_COOLDOWN".equals(director.siteSiegeSnapshot(world).rejection)
                        && director.snapshot(world).starts == 1,
                "persisted site cooldown preserves fallback on a distinct eligible boundary", here());
        System.out.println("PASS disabled and missing-site fallback, mutual exclusion, reopen, two-cycle cooldown, replay");

        open("siege-sealed", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        house = recognized("sealed siege house", true).id;
        world.d.add(player);
        dusk = siegeDusk(0);
        calendar = siegeCalendar(7, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        afterAdmission = waitForSiege(director, calendar, dusk);
        siegeTicks(director, calendar, afterAdmission, 120);
        SiteSiege.Snapshot sealed = director.siteSiegeSnapshot(world);
        check(house == sealed.site && "FRONTAGE".equals(sealed.objective) && sealed.progress >= 4
                        && (world.e(X, Y, Z + 2) & 4) == 0,
                "sealed entry preserves an exterior frontage: " + siegeDetails(sealed), here());
        position(X + 80, Y, Z + 2);
        siegeTicks(director, calendar, afterAdmission + 120, 1);
        check("PLAYER_ABSENT".equals(director.siteSiegeSnapshot(world).outcome)
                        && director.siteSiegeSnapshot(world).active == 0 && director.snapshot(world).starts == 0,
                "leaving releases siege authority and cannot start a second encounter", here());
        System.out.println("PASS sealed frontage, unchanged wooden door and player absence " + siegeDetails(sealed));

        open("siege-partial", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        house = recognized("partial siege house", true).id;
        toggle(X, Y, Z + 2);
        save();
        open("siege-partial", false);
        world.d(X >> 4, Z >> 4);
        position(X + 18, Y, Z + 2);
        world.d.add(player);
        dusk = siegeDusk(0);
        calendar = siegeCalendar(8, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        world.auditChunks = true;
        siegeTicks(director, calendar, dusk, 38);
        SiteSiege.Snapshot unloaded = director.siteSiegeSnapshot(world);
        check(unloaded.inserted == 0 && world.chunkMisses == 0 && director.snapshot(world).starts == 1
                        && "NO_VALID_APPROACH".equals(unloaded.eligibility),
                "unloaded routes safely fall back without fetching chunks: " + siegeDetails(unloaded), here());

        open("siege-partial", false);
        prepare(X, Z);
        position(X + 18, Y, Z + 2);
        world.d.add(player);
        check(settlements.resolve(world, house).coverage == Coverage.PARTIAL,
                "reopened site begins with historical partial evidence", here());
        dusk = siegeDusk(3);
        calendar = siegeCalendar(9, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        afterAdmission = waitForSiege(director, calendar, dusk);
        active = director.siteSiegeSnapshot(world);
        check(active.site == house && "PARTIAL".equals(active.coverage) && "OPENING".equals(active.objective)
                        && active.kind == SiegeDirector.EncounterKind.SITE_SIEGE,
                "partial recognized site admits locally verified route: " + siegeDetails(active), here());
        // Close the exterior defensive line across the chosen lane, beyond the recognized envelope.
        for (int z = Z - 3; z <= Z + 7; z++) {
            world.g(X - 3, Y, z, 5);
            world.g(X - 3, Y + 1, z, 5);
        }
        siegeTicks(director, calendar, afterAdmission, 160);
        SiteSiege.Snapshot blocked = director.siteSiegeSnapshot(world);
        check("ROUTE_LOST".equals(blocked.outcome) && blocked.arrivals == 0 && blocked.progress < 14
                        && director.snapshot(world).starts == 0 && world.a(X - 3, Y, Z + 2) == 5,
                "new obstruction stops approach before wall with no fallback or damage: " + siegeDetails(blocked), here());
        System.out.println("PASS unloaded fallback, locally verified partial site and route invalidation " + siegeDetails(blocked));

        // The optional siege extension must never reset a readable legacy major boundary.
        open("siege-malformed", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        recognized("malformed-data siege house", true);
        world.d.add(player);
        dusk = siegeDusk(0);
        ady legacy = new ady();
        legacy.a("Boundary", dusk);
        legacy.a("Open", false);
        AssaultWorldState decoded = new AssaultWorldState("zto_player_assault");
        decoded.a(legacy);
        ady roundtrip = new ady();
        decoded.b(roundtrip);
        check(roundtrip.g("Boundary") == dusk && !roundtrip.c("SiteSiege"),
                "legacy NBT roundtrips without acquiring a siege extension", here());
        legacy.a("SiteSiege", -1);
        decoded.a(legacy);
        world.a("zto_player_assault", decoded);
        calendar = siegeCalendar(10, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, dusk, 1);
        check(director.snapshot(world).starts == 0, "malformed siege extension preserves replay guard", here());
        director.bind(null, null);
        dusk = siegeDusk(3);
        calendar = siegeCalendar(11, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, dusk, 1);
        check("SITE_STORAGE_UNAVAILABLE".equals(director.siteSiegeSnapshot(world).rejection)
                        && director.snapshot(world).starts == 1,
                "malformed siege data fails closed and preserves player assault eligibility", here());
        System.out.println("PASS legacy major NBT and malformed optional siege data");
    }

    private long siegeDusk(int cycles) {
        long seed = world.v();
        long residue = Math.floorMod(seed ^ (seed >>> 32), 3);
        return ((residue + cycles) * 8 + 4) * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
    }

    private LunarCalendar siegeCalendar(long session, long time, long watermark) {
        return siegeCalendar(session, time, watermark, 0);
    }

    private LunarCalendar siegeCalendar(long session, long time, long watermark, long adjustment) {
        LunarCalendar calendar = new LunarCalendar(session, 0, adjustment, watermark, () -> {}, failure -> {
            throw new AssertionError(failure);
        });
        calendar.rebaseline(0, time, LunarEvent.Cause.LOADED);
        world.q = 3;
        world.a(time);
        world.q();
        return calendar;
    }

    private SiegeDirector siegeDirector(boolean enabled, LunarCalendar calendar) {
        SiegeDirector director = new SiegeDirector(true, 15, 900, 8, 16, false, false, false);
        director.attachSites(enabled, settlements);
        director.bind(world, calendar.view());
        return director;
    }

    private long waitForSiege(SiegeDirector director, LunarCalendar calendar, long dusk) {
        for (long time = dusk; time < dusk + 1100; time++) {
            siegeTicks(director, calendar, time, 1);
            if (director.siteSiegeSnapshot(world).kind == SiegeDirector.EncounterKind.SITE_SIEGE || director.snapshot(world).starts > 0)
                return time + 1;
        }
        throw new AssertionError("admission deadline: " + siegeDetails(director.siteSiegeSnapshot(world)));
    }

    private void siegeTicks(SiegeDirector director, LunarCalendar calendar, long from, int count) {
        for (long time = from; time < from + count; time++) {
            world.a(time);
            world.q();
            for (Object entity : new java.util.ArrayList<Object>(world.b)) {
                if (entity instanceof xr) world.f((xr) entity);
            }
            ticks(1);
            calendar.observe(time);
            director.tick(player, false, null);
        }
    }

    private static String siegeDetails(SiteSiege.Snapshot view) {
        return String.format("%s/%s site=%d %s %s %s | %s | inserted=%d active=%d progress=%.2f steps=%d arrivals=%d"
                        + " | lanes=%d columns=%d peak=%d sky=%d peak=%d protection=%d | %d/%d us total/peak",
                view.kind, view.phase, view.site, view.coverage, view.eligibility, view.rejection, view.approach,
                view.inserted, view.active, view.progress, view.steps, view.arrivals, view.candidates, view.columns,
                view.peakColumns, view.skyReads, view.peakSkyReads, view.protectionReads,
                view.planningNanos / 1000, view.peakPlanningNanos / 1000);
    }

}
