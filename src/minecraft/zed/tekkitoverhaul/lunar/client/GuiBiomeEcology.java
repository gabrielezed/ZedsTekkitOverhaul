package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.AbyssalWorldgen;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.TrenchRegion;
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
    private String trench;
    private String observatory;
    private String observatoryDetails;

    GuiBiomeEcology(LocalLunarCalendar local, World world, GuiScreen parent) {
        this.local = local;
        this.world = world;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        if (Boolean.getBoolean("zto.dev")) {
            int buttonWidth = Math.min(105, (width - 24) / 3);
            controlList.add(new GuiButton(1, width / 2 - buttonWidth * 3 / 2 - 4, height - 25, buttonWidth, 20, "Locate Trench"));
            controlList.add(new GuiButton(2, width / 2 - buttonWidth / 2, height - 25, buttonWidth, 20, "Observatory"));
            controlList.add(new GuiButton(0, width / 2 + buttonWidth / 2 + 4, height - 25, buttonWidth, 20, "Debug Hub"));
        } else {
            controlList.add(new GuiButton(0, width / 2 - 50, height - 25, 100, 20, "Debug Hub"));
        }
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
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(parent);
        } else if (mc.thePlayer != null && mc.theWorld == world && !world.isRemote) {
            int x = MathHelper.floor_double(mc.thePlayer.posX), z = MathHelper.floor_double(mc.thePlayer.posZ);
            if (button.id == 2) {
                ObservatorySites.Site site = ObservatorySites.nearby(world, x, z);
                observatory = site == null ? ObservatorySites.locate(world, x, z) : site.description(x, z);
                observatoryDetails = site == null ? null : site.status();
                trench = null;
                return;
            }
            observatory = null;
            observatoryDetails = null;
            TrenchRegion region = AbyssalWorldgen.locate(world, x, z);
            trench = region == null ? "No planned Trench in search area"
                    : "Planned Trench: " + region.x + ", " + region.z + "  (" + Math.round(Math.hypot(
                            (double) region.x - x, (double) region.z - z)) + " blocks)";
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
        if (trench != null) line(trench, height - 42);
        if (observatory != null) line(observatory, height - (observatoryDetails == null ? 42 : 54));
        if (observatoryDetails != null) line(observatoryDetails, height - 42);
        if (magpie != null && height >= 236) line("Nearby magpie: " + magpie, 176);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void line(String text, int y) { drawString(fontRenderer, fontRenderer.trimStringToWidth(text, width - 24), 12, y, 0xDDDDDD); }
}
