package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public final class PotionsHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);
    private static final float HEADER_HEIGHT = 14.0F;

    public PotionsHud() {
        super("potions", "Potions", 6.0F, 180.0F, 105.0F, HEADER_HEIGHT + 14.0F);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    private record ActivePotion(String name, String duration, IconUse icon) {}

    private List<ActivePotion> getActivePotions() {
        List<ActivePotion> list = new ArrayList<>();
        if (mc.player == null) return list;

        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            String name = effect.getEffect().value().getDescriptionId();
            if (name.contains(".")) {
                String[] parts = name.split("\\.");
                name = parts[parts.length - 1];
            }
            name = name.replace("_", " ");
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            if (effect.getAmplifier() > 0) {
                name += " " + (effect.getAmplifier() + 1);
            }

            String duration;
            if (effect.isInfiniteDuration()) {
                duration = "**:**";
            } else {
                int totalSecs = effect.getDuration() / 20;
                int mins = totalSecs / 60;
                int secs = totalSecs % 60;
                duration = String.format("%d:%02d", mins, secs);
            }

            IconUse icon = IconUse.POTION;

            list.add(new ActivePotion(name, duration, icon));
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<ActivePotion> potions = getActivePotions();
        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        fadeAnim.setTarget((!potions.isEmpty() || editing) ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float padX = 4.0F;
        float headerH = HEADER_HEIGHT;
        float itemH = 12.0F;
        float radius = 5.0F;

        String title = "Potions";
        float maxNameW = Fonts.SF_MEDIUM.getWidth(title, 6.0F);
        float maxDurW = 0.0F;

        if (potions.isEmpty() && editing) {
            potions.add(new ActivePotion("Ночное зрение", "**:**", IconUse.POTION));
            potions.add(new ActivePotion("Огнестойкость", "3:56", IconUse.POTION));
            potions.add(new ActivePotion("Сопротивление", "3:56", IconUse.POTION));
            potions.add(new ActivePotion("Поглощение 4", "56s", IconUse.POTION));
        }

        for (ActivePotion p : potions) {
            maxNameW = Math.max(maxNameW, Fonts.SF_MEDIUM.getWidth(p.name(), 6.0F));
            maxDurW = Math.max(maxDurW, Fonts.SF_MEDIUM.getWidth(p.duration(), 6.0F));
        }

        float width = Math.max(105.0F, padX * 2.0F + maxNameW + maxDurW + 20.0F);
        float height = headerH + potions.size() * itemH + 3.0F;

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
        Fonts.drawString(Fonts.SF_MEDIUM, title, drawX + padX, drawY + 3.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Rows
        float currentY = drawY + headerH + 2.0F;
        for (ActivePotion p : potions) {
            Fonts.drawIcon(p.icon(), drawX + padX, currentY + 2.0F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, p.name(), drawX + padX + 10.0F, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

            float durW = Fonts.SF_MEDIUM.getWidth(p.duration(), 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, p.duration(), drawX + width - padX - durW, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 190, 255), alpha));

            currentY += itemH;
        }
    }
}