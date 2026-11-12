package zed.tekkitoverhaul.devbridge;

import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.assault.SiegeDirector.Spawn;

final class AssaultObservation {
    private AssaultObservation() { }

    static String read(long worldId, Snapshot view) throws BridgeProtocol.Error {
        if (view == null) throw new BridgeProtocol.Error(409, "NOT_READY", "No assault state for this world");
        StringBuilder json = new StringBuilder("{\"kind\":\"PLAYER_ASSAULT\",\"target\":\"PLAYER_VICINITY\"")
                .append(",\"worldId\":").append(worldId).append(",\"lunarSession\":").append(view.lunarSession)
                .append(",\"dimension\":").append(view.dimension).append(",\"state\":\"").append(view.state)
                .append("\",\"reason\":\"").append(view.reason).append("\",\"boundary\":")
                .append(view.boundary == Long.MIN_VALUE ? "null" : Long.toString(view.boundary))
                .append(",\"starts\":").append(view.starts).append(",\"pulses\":").append(view.pulses)
                .append(",\"attempts\":").append(view.attempts).append(",\"elapsed\":").append(view.elapsed)
                .append(",\"budget\":").append(view.budget).append(",\"lastRejection\":")
                .append(view.lastRejection == null ? "null" : "\"" + view.lastRejection + "\"").append(",\"spawns\":[");
        for (int i = 0; i < view.spawns.size(); i++) {
            Spawn spawn = view.spawns.get(i);
            if (i > 0) json.append(',');
            json.append("{\"mob\":\"").append(spawn.mob).append("\",\"x\":").append(spawn.x)
                    .append(",\"y\":").append(spawn.y).append(",\"z\":").append(spawn.z)
                    .append(",\"horizontalSquared\":").append(spawn.horizontalSquared).append('}');
        }
        return json.append("]}").toString();
    }
}
