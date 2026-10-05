package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
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

public final class PotionsHud extends HudElement {

    private final Map<Holder<MobEffect>, Animation> anims = new HashMap<>();
    private final Animation totalWidthAnim = new Animation(70.0F, 0.20F);
    private final Animation totalHeightAnim = new Animation(15.0F, 0.20F);

    private static final float ROW_H = 14.0F;
    private static final float HEADER_H = 14.0F;
    private static final float ICON_SIZE = 8.5F;
    private static final float GAP_X = 2.0F;
    private static final float GAP_Y = 2.5F;

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

    private static String formatDuration(int duration, boolean infinite) {
        if (infinite || duration >= 32767 * 20) return "inf";
        int secs = Math.max(0, duration / 20);
        int mins = secs / 60;
        int rem = secs % 60;
        return (mins < 10 ? "0" + mins : String.valueOf(mins)) + ":" + (rem < 10 ? "0" + rem : String.valueOf(rem));
    }

    private static final class DummyEffect {
        final Holder<MobEffect> holder;
        final String name;
        final String lvl;
        final String dur;

        DummyEffect(Holder<MobEffect> holder, String name, String lvl, String dur) {
            this.holder = holder;
            this.name = name;
            this.lvl = lvl;
            this.dur = dur;
        }
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean inChat = error.IMinecraft.mc.gui != null && error.IMinecraft.mc.gui.screen() instanceof ChatScreen;

        List<MobEffectInstance> effects = new ArrayList<>();
        if (error.IMinecraft.mc.player != null) {
            effects.addAll(error.IMinecraft.mc.player.getActiveEffects());
        }

        Set<Holder<MobEffect>> activeTypes = new HashSet<>();
        for (MobEffectInstance inst : effects) {
            activeTypes.add(inst.getEffect());
            Animation a = anims.computeIfAbsent(inst.getEffect(), k -> new Animation(0.0F, 0.20F));
            a.setTarget(1.0F);
            a.update();
        }

        for (Map.Entry<Holder<MobEffect>, Animation> entry : anims.entrySet()) {
            if (!activeTypes.contains(entry.getKey())) {
                entry.getValue().setTarget(0.0F);
                entry.getValue().update();
            }
        }
        anims.entrySet().removeIf(e -> !activeTypes.contains(e.getKey()) && e.getValue().getValue() <= 0.01F);

        int accent = Theme.getAccentColor();
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();

        float headerIconW = Fonts.getIconWidth(IconUse.POTION, 9.0F);
        float headerTextW = Fonts.SF_MEDIUM.getWidth("Potions", 9.5F);
        float headerW = 6.0F + headerIconW + 4.0F + headerTextW + 7.0F;

