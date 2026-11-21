import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import zed.tekkitoverhaul.assault.AssaultWorldState;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Native simulation using the same disposable world and recognition setup as the settlement suite. */
public class SiteSiegeProbe extends SettlementProbe {
    private final Samples planner = new Samples(), recognition = new Samples(), movement = new Samples();
    private final Samples activeTicks = new Samples();
    private long lastPlannerNanos, firstMeasuredTick, peakPlannerTick, peakPlannerNanos;
    private boolean measured;
    private int peakBlockReads, peakMetadataReads, peakMobs;
    private double peakStep;
    TerritorialActivityService siegeTerritory;
    boolean forceEntityTick;
    private final Samples totalTicks = new Samples();

    SiteSiegeProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            SiteSiegeProbe probe = new SiteSiegeProbe(new File(args[0]));
            probe.siteSiege();
            probe.compound(false);
            probe.industrialFallback();
            probe.warmHouse();
            probe.planningTransitions();

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
        // Independent exterior wall with a single aligned opening.
        for (int z = Z - 3; z <= Z + 7; z++) {
            if (z == Z + 2) continue;
            world.g(X - 3, Y, z, 5);
            world.g(X - 3, Y + 1, z, 5);
        }
        world.auditChunks = true;
        beginSamples();
        long afterAdmission = waitForSiege(director, calendar, dusk);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        check(active.kind == SiegeDirector.EncounterKind.SITE_SIEGE && active.site == house && active.inserted > 0
                        && director.snapshot(world).starts == 0 && "APPROACH_ENTRANCE".equals(active.objective),
                "open entrance admits a committed siege instead of assault: " + siegeDetails(active), here());
        String fixedApproach = active.approach;
        String[] start = fixedApproach.split(" > ")[0].split(",");
        SiteSnapshot.Bounds envelope = settlements.resolve(world, house).envelope;
        check(Integer.parseInt(start[0]) < envelope.minX - 2,
                "opening lane inserted from the west exterior: " + fixedApproach, here());
        position(X + 3, Y, Z + 2);
        siegeTicks(director, calendar, afterAdmission, 100);
        active = director.siteSiegeSnapshot(world);
        check(active.site == house && fixedApproach.equals(active.approach) && active.progress >= 4 && active.steps >= 4
                        && active.peak <= active.activeCap && active.inserted <= active.planned && active.peakColumns <= 28
                        && active.peakSkyReads <= 255 && here().peakReads <= 169 && world.chunkMisses == 0,
                "native movement stays site-directed and within work/population limits: " + siegeDetails(active), here());
        siegeTicks(director, calendar, afterAdmission + 100, 105);
        active = director.siteSiegeSnapshot(world);
        check(active.arrivals > 0 && active.progress >= 14.5,
                "native task crossed the open threshold: " + siegeDetails(active), here());
        for (int tick = 0; tick < 175 && world.b.stream().noneMatch(entity -> entity instanceof nm); tick++)
            siegeTicks(director, calendar, afterAdmission + 205 + tick, 1);
        check(world.b.stream().anyMatch(entity -> entity instanceof nm),
                "native ranged combat resumes after arrival: " + siegeDetails(director.siteSiegeSnapshot(world)), here());
        endSamples("open-walled-house");
        long cooldown = active.cooldownUntil;
        check(cooldown == dusk + 16L * LunarSnapshot.DAY, "two-cycle cooldown", here());
        System.out.println("PASS native site movement " + siegeDetails(active) + " | chunk misses " + world.chunkMisses
                + " | recognition peak " + here().peakReads);
        measureSave();

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
        room(X, Y, Z, false);
        world.g(X, Y, Z + 2, 4);
        world.g(X, Y + 1, Z + 2, 4);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        house = recognized("sealed siege house", true).id;
        world.d.add(player);
        dusk = siegeDusk(0);
        calendar = siegeCalendar(7, dusk - 1, Long.MIN_VALUE);
        director = siegeDirector(true, calendar);
        beginSamples();
        afterAdmission = waitForSiege(director, calendar, dusk);
        siegeTicks(director, calendar, afterAdmission, 120);
        SiteSiege.Snapshot sealed = director.siteSiegeSnapshot(world);
        check(house == sealed.site && "PRESSURE_PERIMETER".equals(sealed.objective) && sealed.progress >= 4
                        && world.a(X, Y, Z + 2) == 4,
                "sealed wall preserves an exterior frontage: " + siegeDetails(sealed), here());
        position(X + 80, Y, Z + 2);
        siegeTicks(director, calendar, afterAdmission + 120, 1);
        check("PLAYER_ABSENT".equals(director.siteSiegeSnapshot(world).outcome)
                        && director.siteSiegeSnapshot(world).active == 0 && director.snapshot(world).starts == 0,
                "leaving releases siege authority and cannot start a second encounter", here());
        endSamples("sealed");
        System.out.println("PASS sealed frontage, unchanged cobblestone and player absence " + siegeDetails(sealed));
        terminalTransitions(director, house);


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
        beginSamples();
        siegeTicks(director, calendar, dusk, 38);
        SiteSiege.Snapshot unloaded = director.siteSiegeSnapshot(world);
        check(unloaded.inserted == 0 && world.chunkMisses == 0 && director.snapshot(world).starts == 1
                        && "NO_VALID_APPROACH".equals(unloaded.eligibility),
                "unloaded routes safely fall back without fetching chunks: " + siegeDetails(unloaded), here());
        endSamples("missing-chunks");
        // The first legacy wave has always started at natural dusk +100.
        siegeTicks(director, calendar, dusk + 38, 62);
        check(director.snapshot(world).currentWave == 0, "fallback must wait until the legacy first-wave tick", here());
        siegeTicks(director, calendar, dusk + 100, 1);
        check(director.snapshot(world).currentWave == 1, "delayed preflight must not postpone the first wave", here());

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
        check(active.site == house && "PARTIAL".equals(active.coverage) && "APPROACH_ENTRANCE".equals(active.objective)
                        && active.kind == SiegeDirector.EncounterKind.SITE_SIEGE,
                "partial recognized site admits locally verified route: " + siegeDetails(active), here());
        siegeTicks(director, calendar, afterAdmission, 35);
        check(director.siteSiegeSnapshot(world).progress > 1, "attacker in motion before closing door", here());
        toggle(X, Y, Z + 2);
        siegeTicks(director, calendar, afterAdmission + 35, 125);
        SiteSiege.Snapshot blocked = director.siteSiegeSnapshot(world);
        check(blocked.failures.containsKey("ROUTE_OBSTRUCTED") && blocked.progress < 14
                        && director.snapshot(world).starts == 0 && (world.e(X, Y, Z + 2) & 4) == 0,
                "closing the door stops moving attackers before the threshold: " + siegeDetails(blocked), here());
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

