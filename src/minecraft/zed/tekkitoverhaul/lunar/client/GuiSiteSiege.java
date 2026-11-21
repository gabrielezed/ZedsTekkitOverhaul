package zed.tekkitoverhaul.lunar.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;
import zed.tekkitoverhaul.assault.SiegeDirector.DebugState;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class GuiSiteSiege extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private SiteSiege.Profile profile = SiteSiege.Profile.AUTO;
    private SiteSiege.Intensity intensity = SiteSiege.Intensity.NATURAL;
    private GuiButton call, cancel, select, strength, details;
    private int page;

    GuiSiteSiege(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
        this.world = world;
        this.parent = parent;
    }

    @Override public void initGui() {
        int left = width / 2 - 126;
        controlList.add(call = new GuiButton(1, left, 23, 80, 20, "Call Site Siege"));
        controlList.add(cancel = new GuiButton(2, left + 82, 23, 91, 20, "Cancel Test Siege"));
        controlList.add(select = new GuiButton(3, left + 175, 23, 77, 20, profile.name()));
        controlList.add(strength = new GuiButton(5, left, 45, 132, 20, intensity.name()));
        controlList.add(details = new GuiButton(4, left, height - 25, 80, 20, "Next page"));
        controlList.add(new GuiButton(0, left + 82, height - 25, 170, 20, "Back to Debug Hub"));
        refreshControls();
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parent);
        else if (button.id == 1) local.requestTestSiege(world, profile, intensity);
        else if (button.id == 2) local.cancelTestSiege(world);
        else if (button.id == 3) {
            SiteSiege.Profile[] profiles = SiteSiege.Profile.values();
            profile = profiles[(profile.ordinal() + 1) % profiles.length];
            select.displayString = profile.name();
        } else if (button.id == 4) {
            SiteSiege.Snapshot view = local.siteSiegeSnapshot(world);
            page = (page + 1) % pages(view);
        } else if (button.id == 5) {
            SiteSiege.Intensity[] choices = SiteSiege.Intensity.values();
            intensity = choices[(intensity.ordinal() + 1) % choices.length];
            strength.displayString = intensity.name();
        }
        refreshControls();
    }

    @Override public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) mc.displayGuiScreen(null);
        else refreshControls();
    }

    private void refreshControls() {
        SiteSiege.Snapshot view = local.siteSiegeSnapshot(world);
        call.enabled = view != null && view.callAvailable;
        cancel.enabled = view != null && view.cancelAvailable;
        select.enabled = call.enabled;
        strength.enabled = call.enabled;
        details.enabled = view != null;
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Site Siege", width / 2, 9, 0xFFFFFF);
        SiteSiege.Snapshot view = local.siteSiegeSnapshot(world);
        if (view == null) {
            row(0, "No local encounter authority");
        } else {
            page = Math.min(page, pages(view) - 1);
            String section = page == 0 ? "Overview" : page == 1 ? "Force" : "Diagnostics";
            drawString(fontRenderer, section + " " + (page + 1) + "/" + pages(view),
                    width / 2 + 11, 51, 0xDDDDDD);
            if (page == 0) overview(view);
            else if (page == 1) force(view);
            else diagnostics(view);
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void overview(SiteSiege.Snapshot view) {
        String reason = "NONE".equals(view.requestReason) ? view.callBlock : view.requestReason;
        row(0, view.request + " | " + view.origin + (reason == null ? "" : " | " + reason));
        row(1, "Site " + (view.site == 0 ? "-" : view.site) + " | " + view.coverage
                + (view.dormant ? " | dormant" : ""));
        String difficulty = view.difficulty == 3 ? "HARD" : view.difficulty == 2 ? "NORMAL"
                : view.difficulty == 1 ? "EASY" : "-";
        row(2, difficulty + " | " + view.intensity + (view.overridden ? " (override)" : " (natural)")
                + " | threat " + (view.threat < 0 ? "-" : view.threat));
        row(3, view.planned + " planned | " + view.activeCap + " active cap | wave " + view.wave + "/" + view.waves);
        row(4, view.objective + " | " + view.tacticalPhase);
        row(5, view.approach);
        row(6, "Objective " + view.objectiveOutcome);
        row(7, "Outcome " + view.outcome + " | " + view.duration / 20 + " s");
        row(8, "Last rejection " + view.rejection);
        row(9, "Deployment " + view.eligibility);
    }

    private void force(SiteSiege.Snapshot view) {
        row(0, view.attempts + " attempts | " + view.inserted + " inserted | " + view.active + " active");
        row(1, view.defeated + " eliminated | " + view.withdrawn + " withdrawn | peak " + view.peak);
        row(2, view.deferred + " deferred | " + view.remaining + " reserves | " + view.abandoned + " undeployed");
        for (int i = 0; i < view.allocations.size(); i++) row(3 + i, "Wave " + (i + 1) + " " + view.allocations.get(i)
                .replace("ZOMBIE", "Zombie").replace("SKELETON", "Skeleton").replace("SKIRMISHER", "Skirm.")
                .replace("BREACHER", "Breach.").replace("TANK", "Tank").replace("THIEF", "Thief"));
        row(6, "Skirmisher   Breacher   Tank   Thief");
        row(7, "Active / inserted / planned");
        row(8, role(view, "SKIRMISHER") + "       " + role(view, "BREACHER") + "       "
                + role(view, "TANK") + "      " + role(view, "THIEF"));
        row(9, "Zombies " + role(view, "ZOMBIE") + " | Skeletons " + role(view, "SKELETON"));
    }

    private static String role(SiteSiege.Snapshot view, String role) {
        return roleCount(view.activeRoles, role) + "/" + roleCount(view.insertedRoles, role)
                + "/" + roleCount(view.plannedRoles, role);
    }

    private static int roleCount(Map<String, Integer> counts, String role) {
        Integer count = counts.get(role);
        return count == null ? 0 : count;
    }

    private List<String> detailRows(SiteSiege.Snapshot view) {
        List<String> rows = new ArrayList<String>();
        LunarSnapshot lunar = local.snapshot(world);
        rows.add("Next cycle " + number(view.nextOpportunityCycle) + " | boundary " + number(view.boundary));
        rows.add("Cooldown " + cooldown(view.cooldownUntil, lunar));
        rows.add("Routes " + view.validRoutes + " valid / " + view.failedRoutes + " failed | " + view.planningColumns + " samples");
        rows.add("Native despawns " + view.nativeDespawns);
        rows.add("Sectors " + view.sectors);
        rows.add("Saboteur active/inserted/planned " + role(view, "SABOTEUR"));
        rows.add("Industry " + view.industry);
        rows.add("Target " + view.objectiveTarget + " | " + view.specialist);
        rows.add("Structural " + view.structuralChanges + "/" + view.structuralBudget + " removed | "
                + view.entranceChanges + " opened");
        rows.add("Steps " + view.steps + " | arrivals " + view.arrivals + " | geometry age " + view.geometryAge);
        rows.add("Planner " + view.peakPlanningNanos / 1000 + " us peak | path " + view.peakPathNanos / 1000 + " us peak");
        rows.add("Siege tick " + view.peakTickNanos / 1000 + " us peak | lifetime " + view.peakLifetime / 20 + " s");
        for (int i = 0; i < view.deployment.size(); i++)
            rows.add("Wave " + (i + 1) + " | " + view.deployment.get(i));
        for (Map.Entry<String, Integer> failure : view.failures.entrySet())
            rows.add(failure.getValue() + " " + failure.getKey());
        for (Map.Entry<String, Integer> failure : view.waveFailures.entrySet())
            rows.add(failure.getValue() + " " + failure.getKey());
        return rows;
    }

    private void diagnostics(SiteSiege.Snapshot view) {
        List<String> lines = detailRows(view);
        int start = (page - 2) * rowsPerPage();
        for (int i = start; i < Math.min(start + rowsPerPage(), lines.size()); i++) row(i - start, lines.get(i));
    }

    private int rowsPerPage() { return Math.max(1, (height - 98) / 10); }

    private int pages(SiteSiege.Snapshot view) {
        if (view == null) return 1;
        int rows = rowsPerPage();
        return 2 + (detailRows(view).size() + rows - 1) / rows;
    }

    private void row(int index, String value) {
        int y = 68 + index * 10;
        if (y + 8 < height - 25)
            drawString(fontRenderer, fontRenderer.trimStringToWidth(value, width - 16), 8, y, 0xDDDDDD);
    }

    private static String number(long value) { return value == Long.MIN_VALUE ? "-" : Long.toString(value); }

    private static String cooldown(long until, LunarSnapshot lunar) {
        if (until == Long.MIN_VALUE || lunar == null || lunar.rawTime >= until) return "clear";
        if (lunar.rawTime < 0 && until > Long.MAX_VALUE + lunar.rawTime) return "active";
        return (until - lunar.rawTime) + " ticks remaining";
    }
}
