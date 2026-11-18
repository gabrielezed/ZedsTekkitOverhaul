import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.imageio.ImageIO;
import paulscode.sound.SoundBuffer;
import paulscode.sound.codecs.CodecWav;
import zed.tekkitoverhaul.survival.aerial.AerialSounds;
import zed.tekkitoverhaul.survival.aerial.client.AerialSoundRegistration;
import zed.tekkitoverhaul.survival.aerial.AerialPredators;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator.Flight;
import zed.tekkitoverhaul.survival.aerial.EntitySkelebird;
import zed.tekkitoverhaul.survival.aerial.client.RenderAerialPredator;

/** Runs the reobfuscated predator in a disposable flat world with the installed Tekkit classes. */
public final class AerialPredatorProbe {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        final List<String> sounds = new ArrayList<String>();
        xd world = new xd(new aia(root, "aerial-predator", false), "aerial-predator", NaturalSpawnProbe.SETTINGS) {
            @Override public void a(nn entity, String sound, float volume, float pitch) {
                if (entity instanceof EntityAerialPredator && sound != null) sounds.add(sound);
            }
            @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
        };
        java.nio.file.Files.createFile(new File(root, "aerial-predator/.zto-disposable").toPath());
        world.q = 2;
        world.a(18000);
        world.q();
        world.r.setSeed(3865L);
        for (int x = 14; x <= 22; x++) {
            for (int z = 14; z <= 22; z++) world.d(x, z);
        }
        yw player = NaturalSpawnProbe.join(world);
        player.aA = "Pilot";
        registryAndEgg(world, player);
        verifySoundPool();

        player.c(288.5, 20, 288.5, 0, 0);
        EntityAerialPredator hunter = place(world, 299, 28, 288.5, 0);
        boundsAndWindup(world, hunter, player);
        check(sounds.indexOf(AerialSounds.WARNING) >= 0
                && sounds.indexOf(AerialSounds.DIVE) > sounds.indexOf(AerialSounds.WARNING),
                "distinct warning and dive cues are emitted in order");
        ady saved = new ady();
        check(hunter.c(saved), "named entity saves through vanilla");
        EntityAerialPredator reloaded = (EntityAerialPredator) aao.a(saved, world);
        check(reloaded != null && reloaded.bb() == hunter.bb() && reloaded.flight() == Flight.CRUISE
                && "-".equals(reloaded.targetName()) && Math.abs(reloaded.o - hunter.o) < 0.001,
                "vanilla reload restores identity, health and position and clears a committed hunt");

        int health = player.bb();
        waitFor(world, hunter, Flight.RECOVER, 45);
        check(player.bb() < health && Math.abs(player.s) < 0.001, "committed contact damages without lifting the pilot");
        double low = hunter.y.b;
        for (int i = 0; i < 36; i++) {
            world.a(hunter, true);
            check(hunter.flight() == Flight.RECOVER, "holds recovery interval");
        }
        check(hunter.y.b > low + 3, "recovery climbs");
        hunter.A();

        player.c(288.5, 20, 288.5, 0, 0);
        player.r = player.s = player.t = 0;
        EntityAerialPredator evaded = place(world, 299, 28, 288.5, 0);
        waitFor(world, evaded, Flight.DIVE, 225);
        double targetX = evaded.destinationX(), targetZ = evaded.destinationZ();
        player.c(288.5, 20, 294.5, 0, 0);
        health = player.bb();
        for (int i = 0; i < 40 && evaded.flight() == Flight.DIVE; i++) {
            check(evaded.destinationX() == targetX && evaded.destinationZ() == targetZ,
                    "commitment keeps its original endpoint");
            world.a(evaded, true);
        }
        check(evaded.flight() == Flight.RECOVER && player.bb() == health && "dive missed".equals(evaded.lastFailure()),
                "a lateral dodge defeats the fixed trajectory");
        for (int x = 287; x <= 290; x++) {
            for (int z = 293; z <= 296; z++) world.g(x, 25, z, 1);
        }
        waitFor(world, evaded, Flight.DISENGAGE, 10);
        check(evaded.lastExit().contains("cover") && player.bb() == health, "roof breaks the hunt during recovery");
        evaded.A();