    private void terminalTransitions(SiegeDirector director, int house) throws Exception {
        position(X + 2, Y, Z + 2);
        String[] causes = {"PEACEFUL", "ATTACKERS_GONE", "LUNAR_TIME_SKIP", "DIMENSION_CHANGED", "WORLD_UNLOADED"};
        for (int i = 0; i < causes.length; i++) {
            director.bind(null, null);
            long dusk = siegeDusk(3 * (i + 1));
            LunarCalendar calendar = siegeCalendar(20 + i, dusk - 1, Long.MIN_VALUE);
            director = siegeDirector(true, calendar);
            long next = waitForSiege(director, calendar, dusk);
            check(director.siteSiegeSnapshot(world).kind == SiegeDirector.EncounterKind.SITE_SIEGE,
                    "fresh natural boundary admits after cooldown: " + siegeDetails(director.siteSiegeSnapshot(world)),
                    here());
            if (i == 0) {
                world.q = 0;
                siegeTicks(director, calendar, next, 1);
            } else if (i == 1) {
                for (Object entity : new ArrayList<Object>(world.b)) if (entity instanceof yy) ((yy) entity).a(md.a(player), 100);
                for (int tick = 0; tick < 1000
                        && director.siteSiegeSnapshot(world).kind == SiegeDirector.EncounterKind.SITE_SIEGE; tick++) {
                    siegeTicks(director, calendar, next + tick, 1);
                    for (Object entity : new ArrayList<Object>(world.b)) if (entity instanceof yy) ((yy) entity).a(md.a(player), 100);
                }
            } else if (i == 2) {
                siegeTicks(director, calendar, next + 30, 1);
            } else if (i == 3) {
                verifyReleaseDuringNativeDispatch();
                director.bind(new xd(world, new akf()), null);
                director.bind(world, calendar.view());
            } else {
                director.bind(null, null);
                save();
                open("siege-sealed", false);
                prepare(X, Z);
                position(X + 2, Y, Z + 2);
                director = siegeDirector(true, siegeCalendar(30, next, dusk));
            }
            SiteSiege.Snapshot terminal = director.siteSiegeSnapshot(world);
            check(terminal.site == house && terminal.active == 0 && causes[i].equals(terminal.outcome)
                            && director.snapshot(world).starts == 0,
                    "terminal transition " + causes[i] + " with no new encounter: " + siegeDetails(terminal), here());
        }
        System.out.println("PASS Peaceful, elimination, lunar resync, dimension change and world detach preserve terminal outcomes");
    }

