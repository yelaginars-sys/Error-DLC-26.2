package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StaffListHudElement extends HudElement implements IMinecraft {

    private static record StaffData(String name, String prefix, String status, int color) {}

    private static final List<StaffData> PREVIEW_STAFF = List.of(
            new StaffData("Steve", "[Admin] ", "ONLINE", ColorUtil.rgba(100, 255, 120, 255)),
            new StaffData("Alex", "[Mod] ", "NEAR", ColorUtil.rgba(100, 200, 255, 255)),
            new StaffData("Notch", "[Owner] ", "VANISH", ColorUtil.rgba(255, 80, 80, 255))
    );

    private final Set<String> staffPrefixes = new HashSet<>(Arrays.asList(
            "supp", "mod", "adm", "owner", "мод", "адм", "владелец", "хелпер", "yt", "ютуб", "admin", "helper", "dev"
    ));

    public StaffListHudElement() {
        super("stafflist", "Staff List", 10.0F, 250.0F, 130.0F, 70.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        List<StaffData> activeStaff = new ArrayList<>();

        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                String displayName = info.getTabListDisplayName() != null ? info.getTabListDisplayName().getString() : name;

                boolean isStaff = false;
                for (String p : staffPrefixes) {
                    if (displayName.toLowerCase().contains(p)) {
                        isStaff = true;
                        break;
                    }
                }

                if (isStaff) {
                    String status = "ONLINE";
                    int color = ColorUtil.rgba(100, 255, 120, 255);
                    if (info.getGameMode() == GameType.SPECTATOR) {
                        status = "SPECTATOR";
                        color = ColorUtil.rgba(255, 220, 70, 255);
                    }
                    activeStaff.add(new StaffData(name, "", status, color));
                }
            }
        }

        if (activeStaff.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            activeStaff.addAll(PREVIEW_STAFF);
        }

        if (activeStaff.isEmpty()) {
            this.height = 0;
            return;
        }

        MsdfFont font = Fonts.SF_MEDIUM;
        float headerSize = 7.5F;
        float itemSize = 6.2F;
        float headerH = 20.0F;
        float itemH = 14.0F;
        float pad = 6.0F;

        float maxContentW = 120.0F;
        for (StaffData d : activeStaff) {
            float nameW = font.getWidth(d.prefix() + d.name(), itemSize);
            float statusW = font.getWidth(d.status(), itemSize);
            maxContentW = Math.max(maxContentW, nameW + statusW + 30.0F);
        }

        this.width = maxContentW;
        this.height = headerH + activeStaff.size() * itemH + pad;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 100);

        Render2D.drawBlur(x, y, width, height, 6.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 6.0F, bgColor, 1.0F, outlineColor);

        // Header
        Fonts.drawString(font, "StaffList", x + 8.0F, y + 4.0F, headerSize, ColorUtil.rgba(240, 240, 250, 255));
        Render2D.drawRect(x + 8.0F, y + headerH - 2.0F, width - 16.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 30));

        float curY = y + headerH + 2.0F;
        for (StaffData staff : activeStaff) {
            // Status dot
            Render2D.drawCircle(x + 10.0F, curY + itemH / 2.0F - 1.0F, 2.5F, staff.color());

            // Name
            Fonts.drawString(font, staff.prefix() + staff.name(), x + 16.0F, curY + (itemH - font.lineHeight(itemSize)) / 2.0F, itemSize, ColorUtil.rgba(240, 240, 245, 255));

            // Status label
            float statW = font.getWidth(staff.status(), itemSize);
            Fonts.drawString(font, staff.status(), x + width - 8.0F - statW, curY + (itemH - font.lineHeight(itemSize)) / 2.0F, itemSize, staff.color());

            curY += itemH;
        }
    }
}
