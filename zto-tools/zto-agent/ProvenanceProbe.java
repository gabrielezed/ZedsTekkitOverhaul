import buildcraft.api.PowerFramework;
import buildcraft.energy.PneumaticPowerFramework;
import buildcraft.factory.TileQuarry;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.settlement.WorldProvenance;

/** Native local controller, ItemBlock, BuildCraft excavation and disposable Anvil save. */
public final class ProvenanceProbe {
    private static final int X = 312, Y = 8, Z = 288;
    private static final String SAVE = "native-provenance";

    public static void main(String[] args) {
        try {
            run(new File(args[0]));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void run(File root) throws Exception {
        aia save = new aia(root, SAVE, false);
        Fixture world = new Fixture(save);
        Path directory = new File(root, SAVE).toPath();
        Files.createFile(directory.resolve(".zto-disposable"));
        // Open a legacy save with pre-existing planks.
        world.g(X, Y, Z, 5);
        world.g(X + 1, Y - 1, Z, 1);
        world.g(X + 2, Y, Z, 1);
        world.g(X + 3, Y, Z, 1);
        world.a(true, (rw) null);

        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        Minecraft game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja error) { throw new AssertionError(error); }
        };
        game.f = world;
        game.R = true;
        aes controller = new aes(game);
        game.c = controller;
        vq player = new vq(game, world, new fc("ProvenanceProbe", ""), 0) {
            @Override public void a(ajw statistic, int count) {}
        };
        game.h = player;
        Field client = FMLClientHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(FMLClientHandler.instance(), game);
        player.c(X - 2.0, Y, Z + 0.5, 0, 0);

        WorldProvenance provenance = new WorldProvenance();
        provenance.register();
        provenance.bind(world);
        provenance.bindPlayer(player);
        check(provenance.snapshot(world, X, Y, Z).position.equals("Unknown history"), "old planks have unknown history");

        aan planks = new aan(5, 64, 0);
        long elapsed = System.nanoTime();
        for (int i = 0; i < 60; i++) {
            check(planks.a(player, world, X + 1, Y - 1, Z, 1), "native placement accepted");
            check(controller.b(X + 1, Y, Z, 1), "native controller removal");
        }
        elapsed = System.nanoTime() - elapsed;
        WorldProvenance.Snapshot repeated = provenance.snapshot(world, X + 1, Y, Z);
        check(repeated.placed == 0 && repeated.manualRemoval == 1, "cycle retains one latest record");
        check(planks.a(player, world, X + 1, Y - 1, Z, 1), "replacement plank");
        check(controller.b(X + 2, Y, Z, 1), "ordinary rock excavation");
        check(!new aan(5, 0, 0).a(player, world, X + 2, Y - 1, Z, 1), "empty stack cannot place");
        PowerFramework.currentFramework = new PneumaticPowerFramework();
        TileQuarry quarry = new TileQuarry();
        quarry.i = world;
        quarry.j = X + 4;
        quarry.k = Y;
        quarry.l = Z;
        quarry.targetX = X + 3;
        quarry.targetY = Y + 1;
        quarry.targetZ = Z;
        quarry.positionReached(null);
        check(world.a(X + 3, Y, Z) == 0, "native quarry dug target");
        WorldProvenance.Snapshot seen = provenance.snapshot(world, X + 1, Y, Z);
        check(seen.hooks == 7 && seen.placed == 1 && seen.manualRemoval == 1 && seen.quarryRemoval == 1,
                "placement, excavation and quarry kept distinct");
        WorldProvenance.Snapshot distant = provenance.snapshot(world, X + 16000, Y, Z);
        check(distant.position.equals("Unknown history") && !world.j(X + 16000, Y, Z), "unloaded remains unknown");
        world.t.g = -1;
        check(provenance.snapshot(world, X + 1, Y, Z).status.equals("OUTSIDE_OVERWORLD"), "dimension scope");
        world.t.g = 0;
        world.a(true, (rw) null);
        provenance.save(world);
        ce.a.a();
        Path evidence = directory.resolve("data/zto_world_provenance.dat");
        long bytes = Files.size(evidence);
        provenance.bind(null);
        Fixture reopened = new Fixture(new aia(root, SAVE, false));
        provenance.bind(reopened);
        WorldProvenance.Snapshot after = provenance.snapshot(reopened, X + 1, Y, Z);
        check(reopened.a(X + 1, Y, Z) == 5 && after.placed == 1 && after.manualRemoval == 1 && after.quarryRemoval == 1,
                "native save and provenance survive reopen: block=" + reopened.a(X + 1, Y, Z)
                        + " status=" + after.status + " counts=" + after.placed + "/" + after.manualRemoval
                        + "/" + after.quarryRemoval);
        byte[] corrupt = {1, 2, 3};
        Files.write(evidence, corrupt);
        provenance.bind(null);
        provenance.bind(reopened);
        check(provenance.snapshot(reopened, X + 1, Y, Z).status.equals("UNREADABLE"), "malformed provenance diagnosed");
        provenance.save(reopened);
        check(Arrays.equals(corrupt, Files.readAllBytes(evidence)), "malformed file preserved");
        System.out.printf("PASS native provenance: 120 controller/item mutations in %.2f ms, %d-byte NBT file%n",
                elapsed / 1000000.0, bytes);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture extends xd {
        Fixture(aia save) {
            super(save, SAVE, NaturalSpawnProbe.SETTINGS);
            for (int x = 17; x <= 21; x++)
                for (int z = 16; z <= 20; z++) d(x, z);
        }

        @Override public void a(nn entity, String sound, float gain, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float gain, float pitch) {}
    }
}
