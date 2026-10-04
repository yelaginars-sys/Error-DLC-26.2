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

import java.util.ArrayList;
import java.util.List;

public final class UseTrackerHudElement extends HudElement implements IMinecraft {

    private static record TrackedUse(String player, String action, String time) {}

    private static final List<TrackedUse> PREVIEW_USES = List.of(
            new TrackedUse("Steve", "съел чарку", "1s назад"),
            new TrackedUse("Alex", "поставил тнт", "3s назад"),
            new TrackedUse("Notch", "заюзал поппер", "5s назад")
    );

    public UseTrackerHudElement() {
        super("usetracker", "Use Tracker", 200.0F, 120.0F, 130.0F, 50.0F, false);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        List<TrackedUse> activeUses = new ArrayList<>();
        if (HudManager.getInstance().isDraggableScreenOpen()) {
            activeUses.addAll(PREVIEW_USES);
        }

        if (activeUses.isEmpty()) {
            this.height = 0;
            return;
        }

        MsdfFont font = Fonts.SF_MEDIUM;
        float headerSize = 7.0F;
        float itemSize = 6.0F;
        float headerH = 18.0F;
        float itemH = 12.0F;
        float pad = 6.0F;

        this.width = 130.0F;
        this.height = headerH + activeUses.size() * itemH + pad;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 100);

        Render2D.drawBlur(x, y, width, height, 6.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 6.0F, bgColor, 1.0F, outlineColor);

        // Header
        Fonts.drawString(font, "Use Tracker", x + 8.0F, y + 4.0F, headerSize, ColorUtil.rgba(240, 240, 250, 255));
        Render2D.drawRect(x + 8.0F, y + headerH - 2.0F, width - 16.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 30));

        float curY = y + headerH + 2.0F;
        for (TrackedUse use : activeUses) {
            String str = use.player() + " " + use.action();
            Fonts.drawString(font, str, x + 8.0F, curY + (itemH - font.lineHeight(itemSize)) / 2.0F, itemSize, ColorUtil.rgba(220, 220, 235, 255));
            float timeW = font.getWidth(use.time(), itemSize);
            Fonts.drawString(font, use.time(), x + width - 8.0F - timeW, curY + (itemH - font.lineHeight(itemSize)) / 2.0F, itemSize, ColorUtil.rgba(160, 160, 180, 255));
            curY += itemH;
        }
    }
}
