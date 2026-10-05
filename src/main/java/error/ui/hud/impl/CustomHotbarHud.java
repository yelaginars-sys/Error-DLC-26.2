package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CustomHotbarHud extends HudElement {

    private static final float BAR_W = 182.0F;
    private static final float BAR_H = 22.0F;
    private static final float SLOT_SIZE = 20.0F;
    private final Animation selectAnim = new Animation(0.0F, 0.22F);
    private final Animation[] cooldownAnims = new Animation[9];

    public CustomHotbarHud() {
        super("custom_hotbar", "Custom Hotbar", 100.0F, 100.0F, BAR_W, BAR_H, true);
        for (int i = 0; i < 9; i++) {
            cooldownAnims[i] = new Animation(0.0F, 0.18F);
        }
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.customHotbar.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    @Override
    public void draw(Render2DEvent event) {
        if (error.IMinecraft.mc.player == null || error.IMinecraft.mc.getWindow() == null) return;

        float screenW = error.IMinecraft.mc.getWindow().getGuiScaledWidth();
        float screenH = error.IMinecraft.mc.getWindow().getGuiScaledHeight();

        boolean inChat = error.IMinecraft.mc.gui != null && error.IMinecraft.mc.gui.screen() instanceof ChatScreen;

        // Position fixed at bottom center unless dragged in edit mode
        if (!this.dragging) {
            this.x = (screenW - BAR_W) * 0.5F;
            this.y = screenH - BAR_H - 1.0F;
        }

        this.width = BAR_W;
        this.height = BAR_H;

        int accent = Theme.getAccentColor();

        // 1. Draw Liquid Glass Background Capsule
        Render2D.drawLiquidGlass(this.x, this.y, BAR_W, BAR_H, 7.0F, 1.0F, accent);

        Player player = error.IMinecraft.mc.player;
        int selectedSlot = Math.max(0, Math.min(8, player.getInventory().getSelectedSlot()));

        // Smooth selected slot animation
        selectAnim.setTarget(selectedSlot);
        selectAnim.update();
        float selX = this.x + 1.0F + selectAnim.getValue() * SLOT_SIZE;

        // 2. Selection Box Highlight + Accent Underline
        Render2D.drawRoundedRect(selX, this.y + 1.0F, SLOT_SIZE, SLOT_SIZE, 5.5F, ColorUtil.rgba(0, 0, 0, 95));
        Render2D.drawRoundedRect(selX + 5.0F, this.y + BAR_H - 2.5F, 10.0F, 1.8F, 1.0F, accent);

        var extractor = event.getGuiGraphicsExtractor();

        // 3. Render 9 Slots
        for (int i = 0; i < 9; i++) {
            float slotX = this.x + 1.0F + i * SLOT_SIZE;
            float slotY = this.y + 1.0F;

            ItemStack stack = player.getInventory().getItem(i);

            if (stack.isEmpty()) {
                // Empty Slot Number: 1..9
                String numStr = String.valueOf(i + 1);
                float nw = Fonts.SF_MEDIUM.getWidth(numStr, 8.5F);
                float tx = slotX + (SLOT_SIZE - nw) * 0.5F;
                float ty = slotY + 5.0F;
                int numColor = ColorUtil.rgba(255, 255, 255, (i == selectedSlot) ? 180 : 85);
                Fonts.drawString(Fonts.SF_MEDIUM, numStr, tx, ty, 8.5F, numColor);
                cooldownAnims[i].setTarget(0.0F);
                cooldownAnims[i].update();
            } else {
                // Occupied Slot: Draw Item + Durability + Count
                float itemX = slotX + 2.0F;
                float itemY = slotY + 2.0F;

                if (extractor != null) {
                    Render2DUtil.flush();
                    try {
                        var pose = extractor.pose();
                        pose.pushMatrix();
                        pose.translate(itemX, itemY);
                        extractor.item(stack, 0, 0);
                        if (error.IMinecraft.mc.font != null) {
                            extractor.itemDecorations(error.IMinecraft.mc.font, stack, 0, 0);
                        }
                        pose.popMatrix();
                    } catch (Throwable ignored) {}
                }

                // Cooldown overlay
                float cd = player.getCooldowns().getCooldownPercent(stack, 0.0F);
                Animation cdAnim = cooldownAnims[i];
                cdAnim.setTarget(cd);
                cdAnim.update();
                float cdVal = cdAnim.getValue();

                if (cdVal > 0.01F) {
                    float cdH = 16.0F * cdVal;
                    Render2D.drawRect(itemX, itemY + (16.0F - cdH), 16.0F, cdH, ColorUtil.rgba(0, 0, 0, 150));
                }
            }
        }
    }
}
