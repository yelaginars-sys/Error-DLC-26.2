package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.combat.AuraModule;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TargetHud extends HudElement implements error.IMinecraft {

    private static class Particle {
        float x, y, vx, vy, size, age, maxAge;
        int color;
    }

    private LivingEntity target;
    private final Animation openAnim = new Animation(0.0F, 0.20F);
    private final Animation widthAnim = new Animation(115.0F, 0.22F);
    private final Animation healthAnim = new Animation(20.0F, 0.25F);
    private final Animation healthTrailAnim = new Animation(20.0F, 0.35F);
    private final Animation absAnim = new Animation(0.0F, 0.25F);
    private final Animation damageBounceAnim = new Animation(0.0F, 0.16F);

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> ambientParticles = new ArrayList<>();
    private int lastHurtTime = 0;
    private float lastHp = -1.0F;

    private static final float CARD_H = 34.0F;
    private static final float HEAD_SIZE = 20.0F;
    private static final float HEAD_RADIUS = 4.5F;
    private static final float RING_SIZE = 23.0F;
    private static final float RING_THICKNESS = 2.6F;
    private static final float ITEM_SCALE = 0.5625F; // 9 / 16
    private static final float ITEM_BOX = 9.5F;

    public TargetHud() {
        super("target_hud", "Target HUD", 100.0F, 150.0F, 125.0F, CARD_H, true);
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

    private List<ItemStack> getEquipList(LivingEntity entity, boolean isChat) {
        List<ItemStack> list = new ArrayList<>();
        if (entity == null) return list;

        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        boolean allArmorEmpty = true;
        for (EquipmentSlot slot : armorSlots) {
            ItemStack s = entity.getItemBySlot(slot);
            if (!s.isEmpty()) {
                allArmorEmpty = false;
                break;
            }
        }

        if (allArmorEmpty && isChat) {
            list.add(new ItemStack(Items.GOLDEN_HELMET));
            list.add(new ItemStack(Items.GOLDEN_CHESTPLATE));
            list.add(new ItemStack(Items.GOLDEN_LEGGINGS));
            list.add(new ItemStack(Items.GOLDEN_BOOTS));
            list.add(new ItemStack(Items.GOLDEN_SWORD));
            return list;
        }

        for (EquipmentSlot slot : armorSlots) {
            list.add(entity.getItemBySlot(slot));
        }
        list.add(entity.getMainHandItem());
        ItemStack off = entity.getOffhandItem();
        if (!off.isEmpty()) {
            list.add(off);
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean isChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        updateTarget(isChat);

        boolean visible = target != null || isChat;
        openAnim.setTarget(visible ? 1.0F : 0.0F);
        openAnim.update();
        float alpha = openAnim.getValue();

        if (alpha <= 0.01F || target == null) {
            particles.clear();
            ambientParticles.clear();
            return;
        }

        Interface iface = Interface.getInstance();
        boolean allowParticles = iface == null || iface.targetHudParticles.getValue();

        int accent = Theme.getAccentColor();

        // 1. Health Calculation & Animations
        float maxHp = Math.max(1.0F, target.getMaxHealth());
        float curHp = target.getHealth();
        float absHp = target.getAbsorptionAmount();
        float totalHp = curHp + absHp;

        if (lastHp >= 0.0F && totalHp < lastHp - 0.05F) {
            damageBounceAnim.setValue(1.0F);
        }
        lastHp = totalHp;
        damageBounceAnim.setTarget(0.0F);
        damageBounceAnim.update();

        healthAnim.setTarget(curHp);
        healthAnim.update();
        healthTrailAnim.setTarget(healthAnim.getValue());
        healthTrailAnim.update();
        absAnim.setTarget(absHp);
        absAnim.update();

        float hpProg = Math.max(0.0F, Math.min(1.0F, healthAnim.getValue() / maxHp));
        float trailProg = Math.max(0.0F, Math.min(1.0F, healthTrailAnim.getValue() / maxHp));
        float absProg = Math.max(0.0F, Math.min(1.0F, absAnim.getValue() / maxHp));

        // 2. Name & Equipment layout
        String rawName = target.getName().getString();
        String displayName = rawName.length() > 12 ? rawName.substring(0, 12) + "..." : rawName;
        String hpText = String.valueOf(Math.round(totalHp));

        List<ItemStack> equip = getEquipList(target, isChat);
        int equipCount = equip.size();
        float equipW = equipCount > 0 ? (equipCount * ITEM_BOX + (equipCount - 1) * 2.5F) : 0.0F;

        float nameW = Fonts.SF_MEDIUM.getWidth(displayName, 9.5F);
        float middleW = Math.max(nameW, equipW);

        // Layout Width: 5(pad) + 19(head) + 6(gap) + middleW + 8(gap) + 18(ring) + 5(pad)
        float targetWidth = 5.0F + HEAD_SIZE + 6.0F + middleW + 8.0F + RING_SIZE + 5.0F;
        targetWidth = Math.max(110.0F, targetWidth);

        widthAnim.setTarget(targetWidth);
        widthAnim.update();

        this.width = widthAnim.getValue();
        this.height = CARD_H;

        // 3. Ambient floating motes around card and circle
        if (allowParticles) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            if (ambientParticles.size() < 12 && rnd.nextInt(3) == 0) {
                Particle p = new Particle();
                p.x = this.x + rnd.nextFloat() * this.width;
                p.y = this.y + rnd.nextFloat() * CARD_H;
                p.vx = (rnd.nextFloat() - 0.5F) * 0.4F;
                p.vy = -0.3F - rnd.nextFloat() * 0.4F;
                p.size = 1.2F + rnd.nextFloat() * 1.6F;
                p.age = 0.0F;
                p.maxAge = 40.0F + rnd.nextFloat() * 30.0F;
                p.color = accent;
                ambientParticles.add(p);
            }

            Iterator<Particle> ambIt = ambientParticles.iterator();
            while (ambIt.hasNext()) {
                Particle p = ambIt.next();
                p.age += 1.0F;
                if (p.age >= p.maxAge) {
                    ambIt.remove();
                    continue;
                }
                p.x += p.vx;
                p.y += p.vy;
                float pAlpha = (float) Math.sin((p.age / p.maxAge) * Math.PI) * alpha;
                Render2D.drawCircle(p.x, p.y, p.size * 0.5F, ColorUtil.withAlpha(p.color, (int) (180 * pAlpha)));
            }
        }

        // 4. Draw Liquid Glass Capsule Card
        Render2D.drawLiquidGlass(this.x, this.y, this.width, CARD_H, 8.0F, alpha, accent);

        // 5. Head Avatar
        float headX = this.x + 5.0F;
        float headY = this.y + (CARD_H - HEAD_SIZE) * 0.5F;

        // Hurt Particles
        if (allowParticles && target.hurtTime > 0 && target.hurtTime != lastHurtTime) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < 9; i++) {
                Particle p = new Particle();
                p.x = headX + HEAD_SIZE * 0.5F;
                p.y = headY + HEAD_SIZE * 0.5F;
                float angle = (float) (rnd.nextDouble() * Math.PI * 2.0);
                float speed = 0.8F + rnd.nextFloat() * 1.5F;
                p.vx = (float) Math.cos(angle) * speed;
                p.vy = (float) Math.sin(angle) * speed;
                p.size = 2.0F + rnd.nextFloat() * 2.0F;
                p.age = 0.0F;
                p.maxAge = 24.0F + rnd.nextFloat() * 18.0F;
                p.color = accent;
                particles.add(p);
            }
        }
        lastHurtTime = target.hurtTime;

        if (allowParticles) {
            Iterator<Particle> pIt = particles.iterator();
            while (pIt.hasNext()) {
                Particle p = pIt.next();
                p.age += 1.0F;
                if (p.age >= p.maxAge) {
                    pIt.remove();
                    continue;
                }
                p.x += p.vx;
                p.y += p.vy;
                p.vx *= 0.94F;
                p.vy *= 0.94F;
                float pAlpha = (1.0F - p.age / p.maxAge) * alpha;
                Render2D.drawCircle(p.x, p.y, p.size * 0.5F, ColorUtil.withAlpha(p.color, (int) (230 * pAlpha)));
            }
        }

        if (target instanceof AbstractClientPlayer ap) {
            Render2D.drawHead(ap, headX, headY, HEAD_SIZE, HEAD_RADIUS, alpha);
        } else {
            Render2D.drawCustomAvatar(headX, headY, HEAD_SIZE, HEAD_RADIUS, alpha);
        }

        // 6. Name Text
        float textX = headX + HEAD_SIZE + 6.0F;
        float textY = this.y + 4.5F;
        Fonts.drawString(Fonts.SF_MEDIUM, displayName, textX, textY, 9.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        // 7. Equipment Row
        float equipY = this.y + 17.0F;
        var extractor = event.getGuiGraphicsExtractor();

        for (int i = 0; i < equipCount; i++) {
            float slotX = textX + i * (ITEM_BOX + 2.5F);
            ItemStack stack = equip.get(i);

            if (stack.isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, "×", slotX + 1.5F, equipY - 1.0F, 9.0F, ColorUtil.rgba(140, 140, 140, (int) (120 * alpha)));
            } else if (extractor != null) {
                Render2DUtil.flush();
                try {
                    var pose = extractor.pose();
                    pose.pushMatrix();
                    pose.translate(slotX, equipY);
                    pose.scale(ITEM_SCALE, ITEM_SCALE);
                    extractor.item(stack, 0, 0);
                    pose.popMatrix();
                } catch (Throwable ignored) {}
            }
        }

        // 8. Circular Health Indicator (Right Side)
        float ringX = this.x + this.width - 5.0F - RING_SIZE;
        float ringY = this.y + (CARD_H - RING_SIZE) * 0.5F;
        float ringCX = ringX + RING_SIZE * 0.5F;
        float ringCY = ringY + RING_SIZE * 0.5F;
        float ringR = RING_SIZE * 0.5F;

        // Dark glass background disc inside circle
        Render2D.drawCircle(ringCX, ringCY, ringR, ColorUtil.rgba(14, 17, 24, (int) (140 * alpha)));

        // Soft circular outer track
        Render2D.drawArc(ringCX, ringCY, ringR, RING_THICKNESS, 0.0F, 360.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));

        // Damage lagging trail arc
        if (trailProg > hpProg + 0.005F) {
            Render2D.drawArc(ringCX, ringCY, ringR, RING_THICKNESS, -90.0F, 360.0F * trailProg, ColorUtil.withAlpha(accent, (int) (95 * alpha)));
        }

        // Active health arc
        if (hpProg > 0.005F) {
            Render2D.drawArc(ringCX, ringCY, ringR, RING_THICKNESS, -90.0F, 360.0F * hpProg, ColorUtil.withAlpha(accent, (int) (255 * alpha)));

            // Glowing tip spark ember
            float endAngleRad = (float) Math.toRadians(-90.0F + 360.0F * hpProg);
            float tipX = ringCX + (float) Math.cos(endAngleRad) * ringR;
            float tipY = ringCY + (float) Math.sin(endAngleRad) * ringR;
            Render2D.drawCircle(tipX, tipY, 3.0F, ColorUtil.withAlpha(accent, (int) (130 * alpha)));
            Render2D.drawCircle(tipX, tipY, 1.8F, ColorUtil.withAlpha(0xFFFFFFFF, (int) (250 * alpha)));

            // Sparkle particle trail from tip
            if (allowParticles && ThreadLocalRandom.current().nextInt(4) == 0) {
                Particle tipP = new Particle();
                tipP.x = tipX;
                tipP.y = tipY;
                float tipAngle = (float) (ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0);
                tipP.vx = (float) Math.cos(tipAngle) * 0.5F;
                tipP.vy = (float) Math.sin(tipAngle) * 0.5F;
                tipP.size = 1.0F + ThreadLocalRandom.current().nextFloat() * 1.2F;
                tipP.age = 0.0F;
                tipP.maxAge = 16.0F;
                tipP.color = accent;
                particles.add(tipP);
            }
        }

        // Absorption golden arc
        if (absProg > 0.005F) {
            Render2D.drawArc(ringCX, ringCY, ringR, RING_THICKNESS, -90.0F, 360.0F * absProg, ColorUtil.rgba(240, 190, 45, (int) (255 * alpha)));
        }

        // Centered HP text inside ring with damage bounce
        float bounce = 1.0F + 0.22F * damageBounceAnim.getValue();
        float hpFontSz = 8.5F * bounce;
        float hpW = Fonts.SF_MEDIUM.getWidth(hpText, hpFontSz);
        float hpX = ringCX - hpW * 0.5F;
        float hpY = ringCY - 4.0F * bounce;
        Fonts.drawString(Fonts.SF_MEDIUM, hpText, hpX, hpY, hpFontSz, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }
}
