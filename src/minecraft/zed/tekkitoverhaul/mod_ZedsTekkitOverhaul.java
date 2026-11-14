package zed.tekkitoverhaul;

import java.util.Map;
import net.minecraft.src.BaseMod;
import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Render;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.config.ZtoConfigLoader;
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
import zed.tekkitoverhaul.survival.tactical.TacticalHostiles;
import zed.tekkitoverhaul.survival.tactical.client.TacticalHostileRenderers;

/** ModLoader 1.2.5 entry point: bootstrap and callbacks only. */
public class mod_ZedsTekkitOverhaul extends BaseMod {
    private ZtoConfig settings;

    public ZtoConfig settings() { return settings; }

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
        settings = ZtoConfigLoader.load(ModLoader.getMinecraftInstance().mcDataDir.toPath().resolve("config"),
                HopperRegistry.hasAssets(), ModLoader.getLogger());
        TacticalHostiles.register();
        AdvancedCraftingRegistry.register(settings);
        MinecraftForge.registerEntityLivingHandler(new MobEquipmentHooks());
        if (settings.disableNightSkipping) MinecraftForge.registerSleepHandler(new BedRespawn());
        petroleumModel = ModLoader.getUniqueBlockModelID(this, false);
        PetroleumFluids.setRenderType(petroleumModel);
        DistillationRegistry.register(settings);
        ModLoader.setInGameHook(this, true, false); // EE2 EMC registration runs on the first eligible tick.
        ModLoader.setInGUIHook(this, true, false);
        System.out.println("[ZTO] v" + getVersion() + " loaded");
    }

    @Override
    public void modsLoaded() {
        LUNAR.register(this, settings.lunarDebugKey, settings.lunarVerboseLogging,
                new SiegeDirector(settings.playerAssault, settings.assaultSpawnBudget, settings.assaultWaveInterval,
                        settings.assaultActiveCap, settings.assaultOuterRadius, settings.lunarPatrols,
                        settings.lunarOutdoorLightPressure, settings.experimentalEnderkinEncounters),
                settings.survivalDebugHud, settings.survivalDebugHudKey, settings.survivalDebugLogCapacity);
        DragDistributionIntegration.register();
        ee2Fuels.onModsLoaded();
        AlchemicalPowerRegistry.register(settings);
        HopperRegistry.register(this, settings);
        TacticalHostiles.registerEggs(settings);
        if (settings.creativeInventoryBackport) creativeInventory.register(this);
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
        TacticalHostileRenderers.register(renderers);
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
        emc.onGameTick(minecraft.mcDataDir, settings.emcProfile);
        zed.tekkitoverhaul.hopper.dev.HopperAcceptance.tick(minecraft);
        MobEquipmentFixture.tick(minecraft);
        return true;
    }

    @Override
    public boolean onTickInGUI(float partialTick, Minecraft minecraft, GuiScreen screen) {
        craftingUx.tick(minecraft, screen, settings.partialRecipeFill, settings.craftingUxShortcut);
        DragDistributionIntegration.renderPreview(minecraft, minecraft.currentScreen);
        return true;
    }
}
