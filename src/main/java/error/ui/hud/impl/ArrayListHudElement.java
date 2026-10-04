package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.Category;
import error.module.Module;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArrayListHudElement extends HudElement implements IMinecraft {

    public ArrayListHudElement() {
        super("arraylist", "ArrayList", 800.0F, 10.0F, 110.0F, 150.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        MsdfFont font = Fonts.SF_MEDIUM;
        float fontSize = 7.0F;
        float rowH = 13.0F;

        List<Module> activeModules = new ArrayList<>();
        if (Client.getInstance() != null && Client.getInstance().moduleManager != null) {
            for (Module m : Client.getInstance().moduleManager.getModules()) {
                if (m.isEnabled() && m.getCategory() != Category.RENDER) {
                    activeModules.add(m);
                }
            }
        }

        activeModules.sort((m1, m2) -> Float.compare(font.getWidth(m2.getName(), fontSize), font.getWidth(m1.getName(), fontSize)));

        if (activeModules.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            if (Client.getInstance() != null && Client.getInstance().moduleManager != null) {
                activeModules.addAll(Client.getInstance().moduleManager.getModules().stream().limit(6).toList());
            }
        }

        if (activeModules.isEmpty()) {
            this.height = 0;
            return;
        }

        float maxW = 0.0F;
        for (Module m : activeModules) {
            maxW = Math.max(maxW, font.getWidth(m.getName(), fontSize));
        }

        this.width = maxW + 14.0F;
        this.height = activeModules.size() * rowH + 4.0F;

        int accent = Theme.getAccentColor();
        float curY = y + 2.0F;

        for (int i = 0; i < activeModules.size(); i++) {
            Module m = activeModules.get(i);
            String name = m.getName();
            float textW = font.getWidth(name, fontSize);
            float rowX = x + width - textW - 10.0F;

            int bg = ColorUtil.rgba(16, 18, 26, 210);
            int barCol = ColorUtil.lerp(accent, ColorUtil.rgba(255, 255, 255, 255), (float) i / Math.max(1, activeModules.size()));

            Render2D.drawBlur(rowX - 2.0F, curY, textW + 10.0F, rowH - 1.0F, 3.5F, bg, 1.0F);
            Render2D.drawRoundedRect(rowX - 2.0F, curY, textW + 10.0F, rowH - 1.0F, 3.5F, bg);
            Render2D.drawRoundedRect(rowX + textW + 6.0F, curY, 2.0F, rowH - 1.0F, 1.0F, barCol);

            Fonts.drawString(font, name, rowX + 1.0F, curY + (rowH - font.lineHeight(fontSize)) / 2.0F, fontSize, ColorUtil.rgba(245, 245, 250, 255));
            curY += rowH;
        }
    }
}
