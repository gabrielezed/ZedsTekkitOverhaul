import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import zed.tekkitoverhaul.survival.aerial.AerialEcology;
import zed.tekkitoverhaul.survival.aerial.AerialEcology.Snapshot;
import zed.tekkitoverhaul.survival.aerial.AerialPredators;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator.Flight;
import zed.tekkitoverhaul.survival.aerial.EntitySkelebird;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

/** Exercises admission and native entity ticks with fixed terrain, player positions and admission rolls. */
public final class AerialEcologyProbe {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Fixture terrain = new Fixture(root, "aerial-exposure", 2, 6000);
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        if (args.length > 1 && "sample".equals(args[1])) {
            sample(root);
            return;
        }
        terrain.world.forceRoll = 0;
        terrain.position(288.5, 52, false);
        terrain.seconds(6);
        check(terrain.snapshot().height > 45 && terrain.snapshot().exposure > 0
                && terrain.snapshot().admissions == 0, "brief high flight builds exposure without an encounter");
        terrain.world.g(288, 69, 288, 1);
        terrain.position(288.5, 70, true);
        terrain.seconds(4);
        check(terrain.snapshot().height < 1 && terrain.snapshot().exposure == 0,
                "landing on a high mountain column clears exposure");
        terrain.position(294.5, 52, false);
        terrain.seconds(5);
        terrain.world.g(294, 56, 288, 20);
        terrain.seconds(2);
        check("COVERED".equals(terrain.snapshot().band) && terrain.snapshot().exposure == 0,
                "solid glass overhead blocks exposure");
        terrain.position(294.5, 4, true);
        terrain.seconds(3);
        check("COVERED".equals(terrain.snapshot().band), "covered passages stay ineligible");

        Fixture day = new Fixture(root, "aerial-day", 2, 6000);
        day.world.forceRoll = 999;
        day.position(288.5, 4, true);
        day.seconds(30);
        int daytimeChance = day.snapshot().chance;
        check(daytimeChance == 18 && day.snapshot().admissions == 0 && "GROUND".equals(day.snapshot().band),
                "ground daytime has a small nonzero chance");
        // Pass the next admission roll and miss the separate aggression roll.
        day.world.passiveAfterAdmission = true;
        day.seconds(15);
        check(day.snapshot().activeMembers == 1 && day.snapshot().status.contains("passing"),
                "daytime can admit one passive predator");
        day.ticks(160);
        check("-".equals(day.mobs().get(0).targetName()), "passing wildlife stays unengaged after approach");

        Fixture night = new Fixture(root, "aerial-night", 2, 18000);
        night.world.forceRoll = 0;
        night.position(288.5, 4, true);
        night.seconds(30);
        check(night.snapshot().chance == 65 && night.snapshot().activeMembers == 1,
                "night is more favorable and still permits only a single ground predator");
        night.ticks(420);
        check(night.warnings == 1 && night.commitments == 1 && night.snapshot().admissions == 1,
                "natural ground hunt commits one actual dive");

        Fixture normal = new Fixture(root, "aerial-normal", 2, 6000);
        admitHigh(normal, 0);
        check(normal.snapshot().activeMembers == 3, "Normal cap admits a three-member flock");
        verifyFormation(normal);
        Fixture fallback = new Fixture(root, "aerial-fallback", 2, 6000);
        fallback.world.lowFormationCeiling = true;
        admitHigh(fallback, 0);
        Snapshot smaller = fallback.snapshot();
        check(smaller.requestedMembers == 3 && smaller.insertedMembers == 2 && smaller.placementTries == 9
                && smaller.ordinaryAdmissions == 1 && "Ordinary".equals(smaller.encounterSource),
                "eight blocked three-bird sites trigger a real two-bird formation");
        verifyFormation(fallback);
        System.out.println("Formation fallback: requested 3, inserted 2 on site 9");
        int lastDive = -100;
        int dives = 0;
        boolean removed = false;
        for (int i = 0; i < 570 && normal.snapshot().activeMembers > 0; i++) {
            int before = normal.activeDives();
            normal.ticks(1);
            int after = normal.activeDives();
            check(after <= 1, "windups and dives never overlap");
            if (before == 0 && after == 1) {
                check(i - lastDive >= 63, "a group attack has a pause before the next windup");
                lastDive = i;
                ++dives;
                if (!removed) {
                    int reserved = normal.snapshot().attackerId;
                    for (EntityAerialPredator mob : normal.mobs()) {
                        if (mob.f == reserved) {
                            normal.world.b.remove(mob); // Simulate the native loaded-list removal on chunk unload.
                            removed = true;
                            break;
                        }
                    }
                }
            }
        }
        check(removed && dives >= 2 && normal.commitments >= 1, "a second predator dives after losing the first attacker");
        check(normal.snapshot().activeMembers == 0 && normal.snapshot().cooldownSeconds > 0,
                "the flock ends on the actual encounter lifecycle");
        normal.position(323.5, 52, false);
        normal.seconds(35);
        check(normal.snapshot().admissions == 1 && normal.snapshot().cooldownSeconds >= 50,
                "crossing a territorial boundary cannot bypass the recovery period");

