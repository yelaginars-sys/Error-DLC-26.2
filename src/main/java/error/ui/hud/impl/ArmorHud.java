package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;

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

        float totalW = count * itemSize + (count - 1) * itemGap;
        float totalH = itemSize + 4.0F;

        this.width = totalW;
        this.height = totalH;

        String posMode = iface != null ? iface.armorPosition.getValue() : "Над иконками голода";
        float renderX = this.x;
        float renderY = this.y;

        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        if ("Над иконками голода".equalsIgnoreCase(posMode)) {
            renderX = screenW / 2.0F + 10.0F;
            renderY = screenH - 55.0F;
            this.x = renderX;
            this.y = renderY;
        }

        float animAlpha = fadeAnim.getValue();
        if (animAlpha <= 0.01F) return;

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor == null) return;

        Render2DUtil.flush();

        for (int i = 0; i < count; i++) {
            ItemStack stack = visibleStacks.get(i);
            float ix = renderX + i * (itemSize + itemGap);
            float iy = renderY;

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
                float fillW = Math.max(2.0F, (itemSize - 2.0F) * durRatio);
                int durColor = durRatio > 0.5F ? ColorUtil.rgba(34, 197, 94, (int) (240 * animAlpha)) :
                        (durRatio > 0.2F ? ColorUtil.rgba(234, 179, 8, (int) (240 * animAlpha)) :
                                ColorUtil.rgba(239, 68, 68, (int) (240 * animAlpha)));

                Render2D.drawRoundedRect(ix + 1.0F, iy + itemSize + 1.5F, fillW, 1.5F, 0.75F, durColor);
            }
        }
    }
}
