package zed.tekkitoverhaul.lunar.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarEventWindow;
import zed.tekkitoverhaul.lunar.LunarPhase;
import zed.tekkitoverhaul.lunar.LunarService;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

final class LunarDiagnostics implements LunarService.Listener {
    static final LunarEventWindow FULL_MOON_NIGHT = new LunarEventWindow(
            LunarSnapshot.NIGHT_START, LunarSnapshot.NIGHT_END, LunarPhase.FULL_MOON);
    private final Deque<String> history = new ArrayDeque<String>();
    private final boolean verbose;
    private String trigger = "Waiting for a natural night start";

    LunarDiagnostics(boolean verbose) { this.verbose = verbose; }

    @Override
    public void onLunarEvent(LunarEvent event) {
        if (event.kind == LunarEvent.Kind.NIGHT_STARTED || event.kind == LunarEvent.Kind.RESYNC) {
            trigger = decision(event);
        }
        String entry = event.kind + " / " + event.cause + "  "
                + describe(event.before) + " -> " + describe(event.after);
        if (event.kind == LunarEvent.Kind.NIGHT_STARTED || event.kind == LunarEvent.Kind.RESYNC) {
            entry += " [" + trigger + "]";
        }
        if (history.size() == 6) history.removeLast();
        history.addFirst(entry);
        if (verbose) ModLoader.getLogger().info("[ZTO lunar] " + entry);
    }

    List<String> history() { return new ArrayList<String>(history); }
    String trigger() { return trigger; }

    static String decision(LunarEvent event) {
        if (event.cause != LunarEvent.Cause.NATURAL) return "Suppressed: " + event.cause;
        if (event.kind != LunarEvent.Kind.NIGHT_STARTED) return "No night-start boundary";
        return FULL_MOON_NIGHT.matches(event.after) ? "Natural full-moon boundary" : "Suppressed: window mismatch";
    }

    private static String describe(LunarSnapshot view) {
        return view == null ? "none" : "D" + view.logicalDay + "@" + view.timeOfDay
                + " P" + view.phase.id + " dim " + view.dimension
                + (FULL_MOON_NIGHT.matches(view) ? " eligible" : "");
    }
}
