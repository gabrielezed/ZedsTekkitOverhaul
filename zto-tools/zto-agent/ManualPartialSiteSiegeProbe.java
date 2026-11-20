import java.io.File;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.settlement.SiteSnapshot;

/** Adds the manual admission contract to the historical partial recognition fixture. */
public final class ManualPartialSiteSiegeProbe extends PartialSettlementProbe {
    private ManualPartialSiteSiegeProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            run(new ManualPartialSiteSiegeProbe(new File(args[0])), false);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    @Override void onFactoryReopened(int id) {
        // A deliberately verifiable loaded lane on the south side of the clipped factory.
        for (int x = FAR - 24; x <= FAR + 3; x++) {
            for (int z = AZ + 6; z <= AZ + 28; z++) world.g(x, AY - 1, z, 1);
        }
        position(FAR - 5, AY, AZ);
        world.d.add(player);
        world.auditChunks = true;
        long time = 6000;
        LunarCalendar calendar = siegeCalendar(86, time, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(false, calendar);
        director.requestTestSiege(world, player, SiteSiege.Profile.PERIMETER);
        siegeTicks(director, calendar, time, 75);
        SiteSiege.Snapshot active = director.siteSiegeSnapshot(world);
        SiteSnapshot site = settlements.resolve(world, id);
        check(site.perimeterUnresolved && active.site == id && "PARTIAL".equals(active.coverage)
                        && active.inserted > 0 && active.progress > 3 && world.chunkMisses == 0,
                "clipped compound admits a verified local exterior test: " + siegeDetails(active)
                        + " | " + active.requestReason, here());
        String[] start = active.approach.split(" > ")[0].split(",");
        check(Integer.parseInt(start[2]) > site.envelope.maxZ + 2 && active.structuralChanges == 0,
                "insertion remains outside the entire recognized envelope", here());
        director.cancelTestSiege(world);
        siegeTicks(director, calendar, time + 75, 1);
        check(director.siteSiegeSnapshot(world).request == SiegeDirector.DebugState.CANCELLED
                        && settlements.resolve(world, id).id == id,
                "cancel leaves the clipped compound identity intact", here());
        director.bind(null, null);
        System.out.println("PASS clipped mixed factory manual perimeter from a verified exterior lane " + siegeDetails(active));
    }
}
