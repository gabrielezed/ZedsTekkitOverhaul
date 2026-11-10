package zed.tekkitoverhaul.devbridge;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** A bounded, one-request-per-connection HTTP/1.1 subset for the local CLI. */
final class BridgeServer implements AutoCloseable {
    private static final int MAX_HEADERS = 4096;
    private static final int MAX_BODY = 1024;
    private static final int READ_MILLIS = 1000;

    private final ServerSocket listener = new ServerSocket();
    private final Set<Socket> clients = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new ThreadFactory() {
                public Thread newThread(Runnable task) {
                    return daemon(task, "ZTO Dev Bridge worker");
                }
            });
    private final BridgeDispatcher dispatcher;
    private final String version;
    private final boolean testing;
    private final byte[] authorization;
    private final Thread shutdownHook = new Thread(new Runnable() {
        public void run() { close(); }
    }, "ZTO Dev Bridge shutdown");
    private Path discovery;
    private long rateWindow = System.nanoTime();
    private int requests;

    BridgeServer(int port, BridgeDispatcher dispatcher, String version) throws IOException {
        this(port, dispatcher, version, false);
    }

    BridgeServer(int port, BridgeDispatcher dispatcher, String version, boolean testing) throws IOException {
        this.testing = testing;
        this.dispatcher = dispatcher;
        this.version = version;
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        authorization = ("Bearer " + token).getBytes(StandardCharsets.US_ASCII);
        try {
            listener.bind(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port), 8);
            discovery = Files.createTempFile("zto-dev-bridge-", ".json");
            restrictDiscovery(discovery);
            String json = "{\"apiVersion\":\"1.0\",\"port\":" + listener.getLocalPort() + ",\"token\":\"" + token + "\"}";
            Files.write(discovery, json.getBytes(StandardCharsets.US_ASCII));
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            daemon(new Runnable() {
                public void run() { accept(); }
            }, "ZTO Dev Bridge listener").start();
        } catch (IOException failure) {
            close();
            throw failure;
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    Path discovery() {
        return discovery;
    }

    private static void restrictDiscovery(Path path) throws IOException {
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            AclEntry owner = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(acl.getOwner())
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
            acl.setAcl(Collections.singletonList(owner));
        } else {
            Files.setPosixFilePermissions(path, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    private void accept() {
        try {
            while (!listener.isClosed()) {
                final Socket client = listener.accept();
                clients.add(client);
                try {
                    workers.execute(new Runnable() {
                        public void run() { serve(client); }
                    });
                } catch (RejectedExecutionException full) {
                    release(client);
                }
            }
        } catch (IOException stopped) {
            close();
        }
    }

    private void serve(Socket socket) {
        try {
            try {
                Request request = new Request(socket);
                String supplied = request.headers.get("authorization");
                if (supplied == null || !MessageDigest.isEqual(authorization, supplied.getBytes(StandardCharsets.US_ASCII))) {
                    throw new BridgeProtocol.Error(401, "UNAUTHORIZED", "A valid bearer token is required");
                }
                if (!allowRequest()) throw new BridgeProtocol.Error(429, "RATE_LIMITED", "Retry in one second");
                send(socket, 200, route(request));
            } catch (BridgeProtocol.Error error) {
                send(socket, error.status, error.json());
            } catch (SocketTimeoutException timeout) {
                send(socket, 408, new BridgeProtocol.Error(408, "TIMEOUT", "The HTTP request expired").json());
            } catch (EOFException incomplete) {
                send(socket, 400, BridgeProtocol.invalid().json());
            }
        } catch (IOException disconnected) {
            // Clients may disconnect before the reply.
        } catch (InterruptedException stopping) {
            Thread.currentThread().interrupt();
        } finally {
            release(socket);
        }
    }

    private synchronized boolean allowRequest() {
        long now = System.nanoTime();
        if (now - rateWindow >= TimeUnit.SECONDS.toNanos(1)) {
            rateWindow = now;
            requests = 0;
        }
        if (requests >= 32) return false;
        requests++;
        return true;
    }

    private String route(Request request) throws IOException, BridgeProtocol.Error, InterruptedException {
        if ("GET".equals(request.method) && request.length == 0) {
            if ("/v1/health".equals(request.path)) return BridgeProtocol.success(null, null, "{\"alive\":true}");
            if ("/v1/capabilities".equals(request.path)) return BridgeProtocol.success(null, null, BridgeProtocol.capabilities(testing));
            if ("/v1/status".equals(request.path)) return BridgeProtocol.success(null, null, dispatcher.status(version));
        } else if ("POST".equals(request.method) && ("/v1/query".equals(request.path)
                || (testing && "/v1/command".equals(request.path)))) {
            if (!"application/json".equalsIgnoreCase(request.headers.get("content-type"))) throw BridgeProtocol.invalid();
            BridgeProtocol.Query query = BridgeProtocol.parse(request.body(), "/v1/command".equals(request.path));
            try {
                return dispatcher.submit(query);
            } catch (BridgeProtocol.Error failure) {
                failure.id = query.id;
                throw failure;
            }
        }
        throw new BridgeProtocol.Error(404, "UNSUPPORTED", "Unknown endpoint or HTTP method");
    }

    private static void send(Socket socket, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.US_ASCII);
        OutputStream output = socket.getOutputStream();
        String headers = "HTTP/1.1 " + status + (status == 200 ? " OK" : " Error")
                + "\r\nContent-Type: application/json\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: "
                + bytes.length + "\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(bytes);
        output.flush();
    }

    @Override
    public void close() {
        try { listener.close(); } catch (IOException ignored) {}
        dispatcher.close();
        workers.shutdownNow();
        for (Socket client : clients) release(client);
        if (discovery != null) {
            try { Files.deleteIfExists(discovery); } catch (IOException ignored) {}
        }
        try { Runtime.getRuntime().removeShutdownHook(shutdownHook); } catch (IllegalStateException ignored) {}
    }

    private void release(Socket socket) {
        try { socket.close(); } catch (IOException ignored) {}
        clients.remove(socket);
    }

    private static final class Request {
        final String method, path;
        final Map<String, String> headers = new HashMap<String, String>();
        final int length;
        private final Socket socket;
        private final InputStream input;
        private final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(READ_MILLIS);
        private int remainingHeaders = MAX_HEADERS;

        Request(Socket socket) throws IOException, BridgeProtocol.Error {
            this.socket = socket;
            input = new BufferedInputStream(socket.getInputStream());
            String[] start = line().split(" ", -1);
            if (start.length != 3 || !"HTTP/1.1".equals(start[2])) throw BridgeProtocol.invalid();
            method = start[0];
            path = start[1];
            String line;
            while (!(line = line()).isEmpty()) {
                int colon = line.indexOf(':');
                if (colon <= 0) throw BridgeProtocol.invalid();
                String name = line.substring(0, colon).toLowerCase(Locale.ROOT);
                if (!name.matches("[a-z0-9-]+") || headers.put(name, line.substring(colon + 1).trim()) != null) {
                    throw BridgeProtocol.invalid();
                }
            }
            if (headers.containsKey("transfer-encoding")) throw BridgeProtocol.invalid();
            String size = headers.get("content-length");
            if (size == null) size = "0";
            if (!size.matches("[0-9]{1,10}")) throw BridgeProtocol.invalid();
            long count = Long.parseLong(size);
            if (count > MAX_BODY) throw new BridgeProtocol.Error(413, "REQUEST_TOO_LARGE", "The body exceeds 1024 bytes");
            length = (int) count;
        }

        String body() throws IOException, BridgeProtocol.Error {
            StringBuilder body = new StringBuilder(length);
            for (int i = 0; i < length; i++) body.append((char) read());
            return body.toString();
        }

        private String line() throws IOException, BridgeProtocol.Error {
            StringBuilder line = new StringBuilder();
            while (true) {
                int value = headerByte();
                if (value == '\r') {
                    if (headerByte() != '\n') throw BridgeProtocol.invalid();
                    return line.toString();
                }
                if (value < 32 && value != '\t') throw BridgeProtocol.invalid();
                line.append((char) value);
            }
        }

        private int headerByte() throws IOException, BridgeProtocol.Error {
            if (--remainingHeaders < 0) {
                throw new BridgeProtocol.Error(413, "REQUEST_TOO_LARGE", "Headers exceed 4096 bytes");
            }
            return read();
        }

        private int read() throws IOException, BridgeProtocol.Error {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new SocketTimeoutException();
            if (input.available() == 0) socket.setSoTimeout((int) TimeUnit.NANOSECONDS.toMillis(remaining) + 1);
            int value = input.read();
            if (value == -1) throw new EOFException();
            if (value > 127) throw BridgeProtocol.invalid();
            return value;
        }
    }
}
