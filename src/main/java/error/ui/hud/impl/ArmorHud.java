package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHud extends HudElement implements IMinecraft {

    private static final float HEIGHT = 26.0F;
    private static final float SLOT_SIZE = 16.0F;
    private static final float PADDING_X = 6.0F;

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public ArmorHud() {
        super("armorhud", "Armor Hud", 10.0F, 160.0F, 100.0F, HEIGHT);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        List<ItemStack> armorItems = new ArrayList<>();
        boolean hasArmor = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            armorItems.add(stack);
            if (!stack.isEmpty()) {
                hasArmor = true;
            }
        }

        boolean editing = isHovered(HudManager.getMouseX(), HudManager.getMouseY()) || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        if (!hasArmor && !editing) {
            fadeAnim.setTarget(0.0F);
            fadeAnim.update();
            return;
        }

        fadeAnim.setTarget(1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();
        float totalWidth = PADDING_X * 2.0F + 4 * 22.0F;

        this.width = totalWidth;
        this.height = HEIGHT;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 30));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 60));
        int bgColor = ColorUtil.applyAlpha(ColorUtil.rgba(14, 14, 18, 195), alpha);

        // Render Background
        Render2D.drawShadow(drawX, drawY, totalWidth, HEIGHT, 5.0F, 6.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, totalWidth + 1.0F, HEIGHT + 1.0F, 5.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, totalWidth, HEIGHT, 5.0F, bgColor);

        // Render Armor Slots
        float currentX = drawX + PADDING_X;
        for (ItemStack stack : armorItems) {
            if (!stack.isEmpty()) {
                int maxDurability = stack.getMaxDamage();
                int currentDurability = maxDurability - stack.getDamageValue();
                int durabilityPercent = maxDurability > 0 ? (int) ((currentDurability / (float) maxDurability) * 100) : 100;

                int durColor = ColorUtil.rgba(65, 220, 120, 255);
                if (durabilityPercent < 30) {
                    durColor = ColorUtil.rgba(235, 75, 75, 255);
                } else if (durabilityPercent < 60) {
                    durColor = ColorUtil.rgba(245, 190, 45, 255);
                }

                String durStr = durabilityPercent + "%";
                float durW = Fonts.SF_MEDIUM.getWidth(durStr, 5.5F);

                Fonts.drawString(Fonts.SF_MEDIUM, durStr, currentX + (20.0F - durW) / 2.0F, drawY + HEIGHT - 7.5F, 5.5F, ColorUtil.applyAlpha(durColor, alpha));
            } else {
                Fonts.drawString(Fonts.SF_MEDIUM, "-", currentX + 8.0F, drawY + HEIGHT - 7.5F, 5.5F, ColorUtil.applyAlpha(ColorUtil.rgba(120, 120, 120, 255), alpha));
            }
            currentX += 22.0F;
        }
    }
}