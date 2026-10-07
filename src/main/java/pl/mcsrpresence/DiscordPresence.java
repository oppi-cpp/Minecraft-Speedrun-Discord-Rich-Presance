package pl.mcsrpresence;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class DiscordPresence {
    public static final String APPLICATION_ID = "1557417651234410516";
    private static final Object LOCK = new Object();
    private static RandomAccessFile pipe;
    private static boolean started;
    private static Stage wantedStage;
    private static boolean clearWanted;
    private static long nonce;
    private static long wantedStartTimestamp;

    private DiscordPresence() {}

    public static void init() {
        if (started) return;
        started = true;
        Thread worker = new Thread(DiscordPresence::workerLoop, "MCSR-Presence-DiscordIPC");
        worker.setDaemon(true);
        worker.start();
    }

    public static void update(Stage stage, long startTimestamp) {
        synchronized (LOCK) {
            wantedStage = stage;
            wantedStartTimestamp = startTimestamp;
            clearWanted = false;
            LOCK.notifyAll();
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            wantedStage = null;
            clearWanted = true;
            LOCK.notifyAll();
        }
    }

    public static void shutdown() {
        started = false;
        closePipe();
        synchronized (LOCK) { LOCK.notifyAll(); }
    }

    private static void workerLoop() {
        Stage sentStage = null;
        long sentStartTimestamp = Long.MIN_VALUE;
        boolean sentClear = false;
        while (started) {
            try {
                if (pipe == null) {
                    connect();
                    sentStage = null;
                    sentStartTimestamp = Long.MIN_VALUE;
                    sentClear = false;
                }
                Stage stage;
                boolean clear;
                long startTimestamp;
                synchronized (LOCK) {
                    stage = wantedStage;
                    startTimestamp = wantedStartTimestamp;
                    clear = clearWanted;
                }
                if (clear && !sentClear) {
                    sendActivity(null, 0L);
                    sentClear = true;
                    sentStage = null;
                } else if (!clear && stage != null && (stage != sentStage || startTimestamp != sentStartTimestamp)) {
                    sendActivity(stage, startTimestamp);
                    sentStage = stage;
                    sentStartTimestamp = startTimestamp;
                    sentClear = false;
                }
                synchronized (LOCK) {
                    try { LOCK.wait(1000L); } catch (InterruptedException ignored) { return; }
                }
            } catch (Throwable e) {
                closePipe();
                try { Thread.sleep(2000L); } catch (InterruptedException ignored) { return; }
            }
        }
    }

    private static void connect() throws IOException {
        IOException last = null;
        for (int i = 0; i < 10; i++) {
            try {
                RandomAccessFile candidate = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                pipe = candidate;
                writeFrame(0, "{\"v\":1,\"client_id\":\"" + APPLICATION_ID + "\"}");
                try { Thread.sleep(120L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return;
            } catch (IOException e) { last = e; }
        }
        throw last != null ? last : new IOException("Discord IPC pipe not found");
    }

    private static void sendActivity(Stage stage, long startTimestamp) throws IOException {
        String activity;
        if (stage == null) {
            activity = "null";
        } else {
            activity = "{" +
                    "\"details\":\"" + esc(stage.label) + "\"," +
                    "\"state\":\"MCSR\"," +
                    (startTimestamp > 0L ? "\"timestamps\":{\"start\":" + startTimestamp + "}," : "") +
                    "\"assets\":{\"large_image\":\"" + esc(stage.asset) + "\",\"large_text\":\"" + esc(stage.label) + "\"}" +
                    "}";
        }
        String json = "{" +
                "\"cmd\":\"SET_ACTIVITY\"," +
                "\"args\":{\"pid\":" + processId() + ",\"activity\":" + activity + "}," +
                "\"nonce\":\"" + (++nonce) + "\"" +
                "}";
        writeFrame(1, json);
    }

    private static void writeFrame(int opcode, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode).putInt(payload.length);
        synchronized (DiscordPresence.class) {
            if (pipe == null) throw new IOException("Discord IPC disconnected");
            pipe.write(header.array());
            pipe.write(payload);
        }
    }

    private static int processId() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            long pid = Long.parseLong(at >= 0 ? name.substring(0, at) : name);
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, pid));
        } catch (Throwable ignored) { return 0; }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void closePipe() {
        RandomAccessFile p = pipe;
        pipe = null;
        if (p != null) try { p.close(); } catch (IOException ignored) {}
    }
}
