package error.ui.hud.impl;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import error.util.render.Render2DUtil;

/**
 * Create by daun kvass
 */
public final class ArmorHud extends HudElement implements IMinecraft {

    public enum Orientation { HORIZONTAL, VERTICAL }
    public enum DurabilityDisplay { PERCENT, BAR }

    private Orientation orientation = Orientation.HORIZONTAL;
    private DurabilityDisplay durabilityDisplay = DurabilityDisplay.PERCENT;
    private boolean showEmptySlots = true;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 190);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public ArmorHud() {
        super("armorhud", "Armor Hud", 10.0F, 160.0F, 82.0F, 28.0F);
    }

    public JsonObject writeConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("orientation", orientation.name());
        obj.addProperty("durabilityDisplay", durabilityDisplay.name());
        obj.addProperty("showEmptySlots", showEmptySlots);
        return obj;
    }

    public void readConfig(JsonObject obj) {
        if (obj == null) return;
        if (obj.has("orientation")) {
            try { this.orientation = Orientation.valueOf(obj.get("orientation").getAsString()); } catch (Exception ignored) {}
        }
        if (obj.has("durabilityDisplay")) {
            try { this.durabilityDisplay = DurabilityDisplay.valueOf(obj.get("durabilityDisplay").getAsString()); } catch (Exception ignored) {}
        }
        if (obj.has("showEmptySlots")) {
            this.showEmptySlots = obj.get("showEmptySlots").getAsBoolean();
        }
    }

    @Override
    public boolean shouldRender() {
        return enabled && mc.player != null;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        if (orientation == Orientation.HORIZONTAL) {
            drawHorizontal(event);
        } else {
            drawVertical(event);
        }
    }


    private void drawHorizontal(Render2DEvent event) {
        float slotSize = 16.0F;
        float gap = 3.5F;
        float pad = 4.5F;

        this.width = pad * 2.0F + (ARMOR_SLOTS.length * slotSize) + ((ARMOR_SLOTS.length - 1) * gap);
        this.height = (durabilityDisplay == DurabilityDisplay.PERCENT) ? 29.0F : 24.0F;

        Render2D.drawShadow(x, y, width, height, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 5.0F, BG_COLOR, 1.0F);

        var extractor = event.getGuiGraphicsExtractor();
        float curX = x + pad;
        float itemY = y + pad;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = mc.player.getItemBySlot(slot);

            Render2D.drawRoundedRect(curX, itemY, slotSize, slotSize, 2.5F, ColorUtil.rgba(255, 255, 255, 8));

            if (stack.isEmpty()) {
                if (showEmptySlots) {
                    Fonts.drawCenteredIcon(IconUse.CROSS, curX + slotSize / 2.0F, itemY + 4.5F, 6.0F, ColorUtil.rgba(140, 145, 160, 120));
                }
            } else {
                if (extractor != null) {
                    Render2DUtil.flush();
                    extractor.pose().pushMatrix();
                    extractor.pose().translate(curX, itemY);
                    extractor.item(stack, 0, 0);
                    extractor.pose().popMatrix();
                }

                if (stack.isDamaged()) {
                    float percent = getDurabilityPercent(stack);
                    int duraColor = getDurabilityColor(percent);

                    if (durabilityDisplay == DurabilityDisplay.PERCENT) {
                        String percentStr = (int) (percent * 100.0F) + "%";
                        Fonts.drawCenteredString(Fonts.SF_MEDIUM, percentStr, curX + slotSize / 2.0F, itemY + slotSize + 1.0F, 6.5F, duraColor);
                    } else {
                        float barY = itemY + slotSize + 1.5F;
                        Render2D.drawRoundedRect(curX, barY, slotSize, 1.5F, 0.75F, ColorUtil.rgba(255, 255, 255, 20));
                        Render2D.drawRoundedRect(curX, barY, slotSize * percent, 1.5F, 0.75F, duraColor);
                    }
                }
            }

            curX += slotSize + gap;
        }
    }

    /**
     * ВЕРТИКАЛЬНЫЙ РЕЖИМ
     */
    private void drawVertical(Render2DEvent event) {
        float slotSize = 16.0F;
        float gap = 3.5F;
        float pad = 4.5F;

        this.width = (durabilityDisplay == DurabilityDisplay.PERCENT) ? 42.0F : 25.0F;
        this.height = pad * 2.0F + (ARMOR_SLOTS.length * slotSize) + ((ARMOR_SLOTS.length - 1) * gap);

        Render2D.drawShadow(x, y, width, height, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 5.0F, BG_COLOR, 1.0F);

        var extractor = event.getGuiGraphicsExtractor();
        float itemX = x + pad;
        float curY = y + pad;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = mc.player.getItemBySlot(slot);

            Render2D.drawRoundedRect(itemX, curY, slotSize, slotSize, 2.5F, ColorUtil.rgba(255, 255, 255, 8));

            if (stack.isEmpty()) {
                if (showEmptySlots) {
                    Fonts.drawCenteredIcon(IconUse.CROSS, itemX + slotSize / 2.0F, curY + 4.5F, 6.0F, ColorUtil.rgba(140, 145, 160, 120));
                }
            } else {
                if (extractor != null) {
                    Render2DUtil.flush();
                    extractor.pose().pushMatrix();
                    extractor.pose().translate(itemX, curY);
                    extractor.item(stack, 0, 0);
                    extractor.pose().popMatrix();
                }

                if (stack.isDamaged()) {
                    float percent = getDurabilityPercent(stack);
                    int duraColor = getDurabilityColor(percent);

                    if (durabilityDisplay == DurabilityDisplay.PERCENT) {
                        String percentStr = (int) (percent * 100.0F) + "%";
                        Fonts.drawString(Fonts.SF_MEDIUM, percentStr, itemX + slotSize + 3.0F, curY + 4.5F, 7.0F, duraColor);
                    } else {
                        float barX = itemX + slotSize + 2.0F;
                        Render2D.drawRoundedRect(barX, curY + 1.0F, 1.5F, slotSize - 2.0F, 0.75F, ColorUtil.rgba(255, 255, 255, 20));
                        float barH = (slotSize - 2.0F) * percent;
                        Render2D.drawRoundedRect(barX, curY + 1.0F + (slotSize - 2.0F - barH), 1.5F, barH, 0.75F, duraColor);
                    }
                }
            }

            curY += slotSize + gap;
        }
    }

    private float getDurabilityPercent(ItemStack stack) {
        if (!stack.isDamaged() || stack.getMaxDamage() == 0) return 1.0F;
        float max = stack.getMaxDamage();
        float current = max - stack.getDamageValue();
        return Math.max(0.0F, Math.min(1.0F, current / max));
    }

    private int getDurabilityColor(float percent) {
        return ColorUtil.rgba(
                (int) (255 * (1.0F - percent)),
                (int) (225 * percent + 30),
                80,
                255
        );
    }

    /**
     * КОНТЕКСТНОЕ МЕНЮ (ПКМ)
     */
    @Override
    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        float menuW = 135.0F;
        float menuH = 56.0F;

        Render2D.drawShadow(menuX, menuY, menuW, menuH, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(menuX, menuY, menuW, menuH, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(14, 14, 18, 240), alpha), 1.0F);

        Fonts.drawString(Fonts.SF_MEDIUM, "Layout", menuX + 6.0F, menuY + 6.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        drawChip(menuX + 48.0F, menuY + 5.0F, 38.0F, 11.5F, "Horiz", orientation == Orientation.HORIZONTAL, alpha);
        drawChip(menuX + 90.0F, menuY + 5.0F, 38.0F, 11.5F, "Vert", orientation == Orientation.VERTICAL, alpha);

        float row2Y = menuY + 21.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Dura", menuX + 6.0F, row2Y + 1.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        drawChip(menuX + 48.0F, row2Y, 38.0F, 11.5F, "Percent", durabilityDisplay == DurabilityDisplay.PERCENT, alpha);
        drawChip(menuX + 90.0F, row2Y, 38.0F, 11.5F, "Bar", durabilityDisplay == DurabilityDisplay.BAR, alpha);

        float row3Y = menuY + 37.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Empty", menuX + 6.0F, row3Y + 1.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        drawChip(menuX + 48.0F, row3Y, 80.0F, 11.5F, showEmptySlots ? "Show" : "Hide", showEmptySlots, alpha);

        return menuH;
    }

    private void drawChip(float cx, float cy, float cw, float ch, String text, boolean active, float alpha) {
        int bg = active ? Theme.getAccentColor() : 0x351C1F2E;
        int textCol = active ? 0xFFFFFFFF : Theme.TEXT_MUTED;
        Render2D.drawRoundedRect(cx, cy, cw, ch, 2.5F, ColorUtil.multiplyAlpha(bg, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, cx + cw / 2.0F, cy + 2.0F, 7.5F, ColorUtil.multiplyAlpha(textCol, alpha));
    }

    @Override
    public boolean handleContextMenuClick(float menuX, float menuY, double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float menuW = 135.0F;
        float menuH = 56.0F;

        if (mouseY >= menuY + 4.0F && mouseY <= menuY + 18.0F) {
            if (mouseX >= menuX + 48.0F && mouseX <= menuX + 86.0F) { this.orientation = Orientation.HORIZONTAL; return true; }
            if (mouseX >= menuX + 90.0F && mouseX <= menuX + 128.0F) { this.orientation = Orientation.VERTICAL; return true; }
        }

        float row2Y = menuY + 20.0F;
        if (mouseY >= row2Y && mouseY <= row2Y + 14.0F) {
            if (mouseX >= menuX + 48.0F && mouseX <= menuX + 86.0F) { this.durabilityDisplay = DurabilityDisplay.PERCENT; return true; }
            if (mouseX >= menuX + 90.0F && mouseX <= menuX + 128.0F) { this.durabilityDisplay = DurabilityDisplay.BAR; return true; }
        }

        float row3Y = menuY + 36.0F;
        if (mouseY >= row3Y && mouseY <= row3Y + 14.0F) {
            if (mouseX >= menuX + 48.0F && mouseX <= menuX + 128.0F) {
                this.showEmptySlots = !this.showEmptySlots;
                return true;
            }
        }

        return mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + menuH;
    }
}