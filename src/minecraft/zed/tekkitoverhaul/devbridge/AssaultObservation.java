package zed.tekkitoverhaul.devbridge;

import java.util.Map;
import zed.tekkitoverhaul.assault.SiegeDirector.Snapshot;
import zed.tekkitoverhaul.assault.SiegeDirector.Spawn;
import zed.tekkitoverhaul.assault.SiegeDirector.WaveSnapshot;

final class AssaultObservation {
    private AssaultObservation() { }

    static String read(long worldId, Snapshot view) throws BridgeProtocol.Error {
        if (view == null) throw new BridgeProtocol.Error(409, "NOT_READY", "No assault state for this world");
        StringBuilder json = new StringBuilder("{\"kind\":\"PLAYER_ASSAULT\",\"target\":\"PLAYER_VICINITY\"")
                .append(",\"worldId\":").append(worldId).append(",\"lunarSession\":").append(view.lunarSession)
                .append(",\"dimension\":").append(view.dimension).append(",\"state\":\"").append(view.state)
                .append("\",\"reason\":\"").append(view.reason).append("\",\"boundary\":")
                .append(view.boundary == Long.MIN_VALUE ? "null" : Long.toString(view.boundary))
                .append(",\"starts\":").append(view.starts).append(",\"currentWave\":").append(view.currentWave)
                .append(",\"nextWaveTicks\":").append(view.nextWaveTicks)
                .append(",\"attempts\":").append(view.attempts).append(",\"elapsed\":").append(view.elapsed)
                .append(",\"budget\":").append(view.budget).append(",\"deferred\":").append(view.deferred)
                .append(",\"skipped\":").append(view.skipped).append(",\"active\":").append(view.active)
                .append(",\"peakActive\":").append(view.peakActive).append(",\"localHostiles\":").append(view.localHostiles)
                .append(",\"activeCap\":").append(view.activeCap).append(",\"localCap\":").append(view.localCap)
                .append(",\"innerRadius\":").append(view.innerRadius).append(",\"outerRadius\":").append(view.outerRadius)
                .append(",\"observedDeaths\":").append(view.observedDeaths).append(",\"removed\":").append(view.removed)
                .append(",\"unloaded\":").append(view.unloaded).append(",\"lastRejection\":")
                .append(code(view.lastRejection)).append(",\"waves\":[");
        for (WaveSnapshot wave : view.waves) {
            if (wave.number > 1) json.append(',');
            json.append("{\"number\":").append(wave.number).append(",\"state\":").append(code(wave.state))
                    .append(",\"outcome\":").append(code(wave.outcome)).append(",\"startedAt\":").append(wave.startedAt)
                    .append(",\"closedAt\":").append(wave.closedAt).append(",\"planned\":").append(wave.planned)
                    .append(",\"allocation\":").append(wave.allocation).append(",\"attempts\":").append(wave.attempts)
                    .append(",\"inserted\":").append(wave.inserted).append(",\"active\":").append(wave.active)
                    .append(",\"deferred\":").append(wave.deferred).append(",\"skipped\":").append(wave.skipped)
                    .append(",\"carriedIn\":").append(wave.carriedIn).append(",\"carriedOut\":").append(wave.carriedOut)
                    .append(",\"localHostiles\":").append(wave.localHostiles).append(",\"peakActive\":").append(wave.peakActive)
                    .append(",\"rejections\":{");
            boolean first = true;
            for (Map.Entry<String, Integer> rejection : wave.rejections.entrySet()) {
                if (!first) json.append(',');
                first = false;
                json.append(code(rejection.getKey())).append(':').append(rejection.getValue());
            }
            json.append("}}");
        }
        json.append("],\"spawns\":[");
        for (int i = 0; i < view.spawns.size(); i++) {
            Spawn spawn = view.spawns.get(i);
            if (i > 0) json.append(',');
            json.append("{\"mob\":\"").append(spawn.mob).append("\",\"wave\":").append(spawn.wave)
                    .append(",\"tick\":").append(spawn.tick).append(",\"x\":").append(spawn.x)
                    .append(",\"y\":").append(spawn.y).append(",\"z\":").append(spawn.z)
                    .append(",\"horizontalSquared\":").append(spawn.horizontalSquared).append('}');
        }
        return json.append("]}").toString();
    }

    private static String code(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
