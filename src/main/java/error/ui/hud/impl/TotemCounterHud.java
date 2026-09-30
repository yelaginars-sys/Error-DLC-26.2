package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TotemCounterHud extends HudElement implements IMinecraft {

    private static final float HEIGHT = 22.0F;
    private static final float PADDING_X = 6.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 195);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);

    public TotemCounterHud() {
        super("totemcounter", "Totem Counter", 10.0F, 190.0F, 65.0F, HEIGHT);
    }

    private int countTotems() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            count += mc.player.getOffhandItem().getCount();
        }
        return count;
    }

    @Override
    public void draw(Render2DEvent event) {
        int totems = countTotems();
        boolean editing = isHovered(HudManager.getMouseX(), HudManager.getMouseY()) || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        if (totems <= 0 && !editing) {
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

        Render2D.drawShadow(drawX, drawY, width, height, 4.0F, 6.0F, ColorUtil.applyAlpha(SHADOW_COLOR, alpha));
        Render2D.drawRoundedRect(drawX, drawY, width, height, 4.0F, ColorUtil.applyAlpha(BG_COLOR, alpha));

        int primaryColor = Theme.getAccentColor();
        Fonts.drawIcon(IconUse.FIGHT, drawX + PADDING_X, drawY + 6.0F, 10.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        String label = "Totems:";
        String countStr = String.valueOf(totems);

        Fonts.drawString(Fonts.SF_MEDIUM, label, drawX + PADDING_X + 13.0F, drawY + 4.5F, 6.0F, ColorUtil.applyAlpha(-1, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, countStr, drawX + width - PADDING_X - Fonts.SF_MEDIUM.getWidth(countStr, 7.0F), drawY + 4.0F, 7.0F, ColorUtil.applyAlpha(primaryColor, alpha));
    }
}
