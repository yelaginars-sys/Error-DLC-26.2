package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.Module;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.*;

public final class KeybindsHud extends HudElement implements IMinecraft {

    private final Animation heightAnim = new Animation(26.0F, 0.20F);
    private final Map<Module, Animation[]> itemAnims = new LinkedHashMap<>();

    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ITEM_SPACING = 11.0F;
    private static final float RADIUS = 6.0F;
    private static final float ITEM_FONT_SIZE = 6.0F;
    private static final float HEADER_FONT_SIZE = 7.5F;

    public KeybindsHud() {
        super("keybinds", "Hotkeys", 6.0F, 38.0F, 85.0F, 26.0F, true);
    }

    public com.google.gson.JsonObject writeConfig() {
        return new com.google.gson.JsonObject();
    }

    public void readConfig(com.google.gson.JsonObject json) {
    }

    @Override
    public void draw(Render2DEvent event) {
        List<Module> boundModules = new ArrayList<>();
        if (Client.getInstance() != null && Client.getInstance().getModuleManager() != null) {
            for (Module m : Client.getInstance().getModuleManager().getModules()) {
                if (m.isEnabled() && m.getBind() != null && m.getBind().isBound()) {
                    boundModules.add(m);
                }
            }
        }

        boolean editing = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        fadeAnim.setTarget((!boundModules.isEmpty() || editing) ? 1.0F : 0.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();

        if (alpha <= 0.01F && boundModules.isEmpty() && itemAnims.isEmpty()) {
            heightAnim.setTarget(15.0F);
            heightAnim.update();
            return;
        }

        float x = getX();
        float y = getY();
        int themeAccent = Theme.getAccentColor();

        float itemFs = ITEM_FONT_SIZE;
        float headerFs = HEADER_FONT_SIZE;
        float hh = HEADER_HEIGHT;
        float is = ITEM_SPACING;
        float r = RADIUS;
        String title = "Hotkeys";

        List<Module> toRemove = new ArrayList<>();
        List<Module> exiting = new ArrayList<>();
        for (Module m : itemAnims.keySet()) {
            if (!boundModules.contains(m)) {
                Animation[] a = itemAnims.get(m);
                if (a[0].getTarget() != 0.0F) {
                    a[0].setTarget(0.0F);
                    a[1].setTarget(-5.0F);
                }
                a[0].update();
                a[1].update();
                if (a[0].getValue() <= 0.02F) toRemove.add(m);
                else exiting.add(m);
            }
        }
        toRemove.forEach(itemAnims::remove);

        for (Module m : boundModules) {
            Animation[] a = itemAnims.computeIfAbsent(m, k -> new Animation[]{new Animation(0.0F, 0.20F), new Animation(-5.0F, 0.20F)});
            if (a[0].getTarget() != 1.0F) {
                a[0].setTarget(1.0F);
                a[1].setTarget(0.0F);
            }
            a[0].update();
            a[1].update();
        }

        float titleW = Fonts.SF_MEDIUM.getWidth(title, headerFs);
        float maxBindWidth = 0;
        float maxNameWidth = 0;
        for (Module m : boundModules) {
            maxBindWidth = Math.max(maxBindWidth, Fonts.SF_MEDIUM.getWidth(m.getBind().getDisplayValue(), itemFs));
            maxNameWidth = Math.max(maxNameWidth, Fonts.SF_MEDIUM.getWidth(m.getName(), itemFs));
        }
        for (Module m : exiting) {
            maxBindWidth = Math.max(maxBindWidth, Fonts.SF_MEDIUM.getWidth(m.getBind().getDisplayValue(), itemFs));
            maxNameWidth = Math.max(maxNameWidth, Fonts.SF_MEDIUM.getWidth(m.getName(), itemFs));
        }
        float maxW = Math.max(70.0F, maxNameWidth + maxBindWidth + 22.0F);
        float headerTitleW = titleW + 6.0F;
        if (headerTitleW > maxW) maxW = headerTitleW;

        int totalVisible = boundModules.size() + exiting.size();
        float targetHeight = Math.max(hh, hh + 1.5F + totalVisible * is);
        heightAnim.setTarget(targetHeight);
        heightAnim.update();
        float h = heightAnim.getValue();

        this.width = maxW;
        this.height = h;

        // Liquid glass background with blur and specular outline
        Render2D.drawShadow(x, y, maxW, h, r, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alpha)));
        int glassFill = ColorUtil.rgba(20, 18, 28, (int) (160 * alpha));
        Render2D.drawBlur(x, y, maxW, h, r, 12.0F, glassFill, alpha);
        Render2D.drawRoundedRect(x, y, maxW, h, r, glassFill);
        Render2D.drawRoundedOutline(x, y, maxW, h, r, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)));

        int hc = (int) (alpha * 255.0F);
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 6.0F, y + 4.0F, headerFs, ColorUtil.withAlpha(themeAccent, hc));

        float base = y + hh;
        int idx = 0;

        for (Module m : exiting) {
            Animation[] a = itemAnims.get(m);
            float ia = alpha * a[0].getValue();
            if (ia <= 0.01F) continue;
            float xo = a[1].getValue();
            int c = (int) (ia * 255.0F);

            String bind = m.getBind().getDisplayValue();
            float bw = Fonts.SF_MEDIUM.getWidth(bind, itemFs);
            float bx = x + maxW - bw - 7.5F + xo;
            Render2D.drawRoundedRect(bx, base + idx * is + 1.0F, bw + 6.0F, 8.0F, 2.0F, ColorUtil.rgba(0, 0, 0, (int) (ia * 60)));
            Render2D.drawRoundedOutline(bx - 0.5F, base + idx * is + 0.5F, bw + 7.0F, 9.0F, 2.5F, 1.0F, ColorUtil.withAlpha(themeAccent, (int) (ia * 30)));
            Fonts.drawString(Fonts.SF_MEDIUM, m.getName(), x + 3.5F + xo, base + idx * is + 3.5F, itemFs, ColorUtil.rgba(235, 235, 235, c));
            Fonts.drawString(Fonts.SF_MEDIUM, bind, x + maxW - bw - 4.5F + xo, base + idx * is + 3.0F, 5.5F, ColorUtil.rgba(235, 235, 235, c));
            idx++;
        }

        for (Module m : boundModules) {
            Animation[] a = itemAnims.get(m);
            if (a == null) continue;
            float ia = alpha * a[0].getValue();
            if (ia <= 0.01F) continue;
            float xo = a[1].getValue();
            int c = (int) (ia * 255.0F);

            String bind = m.getBind().getDisplayValue();
            float bw = Fonts.SF_MEDIUM.getWidth(bind, itemFs);
            float bx = x + maxW - bw - 7.5F + xo;
            Render2D.drawRoundedRect(bx, base + idx * is + 1.0F, bw + 6.0F, 8.0F, 2.0F, ColorUtil.rgba(0, 0, 0, (int) (ia * 60)));
            Render2D.drawRoundedOutline(bx - 0.5F, base + idx * is + 0.5F, bw + 7.0F, 9.0F, 2.5F, 1.0F, ColorUtil.withAlpha(themeAccent, (int) (ia * 30)));
            Fonts.drawString(Fonts.SF_MEDIUM, m.getName(), x + 3.5F + xo, base + idx * is + 3.5F, itemFs, ColorUtil.rgba(235, 235, 235, c));
            Fonts.drawString(Fonts.SF_MEDIUM, bind, x + maxW - bw - 4.5F + xo, base + idx * is + 3.0F, 5.5F, ColorUtil.rgba(235, 235, 235, c));
            idx++;
        }
    }
}