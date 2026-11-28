import java.awt.image.BufferedImage;
import java.io.File;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalWaterFaces;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityDiverZombie;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityDiverSkeleton;
import zed.tekkitoverhaul.exploration.abyssal.guardian.DiverArrow;
import zed.tekkitoverhaul.exploration.abyssal.guardian.AquaticDevelopment;
import zed.tekkitoverhaul.exploration.abyssal.guardian.AquaticDevelopment.Species;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalGuardian;
import zed.tekkitoverhaul.exploration.abyssal.guardian.GuardianEncounters;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.AbyssalWorldgen;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.TrenchRegion;
import zed.tekkitoverhaul.survival.tactical.EntityEnderPulse;

/** Uses the reobfuscated candidate, pinned provider, installed native entity classes and Anvil. */
public final class ObservatoryProbe {
    public static void main(String[] args) {
        try {
            run(new File(args[0]), new File(args[1]), args.length > 2 && "custodian".equals(args[2]), args.length > 3);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File root, File previews, boolean custodianOnly, boolean ownerSave) throws Exception {
        UnderwaterEquipmentProbe.startFixture(root, "setup");
        ZtoConfig config = ZtoConfigLoader.load(new File(root, "config").toPath(), false, Logger.getLogger("observatory"));
        AbyssalRegistry.register(config, 100);
        AbyssalWorldgen.register(config.abyssalId("biome.abyssalTrench"));
        ObservatorySites.register();
        AbyssalWaterFaces.register();
        GuardianEncounters.register();
        zed.tekkitoverhaul.survival.tactical.TacticalHostiles.register();
        zed.tekkitoverhaul.survival.tactical.TacticalHostiles.registerEggs(config);
        zed.tekkitoverhaul.survival.aerial.AerialPredators.register();
        zed.tekkitoverhaul.survival.aerial.AerialPredators.registerEgg(config.aerialPredatorEgg);
        zed.tekkitoverhaul.survival.aerial.AerialPredators.registerSkelebirdEgg(config.skelebirdEgg);
        zed.tekkitoverhaul.survival.aerial.AerialPredators.registerMagpieEgg(config.magpieEgg);
        zed.tekkitoverhaul.assault.SiegeDirector.registerSiteInfantry();
        zed.tekkitoverhaul.survival.CreatureEggs.register(config.abyssalId("item.creatureEgg"));
        zed.tekkitoverhaul.survival.CreatureEggs.audit();
        CustodianProbe.verifyCreativeCatalog(config);
        zed.tekkitoverhaul.emc.EnderPearlScarcity.register();
        Class<?> planClass = Class.forName("zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatoryPlan");
        Method candidate = method(ObservatorySites.class, "candidate", long.class, int.class, int.class);
        Method survey = method(ObservatorySites.class, "survey", xd.class, planClass);
        List<long[]> discovered = new ArrayList<>();
        int nominal = 0, eligible = 0, surveyed = 0;
        long started = System.nanoTime();
        for (long seed : new long[] {32021, -32021, 32022, -32022}) {
            xd world = world(root, "survey-" + seed, seed);
            check(ObservatorySites.supported(world), "pinned Anvil world supported");
            for (int gz = -9; gz <= 9; gz++) for (int gx = -9; gx <= 9; gx++) {
                nominal++;
                Object plan = candidate.invoke(null, seed, gx, gz);
                if (plan == null) continue;
                eligible++;
                int floor = (Integer) survey.invoke(null, world, plan);
                if (floor == 0) continue;
                surveyed++;
                if (discovered.size() < 12) discovered.add(new long[] {seed, gx, gz, floor});
                System.out.printf("Suitable seed=%d x=%d y=%d z=%d width=%d length=%d%n",
                        seed, get(plan, "x"), floor, get(plan, "z"), get(plan, "width") * 2 + 1, get(plan, "length") + 6);
            }
        }
        System.out.printf(Locale.ROOT, "Surveyed %d cells: %d candidates, %d suitable (%.4f per million blocks), %.0f ms%n",
                nominal, eligible, surveyed, surveyed * 1e6 / (nominal * 512.0 * 512.0),
                (System.nanoTime() - started) / 1e6);
        check(discovered.size() >= 2, "representative survey found distinct sites");
        if (!custodianOnly) {
            verifySwimming(root);
            verifyDivers(root);
        }
        verifyWaterFace(root);
        int completed = 0;
        verifyLegacy(root, 1);
        verifyLegacy(root, 2);
        verifyLegacy(root, 3);
        verifyLegacy(root, 4);
        long[] ownerLocation = null;
        for (long[] sample : discovered) {
            long seed = sample[0];
            Object plan = candidate.invoke(null, seed, (int) sample[1], (int) sample[2]);
            int x = get(plan, "x"), z = get(plan, "z"), y = (int) sample[3];
            int minX = get(plan, "minX") >> 4, maxX = get(plan, "maxX") >> 4;
            int minZ = get(plan, "minZ") >> 4, maxZ = get(plan, "maxZ") >> 4;
            String name = "fresh-" + completed + "-" + sample[1] + "-" + sample[2];
            xd world = world(root, name, seed);
            if (world.z().a(minX, minZ)) continue;
            long generation = System.nanoTime();
            world.d(minX, minZ);
            check(!world.z().a(maxX, maxZ), "first slice did not ask for the opposite corner");
            load(world, minX, maxX, minZ, maxZ, false);
            Site site = ObservatorySites.nearby(world, x, z);
            if (site == null || !site.ready(world)) {
                System.out.println("Unready native candidate at " + x + ", " + z + ": " + (site == null ? "unadmitted" : site.status()));
                continue;
            }
            check(site.revision() >= 4 && site.count() >= 8 && site.count() <= 12, "buried plan and reduced allocation");
            System.out.printf("Native site seed=%d entrance=%d,%d,%d id=%x allocated=%d generation=%.0f ms%n",
                    seed, x, y, z, site.identity(), site.count(), (System.nanoTime() - generation) / 1e6);
            verifyBurial(world, site, plan, y, previews, completed);
            verifyLighting(world, minX, maxX, minZ, maxZ, y, site.revision());
            if (completed == 0) {
                ownerLocation = sample;
                defendOld(root, seed, minX, minZ, maxX, maxZ, x, z);
                verifyActors(world, site, x, y, z);
                CustodianProbe.verifyEggs(world, site, config);
                CustodianProbe.exercise(world, site);
                int[] actorHome = site.home(0);
                int actorChunkX = actorHome[0] >> 4, actorChunkZ = actorHome[2] >> 4;
                String population = site.status();
                world.a(new uh(0, y, 0));
                ((ko) world.z()).d(actorChunkX, actorChunkZ);
                world.z().a();
                world.n();
                ce.a.a();
                check(!world.z().a(actorChunkX, actorChunkZ) && !site.ready(world), "native eviction released a site slice");
                world.d(actorChunkX, actorChunkZ);
                check(site.ready(world) && site.status().equals(population), "eviction and reload did not consume slots");
                int modifiedX = x, modifiedZ = z;
                world.g(modifiedX, y + 2, modifiedZ, 5);
                world.z().a(true, null);
                ce.a.a();
                agp.a();
                xd reopened = world(root, name, seed);
                load(reopened, minX, maxX, minZ, maxZ, true);
                Site saved = ObservatorySites.nearby(reopened, x, z);
                check(saved != null && saved.identity() == site.identity() && saved.ready(reopened), "native slice stamps and record reopened");
                check(reopened.a(modifiedX, y + 2, modifiedZ) == 5, "player edit survived without repair");
                check(reopened.b(EntityAbyssalEnderkin.class) > 0, "living population reopened from native entity NBT");
                check(saved.status().equals(site.status()), "population ledger survived save and reopen");
                CustodianProbe.reopened(reopened, saved);
                verifyCaster(reopened, saved);
                reopened.q = 0;
                for (Object entry : new ArrayList<>(reopened.b)) {
                    if (entry instanceof EntityAbyssalEnderkin) ((EntityAbyssalEnderkin) entry).J_();
                }
                check(!saved.status().endsWith("| 0 retired"), "Peaceful retires native living site actors");
                reopened.z().a(true, null);
                ce.a.a();
                agp.a();
                xd terminal = world(root, name, seed);
                load(terminal, minX, maxX, minZ, maxZ, true);
                CustodianProbe.terminal(terminal, ObservatorySites.nearby(terminal, x, z));
                if (!custodianOnly) verifyWild(root, seed);
            }
            completed++;
        }
        check(completed >= 3, "at least three complete native Observatory candidates");
        System.out.printf("Native admission: %d of %d surveyed sites exercised to completion%n", completed, surveyed);
        if (ownerSave) {
            long[] sample = ownerLocation;
            Object plan = candidate.invoke(null, sample[0], (int) sample[1], (int) sample[2]);
            xd owner = world(root, CustodianProbe.OWNER_SAVE, sample[0], 1);
            load(owner, get(plan, "minX") >> 4, get(plan, "maxX") >> 4,
                    get(plan, "minZ") >> 4, get(plan, "maxZ") >> 4, false);
            int x = get(plan, "x"), z = get(plan, "z");
            Site site = ObservatorySites.nearby(owner, x, z);
            check(site != null && site.ready(owner), "owner fixture native site");
            Field frozen = Site.class.getDeclaredField("plan");
            frozen.setAccessible(true);
            Object portal = frozen.get(site);
            Method position = method(portal.getClass(), "position", int.class, int.class, int.class);
            int sill = get(portal, "sill");
            int[] entrance = (int[]) position.invoke(portal, 0, sill + 2, -5);
            int[] openWater = (int[]) position.invoke(portal, 12, 55 - (int) sample[3], -18);
            CustodianProbe.ownerSave(root, owner, site, config, x, z, entrance, openWater);
            System.out.printf("Owner entrance seed=%d X=%d Z=%d base Y=%d%n", sample[0], x, z, sample[3]);
        }
        System.out.println("PASS native Observatory placement, alternatives, stamps, old terrain and finite Enderkin persistence");
    }

    private static void verifyLegacy(File root, int revision) throws Exception {
        long seed = 32021;
        int x = -1170, y = 22, z = 765;
        String name = "legacy-revision-" + revision;
        xd legacy = world(root, name, seed);
        File directory = new File(root, name + "/data/zto-observatories");
        Files.createDirectories(directory.toPath());
        Object plan = method(ObservatorySites.class, "candidate", long.class, int.class, int.class)
                .invoke(null, seed, -3, 1);
        Field id = typeWithField(plan.getClass(), "id").getDeclaredField("id");
        id.setAccessible(true);
        File record = new File(directory, "-3.1.dat");
        try (DataOutputStream stream = new DataOutputStream(Files.newOutputStream(record.toPath()))) {
            stream.writeInt(0x5a544f53);
            stream.writeInt(revision);
            stream.writeLong(id.getLong(plan));
            stream.writeInt(x);
            stream.writeInt(z);
            stream.writeInt(y);
            if (revision == 4) {
                stream.writeByte(12);
                stream.writeByte(1);
            }
            stream.writeLong(0L);
            stream.writeInt(1);
            stream.writeInt(1);
            stream.writeInt(0);
            stream.writeBoolean(false);
        }
        load(legacy, get(plan, "minX") >> 4, get(plan, "maxX") >> 4, get(plan, "minZ") >> 4, get(plan, "maxZ") >> 4, false);
        Site old = ObservatorySites.nearby(legacy, x, z);
        check(old != null && old.revision() == revision && old.ready(legacy) && old.consumed(0) && old.terminal(0)
                && (revision >= 3 ? old.count() >= 8 && old.count() <= 12 : old.count() >= 16), "historical layout and finite slot allocation remain readable");
        yw player = NaturalSpawnProbe.join(legacy);
        player.c(x + 0.5, y + 3, z + 0.5, 180, 0);
        for (int tick = 0; tick < 1800; tick += 20) {
            legacy.a((long) tick);
            GuardianEncounters.tick(legacy, player);
        }
        check(revision == 1 ? legacy.b(EntityAbyssalGuardian.class) > 0 && legacy.b(EntityAbyssalEnderkin.class) == 0
                : legacy.b(EntityAbyssalEnderkin.class) > 0, "historical site population species");
        CustodianProbe.legacy(legacy, old, player);
        System.out.println("PASS revision-" + revision + " architecture, Custodian and terminal slot compatibility");
    }

    private static void verifyWaterFace(File root) throws Exception {
        xd water = world(root, "ceiling", 32021);
        load(water, 63, 65, 63, 65, false);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++)
            water.d(1024 + dx, 82 + dy, 1024 + dz, 9);
        water.d(1024, 83, 1024, 3036);
        vl renderer = new vl(water);
        adz.a.b();
        check(!renderer.n(pb.B, 1024, 82, 1024), "OptiFine emits no fluid face beneath full Abyssal Bricks");
        water.d(1024, 83, 1024, 1);
        check(renderer.n(pb.B, 1024, 82, 1024), "ordinary stone keeps native liquid rendering");
        water.d(1024, 83, 1024, 0);
        check(renderer.n(pb.B, 1024, 82, 1024), "open water keeps its surface face");
        water.d(1024, 83, 1024, 9);
        int[][] neighbors = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
        int[] architecture = {3044, 3045, 3046, 3050, 3051};
        for (int block : architecture) for (int side = 0; side < neighbors.length; side++) {
            int[] offset = neighbors[side];
            int x = 1024 + offset[0], y = 82 + offset[1], z = 1024 + offset[2];
            water.d(x, y, z, block);
            check(!renderer.n(pb.B, 1024, 82, 1024), "no submerged quad beside architecture " + block + " side " + side);
            check(water.a(1024, 82, 1024) == 9 && water.e(1024, 82, 1024) == 0, "source water is unchanged");
            water.d(x, y, z, 9);
        }
        water.d(1025, 82, 1023, 3046);
        water.d(1025, 82, 1024, 3046);
        water.d(1025, 82, 1025, 3046);
        check(!renderer.n(pb.B, 1024, 82, 1024), "water covering a submerged slab row has no side seam");
        water.d(1025, 83, 1024, 0);
        check(renderer.n(pb.B, 1024, 82, 1024), "water at the exposed end of a slab row keeps its side");
        water.d(1025, 83, 1024, 9);
        water.d(1025, 82, 1023, 9);
        water.d(1025, 82, 1025, 9);
        water.d(1024, 83, 1024, 0);
        check(renderer.n(pb.B, 1024, 82, 1024), "true open surface next to a slab is retained");
        water.d(1024, 83, 1024, 9);
        water.d(1025, 82, 1024, 0);
        check(renderer.n(pb.B, 1024, 82, 1024), "side of an open water column is retained");
        water.d(1025, 82, 1024, 9);
        water.d(1024, 81, 1024, 3046);
        for (int side = 2; side < neighbors.length; side++) {
            int[] offset = neighbors[side];
            water.d(1024 + offset[0], 81, 1024 + offset[2], 3036);
        }
        check(!renderer.n(pb.B, 1024, 82, 1024), "no bottom quad over a flooded landing with dry slab-level neighbors");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++)
            water.d(1030 + dx, 82 + dy, 1024 + dz, 11);
        water.d(1031, 82, 1024, 3046);
        check(renderer.n(pb.D, 1030, 82, 1024), "lava retains its native contact quad");
        System.out.println("PASS pinned OptiFine water faces on six sides of five submerged shapes, masonry, open water and lava");
    }

    private static void verifyLighting(xd world, int minX, int maxX, int minZ, int maxZ, int floor, int revision) {
        int lamps = 0, litWater = 0, seams = 0;
        int[][] sides = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        for (int x = minX * 16; x < (maxX + 1) * 16; x++) for (int z = minZ * 16; z < (maxZ + 1) * 16; z++) {
            for (int y = floor - 2; y <= floor + (revision >= 5 ? 32 : 11); y++) {
                if (world.a(x, y, z) != 3038) continue;
                lamps++;
                check(world.b(wl.b, x, y, z) == 5, "generated lamp retains native block emission");
                for (int[] d : sides) {
                    int wx = x + d[0], wy = y + d[1], wz = z + d[2];
                    if (world.a(wx, wy, wz) != 9) continue;
                    check(world.e(wx, wy, wz) == 0, "lit Observatory water is a native source");
                    check(world.b(wl.b, wx, wy, wz) >= 2, "native lamp reaches adjacent water");
                    litWater++;
                    if (wx >> 4 != x >> 4 || wz >> 4 != z >> 4) seams++;
                }
            }
        }
        check(lamps >= 4 && litWater >= 8, "architectural lighting exists");
        System.out.printf("PASS %d lamps and %d source-water light samples, %d across chunk seams%n", lamps, litWater, seams);
    }

    private static void verifyDivers(File root) throws Exception {
        xd tank = world(root, "divers", 32021);
        load(tank, 61, 67, 61, 67, false);
        fill(tank, 1024, 82, 1024, 8);
        tank.g(1026, 82, 1028, 3038);
        check(tank.b(wl.b, 1026, 82, 1028) == 5 && tank.b(wl.b, 1027, 82, 1028) == 2,
                "manually placed native lamp matches generated emission and attenuation");
        yw swimmer = new yw(tank) { @Override public void ab() {} };
        swimmer.c(1024.5, 84, 1027.5, 0, 0);
        check(tank.a((nn) swimmer), "registered native diver target");
        EntityDiverZombie zombie = new EntityDiverZombie(tank);
        zombie.c(1019.5, 82, 1027.5, 0, 0);
        check(tank.a(zombie), "Diver Zombie inserted");
        int health = swimmer.bb();
        for (int tick = 0; tick < 130 && swimmer.bb() == health; tick++) zombie.J_();
        check(swimmer.bb() == health - 3 && zombie.o > 1022 && zombie.p > 82.1, "Diver Zombie swims in three axes and uses native melee");
        zombie.A();
        tank.n();
        yw archerTarget = new yw(tank) { @Override public void ab() {} };
        archerTarget.c(1029.5, 83, 1024.5, 0, 0);
        swimmer.c(1017.5, 82, 1017.5, 0, 0);
        check(tank.a((nn) archerTarget), "registered native arrow target");
        EntityDiverSkeleton skeleton = new EntityDiverSkeleton(tank);
        skeleton.c(1024.5, 82, 1024.5, 0, 0);
        check(tank.a(skeleton), "Diver Skeleton inserted");
        boolean sawArrow = false, aimed = false;
        for (int tick = 0; tick < 110 && archerTarget.bb() == 20; tick++) {
            skeleton.J_();
            aimed |= skeleton.aiming();
            for (Object entry : new ArrayList<>(tank.b)) if (entry instanceof DiverArrow) {
                DiverArrow arrow = (DiverArrow) entry;
                check(arrow.c == skeleton, "native shooter attribution");
                arrow.J_();
                sawArrow = true;
            }
        }
        check(aimed && sawArrow && archerTarget.bb() < 20, "Diver Skeleton hits through native water drag");
        System.setProperty("zto.dev", "true");
        check(!AquaticDevelopment.disposable(tank), "developer insertion rejected in an ordinary save");
        Files.createFile(new File(root, "divers/.zto-disposable-test").toPath());
        archerTarget.c(1024.5, 83, 1024.5, 0, 0);
        check(AquaticDevelopment.spawn(tank, archerTarget, Species.ENDERKIN).startsWith("Spawned manual"),
                "developer can observe a transitory Enderkin in a disposable save");
        for (Object object : tank.b) if (object instanceof EntityAbyssalEnderkin) {
            EntityAbyssalEnderkin manual = (EntityAbyssalEnderkin) object;
            check(!manual.c(new ady()) && manual.siteIdentity() == 0, "manual actor has neither persistent entity nor site slot");
        }
        System.clearProperty("zto.dev");
        System.out.println("PASS native scuba melee, ranged wind-up, underwater arrow and manual lamp comparison");
    }

    private static void verifyCaster(xd world, Site site) {
        EntityAbyssalEnderkin actor = null;
        for (Object object : world.b) if (object instanceof EntityAbyssalEnderkin) {
            EntityAbyssalEnderkin candidate = (EntityAbyssalEnderkin) object;
            if (candidate.M()) { actor = candidate; break; }
        }
        check(actor != null, "living minion for casting");
        int x = (int) Math.floor(actor.o), y = (int) Math.floor(actor.p), z = (int) Math.floor(actor.q);
        fill(world, x, y + 1, z, 8);
        for (Object object : new ArrayList<>(world.b))
            if (object instanceof EntityAbyssalEnderkin && object != actor) ((nn) object).A();
        actor.c(x + 0.5, y + 1, z + 0.5, 0, 0);
        yw player = new yw(world) { @Override public void ab() {} };
        player.c(x + 0.5, y + 1.8, z + 7.5, 0, 0);
        check(world.a((nn) player), "native pulse target");
        boolean aimed = false, shot = false;
        for (int tick = 0; tick < 100 && player.bb() == 20; tick++) {
            actor.J_();
            aimed |= actor.casting() > 0;
            for (Object object : new ArrayList<>(world.b)) if (object instanceof EntityEnderPulse) {
                EntityEnderPulse pulse = (EntityEnderPulse) object;
                if (pulse.G) continue;
                check(pulse.getShooter() == actor, "pulse keeps caster attribution");
                if (!shot) check(pulse.o < actor.o - 0.15 && pulse.q > actor.q + 0.25, "first pulse originates at the extended right hand");
                pulse.J_();
                shot = true;
            }
        }
        check(aimed && shot && player.bb() == 18, "native submerged casting deals one two-HP hit");
        double startX = actor.o, startY = actor.p, startZ = actor.q;
        for (int tick = 0; tick < 35; tick++) actor.J_();
        check(actor.f(startX, startY, startZ) < 0.25, "caster holds range during recharge instead of circling away");
        double px = player.o, py = player.p, pz = player.q;
        for (int yy = y - 1; yy <= y + 4; yy++) world.g(x, yy, z + 4, 3036);
        EntityEnderPulse blocked = new EntityEnderPulse(actor, x + 0.5, y + 1.2, z + 1.5, px, py, pz);
        for (int i = 0; i < 12 && !blocked.G; i++) blocked.J_();
        check(blocked.G && blocked.q < z + 5 && player.bb() == 18, "masonry intercepts the same pulse");
        System.out.println("PASS right-hand charge and release, attributed 2 HP damage and masonry collision");
    }

    private static void fill(xd world, int centerX, int centerY, int centerZ, int radius) {
        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int y = centerY - 2; y <= centerY + 6; y++) {
                    boolean shell = x == centerX - radius || x == centerX + radius
                            || z == centerZ - radius || z == centerZ + radius
                            || y == centerY - 2 || y == centerY + 6;
                    world.g(x, y, z, shell ? 1 : 9);
                }
            }
        }
    }

    private static void verifyWild(File root, long seed) throws Exception {
        xd world = world(root, "wild", seed);
        TrenchRegion trench = AbyssalWorldgen.locate(world, 0, 0);
        check(trench != null, "wild fixture found a Trench");
        int cx = trench.x >> 4, cz = trench.z >> 4;
        load(world, cx - 3, cx + 3, cz - 3, cz + 3, false);
        check(AbyssalWorldgen.isTrench(world.a(trench.x, trench.z).M)
                && ObservatorySites.nearby(world, trench.x, trench.z) == null, "wild fixture uses deep saved Trench outside ruins");
        yw visitor = NaturalSpawnProbe.join(world);
        visitor.c(trench.x + 0.5, 42, trench.z + 0.5, 0, 0);
        for (int i = 1; i <= 180; i++) {
            world.a(i * 320L);
            GuardianEncounters.tick(world, visitor);
        }
        int wild = 0;
        for (Object entry : world.b) {
            if (!(entry instanceof EntityAbyssalGuardian)) continue;
            EntityAbyssalGuardian actor = (EntityAbyssalGuardian) entry;
            check(!actor.siteOwned() && Math.floor(actor.p) <= 42 && AbyssalWorldgen.isTrench(
                    world.a((int) Math.floor(actor.o), (int) Math.floor(actor.q)).M), "wild actor is in deep Trench");
            wild++;
        }
        int divers = world.b(EntityDiverZombie.class) + world.b(EntityDiverSkeleton.class);
        check(wild > 0 && wild <= 2 && wild + divers <= 4, "shared aquatic cap");
        check(divers > 0 && world.b(EntityAbyssalEnderkin.class) == 0, "only environmental species occur away from ruins");
        System.out.println("PASS wild Trench ecology: " + wild + " Fangfish and " + divers + " divers");
    }

    private static void verifySwimming(File root) throws Exception {
        xd tank = world(root, "swim", 32021);
        load(tank, 63, 65, 63, 65, false);
        for (int z = 1020; z <= 1036; z++) for (int x = 1020; x <= 1036; x++) {
            for (int y = 79; y <= 91; y++) {
                boolean shell = x == 1020 || x == 1036 || z == 1020 || z == 1036 || y == 79 || y == 91;
                tank.g(x, y, z, shell ? 1 : 9);
            }
        }
        yw swimmer = NaturalSpawnProbe.join(tank);
        swimmer.c(1024.5, 83, 1025.5, 0, 0);
        EntityAbyssalGuardian actor = new EntityAbyssalGuardian(tank);
        actor.c(1022.5, 81.08, 1025.5, 0, 0);
        check(tank.a(actor), "native Guardian insertion");
        boolean prepared = false, recovered = false, hit = false;
        int startHealth = swimmer.bb();
        long started = System.nanoTime();
        for (int tick = 0; tick < 170; tick++) {
            actor.J_();
            if (actor.state() == EntityAbyssalGuardian.State.WIND_UP) {
                if (!prepared) check(swimmer.bb() == startHealth, "wind-up preceded the first hit");
                prepared = true;
            }
            if (actor.state() == EntityAbyssalGuardian.State.RECOVER) recovered = true;
            if (swimmer.bb() < startHealth) {
                hit = true;
                break;
            }
        }
        check(prepared && recovered && hit && swimmer.bb() == startHealth - 4,
                "normal survival damage follows wind-up and lunge: " + actor.state() + " / " + swimmer.bb());
        check(actor.p > 81.6 && actor.o > 1023, "native swim progressed horizontally and vertically");
        System.out.printf(Locale.ROOT, "PASS native 3D movement and 4 HP normal-difficulty lunge, %.1f ms%n",
                (System.nanoTime() - started) / 1e6);
        actor.A();
        for (int z = 1021; z <= 1031; z++) for (int y = 80; y <= 90; y++) tank.g(1028, y, z, 1);
        swimmer.c(1026.5, 82, 1029.5, 0, 0);
        EntityAbyssalGuardian navigator = new EntityAbyssalGuardian(tank);
        navigator.c(1022.5, 82.08, 1029.5, 0, 0);
        check(tank.a(navigator), "native navigator insertion");
        for (int tick = 0; tick < 24 && navigator.state() != EntityAbyssalGuardian.State.ALERT; tick++) navigator.J_();
        check(navigator.state() == EntityAbyssalGuardian.State.ALERT, "guard notices a player in open water");
        swimmer.c(1031.5, 83, 1029.5, 0, 0);
        boolean reachedOtherSide = false;
        for (int tick = 0; tick < 160; tick++) {
            navigator.J_();
            if (navigator.o > 1029.2) {
                reachedOtherSide = true;
                break;
            }
        }
        check(reachedOtherSide, "bounded navigation found a route around a solid partition: "
                + navigator.state() + " at " + navigator.o + "," + navigator.p + "," + navigator.q);
        for (int y = 83; y <= 86; y++) tank.g(1031, y, 1029, 0);
        navigator.J_();
        check(navigator.state() == EntityAbyssalGuardian.State.RETURN, "dry player breaks pursuit");
        System.out.println("PASS solid partition detour and release at a dry refuge");
    }

    private static void defendOld(File root, long seed, int minX, int minZ, int maxX, int maxZ, int x, int z)
            throws Exception {
        int bx = (maxX << 4) + 8, bz = (maxZ << 4) + 8;
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(null);
        xd writer = world(root, "old", seed);
        writer.g(bx, 48, bz, 54);
        hb chest = (hb) writer.b(bx, 48, bz);
        chest.a(0, new aan(264, 3, 0));
        writer.z().a(true, null);
        ce.a.a();
        agp.a();
        Field field = AbyssalWorldgen.class.getDeclaredField("CALLBACK");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.function.BiFunction<Object[], int[], Object> callback =
                (java.util.function.BiFunction<Object[], int[], Object>) field.get(null);
        zed.tekkitoverhaul.agent.ZTOAgent.registerTrench(callback);
        xd old = world(root, "old", seed);
        old.d(minX, minZ);
        check(!old.z().a(maxX, maxZ), "whole-footprint header test did not load the saved corner");
        load(old, minX, maxX, minZ, maxZ, false);
        check(ObservatorySites.nearby(old, x, z) == null, "one existing corner vetoes whole structure");
        check(old.a(bx, 48, bz) == 54 && ((hb) old.b(bx, 48, bz)).k_(0).a == 3, "old chest and contents preserved");
    }

    private static void verifyBurial(xd world, Site site, Object plan, int floor, File previews, int index) throws Exception {
        Method position = method(plan.getClass(), "position", int.class, int.class, int.class);
        Method roof = method(plan.getClass(), "coverRoof", int.class, int.class);
        int width = get(plan, "width"), length = get(plan, "length");
        BufferedImage top = new BufferedImage(2 * width + 1, length + 6, BufferedImage.TYPE_INT_RGB);
        BufferedImage interior = new BufferedImage(top.getWidth(), top.getHeight(), BufferedImage.TYPE_INT_RGB);
        BufferedImage section = new BufferedImage(length + 6, 54, BufferedImage.TYPE_INT_RGB);
        int covered = 0, qualifying = 0, lamps = 0, details = 0;
        for (int v = -5; v <= length; v++) for (int u = -width; u <= width; u++) {
            int[] at = (int[]) position.invoke(plan, u, 0, v);
            int tx = at[0], tz = at[2], surface = 52;
            while (surface > floor && (wet(world.a(tx, surface, tz)) || world.a(tx, surface, tz) == 0)) surface--;
            int cover = (Integer) roof.invoke(plan, u, v);
            int exposed = world.a(tx, surface,tz);
            top.setRGB(u+width,v+5,color(exposed));
            interior.setRGB(u+width,v+5,color(world.a(tx,floor+3,tz)));
            if (cover >= 0) {
                qualifying++;
                if (surface >= floor + cover + 3 && !architecture(exposed)) covered++;
            }
            for (int y = floor - 1; y <= Math.min(53, floor + (site.revision() >= 5 ? 32 : 30)); y++) {
                int block = world.a(tx,y,tz);
                if (block == 3038 && y <= floor + 12) lamps++;
                if (block >= 3044 && block <= 3051) details++;
                if (u == 0) section.setRGB(v+5,53-y,color(block));
            }
        }
        check(qualifying > 500 && covered >= qualifying * 0.8, "native natural roof cover " + covered + "/" + qualifying);
        check(lamps >= 6 && details >= 24, "native contextual lamps and architectural blocks " + lamps + "/" + details);
        Field frozen = Site.class.getDeclaredField("plan");
        frozen.setAccessible(true);
        Object portal = frozen.get(site);
        int sill = get(portal, "sill");
        int[] mouth = (int[]) position.invoke(plan, 0, 0, -2);
        mouth[1] = floor + sill + 1;
        int profile = site.revision() == 5 ? get(portal, "profile") : -1;
        String entrance = profile < 0 ? (flag(portal, "recessed") ? "recessed" : "terraced")
                : new String[] {"frames", "cleft", "terrace"}[profile];
        System.out.printf("Portal %s approach=%d,%d,%d%n", entrance, mouth[0], mouth[1], mouth[2]);
        int jamb = profile < 0 ? get(portal, "jamb") : profile == 0 ? 4 : profile == 1 ? 3 : 5;
        for (int u = 1 - jamb; u < jamb; u++) {
            int[] opening = (int[]) position.invoke(portal, u, 1, 0);
            for (int rise = 0; rise < 4; rise++)
                check(wet(world.a(opening[0], opening[1] + sill + rise, opening[2])), "clear portal aperture");
        }
        if (profile >= 0) verifyRib(world, portal, position, sill);
        int[] center = site.arenaCenter();
        check(wet(world.a(mouth[0],mouth[1],mouth[2])), "flooded entrance shaft");
        check(swimmable(world, mouth, center, null), "native entrance to chamber");
        check(swimmable(world, mouth, center, site.arenaEntry(false)), "native alternative path around blocked inner approach");
        check(previews.isDirectory() || previews.mkdirs(), "preview directory");
        scene(world, position, portal, previews, index, "arrival", 8, sill+5, -17, 0, sill+3, 1);
        scene(world, position, portal, previews, index, "portal", 1, sill+3, -8, 0, sill+3, 4);
        scene(world, position, portal, previews, index, "threshold", 0, sill+2, site.revision() == 5 ? 1 : -2,
                0, sill-1, 8);
        if (index == 0) {
            scene(world, position, portal, previews, index, "descent", 0, 6, 10, 0, 4, 20);
            scene(world, position, portal, previews, index, "cistern", 2, 5, 22, -4, 2, 30);
            scene(world, position, portal, previews, index, "vault", 3, 4, length-15, 0, 5, length+1);
        }
        writeMap(top, previews, "buried-" + index + "-seabed");
        writeMap(interior, previews, "buried-" + index + "-interior");
        writeMap(section, previews, "buried-" + index + "-section");
        BufferedImage front = new BufferedImage(17, 17, BufferedImage.TYPE_INT_RGB);
        BufferedImage threshold = new BufferedImage(17, 17, BufferedImage.TYPE_INT_RGB);
        for (int u = -8; u <= 8; u++) for (int rise = -5; rise <= 11; rise++) {
            int[] face = (int[]) position.invoke(plan, u, 0, 0);
            int[] inside = (int[]) position.invoke(plan, u, 0, 3);
            int height = floor + sill + rise;
            front.setRGB(u + 8, 11 - rise, color(world.a(face[0], height, face[2])));
            threshold.setRGB(u + 8, 11 - rise, color(world.a(inside[0], height, inside[2])));
        }
        writeMap(front, previews, "buried-" + index + "-portal-front");
        writeMap(threshold, previews, "buried-" + index + "-vestibule-section");
        exterior(world, position, plan, floor + sill, 8, 10,
                new File(previews, "buried-" + index + "-portal-isometric.png"));
        BufferedImage arena = new BufferedImage(21, 13, BufferedImage.TYPE_INT_RGB);
        for (int u = -10; u <= 10; u++) for (int h = -1; h <= 11; h++) {
            int[] at = (int[]) position.invoke(plan, u, 0, length - 9);
            arena.setRGB(u + 10, 11 - h, color(world.a(at[0], floor + h, at[2])));
        }
        writeMap(arena, previews, "buried-" + index + "-arena-section");
        diverView(world, position, plan, floor, sill + 2, -5, 1, length, previews, index, "approach");
        diverView(world, position, plan, floor, sill + 2, 3, -1, length, previews, index, "threshold");
        if (index == 0) {
            diverView(world, position, plan, floor, 7, 7, 1, length, previews, index, "gallery");
            diverView(world, position, plan, floor, 5, 20, 1, length, previews, index, "hall");
            diverView(world, position, plan, floor, 4, length - 15, 1, length, previews, index, "chamber");
        }
        System.out.printf("PASS native burial %d/%d covered, %d lamps, %d details%n",
                covered, qualifying, lamps, details);
    }

    private static void verifyRib(xd world, Object plan, Method position, int sill) throws Exception {
        int span = 21, levels = 16;
        int[][] blocks = new int[span][levels];
        boolean[][] supported = new boolean[span][levels];
        ArrayDeque<int[]> route = new ArrayDeque<>();
        for (int u = 0; u < span; u++) for (int h = 0; h < levels; h++) {
            int[] at = (int[]) position.invoke(plan, u - span / 2, sill + h - 4, 0);
            blocks[u][h] = world.a(at[0], at[1], at[2]);
            if (h == 0 && blocks[u][h] != 0 && !wet(blocks[u][h])) {
                supported[u][h] = true;
                route.add(new int[] {u,h});
            }
        }
        int[][] step = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!route.isEmpty()) {
            int[] cell = route.removeFirst();
            for (int[] delta : step) {
                int u = cell[0] + delta[0], h = cell[1] + delta[1];
                if (u < 0 || u >= span || h < 0 || h >= levels || supported[u][h]
                        || blocks[u][h] == 0 || wet(blocks[u][h])) continue;
                supported[u][h] = true;
                route.add(new int[] {u,h});
            }
        }
        int courses = 0;
        for (int u = 0; u < span; u++) for (int h = 0; h < levels; h++) if (blocks[u][h] == 3049) {
            check(supported[u][h], "stepped portal frieze shares a face-connected course with its footing");
            courses++;
        }
        check(courses >= 5, "native portal has a continuous frieze");
    }

    private static void scene(xd world, Method position, Object plan, File previews, int index, String name,
                              int u, int h, int v, int focusU, int focusH, int focusV) throws Exception {
        int[] camera = (int[]) position.invoke(plan, u, h, v);
        int[] target = (int[]) position.invoke(plan, focusU, focusH, focusV);
        AbyssalSceneSnapshot.write(world::d, new File(previews, "site-" + index + "-" + name + ".zvox"), camera, target);
    }

    /** Bounded perspective diagnostic using generated block IDs, with a fixed water attenuation. */
    private static void diverView(xd world, Method position, Object plan, int floor, int cameraY, int cameraV,
                                  int direction, int length, File previews, int index, String name) throws Exception {
        int minV = Math.max(-5, cameraV - (direction < 0 ? 16 : 0));
        int maxV = Math.min(length, cameraV + (direction > 0 ? 24 : 0));
        int[][][] blocks = new int[maxV - minV + 1][21][31];
        for (int v = minV; v <= maxV; v++) for (int u = -10; u <= 10; u++) {
            int[] at = (int[]) position.invoke(plan, u, 0, v);
            for (int h = 0; h <= 30; h++)
                blocks[v - minV][u + 10][h] = world.a(at[0], floor + h, at[2]);
        }
        BufferedImage image = new BufferedImage(240, 150, BufferedImage.TYPE_INT_RGB);
        int waterColor = 0x173843;
        for (int row = 0; row < image.getHeight(); row++) for (int col = 0; col < image.getWidth(); col++) {
            double dx = direction * (col - 119.5) / 174;
            double dy = (74.5 - row) / 174;
            int pixel = waterColor;
            for (double distance = 0.25; distance <= 24; distance += 0.25) {
                int v = (int) Math.floor(cameraV + 0.5 + direction * distance);
                int u = (int) Math.floor(0.5 + dx * distance);
                int h = (int) Math.floor(cameraY + 0.5 + dy * distance);
                if (v < minV || v > maxV || u < -10 || u > 10 || h < 0 || h > 30) break;
                int id = blocks[v - minV][u + 10][h];
                if (id == 0 || wet(id)) continue;
                int surface = color(id);
                double amount = 1 / (1 + distance * 0.09);
                int r = (int) ((surface >> 16) * amount + (waterColor >> 16) * (1 - amount));
                int g = (int) ((surface >> 8 & 255) * amount + (waterColor >> 8 & 255) * (1 - amount));
                int b = (int) ((surface & 255) * amount + (waterColor & 255) * (1 - amount));
                pixel = r << 16 | g << 8 | b;
                break;
            }
            image.setRGB(col, row, pixel);
        }
        ImageIO.write(image, "png", new File(previews, "buried-" + index + "-" + name + "-perspective.png"));
    }

    private static void writeMap(BufferedImage map, File target, String name) throws Exception {
        BufferedImage enlarged = new BufferedImage(map.getWidth()*6, map.getHeight()*6, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = enlarged.createGraphics();
        graphics.drawImage(map, 0, 0, enlarged.getWidth(), enlarged.getHeight(), null);
        graphics.dispose();
        ImageIO.write(enlarged, "png", new File(target, name + ".png"));
    }

    private static boolean swimmable(xd world, int[] start, int[] goal, int[] blocked) {
        ArrayDeque<int[]> frontier = new ArrayDeque<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        frontier.add(start);
        int[][] moves = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!frontier.isEmpty()) {
            int[] p = frontier.remove();
            if (Arrays.equals(p, goal)) return true;
            for (int[] d : moves) {
                int x=p[0]+d[0], y=p[1]+d[1], z=p[2]+d[2];
                if (Math.abs(x-start[0]) > 66 || Math.abs(z-start[2]) > 66 || y < goal[1]-2 || y > start[1]+1) continue;
                if (blocked != null && Math.abs(x-blocked[0]) <= 2 && Math.abs(z-blocked[2]) <= 2) continue;
                if (!world.z().a(x >> 4,z >> 4) || !wet(world.a(x,y,z)) || !wet(world.a(x,y+1,z))) continue;
                if (seen.add(x + "/" + y + "/" + z)) frontier.add(new int[] {x,y,z});
            }
        }
        return false;
    }

    private static boolean architecture(int block) { return block >= 3036 && block <= 3038 || block >= 3044 && block <= 3051; }
    private static int color(int block) {
        return wet(block) ? 0x17586e : block == 3038 ? 0xb5cda2 : architecture(block) ? 0x88978d
                : block == 0 ? 0x12222a : 0x46534b;
    }

    private static void verifyArchitecture(xd world, Site site, Object plan, int floor, File previews, int index) throws Exception {
        Method position = method(plan.getClass(), "position", int.class, int.class, int.class);
        int[] entrance = (int[]) position.invoke(plan, 0, 2, -2);
        int[] center = site.arenaCenter();
        entrance[1] += floor;
        int[] gate = site.arenaEntry(false);
        check(previews.isDirectory() || previews.mkdirs(), "preview directory");
        int half = get(plan, "width");
        int length = get(plan, "length");
        BufferedImage layout = new BufferedImage((half * 2 + 1) * 5, (length + 6) * 5, BufferedImage.TYPE_INT_RGB);
        BufferedImage section = new BufferedImage((length + 6) * 5, 20 * 5, BufferedImage.TYPE_INT_RGB);
        for (int v = -5; v <= length; v++) {
            for (int u = -half; u <= half; u++) {
                int[] p = (int[]) position.invoke(plan, u, 0, v);
                tile(layout, (half + u) * 5, (v + 5) * 5, world.a(p[0], floor + 2, p[2]));
            }
            int[] p = (int[]) position.invoke(plan, 0, 0, v);
            for (int h = -7; h <= 12; h++) tile(section, (v + 5) * 5, (12 - h) * 5, world.a(p[0], floor + h, p[2]));
        }
        ImageIO.write(layout, "png", new File(previews, "site-" + index + "-plan.png"));
        ImageIO.write(section, "png", new File(previews, "site-" + index + "-section.png"));
        BufferedImage arena = new BufferedImage(105, 100, BufferedImage.TYPE_INT_RGB);
        for (int u = -10; u <= 10; u++) {
            int[] p = (int[]) position.invoke(plan, u, 0, length - 9);
            for (int h = -7; h <= 12; h++) tile(arena, (u + 10) * 5, (12 - h) * 5, world.a(p[0], floor + h, p[2]));
        }
        ImageIO.write(arena, "png", new File(previews, "site-" + index + "-arena-section.png"));
        exterior(world, position, plan, floor, half, length, new File(previews, "site-" + index + "-exterior.png"));
        if (!traversable(world, plan, entrance, center, null)) {
            for (int h = 1; h <= 6; h++) {
                StringBuilder row = new StringBuilder();
                for (int v = -5; v <= length; v++) {
                    int[] p = (int[]) position.invoke(plan, 0, h, v);
                    row.append(wet(world.a(p[0], p[1] + floor, p[2])) ? '.' : '#');
                }
                System.out.println("Centerline y+" + h + ": " + row);
            }
            throw new AssertionError("native water route from entrance to future arena");
        }
        check(traversable(world, plan, entrance, center, gate), "side annex routes around sealed main arena gate");
    }

    private static void exterior(xd world, Method position, Object plan, int floor, int half, int length, File output)
            throws Exception {
        BufferedImage image = new BufferedImage(610, 320, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D draw = image.createGraphics();
        draw.setColor(new java.awt.Color(16, 35, 44));
        draw.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int depth = half + length; depth >= -half - 5; depth--) {
            for (int v = length; v >= -5; v--) {
                int u = depth - v;
                if (u < -half || u > half) continue;
                int[] p = (int[]) position.invoke(plan, u, 0, v);
                for (int h = -7; h <= 11; h++) {
                    int id = world.a(p[0], floor + h, p[2]);
                    if (id == 0 || wet(id)) continue;
                    int x = 435 + (u - v) * 5, y = 222 - depth * 2 - h * 5;
                    int color = id == 3049 ? 0x929681 : id == 3047 ? 0x687c79 : id == 3048 ? 0x87908c
                            : id == 3037 || id == 3045 ? 0x555d52 : color(id);
                    draw.setColor(new java.awt.Color(color).darker());
                    draw.fillPolygon(new int[] {x - 5, x, x, x - 5}, new int[] {y, y + 2, y + 7, y + 5}, 4);
                    draw.setColor(new java.awt.Color(color));
                    draw.fillPolygon(new int[] {x, x + 5, x + 5, x}, new int[] {y + 2, y, y + 5, y + 7}, 4);
                    draw.setColor(new java.awt.Color(color).brighter());
                    draw.fillPolygon(new int[] {x - 5, x, x + 5, x}, new int[] {y, y - 2, y, y + 2}, 4);
                }
            }
        }
        draw.dispose();
        ImageIO.write(image, "png", output);
    }

    private static boolean traversable(xd world, Object plan, int[] from, int[] to, int[] gate) throws Exception {
        int minX = get(plan, "minX"), maxX = get(plan, "maxX");
        int minZ = get(plan, "minZ"), maxZ = get(plan, "maxZ");
        int width = maxX - minX + 1, depth = maxZ - minZ + 1;
        int bottom = from[1] - 1;
        boolean[] seen = new boolean[width * depth * 6];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(from);
        int[][] directions = {{1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}, {0,1,0}, {0,-1,0}};
        int rotation = get(plan, "rotation");
        while (!open.isEmpty()) {
            int[] p = open.removeFirst();
            if (Arrays.equals(p, to)) return true;
            for (int[] d : directions) {
                int x = p[0] + d[0], y = p[1] + d[1], z = p[2] + d[2];
                if (x < minX || x > maxX || z < minZ || z > maxZ || y < bottom || y >= bottom + 6) continue;
                if (gate != null && ((rotation & 1) == 0 ? z == gate[2] && Math.abs(x - gate[0]) <= 3
                        : x == gate[0] && Math.abs(z - gate[2]) <= 3)) continue;
                int i = x - minX + (z - minZ + (y - bottom) * depth) * width;
                if (seen[i] || !wet(world.a(x, y, z)) || !wet(world.a(x, y + 1, z))) continue;
                seen[i] = true;
                open.addLast(new int[] {x,y,z});
            }
        }
        return false;
    }

    private static void verifyActors(xd world, Site site, int x, int y, int z) {
        yw player = NaturalSpawnProbe.join(world);
        int[] center = site.arenaCenter();
        player.c(center[0] + 0.5, center[1] + 1, center[2] + 0.5, 0, 0);
        int lastAdmission = -160, previous = 0;
        for (int attempt = 0; attempt < 90; attempt++) {
            int[] at = attempt % 3 == 0 ? site.arenaEntry(false) : attempt % 3 == 1 ? site.home(2) : center;
            player.c(at[0] + 0.5, at[1] + 1, at[2] + 0.5, attempt % 2 == 0 ? 0 : 180, 0);
            int tick = attempt * 20;
            world.a(tick);
            GuardianEncounters.tick(world, player);
            int now = world.b(EntityAbyssalEnderkin.class);
            if (now != previous) {
                check(now == previous + 1 && tick - lastAdmission >= 160, "finite admission pacing");
                lastAdmission = tick;
                previous = now;
            }
        }
        int admitted = world.b(EntityAbyssalEnderkin.class);
        check(world.b(EntityAbyssalGuardian.class) == 0, "revised site admits only Enderkin");
        check(admitted >= 2 && admitted <= site.count(), "bounded local admission: " + admitted);
        EntityAbyssalEnderkin killed = null;
        for (Object entry : world.b) {
            if (!(entry instanceof EntityAbyssalEnderkin)) continue;
            EntityAbyssalEnderkin actor = (EntityAbyssalEnderkin) entry;
            if (actor.siteIdentity() == site.identity()) {
                killed = actor;
                break;
            }
        }
        check(killed != null && killed.siteIdentity() == site.identity() && killed.bb() == 12, "registered site actor health and identity");
        String before = site.status();
        killed.a(md.a(player), 100);
        check(!site.status().equals(before) && site.status().contains("1 dead"), "native death recorded immediately");
        for (int attempt = 0; attempt < 90; attempt++) {
            world.a(attempt * 20L);
            GuardianEncounters.tick(world, player);
        }
        check(world.b(EntityAbyssalEnderkin.class) <= admitted, "death consumed slot after revisits");
        player.c(x + 500, y + 10, z + 500, 0, 0);
        System.out.println("Local finite population: " + site.status());
    }

    private static void load(xd world, int minX, int maxX, int minZ, int maxZ, boolean reverse) {
        for (int z = minZ - 1; z <= maxZ + 1; z++) for (int x = minX - 1; x <= maxX + 1; x++)
            world.d(reverse ? minX + maxX - x : x, reverse ? minZ + maxZ - z : z);
    }

    private static boolean wet(int block) { return block == 8 || block == 9; }

    private static void tile(BufferedImage image, int x, int y, int block) {
        int color = wet(block) ? 0x1c657c : block == 0 ? 0x101a22
                : block == 3036 ? 0x777c6f : block == 3037 ? 0x515747 : block == 3038 ? 0xbbdbb8 : 0x30343b;
        for (int dz = 0; dz < 5; dz++) for (int dx = 0; dx < 5; dx++) image.setRGB(x + dx, y + dz, color);
    }

    private static xd world(File root, String name, long seed) throws Exception { return world(root, name, seed, 0); }

    private static xd world(File root, String name, long seed, int gameType) throws Exception {
        // Model a new client session. The vanilla stronghold search walks the pooled array tail.
        for (Field field : ad.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType() == int.class) field.setInt(null, 256);
            else if (List.class.isAssignableFrom(field.getType())) ((List<?>) field.get(null)).clear();
        }
        xd world = new xd(new aia(root, name, false), name, new fj(seed, gameType, true, false, vx.b)) {
            @Override public void a(nn entity, String sound, float volume, float pitch) {}
            @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        };
        world.q = 2;
        return world;
    }

    private static int get(Object object, String name) throws Exception {
        Field field = typeWithField(object.getClass(), name).getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(object);
    }

    private static boolean flag(Object object, String name) throws Exception {
        Field field = typeWithField(object.getClass(), name).getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(object);
    }

    private static Class<?> typeWithField(Class<?> type, String name) throws Exception {
        try { type.getDeclaredField(name); return type; }
        catch (NoSuchFieldException missing) {
            if (type.getSuperclass() == null) throw missing;
            return typeWithField(type.getSuperclass(), name);
        }
    }

    private static Method method(Class<?> owner, String name, Class<?>... types) throws Exception {
        try {
            Method method = owner.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException missing) {
            if (owner.getSuperclass() == null) throw missing;
            return method(owner.getSuperclass(), name, types);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
