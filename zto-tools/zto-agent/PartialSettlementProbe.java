import java.io.File;
import java.nio.file.Files;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.settlement.SettlementRecognition.Snapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;

/** Owner-scale native fixtures. Run the same source against the pre-fix archive for the baseline. */
public class PartialSettlementProbe extends SiteSiegeProbe {
    static final int AX = -121, AY = 63, AZ = 256, FAR = AX + 223;

    PartialSettlementProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            run(new PartialSettlementProbe(new File(args[0])), "partial-baseline".equals(args[1]));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    static void run(PartialSettlementProbe probe, boolean baseline) throws Exception {
        ModdedSettlementProbe.register(true);
        probe.ownerFactory(baseline);
        if (!baseline) probe.temporaryWalls();
    }

    private void ownerFactory(boolean baseline) throws Exception {
        open("partial-owner-factory", false);
        for (int x = AX; x <= FAR + 48; x += 48) prepare(x, AZ);
        int authored = 0;
        for (int x = AX - 12; x <= FAR; x++) {
            for (int z = AZ - 6; z <= AZ + 5; z++) {
                boolean ownerWindow = Math.abs(x - AX) <= 6;
                if (ownerWindow && authored++ >= 90) {
                    world.g(x, AY - 1, z, 5);
                } else {
                    world.g(x, AY - 2, z, 1);
                    place(5, x, AY - 2, z, 1);
                }
            }
            for (int y = AY; y <= AY + 1; y++) {
                world.g(x, y, AZ - 6, 5);
                if (y == AY || x < AX - 6 || x > AX + 1) world.g(x, y, AZ + 5, 5);
            }
            // Interior partitions leave a continuous aisle down the center.
            if ((x - AX) % 24 == 10)
                for (int z = AZ + 2; z <= AZ + 5; z++) world.g(x, AY, z, 5);
        }
        world.g(AX - 6, AY - 2, AZ - 5, 5);
        world.g(AX - 5, AY - 2, AZ - 5, 5);
        for (int y = AY; y < AY + 3; y++) world.g(AX + 3, y, AZ + 4, 5);
        for (int z = AZ + 2; z <= AZ + 4; z++) world.g(AX + 3, AY + 3, z, 20);
        world.g(AX - 4, AY, AZ + 3, 58);
        world.g(AX - 3, AY, AZ + 3, 61);
        world.g(AX - 2, AY, AZ + 3, 61);
        ModdedSettlementProbe.mixedEquipment(this, AX - 4, AY, AZ - 3);
        position(AX, AY, AZ);
        ticks(1);
        for (int x = AX - 1; x >= AX - 2; x--) {
            position(x, AY, AZ);
            ticks(1);
        }
        for (int x = AX - 1; x <= AX + 3; x++) {
            position(x, AY, AZ);
            ticks(1);
        }
        for (int z = AZ + 1; z <= AZ + 3; z++) {
            position(AX + 3, AY, z);
            ticks(1);
        }
        for (int delta = 2; delta >= 1; delta--) {
            position(AX + delta, AY, AZ + delta);
            ticks(1);
        }
        position(AX, AY, AZ);
        ticks(4900);
        Snapshot local = here();
        System.out.printf("OWNER %s #%d %s | component %s footprint %s | manual %d inferred %d activity %d "
                        + "walked %d openings %d cover %d/%d tools %d/%d equipment %d peak %d%n",
                local.state, local.id, local.reason, local.component, local.boundary, local.placed, local.inferred,
                local.activity, local.walked, local.uses, local.anchorCover, local.cover, local.workbench.associated,
                local.furnace.associated, local.generic.associated, local.peakReads);
        check(local.placed == 90 && local.inferred == 121 && local.anchorCover == 0 && local.cover == 3
                        && local.activity == 24 && local.walked == 11 && local.uses == 0
                        && local.workbench.associated == 1 && local.furnace.associated == 2 && local.generic.associated >= 7,
                "owner-scale placement, inferred construction, activity, movement and mixed equipment", local);
        if (baseline) {
            check(local.id == 0 && "Candidate".equals(local.state), "pre-fix clipped identity deadlock", local);
            recognitionTiming("owner-scale clipped mixed factory before fix");
            return;
        }
        check(local.id > 0 && local.confidence.contains("High confidence")
                        && settlements.resolve(world, local.id).coverage == Coverage.PARTIAL
                        && ((SiteSnapshot) settlements.nearby(world, AX, AY, AZ, 8).get(0)).id == local.id,
                "partial identity is recognized and discoverable", local);
        int id = local.id;
        recognitionTiming("owner-scale clipped mixed factory");
        world.auditChunks = true;
        assertSiegeSafety(id);

        for (int x = AX + 1; x <= FAR - 5; x++) {
            position(x, AY, AZ);
            ticks(5);
            Snapshot sample = here();
            check(sample.siteCount == 1 && sample.peakReads <= 169, "continuous walk retains one identity and read ceiling", sample);
        }
        ticks(120);
        SiteSnapshot grown = settlements.resolve(world, id);
        System.out.printf("FRONTIER #%d %d known parts %s %s%n", id, grown.parts.size(), grown.envelope, grown.coverage);
        check(grown.parts.size() > 20 && grown.envelope.maxX - grown.envelope.minX >= 210
                        && grown.coverage == Coverage.PARTIAL && here().id == id && world.chunkMisses == 0,
                "factory grows across hundreds of blocks through local construction overlaps", here());
        System.out.printf("PASS ID #%d, %d observed parts,  %s, %s, zero missing-chunk retrievals%n",
                id, grown.parts.size(), grown.envelope, grown.coverage);
        save();
        long registryBytes = Files.size(world.A().a("zto_settlement_sites").toPath());
        open("partial-owner-factory", false);
        prepare(FAR - 5, AZ);
        position(FAR - 5, AY, AZ);
        ticks(110);
        SiteSnapshot reopened = settlements.resolve(world, id);
        check(reopened.parts.size() == grown.parts.size() && reopened.envelope.toString().equals(grown.envelope.toString())
                        && reopened.coverage == Coverage.PARTIAL && here().id == id && here().siteCount == 1,
                "fragment masks and stable identity persist across native save/reopen", here());
        System.out.printf("PASS partial registry saved and reopened under the same ID (%d bytes)%n", registryBytes);
        onFactoryReopened(id);
    }

