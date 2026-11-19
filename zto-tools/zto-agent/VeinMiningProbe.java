import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.TickType;
import eloraam.world.BlockCustomOre;
import eloraam.world.ItemCustomOre;
import ic2.common.BlockTex;
import ic2.common.ElectricItem;
import ic2.common.Ic2Items;
import ic2.common.ItemElectricToolDDrill;
import ic2.common.ItemElectricToolDrill;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.survival.mining.VeinMining;

/** Exercises the pinned controller, vanilla food and the actual IC2 charge implementation. */
public final class VeinMiningProbe {
    private static final int X = 312, Y = 4, Z = 289;
    private static final int COAL = 16, STONE_PICK = 274;
    private static final int[][] SEAM = {{0,0},{1,0},{2,0},{2,1},{1,1},{0,1}};
    private final Fixture world;
    private final Minecraft game;
    private final aes controller;
    private final vq player;
    private final VeinMining mining;
    private final afu modifier;
    private final afu attack;

    public static void main(String[] args) {
        try {
            new VeinMiningProbe(new File(args[0])).run();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private VeinMiningProbe(File root) throws Exception {
        world = new Fixture(root);
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja failure) { throw new AssertionError(failure); }
        };
        game.f = world;
        game.R = true;
        controller = new aes(game);
        game.c = controller;
        player = new vq(game, world, new fc("VeinProbe", ""), 0) {
            @Override public void a(ajw statistic, int amount) {}
        };
        game.h = player;
        player.c(X - 1.0, Y, Z + 0.5, 0, 0);
        player.z = true;
        game.A = (hu) allocate(hu.class);
        game.A.w = attack = new afu("Attack", -100);
        field(FMLClientHandler.class, "client").set(FMLClientHandler.instance(), game);
        RedPowerWorld.blockOres = new BlockCustomOre(140);
        new ItemCustomOre(140 - 256);
        Ic2Items.copperOre = new aan(new BlockTex(249, 32, acn.e).c(3.0F));
        mining = new VeinMining();
        modifier = (afu) field(VeinMining.class, "modifier").get(mining);
        Method attach = VeinMining.class.getDeclaredMethod("attachAgent");
        attach.setAccessible(true);
        check(Boolean.TRUE.equals(attach.invoke(mining)), "register actual startup agent");
    }

