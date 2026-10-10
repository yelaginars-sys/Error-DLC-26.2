package error.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.ui.hud.impl.GpsHud;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerEventManager {

    private static final ServerEventManager INSTANCE = new ServerEventManager();

    public static ServerEventManager getInstance() {
        return INSTANCE;
    }

    public enum ServerType {
        FUN_TIME("FunTime", "mc.funtime.su"),
        SPOOKY_TIME("SpookyTime", "spookytime.net"),
        HOLY_WORLD("HolyWorld", "holyworld.ru");

        private final String displayName;
        private final String defaultIp;

        ServerType(String displayName, String defaultIp) {
            this.displayName = displayName;
            this.defaultIp = defaultIp;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultIp() {
            return defaultIp;
        }
    }

    public static class ServerEventItem {
        public final ServerType server;
        public final int anarchy;
        public final String name;
        public final String phase;
        public final String untilLabel;
        public final String command;
        public final long finishTimeMs;
        public final double x;
        public final double y;
        public final double z;

        public ServerEventItem(ServerType server, int anarchy, String name, String phase, String untilLabel, String command, long finishTimeMs, double x, double y, double z) {
            this.server = server;
            this.anarchy = anarchy;
            this.name = name;
            this.phase = phase != null && !phase.isEmpty() ? phase : "Запущен";
            this.untilLabel = untilLabel != null ? untilLabel : "";
            this.command = command != null && !command.isEmpty() ? command : ("/an " + anarchy);
            this.finishTimeMs = finishTimeMs;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public ServerEventItem(ServerType server, int anarchy, String name, String phase, String untilLabel, String command, long finishTimeMs) {
            this(server, anarchy, name, phase, untilLabel, command, finishTimeMs, Double.NaN, Double.NaN, Double.NaN);
        }

        public boolean hasCoords() {
            return !Double.isNaN(x) && !Double.isNaN(z);
        }

        public long getSecondsLeft() {
            return Math.max(0L, (finishTimeMs - System.currentTimeMillis()) / 1000L);
        }

        public String getFormattedTime() {
            long s = getSecondsLeft();
            long m = s / 60;
            long sec = s % 60;
            return String.format("%02d:%02d", m, sec);
        }

        public boolean isStarted() {
            String p = phase.toLowerCase(Locale.ROOT);
            return p.contains("запущен") || p.contains("активен") || p.contains("идет") || p.contains("активация") || p.contains("лут");
        }
    }

    private static final Pattern COORD_PATTERN_XYZ = Pattern.compile("x:\\s*(-?\\d+)[^yZ\\d]*y:\\s*(-?\\d+)[^z\\d]*z:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORD_PATTERN_XZ = Pattern.compile("x:\\s*(-?\\d+)[^z\\d]*z:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORD_BRACKETS = Pattern.compile("\\[?\\b(-?\\d{2,6})\\b[,\\s]+(?:(-?\\d{1,3})[,\\s]+)?\\b(-?\\d{2,6})\\b\\]?");

    private final List<ServerEventItem> spookyEvents = new CopyOnWriteArrayList<>();
    private final List<ServerEventItem> funtimeEvents = new CopyOnWriteArrayList<>();
    private final List<ServerEventItem> holyworldEvents = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Error-Server-Events");
        t.setDaemon(true);
        return t;
    });

    private ServerEventItem pendingEvent = null;
    private int pendingDelayTicks = -1;
    private long pendingTimeoutMs = 0L;

    private ServerEventManager() {
        EventManager.register(this);
        initMockData();
        executor.scheduleWithFixedDelay(this::pollEvents, 5000L, 20000L, TimeUnit.MILLISECONDS);
    }

    public ServerType detectCurrentServer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            String ip = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            if (ip.contains("spooky")) return ServerType.SPOOKY_TIME;
            if (ip.contains("funtime") || ip.contains("fun-time")) return ServerType.FUN_TIME;
            if (ip.contains("holy") || ip.contains("hw")) return ServerType.HOLY_WORLD;
        }
        return ServerType.FUN_TIME;
    }

    public List<ServerEventItem> getEvents(ServerType type) {
        return switch (type) {
            case SPOOKY_TIME -> spookyEvents;
            case FUN_TIME -> funtimeEvents;
            case HOLY_WORLD -> holyworldEvents;
        };
    }

    public void pollEvents() {
        Minecraft mc = Minecraft.getInstance();
        boolean guiEventsOpen = error.ui.clickgui.LiquidClickGui.isOpen;
        if (mc.getCurrentServer() == null && !guiEventsOpen) {
            return;
        }
        fetchSpookyEvents();
        fetchFunTimeEvents();
    }

    private void fetchSpookyEvents() {
        try {
            URL url = new URI("http://87.120.107.98/party-api/events").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestProperty("User-Agent", "Isle-Events/1.0");
            conn.setRequestProperty("Connection", "close");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    if (json.has("events")) {
                        JsonArray arr = json.getAsJsonArray("events");
                        List<ServerEventItem> list = new ArrayList<>();
                        long now = System.currentTimeMillis();
                        for (JsonElement el : arr) {
                            JsonObject obj = el.getAsJsonObject();
                            int anarchy = obj.has("anarchy") ? obj.get("anarchy").getAsInt() : 0;
                            String name = obj.has("name") ? obj.get("name").getAsString() : "";
                            if (name.isEmpty()) continue;
                            long seconds = obj.has("seconds") ? obj.get("seconds").getAsLong() : 0L;
                            String phase = obj.has("phase") ? obj.get("phase").getAsString() : "Запущен";
                            String untilLabel = obj.has("untilLabel") ? obj.get("untilLabel").getAsString() : "";
                            String cmd = obj.has("command") ? obj.get("command").getAsString() : ("/an " + anarchy);

                            list.add(new ServerEventItem(ServerType.SPOOKY_TIME, anarchy, name, phase, untilLabel, cmd, now + seconds * 1000L));
                        }
                        if (!list.isEmpty()) {
                            spookyEvents.clear();
                            spookyEvents.addAll(list);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void fetchFunTimeEvents() {
        try {
            URL url = new URI("https://funtime.me/api/backend/api/v1/events?event-type=all&server-type=").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Connection", "close");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    if (root.has("response")) {
                        JsonArray servers = root.getAsJsonArray("response");
                        List<ServerEventItem> list = new ArrayList<>();
                        long now = System.currentTimeMillis();

                        for (JsonElement sEl : servers) {
                            JsonObject sObj = sEl.getAsJsonObject();
                            String serverName = sObj.has("server") ? sObj.get("server").getAsString() : "";
                            int anarchy = parseAnarchyNumber(serverName);

                            if (sObj.has("events")) {
                                JsonArray events = sObj.getAsJsonArray("events");
                                for (JsonElement eEl : events) {
                                    JsonObject eObj = eEl.getAsJsonObject();

                                    String phaseRaw = eObj.has("phase") ? eObj.get("phase").getAsString() : "";
                                    if ("CLOSED".equalsIgnoreCase(phaseRaw)) continue;

                                    String eventId = eObj.has("id") ? eObj.get("id").getAsString() : "";
                                    String eventName = mapFunTimeEventName(eventId, eObj);
                                    String phaseDisplay = mapFunTimePhase(phaseRaw);

                                    long secondsLeft = 0L;
                                    if (eObj.has("time-seconds-left")) {
                                        secondsLeft = eObj.get("time-seconds-left").getAsLong();
                                    } else if (eObj.has("time-ms-left")) {
                                        secondsLeft = eObj.get("time-ms-left").getAsLong() / 1000L;
                                    }

                                    double ex = Double.NaN;
                                    double ey = Double.NaN;
                                    double ez = Double.NaN;

                                    if (eObj.has("location-announced") && eObj.get("location-announced").getAsBoolean() && eObj.has("location-event")) {
                                        JsonObject loc = eObj.getAsJsonObject("location-event");
                                        if (loc.has("x")) ex = loc.get("x").getAsDouble();
                                        if (loc.has("y")) ey = loc.get("y").getAsDouble();
                                        if (loc.has("z")) ez = loc.get("z").getAsDouble();
                                    }

                                    String cmd = "/an " + (anarchy > 0 ? anarchy : serverName.replace("anarchy", ""));
                                    list.add(new ServerEventItem(ServerType.FUN_TIME, anarchy, eventName, phaseDisplay, "осталось", cmd, now + secondsLeft * 1000L, ex, ey, ez));
                                }
                            }
                        }

                        if (!list.isEmpty()) {
                            list.sort(Comparator.comparingLong(ServerEventItem::getSecondsLeft));
                            funtimeEvents.clear();
                            funtimeEvents.addAll(list);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private int parseAnarchyNumber(String str) {
        if (str == null || str.isEmpty()) return 0;
        String digits = str.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String mapFunTimeEventName(String id, JsonObject eObj) {
        if (id != null) {
            switch (id.toLowerCase(Locale.ROOT)) {
                case "vulkan": return "Вулкан";
                case "geyser": return "Гейзер";
                case "meteor_rain": return "Метеоритный дождь";
                case "hellm": return "Адский сундук";
                case "myst_beacon": return "Мистический маяк";
                case "deathchest": return "Сундук смерти";
                case "airdrop": return "Аирдроп";
                case "beacon": return "Маяк";
                case "altarundead": return "Алтарь нежити";
            }
        }
        if (eObj.has("event-type")) {
            String type = eObj.get("event-type").getAsString();
            if ("system".equalsIgnoreCase(type)) return "Системный ивент";
            if ("user".equalsIgnoreCase(type)) return "Кастомный ивент";
        }
        return "Ивент";
    }

    private String mapFunTimePhase(String phase) {
        if (phase == null || phase.isEmpty()) return "Запущен";
        return switch (phase.toUpperCase(Locale.ROOT)) {
            case "ACTIVATING" -> "Активация";
            case "RUNNING" -> "Запущен";
            case "OPENED" -> "Открыт";
            case "LOOTING" -> "Сбор лута";
            case "WAITING" -> "Ожидание";
            case "STOPPING" -> "Завершение";
            default -> phase;
        };
    }

    private void initMockData() {
        long now = System.currentTimeMillis();

        if (spookyEvents.isEmpty()) {
            spookyEvents.add(new ServerEventItem(ServerType.SPOOKY_TIME, 105, "Мистический сундук", "Запущен", "до открытия", "/an 105", now + 180 * 1000L));
            spookyEvents.add(new ServerEventItem(ServerType.SPOOKY_TIME, 112, "Метеорит", "Ожидание", "до падения", "/an 112", now + 420 * 1000L));
            spookyEvents.add(new ServerEventItem(ServerType.SPOOKY_TIME, 102, "Вулкан", "Запущен", "до извержения", "/an 102", now + 95 * 1000L));
            spookyEvents.add(new ServerEventItem(ServerType.SPOOKY_TIME, 120, "Андайн Босс", "Скоро", "до спавна", "/an 120", now + 650 * 1000L));
        }

        if (funtimeEvents.isEmpty()) {
            funtimeEvents.add(new ServerEventItem(ServerType.FUN_TIME, 101, "Мистический сундук", "Запущен", "до начала", "/an 101", now + 240 * 1000L));
            funtimeEvents.add(new ServerEventItem(ServerType.FUN_TIME, 104, "Метеорит", "Запущен", "до падения", "/an 104", now + 110 * 1000L));
            funtimeEvents.add(new ServerEventItem(ServerType.FUN_TIME, 107, "Вулкан", "Ожидание", "до начала", "/an 107", now + 380 * 1000L));
        }

        if (holyworldEvents.isEmpty()) {
            holyworldEvents.add(new ServerEventItem(ServerType.HOLY_WORLD, 1, "Сундук Мертвеца", "Запущен", "до открытия", "/an 1", now + 300 * 1000L));
            holyworldEvents.add(new ServerEventItem(ServerType.HOLY_WORLD, 5, "Крушение Дирижабля", "Ожидание", "до крушения", "/an 5", now + 540 * 1000L));
        }
    }

    public void joinEventAndGps(ServerEventItem eventItem) {
        if (eventItem == null) return;
        Minecraft mc = Minecraft.getInstance();

        this.pendingEvent = eventItem;
        this.pendingDelayTicks = 24; // ~1.2s delay to allow server switch before sending /event delay
        this.pendingTimeoutMs = System.currentTimeMillis() + 15000L;

        // Instant GPS if coords are available directly from API
        if (eventItem.hasCoords()) {
            double finalY = Double.isNaN(eventItem.y) ? 64.0D : eventItem.y;
            String label = eventItem.name + " (АН-" + eventItem.anarchy + ")";
            GpsHud.setTarget(label, eventItem.x, finalY, eventItem.z);
            ChatUtil.success("GPS установлен на §e" + label + " §a[" + (int) eventItem.x + " " + (int) finalY + " " + (int) eventItem.z + "]");
            Notify.add("GPS Установлен", "АН-" + eventItem.anarchy + " (" + (int) eventItem.x + ", " + (int) eventItem.z + ")", IconUse.WORLD, Notify.COLOR_SUCCESS);
        }

        if (mc.getConnection() != null) {
            String cmd = eventItem.command.trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            mc.getConnection().sendCommand(cmd);
            ChatUtil.success("Переход на анархию: §e/" + cmd + " §a(Ивент: " + eventItem.name + ")");
            Notify.add("Ивент АН-" + eventItem.anarchy, eventItem.name + " -> переход...", IconUse.WORLD, Notify.COLOR_SUCCESS);
        }

        if (mc.gui != null && mc.gui.screen() != null) {
            mc.setScreenAndShow(null);
        }
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent event) {
        if (pendingDelayTicks > 0) {
            pendingDelayTicks--;
            if (pendingDelayTicks == 0 && pendingEvent != null) {
                // If coordinates weren't pre-announced via API, send /event delay
                if (!pendingEvent.hasCoords()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getConnection() != null) {
                        mc.getConnection().sendCommand("event delay");
                        ChatUtil.info("Запрос координат ивента: §7/event delay");
                    }
                }
            }
        }

        if (pendingEvent != null && System.currentTimeMillis() > pendingTimeoutMs) {
            pendingEvent = null;
            pendingDelayTicks = -1;
        }
    }

    @EventTarget
    public void onPacketReceive(PacketEvent event) {
        if (pendingEvent == null || event.getType() != PacketEvent.Type.RECEIVE) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            String raw = chatPacket.content().getString();
            parseAndSetGps(raw);
        }
    }

    private void parseAndSetGps(String text) {
        if (text == null || text.isBlank() || pendingEvent == null) return;

        double x = Double.NaN;
        double y = Double.NaN;
        double z = Double.NaN;

        Matcher m1 = COORD_PATTERN_XYZ.matcher(text);
        if (m1.find()) {
            try {
                x = Double.parseDouble(m1.group(1));
                y = Double.parseDouble(m1.group(2));
                z = Double.parseDouble(m1.group(3));
            } catch (NumberFormatException ignored) {}
        } else {
            Matcher m2 = COORD_PATTERN_XZ.matcher(text);
            if (m2.find()) {
                try {
                    x = Double.parseDouble(m2.group(1));
                    z = Double.parseDouble(m2.group(2));
                } catch (NumberFormatException ignored) {}
            } else {
                Matcher m3 = COORD_BRACKETS.matcher(text);
                if (m3.find()) {
                    try {
                        x = Double.parseDouble(m3.group(1));
                        if (m3.group(2) != null) y = Double.parseDouble(m3.group(2));
                        z = Double.parseDouble(m3.group(3));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (!Double.isNaN(x) && !Double.isNaN(z)) {
            double finalY = Double.isNaN(y) ? 64.0D : y;
            String label = pendingEvent.name + " (АН-" + pendingEvent.anarchy + ")";

            GpsHud.setTarget(label, x, finalY, z);

            ChatUtil.success("GPS установлен на §e" + label + " §a[" + (int) x + " " + (int) finalY + " " + (int) z + "]");
            Notify.add("GPS Установлен", (int) x + " " + (int) z, IconUse.WORLD, Notify.COLOR_SUCCESS);

            pendingEvent = null;
            pendingDelayTicks = -1;
        }
    }
}
