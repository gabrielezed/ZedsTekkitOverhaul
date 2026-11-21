import java.io.File;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;

/** Exercises ordinary siege discovery with a site recognized through the generic equipment path. */
public final class ModdedSiteSiegeProbe extends TacticalSiteSiegeProbe {
    private ModdedSiteSiegeProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            ModdedSiteSiegeProbe probe = new ModdedSiteSiegeProbe(new File(args[0]));
            probe.mixedModSite();
            probe.protectedEquipment();
            probe.infiltration("open");
            probe.infiltration("interrupt");
            probe.infiltration("sealed");
            probe.infiltration("cargo");
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    private void mixedModSite() throws Exception {
        ModdedSettlementProbe.register(true);
        open("siege-mixed-mod-factory", false);
        prepare(X, Z);
        ModdedSettlementProbe.platform(this, X, Y, Z, 5, true);
        ModdedSettlementProbe.mixedEquipment(this, X, Y, Z);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized("mixed-mod workshop without vanilla utilities", true).id;
        check(here().generic.associated == 8 && here().uses == 0
                        && settlements.locate(world, X + 2, Y, Z + 2).site.id == site,
                "mixed factory is discoverable through ordinary SiteQueries", here());
        world.d.add(player);
        world.auditChunks = true;
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(40, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        waitForSiege(director, calendar, dusk);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        check(active.site == site && active.kind == SiegeDirector.EncounterKind.SITE_SIEGE
                        && active.inserted > 0 && director.snapshot(world).starts == 0 && world.chunkMisses == 0,
                "natural admission uses native approach geometry for mixed evidence: " + siegeDetails(active), here());
        System.out.println("PASS native mixed-mod site discovery and siege admission " + siegeDetails(active));
        director.bind(null, null);
    }

    @SuppressWarnings("unchecked")
    private void protectedEquipment() throws Exception {
        open("siege-protected-equipment", false);
        prepare(X, Z);
        room(X, Y, Z, true);
        position(X + 2, Y, Z + 2);
        walk();
        ticks(560);
        int site = recognized("workshop before installing door-side equipment", true).id;
        int[][] deviceCells = ModdedSettlementProbe.entranceEquipment(this, X, Y, Z + 2);
        world.d.add(player);
        ady[] before = captureDevices(deviceCells);
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(82, time, Long.MIN_VALUE);
        SiegeDirector director = new SiegeDirector(true, 15, 900, 8, 16, false, false, false);
        director.attachSites(false, true, settlements);
        director.bind(world, calendar.view());
        world.auditChunks = true;
        director.requestTestSiege(world, player, SiteSiege.Profile.BREACH);
        long windup = waitForPhase(director, calendar, time, "PRESSURING");
        siegeTicks(director, calendar, windup, 65);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        check(active.site == site && active.entranceChanges == 1 && active.structuralChanges == 0
                        && (world.e(X, Y, Z + 2) & 4) != 0 && world.chunkMisses == 0,
                "eligible door opens between actual pack devices: " + siegeDetails(active), here());
        check(java.util.Arrays.equals(before, captureDevices(deviceCells)), "no tile mutation or duplication", here());
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, windup + 65, 1);
        save();
        director.bind(null, null);
        open("siege-protected-equipment", false);
        prepare(X, Z);
        position(X + 2, Y, Z + 2);
        check(java.util.Arrays.equals(before, captureDevices(deviceCells)) && settlements.resolve(world, site) != null,
                "native inventories, energy and fluid NBT persist on reopen", here());
        System.out.println("PASS mixed workshop entrance, native cable and pipe, IC2 energy, BuildCraft fluid, hopper, chest and unknown tile");
    }

