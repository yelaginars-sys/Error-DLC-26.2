package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.staff.StaffManager;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.regex.Pattern;

public final class StaffHud extends HudElement implements error.IMinecraft {

    public enum StaffStatus {
        VANISHED("VANISH", 0xFFFF4545),
        NEAR("NEAR", 0xFFFFAA00),
        SPEC("SPEC", 0xFF55FFFF),
        STAFF("STAFF", 0xFF55FF55);

        public final String label;
        public final int color;

        StaffStatus(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private record Entry(String name, StaffStatus status, Identifier skin, float alpha) {}

    private static final Pattern STAFF_PATTERN = Pattern.compile(
            ".*((s|ꜱ)upp|mod|der|adm|help|wne|мод|хелп|помо|адм|владе|отри|таф|taf|curat|курато|dev|раз|сапп|yt|ютуб|стажер|сотрудник).*",
            Pattern.CASE_INSENSITIVE
    );

    private final Map<String, Animation> anims = new HashMap<>();
    private final Animation widthAnim = new Animation(85.0F, 0.22F);
    private final Animation heightAnim = new Animation(18.0F, 0.22F);

    private static final float ROW_H = 15.0F;
    private static final float HEADER_H = 17.0F;
    private static final float PILL_R = 7.5F;
    private static final float AVATAR_SIZE = 9.5F;
    private static final float GAP_Y = 3.0F;

    private static final Identifier STEVE_SKIN = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public StaffHud() {
        super("staff_list", "Staffs", 10.0F, 220.0F, 90.0F, 40.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.staffList.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private StaffStatus getPlayerStatus(String name, boolean isSpectator) {
        if (isSpectator) return StaffStatus.SPEC;
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                if (p.getScoreboardName().equalsIgnoreCase(name)) {
                    if (mc.player != null && mc.player.distanceTo(p) <= 50.0F) {
                        return StaffStatus.NEAR;
                    }
                    return StaffStatus.STAFF;
                }
            }
        }
        return StaffStatus.VANISHED;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;

        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        StaffManager manager = StaffManager.getInstance();

        Map<String, StaffStatus> currentStaff = new LinkedHashMap<>();
        Map<String, Identifier> staffSkins = new HashMap<>();

        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                String displayName = info.getTabListDisplayName() != null ? info.getTabListDisplayName().getString() : name;

                boolean isManualStaff = manager != null && manager.isStaff(name);
                boolean isPrefixStaff = STAFF_PATTERN.matcher(displayName).matches() || STAFF_PATTERN.matcher(name).matches();
                boolean isSpectator = info.getGameMode() == net.minecraft.world.level.GameType.SPECTATOR;

                if (isManualStaff || isPrefixStaff || isSpectator) {
                    currentStaff.put(name, getPlayerStatus(name, isSpectator));
                    try {
                        staffSkins.put(name, info.getSkin().body().texturePath());
                    } catch (Throwable ignored) {
                        staffSkins.put(name, STEVE_SKIN);
                    }
                }
            }
        }

        // Update animations
        for (String name : currentStaff.keySet()) {
            Animation a = anims.computeIfAbsent(name, k -> new Animation(0.0F, 0.20F));
            a.setTarget(1.0F);
            a.update();
        }

        anims.entrySet().removeIf(e -> {
            boolean active = currentStaff.containsKey(e.getKey());
            if (!active) {
                e.getValue().setTarget(0.0F);
                e.getValue().update();
            }
            return !active && e.getValue().getValue() <= 0.01F;
        });

        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, StaffStatus> e : currentStaff.entrySet()) {
            Animation a = anims.get(e.getKey());
            if (a != null && a.getValue() > 0.01F) {
                entries.add(new Entry(e.getKey(), e.getValue(), staffSkins.getOrDefault(e.getKey(), STEVE_SKIN), a.getValue()));
            }
        }

        if (entries.isEmpty() && inChat) {
            entries.add(new Entry("Adm1n", StaffStatus.VANISHED, STEVE_SKIN, 1.0F));
            entries.add(new Entry("Moderator", StaffStatus.NEAR, STEVE_SKIN, 1.0F));
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Calculate dynamic width
        float maxRowW = 85.0F;
        float headerTitleW = Fonts.SF_MEDIUM.getWidth("Staffs", 9.0F);
        float headerMinW = 20.0F + headerTitleW + 8.0F;
        maxRowW = Math.max(maxRowW, headerMinW);

        for (Entry e : entries) {
            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 8.5F);
            float statusW = Fonts.SF_MEDIUM.getWidth(e.status.label, 8.0F);
            float rowTotalW = (16.0F + nameW + 8.0F) + 6.0F + (statusW + 12.0F);
            maxRowW = Math.max(maxRowW, rowTotalW);
        }

        float totalH = HEADER_H;
        for (Entry e : entries) {
            totalH += (ROW_H + GAP_Y) * e.alpha;
        }

        widthAnim.setTarget(maxRowW);
        widthAnim.update();
        heightAnim.setTarget(totalH);
        heightAnim.update();

        this.width = widthAnim.getValue();
        this.height = heightAnim.getValue();

        int accent = Theme.getAccentColor();
        float curX = this.x;
        float curY = this.y;

        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();

        // 1. Header Capsule
        Render2D.drawHudCard(extractor, curX, curY, this.width, HEADER_H, PILL_R, 1.0F, accent);

        // Energy Glyph "r"
        Fonts.drawString(Fonts.ENERGY, "r", curX + 6.0F, curY + 2.5F, 10.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Staffs", curX + 19.0F, curY + 3.0F, 9.0F, 0xFFFFFFFF);

        curY += HEADER_H + GAP_Y;

        // 2. Entries: Left capsule (Avatar + Name) and Right capsule (Status)
        for (Entry e : entries) {
            if (e.alpha <= 0.01F) continue;

            int textWhite = ColorUtil.rgba(255, 255, 255, (int) (245 * e.alpha));
            int statusCol = ColorUtil.withAlpha(e.status.color, (int) (245 * e.alpha));

            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 8.5F);
            float statusW = Fonts.SF_MEDIUM.getWidth(e.status.label, 8.0F);

            float leftPillW = 16.0F + nameW + 8.0F;
            float rightPillW = statusW + 12.0F;
            float rightPillX = curX + this.width - rightPillW;

            // Pills (Left & Right)
            Render2D.drawHudPill(extractor, curX, curY, leftPillW, ROW_H, PILL_R, e.alpha, accent);
            Render2D.drawHudPill(extractor, rightPillX, curY, rightPillW, ROW_H, PILL_R, e.alpha, accent);

            // Avatar
            float avatarY = curY + (ROW_H - AVATAR_SIZE) * 0.5F;
            Render2D.drawHead(e.skin, curX + 4.5F, avatarY, AVATAR_SIZE, 3.0F, e.alpha);

            // Name
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, curX + 16.5F, curY + 2.0F, 8.5F, textWhite);

            // Status tag centered in right capsule
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, e.status.label, rightPillX + rightPillW * 0.5F, curY + 2.2F, 8.0F, statusCol);

            curY += (ROW_H + GAP_Y) * e.alpha;
        }
    }
}