        Fixture transit = new Fixture(root, "aerial-transit", 2, 6000);
        admitHigh(transit, 0.18);
        for (int tick = 0; tick < 300 && transit.commitments == 0; tick++) {
            transit.player.c(288.5, 52, 288.5 + tick * 0.18, 0, 0);
            transit.player.t = 0.18;
            transit.ticks(1);
        }
        check(transit.commitments > 0, "natural hunters can commit a dive against a moving pilot");

        Fixture hard = new Fixture(root, "aerial-hard", 3, 6000);
        hard.position(288.5, 4, true);
        for (int i = 0; i < 4800; i++) hard.territory.tick(hard.player, false);
        admitHigh(hard, 0);
        verifyFormation(hard);
        check(hard.snapshot().activeMembers >= 4 && hard.snapshot().activeMembers <= 5
                && hard.snapshot().activity > 0 && hard.snapshot().chance > normal.snapshot().chance,
                "territorial activity and Hard produce higher admission pressure and a larger bounded flock");
        int hardSize = hard.snapshot().activeMembers;
        hard.mobs().get(0).A();
        hard.seconds(1);
        check(hard.snapshot().activeMembers == hardSize - 1, "a dead member leaves the roster");
        EntityAerialPredator wild = hard.mobs().get(0);
        ady tag = new ady();
        check(wild.c(tag) && tag.f("ZtoAerialWildTicks") > 0, "wild origin is recorded");
        EntityAerialPredator returned = (EntityAerialPredator) aao.a(tag, hard.world);
        check(returned != null && returned.flight() == Flight.CRUISE && "-".equals(returned.targetName()),
                "wild reload has no transient flock or target");
        for (int tick = 0; tick < 390 && hard.commitments < 2; tick++) {
            hard.ticks(1);
            check(hard.activeDives() <= 1, "Hard flock keeps a single attack turn");
        }
        check(hard.commitments >= 2, "Hard flock commits multiple spaced dives");
        hard.position(288.5, 4, true);
        hard.seconds(1);
        check(hard.snapshot().activeMembers == 0 && hard.snapshot().lastEnd.contains("descended"),
                "descending ends the entire airborne encounter");
        int quiet = hard.snapshot().cooldownSeconds;
        hard.world.z.a();
        Fixture reopened = new Fixture(root, "aerial-hard", 3, 6000);
        check(reopened.snapshot().cooldownSeconds == quiet, "quiet period persists through native map storage");

        Fixture blocked = new Fixture(root, "aerial-unloaded", 2, 6000);
        blocked.world.limitPlacement = true;
        blocked.world.forceRoll = 0;
        blocked.position(288.5, 52, false);
        blocked.seconds(12);
        Snapshot failed = blocked.snapshot();
        check(failed.placementTries > 0 && failed.placementTries <= 16 && failed.admissions == 0,
                "unloaded spawn attempts terminate within the placement budget");
        blocked.seconds(8);
        check(blocked.snapshot().placementTries == failed.placementTries && blocked.snapshot().checkSeconds > 0,
                "a failed placement waits for another timed pulse");
        blocked.world.limitPlacement = false;
        for (int i = 0; i < 5; i++) {
            EntityAerialPredator existing = i % 2 == 0
                    ? new EntitySkelebird(blocked.world) : new EntityAerialPredator(blocked.world);
            existing.c(272 + i, 60, 291, 0, 0);
            check(blocked.world.a(existing), "fixture fills the aerial cap");
        }
        blocked.seconds(15);
        check(blocked.snapshot().admissions == 0 && blocked.snapshot().lastAttempt.contains("cap"),
                "loaded predators consume the admission cap");

