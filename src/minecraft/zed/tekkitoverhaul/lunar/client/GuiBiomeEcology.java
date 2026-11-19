package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie;
import zed.tekkitoverhaul.survival.aerial.ForestWildlife;
import zed.tekkitoverhaul.survival.tactical.BiomeEcology;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;

final class GuiBiomeEcology extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private final GuiScreen parent;
    private BiomeEcology.Profile habitat;
    private NaturalSpawnPressure.Snapshot spawn;
    private String magpie;
    private int refreshTicks;

    GuiBiomeEcology(LocalLunarCalendar local, World world, GuiScreen parent) {
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
        else if (++refreshTicks >= 10) refresh();
    }

    private void refresh() {
        refreshTicks = 0;
        habitat = mc.thePlayer == null ? null : BiomeEcology.sample(world, MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.posZ));
        spawn = local.naturalSpawnSnapshot(world);
        magpie = null;
        if (mc.thePlayer == null) return;
        double nearest = 64;
        for (Object entry : world.getEntitiesWithinAABB(EntityMagpie.class, mc.thePlayer.boundingBox.expand(8, 8, 8))) {
            EntityMagpie bird = (EntityMagpie) entry;
            double distance = bird.getDistanceSqToEntity(mc.thePlayer);
            if (bird.isEntityAlive() && distance < nearest) {
                nearest = distance;
                magpie = bird.ecologyStatus();
            }
        }
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
        drawCenteredString(fontRenderer, "Biome Ecology", width / 2, 8, 0xFFFFFF);
        drawRect(6, 21, width - 6, height - 29, 0xB0121212);
        if (habitat == null) line("Biome unavailable", 28);
        else {
            line(habitat.biomeName + " (" + habitat.biomeId + ") | " + habitat.family.label, 28);
            line("Skirmisher / Breacher / Thief / Tank", 44);
            line(habitat.allowsEnderkin() ? habitat.weights() : "Protected habitat", 56);
        }
        if (spawn != null) {
            line(spawn.ecologyEnabled ? spawn.status : "Natural Enderkin disabled in settings", 76);
            line("Pass activity " + (spawn.passActivity < 0 ? "-" : spawn.passActivity)
                    + " | Hostile cap " + (spawn.vanillaCap < 0 ? "-" : spawn.vanillaCap + " -> " + spawn.effectiveCap), 88);
            line("Selected: " + spawn.lastEcologySelection, 108);
            line("Rejected: " + spawn.lastEcologyRejection, 120);
        }
        ForestWildlife wildlife = local.forestWildlife();
        line("Magpies " + wildlife.loaded() + "/" + ForestWildlife.LOADED_CAP + " loaded at last check", 140);
        line(wildlife.status(), 152);
        line(local.aerialSnapshot().preyStatus, 164);
        if (magpie != null && height >= 224) line("Nearby magpie: " + magpie, 176);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void line(String text, int y) { drawString(fontRenderer, fontRenderer.trimStringToWidth(text, width - 24), 12, y, 0xDDDDDD); }
}
