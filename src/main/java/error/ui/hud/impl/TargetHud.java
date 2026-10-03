package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.impl.combat.AuraModule;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class TargetHud extends HudElement implements IMinecraft {

    private final Animation healthAnim = new Animation(20.0F, 0.25F);
    private LivingEntity targetEntity = null;

    public TargetHud() {
        super("target_hud", "Target HUD", 240.0F, 180.0F, 140.0F, 42.0F, true);
    }

    private LivingEntity resolveTarget() {
        AuraModule aura = AuraModule.INSTANCE;
        if (aura != null && aura.isEnabled() && aura.getTarget() != null && aura.getTarget().isAlive()) {
            return aura.getTarget();
        }

        if (mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }

        if (mc.gui.screen() instanceof ChatScreen) {
            return mc.player;
        }

        return null;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        LivingEntity curTarget = resolveTarget();
        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;

        fadeAnim.setTarget(curTarget != null ? 1.0F : 0.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();

        if (alpha <= 0.01F) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        if (curTarget != null) {
            this.targetEntity = curTarget;
        }

        if (this.targetEntity == null) return;

        float cardW = 145.0F;
        float cardH = 42.0F;
        this.width = cardW;
        this.height = cardH;

        float x = getX();
        float y = getY();

        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (150 * alpha));
        int glassFill = ColorUtil.rgba(18, 16, 26, (int) (225 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (40 * alpha));
        int themeAccent = Theme.getAccentColor();

        // Glass background card
        Render2D.drawShadow(x, y, cardW, cardH, 7.0F, 10.0F, shadowCol);
        Render2D.drawBlur(x, y, cardW, cardH, 7.0F, 16.0F, glassFill, alpha);
        Render2D.drawRoundedRect(x, y, cardW, cardH, 7.0F, glassFill);
        Render2D.drawRoundedOutline(x, y, cardW, cardH, 7.0F, 1.0F, glassBorder);

        // Player Head / Avatar
        float headSize = 28.0F;
        float headX = x + 7.0F;
        float headY = y + (cardH - headSize) / 2.0F;

        if (targetEntity instanceof AbstractClientPlayer clientPlayer) {
            Render2D.drawCustomAvatar(headX, headY, headSize, 5.0F, alpha);
        } else {
            Render2D.drawRoundedRect(headX, headY, headSize, headSize, 5.0F, ColorUtil.rgba(40, 36, 54, (int) (220 * alpha)));
        }
        Render2D.drawRoundedOutline(headX, headY, headSize, headSize, 5.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));

        float contentX = headX + headSize + 8.0F;
        float availableTextW = cardW - (contentX - x) - 8.0F;

        // Target Name
        String name = targetEntity.getName().getString();
        Fonts.drawString(Fonts.SF_MEDIUM, name, contentX, y + 6.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        // Health & Absorption
        float hp = targetEntity.getHealth();
        float maxHp = Math.max(1.0F, targetEntity.getMaxHealth());
        float absorb = targetEntity.getAbsorptionAmount();

        healthAnim.setTarget(hp);
        healthAnim.update();
        float animHp = healthAnim.getValue();

        String hpText = String.format("%.1f HP", hp + absorb);
        float hpW = Fonts.SF_MEDIUM.getWidth(hpText, 6.5F);
        Fonts.drawString(Fonts.SF_MEDIUM, hpText, x + cardW - hpW - 8.0F, y + 6.5F, 6.5F, ColorUtil.rgba(220, 220, 240, (int) (210 * alpha)));

        // Distance Text
        float dist = mc.player.distanceTo(targetEntity);
        String distText = String.format("%.1fm", dist);
        Fonts.drawString(Fonts.SF_MEDIUM, distText, contentX, y + 17.0F, 6.5F, ColorUtil.rgba(170, 175, 200, (int) (190 * alpha)));

        // Health Bar at the bottom
        float barX = contentX;
        float barY = y + cardH - 11.0F;
        float barW = availableTextW;
        float barH = 4.0F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, 2.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));

        float pct = Math.min(1.0F, Math.max(0.0F, animHp / maxHp));
        float fillW = Math.max(2.0F, barW * pct);

        int hpCol1 = ColorUtil.multiplyAlpha(themeAccent, alpha);
        int hpCol2 = ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), (int) (180 * alpha));

        Render2D.drawGradientRound(barX, barY, fillW, barH, 2.0F, hpCol1, hpCol2, hpCol2, hpCol1);

        // Armor & Item Icons (if player)
        if (targetEntity instanceof Player player) {
            float armorX = contentX + 35.0F;
            float armorY = y + 16.0F;
            float iconSize = 9.0F;

            List<ItemStack> armorItems = new ArrayList<>();
            net.minecraft.world.entity.EquipmentSlot[] slots = {
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET
            };
            for (var slot : slots) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack != null && !stack.isEmpty()) armorItems.add(stack);
            }

            for (int i = 0; i < armorItems.size(); i++) {
                Render2D.drawRoundedRect(armorX + i * 11.0F, armorY, iconSize, iconSize, 2.0F, ColorUtil.rgba(255, 255, 255, (int) (30 * alpha)));
            }
        }
    }
}
