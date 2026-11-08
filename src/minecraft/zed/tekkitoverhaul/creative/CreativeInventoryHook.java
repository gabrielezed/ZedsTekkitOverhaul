package zed.tekkitoverhaul.creative;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.src.BaseMod;
import net.minecraft.src.GameSettings;
import net.minecraft.src.GuiContainerCreative;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import org.lwjgl.input.Keyboard;

public final class CreativeInventoryHook implements ITickHandler {
    private GuiScreen observed;
    private final KeyBinding restoreKey = new KeyBinding("Restore ZTO Creative Inventory", Keyboard.KEY_F6);
    private boolean classicSelected;
    private boolean catalogUnavailable;
    private long lastKeyboardEvent;

    public void register(BaseMod mod) {
        if (CreativeInventoryTextures.available()) {
            ModLoader.registerKey(mod, restoreKey, false);
            FMLCommonHandler.instance().registerTickHandler(this);
        }
    }

    public void onKeyPressed(KeyBinding key) {
        if (key == restoreKey) restore(ModLoader.getMinecraftInstance());
    }

    private void restore(Minecraft game) {
        GuiScreen screen = game.currentScreen;
        if (classicSelected && canUse(game) && (screen == null || screen.getClass() == GuiContainerCreative.class)) {
            classicSelected = false;
            observed = null;
        }
    }

    String restoreKeyName() { return GameSettings.getKeyDisplayString(restoreKey.keyCode); }

    @Override
    public void tickStart(EnumSet<TickType> ticks, Object... data) {
        Minecraft game = ModLoader.getMinecraftInstance();
        GuiScreen screen = game.currentScreen;
        if (classicSelected) {
            // FML polls key state once per game tick. A short tap can be fully consumed by the GUI between polls.
            long event = Keyboard.getEventNanoseconds();
            if (event != lastKeyboardEvent) {
                lastKeyboardEvent = event;
                if (restoreKey.keyCode > 0 && Keyboard.getEventKey() == restoreKey.keyCode) restore(game);
            }
        }
        if (screen == observed) return;
        observed = screen;
        if (classicSelected || catalogUnavailable || screen == null || screen.getClass() != GuiContainerCreative.class
                || !canUse(game) || game.thePlayer.inventory.getItemStack() != null) return;

        CreativeCatalog catalog;
        try {
            List<?> entries = CreativeCatalog.readLegacyEntries(((GuiContainerCreative) screen).inventorySlots);
            catalog = new CreativeCatalog(entries);
        } catch (ReflectiveOperationException failure) {
            catalogUnavailable = true;
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] Cannot read the legacy creative catalog", failure);
            return;
        }
        game.displayGuiScreen(new GuiCreativeBackport(game.thePlayer, catalog, this));
    }

    static boolean canUse(Minecraft game) {
        return game.theWorld != null && !game.theWorld.isRemote && game.thePlayer != null
                && game.thePlayer.isEntityAlive() && game.playerController != null
                && game.playerController.isInCreativeMode() && game.thePlayer.capabilities.isCreativeMode;
    }

    void useClassic(Minecraft game) {
        classicSelected = true;
        lastKeyboardEvent = Keyboard.getEventNanoseconds();
        game.displayGuiScreen(new GuiContainerCreative(game.thePlayer));
    }

    @Override
    public void tickEnd(EnumSet<TickType> ticks, Object... data) { }

    @Override
    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }

    @Override
    public String getLabel() { return "ZTO creative inventory"; }
}
