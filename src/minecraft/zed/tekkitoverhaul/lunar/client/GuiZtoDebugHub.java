package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;

final class GuiZtoDebugHub extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;

    GuiZtoDebugHub(LocalLunarCalendar local, World world) {
        this.local = local;
        this.world = world;
    }

    @Override
    public void initGui() {
        int left = width / 2 - 100;
        int top = height / 2 - 96;
        GuiButton lunar = new GuiButton(0, left, top, "Lunar Cycle");
        lunar.enabled = local.service(world) != null;
        controlList.add(lunar);
        GuiButton survival = new GuiButton(1, left, top + 24, "Survival & Assault");
        survival.enabled = local.survivalDiagnostics(world) != null;
        controlList.add(survival);
        controlList.add(new GuiButton(2, left, top + 48, "Territorial Threat Monitor"));
        controlList.add(new GuiButton(4, left, top + 72, "Aerial Predators"));
        controlList.add(new GuiButton(5, left, top + 96, "Electric Jetpack Weather"));
        controlList.add(new GuiButton(6, left, top + 120, "Biome Ecology"));
        controlList.add(new GuiButton(7, left, top + 144, "Settlement Recognition"));
        controlList.add(new GuiButton(3, left, top + 171, "Back to Game"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: mc.displayGuiScreen(new GuiLunarDebug(local, world, this)); break;
            case 1: mc.displayGuiScreen(new GuiSurvivalDebug(local, world, local.survivalHud(), this)); break;
            case 2: mc.displayGuiScreen(new GuiTerritorialThreat(local, world, this)); break;
            case 4: mc.displayGuiScreen(new GuiAerialPredatorDebug(local, world, this)); break;
            case 5: mc.displayGuiScreen(new GuiJetpackWeather(this)); break;
            case 6: mc.displayGuiScreen(new GuiBiomeEcology(local, world, this)); break;
            case 7: mc.displayGuiScreen(new GuiSettlementRecognition(local, world, this)); break;
            case 3: mc.displayGuiScreen(null); break;
            default: break;
        }
    }

    @Override
    public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) mc.displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "ZTO Debug Hub", width / 2, height / 2 - 112, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