    private void run() {
        ItemElectricToolDrill drill = new ItemElectricToolDrill(22001, 0);
        ItemElectricToolDDrill diamond = new ItemElectricToolDDrill(22002, 0);
        drill.init();
        diamond.init();
        Result ordinary = measure("ordinary stone", new aan(STONE_PICK, 1, 0), false, 0);
        Result manual = measure("vein stone", new aan(STONE_PICK, 1, 0), true, 0);
        Result saturated = measure("vein stone with saturation", new aan(STONE_PICK, 1, 0), true, 2);
        Result miningDrill = measure("mining drill", powered(drill, 1000), true, 0);
        Result diamondDrill = measure("diamond drill", powered(diamond, 1000), true, 0);
        check(ordinary.cost == 6 && manual.cost == 6 && ordinary.food == 20 && manual.food == 19
                && saturated.food == 20 && saturated.saturation == 1, "native wear and exhaustion reach the expected food and saturation thresholds");
        check(miningDrill.cost == 6 * drill.operationEnergyCost && diamondDrill.cost == 6 * diamond.operationEnergyCost
                && miningDrill.food == 20 && diamondDrill.food == 20 && miningDrill.ticks < manual.ticks
                && diamondDrill.ticks < miningDrill.ticks, "native EU accounting and faster drills");

        begin(new aan(STONE_PICK, 1, 0), 20, 0, true);
        placeSeam();
        ordinaryBlock(X, Z);
        attack.e = false;
        for (int i = 0; i < 80; i++) tick();
        check(remaining() == 5, "release stops the pending action");

        begin(new aan(STONE_PICK, 1, 0), 5, 0, true);
        placeSeam();
        ordinaryBlock(X, Z);
        for (int i = 0; i < 80; i++) tick();
        check(remaining() == 5, "low food permits the ordinary starter only");

        begin(powered(drill, drill.operationEnergyCost + 1), 20, 0, true);
        placeSeam();
        ordinaryBlock(X, Z);
        for (int i = 0; i < 80; i++) tick();
        check(remaining() == 5 && charge(player.av()) == 1, "drill stops when it cannot pay for another block");

        begin(new aan(STONE_PICK, 1, 0), 20, 0, true);
        placeSeam();
        ordinaryBlock(X, Z);
        player.ap.a(0, new aan(STONE_PICK, 1, 0));
        for (int i = 0; i < 80; i++) tick();
        check(remaining() == 5, "replacement stack cancels without consuming it");

        begin(new aan(278, 1, 0), 20, 0, true);
        for (int x = X - 2; x <= X + 2; x++)
            for (int z = Z; z <= Z + 5; z++) world.g(x, Y, z, COAL);
        ordinaryBlock(X, Z);
        for (int i = 0; i < 600; i++) tick();
        check(player.av().i() == 24 && world.a(X + 2, Y, Z + 5) == COAL, "24-block cap and six-block Manhattan radius");

        begin(new aan(278, 1, 0), 20, 0, true);
        world.d(X, Y, Z, 73, 0);
        world.d(X + 1, Y, Z, 74, 0);
        world.d(X + 2, Y, Z, 15, 0);
        ordinaryBlock(X, Z);
        for (int i = 0; i < 80; i++) tick();
        check(world.a(X + 1, Y, Z) == 0 && world.a(X + 2, Y, Z) == 15, "lit and unlit redstone match within one seam");
        begin(new aan(278, 1, 0), 20, 0, true);
        world.d(X, Y, Z, 140, 4);
        world.d(X + 1, Y, Z, 140, 4);
        world.d(X + 2, Y, Z, 140, 5);
        world.d(X + 3, Y, Z, 249, 0);
        ordinaryBlock(X, Z);
        for (int i = 0; i < 80; i++) tick();
        check(world.a(X + 1, Y, Z) == 0 && world.e(X + 2, Y, Z) == 5
                && world.a(X + 3, Y, Z) == 249, "RedPower crop keeps metal metadata and IC2 registry distinct");
        begin(new aan(278, 1, 0), 20, 0, true);
        world.d(X, Y, Z, 249, 0);
        world.d(X + 1, Y, Z, 249, 0);
        ordinaryBlock(X, Z);
        for (int i = 0; i < 80; i++) tick();
        check(world.a(X + 1, Y, Z) == 0, "registered IC2 copper seam");

        begin(new aan(278, 1, 0), 20, 0, true);
        placeSeam();
        ordinaryBlock(X, Z);
        game.s = new vp();
        for (int i = 0; i < 80; i++) tick();
        game.s = null;
        check(remaining() == 5, "opening a GUI stops the action");

        begin(new aan(278, 1, 0), 20, 0, true);
        placeSeam();
        ordinaryBlock(X, Z);
        player.c(X - 6, Y, Z + 0.5, 0, 0);
        for (int i = 0; i < 80; i++) tick();
        check(remaining() == 5, "out-of-range movement stops the action");

        begin(new aan(278, 1, 0), 20, 0, true);
        player.c(318, Y, Z + 0.5, 0, 0);
        world.g(319, Y, Z, COAL);
        world.g(320, Y, Z, COAL);
        ordinaryBlock(319, Z);
        ko provider = (ko) world.z();
        world.a(new uh(700, Y, 700));
        provider.d(20, 18);
        provider.a();
        check(!provider.a(20, 18), "adjacent chunk unloads during pending harvest");
        for (int i = 0; i < 80; i++) tick();
        check(!provider.a(20, 18), "vein mining does not reload its frontier");
        provider.b(20, 18);
        check(world.a(320, Y, Z) == COAL, "unloaded ore remains");
        System.out.println("PASS release, food and charge cutoffs, tool replacement, cap, radius, GUI, range, unload and native ore identities");
    }

