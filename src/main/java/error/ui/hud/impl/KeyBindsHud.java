package error.ui.hud.impl;

import error.Client;
import error.event.list.Render2DEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.render.Interface;
import error.setting.impl.BindSetting;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.KeyUtil;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.*;

public final class KeyBindsHud extends HudElement implements error.IMinecraft {

    private record Entry(String name, String keyName, String categoryIcon, float alpha) {}

    private final Map<Module, Animation> anims = new HashMap<>();
    private final Animation widthAnim = new Animation(80.0F, 0.22F);
    private final Animation heightAnim = new Animation(18.0F, 0.22F);

    private static final float ROW_H = 15.0F;
    private static final float HEADER_H = 17.0F;
    private static final float PILL_R = 7.5F;
    private static final float GAP_Y = 3.0F;

    public KeyBindsHud() {
        super("keybinds", "Hotkeys", 10.0F, 100.0F, 85.0F, 40.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.binds.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private static String getCategoryIcon(Category category) {
        if (category == null) return "•";
        return switch (category) {
            case COMBAT -> Fonts.NURIK_COMBAT;
            case MOVEMENT -> Fonts.NURIK_MOVEMENT;
            case RENDER -> Fonts.NURIK_VISUALS;
            case PLAYER -> Fonts.NURIK_PLAYER;
            case MISC -> Fonts.NURIK_MISC;
            default -> "•";
        };
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;

        List<Module> allModules = Client.getInstance() != null && Client.getInstance().getModuleManager() != null
                ? Client.getInstance().getModuleManager().getModules() : Collections.emptyList();

        for (Module m : allModules) {
            BindSetting bs = m.getBind();
            boolean hasValidBind = bs != null && !bs.getValue().isEmpty() && BindSetting.isValidKey(bs.getValue().get(0));
            boolean shouldShow = hasValidBind && m.isState();

            Animation a = anims.computeIfAbsent(m, k -> new Animation(0.0F, 0.20F));
            a.setTarget(shouldShow ? 1.0F : 0.0F);
            a.update();
        }

        anims.entrySet().removeIf(e -> {
            BindSetting bs = e.getKey().getBind();
            boolean valid = bs != null && !bs.getValue().isEmpty() && BindSetting.isValidKey(bs.getValue().get(0));
            return (!valid || !e.getKey().isState()) && e.getValue().getValue() <= 0.01F;
        });

        List<Entry> entries = new ArrayList<>();
        for (Module m : allModules) {
            Animation a = anims.get(m);
            if (a != null && a.getValue() > 0.01F) {
                int key = m.getBind().getValue().get(0);
                String kName = KeyUtil.getKeyName(key);
                entries.add(new Entry(m.getName(), kName, getCategoryIcon(m.getCategory()), a.getValue()));
            }
        }

        // Preview dummy items in chat if empty
        if (entries.isEmpty() && inChat) {
            entries.add(new Entry("AttackAura", "R", Fonts.NURIK_COMBAT, 1.0F));
            entries.add(new Entry("Velocity", "V", Fonts.NURIK_MOVEMENT, 1.0F));
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Calculate dynamic width
        float maxRowW = 75.0F;
        float headerTitleW = Fonts.SF_MEDIUM.getWidth("Hotkeys", 9.0F);
        float headerMinW = 20.0F + headerTitleW + 8.0F;
        maxRowW = Math.max(maxRowW, headerMinW);

        for (Entry e : entries) {
            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 8.5F);
            float keyW = Fonts.SF_MEDIUM.getWidth(e.keyName, 8.0F);
            float rowTotalW = (16.0F + nameW + 8.0F) + 6.0F + (keyW + 12.0F);
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

        // 1. Header Capsule matching Energy HUD
        int headerBg = ColorUtil.rgba(14, 16, 22, 175);
        int outlineCol = ColorUtil.rgba(255, 255, 255, 20);

        Render2D.drawShadow(curX, curY, this.width, HEADER_H, PILL_R, 6.0F, ColorUtil.rgba(0, 0, 0, 80));
        Render2D.drawRoundedRect(curX, curY, this.width, HEADER_H, PILL_R, headerBg);
        Render2D.drawRoundedOutline(curX, curY, this.width, HEADER_H, PILL_R, 0.75F, outlineCol);

        // Header Energy Icon "p" or NURIK_BIND
        Fonts.drawString(Fonts.ENERGY, "p", curX + 6.0F, curY + 2.5F, 10.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Hotkeys", curX + 19.0F, curY + 3.0F, 9.0F, 0xFFFFFFFF);

        curY += HEADER_H + GAP_Y;

        // 2. Entries: Left capsule (Icon + Name) and Right capsule (Keybind)
        for (Entry e : entries) {
            if (e.alpha <= 0.01F) continue;

            int rowBg = ColorUtil.rgba(14, 16, 22, (int) (165 * e.alpha));
            int rowOutline = ColorUtil.rgba(255, 255, 255, (int) (18 * e.alpha));
            int textWhite = ColorUtil.rgba(255, 255, 255, (int) (245 * e.alpha));
            int keyColor = ColorUtil.rgba(215, 225, 240, (int) (235 * e.alpha));

            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 8.5F);
            float keyW = Fonts.SF_MEDIUM.getWidth(e.keyName, 8.0F);

            float leftPillW = 16.0F + nameW + 8.0F;
            float rightPillW = keyW + 12.0F;

            float rightPillX = curX + this.width - rightPillW;

            // Left Capsule
            Render2D.drawShadow(curX, curY, leftPillW, ROW_H, PILL_R, 5.0F, ColorUtil.rgba(0, 0, 0, (int) (60 * e.alpha)));
            Render2D.drawRoundedRect(curX, curY, leftPillW, ROW_H, PILL_R, rowBg);
            Render2D.drawRoundedOutline(curX, curY, leftPillW, ROW_H, PILL_R, 0.65F, rowOutline);

            // Category Icon in left capsule
            if (e.categoryIcon != null && !e.categoryIcon.equals("•")) {
                Fonts.drawString(Fonts.ICONS_NURIK, e.categoryIcon, curX + 5.5F, curY + 2.0F, 8.0F, ColorUtil.withAlpha(accent, (int) (230 * e.alpha)));
            } else {
                Render2D.drawCircle(curX + 8.0F, curY + ROW_H * 0.5F, 2.0F, ColorUtil.withAlpha(accent, (int) (230 * e.alpha)));
            }

            // Module Name
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, curX + 16.0F, curY + 2.0F, 8.5F, textWhite);

            // Right Capsule
            Render2D.drawShadow(rightPillX, curY, rightPillW, ROW_H, PILL_R, 5.0F, ColorUtil.rgba(0, 0, 0, (int) (60 * e.alpha)));
            Render2D.drawRoundedRect(rightPillX, curY, rightPillW, ROW_H, PILL_R, rowBg);
            Render2D.drawRoundedOutline(rightPillX, curY, rightPillW, ROW_H, PILL_R, 0.65F, rowOutline);

            // Key name centered in right capsule
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, e.keyName, rightPillX + rightPillW * 0.5F, curY + 2.2F, 8.0F, keyColor);

            curY += (ROW_H + GAP_Y) * e.alpha;
        }
    }
}
