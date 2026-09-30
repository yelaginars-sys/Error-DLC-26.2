package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 12.0F;

    public ArmorHud() {
        super("armor", "Armor Hud", 200.0F, 280.0F, 70.0F, 28.0F, false);
    }

    private List<ItemStack> getArmorItems() {
        List<ItemStack> list = new ArrayList<>();
        if (mc.player == null) return list;
        list.add(mc.player.getItemBySlot(EquipmentSlot.HEAD));
        list.add(mc.player.getItemBySlot(EquipmentSlot.CHEST));
        list.add(mc.player.getItemBySlot(EquipmentSlot.LEGS));
        list.add(mc.player.getItemBySlot(EquipmentSlot.FEET));
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<ItemStack> armor = getArmorItems();
        boolean editing = isDragging();

        boolean hasArmor = false;
        for (ItemStack stack : armor) {
            if (!stack.isEmpty()) {
                hasArmor = true;
                break;
            }
        }

        if (!hasArmor && editing) {
            armor.clear();
            armor.add(new ItemStack(Items.DIAMOND_HELMET));
            armor.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
            armor.add(new ItemStack(Items.NETHERITE_LEGGINGS));
            armor.add(new ItemStack(Items.DIAMOND_BOOTS));
            hasArmor = true;
        }

        fadeAnim.setTarget(hasArmor ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float padX = 4.0F;
        float padY = 3.0F;
        float itemSize = 12.0F;
        float gap = 3.0F;
        float radius = 5.0F;

        int count = 0;
        for (ItemStack stack : armor) {
            if (!stack.isEmpty()) count++;
        }

        float contentW = count > 0 ? count * itemSize + (count - 1) * gap : 0.0F;
        float width = Math.max(padX * 2.0F + contentW, padX * 2.0F + 50.0F);
        float height = HEADER_HEIGHT + itemSize + padY * 2.0F + 3.0F;

        this.width = width;
        this.height = height;

        int primaryColor = Theme.getAccentColor();

        // Liquid glass background with blur and specular outline
        Render2D.drawShadow(drawX, drawY, width, height, radius, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alpha)));
        int glassFill = ColorUtil.rgba(20, 18, 28, (int) (160 * alpha));
        Render2D.drawBlur(drawX, drawY, width, height, radius, 12.0F, glassFill, alpha);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, glassFill);
        Render2D.drawRoundedOutline(drawX, drawY, width, height, radius, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)));

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, HEADER_HEIGHT, radius, ColorUtil.rgba(255, 255, 255, (int) (10 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Armor", drawX + padX, drawY + 2.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Items + durability line
        float cx = drawX + padX;
        float cy = drawY + HEADER_HEIGHT + padY;

        var extractor = error.util.RenderExtend.currentGuiGraphicsExtractor();

        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;

            if (extractor != null) {
                extractor.item(stack, (int) cx, (int) cy);
            }

            // Durability line under item
            if (stack.isDamageableItem()) {
                float maxDamage = stack.getMaxDamage();
                float damage = stack.getDamageValue();
                float percent = Math.max(0.0F, 1.0F - (damage / maxDamage));

                float lineY = cy + itemSize + 1.5F;
                float lineW = itemSize * percent;

                int durColor = percent > 0.5F
                        ? ColorUtil.lerp(ColorUtil.rgba(255, 215, 65, 255), ColorUtil.rgba(65, 225, 120, 255), (percent - 0.5F) * 2.0F)
                        : ColorUtil.lerp(ColorUtil.rgba(240, 65, 65, 255), ColorUtil.rgba(255, 215, 65, 255), percent * 2.0F);

                Render2D.drawRect(cx, lineY, lineW, 1.5F, ColorUtil.applyAlpha(durColor, alpha));
            }

            cx += itemSize + gap;
        }
    }
}