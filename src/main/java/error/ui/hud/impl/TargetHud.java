package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.combat.AuraModule;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class TargetHud extends HudElement implements error.IMinecraft {

    private LivingEntity target;
    private final Animation openAnim = new Animation(0.0F, 0.22F);
    private final Animation healthAnim = new Animation(20.0F, 0.25F);
    private final Animation absAnim = new Animation(0.0F, 0.25F);

    private static final float CARD_W = 125.0F;
    private static final float CARD_H = 43.0F;
    private static final float CARD_R = 8.0F;
    private static final float AVATAR_SIZE = 24.0F;
    private static final float AVATAR_R = 5.0F;

    private static final Identifier STEVE_SKIN = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public TargetHud() {
        super("target_hud", "Target HUD", 100.0F, 150.0F, CARD_W, CARD_H, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.targetHud.getValue())) {
            return false;
        }
        if (mc.player == null || mc.level == null) return false;

        boolean isChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        updateTarget(isChat);
        return target != null || isChat;
    }

    private void updateTarget(boolean isChat) {
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            this.target = AuraModule.INSTANCE.getTarget();
        } else if (mc.crosshairPickEntity instanceof LivingEntity living && living != mc.player && living.isAlive()) {
            this.target = living;
        } else if (isChat) {
            this.target = mc.player;
        } else {
            this.target = null;
        }
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean isChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        updateTarget(isChat);

        boolean hasTarget = this.target != null;
        openAnim.setTarget(hasTarget ? 1.0F : 0.0F);
        openAnim.update();

        float a = openAnim.getValue();
        if (a <= 0.01F || this.target == null) return;

        this.width = CARD_W;
        this.height = CARD_H;

        float hp = this.target.getHealth();
        float maxHp = Math.max(1.0F, this.target.getMaxHealth());
        float abs = this.target.getAbsorptionAmount();

        healthAnim.setTarget(hp);
        healthAnim.update();

        absAnim.setTarget(abs);
        absAnim.update();

        float smoothHp = healthAnim.getValue();
        float smoothAbs = absAnim.getValue();

        float curX = this.x;
        float curY = this.y;
        int accent = Theme.getAccentColor();

        // 1. Main Background Card in Liquid Glass (matching Energy HUD & ClickGUI)
        Render2D.drawShadow(curX, curY, CARD_W, CARD_H, CARD_R, 7.0F, ColorUtil.rgba(0, 0, 0, (int) (75 * a)));
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Blur.of(curX, curY, CARD_W, CARD_H)
                    .radius(Math.round(CARD_R))
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(14, 16, 22, (int) (115 * a)))
                    .alpha(a)
                    .render(extractor);

            Outline.of(curX, curY, CARD_W, CARD_H)
                    .radius(Math.round(CARD_R))
                    .thickness(0.85F)
                    .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 32))
                    .alpha(a)
                    .render(extractor);
        }

        // 2. Avatar
        Identifier skin = STEVE_SKIN;
        if (this.target instanceof AbstractClientPlayer clientPlayer) {
            try {
                skin = clientPlayer.getSkin().body().texturePath();
            } catch (Throwable ignored) {}
        }

        float avatarX = curX + 6.0F;
        float avatarY = curY + 6.0F;
        Render2D.drawRoundedRect(avatarX - 0.5F, avatarY - 0.5F, AVATAR_SIZE + 1.0F, AVATAR_SIZE + 1.0F, AVATAR_R, ColorUtil.rgba(255, 255, 255, (int) (25 * a)));
        Render2D.drawHead(skin, avatarX, avatarY, AVATAR_SIZE, AVATAR_R, a);
        Render2D.drawRoundedOutline(avatarX - 0.5F, avatarY - 0.5F, AVATAR_SIZE + 1.0F, AVATAR_SIZE + 1.0F, AVATAR_R, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (40 * a)));

        // 3. Name & HP Info
        String name = this.target.getName().getString();
        float maxNameW = CARD_W - 42.0F;
        if (Fonts.SF_MEDIUM.getWidth(name, 9.0F) > maxNameW) {
            while (name.length() > 3 && Fonts.SF_MEDIUM.getWidth(name + "...", 9.0F) > maxNameW) {
                name = name.substring(0, name.length() - 1);
            }
            name += "...";
        }

        Fonts.drawString(Fonts.SF_MEDIUM, name, curX + 35.0F, curY + 6.5F, 9.0F, ColorUtil.rgba(255, 255, 255, (int) (250 * a)));

        String hpText = String.format("HP: %.1f", Math.max(0.0F, smoothHp));
        if (smoothAbs > 0.05F) {
            hpText += String.format(" (%.1f)", smoothAbs);
        }
        Fonts.drawString(Fonts.SF_MEDIUM, hpText, curX + 35.0F, curY + 17.5F, 8.0F, ColorUtil.rgba(200, 215, 230, (int) (230 * a)));

        // 4. Health Bar & Absorption Bar
        float barX = curX + 6.0F;
        float barY = curY + 34.0F;
        float barW = CARD_W - 12.0F;
        float barH = 4.0F;
        float barR = 2.0F;

        // Track
        Render2D.drawRoundedRect(barX, barY, barW, barH, barR, ColorUtil.rgba(255, 255, 255, (int) (18 * a)));

        // Health Fill
        float hpRatio = Mth.clamp(smoothHp / maxHp, 0.0F, 1.0F);
        float fillW = barW * hpRatio;
        if (fillW > 1.0F) {
            int hpColor = ColorUtil.withAlpha(accent, (int) (240 * a));
            Render2D.drawRoundedRect(barX, barY, fillW, barH, barR, hpColor);
        }

        // Absorption Fill
        if (smoothAbs > 0.05F) {
            float absRatio = Mth.clamp(smoothAbs / maxHp, 0.0F, 1.0F);
            float absW = barW * absRatio;
            if (absW > 1.0F) {
                int absCol = ColorUtil.rgba(250, 199, 32, (int) (220 * a));
                Render2D.drawRoundedRect(barX, barY, Math.min(barW, absW), barH, barR, absCol);
            }
        }
    }
}
