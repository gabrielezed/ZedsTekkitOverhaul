package zed.tekkitoverhaul.lunar.client;

import java.io.File;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.src.BaseMod;
import net.minecraft.src.Chunk;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.WorldClient;
import net.minecraft.src.forge.ISaveEventHandler;
import net.minecraft.src.forge.MinecraftForge;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarService;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Owns the one active single-player save session. */
public final class LocalLunarCalendar implements ITickHandler, ISaveEventHandler {
    private Minecraft game;
    private Thread owner;
    private World world;
    private ISaveHandler save;
    private LunarWorldState state;
    private LunarCalendar calendar;
    private LunarDiagnostics diagnostics;
    private SiegeDirector assault;
    private long sessionId;
    private boolean verbose;
    private KeyBinding key;
    private long lastKeyEvent;
    private String storageStatus = "No active local save";

    public void register(BaseMod mod, int keyCode, boolean verbose, SiegeDirector assault) {
        game = ModLoader.getMinecraftInstance();
        owner = Thread.currentThread();
        this.verbose = verbose;
        this.assault = assault;
        if (keyCode > 0 && keyCode < 256) {
            key = new KeyBinding("Open ZTO Lunar Debug", keyCode);
            ModLoader.registerKey(mod, key, false);
        }
        MinecraftForge.registerSaveHandler(this);
        FMLCommonHandler.instance().registerTickHandler(this);
        LunarMoonRenderer.register(this);
    }

    public void onKeyPressed(KeyBinding pressed) {
        if (key != null && key == pressed && game.currentScreen == null && local(game.theWorld)) {
            lastKeyEvent = Keyboard.getEventNanoseconds();
            game.displayGuiScreen(new GuiLunarDebug(this, game.theWorld));
        }
    }

    public LunarService service(World context) {
        return context != null && context == world && context == game.theWorld && calendar != null
                ? calendar.view() : null;
    }

    public SiegeDirector.Snapshot assaultSnapshot(World context) {
        return context != null && context == game.theWorld ? assault.snapshot(context) : null;
    }

    /** Aligns a future night without touching vanilla time or replaying crossed boundaries. */
    public boolean alignBeforeNight(World context, LunarPhase phase, int latestTick) {
        LunarCalendar authority = authority(context);
        if (authority == null || !authority.view().snapshot().schedulingEligible
                || authority.view().snapshot().timeOfDay > latestTick) return false;
        authority.setPhase(phase);
        return true;
    }

    LunarSnapshot snapshot(World context) {
        LunarService service = service(context);
        return service == null ? null : service.snapshot();
    }

    LunarCalendar authority(World context) { return service(context) == null ? null : calendar; }
    LunarDiagnostics diagnostics() { return diagnostics; }
    String storageStatus() { return storageStatus; }

    private static boolean local(World world) {
        return world != null && !(world instanceof WorldClient) && !world.isRemote;
    }

    @Override
    public void onWorldLoad(World incoming) {
        if (Thread.currentThread() != owner || !local(incoming) || !incoming.isNewWorld) return;
        File file = incoming.getSaveHandler().getMapFileFromName(LunarWorldState.KEY);
        if (file != null && !file.exists()) {
            long rawTime = incoming.getWorldTime();
            long alignment = -Math.floorDiv(rawTime, (long) LunarSnapshot.DAY);
            incoming.setItemData(LunarWorldState.KEY, LunarWorldState.create(alignment, rawTime, "New save alignment"));
        }
    }

    private void bind(World active) {
        if (active == world) return;
        if (!local(active)) {
            detach();
            return;
        }
        ISaveHandler handler = active.getSaveHandler();
        boolean sameSave = save == handler;
        if (!sameSave) detach();
        world = active;
        save = handler;
        if (sameSave) {
            if (calendar != null) {
                assault.bind(null, null);
                active.setItemData(LunarWorldState.KEY, state);
                calendar.rebaseline(active.worldProvider.worldType, active.getWorldTime(), LunarEvent.Cause.DIMENSION_CHANGED);
                assault.bind(active, calendar.view());
            }
            return;
        }

        File file = handler.getMapFileFromName(LunarWorldState.KEY);
        state = (LunarWorldState) active.loadItemData(LunarWorldState.class, LunarWorldState.KEY);
        if (state == null && file != null && !file.exists()) {
            long time = active.getWorldTime();
            // Vanilla may return a negative atlas index for signed or overflowing raw days.
            int cell = (int) (time / LunarSnapshot.DAY) % 8;
            boolean standard = time >= 0 && cell >= 0;
            state = LunarWorldState.create(standard ? 4 : 0, time,
                    standard ? "Aligned legacy save" : "Fallback alignment for unusual legacy time");
            active.setItemData(LunarWorldState.KEY, state);
        }
        storageStatus = state == null ? "Lunar data missing or unreadable" : state.status;
        if (verbose) ModLoader.getLogger().info("[ZTO lunar] " + storageStatus);
        if (state == null || !state.valid) return;

        calendar = new LunarCalendar(++sessionId, state.baseline, state.adjustment, state.watermark, new Runnable() {
            public void run() {
                state.adjustment = calendar.adjustment();
                state.watermark = calendar.watermark();
                state.markDirty();
            }
        }, new Consumer<RuntimeException>() {
            public void accept(RuntimeException failure) {
                storageStatus = state.status + " | listener failure: " + failure.getClass().getSimpleName();
                if (verbose) ModLoader.getLogger().log(Level.WARNING, "[ZTO lunar] Listener failed", failure);
            }
        });
        if (key != null || verbose) {
            diagnostics = new LunarDiagnostics(verbose);
            calendar.view().subscribe(diagnostics);
        }
        LunarEvent.Cause cause = state.initializedHere ? LunarEvent.Cause.INITIALIZED : LunarEvent.Cause.LOADED;
        calendar.rebaseline(active.worldProvider.worldType, active.getWorldTime(), cause);
        assault.bind(active, calendar.view());
    }

    private void detach() {
        assault.bind(null, null);
        if (calendar != null) calendar.detach();
        calendar = null;
        diagnostics = null;
        state = null;
        save = null;
        world = null;
        storageStatus = "No active local save";
    }

    @Override
    public void tickStart(EnumSet<TickType> ticks, Object... data) {
        if (ticks.contains(TickType.WORLDLOAD)) bind(game.theWorld);
        if (!ticks.contains(TickType.RENDER)) return;
        if (!local(game.theWorld) && world != null) detach();
        if (key != null) {
            // FML polls the held state. Both edges of a short tap can occur between polls.
            long event = Keyboard.getEventNanoseconds();
            if (event != lastKeyEvent) {
                lastKeyEvent = event;
                if (key.keyCode > 0 && Keyboard.getEventKey() == key.keyCode) onKeyPressed(key);
            }
        }
    }

    @Override
    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (!ticks.contains(TickType.WORLD)) return;
        bind(game.theWorld);
        if (calendar != null) {
            calendar.observe(world.getWorldTime());
            assault.tick(game.thePlayer, game.isGamePaused);
        }
    }

    @Override
    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.WORLD, TickType.WORLDLOAD, TickType.RENDER); }

    @Override
    public String getLabel() { return "ZTO Lunar Calendar"; }

    public void onWorldSave(World world) { }
    public void onChunkLoad(World world, Chunk chunk) { }
    public void onChunkUnload(World world, Chunk chunk) { }
    public void onChunkLoadData(World world, Chunk chunk, NBTTagCompound data) { }
    public void onChunkSaveData(World world, Chunk chunk, NBTTagCompound data) { }
}