    private void infiltration(String layout) throws Exception {
        String saveName = "siege-infiltration-" + layout;
        tacticalHouse(saveName, layout.equals("sealed") ? 4 : 64, false);
        world.q = 3;
        int x = X + 2, z = Z + 1;
        boolean cargo = layout.equals("cargo");
        io hopper = null;
        if (cargo) {
            java.lang.reflect.Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
            register.setAccessible(true);
            register.invoke(null, zed.tekkitoverhaul.survival.tactical.EntitySaboteur.class, "ZTO.Saboteur", 209);
            place(ModdedSettlementProbe.pipeItem.bQ, x, Y - 1, z, 1);
        } else {
            world.g(x, Y, z, 188);
            hopper = (io) world.b(x, Y, z);
            hopper.a(0, new aan(263, 9, 0));
        }
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(83, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.AUTO, SiteSiege.Intensity.MAXIMUM);
        boolean windup = false;
        SiteSiege.Snapshot view = director.siteSiegeSnapshot(world);
        for (int i = 0; i < 800; i++) {
            siegeTicks(director, calendar, time++, 1);
            player.l(20);
            view = director.siteSiegeSnapshot(world);
            if (i == 290 && view.entranceChanges == 1) {
                // The defender clears the opened threshold before the industrial reserve arrives.
                for (Object actor : new java.util.ArrayList<Object>(world.b))
                    if (actor instanceof yy) ((yy) actor).a(md.a(player), 100);
            }
            windup |= view.industry.contains("pose 2");
            if (layout.equals("interrupt") && windup) {
                for (Object actor : new java.util.ArrayList<Object>(world.b))
                    if (actor instanceof zed.tekkitoverhaul.survival.tactical.EntitySaboteur)
                        ((yy) actor).a(md.a(player), 100);
            }
            if (cargo && view.industry.contains("COMPONENT_STOLEN")) break;
            if (view.industry.contains("APPLIED")) break;
        }
        if (cargo) {
            check(view.industry.contains("COMPONENT_STOLEN") && world.a(x, Y, z) == 0,
                    "one siege-assigned pipe removed | " + view.industry + " | " + siegeDetails(view), here());
            save();
            director.bind(null, null);
            open(saveName, false);
            prepare(X, Z);
            int recovered = 0;
            for (Object actor : new java.util.ArrayList<Object>(world.b)) {
                if (actor instanceof zed.tekkitoverhaul.survival.tactical.EntitySaboteur)
                    ((yy) actor).a(md.a(player), 100);
            }
            for (Object actor : world.b) {
                if (actor instanceof fq && ((fq) actor).a.c == ModdedSettlementProbe.pipeItem.bQ)
                    recovered += ((fq) actor).a.a;
            }
            check(recovered == 1 && world.a(x, Y, z) == 0, "one carried component recovered on reopen", here());
            System.out.println("PASS siege-assigned pipe teardown, Save/Quit in flight and one native recovery");
            return;
        }
        if (layout.equals("sealed")) {
            check(!view.insertedRoles.containsKey("SABOTEUR") && !windup && view.entranceChanges == 0,
                    "sealed workshop denies infiltration " + siegeDetails(view), here());
        } else check(windup && view.industry.contains("APPLIED") == layout.equals("open") && view.entranceChanges == 1
                        && Integer.valueOf(1).equals(view.insertedRoles.get("SABOTEUR")) && hopper.k_(0).a == 9,
                "physical infiltration and conserved fuel | " + view.industry + " | " + siegeDetails(view), here());
        if (layout.equals("open")) {
            position(X + 21, Y, Z + 2);
            boolean validLease = false;
            for (Object actor : world.b) {
                if (actor instanceof zed.tekkitoverhaul.survival.tactical.EntitySaboteur) {
                    zed.tekkitoverhaul.survival.tactical.EntitySaboteur saboteur =
                            (zed.tekkitoverhaul.survival.tactical.EntitySaboteur) actor;
                    validLease |= saboteur.canMaintainDisruption(world.b(x, Y, z));
                }
            }
            check(validLease, "site disruption lease survives the defender leaving the machine room", here());
            position(X + 2, Y, Z + 2);
        }
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, time, 1);
        save();
        director.bind(null, null);
        open(saveName, false);
        prepare(X, Z);
        check(((io) world.b(x, Y, z)).k_(0).a == 9, "stock survives siege cancellation and reopen", here());
        System.out.println("PASS site-assigned Saboteur " + layout + " and workshop stock on reopen");
    }

    private ady[] captureDevices(int[][] cells) {
        ady[] result = new ady[cells.length];
        for (int i = 0; i < cells.length; i++) {
            int[] cell = cells[i];
            kw device = world.b(cell[0], cell[1], cell[2]);
            check(device != null, "native device at entry " + i, here());
            result[i] = new ady();
            device.b(result[i]);
        }
        return result;
    }
}
