package zed.tekkitoverhaul.devbridge;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;

/** The legacy local world is owned by Minecraft's client loop. */
public final class DevBridge implements ITickHandler {
    private final BridgeDispatcher dispatcher;
    private final LocalLunarCalendar lunar;
    private final LunarObserver lunarObserver = new LunarObserver();
    private WeakReference<World> observed = new WeakReference<World>(null);
    private long worldSequence;
    private long worldId;
    private long tick;
    private long simulationTicks;
    private final HopperTestFixture fixture;
    private final DisposableWorldBootstrap bootstrap;

    private DevBridge(BridgeDispatcher dispatcher, LocalLunarCalendar lunar, HopperTestFixture fixture,
            DisposableWorldBootstrap bootstrap) {
        this.fixture = fixture;
        this.dispatcher = dispatcher;
        this.lunar = lunar;
        this.bootstrap = bootstrap;
    }

    public static void startIfEnabled(String version, LocalLunarCalendar lunar) {
        if (!"true".equals(System.getProperty("zto.devBridge.enabled"))
                || !"true".equals(System.getProperty("zto.dev"))) return;
        BridgeServer server = null;
        try {
            int port = Integer.parseInt(System.getProperty("zto.devBridge.port", "0"));
            BridgeDispatcher dispatcher = new BridgeDispatcher();
            boolean testing = "true".equals(System.getProperty("zto.devBridge.testing"));
            HopperTestFixture fixture = testing ? new HopperTestFixture() : null;
            server = new BridgeServer(port, dispatcher, version, testing);
            DisposableWorldBootstrap bootstrap = testing ? new DisposableWorldBootstrap(server.discovery(), fixture) : null;
            FMLCommonHandler.instance().registerTickHandler(new DevBridge(dispatcher, lunar, fixture, bootstrap));
            ModLoader.getLogger().info("[ZTO] Read-only Dev Bridge enabled. Discovery: " + server.discovery());
        } catch (IOException failure) {
            startupFailed(server, failure);
        } catch (RuntimeException failure) {
            startupFailed(server, failure);
        }
    }

    private static void startupFailed(BridgeServer server, Exception failure) {
        if (server != null) server.close();
        ModLoader.getLogger().warning("[ZTO] Dev Bridge startup failed (" + failure.getClass().getSimpleName() + ")");
    }

    @Override
    public void tickStart(EnumSet<TickType> types, Object... data) {
        if (types.contains(TickType.WORLDLOAD)) observeWorld(ModLoader.getMinecraftInstance());
    }

    @Override
    public void tickEnd(EnumSet<TickType> types, Object... data) {
        if (types.contains(TickType.WORLD)) {
            tick++;
            Minecraft game = ModLoader.getMinecraftInstance();
            World world = game.theWorld;
            if (world != null && world == observed.get() && !game.isGamePaused
                    && !world.isRemote && !game.isMultiplayerWorld()) simulationTicks++;
        }
        if (!types.contains(TickType.RENDER)) return;
        final Minecraft game = ModLoader.getMinecraftInstance();
        if (bootstrap != null) bootstrap.tick(game);
        World world = observeWorld(game);

        String state = "ready";
        if (world == null) state = game.theWorld == null ? "no_world" : "multiplayer";
        else if (game.thePlayer == null) state = "no_world";
        else if (game.isGamePaused || (game.currentScreen != null && game.currentScreen.doesGuiPauseGame())) state = "paused";

        final int dimension = world == null ? 0 : world.worldProvider.worldType;
        dispatcher.observe(state, dimension, worldId, tick);
        if ("ready".equals(state)) {
            final World current = world;
            dispatcher.drain(new BridgeDispatcher.Reader() {
                public String read(BridgeProtocol.Query query) throws BridgeProtocol.Error {
                    return readQuery(game, current, dimension, query);
                }
            });
        }
    }

    private World observeWorld(Minecraft game) {
        World world = game.theWorld;
        if (world != null && (game.isMultiplayerWorld() || world.isRemote)) world = null;
        if (world != observed.get() || (world == null && worldId != 0)) {
            observed = new WeakReference<World>(world);
            worldId = world == null ? 0 : ++worldSequence;
            simulationTicks = 0;
            if (fixture != null) fixture.bind(worldId, world);
        }
        lunarObserver.bind(worldId, lunar.service(world));
        return world;
    }

    private String readQuery(Minecraft game, World world, int dimension, BridgeProtocol.Query query) throws BridgeProtocol.Error {
        if ("inventory.player".equals(query.op)) return BridgeSnapshots.player(game.thePlayer.inventory, worldId);
        if ("test.context".equals(query.op)) {
            return fixture == null ? "{\"allowed\":false}" : fixture.context(game.mcDataDir, world, dimension, simulationTicks);
        }
        if (query.sessionId != 0 && fixture != null) {
            return fixture.command(game, world, dimension, simulationTicks, query, lunar);
        }
        if ("inventory.container".equals(query.op) || "machine.inspect".equals(query.op)) {
            if (query.worldId != worldId) throw new BridgeProtocol.Error(409, "STALE_WORLD", "The active world has changed");
            checkCoordinates(dimension, query);
            return BridgeSnapshots.inspect(world, query, simulationTicks);
        }
        if ("lunar.snapshot".equals(query.op)) return lunarObserver.snapshot();
        if ("lunar.events".equals(query.op)) return lunarObserver.poll(query.worldId, query.afterSequence);
        if ("assault.snapshot".equals(query.op)) return AssaultObservation.read(worldId, lunar.assaultSnapshot(world));
        return read(world, dimension, query);
    }

    private static String read(World world, int dimension, BridgeProtocol.Query query) throws BridgeProtocol.Error {
        if ("world.time".equals(query.op)) {
            long time = world.getWorldTime();
            return "{\"dimension\":" + dimension + ",\"worldTime\":" + time + ",\"day\":" + Math.floorDiv(time, 24000L)
                    + ",\"dayTime\":" + Math.floorMod(time, 24000L) + ",\"raining\":" + world.isRaining()
                    + ",\"thundering\":" + world.isThundering() + "}";
        }
        checkCoordinates(dimension, query);
        Chunk chunk = BridgeSnapshots.loaded(world, query.x, query.z);
        int x = query.x & 15;
        int z = query.z & 15;
        return "{\"dimension\":" + dimension + ",\"x\":" + query.x + ",\"y\":" + query.y + ",\"z\":" + query.z
                + ",\"loaded\":true,\"blockId\":" + chunk.getBlockID(x, query.y, z)
                + ",\"metadata\":" + chunk.getBlockMetadata(x, query.y, z)
                + ",\"tilePresent\":" + chunk.chunkTileEntityMap.containsKey(new ChunkPosition(x, query.y, z)) + "}";
    }

    private static void checkCoordinates(int dimension, BridgeProtocol.Query query) throws BridgeProtocol.Error {
        if (query.dimension != dimension) {
            throw new BridgeProtocol.Error(400, "INVALID_ARGUMENT", "Use the active local dimension");
        }
        if (query.x < -30000000 || query.x >= 30000000 || query.z < -30000000 || query.z >= 30000000
                || query.y < 0 || query.y >= 256) {
            throw new BridgeProtocol.Error(400, "OUT_OF_BOUNDS", "The block coordinates are outside legacy world bounds");
        }
    }

    @Override
    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.WORLD, TickType.WORLDLOAD, TickType.RENDER); }

    @Override
    public String getLabel() { return "ZTO Dev Bridge"; }
}
