package error.module.impl.misc;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.ui.hud.impl.GpsHud;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoEvent extends Module {
    public static AutoEvent INSTANCE;

    public final MultiModeSetting eventsTracked = multiMode("Отслеживать эвенты", List.of("Мистик", "Метеорит", "Вулкан", "Караван", "Андайн"), "Мистик", "Метеорит", "Вулкан", "Караван", "Андайн");
    public final MultiModeSetting notifications = multiMode("Уведомления", List.of("Chat", "Notify", "GPS"), "Chat", "Notify", "GPS");
    public final CheckBox autoGps = checkbox("Автоматический GPS", true);
    public final CheckBox copyCoords = checkbox("Копировать координаты", true);
    public final SliderSetting gpsTime = slider("Время сброса GPS (сек)", 300.0f, 30.0f, 600.0f, 15.0f);

    private static final Pattern COORD_PATTERN_XYZ = Pattern.compile("x:\\s*(-?\\d+)[^yZ\\d]*y:\\s*(-?\\d+)[^z\\d]*z:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORD_PATTERN_XZ = Pattern.compile("x:\\s*(-?\\d+)[^z\\d]*z:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORD_BRACKETS_EVENT = Pattern.compile("\\[\\s*(-?\\d{2,6})\\s*[,\\s]\\s*(?:(-?\\d{1,3})\\s*[,\\s]\\s*)?(-?\\d{2,6})\\s*\\]");

    private String lastEventKey = "";
    private long lastEventTime = 0L;

    public AutoEvent() {
        super("AutoEvent", "Автоматический парсинг и навигация к ивентам (Мистик, Метеорит, Вулкан)", Category.MISC);
        INSTANCE = this;
    }

    @EventTarget
    public void onPacketReceive(PacketEvent event) {
        if (!inGame() || player() == null || event.getType() != PacketEvent.Type.RECEIVE) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            String rawMessage = chatPacket.content().getString();
            parseEventMessage(rawMessage);
        }
    }

    public void parseEventMessage(String text) {
        if (text == null || text.isBlank()) return;
        String lower = text.toLowerCase(Locale.ROOT);

        // Filter out normal chat, player achievements, or player messages
        if (lower.contains("получил достижение") || lower.contains("убил пользователя") || lower.contains("купил") || lower.contains("продал")) {
            return;
        }

        String detectedEvent = null;
        if (eventsTracked.isEnabled("Мистик") && (lower.contains("мистический") || lower.contains("мистик") || lower.contains("тайный сундук"))) {
            detectedEvent = "Мистический сундук";
        } else if (eventsTracked.isEnabled("Метеорит") && (lower.contains("метеорит") || lower.contains("упал метеор"))) {
            detectedEvent = "Метеорит";
        } else if (eventsTracked.isEnabled("Вулкан") && (lower.contains("извержение") || lower.contains("вулкан"))) {
            detectedEvent = "Вулкан";
        } else if (eventsTracked.isEnabled("Караван") && (lower.contains("караван") || lower.contains("обоз"))) {
            detectedEvent = "Караван";
        } else if (eventsTracked.isEnabled("Андайн") && (lower.contains("андайн") || lower.contains("босс"))) {
            detectedEvent = "Андайн Босс";
        }

        if (detectedEvent == null) return;

        ParsedCoords coords = extractCoordinates(text);
        if (coords == null) return;

        // Deduplicate events received within 20 seconds
        String currentKey = detectedEvent + "_" + (int) coords.x + "_" + (int) coords.z;
        long now = System.currentTimeMillis();
        if (currentKey.equals(lastEventKey) && (now - lastEventTime < 20000L)) {
            return;
        }

        lastEventKey = currentKey;
        lastEventTime = now;

        // Auto set GPS
        if (autoGps.getValue() || notifications.isEnabled("GPS")) {
            if (Double.isNaN(coords.y)) {
                GpsHud.setTarget(detectedEvent, coords.x, 64, coords.z);
            } else {
                GpsHud.setTarget(detectedEvent, coords.x, coords.y, coords.z);
            }
        }

        // Copy to clipboard
        if (copyCoords.getValue()) {
            String coordStr = (int) coords.x + " " + (Double.isNaN(coords.y) ? "" : (int) coords.y + " ") + (int) coords.z;
            try {
                mc.keyboardHandler.setClipboard(coordStr);
            } catch (Throwable ignored) {}
        }

        // Notifications
        String posStr = "X: " + (int) coords.x + (Double.isNaN(coords.y) ? "" : " Y: " + (int) coords.y) + " Z: " + (int) coords.z;

        if (notifications.isEnabled("Chat")) {
            ChatUtil.success("Обнаружен ивент §e[★ " + detectedEvent + "] §a(" + posStr + ")");
        }

        if (notifications.isEnabled("Notify")) {
            Notify.add("Ивент: " + detectedEvent, posStr, IconUse.WORLD, Notify.COLOR_SUCCESS);
        }
    }

    private ParsedCoords extractCoordinates(String text) {
        Matcher m1 = COORD_PATTERN_XYZ.matcher(text);
        if (m1.find()) {
            try {
                double x = Double.parseDouble(m1.group(1));
                double y = Double.parseDouble(m1.group(2));
                double z = Double.parseDouble(m1.group(3));
                return new ParsedCoords(x, y, z);
            } catch (NumberFormatException ignored) {}
        }

        Matcher m2 = COORD_PATTERN_XZ.matcher(text);
        if (m2.find()) {
            try {
                double x = Double.parseDouble(m2.group(1));
                double z = Double.parseDouble(m2.group(2));
                return new ParsedCoords(x, Double.NaN, z);
            } catch (NumberFormatException ignored) {}
        }

        Matcher m3 = COORD_BRACKETS_EVENT.matcher(text);
        if (m3.find()) {
            try {
                double x = Double.parseDouble(m3.group(1));
                double y = m3.group(2) != null ? Double.parseDouble(m3.group(2)) : Double.NaN;
                double z = Double.parseDouble(m3.group(3));
                return new ParsedCoords(x, y, z);
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    private record ParsedCoords(double x, double y, double z) {}
}
