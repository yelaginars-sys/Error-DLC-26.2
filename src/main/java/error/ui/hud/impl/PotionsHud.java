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

    private static final float ROW_H = 13.0F;
    private static final float HEADER_H = 14.5F;
    private static final float PILL_R = 6.25F;
    private static final float ICON_SIZE = 7.5F;
    private static final float GAP_Y = 2.0F;

    public PotionsHud() {
        super("potions", "Potions", 10.0F, 160.0F, 75.0F, 35.0F, true);
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
        float maxRowW = 72.0F;
        float headerTitleW = Fonts.SF_MEDIUM.getWidth("Potions", 7.5F);
        float headerMinW = 18.0F + headerTitleW + 6.0F;
        maxRowW = Math.max(maxRowW, headerMinW);

        for (EffectEntry e : entries) {
            float nameW = Fonts.SF_MEDIUM.getWidth(e.name + e.lvl, 7.0F);
            float durW = Fonts.SF_MEDIUM.getWidth(e.durationStr, 6.8F);
            float rowTotalW = (13.5F + nameW + 6.0F) + 5.0F + (durW + 9.0F);
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
        Render2D.drawShadow(curX, curY, this.width, HEADER_H, PILL_R, 5.0F, ColorUtil.rgba(0, 0, 0, 70));
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
                    .thickness(0.75F)
                    .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 32))
                    .alpha(1.0F)
                    .render(extractor);
        }

        // Energy Glyph "q"
        Fonts.drawString(Fonts.ENERGY, "q", curX + 5.0F, curY + 2.0F, 8.5F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Potions", curX + 16.0F, curY + 2.2F, 7.5F, 0xFFFFFFFF);

        curY += HEADER_H + GAP_Y;

        // 2. Entries: Left capsule (Icon + Name + Level) and Right capsule (Duration)
        for (EffectEntry e : entries) {
            if (e.alpha <= 0.01F) continue;

            int textWhite = ColorUtil.rgba(255, 255, 255, (int) (245 * e.alpha));
            int durColor = getDurationColor(e.durationTicks, e.alpha);

            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 7.0F);
            float lvlW = e.lvl.isEmpty() ? 0 : Fonts.SF_MEDIUM.getWidth(e.lvl, 6.5F);
            float durTextW = Fonts.SF_MEDIUM.getWidth(e.durationStr, 6.8F);

            float leftPillW = 13.5F + nameW + lvlW + 6.0F;
            float rightPillW = durTextW + 9.0F;
            float rightPillX = curX + this.width - rightPillW;

            // Shadows
            Render2D.drawShadow(curX, curY, leftPillW, ROW_H, PILL_R, 3.5F, ColorUtil.rgba(0, 0, 0, (int) (45 * e.alpha)));
            Render2D.drawShadow(rightPillX, curY, rightPillW, ROW_H, PILL_R, 3.5F, ColorUtil.rgba(0, 0, 0, (int) (45 * e.alpha)));

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
                        .thickness(0.65F)
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
                        .thickness(0.65F)
                        .verticalGradient(Color.rgba(255, 255, 255, (int) (28 * e.alpha)), Color.rgba(255, 255, 255, (int) (6 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);
            }

            // Effect Icon
            Identifier sprite = Hud.getMobEffectSprite(e.holder);
            if (extractor != null && sprite != null) {
                Render2DUtil.flush();
                float iconX = curX + 3.5F;
                float iconY = curY + (ROW_H - ICON_SIZE) / 2.0F;
                int iconTint = ColorUtil.rgba(255, 255, 255, (int) (240 * e.alpha));
                try {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE), iconTint);
                } catch (Throwable t) {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE));
                }
            } else {
                Render2D.drawCircle(curX + 6.5F, curY + ROW_H * 0.5F, 1.8F, ColorUtil.withAlpha(accent, (int) (230 * e.alpha)));
            }

            // Name + Level
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, curX + 13.5F, curY + 1.8F, 7.0F, textWhite);
            if (!e.lvl.isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, e.lvl, curX + 13.5F + nameW, curY + 1.9F, 6.5F, ColorUtil.withAlpha(accent, (int) (240 * e.alpha)));
            }

            // Duration text centered in right capsule
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, e.durationStr, rightPillX + rightPillW * 0.5F, curY + 1.8F, 6.8F, durColor);

            curY += (ROW_H + GAP_Y) * e.alpha;
        }
    }
}
