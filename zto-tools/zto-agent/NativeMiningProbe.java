import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import ic2.common.ElectricItem;
import ic2.common.ItemElectricToolDDrill;
import ic2.common.ItemElectricToolDrill;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;

/** Exercises the pinned controller, vanilla food and the actual IC2 charge implementation. */
public final class NativeMiningProbe {
    private static final int X = 312, Y = 4, Z = 289;
    private static final int COAL = 16, STONE_PICK = 274;
    private static final int[][] SEAM = {{0,0},{1,0},{2,0},{2,1},{1,1},{0,1}};
    private final Fixture world;
    private final Minecraft game;
    private final aes controller;
    private final vq player;

    public static void main(String[] args) {
        try {
            new NativeMiningProbe(new File(args[0])).run();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private NativeMiningProbe(File root) throws Exception {
        world = new Fixture(root);
        Files.createFile(new File(root, "native-mining/.zto-disposable").toPath());
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
        Field client = FMLClientHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(FMLClientHandler.instance(), game);
        player.c(X - 1.0, Y, Z + 0.5, 0, 0);
        player.z = true;
    }

    private void run() {
        ItemElectricToolDrill drill = new ItemElectricToolDrill(22001, 0);
        ItemElectricToolDDrill diamond = new ItemElectricToolDDrill(22002, 0);
        drill.init();
        diamond.init();
        measure("stone pickaxe", new aan(STONE_PICK, 1, 0), 6);
        measure("Mining Drill", powered(drill, 1000), 6 * drill.operationEnergyCost);
        measure("Diamond Drill", powered(diamond, 1000), 6 * diamond.operationEnergyCost);
    }

    private void measure(String name, aan stack, int expectedCost) {
        controller.a();
        for (Object entity : world.b) if (entity instanceof fq) ((fq) entity).A();
        player.ap.c = 0;
        player.ap.a(0, stack);
        for (int[] point : SEAM) world.g(X + point[0], Y, Z + point[1], COAL);
        world.g(X + 3, Y, Z + 1, 1);
        world.g(X + 3, Y, Z + 2, COAL);
        int before = charge(stack);
        controller.a(X, Y, Z, 4);
        controller.c(X, Y, Z, 4);
        check(world.a(X, Y, Z) == COAL, "interrupted start leaves ore intact");
        controller.a(); // Native reset on attack release.
        int ticks = ordinaryBlock(X, Z);
        for (int i = 0; i < 80; ++i) controller.c(X, Y, Z, 4);
        check(world.a(X + 1, Y, Z) == COAL && world.a(X, Y, Z + 1) == COAL,
                "holding attack on the exhausted target leaves adjacent ore intact");
        for (int i = 1; i < SEAM.length; i++) ticks += ordinaryBlock(X + SEAM[i][0], Z + SEAM[i][1]);
        check(world.a(X + 3, Y, Z + 1) == 1 && world.a(X + 3, Y, Z + 2) == COAL,
                "ordinary mining leaves stone and diagonal ore intact");
        int drops = 0;
        for (Object entity : world.b) if (entity instanceof fq && !((fq) entity).G && ((fq) entity).a.c == 263) drops += ((fq) entity).a.a;
        int cost = before < 0 ? stack.i() : before - charge(stack);
        check(drops == SEAM.length && cost == expectedCost, "one drop and native tool cost per targeted ore");
        System.out.printf("PASS native %s: %d coal, cost %d %s, %d mining ticks%n",
                name, drops, cost, before < 0 ? "durability" : "EU", ticks);
    }

    private int ordinaryBlock(int x, int z) {
        controller.a(x, Y, z, 4);
        int ticks = 0;
        while (world.a(x, Y, z) != 0 && ticks++ < 150) {
            controller.c(x, Y, z, 4);
            player.aF().a(player);
        }
        check(world.a(x, Y, z) == 0, "normal Survival controller harvested its starter");
        return ticks;
    }

    private static aan powered(ItemElectricToolDrill drill, int eu) {
        aan stack = new aan(drill);
        check(ElectricItem.charge(stack, eu, 1, true, false) == eu, "native charging");
        return stack;
    }

    private static int charge(aan stack) {
        return stack.a() instanceof ItemElectricToolDrill ? ElectricItem.discharge(stack, 10000, 1, true, true) : -1;
    }

    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }

    private static final class Fixture extends xd {
        Fixture(File root) {
            super(new aia(root, "native-mining", false), "native-mining", NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 17; x <= 21; x++)
                for (int z = 16; z <= 20; z++) d(x, z);
        }
        @Override public void a(nn entity, String sound, float gain, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float gain, float pitch) {}
    }
}
