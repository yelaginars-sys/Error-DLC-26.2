package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;

import java.util.*;

public final class CooldownsHudElement extends HudElement implements IMinecraft {

    public static record CooldownEntry(String name, long expireTime, int dotColor) {}

    private static final List<CooldownEntry> PREVIEW_ENTRIES = List.of(
            new CooldownEntry("Зачарованное золотое яблоко", System.currentTimeMillis() + 136000L, ColorUtil.rgba(255, 215, 0, 255)),
            new CooldownEntry("[*] Дезориентация", System.currentTimeMillis() + 54000L, ColorUtil.rgba(80, 220, 120, 255)),
            new CooldownEntry("[*] Огненный шар", System.currentTimeMillis() + 12000L, ColorUtil.rgba(255, 120, 50, 255)),
            new CooldownEntry("[*] Снежок заморозка", System.currentTimeMillis() + 4000L, ColorUtil.rgba(100, 220, 255, 255)),
            new CooldownEntry("Тотем бессмертия", System.currentTimeMillis() + 1000L, ColorUtil.rgba(240, 240, 240, 255))
    );

    private static final Map<String, CooldownEntry> ACTIVE_COOLDOWNS = new LinkedHashMap<>();

    public static void setCooldown(String name, long durationMs, int dotColor) {
        ACTIVE_COOLDOWNS.put(name, new CooldownEntry(name, System.currentTimeMillis() + durationMs, dotColor));
    }

    public CooldownsHudElement() {
        super("cooldowns", "Cooldowns", 680.0F, 10.0F, 175.0F, 120.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        MsdfFont font = Fonts.SF_MEDIUM;
        float fontUnit = 0.42F;

        long now = System.currentTimeMillis();
        ACTIVE_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue().expireTime() <= now);

        List<CooldownEntry> list = new ArrayList<>(ACTIVE_COOLDOWNS.values());
        if (list.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            list.addAll(PREVIEW_ENTRIES);
        }

        if (list.isEmpty()) {
            this.height = 0;
            return;
        }

        float padding = 8.0F;
        float headerH = 20.0F;
        float itemH = 16.0F;
        float totalH = headerH + list.size() * itemH + padding;
        float maxW = 180.0F;

        this.width = maxW;
        this.height = totalH;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 100);

        Render2D.drawBlur(x, y, width, height, 8.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 8.0F, bgColor, 1.0F, outlineColor);

        // Header
        Fonts.drawString(font, "Cooldowns", x + 10.0F, y + 5.0F, 0.48F, ColorUtil.rgba(240, 240, 250, 255));
        Render2D.drawRect(x + 10.0F, y + headerH - 2.0F, width - 20.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 30));

        float curY = y + headerH + 2.0F;
        for (CooldownEntry entry : list) {
            long remainingMs = Math.max(0L, entry.expireTime() - now);
            long totalSec = remainingMs / 1000L;
            String timeStr;
            if (totalSec >= 60) {
                timeStr = (totalSec / 60) + "m " + (totalSec % 60) + "s";
            } else {
                timeStr = totalSec + "s";
            }

            // Status Dot
            Render2D.drawCircle(x + 12.0F, curY + itemH / 2.0F - 1.0F, 2.5F, entry.dotColor());

            // Item Name
            String name = entry.name();
            if (font.getWidth(name, fontUnit) > 105.0F) {
                name = name.substring(0, Math.min(name.length(), 14)) + "...";
            }
            Fonts.drawString(font, name, x + 20.0F, curY + (itemH - font.lineHeight(fontUnit)) / 2.0F, fontUnit, ColorUtil.rgba(230, 230, 230, 255));

            // Time
            float timeW = font.getWidth(timeStr, fontUnit);
            Fonts.drawString(font, timeStr, x + width - 10.0F - timeW, curY + (itemH - font.lineHeight(fontUnit)) / 2.0F, fontUnit, ColorUtil.rgba(180, 180, 190, 255));

            curY += itemH;
        }
    }
}
