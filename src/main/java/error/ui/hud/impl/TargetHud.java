package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.module.impl.combat.AuraModule;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class TargetHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);

    private float healthAnimation = 0.0F;
    private float absorptionAnimation = 0.0F;
    private LivingEntity target;
    private float lastHealth = -1.0F;
    private float hitFlash = 0.0F;
    private float hitFlashTarget = 0.0F;
    private long hitFlashHoldUntil = 0L;

    private final List<HitParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private static class HitParticle {
        float x, y;
        float vx, vy;
        float life;
        float maxLife;
        float size;

        HitParticle(float x, float y, float vx, float vy, float life, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
        }

        boolean update() {
            x += vx;
            y += vy;
            vy += 0.02F;
            vx *= 0.97F;
            vy *= 0.97F;
            life -= 1.0F;
            return life <= 0;
        }

        float alpha() {
            return life / maxLife;
        }
    }

    public TargetHud() {
        super("targethud", "Target HUD", 140.0F, 120.0F, 94.0F, 32.0F);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    private LivingEntity getTarget() {
        AuraModule aura = AuraModule.INSTANCE;
        if (aura != null && aura.isEnabled() && aura.getTarget() != null) return aura.getTarget();
        if (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen) return mc.player;
        return null;
    }

    @Override
    public void draw(Render2DEvent event) {
        LivingEntity currentTarget = getTarget();
        if (currentTarget != null) this.target = currentTarget;
        if (this.target == null) {
            fadeAnim.setTarget(0.0F);
            fadeAnim.update();
            return;
        }

        fadeAnim.setTarget(1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float currentHp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float absorptionHp = target.getAbsorptionAmount();

        boolean gotHit = lastHealth > 0 && currentHp < lastHealth;
        if (gotHit) {
            hitFlashTarget = 1.0F;
            hitFlashHoldUntil = System.currentTimeMillis() + 900L;
        }
        lastHealth = currentHp;

        boolean holding = hitFlashHoldUntil > 0 && System.currentTimeMillis() < hitFlashHoldUntil;
        if (hitFlashTarget > 0) {
            hitFlash = hitFlash + (hitFlashTarget - hitFlash) * 0.25F;
            if (hitFlash >= 0.95F && !holding) {
                hitFlash = 1.0F;
                hitFlashTarget = 0.0F;
            }
        } else {
            hitFlash = hitFlash + (0.0F - hitFlash) * (holding ? 1.0F : 0.025F);
            if (hitFlash < 0.01F) {
                hitFlash = 0.0F;
                hitFlashHoldUntil = 0L;
            }
        }

        healthAnimation = Mth.clamp(healthAnimation + ((currentHp / Math.max(1.0F, maxHp)) - healthAnimation) * 0.1F, 0.0F, 1.0F);
        float absorptionPercent = absorptionHp > 0 ? Math.min(absorptionHp / Math.max(1.0F, maxHp), 1.0F) : 0.0F;
        absorptionAnimation = Mth.clamp(absorptionAnimation + (absorptionPercent - absorptionAnimation) * 0.08F, 0.0F, 1.0F);

        float drawX = getX();
        float drawY = getY();

        float headSize = 26.0F;
        float padding = 3.0F;
        float radius = 7.0F;
        float width = 94.0F;
        float height = headSize + padding * 2.0F;

        this.width = width;
        this.height = height;

        float headX = drawX + padding;
        float headY = drawY + (height - headSize) / 2.0F;

        // Radial particles on hit
        if (gotHit) {
            int count = 12;
            for (int i = 0; i < count; i++) {
                float angle = (float) (i * (Math.PI * 2 / count) + random.nextFloat() * 0.3F);
                float r = headSize / 2.0F;
                float cx = headX + headSize / 2.0F + (float) (Math.cos(angle) * r);
                float cy = headY + headSize / 2.0F + (float) (Math.sin(angle) * r);
                float speed = 0.3F + random.nextFloat() * 0.5F;
                float vx = (float) (Math.cos(angle) * speed);
                float vy = (float) (Math.sin(angle) * speed);
                float size = 2.0F + random.nextFloat() * 2.0F;
                float life = 20.0F + random.nextFloat() * 15.0F;
                particles.add(new HitParticle(cx, cy, vx, vy, life, size));
            }
        }

        Iterator<HitParticle> iter = particles.iterator();
        while (iter.hasNext()) {
            HitParticle p = iter.next();
            if (p.update()) {
                iter.remove();
            } else {
                int pCol = ColorUtil.applyAlpha(ACCENT_PURPLE, alpha * p.alpha());
                Render2D.drawRoundedRect(p.x, p.y, p.size, p.size, p.size / 2.0F, pCol);
            }
        }

        // Render Background (Waper Style: 15% outer accent -> 40% border outline -> dark container)
        int glowColor = ColorUtil.applyAlpha(ACCENT_PURPLE, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(ACCENT_PURPLE, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(14, 14, 18, (int) (160 * alpha));

        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Head render
        if (target instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawHead(clientPlayer, headX, headY, headSize, 6.0F, alpha);
        } else {
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 6.0F, ColorUtil.rgba(30, 30, 30, (int) (200 * alpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "?", headX + headSize / 2.0F - 3.0F, headY + headSize / 2.0F - 5.0F, 12.0F, ColorUtil.applyAlpha(-1, alpha));
        }

        // Hit flash overlay
        if (hitFlash > 0.01F) {
            int redFlash = ColorUtil.rgba(255, 0, 0, (int) (hitFlash * 127 * alpha));
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 6.0F, redFlash);
        }

        // Text labels
        float contentX = headX + headSize + 4.0F;
        String name = target.getScoreboardName();
        if (name.length() > 10) name = name.substring(0, 10) + "..";

        Fonts.drawString(Fonts.SF_MEDIUM, name, contentX, drawY + 4.5F, 6.5F, ColorUtil.applyAlpha(-1, alpha));

        // Health: 20,0
        String hpValueStr = String.format(java.util.Locale.US, "%.1f", currentHp + absorptionHp).replace('.', ',');
        Fonts.drawString(Fonts.SF_MEDIUM, "Health: ", contentX, drawY + 13.5F, 5.5F, ColorUtil.applyAlpha(ACCENT_PURPLE, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, hpValueStr, contentX + Fonts.SF_MEDIUM.getWidth("Health: ", 5.5F), drawY + 13.5F, 5.5F, ColorUtil.applyAlpha(-1, alpha));

        // Health Bar at bottom
        float barX = contentX;
        float barY = drawY + height - 7.0F;
        float barW = width - (contentX - drawX) - padding;
        float barH = 4.0F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, 1.5F, ColorUtil.rgba(25, 25, 32, (int) (180 * alpha)));

        float fillW = barW * healthAnimation;
        if (fillW > 0.5F) {
            int hpColor = ColorUtil.lerp(0xFFE1B800, ACCENT_PURPLE, healthAnimation);
            Render2D.drawRoundedRect(barX, barY, fillW, barH, 1.5F, ColorUtil.applyAlpha(hpColor, alpha));
        }

        if (absorptionAnimation > 0.01F) {
            float absW = barW * absorptionAnimation;
            int absColor = ColorUtil.rgba(240, 195, 40, (int) (220 * alpha));
            Render2D.drawRoundedRect(barX, barY, absW, barH, 1.5F, absColor);
        }
    }
}