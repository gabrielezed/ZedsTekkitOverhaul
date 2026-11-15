import java.io.File;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.territory.TerritorialActivityService.Snapshot;
import zed.tekkitoverhaul.territory.ThreatAssessment;

/** Drives normal samples and natural dusk in a marked, isolated legacy save. */
final class TerritorialRuntimeProbe {
    private final LocalLunarCalendar lunar;
    private final boolean adapt;
    private final boolean enderkin;
    private final long boundary;
    private final File territoryFile;
    private final File fresh;
    private int stage;
    private long clock, due;
    private int presence, regions, selected;

    TerritorialRuntimeProbe(LocalLunarCalendar lunar, boolean adapt, boolean enderkin, File directory, String save) {
        this.lunar = lunar;
        this.adapt = adapt;
        this.enderkin = enderkin;
        boundary = enderkin ? 8 * 24000L + 13000L : 13000L;
        territoryFile = new File(directory, "saves/" + save + "/data/zto_territorial_activity.dat");
        fresh = new File(directory, "saves/" + save + "-Fresh");
        require(!territoryFile.exists(), "legacy save starts without territorial history");
    }

    boolean tick(Minecraft game) throws Exception {
        Snapshot view = lunar.territorialSnapshot(game.f);
        if (view == null) return false;
        switch (stage) {
            case 0:
                require("READY".equals(view.status) && view.count == 0, "legacy save starts ready and empty");
                stage++;
                break;
            case 1:
                if (view.presence < 8) break;
                presence = view.presence;
                clock = view.clock;
                game.i();
                due = System.nanoTime() + 1_500_000_000L;
                stage++;
                break;
            case 2:
                require(view.clock == clock && view.presence == presence, "pause freezes clock and sampling");
                if (System.nanoTime() < due) break;
                require(game.s instanceof lb, "native pause menu opened");
                game.a((vp) null);
                game.f.a(12000L);
                stage++;
                break;
            case 3:
                if (view.clock <= clock) break;
                require(view.clock - clock < 100 && view.presence >= presence, "forward edit only advances live ticks");
                clock = view.clock;
                game.f.a(11000L);
                stage++;
                break;
            case 4:
                if (view.clock <= clock) break;
                require(view.clock - clock < 100 && view.presence >= presence, "rewind only advances live ticks");
                moveToQuietRegion(game);
                stage++;
                break;
            case 5:
                if (view.count < 2) break;
                require(view.presence < presence && (view.threat.regionX != 0 || view.threat.regionZ != 3),
                        "distinct cell with brief passage");
                require(view.threat.threat < 18, "quiet cell can lower the preferred budget");
                game.f.a(boundary - 20);
                stage++;
                break;
            case 6:
                SiegeDirector.Snapshot assault = lunar.assaultSnapshot(game.f);
                if (assault == null || assault.starts == 0) break;
                require(assault.state == SiegeDirector.State.ACTIVE && assault.boundary == boundary,
                        "natural full-moon admission");
                selected = adapt ? 14 : 15;
                require(assault.territorialAdaptation == adapt && assault.configuredBudget == 15 && assault.budget == selected
                        && (assault.admissionThreat != null) == adapt, "opt-in and admitted budget");
                int allocation = 0;
                for (Object wave : assault.waves) allocation += ((SiegeDirector.WaveSnapshot) wave).planned;
                require(assault.waves.size() == 3 && allocation == selected, "ordinary wave allocation");
                System.out.println("ALPHA6 territory admission configured=15 selected=" + selected + " threat="
                        + (assault.admissionThreat == null ? "disabled" : assault.admissionThreat.threat)
                        + " enderkin=" + assault.experimentalEnderkin);
                home(game);
                stage++;
                break;
            case 7:
                if (view.threat.regionX != 0) break;
                assault = lunar.assaultSnapshot(game.f);
                require(assault.budget == selected && view.presence >= presence, "moving leaves admission frozen");
                if (enderkin) {
                    game.h.l(20);
                    SiegeDirector.WaveSnapshot second = (SiegeDirector.WaveSnapshot) assault.waves.get(1);
                    SiegeDirector.WaveSnapshot third = (SiegeDirector.WaveSnapshot) assault.waves.get(2);
                    require("SKIRMISHER".equals(second.enderkin.selected) && "BREACHER".equals(third.enderkin.selected),
                            "combined experiment retains eligible substitution slots");
                    Alpha6RuntimeProbe.clearHostiles(game);
                    if (!second.enderkin.inserted || !third.enderkin.inserted) {
                        require(assault.state == SiegeDirector.State.ACTIVE, "combined substitutions inserted before resolution: "
                                + second.enderkin.summary() + " | " + third.enderkin.summary());
                        break;
                    }
                    System.out.println("ALPHA6 territory both inserted Skirmisher and Breacher within the selected budget");
                }
                if (assault.elapsed < 150) break;
                require(assault.peakActive <= assault.activeCap && assault.spawns.size() <= selected,
                        "existing population and allocation limits");
                regions = view.count;
                game.a(-1);
                stage++;
                break;
            case 8:
                require(view.dimension == -1 && view.threat == null && view.count == 0
                        && "OUTSIDE_OVERWORLD".equals(view.status), "Nether has no Overworld territory");
                game.a(0);
                home(game);
                stage++;
                break;
            case 9:
                require(view.dimension == 0 && view.count == regions && view.presence >= presence, "overworld history reattached");
                presence = view.presence;
                Alpha6RuntimeProbe.saveQuit(game);
                require(game.f == null && territoryFile.isFile(), "native Save and Quit persisted territory");
                Alpha6RuntimeProbe.open(game);
                home(game);
                stage++;
                break;
            case 10:
                require(view.count == regions && view.presence == presence, "save reopened with existing records and score");
                assault = lunar.assaultSnapshot(game.f);
                require(assault.starts == 0 && assault.boundary == boundary, "no replay after reopen");
                System.out.println("ALPHA6 territory reopened regions=" + regions + " presence=" + presence
                        + " clock=" + view.clock + " file-bytes=" + territoryFile.length());
                if (!fresh.isDirectory()) return true;
                require(new File(fresh, ".zto-disposable-test").isFile() && !new File(fresh, "level.dat").exists(),
                        "fresh world is marked and has no existing level");
                Alpha6RuntimeProbe.saveQuit(game);
                Alpha6RuntimeProbe.open(game, fresh.getName(), new fj(911L, 0, false, false, vx.c));
                stage++;
                break;
            case 11:
                require(view.count == 0 && view.clock < 100, "fresh world has independent history");
                stage++;
                break;
            case 12:
                if (view.count == 0) break;
                require(view.count == 1 && view.presence == 1, "fresh world records its first visit");
                System.out.println("ALPHA6 fresh territory regions=" + view.count + " clock=" + view.clock);
                return true;
            default: throw new AssertionError("Unknown territorial probe stage " + stage);
        }
        return false;
    }

    private static void home(Minecraft game) {
        game.h.c(34.5, 121, 215.5, 0, 0);
        game.h.r = game.h.s = game.h.t = 0;
    }

    private void moveToQuietRegion(Minecraft game) {
        for (int distance = 96; distance <= 288; distance += 64) {
            int[][] candidates = {{34 + distance, 215}, {34 - distance, 215}, {34, 215 + distance}, {34, 215 - distance}};
            for (int[] candidate : candidates) {
                int x = candidate[0], z = candidate[1];
                game.h.c(x + .5, 121, z + .5, 0, 0);
                Snapshot view = lunar.territorialSnapshot(game.f);
                if (view.threat.environment == ThreatAssessment.Environment.BASELINE) {
                    game.f.g(x, 120, z, 1);
                    game.h.r = game.h.s = game.h.t = 0;
                    return;
                }
            }
        }
        throw new AssertionError("No baseline biome found near the arena");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
