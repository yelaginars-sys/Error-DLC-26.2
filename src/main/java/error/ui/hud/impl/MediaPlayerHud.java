package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

public final class MediaPlayerHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);

    public MediaPlayerHud() {
        super("media_player", "Media Player", 140.0F, 10.0F, 100.0F, 30.0F);
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        fadeAnim.setTarget(editing ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float width = 100.0F;
        float height = 30.0F;
        float radius = 5.0F;

        this.width = width;
        this.height = height;

        int primaryColor = ACCENT_PURPLE;
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(14, 14, 18, (int) (160 * alpha));

        // Background (Waper Style)
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Track Thumbnail Placeholder Box
        float artX = drawX + 3.5F;
        float artY = drawY + 3.5F;
        float artSize = 23.0F;
        Render2D.drawRoundedRect(artX, artY, artSize, artSize, 3.0F, ColorUtil.rgba(30, 32, 42, (int) (200 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "🎵", artX + 6.0F, artY + 6.0F, 10.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Track Title & Artist Text
        float textX = artX + artSize + 5.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "ссница Восьмі", textX, drawY + 5.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Виктор Цой", textX, drawY + 13.5F, 5.5F, ColorUtil.applyAlpha(ColorUtil.rgba(160, 160, 175, 255), alpha));

        // Progress Line
        float barX = textX;
        float barY = drawY + height - 5.0F;
        float barW = width - (textX - drawX) - 4.0F;
        Render2D.drawRoundedRect(barX, barY, barW, 2.0F, 1.0F, ColorUtil.rgba(35, 37, 48, (int) (180 * alpha)));
        Render2D.drawRoundedRect(barX, barY, barW * 0.65F, 2.0F, 1.0F, ColorUtil.applyAlpha(primaryColor, alpha));
    }
}
