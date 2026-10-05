package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.staff.StaffManager;
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
import java.util.Locale;

public final class StaffHud extends HudElement implements error.IMinecraft {

    public record StaffEntry(String name, String role, boolean isVanish, float alpha) {}

    private final Animation totalWidthAnim = new Animation(85.0F, 0.20F);
    private final Animation totalHeightAnim = new Animation(15.0F, 0.20F);

    private static final float ROW_H = 14.0F;
    private static final float HEADER_H = 14.0F;
    private static final float GAP_X = 2.0F;
    private static final float GAP_Y = 2.5F;

    public StaffHud() {
        super("staff_list", "Staff List", 10.0F, 220.0F, 90.0F, 40.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.staffList.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;

        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        StaffManager manager = StaffManager.getInstance();

        List<StaffEntry> staffList = new ArrayList<>();

        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                String displayName = info.getTabListDisplayName() != null ? info.getTabListDisplayName().getString() : name;

                boolean isManualStaff = manager.isStaff(name);
                boolean isPrefixStaff = StaffManager.hasStaffPrefix(displayName);
                boolean isSpectator = info.getGameMode() == net.minecraft.world.level.GameType.SPECTATOR;

                if (isManualStaff || isPrefixStaff || isSpectator) {
                    String role = "STAFF";
                    String lowerDisp = displayName.toLowerCase(Locale.ROOT);
                    if (lowerDisp.contains("admin") || lowerDisp.contains("админ")) role = "ADMIN";
                    else if (lowerDisp.contains("moder") || lowerDisp.contains("модер")) role = "MODER";
                    else if (lowerDisp.contains("helper") || lowerDisp.contains("хелпер")) role = "HELPER";
                    else if (isSpectator) role = "SPEC";

                    staffList.add(new StaffEntry(name, role, isSpectator, 1.0F));
                }
            }
        }

        // Preview in ChatScreen if no staff online
        if (staffList.isEmpty() && inChat) {
            staffList.add(new StaffEntry("Adm1n", "SPEC", true, 1.0F));
            staffList.add(new StaffEntry("Moderator", "ACTIVE", false, 1.0F));
        }

        int accent = Theme.getAccentColor();

        float headerIconW = Fonts.getIconWidth(IconUse.STAFF, 9.0F);
        float headerTextW = Fonts.SF_MEDIUM.getWidth("Staff", 9.5F);
        float headerW = 6.0F + headerIconW + 4.0F + headerTextW + 7.0F;

        if (staffList.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        float maxRowW = headerW;
        float totalH = HEADER_H;

        for (StaffEntry entry : staffList) {
            float nameTextW = Fonts.SF_MEDIUM.getWidth(entry.name, 9.0F);
            float roleTextW = Fonts.SF_MEDIUM.getWidth(entry.role, 8.5F);

            float leftPillW = 6.0F + 4.0F + 3.0F + nameTextW + 6.0F;
            float rightPillW = 5.0F + roleTextW + 5.0F;
            float rowW = leftPillW + GAP_X + rightPillW;

            if (rowW > maxRowW) maxRowW = rowW;
            totalH += (ROW_H + GAP_Y) * entry.alpha;
        }

        totalWidthAnim.setTarget(maxRowW);
        totalWidthAnim.update();
        totalHeightAnim.setTarget(totalH);
        totalHeightAnim.update();

        this.width = totalWidthAnim.getValue();
        this.height = totalHeightAnim.getValue();

        // 1. Draw Header Capsule [ 🛡 Staff ]
        Render2D.drawLiquidGlass(this.x, this.y, headerW, HEADER_H, 4.0F, 1.0F, accent);
        Fonts.drawIcon(IconUse.STAFF, this.x + 6.0F, this.y + 2.5F, 9.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Staff", this.x + 6.0F + headerIconW + 4.0F, this.y + 2.5F, 9.5F, 0xFFFFFFFF);

        // 2. Draw Active Staff Rows
        float currY = this.y + HEADER_H + GAP_Y;
        for (StaffEntry entry : staffList) {
            float a = entry.alpha;
            if (a <= 0.01F) continue;

            float nameTextW = Fonts.SF_MEDIUM.getWidth(entry.name, 9.0F);
            float roleTextW = Fonts.SF_MEDIUM.getWidth(entry.role, 8.5F);

            float leftPillW = 6.0F + 4.0F + 3.0F + nameTextW + 6.0F;
            float rightPillW = Math.max(5.0F + roleTextW + 5.0F, this.width - leftPillW - GAP_X);

            int textAlpha = ColorUtil.rgba(255, 255, 255, (int) (255 * a));
            int roleCol = entry.isVanish ? ColorUtil.rgba(245, 180, 50, (int) (255 * a)) : ColorUtil.withAlpha(accent, (int) (255 * a));
            int dotCol = entry.isVanish ? ColorUtil.rgba(245, 180, 50, (int) (255 * a)) : ColorUtil.rgba(80, 240, 110, (int) (255 * a));

            // Left Pill: [ ● PlayerName ]
            Render2D.drawLiquidGlass(this.x, currY, leftPillW, ROW_H, 3.5F, a, accent);
            Render2D.drawCircle(this.x + 6.0F, currY + ROW_H * 0.5F, 2.0F, dotCol);
            Fonts.drawString(Fonts.SF_MEDIUM, entry.name, this.x + 12.0F, currY + 2.5F, 9.0F, textAlpha);

            // Right Pill: [ ROLE ]
            Render2D.drawLiquidGlass(this.x + leftPillW + GAP_X, currY, rightPillW, ROW_H, 3.5F, a, accent);
            Fonts.drawString(Fonts.SF_MEDIUM, entry.role, this.x + leftPillW + GAP_X + 5.0F, currY + 2.8F, 8.5F, roleCol);

            currY += (ROW_H + GAP_Y) * a;
        }
    }
}
