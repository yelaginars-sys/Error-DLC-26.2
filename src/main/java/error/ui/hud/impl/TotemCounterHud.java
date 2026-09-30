package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TotemCounterHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 12.0F;

    public TotemCounterHud() {
        super("totem_counter", "Totem Counter", 160.0F, 140.0F, 40.0F, 28.0F);
    }

    private int countTotems() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
        }
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            count += mc.player.getOffhandItem().getCount();
        }
        return count;
    }

    @Override
    public void draw(Render2DEvent event) {
        int totems = countTotems();
        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        if (totems == 0 && editing) totems = 1;

        fadeAnim.setTarget(totems > 0 ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float itemSize = 12.0F;
        float padX = 4.0F;
        float padY = 3.0F;
        float radius = 5.0F;

        String countStr = String.valueOf(totems);
        float countW = Fonts.SF_MEDIUM.getWidth(countStr, 6.0F);
        float contentW = itemSize + 4.0F + countW;
        float width = padX * 2.0F + contentW;
        float height = HEADER_HEIGHT + itemSize + padY * 2.0F;

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
        Render2D.drawRoundedRect(drawX, drawY, width, HEADER_HEIGHT, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, "Totem", drawX + padX, drawY + 2.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Item Totem & Count
        float cx = drawX + padX;
        float cy = drawY + HEADER_HEIGHT + padY;

        var extractor = error.util.RenderExtend.currentGuiGraphicsExtractor();
        if (extractor != null) {
            extractor.item(new ItemStack(Items.TOTEM_OF_UNDYING), (int) cx, (int) cy);
        } else {
            Fonts.drawString(Fonts.SF_MEDIUM, "🗿", cx, cy, 8.0F, ColorUtil.applyAlpha(-1, alpha));
        }

        cx += itemSize + 4.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, countStr, cx, cy + (itemSize - 6.0F) / 2.0F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(235, 235, 235, 255), alpha));
    }
}
