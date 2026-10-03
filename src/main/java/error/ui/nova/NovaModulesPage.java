package error.ui.nova;

import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.ui.modern.ModernAnim;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.KeyUtil;
import error.util.render.Render2D;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NovaModulesPage — Компактная верстка для Nova ClickGUI с полным RGB Color Picker (полный выбор всех цветов HSB/RGB).
 */
public final class NovaModulesPage {

    private static final Set<Setting<?>> expanded = new HashSet<>();

    private NovaModulesPage() {}

    public static void render(NovaGui g, float x, float y, float w, float h) {
        float splitFrac = 0.36F;
        float leftW  = w * splitFrac;
        float rightW = w - leftW - 6.0F;
        float rightX = x + leftW + 6.0F;

        renderModuleList(g, x, y, leftW, h);
        renderModuleSettings(g, rightX, y, rightW, h);
    }

    // ─────────────────────────────────────────────────────────────
    //  1. ЛЕВАЯ КОЛОНКА — СПИСОК МОДУЛЕЙ
    // ─────────────────────────────────────────────────────────────
    private static void renderModuleList(NovaGui g, float x, float y, float w, float h) {
        g.rect(x, y, w, h, 7.0F, ColorUtil.rgba(12, 14, 22, (int)(75 * g.alpha)));
        g.outline(x, y, w, h, 7.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));

        float titleH = 18.0F;
        String title = g.searchQuery.isBlank() ? g.activeSection.label : "Поиск";

