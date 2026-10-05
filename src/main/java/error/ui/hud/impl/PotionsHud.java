package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

public final class PotionsHud extends HudElement implements error.IMinecraft {

    private record EffectEntry(Holder<MobEffect> holder, String name, String lvl, String durationStr, int durationTicks, float alpha) {}

    private final Map<Holder<MobEffect>, Animation> anims = new HashMap<>();
    private final Animation widthAnim = new Animation(85.0F, 0.22F);
    private final Animation heightAnim = new Animation(18.0F, 0.22F);

    private static final float ROW_H = 15.0F;
    private static final float HEADER_H = 17.0F;
    private static final float PILL_R = 7.5F;
    private static final float ICON_SIZE = 9.0F;
    private static final float GAP_Y = 3.0F;

    public PotionsHud() {
        super("potions", "Potions", 10.0F, 160.0F, 90.0F, 40.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.potions.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> String.valueOf(num);
        };
    }

    private static String formatDuration(int duration, boolean infinite) {
        if (infinite || duration >= 32767 * 20) return "**:**";
        int secs = Math.max(0, duration / 20);
        int mins = secs / 60;
        int rem = secs % 60;
        return (mins < 10 ? "0" + mins : String.valueOf(mins)) + ":" + (rem < 10 ? "0" + rem : String.valueOf(rem));
    }

