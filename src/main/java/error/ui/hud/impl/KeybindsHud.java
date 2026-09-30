package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.module.Module;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.util.ArrayList;
import java.util.List;

public final class KeybindsHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ROW_HEIGHT = 12.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_BOTTOM = 4.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 195);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);

    public KeybindsHud() {
        super("hotkeys", "HotKeys", 10.0F, 80.0F, 95.0F, HEADER_HEIGHT + PADDING_BOTTOM);
    }

    private record RawBind(String name, String keyName) {}

    private List<RawBind> getCurrentBinds() {
        List<RawBind> list = new ArrayList<>();
        if (Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
            for (Module m : Client.INSTANCE.moduleManager.getModules()) {
                if (m.isEnabled() && m.getBind() != null && m.getBind().isBound()) {
                    String display = m.getBind().getDisplayValue();
                    if (!display.equalsIgnoreCase("NONE")) {
                        list.add(new RawBind(m.getName(), "[" + display + "]"));
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<RawBind> binds = getCurrentBinds();
        boolean editing = isHovered(HudManager.getMouseX(), HudManager.getMouseY()) || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        if (binds.isEmpty() && !editing) {
            fadeAnim.setTarget(0.0F);
            fadeAnim.update();
            return;
        }

        fadeAnim.setTarget(1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();
        float contentHeight = binds.size() * ROW_HEIGHT;
        float totalHeight = HEADER_HEIGHT + contentHeight + PADDING_BOTTOM;

        this.height = totalHeight;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 30));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 60));
        int bgColor = ColorUtil.applyAlpha(BG_COLOR, alpha);

        // Render Background (Glow + Border + Container)
        Render2D.drawShadow(drawX, drawY, width, totalHeight, 5.0F, 6.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, totalHeight + 1.0F, 5.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, totalHeight, 5.0F, bgColor);

        // Header Title
        Fonts.drawIcon(IconUse.KEYBOARD, drawX + PADDING_X, drawY + 3.5F, 8.0F, ColorUtil.applyAlpha(primaryColor, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "HotKeys", drawX + PADDING_X + 11.0F, drawY + 4.0F, 6.5F, ColorUtil.applyAlpha(-1, alpha));

        // Render Active Binds
        float currentY = drawY + HEADER_HEIGHT;
        if (binds.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "No active keys", drawX + PADDING_X, currentY + 1.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 180, 255), alpha));
        } else {
            for (RawBind b : binds) {
                Fonts.drawString(Fonts.SF_MEDIUM, b.name(), drawX + PADDING_X, currentY + 1.5F, 6.0F, ColorUtil.applyAlpha(-1, alpha));
                float keyW = Fonts.SF_MEDIUM.getWidth(b.keyName(), 6.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, b.keyName(), drawX + width - PADDING_X - keyW, currentY + 1.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));
                currentY += ROW_HEIGHT;
            }
        }
    }
}