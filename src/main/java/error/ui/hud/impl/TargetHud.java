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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class TargetHud extends HudElement implements error.IMinecraft {

    private LivingEntity target;
    private LivingEntity renderTarget;
    private long lastTargetTime = 0L;
    private final Animation openAnim = new Animation(0.0F, 0.22F);
    private final Animation healthAnim = new Animation(20.0F, 0.25F);
    private final Animation absAnim = new Animation(0.0F, 0.25F);

    private static final float CARD_W = 118.0F;
    private static final float CARD_H = 37.0F;
    private static final float CARD_R = 7.0F;
    private static final float AVATAR_SIZE = 22.0F;
    private static final float AVATAR_R = 4.5F;

    private static final Identifier STEVE_SKIN = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    private static final class HeadParticle {
        float x, y;
        float vx, vy;
        float size;
        float life;
        float maxLife;
        int color;

        HeadParticle(float x, float y, float vx, float vy, float size, float maxLife, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.maxLife = maxLife;
            this.life = maxLife;
            this.color = color;
        }

        boolean update() {
            x += vx;
            y += vy;
            vx *= 0.93F;
            vy *= 0.93F;
            life -= 0.045F;
            return life > 0;
        }
    }

    private final List<HeadParticle> headParticles = new ArrayList<>();
    private int lastTargetHurtTime = 0;

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
        return target != null || isChat || (renderTarget != null && openAnim.getValue() > 0.01F);
    }

    private void updateTarget(boolean isChat) {
        if (isChat) {
            this.target = mc.player;
            this.renderTarget = mc.player;
            this.lastTargetTime = System.currentTimeMillis();
            return;
        }

        LivingEntity current = null;
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            current = AuraModule.INSTANCE.getTarget();
        } else if (mc.crosshairPickEntity instanceof LivingEntity living && living != mc.player && living.isAlive()) {
            current = living;
        }

        long now = System.currentTimeMillis();
        if (current != null) {
            this.target = current;
            this.renderTarget = current;
            this.lastTargetTime = now;
        } else if (this.target != null) {
            // Retain target for 4000ms (4 seconds) after losing crosshair/aura
            if (now - this.lastTargetTime > 4000L || !this.target.isAlive() || this.target.isRemoved()) {
                this.target = null;
            }
        }
    }

    private List<ItemStack> getEquippedItems(LivingEntity entity, boolean isChat) {
        List<ItemStack> items = new ArrayList<>();
        if (entity == null) return items;

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        ItemStack mainHand = entity.getMainHandItem();
        if (!mainHand.isEmpty()) {
            items.add(mainHand);
        }

        ItemStack offHand = entity.getOffhandItem();
        if (!offHand.isEmpty()) {
            items.add(offHand);
        }

        if (items.isEmpty() && isChat) {
            ItemStack helm = new ItemStack(Items.NETHERITE_HELMET);
            ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
            chest.setDamageValue((int) (chest.getMaxDamage() * 0.25F));
            ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
            legs.setDamageValue((int) (legs.getMaxDamage() * 0.50F));
            ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
            ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
            ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);

            items.add(helm);
            items.add(chest);
            items.add(legs);
            items.add(boots);
            items.add(sword);
            items.add(totem);
        }

        return items;
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean isChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        updateTarget(isChat);

        boolean hasTarget = this.target != null;
        openAnim.setTarget(hasTarget ? 1.0F : 0.0F);
        openAnim.update();

        float a = openAnim.getValue();
        if (a <= 0.01F) {
            if (this.target == null) this.renderTarget = null;
            return;
        }

        LivingEntity entity = this.renderTarget;
        if (entity == null) return;

        this.width = CARD_W;
        this.height = CARD_H;

        float hp = entity.getHealth();
        float maxHp = Math.max(1.0F, entity.getMaxHealth());
        float abs = entity.getAbsorptionAmount();

        healthAnim.setTarget(hp);
        healthAnim.update();

        absAnim.setTarget(abs);
        absAnim.update();

        float smoothHp = healthAnim.getValue();
        float smoothAbs = absAnim.getValue();

        float curX = this.x;
        float curY = this.y;
        int accent = Theme.getAccentColor();

        // 1. Main Background Card
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        Render2D.drawHudCard(extractor, curX, curY, CARD_W, CARD_H, CARD_R, a, accent);

        // 2. Avatar on the left
        Identifier skin = STEVE_SKIN;
        if (entity instanceof AbstractClientPlayer clientPlayer) {
            try {
                skin = clientPlayer.getSkin().body().texturePath();
            } catch (Throwable ignored) {}
        }

        float avatarX = curX + 5.0F;
        float avatarY = curY + 5.0F;

        // Head particle spawning & simulation
        int currentHurt = entity.hurtTime;
        if (currentHurt > 0 && currentHurt > lastTargetHurtTime) {
            for (int i = 0; i < 5; i++) {
                if (headParticles.size() >= 40) break;
                float px = avatarX + (float) Math.random() * AVATAR_SIZE;
                float py = avatarY + (float) Math.random() * AVATAR_SIZE;
                float angle = (float) (Math.random() * Math.PI * 2.0);
                float spd = 0.4F + (float) Math.random() * 1.4F;
                float pvx = (float) Math.cos(angle) * spd;
                float pvy = (float) Math.sin(angle) * spd - 0.4F;
                int pColor = Math.random() < 0.65 ? ColorUtil.rgba(255, 45, 45, 240) : ColorUtil.withAlpha(accent, 240);
                headParticles.add(new HeadParticle(px, py, pvx, pvy, 1.4F + (float) Math.random() * 1.4F, 1.0F, pColor));
            }
        } else if (Math.random() < 0.22) {
            if (headParticles.size() < 25) {
                float px = avatarX + (float) Math.random() * AVATAR_SIZE;
                float py = avatarY + (float) Math.random() * (AVATAR_SIZE * 0.7F);
                float pvx = ((float) Math.random() - 0.5F) * 0.5F;
                float pvy = -0.25F - (float) Math.random() * 0.4F;
                int pColor = ColorUtil.withAlpha(accent, 200);
                headParticles.add(new HeadParticle(px, py, pvx, pvy, 1.1F + (float) Math.random() * 1.1F, 1.0F, pColor));
            }
        }
        lastTargetHurtTime = currentHurt;

        // Render head particles
        headParticles.removeIf(p -> {
            boolean alive = p.update();
            if (alive) {
                float progress = p.life / p.maxLife;
                int col = ColorUtil.withAlpha(p.color, (int) (ColorUtil.alpha(p.color) * progress * a));
                Render2D.drawRoundedRect(p.x, p.y, p.size, p.size, p.size / 2.0F, col);
            }
            return !alive;
        });

        Render2D.drawRoundedRect(avatarX - 0.5F, avatarY - 0.5F, AVATAR_SIZE + 1.0F, AVATAR_SIZE + 1.0F, AVATAR_R, ColorUtil.rgba(255, 255, 255, (int) (20 * a)));
        Render2D.drawHead(skin, avatarX, avatarY, AVATAR_SIZE, AVATAR_R, a);

        // Head red hurt flash on hit
        if (entity.hurtTime > 0) {
            float hurtProg = (float) entity.hurtTime / 10.0F;
            Render2D.drawRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, AVATAR_R,
                    ColorUtil.rgba(255, 30, 30, (int) (150 * hurtProg * a)));
        }

        Render2D.drawRoundedOutline(avatarX - 0.5F, avatarY - 0.5F, AVATAR_SIZE + 1.0F, AVATAR_SIZE + 1.0F, AVATAR_R, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (40 * a)));

        // 3. Top Row: Nickname on the left + HP in the right corner
        float startX = curX + 31.0F;
        float topY = curY + 4.5F;

        String hpText = String.format("%.1f HP", Math.max(0.0F, smoothHp));
        if (smoothAbs > 0.05F) {
            hpText += String.format(" (+%.1f)", smoothAbs);
        }

        float hpW = Fonts.SF_MEDIUM.getWidth(hpText, 7.5F);
        float rightX = curX + CARD_W - 6.0F;
        float hpX = rightX - hpW;

        String name = entity.getName().getString();
        float nameX = startX;
        float maxNameW = hpX - 4.0F - nameX;
        if (Fonts.SF_MEDIUM.getWidth(name, 7.5F) > maxNameW && maxNameW > 5.0F) {
            while (name.length() > 2 && Fonts.SF_MEDIUM.getWidth(name + "..", 7.5F) > maxNameW) {
                name = name.substring(0, name.length() - 1);
            }
            name += "..";
        }

        Fonts.drawString(Fonts.SF_MEDIUM, name, nameX, topY, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * a)));
        Fonts.drawString(Fonts.SF_MEDIUM, hpText, hpX, topY, 7.5F, ColorUtil.withAlpha(accent, (int) (255 * a)));

        // 4. Middle Row: Armor with durability bars + Hand items under HP & Nick
        float itemsY = curY + 15.0F;
        List<ItemStack> equippedItems = getEquippedItems(entity, isChat);

        if (extractor != null && !equippedItems.isEmpty()) {
            Render2DUtil.flush();
            float itemScale = 0.62F;
            float itemW = 10.0F;
            float itemGap = 3.0F;

            int maxDisplay = Math.min(6, (int) ((CARD_W - 36.0F) / (itemW + itemGap)));
            int displayCount = Math.min(maxDisplay, equippedItems.size());

            for (int i = 0; i < displayCount; i++) {
                ItemStack stack = equippedItems.get(i);
                float ix = startX + i * (itemW + itemGap);

                try {
                    var pose = extractor.pose();
                    pose.pushMatrix();
                    pose.translate(ix, itemsY);
                    pose.scale(itemScale, itemScale);
                    extractor.item(stack, 0, 0);
                    pose.popMatrix();
                } catch (Throwable ignored) {}

                // Durability bar underneath item
                if (stack.isDamaged() && stack.getMaxDamage() > 0) {
                    float durRatio = 1.0F - ((float) stack.getDamageValue() / (float) stack.getMaxDamage());
                    durRatio = Mth.clamp(durRatio, 0.0F, 1.0F);
                    float fillW = Math.max(1.0F, itemW * durRatio);
                    int durColor = durRatio > 0.6F ? ColorUtil.rgba(40, 230, 90, (int) (240 * a)) :
                            (durRatio > 0.3F ? ColorUtil.rgba(240, 190, 20, (int) (240 * a)) :
                                    ColorUtil.rgba(240, 45, 45, (int) (240 * a)));

                    Render2D.drawRoundedRect(ix, itemsY + 10.5F, itemW, 1.5F, 0.5F, ColorUtil.rgba(0, 0, 0, (int) (140 * a)));
                    Render2D.drawRoundedRect(ix, itemsY + 10.5F, fillW, 1.5F, 0.5F, durColor);
                }
            }
        }

        // 5. Health Bar & Absorption Bar along bottom
        float barX = curX + 5.0F;
        float barY = curY + 30.0F;
        float barW = CARD_W - 10.0F;
        float barH = 3.0F;
        float barR = 1.5F;

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
