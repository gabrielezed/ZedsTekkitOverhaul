import java.io.File;
import java.lang.reflect.Method;
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
    private final File directory;
    private final String save;
    private final boolean elevated;
    private int stage;
    private long clock, due;
    private int presence, regions, selected;
    private int nextCell;
    private String pendingCheckpoint;
    private String profile;
    private int priorReturns;

    TerritorialRuntimeProbe(LocalLunarCalendar lunar, boolean adapt, boolean enderkin, File directory, String save,
            boolean elevated) {
        this.lunar = lunar;
        this.adapt = adapt;
        this.enderkin = enderkin;
        this.directory = directory;
        this.save = save;
        this.elevated = elevated;
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
                if (!checkpoint("unvisited", view, null)) break;
                if (elevated) game.f.a(2000L);
                stage++;
                break;
            case 1:
                if (view.presence < (elevated ? 16 : 8)) break;
                if (!checkpoint("sustained", view, null)) break;
                if (adapt && !enderkin) {
                    measureAssessment(game, view.threat.threat);
                    if (!elevated) verifyQuantumSuit(game, view.threat);
                }
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
                if (!elevated) moveToQuietRegion(game);
                stage++;
                break;
            case 5:
                if (!elevated) {
                    if (view.count < 2) break;
                    require(view.presence < presence && (view.threat.regionX != 0 || view.threat.regionZ != 3),
                            "distinct cell with qualified presence");
                    require(view.threat.threat < 16, "quiet cell can lower the preferred budget");
                    if (!checkpoint("other-region", view, null)) break;
                } else {
                    require(view.threat.threat >= 24, "sustained history yields elevated pressure");
                }
                game.f.a(boundary - 20);
                stage++;
                break;
            case 6:
                SiegeDirector.Snapshot assault = lunar.assaultSnapshot(game.f);
                if (assault == null || assault.starts == 0) break;
                require(assault.state == SiegeDirector.State.ACTIVE && assault.boundary == boundary,
                        "natural full-moon admission");
                selected = adapt && !elevated ? 14 : 15;
                profile = elevated ? "ELEVATED" : adapt ? "QUIET" : "DISABLED";
                require(assault.territorialAdaptation == adapt && assault.configuredBudget == 15 && assault.budget == selected
                        && profile.equals(assault.admissionProfile) && (assault.admissionThreat != null) == adapt,
                        "opt-in and admitted profile");
                int allocation = 0;
                for (Object wave : assault.waves) allocation += ((SiegeDirector.WaveSnapshot) wave).planned;
                require(assault.waves.size() == 3 && allocation == selected
                        && ((SiegeDirector.WaveSnapshot) assault.waves.get(0)).planned == (elevated ? 6 : adapt ? 2 : 4),
                        "contextual wave allocation");
                if (!checkpoint("admitted", view, assault)) break;
                System.out.println("ALPHA6 territory admission configured=15 selected=" + selected + " threat="
                        + (assault.admissionThreat == null ? "disabled" : assault.admissionThreat.threat)
                        + " profile=" + profile + " enderkin=" + assault.experimentalEnderkin);
                home(game);
                stage++;
                break;
            case 7:
                if (view.threat.regionX != 0) break;
                assault = lunar.assaultSnapshot(game.f);
                require(assault.budget == selected && profile.equals(assault.admissionProfile)
                        && view.presence >= presence, "moving leaves admission frozen");
                if (adapt && !enderkin) {
                    game.h.l(20);
                    Alpha6RuntimeProbe.clearHostiles(game);
                    if (assault.state == SiegeDirector.State.ACTIVE) break;
                    require(assault.state == SiegeDirector.State.RESOLVED && !assault.spawns.isEmpty(),
                            "contextual encounter inserted mobs and resolved");
                    if (!checkpoint("resolved", view, assault)) break;
                }
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
                if (!checkpoint("nether", view, null)) break;
                game.a(0);
                home(game);
                stage++;
                break;
            case 9:
                require(view.dimension == 0 && view.count == regions && view.presence >= presence, "overworld history reattached");
                if (!checkpoint("returned", view, null)) break;
                presence = view.presence;
                Alpha6RuntimeProbe.saveQuit(game);
                require(game.f == null && territoryFile.isFile(), "native Save and Quit persisted territory");
                Alpha6RuntimeProbe.open(game);
                home(game);
                stage++;
                break;
            case 10:
                require(view.count == regions && view.presence >= presence, "save reopened with existing records and score");
                assault = lunar.assaultSnapshot(game.f);
                require(assault.starts == 0 && assault.boundary == boundary, "no replay after reopen");
                if (!checkpoint("reopened", view, null)) break;
                System.out.println("ALPHA6 territory reopened regions=" + regions + " presence=" + presence
                        + " clock=" + view.clock + " file-bytes=" + territoryFile.length());
                if (elevated) {
                    reportBiomes(game);
                    presence = view.presence;
                    priorReturns = view.returns;
                    clock = view.clock;
                    moveToQuietRegion(game);
                    stage = 14;
                    break;
                }
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
                nextCell = view.threat.regionX + 1;
                stage++;
                break;
            case 13:
                if (game.h.o < nextCell * 64 + 2) {
                    game.h.b(0.30D, 0, 0);
                    break;
                }
                if (view.count < 2) break;
                require(view.threat.regionX == nextCell && view.presence > 0, "collision-aware movement crossed into a distinct cell");
                System.out.println("ALPHA6 movement recorded adjacent region " + nextCell + "," + view.threat.regionZ);
                return true;
            case 14:
                if (view.clock - clock < 3600) break;
                if (!checkpoint("absence", view, null)) break;
                home(game);
                stage++;
                break;
            case 15:
                require(view.threat.regionX == 0 && view.threat.regionZ == 3 && view.presence <= presence - 3,
                        "three minutes away decay at least three presence points");
                if (!checkpoint("before-return-credit", view, null)) break;
                stage++;
                break;
            case 16:
                if (view.returns == priorReturns) break;
                require(view.returns == priorReturns + 2 && view.threat.environment == ThreatAssessment.Environment.BASELINE,
                        "sustained return adds credit and preserves environmental identity");
                return checkpoint("return-credit", view, null);
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
                if (view.threat.environment == ThreatAssessment.Environment.BASELINE && view.threat.threat <= 13) {
                    game.f.g(x, 120, z, 1);
                    game.h.r = game.h.s = game.h.t = 0;
                    return;
                }
            }
        }
        throw new AssertionError("No baseline biome found near the arena");
    }

    private boolean checkpoint(String label, Snapshot view, SiegeDirector.Snapshot assault) {
        if (!Boolean.getBoolean("zto.devBridge.enabled")) return true;
        if (!label.equals(pendingCheckpoint)) {
            pendingCheckpoint = label;
            ThreatAssessment threat = view.threat;
            String local = threat == null ? "null" : threat.regionX + "," + threat.regionZ + "," + threat.activity
                    + "," + threat.environmental + "," + threat.environment + "," + threat.threat + "," + threat.observed;
            String admission = assault == null ? "null" : assault.admissionProfile + "," + assault.budget + ","
                    + assault.admissionThreat.threat + "," + assault.activeCap + "," + assault.state + ","
                    + ((SiegeDirector.WaveSnapshot) assault.waves.get(0)).planned + ","
                    + ((SiegeDirector.WaveSnapshot) assault.waves.get(1)).planned + ","
                    + ((SiegeDirector.WaveSnapshot) assault.waves.get(2)).planned;
            System.out.println("ALPHA6 HTTP " + label + " " + view.status + "," + view.dimension + ","
                    + view.clock + "," + view.count + "," + view.presence + "," + view.returns + " " + local + " " + admission);
        }
        return new File(directory, save + "-territory-http-" + label + ".ack").isFile();
    }

    private void reportBiomes(Minecraft game) {
        boolean exposed = false, swamp = false;
        for (int z = -1024; z <= 1024 && !(exposed && swamp); z += 64) {
            for (int x = -1024; x <= 1024 && !(exposed && swamp); x += 64) {
                game.h.c(x + 0.5, 121, z + 0.5, 0, 0);
                ThreatAssessment sample = lunar.territorialSnapshot(game.f).threat;
                if (sample.activity != 0 || sample.observed) continue;
                if (!exposed && sample.environment == ThreatAssessment.Environment.EXPOSED && sample.environmental == 16) {
                    System.out.println("ALPHA6 unvisited exposed environment x=" + x + " z=" + z + " threat=" + sample.threat);
                    exposed = true;
                }
                if (!swamp && sample.environment == ThreatAssessment.Environment.SWAMPLAND && sample.environmental == 18) {
                    System.out.println("ALPHA6 unvisited swamp environment x=" + x + " z=" + z + " threat=" + sample.threat);
                    swamp = true;
                }
            }
        }
        require(exposed && swamp, "the pack's biome manager distinguishes unvisited exposed terrain and swamp");
    }

    private void measureAssessment(Minecraft game, int expected) {
        final int samples = 512;
        for (int run = 0; run < 5; run++) {
            int checksum = 0;
            long start = System.nanoTime();
            for (int i = 0; i < samples; i++) checksum += lunar.territorialSnapshot(game.f).threat.threat;
            long elapsed = System.nanoTime() - start;
            require(checksum == expected * samples, "assessment is stable within a game tick");
            if (run > 1) System.out.println("ALPHA6 territory assessment + snapshot ns/op=" + elapsed / samples);
        }
    }

    private void verifyQuantumSuit(Minecraft game, ThreatAssessment expected) throws Exception {
        Method getItem = Class.forName("ic2.api.Items").getMethod("getItem", String.class);
        String[] names = {"quantumBoots", "quantumLeggings", "quantumBodyarmor", "quantumHelmet"};
        aan[] armor = game.h.ap.b;
        aan[] original = armor.clone();
        try {
            for (int slot = 0; slot < names.length; slot++) {
                armor[slot] = (aan) getItem.invoke(null, names[slot]);
                require(armor[slot] != null, "installed pack provides " + names[slot]);
            }
            ThreatAssessment equipped = lunar.territorialSnapshot(game.f).threat;
            require(equipped.activity == expected.activity && equipped.environmental == expected.environmental
                    && equipped.threat == expected.threat, "QuantumSuit leaves the same-tick threat unchanged");
            System.out.println("ALPHA6 installed IC2 QuantumSuit leaves threat at " + equipped.threat);
        } finally {
            System.arraycopy(original, 0, armor, 0, original.length);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
