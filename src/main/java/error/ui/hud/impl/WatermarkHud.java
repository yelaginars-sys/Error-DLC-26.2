package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.player.AbstractClientPlayer;

public final class WatermarkHud extends HudElement implements IMinecraft {

    public WatermarkHud() {
        super("watermark", "Watermark", 6.0F, 6.0F, 240.0F, 20.0F, true);
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

        float padding = 3.0F;
        float fontSize = 7.5F;
        float dotFontSize = 12.0F;
        float headSize = 10.0F;
        float r = 4.0F;

        String username = mc.player != null ? mc.player.getScoreboardName() : "User";
        String role = (username.equalsIgnoreCase("zxcwashik") || username.equalsIgnoreCase("yelag")) ? "Разработчик" : "Пользователь";
        String clientName = "Waper";
        String fpsInfo = mc.getFps() + "fps";
        String serverInfo = getServerAddress();

        String fpsIcon = IconUse.FPS.glyph;
        String ipIcon = IconUse.GLOBE.glyph;

        String dot = ".";
        int dotColor = ColorUtil.rgba(150, 150, 150, ta);

        float dotW = Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 4.0F;

        float totalWidth = padding * 2.0F;
        totalWidth += Fonts.SF_MEDIUM.getWidth(clientName, fontSize);
        totalWidth += dotW;
        totalWidth += headSize + 2.0F;
        totalWidth += Fonts.SF_MEDIUM.getWidth(username + " [" + role + "]", fontSize);
        totalWidth += dotW;
        totalWidth += Fonts.ICONS.getWidth(fpsIcon, fontSize) + Fonts.SF_MEDIUM.getWidth(" " + fpsInfo, fontSize);
        totalWidth += dotW;
        totalWidth += Fonts.ICONS.getWidth(ipIcon, fontSize) + Fonts.SF_MEDIUM.getWidth(" " + serverInfo, fontSize);

        float totalHeight = Math.max(headSize, 9.0F) + padding * 2.0F;

        this.width = totalWidth;
        this.height = totalHeight;

        int themeAccent = Theme.getAccentColor();

        // Liquid glass background with blur and specular outline
        Render2D.drawShadow(x, y, totalWidth, totalHeight, r, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alpha)));
        int glassFill = ColorUtil.rgba(20, 18, 28, (int) (160 * alpha));
        Render2D.drawBlur(x, y, totalWidth, totalHeight, r, 12.0F, glassFill, alpha);
        Render2D.drawRoundedRect(x, y, totalWidth, totalHeight, r, glassFill);
        Render2D.drawRoundedOutline(x, y, totalWidth, totalHeight, r, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)));

        float currentX = x + padding;
        float textY = y + (totalHeight - 7.5F) / 2.0F;
        float headY = y + (totalHeight - headSize) / 2.0F;
        float dotY = y + (totalHeight - 12.0F) / 2.0F - 3.5F;

        // Client Name ("Waper" in primary theme accent)
        float clientW = Fonts.SF_MEDIUM.getWidth(clientName, fontSize);
        Fonts.drawString(Fonts.SF_MEDIUM, clientName, currentX, textY, fontSize, ColorUtil.withAlpha(themeAccent, ta));
        currentX += clientW;

        // Dot
        currentX += 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX, dotY, dotFontSize, dotColor);
        currentX += Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 2.0F;

        // Player Head
        if (mc.player instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawHead(clientPlayer, currentX, headY, headSize, headSize * 0.5F, alpha);
        }
        currentX += headSize + 2.0F;

        // Username
        String displayName = username;
        float maxNameW = 50.0F;
        float nameW = Fonts.SF_MEDIUM.getWidth(displayName, fontSize);
        if (nameW > maxNameW) {
            while (Fonts.SF_MEDIUM.getWidth(displayName + "...", fontSize) > maxNameW && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }
        Fonts.drawString(Fonts.SF_MEDIUM, displayName, currentX, textY, fontSize, ColorUtil.rgba(255, 255, 255, ta));
        currentX += Fonts.SF_MEDIUM.getWidth(displayName, fontSize);

        // Role
        String rText = " [" + role + "]";
        int roleCol = (username.equalsIgnoreCase("zxcwashik") || username.equalsIgnoreCase("yelag")) ? ColorUtil.withAlpha(themeAccent, ta) : dotColor;
        Fonts.drawString(Fonts.SF_MEDIUM, rText, currentX, textY, fontSize, roleCol);
        currentX += Fonts.SF_MEDIUM.getWidth(rText, fontSize);

        // Dot
        currentX += 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX, dotY, dotFontSize, dotColor);
        currentX += Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 2.0F;

        // FPS
        Fonts.drawIcon(IconUse.FPS, currentX, textY, fontSize, ColorUtil.rgba(255, 255, 255, ta));
        currentX += Fonts.ICONS.getWidth(fpsIcon, fontSize) + 1.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, fpsInfo, currentX, textY, fontSize, ColorUtil.rgba(255, 255, 255, ta));
        currentX += Fonts.SF_MEDIUM.getWidth(fpsInfo, fontSize);

        // Dot
        currentX += 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, dot, currentX, dotY, dotFontSize, dotColor);
        currentX += Fonts.SF_MEDIUM.getWidth(dot, dotFontSize) + 2.0F;

        // Server IP
        Fonts.drawIcon(IconUse.GLOBE, currentX, textY, fontSize, ColorUtil.rgba(255, 255, 255, ta));
        currentX += Fonts.ICONS.getWidth(ipIcon, fontSize) + 3.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, serverInfo, currentX, textY, fontSize, ColorUtil.rgba(255, 255, 255, ta));
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