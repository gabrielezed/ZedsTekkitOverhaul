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

    GuiSiteSiege(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
        this.world = world;
        this.parent = parent;
    }

    @Override public void initGui() {
        controlList.add(new GuiButton(0, width / 2 - 100, height - 27, "Back to Debug Hub"));
    }

    @Override protected void actionPerformed(GuiButton button) { mc.displayGuiScreen(parent); }

    @Override public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) mc.displayGuiScreen(null);
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Site Siege", width / 2, 9, 0xFFFFFF);
        SiteSiege.Snapshot view = local.siteSiegeSnapshot(world);
        if (view == null) {
            row(0, "No local encounter authority");
        } else {
            LunarSnapshot lunar = local.snapshot(world);
            row(0, "Major encounter " + view.kind + " | " + local.assaultEligibility(world).label);
            row(1, "Siege " + view.phase + " | " + view.eligibility);
            row(2, "Next siege cycle " + number(view.nextOpportunityCycle) + " | last dusk " + number(view.boundary)
                    + (view.opportunity ? " (eligible)" : ""));
            String siteLabel = view.phase.equals("ACTIVE") || view.phase.equals("PLANNING") ? "Site " : "Last site ";
            row(3, siteLabel + (view.site == 0 ? "-" : view.site) + " | " + view.identity + " | " + view.coverage
                    + (view.dormant ? " | dormant" : ""));
            row(4, "Dimension " + (view.siteSession == 0 ? "-" : view.dimension) + " | session " + view.siteSession);
            row(5, "Approach " + view.approach + " | age " + view.geometryAge);
            row(6, "Objective " + view.objective + " | " + view.steps + " steps | " + view.arrivals + " arrived");
            row(7, "Attackers " + view.active + " active / " + view.inserted + " inserted | " + view.remaining + " left");
            row(8, "Elapsed " + view.elapsed + " ticks | last outcome " + view.outcome);
            row(9, "Last rejection " + view.rejection);
            row(10, "Cooldown " + cooldown(view.cooldownUntil, lunar));
            row(11, "Samples " + view.candidates + " lanes | " + view.columns + " columns | " + view.skyReads + " sky reads");
            row(12, "Peak " + view.peakColumns + " columns / " + view.peakSkyReads + " sky reads per sample");
            row(13, "Protection " + view.protectionReads + " reads | insertion tries " + view.attempts);
            row(14, "Planner " + view.planningNanos / 1000 + " us total | " + view.peakPlanningNanos / 1000 + " us peak");
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void row(int index, String value) {
        drawString(fontRenderer, fontRenderer.trimStringToWidth(value, width - 16), 8, 25 + index * 11, 0xDDDDDD);
    }

    private static String number(long value) { return value == Long.MIN_VALUE ? "-" : Long.toString(value); }

    private static String cooldown(long until, LunarSnapshot lunar) {
        if (until == Long.MIN_VALUE || lunar == null || lunar.rawTime >= until) return "clear";
        if (lunar.rawTime < 0 && until > Long.MAX_VALUE + lunar.rawTime) return "active";
        return (until - lunar.rawTime) + " ticks remaining";
    }
}
