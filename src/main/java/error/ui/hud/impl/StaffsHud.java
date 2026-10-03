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
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;

public final class StaffsHud extends HudElement implements IMinecraft {
    private final Animation heightAnim = new Animation(0.0F, 0.22F);
    private final Animation chatOffsetAnim = new Animation(0.0F, 0.22F);

    public record StaffMember(String name, String role, boolean active) {}

    public StaffsHud() {
        super("staffs", "Staffs", 10.0F, 50.0F, 130.0F, 30.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (!isEnabled() || mc.player == null) return;

        List<StaffMember> staffList = getOnlineStaff();
        fadeAnim.setTarget(staffList.isEmpty() ? 0.0F : 1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float headerH = 18.0F;
        float itemH = 13.0F;
        float targetH = headerH + (staffList.size() * itemH) + 4.0F;

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

        // Header: Staffs Title + Staff/Shield Icon
        Fonts.drawString(Fonts.SF_MEDIUM, "Staffs", renderX + 8.0F, renderY + 4.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawIcon(IconUse.STAFF, renderX + width - 16.0F, renderY + 4.5F, 7.5F, ColorUtil.multiplyAlpha(accent, alpha));

        // Line Divider
        Render2D.drawRoundedRect(renderX + 6.0F, renderY + headerH, width - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

        float curY = renderY + headerH + 3.0F;
        Render2D.pushScissor(renderX, renderY + headerH, width, height - headerH);
        for (StaffMember staff : staffList) {
            Fonts.drawIcon(IconUse.PERSONS, renderX + 8.0F, curY + 1.0F, 6.5F, ColorUtil.multiplyAlpha(accent, alpha));

            float textX = renderX + 18.0F;
            if (!staff.role().isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, "● " + staff.role() + " ", textX, curY + 1.0F, 6.0F, ColorUtil.rgba(255, 100, 100, (int) (230 * alpha)));
                textX += Fonts.SF_MEDIUM.getWidth("● " + staff.role() + " ", 6.0F);
            }
            Fonts.drawString(Fonts.SF_MEDIUM, staff.name(), textX, curY + 1.0F, 6.5F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));

            String status = staff.active() ? "Active" : "Vanish";
            int statusCol = staff.active() ? ColorUtil.rgba(85, 255, 135, (int) (240 * alpha)) : ColorUtil.rgba(255, 80, 100, (int) (240 * alpha));
            float statusW = Fonts.SF_MEDIUM.getWidth(status, 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, status, renderX + width - statusW - 8.0F, curY + 1.0F, 6.0F, statusCol);

            curY += itemH;
        }
        Render2D.popScissor();
    }

    private List<StaffMember> getOnlineStaff() {
        List<StaffMember> list = new ArrayList<>();
        if (mc.getConnection() == null) return list;

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().name();
            String lower = name.toLowerCase();
            if (lower.contains("admin") || lower.contains("owner") || lower.contains("mod") || lower.contains("staff") || lower.contains("helper")) {
                String role = lower.contains("owner") ? "OWNER" : lower.contains("admin") ? "ADMIN" : "STAFF";
                list.add(new StaffMember(name, role, true));
            }
        }
        return list;
    }
}
