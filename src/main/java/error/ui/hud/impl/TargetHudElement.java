package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.impl.combat.AuraModule;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public final class TargetHudElement extends HudElement implements IMinecraft {

    private final Animation hpAnimation = new Animation(20.0F, 0.25F);

    public TargetHudElement() {
        super("targethud", "Target HUD", 350.0F, 220.0F, 185.0F, 48.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        LivingEntity target = getTarget();
        if (target == null && HudManager.getInstance().isDraggableScreenOpen()) {
            target = mc.player;
        }

        if (target == null) {
            this.height = 0;
            return;
        }

        this.width = 185.0F;
        this.height = 48.0F;

        MsdfFont font = Fonts.SF_MEDIUM;

        float hp = target.getHealth();
        float maxHp = target.getMaxHealth() > 0 ? target.getMaxHealth() : 20.0F;

        hpAnimation.setTarget(hp);
        hpAnimation.update();
        float animHp = hpAnimation.getValue();
        float animPercent = Math.max(0.0F, Math.min(1.0F, animHp / maxHp));

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 120);

        Render2D.drawBlur(x, y, width, height, 8.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 8.0F, bgColor, 1.0F, outlineColor);

        // Player Head
        Identifier skinLocation = null;
        if (target instanceof AbstractClientPlayer clientPlayer) {
            if (clientPlayer.getSkin() != null && clientPlayer.getSkin().body() != null) {
                skinLocation = clientPlayer.getSkin().body().texturePath();
            }
        }
        if (skinLocation != null) {
            Render2D.drawHead(skinLocation, x + 6.0F, y + 6.0F, 36.0F, 6.0F);
        } else {
            Render2D.drawRoundedRect(x + 6.0F, y + 6.0F, 36.0F, 36.0F, 6.0F, ColorUtil.rgba(40, 40, 50, 255));
        }

        // Target Name & HP Text
        String name = target.getName().getString();
        if (name.length() > 12) name = name.substring(0, 12) + "...";

        float nameX = x + 48.0F;
        Fonts.drawString(font, name, nameX, y + 6.0F, 7.5F, ColorUtil.rgba(240, 240, 250, 255));

        String hpText = String.format("hp %.1f", hp);
        float hpTextW = font.getWidth(hpText, 6.5F);
        Fonts.drawString(font, hpText, x + width - 8.0F - hpTextW, y + 7.0F, 6.5F, ColorUtil.rgba(255, 120, 120, 255));

        // Animated Health Bar
        float barX = nameX;
        float barY = y + 21.0F;
        float barW = width - 56.0F;
        float barH = 5.0F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, 2.5F, ColorUtil.rgba(40, 40, 50, 200));

        float fillW = barW * animPercent;
        if (fillW > 0.0F) {
            int hpColor = ColorUtil.rgba((int) ((1.0F - animPercent) * 255), (int) (animPercent * 255), 80, 255);
            Render2D.drawGradientRound(barX, barY, fillW, barH, 2.5F, Theme.getAccentColor(), hpColor, hpColor, Theme.getAccentColor());
        }

        // Target Equipment Items
        var extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();

            List<ItemStack> equip = new ArrayList<>();
            equip.add(target.getMainHandItem());
            equip.add(target.getItemBySlot(EquipmentSlot.HEAD));
            equip.add(target.getItemBySlot(EquipmentSlot.CHEST));
            equip.add(target.getItemBySlot(EquipmentSlot.LEGS));
            equip.add(target.getItemBySlot(EquipmentSlot.FEET));
            equip.add(target.getOffhandItem());

            float equipX = barX;
            float equipY = y + 30.0F;
            float itemSize = 13.0F;
            float itemScale = itemSize / 16.0F;

            for (ItemStack stack : equip) {
                if (stack == null || stack.isEmpty()) continue;
                pose.pushMatrix();
                pose.translate(equipX, equipY);
                pose.scale(itemScale, itemScale);
                extractor.item(stack, 0, 0);
                pose.popMatrix();

                equipX += itemSize + 3.0F;
            }
        }
    }

    private LivingEntity getTarget() {
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            return AuraModule.INSTANCE.getTarget();
        }
        return null;
    }
}
