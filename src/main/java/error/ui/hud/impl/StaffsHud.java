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

import java.util.ArrayList;
import java.util.List;

public final class StaffsHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 14.0F;
    private static final float ROW_HEIGHT = 11.5F;
    private static final float PADDING_X = 5.0F;
    private static final float PADDING_BOTTOM = 4.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 195);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);

    public StaffsHud() {
        super("staffs", "Staff List", 10.0F, 140.0F, 90.0F, HEADER_HEIGHT + PADDING_BOTTOM);
    }

    public record StaffMember(String name, String role, boolean online) {}

    private List<StaffMember> getStaffList() {
        List<StaffMember> list = new ArrayList<>();
        if (mc.getSingleplayerServer() == null && mc.getConnection() != null) {
            mc.getConnection().getOnlinePlayers().forEach(p -> {
                String name = p.getProfile().name();
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.contains("admin") || lower.contains("mod") || lower.contains("staff") || lower.contains("helper") || lower.contains("owner")) {
                        list.add(new StaffMember(name, "Staff", true));
                    }
                }
            });
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<StaffMember> staffList = getStaffList();
        boolean editing = isHovered(HudManager.getMouseX(), HudManager.getMouseY()) || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);

        if (staffList.isEmpty() && !editing) {
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
        float contentHeight = staffList.size() * ROW_HEIGHT;
        float totalHeight = HEADER_HEIGHT + contentHeight + PADDING_BOTTOM;

        this.height = totalHeight;

        // Render Background & Header
        Render2D.drawShadow(drawX, drawY, width, totalHeight, 4.0F, 6.0F, ColorUtil.applyAlpha(SHADOW_COLOR, alpha));
        Render2D.drawRoundedRect(drawX, drawY, width, totalHeight, 4.0F, ColorUtil.applyAlpha(BG_COLOR, alpha));

        // Header Title
        int primaryColor = Theme.getAccentColor();
        Fonts.drawIcon(IconUse.STAFF, drawX + PADDING_X, drawY + 3.0F, 8.0F, ColorUtil.applyAlpha(primaryColor, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Staff List", drawX + PADDING_X + 11.0F, drawY + 3.5F, 6.5F, ColorUtil.applyAlpha(-1, alpha));

        // Render Staff Members
        float currentY = drawY + HEADER_HEIGHT;
        if (staffList.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "No staff online", drawX + PADDING_X, currentY + 1.0F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 180, 255), alpha));
        } else {
            for (StaffMember staff : staffList) {
                int statusColor = staff.online() ? ColorUtil.rgba(65, 220, 120, 255) : ColorUtil.rgba(235, 75, 75, 255);
                Fonts.drawString(Fonts.SF_MEDIUM, staff.name(), drawX + PADDING_X, currentY + 1.0F, 6.0F, ColorUtil.applyAlpha(-1, alpha));
                Fonts.drawString(Fonts.SF_MEDIUM, staff.role(), drawX + width - PADDING_X - Fonts.SF_MEDIUM.getWidth(staff.role(), 5.5F), currentY + 1.5F, 5.5F, ColorUtil.applyAlpha(statusColor, alpha));
                currentY += ROW_HEIGHT;
            }
        }
    }
}
