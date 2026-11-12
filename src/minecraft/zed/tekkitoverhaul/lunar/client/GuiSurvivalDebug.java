package zed.tekkitoverhaul.lunar.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import zed.tekkitoverhaul.assault.AssaultDiagnostics;
import zed.tekkitoverhaul.assault.SiegeDirector.Eligibility;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.assault.SiegeDirector.Spawn;
import zed.tekkitoverhaul.assault.SiegeDirector.WaveSnapshot;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class GuiSurvivalDebug extends GuiScreen {
    private static final int LINE_HEIGHT = 10;
    private static final int CONTENT_TOP = 51;
    private final LocalLunarCalendar local;
    private final World world;
    private final SurvivalDebugHud hud;
    private GuiButton overlayButton;
    private boolean historyPage;
    private int offset;
    private int maxOffset;
    private List<String> history = Collections.emptyList();
    private List<String> historyRows = Collections.emptyList();
    private int historyWidth;

    GuiSurvivalDebug(LocalLunarCalendar local, World world, SurvivalDebugHud hud) {
        this.local = local;
        this.world = world;
        this.hud = hud;
    }

    @Override
    public void initGui() {
        int x = left();
        controlList.add(new GuiButton(0, x, 24, 58, 20, "Overview"));
        controlList.add(new GuiButton(1, x + 62, 24, 58, 20, "History"));
        overlayButton = new GuiButton(2, right() - 100, 24, 100, 20, overlayLabel());
        controlList.add(overlayButton);
        controlList.add(new GuiButton(3, right() - 56, height - 25, 56, 20, "Close"));
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void updateScreen() {
        if (local.survivalDiagnostics(world) == null) mc.displayGuiScreen(null);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 3) {
            mc.displayGuiScreen(null);
        } else if (button.id == 2) {
            hud.toggleOverlay();
            overlayButton.displayString = overlayLabel();
        } else {
            historyPage = button.id == 1;
            offset = 0;
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (hud.isShortcut(key)) {
            hud.consumeKeyEvent();
            mc.displayGuiScreen(null);
        } else if (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT) {
            scroll((key == Keyboard.KEY_PRIOR ? -1 : 1) * visibleRows());
        } else if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_END) {
            offset = (key == Keyboard.KEY_HOME) == historyPage ? maxOffset : 0;
        } else {
            super.keyTyped(character, key);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll(wheel > 0 ? -3 : 3);
    }

    private void scroll(int rows) {
        offset = Math.max(0, Math.min(maxOffset, offset + (historyPage ? -rows : rows)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        int x = left(), right = right();
        drawString(fontRenderer, "ZTO | Survival Debug", x, 8, 0xFFFFFF);
        drawRect(x, CONTENT_TOP - 3, right, height - 29, 0xB04C4C4C);
        drawRect(x + 1, CONTENT_TOP - 2, right - 1, height - 30, 0xB0121212);

        AssaultDiagnostics diagnostics = local.survivalDiagnostics(world);
        LunarSnapshot moon = local.snapshot(world);
        Snapshot assault = local.assaultSnapshot(world);
        if (diagnostics != null && moon != null && assault != null) {
            int textWidth = right - x - 18;
            Eligibility eligibility = local.assaultEligibility(world);
            String state = SurvivalDebugHud.stateLabel(assault, moon, eligibility);
            List<String> rows = historyPage ? historyRows(diagnostics, textWidth)
                    : overview(moon, assault, eligibility, state, diagnostics, textWidth);
            maxOffset = Math.max(0, rows.size() - visibleRows());
            offset = Math.min(offset, maxOffset);
            int start = historyPage ? maxOffset - offset : offset;
            int end = Math.min(rows.size(), start + visibleRows());
            for (int i = start; i < end; i++) {
                int color = !historyPage && i == 0 ? SurvivalDebugHud.stateColor(state) : 0xDDDDDD;
                drawString(fontRenderer, rows.get(i), x + 6, CONTENT_TOP + 1 + (i - start) * LINE_HEIGHT, color);
            }
            String range = rows.isEmpty() ? "0" : (start + 1) + "-" + end + "/" + rows.size();
            drawString(fontRenderer, (historyPage ? "History" : "Overview") + " | " + range + " | Wheel / PgUp / PgDn",
                    x, height - 19, 0xBBBBBB);
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private List<String> historyRows(AssaultDiagnostics diagnostics, int width) {
        List<String> current = diagnostics.history();
        if (current != history || historyWidth != width) {
            history = current;
            historyWidth = width;
            historyRows = new ArrayList<String>();
            for (String event : history) append(historyRows, event, width);
        }
        return historyRows;
    }

    private List<String> overview(LunarSnapshot moon, Snapshot assault, Eligibility eligibility, String state,
                                  AssaultDiagnostics diagnostics, int width) {
        List<String> rows = new ArrayList<String>();
        append(rows, "PLAYER_ASSAULT | " + state + " | " + AssaultDiagnostics.label(assault.reason), width);
        append(rows, "Lunar phase: " + moon.phase.label + " | Day time: " + moon.timeOfDay + "t", width);
        append(rows, "World time: " + moon.rawTime + "t | Dimension: " + moon.dimension + " | Session: " + moon.sessionId, width);
        append(rows, "Lunar transition: " + (diagnostics.lunarCause() == null ? "None"
                : AssaultDiagnostics.label(diagnostics.lunarCause().name())), width);
        String dusk = eligibility == Eligibility.DUSK ? " in " + (LunarSnapshot.NIGHT_START - moon.timeOfDay) + "t" : "";
        append(rows, "Eligibility: " + eligibility.label + dusk, width);
        append(rows, "PATROL | " + assault.patrol.state + " | " + AssaultDiagnostics.label(assault.patrol.reason), width);
        append(rows, "Patrol night: " + (assault.patrol.eligibleNight ? "Eligible" : "Ineligible")
                + " | Last patrol spawns: " + assault.patrol.spawned + "/" + assault.patrol.budget, width);
        append(rows, "Trigger: " + AssaultDiagnostics.label(assault.trigger) + " | Elapsed: " + assault.elapsed + "t", width);
        append(rows, "Wave: " + assault.currentWave + "/" + assault.waves.size() + " | Next in "
                + (assault.nextWaveTicks < 0 ? "N/A" : assault.nextWaveTicks + "t") + " | Candidate attempts: " + assault.attempts, width);
        int skeletons = 0;
        for (Spawn spawn : assault.spawns) if ("SKELETON".equals(spawn.mob)) skeletons++;
        int spawned = assault.spawns.size();
        append(rows, "Assault spawns: " + spawned + "/" + assault.budget + " | Skeletons: " + skeletons
                + " | Zombies: " + (spawned - skeletons), width);
        append(rows, "Active assault: " + assault.active + "/" + assault.activeCap + " | Peak: " + assault.peakActive
                + " | Nearby hostiles: " + (assault.localHostiles < 0 ? "N/A" : assault.localHostiles + "/" + assault.localCap), width);
        append(rows, "Deferred: " + assault.deferred + " | Skipped: " + assault.skipped, width);
        append(rows, "Deaths observed: " + assault.observedDeaths + " | Removed: " + assault.removed
                + " | Unloaded: " + assault.unloaded, width);
        append(rows, "Last rejection: " + AssaultDiagnostics.label(assault.lastRejection), width);
        if (assault.starts > 0) for (WaveSnapshot wave : assault.waves) {
            append(rows, "Wave " + wave.number + " | " + wave.state + " | " + AssaultDiagnostics.label(wave.outcome), width);
            append(rows, "  Inserted: " + wave.inserted + "/" + wave.allocation + " | Attempts: " + wave.attempts
                    + " | Deferred: " + wave.deferred + " | Skipped: " + wave.skipped + " | Active: " + wave.active, width);
            append(rows, "  Nearby: " + (wave.localHostiles < 0 ? "N/A" : wave.localHostiles) + " | Peak active assault: "
                    + wave.peakActive + " | Carry in/out: " + wave.carriedIn + "/" + wave.carriedOut, width);
            if (!wave.rejections.isEmpty()) append(rows, "  Constraints: " + AssaultDiagnostics.counts(wave.rejections), width);
        }
        if (spawned != 0) {
            Spawn last = assault.spawns.get(spawned - 1);
            append(rows, "Last spawn: " + AssaultDiagnostics.label(last.mob) + " at " + AssaultDiagnostics.position(last), width);
            append(rows, "Horizontal distance at spawn: " + AssaultDiagnostics.distance(last) + " blocks", width);
        }
        return rows;
    }

    private void append(List<String> rows, String line, int width) {
        while (!line.isEmpty()) {
            int fit = SurvivalDebugHud.fittingCharacters(fontRenderer, line, width);
            int space = fit == line.length() ? -1 : line.lastIndexOf(' ', fit);
            int split = space > 0 ? space : Math.max(1, fit);
            rows.add(line.substring(0, split));
            line = line.substring(split).trim();
        }
    }

    private String overlayLabel() { return hud.overlayVisible() ? "Overlay: On" : "Overlay: Off"; }
    private int visibleRows() { return Math.max(1, (height - CONTENT_TOP - 32) / LINE_HEIGHT); }
    private int left() { return Math.max(6, width / 2 - 245); }
    private int right() { return width - left(); }
}
