package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

public final class StaffsHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);
    private static final float HEADER_HEIGHT = 14.0F;

    public StaffsHud() {
        super("staffs", "Staff List", 6.0F, 60.0F, 90.0F, HEADER_HEIGHT + 14.0F);
    }

    private List<String> getOnlineStaff() {
        List<String> list = new ArrayList<>();
        if (mc.getConnection() == null) return list;

        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry != null && entry.getProfile() != null) {
                String name = entry.getProfile().name();
                if (isStaffName(name)) {
                    list.add(name);
                }
            }
        }
        return list;
    }

    private boolean isStaffName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("admin") || lower.contains("mod") || lower.contains("helper") || lower.contains("staff") || lower.contains("owner");
    }

    @Override
    public void draw(Render2DEvent event) {
        List<String> staff = getOnlineStaff();
        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        fadeAnim.setTarget((!staff.isEmpty() || editing) ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float padX = 4.0F;
        float headerH = HEADER_HEIGHT;
        float itemH = 12.0F;
        float radius = 5.0F;

        String title = "Staff";
        float maxW = Fonts.SF_MEDIUM.getWidth(title, 6.0F);

        if (staff.isEmpty() && editing) {
            maxW = Math.max(maxW, Fonts.SF_MEDIUM.getWidth("No staff online", 6.0F));
        } else {
            for (String s : staff) {
                maxW = Math.max(maxW, Fonts.SF_MEDIUM.getWidth(s, 6.0F));
            }
        }

        float width = Math.max(80.0F, padX * 2.0F + maxW + 10.0F);
        int itemCount = staff.isEmpty() ? 1 : staff.size();
        float height = headerH + itemCount * itemH + 3.0F;

        this.width = width;
        this.height = height;

        int primaryColor = ACCENT_PURPLE;
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(14, 14, 18, (int) (160 * alpha));
        int headerBg = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));

        // Background (Waper Style)
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, headerH, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, title, drawX + padX, drawY + 3.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Rows
        float currentY = drawY + headerH + 2.0F;
        if (staff.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "No staff online", drawX + padX, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(150, 150, 160, 255), alpha));
        } else {
            for (String s : staff) {
                Fonts.drawString(Fonts.SF_MEDIUM, s, drawX + padX, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
                currentY += itemH;
            }
        }
    }
}
