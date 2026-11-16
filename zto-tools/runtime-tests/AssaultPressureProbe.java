import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.assault.SiegeDirector.Spawn;
import zed.tekkitoverhaul.assault.SiegeDirector.WaveSnapshot;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.territory.ThreatAssessment;

/** Leaves every inserted mob in the disposable arena and observes actual overlapping waves. */
@SuppressWarnings("unchecked") // RetroGuard strips generic signatures from the packaged snapshots.
final class AssaultPressureProbe {
    private static final long BOUNDARY = 8 * 24000L + 13000L;
    private final LocalLunarCalendar lunar;
    private final File directory;
    private final String save, profile;
    private final boolean hard, enderkin;
    private final int x, y, z, minimumThreat;
    private final List<Integer> damageTicks = new ArrayList<Integer>();
    private int stage, health = 20, minimumHealth = 20, heals, overlap, recenterings;
    private long lastClock = -1;
    private ThreatAssessment admission;
    private final Method discharge;
    private final long initialCharge;
    private final kw protectedMachine;
    private final Random placement;
    private boolean stagedSpecialCandidate;

    AssaultPressureProbe(Minecraft game, LocalLunarCalendar lunar, File directory, String save,
            String mode, int x, int y, int z, Random placement) throws Exception {
        this.lunar = lunar;
        this.directory = directory;
        this.save = save;
        this.x = x;
        this.y = y;
        this.z = z;
        this.placement = placement;
        hard = mode.equals("composition-hard");
        enderkin = mode.equals("composition-both");
        boolean quantum = mode.equals("composition-quantum");
        boolean elevated = hard || enderkin || quantum || mode.equals("composition-elevated");
        profile = elevated ? "ELEVATED" : mode.substring("composition-".length()).toUpperCase(java.util.Locale.ROOT);
        minimumThreat = elevated ? 25 : profile.equals("ORDINARY") ? 18 : 0;
        game.A.C = game.f.q = hard ? 3 : 2;
        game.h.aT.a = false;
        protectedMachine = hard ? shelter(game) : null;
        if (quantum) {
            Class<?> electric = Class.forName("ic2.api.ElectricItem");
            discharge = electric.getMethod("discharge", aan.class, int.class, int.class, boolean.class, boolean.class);
            Method charge = electric.getMethod("charge", aan.class, int.class, int.class, boolean.class, boolean.class);
            Method getItem = Class.forName("ic2.api.Items").getMethod("getItem", String.class);
            String[] names = {"quantumBoots", "quantumLeggings", "quantumBodyarmor", "quantumHelmet"};
            for (int slot = 0; slot < names.length; slot++) {
                aan stack = (aan) getItem.invoke(null, names[slot]);
                Alpha6RuntimeProbe.check(stack != null, "installed IC2 supplies " + names[slot]);
                game.h.ap.b[slot] = stack;
                charge.invoke(null, stack, 1_000_000, 3, true, false);
            }
            initialCharge = armorCharge(game);
            Alpha6RuntimeProbe.check(initialCharge > 0, "installed QuantumSuit was charged");
        } else {
            discharge = null;
            initialCharge = 0;
        }
    }

