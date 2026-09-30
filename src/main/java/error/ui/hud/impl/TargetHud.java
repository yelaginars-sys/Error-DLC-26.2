package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.impl.combat.AuraModule;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.MathUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class TargetHud extends HudElement implements IMinecraft {

    private float healthAnimation = 0.0F;
    private float absorptionAnimation = 0.0F;
    private LivingEntity target = null;
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
            return life <= 0.0F;
        }

        float alpha() {
            return life / maxLife;
        }
    }

    public TargetHud() {
        super("target", "Target HUD", 30.0F, 30.0F, 94.0F, 32.0F);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    @Override
    public void draw(Render2DEvent event) {
        LivingEntity currentTarget = getTarget();
        if (currentTarget != null) target = currentTarget;

        boolean visible = target != null;
        fadeAnim.setTarget(visible ? 1.0F : 0.0F);
        fadeAnim.update();

        float anim = fadeAnim.getValue();
        if (anim <= 0.01F || target == null) return;

        int fa = (int) (anim * 255.0F);
        int themeAccent = Theme.getAccentColor();

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
            hitFlash = MathUtil.lerp(hitFlash, hitFlashTarget, 0.25F);
            if (hitFlash >= 0.95F && !holding) {
                hitFlash = 1.0F;
                hitFlashTarget = 0.0F;
            }
        } else {
            hitFlash = MathUtil.lerp(hitFlash, 0.0F, holding ? 1.0F : 0.025F);
            if (hitFlash < 0.01F) {
                hitFlash = 0.0F;
                hitFlashHoldUntil = 0L;
            }
        }

        healthAnimation = Mth.clamp(MathUtil.lerp(healthAnimation, currentHp / Math.max(1.0F, maxHp), 0.1F), 0.0F, 1.0F);
        float absorptionPercent = absorptionHp > 0 ? Math.min(absorptionHp / Math.max(1.0F, maxHp), 1.0F) : 0.0F;
        absorptionAnimation = Mth.clamp(MathUtil.lerp(absorptionAnimation, absorptionPercent, 0.08F), 0.0F, 1.0F);

        float x = getX();
        float y = getY();

        float headSize = 26.0F;
        float padding = 3.0F;
        float gap = 3.0F;
        float barH = 4.5F;
        float barRound = 1.0F;
        float round = 7.0F;
        float width = 94.0F;
        float height = headSize + padding * 2.0F;

        this.width = width;
        this.height = height;

        float headX = x + padding;
        float headY = y + (height - headSize) / 2.0F;

        if (gotHit) {
            int count = 14;
            for (int i = 0; i < count; i++) {
                float angle = (float) (i * (Math.PI * 2.0 / count) + random.nextFloat() * 0.3F);
                float radius = headSize / 2.0F;
                float cx = headX + headSize / 2.0F + (float) (Math.cos(angle) * radius);
                float cy = headY + headSize / 2.0F + (float) (Math.sin(angle) * radius);
                float speed = 0.3F + random.nextFloat() * 0.5F;
                float vx = (float) (Math.cos(angle) * speed);
                float vy = (float) (Math.sin(angle) * speed);
                float size = 2.0F + random.nextFloat() * 2.0F;
                float life = 20.0F + random.nextFloat() * 15.0F;
                particles.add(new HitParticle(cx, cy, vx, vy, life, size, themeAccent));
            }
        }

        Iterator<HitParticle> iter = particles.iterator();
        while (iter.hasNext()) {
            HitParticle p = iter.next();
            if (p.update()) {
                iter.remove();
            }
        }

        // Exact Waper Background layers: Subtle accent glow -> subtle accent stroke -> blur shadow -> dark container
        Render2D.drawRoundedRect(x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, round + 2.0F, ColorUtil.withAlpha(themeAccent, (int) (anim * 15)));
        Render2D.drawRoundedRect(x - 0.5F, y - 0.5F, width + 1.0F, height + 1.0F, round + 0.5F, ColorUtil.withAlpha(themeAccent, (int) (anim * 40)));
        Render2D.drawShadow(x, y, width, height, round, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * anim)));
        Render2D.drawRoundedRect(x, y, width, height, round, ColorUtil.rgba(0, 0, 0, (int) (160 * anim)));

        // Head rendering
        if (target instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawHead(clientPlayer, headX, headY, headSize, 6.0F, anim);
        } else {
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 6.0F, ColorUtil.rgba(30, 30, 30, (int) (200 * anim)));
            Fonts.drawString(Fonts.SF_MEDIUM, "?", headX + headSize / 2.0F - 4.0F, headY + headSize / 2.0F - 6.0F, 14.0F, ColorUtil.rgba(255, 255, 255, fa));
        }

        // Hit flash overlay
        if (hitFlash > 0.01F) {
            int redAlpha = (int) (hitFlash * 127.0F * anim);
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 6.0F, ColorUtil.rgba(255, 0, 0, redAlpha));
        }

        float textX = headX + headSize + gap;
        String nameStr = target.getName().getString();
        float nameSize = 6.5F;
        float hpLabelSize = 5.5F;
        float hpNumSize = 6.0F;

        float nameY = y + padding + 2.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, nameStr, textX, nameY, nameSize, ColorUtil.rgba(255, 255, 255, fa));

        float healthY = nameY + nameSize + 2.5F;
        int themeColor = ColorUtil.withAlpha(themeAccent, fa);
        String healthLabel = "Health:";
        float labelW = Fonts.SF_MEDIUM.getWidth(healthLabel, hpLabelSize);
        Fonts.drawString(Fonts.SF_MEDIUM, healthLabel, textX, healthY, hpLabelSize, themeColor);

        String hpNum = String.format("%.1f", Math.max(0.0F, currentHp)).replace(".", ",");
        Fonts.drawString(Fonts.SF_MEDIUM, hpNum, textX + labelW + 1.0F, healthY - 0.3F, hpNumSize, ColorUtil.rgba(255, 255, 255, fa));

        float barX = headX + headSize + 4.0F;
        float barY = y + height - padding - barH - 1.5F;
        float barW = width - (headX + headSize + 4.0F - x) - padding - 3.0F;

        int barBg = ColorUtil.rgba(
                (int) (((themeAccent >> 16) & 0xFF) * 0.2F),
                (int) (((themeAccent >> 8) & 0xFF) * 0.2F),
                (int) ((themeAccent & 0xFF) * 0.2F),
                (int) (180 * anim)
        );
        Render2D.drawRoundedRect(barX, barY, barW, barH, barRound, barBg);

        if (healthAnimation > 0.001F) {
            float filledW = barW * healthAnimation;
            if (filledW > 1.0F) {
                Render2D.drawRoundedRect(barX, barY, filledW, barH, barRound, themeColor);
            }
        }

        if (absorptionHp > 0 || absorptionAnimation > 0.01F) {
            int goldColor = ColorUtil.rgba(255, 215, 0, fa);
            Render2D.drawRoundedRect(barX, barY, barW * absorptionAnimation, barH, barRound, goldColor);
        }

        // Particle rendering
        for (HitParticle p : particles) {
            int pAlpha = (int) (p.alpha() * fa);
            int pColor = ColorUtil.withAlpha(p.color, pAlpha);
            Render2D.drawRoundedRect(p.x - p.size / 2.0F, p.y - p.size / 2.0F, p.size, p.size, p.size / 2.0F, pColor);
        }
    }

    private LivingEntity getTarget() {
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            return AuraModule.INSTANCE.getTarget();
        }
        if (mc.gui != null && mc.gui.screen() instanceof ChatScreen) return mc.player;
        return null;
    }
}