        if (effects.isEmpty()) {
            if (!inChat) {
                this.width = 0;
                this.height = 0;
                return;
            }

            // Preview in ChatScreen with 2 dummy effects
            Render2D.drawHudPill(this.x, this.y, headerW, HEADER_H, 1.0F);
            Fonts.drawIcon(IconUse.POTION, this.x + 6.0F, this.y + 2.5F, 9.0F, accent);
            Fonts.drawString(Fonts.SF_MEDIUM, "Potions", this.x + 6.0F + headerIconW + 4.0F, this.y + 2.5F, 9.5F, 0xFFFFFFFF);

            List<DummyEffect> dummies = List.of(
                    new DummyEffect(MobEffects.SPEED, "Скорость", " LVL. 2", "01:25"),
                    new DummyEffect(MobEffects.STRENGTH, "Сила", " LVL. 1", "00:45")
            );

            float rowY = this.y + HEADER_H + GAP_Y;
            float maxPreviewW = headerW;

            for (DummyEffect d : dummies) {
                float durTextW = Fonts.SF_MEDIUM.getWidth(d.dur, 8.5F);
                float durW = 4.5F + ICON_SIZE + 3.5F + durTextW + 4.5F;

                float nameTextW = Fonts.SF_MEDIUM.getWidth(d.name, 8.5F);
                float lvlTextW = d.lvl.isEmpty() ? 0 : Fonts.SF_MEDIUM.getWidth(d.lvl, 8.0F);
                float nameW = 5.5F + nameTextW + lvlTextW + 5.5F;

                // Left Pill
                Render2D.drawHudPill(this.x, rowY, durW, ROW_H, 0.85F);

                // Effect Logo/Sprite
                Identifier sprite = Hud.getMobEffectSprite(d.holder);
                if (extractor != null && sprite != null) {
                    Render2DUtil.flush();
                    float iconX = this.x + 4.5F;
                    float iconY = rowY + (ROW_H - ICON_SIZE) / 2.0F;
                    try {
                        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE), 0xFFFFFFFF);
                    } catch (Throwable t) {
                        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE));
                    }
                }

                Fonts.drawString(Fonts.SF_MEDIUM, d.dur, this.x + 4.5F + ICON_SIZE + 3.5F, rowY + 2.5F, 8.5F, 0xFFFFFFFF);

                // Right Pill
                Render2D.drawHudPill(this.x + durW + GAP_X, rowY, nameW, ROW_H, 0.85F);
                Fonts.drawString(Fonts.SF_MEDIUM, d.name, this.x + durW + GAP_X + 5.5F, rowY + 2.5F, 8.5F, 0xFFFFFFFF);
                if (!d.lvl.isEmpty()) {
                    Fonts.drawString(Fonts.SF_MEDIUM, d.lvl, this.x + durW + GAP_X + 5.5F + nameTextW, rowY + 2.8F, 8.0F, accent);
                }

                float rowTotalW = durW + GAP_X + nameW;
                if (rowTotalW > maxPreviewW) maxPreviewW = rowTotalW;
                rowY += ROW_H + GAP_Y;
            }

            this.width = maxPreviewW;
            this.height = (rowY - this.y) - GAP_Y;
            return;
        }

        // Calculate size
        float maxRowW = headerW;
        float totalH = HEADER_H;

        for (MobEffectInstance inst : effects) {
            Animation anim = anims.get(inst.getEffect());
            float a = anim != null ? anim.getValue() : 1.0F;
            if (a <= 0.01F) continue;

            String durStr = formatDuration(inst.getDuration(), inst.isInfiniteDuration());
            String nameStr = inst.getEffect().value().getDisplayName().getString();
            int amp = inst.getAmplifier() + 1;
            String lvlStr = amp > 1 ? " LVL. " + amp : "";

            float durTextW = Fonts.SF_MEDIUM.getWidth(durStr, 8.5F);
            float durW = 4.5F + ICON_SIZE + 3.5F + durTextW + 4.5F;

            float nameTextW = Fonts.SF_MEDIUM.getWidth(nameStr, 8.5F);
            float lvlTextW = lvlStr.isEmpty() ? 0 : Fonts.SF_MEDIUM.getWidth(lvlStr, 8.0F);
            float nameW = 5.5F + nameTextW + lvlTextW + 5.5F;

            float rowW = durW + GAP_X + nameW;
            if (rowW > maxRowW) maxRowW = rowW;
            totalH += (ROW_H + GAP_Y) * a;
        }

        totalWidthAnim.setTarget(maxRowW);
        totalWidthAnim.update();
        totalHeightAnim.setTarget(totalH);
        totalHeightAnim.update();

        this.width = totalWidthAnim.getValue();
        this.height = totalHeightAnim.getValue();

        // 1. Draw Header Pill
        Render2D.drawHudPill(this.x, this.y, headerW, HEADER_H, 1.0F);
        Fonts.drawIcon(IconUse.POTION, this.x + 6.0F, this.y + 2.5F, 9.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Potions", this.x + 6.0F + headerIconW + 4.0F, this.y + 2.5F, 9.5F, 0xFFFFFFFF);

        // 2. Draw Active Effect Rows
        float currY = this.y + HEADER_H + GAP_Y;
        for (MobEffectInstance inst : effects) {
            Animation anim = anims.get(inst.getEffect());
            float a = anim != null ? anim.getValue() : 1.0F;
            if (a <= 0.01F) continue;

            String durStr = formatDuration(inst.getDuration(), inst.isInfiniteDuration());
            String nameStr = inst.getEffect().value().getDisplayName().getString();
            int amp = inst.getAmplifier() + 1;
            String lvlStr = amp > 1 ? " LVL. " + amp : "";

            float durTextW = Fonts.SF_MEDIUM.getWidth(durStr, 8.5F);
            float durW = 4.5F + ICON_SIZE + 3.5F + durTextW + 4.5F;

            float nameTextW = Fonts.SF_MEDIUM.getWidth(nameStr, 8.5F);
            float lvlTextW = lvlStr.isEmpty() ? 0 : Fonts.SF_MEDIUM.getWidth(lvlStr, 8.0F);
            float nameW = 5.5F + nameTextW + lvlTextW + 5.5F;

            int textAlpha = ColorUtil.rgba(255, 255, 255, (int) (255 * a));
            int accentAlpha = ColorUtil.withAlpha(accent, (int) (255 * a));

            // Left Pill: [ (Logo) MM:SS ]
            Render2D.drawHudPill(this.x, currY, durW, ROW_H, a);

            // Effect Logo / Sprite
            Identifier sprite = Hud.getMobEffectSprite(inst.getEffect());
            if (extractor != null && sprite != null) {
                Render2DUtil.flush();
                float iconX = this.x + 4.5F;
                float iconY = currY + (ROW_H - ICON_SIZE) / 2.0F;
                try {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE), textAlpha);
                } catch (Throwable t) {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int) iconX, (int) iconY, (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE));
                }
            }

            Fonts.drawString(Fonts.SF_MEDIUM, durStr, this.x + 4.5F + ICON_SIZE + 3.5F, currY + 2.5F, 8.5F, textAlpha);

            // Right Pill: [ Name LVL. X ]
            Render2D.drawHudPill(this.x + durW + GAP_X, currY, nameW, ROW_H, a);
            Fonts.drawString(Fonts.SF_MEDIUM, nameStr, this.x + durW + GAP_X + 5.5F, currY + 2.5F, 8.5F, textAlpha);
            if (!lvlStr.isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, lvlStr, this.x + durW + GAP_X + 5.5F + nameTextW, currY + 2.8F, 8.0F, accentAlpha);
            }

            currY += (ROW_H + GAP_Y) * a;
        }
    }
}
