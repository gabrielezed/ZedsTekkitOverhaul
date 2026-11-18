package zed.tekkitoverhaul.survival.weather.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiInventory;
import net.minecraft.src.GuiScreen;
import zed.tekkitoverhaul.survival.weather.JetpackWeather;

public final class JetpackWeatherStatus {
    private JetpackWeatherStatus() {}

    public static void drawInventory(Minecraft minecraft, GuiScreen screen) {
        JetpackWeather gear = JetpackWeather.LOCAL;
        if (!(screen instanceof GuiInventory) || !gear.equipped() || !gear.available()) return;
        JetpackWeather.Phase phase = gear.phase();
        int color = phase == JetpackWeather.Phase.FAULT ? 0xFF9373 : 0xE4EBCB;
        String state = phase == JetpackWeather.Phase.DRY ? "Stable" : phase.name().toLowerCase(Locale.ROOT);
        screen.drawCenteredString(minecraft.fontRenderer, "Electric Jetpack   " + state,
                screen.width / 2, screen.height / 2 + 88, color);
    }
}
