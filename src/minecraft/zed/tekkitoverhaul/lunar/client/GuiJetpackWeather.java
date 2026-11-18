package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import zed.tekkitoverhaul.survival.weather.JetpackWeather;

final class GuiJetpackWeather extends GuiScreen {
    private final GuiScreen parent;

    GuiJetpackWeather(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        controlList.add(new GuiButton(0, width / 2 - 100, height - 32, "Back to Debug Hub"));
    }

    @Override
    protected void actionPerformed(GuiButton button) { mc.displayGuiScreen(parent); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        JetpackWeather gear = JetpackWeather.LOCAL;
        boolean rain = mc.theWorld != null && mc.theWorld.isRaining();
        boolean storm = rain && mc.theWorld.isThundering();
        String[] lines = {
            "Electric Jetpack",
            gear.status(),
            "Equipment " + (gear.equipped() ? "detected" : "absent") + " | Hook " + (gear.available() ? "active" : "inactive"),
            "Weather " + (storm ? "thunderstorm" : rain ? "rain" : "clear")
                    + " | Rain exposure " + (gear.available() ? gear.exposed() ? "yes" : "no" : "-"),
            gear.phase() + " | Wetness " + gear.wetness() + "/100 | Timer " + gear.remaining() + " ticks",
            gear.phase() == JetpackWeather.Phase.FAULT ? "Ascent interrupted. Hold thrust to brake descent." : "Native propulsion"
        };
        for (int i = 0; i < lines.length; ++i) drawCenteredString(fontRenderer, lines[i], width / 2, 25 + i * 14, 0xDFEBE7);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
