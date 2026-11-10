package zed.tekkitoverhaul.lunar.client;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.GuiTextField;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class GuiLunarDebug extends GuiScreen {
    private final LocalLunarCalendar local;
    private final World world;
    private GuiTextField delta;
    private GuiButton select;
    private GuiButton reset;
    private LunarPhase selected = LunarPhase.NEW_MOON;
    private boolean confirmReset;
    private String result = "";
    private String preview = "";
    private String jumpTicks = "+24000";
    private int historyOffset;

    GuiLunarDebug(LocalLunarCalendar local, World world) {
        this.local = local;
        this.world = world;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int left = width / 2 - 140;
        int controls = height - 64;
        controlList.add(new GuiButton(0, left, controls, 92, 20, "Next phase"));
        controlList.add(new GuiButton(1, left + 94, controls, 92, 20, "Advance cycle"));
        reset = new GuiButton(2, left + 188, controls, 92, 20, "Reset alignment");
        controlList.add(reset);
        select = new GuiButton(3, left, controls + 21, 154, 20, selected.label + " >");
        controlList.add(select);
        controlList.add(new GuiButton(4, left + 156, controls + 21, 124, 20, "Set phase"));
        delta = new GuiTextField(fontRenderer, left, controls + 43, 154, 18);
        delta.setMaxStringLength(20);
        delta.setText(jumpTicks);
        controlList.add(new GuiButton(5, left + 156, controls + 42, 124, 20, "Simulate time jump"));
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    @Override
    public void updateScreen() {
        if (mc.theWorld != world || world.isRemote) {
            mc.displayGuiScreen(null);
        } else {
            delta.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT) {
            historyOffset = Math.max(0, Math.min(5, historyOffset + (key == Keyboard.KEY_PRIOR ? -1 : 1)));
        } else if (key == Keyboard.KEY_ESCAPE || !delta.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
        jumpTicks = delta.getText();
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        delta.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        LunarCalendar calendar = local.authority(world);
        if (calendar == null) {
            result = "Rejected: lunar authority unavailable";
            return;
        }
        try {
            boolean resetRequested = button.id == 2 && confirmReset;
            confirmReset = button.id == 2 && !resetRequested;
            reset.displayString = confirmReset ? "Confirm reset" : "Reset alignment";
            preview = "";
            switch (button.id) {
                case 0:
                    calendar.advance(1);
                    result = "Applied: next phase";
                    break;
                case 1:
                    calendar.advance(8);
                    result = "Applied: advanced eight days";
                    break;
                case 2:
                    if (resetRequested) calendar.reset();
                    result = resetRequested ? "Applied: original alignment" : "Press Confirm reset to apply";
                    break;
                case 3:
                    selected = LunarPhase.onDay(selected.id + 1);
                    select.displayString = selected.label + " >";
                    result = "Selected: " + selected.label;
                    break;
                case 4:
                    calendar.setPhase(selected);
                    result = "Applied: " + selected.label;
                    break;
                case 5:
                    LunarEvent event = calendar.preview(Long.parseLong(delta.getText().trim()));
                    result = event == null ? "Preview only: no boundary event"
                            : "Preview only: " + event.after.phase.label + " at day " + event.after.logicalDay
                            + " @" + event.after.timeOfDay;
                    if (event != null) {
                        preview = event.cause + " / " + LunarDiagnostics.decision(event)
                                + " / match " + LunarDiagnostics.FULL_MOON_NIGHT.matches(event.after);
                    }
                    break;
                default:
                    return;
            }
        } catch (NumberFormatException invalid) {
            result = "Rejected: enter a signed tick count";
        } catch (IllegalArgumentException invalid) {
            result = "Rejected: " + invalid.getMessage();
        } catch (ArithmeticException invalid) {
            result = "Rejected: time jump exceeds clock range";
        } catch (IllegalStateException invalid) {
            result = "Rejected: " + invalid.getMessage();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        int x = 7;
        int y = 5;
        line("Lunar Debug | Local single-player authority", x, y);
        line("Save: " + local.storageStatus(), x, y += 10);
        line("Renderer: " + LunarMoonRenderer.status(), x, y += 10);
        LunarSnapshot view = local.snapshot(world);
        if (view != null) {
            line(view.phase.label + " | ID " + view.phase.id + " | atlas cell " + LunarMoonRenderer.atlasCell(view), x, y += 10);
            line("Day " + view.logicalDay + " | cycle " + view.cycleIndex + " | position " + view.dayInCycle
                    + " | completed " + view.completedCycles, x, y += 10);
            line("Clock " + view.rawTime + " | time of day " + view.timeOfDay
                    + " | next phase in " + view.ticksToNextPhase + " ticks", x, y += 10);
            line("Session " + view.sessionId + " | dimension " + view.dimension + " | night " + view.night
                    + " | scheduling " + view.schedulingEligible, x, y += 10);
            line("Full Moon [13000, 23000) | match " + LunarDiagnostics.FULL_MOON_NIGHT.matches(view)
                    + " | days to full " + view.daysUntil(LunarPhase.FULL_MOON), x, y += 10);
            LunarDiagnostics diagnostics = local.diagnostics();
            if (diagnostics != null) {
                line("Diagnostic trigger: " + diagnostics.trigger(), x, y += 10);
                line("Transitions (Page Up / Down)", x, y += 10);
                int index = 0;
                for (String entry : diagnostics.history()) {
                    if (index++ < historyOffset) continue;
                    for (Object wrapped : fontRenderer.listFormattedStringToWidth(entry, width - 14)) {
                        if (y + 10 >= height - 84) break;
                        line((String) wrapped, x, y += 10);
                    }
                    if (y + 10 >= height - 84) break;
                }
            }
        } else {
            line("Lunar state unavailable", x, y + 10);
        }
        line(result, x, height - 84);
        line(preview, x, height - 74);
        delta.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void line(String text, int x, int y) {
        drawString(fontRenderer, fontRenderer.trimStringToWidth(text, width - 2 * x), x, y, 0xFFFFFF);
    }
}
