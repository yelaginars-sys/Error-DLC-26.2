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

        public SettingItem(String key, String name) {
            this.key = key;
            this.name = name;
        }
    }

    private final List<SettingItem> settingItems = List.of(
            new SettingItem("Coordinates", "Координаты"),
            new SettingItem("Discord", "Пользователь (UID)"),
            new SettingItem("Ping", "Пинг"),
            new SettingItem("FPS", "FPS счетчик"),
            new SettingItem("BPS", "Скорость (BPS)"),
            new SettingItem("Time", "Время и дата"),
            new SettingItem("Server", "Сервер")
    );

    public WatermarkHudElement() {
        super("watermark", "Watermark", 400.0F, 10.0F, 220.0F, 20.0F, true);
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

        boolean isChat = screen() instanceof ChatScreen;
        if (!isChat && settingsOpen) {
            settingsOpen = false;
        }

        float H = 20.0F;
        float RADIUS = 8.0F;

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
        float fontSize = 7.0F;
        float padX = 7.0F;
        float gap = 6.0F;

        float logoSize = 13.0F;
        float totalW = padX + logoSize + gap;

        if (showDiscord) totalW += font.getWidth(username + " | " + uid, fontSize) + gap;
        if (showPing) totalW += font.getWidth(ping + " ms", fontSize) + gap;
        if (showFps) totalW += font.getWidth(fps + " FPS", fontSize) + gap;
        if (showBps) totalW += font.getWidth(String.format(java.util.Locale.US, "%.1f bps", bps), fontSize) + gap;
        if (showServer) totalW += font.getWidth(serverName, fontSize) + gap;
        if (showCoords) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            totalW += font.getWidth(px + " " + py + " " + pz, fontSize) + gap;
        }
        if (showTime) totalW += font.getWidth(timeStr, fontSize) + gap;
        totalW += padX - gap;

        this.width = Math.max(120.0F, totalW);
        this.height = H;

        int bgColor = ColorUtil.rgba(14, 16, 24, 230);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 140);

        Render2D.drawBlur(x, y, width, height, RADIUS, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, RADIUS, bgColor, 1.0F, outlineColor);

        float curX = x + padX;
        float textY = y + (height - fontSize) / 2.0F - 0.5F;

        // Logo
        Render2D.drawTexture(LOGO_TEXTURE, curX, y + (H - logoSize) / 2.0F, logoSize, logoSize, ColorUtil.rgba(255, 255, 255, 255));
        curX += logoSize + gap;

        // Discord user | UID
        if (showDiscord) {
            String str = username + " | " + uid;
            Fonts.drawString(font, str, curX, textY, fontSize, ColorUtil.rgba(245, 245, 250, 255));
            curX += font.getWidth(str, fontSize) + gap;
        }

        // Ping
        if (showPing) {
            String str = ping + " ms";
            Fonts.drawString(font, str, curX, textY, fontSize, ColorUtil.rgba(190, 205, 230, 255));
            curX += font.getWidth(str, fontSize) + gap;
        }

        // FPS
        if (showFps) {
            String str = fps + " FPS";
            Fonts.drawString(font, str, curX, textY, fontSize, ColorUtil.rgba(190, 205, 230, 255));
            curX += font.getWidth(str, fontSize) + gap;
        }

        // BPS
        if (showBps) {
            String str = String.format(java.util.Locale.US, "%.1f bps", bps);
            Fonts.drawString(font, str, curX, textY, fontSize, ColorUtil.rgba(190, 205, 230, 255));
            curX += font.getWidth(str, fontSize) + gap;
        }

        // Server
        if (showServer) {
            Fonts.drawString(font, serverName, curX, textY, fontSize, ColorUtil.rgba(190, 205, 230, 255));
            curX += font.getWidth(serverName, fontSize) + gap;
        }

        // Coords
        if (showCoords) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            String str = px + " " + py + " " + pz;
            Fonts.drawString(font, str, curX, textY, fontSize, ColorUtil.rgba(190, 205, 230, 255));
            curX += font.getWidth(str, fontSize) + gap;
        }

        // Time
        if (showTime) {
            Fonts.drawString(font, timeStr, curX, textY, fontSize, ColorUtil.rgba(245, 245, 250, 255));
        }

        // Settings Panel inside ChatScreen
        if (isChat && settingsOpen) {
            renderSettingsPanel();
        }
    }

    private void renderSettingsPanel() {
        panelW = 140.0F;
        panelH = settingItems.size() * 14.0F + 16.0F;
        panelX = x;
        panelY = y + height + 6.0F;

        int panelBg = ColorUtil.rgba(16, 18, 26, 240);
        int borderCol = ColorUtil.rgba(255, 255, 255, 45);
        int accent = Theme.getAccentColor();

        Render2D.drawBlur(panelX, panelY, panelW, panelH, 7.0F, panelBg, 1.0F);
        Render2D.drawRoundedRectWithOutline(panelX, panelY, panelW, panelH, 7.0F, panelBg, 1.0F, borderCol);

        Fonts.drawString(Fonts.SF_MEDIUM, "Настройки Watermark", panelX + 6.0F, panelY + 5.0F, 6.5F, ColorUtil.rgba(240, 240, 250, 255));

        float rowY = panelY + 16.0F;
        for (SettingItem item : settingItems) {
            boolean active = isSettingEnabled(item.key);
            int toggleBg = active ? accent : ColorUtil.rgba(35, 38, 52, 200);

            Render2D.drawRoundedRect(panelX + panelW - 20.0F, rowY + 1.0F, 14.0F, 8.0F, 4.0F, toggleBg);
            Render2D.drawRoundedRect(panelX + panelW - (active ? 13.0F : 19.0F), rowY + 2.0F, 6.0F, 6.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 255));

            Fonts.drawString(Fonts.SF_MEDIUM, item.name, panelX + 6.0F, rowY + 1.5F, 6.0F, ColorUtil.rgba(220, 225, 240, 255));
            rowY += 14.0F;
        }
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (!(screen() instanceof ChatScreen)) return false;

        if (button == 1 && isHovered(mouseX, mouseY)) {
            settingsOpen = !settingsOpen;
            return true;
        }

        if (button == 0 && settingsOpen) {
            float rowY = panelY + 16.0F;
            for (SettingItem item : settingItems) {
                if (mouseX >= panelX + 4.0F && mouseX <= panelX + panelW - 4.0F && mouseY >= rowY && mouseY <= rowY + 13.0F) {
                    toggleSetting(item.key);
                    return true;
                }
                rowY += 14.0F;
            }
        }

        return false;
    }
}
