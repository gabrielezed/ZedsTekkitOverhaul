package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.settlement.WorldProvenance;
import zed.tekkitoverhaul.territory.TerritorialActivityService;

final class GuiSettlementRecognition extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private WorldProvenance.Snapshot provenance;
    private TerritorialActivityService.Snapshot territory;
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
        territory = local.territorialSnapshot(world);
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
        if (provenance == null) {
            line("Waiting for the active local world");
        } else {
            line("Dimension " + provenance.dimension + " | " + x + ", " + y + ", " + z);
            line("Territorial Activity: " + (territory == null || territory.threat == null
                    ? "Unavailable" : territory.localActivity));
            line("Settlement classification: pending implementation");
            line("Provenance storage: " + provenance.status + " (" + provenance.sections
                    + "/" + provenance.capacity + " sections)");
            line("Observation at feet: " + provenance.position);
            line("Local 8x8x8 section | observed positions");
            line("Manual placements: " + provenance.placed + " | manual removals: " + provenance.manualRemoval);
            line("BuildCraft quarry removals: " + provenance.quarryRemoval);
            line("Hooks | place " + hook(1) + ", remove " + hook(2) + ", quarry " + hook(4));
            line("Unobserved history: unknown");
            if (provenance.evicted) line("Old sections evicted this session");
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private String hook(int flag) { return (provenance.hooks & flag) != 0 ? "on" : "off"; }

    private void line(String value) {
        drawString(fontRenderer, value, Math.max(8, width / 2 - 152), row, 0xDDDDDD);
        row += 12;
    }
}