    private static int getDurationColor(int durationTicks, float alpha) {
        int a = (int) (240 * alpha);
        if (durationTicks <= 200) { // <= 10 sec
            return ColorUtil.rgba(255, 85, 85, a);
        } else if (durationTicks <= 600) { // <= 30 sec
            return ColorUtil.rgba(255, 170, 0, a);
        }
        return ColorUtil.rgba(235, 235, 235, a);
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;

        List<MobEffectInstance> effects = new ArrayList<>();
        if (mc.player != null) {
            effects.addAll(mc.player.getActiveEffects());
        }

        Set<Holder<MobEffect>> activeHolders = new HashSet<>();
        for (MobEffectInstance inst : effects) {
            activeHolders.add(inst.getEffect());
            Animation a = anims.computeIfAbsent(inst.getEffect(), k -> new Animation(0.0F, 0.20F));
            a.setTarget(1.0F);
            a.update();
        }

        anims.entrySet().removeIf(e -> {
            boolean active = activeHolders.contains(e.getKey());
            if (!active) {
                e.getValue().setTarget(0.0F);
                e.getValue().update();
            }
            return !active && e.getValue().getValue() <= 0.01F;
        });

        List<EffectEntry> entries = new ArrayList<>();
        for (MobEffectInstance inst : effects) {
            Animation a = anims.get(inst.getEffect());
            if (a != null && a.getValue() > 0.01F) {
                String durStr = formatDuration(inst.getDuration(), inst.isInfiniteDuration());
                String nameStr = inst.getEffect().value().getDisplayName().getString();
                int amp = inst.getAmplifier() + 1;
                String lvlStr = amp > 1 ? " " + toRoman(amp) : "";
                entries.add(new EffectEntry(inst.getEffect(), nameStr, lvlStr, durStr, inst.getDuration(), a.getValue()));
            }
        }

        if (entries.isEmpty() && inChat) {
            entries.add(new EffectEntry(MobEffects.STRENGTH, "Сила", " II", "01:45", 2100, 1.0F));
            entries.add(new EffectEntry(MobEffects.SPEED, "Скорость", " II", "00:25", 500, 1.0F));
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Calculate dynamic width
        float maxRowW = 85.0F;
        float headerTitleW = Fonts.SF_MEDIUM.getWidth("Potions", 9.0F);
        float headerMinW = 20.0F + headerTitleW + 8.0F;
        maxRowW = Math.max(maxRowW, headerMinW);

        for (EffectEntry e : entries) {
            float nameW = Fonts.SF_MEDIUM.getWidth(e.name + e.lvl, 8.5F);
            float durW = Fonts.SF_MEDIUM.getWidth(e.durationStr, 8.0F);
            float rowTotalW = (16.0F + nameW + 8.0F) + 6.0F + (durW + 12.0F);
            maxRowW = Math.max(maxRowW, rowTotalW);
        }

        float totalH = HEADER_H;
        for (EffectEntry e : entries) {
            totalH += (ROW_H + GAP_Y) * e.alpha;
        }

        widthAnim.setTarget(maxRowW);
        widthAnim.update();
        heightAnim.setTarget(totalH);
        heightAnim.update();

        this.width = widthAnim.getValue();
        this.height = heightAnim.getValue();

        int accent = Theme.getAccentColor();
        float curX = this.x;
        float curY = this.y;

        // 1. Header Capsule in Liquid Glass
        Render2D.drawShadow(curX, curY, this.width, HEADER_H, PILL_R, 6.0F, ColorUtil.rgba(0, 0, 0, 75));
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Blur.of(curX, curY, this.width, HEADER_H)
                    .radius(Math.round(PILL_R))
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(14, 16, 22, 115))
                    .alpha(1.0F)
                    .render(extractor);

            Outline.of(curX, curY, this.width, HEADER_H)
                    .radius(Math.round(PILL_R))
                    .thickness(0.85F)
                    .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 32))
                    .alpha(1.0F)
                    .render(extractor);
        }

        // Energy Glyph "q"
        Fonts.drawString(Fonts.ENERGY, "q", curX + 6.0F, curY + 2.5F, 10.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Potions", curX + 19.0F, curY + 3.0F, 9.0F, 0xFFFFFFFF);

        curY += HEADER_H + GAP_Y;

        // 2. Entries: Left capsule (Icon + Name + Level) and Right capsule (Duration)
        for (EffectEntry e : entries) {
            if (e.alpha <= 0.01F) continue;

            int textWhite = ColorUtil.rgba(255, 255, 255, (int) (245 * e.alpha));
            int durColor = getDurationColor(e.durationTicks, e.alpha);

            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 8.5F);
            float lvlW = e.lvl.isEmpty() ? 0 : Fonts.SF_MEDIUM.getWidth(e.lvl, 8.0F);
            float durTextW = Fonts.SF_MEDIUM.getWidth(e.durationStr, 8.0F);

            float leftPillW = 16.0F + nameW + lvlW + 8.0F;
            float rightPillW = durTextW + 12.0F;
            float rightPillX = curX + this.width - rightPillW;

            // Shadows
            Render2D.drawShadow(curX, curY, leftPillW, ROW_H, PILL_R, 4.0F, ColorUtil.rgba(0, 0, 0, (int) (50 * e.alpha)));
            Render2D.drawShadow(rightPillX, curY, rightPillW, ROW_H, PILL_R, 4.0F, ColorUtil.rgba(0, 0, 0, (int) (50 * e.alpha)));

            // Liquid Glass Kawase Blur & Specular Outlines
            if (extractor != null) {
                Render2DUtil.flush();

                // Left Capsule Blur & Outline
                Blur.of(curX, curY, leftPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .type(BlurType.KAWASE)
                        .strength(3)
                        .tint(Color.rgba(14, 16, 22, (int) (110 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);

                Outline.of(curX, curY, leftPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .thickness(0.7F)
                        .verticalGradient(Color.rgba(255, 255, 255, (int) (28 * e.alpha)), Color.rgba(255, 255, 255, (int) (6 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);

                // Right Capsule Blur & Outline
                Blur.of(rightPillX, curY, rightPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .type(BlurType.KAWASE)
                        .strength(3)
                        .tint(Color.rgba(14, 16, 22, (int) (110 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);

                Outline.of(rightPillX, curY, rightPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .thickness(0.7F)
                        .verticalGradient(Color.rgba(255, 255, 255, (int) (28 * e.alpha)), Color.rgba(255, 255, 255, (int) (6 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);
            }

            // Effect Icon
            Identifier sprite = Hud.getMobEffectSprite(e.holder);
            if (extractor != null && sprite != null) {
                Render2DUtil.flush();
                float iconX = curX + 4.5F;
                float iconY = curY + (ROW_H - ICON_SIZE) / 2.0F;
                int iconTint = ColorUtil.rgba(255, 255, 255, (int) (240 * e.alpha));
                try {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE), iconTint);
                } catch (Throwable t) {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE));
                }
            } else {
                Render2D.drawCircle(curX + 8.0F, curY + ROW_H * 0.5F, 2.0F, ColorUtil.withAlpha(accent, (int) (230 * e.alpha)));
            }

            // Name + Level
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, curX + 16.0F, curY + 2.0F, 8.5F, textWhite);
            if (!e.lvl.isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, e.lvl, curX + 16.0F + nameW, curY + 2.2F, 8.0F, ColorUtil.withAlpha(accent, (int) (240 * e.alpha)));
            }

            // Duration text centered in right capsule
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, e.durationStr, rightPillX + rightPillW * 0.5F, curY + 2.2F, 8.0F, durColor);

            curY += (ROW_H + GAP_Y) * e.alpha;
        }
    }
}
