package error.ui.hud.impl;

import error.Client;
import error.event.list.Render2DEvent;
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

public final class KeyBindsHud extends HudElement {

    private final Map<Module, Animation> anims = new HashMap<>();
    private final Animation totalWidthAnim = new Animation(60.0F, 0.20F);
    private final Animation totalHeightAnim = new Animation(15.0F, 0.20F);

    private static final float ROW_H = 13.0F;
    private static final float HEADER_H = 14.0F;
    private static final float GAP_X = 2.0F;
    private static final float GAP_Y = 2.5F;

    public KeyBindsHud() {
        super("keybinds", "Binds", 10.0F, 100.0F, 80.0F, 40.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.binds.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean inChat = error.IMinecraft.mc.gui != null && error.IMinecraft.mc.gui.screen() instanceof ChatScreen;

        List<Module> activeBoundModules = new ArrayList<>();
        if (Client.getInstance() != null && Client.getInstance().getModuleManager() != null) {
            for (Module m : Client.getInstance().getModuleManager().getModules()) {
                BindSetting bs = m.getBind();
                if (bs != null && !bs.getValue().isEmpty()) {
                    int primaryKey = bs.getValue().get(0);
                    if (BindSetting.isValidKey(primaryKey)) {
                        Animation anim = anims.computeIfAbsent(m, k -> new Animation(0.0F, 0.20F));
                        anim.setTarget(m.isState() ? 1.0F : 0.0F);
                        anim.update();
                        if (anim.getValue() > 0.01F) {
                            activeBoundModules.add(m);
                        }
                    }
                }
            }
        }

        // Cleanup
        anims.entrySet().removeIf(e -> {
            BindSetting bs = e.getKey().getBind();
            return (bs == null || bs.getValue().isEmpty() || !BindSetting.isValidKey(bs.getValue().get(0)))
                    && e.getValue().getValue() <= 0.01F;
        });

        int accent = Theme.getAccentColor();

        float headerIconW = Fonts.getIconWidth(IconUse.KEYBOARD, 9.0F);
        float headerTextW = Fonts.SF_MEDIUM.getWidth("Binds", 9.5F);
        float headerW = 6.0F + headerIconW + 4.0F + headerTextW + 7.0F;

        if (activeBoundModules.isEmpty()) {
            if (!inChat) {
                this.width = 0;
                this.height = 0;
                return;
            }

            // Preview in ChatScreen
            Render2D.drawHudPill(this.x, this.y, headerW, HEADER_H, 1.0F);
            Fonts.drawIcon(IconUse.KEYBOARD, this.x + 6.0F, this.y + 2.5F, 9.0F, accent);
            Fonts.drawString(Fonts.SF_MEDIUM, "Binds", this.x + 6.0F + headerIconW + 4.0F, this.y + 2.5F, 9.5F, 0xFFFFFFFF);

            // Dummy row in ChatScreen
            float rowY = this.y + HEADER_H + GAP_Y;
            String dummyKey = "» V";
            String dummyName = "Velocity";
            float keyW = Fonts.SF_MEDIUM.getWidth(dummyKey, 9.0F) + 10.0F;
            float nameW = Fonts.SF_MEDIUM.getWidth(dummyName, 9.0F) + 10.0F;

            Render2D.drawHudPill(this.x, rowY, keyW, ROW_H, 0.7F);
            Fonts.drawString(Fonts.SF_MEDIUM, "»", this.x + 5.0F, rowY + 2.0F, 9.0F, accent);
            Fonts.drawString(Fonts.SF_MEDIUM, "V", this.x + 5.0F + Fonts.SF_MEDIUM.getWidth("» ", 9.0F), rowY + 2.0F, 9.0F, 0xFFFFFFFF);

            Render2D.drawHudPill(this.x + keyW + GAP_X, rowY, nameW, ROW_H, 0.7F);
            Fonts.drawString(Fonts.SF_MEDIUM, dummyName, this.x + keyW + GAP_X + 5.0F, rowY + 2.0F, 9.0F, 0xFFFFFFFF);

            this.width = Math.max(headerW, keyW + GAP_X + nameW);
            this.height = HEADER_H + GAP_Y + ROW_H;
            return;
        }

        // Calculate max bounds
        float maxRowW = headerW;
        float totalH = HEADER_H;

        for (Module m : activeBoundModules) {
            Animation anim = anims.get(m);
            float a = anim != null ? anim.getValue() : 1.0F;
            int key = m.getBind().getValue().get(0);
            String keyName = KeyUtil.getKeyName(key);
            String keyStr = "» " + keyName;
            float kw = Fonts.SF_MEDIUM.getWidth(keyStr, 9.0F) + 10.0F;
            float nw = Fonts.SF_MEDIUM.getWidth(m.getName(), 9.0F) + 10.0F;
            float rowW = kw + GAP_X + nw;
            if (rowW > maxRowW) maxRowW = rowW;
            totalH += (ROW_H + GAP_Y) * a;
        }

        totalWidthAnim.setTarget(maxRowW);
        totalWidthAnim.update();
        totalHeightAnim.setTarget(totalH);
        totalHeightAnim.update();

        this.width = totalWidthAnim.getValue();
        this.height = totalHeightAnim.getValue();

        // 1. Draw Header Pill
        Render2D.drawHudPill(this.x, this.y, headerW, HEADER_H, 1.0F);
        Fonts.drawIcon(IconUse.KEYBOARD, this.x + 6.0F, this.y + 2.5F, 9.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Binds", this.x + 6.0F + headerIconW + 4.0F, this.y + 2.5F, 9.5F, 0xFFFFFFFF);

        // 2. Draw Active Rows
        float currY = this.y + HEADER_H + GAP_Y;
        for (Module m : activeBoundModules) {
            Animation anim = anims.get(m);
            float a = anim != null ? anim.getValue() : 1.0F;
            if (a <= 0.01F) continue;

            int key = m.getBind().getValue().get(0);
            String keyName = KeyUtil.getKeyName(key);
            String keyStr = "» " + keyName;
            float kw = Fonts.SF_MEDIUM.getWidth(keyStr, 9.0F) + 10.0F;
            float nw = Fonts.SF_MEDIUM.getWidth(m.getName(), 9.0F) + 10.0F;

            int textColor = ColorUtil.rgba(255, 255, 255, (int) (255 * a));
            int accentAlpha = ColorUtil.withAlpha(accent, (int) (255 * a));

            // Left Pill: [ » KEY ]
            Render2D.drawHudPill(this.x, currY, kw, ROW_H, a);
            Fonts.drawString(Fonts.SF_MEDIUM, "»", this.x + 5.0F, currY + 2.0F, 9.0F, accentAlpha);
            Fonts.drawString(Fonts.SF_MEDIUM, keyName, this.x + 5.0F + Fonts.SF_MEDIUM.getWidth("» ", 9.0F), currY + 2.0F, 9.0F, textColor);

            // Right Pill: [ ModuleName ]
            Render2D.drawHudPill(this.x + kw + GAP_X, currY, nw, ROW_H, a);
            Fonts.drawString(Fonts.SF_MEDIUM, m.getName(), this.x + kw + GAP_X + 5.0F, currY + 2.0F, 9.0F, textColor);

            currY += (ROW_H + GAP_Y) * a;
        }
    }
}
