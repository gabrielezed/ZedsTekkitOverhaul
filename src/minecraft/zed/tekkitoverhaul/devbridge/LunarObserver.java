package zed.tekkitoverhaul.devbridge;

import java.util.ArrayDeque;
import java.util.Deque;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarService;
import zed.tekkitoverhaul.lunar.LunarSnapshot;

/** Game-thread observer. Cursors belong to one active bridge world instance. */
final class LunarObserver implements LunarService.Listener {
    private static final int CAPACITY = 8;
    private final Deque<LunarEvent> events = new ArrayDeque<LunarEvent>(CAPACITY);
    private LunarService service;
    private long worldId;
    private long sequence;

    void bind(long id, LunarService next) {
        if (worldId == id && service == next) return;
        if (service != null) service.unsubscribe(this);
        service = next;
        worldId = id;
        sequence = 0;
        events.clear();
        if (service != null) service.subscribe(this);
    }

    @Override
    public void onLunarEvent(LunarEvent event) {
        if (events.size() == CAPACITY) events.removeFirst();
        events.addLast(event);
        sequence++;
    }

    String snapshot() throws BridgeProtocol.Error {
        LunarSnapshot view = requireSnapshot();
        return "{\"worldId\":" + worldId + ",\"sessionId\":" + view.sessionId
                + ",\"dimension\":" + view.dimension + ",\"rawTime\":" + view.rawTime
                + ",\"timeOfDay\":" + view.timeOfDay + ",\"logicalDay\":" + view.logicalDay
                + ",\"dayInCycle\":" + view.dayInCycle + ",\"cycleIndex\":" + view.cycleIndex
                + ",\"completedCycles\":" + view.completedCycles + ",\"phase\":\"" + view.phase.name()
                + "\",\"phaseId\":" + view.phase.id + ",\"night\":" + view.night
                + ",\"ticksToNextPhase\":" + view.ticksToNextPhase + ",\"schedulingEligible\":" + view.schedulingEligible
                + ",\"latestSequence\":" + sequence + "}";
    }

    String poll(long id, long after) throws BridgeProtocol.Error {
        if (worldId != id) throw new BridgeProtocol.Error(409, "STALE_CURSOR", "Use the active bridge world ID");
        requireSnapshot();
        if (after > sequence) throw new BridgeProtocol.Error(409, "STALE_CURSOR", "The sequence is ahead of the observer");
        long first = sequence - events.size() + 1;
        long dropped = Math.max(0, first - 1 - after);
        StringBuilder json = new StringBuilder("{\"worldId\":").append(worldId)
                .append(",\"latestSequence\":").append(sequence).append(",\"dropped\":").append(dropped)
                .append(",\"events\":[");
        long number = first;
        boolean comma = false;
        for (LunarEvent event : events) {
            if (number > after) {
                if (comma) json.append(',');
                comma = true;
                json.append("{\"sequence\":").append(number)
                        .append(",\"kind\":\"").append(event.kind.name())
                        .append("\",\"cause\":\"").append(event.cause.name())
                        .append("\",\"before\":").append(position(event.before))
                        .append(",\"after\":").append(position(event.after))
                        .append(",\"dayDelta\":").append(event.dayDelta)
                        .append(",\"cycleDelta\":").append(event.cycleDelta)
                        .append(",\"nightStartsCrossed\":").append(event.nightStartsCrossed)
                        .append(",\"nightEndsCrossed\":").append(event.nightEndsCrossed)
                        .append('}');
            }
            number++;
        }
        return json.append("]}").toString();
    }

    private LunarSnapshot requireSnapshot() throws BridgeProtocol.Error {
        LunarSnapshot view = service == null ? null : service.snapshot();
        if (view == null) throw new BridgeProtocol.Error(409, "LUNAR_UNAVAILABLE", "No validated local lunar calendar");
        return view;
    }

    private static String position(LunarSnapshot view) {
        if (view == null) return "null";
        return "{\"rawTime\":" + view.rawTime + ",\"logicalDay\":" + view.logicalDay
                + ",\"phaseId\":" + view.phase.id + ",\"dimension\":" + view.dimension
                + ",\"schedulingEligible\":" + view.schedulingEligible + "}";
    }
}
