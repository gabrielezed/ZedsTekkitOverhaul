package zed.tekkitoverhaul.devbridge;

import zed.tekkitoverhaul.territory.TerritorialActivityService.Snapshot;
import zed.tekkitoverhaul.territory.ThreatAssessment;

final class TerritoryObservation {
    private TerritoryObservation() { }

    static String read(long worldId, Snapshot view) throws BridgeProtocol.Error {
        if (view == null) throw new BridgeProtocol.Error(409, "NOT_READY", "No territorial state for this world");
        return "{\"worldId\":" + worldId + ",\"status\":\"" + view.status + "\",\"dimension\":" + view.dimension
                + ",\"clock\":" + view.clock + ",\"count\":" + view.count + ",\"capacity\":" + view.capacity
                + ",\"presence\":" + view.presence + ",\"returns\":" + view.returns + ",\"localActivity\":" + view.localActivity
                + ",\"observationAge\":" + view.observationAge + ",\"lastEviction\":\"" + view.lastEviction
                + "\",\"threat\":" + threat(view.threat) + "}";
    }

    static String threat(ThreatAssessment view) {
        if (view == null) return "null";
        return "{\"dimension\":" + view.dimension + ",\"regionX\":" + view.regionX + ",\"regionZ\":" + view.regionZ
                + ",\"activity\":" + view.activity + ",\"environmental\":" + view.environmental
                + ",\"environment\":\"" + view.environment + "\",\"value\":" + view.threat
                + ",\"observed\":" + view.observed + "}";
    }
}