        g.text(g.font(7.5F), title, x + 8.0F, y + 3.5F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

        try {
            Render2D.drawTexture("textures/system/separator1.png", x + 6.0F, y + titleH - 2.0F, w - 12.0F, 2.0F, 0.0F, ColorUtil.rgba(255, 255, 255, (int)(25 * g.alpha)));
        } catch (Throwable ignored) {
            g.rect(x + 6.0F, y + titleH - 1.0F, w - 12.0F, 0.5F, 0.0F, NovaTheme.DIVIDER());
        }

        List<Module> modules = g.getModulesForSection(g.activeSection);
        float listY   = y + titleH;
        float listH   = h - titleH;
        float itemH   = 17.0F;
        float itemGap = 2.0F;

        g.pushClip(x, listY, w, listH);

        float curY = listY - g.scroll;

        if (modules.isEmpty()) {
            g.text(g.font(6.5F), "ничего не найдено", x + 8.0F, listY + 10.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));
        } else {
            int accent = Theme.getAccentColor();
            int ar     = ColorUtil.red(accent);
            int ag     = ColorUtil.green(accent);
            int ab     = ColorUtil.blue(accent);

            for (Module m : modules) {
                if (curY + itemH > listY && curY < listY + listH) {
                    boolean isSelected = (g.selectedModule == m);
                    boolean isEnabled  = m.isEnabled();
                    boolean hovered    = g.hovered(x + 4.0F, curY, w - 8.0F, itemH);

                    float hAnim = ModernAnim.value("nova_ml:" + m.getName(), isSelected ? 1.0F : (hovered ? 0.5F : 0.0F), 13.0F);

                    int bgCol = isEnabled
                            ? ColorUtil.rgba(ar, ag, ab, (int)((18 + 32 * hAnim) * g.alpha))
                            : ColorUtil.rgba(255, 255, 255, (int)((6 + 14 * hAnim) * g.alpha));
                    g.rect(x + 4.0F, curY, w - 8.0F, itemH, 5.0F, bgCol);

                    int borderAlpha = isSelected ? 35 : (hovered ? 16 : 8);
                    g.outline(x + 4.0F, curY, w - 8.0F, itemH, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(borderAlpha * g.alpha)));

                    if (isEnabled) {
                        g.rect(x + 6.0F, curY + 3.5F, 2.0F, itemH - 7.0F, 1.0F, ColorUtil.multiplyAlpha(accent, g.alpha));
                    }

                    int textCol;
                    if (isSelected && isEnabled)       textCol = accent;
                    else if (isSelected)               textCol = NovaTheme.TEXT();
                    else if (isEnabled)                textCol = accent;
                    else                               textCol = ColorUtil.lerp(NovaTheme.TEXT_MUTED(), NovaTheme.TEXT_SEC(), hAnim);

                    // Интерактивный тумблер переключателя на правом краю элемента модуля
                    float togW = 15.0F;
                    float togH = 8.0F;
                    float togX = x + w - 10.0F - togW;
                    float togY = curY + (itemH - togH) / 2.0F;

                    g.toggle(togX, togY, isEnabled, "nova_ml_tgl:" + m.getName());

                    var font = g.font(6.2F);
                    float textOff = isEnabled ? 11.0F : 8.0F;
                    float maxTextW = Math.max(10.0F, togX - (x + textOff) - 4.0F);
                    g.text(font, g.clip(font, m.getName(), maxTextW), x + textOff, g.textY(curY, itemH, font), ColorUtil.multiplyAlpha(textCol, g.alpha));

                    final Module captured = m;
                    g.hit(x + 4.0F, curY, w - 8.0F, itemH, btn -> {
                        g.selectedModule = captured;
                        if (btn == 0) {
                            // ЛКМ: вкл/выкл модуля через переключатель
                            captured.toggle();
                        } else if (btn == 2) {
                            // СКМ (Колесико мыши): открывает прикольное контекстное меню модуля!
                            g.openContextMenu(captured, g.mouseX, g.mouseY);
                        }
                        // ПКМ (btn == 1): выбор и открытие настроек модуля
                        return true;
                    });
                }
                curY += itemH + itemGap;
            }
        }

        g.popClip();
        float totalH = modules.size() * (itemH + itemGap);
        g.maxScroll = Math.max(0.0F, totalH - listH);
    }

    // ─────────────────────────────────────────────────────────────
    //  2. ПРАВАЯ ПАНЕЛЬ — НАСТРОЙКИ ВЫБРАННОГО МОДУЛЯ
    // ─────────────────────────────────────────────────────────────
    private static void renderModuleSettings(NovaGui g, float x, float y, float w, float h) {
        Module module = g.selectedModule;

        g.rect(x, y, w, h, 7.0F, ColorUtil.rgba(12, 14, 22, (int)(65 * g.alpha)));
        g.outline(x, y, w, h, 7.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));

        if (module == null) {
            g.text(g.font(7.5F), "Выберите модуль для настройки", x + 10.0F, y + 14.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));
            return;
        }

        float cardH = 30.0F;
        g.rect(x + 5.0F, y + 5.0F, w - 10.0F, cardH, 6.0F, ColorUtil.rgba(16, 18, 28, (int)(150 * g.alpha)));
        g.outline(x + 5.0F, y + 5.0F, w - 10.0F, cardH, 6.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(20 * g.alpha)));

        g.text(g.font(8.5F), module.getName(), x + 10.0F, y + 5.5F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

        String desc = module.getDescription() != null ? module.getDescription() : "Параметры и конфигурация";
        var df = g.font(6.2F);
        g.text(df, g.clip(df, desc, w - 80.0F), x + 10.0F, y + 17.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

        float togX = x + w - 28.0F;
        float togY = y + 11.0F;
        g.toggle(togX, togY, module.isEnabled(), "nova_tgl:" + module.getName());
        g.hit(togX, togY, 16.0F, 8.0F, btn -> { module.toggle(); return true; });

        if (module.getBind() != null) {
            String bindStr = g.isBinding(module) ? "..." : (module.getBind().isEmpty() ? "NONE" : KeyUtil.getKeyName(module.getBind().get(0)));
            var bf = g.font(6.0F);
            g.textRight(bf, bindStr, togX - 5.0F, g.textY(y + 5.0F, cardH, bf), ColorUtil.multiplyAlpha(NovaTheme.TEXT_FAINT(), g.alpha));
            g.hit(togX - 35.0F, y + 6.0F, 28.0F, 18.0F, btn -> { g.startBind(module); return true; });
        }

        float settingsY = y + cardH + 9.0F;
        float settingsH = h - (cardH + 9.0F);
        List<Setting<?>> settings = module.getSettings();

        g.pushClip(x + 5.0F, settingsY, w - 10.0F, settingsH);

        float gapCol = 5.0F;
        float cardW  = (w - 10.0F - gapCol) / 2.0F;
        float leftY  = settingsY - g.rightScroll;
        float rightY = settingsY - g.rightScroll;

        for (Setting<?> s : settings) {
            if (s == null || !s.isVisible()) continue;
            float sh = settingHeight(s);
            if (leftY <= rightY) {
                renderCard(g, s, x + 5.0F, leftY, cardW, sh);
                leftY += sh + 4.0F;
            } else {
                renderCard(g, s, x + 5.0F + cardW + gapCol, rightY, cardW, sh);
                rightY += sh + 4.0F;
            }
        }

        g.popClip();
        float totalH = Math.max(leftY, rightY) - (settingsY - g.rightScroll);
    }

    private static float settingHeight(Setting<?> s) {
        if (s instanceof ModeSetting mode) {
            float e = ModernAnim.get("drop:" + System.identityHashCode(mode), 0.0F);
            return 14.0F + (mode.getModes().size() * 10.5F + 2.0F) * Mth.clamp(e, 0.0F, 1.0F);
        }
        if (s instanceof MultiModeSetting multi) {
            float e = ModernAnim.get("drop:" + System.identityHashCode(multi), 0.0F);
            return 14.0F + (multi.getModes().size() * 10.5F + 2.0F) * Mth.clamp(e, 0.0F, 1.0F);
        }
        return 14.0F;
    }

    private static void renderCard(NovaGui g, Setting<?> s, float x, float y, float w, float h) {
        g.rect(x, y, w, h, 5.0F, ColorUtil.rgba(18, 20, 30, (int)(110 * g.alpha)));
        g.outline(x, y, w, h, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(14 * g.alpha)));
        float px = x + 4.0F;
        float pw = w - 8.0F;

        if      (s instanceof SliderSetting    sl) renderSlider   (g, sl, px, y, pw);
        else if (s instanceof CheckBox         cb) renderCheckbox (g, cb, px, y, pw);
        else if (s instanceof ModeSetting     mode) renderMode    (g, mode, px, y, pw);
        else if (s instanceof MultiModeSetting mm) renderMultiMode(g, mm, px, y, pw);
        else if (s instanceof BindSetting     bind) renderBind    (g, bind, px, y, pw);
        else if (s instanceof ColorSetting    col)  renderColor   (g, col, px, y, pw);
    }

    private static void renderSlider(NovaGui g, SliderSetting s, float x, float y, float w) {
        var nf  = g.font(6.0F);
        var vf  = g.font(5.5F);
        String valStr = String.valueOf(s.getValue());
        float  nameW  = nf.font().getWidth(s.getName(), nf.size());
        float  valW   = vf.font().getWidth(valStr, vf.size());

        g.text(nf, s.getName(), x, g.textY(y, 14.0F, nf), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        float valX = x + w - valW;
        g.text(vf, valStr, valX, g.textY(y, 14.0F, vf), ColorUtil.multiplyAlpha(Theme.getAccentColor(), g.alpha));

        float sliderX = x + nameW + 5.0F;
        float sliderW = (valX - 5.0F) - sliderX;
        float min = s.getMin(), max = s.getMax();
        float frac = (max - min) <= 0.0F ? 0.0F : (s.getValue() - min) / (max - min);

        if (sliderW > 8.0F)
            g.slider(sliderX, y + 5.5F, sliderW, frac, s, frc -> {
                float nv   = min + (float) frc * (max - min);
                float step = s.getStep();
                s.setValue(step > 0.0F ? Math.round(nv / step) * step : nv);
            });
    }

    private static void renderCheckbox(NovaGui g, CheckBox s, float x, float y, float w) {
        boolean hov = g.hovered(x, y, w, 14.0F);
        var f = g.font(6.0F);
        g.text(f, g.clip(f, s.getName(), w - 20.0F), x, g.textY(y, 14.0F, f), ColorUtil.multiplyAlpha(hov ? NovaTheme.TEXT() : NovaTheme.TEXT_SEC(), g.alpha));
        float tx = x + w - 16.0F;
        float ty = y + (14.0F - 8.0F) / 2.0F;
        g.toggle(tx, ty, s.getValue(), s);
        g.hit(x, y, w, 14.0F, btn -> { s.setValue(!s.getValue()); return true; });
    }

    private static void renderMode(NovaGui g, ModeSetting s, float x, float y, float w) {
        String key  = "drop:" + System.identityHashCode(s);
        boolean exp = expanded.contains(s);
        float   t   = Mth.clamp(ModernAnim.value(key, exp ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var f  = g.font(6.0F);
        var sf = g.font(5.5F);
        String cur  = s.getValue();
        float  pillW = Math.min(w * 0.55F, sf.font().getWidth(cur, sf.size()) + 12.0F);

        g.text(f, g.clip(f, s.getName(), w - pillW - 4.0F), x, g.textY(y, 14.0F, f), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        float px = x + w - pillW;
        g.rect(px, y + 1.5F, pillW, 10.5F, 3.0F, ColorUtil.rgba(255, 255, 255, (int)(12 * g.alpha)));
        g.outline(px, y + 1.5F, pillW, 10.5F, 3.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(18 * g.alpha)));
        g.text(sf, g.clip(sf, cur, pillW - 10.0F), px + 3.0F, g.textY(y + 1.5F, 10.5F, sf), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        g.hit(x, y, w, 14.0F, btn -> { if (expanded.contains(s)) expanded.remove(s); else expanded.add(s); return true; });

        if (t > 0.01F) {
            float dH = (s.getModes().size() * 10.5F + 2.0F) * t;
            g.pushClip(x, y + 14.0F, w, dH);
            float oy = y + 14.0F + 1.5F;
            for (String mode : s.getModes()) {
                boolean active = s.is(mode);
                boolean hov    = g.hovered(x, oy, w, 10.5F);
                float   ma     = ModernAnim.value("mv:" + System.identityHashCode(s) + mode, active ? 1.0F : (hov ? 0.5F : 0.0F), 14.0F);
                if (ma > 0.01F)
                    g.rect(x, oy, w, 10.5F, 3.0F, ColorUtil.multiplyAlpha(NovaTheme.SIDEBAR_ACTIVE(), ma));
                g.rect(x + 3.0F, oy + 4.2F, 2.0F, 2.0F, 1.0F, ColorUtil.multiplyAlpha(active ? Theme.getAccentColor() : NovaTheme.TEXT_FAINT(), g.alpha));
                g.text(sf, g.clip(sf, mode, w - 12.0F), x + 8.0F, g.textY(oy, 10.5F, sf), ColorUtil.multiplyAlpha(active ? Theme.getAccentColor() : NovaTheme.TEXT_SEC(), g.alpha));
                g.hit(x, oy, w, 10.5F, btn -> { s.setValue(mode); expanded.remove(s); return true; });
                oy += 10.5F;
            }
            g.popClip();
        }
    }

    private static void renderMultiMode(NovaGui g, MultiModeSetting s, float x, float y, float w) {
        String key  = "drop:" + System.identityHashCode(s);
        boolean exp = expanded.contains(s);
        float   t   = Mth.clamp(ModernAnim.value(key, exp ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var f  = g.font(6.0F);
        var sf = g.font(5.5F);
        String disp = s.getValue() != null ? String.join(", ", s.getValue()) : "";
        float  pillW = Math.min(w * 0.55F, sf.font().getWidth(disp, sf.size()) + 12.0F);

        g.text(f, g.clip(f, s.getName(), w - pillW - 4.0F), x, g.textY(y, 14.0F, f), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        float px = x + w - pillW;
        g.rect(px, y + 1.5F, pillW, 10.5F, 3.0F, ColorUtil.rgba(255, 255, 255, (int)(12 * g.alpha)));
        g.outline(px, y + 1.5F, pillW, 10.5F, 3.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(18 * g.alpha)));
        g.text(sf, g.clip(sf, disp, pillW - 10.0F), px + 3.0F, g.textY(y + 1.5F, 10.5F, sf), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        g.hit(x, y, w, 14.0F, btn -> { if (expanded.contains(s)) expanded.remove(s); else expanded.add(s); return true; });

        if (t > 0.01F && s.getModes() != null) {
            float dH = (s.getModes().size() * 10.5F + 2.0F) * t;
            g.pushClip(x, y + 14.0F, w, dH);
            float oy = y + 14.0F + 1.5F;
            for (String mode : s.getModes()) {
                boolean active = s.isEnabled(mode);
                boolean hov    = g.hovered(x, oy, w, 10.5F);
                float   ma     = ModernAnim.value("mmv:" + System.identityHashCode(s) + mode, active ? 1.0F : (hov ? 0.5F : 0.0F), 14.0F);
                if (ma > 0.01F)
                    g.rect(x, oy, w, 10.5F, 3.0F, ColorUtil.multiplyAlpha(NovaTheme.SIDEBAR_ACTIVE(), ma));
                g.rect(x + 3.0F, oy + 4.2F, 2.0F, 2.0F, 1.0F, ColorUtil.multiplyAlpha(active ? Theme.getAccentColor() : NovaTheme.TEXT_FAINT(), g.alpha));
                g.text(sf, g.clip(sf, mode, w - 12.0F), x + 8.0F, g.textY(oy, 10.5F, sf), ColorUtil.multiplyAlpha(active ? Theme.getAccentColor() : NovaTheme.TEXT_SEC(), g.alpha));
                g.hit(x, oy, w, 10.5F, btn -> { s.toggle(mode); return true; });
                oy += 10.5F;
            }
            g.popClip();
        }
    }

    private static void renderBind(NovaGui g, BindSetting s, float x, float y, float w) {
        boolean binding = g.isBinding(s);
        String  bindStr = binding ? "..." : (s.isEmpty() ? "NONE" : KeyUtil.getKeyName(s.get(0)));
        var f   = g.font(6.0F);
        var sf  = g.font(5.5F);
        float bw = sf.font().getWidth(bindStr, sf.size()) + 7.0F;

        g.text(f, g.clip(f, s.getName(), w - bw - 4.0F), x, g.textY(y, 14.0F, f), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        float bx = x + w - bw;
        g.rect(bx, y + 1.5F, bw, 10.5F, 3.0F, ColorUtil.rgba(255, 255, 255, (int)(12 * g.alpha)));
        g.text(sf, bindStr, bx + 3.5F, g.textY(y + 1.5F, 10.5F, sf), ColorUtil.multiplyAlpha(binding ? Theme.getAccentColor() : NovaTheme.TEXT_SEC(), g.alpha));
        g.hit(x, y, w, 14.0F, btn -> { g.startBind(s); return true; });
    }

    private static void renderColor(NovaGui g, ColorSetting s, float x, float y, float w) {
        var f = g.font(6.0F);
        g.text(f, g.clip(f, s.getName(), w - 16.0F), x, g.textY(y, 14.0F, f), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        g.rect(x + w - 11.0F, y + 2.5F, 9.0F, 8.0F, 2.0F, ColorUtil.multiplyAlpha(s.getValue(), g.alpha));
        g.outline(x + w - 11.0F, y + 2.5F, 9.0F, 8.0F, 2.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(22 * g.alpha)));
    }
}
