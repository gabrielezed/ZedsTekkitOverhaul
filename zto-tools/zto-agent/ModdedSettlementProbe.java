import buildcraft.factory.BlockTank;
import buildcraft.factory.TileTank;
import buildcraft.energy.BlockOilStill;
import buildcraft.energy.BlockOilFlowing;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.pipes.PipeItemsStone;
import cpw.mods.fml.common.registry.FMLRegistry;
import dan200.computer.shared.BlockComputer;
import dan200.computer.shared.TileEntityComputer;
import ee.BlockEEDevice;
import ee.TileCollector;
import eloraam.machine.TileDeploy;
import ic2.common.BlockMachine;
import ic2.common.BlockCable;
import ic2.common.BlockRubLeaves;
import ic2.common.BlockRubWood;
import ic2.common.Ic2Items;
import ic2.common.TileEntityIronFurnace;
import ic2.common.TileEntityCable;
import ic2.common.TileEntityMacerator;
import ic2.common.WorldGenRubTree;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Random;
import net.minecraft.client.Minecraft;
import powercrystals.netherores.BlockNetherOres;
import railcraft.common.utility.BlockUtility;
import railcraft.common.utility.EnumUtility;
import railcraft.common.utility.TileRollingMachine;
import zed.tekkitoverhaul.hopper.BlockHopper;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.settlement.ProtectedInterior.Assessment;
import zed.tekkitoverhaul.settlement.ProtectedInterior.Evidence;
import zed.tekkitoverhaul.settlement.ProtectedInterior.State;
import zed.tekkitoverhaul.settlement.SettlementRecognition.Snapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot;
import zed.tekkitoverhaul.settlement.SiteSnapshot.Coverage;

/** Registers real pack components directly so their native constructors run without display initialization. */
public final class ModdedSettlementProbe extends SettlementProbe {
    private static final int MASONRY = 180, DEVICE = 181, TANK = 182, PIPE = 183, REDPOWER = 184,
            RAILCRAFT = 185, EE2 = 186, COMPUTER = 187, HOPPER = 188, IC2 = 189, ORE = 190, LEAVES = 191, OIL = 193;
    private static final int FEET = Y + 8;
    private static final int RUBBER_WOOD = 194;
    private static yr pipeItem;

    private ModdedSettlementProbe(File root) throws Exception { super(root); }

