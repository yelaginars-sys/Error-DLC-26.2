package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

public final class StaffsHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 15.0F;

    public record StaffEntry(String name, String status, int statusColor) {}

    public StaffsHud() {
        super("staffs", "Staff List", 6.0F, 90.0F, 90.0F, HEADER_HEIGHT + 14.0F, false);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    private List<StaffEntry> getOnlineStaff() {
        List<StaffEntry> list = new ArrayList<>();
        if (mc.getConnection() == null) return list;

        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry != null && entry.getProfile() != null) {
                String name = entry.getProfile().name();
                if (isStaffName(name)) {
                    list.add(new StaffEntry(name, "ONLINE", ColorUtil.rgba(140, 240, 140, 255)));
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
        List<StaffEntry> staff = getOnlineStaff();
        boolean editing = isDragging();

        fadeAnim.setTarget((!staff.isEmpty() || editing) ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float padX = 4.5F;
        float headerH = HEADER_HEIGHT;
        float itemH = 11.0F;
        float radius = 6.0F;

        String title = "Staff";
        float maxNameW = Fonts.SF_MEDIUM.getWidth(title, 7.5F);
        float maxStatusW = 0.0F;

        if (staff.isEmpty() && editing) {
            staff.add(new StaffEntry("Admin", "NEAR", ColorUtil.rgba(255, 100, 100, 255)));
            staff.add(new StaffEntry("Moderator", "ONLINE", ColorUtil.rgba(140, 240, 140, 255)));
        }

        for (StaffEntry s : staff) {
            maxNameW = Math.max(maxNameW, Fonts.SF_MEDIUM.getWidth(s.name(), 6.0F));
            maxStatusW = Math.max(maxStatusW, Fonts.SF_MEDIUM.getWidth(s.status(), 6.0F));
        }

        float width = Math.max(85.0F, padX * 2.0F + maxNameW + maxStatusW + 16.0F);
        int itemCount = staff.isEmpty() ? 1 : staff.size();
        float height = headerH + itemCount * itemH + 3.0F;

        this.width = width;
        this.height = height;

        int primaryColor = Theme.getAccentColor();

        // Liquid glass background with blur and specular outline
        Render2D.drawShadow(drawX, drawY, width, height, radius, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alpha)));
        int glassFill = ColorUtil.rgba(20, 18, 28, (int) (160 * alpha));
        Render2D.drawBlur(drawX, drawY, width, height, radius, 12.0F, glassFill, alpha);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, glassFill);
        Render2D.drawRoundedOutline(drawX, drawY, width, height, radius, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)));

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, headerH, radius, ColorUtil.rgba(255, 255, 255, (int) (10 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, title, drawX + padX, drawY + 4.0F, 7.5F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Rows
        float currentY = drawY + headerH + 1.5F;
        if (staff.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "No staff online", drawX + padX, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(150, 150, 160, 255), alpha));
        } else {
            for (StaffEntry s : staff) {
                Fonts.drawString(Fonts.SF_MEDIUM, s.name(), drawX + padX, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(235, 235, 235, 255), alpha));
                float statW = Fonts.SF_MEDIUM.getWidth(s.status(), 6.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, s.status(), drawX + width - padX - statW, currentY + 2.5F, 6.0F, ColorUtil.applyAlpha(s.statusColor(), alpha));
                currentY += itemH;
            }
        }
    }
}
