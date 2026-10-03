package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class CooldownsHud extends HudElement implements IMinecraft {
    private final Animation heightAnim = new Animation(0.0F, 0.22F);
    private final Animation chatOffsetAnim = new Animation(0.0F, 0.22F);

    public record CooldownItem(String name, ItemStack stack, float remainingSeconds) {}

    public CooldownsHud() {
        super("cooldowns", "Cooldowns", 200.0F, 200.0F, 125.0F, 30.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (!isEnabled() || mc.player == null) return;

        List<CooldownItem> activeCooldowns = getActiveCooldowns();
        fadeAnim.setTarget(activeCooldowns.isEmpty() ? 0.0F : 1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float headerH = 18.0F;
        float itemH = 13.0F;
        float targetH = headerH + (activeCooldowns.size() * itemH) + 4.0F;

        heightAnim.setTarget(targetH);
        heightAnim.update();
        this.height = heightAnim.getValue();

        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
        chatOffsetAnim.setTarget(chatOpen ? -20.0F : 0.0F);
        chatOffsetAnim.update();

        float renderY = (dragging ? getY() : getY()) + chatOffsetAnim.getValue();
        float renderX = getX();

        int accent = Theme.getAccentColor();
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));
        int glassFill = ColorUtil.rgba(16, 18, 26, (int) (205 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (35 * alpha));

        Render2D.drawShadow(renderX, renderY, width, height, 8.0F, 8.0F, shadowCol);
        Render2D.drawBlur(renderX, renderY, width, height, 8.0F, 14.0F, glassFill, alpha);
        Render2D.drawRoundedRect(renderX, renderY, width, height, 8.0F, glassFill);
        Render2D.drawRoundedOutline(renderX, renderY, width, height, 8.0F, 1.0F, glassBorder);

        // Header: Cooldowns Title + Hourglass Icon
        Fonts.drawString(Fonts.SF_MEDIUM, "Cooldowns", renderX + 8.0F, renderY + 4.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawIcon(IconUse.CLOCK, renderX + width - 16.0F, renderY + 4.5F, 7.5F, ColorUtil.multiplyAlpha(accent, alpha));

        // Line Divider
        Render2D.drawRoundedRect(renderX + 6.0F, renderY + headerH, width - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

        float curY = renderY + headerH + 3.0F;
        Render2D.pushScissor(renderX, renderY + headerH, width, height - headerH);
        for (CooldownItem cd : activeCooldowns) {
            Fonts.drawIcon(IconUse.CLOCK, renderX + 8.0F, curY + 1.0F, 6.5F, ColorUtil.multiplyAlpha(accent, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, cd.name(), renderX + 18.0F, curY + 1.0F, 6.5F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));

            String timeStr = String.format("0:%02ds", Math.round(cd.remainingSeconds()));
            float timeW = Fonts.SF_MEDIUM.getWidth(timeStr, 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, timeStr, renderX + width - timeW - 8.0F, curY + 1.0F, 6.0F, ColorUtil.rgba(180, 185, 205, (int) (190 * alpha)));

            curY += itemH;
        }
        Render2D.popScissor();
    }

    private List<CooldownItem> getActiveCooldowns() {
        List<CooldownItem> list = new ArrayList<>();
        if (mc.player == null) return list;

        Item[] trackedItems = {
                Items.FIREWORK_ROCKET,
                Items.ENDER_PEARL,
                Items.CHORUS_FRUIT,
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE
        };

        String[] names = {
                "Фейерверк",
                "Эндер-жемчуг",
                "Хорус",
                "Золотое яблоко",
                "Зач. яблоко"
        };

        var cooldowns = mc.player.getCooldowns();
        for (int i = 0; i < trackedItems.length; i++) {
            Item item = trackedItems[i];
            ItemStack stack = new ItemStack(item);
            if (cooldowns.isOnCooldown(stack)) {
                float pct = cooldowns.getCooldownPercent(stack, 0.0F);
                float remSecs = pct * 15.0F;
                list.add(new CooldownItem(names[i], stack, remSecs));
            }
        }
        return list;
    }
}