    public static void main(String[] args) {
        try {
            File root = new File(args[0]);
            Field gameDirectory = Minecraft.class.getDeclaredField("aj");
            gameDirectory.setAccessible(true);
            gameDirectory.set(null, root);
            ModdedSettlementProbe probe = new ModdedSettlementProbe(root);
            boolean profile = args.length == 2;
            register(!profile);
            if (profile) {
                probe.profile();
            } else {
                probe.futureMod();
                probe.coveredFactory();
                probe.mixedFactory();
                probe.negativeControls();
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    static void register(boolean ic2) {
        FMLRegistry.registerRegistry(new ClientRegistry());
        new Masonry();
        new Device();
        new vd(MASONRY - 256);
        new vd(DEVICE - 256);
        new BlockTank(TANK);
        BuildCraftTransport.genericPipeBlock = new BlockGenericPipe(PIPE);
        pipeItem = BlockGenericPipe.registerPipe(6000, PipeItemsStone.class);
        eloraam.machine.BlockMachine machine = new eloraam.machine.BlockMachine(REDPOWER);
        RedPowerMachine.blockMachine = machine;
        machine.addTileEntityMapping(0, TileDeploy.class);
        new BlockUtility(RAILCRAFT);
        BlockEEDevice collector = new BlockEEDevice(EE2) {};
        collector.addTileEntityMapping(0, TileCollector.class);
        new BlockComputer(COMPUTER);
        new BlockHopper(HOPPER);
        new BlockNetherOres(ORE, 0);
        new BlockRubLeaves(LEAVES);
        new BlockRubWood(RUBBER_WOOD);
        Ic2Items.rubberWood = new aan(RUBBER_WOOD, 1, 0);
        Ic2Items.rubberLeaves = new aan(LEAVES, 1, 0);
        new BlockOilFlowing(OIL - 1, acn.g);
        new BlockOilStill(OIL, acn.g);
        ModLoader.registerTileEntity(FixtureInventory.class, "ZTO probe inventory");
        ModLoader.registerTileEntity(FixtureController.class, "ZTO probe controller");
        ModLoader.registerTileEntity(TileTank.class, "net.minecraft.src.buildcraft.factory.TileTank");
        ModLoader.registerTileEntity(TileGenericPipe.class, "net.minecraft.src.buildcraft.transport.GenericPipe");
        ModLoader.registerTileEntity(TileDeploy.class, "RPDeploy");
        ModLoader.registerTileEntity(TileCollector.class, "Mk1Collector");
        ModLoader.registerTileEntity(TileEntityComputer.getComputerClass(), "computer");
        ModLoader.registerTileEntity(TileHopper.class, "ZTO probe hopper");
        if (ic2) registerIc2();
    }

    private static void registerIc2() {
        new BlockMachine(IC2);
        ModLoader.registerTileEntity(TileEntityIronFurnace.class, "Iron Furnace");
        ModLoader.registerTileEntity(TileEntityMacerator.class, "Macerator");
    }

    /** With no vanilla utility, open construction qualifies through equipment alone. */
    static void mixedEquipment(SettlementProbe probe, int x, int y, int z) {
        xd world = probe.world;
        install(world, x, y, z, IC2, 3, TileEntityMacerator.class);
        install(world, x + 1, y, z, TANK, 0, TileTank.class);
        install(world, x + 2, y, z, REDPOWER, 0, TileDeploy.class);
        install(world, x + 3, y, z, RAILCRAFT, EnumUtility.ROLLING_MACHINE.getId(), TileRollingMachine.class);
        install(world, x + 4, y, z, COMPUTER, 3, TileEntityComputer.class);
        install(world, x + 2, y, z + 1, HOPPER, 0, TileHopper.class);
        install(world, x + 1, y, z + 1, EE2, 0, TileCollector.class);
        install(world, x, y, z + 1, IC2, 1, TileEntityIronFurnace.class);
    }

    static int[][] entranceEquipment(SettlementProbe probe, int x, int y, int z) {
        xd world = probe.world;
        int cable = new BlockCable(228).bO;
        ModLoader.registerTileEntity(TileEntityCable.class, "Cable");
        int[][] cells = {
            {x, y, z - 1}, {x - 1, y, z - 1}, {x + 1, y, z - 1}, {x + 1, y, z + 1},
            {x + 2, y, z + 1}, {x + 2, y, z - 1}, {x + 1, y, z + 2}
        };
        install(world, x, y, z - 1, cable, 0, TileEntityCable.class);
        probe.place(pipeItem.bQ, x - 1, y - 1, z - 1, 1);
        check(world.b(x - 1, y, z - 1) instanceof TileGenericPipe, "native BuildCraft pipe", null);
        install(world, x + 1, y, z - 1, IC2, 3, TileEntityMacerator.class);
        ((TileEntityMacerator) world.b(x + 1, y, z - 1)).energy = 137;
        install(world, x + 1, y, z + 1, TANK, 0, TileTank.class);
        TileTank tank = (TileTank) world.b(x + 1, y, z + 1);
        tank.liquidId = 9;
        tank.stored = 3750;
        install(world, x + 2, y, z + 1, HOPPER, 0, TileHopper.class);
        world.g(x + 2, y, z - 1, 54);
        ((io) world.b(x + 2, y, z - 1)).a(0, new aan(265, 11, 0));
        ((io) world.b(x + 2, y, z + 1)).a(0, new aan(263, 9, 0));
        install(world, x + 1, y, z + 2, DEVICE, 8, FixtureController.class);
        return cells;
    }

    static void installMacerator(xd world, int x, int y, int z) {
        install(world, x, y, z, IC2, 3, TileEntityMacerator.class);
    }

    private static void install(xd world, int x, int y, int z, int id, int meta, Class<? extends kw> type) {
        check(world.d(x, y, z, id, meta) && type.isInstance(world.b(x, y, z)),
                "native placement and tile " + type.getSimpleName(), null);
    }

    private void futureMod() throws Exception {
        open("future-observed", false);
        prepare(X, Z);
        platform(X, FEET, Z, MASONRY, true);
        world.g(X + 4, FEET - 1, Z + 4, 0);
        place(DEVICE, X + 4, FEET - 2, Z + 4, 1); // This metadata variant is ordinary construction.
        install(world, X + 1, FEET, Z + 1, DEVICE, 7, FixtureInventory.class);
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(560);
        Snapshot quiet = here();
        check(quiet.placed == 25 && quiet.generic.associated == 1 && quiet.inventories == 1
                && quiet.id == 0 && quiet.cover == 0, "unknown native placement and inventory supply distinct evidence", quiet);
        useUtility(X + 1, FEET, Z + 1);
        ticks(110);
        Snapshot observed = recognized("future mod uses native placement, metadata-dependent tile and activation", true);
        check(observed.generic.opened == 1 && observed.uses == 1 && observed.connections == 0,
                "future inventory contributes without specialized evidence", observed);

        open("future-unlogged", false);
        prepare(X, Z);
        platform(X, FEET, Z, MASONRY, false);
        for (int dx = 0; dx < 5; dx++)
            for (int dz = 0; dz < 5; dz++) world.g(X + dx, FEET - 2, Z + dz, 1);
        install(world, X + 1, FEET, Z + 1, DEVICE, 7, FixtureInventory.class);
        install(world, X + 2, FEET, Z + 1, DEVICE, 8, FixtureController.class);
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(560);
        useUtility(X + 1, FEET, Z + 1);
        ticks(25);
        useUtility(X + 1, FEET, Z + 1);
        ticks(110);
        Snapshot repeated = here();
        check(repeated.id == 0 && repeated.generic.opened == 1 && repeated.contextual == 25,
                "repeated clicks on one unchanged device cannot promote an unlogged platform", repeated);
        useUtility(X + 2, FEET, Z + 1);
        ticks(110);
        Snapshot old = recognized("old platform with distinct native device use", false);
        check(old.generic.opened == 2 && old.genericTypes == 2 && old.placed == 0 && old.inferred == 25
                        && old.generic.associated == 2 && old.inventories == 1,
                "unknown full machine can contribute without an inventory", old);
        save();
        open("future-unlogged", false);
        prepare(X, Z);
        position(X + 2, FEET, Z + 2);
        ticks(110);
        check(here().id == old.id && here().siteCount == 1 && here().generic.opened == 0
                        && settlements.resolve(world, old.id).envelope.toString().equals(old.boundary),
                "legacy inference saves identity and clears session-local use", here());
    }

    private void mixedFactory() throws Exception {
        open("mixed-factory-and-outpost", false);
        prepare(X, Z);
        platform(X, FEET, Z, 5, true);
        mixedEquipment(this, X, FEET, Z);
        position(X + 2, FEET, Z + 2);
        walk();
        for (int i = 0; i < 560 && here().id == 0; i++) ticks(1);
        Snapshot mixed = recognized("mixed Tekkit factory with no vanilla utility or adapter connection", true);
        printEvidence(mixed);
        check(mixed.ic2.associated == 1 && mixed.unsupportedIndustrial == 1 && mixed.generic.associated == 8
                        && mixed.genericTypes == 8 && mixed.inventories >= 4 && mixed.supports >= 2
                        && mixed.connections == 0 && mixed.cover == 0 && mixed.uses == 0 && mixed.peakReads <= 169
                        && mixed.reason.contains("local equipment"), "all seven families contribute generically, including unsupported IC2", mixed);
        world.g(X + 5, FEET, Z + 3, 52);
        useUtility(X + 2, FEET, Z + 1);
        ticks(25);
        useUtility(X + 4, FEET, Z);
        ((TileEntityComputer) world.b(X + 4, FEET, Z)).shutdown();
        ticks(110);
        check(here().generic.opened == 2 && here().generic.found == 9 && here().generic.associated == 8
                        && settlements.locate(world, X + 2, FEET, Z + 2).site.id == mixed.id,
                "ZTO container and ComputerCraft screen count while a nearby unrelated tile stays unlinked", here());

        int distant = X + 32;
        prepare(distant, Z);
        platform(distant, FEET, Z, 5, true);
        mixedEquipment(this, distant, FEET, Z);
        pipe(X + 3, FEET, Z + 1);
        for (int x = X + 3; x <= distant + 1; x++) pipe(x, FEET, Z + 2);
        position(distant + 2, FEET, Z + 2);
        walk();
        ticks(560);
        Snapshot outpost = recognized("outpost linked only by a long BuildCraft pipe run", true);
        SiteSnapshot first = settlements.resolve(world, mixed.id);
        check(outpost.id != mixed.id && outpost.siteCount == 2 && outpost.verified == 25
                        && first.envelope.toString().equals(mixed.boundary)
                        && outpost.generic.found > outpost.generic.associated && outpost.connections == 0,
                "pipe run cannot join or enlarge independent sites", outpost);
        check(settlements.locate(world, X + 16, FEET, Z + 2).site == null,
                "intervening pipe has no site membership", outpost);
        save();
        open("mixed-factory-and-outpost", false);
        prepare(X, Z);
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(110);
        check(here().id == mixed.id && here().siteCount == 2 && here().generic.associated >= 8
                        && settlements.resolve(world, mixed.id).coverage == Coverage.COMPLETE
                        && settlements.resolve(world, outpost.id).envelope.toString().equals(outpost.boundary),
                "both sites retain their original bounds and no duplicates after reload", here());

        open("old-mixed-factory", false);
        prepare(X, Z);
        platform(X, FEET, Z, 5, false);
        mixedEquipment(this, X, FEET, Z);
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(560);
        check(here().id == 0 && here().inferred == 25 && here().generic.associated == 8,
                "unlogged mixed factory needs player use", here());
        useUtility(X + 2, FEET, Z + 1);
        ticks(25);
        useUtility(X, FEET, Z + 1);
        ticks(110);
        Snapshot unlogged = recognized("unlogged mixed factory after two real native container openings", false);
        check(unlogged.placed == 0 && unlogged.generic.opened == 2 && unlogged.generic.associated == 8,
                "old mixed factory promotes without rebuilding equipment", unlogged);
    }

    private void coveredFactory() throws Exception {
        open("minimal-cabin-to-mixed-factory", false);
        prepare(X, Z);
        platform(X, FEET, Z, 5, true);
        for (int x = X - 1; x <= X + 5; x++) {
            for (int z = Z - 1; z <= Z + 5; z++) {
                if (x == X - 1 || x == X + 5 || z == Z - 1 || z == Z + 5) {
                    for (int y = FEET - 1; y < FEET + 3; y++) world.g(x, y, z, 5);
                }
                world.g(x, FEET + 3, z, 5);
            }
        }
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(560);
        Snapshot cabin = recognized("wooden cabin without machinery or utility", true);
        check(cabin.uses == 0 && cabin.utilities == 0 && cabin.cover >= 4
                        && settlements.protection(world, X + 2, FEET, Z + 2).state == State.PROTECTED,
                "native wood and geometry preserve a technical-light cabin", cabin);
        mixedEquipment(this, X, FEET, Z);
        ticks(110);
        Snapshot fitted = here();
        Assessment geometry = settlements.protection(world, X + 2, FEET, Z + 2);
        check(fitted.id == cabin.id && fitted.boundary.equals(cabin.boundary) && fitted.generic.associated == 8
                        && geometry.overhead == Evidence.YES && geometry.state == State.UNKNOWN
                        && "Ambiguous collision geometry".equals(geometry.reason),
                "mixed factory preserves the cabin footprint and roof, with conservative collision: " + geometry.reason, fitted);
        printEvidence(fitted);
    }

    private static void printEvidence(Snapshot snapshot) {
        System.out.printf("EVIDENCE equipment found/associated %d/%d (%d types, %d inventories, %d supporting); "
                        + "generic / IC2 detail reads %d/%d, peak %d; IC2 endpoints %d, paths %d, outside profile %d%n",
                snapshot.generic.found, snapshot.generic.associated, snapshot.genericTypes, snapshot.inventories,
                snapshot.supports, snapshot.genericReads, snapshot.ic2Reads, snapshot.peakReads,
                snapshot.ic2.associated, snapshot.connections, snapshot.unsupportedIndustrial);
    }

    private void negativeControls() throws Exception {
        open("modded-negative-controls", false);
        prepare(X, Z);
        platform(X, FEET, Z, MASONRY, false);
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(560);
        check(here().id == 0 && here().placed == 0 && here().generic.found == 0,
                "unfamiliar inert blocks without use or equipment do not establish a site", here());
        install(world, X, FEET, Z, DEVICE, 7, FixtureInventory.class);
        pipe(X + 4, FEET, Z + 4);
        // Raw generation-style placement supplies no player provenance.
        for (int i = 0; i < 5; i++) {
            world.d(X + i, FEET - 2, Z - 1, ORE, i);
            world.d(X + i, FEET + 2, Z + 4, LEAVES, 0);
            world.d(X + i, FEET + 2, Z + 5, OIL, 0);
        }
        world.g(X + 5, FEET, Z + 3, 52); // Native dungeon spawn tile.
        position(X + 2, FEET, Z + 2);
        walk();
        ticks(560);
        Snapshot quiet = here();
        printEvidence(quiet);
        check(quiet.id == 0 && quiet.placed == 0 && quiet.generic.found == 3 && quiet.generic.associated == 1
                        && quiet.connections == 0, "an unused device, unrelated spawner, conduit, ores and foliage cannot establish a site", quiet);
        world.hideChunk(X >> 4, Z >> 4);
        world.auditChunks = true;
        ticks(110);
        check(world.chunkMisses == 0 && here().id == 0 && "Terrain unavailable".equals(here().assessment),
                "a chunk containing devices can go unavailable without retrieval", here());

        int x = X + 40;
        world.auditChunks = false;
        prepare(x, Z);
        world.g(x, Y - 1, Z, 2);
        check(new WorldGenRubTree().grow(world, x, Y, Z, new Random(29L)) && world.a(x, Y, Z) == RUBBER_WOOD,
                "native IC2 rubber tree generation", here());
        position(x + 2, Y, Z + 2);
        walk();
        ticks(560);
        check(here().id == 0 && here().placed == 0, "native modded vegetation supplies no observed construction", here());
        System.out.println("PASS inert blocks, modded ore and oil, generated rubber tree, isolated pipe and device, unrelated spawner and missing terrain");
    }

    static void platform(SettlementProbe probe, int x, int y, int z, int id, boolean observed) {
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                if (observed) {
                    probe.world.g(x + dx, y - 2, z + dz, 1);
                    probe.world.g(x + dx, y - 1, z + dz, 0);
                    probe.place(id, x + dx, y - 2, z + dz, 1);
                } else {
                    probe.world.g(x + dx, y - 1, z + dz, id);
                }
            }
        }
    }

    private void platform(int x, int y, int z, int id, boolean observed) {
        platform(this, x, y, z, id, observed);
    }

    private void pipe(int x, int y, int z) {
        if (world.a(x, y - 1, z) == 0) world.g(x, y - 1, z, 1);
        place(pipeItem.bQ, x, y - 1, z, 1);
        check(world.b(x, y, z) instanceof TileGenericPipe, "native custom ItemPipe placement", null);
    }

    private void profile() throws Exception {
        open("generic-profile", false);
        prepare(X, Z);
        platform(X, FEET, Z, MASONRY, true);
        position(X + 2, FEET, Z + 2);
        for (int z = 0; z < 4; z++) {
            for (int x = 0; x < 5; x++) {
                if (x == 2 && z == 2) continue;
                if ((x + z) % 3 == 0) install(world, X + x, FEET, Z + z, TANK, 0, TileTank.class);
                else install(world, X + x, FEET, Z + z, HOPPER, 0, TileHopper.class);
            }
        }
        world.auditChunks = true;
        recognitionTiming("native mixed equipment, generic only");
        check(here().genericReads == 50 && here().ic2Reads == 0 && !here().ic2Available && world.chunkMisses == 0,
                "separate generic-only read budget and no adapter dependency", here());
        printEvidence(here());
        // Existing adapter configuration was resolved without IC2. A newly registered machine still has generic evidence.
        registerIc2();
        install(world, X + 2, FEET, Z + 4, IC2, 3, TileEntityMacerator.class);
        ticks(110);
        check(!here().ic2Available && here().ic2Reads == 0 && here().generic.associated == 20,
                "real IC2 machine supplies evidence with the specialized adapter unavailable", here());
        System.out.println("PASS real IC2 machine recognized generically with adapter unavailable");
    }

    private static final class Masonry extends pb {
        Masonry() { super(MASONRY, 1, acn.e); }
    }

    /** Uses a Forge metadata-dependent tile mapping, with no BlockContainer ancestry. */
    private static final class Device extends pb {
        Device() { super(DEVICE, 1, acn.e); }
        @Override public boolean hasTileEntity(int metadata) { return metadata == 7 || metadata == 8; }
        @Override public kw getTileEntity(int metadata) {
            return metadata == 7 ? new FixtureInventory() : metadata == 8 ? new FixtureController() : null;
        }
        @Override public boolean b(xd world, int x, int y, int z, yw player) {
            kw tile = world.b(x, y, z);
            if (tile instanceof FixtureInventory) player.a((io) tile);
            else if (tile instanceof FixtureController) player.c(x, y, z);
            else return false;
            return true;
        }
    }

    public static final class FixtureInventory extends hb {}
    public static final class FixtureController extends kw {}
}