        blocked.nativeSpawns.register();
        vf.a(blocked.world, false, false);
        blocked.seconds(15);
        check(blocked.snapshot().lastAttempt.contains("disabled by world"), "the vanilla hostile-pass setting gates admission");

        AerialEcology disabled = new AerialEcology(false, 90, blocked.territory, blocked.nativeSpawns);
        disabled.bind(blocked.world);
        for (int i = 0; i < 1200; i++) disabled.tick(blocked.player, false);
        check(disabled.snapshot().status.contains("Disabled") && disabled.snapshot().admissions == 0,
                "natural admission can be disabled independently of the registered species");
        verifySkelebirdEcology(root);
        verifyNaturalRestage(root);
        verifyChickenHunting(root);
        System.out.printf("Admission fixtures: Normal flock 3, Hard flock %d at activity %d, ground rolls %d/%d per 1000%n",
                hardSize, hard.snapshot().activity, daytimeChance, night.snapshot().chance);
        System.out.println("PASS aerial altitude, exposure, ground day/night, ambient flight, safe spawn, "
                + "Normal/Hard territory flocks, stagger, lost attacker, cooldown, reload, bounds and disabled admission");
    }

    private static void verifyNaturalRestage(File root) throws Exception {
        Fixture fixture = new Fixture(root, "aerial-natural-restage", 2, 18000);
        fixture.position(288.5, 4, true);
        fixture.world.forceRoll = 0;
        fixture.seconds(30);
        EntityAerialPredator bird = fixture.mobs().get(0);
        for (int i = 0; i < 210 && bird.flight() != Flight.PREPARE; ++i) fixture.ticks(1);
        check(bird.flight() == Flight.PREPARE, "naturally admitted ground Fugl warns");
        int x = (int) Math.floor((bird.o + fixture.player.o) * 0.5);
        int y = (int) Math.floor((bird.p + fixture.player.y.b + 1) * 0.5);
        int z = (int) Math.floor((bird.q + fixture.player.q) * 0.5);
        for (int bx = x - 1; bx <= x + 1; ++bx)
            for (int by = y - 2; by <= y + 2; ++by)
                for (int bz = z - 1; bz <= z + 1; ++bz) fixture.world.g(bx, by, bz, 1);
        for (int i = 0; i < 22 && bird.flight() == Flight.PREPARE; ++i) fixture.ticks(1);
        check(bird.rejectedWarnings() == 1 && bird.diveCommitments() == 0 && fixture.snapshot().attackerId < 0
                && fixture.snapshot().gapTicks > 0, "obstruction cancels the warning and releases the turn");
        boolean changedLane = false;
        for (int i = 0; i < 400 && bird.diveCommitments() == 0; ++i) {
            fixture.ticks(1);
            changedLane |= bird.diveStatus().contains("different lane");
        }
        check(changedLane && bird.diveCommitments() == 1 && fixture.warnings >= 2,
                "Fugl commits from an alternate lane with the barrier still standing: " + bird.lastExit()
                        + " / " + bird.lastDiveRejection());

        Fixture skeleton = new Fixture(root, "aerial-natural-sidestep", 2, 18000);
        skeleton.position(288.5, 4, true);
        for (int i = 0; i < 4800; ++i) skeleton.territory.tick(skeleton.player, false);
        skeleton.world.speciesRoll = 7;
        skeleton.world.forceRoll = 0;
        skeleton.seconds(30);
        EntityAerialPredator crossing = skeleton.mobs().get(0);
        check(crossing instanceof EntitySkelebird, "established Normal night ground admits a solitary Skelebird");
        for (int i = 0; i < 300 && crossing.flight() != Flight.PREPARE; ++i) skeleton.ticks(1);
        check(crossing.flight() == Flight.PREPARE, "natural Skelebird locks its warning aim");
        double lockX = crossing.destinationX(), lockZ = crossing.destinationZ();
        double dx = crossing.o - skeleton.player.o, dz = crossing.q - skeleton.player.q;
        double length = Math.sqrt(dx * dx + dz * dz);
        skeleton.player.c(lockX - dz / length * 4, 4, lockZ + dx / length * 4, 0, 0);
        skeleton.player.z = true;
        for (int i = 0; i < 70 && crossing.flight() != Flight.RECOVER; ++i) skeleton.ticks(1);
        check(crossing.diveCommitments() == 1 && crossing.lastFailure().contains("missed"),
                "the warning-time sidestep defeats a natural Skelebird crossing");
        System.out.println("PASS ordinary Fugl restage around a persistent barrier and natural Skelebird warning-time dodge");
    }

    private static void sample(File root) throws Exception {
        measure(root, "ground-day-N0", 2, 6000, 4, false, false, 2400);
        measure(root, "ground-night-N0", 2, 18000, 4, false, false, 2400);
        measure(root, "ground-night-N24", 2, 18000, 4, true, false, 2400);
        measure(root, "ground-night-H24", 3, 18000, 4, true, false, 2400);
        measure(root, "ridge-night-H24", 3, 18000, 4, true, false, 1600);
        measure(root, "shelter-night-H24", 3, 18000, 4, true, true, 400);
        measure(root, "low-air-day-N0", 2, 6000, 13, false, false, 400);
        measure(root, "exposed-day-N0", 2, 6000, 27, false, false, 400);
        measure(root, "high-night-N0", 2, 18000, 52, false, false, 400);
        measure(root, "high-night-H0", 3, 18000, 52, false, false, 400);
        measure(root, "high-night-H24", 3, 18000, 52, true, false, 400);
    }

    private static void measure(File root, String name, int difficulty, int time, int altitude,
                                boolean established, boolean sheltered, int trials) throws Exception {
        Fixture fixture = new Fixture(root, name, difficulty, time);
        fixture.position(288.5, 4, true);
        if (established) for (int t = 0; t < 4800; ++t) fixture.territory.tick(fixture.player, false);
        if (sheltered) fixture.world.g(288, 7, 288, 20);
        if (name.startsWith("ridge")) {
            for (int x = 278; x <= 298; ++x) {
                for (int y = 4; y <= 15; ++y) fixture.world.g(x, y, 277, 1);
            }
        }
        fixture.position(288.5, altitude, altitude == 4);
        fixture.seconds(130);
        Field nextCheck = AerialEcology.class.getDeclaredField("nextCheck");
        nextCheck.setAccessible(true);
        Field data = AerialEcology.class.getDeclaredField("state");
        data.setAccessible(true);
        Object saved = data.get(fixture.ecology);
        Field cooldown = saved.getClass().getDeclaredField("cooldownSeconds");
        cooldown.setAccessible(true);
        Field birdRandom = nn.class.getDeclaredField("U");
        birdRandom.setAccessible(true);
        fixture.world.r = new Random(926833L);
        int attempts = 0, misses = 0, blocked = 0, hunting = 0, passing = 0;
        int acquired = 0, warned = 0, committed = 0, fugl = 0, skeletal = 0, fallback = 0;
        int skeletalGroups = 0;
        int[] sizes = new int[5];
        for (int trial = 0; trial < trials; ++trial) {
            // Each trial samples an eligible check independently of time spent on quiet cooldown.
            nextCheck.setInt(fixture.ecology, 0);
            cooldown.setInt(saved, 0);
            fixture.seconds(1);
            Snapshot result = fixture.snapshot();
            if (result.checkSeconds == 0) continue;
            ++attempts;
            List<EntityAerialPredator> birds = fixture.mobs();
            if (birds.isEmpty()) {
                if (result.lastAttempt.contains("roll missed")) ++misses;
                else ++blocked;
                continue;
            }
            ++sizes[birds.size() - 1];
            if (birds.get(0) instanceof EntitySkelebird) ++skeletalGroups;
            if (result.placementTries > 8) ++fallback;
            for (EntityAerialPredator bird : birds) {
                if (bird instanceof EntitySkelebird) ++skeletal;
                else ++fugl;
                birdRandom.set(bird, new Random(7519L + trial));
            }
            if (result.status.contains("hunt")) {
                ++hunting;
                boolean hasTarget = false;
                fixture.warnings = fixture.commitments = 0;
                for (int t = 0; t < 660 && fixture.commitments == 0 && fixture.snapshot().activeMembers > 0; ++t) {
                    fixture.ticks(1);
                    for (EntityAerialPredator bird : birds) hasTarget |= !"-".equals(bird.targetName());
                }
                if (hasTarget) ++acquired;
                if (fixture.warnings > 0) ++warned;
                if (fixture.commitments > 0) ++committed;
            } else ++passing;
            for (EntityAerialPredator bird : birds) bird.A();
            fixture.world.n();
            fixture.position(288.5, altitude, altitude == 4);
            fixture.seconds(1);
        }
        Snapshot end = fixture.snapshot();
        System.out.printf("%s %s activity=%d odds=%d trials=%d checks=%d miss=%d blocked=%d admitted=%d Fugl=%d Skelebird=%d skeletalGroups=%d sizes=%s fallback=%d passing=%d hunting=%d acquired=%d warned=%d committed=%d%n",
                name, end.band, end.activity, end.chance, trials, attempts, misses, blocked, hunting + passing, fugl, skeletal,
                skeletalGroups, Arrays.toString(sizes), fallback, passing, hunting, acquired, warned, committed);
    }

    private static void verifyChickenHunting(File root) throws Exception {
        Fixture farm = new Fixture(root, "fugl-chicken-hunt", 2, 6000);
        farm.position(288.5, 4, true);
        farm.world.passiveAfterAdmission = true;
        for (int i = 0; i < 45 && farm.snapshot().admissions == 0; ++i) farm.seconds(1);
        check(farm.snapshot().activeMembers == 1 && farm.snapshot().status.contains("passing"),
                "chicken fixture uses the ordinary passive daytime encounter");
        EntityAerialPredator bird = farm.mobs().get(0);
        java.lang.reflect.Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(bird, new Random(44L) {
            @Override public int nextInt(int bound) { return bound == 8 ? 0 : super.nextInt(bound); }
        });

        farm.ticks(80); // Finish the arrival leg before placing the farm alongside the passing bird.
        rd exposed = chicken(farm, bird.o + 7, bird.q);
        rd roofed = chicken(farm, bird.o - 9, bird.q - 7);
        for (int x = (int) roofed.o - 2; x <= (int) roofed.o + 2; ++x) {
            for (int z = (int) roofed.q - 2; z <= (int) roofed.q + 2; ++z) farm.world.g(x, 7, z, 1);
        }
        rd second = chicken(farm, bird.o + 10, bird.q + 2);
        boolean activePrey = false;
        for (int tick = 0; tick < 365; ++tick) {
            farm.ticks(1);
            activePrey |= farm.snapshot().preyStatus.contains("Chicken #" + exposed.f);
        }
        check(activePrey && exposed.bb() <= 0 && farm.commitments == 1,
                "the passing Fugl selects exposed prey and commits one damaging dive; " + bird.lastExit()
                        + " / " + bird.lastFailure() + " / " + farm.commitments + " health " + exposed.bb());
        check(roofed.bb() == 4 && second.bb() == 4,
                "roofed and subsequent chickens take no damage");

        Fixture enclosure = new Fixture(root, "fugl-walled-prey", 2, 6000);
        enclosure.position(288.5, 4, true);
        enclosure.world.passiveAfterAdmission = true;
        for (int i = 0; i < 45 && enclosure.snapshot().admissions == 0; ++i) enclosure.seconds(1);
        EntityAerialPredator passer = enclosure.mobs().get(0);
        random.set(passer, new Random(44L) {
            @Override public int nextInt(int bound) { return bound == 8 ? 0 : super.nextInt(bound); }
        });
        enclosure.ticks(80);
        int gx = (int) Math.floor(passer.o + 7), gz = (int) Math.floor(passer.q);
        for (int x = gx - 1; x <= gx + 1; ++x) {
            for (int z = gz - 1; z <= gz + 1; ++z) {
                if (x == gx && z == gz) continue;
                for (int y = 4; y <= 10; ++y) enclosure.world.g(x, y, z, 1);
            }
        }
        rd walled = chicken(enclosure, gx + 0.5, gz + 0.5);
        enclosure.ticks(365);
        check(walled.bb() == 4 && enclosure.commitments == 0, "tall side walls prevent a descending strike");
        System.out.println("PASS exposed chicken damage, roofed and walled enclosure protection, one-prey budget");
    }

    private static rd chicken(Fixture farm, double x, double z) {
        rd prey = new rd(farm.world);
        prey.c(x, 4, z, 0, 0);
        check(farm.world.a(prey), "insert farm chicken");
        return prey;
    }

    private static void verifySkelebirdEcology(File root) throws Exception {
        Fixture daylight = new Fixture(root, "skelebird-daylight", 3, 6000);
        daylight.world.speciesRoll = 0;
        admitHigh(daylight, 0);
        check(!(daylight.mobs().get(0) instanceof EntitySkelebird), "day encounters remain Fugl");

        Fixture easy = new Fixture(root, "skelebird-easy", 1, 18000);
        easy.world.speciesRoll = 12;
        admitHigh(easy, 0);
        check(!(easy.mobs().get(0) instanceof EntitySkelebird), "a species roll misses on Easy");

        Fixture normal = new Fixture(root, "skelebird-normal", 2, 18000);
        normal.world.speciesRoll = 12;
        admitHigh(normal, 0);
        check(normal.mobs().size() == 1 && normal.mobs().get(0) instanceof EntitySkelebird,
                "the same roll gives a solo Skelebird on Normal at night");

        Fixture hard = new Fixture(root, "skelebird-hard", 3, 18000);
        hard.position(288.5, 4, true);
        for (int i = 0; i < 4800; ++i) hard.territory.tick(hard.player, false);
        hard.world.speciesRoll = 31;
        admitHigh(hard, 0);
        verifyFormation(hard);
        check(hard.snapshot().activity >= 12 && hard.mobs().size() == 3
                && hard.mobs().stream().allMatch(mob -> mob instanceof EntitySkelebird),
                "territorial pressure adds a bounded Hard group at a roll above the quiet share");
        ady tag = new ady();
        check(hard.mobs().get(0).c(tag) && tag.f("ZtoAerialWildTicks") > 0, "wild Skelebird origin is saved");
        EntitySkelebird returned = (EntitySkelebird) aao.a(tag, hard.world);
        check(returned.flight() == Flight.CRUISE && "-".equals(returned.targetName()), "wild reload retires from the hunt");
        int released = -64;
        for (int tick = 0; tick < 500 && hard.commitments < 2; ++tick) {
            int before = hard.activeDives();
            hard.ticks(1);
            int after = hard.activeDives();
            check(after <= 1, "a Skelebird flock has one active attack turn");
            if (before != 0 && after == 0) released = tick;
            if (before == 0 && after != 0) check(tick - released >= 63, "Skelebird turns have a recovery gap");
        }
        check(hard.commitments >= 2, "multiple Skelebirds actually commit passes");
        hard.position(288.5, 4, true);
        hard.seconds(2);
        check(hard.snapshot().activeMembers == 0 && hard.snapshot().cooldownSeconds >= 89
                && hard.snapshot().lastEnd.contains("descended"), "descent ends the Skelebird flock and preserves cooldown");
        System.out.println("PASS Skelebird night share, Easy/Normal, Hard territory flock, staggered turns, wild reload and shared cooldown");
    }

    private static void admitHigh(Fixture fixture, double speedZ) {
        fixture.world.forceRoll = 999;
        fixture.position(288.5, 52, false);
        fixture.seconds(25);
        check("HIGH_SKY".equals(fixture.snapshot().band) && fixture.snapshot().admissions == 0,
                "sustained clearance reaches high sky before a successful roll");
        fixture.world.forceRoll = 0;
        fixture.player.t = speedZ;
        for (int second = 0; second < 16 && fixture.snapshot().admissions == 0; second++) fixture.seconds(1);
        check(fixture.snapshot().admissions == 1, "a natural group entered a loaded high-sky fixture");
    }

    private static void verifyFormation(Fixture fixture) {
        List<EntityAerialPredator> mobs = fixture.mobs();
        check(mobs.size() == fixture.snapshot().activeMembers, "world insertion matches membership");
        for (EntityAerialPredator mob : mobs) {
            check(mob.q < fixture.player.q - 5 && fixture.player.f(mob) >= 24 * 24,
                    "placement behind the view and separated from the player");
            check(fixture.world.b((int) mob.o, 0, (int) mob.q, (int) mob.o, 0, (int) mob.q)
                    && fixture.world.a(mob, mob.y).isEmpty(), "loaded collision-free box");
            for (EntityAerialPredator other : mobs) {
                if (mob != other) check(mob.f(other) >= 3 * 3, "formation slots are separated");
            }
        }
    }

    private static final class Fixture {
        final ProbeWorld world;
        final yw player;
        final TerritorialActivityService territory = new TerritorialActivityService();
        final NaturalSpawnPressure nativeSpawns = new NaturalSpawnPressure(territory, 70, true);
        final AerialEcology ecology = new AerialEcology(true, 90, territory, nativeSpawns);
        int warnings, commitments;

        Fixture(File root, String name, int difficulty, long time) throws Exception {
            world = new ProbeWorld(root, name);
            File marker = new File(root, name + "/.zto-disposable");
            if (!marker.exists()) Files.createFile(marker.toPath());
            world.q = difficulty;
            world.a(time);
            world.q();
            for (int x = 14; x <= 22; x++) {
                for (int z = 14; z <= 22; z++) world.d(x, z);
            }
            world.r = new Random(3865L) {
                @Override public int nextInt(int bound) {
                    if (bound == 100) return world.speciesRoll;
                    if (bound == 6 && world.lowFormationCeiling) return 0;
                    if (bound != 1000) return super.nextInt(bound);
                    if (world.passiveAfterAdmission) {
                        world.passiveAfterAdmission = false;
                        return 0;
                    }
                    return world.forceRoll;
                }
            };
            player = NaturalSpawnProbe.join(world);
            player.aA = "Pilot";
            territory.bind(world);
            nativeSpawns.bind(world);
            ecology.bind(world);
        }

        void position(double x, double y, boolean grounded) {
            player.c(x, y, 288.5, 0, 0);
            player.z = grounded;
            player.r = player.s = player.t = 0;
        }

        Snapshot snapshot() { return ecology.snapshot(); }
        void seconds(int seconds) { for (int i = 0; i < seconds * 20; i++) ecology.tick(player, false); }

        void ticks(int count) {
            for (int i = 0; i < count; i++) {
                wu.b(); // Reset the native AABB and ray vector pools like the windowed client loop.
                bo.b();
                for (EntityAerialPredator mob : mobs()) {
                    Flight before = mob.flight();
                    world.a(mob, true);
                    if (before != Flight.PREPARE && mob.flight() == Flight.PREPARE) ++warnings;
                    if (before != Flight.DIVE && mob.flight() == Flight.DIVE) ++commitments;
                }
                player.l(20);
                player.r = player.s = player.t = 0;
                ecology.tick(player, false);
            }
        }

        int activeDives() {
            int count = 0;
            for (EntityAerialPredator mob : mobs()) {
                if (mob.flight() == Flight.PREPARE || mob.flight() == Flight.DIVE) ++count;
            }
            return count;
        }

        List<EntityAerialPredator> mobs() {
            List<EntityAerialPredator> result = new ArrayList<EntityAerialPredator>();
            for (Object entry : world.b) {
                if (entry instanceof EntityAerialPredator && !((nn) entry).G) result.add((EntityAerialPredator) entry);
            }
            return result;
        }
    }

    private static final class ProbeWorld extends xd {
        int forceRoll = 999, speciesRoll = 99;
        boolean limitPlacement, passiveAfterAdmission, lowFormationCeiling;
        ProbeWorld(File root, String name) {
            super(new aia(root, name, false), name, NaturalSpawnProbe.SETTINGS);
        }
        @Override public void a(nn entity, String sound, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        @Override public boolean m(int x, int y, int z) {
            return (!lowFormationCeiling || y < 64) && super.m(x, y, z);
        }
        @Override public boolean b(int x, int y, int z, int maxX, int maxY, int maxZ) {
            if (limitPlacement && (x < 267 || z < 267 || maxX > 309 || maxZ > 309)) return false;
            return super.b(x, y, z, maxX, maxY, maxZ);
        }
        @Override public int f(int x, int z) {
            check(!limitPlacement || (x >= 267 && z >= 267 && x <= 309 && z <= 309),
                    "no height lookup in unavailable placement chunks");
            return super.f(x, z);
        }
    }

    private static void check(boolean expected, String message) {
        if (!expected) throw new AssertionError(message);
    }
}
