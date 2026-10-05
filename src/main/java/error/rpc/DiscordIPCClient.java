package error.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordIPCClient implements Closeable {

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;

    private final String clientId;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private Closeable pipeResource;
    private ByteChannel channel;
    private RandomAccessFile winRaf;

    private String connectedPipe = "";
    private String lastError = "Not connected";
    private int currentPid = -1;

    public DiscordIPCClient(String clientId) {
        this.clientId = clientId;
        try {
            this.currentPid = (int) ProcessHandle.current().pid();
        } catch (Throwable t) {
            try {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                int atIndex = jvmName.indexOf('@');
                if (atIndex > 0) {
                    this.currentPid = Integer.parseInt(jvmName.substring(0, atIndex));
                }
            } catch (Throwable ignored) {
                this.currentPid = 1000;
            }
        }
    }

    public synchronized boolean connect() {
        if (connected.get()) {
            return true;
        }

        closeResources();
        boolean isWin = isWindows();

        for (int i = 0; i < 10; i++) {
            try {
                if (isWin) {
                    try {
                        winRaf = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                    } catch (Throwable pipeNotFound) {
                        continue;
                    }
                    pipeResource = winRaf;
                    connectedPipe = "discord-ipc-" + i;
                } else {
                    File pipeFile = getUnixPipeFile(i);
                    if (pipeFile == null || !pipeFile.exists()) {
                        continue;
                    }
                    SocketChannel unixChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    unixChannel.connect(UnixDomainSocketAddress.of(pipeFile.toPath()));
                    channel = unixChannel;
                    pipeResource = channel;
                    connectedPipe = pipeFile.getAbsolutePath();
                }

                // Send Handshake
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", clientId);
                writePacket(OP_HANDSHAKE, handshake.toString());

                // Read READY response
                Packet readyPacket = readPacket();
                if (readyPacket != null && readyPacket.opcode == OP_FRAME) {
                    handleReadyPayload(readyPacket.payload);
                    connected.set(true);
                    lastError = "OK";
                    System.out.println("[DiscordRPC] Connected to Discord IPC via " + connectedPipe + "!");
                    return true;
                } else {
                    lastError = "Invalid handshake response from Discord";
                    closeResources();
                }
            } catch (Throwable e) {
                lastError = e.getMessage();
                closeResources();
            }
        }

        lastError = "Discord client not found / not running";
        return false;
    }

    private void handleReadyPayload(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("evt") && "READY".equalsIgnoreCase(root.get("evt").getAsString())) {
                if (root.has("data") && root.get("data").isJsonObject()) {
                    JsonObject data = root.getAsJsonObject("data");
                    if (data.has("user") && data.get("user").isJsonObject()) {
                        JsonObject user = data.getAsJsonObject("user");
                        String id = user.has("id") && !user.get("id").isJsonNull() ? user.get("id").getAsString() : "";
                        String username = user.has("username") && !user.get("username").isJsonNull() ? user.get("username").getAsString() : "";
                        String globalName = user.has("global_name") && !user.get("global_name").isJsonNull() ? user.get("global_name").getAsString() : "";
                        String avatar = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : "";

                        String displayName = (!globalName.isEmpty()) ? globalName : username;
                        DiscordProfileCache.setUser(displayName, id, avatar);
                        System.out.println("[DiscordRPC] Profile loaded: " + displayName + " (ID: " + id + ")");
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[DiscordRPC] Error parsing READY packet: " + t.getMessage());
        }
    }

    public synchronized boolean sendActivity(DiscordRichPresence presence) {
        if (!connected.get()) {
            if (!connect()) {
                return false;
            }
        }

        try {
            JsonObject root = new JsonObject();
            root.addProperty("cmd", "SET_ACTIVITY");
            root.addProperty("nonce", UUID.randomUUID().toString());

            JsonObject args = new JsonObject();
            args.addProperty("pid", currentPid);

            JsonObject activity = new JsonObject();

            if (presence.state != null && !presence.state.trim().isEmpty()) {
                activity.addProperty("state", presence.state);
            }
            if (presence.details != null && !presence.details.trim().isEmpty()) {
                activity.addProperty("details", presence.details);
            }

            if (presence.startTimestamp > 0) {
                JsonObject timestamps = new JsonObject();
                timestamps.addProperty("start", presence.startTimestamp);
                if (presence.endTimestamp > 0) {
                    timestamps.addProperty("end", presence.endTimestamp);
                }
                activity.add("timestamps", timestamps);
            }

            JsonObject assets = new JsonObject();
            boolean hasAssets = false;
            if (presence.largeImageKey != null && !presence.largeImageKey.trim().isEmpty()) {
                assets.addProperty("large_image", presence.largeImageKey.trim());
                if (presence.largeImageText != null && !presence.largeImageText.trim().isEmpty()) {
                    assets.addProperty("large_text", presence.largeImageText.trim());
                }
                hasAssets = true;
            }
            if (presence.smallImageKey != null && !presence.smallImageKey.trim().isEmpty()) {
                assets.addProperty("small_image", presence.smallImageKey.trim());
                if (presence.smallImageText != null && !presence.smallImageText.trim().isEmpty()) {
                    assets.addProperty("small_text", presence.smallImageText.trim());
                }
                hasAssets = true;
            }
            if (hasAssets) {
                activity.add("assets", assets);
            }

            JsonArray buttons = new JsonArray();
            JsonArray buttonUrls = new JsonArray();
            addButtonIfValid(buttons, buttonUrls, presence.button_label_1, presence.button_url_1);
            addButtonIfValid(buttons, buttonUrls, presence.button_label_2, presence.button_url_2);
            if (buttons.size() > 0) {
                activity.add("buttons", buttons);
                JsonObject metadata = new JsonObject();
                metadata.add("button_urls", buttonUrls);
                activity.add("metadata", metadata);
            }

            args.add("activity", activity);
            root.add("args", args);
            writePacket(OP_FRAME, root.toString());
            try {
                Packet reply = readPacket();
            } catch (Throwable ignored) {}
            return true;
        } catch (Throwable e) {
            lastError = "Send activity error: " + e.getMessage();
            connected.set(false);
            closeResources();
            return false;
        }
    }

    private void addButtonIfValid(JsonArray buttons, JsonArray buttonUrls, String label, String url) {
        if (label == null || label.trim().isEmpty() || url == null || url.trim().isEmpty()) {
            return;
        }
        String sanitizedUrl = url.trim();
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
            sanitizedUrl = "https://" + sanitizedUrl;
        }

        JsonObject button = new JsonObject();
        button.addProperty("label", label.trim());
        button.addProperty("url", sanitizedUrl);
        buttons.add(button);

        if (buttonUrls != null) {
            buttonUrls.add(sanitizedUrl);
        }
    }

    private void writePacket(int opcode, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + payloadBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(payloadBytes.length);
        buf.put(payloadBytes);

        if (winRaf != null) {
            winRaf.write(buf.array());
        } else if (channel != null) {
            buf.flip();
            while (buf.hasRemaining()) {
                channel.write(buf);
            }
        } else {
            throw new IOException("No open pipe channel");
        }
    }

    private Packet readPacket() throws IOException {
        byte[] header = new byte[8];
        if (winRaf != null) {
            winRaf.readFully(header);
        } else if (channel != null) {
            ByteBuffer headerBuf = ByteBuffer.wrap(header);
            readFully(channel, headerBuf);
        } else {
            return null;
        }

        ByteBuffer headerWrap = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = headerWrap.getInt();
        int length = headerWrap.getInt();

        if (length < 0 || length > 10 * 1024 * 1024) {
            throw new IOException("Invalid packet length: " + length);
        }

        byte[] payload = new byte[length];
        if (winRaf != null) {
            winRaf.readFully(payload);
        } else if (channel != null) {
            ByteBuffer payloadBuf = ByteBuffer.wrap(payload);
            readFully(channel, payloadBuf);
        }

        return new Packet(opcode, new String(payload, StandardCharsets.UTF_8));
    }

    private void readFully(ByteChannel ch, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int read = ch.read(buf);
            if (read == -1) {
                throw new IOException("End of stream reached");
            }
        }
    }

    private synchronized void closeResources() {
        if (pipeResource != null) {
            try {
                pipeResource.close();
            } catch (Throwable ignored) {
            }
            pipeResource = null;
        }
        winRaf = null;
        channel = null;
    }

    @Override
    public synchronized void close() {
        if (closed.compareAndSet(false, true)) {
            connected.set(false);
            try {
                if (winRaf != null || channel != null) {
                    JsonObject empty = new JsonObject();
                    writePacket(OP_CLOSE, empty.toString());
                }
            } catch (Throwable ignored) {
            }
            closeResources();
            System.out.println("[DiscordRPC] Disconnected from Discord IPC.");
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public String getConnectedPipe() {
        return connectedPipe;
    }

    public String getLastError() {
        return lastError;
    }

    public String getClientId() {
        return clientId;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static File getUnixPipeFile(int index) {
        String[] paths = new String[]{
                System.getenv("XDG_RUNTIME_DIR"),
                System.getenv("TMPDIR"),
                System.getenv("TMP"),
                System.getenv("TEMP"),
                "/tmp"
        };
        for (String base : paths) {
            if (base != null && !base.isEmpty()) {
                File f = new File(base, "discord-ipc-" + index);
                if (f.exists()) {
                    return f;
                }
            }
        }
        return new File("/tmp", "discord-ipc-" + index);
    }

    private static class Packet {
        final int opcode;
        final String payload;

        Packet(int opcode, String payload) {
            this.opcode = opcode;
            this.payload = payload;
        }
    }
}
