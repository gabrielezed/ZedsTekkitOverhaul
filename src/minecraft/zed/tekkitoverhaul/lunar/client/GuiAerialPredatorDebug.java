package zed.tekkitoverhaul.lunar.client;

import java.util.Locale;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;

final class GuiAerialPredatorDebug extends GuiScreen {
    private final World world;
    private final GuiScreen parent;
    private String[] lines = {};
    private int refresh;

    GuiAerialPredatorDebug(World world, GuiScreen parent) {
        this.world = world;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        controlList.add(new GuiButton(0, width / 2 - 100, height - 29, "Back to Debug Hub"));
        refresh = 0;
        updateScreen();
    }

    @Override
    public void updateScreen() {
        if (mc.theWorld != world || mc.thePlayer == null || world.isRemote) {
            mc.displayGuiScreen(null);
            return;
        }
        if (refresh-- > 0) return;
        refresh = 9;
        EntityAerialPredator nearest = null;
        double best = 48 * 48;
        int population = 0;
        for (Object entry : world.getEntitiesWithinAABB(EntityAerialPredator.class,
                mc.thePlayer.boundingBox.expand(48, 48, 48))) {
            EntityAerialPredator mob = (EntityAerialPredator) entry;
            double distance = mc.thePlayer.getDistanceSqToEntity(mob);
            if (mob.isDead || distance > 48 * 48) continue;
            ++population;
            if (distance > best) continue;
            nearest = mob;
            best = distance;
        }
        if (nearest == null) {
            lines = new String[] {"No aerial predator within 48 blocks", "Placement via Creative spawn egg"};
            return;
        }
        double clearance = nearest.terrainClearance();
        lines = new String[] {
            "Predators within 48 blocks: " + population,
            "Nearest #" + nearest.entityId + "  " + nearest.flight() + "  Target: " + nearest.targetName(),
            String.format(Locale.ROOT, "Y %.1f  Terrain clearance: %s", nearest.posY,
                    clearance < 0 ? "covered or unavailable" : String.format(Locale.ROOT, "%.1f blocks", clearance)),
            String.format(Locale.ROOT, "Destination: %.1f / %.1f / %.1f",
                    nearest.destinationX(), nearest.destinationY(), nearest.destinationZ()),
            "Phase: " + nearest.phaseTicks() + " ticks  Remaining: " + nearest.remainingPhaseTicks(),
            "Last movement or attack failure: " + nearest.lastFailure(),
            "Last disengagement: " + nearest.lastExit()
        };
    }

    @Override
    protected void actionPerformed(GuiButton button) { mc.displayGuiScreen(parent); }

    @Override
    protected void keyTyped(char key, int code) {
        if (code == 1) mc.displayGuiScreen(parent);
        else super.keyTyped(key, code);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Aerial Predator", width / 2, 14, 0xFFFFFF);
        for (int i = 0; i < lines.length; i++) {
            drawCenteredString(fontRenderer, lines[i], width / 2, 34 + i * 12, 0xDFEBE7);
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
