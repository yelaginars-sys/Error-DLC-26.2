package error.ui.shadertoy;

import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.ui.modern.ModernAnim;
import error.ui.modern.ModernTheme;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.KeyUtil;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShaderToyModulesPage {

    private static final Set<Setting<?>> expandedSettings = new HashSet<>();

    private static boolean isExpanded(Setting<?> setting) {
        return expandedSettings.contains(setting);
    }

    private static void toggleExpanded(Setting<?> setting) {
        if (!expandedSettings.remove(setting)) {
            expandedSettings.add(setting);
        }
    }

    private static void setExpanded(Setting<?> setting, boolean expanded) {
        if (expanded) {
            expandedSettings.add(setting);
        } else {
            expandedSettings.remove(setting);
        }
    }

    // --- 1. LEFT COLUMN: MODULES LIST ---
    public static void renderLeftColumn(ShaderToyGui g, float x, float y, float w, float h) {
        Category activeCategory = g.activeCategory;
        String title = g.searchQuery.isBlank() ? activeCategory.getDisplayName() : "Поиск";

        var headerFont = g.font(8.5F);
        g.text(headerFont, title, x + 2.0F, y + 2.0F, ColorUtil.rgba(255, 255, 255, 255));

        float listY = y + 16.0F;
        float listH = h - 16.0F;

        List<Module> modules = g.getModulesForCategory(activeCategory, g.searchQuery);

        g.pushClip(x, listY, w, listH);

        float itemY = listY - g.leftScroll;
        float itemH = 16.0F;

        if (modules.isEmpty()) {
            g.text(g.font(7.5F), "ничего не найдено", x + 2.0F, listY + 10.0F, ColorUtil.rgba(120, 125, 140, 255));
        } else {
            int accent = Theme.getAccentColor();
            int r = ColorUtil.red(accent);
            int gr = ColorUtil.green(accent);
            int b = ColorUtil.blue(accent);

            for (Module m : modules) {
                if (itemY + itemH > listY && itemY < listY + listH) {
                    boolean isSelected = (g.selectedModule == m);
                    boolean isEnabled = m.isEnabled();
                    boolean hovered = g.hovered(x, itemY, w, itemH);

                    float hAnim = ModernAnim.value("st_modhover:" + m.getName(), isSelected ? 1.0F : (hovered ? 0.5F : 0.0F), 14.0F);

                    int bgCol;
                    if (isEnabled) {
                        bgCol = ColorUtil.rgba(r, gr, b, (int) (15 + 30 * hAnim));
                    } else {
                        bgCol = ColorUtil.rgba(255, 255, 255, (int) (5 + 12 * hAnim));
                    }

                    g.rect(x, itemY, w, itemH, 4.5F, bgCol);
                    int borderCol = ColorUtil.rgba(255, 255, 255, isSelected ? 35 : (hovered ? 18 : 6));
                    g.outline(x, itemY, w, itemH, 4.5F, 0.4F, borderCol);

                    // Neon Active Bar
                    if (isEnabled) {
                        g.rect(x + 2.0F, itemY + 3.0F, 2.0F, 10.0F, 1.0F, accent);
                    }

                    int textCol;
                    if (isSelected) {
                        textCol = isEnabled ? accent : ColorUtil.rgba(255, 255, 255, 255);
                    } else if (isEnabled) {
                        textCol = accent;
                    } else {
                        textCol = ColorUtil.lerp(ColorUtil.rgba(140, 142, 152, 255), ColorUtil.rgba(215, 215, 220, 255), hAnim);
                    }

                    var font = g.font(6.5F);
                    float textOffset = isEnabled ? 7.0F : 5.0F;
                    g.text(font, g.clip(font, m.getName(), w - 14.0F), x + textOffset, g.textY(itemY, itemH, font), textCol);

                    g.hit(x, itemY, w, itemH, button -> {
                        g.selectedModule = m;
                        if (button == 1) {
                            m.toggle();
                        }
                        return true;
                    });
                }
                itemY += itemH + 2.0F;
            }
        }

        g.popClip();

        float totalH = modules.size() * (itemH + 2.0F);
        g.maxLeftScroll = Math.max(0.0F, totalH - listH);
    }

    // --- 2. RIGHT PANEL: SETTINGS EDITOR ---
    public static void renderRightSettings(ShaderToyGui g, float x, float y, float w, float h) {
        if (g.activePage == ShaderToyGui.Page.THEMES) {
            renderThemesPage(g, x, y, w, h);
            return;
        }

        Module module = g.selectedModule;
        if (module == null) {
            g.text(g.font(8.5F), "Выберите модуль для настройки", x + 10.0F, y + 20.0F, ColorUtil.rgba(120, 125, 140, 255));
            return;
        }

        // Module Header Card
        float headerCardH = 30.0F;
        int headerBg = ColorUtil.rgba(16, 18, 25, 160);
        int headerBorder = ColorUtil.rgba(255, 255, 255, 15);
        g.rect(x, y, w, headerCardH, 7.0F, headerBg);
        g.outline(x, y, w, headerCardH, 7.0F, 0.5F, headerBorder);

        var titleFont = g.font(9.5F);
        g.text(titleFont, module.getName(), x + 10.0F, y + 4.0F, ColorUtil.rgba(255, 255, 255, 255));

        var descFont = g.font(7.5F);
        String desc = module.getDescription() != null ? module.getDescription() : "Настройка параметров модуля";
        g.text(descFont, g.clip(descFont, desc, w - 85.0F), x + 10.0F, y + 17.0F, ColorUtil.rgba(130, 135, 150, 255));

        // Main Toggle Switch & Bind Button
        float toggleX = x + w - 26.0F;
        g.toggle(toggleX, y + 10.0F, module.isEnabled(), "st_modtgl:" + module.getName());
        g.hit(toggleX, y + 10.0F, 20.0F, 10.0F, button -> {
            module.toggle();
            return true;
        });

        if (module.getBind() != null) {
            String bindStr = g.isBinding(module) ? "..." : (module.getBind().isEmpty() ? "NONE" : KeyUtil.getKeyName(module.getBind().get(0)));
            var bindFont = g.font(7.0F);
            g.textRight(bindFont, bindStr, toggleX - 6.0F, g.textY(y, headerCardH, bindFont), ColorUtil.rgba(100, 105, 120, 255));
            g.hit(toggleX - 35.0F, y + 6.0F, 25.0F, 18.0F, button -> {
                g.startBind(module);
                return true;
            });
        }

        // Settings Grid Area
        float settingsY = y + headerCardH + 6.0F;
        float settingsH = h - (headerCardH + 6.0F);

        g.pushClip(x, settingsY, w, settingsH);

        List<Setting<?>> settings = module.getSettings();
        float colGap = 6.0F;
        float cardW = (w - colGap) / 2.0F;

        float leftY = settingsY - g.rightScroll;
        float rightY = settingsY - g.rightScroll;

        for (Setting<?> setting : settings) {
            if (setting == null || !setting.isVisible()) continue;

            float cardH = settingHeight(setting);

            if (leftY <= rightY) {
                renderSettingCard(g, setting, x, leftY, cardW, cardH);
                leftY += cardH + 5.0F;
            } else {
                renderSettingCard(g, setting, x + cardW + colGap, rightY, cardW, cardH);
                rightY += cardH + 5.0F;
            }
        }

        g.popClip();

        float totalH = Math.max(leftY, rightY) - (settingsY - g.rightScroll);
        g.maxRightScroll = Math.max(0.0F, totalH - settingsH);
    }

    private static void renderThemesPage(ShaderToyGui g, float x, float y, float w, float h) {
        var titleFont = g.font(9.0F);
        g.text(titleFont, "Темы и ShaderToy Задник", x, y, ColorUtil.rgba(255, 255, 255, 255));

        // Shader style selector card
        float cardY = y + 14.0F;
        float cardH = 46.0F;
        int cardBg = ColorUtil.rgba(19, 21, 28, 110);
        int cardBorder = ColorUtil.rgba(255, 255, 255, 15);

        g.rect(x, cardY, w, cardH, 6.0F, cardBg);
        g.outline(x, cardY, w, cardH, 6.0F, 0.4F, cardBorder);

        g.text(g.font(8.0F), "Шейдерный стиль фона (ShaderToy)", x + 8.0F, cardY + 6.0F, ColorUtil.rgba(255, 255, 255, 255));

        ShaderToyGui.ShaderStyle[] styles = ShaderToyGui.ShaderStyle.values();
        float btnW = (w - 16.0F - (styles.length - 1) * 4.0F) / styles.length;
        float btnH = 16.0F;
        float btnY = cardY + 22.0F;

        for (int i = 0; i < styles.length; i++) {
            ShaderToyGui.ShaderStyle style = styles[i];
            float bx = x + 8.0F + i * (btnW + 4.0F);
            boolean active = (g.currentShaderStyle == style);

            g.pill(bx, btnY, btnW, btnH, style.getDisplayName(), null, null, active, "st_style_" + style.name(), button -> {
                g.currentShaderStyle = style;
                return true;
            });
        }

        // Color Accents Card
        float accentY = cardY + cardH + 8.0F;
        float accentH = 34.0F;
        g.rect(x, accentY, w, accentH, 6.0F, cardBg);
        g.outline(x, accentY, w, accentH, 6.0F, 0.4F, cardBorder);

        g.text(g.font(8.0F), "Акцентный цвет интерфейса", x + 8.0F, accentY + 5.0F, ColorUtil.rgba(255, 255, 255, 255));

        float boxW = 20.0F;
        float boxH = 14.0F;
        float startBoxX = x + w - (ModernTheme.ACCENTS.length * (boxW + 4.0F)) - 8.0F;

        for (int i = 0; i < ModernTheme.ACCENTS.length; i++) {
            int color = ModernTheme.ACCENTS[i];
            float bx = startBoxX + i * (boxW + 4.0F);

            g.rect(bx, accentY + 10.0F, boxW, boxH, 3.0F, color);
            g.outline(bx, accentY + 10.0F, boxW, boxH, 3.0F, 0.6F, ColorUtil.rgba(255, 255, 255, 60));

            g.hit(bx, accentY + 10.0F, boxW, boxH, button -> {
                Theme.setAccentColor(color);
                return true;
            });
        }
    }

    private static float settingHeight(Setting<?> setting) {
        if (setting instanceof ModeSetting mode) {
            float exp = ModernAnim.get("drop:" + System.identityHashCode(mode), 0.0F);
            return 14.0F + (mode.getModes().size() * 11.0F + 3.0F) * Mth.clamp(exp, 0.0F, 1.0F);
        } else if (setting instanceof MultiModeSetting multi) {
            float exp = ModernAnim.get("drop:" + System.identityHashCode(multi), 0.0F);
            return 14.0F + (multi.getModes().size() * 11.0F + 3.0F) * Mth.clamp(exp, 0.0F, 1.0F);
        } else {
            return 14.0F;
        }
    }

    private static void renderSettingCard(ShaderToyGui g, Setting<?> setting, float x, float y, float w, float h) {
        int cardBg = ColorUtil.rgba(19, 21, 28, 90);
        int cardBorder = ColorUtil.rgba(255, 255, 255, 10);

        g.rect(x, y, w, h, 6.0F, cardBg);
        g.outline(x, y, w, h, 6.0F, 0.4F, cardBorder);

        float padding = 4.0F;
        float innerX = x + padding;
        float innerY = y;
        float innerW = w - padding * 2.0F;

        if (setting instanceof SliderSetting slider) {
            renderSlider(g, slider, innerX, innerY, innerW);
        } else if (setting instanceof CheckBox cb) {
            renderCheckBox(g, cb, innerX, innerY, innerW);
        } else if (setting instanceof ModeSetting mode) {
            renderMode(g, mode, innerX, innerY, innerW);
        } else if (setting instanceof MultiModeSetting multi) {
            renderMultiMode(g, multi, innerX, innerY, innerW);
        } else if (setting instanceof BindSetting bind) {
            renderBind(g, bind, innerX, innerY, innerW);
        } else if (setting instanceof ColorSetting color) {
            renderColor(g, color, innerX, innerY, innerW);
        }
    }

    private static void renderSlider(ShaderToyGui g, SliderSetting setting, float x, float y, float w) {
        var nameFont = g.font(6.5F);
        var valFont = g.font(6.0F);
        String name = setting.getName();
        String valStr = String.valueOf(setting.getValue());

        float nameW = nameFont.font().getWidth(name, nameFont.size());
        float valW = valFont.font().getWidth(valStr, valFont.size());

        // Name on left
        g.text(nameFont, name, x, g.textY(y, 14.0F, nameFont), ColorUtil.rgba(200, 205, 215, 255));

        // Value text on right
        float valX = x + w - valW;
        g.text(valFont, valStr, valX, g.textY(y, 14.0F, valFont), Theme.getAccentColor());

        // Slider track in between name and valStr
        float sliderX = x + nameW + 6.0F;
        float sliderW = (valX - 6.0F) - sliderX;
        float min = setting.getMin();
        float max = setting.getMax();
        float frac = (max - min) <= 0.0F ? 0.0F : (setting.getValue() - min) / (max - min);

        if (sliderW > 10.0F) {
            g.slider(sliderX, y + 5.5F, sliderW, frac, setting, fraction -> {
                float newVal = min + (float) fraction * (max - min);
                float step = setting.getStep();
                float stepped = step > 0.0F ? Math.round(newVal / step) * step : newVal;
                setting.setValue(stepped);
            });
        }
    }

    private static void renderCheckBox(ShaderToyGui g, CheckBox setting, float x, float y, float w) {
        boolean hover = g.hovered(x, y, w, 14.0F);
        var font = g.font(6.5F);
        g.text(font, g.clip(font, setting.getName(), w - 22.0F), x, g.textY(y, 14.0F, font), hover ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(200, 205, 215, 255));

        float toggleW = 17.0F;
        float toggleH = 8.5F;
        float toggleX = x + w - toggleW;
        float toggleY = y + (14.0F - toggleH) / 2.0F;

        g.toggle(toggleX, toggleY, setting.getValue(), setting);
        g.hit(x, y, w, 14.0F, button -> {
            setting.setValue(!setting.getValue());
            return true;
        });
    }

    private static void renderMode(ShaderToyGui g, ModeSetting setting, float x, float y, float w) {
        String key = "drop:" + System.identityHashCode(setting);
        boolean expState = isExpanded(setting);
        float exp = Mth.clamp(ModernAnim.value(key, expState ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var font = g.font(6.5F);
        var subFont = g.font(6.0F);
        String current = setting.getValue();
        float modeW = Math.min(w * 0.55F, subFont.font().getWidth(current, subFont.size()) + 14.0F);

        g.text(font, g.clip(font, setting.getName(), w - modeW - 4.0F), x, g.textY(y, 14.0F, font), ColorUtil.rgba(200, 205, 215, 255));
        float modeX = x + w - modeW;
        g.rect(modeX, y + 1.5F, modeW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.outline(modeX, y + 1.5F, modeW, 11.0F, 3.0F, 0.4F, ColorUtil.rgba(255, 255, 255, 15));
        g.text(subFont, g.clip(subFont, current, modeW - 12.0F), modeX + 4.0F, g.textY(y + 1.5F, 11.0F, subFont), ColorUtil.rgba(180, 185, 200, 255));
        g.chevron(modeX + modeW - 4.0F, y + 7.0F, 2.0F, 180.0F * exp, Theme.getAccentColor());

        g.hit(x, y, w, 14.0F, button -> {
            toggleExpanded(setting);
            return true;
        });

        if (exp > 0.01F) {
            float dropH = (setting.getModes().size() * 11.0F + 3.0F) * exp;
            g.pushClip(x, y + 14.0F, w, dropH);
            float optionY = y + 14.0F + 2.0F;

            for (String mode : setting.getModes()) {
                boolean active = setting.is(mode);
                boolean hov = g.hovered(x, optionY, w, 11.0F);
                float modeAnim = ModernAnim.value("mode:" + System.identityHashCode(setting) + mode, active ? 1.0F : (hov ? 0.6F : 0.0F), 15.0F);
                if (modeAnim > 0.01F) {
                    g.rect(x, optionY, w, 11.0F, 3.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), modeAnim * 0.4F));
                }

                g.rect(x + 4.0F, optionY + 5.5F - 1.0F, 2.0F, 2.0F, 1.0F, active ? Theme.getAccentColor() : ColorUtil.rgba(90, 95, 110, 255));
                g.text(subFont, g.clip(subFont, mode, w - 14.0F), x + 10.0F, g.textY(optionY, 11.0F, subFont), active ? Theme.getAccentColor() : ColorUtil.rgba(180, 185, 200, 255));
                g.hit(x, optionY, w, 11.0F, button -> {
                    setting.setValue(mode);
                    setExpanded(setting, false);
                    return true;
                });
                optionY += 11.0F;
            }

            g.popClip();
        }
    }

    private static void renderMultiMode(ShaderToyGui g, MultiModeSetting setting, float x, float y, float w) {
        String key = "drop:" + System.identityHashCode(setting);
        boolean expState = isExpanded(setting);
        float exp = Mth.clamp(ModernAnim.value(key, expState ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var font = g.font(6.5F);
        var subFont = g.font(6.0F);
        String display = setting.getValue() != null ? String.join(", ", setting.getValue()) : "";
        float pillW = Math.min(w * 0.55F, subFont.font().getWidth(display, subFont.size()) + 14.0F);

        g.text(font, g.clip(font, setting.getName(), w - pillW - 4.0F), x, g.textY(y, 14.0F, font), ColorUtil.rgba(200, 205, 215, 255));
        float pillX = x + w - pillW;
        g.rect(pillX, y + 1.5F, pillW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.outline(pillX, y + 1.5F, pillW, 11.0F, 3.0F, 0.4F, ColorUtil.rgba(255, 255, 255, 15));
        g.text(subFont, g.clip(subFont, display, pillW - 12.0F), pillX + 4.0F, g.textY(y + 1.5F, 11.0F, subFont), ColorUtil.rgba(180, 185, 200, 255));
        g.chevron(pillX + pillW - 4.0F, y + 7.0F, 2.0F, 180.0F * exp, Theme.getAccentColor());

        g.hit(x, y, w, 14.0F, button -> {
            toggleExpanded(setting);
            return true;
        });

        if (exp > 0.01F && setting.getModes() != null) {
            float dropH = (setting.getModes().size() * 11.0F + 3.0F) * exp;
            g.pushClip(x, y + 14.0F, w, dropH);
            float optionY = y + 14.0F + 2.0F;

            for (String mode : setting.getModes()) {
                boolean active = setting.isEnabled(mode);
                boolean hov = g.hovered(x, optionY, w, 11.0F);
                float modeAnim = ModernAnim.value("multi:" + System.identityHashCode(setting) + mode, active ? 1.0F : (hov ? 0.6F : 0.0F), 15.0F);
                if (modeAnim > 0.01F) {
                    g.rect(x, optionY, w, 11.0F, 3.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), modeAnim * 0.4F));
                }

                g.rect(x + 4.0F, optionY + 5.5F - 1.0F, 2.0F, 2.0F, 1.0F, active ? Theme.getAccentColor() : ColorUtil.rgba(90, 95, 110, 255));
                g.text(subFont, g.clip(subFont, mode, w - 14.0F), x + 10.0F, g.textY(optionY, 11.0F, subFont), active ? Theme.getAccentColor() : ColorUtil.rgba(180, 185, 200, 255));
                g.hit(x, optionY, w, 11.0F, button -> {
                    setting.toggle(mode);
                    return true;
                });
                optionY += 11.0F;
            }

            g.popClip();
        }
    }

    private static void renderBind(ShaderToyGui g, BindSetting setting, float x, float y, float w) {
        var font = g.font(6.5F);
        var subFont = g.font(6.0F);
        boolean binding = g.isBinding(setting);
        String bindStr = binding ? "..." : (setting.isEmpty() ? "NONE" : KeyUtil.getKeyName(setting.get(0)));
        float bindW = subFont.font().getWidth(bindStr, subFont.size()) + 8.0F;

        g.text(font, g.clip(font, setting.getName(), w - bindW - 4.0F), x, g.textY(y, 14.0F, font), ColorUtil.rgba(200, 205, 215, 255));
        float bindX = x + w - bindW;
        g.rect(bindX, y + 1.5F, bindW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, bindStr, bindX + 4.0F, g.textY(y + 1.5F, 11.0F, subFont), binding ? Theme.getAccentColor() : ColorUtil.rgba(180, 185, 200, 255));
        g.hit(x, y, w, 14.0F, button -> {
            g.startBind(setting);
            return true;
        });
    }

    private static void renderColor(ShaderToyGui g, ColorSetting setting, float x, float y, float w) {
        var font = g.font(6.5F);
        g.text(font, g.clip(font, setting.getName(), w - 18.0F), x, g.textY(y, 14.0F, font), ColorUtil.rgba(200, 205, 215, 255));
        g.rect(x + w - 12.0F, y + 2.5F, 10.0F, 9.0F, 2.0F, setting.getValue());
        g.outline(x + w - 12.0F, y + 2.5F, 10.0F, 9.0F, 2.0F, 0.4F, ColorUtil.rgba(255, 255, 255, 20));
    }
}
