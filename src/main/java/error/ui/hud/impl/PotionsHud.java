package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;
import java.util.Locale;

public final class PotionsHud extends HudElement implements IMinecraft {
    private final Animation heightAnim = new Animation(0.0F, 0.22F);
    private final Animation chatOffsetAnim = new Animation(0.0F, 0.22F);

    public PotionsHud() {
        super("potions", "Potions", 200.0F, 100.0F, 135.0F, 30.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (!isEnabled() || mc.player == null) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        fadeAnim.setTarget(effects.isEmpty() ? 0.0F : 1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float headerH = 18.0F;
        float itemH = 13.0F;
        float targetH = headerH + (effects.size() * itemH) + 4.0F;

        heightAnim.setTarget(targetH);
        heightAnim.update();
        this.height = heightAnim.getValue();

        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
        chatOffsetAnim.setTarget(chatOpen ? -20.0F : 0.0F);
        chatOffsetAnim.update();

        float renderY = (dragging ? getY() : getY()) + chatOffsetAnim.getValue();
        float renderX = getX();

        int accent = Theme.getAccentColor();
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));
        int glassFill = ColorUtil.rgba(16, 18, 26, (int) (205 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (35 * alpha));

        Render2D.drawShadow(renderX, renderY, width, height, 8.0F, 8.0F, shadowCol);
        Render2D.drawBlur(renderX, renderY, width, height, 8.0F, 14.0F, glassFill, alpha);
        Render2D.drawRoundedRect(renderX, renderY, width, height, 8.0F, glassFill);
        Render2D.drawRoundedOutline(renderX, renderY, width, height, 8.0F, 1.0F, glassBorder);

        // Header: Potions Title + Potion Icon
        Fonts.drawString(Fonts.SF_MEDIUM, "Potions", renderX + 8.0F, renderY + 4.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawIcon(IconUse.POTION, renderX + width - 16.0F, renderY + 4.5F, 7.5F, ColorUtil.multiplyAlpha(accent, alpha));

        // Line Divider
        Render2D.drawRoundedRect(renderX + 6.0F, renderY + headerH, width - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

        float curY = renderY + headerH + 3.0F;
        Render2D.pushScissor(renderX, renderY + headerH, width, height - headerH);
        for (MobEffectInstance effect : effects) {
            String name = Localization.get(effect.getEffect().value().getDescriptionId());
            int amp = effect.getAmplifier() + 1;
            String roman = amp > 1 ? " " + toRoman(amp) : "";

            int durationSecs = effect.getDuration() / 20;
            int mins = durationSecs / 60;
            int secs = durationSecs % 60;
            String timeStr = durationSecs > 8000 ? "8333:19" : String.format(Locale.ROOT, "%d:%02d", mins, secs);

            Fonts.drawIcon(IconUse.POTION, renderX + 8.0F, curY + 1.0F, 6.5F, ColorUtil.multiplyAlpha(accent, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, name + roman, renderX + 18.0F, curY + 1.0F, 6.5F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));

            int timeColor = durationSecs < 30 ? ColorUtil.rgba(245, 190, 45, (int) (240 * alpha)) : ColorUtil.rgba(180, 185, 205, (int) (190 * alpha));
            float timeW = Fonts.SF_MEDIUM.getWidth(timeStr, 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, timeStr, renderX + width - timeW - 8.0F, curY + 1.0F, 6.0F, timeColor);

            curY += itemH;
        }
        Render2D.popScissor();
    }

    private String toRoman(int num) {
        return switch (num) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }
}
