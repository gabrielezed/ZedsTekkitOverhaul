package zed.tekkitoverhaul.lunar.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.aerial.AerialEcology;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;

final class GuiAerialPredatorDebug extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private String[] lines = {};
    private int refresh;

    GuiAerialPredatorDebug(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
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
        refresh = 19;
        AerialEcology.Snapshot ecology = local.aerialSnapshot();
        List<String> display = new ArrayList<String>();
        String height = ecology.height < 0 ? "-" : String.format(Locale.ROOT, "%.1f blocks", ecology.height);
        display.add(String.format(Locale.ROOT, "%s   Clearance %s   Exposure %.0f/100", ecology.band, height, ecology.exposure));
        display.add(ecology.status);
        display.add("Cooldown " + ecology.cooldownSeconds + "s   Check " + ecology.checkSeconds + "s");
        display.add("Last check activity " + ecology.activity + "   Chance " + ecology.chance + "/1000");
        display.add("Last check loaded predators " + ecology.loadedPredators + "   Local hostiles " + ecology.localHostiles);
        display.add("Last attempt: " + ecology.lastAttempt + "  (" + ecology.placementTries + " sites)");
        display.add("Last end: " + ecology.lastEnd);
        String group = ecology.activeMembers > 1 ? "1 flock" : ecology.activeMembers == 1 ? "1 predator" : "No active encounter";
        display.add(group + "   " + ecology.membership);
        if (ecology.activeMembers > 0) {
            display.add(ecology.attackerId < 0 ? "Dive pause: " + ecology.gapTicks + "t"
                    : "Dive reserved by #" + ecology.attackerId);
        }

        EntityAerialPredator nearest = null;
        double best = 48 * 48;
        for (Object entry : world.getEntitiesWithinAABB(EntityAerialPredator.class,
                mc.thePlayer.boundingBox.expand(48, 48, 48))) {
            EntityAerialPredator mob = (EntityAerialPredator) entry;
            double distance = mc.thePlayer.getDistanceSqToEntity(mob);
            if (mob.isDead || distance > best) continue;
            nearest = mob;
            best = distance;
        }
        if (nearest != null) {
            display.add("Nearest #" + nearest.entityId + "  " + nearest.flight() + "  Target " + nearest.targetName());
            display.add(String.format(Locale.ROOT, "Destination %.1f / %.1f / %.1f",
                    nearest.destinationX(), nearest.destinationY(), nearest.destinationZ()));
            display.add("Phase " + nearest.phaseTicks() + "t   Remaining " + nearest.remainingPhaseTicks() + "t");
            display.add(nearest.diveStatus() + " | Dives " + nearest.diveCommitments()
                    + " | Failed warnings " + nearest.rejectedWarnings());
            display.add("Dive rejection: " + nearest.lastDiveRejection());
            display.add("Last movement or attack failure: " + nearest.lastFailure());
            display.add("Last disengagement: " + nearest.lastExit());
        } else {
            display.add("No predator within 48 blocks");
        }
        lines = display.toArray(new String[display.size()]);
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
            drawCenteredString(fontRenderer, lines[i], width / 2, 28 + i * 10, 0xDFEBE7);
        }
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
