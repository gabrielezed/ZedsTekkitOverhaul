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
import zed.tekkitoverhaul.settlement.ProtectedInterior.Assessment;
import zed.tekkitoverhaul.settlement.SiteSnapshot;

final class GuiSettlementRecognition extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private WorldProvenance.Snapshot provenance;
    private SettlementRecognition.Snapshot site;
    private SiteSnapshot footprint;
    private Assessment protection;
    private int x, y, z, ticks, row, page;
    private static final String[] NEXT_PAGE = {"Survey & Storage", "Protection", "Industry", "IC2 Wiring", "Assessment"};

    GuiSettlementRecognition(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
        this.world = world;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        controlList.add(new GuiButton(0, width / 2 - 106, height - 25, 100, 20, "Debug Hub"));
        controlList.add(new GuiButton(1, width / 2 + 6, height - 25, 100, 20,
                NEXT_PAGE[page]));
        refresh();
    }

    @Override
    public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) mc.displayGuiScreen(null);
        else if (++ticks >= 10 || (page == 2 && protection != null && protection.reads == 0)) refresh();
    }

    private void refresh() {
        ticks = 0;
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            provenance = null;
            site = null;
            footprint = null;
            protection = null;
            return;
        }
        x = MathHelper.floor_double(player.posX);
        y = MathHelper.floor_double(player.boundingBox.minY);
        z = MathHelper.floor_double(player.posZ);
        provenance = local.provenanceSnapshot(world, x, y, z);
        site = local.settlementSnapshot(world, x, y, z);
        footprint = site == null || site.id == 0 ? null : local.siteQueries().resolve(world, site.id);
        protection = page == 2 ? local.siteQueries().protection(world, x, y, z) : null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parent);
        else {
            page = (page + 1) % NEXT_PAGE.length;
            button.displayString = NEXT_PAGE[page];
            refresh();
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
            if (page == 1) drawTechnical();
            else if (page == 2) drawProtection();
            else if (page == 3) drawIndustry();
            else if (page == 4) drawWiring();
            else drawAssessment();
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void drawAssessment() {
        line("Position " + x + ", " + y + ", " + z + " | Dimension " + provenance.dimension);
        if (site.id > 0) {
            line("Site envelope " + site.boundary);
            line(site.knownParts + " known parts | " + (footprint == null ? "Unavailable" : footprint.coverage)
                    + " | Last confirmed tick " + site.observedTick);
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

    private void drawProtection() {
        line("Local Protection at " + x + ", " + y + ", " + z);
        if (protection == null) {
            line("UNKNOWN | Awaiting local geometry");
            return;
        }
        line(protection.state + " | " + protection.reason);
        line("Overhead cover: " + protection.overhead);
        line("Enclosure: " + protection.enclosure);
        line("Passable exterior route: " + protection.exteriorRoute);
        line("Local geometry reads: " + protection.reads);
        if (footprint != null) {
            line("Site #" + footprint.id + " | Footprint " + footprint.coverage + " | " + footprint.parts.size() + " known parts");
            line(footprint.contains(x, y, z) ? "Within a known part" : "Outside the known parts");
        }
    }

    private void drawIndustry() {
        if (site.generic.found < 0) return;
        line("Verified placements in window: " + site.verified);
        line("Inferred platform blocks: " + site.contextual + " | Unattributed solid blocks: " + site.unattributed);
        line("Loaded equipment: " + site.generic.found + " | Associated: " + site.generic.associated);
        line("Associated inventory endpoints: " + site.inventories + " | Equipment types: " + site.genericTypes);
        line("Successful device openings: " + (site.useHook ? site.generic.opened + site.ic2.opened : "?"));
        line("Nearby associations: " + site.localAssociations + " | Supporting equipment: " + site.supports);
        line("Verified IC2 paths: " + (site.ic2Available ? site.connections : "?"));
        line("Survey reads for equipment / IC2 details: " + site.genericReads + " / " + site.ic2Reads);
    }

    private void drawWiring() {
        if (!site.ic2Available) {
            line("Pinned IC2 adapter unavailable");
            return;
        }
        if (site.ic2.found < 0) return;
        utility("Machines (found / linked / openings)", site.ic2);
        line("Linked types " + site.machineTypes + " | Matching placements " + site.placedMachines);
        line("Unlinked " + (site.ic2.found - site.ic2.associated) + " | Unsupported " + site.unsupportedIndustrial);
        line("Cables " + site.cables + " | Linked clusters " + site.clusters + " | Local pairs " + site.connections);
        if (site.limitedNetworks > 0) line("Overlong or clipped cable groups " + site.limitedNetworks);
        if (site.connections == 0 && site.cables > 0) line("No verified local source-to-sink path");
        if (site.useHook) line("Excluded openings (expired / unlinked or changed): " + site.ic2.expired + " / " + site.ic2.outside);
        else line("Openings unavailable: use hook inactive");
        if (site.networkUsed) line("Local wiring corroborated the decision");
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
