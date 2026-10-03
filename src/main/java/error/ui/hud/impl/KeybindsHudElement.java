package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.Module;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;

import java.util.ArrayList;
import java.util.List;

public final class KeybindsHudElement extends HudElement implements IMinecraft {

    private static record PreviewBind(String name, String key) {}

    private static final List<PreviewBind> PREVIEW_BINDS = List.of(
            new PreviewBind("AttackAura", "R"),
            new PreviewBind("TargetStrafe", "V"),
            new PreviewBind("AutoTotem", "G"),
            new PreviewBind("Velocity", "M5")
    );

    public KeybindsHudElement() {
        super("keybinds", "Keybinds", 710.0F, 340.0F, 135.0F, 100.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        MsdfFont font = Fonts.SF_MEDIUM;
        float fontUnit = 0.44F;

        List<PreviewBind> activeBinds = new ArrayList<>();

        if (Client.getInstance() != null && Client.getInstance().moduleManager != null) {
            for (Module m : Client.getInstance().moduleManager.getModules()) {
                if (m.isEnabled() && m.getBind() != null && m.getBind().isBound()) {
                    activeBinds.add(new PreviewBind(m.getName(), m.getBind().getDisplayString()));
                }
            }
        }

        if (activeBinds.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            activeBinds.addAll(PREVIEW_BINDS);
        }

        if (activeBinds.isEmpty()) {
            this.height = 0;
            return;
        }

        float padding = 8.0F;
        float headerH = 20.0F;
        float itemH = 16.0F;
        float totalH = headerH + activeBinds.size() * itemH + padding;
        float maxW = 140.0F;

        this.width = maxW;
        this.height = totalH;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 100);

        Render2D.drawBlur(x, y, width, height, 8.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 8.0F, bgColor, 1.0F, outlineColor);

        // Header
        Fonts.drawString(font, "Keybinds", x + 10.0F, y + 5.0F, 0.48F, ColorUtil.rgba(240, 240, 250, 255));
        Render2D.drawRect(x + 10.0F, y + headerH - 2.0F, width - 20.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 30));

        float curY = y + headerH + 2.0F;
        for (PreviewBind bind : activeBinds) {
            // Module Name
            Fonts.drawString(font, bind.name(), x + 10.0F, curY + (itemH - font.lineHeight(fontUnit)) / 2.0F, fontUnit, ColorUtil.rgba(230, 230, 230, 255));

            // Key + badge
            String keyStr = bind.key() + "  [x]";
            float keyW = font.getWidth(keyStr, fontUnit);
            Fonts.drawString(font, keyStr, x + width - 10.0F - keyW, curY + (itemH - font.lineHeight(fontUnit)) / 2.0F, fontUnit, ColorUtil.rgba(180, 180, 200, 255));

            curY += itemH;
        }
    }
}
