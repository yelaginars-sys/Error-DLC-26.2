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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemHudElement extends HudElement implements IMinecraft {

    private static record ItemInfo(ItemStack stack, int count) {}

    public ItemHudElement() {
        super("itemhud", "Item HUD", 200.0F, 90.0F, 120.0F, 22.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        List<ItemInfo> items = new ArrayList<>();

        int totems = getItemCount(Items.TOTEM_OF_UNDYING);
        int gapples = getItemCount(Items.GOLDEN_APPLE) + getItemCount(Items.ENCHANTED_GOLDEN_APPLE);
        int pearls = getItemCount(Items.ENDER_PEARL);
        int crystals = getItemCount(Items.END_CRYSTAL);

        if (totems > 0) items.add(new ItemInfo(new ItemStack(Items.TOTEM_OF_UNDYING), totems));
        if (gapples > 0) items.add(new ItemInfo(new ItemStack(Items.GOLDEN_APPLE), gapples));
        if (pearls > 0) items.add(new ItemInfo(new ItemStack(Items.ENDER_PEARL), pearls));
        if (crystals > 0) items.add(new ItemInfo(new ItemStack(Items.END_CRYSTAL), crystals));

        if (items.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            items.add(new ItemInfo(new ItemStack(Items.TOTEM_OF_UNDYING), 4));
            items.add(new ItemInfo(new ItemStack(Items.GOLDEN_APPLE), 12));
            items.add(new ItemInfo(new ItemStack(Items.ENDER_PEARL), 16));
        }

        if (items.isEmpty()) {
            this.height = 0;
            return;
        }

        MsdfFont font = Fonts.SF_MEDIUM;
        float fontSize = 6.5F;
        float itemSize = 14.0F;
        float itemGap = 6.0F;
        float padX = 6.0F;
        float H = 22.0F;

        float totalW = padX * 2.0F;
        for (ItemInfo info : items) {
            String cnt = String.valueOf(info.count());
            totalW += itemSize + 3.0F + font.getWidth(cnt, fontSize) + itemGap;
        }
        totalW -= itemGap;

        this.width = Math.max(60.0F, totalW);
        this.height = H;

        int bgColor = ColorUtil.rgba(18, 20, 28, 220);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 140);

        Render2D.drawBlur(x, y, width, height, 5.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 5.0F, bgColor, 1.0F, outlineColor);

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();

            float curX = x + padX;
            for (ItemInfo info : items) {
                pose.pushMatrix();
                pose.translate(curX, y + (H - itemSize) / 2.0F);
                pose.scale(itemSize / 16.0F, itemSize / 16.0F);
                extractor.item(info.stack(), 0, 0);
                pose.popMatrix();

                curX += itemSize + 3.0F;
                String cnt = String.valueOf(info.count());
                Fonts.drawString(font, cnt, curX, y + (H - font.lineHeight(fontSize)) / 2.0F, fontSize, ColorUtil.rgba(240, 240, 250, 255));
                curX += font.getWidth(cnt, fontSize) + itemGap;
            }
        }
    }

    private int getItemCount(net.minecraft.world.item.Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != null && stack.is(item)) {
                count += stack.getCount();
            }
        }
        if (mc.player.getOffhandItem().is(item)) {
            count += mc.player.getOffhandItem().getCount();
        }
        return count;
    }
}
