import java.awt.image.BufferedImage;
import java.io.File;
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
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalGuardian;
import zed.tekkitoverhaul.exploration.abyssal.guardian.GuardianEncounters;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.AbyssalWorldgen;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.TrenchRegion;

/** Uses the reobfuscated candidate, pinned provider, installed native entity classes and Anvil. */
public final class ObservatoryProbe {
    public static void main(String[] args) {
        try {
            run(new File(args[0]), new File(args[1]));
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File root, File previews) throws Exception {
        UnderwaterEquipmentProbe.startFixture(root, "setup");
        ZtoConfig config = ZtoConfigLoader.load(new File(root, "config").toPath(), false, Logger.getLogger("observatory"));
        AbyssalRegistry.register(config, 100);
        AbyssalWorldgen.register(config.abyssalId("biome.abyssalTrench"));
        ObservatorySites.register();
        GuardianEncounters.register();
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
        verifySwimming(root);
        int completed = 0;
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
            System.out.printf("Native site seed=%d entrance=%d,%d,%d id=%x allocated=%d generation=%.0f ms%n",
                    seed, x, y, z, site.identity(), site.count(), (System.nanoTime() - generation) / 1e6);
            verifyArchitecture(world, site, plan, y, previews, completed);
            if (completed == 0) {
                defendOld(root, seed, minX, minZ, maxX, maxZ, x, z);
                verifyActors(world, site, x, y, z);
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
                check(reopened.b(EntityAbyssalGuardian.class) > 0, "living population reopened from native entity NBT");
                check(saved.status().equals(site.status()), "population ledger survived save and reopen");
                reopened.q = 0;
                for (Object entry : new ArrayList<>(reopened.b)) {
                    if (entry instanceof EntityAbyssalGuardian) ((EntityAbyssalGuardian) entry).J_();
                }
                check(!saved.status().endsWith("| 0 retired"), "Peaceful retires native living site actors");
                verifyWild(root, seed);
            }
            if (++completed == 3) break;
        }
        check(completed == 3, "three complete native Observatory candidates");
        System.out.println("PASS native Observatory placement, alternatives, stamps, old terrain and finite Guardian persistence");
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
        check(wild > 0 && wild <= 2, "wild encounters use the local rarity cap: " + wild);
        System.out.println("PASS wild encounters in deep saved Trench, capped at " + wild);
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
        check(prepared && recovered && hit && swimmer.bb() == startHealth - 3,
                "normal survival damage follows wind-up and lunge: " + actor.state() + " / " + swimmer.bb());
        check(actor.p > 81.6 && actor.o > 1023, "native swim progressed horizontally and vertically");
        System.out.printf(Locale.ROOT, "PASS native 3D movement and 3 HP normal-difficulty lunge, %.1f ms%n",
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

    private static void verifyArchitecture(xd world, Site site, Object plan, int floor, File previews, int index) throws Exception {
        Method position = method(plan.getClass(), "position", int.class, int.class, int.class);
        int[] entrance = (int[]) position.invoke(plan, 0, 2, -2);
        int[] center = (int[]) position.invoke(plan, 0, 3, get(plan, "length") - 9);
        entrance[1] += floor;
        center[1] += floor;
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
        BufferedImage image = new BufferedImage(550, 310, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D draw = image.createGraphics();
        draw.setColor(new java.awt.Color(16, 35, 44));
        draw.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int depth = half + 5; depth >= -half - length; depth--) {
            for (int v = length; v >= -5; v--) {
                int u = depth + v;
                if (u < -half || u > half) continue;
                int[] p = (int[]) position.invoke(plan, u, 0, v);
                for (int h = -7; h <= 11; h++) {
                    int id = world.a(p[0], floor + h, p[2]);
                    if (id < 3032 || id > 3038) continue;
                    int x = 170 + (u + v) * 5, y = 125 + (u - v) * 2 - h * 5;
                    int color = id == 3036 ? 0x687771 : id == 3037 ? 0x50594b : id == 3038 ? 0xb3d0ad
                            : id == 3033 ? 0x54584a : id == 3034 ? 0x64746b : 0x415957;
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
        player.c(x + 0.5, y + 3, z + 0.5, 0, 0);
        for (int attempt = 0; attempt < 90; attempt++) {
            world.a(attempt * 20L);
            GuardianEncounters.tick(world, player);
        }
        int admitted = world.b(EntityAbyssalGuardian.class);
        check(admitted >= 4 && admitted <= site.count(), "bounded local admission: " + admitted);
        EntityAbyssalGuardian killed = null;
        for (Object entry : world.a(EntityAbyssalGuardian.class, wu.b(x - 30, y - 10, z - 30, x + 30, y + 14, z + 30))) {
            EntityAbyssalGuardian actor = (EntityAbyssalGuardian) entry;
            if (actor.siteOwned()) {
                killed = actor;
                break;
            }
        }
        check(killed != null && killed.siteIdentity() == site.identity() && killed.bb() == 16, "registered site actor health and identity");
        String before = site.status();
        killed.a(md.a(player), 100);
        check(!site.status().equals(before) && site.status().contains("1 dead"), "native death recorded immediately");
        for (int attempt = 0; attempt < 90; attempt++) {
            world.a(attempt * 20L);
            GuardianEncounters.tick(world, player);
        }
        check(world.b(EntityAbyssalGuardian.class) <= admitted, "death consumed slot after revisits");
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

    private static xd world(File root, String name, long seed) throws Exception {
        // Model a new client session. The vanilla stronghold search walks the pooled array tail.
        for (Field field : ad.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType() == int.class) field.setInt(null, 256);
            else if (List.class.isAssignableFrom(field.getType())) ((List<?>) field.get(null)).clear();
        }
        xd world = new xd(new aia(root, name, false), name, new fj(seed, 0, true, false, vx.b)) {
            @Override public void a(nn entity, String sound, float volume, float pitch) {}
            @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        };
        world.q = 2;
        return world;
    }

    private static int get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(object);
    }

    private static Method method(Class<?> owner, String name, Class<?>... types) throws Exception {
        Method method = owner.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
