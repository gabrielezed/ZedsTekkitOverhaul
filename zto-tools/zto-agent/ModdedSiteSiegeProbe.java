import java.io.File;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;

/** Exercises ordinary siege discovery with a site recognized through the generic equipment path. */
public final class ModdedSiteSiegeProbe extends SiteSiegeProbe {
    private ModdedSiteSiegeProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            new ModdedSiteSiegeProbe(new File(args[0])).mixedModSite();
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
}
