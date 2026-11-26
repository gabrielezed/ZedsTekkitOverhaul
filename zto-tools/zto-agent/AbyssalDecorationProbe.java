import java.io.File;
import java.util.logging.Logger;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
import zed.tekkitoverhaul.exploration.abyssal.AbyssalRegistry;
import zed.tekkitoverhaul.exploration.abyssal.MarineType;

/** Abyssal decoration fixture on the reobfuscated candidate and pinned pack. */
public final class AbyssalDecorationProbe {
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
        UnderwaterEquipmentProbe.Fixture world = UnderwaterEquipmentProbe.startFixture(root, "abyssal");
        ZtoConfig config = ZtoConfigLoader.load(new File(root, "config").toPath(), false, Logger.getLogger("abyssal"));
        AbyssalRegistry.register(config, 100);
        int kelp = config.abyssalId("block.abyssalKelp");
        int frond = config.abyssalId("item.kelpFrond") + 256;
        int stone = config.abyssalId("block.abyssalStone");
        for (AbyssalRegistry.MaterialType type : AbyssalRegistry.MaterialType.values()) {
            check(type.block() != null && new aan(type.block()).a() != null, "registered ItemBlock " + type);
        }
        int lamp = config.abyssalId("block.abyssalLamp");
        check(pb.q[lamp] == 5, "native lamp light is 5");
        check(pb.q[config.abyssalId("block.ghostAnemone")] == 3, "native anemone light is 3");
        recipe(config.abyssalId("block.abyssalBricks"), 4, stone, stone, 0, stone, stone);
        recipe(config.abyssalId("block.erodedAbyssalBricks"), 1, config.abyssalId("block.abyssalBricks"), 12);
        recipe(lamp, 1, stone, config.abyssalId("item.luminousPolyp") + 256);
        int shell = config.abyssalId("item.abyssalShell") + 256;
        int shellstone = config.abyssalId("block.shellstone");
        recipe(shell, 1, shellstone);
        recipe(shellstone, 1, shell, stone);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.b(X + dx, Y - 1, Z + dz, stone, 0);
                for (int y = Y; y <= Y + 5; y++) world.b(X + dx, y, Z + dz, 9, 0);
            }
        }
        yw diver = NaturalSpawnProbe.join(world);
        aan stack = new aan(frond, 5, 0);
        for (int y = Y; y <= Y + 3; y++) {
            check(stack.a(diver, world, X, y - 1, Z, 1), "place segment at height " + y);
            check(world.a(X, y, Z) == kelp && world.e(X, y, Z) == 8, "occupied source");
        }
        check(!stack.a(diver, world, X, Y + 3, Z, 1) && stack.a == 1, "fifth segment rejected");
        diver.c(X + 0.5, Y + 1, Z + 0.5, 0, 0);
        diver.B();
        check(diver.Y() == 299, "eyes in Kelp consume air");
        check(diver.g_(), "Kelp cell participates in native water movement");
        check(MarineType.KELP.block().c(world, X, Y, Z) == null, "no collision box");

        int[] plants = {3040, 3041, 3042, 3043};
        int[] cuttings = {22274, 22275, 22276, 22277};
        for (int i = 0; i < plants.length; i++) {
            int x = X + (i % 2 == 0 ? -1 : 1);
            int z = Z + (i < 2 ? -1 : 1);
            aan cutting = new aan(cuttings[i], 2, 0);
            diver.c(x + 0.5, Y, z + 0.5, 90, 0);
            check(cutting.a(diver, world, x, Y - 1, z, 1) && cutting.a == 1, "place marine type " + i);
            check(world.a(x, Y, z) == plants[i] && world.e(x, Y, z) == (i == 1 ? 9 : 8), "metadata " + i);
            check(pb.m[plants[i]].c(world, x, Y, z) == null, "marine type has no collision " + i);
            check(pb.m[plants[i]].a(world.e(x, Y, z), new java.util.Random(1), 0) == cuttings[i], "drop ID " + i);
            diver.g(300);
            diver.B();
            check(diver.Y() == 299, "marine type is not breathable " + i);
        }
        world.a(true, (rw) null);
        ce.a.a();
        agp.a();
        UnderwaterEquipmentProbe.Fixture reopened = new UnderwaterEquipmentProbe.Fixture(root, "abyssal");
        for (int y = Y; y <= Y + 3; y++) {
            check(reopened.a(X, y, Z) == kelp && reopened.e(X, y, Z) == 8, "segment survives disk reload");
        }
        check(MarineType.KELP.block().removeBlockByPlayer(reopened, diver, X, Y + 3, Z), "player removal succeeds");
        check(reopened.a(X, Y + 3, Z) == 9 && reopened.e(X, Y + 3, Z) == 0, "source restored");
        check(stack.a(diver, reopened, X, Y + 2, Z, 1), "plant again after harvest");
        check(reopened.a(X + 1, Y, Z - 1) == 3041 && reopened.e(X + 1, Y, Z - 1) == 9,
                "fan orientation survives disk reload");
        reopened.g(X, Y - 1, Z, 0);
        int fronds = 0;
        for (Object entity : reopened.b) {
            if (entity instanceof fq && ((fq) entity).a.c == frond) fronds += ((fq) entity).a.a;
        }
        check(fronds == 4, "support cascade drops exactly four fronds");
        for (int y = Y; y <= Y + 3; y++) {
            check(reopened.a(X, y, Z) == 9 || reopened.a(X, y, Z) == 8, "support loss restores water");
            check(reopened.a(X + 1, y, Z) == 9 || reopened.a(X + 1, y, Z) == 8, "neighbor remains water");
        }
        System.out.println("PASS material ItemBlocks, five recipes, light levels 5 and 3, marine placement, native air, collision, reload and restoration");
    }

    private static void recipe(int resultId, int count, int... inputs) {
        ade grid = new ade(new dd() {
            @Override public boolean b(yw player) { return true; }
        }, 3, 3);
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != 0) grid.a(i, new aan(inputs[i], 1, 0));
        }
        aan result = fr.a().a(grid);
        check(result != null && result.c == resultId && result.a == count, "native recipe " + resultId);
    }

    private static void check(boolean value, String detail) {
        if (!value) throw new AssertionError(detail);
    }
}
