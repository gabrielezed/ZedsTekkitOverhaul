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
