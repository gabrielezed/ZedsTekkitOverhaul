package zed.tekkitoverhaul.exploration.abyssal.guardian.client;

import java.util.EnumSet;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.exploration.abyssal.guardian.GuardianEncounters;

public final class GuardianWorldTick implements ITickHandler {
    private final Minecraft game;

    private GuardianWorldTick(Minecraft game) { this.game = game; }

    public static void register(Minecraft game) {
        FMLCommonHandler.instance().registerTickHandler(new GuardianWorldTick(game));
    }

    @Override public void tickStart(EnumSet<TickType> types, Object... data) {}

    @Override public void tickEnd(EnumSet<TickType> types, Object... data) {
        if (types.contains(TickType.WORLD) && !game.isGamePaused)
            GuardianEncounters.tick(game.theWorld, game.thePlayer);
    }

    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.WORLD); }
    @Override public String getLabel() { return "ZTO Abyssal Encounters"; }
}
