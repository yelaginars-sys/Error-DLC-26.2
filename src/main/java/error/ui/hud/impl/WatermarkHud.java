package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.player.AbstractClientPlayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class WatermarkHud extends HudElement implements IMinecraft {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public WatermarkHud() {
        super("watermark", "Watermark", 6.0F, 6.0F, 240.0F, 20.0F);
    }

    @Override
    public void draw(Render2DEvent event) {
        fadeAnim.setTarget(1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        String clientName = "Error DLC";
        String username = mc.player != null ? mc.player.getScoreboardName() : "User";
        String role = username.equalsIgnoreCase("zxcwashik") || username.equalsIgnoreCase("yelag") ? "Developer" : "User";
        String fpsInfo = mc.getFps() + " fps";
        String serverInfo = getServerAddress();

        float padding = 4.0F;
        float fontSize = 6.5F;
        float dotFontSize = 10.0F;
        float headSize = 10.0F;
        float radius = 5.0F;
        String dot = "•";

        float clientW = Fonts.SF_MEDIUM.getWidth(clientName, fontSize);
        float userW = Fonts.SF_MEDIUM.getWidth(username + " [" + role + "]", fontSize);
        float fpsW = Fonts.ICONS.getWidth(IconUse.FPS.glyph, fontSize) + Fonts.SF_MEDIUM.getWidth(" " + fpsInfo, fontSize);
        float ipW = Fonts.ICONS.getWidth(IconUse.GLOBE.glyph, fontSize) + Fonts.SF_MEDIUM.getWidth(" " + serverInfo, fontSize);
        float dotW = Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 4.0F;

        float totalWidth = padding * 2.0F + clientW + dotW + headSize + 3.0F + userW + dotW + fpsW + dotW + ipW;
        float totalHeight = 18.0F;

        this.width = totalWidth;
        this.height = totalHeight;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 35));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 70));
        int bgColor = ColorUtil.applyAlpha(ColorUtil.rgba(14, 14, 18, 195), alpha);
        int dotColor = ColorUtil.rgba(150, 150, 150, (int) (alpha * 255));
        int whiteColor = ColorUtil.applyAlpha(-1, alpha);

        // Render Background (Glow border + Blur style + Dark container)
        Render2D.drawShadow(drawX, drawY, totalWidth, totalHeight, radius, 6.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, totalWidth + 1.0F, totalHeight + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, totalWidth, totalHeight, radius, bgColor);

        float currentX = drawX + padding;
        float textY = drawY + (totalHeight - 6.5F) / 2.0F;
        float headY = drawY + (totalHeight - headSize) / 2.0F;
        float dotY = drawY + (totalHeight - 8.0F) / 2.0F;

        // 1. Client Name
        Fonts.drawString(Fonts.SF_MEDIUM, clientName, currentX, textY, fontSize, ColorUtil.applyAlpha(primaryColor, alpha));
        currentX += clientW;

        // Dot
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX + 2.0F, dotY, dotFontSize, dotColor);
        currentX += dotW;

        // 2. Player Head
        if (mc.player instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawHead(clientPlayer, currentX, headY, headSize, headSize, radius * 0.5F, alpha);
        }
        currentX += headSize + 3.0F;

        // 3. Username & Role
        Fonts.drawString(Fonts.SF_MEDIUM, username, currentX, textY, fontSize, whiteColor);
        currentX += Fonts.SF_MEDIUM.getWidth(username, fontSize);
        String roleStr = " [" + role + "]";
        int roleColor = role.equalsIgnoreCase("Developer") ? primaryColor : dotColor;
        Fonts.drawString(Fonts.SF_MEDIUM, roleStr, currentX, textY, fontSize, roleColor);
        currentX += Fonts.SF_MEDIUM.getWidth(roleStr, fontSize);

        // Dot
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX + 2.0F, dotY, dotFontSize, dotColor);
        currentX += dotW;

        // 4. FPS
        Fonts.drawIcon(IconUse.FPS, currentX, textY, fontSize, whiteColor);
        currentX += Fonts.ICONS.getWidth(IconUse.FPS.glyph, fontSize) + 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, fpsInfo, currentX, textY, fontSize, whiteColor);
        currentX += Fonts.SF_MEDIUM.getWidth(fpsInfo, fontSize);

        // Dot
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX + 2.0F, dotY, dotFontSize, dotColor);
        currentX += dotW;

        // 5. Server IP
        Fonts.drawIcon(IconUse.GLOBE, currentX, textY, fontSize, whiteColor);
        currentX += Fonts.ICONS.getWidth(IconUse.GLOBE.glyph, fontSize) + 3.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, serverInfo, currentX, textY, fontSize, whiteColor);
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