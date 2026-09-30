package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CooldownsHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 15.0F;

    public record CooldownEntry(String name, String time, Item item) {}

    public CooldownsHud() {
        super("cooldowns", "Cooldowns", 6.0F, 40.0F, 85.0F, HEADER_HEIGHT + 14.0F);
    }

    private List<CooldownEntry> getActiveCooldowns() {
        List<CooldownEntry> list = new ArrayList<>();
        if (mc.player == null) return list;

        Item[] trackedItems = new Item[]{
                Items.ENDER_PEARL,
                Items.CHORUS_FRUIT,
                Items.SHIELD,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.GOLDEN_APPLE,
                Items.WIND_CHARGE
        };

        for (Item item : trackedItems) {
            if (mc.player.getCooldowns().isOnCooldown(new ItemStack(item))) {
                float percent = mc.player.getCooldowns().getCooldownPercent(new ItemStack(item), 0.0F);
                if (percent > 0.001F) {
                    float secs = percent * 15.0F;
                    String timeStr = String.format(Locale.US, "%.1fs", secs);
                    String name = item.getName(new ItemStack(item)).getString();
                    list.add(new CooldownEntry(name, timeStr, item));
                }
            }
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<CooldownEntry> cds = getActiveCooldowns();
        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        fadeAnim.setTarget((!cds.isEmpty() || editing) ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float padX = 4.5F;
        float headerH = HEADER_HEIGHT;
        float itemH = 11.0F;
        float radius = 6.0F;

        String title = "Cooldowns";
        float maxNameW = Fonts.SF_MEDIUM.getWidth(title, 7.5F);
        float maxCdW = 0.0F;

        if (cds.isEmpty() && editing) {
            cds.add(new CooldownEntry("Ender Pearl", "8.4s", Items.ENDER_PEARL));
            cds.add(new CooldownEntry("Chorus Fruit", "1.2s", Items.CHORUS_FRUIT));
        }

        for (CooldownEntry e : cds) {
            maxNameW = Math.max(maxNameW, Fonts.SF_MEDIUM.getWidth(e.name(), 6.0F));
            maxCdW = Math.max(maxCdW, Fonts.SF_MEDIUM.getWidth(e.time(), 6.0F));
        }

        float width = Math.max(85.0F, padX * 2.0F + maxNameW + maxCdW + 16.0F);
        int itemCount = cds.isEmpty() ? 1 : cds.size();
        float height = headerH + itemCount * itemH + 3.0F;

        this.width = width;
        this.height = height;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));
        int headerBg = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));

        // Liquid glass background with blur
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawBlur(drawX, drawY, width, height, radius, 12.0F, bgColor, alpha);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, headerH, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, title, drawX + padX, drawY + 4.0F, 7.5F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Rows
        float currentY = drawY + headerH + 1.5F;
        if (cds.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "No cooldowns", drawX + padX, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(150, 150, 160, 255), alpha));
        } else {
            for (CooldownEntry e : cds) {
                Fonts.drawString(Fonts.SF_MEDIUM, e.name(), drawX + padX, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(235, 235, 235, 255), alpha));
                float cdW = Fonts.SF_MEDIUM.getWidth(e.time(), 6.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, e.time(), drawX + width - padX - cdW, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 180, 255), alpha));
                currentY += itemH;
            }
        }
    }
}
