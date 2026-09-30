package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

public final class CooldownsHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);
    private static final float HEADER_HEIGHT = 14.0F;

    public CooldownsHud() {
        super("cooldowns", "Cooldowns", 6.0F, 40.0F, 80.0F, HEADER_HEIGHT + 14.0F);
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

        float padX = 4.0F;
        float headerH = HEADER_HEIGHT;
        float width = 80.0F;
        float height = headerH + 3.0F;
        float radius = 5.0F;

        this.width = width;
        this.height = height;

        int primaryColor = ACCENT_PURPLE;
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(14, 14, 18, (int) (160 * alpha));
        int headerBg = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));

        // Background (Waper Style)
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, headerH, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, "Cooldowns", drawX + padX, drawY + 3.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));
    }
}
