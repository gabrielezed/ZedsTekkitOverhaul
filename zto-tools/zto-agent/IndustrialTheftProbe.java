import buildcraft.api.EntityPassiveItem;
import buildcraft.api.Orientations;
import buildcraft.api.IPowerReceptor;
import buildcraft.api.PowerFramework;
import buildcraft.api.PowerProvider;
import buildcraft.energy.PneumaticPowerFramework;
import buildcraft.energy.PneumaticPowerProvider;
import buildcraft.factory.BlockTank;
import buildcraft.factory.TileTank;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.PipeTransportLiquids;
import buildcraft.transport.PipeTransportPower;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.pipes.PipeItemsCobblestone;
import buildcraft.transport.pipes.PipeItemsStone;
import buildcraft.transport.pipes.PipeLiquidsStone;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.FMLRegistry;
import eloraam.base.BlockMicro;
import eloraam.base.ItemMicro;
import eloraam.control.TileRibbon;
import eloraam.control.MicroPlacementRibbon;
import eloraam.core.CoverLib;
import eloraam.core.RedPowerLib;
import eloraam.core.TileCovered;
import eloraam.core.TubeLib;
import eloraam.core.TubeItem;
import eloraam.machine.MicroPlacementTube;
import eloraam.machine.TileMagTube;
import eloraam.machine.TilePipe;
import eloraam.machine.TileRedstoneTube;
import eloraam.machine.TileRestrictTube;
import eloraam.machine.TileTube;
import eloraam.wiring.MicroPlacementWire;
import eloraam.wiring.MicroPlacementJacket;
import eloraam.wiring.TileBluewire;
import eloraam.wiring.TileCable;
import eloraam.wiring.TileInsulatedWire;
import eloraam.wiring.TileRedwire;
import eloraam.wiring.TileWiring;
import ic2.api.Direction;
import ic2.api.IEnergySink;
import ic2.api.IEnergySource;
import ic2.common.BlockCable;
import ic2.common.EnergyNet;
import ic2.common.ItemCable;
import ic2.common.TileEntityCable;
import ic2.common.TileEntityCableDetector;
import ic2.common.TileEntityCableSplitter;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Random;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.hopper.BlockHopper;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.survival.tactical.EntitySaboteur;
import zed.tekkitoverhaul.survival.tactical.EnderkinEcology;
import zed.tekkitoverhaul.survival.tactical.client.EnderTrail;

/** Pinned native pipe teardown, cargo recovery and collision-aware trail on disposable terrain. */
public final class IndustrialTheftProbe {
    private static final int PIPE_BLOCK = 166;
    public static void main(String[] args) throws Exception {
        try {
            run(new File(args[0]));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0); // Anvil's native IO thread lives as long as the Java process.
    }

    private static void verifyBuildCraft(TestWorld world, yw player, Method candidate) throws Exception {
        PowerFramework.currentFramework = new PneumaticPowerFramework();
        String[][] stock = {
            {"ItemsWood", "4050"}, {"ItemsCobblestone", "4051"}, {"ItemsStone", "4052"},
            {"ItemsIron", "4053"}, {"ItemsGold", "4054"}, {"ItemsDiamond", "4055"},
            {"ItemsObsidian", "4056"}, {"LiquidsWood", "4057"}, {"LiquidsCobblestone", "4058"},
            {"LiquidsStone", "4059"}, {"LiquidsIron", "4060"}, {"LiquidsGold", "4061"},
            {"PowerWood", "4064"}, {"PowerStone", "4066"}, {"PowerGold", "4068"}
        };
        for (String[] entry : stock)
            verifyPipe(world, player, Class.forName("buildcraft.transport.pipes.Pipe" + entry[0]),
                    Integer.parseInt(entry[1]), candidate);
        String[][] additional = {
            {"ItemsAdvancedWood", "4045"}, {"ItemsAdvancedInsertion", "4044"}, {"ItemsDistributor", "4046"},
            {"ItemsRedstone", "4043"}, {"LiquidsRedstone", "4042"}
        };
        for (String[] entry : additional)
            verifyPipe(world, player, Class.forName("buildcraft.additionalpipes.pipes.Pipe" + entry[0]),
                    Integer.parseInt(entry[1]), candidate);
        String[] teleports = {"ItemTeleport", "LiquidsTeleport", "PowerTeleport"};
        for (int i = 0; i < teleports.length; i++) {
            Class<?> type = Class.forName("buildcraft.additionalpipes.pipes.Pipe" + teleports[i]);
            int item = BlockGenericPipe.registerPipe(4047 + i, type).bQ;
            Pipe endpoint = placePipe(world, player, 312, item);
            check(candidate.invoke(null, endpoint.container) == null, "linked endpoint is out of scope: " + teleports[i]);
            world.g(312, 4, 289, 0);
        }
        verifyPipeThroughput(world, player);
        System.out.println("PASS native registered pipe teardown, item placement and topology for 15 BuildCraft and five Additional Pipes variants");
    }

