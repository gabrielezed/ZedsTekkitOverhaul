package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.settlement.WorldProvenance;
import zed.tekkitoverhaul.settlement.SettlementRecognition;
import zed.tekkitoverhaul.settlement.SettlementRecognition.UtilitySnapshot;

final class GuiSettlementRecognition extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private WorldProvenance.Snapshot provenance;
    private SettlementRecognition.Snapshot site;
    private int x, y, z, ticks, row;
    private boolean technical;

    GuiSettlementRecognition(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
        this.world = world;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        controlList.add(new GuiButton(0, width / 2 - 106, height - 25, 100, 20, "Debug Hub"));
        controlList.add(new GuiButton(1, width / 2 + 6, height - 25, 100, 20,
                technical ? "Assessment" : "Survey & Storage"));
        refresh();
    }

    @Override
    public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) mc.displayGuiScreen(null);
        else if (++ticks >= 10) refresh();
    }

    private void refresh() {
        ticks = 0;
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            provenance = null;
            site = null;
            return;
        }
        x = MathHelper.floor_double(player.posX);
        y = MathHelper.floor_double(player.boundingBox.minY);
        z = MathHelper.floor_double(player.posZ);
        provenance = local.provenanceSnapshot(world, x, y, z);
        site = local.settlementSnapshot(world, x, y, z);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parent);
        else {
            technical = !technical;
            button.displayString = technical ? "Assessment" : "Survey & Storage";
        }
    }

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
        drawCenteredString(fontRenderer, "Settlement Recognition", width / 2, 8, 0xFFFFFF);
        row = 25;
        if (provenance == null || site == null) {
            line("Waiting for the active local world");
        } else {
            line(site.state + (site.id == 0 ? "" : " | Site #" + site.id + " | " + site.confidence));
            line(site.reason);
            line((site.queued ? "Pending survey | Latest: " : "Survey: ") + site.assessment
                    + (site.assessmentAge < 0 ? "" : " (" + site.assessmentAge + " ticks ago)"));
            row += 5;
            if (technical) drawTechnical();
            else drawAssessment();
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void drawAssessment() {
        line("Position " + x + ", " + y + ", " + z + " | Dimension " + provenance.dimension);
        if (site.id > 0) {
            line("Site bounds " + site.boundary);
            line("Last confirmed site observation: tick " + site.observedTick);
        }
        line("Selected component " + site.component);
        line("Connected construction: " + metric(site.placed) + " matching | " + metric(site.inferred) + " inferred");
        line("Cover above survey anchor: " + metric(site.anchorCover) + "/9 | Decision cover: " + metric(site.cover) + "/9");
        line("Territorial Activity: " + metric(site.activity) + " | Walked columns: " + metric(site.walked));
        line("Utilities: found / linked / recent openings");
        utility("Workbench", site.workbench);
        utility("Furnace", site.furnace);
        utility("Chest", site.chest);
        if (!site.useHook) line("Openings unavailable: use hook inactive");
    }

    private void drawTechnical() {
        if (site.assessedTick >= 0) {
            line("Survey anchor " + site.anchorX + ", " + site.anchorY + ", " + site.anchorZ + " | Feet Y " + site.footY);
            line("Window " + site.extent + " | " + site.sampledLayers + "/9 layers");
            line("Clipping faces: " + site.clipping + " | Peak reads/tick: " + site.peakReads);
        }
        if (site.queued) line("Pending window " + site.pendingExtent);
        line("Registry: " + site.registry + " | " + site.siteCount + " sites");
        line("Provenance storage: " + provenance.sections + " of " + provenance.capacity + " sections used");
        line("Local history (one 8x8x8 section): " + provenance.placed + " placed, "
                + provenance.manualRemoval + " removed, " + provenance.quarryRemoval + " quarry");
        line("At feet (one block): " + provenance.position);
        line("Provenance: " + provenance.status + (provenance.evicted ? " | oldest sections evicted" : ""));
        line("Hooks: place " + hook(1) + ", remove " + hook(2) + ", quarry " + hook(4)
                + ", use " + (site.useHook ? "on" : "off"));
        line("Openings excluded: expired / outside or changed");
        excluded("Workbench", site.workbench);
        excluded("Furnace", site.furnace);
        excluded("Chest", site.chest);
    }

    private void utility(String name, UtilitySnapshot utility) {
        line(name + ": " + metric(utility.found) + " / " + metric(utility.associated) + " / "
                + (site.useHook ? metric(utility.opened) : "?"));
    }

    private void excluded(String name, UtilitySnapshot utility) {
        line(name + ": " + metric(utility.expired) + " / " + metric(utility.outside));
    }

    private String hook(int flag) { return (provenance.hooks & flag) != 0 ? "on" : "off"; }
    private String metric(int value) { return value < 0 ? "?" : Integer.toString(value); }

    private void line(String value) {
        int left = Math.max(8, width / 2 - 165);
        for (Object subline : fontRenderer.listFormattedStringToWidth(value, width - 2 * left)) {
            drawString(fontRenderer, (String) subline, left, row, 0xDDDDDD);
            row += 10;
        }
    }
}