    boolean tick(Minecraft game, Snapshot encounter) throws Exception {
        TerritorialActivityService.Snapshot territory = lunar.territorialSnapshot(game.f);
        if (stage >= 2 && encounter.starts > 0) {
            Alpha6RuntimeProbe.check(encounter.state != SiegeDirector.State.ABORTED,
                    "pressure fixture aborted: " + encounter.reason + " health=" + game.h.bb() + " y=" + game.h.p);
        }
        if (territory == null || territory.threat == null) return false;
        if (stage == 0) {
            game.f.a(BOUNDARY - 11000);
            stage++;
            return false;
        }
        if (stage == 1) {
            if (territory.threat.threat < minimumThreat) return false;
            game.f.a(BOUNDARY - 20);
            stage++;
            return false;
        }
        if (encounter.starts == 0) return false;
        if (admission == null && !profile.equals("DISABLED")) admission = encounter.admissionThreat;
        Alpha6RuntimeProbe.check(encounter.starts == 1 && encounter.boundary == BOUNDARY
                && profile.equals(encounter.admissionProfile) && encounter.admissionThreat == admission
                && encounter.experimentalEnderkin == enderkin, "natural admission freezes composition context");
        Alpha6RuntimeProbe.check(encounter.activeCap == 8 && encounter.localCap == 28 && encounter.peakActive <= 8
                && encounter.spawns.size() <= encounter.budget, "production caps and budget");
        if (stage == 2 && territory.clock != lastClock) {
            lastClock = territory.clock;
            int now = game.h.bb();
            if (now < health) damageTicks.add(encounter.elapsed);
            minimumHealth = Math.min(minimumHealth, now);
            Alpha6RuntimeProbe.check(now > 0, "fixture player survived");
            if (now <= 12) {
                game.h.l(20);
                heals++;
            }
            // Keep a passive test player from being pushed off the raised test arena.
            // Supply a repeatable outdoor candidate and light roll for the first special opportunity.
            boolean stageSpecial = enderkin && !stagedSpecialCandidate && encounter.currentWave == 1
                    && encounter.nextWaveTicks >= 0 && encounter.nextWaveTicks <= 12;
            if (stageSpecial) {
                placement.setSeed(3L);
                stagedSpecialCandidate = true;
            }
            if (stageSpecial || (!hard && (Math.abs(game.h.o - x - .5) > 8 || Math.abs(game.h.q - z - .5) > 8))) {
                game.h.c(x + .5, y, z + .5, game.h.w, game.h.x);
                game.h.r = game.h.s = game.h.t = 0;
                recenterings++;
            }
            health = game.h.bb();
            int wavesActive = 0;
            List<WaveSnapshot> waves = encounter.waves;
            for (WaveSnapshot wave : waves) if (wave.active > 0) wavesActive++;
            overlap = Math.max(overlap, wavesActive);
        }
        if (encounter.state == SiegeDirector.State.ACTIVE) return false;
        if (stage == 2) {
            verifyOutcome(game, encounter, territory);
            game.h.aT.a = true; // Protect the passive player while the HTTP reader acknowledges resolution.
            stage++;
        }
        return new File(directory, save + "-territory-http-pressure.ack").isFile();
    }