    private void verifyReleaseDuringNativeDispatch() throws Exception {
        yy mob = null;
        for (Object entity : world.b) if (entity instanceof yy && !((yy) entity).G) mob = (yy) entity;
        check(mob != null, "native skeleton for release dispatch", here());
        Field tasksField = acq.class.getDeclaredField("bT");
        tasksField.setAccessible(true);
        nb tasks = (nb) tasksField.get(mob);
        Field entries = nb.class.getDeclaredField("a");
        entries.setAccessible(true);
        Object approach = null;
        for (Object entry : (List<?>) entries.get(tasks)) {
            Field action = entry.getClass().getDeclaredField("a");
            action.setAccessible(true);
            Object task = action.get(entry);
            if (task.getClass().getSimpleName().equals("PerimeterApproachTask")) approach = task;
        }
        check(approach != null, "stock skeleton uses the pinned action-task field", here());
        final Object task = approach;
        final Method release = task.getClass().getDeclaredMethod("release");
        release.setAccessible(true);
        tasks.a(0, new rc() {
            @Override public boolean a() { return true; }
            @Override public void c() {
                try { release.invoke(task); }
                catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            }
        });
        tasks.a();
        check(!((rc) task).a(), "released task declines future updates", here());
        tasks.a();
        System.out.println("PASS native scheduler accepts release between selection and update");
    }