    private Result measure(String name, aan stack, boolean vein, int saturation) {
        begin(stack, 20, saturation, vein);
        placeSeam();
        world.g(X + 3, Y, Z + 2, COAL);
        world.g(X + 4, Y, Z, COAL);
        int before = charge(stack);
        int ticks = ordinaryBlock(X, Z);
        if (vein) {
            // Direct and held attack entry points are suppressed once the gesture owns the seam.
            check(!controller.b(X + 1, Y, Z, 4), "competing native harvest blocked");
            while (remaining() > 0 && ticks < 600) {
                tick();
                ticks++;
            }
        } else {
            for (int i = 1; i < SEAM.length; i++) ticks += ordinaryBlock(X + SEAM[i][0], Z + SEAM[i][1]);
        }
        check(remaining() == 0 && world.a(X + 3, Y, Z + 2) == COAL && world.a(X + 4, Y, Z) == COAL,
                "one connected seam harvested and two isolated ores preserved");
        int drops = 0;
        for (Object entity : world.b) if (entity instanceof fq && !((fq) entity).G && ((fq) entity).a.c == 263) drops += ((fq) entity).a.a;
        check(drops == SEAM.length, "native drops exactly once");
        int cost = before < 0 ? stack.i() : before - charge(stack);
        int food = player.aF().a();
        float remainingSaturation = player.aF().d();
        System.out.printf("VEIN %s blocks=6 ticks=%d cost=%d %s food=20->%d saturation=%d->%.1f%n",
                name, ticks, cost, before < 0 ? "wear" : "EU", food, saturation, remainingSaturation);
        return new Result(ticks, cost, food, remainingSaturation);
    }

    private void begin(aan stack, int food, int saturation, boolean vein) {
        attack.e = modifier.e = false;
        tick();
        controller.a();
        for (Object entity : world.b) if (entity instanceof fq) ((fq) entity).A();
        for (int x = X - 2; x <= X + 6; x++)
            for (int z = Z; z <= Z + 6; z++) world.g(x, Y, z, 0);
        player.ap.c = 0;
        player.ap.a(0, stack);
        ady stats = new ady();
        stats.a("foodLevel", food);
        stats.a("foodSaturationLevel", (float) saturation);
        player.aF().a(stats);
        modifier.e = vein;
        attack.e = true;
    }

    private int ordinaryBlock(int x, int z) {
        controller.a(x, Y, z, 4);
        int ticks = 0;
        while (world.a(x, Y, z) != 0 && ticks++ < 150) {
            controller.c(x, Y, z, 4);
            tick();
        }
        check(world.a(x, Y, z) == 0, "normal Survival controller harvested its starter");
        return ticks;
    }

    private void tick() {
        mining.tickEnd(EnumSet.of(TickType.GAME));
        player.aF().a(player);
    }

    private void placeSeam() { for (int[] point : SEAM) world.g(X + point[0], Y, Z + point[1], COAL); }
    private int remaining() {
        int blocks = 0;
        for (int[] point : SEAM) if (world.a(X + point[0], Y, Z + point[1]) != 0) blocks++;
        return blocks;
    }

    private static aan powered(ItemElectricToolDrill drill, int eu) {
        aan stack = new aan(drill);
        check(ElectricItem.charge(stack, eu, 1, true, false) == eu, "native charging");
        return stack;
    }

    private static int charge(aan stack) {
        return stack.a() instanceof ItemElectricToolDrill ? ElectricItem.discharge(stack, 10000, 1, true, true) : -1;
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        return unsafe.getMethod("allocateInstance", Class.class).invoke(field(unsafe, "theUnsafe").get(null), type);
    }

    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }

    private static final class Result {
        final int ticks, cost, food;
        final float saturation;
        Result(int ticks, int cost, int food, float saturation) {
            this.ticks = ticks;
            this.cost = cost;
            this.food = food;
            this.saturation = saturation;
        }
    }

    private static final class Fixture extends xd {
        Fixture(File root) {
            super(new aia(root, "vein-mining", false), "vein-mining", NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 17; x <= 21; x++)
                for (int z = 16; z <= 20; z++) d(x, z);
        }
        @Override public void a(nn entity, String sound, float gain, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float gain, float pitch) {}
    }
}
