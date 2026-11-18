package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.assault.SiegeDirector;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.territory.ThreatAssessment;

final class GuiTerritorialThreat extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private TerritorialActivityService.Snapshot territory;
    private NaturalSpawnPressure.Snapshot spawning;
    private SiegeDirector.Snapshot assault;
    private int refreshTicks;
    private int row;

    GuiTerritorialThreat(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
        this.world = world;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        controlList.add(new GuiButton(0, width / 2 - 50, height - 25, 100, 20, "Debug Hub"));
        refresh();
    }

    @Override
    public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) {
            mc.displayGuiScreen(null);
        } else if (++refreshTicks >= 10) {
            refresh();
        }
    }

    private void refresh() {
        refreshTicks = 0;
        territory = local.territorialSnapshot(world);
        spawning = local.naturalSpawnSnapshot(world);
        assault = local.assaultSnapshot(world);
    }

    @Override
    protected void actionPerformed(GuiButton button) { mc.displayGuiScreen(parent); }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(character, key);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Territorial Threat Monitor", width / 2, 8, 0xFFFFFF);
        drawRect(6, 21, width - 6, height - 29, 0xB0121212);
        row = 26;
        drawTerritory();
        row += 4;
        drawSpawning();
        row += 4;
        drawAssault();
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void drawTerritory() {
        heading("Current territory");
        if (territory == null) {
            line("Waiting for local world");
            return;
        }
        line("Dimension " + territory.dimension + " | Storage " + territory.status
                + " | Cells " + territory.count + "/" + territory.capacity);
        ThreatAssessment threat = territory.threat;
        if (threat == null) {
            line("Assessment unavailable for this player or dimension");
            return;
        }
        line("Cell " + threat.regionX + ", " + threat.regionZ + " | Local activity " + territory.localActivity
                + " | Blended " + threat.activity);
        line("Presence " + territory.presence + " | Returns " + territory.returns
                + " | Last seen " + (threat.observed ? territory.observationAge / 20 + "s ago" : "never"));
        line("Environment " + threat.environmental + " (" + threat.environment + ") | Threat "
                + threat.threat + "/" + ThreatAssessment.MAX);
        line("Absence decay: presence -1/" + territory.presenceDecayTicks / 20 + "s, returns -1/"
                + territory.returnDecayTicks / 20 + "s");
    }

    private void drawSpawning() {
        heading("Ordinary hostile spawning | Latest pass");
        if (spawning == null) {
            line("Waiting for local world");
            return;
        }
        line(spawning.status);
        line("Configured max bonus +" + spawning.maxIncreasePercent + "%");
        if (spawning.count < 0) return;
        line("Loaded at start " + spawning.count + " | Vanilla cap " + spawning.vanillaCap
                + " | Effective " + spawning.effectiveCap);
        line("Applied bonus +" + spawning.increasePercent + "% | Pass activity "
                + (spawning.passActivity < 0 ? "-" : spawning.passActivity));
        line("Inserted " + spawning.successfulSpawns + " | Cap-limited passes " + spawning.limitedPasses
                + " (world session)");
    }

    private void drawAssault() {
        heading("Lunar assault | Captured at admission");
        line("Territorial adaptation " + (local.territorialAssaultsEnabled() ? "enabled" : "disabled"));
        if (assault == null || assault.starts == 0) {
            line("No recorded admission in this dimension session");
            return;
        }
        ThreatAssessment frozen = assault.admissionThreat;
        line("Captured threat " + (frozen == null ? "unavailable" : frozen.threat + "/" + ThreatAssessment.MAX
                + " | Cell " + frozen.regionX + ", " + frozen.regionZ));
        line(assault.admissionProfile + " | Budget " + assault.budget + "/" + assault.configuredBudget);
    }

    private void heading(String text) { print(text, 0xFFD48A); }
    private void line(String text) { print(text, 0xE0E0E0); }

    private void print(String text, int color) {
        drawString(fontRenderer, fontRenderer.trimStringToWidth(text, width - 24), 12, row, color);
        row += 10;
    }
}
