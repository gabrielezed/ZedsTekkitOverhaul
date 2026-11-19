package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.settlement.WorldProvenance;
import zed.tekkitoverhaul.settlement.SettlementRecognition;

final class GuiSettlementRecognition extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private WorldProvenance.Snapshot provenance;
    private SettlementRecognition.Snapshot site;
    private int x, y, z, ticks, row;

    GuiSettlementRecognition(LocalLunarCalendar local, World world, GuiScreen parent) {
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
        if (mc.theWorld != world || world.isRemote) mc.displayGuiScreen(null);
        else if (++ticks >= 10) refresh();
    }

    private void refresh() {
        ticks = 0;
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        x = MathHelper.floor_double(player.posX);
        y = MathHelper.floor_double(player.boundingBox.minY);
        z = MathHelper.floor_double(player.posZ);
        provenance = local.provenanceSnapshot(world, x, y, z);
        site = local.settlementSnapshot(world, x, y, z);
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
        drawCenteredString(fontRenderer, "Settlement Recognition", width / 2, 8, 0xFFFFFF);
        row = 29;
        if (provenance == null || site == null) {
            line("Waiting for the active local world");
        } else {
            line("Dimension " + provenance.dimension + " | " + x + ", " + y + ", " + z);
            line(site.state + (site.id == 0 ? "" : " | Site #" + site.id) + " | " + site.confidence);
            line(site.reason);
            line("Bounds " + site.boundary);
            line("Last observation tick " + (site.observedTick < 0 ? "-" : site.observedTick));
            line("Activity " + metric(site.activity) + " | visited columns " + metric(site.walked)
                    + " | utility openings " + metric(site.uses));
            line("Connected blocks | matching placement " + metric(site.placed) + ", inferred " + metric(site.inferred));
            line("Cover columns " + metric(site.cover) + "/9 | utility blocks " + metric(site.utilities));
            line("Local history | placed " + provenance.placed + ", removed " + provenance.manualRemoval
                    + ", quarry " + provenance.quarryRemoval);
            line("At feet | " + provenance.position);
            line("Registry " + site.registry + " (" + site.siteCount + ") | provenance " + provenance.status
                    + " (" + provenance.sections + "/" + provenance.capacity + ")");
            line("Hooks | place " + hook(1) + ", remove " + hook(2) + ", quarry " + hook(4)
                    + ", use " + (site.useHook ? "on" : "off"));
            line("Survey " + (site.queued ? "in progress" : "idle") + " | peak block reads/tick " + site.peakReads);
            if (provenance.evicted) line("Older provenance sections were evicted");
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private String hook(int flag) { return (provenance.hooks & flag) != 0 ? "on" : "off"; }
    private String metric(int value) { return value < 0 ? "?" : Integer.toString(value); }

    private void line(String value) {
        drawString(fontRenderer, value, Math.max(8, width / 2 - 152), row, 0xDDDDDD);
        row += 11;
    }
}
