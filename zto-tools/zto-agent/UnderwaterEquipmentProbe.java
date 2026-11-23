import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.FMLRegistry;
import buildcraft.core.DefaultProps;
import buildcraft.transport.pipes.PipeItemsDiamond;
import ic2.api.Direction;
import ic2.api.Items;
import ic2.common.EnergyNet;
import ic2.common.TileEntityCable;
import ic2.common.TileEntityGenerator;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
import zed.tekkitoverhaul.agent.ZTOAgent;
import zed.tekkitoverhaul.exploration.underwater.TileAirCompressor;
import zed.tekkitoverhaul.exploration.underwater.UnderwaterRegistry;

/** Production-obfuscated gameplay fixture. No window, input or OpenGL context is created. */
public final class UnderwaterEquipmentProbe {
    private static final int MASK = 22268, TANK = 22269, FLIPPERS = 22270, COMPRESSOR = 3031;
    private static final String OXYGEN = "ZTO_OxygenTicks", EU = "ZTO_CompressorEU";

    public static void main(String[] args) {
        try {
            run(new File(args[0]));
            System.exit(0); // The native region I/O thread otherwise keeps the fixture alive.
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(File root) throws Exception {
        Files.createDirectories(new File(root, "config").toPath());
        Field directory = Minecraft.class.getDeclaredField("aj");
        directory.setAccessible(true);
        directory.set(null, root);
        FMLClientHandler handler = FMLClientHandler.instance();
        Fixture world = new Fixture(root);
        FMLCommonHandler.instance().beginLoading(handler);
        FMLRegistry.registerRegistry(new ClientRegistry());
        Files.createFile(new File(root, "underwater/.zto-disposable").toPath());
        HeadlessClient client = new HeadlessClient();
        client.F = root;
        client.f = world;
        client.p = new aaw(null, null) {
            @Override public int b(String path) { return 0; }
            @Override public int[] a(String path) { return new int[256 * 256]; }
        };
        Field clientField = FMLClientHandler.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(handler, client);
        Field mods = Loader.class.getDeclaredField("mods");
        mods.setAccessible(true);
        mods.set(Loader.instance(), new ArrayList<>());
        new mod_IC2().load();
        ModLoader.registerTileEntity(TileEntityCable.class, "Cable");
        ModLoader.registerTileEntity(TileEntityGenerator.class, "Generator");
        BuildCraftCore.initialize();
        // Use BuildCraft's own item registration without constructing its OpenGL pipe renderer.
        Method createPipe = BuildCraftTransport.class.getDeclaredMethod("createPipe", int.class, Class.class,
                String.class, Object.class, Object.class, Object.class);
        createPipe.setAccessible(true);
        BuildCraftTransport.pipeItemsDiamond = (yr) createPipe.invoke(null, DefaultProps.PIPE_ITEMS_DIAMOND_ID,
                PipeItemsDiamond.class, "Diamond Transport Pipe", yr.n, pb.M, yr.n);
        UnderwaterRegistry.register(ZtoConfigLoader.load(new File(root, "config").toPath(), false,
                Logger.getLogger("underwater-fixture")));
        verifyRecipes();
        verifyMask(client, world);
        yw diver = NaturalSpawnProbe.join(world);
        verifyBreathing(world, diver);
        verifySwimming(world, diver);
        verifyMachine(root, world, diver);
    }

    private static void verifyRecipes() {
        aan rubber = Items.getItem("rubber");
        aan cell = Items.getItem("cell");
        aan alloy = Items.getItem("advancedAlloy");
        aan boots = Items.getItem("rubberBoots");
        aan carbon = Items.getItem("carbonPlate");
        aan compressor = Items.getItem("compressor");
        aan pipe = new aan(BuildCraftTransport.pipeItemsDiamond);
        recipe(MASK, null, new aan(102, 1, 0), null, rubber, null, rubber, null, new aan(334, 1, 0), null);
        recipe(TANK, alloy, cell, alloy, alloy, new aan(299, 1, 0), alloy, null, null, null);
        recipe(FLIPPERS, carbon, boots, carbon, carbon, null, carbon, null, null, null);
        recipe(COMPRESSOR, pipe, null, pipe, cell, compressor, cell, cell, null, cell);
        System.out.printf("PASS native shaped recipes and metadata, IDs %d, %d, %d, %d%n",
                MASK, TANK, FLIPPERS, COMPRESSOR);
    }

    private static void verifyMask(HeadlessClient client, Fixture world) throws Exception {
        Class.forName("lr", false, UnderwaterEquipmentProbe.class.getClassLoader()).getDeclaredMethods();
        vq local = new vq(client, world, new fc("UnderwaterFixture", ""), 0);
        client.h = local;
        float original = 0.1F;
        check(ZTOAgent.underwaterFog(original, local) == original, "camera without mask keeps native fog");
        local.ap.b[3] = new aan(MASK, 1, 0);
        check(Math.abs(ZTOAgent.underwaterFog(original, local) - original / 1.30F) < 1E-7,
                "mask callback applies only the proposed water fog density");
        check(ZTOAgent.underwaterFog(original, new yw(world) { @Override public void ab() {} }) == original,
                "other cameras retain native density");
        for (int id : new int[] {MASK, TANK, FLIPPERS}) {
            aan stack = new aan(id, 1, 0);
            String texture = ((forge.IArmorTextureProvider) stack.a()).getArmorTextureFile(stack);
            check(UnderwaterRegistry.class.getResource(texture) != null, "worn texture resolves for " + id);
        }
        System.out.println("PASS renderer linkage, local mask density selection and worn texture resource lookup");
    }

    private static void recipe(int id, aan... pattern) {
        ade grid = new ade(new dd() {
            @Override public boolean b(yw player) { return true; }
        }, 3, 3);
        for (int index = 0; index < pattern.length; index++) {
            grid.a(index, pattern[index] == null ? null : pattern[index].k());
        }
        aan result = fr.a().a(grid);
        check(result != null && result.c == id, "exact native recipe " + id);
        if (id == TANK) check(charge(result) == 0 && !result.d(), "new tank is empty and nonstackable");
        for (int index = 0; index < pattern.length; index++) {
            if (pattern[index] == null) continue;
            aan wrong = pattern[index].k();
            wrong.b(wrong.i() + 1);
            grid.a(index, wrong);
            aan rejected = fr.a().a(grid);
            check(rejected == null || rejected.c != id, "no wildcard at slot " + index + " of " + id);
            grid.a(index, pattern[index].k());
        }
    }

    private static void verifyBreathing(Fixture world, yw diver) {
        diver.c(288.5, 6, 288.5, 0, 0);
        for (int y = 5; y <= 9; y++) world.b(288, y, 288, 8, 1);
        aan spare = tank(200);
        diver.ap.a[0] = spare;
        diver.B();
        check(diver.Y() == 299 && charge(spare) == 200, "carried tank does not supply native air");
        diver.ap.b[3] = new aan(MASK, 1, 0);
        diver.B();
        check(diver.Y() == 298, "mask has no breathing effect");
        aan worn = tank(3);
        diver.ap.b[2] = worn;
        for (int i = 2; i >= 0; i--) {
            diver.B();
            check(diver.Y() == 298 && charge(worn) == i, "one charge per submerged native air update");
        }
        diver.B();
        check(diver.Y() == 297, "normal air resumes after final oxygen");
        diver.g(-19);
        int health = diver.bb();
        diver.B();
        check(diver.Y() == 0 && diver.bb() < health, "empty tank preserves native drowning");
        diver.ap.b[2] = spare;
        diver.B();
        check(diver.Y() == 0 && charge(spare) == 199, "charged swap holds air without replenishing reserve");
        diver.ap.a[0] = null;
        diver.ap.b[2] = null;
        diver.B();
        check(diver.Y() == -1, "unequipping stops breathing support");
        diver.ap.b[2] = spare;
        diver.aT.a = true;
        diver.B();
        check(charge(spare) == 199, "creative invulnerability avoids consumption");
        diver.aT.a = false;
        diver.c(288.5, 10, 288.5, 0, 0);
        diver.B();
        check(diver.Y() == 300 && charge(spare) == 199, "surfacing restores air only");
        diver.c(288.5, 6, 288.5, 0, 0);
        diver.B();
        check(diver.Y() == 300 && charge(spare) == 198, "re-entry continues prior charge");
        ady saved = new ady();
        diver.b(saved);
        yw restored = new yw(world) { @Override public void ab() {} };
        restored.a(saved);
        check(charge(restored.ap.b[2]) == 198, "player armor NBT round trip");
        diver.ap.b[2] = tank(6000);
        diver.g(300);
        for (int tick = 0; tick < 6000; tick++) diver.B();
        check(charge(diver.ap.b[2]) == 0 && diver.Y() == 300, "full tank lasts exactly 6000 submerged updates");
        diver.B();
        check(diver.Y() == 299, "full tank eventually exhausts");
        System.out.println("PASS native air, flowing water, final charge, drowning, swaps, creative and player NBT");
    }

    private static void verifySwimming(Fixture world, yw diver) {
        for (int x = 290; x <= 305; x++) {
            for (int z = 288; z <= 294; z++) {
                world.g(x, 4, z, 1);
                for (int y = 5; y <= 12; y++) world.g(x, y, z, 9);
                world.g(x, 19, z, 1);
            }
        }
        double ordinary = swim(diver, 10, false, 1);
        double flippers = swim(diver, 10, true, 1);
        check(ordinary > 0.2 && Math.abs(flippers / ordinary - 1.25) < .015,
                "water travel ratio " + flippers / ordinary);
        double land = swim(diver, 22, false, 1);
        double finsOnLand = swim(diver, 22, true, 1);
        check(Math.abs(land - finsOnLand) < 1E-6, "land acceleration is unchanged: " + land + " / " + finsOnLand);
        check(Math.abs(swim(diver, 10, false, 0) - swim(diver, 10, true, 0)) < 1E-6,
                "stationary water drift is unchanged");
        check(Math.abs(swim(diver, 10, false, 1) - ordinary) < 1E-6,
                "unequipping restores native thrust");
        System.out.printf("PASS 20-tick native travel: %.4f blocks vanilla, %.4f with flippers%n", ordinary, flippers);
    }

    private static double swim(yw diver, int y, boolean fins, float forward) {
        diver.c(297.5, y, 290.5, -90, 0);
        diver.r = diver.s = diver.t = 0;
        diver.z = false;
        diver.ap.b[0] = fins ? new aan(FLIPPERS, 1, 0) : null;
        for (int tick = 0; tick < 20; tick++) {
            diver.B();
            diver.a_(0F, forward);
        }
        return diver.o - 297.5D;
    }

    private static void verifyMachine(File root, Fixture world, yw diver) throws Exception {
        int x = 289, y = 5, z = 295;
        check(world.g(x, y, z, COMPRESSOR), "compressor placement");
        TileAirCompressor tile = (TileAirCompressor) world.b(x, y, z);
        tile.a(0, new aan(1, 1, 0));
        check(tile.k_(0) == null, "direct insertion rejects unrelated items");
        aan tank = tank(5997);
        aan other = tank(276);
        diver.ap.a[9] = tank;
        Constructor<?> ctor = Class.forName("zed.tekkitoverhaul.exploration.underwater.ContainerAirCompressor")
                .getDeclaredConstructor(aak.class, TileAirCompressor.class);
        ctor.setAccessible(true);
        dd container = (dd) ctor.newInstance(diver.ap, tile);
        check(container.a(1, 0, true, diver) != null && tile.k_(0) == tank && diver.ap.a[9] == null, "shift-click inserts one tank");
        diver.ap.a[10] = other;
        check(container.a(2, 0, true, diver) == null && diver.ap.a[10] == other && tile.k_(0) == tank,
                "a second shift-click cannot replace an occupied machine slot");
        check(container.a(0, 0, true, diver) != null && tile.k_(0) == null && diver.ap.a[8] == tank,
                "shift-click retrieves the same tank");
        diver.ap.a[8] = null;
        tile.a(0, tank);
        tile.n_();
        check(charge(tank) == 5997, "no recharge without EU");
        aan copper = Items.getItem("insulatedCopperCableBlock");
        world.b(x - 1, y, z, copper.c, copper.i());
        TileEntityCable cable = (TileEntityCable) world.b(x - 1, y, z);
        cable.onCreated();
        aan generator = Items.getItem("generator");
        world.b(x - 2, y, z, generator.c, generator.i());
        TileEntityGenerator source = (TileEntityGenerator) world.b(x - 2, y, z);
        source.onCreated();
        source.a(1, new aan(263, 1, 0));
        EnergyNet net = EnergyNet.getForWorld(world);
        source.n_();
        check(source.isAddedToEnergyNet() && net.getTotalEnergyConducted(cable) > 0
                && buffered(tile) > 0, "pinned IC2 EnergyNet delivers LV through a real Copper Cable");
        int before = buffered(tile);
        tile.n_();
        check(charge(tank) == 6000 && buffered(tile) == before - 1, "one paid, clamped final cycle");
        tile.n_();
        check(buffered(tile) == before - 1 && tile.injectEnergy(Direction.XN, 33) == 33,
                "full tank stops charging and overvoltage leaves the buffer intact");
        check(charge(other) == 276, "charging did not modify the second tank");
        tile.a(0, tank(1100));
        world.a(true, (rw) null);
        ce.a.a();
        agp.a();
        tile.onChunkUnload();
        check(!tile.isAddedToEnergyNet(), "chunk unload disconnects the sink");
        Fixture reopened = new Fixture(root);
        TileAirCompressor restored = (TileAirCompressor) reopened.b(x, y, z);
        ady state = new ady();
        if (restored != null) restored.b(state);
        check(restored != null && charge(restored.k_(0)) == 1100 && buffered(restored) == before - 1
                && !restored.isAddedToEnergyNet(), "disk reload block=" + reopened.a(x, y, z) + " state=" + state
                + " expected EU=" + (before - 1) + " cable EU=" + net.getTotalEnergyConducted(cable));
        restored.n_();
        check(restored.isAddedToEnergyNet() && charge(restored.k_(0)) == 1104
                && buffered(restored) == before - 2, "loaded sink reconnects and resumes from saved EU");
        reopened.g(x, y, z, 0);
        int drops = 0;
        for (Object entry : reopened.b) {
            if (entry instanceof fq && ((fq) entry).a.c == TANK && charge(((fq) entry).a) == 1104) drops++;
        }
        check(drops == 1 && restored.k_(0) == null && !restored.isAddedToEnergyNet(), "break drops the charged tank once");
        System.out.println("PASS native IC2 EU through copper, slot transfers, clamped charging, disk reload and one break drop");
    }

    private static aan tank(int ticks) {
        aan stack = new aan(TANK, 1, 0);
        stack.d = new ady();
        stack.d.a(OXYGEN, ticks);
        return stack;
    }

    private static int charge(aan tank) { return tank.d == null ? 0 : tank.d.f(OXYGEN); }

    private static int buffered(TileAirCompressor tile) {
        ady data = new ady();
        tile.b(data);
        return data.f(EU);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Fixture extends xd {
        Fixture(File root) {
            super(new aia(root, "underwater", false), "underwater", NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 17; x <= 20; x++) for (int z = 17; z <= 20; z++) d(x, z);
        }

        @Override public void a(nn entity, String sound, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
    }

    private static final class HeadlessClient extends Minecraft {
        HeadlessClient() { super(null, null, null, 1, 1, false); }
        @Override public void a(ja failure) { throw new AssertionError(failure); }
    }
}
