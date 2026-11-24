import ic2.api.Items;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.exploration.motorboat.ContainerMotorboat;
import zed.tekkitoverhaul.exploration.motorboat.EntityMotorboat;
import zed.tekkitoverhaul.exploration.motorboat.MotorboatRegistry;
import zed.tekkitoverhaul.exploration.motorboat.client.MotorboatClient;
import zed.tekkitoverhaul.mod_ZedsTekkitOverhaul;
import zed.tekkitoverhaul.survival.aerial.client.AerialSoundRegistration;

/** One headless lake in the pinned pack, using the production-obfuscated item, entity and container. */
public final class MotorboatProbe {
    private static final String FUEL = "ZTO_MotorboatFuel";
    private static final String METER = "ZTO_MotorboatTicks";
    private static final int EMPTY = 325;
    private static final int BOAT = 333;
    private static final int OUTBOARD = 22271;
    private static final int MOTORBOAT = 22272;
    private static final int X = 291, Y = 12, Z = 291;

    public static void main(String[] args) {
        try {
            run(new File(args[0]));
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File root) throws Exception {
        UnderwaterEquipmentProbe.Fixture world = UnderwaterEquipmentProbe.startFixture(root, "motorboat");
        Minecraft client = ModLoader.getMinecraftInstance();
        BuildCraftEnergy.initialize();
        ZtoConfig config = ZtoConfigLoader.load(new File(root, "config").toPath(), false, Logger.getLogger("motorboat"));
        DistillationRegistry.register(config, new mod_ZedsTekkitOverhaul());
        MotorboatRegistry.register(config);
        MotorboatClient.register(client);
        check("ZTO.Motorboat".equals(aao.a(240)), "stable Motorboat entity ID");
        aan iron = Items.getItem("refinedIronIngot");
        aan gear = new aan(BuildCraftCore.ironGearItem);
        check(iron != null && iron.i() == 0, "native Refined Iron metadata");
        check(recipe(iron, gear, iron, iron, new aan(BuildCraftEnergy.engineBlock, 1, 2), iron, null, gear, null).c == OUTBOARD,
                "native Outboard recipe");
        aan wrongEngine = recipe(iron, gear, iron, iron, new aan(BuildCraftEnergy.engineBlock, 1, 1), iron, null, gear, null);
        check(wrongEngine == null || wrongEngine.c != OUTBOARD, "Steam Engine is rejected");
        aan item = recipe(new aan(BOAT, 1, 0), null, null, new aan(OUTBOARD, 1, 0));
        check(item != null && item.c == MOTORBOAT && item.d == null && item.c() == 1, "crafting gives an empty Motorboat");
        System.out.println("PASS native shaped recipes, registered IDs and exact engine metadata");

        for (int x = X - 3; x <= X + 22; x++) {
            for (int z = Z - 3; z <= Z + 41; z++) {
                for (int y = Y - 2; y <= Y; y++) world.b(x, y, z, 9, 0);
            }
        }
        vq player = player(client, world);
        aimAtLake(player, X, Z);
        item.a().a(item, world, player);
        EntityMotorboat boat = findBoat(world);
        check(boat != null && item.a == 0 && boat.fuel() == 0 && aao.a(boat) == 240, "native water placement");
        aimAtLake(player, X, Z);
        aan blocked = new aan(MOTORBOAT, 1, 0);
        blocked.a().a(blocked, world, player);
        check(blocked.a == 1, "overlapping placement keeps the item");

        player.a.e = true;
        boat.c(player);
        check(client.s instanceof gb && ((gb) client.s).d instanceof ContainerMotorboat && player.j == null,
                "sneak interaction opens native fueling without mounting");
        ContainerMotorboat panel = (ContainerMotorboat) ((gb) client.s).d;
        int gasoline = bucket(DistillateType.GASOLINE);
        for (int bad : new int[] {BuildCraftEnergy.bucketFuel.bQ, bucket(DistillateType.DIESEL),
                bucket(DistillateType.KEROSENE), 327}) {
            aan wrong = new aan(bad, 1, 0);
            player.ap.b(wrong);
            panel.a(0, 0, false, player);
            check(player.ap.k() == wrong && panel.b(0).b() == null, "rejects foreign fuel " + bad);
        }
        player.ap.b((aan) null);
        player.ap.a[9] = new aan(gasoline, 1, 0);
        check(panel.a(2, 0, true, player) != null && player.ap.a[9] == null, "Shift-click inserts gasoline bucket");
        boat.J_();
        check(boat.fuel() == 1000 && panel.b(0).b() == null && panel.b(1).b().c == EMPTY, "conserves one bucket per fill");
        player.ap.b(new aan(gasoline, 1, 0));
        panel.a(0, 0, false, player);
        boat.J_();
        check(boat.fuel() == 1000 && panel.b(0).b().c == gasoline, "occupied return defers filling");
        check(panel.a(1, 0, true, player) != null && panel.b(1).b() == null, "Shift-click returns the empty bucket");
        panel.a(player);
        client.s = null;
        boat.J_();
        check(boat.fuel() == 2000 && panel.b(1).b().c == EMPTY, "closed screen leaves each slot under entity ownership");
        player.a.e = false;
        boat.c(player);
        check(player.j == boat && boat.i == player, "normal interaction mounts the boat");
        for (int i = 0; i < 24; i++) boat.J_();
        check(boat.fuel() == 2000 && world.motorSounds == 0, "occupied idle is silent and costs no gasoline");

        ep vanilla = new ep(world, X + 18.5, Y + 1, Z + 0.5);
        yw vanillaRider = NaturalSpawnProbe.join(world);
        vanilla.c(vanillaRider);
        player.a.b = 1;
        double accelerated = 0;
        double legacy = 0;
        for (int i = 0; i < 20; i++) {
            double before = boat.q;
            boat.J_();
            accelerated = boat.q - before;
            before = vanilla.q;
            vanillaRider.t = 1;
            vanilla.J_();
            legacy = vanilla.q - before;
        }
        check(Math.abs(accelerated - 1D) < 0.0001 && Math.abs(legacy - 0.4D) < 0.0001,
                "steady speed, motor " + accelerated + ", vanilla " + legacy);
        check(boat.fuel() == 1990 && world.motorSounds == 3, "one mB per two powered ticks and bounded sound");
        player.a.a = 1;
        float heading = boat.u;
        boat.J_();
        check(Math.abs(boat.u - heading + 7F) < 0.001 && Math.hypot(boat.r, boat.t) <= 1.0001,
                "steering changes heading within the turn and speed caps");
        player.a.a = player.a.b = 0;
        int fuelAtRelease = boat.fuel();
        double speed = Math.hypot(boat.r, boat.t);
        int sounds = world.motorSounds;
        boat.J_();
        check(!boat.isPropelling() && Math.hypot(boat.r, boat.t) < speed
                && boat.fuel() == fuelAtRelease && world.motorSounds == sounds, "coasting is silent and unmetered");
        boat.i_();
        boat.c(player);
        check(player.j == null, "vanilla dismount releases the rider");
        System.out.printf("PASS native steering and mount/dismount, %.2f vs %.2f blocks/tick, exact burn and coast%n",
                accelerated, legacy);

        player.c(boat.o, boat.p + 1, boat.q, 0, 0);
        player.ap.b(new aan(gasoline, 1, 0));
        panel.a(0, 0, false, player);
        world.a(boat, false); // Reindex the chunk after direct, deterministic entity ticks.
        world.a(true, (rw) null);
        ce.a.a();
        agp.a();
        UnderwaterEquipmentProbe.Fixture reopened = new UnderwaterEquipmentProbe.Fixture(root, "motorboat");
        EntityMotorboat restored = findBoat(reopened);
        check(restored != null && restored.fuel() == fuelAtRelease, "fuel survives disk save and chunk reopen");
        player = player(client, reopened);
        player.c(restored.o, restored.p + 1, restored.q, 0, 0);
        panel = new ContainerMotorboat(player.ap, restored);
        check(panel.b(0).b().c == gasoline && panel.b(1).b().c == EMPTY, "both fuel slots survive a disk save");
        ady saved = new ady();
        check(restored.c(saved) && saved.f(METER) == 1, "half-mB meter survives save and reload");
        saved.a(FUEL, 3500);
        EntityMotorboat nearlyFull = (EntityMotorboat) aao.a(saved, reopened);
        ContainerMotorboat nearlyFullPanel = new ContainerMotorboat(player.ap, nearlyFull);
        nearlyFullPanel.a(1, 0, true, player);
        nearlyFull.J_();
        check(nearlyFull.fuel() == 3500 && nearlyFullPanel.b(0).b().c == gasoline,
                "less than one bucket of free space leaves the gasoline intact");
        ady reserve = new ady();
        for (String key : new String[] {"Pos", "Motion", "Rotation"}) reserve.a(key, saved.n(key));
        reserve.a("id", "ZTO.Motorboat");
        reserve.a(FUEL, 1);
        reserve.a(METER, 1);
        EntityMotorboat ending = (EntityMotorboat) aao.a(reserve, reopened);
        ending.c(X + 8.5, restored.p, Z + 0.5, -90, 0);
        player.c(ending.o, ending.p + 1, ending.q, 0, 0);
        ending.c(player);
        player.a.b = 1;
        for (int i = 0; i < 40 && ending.fuel() > 0; i++) ending.J_();
        check(ending.fuel() == 0 && ending.isPropelling(), "final powered tick exhausts the last mB");
        sounds = reopened.motorSounds;
        for (int i = 0; i < 100; i++) ending.J_();
        check(!ending.isPropelling() && Math.hypot(ending.r, ending.t) < 0.003 && ending.p >= Y + 0.2
                && reopened.motorSounds == sounds, "empty boat coasts to a float with no powered sound");
        player.h(ending);

        restored.a(md.a(player), 6);
        restored.a(md.a(player), 6);
        check(restored.G && !panel.b(player) && panel.a(0, 0, true, player) == null, "dismantle invalidates the old container");
        aan recovered = null;
        int drops = 0;
        for (Object entity : reopened.b) {
            if (!(entity instanceof fq)) continue;
            aan drop = ((fq) entity).a;
            if (drop.c == MOTORBOAT) recovered = drop;
            if (drop.c == MOTORBOAT || drop.c == gasoline || drop.c == EMPTY) drops++;
        }
        check(drops == 3 && recovered != null && recovered.d.f(FUEL) == fuelAtRelease,
                "one fueled item and both buckets drop exactly once");
        player.aT.d = true;
        aimAtLake(player, X + 4, Z);
        recovered.a().a(recovered, reopened, player);
        EntityMotorboat replaced = findBoat(reopened);
        check(recovered.a == 0 && replaced != null && replaced.fuel() == fuelAtRelease,
                "creative placement also consumes the fueled item and restores the reserve once");
        for (int x = X + 1; x <= X + 7; x++) {
            for (int y = Y; y <= Y + 3; y++) reopened.b(x, y, Z + 5, 1, 0);
        }
        replaced.c(player);
        player.a.b = 1;
        for (int i = 0; i < 40; i++) replaced.J_();
        int blockedFuel = replaced.fuel();
        for (int i = 0; i < 20; i++) replaced.J_();
        check(!replaced.G && replaced.q < Z + 5 && replaced.fuel() == blockedFuel
                && !replaced.isPropelling(), "solid shoreline stops the motor without breaking or burning fuel at rest");
        System.out.println("PASS bucket conservation, capacity, disk reload, metered exhaustion and fueled recovery");
        verifyArtwork();
        verifySound();
    }

    private static int bucket(DistillateType type) { return DistillationRegistry.bucketFor(type.id).bQ; }

    private static aan recipe(aan... input) {
        ade grid = new ade(new dd() {
            @Override public boolean b(yw player) { return true; }
        }, 3, 3);
        for (int i = 0; i < input.length; i++) grid.a(i, input[i]);
        return fr.a().a(grid);
    }

    private static vq player(Minecraft client, xd world) {
        client.f = world;
        vq player = new vq(client, world, new fc("MotorboatFixture", ""), 0);
        client.h = player;
        client.i = player;
        player.a = new oy();
        return player;
    }

    private static void aimAtLake(yw player, int x, int z) {
        player.c(x + 0.5, Y + 3, z - 1.1, 0, 68);
    }

    private static EntityMotorboat findBoat(xd world) {
        for (Object entry : world.b) {
            if (entry instanceof EntityMotorboat && !((EntityMotorboat) entry).G) return (EntityMotorboat) entry;
        }
        return null;
    }

    private static void verifyArtwork() throws Exception {
        Map<Class<? extends nn>, um> renderers = new HashMap<>();
        MotorboatClient.registerRenderer(renderers);
        um renderer = renderers.get(EntityMotorboat.class);
        Field modelField = ea.class.getDeclaredField("a");
        modelField.setAccessible(true);
        wy model = (wy) modelField.get(renderer);
        Field motor = model.getClass().getDeclaredField("motor");
        motor.setAccessible(true);
        qp[] parts = (qp[]) motor.get(model);
        lq clamp = (lq) parts[2].l.get(0);
        lq propeller = (lq) parts[5].l.get(0);
        check(model.a.length == 5 && model.a[2].c == 11F && clamp.a == 10F && clamp.d == 15F
                && propeller.a == 18F && propeller.e == 9F, "native hull and stern-mounted submerged model coordinates");
        Class<?> artwork = Class.forName("zed.tekkitoverhaul.exploration.motorboat.client.MotorboatArtwork");
        Method icon = artwork.getDeclaredMethod("motorboatIcon");
        icon.setAccessible(true);
        BufferedImage actual = (BufferedImage) icon.invoke(null);
        BufferedImage original = ImageIO.read(artwork.getResource("/gui/items.png"));
        check(actual.getRGB(5, 7) == original.getRGB(133, 135)
                && actual.getRGB(12, 2) != original.getRGB(140, 130), "production inventory icon retains the native wood sprite");
        Method engine = artwork.getDeclaredMethod("engine");
        engine.setAccessible(true);
        BufferedImage texture = (BufferedImage) engine.invoke(null);
        check(texture.getRGB(6, 0) == 0xFFD2D2D2 && texture.getRGB(7, 1) == 0xFFB2BEC1,
                "installed BuildCraft and IC2 metallic surface samples");
        System.out.println("PASS packaged sprite composition, native hull model and stern motor bounds");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void verifySound() throws Exception {
        Field handlerField = AerialSoundRegistration.class.getDeclaredField("HANDLER");
        handlerField.setAccessible(true);
        AerialSoundRegistration handler = (AerialSoundRegistration) handlerField.get(null);
        sd manager = new sd();
        handler.onLoadSoundSettings(manager);
        ub cue = manager.getSoundsPool().a("zto.motorboat.running");
        check(cue != null, "running cue is registered in the native sound pool");
        try (AudioInputStream clip = AudioSystem.getAudioInputStream(cue.b)) {
            check(clip.getFormat().getChannels() == 1 && clip.getFormat().getSampleRate() == 22050
                    && clip.getFrameLength() < 8800, "short positional mono outboard cue");
        }
        System.out.println("PASS native engine sound registration and packaged mono cue");
    }
}