    @SuppressWarnings("unchecked")
    @Override void onCompoundReady(int house, int outpost) throws Exception {
        position(X + 2, Y, Z + 2);
        world.d.add(player);
        world.auditChunks = true;
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(40, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        beginSamples();
        long after = waitForSiege(director, calendar, dusk);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        SiteSnapshot compound = settlements.resolve(world, house);
        check(active.site == house && active.inserted > 0 && compound.parts.size() == 2,
                "two native parts and independent outpost allow one compound siege: " + siegeDetails(active), here());
        String[] start = active.approach.split(" > ")[0].split(",");
        int sx = Integer.parseInt(start[0]), sz = Integer.parseInt(start[2]);
        SiteSnapshot.Bounds bounds = compound.envelope;
        check(sx < bounds.minX - 2 || sx > bounds.maxX + 2 || sz < bounds.minZ - 2 || sz > bounds.maxZ + 2,
                "no insertion in the exposed courtyard", here());
        String route = active.approach;
        position(X + 12, Y, Z + 2);
        siegeTicks(director, calendar, after, 130);
        active = director.siteSiegeSnapshot(world);
        check(active.site == house && route.equals(active.approach) && active.progress > 4
                        && settlements.resolve(world, outpost).id == outpost && world.chunkMisses == 0,
                "moving to the outpost preserves movement toward the original compound", here());
        director.bind(null, null);
        position(X + 2, Y, Z + 2);
        endSamples("compound-two-nearby-sites");
        System.out.println("PASS compound envelope, courtyard exclusion and independent outpost target stability");
    }

    @SuppressWarnings("unchecked")
    private void industrialFallback() throws Exception {
        open("siege-industrial-obstructed", false);
        prepare(X, Z);
        int feet = Y + 8;
        room(X, feet, Z, true);
        position(X + 2, feet, Z + 2);
        walk();
        ticks(580);
        int site = recognized("elevated industrial core", true).id;
        industrialWorkshop(X + 3, feet, Z + 2);
        save();
        open("siege-industrial-obstructed", false);
        prepare(X, Z);
        position(X - 14, feet, Z + 2);
        check(settlements.resolve(world, site).coverage == Coverage.PARTIAL,
                "industrial core retains partial coverage after major expansion", here());
        world.d.add(player);
        world.auditChunks = true;
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(41, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        beginSamples();
        siegeTicks(director, calendar, dusk, 48);
        SiteSiege.Snapshot inactive = director.siteSiegeSnapshot(world);
        check(inactive.site == site && inactive.inserted == 0 && inactive.candidates > 0
                        && "SURFACE_OUT_OF_REACH".equals(inactive.rejection) && world.chunkMisses == 0,
                "elevated industrial geometry fails closed: " + siegeDetails(inactive), here());
        endSamples("elevated-industrial");
        System.out.println("PASS elevated dense industrial approach safely rejected");
    }

    @SuppressWarnings("unchecked")
    private void warmHouse() throws Exception {
        open("siege-warm-house", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized("warm siege house", true).id;
        toggle(X, Y, Z + 2);
        world.d.add(player);
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(42, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        beginSamples();
        long after = waitForSiege(director, calendar, dusk);
        siegeTicks(director, calendar, after, 205);
        check(director.siteSiegeSnapshot(world).site == site && director.siteSiegeSnapshot(world).arrivals > 0,
                "warm house reaches its route objective", here());
        endSamples("warm-house");
    }

    @SuppressWarnings("unchecked")
    private void planningTransitions() throws Exception {
        open("siege-planning-transition", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized("planning transition house", true).id;
        toggle(X, Y, Z + 2);
        world.d.add(player);
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(50, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        check(director.siteSiegeSnapshot(world).nextOpportunityCycle == calendar.view().snapshot().cycleIndex,
                "monitor forecasts the imminent eligible dusk", here());
        siegeTicks(director, calendar, dusk, 1);
        check("PLANNING".equals(director.siteSiegeSnapshot(world).phase),
                "first sample leaves the plan uncommitted", here());
        director.tick(player, true, null);
        check("PLANNING".equals(director.siteSiegeSnapshot(world).phase),
                "a pause preserves a pending plan at the same world time", here());
        siegeTicks(director, calendar, dusk + 2, 1);
        SiteSiege.Snapshot cancelled = director.siteSiegeSnapshot(world);
        check("LUNAR_TIME_SKIP".equals(cancelled.eligibility) && cancelled.inserted == 0
                        && cancelled.cooldownUntil == Long.MIN_VALUE && director.snapshot(world).starts == 0,
                "resync cancels planning without consuming the major budget or granting cooldown", here());

        director.bind(null, null);
        dusk = siegeDusk(3);
        calendar = siegeCalendar(51, dusk - 1, dusk - LunarSnapshot.DAY);
        director = siegeDirector(true, calendar);
        siegeTicks(director, calendar, dusk, 48);
        SiteSiege.Snapshot waiting = director.siteSiegeSnapshot(world);
        check(waiting.kind == SiegeDirector.EncounterKind.SITE_SIEGE && waiting.site == site && waiting.inserted == 0
                        && "WAITING_FOR_DARKNESS".equals(waiting.eligibility) && waiting.cooldownUntil > dusk,
                "validated geometry commits before dusk fallback deadline", here());
        toggle(X, Y, Z + 2);
        siegeTicks(director, calendar, dusk + 48, 1050);
        SiteSiege.Snapshot blocked = director.siteSiegeSnapshot(world);
        check("APPROACH_ENTRANCE".equals(blocked.objective) && blocked.arrivals > 0
                        && blocked.failures.keySet().stream().anyMatch(reason -> reason.toString().contains("ROUTE_OBSTRUCTED"))
                        && director.snapshot(world).starts == 0 && blocked.cooldownUntil == waiting.cooldownUntil,
                "committed twilight obstruction cannot start a late ordinary assault: " + siegeDetails(blocked), here());
        System.out.println("PASS planning pause, pre-commit resync, forecast and twilight ownership without late fallback");
    }

    long siegeDusk(int cycles) {
        long seed = world.v();
        long residue = Math.floorMod(seed ^ (seed >>> 32), 3);
        return ((residue + cycles) * 8 + 4) * LunarSnapshot.DAY + LunarSnapshot.NIGHT_START;
    }

    LunarCalendar siegeCalendar(long session, long time, long watermark) {
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

    SiegeDirector siegeDirector(boolean enabled, LunarCalendar calendar) {
        SiegeDirector director = new SiegeDirector(true, 15, 900, 8, 16, false, false, false);
        director.attachSites(enabled, settlements);
        director.bind(world, calendar.view());
        return director;
    }

    long waitForSiege(SiegeDirector director, LunarCalendar calendar, long dusk) {
        for (long time = dusk; time < dusk + 1100; time++) {
            siegeTicks(director, calendar, time, 1);
            SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
            if (view.inserted > 0 || view.phase.equals("TERMINAL") || director.snapshot(world).starts > 0) return time + 1;
        }
        throw new AssertionError("admission deadline: " + siegeDetails(director.siteSiegeSnapshot(world)));
    }

    void siegeTicks(SiegeDirector director, LunarCalendar calendar, long from, int count) {
        for (long time = from; time < from + count; time++) {
            world.a(time);
            world.q();
            long entityStarted = System.nanoTime();
            int moved = 0;
            for (Object entity : new ArrayList<Object>(world.b)) {
                if (entity instanceof yy && !((yy) entity).G) {
                    yy mob = (yy) entity;
                    double x = mob.o, z = mob.q;
                    if (forceEntityTick) {
                        // Model a Forge-forced entity update, then let World reconcile chunk membership.
                        mob.J_();
                        world.a(mob, false);
                    } else {
                        world.f(mob);
                    }
                    if (measured) peakStep = Math.max(peakStep, Math.hypot(mob.o - x, mob.q - z));
                    moved++;
                }
            }
            if (measured && moved > 0) {
                movement.add(System.nanoTime() - entityStarted);
                peakMobs = Math.max(peakMobs, moved);
            }
            long recognitionStarted = System.nanoTime();
            ticks(1);
            if (measured) recognition.add(System.nanoTime() - recognitionStarted);
            calendar.observe(time);
            world.auditBlocks = measured;
            world.blockReads = world.metadataReads = 0;
            long directorStarted = System.nanoTime();
            director.tick(player, false, siegeTerritory);
            long spent = System.nanoTime() - directorStarted;
            world.auditBlocks = false;
            if (measured) {
                totalTicks.add(System.nanoTime() - entityStarted);
                peakBlockReads = Math.max(peakBlockReads, world.blockReads);
                peakMetadataReads = Math.max(peakMetadataReads, world.metadataReads);
                SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
                long delta = view.planningNanos - lastPlannerNanos;
                if (delta > 0) {
                    if (firstMeasuredTick == Long.MIN_VALUE) firstMeasuredTick = time;
                    if (delta > peakPlannerNanos) {
                        peakPlannerNanos = delta;
                        peakPlannerTick = time - firstMeasuredTick;
                    }
                    planner.add(delta);
                }
                if (view.active > 0) activeTicks.add(spent);
                lastPlannerNanos = view.planningNanos;
            }
        }
    }

    private void measureSave() throws Exception {
        AssaultWorldState state = (AssaultWorldState) world.a(AssaultWorldState.class, "zto_player_assault");
        Samples nbt = new Samples();
        for (int i = 0; i < 1024; i++) {
            ady tag = new ady();
            long start = System.nanoTime();
            state.b(tag);
            if (i >= 512) nbt.add(System.nanoTime() - start);
        }
        long start = System.nanoTime();
        save();
        System.out.printf(Locale.ROOT, "PROFILE %s | full native save and flush %.2f us%n",
                nbt.report("one-row encounter NBT"), (System.nanoTime() - start) / 1000.0);
    }

    void beginSamples() {
        measured = true;
        world.measurePaths = true;
        world.pathNanos = world.peakPathNanos = world.pathCalls = 0;
        lastPlannerNanos = peakPlannerTick = peakPlannerNanos = 0;
        firstMeasuredTick = Long.MIN_VALUE;
        peakBlockReads = peakMetadataReads = peakMobs = 0;
        peakStep = 0;
    }

    void endSamples(String name) {
        measured = false;
        world.measurePaths = false;
        System.out.println("PROFILE " + name + " | " + planner.report("planner") + " peak offset=" + peakPlannerTick
                + " | " + recognition.report("territory+recognition")
                + " | " + movement.report("native hostile tick")
                + " | " + activeTicks.report("active director tick") + " | " + totalTicks.report("fixture tick")
                + String.format(Locale.ROOT, " | peak block/meta calls %d/%d | mobs %d | step %.3f blocks",
                        peakBlockReads, peakMetadataReads, peakMobs, peakStep)
                + " | native path calls " + world.pathCalls + " | " + world.pathNanos / 1000 + "/"
                + world.peakPathNanos / 1000 + " us total/peak");
        check(peakStep < 1.0 && here().peakReads <= 169,
                "sampled movement and recognition respect native fixture bounds", here());
    }

    private static final class Samples {
        private final List<Long> values = new ArrayList<Long>();

        void add(long duration) { values.add(duration); }

        String report(String label) {
            if (values.isEmpty()) return label + " n=0";
            Collections.sort(values);
            String result = String.format(Locale.ROOT, "%s n=%d us median=%.2f p95=%.2f p99=%s max=%.2f",
                    label, values.size(), percentile(50), percentile(95),
                    values.size() >= 100 ? String.format(Locale.ROOT, "%.2f", percentile(99)) : "-",
                    percentile(100));
            values.clear();
            return result;
        }

        private double percentile(int rank) {
            return values.get((int) Math.ceil(values.size() * rank / 100.0) - 1) / 1000.0;
        }
    }

    static String siegeDetails(SiteSiege.Snapshot view) {
        return String.format("%s/%s site=%d %s %s %s | %s | inserted=%d active=%d progress=%.2f steps=%d arrivals=%d"
                        + " | lanes=%d columns=%d peak=%d sky=%d peak=%d protection=%d | %d/%d us total/peak",
                view.kind, view.phase, view.site, view.coverage, view.eligibility, view.rejection, view.approach,
                view.inserted, view.active, view.progress, view.steps, view.arrivals, view.candidates, view.columns,
                view.peakColumns, view.skyReads, view.peakSkyReads, view.protectionReads,
                view.planningNanos / 1000, view.peakPlanningNanos / 1000)
                + " | " + view.intensity + " threat=" + view.threat + " wave=" + view.wave + "/" + view.waves
                + " planned=" + view.planned + " cap=" + view.activeCap + " peak=" + view.peak
                + " attempted=" + view.attempts + " defeated=" + view.defeated + " withdrawn=" + view.withdrawn
                + " deferred=" + view.deferred + " reserve=" + view.remaining + " undeployed=" + view.abandoned
                + " | " + view.insertedRoles + " | " + view.failures + " | objective " + view.objectiveOutcome + " | outcome " + view.outcome + " | lifetime " + view.peakLifetime
                + " ticks | path " + view.pathNanos / 1000 + "/" + view.peakPathNanos / 1000 + " us total/peak";
    }

}
