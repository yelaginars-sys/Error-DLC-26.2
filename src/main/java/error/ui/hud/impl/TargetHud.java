package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.module.impl.combat.AuraModule;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class TargetHud extends HudElement implements IMinecraft {

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
        int color;

        HitParticle(float x, float y, float vx, float vy, float life, float size, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.color = color;
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
        super("targethud", "Target HUD", 140.0F, 120.0F, 130.0F, 34.0F);
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
        if (currentTarget != null) target = currentTarget;

        if (target == null) {
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
            hitFlash = Mth.lerp(0.25F, hitFlash, hitFlashTarget);
            if (hitFlash >= 0.95F && !holding) {
                hitFlash = 1.0F;
                hitFlashTarget = 0.0F;
            }
        } else {
            hitFlash = Mth.lerp(holding ? 1.0F : 0.025F, hitFlash, 0.0F);
            if (hitFlash < 0.01F) {
                hitFlash = 0.0F;
                hitFlashHoldUntil = 0L;
            }
        }

        healthAnimation = Mth.clamp(Mth.lerp(0.1F, healthAnimation, currentHp / maxHp), 0.0F, 1.0F);
        float absorptionPercent = absorptionHp > 0 ? Math.min(absorptionHp / maxHp, 1.0F) : 0.0F;
        absorptionAnimation = Mth.clamp(Mth.lerp(0.08F, absorptionAnimation, absorptionPercent), 0.0F, 1.0F);

        float drawX = getX();
        float drawY = getY();

        float headSize = 26.0F;
        float padding = 4.0F;
        float gap = 4.0F;
        float barH = 5.0F;
        float radius = 6.0F;
        float width = 135.0F;
        float height = headSize + padding * 2.0F;

        this.width = width;
        this.height = height;

        float headX = drawX + padding;
        float headY = drawY + (height - headSize) / 2.0F;

        int primaryColor = Theme.getAccentColor();

        // Hit particles burst
        if (gotHit) {
            int count = 14;
            for (int i = 0; i < count; i++) {
                float angle = (float) (i * (Math.PI * 2 / count) + random.nextFloat() * 0.3F);
                float rad = headSize / 2.0F;
                float cx = headX + headSize / 2.0F + (float) (Math.cos(angle) * rad);
                float cy = headY + headSize / 2.0F + (float) (Math.sin(angle) * rad);
                float speed = 0.3F + random.nextFloat() * 0.5F;
                float vx = (float) (Math.cos(angle) * speed);
                float vy = (float) (Math.sin(angle) * speed);
                float size = 2.0F + random.nextFloat() * 2.0F;
                float life = 20.0F + random.nextFloat() * 15.0F;
                particles.add(new HitParticle(cx, cy, vx, vy, life, size, primaryColor));
            }
        }

        Iterator<HitParticle> iter = particles.iterator();
        while (iter.hasNext()) {
            HitParticle p = iter.next();
            if (p.update()) {
                iter.remove();
            }
        }

        // Render Background (Glow + Border + Blur Container)
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 30));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 60));
        int bgColor = ColorUtil.applyAlpha(ColorUtil.rgba(14, 14, 18, 200), alpha);

        Render2D.drawShadow(drawX, drawY, width, height, radius, 6.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Player Head with hit flash
        if (target instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawHead(clientPlayer, headX, headY, headSize, 4.0F, alpha);
        } else {
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 4.0F, ColorUtil.rgba(30, 30, 30, (int) (200 * alpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "?", headX + headSize / 2.0F - 3.0F, headY + headSize / 2.0F - 5.0F, 12.0F, ColorUtil.applyAlpha(-1, alpha));
        }

        if (hitFlash > 0.01F) {
            int redFlash = ColorUtil.rgba(255, 0, 0, (int) (hitFlash * 140 * alpha));
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 4.0F, redFlash);
        }

        // Text labels: Name & Health
        float textX = headX + headSize + gap;
        String nameStr = target.getScoreboardName();
        float nameY = drawY + padding + 1.0F;
        float hpY = nameY + 9.0F;

        Fonts.drawString(Fonts.SF_MEDIUM, nameStr, textX, nameY, 7.0F, ColorUtil.applyAlpha(-1, alpha));

        String healthLabel = "Health: ";
        String hpNum = String.format("%.1f", currentHp).replace(".", ",");
        float labelW = Fonts.SF_MEDIUM.getWidth(healthLabel, 6.0F);

        Fonts.drawString(Fonts.SF_MEDIUM, healthLabel, textX, hpY, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, hpNum, textX + labelW, hpY, 6.0F, ColorUtil.applyAlpha(-1, alpha));

        // Health Bar
        float barX = headX + headSize + gap;
        float barY = drawY + height - padding - barH;
        float barW = width - (headX + headSize + gap - drawX) - padding;

        int barTrackColor = ColorUtil.rgba(255, 255, 255, (int) (20 * alpha));
        Render2D.drawRoundedRect(barX, barY, barW, barH, 2.0F, barTrackColor);

        if (healthAnimation > 0) {
            float filledW = barW * healthAnimation;
            if (filledW > 1.0F) {
                Render2D.drawRoundedRect(barX, barY, filledW, barH, 2.0F, ColorUtil.applyAlpha(primaryColor, alpha));
            }
        }

        if (absorptionHp > 0 || absorptionAnimation > 0.01F) {
            int goldColor = ColorUtil.rgba(255, 215, 0, (int) (220 * alpha));
            Render2D.drawRoundedRect(barX, barY, barW * absorptionAnimation, barH, 2.0F, goldColor);
        }

        // Render particles
        for (HitParticle p : particles) {
            int pColor = ColorUtil.applyAlpha(p.color, p.alpha() * alpha);
            Render2D.drawRoundedRect(p.x - p.size / 2.0F, p.y - p.size / 2.0F, p.size, p.size, p.size / 2.0F, pColor);
        }
    }
}