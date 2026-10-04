package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public final class HotbarHudElement extends HudElement implements IMinecraft {

    public HotbarHudElement() {
        super("hotbar", "Custom Hotbar", 200.0F, 500.0F, 202.0F, 24.0F, false);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        float slotSize = 20.0F;
        float slotGap = 2.0F;
        float padX = 3.0F;
        float padY = 2.0F;
        float radius = 5.0F;

        this.width = padX * 2.0F + 9 * slotSize + 8 * slotGap;
        this.height = padY * 2.0F + slotSize;

        int bgColor = ColorUtil.rgba(14, 16, 24, 230);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 140);
        int accent = Theme.getAccentColor();

        Render2D.drawBlur(x, y, width, height, radius, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, radius, bgColor, 1.0F, outlineColor);

        int selectedSlot = mc.player.getInventory().getSelectedSlot();

        for (int i = 0; i < 9; i++) {
            float slotX = x + padX + i * (slotSize + slotGap);
            float slotY = y + padY;
            boolean selected = (i == selectedSlot);

            int slotBg = selected ? ColorUtil.withAlpha(accent, 160) : ColorUtil.rgba(255, 255, 255, 12);
            int slotOutline = selected ? accent : ColorUtil.rgba(255, 255, 255, 25);

            Render2D.drawRoundedRect(slotX, slotY, slotSize, slotSize, 3.5F, slotBg);
            Render2D.drawRoundedOutline(slotX, slotY, slotSize, slotSize, 3.5F, 0.7F, slotOutline);
        }

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();
            MsdfFont font = Fonts.SF_MEDIUM;
            float fontSize = 5.5F;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack == null || stack.isEmpty()) continue;

                float slotX = x + padX + i * (slotSize + slotGap);
                float slotY = y + padY;

                pose.pushMatrix();
                pose.translate(slotX + 2.0F, slotY + 2.0F);
                pose.scale(16.0F / 16.0F, 16.0F / 16.0F);
                extractor.item(stack, 0, 0);
                pose.popMatrix();

                if (stack.getCount() > 1) {
                    String cnt = String.valueOf(stack.getCount());
                    float cntW = font.getWidth(cnt, fontSize);
                    Fonts.drawString(font, cnt, slotX + slotSize - cntW - 1.5F, slotY + slotSize - fontSize - 1.0F, fontSize, ColorUtil.rgba(255, 255, 255, 255));
                }
            }
        }
    }
}
