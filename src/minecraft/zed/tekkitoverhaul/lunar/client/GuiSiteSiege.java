package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;
import zed.tekkitoverhaul.assault.SiteSiege;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class GuiSiteSiege extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private SiteSiege.Profile profile = SiteSiege.Profile.AUTO;
    private GuiButton call, cancel, select, details;
    private boolean showDetails;

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
        controlList.add(details = new GuiButton(4, left, height - 25, 80, 20, "Details"));
        controlList.add(new GuiButton(0, left + 82, height - 25, 170, 20, "Back to Debug Hub"));
        refreshControls();
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parent);
        else if (button.id == 1) local.requestTestSiege(world, profile);
        else if (button.id == 2) local.cancelTestSiege(world);
        else if (button.id == 3) {
            SiteSiege.Profile[] profiles = SiteSiege.Profile.values();
            profile = profiles[(profile.ordinal() + 1) % profiles.length];
            select.displayString = profile.name();
        } else if (button.id == 4) {
            showDetails = !showDetails;
            details.displayString = showDetails ? "Overview" : "Details";
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
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Site Siege", width / 2, 9, 0xFFFFFF);
        SiteSiege.Snapshot view = local.siteSiegeSnapshot(world);
        if (view == null) {
            row(0, "No local encounter authority");
        } else if (showDetails) {
            LunarSnapshot lunar = local.snapshot(world);
            row(0, "Major encounter " + view.kind + " | " + local.assaultEligibility(world).label);
            row(1, "Next siege cycle " + number(view.nextOpportunityCycle) + " | boundary " + number(view.boundary)
                    + (view.opportunity ? " (eligible)" : ""));
            row(2, "Cooldown " + cooldown(view.cooldownUntil, lunar));
            row(3, "Dimension " + (view.siteSession == 0 ? "-" : view.dimension) + " | session " + view.siteSession);
            row(4, "Steps " + view.steps + " | arrivals " + view.arrivals + " | geometry age " + view.geometryAge);
            row(5, "Lanes " + view.candidates + " | " + view.columns + " columns | " + view.skyReads + " sky reads");
            row(6, "Peak " + view.peakColumns + " columns / " + view.peakSkyReads + " sky reads");
            row(7, "Protection " + view.protectionReads + " reads | insertion tries " + view.attempts);
            row(8, "Planner " + view.planningNanos / 1000 + " us total | " + view.peakPlanningNanos / 1000 + " us peak");
        } else {
            row(0, "Request " + view.request + " | " + view.requestReason);
            row(1, "Origin " + view.origin + " | encounter " + view.kind);
            row(2, "Site " + (view.site == 0 ? "-" : view.site) + " | " + view.coverage
                    + (view.dormant ? " | dormant" : ""));
            row(3, "Siege " + view.phase + " | " + view.eligibility);
            row(4, "Objective " + view.objective + " | profile " + view.profile);
            row(5, "Approach " + view.approach);
            row(6, "Attackers " + view.active + " active / " + view.inserted + " inserted | " + view.remaining + " left");
            row(7, "Last rejection " + view.rejection);
            row(8, "Last resolution " + view.outcome);
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void row(int index, String value) {
        drawString(fontRenderer, fontRenderer.trimStringToWidth(value, width - 16), 8, 49 + index * 11, 0xDDDDDD);
    }

    private static String number(long value) { return value == Long.MIN_VALUE ? "-" : Long.toString(value); }

    private static String cooldown(long until, LunarSnapshot lunar) {
        if (until == Long.MIN_VALUE || lunar == null || lunar.rawTime >= until) return "clear";
        if (lunar.rawTime < 0 && until > Long.MAX_VALUE + lunar.rawTime) return "active";
        return (until - lunar.rawTime) + " ticks remaining";
    }
}
