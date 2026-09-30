package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.player.AbstractClientPlayer;

public final class WatermarkHud extends HudElement implements IMinecraft {

    public static final int PRIMARY_PURPLE = ColorUtil.rgba(166, 130, 255, 255);
    public static final int SECONDARY_PURPLE = ColorUtil.rgba(194, 165, 255, 255);

    public WatermarkHud() {
        super("watermark", "Watermark", 6.0F, 6.0F, 240.0F, 20.0F);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    @Override
    public void draw(Render2DEvent event) {
        fadeAnim.setTarget(1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float x = getX();
        float y = getY();
        int ta = (int) (alpha * 255.0F);

        float padding = 3.5F;
        float fontSize = 7.5F;
        float dotFontSize = 12.0F;
        float headSize = 10.0F;
        float r = 4.0F;

        String username = mc.player != null ? mc.player.getScoreboardName() : "User";
        String role = (username.equalsIgnoreCase("zxcwashik") || username.equalsIgnoreCase("yelag")) ? "Разработчик" : "Пользователь";
        String clientName = "Waper";
        String fpsInfo = mc.getFps() + "fps";
        String serverInfo = getServerAddress();

        String dot = ".";
        int dotColor = ColorUtil.rgba(150, 150, 150, ta);

        float dotW = Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 4.0F;

        float totalWidth = padding * 2.0F;
        totalWidth += Fonts.SF_MEDIUM.getWidth(clientName, fontSize);
        totalWidth += dotW;
        totalWidth += headSize + 2.0F;

        String displayName = username;
        float maxNameW = 50.0F;
        if (Fonts.SF_MEDIUM.getWidth(displayName, fontSize) > maxNameW) {
            while (Fonts.SF_MEDIUM.getWidth(displayName + "...", fontSize) > maxNameW && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }

        String roleText = " [" + role + "]";
        totalWidth += Fonts.SF_MEDIUM.getWidth(displayName, fontSize);
        totalWidth += Fonts.SF_MEDIUM.getWidth(roleText, fontSize);
        totalWidth += dotW;
        totalWidth += Fonts.ICONS.getWidth(IconUse.FPS.glyph, fontSize) + Fonts.SF_MEDIUM.getWidth(" " + fpsInfo, fontSize);
        totalWidth += dotW;
        totalWidth += Fonts.ICONS.getWidth(IconUse.GLOBE.glyph, fontSize) + Fonts.SF_MEDIUM.getWidth(" " + serverInfo, fontSize);

        float totalHeight = Math.max(headSize, 8.0F) + padding * 2.0F;

        this.width = totalWidth;
        this.height = totalHeight;

        // 1. Layer 1: Accent glow (15% opacity)
        Render2D.drawRoundedRect(x - 2.0F, y - 2.0F, totalWidth + 4.0F, totalHeight + 4.0F, r + 2.0F, ColorUtil.applyAlpha(PRIMARY_PURPLE, (int) (alpha * 15)));

        // 2. Layer 2: Subtle outer border (40% opacity)
        Render2D.drawRoundedRect(x - 0.5F, y - 0.5F, totalWidth + 1.0F, totalHeight + 1.0F, r + 0.5F, ColorUtil.applyAlpha(PRIMARY_PURPLE, (int) (alpha * 40)));

        // 3. Layer 3: Dark container background
        Render2D.drawRoundedRect(x, y, totalWidth, totalHeight, r, ColorUtil.rgba(14, 14, 18, (int) (160 * alpha)));

        float currentX = x + padding;
        float textY = y + (totalHeight - 7.5F) / 2.0F;
        float headY = y + (totalHeight - headSize) / 2.0F;
        float dotY = y + (totalHeight - 12.0F) / 2.0F - 3.5F;

        // Client Name (Waper in primary purple)
        float clientW = Fonts.SF_MEDIUM.getWidth(clientName, fontSize);
        Fonts.drawString(Fonts.SF_MEDIUM, clientName, currentX, textY, fontSize, ColorUtil.applyAlpha(PRIMARY_PURPLE, alpha));
        currentX += clientW;

        // Dot
        currentX += 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX, dotY, dotFontSize, dotColor);
        currentX += Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 2.0F;

        // Head
        if (mc.player instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawHead(clientPlayer, currentX, headY, headSize, headSize * 0.5F, alpha);
        }
        currentX += headSize + 2.0F;

        // Display Name (White)
        Fonts.drawString(Fonts.SF_MEDIUM, displayName, currentX, textY, fontSize, ColorUtil.applyAlpha(-1, alpha));
        currentX += Fonts.SF_MEDIUM.getWidth(displayName, fontSize);

        // Role Text ([Разработчик] / [Пользователь] in Primary Purple)
        int roleColor = (username.equalsIgnoreCase("zxcwashik") || username.equalsIgnoreCase("yelag")) ? PRIMARY_PURPLE : dotColor;
        Fonts.drawString(Fonts.SF_MEDIUM, roleText, currentX, textY, fontSize, ColorUtil.applyAlpha(roleColor, alpha));
        currentX += Fonts.SF_MEDIUM.getWidth(roleText, fontSize);

        // Dot
        currentX += 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX, dotY, dotFontSize, dotColor);
        currentX += Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 2.0F;

        // FPS Icon & Info
        Fonts.drawIcon(IconUse.FPS, currentX, textY, fontSize, ColorUtil.applyAlpha(-1, alpha));
        currentX += Fonts.ICONS.getWidth(IconUse.FPS.glyph, fontSize) + 1.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, fpsInfo, currentX, textY, fontSize, ColorUtil.applyAlpha(-1, alpha));
        currentX += Fonts.SF_MEDIUM.getWidth(fpsInfo, fontSize);

        // Dot
        currentX += 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX, dotY, dotFontSize, dotColor);
        currentX += Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 2.0F;

        // IP Icon & Server Info
        Fonts.drawIcon(IconUse.GLOBE, currentX, textY, fontSize, ColorUtil.applyAlpha(-1, alpha));
        currentX += Fonts.ICONS.getWidth(IconUse.GLOBE.glyph, fontSize) + 3.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, serverInfo, currentX, textY, fontSize, ColorUtil.applyAlpha(-1, alpha));
    }

    private String getServerAddress() {
        try {
            if (mc.getConnection() != null && mc.getConnection().getConnection() != null) {
                if (mc.getSingleplayerServer() != null) return "Singleplayer";
                String addr = mc.getConnection().getConnection().getRemoteAddress().toString();
                return addr.split(":")[0].replace("/", "");
            }
        } catch (Exception ignored) {}
        return "localhost";
    }
}