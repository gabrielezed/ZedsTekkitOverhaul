import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;
import zed.tekkitoverhaul.creative.CreativeInventoryHook;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.mod_ZedsTekkitOverhaul;

public final class ConfigRuntimeProbe implements ITickHandler {
    private final String mode;
    private boolean done;

    private ConfigRuntimeProbe(String mode) { this.mode = mode; }

    public static void main(String[] args) throws Exception {
        File game = new File(args[0]).getCanonicalFile();
        if (!"survival-hud-runtime".equals(game.getParentFile().getParentFile().getName())
                || !new File(game, "mods/ZedsTekkitOverhaul.jar").isFile()) {
            throw new IllegalArgumentException("Use the isolated Tekkit Dev copy");
        }
        field(Minecraft.class, "aj").set(null, game);
        FMLCommonHandler.instance().registerTickHandler(new ConfigRuntimeProbe(args[1]));
        Minecraft.main(new String[] {"ZTOTest"});
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    public String getLabel() { return "ZTO startup settings probe"; }
    public void tickStart(EnumSet<TickType> ticks, Object... data) { }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        if (game == null || !(game.s instanceof xt)) return;
        done = true;
        try {
            SilentRuntimeAudio.verify(game);
            game.C.c();
            game.C.a("random.click", 1.0F, 1.0F);
            SilentRuntimeAudio.verify(game);
            System.out.println("CONFIG AUDIO music=0 effects=0 backend=absent after playback calls");
            mod_ZedsTekkitOverhaul mod = null;
            for (BaseMod entry : ModLoader.getLoadedMods()) {
                if (entry instanceof mod_ZedsTekkitOverhaul) mod = (mod_ZedsTekkitOverhaul) entry;
            }
            check(mod != null, "mod loaded");
            ZtoConfig settings = mod.settings();
            boolean custom = mode.equals("legacy") || mode.equals("restart");
            boolean edited = mode.equals("canonical") || mode.equals("write-blocked");
            check(settings.experimentalEnderkinEncounters == custom, "experimental opt-in");
            check(settings.disableNightSkipping != custom, "bed policy");
            check(settings.partialRecipeFill == custom && settings.craftingUxShortcut == (custom ? 34 : 0),
                    "crafting settings");
            check(settings.creativeInventoryBackport != custom, "creative setting");
            Object creative = field(mod.getClass(), "creativeInventory").get(mod);
            check(Arrays.asList(game.A.A).contains(field(CreativeInventoryHook.class, "restoreKey").get(creative))
                    == (!custom && CreativeInventoryHook.class.getResource("/zed/creative-1.4.7/allitems.png") != null),
                    "creative key registration");
            check(AdvancedCraftingRegistry.foundry.bO == (custom || edited ? 3080 : 3001), "registered foundry ID");
            check((HopperRegistry.block != null) == HopperRegistry.hasAssets(), "Hopper asset gate");
            check(settings.emcProfile.equals(custom || edited ? "zto-emc 1 AUDIT\n" : ""), "EMC snapshot");

            Object lunar = field(mod.getClass(), "LUNAR").get(null);
            check((field(LocalLunarCalendar.class, "key").get(lunar) != null) == custom, "lunar key registration");
            check(field(LocalLunarCalendar.class, "verbose").getBoolean(lunar) == custom, "verbose logging setting");
            Object hud = field(LocalLunarCalendar.class, "survivalHud").get(lunar);
            check((hud != null) == custom && settings.survivalDebugHudKey == (custom ? 64 : 68), "HUD registration");
            check(!custom || field(LocalLunarCalendar.class, "survivalLogCapacity").getInt(lunar) == 20, "HUD capacity");
            Object director = field(LocalLunarCalendar.class, "assault").get(lunar);
            check(field(SiegeDirector.class, "budget").getInt(director) == (custom ? 24 : 15), "assault budget");
            check(field(SiegeDirector.class, "interval").getInt(director) == (custom ? 400 : 900), "wave interval");
            check(field(SiegeDirector.class, "activeCap").getInt(director) == (custom ? 5 : 8), "active cap");
            check(field(SiegeDirector.class, "outerRadius").getInt(director) == (custom ? 21 : 16), "outer radius");
            check(settings.playerAssault != custom && settings.lunarPatrols != custom
                    && settings.lunarOutdoorLightPressure != custom, "survival flags");
            check(field(SiegeDirector.class, "enabled").getBoolean(director) != custom, "assault admission");
            check(field(SiegeDirector.class, "experimentalEnderkin").getBoolean(director) == custom, "director opt-in");
            Object patrol = field(SiegeDirector.class, "patrol").get(director);
            check(field(patrol.getClass(), "enabled").getBoolean(patrol) != custom, "patrol admission");
            for (Object owner : new Object[] {director, patrol}) {
                Object spawns = field(owner.getClass(), "spawns").get(owner);
                check(field(spawns.getClass(), "outdoorPressure").getBoolean(spawns) != custom, "placement light policy");
            }
            check(!Boolean.getBoolean("zto.devBridge.enabled"), "Dev Bridge disabled");
            System.out.println("CONFIG PASS " + mode);
            System.out.flush();
            // No world is open. A legacy resource worker may keep the client alive during shutdown.
            Runtime.getRuntime().halt(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
