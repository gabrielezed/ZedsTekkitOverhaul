package zed.tekkitoverhaul.devbridge;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BridgeProtocol {
    static final String CAPABILITIES = "{\"access\":\"read\",\"mode\":\"local_singleplayer\",\"operations\":["
            + "{\"op\":\"world.time\",\"schema\":\"world.time@1\"},"
            + "{\"op\":\"world.block\",\"schema\":\"world.block@1\"},"
            + "{\"op\":\"lunar.snapshot\",\"schema\":\"lunar.snapshot@1\"},"
            + operation("lunar.events") + "," + operation("inventory.player") + ","
            + operation("inventory.container") + "," + operation("machine.inspect") + ","
            + operation("test.context") + "]}";

    static String capabilities(boolean testing) {
        if (!testing) return CAPABILITIES;
        return CAPABILITIES.substring(0, CAPABILITIES.length() - 2) + ","
                + operation("fixture.create") + "," + operation("fixture.reset") + ","
                + operation("fixture.destroy") + "," + operation("test.input") + ","
                + operation("hopper.prepare_capacity") + "," + operation("hopper.power_on") + ","
                + operation("hopper.power_off") + "],\"testing\":true}";
    }

    private static String operation(String op) {
        return "{\"op\":\"" + op + "\",\"schema\":\"" + op + "@1\"}";
    }

    // The wire schema uses only ASCII identifiers, flat integer arguments and one nested object.
    private static final String SPACE = "[ \\r\\n\\t]*";
    private static final String STRING = "\"[A-Za-z0-9_.-]{1,64}\"";
    private static final Pattern FIELD = field(STRING + "|\\{[^{}]*\\}");
    private static final Pattern ARG = field("-?(?:0|[1-9][0-9]{0,18})");

    private BridgeProtocol() {}

    private static Pattern field(String value) {
        return Pattern.compile(SPACE + "\"([A-Za-z]+)\"" + SPACE + ":" + SPACE + "(" + value + ")" + SPACE);
    }

    static Query parse(String body) throws Error {
        return parse(body, false);
    }

    static Query parse(String body, boolean command) throws Error {
        Map<String, String> fields = object(body, FIELD);
        String id = identifier(fields.get("requestId"));
        try {
            String op = identifier(fields.get("op"));
            String args = fields.get("args");
            if (fields.size() != 3 || args == null) throw invalid();
            Map<String, String> values = object(args, ARG);
            if (command) {
                boolean input = "test.input".equals(op);
                if (!input && !"fixture.create".equals(op) && !"fixture.reset".equals(op)
                        && !"fixture.destroy".equals(op) && !"hopper.prepare_capacity".equals(op)
                        && !"hopper.power_on".equals(op) && !"hopper.power_off".equals(op)) {
                    throw new Error(404, "UNSUPPORTED", "Unknown command operation");
                }
                if (values.size() != (input ? 5 : 4)) throw invalid();
                int count = input ? integer(values, "count") : 0;
                if (input && (count < 1 || count > 8)) throw invalid();
                return new Query(id, op, 0, 0, 0, 0, positive(values, "worldId"),
                        nonnegative(values, "revision"), positive(values, "sessionId"), positive(values, "runId"), count);
            }
            if ("world.time".equals(op) || "lunar.snapshot".equals(op)
                    || "inventory.player".equals(op) || "test.context".equals(op)) {
                if (!values.isEmpty()) throw invalid();
                return new Query(id, op, 0, 0, 0, 0, 0, 0);
            }
            if ("inventory.container".equals(op) || "machine.inspect".equals(op)) {
                if (values.size() != 5) throw invalid();
                return new Query(id, op, integer(values, "dimension"), integer(values, "x"),
                        integer(values, "y"), integer(values, "z"), positive(values, "worldId"), 0);
            }
            if ("world.block".equals(op)) {
                if (values.size() != 4) throw invalid();
                return new Query(id, op, integer(values, "dimension"), integer(values, "x"),
                        integer(values, "y"), integer(values, "z"), 0, 0);
            }
            if ("lunar.events".equals(op)) {
                if (values.size() != 2) throw invalid();
                long worldId = nonnegative(values, "worldId");
                if (worldId == 0) throw invalid();
                return new Query(id, op, 0, 0, 0, 0, worldId, nonnegative(values, "afterSequence"));
            }
            throw new Error(404, "UNSUPPORTED", "Unknown query operation");
        } catch (Error failure) {
            failure.id = id;
            throw failure;
        }
    }

    private static Map<String, String> object(String input, Pattern field) throws Error {
        String value = input.replaceAll("^" + SPACE + "|" + SPACE + "$", "");
        if (!value.startsWith("{") || !value.endsWith("}")) throw invalid();
        String content = value.substring(1, value.length() - 1);
        Map<String, String> result = new HashMap<String, String>();
        if (content.matches(SPACE)) return result;
        Matcher matcher = field.matcher(content);
        int offset = 0;
        while (matcher.region(offset, content.length()).lookingAt()) {
            if (result.put(matcher.group(1), matcher.group(2)) != null) throw invalid();
            offset = matcher.end();
            if (offset == content.length()) return result;
            if (content.charAt(offset++) != ',') break;
        }
        throw invalid();
    }

    private static String identifier(String value) throws Error {
        if (value == null || !value.matches(STRING)) throw invalid();
        return value.substring(1, value.length() - 1);
    }

    private static int integer(Map<String, String> args, String key) throws Error {
        try {
            return Integer.parseInt(args.get(key));
        } catch (NumberFormatException failure) {
            throw invalid();
        }
    }

    private static long nonnegative(Map<String, String> args, String key) throws Error {
        try {
            long value = Long.parseLong(args.get(key));
            if (value < 0) throw invalid();
            return value;
        } catch (NumberFormatException failure) {
            throw invalid();
        }
    }

    private static long positive(Map<String, String> args, String key) throws Error {
        long value = nonnegative(args, key);
        if (value == 0) throw invalid();
        return value;
    }

    static Error invalid() {
        return new Error(400, "INVALID_ARGUMENT", "The request does not match the v1 schema");
    }

    static String success(String id, Long tick, String data) {
        return prefix(id) + ",\"ok\":true" + (tick == null ? "" : ",\"tick\":" + tick) + ",\"data\":" + data + "}";
    }

    private static String prefix(String id) {
        return "{\"apiVersion\":\"1.0\",\"requestId\":" + (id == null ? "null" : "\"" + id + "\"");
    }

    static final class Error extends Exception {
        final int status;
        final String code;
        String id;

        Error(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        String json() {
            return prefix(id) + ",\"ok\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + getMessage() + "\"}}";
        }
    }

    static final class Query {
        final String id;
        final String op;
        final int dimension, x, y, z;
        final long worldId, afterSequence, sessionId, runId;
        final int count;

        Query(String id, String op, int dimension, int x, int y, int z, long worldId, long afterSequence) {
            this(id, op, dimension, x, y, z, worldId, afterSequence, 0, 0, 0);
        }

        Query(String id, String op, int dimension, int x, int y, int z, long worldId,
                long afterSequence, long sessionId, long runId, int count) {
            this.sessionId = sessionId;
            this.runId = runId;
            this.count = count;
            this.id = id;
            this.op = op;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldId = worldId;
            this.afterSequence = afterSequence;
        }
    }
}
