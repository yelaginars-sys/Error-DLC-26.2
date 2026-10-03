package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class WatermarkHudElement extends HudElement implements IMinecraft {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public WatermarkHudElement() {
        super("watermark", "Watermark", 10.0F, 10.0F, 155.0F, 22.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        float h = 22.0F;
        float r = 6.0F;

        int fps = mc.getFps();
        String timeStr = LocalTime.now().format(TIME_FORMATTER);
        String clientName = "sk3dguard";

        MsdfFont font = Fonts.SF_MEDIUM;
        float fontUnit = 0.50F;

        float nameW = font.getWidth(clientName, fontUnit);
        float fpsW = font.getWidth(fps + " fps", fontUnit);
        float timeW = font.getWidth(timeStr, fontUnit);

        float calcW = 12.0F + nameW + 12.0F + fpsW + 12.0F + timeW + 10.0F;
        this.width = Math.max(145.0F, calcW);
        this.height = h;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 120);

        Render2D.drawBlur(x, y, width, height, r, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, r, bgColor, 1.0F, outlineColor);

        float curX = x + 8.0F;
        float textY = y + (height - font.lineHeight(fontUnit)) / 2.0F + 1.0F;

        // Client Name
        Fonts.drawString(font, clientName, curX, textY, fontUnit, Theme.getAccentColor());
        curX += nameW + 8.0F;

        // Separator dot
        Render2D.drawCircle(curX, y + height / 2.0F, 1.5F, ColorUtil.rgba(255, 255, 255, 100));
        curX += 8.0F;

        // FPS
        Fonts.drawString(font, fps + " fps", curX, textY, fontUnit, ColorUtil.rgba(220, 220, 220, 255));
        curX += fpsW + 8.0F;

        // Separator dot
        Render2D.drawCircle(curX, y + height / 2.0F, 1.5F, ColorUtil.rgba(255, 255, 255, 100));
        curX += 8.0F;

        // Clock Time
        Fonts.drawString(font, timeStr, curX, textY, fontUnit, ColorUtil.rgba(180, 180, 180, 255));
    }
}
