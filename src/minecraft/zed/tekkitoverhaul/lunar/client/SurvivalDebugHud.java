package zed.tekkitoverhaul.lunar.client;

import net.minecraft.client.Minecraft;
import net.minecraft.src.BaseMod;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.Gui;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.ScaledResolution;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.assault.AssaultDiagnostics;
import zed.tekkitoverhaul.assault.SiegeDirector.Eligibility;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.assault.SiegeDirector.State;
import zed.tekkitoverhaul.assault.SiegeDirector.WaveSnapshot;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class SurvivalDebugHud extends Gui {
    private final LocalLunarCalendar local;
    private final Minecraft game;
    private final KeyBinding key;
    private boolean visible = true;
    private long handledEvent = Long.MIN_VALUE;
    private long polledEvent = Long.MIN_VALUE;

    SurvivalDebugHud(LocalLunarCalendar local, Minecraft game, BaseMod mod, int keyCode, int lunarKey) {
        this.local = local;
        this.game = game;
        if (keyCode > 0 && keyCode < 256) {
            if (keyCode == lunarKey) {
                keyCode = lunarKey == Keyboard.KEY_F10 ? Keyboard.KEY_F9 : Keyboard.KEY_F10;
                ModLoader.getLogger().info("[ZTO] Survival Debug key overlaps Lunar Debug. Using " + Keyboard.getKeyName(keyCode));
            }
            key = new KeyBinding("Open ZTO Survival Debug", keyCode);
            ModLoader.registerKey(mod, key, false);
        } else {
            key = null;
        }
    }

    void onKeyPressed(KeyBinding pressed) {
        if (key == null || key != pressed || game.currentScreen != null
                || local.survivalDiagnostics(game.theWorld) == null || handledEvent == Keyboard.getEventNanoseconds()) return;
        handledEvent = Keyboard.getEventNanoseconds();
        if (GuiSurvivalDebug.isShiftKeyDown()) toggleOverlay();
        else game.displayGuiScreen(new GuiSurvivalDebug(local, game.theWorld, this));
    }

    void pollKey() {
        if (key == null) return;
        long event = Keyboard.getEventNanoseconds();
        if (event == polledEvent) return;
        polledEvent = event;
        if (key.keyCode > 0 && Keyboard.getEventKey() == key.keyCode && Keyboard.getEventKeyState()) onKeyPressed(key);
    }

    boolean isShortcut(int code) { return key != null && key.keyCode > 0 && key.keyCode == code; }

    void consumeKeyEvent() { handledEvent = Keyboard.getEventNanoseconds(); }
    void toggleOverlay() { visible = !visible; }
    boolean overlayVisible() { return visible; }

    void render() {
        if (!visible || game.currentScreen != null || game.gameSettings.hideGUI || game.gameSettings.showDebugInfo) return;
        World world = game.theWorld;
        AssaultDiagnostics diagnostics = local.survivalDiagnostics(world);
        if (diagnostics == null) return;
        LunarSnapshot moon = local.snapshot(world);
        Snapshot assault = local.assaultSnapshot(world);
        if (moon == null || assault == null) return;
        Eligibility eligibility = local.assaultEligibility(world);
        String state = stateLabel(assault, moon, eligibility);
        WaveSnapshot wave = assault.currentWave == 0 ? null : assault.waves.get(assault.currentWave - 1);
        String next = assault.nextWaveTicks < 0 ? "" : " | Next " + ((assault.nextWaveTicks + 19) / 20) + "s";
        String[] lines = {
            "ZTO | Survival Debug",
            moon.phase.label + " | " + moon.timeOfDay + "t",
            "Assault: " + state,
            "Wave " + assault.currentWave + "/" + assault.waves.size() + next,
            wave == null ? "Planned " + assault.budget : "Wave " + wave.inserted + "/" + wave.allocation
                    + " | Tried " + wave.attempts,
            "Total " + assault.spawns.size() + "/" + assault.budget + " | Active " + assault.active,
            "Deferred " + assault.deferred + " | Skipped " + assault.skipped,
            "Last: " + (assault.state == State.ACTIVE && assault.lastRejection != null
                    ? AssaultDiagnostics.label(assault.lastRejection) : diagnostics.lastAssaultEvent())
        };
        FontRenderer font = game.fontRenderer;
        ScaledResolution scale = new ScaledResolution(game.gameSettings, game.displayWidth, game.displayHeight);
        int maxWidth = scale.getScaledWidth() / 2 - 18;
        int contentWidth = 0;
        for (int i = 0; i < lines.length; i++) {
            int width = textWidth(font, lines[i]);
            if (width > maxWidth) {
                int end = fittingCharacters(font, lines[i], maxWidth - textWidth(font, "..."));
                lines[i] = lines[i].substring(0, end) + "...";
                width = textWidth(font, lines[i]);
            }
            contentWidth = Math.max(contentWidth, width);
        }
        int x = 5, y = 5;
        int bottom = y + lines.length * 10 + 8;
        int right = x + contentWidth + 10;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            drawRect(x, y, right, bottom, 0xB0262626);
            drawRect(x + 1, y + 1, right - 1, bottom - 1, 0xA0101010);
            for (int i = 0; i < lines.length; i++) {
                drawString(font, lines[i], x + 5, y + 5 + i * 10, i == 2 ? stateColor(state) : 0xDDDDDD);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static int textWidth(FontRenderer font, String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) width += glyphWidth(font, text.charAt(i));
        return width;
    }

    static int fittingCharacters(FontRenderer font, String text, int width) {
        int i = 0;
        while (i < text.length()) {
            width -= glyphWidth(font, text.charAt(i));
            if (width < 0) break;
            i++;
        }
        return i;
    }

    private static int glyphWidth(FontRenderer font, char glyph) {
        // The 1.2.5 renderer advances four pixels for spaces regardless of the measured width.
        return glyph == ' ' ? 4 : font.getCharWidth(glyph);
    }

    static String stateLabel(Snapshot assault, LunarSnapshot moon, Eligibility eligibility) {
        if (eligibility == Eligibility.DUSK && LunarSnapshot.NIGHT_START - moon.timeOfDay <= 1000) return "Imminent";
        switch (assault.state) {
            case ACTIVE: return "Active";
            case RESOLVED: return "Completed";
            case ABORTED: return "Interrupted";
            default: return eligibility == Eligibility.UNAVAILABLE ? "Unavailable" : "Waiting";
        }
    }

    static int stateColor(String state) {
        if ("Active".equals(state) || "Completed".equals(state)) return 0x86D286;
        if ("Imminent".equals(state)) return 0xF5D67B;
        if ("Interrupted".equals(state)) return 0xE78881;
        return 0xAAAAAA;
    }
}
