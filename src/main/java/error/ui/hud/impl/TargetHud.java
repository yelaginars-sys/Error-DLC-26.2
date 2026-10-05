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
    private final Animation openAnim = new Animation(0.0F, 0.22F);
    private final Animation healthAnim = new Animation(20.0F, 0.25F);
    private final Animation absAnim = new Animation(0.0F, 0.25F);

    private static final float CARD_W = 118.0F;
    private static final float CARD_H = 37.0F;
    private static final float CARD_R = 7.0F;
    private static final float AVATAR_SIZE = 22.0F;
    private static final float AVATAR_R = 4.5F;

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

        // 1. Main Background Card in Liquid Glass
        Render2D.drawShadow(curX, curY, CARD_W, CARD_H, CARD_R, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (70 * a)));
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Blur.of(curX, curY, CARD_W, CARD_H)
                    .radius(Math.round(CARD_R))
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(14, 16, 22, (int) (120 * a)))
                    .alpha(a)
                    .render(extractor);

            Outline.of(curX, curY, CARD_W, CARD_H)
                    .radius(Math.round(CARD_R))
                    .thickness(0.8F)
                    .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 30))
                    .alpha(a)
                    .render(extractor);
        }

        // 2. Avatar on the left
        Identifier skin = STEVE_SKIN;
        if (this.target instanceof AbstractClientPlayer clientPlayer) {
            try {
                skin = clientPlayer.getSkin().body().texturePath();
            } catch (Throwable ignored) {}
        }

        float avatarX = curX + 5.0F;
        float avatarY = curY + 5.0F;
        Render2D.drawRoundedRect(avatarX - 0.5F, avatarY - 0.5F, AVATAR_SIZE + 1.0F, AVATAR_SIZE + 1.0F, AVATAR_R, ColorUtil.rgba(255, 255, 255, (int) (20 * a)));
        Render2D.drawHead(skin, avatarX, avatarY, AVATAR_SIZE, AVATAR_R, a);
        Render2D.drawRoundedOutline(avatarX - 0.5F, avatarY - 0.5F, AVATAR_SIZE + 1.0F, AVATAR_SIZE + 1.0F, AVATAR_R, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (40 * a)));

        // 3. Top Row: HP in top-left + Nickname next to it
        float startX = curX + 31.0F;
        float topY = curY + 4.5F;

        String hpText = String.format("%.1f HP", Math.max(0.0F, smoothHp));
        if (smoothAbs > 0.05F) {
            hpText += String.format(" (+%.1f)", smoothAbs);
        }

        float hpW = Fonts.SF_MEDIUM.getWidth(hpText, 7.5F);
        Fonts.drawString(Fonts.SF_MEDIUM, hpText, startX, topY, 7.5F, ColorUtil.withAlpha(accent, (int) (255 * a)));

        String name = this.target.getName().getString();
        float nameX = startX + hpW + 4.0F;
        float maxNameW = (curX + CARD_W - 5.0F) - nameX;
        if (Fonts.SF_MEDIUM.getWidth(name, 7.5F) > maxNameW) {
            while (name.length() > 2 && Fonts.SF_MEDIUM.getWidth(name + "..", 7.5F) > maxNameW) {
                name = name.substring(0, name.length() - 1);
            }
            name += "..";
        }
        Fonts.drawString(Fonts.SF_MEDIUM, name, nameX, topY, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * a)));

        // 4. Middle Row: Armor with durability bars + Hand items under HP & Nick
        float itemsY = curY + 15.0F;
        List<ItemStack> equippedItems = getEquippedItems(this.target, isChat);

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
