package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudElement implements error.IMinecraft {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private List<ItemStack> previewStacks;

    public ArmorHud() {
        super("armor_hud", "Armor HUD", 10.0F, 100.0F, 76.0F, 22.0F, true);
    }

    private List<ItemStack> getPreviewStacks() {
        if (previewStacks == null) {
            previewStacks = new ArrayList<>();

            ItemStack helm = new ItemStack(Items.GOLDEN_HELMET);
            ItemStack chest = new ItemStack(Items.GOLDEN_CHESTPLATE);
            chest.setDamageValue((int) (chest.getMaxDamage() * 0.15F));

            ItemStack legs = new ItemStack(Items.GOLDEN_LEGGINGS);
            legs.setDamageValue((int) (legs.getMaxDamage() * 0.40F));

            ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
            boots.setDamageValue((int) (boots.getMaxDamage() * 0.75F));

            previewStacks.add(helm);
            previewStacks.add(chest);
            previewStacks.add(legs);
            previewStacks.add(boots);
        }
        return previewStacks;
    }

    private boolean isAllArmorEmpty() {
        if (mc.player == null) return true;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!mc.player.getItemBySlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.armorHud.getValue())) {
            return false;
        }
        if (mc.player == null || mc.level == null) return false;

        boolean isPreviewMode = (screen() instanceof ChatScreen) && isAllArmorEmpty();
        return isPreviewMode || !isAllArmorEmpty();
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;

        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.armorHud.getValue())) {
            return;
        }

        boolean isPreviewMode = (screen() instanceof ChatScreen) && isAllArmorEmpty();
        List<ItemStack> visibleStacks = new ArrayList<>();
        if (isPreviewMode) {
            visibleStacks.addAll(getPreviewStacks());
        } else {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = mc.player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    visibleStacks.add(stack);
                }
            }
        }

        int count = visibleStacks.size();
        if (count == 0) return;

        float itemSize = 16.0F;
        float itemGap = 3.0F;
        float padX = 4.0F;
        float padY = 2.5F;

        float totalW = padX * 2 + count * itemSize + (count - 1) * itemGap;
        float totalH = itemSize + padY * 2 + 3.0F;

        this.width = totalW;
        this.height = totalH;

        String posMode = iface != null ? iface.armorPosition.getValue() : "Над иконками голода";
        float renderX = this.x;
        float renderY = this.y;

        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        if ("Над иконками голода".equalsIgnoreCase(posMode)) {
            renderX = screenW / 2.0F + 20.0F;
            float baseY = screenH - 58.0F;

            if (mc.player != null) {
                if (mc.player.isCreative()) {
                    baseY = screenH - 46.0F; // right above the hotbar on the right in creative
                } else if (mc.player.getAirSupply() < mc.player.getMaxAirSupply()) {
                    baseY -= 10.0F; // moved up if bubbles are shown above hunger
                }
            }

            renderY = baseY;
            this.x = renderX;
            this.y = renderY;
        }

        float animAlpha = fadeAnim.getValue();
        if (animAlpha <= 0.01F) return;

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor == null) return;

        // Draw Liquid Glass HUD background card with shadow
        Render2D.drawShadow(renderX, renderY, totalW, totalH, 6.0F, 5.0F, ColorUtil.rgba(0, 0, 0, (int) (60 * animAlpha)));
        Render2D.drawHudCard(renderX, renderY, totalW, totalH, 6.0F, animAlpha);

        Render2DUtil.flush();

        float startX = renderX + padX;
        float startY = renderY + padY + 1.0F;

        for (int i = 0; i < count; i++) {
            ItemStack stack = visibleStacks.get(i);
            float ix = startX + i * (itemSize + itemGap);
            float iy = startY;

            if (stack.isEmpty()) {
                // Empty slot number preview (1..4)
                String slotNum = String.valueOf(i + 1);
                float numW = Fonts.SF_MEDIUM.getWidth(slotNum, 9.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, slotNum, ix + (itemSize - numW) * 0.5F, iy + 3.0F, 9.0F, ColorUtil.rgba(255, 255, 255, (int) (140 * animAlpha)));
            } else {
                // Item icon
                try {
                    var pose = extractor.pose();
                    pose.pushMatrix();
                    pose.translate(ix, iy);
                    extractor.item(stack, 0, 0);
                    pose.popMatrix();
                } catch (Throwable ignored) {}

                // Durability bar underneath icon
                if (stack.isDamaged() && stack.getMaxDamage() > 0) {
                    float durRatio = 1.0F - ((float) stack.getDamageValue() / (float) stack.getMaxDamage());
                    float fillW = Math.max(1.0F, (itemSize - 2.0F) * durRatio);
                    int durColor = durRatio > 0.6F ? ColorUtil.rgba(0, 255, 100, (int) (240 * animAlpha)) :
                            (durRatio > 0.3F ? ColorUtil.rgba(255, 200, 0, (int) (240 * animAlpha)) :
                                    ColorUtil.rgba(255, 50, 50, (int) (240 * animAlpha)));

                    // Background track
                    Render2D.drawRoundedRect(ix + 1.0F, iy + itemSize + 1.0F, itemSize - 2.0F, 2.0F, 0.5F, ColorUtil.rgba(0, 0, 0, (int) (120 * animAlpha)));
                    // Filled progress
                    Render2D.drawRoundedRect(ix + 1.0F, iy + itemSize + 1.0F, fillW, 2.0F, 0.5F, durColor);
                }
            }
        }
    }
}