    void onFactoryReopened(int id) {}

    private void temporaryWalls() throws Exception {
        open("partial-temporary-walls", false);
        prepare(X, Z);
        for (int i = -6; i <= 10; i++) {
            for (int y = Y; y < Y + 4; y++) {
                place(5, X + i, y - 1, Z - 6, 1);
                if (i > -6) place(5, X - 6, y - 1, Z + i, 1);
            }
        }
        position(X, Y, Z);
        for (int i = 0; i < 3; i++) {
            walk();
            position(X + i + 1, Y, Z);
            ticks(1);
        }
        position(X, Y, Z);
        ticks(2400);
        Snapshot wall = here();
        check(wall.placed >= 80 && wall.activity >= 6 && wall.id == 0 && wall.siteCount == 0 && wall.peakReads <= 169,
                "clipped authored temporary walls without a constructed work area stay unrecognized", wall);
        System.out.println("PASS clipped temporary walls rejected despite verified placements and activity");
    }

    @SuppressWarnings("unchecked")
    private void assertSiegeSafety(int id) {
        world.d.add(player);
        long dusk = siegeDusk(0);
        LunarCalendar calendar = siegeCalendar(52, dusk - 1, Long.MIN_VALUE);
        SiegeDirector director = siegeDirector(true, calendar);
        waitForSiege(director, calendar, dusk);
        SiteSiege.Snapshot admission = director.siteSiegeSnapshot(world);
        check(admission.site == id && admission.inserted == 0 && "SITE_FOOTPRINT_UNRESOLVED".equals(admission.rejection)
                        && director.snapshot(world).starts == 1 && world.b.isEmpty()
                        && settlements.resolve(world, id).coverage == Coverage.PARTIAL,
                "an unresolved exterior produces no siege insertion and preserves site identity", here());
        director.bind(null, null);
        world.d.remove(player);
        System.out.println("PASS partial site discoverable, unresolved exterior safely rejected, ordinary assault retained");
    }
}
