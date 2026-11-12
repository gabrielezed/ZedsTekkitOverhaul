package zed.tekkitoverhaul;

import java.util.Map;
import net.minecraft.src.BaseMod;
import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.MLProp;
import net.minecraft.src.Render;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;
import zed.tekkitoverhaul.crafting.CraftingUx;
import zed.tekkitoverhaul.creative.CreativeInventoryHook;
import zed.tekkitoverhaul.devbridge.DevBridge;
import zed.tekkitoverhaul.devbridge.MobEquipmentFixture;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.client.DistillateBucketTextures;
import zed.tekkitoverhaul.emc.EmcIntegration;
import zed.tekkitoverhaul.equipment.MobEquipmentHooks;
import zed.tekkitoverhaul.equipment.RenderMobEquipment;
import zed.tekkitoverhaul.fluids.PetroleumFluids;
import zed.tekkitoverhaul.fluids.client.RenderPetroleum;
import zed.tekkitoverhaul.integration.ee2.EE2FuelIntegration;
import zed.tekkitoverhaul.integration.inventory.DragDistributionIntegration;
import zed.tekkitoverhaul.power.AlchemicalPowerRegistry;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.RenderHopper;
import zed.tekkitoverhaul.lunar.client.LocalLunarCalendar;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.survival.BedRespawn;

/** ModLoader 1.2.5 entry point: bootstrap and callbacks only. */
public class mod_ZedsTekkitOverhaul extends BaseMod {
    @MLProp(info = "Enable the single-player creative inventory backport when its local assets are installed")
    public static boolean creativeInventoryBackport = true;
    @MLProp(info = "Allow Alt-click on the recipe prepare button to place available ingredients")
    public static boolean partialRecipeFill = false;
    @MLProp(info = "Optional LWJGL key code for the hovered crafting UX button (0 disables it)")
    public static int craftingUxShortcut = 0;
    @MLProp(info = "LWJGL key code for the single-player Lunar Debug menu (0 disables it)")
    public static int lunarDebugKey = 0;
    @MLProp(info = "Log lunar transitions, save initialization and listener failures")
    public static boolean lunarVerboseLogging = false;
    @MLProp(info = "Enable natural full-moon player assaults in local Survival")
    public static boolean playerAssault = true;
    @MLProp(info = "Enable natural first-quarter and waxing-gibbous patrols in local Survival")
    public static boolean lunarPatrols = true;
    @MLProp(info = "Use Overworld beds as respawn anchors without sleeping or advancing the night")
    public static boolean disableNightSkipping = true;
    @MLProp(info = "Maximum enemies per assault (1 to 24)")
    public static int assaultSpawnBudget = 15;
    @MLProp(info = "Simulation ticks between assault waves while enemies remain (400 to 1200)")
    public static int assaultWaveInterval = 900;
    @MLProp(info = "Maximum active assault enemies (2 to 12)")
    public static int assaultActiveCap = 8;
    @MLProp(info = "Maximum horizontal assault spawn distance in blocks (14 to 28)")
    public static int assaultOuterRadius = 16;
    @MLProp(info = "Enable the local single-player Survival Debug HUD")
    public static boolean survivalDebugHud = false;
    @MLProp(info = "LWJGL key code for Survival Debug (0 disables its shortcut)")
    public static int survivalDebugHudKey = Keyboard.KEY_F10;
    @MLProp(info = "Survival Debug history entries per save session (20 to 300)")
    public static int survivalDebugLogCapacity = 250;

    private static final LocalLunarCalendar LUNAR = new LocalLunarCalendar();

    public static zed.tekkitoverhaul.lunar.LunarService lunarService(net.minecraft.src.World world) {
        return LUNAR.service(world);
    }

    private final EE2FuelIntegration ee2Fuels = new EE2FuelIntegration();
    private final EmcIntegration emc = new EmcIntegration();
    private final CreativeInventoryHook creativeInventory = new CreativeInventoryHook();
    private final CraftingUx craftingUx = new CraftingUx();
    private int petroleumModel;

    @Override
    public String getVersion() { return BuildVersion.VERSION; }

    @Override
    public void load() {
        AdvancedCraftingRegistry.register();
        MinecraftForge.registerEntityLivingHandler(new MobEquipmentHooks());
        if (disableNightSkipping) MinecraftForge.registerSleepHandler(new BedRespawn());
        petroleumModel = ModLoader.getUniqueBlockModelID(this, false);
        PetroleumFluids.setRenderType(petroleumModel);
        DistillationRegistry.register();
        ModLoader.setInGameHook(this, true, false); // EE2 EMC registration runs on the first eligible tick.
        ModLoader.setInGUIHook(this, true, false);
        System.out.println("[ZTO] v" + getVersion() + " loaded");
    }

    @Override
    public void modsLoaded() {
        LUNAR.register(this, lunarDebugKey, lunarVerboseLogging,
                new SiegeDirector(playerAssault, assaultSpawnBudget, assaultWaveInterval, assaultActiveCap,
                        assaultOuterRadius, lunarPatrols),
                survivalDebugHud, survivalDebugHudKey, survivalDebugLogCapacity);
        DragDistributionIntegration.register();
        ee2Fuels.onModsLoaded();
        AlchemicalPowerRegistry.register();
        HopperRegistry.register(this);
        if (creativeInventoryBackport) creativeInventory.register(this);
        DevBridge.startIfEnabled(getVersion(), LUNAR);
    }

    @Override
    public void keyboardEvent(KeyBinding key) {
        creativeInventory.onKeyPressed(key);
        LUNAR.onKeyPressed(key);
    }

    @Override
    public void registerAnimation(Minecraft minecraft) {
        RenderPetroleum.preloadTextures();
        DistillateBucketTextures.register();
    }

    @Override
    public void addRenderer(Map<Class<? extends Entity>, Render> renderers) {
        RenderMobEquipment.register(renderers);
    }

    @Override
    public boolean renderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId) {
        if (HopperRegistry.block != null && modelId == HopperRegistry.model)
            return RenderHopper.render(renderer, world, x, y, z, block);
        return modelId == petroleumModel && RenderPetroleum.render(renderer, world, x, y, z, block);
    }

    @Override
    public int addFuel(int id, int metadata) { return ee2Fuels.getBurnTime(id, metadata); }

    @Override
    public boolean onTickInGame(float partialTick, Minecraft minecraft) {
        emc.onGameTick(minecraft.mcDataDir);
        zed.tekkitoverhaul.hopper.dev.HopperAcceptance.tick(minecraft);
        MobEquipmentFixture.tick(minecraft);
        return true;
    }

    @Override
    public boolean onTickInGUI(float partialTick, Minecraft minecraft, GuiScreen screen) {
        craftingUx.tick(minecraft, screen, partialRecipeFill, craftingUxShortcut);
        DragDistributionIntegration.renderPreview(minecraft, minecraft.currentScreen);
        return true;
    }
}