        approaches(world, player);
        retryBlockedWindup(world, player);
        groundAndTerrain(world, player);
        EntityAerialPredator falling = place(world, 292, 24, 288.5, 0);
        EntityAerialPredator wounded = place(world, 295, 25, 288.5, 0);
        check(wounded.a(md.a(player), 4) && sounds.contains(AerialSounds.HURT), "hurt emits a species cue");
        wounded.A();
        check(falling.a(md.a(player), 20) && falling.flight() == Flight.FALL, "ordinary player damage kills the predator");
        check(sounds.contains(AerialSounds.DEATH), "death emits a species cue");
        int priorAmbient = Collections.frequency(sounds, AerialSounds.AMBIENT);
        EntityAerialPredator idle = place(world, 290, 17, 289, 0);
        for (int tick = 0; tick < 1000; tick++) {
            if (tick % 80 == 0) idle.c(290, 17, 289, 0, 0);
            world.a(idle, true);
        }
        int ambient = Collections.frequency(sounds, AerialSounds.AMBIENT) - priorAmbient;
        check(ambient >= 1 && ambient <= 4, "ambient calls have a multi-second cadence: " + ambient);
        idle.A();
        double deathY = falling.p;
        for (int i = 0; i < 12; i++) world.a(falling, true);
        check(falling.p < deathY - 1, "a dead predator loses lift (vertical displacement " + (falling.p - deathY) + ")");
        check(world.a(reloaded), "reload can be inserted");
        world.q = 0;
        world.a(reloaded, true);
        check(reloaded.G, "Peaceful removes the predator");
        skelebirdCombat(world, player, sounds);
        System.out.println("PASS Fugl naming, legacy egg and save IDs, dive pose, atlas, flight, ground and airborne acquisition, "
                + "eight approach bearings, blocked warning retry, impact, dodge, climb, shelter, death fall, NBT and native WAV audio");
    }

    private static void registryAndEgg(xd world, yw player) throws Exception {
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        AerialPredators.registerEgg(22009);
        AerialPredators.registerSkelebirdEgg(22010);
        yr egg = yr.e[22265];
        aan stack = new aan(egg);
        Properties names = new Properties();
        FMLCommonHandler.instance().handleLanguageLoad(names, "en_US");
        check(names.containsValue("Fugl Spawn Egg") && egg.c(), "localized two-pass egg");
        check("Fugl".equals(names.getProperty("entity.ZTO.AerialPredator.name")), "Fugl has a localized display name");
        player.aT.d = true;
        check(egg.a(stack, player, world, 290, 3, 288, 1) && stack.a == 1, "Creative egg spawns without spending");
        check(!egg.a(stack, player, world, 290, 3, 288, 1), "egg rejects occupied space");
        EntityAerialPredator spawned = (EntityAerialPredator) world.a(EntityAerialPredator.class,
                wu.b(286, 4, 286, 294, 8, 292)).get(0);
        check("ZTO.AerialPredator".equals(aao.b(spawned)), "legacy entity save identifier");
        for (int tick = 0; tick < 32; tick++) world.a(spawned, true);
        check(spawned.y.b > 7, "egg hatch climbs from ground level");
        spawned.A();
        yr skeletalEgg = yr.e[22266];
        aan skeletalStack = new aan(skeletalEgg);
        ArrayList<aan> creative = new ArrayList<aan>();
        skeletalEgg.addCreativeItems(creative);
        check(creative.size() == 1 && creative.get(0).c == 22266, "native Creative enumeration includes the egg");
        check(names.containsValue("Skelebird Spawn Egg") && skeletalEgg.c()
                && skeletalEgg.a(skeletalStack, player, world, 296, 3, 288, 1) && skeletalStack.a == 1,
                "Skelebird has its own localized layered Creative egg");
        EntitySkelebird skeleton = (EntitySkelebird) world.a(EntitySkelebird.class,
                wu.b(295, 4, 287, 298, 8, 290)).get(0);
        check("ZTO.Skelebird".equals(aao.b(skeleton)) && skeleton.bb() == 14,
                "Skelebird has its own save name and lower health");
        skeleton.A();
        player.aT.d = false;

        Map<Class<? extends nn>, um> renderers = new HashMap<Class<? extends nn>, um>();
        RenderAerialPredator.register(renderers);
        check(renderers.get(EntityAerialPredator.class) instanceof RenderAerialPredator, "native renderer mapping");
        check(renderers.get(EntitySkelebird.class) instanceof RenderAerialPredator, "Skelebird renderer mapping");
        verifyDivePose();
        verifySkelebirdPose();
        BufferedImage texture = ImageIO.read(EntityAerialPredator.class.getResource(EntityAerialPredator.TEXTURE));
        check(texture.getWidth() == 64 && texture.getHeight() == 64
                && texture.getRGB(17, 5) != texture.getRGB(10, 40), "readable model atlas");
        BufferedImage bones = ImageIO.read(EntitySkelebird.class.getResource(EntitySkelebird.TEXTURE));
        check(bones.getWidth() == 64 && bones.getHeight() == 64 && bones.getRGB(5, 54) == 0xFF494949
                && bones.getRGB(63, 63) == 0, "Skelebird has its own low-resolution skull and atlas");
    }

    private static void verifyDivePose() throws Exception {
        Method pitch = RenderAerialPredator.class.getDeclaredMethod("modelPitch", float.class);
        pitch.setAccessible(true);
        double diveAngle = Math.toRadians((Float) pitch.invoke(null, 43F));
        double climbAngle = Math.toRadians((Float) pitch.invoke(null, -20F));
        // After model scaling, the beak points along -Z. Its Y component is sin(pitch).
        check(Math.sin(diveAngle) < -0.65 && Math.sin(climbAngle) > 0.32, "beak follows descending and climbing pitch");
        Class<?> modelClass = Class.forName("zed.tekkitoverhaul.survival.aerial.client.ModelAerialPredator");
        java.lang.reflect.Constructor<?> constructor = modelClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object model = constructor.newInstance();
        Method pose = modelClass.getDeclaredMethod("pose", Flight.class, float.class, int.class);
        pose.setAccessible(true);
        Field wings = modelClass.getDeclaredField("wings");
        wings.setAccessible(true);
        qp[] wing = (qp[]) wings.get(model);
        pose.invoke(model, Flight.DIVE, 0F, 0);
        float folded = wing[1].g;
        check(folded < -0.9F && wing[0].g == -folded, "symmetric backward wing sweep during the dive");
        pose.invoke(model, Flight.RECOVER, 0F, 3);
        check(wing[1].g > folded && wing[1].g < -0.3F, "wing unfolds progressively during recovery");
    }

    private static void verifySoundPool() {
        sd manager = new sd();
        AerialSoundRegistration sounds = new AerialSoundRegistration();
        sounds.onLoadSoundSettings(manager);
        sounds.onLoadSoundSettings(manager);
        check(manager.getSoundsPool().a == 15, "sound registration is idempotent per pool");
        for (String key : new String[] {AerialSounds.AMBIENT, AerialSounds.WARNING, AerialSounds.DIVE,
                AerialSounds.HURT, AerialSounds.DEATH, "zto.jetpack.warning", "zto.jetpack.fault", "zto.jetpack.recovery",
                "zto.skelebird.ambient", "zto.skelebird.warning", "zto.skelebird.slash",
                "zto.skelebird.hurt", "zto.skelebird.death"}) {
            ub clip = manager.getSoundsPool().a(key);
            check(clip != null, "native sound pool resolves " + key);
            CodecWav codec = new CodecWav();
            check(codec.initialize(clip.b), "pinned WAV decoder opens " + key);
            SoundBuffer buffer = codec.readAll();
            check(buffer != null && buffer.audioFormat.getChannels() == 1
                    && buffer.audioFormat.getSampleRate() == 22050 && buffer.audioData.length > 8000,
                    "decodes a positional PCM clip for " + key);
            codec.cleanup();
        }
        sd recreated = new sd();
        sounds.onLoadSoundSettings(recreated);
        check(recreated.getSoundsPool().a == 15, "a recreated sound manager gets the same sound set");
    }

    private static void verifySkelebirdPose() throws Exception {
        Class<?> modelClass = Class.forName("zed.tekkitoverhaul.survival.aerial.client.ModelSkelebird");
        java.lang.reflect.Constructor<?> constructor = modelClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object model = constructor.newInstance();
        Method pose = modelClass.getDeclaredMethod("pose", Flight.class, float.class, int.class);
        pose.setAccessible(true);
        Field shoulders = modelClass.getDeclaredField("shoulders");
        shoulders.setAccessible(true);
        qp[] wing = (qp[]) shoulders.get(model);
        pose.invoke(model, Flight.PREPARE, 0F, 0);
        check(wing[1].h < -0.8F && wing[0].h > 0.8F, "warning raises the wing bones");
        pose.invoke(model, Flight.DIVE, 0F, 0);
        float sweep = wing[1].g;
        check(sweep < -0.6F && Math.abs(wing[1].h) < 0.1F, "crossing sweeps wings back in a flat pose");
        pose.invoke(model, Flight.RECOVER, 0F, 8);
        check(wing[1].g > sweep, "recovery reopens the wings");
    }

    private static void skelebirdCombat(xd world, yw player, List<String> sounds) throws Exception {
        world.q = 2;
        world.a(18000);
        player.aT.a = false;
        player.c(288.5, 26, 288.5, 0, 0);
        player.z = false;
        player.l(20);
        player.Y = 0;
        player.r = player.s = player.t = 0;
        EntitySkelebird hunter = new EntitySkelebird(world);
        place(world, hunter, 272, 32, 288.5, -90);
        waitFor(world, hunter, Flight.PREPARE, 220);
        int health = player.bb();
        for (int i = 0; i < 23; i++) {
            world.a(hunter, true);
            check(hunter.flight() == Flight.PREPARE && player.bb() == health, "slashing warning allows reaction time");
        }
        waitFor(world, hunter, Flight.DIVE, 2);
        check(sounds.indexOf("zto.skelebird.warning") >= 0
                && sounds.indexOf("zto.skelebird.slash") > sounds.indexOf("zto.skelebird.warning"),
                "own warning and rush play in order");
        check(hunter.s < 0 && -hunter.s < 0.5 * Math.hypot(hunter.r, hunter.t), "shallow diagonal flight");
        double vx = hunter.r, vz = hunter.t;
        ady tag = new ady();
        check(hunter.c(tag), "Skelebird saves through vanilla");
        EntitySkelebird restored = (EntitySkelebird) aao.a(tag, world);
        check(restored.flight() == Flight.CRUISE && "-".equals(restored.targetName())
                && restored.bb() == hunter.bb(), "Skelebird reload clears the transient attack");
        waitFor(world, hunter, Flight.RECOVER, 45);
        double crossing = ((hunter.o - player.o) * vx + (hunter.q - player.q) * vz) / Math.hypot(vx, vz);
        check(player.bb() == health - 5 && crossing > 2, "one swept contact and a continued crossing, health "
                + health + " -> " + player.bb() + ", runout " + crossing + ", " + hunter.lastFailure());
        double low = hunter.p;
        for (int i = 0; i < 53; i++) {
            world.a(hunter, true);
            check(hunter.flight() == Flight.RECOVER, "Skelebird holds its recovery interval");
        }
        check(hunter.p > low + 3, "Skelebird climbs away");
        hunter.A();

        EntitySkelebird dodged = new EntitySkelebird(world);
        player.r = player.s = player.t = 0;
        player.Y = 0;
        place(world, dodged, 272, 32, 288.5, -90);
        waitFor(world, dodged, Flight.PREPARE, 220);
        player.c(288.5, 26, 280.5, 0, 0);
        health = player.bb();
        waitFor(world, dodged, Flight.DIVE, 25);
        check(Math.abs(dodged.destinationZ() - 288.5) < 0.01, "warning locks the pass before launch");
        waitFor(world, dodged, Flight.RECOVER, 40);
        check(player.bb() == health && "slashing pass missed".equals(dodged.lastFailure()), "a warning-time dodge works");
        world.g(288, 30, 280, 20);
        waitFor(world, dodged, Flight.DISENGAGE, 2);
        check(dodged.lastExit().contains("cover"), "glass overhead protects the pilot");
        check(dodged.a(md.a(new nm(world), player), 3) && sounds.contains("zto.skelebird.hurt"),
                "Skelebird takes arrow damage and emits its hurt cue");
        dodged.A();

        player.c(288.5, 4, 288.5, 0, 0);
        player.z = true;
        EntitySkelebird ground = new EntitySkelebird(world);
        place(world, ground, 272, 12, 288.5, -90);
        seed(ground, new Random(27L) {
            @Override public int nextInt(int bound) { return bound == 24 ? 0 : super.nextInt(bound); }
        });
        for (int i = 0; i < 100; i++) {
            world.a(ground, true);
            check("-".equals(ground.targetName()), "manual Normal birds ignore grounded players");
        }
        world.q = 3;
        ground.c(272, 12, 288.5, -90, 0);
        waitFor(world, ground, Flight.DIVE, 230);
        waitFor(world, ground, Flight.DISENGAGE, 110);
        check(ground.diveCommitments() == 1 && "attack budget".equals(ground.lastExit()), "Hard ground hunt spends one turn");
        ground.c(272, 24, 288.5, -90, 0);
        ground.r = ground.s = ground.t = 0;
        seed(ground, new Random(37L) {
            @Override public int nextInt(int bound) { return bound == 3 ? 2 : super.nextInt(bound); }
        });
        int bones = countBones(world);
        check(ground.a(md.a(player), 20) && ground.flight() == Flight.FALL
                && sounds.contains("zto.skelebird.death"), "ordinary damage kills Skelebird and emits its death cue");
        check(countBones(world) == bones + 2, "native player kill drops the vanilla bone roll");
        double deathY = ground.p;
        for (int i = 0; i < 15; ++i) world.a(ground, true);
        check(ground.p < deathY - 1, "Skelebird loses lift on death");
        check(!ground.a(md.a(player), 20) && countBones(world) == bones + 2, "death cannot emit bones twice");
        System.out.println("PASS Skelebird registry, egg, native WAV, shallow pass, warning-time evade, contact, ground limits, cover, recovery, bone drops and NBT");
    }

    private static int countBones(xd world) {
        int count = 0;
        for (Object entity : world.b) {
            if (entity instanceof fq) {
                aan stack = ((fq) entity).a;
                if (stack.c == yr.aX.bQ) count += stack.a;
            }
        }
        return count;
    }

    private static void approaches(xd world, yw player) throws Exception {
        for (int index = 0; index < 8; index++) {
            boolean ground = index < 4;
            world.a(index % 2 == 0 ? 6000 : 18000);
            world.q();
            double y = ground ? 4 : 22;
            player.c(288.5, y, 288.5, 0, 0);
            player.z = ground;
            double angle = index * Math.PI / 4;
            EntityAerialPredator mob = place(world, 288.5 + Math.cos(angle) * 13,
                    y + 6 + index % 3 * 4, 288.5 + Math.sin(angle) * 13, 47 * index);
            seed(mob, new Random(index + 123L) {
                @Override public int nextInt(int bound) { return bound == 9 || bound == 42 ? 0 : super.nextInt(bound); }
            });
            waitFor(world, mob, Flight.DIVE, 220);
            check(mob.diveCommitments() == 1 && mob.rejectedWarnings() == 0,
                    "open approach commits from bearing " + index);
            mob.A();
        }
        world.a(18000);
        world.q();
    }

    private static void retryBlockedWindup(xd world, yw player) throws Exception {
        player.c(288.5, 20, 288.5, 0, 0);
        player.z = false;
        EntityAerialPredator mob = place(world, 302, 29, 288.5, 0);
        waitFor(world, mob, Flight.PREPARE, 210);
        int bx = (int) Math.floor((mob.o + player.o) * 0.5);
        int by = (int) Math.floor((mob.p + player.y.b + 1) * 0.5);
        int bz = (int) Math.floor((mob.q + player.q) * 0.5);
        for (int x = bx - 1; x <= bx + 1; x++)
            for (int y = by - 2; y <= by + 2; y++)
                for (int z = bz - 1; z <= bz + 1; z++) world.g(x, y, z, 1);
        waitFor(world, mob, Flight.RECOVER, 23);
        check(mob.diveCommitments() == 0 && mob.rejectedWarnings() == 1
                && mob.lastDiveRejection().contains("corridor"), "new obstacle cancels the launch without spending a dive");
        for (int x = bx - 1; x <= bx + 1; x++)
            for (int y = by - 2; y <= by + 2; y++)
                for (int z = bz - 1; z <= bz + 1; z++) world.g(x, y, z, 0);
        waitFor(world, mob, Flight.DIVE, 235);
        check(mob.diveCommitments() == 1, "the hunt restages after a rejected warning");
        mob.A();
    }

    private static void groundAndTerrain(xd world, yw player) throws Exception {
        player.c(288.5, 4, 288.5, 0, 0);
        player.z = true;
        EntityAerialPredator ground = place(world, 299, 16, 288.5, 0);
        // Force the low-probability night roll without altering detection or the attack state.
        seed(ground, new Random(17L) {
            @Override public int nextInt(int bound) { return bound == 9 ? 0 : super.nextInt(bound); }
        });
        waitFor(world, ground, Flight.PREPARE, 220);
        waitFor(world, ground, Flight.DISENGAGE, 110);
        check("attack budget".equals(ground.lastExit()), "ground hunt spends one attack opportunity");
        ground.A();

        player.aT.a = true;
        for (int z = 275; z <= 305; z++) {
            for (int y = 4; y < 21; y++) world.g(305, y, z, y >= 17 ? 18 : 1);
        }
        EntityAerialPredator flyer = place(world, 303.4, 15, 288.5, -90);
        double start = flyer.q;
        double previousYaw = flyer.u;
        int contacts = 0;
        for (int tick = 0; tick < 100; tick++) {
            world.a(flyer, true);
            if (flyer.A) contacts++;
            double turn = Math.IEEEremainder(flyer.u - previousYaw, 360);
            check(Math.abs(turn) <= 11.1, "steering bounds each turn");
            previousYaw = flyer.u;
        }
        check(contacts <= 2 && Math.abs(start - flyer.q) > 4, "cliff and canopy do not trap or repeatedly clip the flyer");
        flyer.A();
    }

    private static void boundsAndWindup(xd world, EntityAerialPredator mob, yw player) {
        int health = player.bb();
        waitFor(world, mob, Flight.PREPARE, 220);
        for (int i = 0; i < 17; i++) {
            world.a(mob, true);
            check(mob.flight() == Flight.PREPARE && player.bb() == health, "full preparation precedes damage");
        }
        waitFor(world, mob, Flight.DIVE, 2);
        check(player.bb() == health && mob.s < 0, "dive begins with downward movement and no remote damage");
    }

    private static EntityAerialPredator place(xd world, double x, double y, double z, float yaw) throws Exception {
        EntityAerialPredator mob = new EntityAerialPredator(world);
        place(world, mob, x, y, z, yaw);
        return mob;
    }

    private static void place(xd world, EntityAerialPredator mob, double x, double y, double z, float yaw) throws Exception {
        seed(mob, new Random(3865L));
        mob.c(x, y, z, yaw, 0);
        check(world.a(mob), "loaded fixture spawn");
    }

    private static void seed(EntityAerialPredator mob, Random seed) throws Exception {
        Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(mob, seed);
    }

    private static void waitFor(xd world, EntityAerialPredator mob, Flight phase, int ticks) {
        for (int i = 0; i < ticks && mob.flight() != phase; i++) world.a(mob, true);
        check(mob.flight() == phase, "expected " + phase + ", saw " + mob.flight()
                + " [" + mob.lastFailure() + ", " + mob.lastExit() + "]");
    }

    private static void check(boolean expected, String description) {
        if (!expected) throw new AssertionError(description);
    }
}
