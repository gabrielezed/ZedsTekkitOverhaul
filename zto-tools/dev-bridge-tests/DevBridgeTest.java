package zed.tekkitoverhaul.devbridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zed.tekkitoverhaul.lunar.LunarCalendar;
import zed.tekkitoverhaul.lunar.LunarEvent;

public final class DevBridgeTest {
    private static final String QUERY = "{\"requestId\":\"check\",\"op\":\"world.time\",\"args\":{}}";
    private static int port;
    private static String token;

    public static void main(String[] args) throws Exception {
        lunarObserver();
        BridgeDispatcher dispatcher = new BridgeDispatcher();
        BridgeServer server = new BridgeServer(0, dispatcher, "test");
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            String discovery = new String(Files.readAllBytes(server.discovery()), StandardCharsets.US_ASCII);
            port = Integer.parseInt(field(discovery, "\"port\":([0-9]+)"));
            token = field(discovery, "\"token\":\"([A-Za-z0-9_-]{43})\"");
            expect("/health", null, null, 401, "UNAUTHORIZED");
            expect("/health", null, token, 200, "\"alive\":true");
            expect("/capabilities", null, token, 200, "world.block@1");
            expect("/capabilities", null, token, 200, "lunar.events@1");
            expect("/status", null, token, 200, "no_world");
            expect("/query", QUERY, token, 503, "NOT_READY");
            expect("/query", "{", token, 400, "INVALID_ARGUMENT");
            expect("/query", QUERY.replace("\"args\":{}", "\"args\":{},\"args\":{}"), token, 400, "INVALID_ARGUMENT");
            expect("/query", new String(new char[1025]).replace('\0', 'x'), token, 413, "REQUEST_TOO_LARGE");
            expect("/query", QUERY.replace("world.time", "world.players"), token, 404, "UNSUPPORTED");
            expect("/command", "{}", token, 404, "UNSUPPORTED");

            dispatcher.observe("ready", 0, 1, 7);
            Future<String> first = callers.submit(() -> expect("/query", QUERY, token, 200, "\"tick\":7"));
            Future<String> second = callers.submit(() -> expect("/query", QUERY, token, 200, "\"tick\":7"));
            waitForQueue(dispatcher, 2);
            expect("/query", QUERY, token, 503, "BUSY");
            Thread owner = Thread.currentThread();
            dispatcher.drain(query -> {
                check(Thread.currentThread() == owner, "Snapshot was read on the wrong thread");
                return "{\"worldTime\":42}";
            });
            String sample = first.get(2, TimeUnit.SECONDS);
            check(sample.contains("\"requestId\":\"check\"") && sample.contains("\"worldTime\":42"), "Bad dispatch result");
            second.get(2, TimeUnit.SECONDS);

            dispatcher.observe("ready", 0, 1, 8);
            Future<String> stale = callers.submit(() -> expect("/query", QUERY, token, 503, "NOT_READY"));
            waitForQueue(dispatcher, 1);
            dispatcher.observe("ready", -1, 2, 9);
            dispatcher.drain(query -> { throw new AssertionError("An old world query survived the transition"); });
            stale.get(2, TimeUnit.SECONDS);

            dispatcher.observe("paused", -1, 2, 9);
            expect("/status", null, token, 200, "paused");
            expect("/query", QUERY, token, 503, "NOT_READY");
            dispatcher.observe("ready", -1, 2, 10);
            expect("/query", QUERY, token, 504, "TIMEOUT");
            check(dispatcher.status("test").contains("\"queueDepth\":0"), "Timed-out query was retained");
            dispatcher.drain(query -> { throw new AssertionError("An expired query was serviced"); });

            Thread.sleep(1050);
            for (int i = 0; i < 32; i++) expect("/health", null, token, 200, "alive");
            expect("/health", null, token, 429, "RATE_LIMITED");
            System.out.println(sample);
        } finally {
            server.close();
            callers.shutdownNow();
        }
        check(!Files.exists(server.discovery()), "Discovery file was retained");
        try (BridgeServer restarted = new BridgeServer(port, new BridgeDispatcher(), "test")) {
            expect("/health", null, token, 401, "UNAUTHORIZED");
        }
        System.out.println("Dev Bridge transport, dispatch, deadlines, world transitions and restart passed.");
    }

    private static void lunarObserver() throws Exception {
        String cursor = QUERY.replace("world.time", "lunar.events")
                .replace("{}", "{\"worldId\":1,\"afterSequence\":0}");
        BridgeProtocol.Query query = BridgeProtocol.parse(cursor);
        check(query.worldId == 1 && query.afterSequence == 0, "Lunar cursor parsing");
        try {
            BridgeProtocol.parse(cursor.replace("afterSequence\":0", "afterSequence\":-1"));
            throw new AssertionError("Negative sequence accepted");
        } catch (BridgeProtocol.Error expected) {
            check("INVALID_ARGUMENT".equals(expected.code), "Unexpected cursor validation error");
        }

        LunarObserver observer = new LunarObserver();
        LunarCalendar calendar = new LunarCalendar(1, 0, 0, 0, () -> {}, failure -> {
            throw new AssertionError(failure);
        });
        calendar.rebaseline(0, 108999, LunarEvent.Cause.LOADED);
        observer.bind(1, calendar.view());
        observer.bind(1, calendar.view());
        check(observer.poll(1, 0).contains("\"events\":[]"), "New cursor must start empty");
        calendar.observe(109000);
        String event = observer.poll(1, 0);
        check(event.contains("\"latestSequence\":1") && event.contains("\"kind\":\"NIGHT_STARTED\"")
                && event.contains("\"cause\":\"NATURAL\""), "Natural boundary observed exactly once");
        check(observer.snapshot().contains("\"phase\":\"FULL_MOON\""), "Lunar phase from service");
        check(observer.poll(1, 1).contains("\"events\":[]"), "Consumed cursor");
        for (int index = 0; index < 8; index++) calendar.advance(1);
        check(observer.poll(1, 0).contains("\"dropped\":1"), "Wrapped cursor gap");
        observer.bind(2, calendar.view());
        check(observer.poll(2, 0).contains("\"events\":[]"), "New world clears observer even with same save");
        try {
            observer.poll(1, 0);
            throw new AssertionError("Previous world cursor accepted");
        } catch (BridgeProtocol.Error expected) {
            check("STALE_CURSOR".equals(expected.code), "Unexpected stale cursor error");
        }
        observer.bind(0, null);
        calendar.advance(1);
        observer.bind(3, calendar.view());
        check(observer.poll(3, 0).contains("\"events\":[]"), "Detached observer retained old events");
    }

    private static String field(String value, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(value);
        check(matcher.find(), "Missing discovery field");
        return matcher.group(1);
    }

    private static void waitForQueue(BridgeDispatcher dispatcher, int size) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
        while (System.nanoTime() < deadline) {
            if (dispatcher.status("test").contains("\"queueDepth\":" + size)) return;
            Thread.sleep(2);
        }
        throw new AssertionError("Queue did not reach size " + size);
    }

    private static String expect(String path, String body, String bearer, int status, String result) throws Exception {
        URL url = new URL("http://127.0.0.1:" + port + "/v1" + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        try {
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.US_ASCII);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setFixedLengthStreamingMode(bytes.length);
                connection.setDoOutput(true);
                try (java.io.OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }
            int actual = connection.getResponseCode();
            check(actual == status, "Expected HTTP " + status + " for " + path + ", received " + actual);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 400 ? connection.getErrorStream() : connection.getInputStream(), StandardCharsets.US_ASCII))) {
                String json = reader.readLine();
                check(json != null && json.contains(result), "Missing result for " + path + ": " + json);
                return json;
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void check(boolean condition, String failure) {
        if (!condition) throw new AssertionError(failure);
    }
}