    private void verifyOutcome(Minecraft game, Snapshot encounter, TerritorialActivityService.Snapshot territory)
            throws Exception {
        Alpha6RuntimeProbe.check(encounter.state == SiegeDirector.State.RESOLVED
                && "ENCOUNTER_TIMEOUT".equals(encounter.reason), "active combat reached the final timeout");
        Alpha6RuntimeProbe.check(encounter.peakActive == 8 && overlap >= 2, "simultaneous cap and wave overlap");
        List<WaveSnapshot> waves = encounter.waves;
        Alpha6RuntimeProbe.check(waves.stream().anyMatch(wave -> wave.rejections.containsKey("ASSAULT_CAP")),
                "active cap deferred an allocated slot");
        long chargeUsed = initialCharge - armorCharge(game);
        if (hard) {
            Alpha6RuntimeProbe.check(damageTicks.isEmpty() && game.f.a(x + 3, y, z) == 64
                    && game.f.a(x + 3, y + 1, z) == 64, "closed Hard shelter remains intact");
            Alpha6RuntimeProbe.check(game.f.b(x - 2, y, z - 2) == protectedMachine, "protected RedPower TileEntity survived");
        } else if (discharge != null) {
            Alpha6RuntimeProbe.check(chargeUsed > 0 && heals == 0, "QuantumSuit absorbed pressure without safety healing");
        } else {
            Alpha6RuntimeProbe.check(!damageTicks.isEmpty(), "open leather fixture takes real damage");
        }
        String[] expected = hard ? new String[] {"SSSSSS", "SSSSS", "SSSS"}
                : profile.equals("QUIET") ? new String[] {"ZZ", "ZZZSZZ", "ZZZSZZ"}
                : profile.equals("ELEVATED") ? new String[] {"SZSZSZ", "ZZSZZ", "SZSZ"}
                : new String[] {"ZZSZ", "ZZSZZ", "SZSZSZ"};
        StringBuilder slots = new StringBuilder();
        StringBuilder actual = new StringBuilder();
        List<Spawn> spawns = encounter.spawns;
        for (WaveSnapshot wave : waves) {
            StringBuilder vanilla = new StringBuilder();
            List<String> vanillaSlots = wave.vanillaSlots;
            for (String mob : vanillaSlots) vanilla.append(mob.charAt(0));
            Alpha6RuntimeProbe.check(expected[wave.number - 1].equals(vanilla.substring(0, wave.planned)),
                    "planned vanilla slots in wave " + wave.number);
            int inserted = 0;
            for (Spawn spawn : spawns) {
                if (spawn.wave != wave.number) continue;
                String mob = vanillaSlots.get(inserted);
                if (inserted == 0 && wave.enderkin.inserted) mob = wave.enderkin.selected;
                Alpha6RuntimeProbe.check(mob.equals(spawn.mob), "actual insertion matches its slot or substitution");
                actual.append(spawn.mob).append(',');
                inserted++;
            }
            Alpha6RuntimeProbe.check(inserted == wave.inserted && wave.equipped <= wave.equipmentCap,
                    "bounded insertion and equipment");
            slots.append(wave.composition).append(':').append(vanilla).append(',');
        }
        Alpha6RuntimeProbe.check(encounter.loadouts.equipped <= encounter.loadouts.cap, "encounter equipment cap");
        if (enderkin) {
            WaveSnapshot second = waves.get(1);
            Alpha6RuntimeProbe.check("SKIRMISHER".equals(second.enderkin.selected) && second.enderkin.inserted
                    && "BREACHER".equals(waves.get(2).enderkin.selected), "independent seeded substitutions: "
                    + second.enderkin.summary() + " | " + waves.get(2).enderkin.summary());
        }
        List<Integer> gaps = new ArrayList<Integer>();
        for (int i = 1; i < damageTicks.size(); i++) gaps.add(damageTicks.get(i) - damageTicks.get(i - 1));
        Collections.sort(gaps);
        String interval = gaps.isEmpty() ? "-" : gaps.get(0) + "/" + gaps.get(gaps.size() / 2) + "/" + gaps.get(gaps.size() - 1);
        System.out.println("ALPHA6 pressure profile=" + profile + " difficulty=" + game.f.q + " peak=" + encounter.peakActive
                + " overlap=" + overlap + " firstDamage=" + (damageTicks.isEmpty() ? -1 : damageTicks.get(0))
                + " damageSamples=" + damageTicks.size() + " gapsMinMedianMax=" + interval
                + " minimumHealth=" + minimumHealth + " safetyHeals=" + heals + " recenterings=" + recenterings
                + " quantumEUUsed=" + chargeUsed + " terminalTick=" + encounter.elapsed);
        ThreatAssessment current = territory.threat;
        System.out.println("ALPHA6 HTTP pressure " + territory.status + "," + territory.dimension + "," + territory.clock
                + "," + territory.count + "," + territory.presence + "," + territory.returns + " " + current.regionX
                + "," + current.regionZ + "," + current.activity + "," + current.environmental + "," + current.environment
                + "," + current.threat + "," + current.observed + " " + profile + "," + encounter.budget + ","
                + (admission == null ? "-" : admission.threat) + "," + encounter.activeCap + "," + encounter.state + ","
                + waves.get(0).planned + "," + waves.get(1).planned + "," + waves.get(2).planned
                + " " + slots + " " + actual + " " + encounter.peakActive + "," + encounter.reason
                + " " + (admission == null ? "-" : admission.activity + "," + admission.environmental + "," + admission.environment));
    }

    private long armorCharge(Minecraft game) throws Exception {
        if (discharge == null) return 0;
        long charge = 0;
        for (aan armor : game.h.ap.b) {
            if (armor != null) charge += (Integer) discharge.invoke(null, armor, 1_000_000, 3, true, true);
        }
        return charge;
    }

    private kw shelter(Minecraft game) throws Exception {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            for (int dy = 0; dy <= 3; dy++) {
                if (dy == 3 || Math.max(Math.abs(dx), Math.abs(dz)) == 3) game.f.g(x + dx, y + dy, z + dz, 1);
            }
        }
        game.f.b(x + 3, y + 1, z, 64, 8);
        game.f.d(x + 3, y, z, 64, 0);
        int machine = ((pb) Class.forName("RedPowerMachine").getField("blockMachine").get(null)).bO;
        game.f.d(x - 2, y, z - 2, machine, 1);
        kw tile = game.f.b(x - 2, y, z - 2);
        Alpha6RuntimeProbe.check(tile != null, "shelter has an installed RedPower TileEntity");
        return tile;
    }
}