    private static void verifyPipeThroughput(TestWorld world, yw player) throws Exception {
        int x = 312, z = 289, liquids = 4059 + 256, power = 4066 + 256;
        BlockTank tank = new BlockTank(165);
        PipeTransportLiquids target = (PipeTransportLiquids) placePipe(world, player, x, liquids).transport;
        PipeTransportLiquids neighbor = (PipeTransportLiquids) placePipe(world, player, x + 1, liquids).transport;
        world.g(x + 2, 4, z, tank.bO);
        TileTank receiver = (TileTank) world.b(x + 2, 4, z);
        EntitySaboteur actor = start(world, player, x - 1.5, z + 0.5);
        check(actor.pose() == EntitySaboteur.DISMANTLE
                && target.fill(Orientations.XNeg, 20, 9, true) == 20, "enqueue liquid during windup");
        act(actor, 31);
        check("UNSAFE_TRANSPORT".equals(actor.lastOutcome()), "incoming liquid cancels dismantling");
        for (int tick = 0; tick < 100; tick++) {
            nativeTicks(world, 1);
            check(receiver.stored + target.getLiquidQuantity() + neighbor.getLiquidQuantity() == 20,
                    "liquid is conserved in all seven buffers and the tank");
        }
        check(receiver.stored == 20 && receiver.liquidId == 9, "native liquid reaches the tank");
        actor = start(world, player, x - 1.5, z + 0.5);
        act(actor, 65);
        aan component = recovered(actor, liquids, 0);
        check(component.a(player, world, x, 3, z, 1), "reinstall the emptied liquid pipe");
        PipeTransportLiquids restored = (PipeTransportLiquids) ((TileGenericPipe) world.b(x, 4, z)).pipe.transport;
        check(restored.fill(Orientations.XNeg, 20, 9, true) == 20, "resume native fluid feed");
        nativeTicks(world, 100);
        check(receiver.stored == 40 && restored.getLiquidQuantity() + neighbor.getLiquidQuantity() == 0,
                "native liquid transfer resumes after reinstallation");
        for (int dx = 0; dx <= 2; dx++) world.g(x + dx, 4, z, 0);

        placePipe(world, player, x, power);
        placePipe(world, player, x + 1, power);
        world.g(x + 2, 4, z, 54);
        PowerSink sink = new PowerSink();
        world.a(x + 2, 4, z, sink);
        PipeTransportPower transport = (PipeTransportPower) ((TileGenericPipe) world.b(x, 4, z)).pipe.transport;
        sink.request = 100;
        nativeTicks(world, 6);
        transport.receiveEnergy(Orientations.XNeg, 100);
        nativeTicks(world, 10);
        check(sink.provider.energyStored == 98, "native power delivery accounts for two one-percent pipe losses");
        sink.request = 0;
        nativeTicks(world, 10);
        actor = start(world, player, x - 1.5, z + 0.5);
        act(actor, 65);
        component = recovered(actor, power, 0);
        check(sink.provider.energyStored == 98 && component.a(player, world, x, 3, z, 1),
                "disconnecting a quiescent branch preserves previously delivered power");
        transport = (PipeTransportPower) ((TileGenericPipe) world.b(x, 4, z)).pipe.transport;
        sink.request = 100;
        nativeTicks(world, 6);
        transport.receiveEnergy(Orientations.XNeg, 100);
        nativeTicks(world, 10);
        check(sink.provider.energyStored == 196, "native power transfers resume after reinstallation");
        for (int dx = 0; dx <= 2; dx++) world.g(x + dx, 4, z, 0);
        System.out.println("PASS native BuildCraft wet windup cancellation, 20 + 20 mB conservation and 98 + 98 power delivery after line repair");
    }

    private static void nativeTicks(TestWorld world, int count) {
        for (int tick = 0; tick < count; tick++) {
            world.a(world.w() + 1L);
            world.n();
        }
    }

    private static final class PowerSink extends hb implements IPowerReceptor {
        private PowerProvider provider = new PneumaticPowerProvider();
        int request;

        PowerSink() { provider.configure(1, 0, 1000, 1000, 1000); }
        public PowerProvider getPowerProvider() { return provider; }
        public void setPowerProvider(PowerProvider replacement) { provider = replacement; }
        public void doWork() {}
        public int powerRequest() { return request; }
    }

    @SuppressWarnings("unchecked")
    private static void verifyPipe(TestWorld world, yw player, Class<?> variant, int slot, Method candidate)
            throws Exception {
        int item = yr.e[slot + 256] != null ? slot + 256 : BlockGenericPipe.registerPipe(slot, variant).bQ;
        check(BlockGenericPipe.pipes.get(item) == variant, "registered variant " + variant.getSimpleName());
        // Install through ItemPipe's normal placement path, against a stone support.
        int x = 312;
        Pipe target = placePipe(world, player, x, item);
        int comparatorItem = variant.getSimpleName().contains("Cobblestone") ? item
                : target.transport instanceof PipeTransportPower ? 4066 + 256
                : target.transport instanceof PipeTransportLiquids ? 4059 + 256 : 4052 + 256;
        if (yr.e[comparatorItem] == null) {
            Class<?> comparator = target.transport instanceof PipeTransportPower
                    ? Class.forName("buildcraft.transport.pipes.PipePowerStone")
                    : target.transport instanceof PipeTransportLiquids ? PipeLiquidsStone.class : PipeItemsStone.class;
            BlockGenericPipe.registerPipe(comparatorItem - 256, comparator);
        }
        placePipe(world, player, x + 1, comparatorItem);
        placePipe(world, player, x + 3, comparatorItem);
        placePipe(world, player, x + 4, comparatorItem);
        BlockGenericPipe block = (BlockGenericPipe) pb.m[PIPE_BLOCK];
        check(block.isPipeConnected(world, x + 1, 4, 289, x, 4, 289), "native adjacency " + variant.getSimpleName());

        if (target.transport instanceof PipeTransportPower) {
            PipeTransportPower power = (PipeTransportPower) target.transport;
            power.internalNextPower[0] = 12;
            check(candidate.invoke(null, target.container) == null, "queued power blocks dismantle");
            power.internalNextPower[0] = 0;
            power.nextPowerQuery[0] = 7;
            check(candidate.invoke(null, target.container) == null, "pending power request blocks dismantle");
            power.nextPowerQuery[0] = 0;
        }
        if (variant.getSimpleName().equals("PipeItemsDiamond") || variant.getSimpleName().equals("PipeItemsAdvancedWood")) {
            Field filters = target.logic.getClass().getDeclaredField("items");
            filters.setAccessible(true);
            Object[] items = (Object[]) filters.get(target.logic);
            items[0] = new aan(4, 1, 0);
            check(candidate.invoke(null, target.container) == null && ((aan) items[0]).a == 1,
                    "stocked filters make the component ineligible");
            items[0] = null;
        }
        if (variant.getSimpleName().equals("PipeItemsDistributor")) {
            int[] weights = (int[]) target.logic.getClass().getField("distData").get(target.logic);
            weights[2] = 3;
            check(candidate.invoke(null, target.container) == null, "configured distribution weights stay installed");
            weights[2] = 1;
        }
        check(candidate.invoke(null, target.container) != null, "adapter admits empty " + variant.getSimpleName());
        EntitySaboteur actor = start(world, player, x - 1.5, 289.5);
        check(actor.pose() == EntitySaboteur.DISMANTLE, "acquires " + variant.getSimpleName() + " pose=" + actor.pose()
                + " target=" + actor.targetType() + " outcome=" + actor.lastOutcome());
        act(actor, 65);
        check(actor.pose() == EntitySaboteur.CARRY && world.a(x, 4, 289) == 0
                && !block.isPipeConnected(world, x + 1, 4, 289, x, 4, 289)
                && block.isPipeConnected(world, x + 3, 4, 289, x + 4, 4, 289), "isolated topology break " + variant.getSimpleName());
        aan recovered = recovered(actor, item, 0);
        check(recovered.a(player, world, x, 3, 289, 1), "reinstall " + variant.getSimpleName());
        check(block.isPipeConnected(world, x + 1, 4, 289, x, 4, 289), "restored topology " + variant.getSimpleName());
        for (int dx : new int[] {0, 1, 3, 4}) world.g(x + dx, 4, 289, 0);
    }

