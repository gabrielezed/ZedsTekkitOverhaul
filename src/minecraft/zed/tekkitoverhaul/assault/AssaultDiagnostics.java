package zed.tekkitoverhaul.assault;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarService;

/** One optional local save session's diagnostic history, owned by the simulation thread. */
public final class AssaultDiagnostics implements LunarService.Listener {
    private final int capacity;
    private final ArrayDeque<String> events = new ArrayDeque<String>();
    private final Map<String, Integer> rejections = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> pulseRejections = new LinkedHashMap<String, Integer>();
    private List<String> history;
    private String lastAssaultEvent = "Waiting for full moon";
    private LunarEvent.Cause lunarCause;

    public AssaultDiagnostics(int capacity) {
        this.capacity = Math.max(20, Math.min(300, capacity));
    }

    public void record(long time, String category, String message) {
        if (events.size() == capacity) events.removeFirst();
        events.addLast("[" + time + "] " + category + " | " + message);
        history = null;
    }

    @Override
    public void onLunarEvent(LunarEvent event) {
        lunarCause = event.cause;
        if (event.kind == LunarEvent.Kind.PHASE_CHANGED || event.kind == LunarEvent.Kind.RESYNC) {
            record(event.after.rawTime, "LUNAR", event.after.phase.label + " / " + label(event.cause.name()));
        } else if (event.kind == LunarEvent.Kind.NIGHT_STARTED && AssaultSchedule.WINDOW.matches(event.after)) {
            record(event.after.rawTime, "LUNAR", "Full moon window opened");
        }
    }

    void resetEncounter() {
        rejections.clear();
        pulseRejections.clear();
    }

    void assault(long time, String message) {
        lastAssaultEvent = message;
        record(time, "ASSAULT", message);
    }

    void beginPulse(long time, int pulse) {
        pulseRejections.clear();
        record(time, "PULSE", "Pulse " + pulse + " started");
    }

    void reject(String reason) {
        increment(rejections, reason);
        increment(pulseRejections, reason);
    }

    void spawned(long time, int pulse, SiegeDirector.Spawn spawn) {
        lastAssaultEvent = label(spawn.mob) + " spawned";
        record(time, "SPAWN", "Pulse " + pulse + ": " + lastAssaultEvent + " at "
                + position(spawn) + ", " + distance(spawn) + " blocks away (horizontal)");
    }

    void endPulse(long time, int pulse, int spawned, int attempts) {
        lastAssaultEvent = "Pulse " + pulse + ": +" + spawned + " assault mobs";
        String message = lastAssaultEvent + " in " + attempts + " attempts";
        if (!pulseRejections.isEmpty()) message += " / " + counts(pulseRejections);
        record(time, "PULSE", message);
    }

    public List<String> history() {
        if (history == null) history = Collections.unmodifiableList(new ArrayList<String>(events));
        return history;
    }

    public String lastAssaultEvent() { return lastAssaultEvent; }
    public LunarEvent.Cause lunarCause() { return lunarCause; }
    public String rejectionSummary() { return rejections.isEmpty() ? "None" : counts(rejections); }

    private static void increment(Map<String, Integer> counts, String key) {
        Integer count = counts.get(key);
        counts.put(key, count == null ? 1 : count + 1);
    }

    private static String counts(Map<String, Integer> counts) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (text.length() > 0) text.append(", ");
            text.append(entry.getValue()).append(' ').append(label(entry.getKey()));
        }
        return text.toString();
    }

    public static String label(String code) {
        if (code == null) return "None";
        String words = code.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    public static String position(SiegeDirector.Spawn spawn) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", spawn.x, spawn.y, spawn.z);
    }

    public static String distance(SiegeDirector.Spawn spawn) {
        return String.format(Locale.ROOT, "%.1f", Math.sqrt(spawn.horizontalSquared));
    }
}
