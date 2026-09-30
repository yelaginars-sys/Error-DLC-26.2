package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public final class PotionsHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ROW_HEIGHT = 12.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_BOTTOM = 4.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 195);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);

    public PotionsHud() {
        super("potions", "Potions", 10.0F, 180.0F, 105.0F, HEADER_HEIGHT + PADDING_BOTTOM);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    private record ActivePotion(String name, String duration, boolean harmful) {}

    private List<ActivePotion> getActivePotions() {
        List<ActivePotion> list = new ArrayList<>();
        if (mc.player != null) {
            for (MobEffectInstance effect : mc.player.getActiveEffects()) {
                Holder<MobEffect> holder = effect.getEffect();
                String name = holder.value().getDisplayName().getString();
                if (effect.getAmplifier() > 0) {
                    name += " " + (effect.getAmplifier() + 1);
                }
                int ticks = effect.getDuration();
                int seconds = (ticks / 20) % 60;
                int minutes = (ticks / 20) / 60;
                String durationStr = String.format("%d:%02d", minutes, seconds);
                boolean harmful = !holder.value().isBeneficial();
                list.add(new ActivePotion(name, durationStr, harmful));
            }
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<ActivePotion> potions = getActivePotions();
        boolean editing = isHovered(HudManager.getMouseX(), HudManager.getMouseY()) || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        if (potions.isEmpty() && !editing) {
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
        float contentHeight = potions.size() * ROW_HEIGHT;
        float totalHeight = HEADER_HEIGHT + contentHeight + PADDING_BOTTOM;

        this.height = totalHeight;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 30));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 60));
        int bgColor = ColorUtil.applyAlpha(BG_COLOR, alpha);

        // Render Background
        Render2D.drawShadow(drawX, drawY, width, totalHeight, 5.0F, 6.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, totalHeight + 1.0F, 5.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, totalHeight, 5.0F, bgColor);

        // Header Title
        Fonts.drawIcon(IconUse.POTION, drawX + PADDING_X, drawY + 3.5F, 8.0F, ColorUtil.applyAlpha(primaryColor, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Potions", drawX + PADDING_X + 11.0F, drawY + 4.0F, 6.5F, ColorUtil.applyAlpha(-1, alpha));

        // Render Active Effects
        float currentY = drawY + HEADER_HEIGHT;
        if (potions.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "No active effects", drawX + PADDING_X, currentY + 1.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 180, 255), alpha));
        } else {
            for (ActivePotion p : potions) {
                int textColor = p.harmful() ? ColorUtil.rgba(235, 75, 75, 255) : -1;
                Fonts.drawString(Fonts.SF_MEDIUM, p.name(), drawX + PADDING_X, currentY + 1.5F, 6.0F, ColorUtil.applyAlpha(textColor, alpha));
                float durationW = Fonts.SF_MEDIUM.getWidth(p.duration(), 6.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, p.duration(), drawX + width - PADDING_X - durationW, currentY + 1.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));
                currentY += ROW_HEIGHT;
            }
        }
    }
}