package zed.tekkitoverhaul;

import net.minecraft.src.BaseMod;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.MLProp;
import net.minecraft.src.RenderBlocks;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;
import zed.tekkitoverhaul.creative.CreativeInventoryHook;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.client.DistillateBucketTextures;
import zed.tekkitoverhaul.emc.EmcIntegration;
import zed.tekkitoverhaul.fluids.PetroleumFluids;
import zed.tekkitoverhaul.fluids.client.RenderPetroleum;
import zed.tekkitoverhaul.integration.ee2.EE2FuelIntegration;
import zed.tekkitoverhaul.power.AlchemicalPowerRegistry;

/** ModLoader 1.2.5 entry point: bootstrap and callbacks only. */
public class mod_ZedsTekkitOverhaul extends BaseMod {
    @MLProp(info = "Enable the single-player creative inventory backport when its local assets are installed")
    public static boolean creativeInventoryBackport = true;

    private final EE2FuelIntegration ee2Fuels = new EE2FuelIntegration();
    private final EmcIntegration emc = new EmcIntegration();
    private final CreativeInventoryHook creativeInventory = new CreativeInventoryHook();
    private int petroleumModel;

    @Override
    public String getVersion() { return "0.7.0-distillation-prototype"; }

    @Override
    public void load() {
        AdvancedCraftingRegistry.register();
        petroleumModel = ModLoader.getUniqueBlockModelID(this, false);
        PetroleumFluids.setRenderType(petroleumModel);
        DistillationRegistry.register();
        ModLoader.setInGameHook(this, true, false); // EE2 EMC registration runs on the first eligible tick.
        System.out.println("[ZTO] v" + getVersion() + " loaded");
    }

    @Override
    public void modsLoaded() {
        ee2Fuels.onModsLoaded();
        AlchemicalPowerRegistry.register();
        if (creativeInventoryBackport) creativeInventory.register(this);
    }

    @Override
    public void keyboardEvent(KeyBinding key) { creativeInventory.onKeyPressed(key); }

    @Override
    public void registerAnimation(Minecraft minecraft) {
        RenderPetroleum.preloadTextures();
        DistillateBucketTextures.register();
    }

    @Override
    public boolean renderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId) {
        return modelId == petroleumModel && RenderPetroleum.render(renderer, world, x, y, z, block);
    }

    @Override
    public int addFuel(int id, int metadata) { return ee2Fuels.getBurnTime(id, metadata); }

    @Override
    public boolean onTickInGame(float partialTick, Minecraft minecraft) {
        emc.onGameTick(minecraft.mcDataDir);
        return true;
    }
}
