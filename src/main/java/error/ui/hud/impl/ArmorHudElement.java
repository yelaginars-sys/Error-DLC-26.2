package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHudElement extends HudElement implements IMinecraft {

    private List<ItemStack> previewArmorCache = null;

    private List<ItemStack> getPreviewArmor() {
        if (previewArmorCache == null) {
            previewArmorCache = List.of(
                    new ItemStack(Items.DIAMOND_HELMET),
                    new ItemStack(Items.NETHERITE_CHESTPLATE),
                    new ItemStack(Items.NETHERITE_LEGGINGS),
                    new ItemStack(Items.NETHERITE_BOOTS),
                    new ItemStack(Items.TOTEM_OF_UNDYING),
                    new ItemStack(Items.END_CRYSTAL)
            );
        }
        return previewArmorCache;
    }

    public ArmorHudElement() {
        super("armorhud", "Armor HUD", 380.0F, 430.0F, 120.0F, 28.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        List<ItemStack> items = new ArrayList<>();

        if (mc.player != null) {
            items.add(mc.player.getItemBySlot(EquipmentSlot.FEET));
            items.add(mc.player.getItemBySlot(EquipmentSlot.LEGS));
            items.add(mc.player.getItemBySlot(EquipmentSlot.CHEST));
            items.add(mc.player.getItemBySlot(EquipmentSlot.HEAD));
            items.add(mc.player.getOffhandItem());
            items.add(mc.player.getMainHandItem());
        }

        boolean hasItems = items.stream().anyMatch(s -> s != null && !s.isEmpty());
        if (!hasItems && HudManager.getInstance().isDraggableScreenOpen()) {
            items = getPreviewArmor();
            hasItems = true;
        }

        if (!hasItems) {
            this.height = 0;
            return;
        }

        float padding = 6.0F;
        float itemSlotW = 18.0F;
        float totalW = items.size() * itemSlotW + padding * 2.0F;

        this.width = totalW;
        this.height = 28.0F;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 100);

        Render2D.drawBlur(x, y, width, height, 6.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 6.0F, bgColor, 1.0F, outlineColor);

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor == null) return;

        Render2DUtil.flush();
        Matrix3x2fStack pose = extractor.pose();

        MsdfFont font = Fonts.SF_MEDIUM;
        float fontUnit = 0.36F;
        float curX = x + padding;

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                curX += itemSlotW;
                continue;
            }

            pose.pushMatrix();
            pose.translate(curX, y + 4.0F);
            pose.scale(1.0F, 1.0F);
            extractor.item(stack, 0, 0);
            pose.popMatrix();

            if (stack.isDamageableItem()) {
                int maxDamage = stack.getMaxDamage();
                int damage = stack.getDamageValue();
                int durPercent = Math.max(0, 100 - (damage * 100 / maxDamage));

                int durColor = ColorUtil.rgba((int) ((100 - durPercent) * 2.55F), (int) (durPercent * 2.55F), 50, 255);
                String durStr = durPercent + "%";
                float durW = font.getWidth(durStr, fontUnit);
                Fonts.drawString(font, durStr, curX + (16.0F - durW) / 2.0F, y + 18.0F, fontUnit, durColor);
            } else if (stack.getCount() > 1) {
                String cntStr = String.valueOf(stack.getCount());
                float cntW = font.getWidth(cntStr, fontUnit);
                Fonts.drawString(font, cntStr, curX + 16.0F - cntW, y + 18.0F, fontUnit, ColorUtil.rgba(255, 255, 255, 255));
            }

            curX += itemSlotW;
        }
    }
}
