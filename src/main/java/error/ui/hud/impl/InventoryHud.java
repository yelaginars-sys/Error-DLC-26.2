package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.world.item.ItemStack;

public final class InventoryHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 13.0F;

    public InventoryHud() {
        super("inventory", "Inventory", 60.0F, 60.0F, 110.0F, 50.0F);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        boolean hasItems = false;
        for (int i = 9; i < 36; i++) {
            if (!mc.player.getInventory().getItem(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        fadeAnim.setTarget((hasItems || editing) ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float cell = 12.0F;
        float headerH = HEADER_HEIGHT;
        float width = cell * 9.0F;
        float height = headerH + cell * 3.0F + 2.0F;
        float radius = 5.0F;

        this.width = width;
        this.height = height;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));
        int headerBg = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));

        // Background with blur and theme accent glow
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawBlur(drawX, drawY, width, height, radius, 12.0F, bgColor, alpha);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, headerH, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, "Inventory", drawX + 4.0F, drawY + 3.0F, 6.5F, ColorUtil.applyAlpha(primaryColor, alpha));

        // 9x3 Grid
        float gridY = drawY + headerH;
        int gridDividerCol = ColorUtil.rgba(255, 255, 255, (int) (22 * alpha));

        var extractor = error.util.RenderExtend.currentGuiGraphicsExtractor();

        for (int row = 0; row < 3; row++) {
            float rowY = gridY + row * cell;
            for (int col = 1; col < 9; col++) {
                Render2D.drawRect(drawX + col * cell - 0.25F, rowY + 1.5F, 0.5F, cell - 3.0F, gridDividerCol);
            }
            for (int col = 0; col < 9; col++) {
                ItemStack stack = mc.player.getInventory().getItem(9 + row * 9 + col);
                if (stack.isEmpty()) continue;

                if (extractor != null) {
                    extractor.item(stack, (int) (drawX + col * cell), (int) rowY);
                }
            }
        }
    }
}