    private static Pipe placePipe(TestWorld world, yw player, int x, int item) {
        aan stack = new aan(item, 1, 0);
        check(stack.a(player, world, x, 3, 289, 1) && stack.a == 0, "native ItemPipe placement for " + item);
        return ((TileGenericPipe) world.b(x, 4, 289)).pipe;
    }

    private static void verifyIC2(TestWorld world, yw player, File root) throws Exception {
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        Minecraft game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja failure) { throw new AssertionError(failure); }
        };
        game.f = world;
        Field client = FMLClientHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(FMLClientHandler.instance(), game);
        FMLRegistry.registerRegistry(new ClientRegistry());
        BlockCable block = new BlockCable(228);
        ItemCable item = new ItemCable(29928, 0);
        int x = 312;
        for (int variant = 0; variant <= 12; variant++) {
            TileEntityCable cable = placeCable(world, player, x, 289, item.bQ, variant);
            check(world.a(x, 4, 289) == block.bO && cable.addedToEnergyNet, "native cable registration " + variant);
            if (variant == 11) {
                ((TileEntityCableDetector) cable).setActiveWithoutNotify(true);
                check(block.b((ali) world, x, 4, 289, 5), "active detector emits native redstone");
            } else if (variant == 12) {
                world.d(x, 3, 289, 76, 5);
                check(world.w(x, 4, 289), "splitter has a native redstone input");
                ((TileEntityCableSplitter) cable).n_();
                check(!cable.addedToEnergyNet, "powered splitter leaves EnergyNet before dismantling");
            }
            EntitySaboteur actor = start(world, player, x - 1.5, 289.5);
            check(actor.pose() == EntitySaboteur.DISMANTLE, "acquires cable " + variant);
            act(actor, 65);
            check(actor.pose() == EntitySaboteur.CARRY && cable.l() && !cable.addedToEnergyNet && world.a(x, 4, 289) == 0,
                    "native cable invalidation " + variant);
            aan recovered = recovered(actor, item.bQ, variant);
            if (variant == 12) world.g(x, 3, 289, 2);
            check(recovered.a(player, world, x, 3, 289, 1) && world.e(x, 4, 289) == variant,
                    "exact IC2 variant reinstalls " + variant);
            if (variant == 11) check(!block.b((ali) world, x, 4, 289, 5), "reinstalled detector starts inactive");
            world.g(x, 4, 289, 0);
        }
        TileEntityCable live = placeCable(world, player, x, 289, item.bQ, 9);
        placeCable(world, player, x - 1, 290, item.bQ, 9);
        placeCable(world, player, x, 290, item.bQ, 9);
        placeCable(world, player, x + 1, 290, item.bQ, 9);
        Source source = new Source();
        Sink sink = new Sink();
        world.g(x - 1, 4, 289, 54);
        world.g(x + 1, 4, 289, 54);
        world.a(x - 1, 4, 289, source);
        world.a(x + 1, 4, 289, sink);
        EnergyNet net = EnergyNet.getForWorld(world);
        check(net.emitEnergyFrom(source, 32) < 32 && sink.received > 0
                && net.getTotalEnergyConducted(live) > 0, "EU initially uses the short branch");
        EntitySaboteur first = start(world, player, x - 0.5, 288.5);
        EntitySaboteur second = start(world, player, x - 0.5, 288.5);
        check(first.pose() == EntitySaboteur.DISMANTLE && second.pose() == EntitySaboteur.DISMANTLE,
                "two actors contest a live cable");
        act(first, 65);
        act(second, 65);
        check(first.pose() == EntitySaboteur.CARRY && second.pose() == EntitySaboteur.CANCELLED
                && "TARGET_CHANGED".equals(second.lastOutcome()) && !live.addedToEnergyNet,
                "one winner and native EnergyNet invalidation");
        int before = sink.received;
        net.emitEnergyFrom(source, 32);
        check(sink.received > before, "the alternate IC2 route still delivers EU");
        aan recovered = recovered(first, item.bQ, 9);
        check(recovered.a(player, world, x, 3, 289, 1), "reinstall glass fibre");
        TileEntityCable restored = (TileEntityCable) world.b(x, 4, 289);
        restored.onCreated();
        net.emitEnergyFrom(source, 32);
        check(net.getTotalEnergyConducted(restored) > 0, "restored short branch carries EU");
        for (int dx = -1; dx <= 1; dx++) world.g(x + dx, 4, 290, 0);
        world.g(x - 1, 4, 289, 0);
        world.g(x + 1, 4, 289, 0);
        world.g(x, 4, 289, 0);
        TestWorld habitat = new TestWorld(root, "transport-habitat");
        yw resident = NaturalSpawnProbe.join(habitat);
        placeCable(habitat, resident, x, 289, item.bQ, 9);
        verifyNaturalCover(habitat, resident, x, 289, item.bQ);
        System.out.println("PASS all 13 IC2 cable variants, live EnergyNet alternate route, reconnection and contest; natural exposed cable selection");
    }

    private static TileEntityCable placeCable(TestWorld world, yw player, int x, int z, int item, int variant) {
        check(new aan(item, 1, variant).a(player, world, x, 3, z, 1), "native ItemCable placement " + variant);
        TileEntityCable cable = (TileEntityCable) world.b(x, 4, z);
        cable.onCreated();
        return cable;
    }

    private static void verifyNaturalCover(TestWorld world, yw player, int x, int z, int item) throws Exception {
        world.a(18000L);
        player.c(x - 5.5, 4, z + 5.5, 0, 0);
        ajg original = new ajg(world);
        original.c(x - 4.5, 4, z + 0.5, 0, 0);
        Random prior = world.r;
        world.r = new Random() {
            @Override public int nextInt(int bound) { return bound == 1000 ? 100 : super.nextInt(bound); }
        };
        acq selected = EnderkinEcology.select(original, 0);
        check(selected instanceof EntitySaboteur, "exposed transport-only habitat");
        for (int y = 4; y <= 5; y++)
            for (int dz = -1; dz <= 1; dz++) world.g(x - 2, y, z + dz, 1);
        check(EnderkinEcology.select(original, 0) == original, "same roll behind a solid wall");
        for (int y = 4; y <= 5; y++)
            for (int dz = -1; dz <= 1; dz++) world.g(x - 2, y, z + dz, 0);
        world.r = prior;
        EntitySaboteur natural = (EntitySaboteur) selected;
        randomizeActor(natural);
        Field scan = EntitySaboteur.class.getDeclaredField("nextScan");
        scan.setAccessible(true);
        scan.setInt(natural, 1);
        check(world.a(natural), "insert the naturally selected Saboteur");
        int windup = -1, theft = -1, escape = -1;
        for (int tick = 0; tick < 300 && escape < 0; tick++) {
            nativeTicks(world, 1);
            if (windup < 0 && natural.pose() == EntitySaboteur.DISMANTLE) windup = tick;
            if (theft < 0 && natural.pose() == EntitySaboteur.CARRY) theft = tick;
            if ("ESCAPE".equals(natural.recoveryOutcome())) escape = tick;
        }
        check(windup >= 0 && theft - windup == 35 && escape - theft >= 100 && escape - theft < 110
                && world.a(x, 4, z) == 0 && !natural.G, "native approach, 35-tick dismantle and one short escape: "
                + windup + "/" + theft + "/" + escape + " scans=" + natural.scans() + " pose=" + natural.pose()
                + " outcome=" + natural.lastOutcome());
        int recovered = 0;
        for (Object entity : world.b) {
            if (!(entity instanceof fq) || ((fq) entity).G) continue;
            aan stack = ((fq) entity).a;
            if (stack.c == item && stack.i() == 9) recovered += stack.a;
        }
        check(recovered == 1, "escape releases one exact component while the Saboteur is still alive");
        System.out.printf("Native cable encounter ticks: windup %d, teardown %d, escape recovery %d%n", windup, theft, escape);
    }

    private static final class Source extends hb implements IEnergySource {
        public boolean emitsEnergyTo(kw receiver, Direction direction) { return true; }
        public boolean isAddedToEnergyNet() { return true; }
        public int getMaxEnergyOutput() { return 32; }
    }

    private static final class Sink extends hb implements IEnergySink {
        int received;
        public boolean acceptsEnergyFrom(kw emitter, Direction direction) { return true; }
        public boolean isAddedToEnergyNet() { return true; }
        public boolean demandsEnergy() { return true; }
        public int injectEnergy(Direction direction, int amount) { received += amount; return 0; }
    }

    private static void verifyRedPower(TestWorld world, yw player) throws Exception {
        BlockMicro block = new BlockMicro(136);
        RedPowerBase.blockMicro = block;
        CoverLib.blockCoverPlate = block;
        RedPowerLib.addCompatibleMapping(0, 1);
        ItemMicro item = new ItemMicro(block.bO - 256);
        block.addTileEntityMapping(0, TileCovered.class);
        int[] types = {1, 2, 3, 5, 7, 8, 9, 10, 11, 12};
        Class<?>[] tiles = {TileRedwire.class, TileInsulatedWire.class, TileCable.class, TileBluewire.class,
            TilePipe.class, TileTube.class, TileRedstoneTube.class, TileRestrictTube.class, TileMagTube.class, TileRibbon.class};
        MicroPlacementWire wire = new MicroPlacementWire();
        MicroPlacementTube tube = new MicroPlacementTube();
        int x = 312, z = 289;
        for (int i = 0; i < types.length; i++) {
            int type = types[i];
            block.addTileEntityMapping(type, tiles[i]);
            block.registerPlacement(type, type == 12 ? new MicroPlacementRibbon() : type >= 7 ? tube : wire);
            int meta = type << 8 | (type == 2 ? 14 : type == 3 ? 8 : 0);
            check(new aan(item, 1, meta).a(player, world, x, 3, z, 1), "native micro placement " + type);
            check(tiles[i].isInstance(world.b(x, 4, z)), "native multipart tile " + type);
            check(new aan(item, 1, meta).a(player, world, x + 1, 3, z, 1), "adjacent multipart " + type);
            int connected = nativeConnections(world, x + 1, z, type);
            check(connected != 0, "native RedPower connection for " + type);
            EntitySaboteur actor = start(world, player, x - 1.5, z + 0.5);
            check(actor.pose() == EntitySaboteur.DISMANTLE, "acquires RedPower " + type);
            act(actor, 65);
            check(actor.pose() == EntitySaboteur.CARRY && world.a(x, 4, z) == 0
                    && nativeConnections(world, x + 1, z, type) == 0, "part gone and native route broken " + type);
            aan recovered = recovered(actor, item.bQ, meta);
            check(recovered.a(player, world, x, 3, z, 1)
                    && nativeConnections(world, x + 1, z, type) == connected, "native route restored " + type);
            world.g(x, 4, z, 0);
            world.g(x + 1, 4, z, 0);
        }
        verifyJackets(world, player, block, item, x, z);
        verifyWireColors(world, player, item, x, z);
        verifyTubeTraffic(world, player, item, x, z);
        verifyCoveredParts(world, player, item, x, z);
        verifyWireSignals(world, player, item, x, z);
        System.out.println("PASS all ten RedPower transport classes, native multipart harvest and exact item reconnect");
    }

    private static void verifyWireColors(TestWorld world, yw player, ItemMicro item, int x, int z) throws Exception {
        for (int type : new int[] {2, 3}) {
            int variants = type == 2 ? 16 : 17;
            for (int color = 0; color < variants; color++) {
                int meta = type << 8 | color;
                check(new aan(item, 1, meta).a(player, world, x, 3, z, 1), "place a native wire color");
                EntitySaboteur actor = start(world, player, x - 1.5, z + 0.5);
                check(actor.pose() == EntitySaboteur.DISMANTLE, "acquire an exposed wire color");
                act(actor, 65);
                check(world.a(x, 4, z) == 0, "wire color was removed");
                aan recovered = recovered(actor, item.bQ, meta);
                check(recovered.a(player, world, x, 3, z, 1)
                        && ((TileWiring) world.b(x, 4, z)).Metadata == color, "original wire color restored");
                world.g(x, 4, z, 0);
            }
        }
    }

    private static void verifyJackets(TestWorld world, yw player, BlockMicro block, ItemMicro item, int x, int z)
            throws Exception {
        MicroPlacementJacket placement = new MicroPlacementJacket();
        for (int variant = 64; variant <= 66; variant++) {
            block.registerPlacement(variant, placement);
            int meta = variant << 8;
            check(new aan(item, 1, meta).a(player, world, x, 3, z, 1), "native jacketed wire placement");
            TileWiring wiring = (TileWiring) world.b(x, 4, z);
            check((wiring.ConSides & 64) != 0, "registered center part");
            EntitySaboteur actor = start(world, player, x - 1.5, z + 0.5);
            check(actor.pose() == EntitySaboteur.DISMANTLE, "select jacketed wire center");
            act(actor, 65);
            aan recovered = recovered(actor, item.bQ, meta);
            check(recovered.a(player, world, x, 3, z, 1) && world.b(x, 4, z) != wiring,
                    "reinstall exact jacketed part");
            world.g(x, 4, z, 0);
        }
    }

    private static void verifyWireSignals(TestWorld world, yw player, ItemMicro item, int x, int z) throws Exception {
        int redwire = 1 << 8;
        check(new aan(item, 1, redwire).a(player, world, x, 3, z, 1), "signal route target wire");
        check(new aan(item, 1, redwire).a(player, world, x + 1, 3, z, 1), "signal route receiver wire");
        TileRedwire receiver = (TileRedwire) world.b(x + 1, 4, z);
        world.d(x - 1, 4, z, 76, 5);
        nativeTicks(world, 20);
        check(receiver.PowerState > 0, "native red alloy signal is live");
        EntitySaboteur actor = start(world, player, x + 0.5, z + 2.0);
        check(actor.pose() == EntitySaboteur.DISMANTLE, "acquires powered red alloy wire");
        act(actor, 65);
        nativeTicks(world, 20);
        check(receiver.PowerState == 0, "signal drops only beyond the removed wire");
        aan component = recovered(actor, item.bQ, redwire);
        check(component.a(player, world, x, 3, z, 1), "restore red alloy wire");
        nativeTicks(world, 20);
        check(receiver.PowerState > 0, "native red alloy signal returns after placement");
        for (int dx = -1; dx <= 1; dx++) world.g(x + dx, 4, z, 0);
    }

    private static void verifyTubeTraffic(TestWorld world, yw player, ItemMicro item, int x, int z) throws Exception {
        int meta = 8 << 8;
        check(new aan(item, 1, meta).a(player, world, x, 3, z, 1), "place tube awaiting cargo");
        check(new aan(item, 1, meta).a(player, world, x + 1, 3, z, 1), "place tube routing to chest");
        world.g(x + 2, 4, z, 54);
        TileTube tube = (TileTube) world.b(x, 4, z);
        tube.paintColor = 3;
        EntitySaboteur painted = start(world, player, x - 1.5, z + 0.5);
        check(painted.disruptionTarget() != tube, "painted routing channel remains installed");
        tube.paintColor = 0;
        EntitySaboteur actor = start(world, player, x - 1.5, z + 0.5);
        check(actor.pose() == EntitySaboteur.DISMANTLE, "starts dismantling the empty pneumatic tube");
        check(tube.tubeItemEnter(4, 0, new TubeItem(4, new aan(4, 5, 0))), "insert five native tube items");
        act(actor, 31);
        check("UNSAFE_TRANSPORT".equals(actor.lastOutcome()) && actor.pose() == EntitySaboteur.CANCELLED,
                "new tube cargo cancels the windup");
        for (int tick = 0; tick < 40; tick++) {
            tube.n_();
            ((TileTube) world.b(x + 1, 4, z)).n_();
        }
        hb receiver = (hb) world.b(x + 2, 4, z);
        check(receiver.k_(0).a == 5 && tube.getTubeFlow().contents.isEmpty()
                && ((TileTube) world.b(x + 1, 4, z)).getTubeFlow().contents.isEmpty(),
                "all five native tube items arrive and both queues empty");
        actor = start(world, player, x - 1.5, z + 0.5);
        check(actor.pose() == EntitySaboteur.DISMANTLE, "tube is eligible again after its queue clears");
        act(actor, 65);
        aan recovered = recovered(actor, item.bQ, meta);
        check(recovered.a(player, world, x, 3, z, 1), "reinstall the emptied tube");
        tube = (TileTube) world.b(x, 4, z);
        check(tube.tubeItemEnter(4, 0, new TubeItem(4, new aan(4, 3, 0))), "insert after reconnection");
        for (int tick = 0; tick < 40; tick++) {
            tube.n_();
            ((TileTube) world.b(x + 1, 4, z)).n_();
        }
        check(receiver.k_(0).a == 8, "three more items arrive after native reinstallation");
        for (int dx = 0; dx <= 2; dx++) world.g(x + dx, 4, z, 0);
        System.out.println("PASS pneumatic tube arrival cancels windup and both native runs reconcile 5 + 3 items");
    }

    private static void verifyCoveredParts(TestWorld world, yw player, ItemMicro item, int x, int z) throws Exception {
        int meta = 8 << 8;
        check(new aan(item, 1, meta).a(player, world, x, 3, z, 1), "place tube to cover");
        TileTube covered = (TileTube) world.b(x, 4, z);
        check(covered.tryAddCover(4, 0), "native west-face cover");
        check(new aan(item, 1, meta).a(player, world, x, 3, z + 1, 1), "place exposed neighbor");
        EntitySaboteur actor = start(world, player, x - 1.25, z + 0.85);
        check(actor.pose() == EntitySaboteur.DISMANTLE && actor.disruptionTarget() == world.b(x, 4, z + 1),
                "chooses the exposed neighbor when the closer tube face is covered");
        act(actor, 65);
        check(world.b(x, 4, z) == covered && covered.CoverSides == 1 << 4 && world.a(x, 4, z + 1) == 0,
                "only the exposed tube disappears");
        recovered(actor, item.bQ, meta);
        world.g(x, 4, z, 0);

        check(new aan(item, 1, meta).a(player, world, x, 3, z, 1), "place tube for mid-windup cover");
        TileTube tube = (TileTube) world.b(x, 4, z);
        actor = start(world, player, x - 1.5, z + 0.5);
        check(actor.pose() == EntitySaboteur.DISMANTLE && tube.tryAddCover(4, 0),
                "close the exact face during its windup");
        act(actor, 31);
        check(actor.pose() == EntitySaboteur.CANCELLED && "NO_SIGHT".equals(actor.lastOutcome())
                && world.b(x, 4, z) == tube && tube.CoverSides == 1 << 4, "cover cancels a committed attempt");
        world.g(x, 4, z, 0);

        int redwire = 1 << 8;
        world.g(x + 1, 4, z, 1);
        check(new aan(item, 1, redwire).a(player, world, x, 3, z, 1), "floor wire face");
        check(new aan(item, 1, redwire).a(player, world, x + 1, 4, z, 4), "east wire face on the same tile");
        TileWiring wiring = (TileWiring) world.b(x, 4, z);
        check(wiring.tryAddCover(2, 0), "native north cover");
        actor = start(world, player, x - 1.5, z + 0.5);
        check(actor.pose() == EntitySaboteur.DISMANTLE && actor.disruptionTarget() == wiring,
                "selects the reachable floor face");
        int previousSides = wiring.ConSides;
        act(actor, 65);
        check(world.b(x, 4, z) == wiring && wiring.ConSides == (previousSides & ~1) && wiring.CoverSides == 1 << 2,
                "native harvest preserves the east wire face and north cover");
        aan recovered = recovered(actor, item.bQ, redwire);
        check(recovered.a(player, world, x, 3, z, 1) && wiring.ConSides == previousSides,
                "native placement restores only the removed face");
        world.g(x, 4, z, 0);
        world.g(x + 1, 4, z, 0);
        System.out.println("PASS covered RedPower tube and unaffected neighbor face, cover and exact wire part recovery");
    }

    private static int nativeConnections(TestWorld world, int x, int z, int type) {
        kw tile = world.b(x, 4, z);
        if (type == 7) {
            TilePipe pipe = (TilePipe) tile;
            pipe.cacheCon();
            return pipe.ConCache;
        }
        return type >= 8 && type <= 11 ? TubeLib.getConnections(world, x, 4, z) : ((TileWiring) tile).getConnectionMask();
    }

    private static aan recovered(EntitySaboteur actor, int item, int metadata) throws Exception {
        // Exercise death recovery and retain the emitted stack as the sole item used for reinstallation.
        Field cargo = EntitySaboteur.class.getDeclaredField("stolenComponent");
        cargo.setAccessible(true);
        aan expected = (aan) cargo.get(actor);
        check(expected != null && expected.a == 1 && expected.c == item && expected.i() == metadata,
                "exact recoverable stack " + item + ":" + metadata);
        actor.A();
        check("DEATH".equals(actor.recoveryOutcome()) && cargo.get(actor) == null, "death clears carried ownership");
        fq drop = null;
        for (Object entity : actor.k.b) {
            if (!(entity instanceof fq) || ((fq) entity).G || ((fq) entity).a != expected) continue;
            check(drop == null, "the exact cargo is only emitted once");
            drop = (fq) entity;
        }
        check(drop != null, "the exact cargo was emitted");
        drop.A();
        return expected;
    }

    @SuppressWarnings("unchecked")
    private static void run(File root) throws Exception {
        Method register = aao.class.getDeclaredMethod("a", Class.class, String.class, int.class);
        register.setAccessible(true);
        register.invoke(null, EntitySaboteur.class, "ZTO.Saboteur", 209);
        TestWorld world = new TestWorld(root);
        yw player = NaturalSpawnProbe.join(world);
        player.c(289, 4, 289.5, 0, 0);
        BlockGenericPipe block = new BlockGenericPipe(PIPE_BLOCK);
        BuildCraftTransport.genericPipeBlock = block;
        int stone = BlockGenericPipe.registerPipe(22311, PipeItemsStone.class).bQ;
        int cobble = BlockGenericPipe.registerPipe(22312, PipeItemsCobblestone.class).bQ;
        int fluid = BlockGenericPipe.registerPipe(22313, PipeLiquidsStone.class).bQ;
        Pipe target = install(world, 292, stone);
        install(world, 293, stone);
        world.g(294, 4, 289, 54);
        hb receiver = (hb) world.b(294, 4, 289);
        Pipe separate = install(world, 299, cobble);
        Pipe liquid = install(world, 303, fluid);
        check(block.isPipeConnected(world, 293, 4, 289, 292, 4, 289), "native connection");

        Method candidate = Class.forName("zed.tekkitoverhaul.survival.tactical.BuildCraftPipeTheft")
                .getDeclaredMethod("find", kw.class);
        candidate.setAccessible(true);
        check(candidate.invoke(null, separate.container) != null, "empty cobblestone pipe is supported");
        check(candidate.invoke(null, liquid.container) != null, "empty liquid transport supports physical theft");
        PipeTransportLiquids buffered = (PipeTransportLiquids) liquid.transport;
        buffered.fill(Orientations.XPos, 11, 9, true);
        check(buffered.getLiquidQuantity() == 11 && candidate.invoke(null, liquid.container) == null,
                "liquid in a travel queue stays in the pipe");
        world.g(303, 4, 289, 0);
        PipeTransportItems transport = (PipeTransportItems) target.transport;
        Field pending = PipeTransportItems.class.getDeclaredField("entitiesToLoad");
        pending.setAccessible(true);
        Collection<Object> queued = (Collection<Object>) pending.get(transport);
        queued.add(null);
        check(candidate.invoke(null, target.container) == null, "cargo awaiting the first tick after reload makes pipe ineligible");
        queued.clear();

        EntitySaboteur contested = start(world, player);
        check(contested.pose() == EntitySaboteur.DISMANTLE, "pipe is empty when dismantling starts");
        EntityPassiveItem cargo = new EntityPassiveItem(world, 292.1, 4.25, 289.5, new aan(4, 5, 0));
        transport.entityEntering(cargo, Orientations.XPos);
        check(candidate.invoke(null, target.container) == null, "in-flight payload makes pipe ineligible");
        act(contested, 65);
        check(contested.pose() == EntitySaboteur.CANCELLED && world.a(292, 4, 289) == PIPE_BLOCK,
                "incoming transport interrupts the windup at contact");
        for (int tick = 0; tick < 220; tick++) world.n();
        int delivered = 0;
        for (int slot = 0; slot < receiver.a(); slot++) {
            aan stack = receiver.k_(slot);
            if (stack != null && stack.c == 4) delivered += stack.a;
        }
        check(delivered == 5 && world.b(fq.class) == 0, "native transport delivers all five payload items to the chest");

        EntitySaboteur interrupted = start(world, player);
        check(interrupted.pose() == EntitySaboteur.DISMANTLE && world.a(292, 4, 289) == PIPE_BLOCK, "actual pipe acquired");
        interrupted.a(md.a(player), 2);
        check(interrupted.pose() == EntitySaboteur.CANCELLED && world.a(292, 4, 289) == PIPE_BLOCK,
                "player hit cancels before removal");

        EntitySaboteur thief = start(world, player);
        act(thief, 64);
        check(world.a(292, 4, 289) == PIPE_BLOCK && thief.pose() == EntitySaboteur.DISMANTLE, "pipe remains through windup");
        act(thief, 65);
        check(world.a(292, 4, 289) == 0 && world.b(292, 4, 289) == null
                && BlockGenericPipe.getPipe(world, 292, 4, 289) == null && target.container.l(),
                "block, tile and persistent pipe removed");
        check(!block.isPipeConnected(world, 293, 4, 289, 292, 4, 289) && world.b(fq.class) == 0,
                "real connection severed and no duplicate harvest");
        check(thief.pose() == EntitySaboteur.CARRY && world.a(thief), "cargo escape state");
        double originX = thief.o, originZ = thief.q;
        for (int tick = 0; tick < 45; tick++) world.n();
        check(Math.hypot(thief.o - originX, thief.q - originZ) > 4, "uses native movement to leave the pipe");
        // The native network assertion above uses synthetic pipe registrations. Remove its
        // remaining pieces before saving the carrier in a normal Anvil world.
        world.g(293, 4, 289, 0);
        world.g(299, 4, 289, 0);
        world.a(true, null);
        ce.a.a();
        agp.a();
        TestWorld reopened = new TestWorld(root);
        EntitySaboteur restored = null;
        for (Object entity : reopened.b) if (entity instanceof EntitySaboteur) restored = (EntitySaboteur) entity;
        check(restored != null && reopened.a(292, 4, 289) == 0, "Anvil reload preserves the carrier and missing pipe");
        yw returning = NaturalSpawnProbe.join(reopened);
        check(restored.a(md.a(returning), 20), "reloaded carrier can be killed");
        restored.A();
        check(reopened.b(fq.class) == 1, "serialized cargo released exactly once");
        for (Object entity : reopened.b) if (entity instanceof fq) {
            aan stack = ((fq) entity).a;
            check(stack.a == 1 && stack.c == stone && stack.i() == 0, "exact registered component and metadata");
        }

        HopperRegistry.block = new BlockHopper(193);
        check(world.d(292, 4, 289, 193, 5) && world.g(293, 4, 289, 54), "native hopper feeds a chest");
        TileHopper hopper = (TileHopper) world.b(292, 4, 289);
        hopper.a(0, new aan(4, 3, 0));
        EntitySaboteur pausing = start(world, player);
        check(pausing.pose() == EntitySaboteur.WINDUP && world.a(pausing), "temporary sabotage still uses its own windup");
        act(pausing, 55);
        check(hopper.isDisrupted() && world.a(292, 4, 289) == 193, "temporary sabotage pauses without removing the Hopper");
        for (int tick = 0; tick < 18; tick++) world.n();
        check(hopper.k_(0).a == 3, "three items remain while Hopper output is paused");
        for (int tick = 0; tick < 95; tick++) world.n();
        check(!hopper.isDisrupted() && hopper.k_(0) == null, "Hopper sends its items after lease expiry");

        player.c(290.5, 4, 303.5, 0, 0);
        for (int z = 302; z <= 304; z++) {
            world.g(293, 4, z, 1);
            world.g(293, 5, z, 1);
        }
        EnderTrail trail = new EnderTrail();
        Method activate = EnderTrail.class.getDeclaredMethod("start", xd.class, yw.class, uh.class);
        Method emit = EnderTrail.class.getDeclaredMethod("emit", xd.class, int.class);
        activate.setAccessible(true);
        emit.setAccessible(true);
        player.V = 100;
        check((Boolean) activate.invoke(trail, world, player, new uh(302, 4, 303)), "temporary trail finds a detour");
        world.markers = 0;
        world.checkObstacle = true;
        emit.invoke(trail, world, 100);
        check(world.markers > 2 && world.markers <= 12 && world.furthest > 293 && world.detour, "markers go around the wall");
        int count = world.markers;
        emit.invoke(trail, world, 101);
        emit.invoke(trail, world, 185);
        check(world.markers == count, "throttles and expires automatically");
        player.V = 190;
        check((Boolean) activate.invoke(trail, world, player, new uh(302, 4, 303)), "can request guidance again");
        emit.invoke(trail, world, 190);
        check(world.markers > count, "repeated activation emits markers");
        player.c(750, 4, 750, 0, 0);
        check(!(Boolean) activate.invoke(trail, world, player, new uh(780, 4, 750)), "unloaded corridor rejected");
        System.out.println("PASS Saboteur item delivery, interruption, teardown, escape, Anvil cargo, and temporary Hopper pause; Ender Trail detour and expiry");

        verifyBuildCraft(world, player, candidate);
        verifyIC2(world, player, root);
        verifyRedPower(world, player);
        verifyTransportLifecycle(root, stone);
    }

    private static void verifyTransportLifecycle(File root, int stone) throws Exception {
        Method register = kw.class.getDeclaredMethod("a", Class.class, String.class);
        register.setAccessible(true);
        register.invoke(null, TileGenericPipe.class, "net.minecraft.src.buildcraft.transport.GenericPipe");
        TestWorld initial = new TestWorld(root, "pending-dismantle");
        yw player = NaturalSpawnProbe.join(initial);
        placePipe(initial, player, 312, stone);
        initial.q = 1;
        check(start(initial, player, 310.5, 289.5).pose() != EntitySaboteur.DISMANTLE,
                "Easy excludes physical removal");
        initial.q = 3;
        EntitySaboteur first = start(initial, player, 310.5, 289.5);
        check(first.pose() == EntitySaboteur.DISMANTLE && initial.a(first), "save with a pending windup");
        initial.a(true, null);
        ce.a.a();
        agp.a();
        TestWorld reopened = new TestWorld(root, "pending-dismantle");
        EntitySaboteur saved = null;
        for (Object entity : reopened.b) if (entity instanceof EntitySaboteur) saved = (EntitySaboteur) entity;
        check(saved != null && saved.pose() == EntitySaboteur.IDLE && saved.disruptionTarget() == null
                && BlockGenericPipe.getPipe(reopened, 312, 4, 289).itemID == stone,
                "pre-removal Anvil reload clears the transient reservation and preserves the pipe");
        saved.A();
        player = NaturalSpawnProbe.join(reopened);
        EntitySaboteur replaced = start(reopened, player, 310.5, 289.5);
        check(replaced.pose() == EntitySaboteur.DISMANTLE, "acquire after reclaiming the saved component");
        reopened.g(312, 4, 289, 0);
        placePipe(reopened, player, 312, stone);
        act(replaced, 31);
        check("TARGET_CHANGED".equals(replaced.lastOutcome()) && reopened.a(312, 4, 289) == PIPE_BLOCK,
                "same-ID replacement survives windup");
        reopened.g(312, 4, 289, 0);
        placePipe(reopened, player, 319, stone);
        EntitySaboteur boundary = start(reopened, player, 317.5, 289.5);
        check(boundary.pose() == EntitySaboteur.DISMANTLE, "begin at the loaded edge");
        ko provider = (ko) reopened.z();
        reopened.a(new uh(700, 4, 700));
        provider.d(20, 18);
        provider.a();
        check(!provider.a(20, 18), "unload the neighboring chunk");
        act(boundary, 31);
        check("UNLOADED".equals(boundary.lastOutcome()) && !provider.a(20, 18)
                && reopened.a(319, 4, 289) == PIPE_BLOCK, "unloaded halo cancels without loading");
        reopened.g(319, 4, 289, 0);

        // Keep using the legacy keys, including the original pipe stack without a registry-class tag.
        ady legacy = new ady();
        legacy.a("ZtoPipeStolen", true);
        legacy.a("ZtoPipeCargo", new aan(stone, 1, 0).b(new ady()));
        EntitySaboteur oldCargo = new EntitySaboteur(reopened);
        oldCargo.a(legacy);
        oldCargo.c(305.5, 4, 289.5, 0, 0);
        recovered(oldCargo, stone, 0);
        ady savedFlag = new ady();
        oldCargo.b(savedFlag);
        check(savedFlag.o("ZtoPipeStolen"), "the one-theft flag survives cargo recovery");

        ady missing = new ady();
        int absentId = 22400;
        check(yr.e[absentId] == null, "missing-optional fixture registry slot");
        missing.a("ZtoPipeCargo", new aan(absentId, 1, 0).b(new ady()));
        EntitySaboteur unresolved = new EntitySaboteur(reopened);
        unresolved.a(missing);
        unresolved.c(305.5, 4, 289.5, 0, 0);
        unresolved.A();
        ady roundTrip = new ady();
        unresolved.b(roundTrip);
        check(!unresolved.G && "WAITING_FOR_RELEASE".equals(unresolved.recoveryOutcome())
                && roundTrip.m("ZtoPipeCargo").e("id") == absentId, "missing optional item stays attached through save");
        System.out.println("PASS pending-windup Anvil reload, replacement, unloaded boundary, legacy cargo and unresolved optional registry");
    }

    private static Pipe install(TestWorld world, int x, int item) {
        Pipe pipe = BlockGenericPipe.createPipe(world, x, 4, 289, item);
        check(world.g(x, 4, 289, PIPE_BLOCK), "install pipe block");
        TileGenericPipe tile = (TileGenericPipe) world.b(x, 4, 289);
        tile.pipe = pipe;
        pipe.setTile(tile);
        pipe.setWorld(world);
        return pipe;
    }

    private static EntitySaboteur start(TestWorld world, yw player) throws Exception {
        return start(world, player, 290.5, 289.5);
    }

    private static EntitySaboteur start(TestWorld world, yw player, double x, double z) throws Exception {
        EntitySaboteur mob = new EntitySaboteur(world);
        player.c(x - 1.5, 4, z, 0, 0);
        mob.c(x, 4, z, 0, 0);
        mob.z = true;
        randomizeActor(mob);
        mob.engageEncounterPlayer(player);
        act(mob, 30);
        act(mob, 30);
        return mob;
    }

    private static void randomizeActor(EntitySaboteur mob) throws Exception {
        Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        random.set(mob, new Random(32021L) {
            @Override public int nextInt(int limit) { return limit == 3 ? 0 : super.nextInt(limit); }
        });
    }

    private static void act(EntitySaboteur mob, int tick) throws Exception {
        mob.V = tick;
        Method fight = EntitySaboteur.class.getDeclaredMethod("fight", double.class, boolean.class);
        fight.setAccessible(true);
        fight.invoke(mob, 4.0, true);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestWorld extends xd {
        int markers;
        boolean checkObstacle, detour;
        double furthest;

        TestWorld(File root) {
            this(root, "pipe-and-trail");
        }

        TestWorld(File root, String name) {
            super(new aia(root, name, false), name, NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 14; x <= 22; x++)
                for (int z = 14; z <= 22; z++) d(x, z);
        }

        @Override public void a(nn entity, String key, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String key, float volume, float pitch) {}

        @Override
        public void a(String particle, double x, double y, double z, double r, double g, double b) {
            if (!checkObstacle || !"reddust".equals(particle)) return;
            check(!(x >= 293 && x < 294 && z >= 302 && z < 305 && y < 6), "never emits markers through the wall");
            detour |= z < 302 || z >= 305 || y >= 6;
            furthest = Math.max(furthest, x);
            markers++;
        }
    }
}
