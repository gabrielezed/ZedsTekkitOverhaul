package zed.tekkitoverhaul.devbridge;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Jobs carry only protocol values. Only the client tick supplies a reader. */
final class BridgeDispatcher {
    interface Reader {
        String read(BridgeProtocol.Query query) throws BridgeProtocol.Error;
    }

    private static final int CAPACITY = 2;
    private static final long DEADLINE = TimeUnit.MILLISECONDS.toNanos(750);
    private final ArrayDeque<Job> jobs = new ArrayDeque<Job>();
    private String state = "no_world";
    private int dimension;
    private long worldId;
    private long tick;
    private long lastFrame = System.nanoTime();
    private boolean closed;

    synchronized void observe(String nextState, int nextDimension, long nextWorldId, long nextTick) {
        if (closed) return;
        if (worldId != nextWorldId || !"ready".equals(nextState)) cancel();
        state = nextState;
        dimension = nextDimension;
        worldId = nextWorldId;
        tick = nextTick;
        lastFrame = System.nanoTime();
    }

    private String state() {
        if (closed) return "closed";
        return System.nanoTime() - lastFrame >= DEADLINE ? "stalled" : state;
    }

    synchronized String status(String version) {
        String current = state();
        return "{\"modVersion\":\"" + version + "\",\"side\":\"local_client\",\"state\":\"" + current
                + "\",\"worldReady\":" + "ready".equals(current) + ",\"dimension\":"
                + (worldId == 0 ? "null" : dimension) + ",\"worldId\":" + (worldId == 0 ? "null" : worldId)
                + ",\"tick\":" + tick + ",\"queueDepth\":" + jobs.size() + "}";
    }

    String submit(BridgeProtocol.Query query) throws BridgeProtocol.Error, InterruptedException {
        Job job = new Job(query);
        synchronized (this) {
            if (!"ready".equals(state())) throw notReady();
            if (jobs.size() >= CAPACITY) throw new BridgeProtocol.Error(503, "BUSY", "The game queue is full");
            jobs.addLast(job);
        }
        try {
            long remaining = Math.max(0, job.deadline - System.nanoTime());
            return job.result.get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException expired) {
            throw new BridgeProtocol.Error(504, "TIMEOUT", "The client did not service the query before its deadline");
        } catch (ExecutionException failed) {
            throw (BridgeProtocol.Error) failed.getCause();
        } finally {
            synchronized (this) {
                jobs.remove(job);
            }
        }
    }

    void drain(Reader reader) {
        for (int i = 0; i < CAPACITY; i++) {
            Job job;
            long observedTick;
            synchronized (this) {
                if (closed || !"ready".equals(state)) return;
                job = jobs.pollFirst();
                observedTick = tick;
            }
            if (job == null) return;
            if (System.nanoTime() - job.deadline >= 0) {
                job.result.completeExceptionally(new BridgeProtocol.Error(504, "TIMEOUT", "The query expired"));
                continue;
            }
            try {
                job.result.complete(BridgeProtocol.success(job.query.id, observedTick, reader.read(job.query)));
            } catch (BridgeProtocol.Error error) {
                job.result.completeExceptionally(error);
            } catch (RuntimeException failure) {
                job.result.completeExceptionally(new BridgeProtocol.Error(500, "INTERNAL_ERROR", "The game query failed"));
            }
        }
    }

    synchronized void close() {
        closed = true;
        cancel();
    }

    private void cancel() {
        Job job;
        while ((job = jobs.pollFirst()) != null) job.result.completeExceptionally(notReady());
    }

    private static BridgeProtocol.Error notReady() {
        return new BridgeProtocol.Error(503, "NOT_READY", "An active local world is required");
    }

    private static final class Job {
        final BridgeProtocol.Query query;
        final long deadline = System.nanoTime() + DEADLINE;
        final CompletableFuture<String> result = new CompletableFuture<String>();

        Job(BridgeProtocol.Query query) {
            this.query = query;
        }
    }
}
