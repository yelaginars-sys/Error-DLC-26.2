package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class WatermarkHudElement extends HudElement implements IMinecraft {

    private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("client", "textures/hud/logo.png");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Toggle settings for Watermark items
    public boolean showCoords = true;
    public boolean showDiscord = true;
    public boolean showPing = true;
    public boolean showFps = true;
    public boolean showBps = true;
    public boolean showTime = true;
    public boolean showServer = false;

    // Interactive settings panel state
    public boolean settingsOpen = false;
    private float panelX, panelY, panelW, panelH;

    public static class SettingItem {
        public final String key;
        public final String name;
        public final String icon;

        public SettingItem(String key, String name, String icon) {
            this.key = key;
            this.name = name;
            this.icon = icon;
        }
    }

    private final List<SettingItem> settingItems = List.of(
            new SettingItem("Coordinates", "Координаты", "J"),
            new SettingItem("Discord", "Пользователь (UID)", "N"),
            new SettingItem("Ping", "Пинг", "k"),
            new SettingItem("FPS", "FPS счетчик", "C"),
            new SettingItem("BPS", "Скорость (BPS)", "H"),
            new SettingItem("Time", "Время и дата", "U"),
            new SettingItem("Server", "Сервер", "S")
    );

    public WatermarkHudElement() {
        super("watermark", "Watermark", 400.0F, 10.0F, 220.0F, 22.0F, true);
    }

    public boolean isSettingEnabled(String key) {
        return switch (key) {
            case "Coordinates" -> showCoords;
            case "Discord" -> showDiscord;
            case "Ping" -> showPing;
            case "FPS" -> showFps;
            case "BPS" -> showBps;
            case "Time" -> showTime;
            case "Server" -> showServer;
            default -> true;
        };
    }

    public void toggleSetting(String key) {
        switch (key) {
            case "Coordinates" -> showCoords = !showCoords;
            case "Discord" -> showDiscord = !showDiscord;
            case "Ping" -> showPing = !showPing;
            case "FPS" -> showFps = !showFps;
            case "BPS" -> showBps = !showBps;
            case "Time" -> showTime = !showTime;
            case "Server" -> showServer = !showServer;
        }
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        if (!(screen() instanceof ChatScreen) && settingsOpen) {
            settingsOpen = false;
        }

        float h = 22.0F;
        float r = 8.0F;

        int fps = mc.getFps();
        String timeStr = LocalTime.now().format(TIME_FORMATTER);
        String username = mc.player.getName().getString();
        String uid = "#1";
        int ping = 0;
        try {
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
                ping = mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency();
            }
        } catch (Throwable ignored) {}

        double dx = mc.player.getX() - mc.player.xo;
        double dz = mc.player.getZ() - mc.player.zo;
        float bps = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0F);

        String serverName = mc.hasSingleplayerServer() ? "Одиночная" : "Сервер";

        MsdfFont font = Fonts.SF_MEDIUM;
        float fontSize = 7.5F;
        float padX = 8.0F;
        float gap = 6.0F;

        float totalW = padX + 14.0F + gap; // Logo
        if (showDiscord) totalW += font.getWidth(username + " | " + uid, fontSize) + gap;
        if (showPing) totalW += font.getWidth("k " + ping + " ms", fontSize) + gap;
        if (showFps) totalW += font.getWidth("C " + fps + " FPS", fontSize) + gap;
        if (showBps) totalW += font.getWidth(String.format("H %.1f bps", bps), fontSize) + gap;
        if (showServer) totalW += font.getWidth("S " + serverName, fontSize) + gap;
        if (showCoords) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            totalW += font.getWidth("J " + px + " " + py + " " + pz, fontSize) + gap;
        }
        if (showTime) totalW += font.getWidth("U " + timeStr, fontSize) + gap;
        totalW += padX - gap;

        this.width = Math.max(140.0F, totalW);
        this.height = h;

        int bgColor = ColorUtil.rgba(12, 12, 18, 230);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 160);

        Render2D.drawBlur(x, y, width, height, r, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, r, bgColor, 1.0F, outlineColor);

        float curX = x + padX;
        float textY = y + (height - font.lineHeight(fontSize)) / 2.0F + 1.0F;

        // Logo icon
        Render2D.drawRoundedRect(curX, y + 4.0F, 14.0F, 14.0F, 3.0F, Theme.getAccentColor());
        curX += 14.0F + gap;

        // User / Discord
        if (showDiscord) {
            Fonts.drawString(font, username, curX, textY, fontSize, ColorUtil.rgba(255, 255, 255, 255));
            curX += font.getWidth(username, fontSize);
            Fonts.drawString(font, " | " + uid, curX, textY, fontSize, Theme.getAccentColor());
            curX += font.getWidth(" | " + uid, fontSize) + gap;
        }

        // Ping
        if (showPing) {
            Fonts.drawString(font, ping + " ms", curX, textY, fontSize, ColorUtil.rgba(220, 220, 220, 255));
            curX += font.getWidth(ping + " ms", fontSize) + gap;
        }

        // FPS
        if (showFps) {
            Fonts.drawString(font, fps + " FPS", curX, textY, fontSize, Theme.getAccentColor());
            curX += font.getWidth(fps + " FPS", fontSize) + gap;
        }

        // BPS
        if (showBps) {
            String bpsStr = String.format("%.1f bps", bps);
            Fonts.drawString(font, bpsStr, curX, textY, fontSize, ColorUtil.rgba(200, 200, 220, 255));
            curX += font.getWidth(bpsStr, fontSize) + gap;
        }

        // Server
        if (showServer) {
            Fonts.drawString(font, serverName, curX, textY, fontSize, ColorUtil.rgba(220, 220, 220, 255));
            curX += font.getWidth(serverName, fontSize) + gap;
        }

        // Coords
        if (showCoords) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            String coordsStr = px + " " + py + " " + pz;
            Fonts.drawString(font, coordsStr, curX, textY, fontSize, ColorUtil.rgba(180, 220, 255, 255));
            curX += font.getWidth(coordsStr, fontSize) + gap;
        }

        // Time
        if (showTime) {
            Fonts.drawString(font, timeStr, curX, textY, fontSize, ColorUtil.rgba(200, 200, 200, 255));
        }

        // Render Settings Panel if open
        if (settingsOpen && screen() instanceof ChatScreen) {
            drawSettingsPanel();
        }
    }

    private void drawSettingsPanel() {
        float itemH = 16.0F;
        float headerH = 20.0F;
        panelW = Math.max(140.0F, width);
        panelH = headerH + settingItems.size() * itemH + 6.0F;
        panelX = x + (width - panelW) / 2.0F;
        panelY = y + height + 6.0F;

        int panelBg = ColorUtil.rgba(15, 15, 22, 240);
        int outline = ColorUtil.withAlpha(Theme.getAccentColor(), 180);

        Render2D.drawBlur(panelX, panelY, panelW, panelH, 6.0F, panelBg, 1.0F);
        Render2D.drawRoundedRectWithOutline(panelX, panelY, panelW, panelH, 6.0F, panelBg, 1.0F, outline);

        MsdfFont font = Fonts.SF_MEDIUM;
        float titleW = font.getWidth("Настройки Watermark", 7.0F);
        Fonts.drawString(font, "Настройки Watermark", panelX + (panelW - titleW) / 2.0F, panelY + 5.0F, 7.0F, Theme.getAccentColor());

        for (int i = 0; i < settingItems.size(); i++) {
            SettingItem item = settingItems.get(i);
            float rowX = panelX + 6.0F;
            float rowY = panelY + headerH + i * itemH;
            float rowW = panelW - 12.0F;

            boolean enabled = isSettingEnabled(item.key);
            Fonts.drawString(font, item.name, rowX, rowY + 3.0F, 6.5F, ColorUtil.rgba(230, 230, 240, 255));

            // Switch toggle
            float swW = 18.0F;
            float swH = 9.0F;
            float swX = rowX + rowW - swW;
            float swY = rowY + 3.0F;

            int trackColor = enabled ? Theme.getAccentColor() : ColorUtil.rgba(45, 45, 55, 255);
            Render2D.drawRoundedRect(swX, swY, swW, swH, 4.5F, trackColor);

            float knobSize = 7.0F;
            float knobX = enabled ? (swX + swW - knobSize - 1.0F) : (swX + 1.0F);
            Render2D.drawRoundedRect(knobX, swY + 1.0F, knobSize, knobSize, 3.5F, ColorUtil.rgba(255, 255, 255, 255));
        }
    }
}
