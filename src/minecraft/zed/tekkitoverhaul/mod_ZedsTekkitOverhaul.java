package zed.tekkitoverhaul;

import net.minecraft.src.BaseMod;
import net.minecraft.src.ModLoader;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.integration.ee2.EE2FuelIntegration;
import zed.tekkitoverhaul.integration.ee2.IC2EmcIntegration;
import zed.tekkitoverhaul.integration.ee2.AdvancedCraftingEmcIntegration;

/** ModLoader 1.2.5 entry point: bootstrap and callbacks only. */
public class mod_ZedsTekkitOverhaul extends BaseMod {
    // Keep the legacy IC2 integration in its existing disabled state.
    private static final boolean ENABLE_IC2_EMC_INTEGRATION = false;
    private final EE2FuelIntegration ee2Fuels = new EE2FuelIntegration();
    private final AdvancedCraftingEmcIntegration craftingEmc = new AdvancedCraftingEmcIntegration();
    private IC2EmcIntegration ic2Emc;

    @Override
    public String getVersion() { return "0.7.0-distillation-prototype"; }

    @Override
    public void load() {
        AdvancedCraftingRegistry.register();
        DistillationRegistry.register();
        ModLoader.setInGameHook(this, true, false); // EE2 EMC registration runs on the first eligible tick.
        System.out.println("[ZTO] v" + getVersion() + " loaded");
    }

    @Override
    public void modsLoaded() { ee2Fuels.onModsLoaded(); }

    @Override
    public int addFuel(int id, int metadata) { return ee2Fuels.getBurnTime(id, metadata); }

    @Override
    public boolean onTickInGame(float partialTick, Minecraft minecraft) {
        craftingEmc.onGameTick();
        if (ENABLE_IC2_EMC_INTEGRATION) {
            if (ic2Emc == null) ic2Emc = new IC2EmcIntegration();
            ic2Emc.onGameTick();
        }
        return true;
    }
}
