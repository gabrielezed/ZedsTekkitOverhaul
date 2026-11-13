package zed.tekkitoverhaul.assault;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarService;

/** One optional local save session's diagnostic history, owned by the simulation thread. */
public final class AssaultDiagnostics implements LunarService.Listener {
    private final int capacity;
    private final ArrayDeque<String> events = new ArrayDeque<String>();
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

    void assault(long time, String message) {
        lastAssaultEvent = message;
        record(time, "ASSAULT", message);
    }

    void beginWave(long time, AssaultWave wave) {
        assault(time, "Wave " + wave.number + " started with " + wave.allocation() + " planned spawns");
    }

    void spawned(long time, SiegeDirector.Spawn spawn) {
        lastAssaultEvent = label(spawn.mob) + " spawned | " + label(spawn.profile)
                + (spawn.outdoorLight ? " | Outdoor light" : " | Vanilla light");
        record(time, "SPAWN", "Wave " + spawn.wave + ": " + lastAssaultEvent + " at "
                + position(spawn) + ", " + distance(spawn) + " blocks away (horizontal)");
    }

    void endWave(long time, AssaultWave wave) {
        lastAssaultEvent = "Wave " + wave.number + ": " + label(wave.outcome);
        String message = lastAssaultEvent + " | Inserted " + wave.inserted + "/" + wave.allocation()
                + " in " + wave.attempts + " attempts | Skipped " + wave.skipped() + " | Carried " + wave.carriedOut
                + " | Outdoor light " + wave.outdoorLight + " | Vanilla light " + (wave.inserted - wave.outdoorLight)
                + " | Equipped " + wave.equipped + "/" + LoadoutPolicy.assaultWaveCap(wave.number)
                + " | Nearby " + (wave.localHostiles < 0 ? "N/A" : wave.localHostiles) + " | Peak active " + wave.peakActive;
        if (!wave.rejections.isEmpty()) message += " | " + counts(wave.rejections);
        record(time, "WAVE", message);
    }

    public List<String> history() {
        if (history == null) history = Collections.unmodifiableList(new ArrayList<String>(events));
        return history;
    }

    public String lastAssaultEvent() { return lastAssaultEvent; }
    public LunarEvent.Cause lunarCause() { return lunarCause; }
    public static String counts(Map<String, Integer> counts) {
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
