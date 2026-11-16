import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
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
    private final boolean hard, enderkin, sheltered, balanced, retreat, baseline;
    private final int doorId;
    private final boolean powered;
    private final int x, y, z, minimumThreat;
    private final List<Integer> damageTicks = new ArrayList<Integer>();
    private int stage, health = 20, minimumHealth = 20, heals, overlap, recenterings;
    private long lastClock = -1;
    private ThreatAssessment admission;
    private final Method discharge;
    private final long initialCharge;
    private final kw protectedMachine;
    private final hb protectedChest;
    private final ady originalMachine, originalChest;
    private final int machineBlock, machineMetadata;
    private final Random placement;
    private boolean stagedSpecialCandidate;
    private final boolean stageSpecialCandidate;
    private int firstTarget = -1, firstProjectile = -1, firstPulseShot = -1;
    private int capTicks, naturalBreak = -1, firstPulseAim = -1, firstPressure = -1;
    private int skirmisherTicks, breacherTicks;
    private boolean comparisonInstalled, retreatMoving;
    private int reachedShelter = -1;

    AssaultPressureProbe(Minecraft game, LocalLunarCalendar lunar, File directory, String save,
            String mode, int x, int y, int z, Random placement) throws Exception {
        this.lunar = lunar;
        this.directory = directory;
        this.save = save;
        this.x = x;
        this.y = y;
        this.z = z;
        this.placement = placement;
        hard = mode.startsWith("composition-hard");
        balanced = mode.equals("composition-hard-balanced");
        retreat = mode.equals("composition-hard-retreat");
        baseline = mode.equals("composition-hard-baseline-open");
        sheltered = hard && !mode.equals("composition-hard-open") && !balanced && !baseline;
        powered = mode.equals("composition-hard-powered") || retreat;
        doorId = mode.equals("composition-hard") ? 64 : 71;
        enderkin = mode.equals("composition-both") || mode.endsWith("-enderkin");
        stageSpecialCandidate = mode.equals("composition-both");
        boolean quantum = mode.equals("composition-quantum");
        boolean elevated = hard || quantum || stageSpecialCandidate || mode.startsWith("composition-elevated");
        profile = elevated ? "ELEVATED" : mode.substring("composition-".length()).replace("-enderkin", "")
                .toUpperCase(java.util.Locale.ROOT);
        minimumThreat = elevated ? 25 : profile.equals("ORDINARY") ? 18 : 0;
        game.A.C = game.f.q = hard ? 3 : 2;
        game.h.aT.a = false;
        protectedMachine = sheltered ? shelter(game) : null;
        protectedChest = sheltered ? (hb) game.f.b(x - 2, y, z + 2) : null;
        machineBlock = sheltered ? game.f.a(x - 2, y, z - 2) : 0;
        machineMetadata = sheltered ? game.f.e(x - 2, y, z - 2) : 0;
        originalMachine = tileState(protectedMachine);
        originalChest = tileState(protectedChest);
        if (retreat) prepareRetreat(game);
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
        if (balanced && !comparisonInstalled) {
            installBalancedComparison();
            comparisonInstalled = true;
        }
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
            boolean stageSpecial = stageSpecialCandidate && !stagedSpecialCandidate && encounter.currentWave == 1
                    && encounter.nextWaveTicks >= 0 && encounter.nextWaveTicks <= 12;
            if (stageSpecial) {
                placement.setSeed(3L);
                stagedSpecialCandidate = true;
            }
            if (stageSpecial || (!sheltered && (Math.abs(game.h.o - x - .5) > 8 || Math.abs(game.h.q - z - .5) > 8))) {
                game.h.c(x + .5, y, z + .5, game.h.w, game.h.x);
                game.h.r = game.h.s = game.h.t = 0;
                recenterings++;
            }
            health = game.h.bb();
            int wavesActive = 0;
            List<WaveSnapshot> waves = encounter.waves;
            for (WaveSnapshot wave : waves) if (wave.active > 0) wavesActive++;
            overlap = Math.max(overlap, wavesActive);
            if (encounter.state == SiegeDirector.State.ACTIVE && encounter.active >= encounter.activeCap) capTicks++;
            if (sheltered && naturalBreak < 0 && game.f.a(x + 3, y, z) == 0) naturalBreak = encounter.elapsed;
            observeCombat(game, encounter.elapsed);
            if (retreat) retreat(game, encounter.elapsed);
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
        if (sheltered) {
            if (doorId == 71) {
                Alpha6RuntimeProbe.check(game.f.a(x + 3, y, z) == doorId && game.f.a(x + 3, y + 1, z) == doorId
                        && ((game.f.e(x + 3, y, z) & 4) != 0) == (powered && !retreat), "iron door and its expected state survived");
            }
            Alpha6RuntimeProbe.check(game.f.b(x - 2, y, z - 2) == protectedMachine
                    && game.f.b(x - 2, y, z + 2) == protectedChest
                    && game.f.a(x - 2, y, z - 2) == machineBlock && game.f.e(x - 2, y, z - 2) == machineMetadata
                    && originalMachine.equals(tileState(protectedMachine)) && originalChest.equals(tileState(protectedChest)),
                    "installed RedPower TileEntity and stocked chest retain identity and NBT");
            if (doorId == 64) nativeDoorChecks(game);
        } else if (discharge != null) {
            Alpha6RuntimeProbe.check(chargeUsed > 0 && heals == 0, "QuantumSuit absorbed pressure without safety healing");
        } else {
            Alpha6RuntimeProbe.check(!damageTicks.isEmpty(), "open leather fixture takes real damage");
        }
        if (retreat) {
            Alpha6RuntimeProbe.check(reachedShelter >= 0, "player traversed the powered entrance and shut it");
        }
        String[] expected = baseline ? new String[] {"SSSSSS", "SSSSS", "SSSS"}
                : balanced ? new String[] {"SZSZSZ", "SZSZS", "SZSZ"}
                : hard ? new String[] {"SZSSZS", "SSZSS", "SSZS"}
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
        if (stageSpecialCandidate) {
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
        System.out.println("ALPHA6 combat firstTarget=" + firstTarget + " firstProjectile=" + firstProjectile + " capTicks=" + capTicks
                + " deaths=" + encounter.observedDeaths + " remaining=" + encounter.active
                + " naturalDoorBreak=" + naturalBreak + " doorId=" + (sheltered ? doorId : -1) + " powered=" + powered
                + " reachedShelter=" + reachedShelter + " fixtureBalancedMix=" + balanced
                + " firstPulseAim=" + firstPulseAim + " firstPulseShot=" + firstPulseShot + " skirmisherPresentTicks=" + skirmisherTicks
                + " firstBreachPressure=" + firstPressure + " breacherPresentTicks=" + breacherTicks);
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

    private void observeCombat(Minecraft game, int elapsed) {
        if (firstTarget >= 0 && firstProjectile >= 0 && !enderkin) return;
        boolean seesSkirmisher = false, seesBreacher = false;
        List<nn> entities = game.f.a(nn.class, wu.b(x - 32, y - 12, z - 32, x + 33, y + 16, z + 33));
        for (nn entity : entities) {
            boolean pulse = entity instanceof EntityEnderPulse;
            if (pulse && firstPulseShot < 0) firstPulseShot = elapsed;
            if (firstProjectile < 0 && (pulse || (entity instanceof nm && ((nm) entity).c instanceof xr))) {
                firstProjectile = elapsed;
            }
            if (firstTarget < 0 && entity instanceof yy && ((yy) entity).aT() instanceof yw) {
                firstTarget = elapsed;
            }
            if (entity instanceof EntitySkirmisher) {
                seesSkirmisher = true;
                if (firstPulseAim < 0 && ((EntitySkirmisher) entity).isAiming()) firstPulseAim = elapsed;
            } else if (entity instanceof EntityBreacher) {
                seesBreacher = true;
                if (firstPressure < 0 && ((EntityBreacher) entity).isPressureSwing()) firstPressure = elapsed;
            }
        }
        if (seesSkirmisher) skirmisherTicks++;
        if (seesBreacher) breacherTicks++;
    }

    private static ady tileState(kw tile) {
        ady state = new ady();
        if (tile != null) tile.b(state);
        return state;
    }

    @SuppressWarnings("rawtypes")
    private void installBalancedComparison() throws ReflectiveOperationException {
        // Only the disposable comparator changes the frozen slot pattern; wave budgets and mob AI remain live.
        SiegeDirector director = (SiegeDirector) field(LocalLunarCalendar.class, "assault").get(lunar);
        Object[] waves = (Object[]) field(SiegeDirector.class, "waves").get(director);
        Field composition = field(waves[0].getClass(), "composition");
        Object alternating = null;
        for (Object pattern : composition.getType().getEnumConstants()) {
            if (((Enum) pattern).name().equals("ELEVATED_OPENING")) alternating = pattern;
        }
        Alpha6RuntimeProbe.check(alternating != null, "alternate balanced fixture pattern");
        for (Object wave : waves) composition.set(wave, alternating);
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private void prepareRetreat(Minecraft game) {
        for (int dx = 4; dx <= 7; dx++) {
            game.f.g(x + dx, y, z - 1, 1);
            game.f.g(x + dx, y, z + 1, 1);
        }
        game.h.c(x + 7.5, y, z + .5, 90, 0);
        game.h.a = new oy() {
            @Override
            public void a() { b = retreatMoving ? 1.0F : 0.0F; }
        };
    }

    private void retreat(Minecraft game, int elapsed) {
        if (reachedShelter >= 0 || (damageTicks.isEmpty() && elapsed < 300)) return;
        if (game.h.o > x + 2.65) {
            game.h.w = (float) Math.toDegrees(Math.atan2(game.h.o - (x + 1.5), z + .5 - game.h.q));
            retreatMoving = true;
        } else {
            retreatMoving = false;
            pb.m[69].b(game.f, x + 2, y, z, game.h);
            reachedShelter = elapsed;
        }
    }

    /** Drive the installed task at a fixed path node to isolate the native door rule from target acquisition. */
    private void nativeDoorChecks(Minecraft game) {
        int doorX = x - 8;
        int[][] cases = {{64, 2, 0, 1}, {64, 3, 0, 1}, {71, 3, 0, 0}, {71, 3, 4, 0}, {64, 3, 4, 0}};
        for (int[] fixture : cases) {
            int id = fixture[0], difficulty = fixture[1], data = fixture[2];
            game.f.q = difficulty;
            game.f.g(doorX, y - 1, z, 1);
            game.f.b(doorX, y + 1, z, id, 8);
            game.f.d(doorX, y, z, id, data);
            ajg zombie = new ajg(game.f);
            zombie.c(doorX - .35, y, z + .5, 0, 0);
            zombie.A = true;
            zombie.aM().a(new vu(new e[] {new e(doorX, y, z), new e(doorX + 1, y, z)}), .23F);
            ue task = new ue(zombie);
            boolean selected = task.a();
            Alpha6RuntimeProbe.check(selected == (fixture[3] == 1), "native door selection id=" + id + " data=" + data);
            int broke = -1;
            if (selected) {
                task.c();
                for (int tick = 1; tick <= 240; tick++) {
                    Alpha6RuntimeProbe.check(task.b(), "native task retained for each attack tick");
                    task.e();
                    if (game.f.a(doorX, y, z) == 0) { broke = tick; break; }
                }
            }
            Alpha6RuntimeProbe.check(broke == (selected && difficulty == 3 ? 240 : -1), "native door break rule");
            Alpha6RuntimeProbe.check(game.f.a(doorX, y + 1, z) == (broke < 0 ? id : 0), "matching upper door half");
            System.out.println("ALPHA6 nativeDoor id=" + id + " difficulty=" + difficulty + " metadata=" + data
                    + " selected=" + selected + " breakTick=" + broke);
            game.f.g(doorX, y, z, 0);
            game.f.g(doorX, y + 1, z, 0);
        }
        game.f.q = 3;
    }

    private kw shelter(Minecraft game) throws Exception {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            for (int dy = 0; dy <= 3; dy++) {
                if (dy == 3 || Math.max(Math.abs(dx), Math.abs(dz)) == 3) game.f.g(x + dx, y + dy, z + dz, 1);
            }
        }
        game.f.b(x + 3, y + 1, z, doorId, 8);
        game.f.d(x + 3, y, z, doorId, 0);
        if (powered) {
            game.f.g(x + 2, y - 1, z, 1);
            game.f.d(x + 2, y, z, retreat ? 69 : 76, retreat ? 13 : 5);
            Alpha6RuntimeProbe.check((game.f.e(x + 3, y, z) & 4) != 0, "vanilla redstone opened iron door");
        }
        int machine = ((pb) Class.forName("RedPowerMachine").getField("blockMachine").get(null)).bO;
        game.f.d(x - 2, y, z - 2, machine, 1);
        kw tile = game.f.b(x - 2, y, z - 2);
        Alpha6RuntimeProbe.check(tile != null, "shelter has an installed RedPower TileEntity");
        game.f.d(x - 2, y, z + 2, 54, 0);
        ((hb) game.f.b(x - 2, y, z + 2)).a(0, new aan(265, 17, 0));
        return tile;
    }
}
