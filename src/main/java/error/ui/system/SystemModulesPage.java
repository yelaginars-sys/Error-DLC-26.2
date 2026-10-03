package error.ui.system;

import error.Client;
import error.config.ConfigManager;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.ui.modern.ModernAnim;
import error.util.client.clients.ColorUtil;
import error.util.client.persiki.KeyUtil;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SystemModulesPage {

    private static final Set<Object> expandedSettings = new HashSet<>();

    private static boolean isExpanded(Object setting) {
        return expandedSettings.contains(setting);
    }

    private static void toggleExpanded(Object setting) {
        if (!expandedSettings.remove(setting)) {
            expandedSettings.add(setting);
        }
    }

    private static void setExpanded(Object setting, boolean expanded) {
        if (expanded) {
            expandedSettings.add(setting);
        } else {
            expandedSettings.remove(setting);
        }
    }

    public static void renderContent(SystemGui g, float x, float y, float w, float h) {
        if (g.activeSection.getGroup() == SystemGui.SectionGroup.SETTINGS) {
            renderSettingsPages(g, x, y, w, h);
            return;
        }

        Category cat = g.activeSection.getCategory();
        List<Module> modules = g.getModulesForCategory(cat, g.searchQuery);

        g.pushClip(x, y, w, h);

        float colGap = 6.0F;
        float colW = (w - colGap * 2.0F) / 3.0F;

        float col0Y = y - g.scroll;
        float col1Y = y - g.scroll;
        float col2Y = y - g.scroll;

        if (modules.isEmpty()) {
            g.text(g.font(7.5F), "Модули не найдены", x + 4.0F, y + 10.0F, SystemTheme.TEXT_MUTED());
        } else {
            for (Module m : modules) {
                float cardH = calculateCardHeight(m);

                if (col0Y <= col1Y && col0Y <= col2Y) {
                    renderFullModuleCard(g, m, x, col0Y, colW, cardH);
                    col0Y += cardH + colGap;
                } else if (col1Y <= col0Y && col1Y <= col2Y) {
                    renderFullModuleCard(g, m, x + colW + colGap, col1Y, colW, cardH);
                    col1Y += cardH + colGap;
                } else {
                    renderFullModuleCard(g, m, x + (colW + colGap) * 2.0F, col2Y, colW, cardH);
                    col2Y += cardH + colGap;
                }
            }
        }

        g.popClip();

        float maxColumnY = Math.max(col0Y, Math.max(col1Y, col2Y));
        float totalH = maxColumnY - (y - g.scroll);
        g.maxScroll = Math.max(0.0F, totalH - h);
    }

    private static float calculateCardHeight(Module m) {
        float h = 20.0F; // Card header height
        if (m.getSettings() != null && !m.getSettings().isEmpty()) {
            h += 4.0F;
            for (Setting<?> setting : m.getSettings()) {
                if (setting != null && setting.isVisible()) {
                    h += settingHeight(setting) + 3.5F;
                }
            }
        }
        return h + 4.0F;
    }

    private static float settingHeight(Object setting) {
        if (setting instanceof CheckBox) {
            return 13.0F;
        } else if (setting instanceof SliderSetting) {
            return 17.0F;
        } else if (setting instanceof ModeSetting mode) {
            if (mode.getModes().size() <= 3) {
                return 22.0F; // Segmented pill buttons row
            }
            if (isExpanded(mode)) {
                return 14.0F + (mode.getModes().size() * 11.0F + 2.0F);
            }
            return 14.0F;
        } else if (setting instanceof MultiModeSetting multi) {
            if (isExpanded(multi)) {
                return 14.0F + (multi.getModes().size() * 11.0F + 2.0F);
            }
            return 14.0F;
        }
        return 13.0F;
    }

    private static void renderFullModuleCard(SystemGui g, Module m, float x, float y, float w, float h) {
        boolean isEnabled = m.isEnabled();
        boolean hovered = g.hovered(x, y, w, h);

        int cardBg = isEnabled ? ColorUtil.rgba(22, 25, 34, 220) : SystemTheme.CARD();
        int borderCol = hovered ? SystemTheme.BORDER_ACTIVE() : SystemTheme.BORDER();

        g.rect(x, y, w, h, 8.0F, cardBg);
        g.outline(x, y, w, h, 8.0F, 0.4F, borderCol);

        // Card Header
        var titleFont = g.font(6.8F);
        int titleCol = isEnabled ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_SECONDARY();

        // Icon + Title
        g.icon("gear", 6.5F, "\uEA06", x + 10.0F, y + 10.0F, isEnabled ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_MUTED());
        g.text(titleFont, g.clip(titleFont, m.getName(), w - 50.0F), x + 18.0F, y + 3.5F, titleCol);

        // Keybind Pill
        int bindKey = m.getBind() != null && !m.getBind().isEmpty() ? m.getBind().get(0) : 0;
        String keyStr = bindKey != 0 ? KeyUtil.getKeyName(bindKey) : "None";
        float keyW = g.font(4.8F).font().getWidth(keyStr, 4.8F) + 6.0F;
        float keyX = x + w - 38.0F - keyW;
        g.rect(keyX, y + 5.0F, keyW, 10.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.textCenter(g.font(4.8F), keyStr, keyX + keyW / 2.0F, g.textY(y + 5.0F, 10.0F, g.font(4.8F)), SystemTheme.TEXT_MUTED());

        g.hit(keyX, y + 4.0F, keyW, 12.0F, button -> {
            g.selectedModule = m;
            g.startBind(m);
            return true;
        });

        // Toggle Switch on Right
        float toggleX = x + w - 20.0F;
        float toggleY = y + 6.0F;
        g.toggle(toggleX, toggleY, isEnabled, "sys_tgl:" + m.getName());

        g.hit(toggleX, toggleY - 1.0F, 16.0F, 12.0F, button -> {
            g.selectedModule = m;
            m.toggle();
            return true;
        });

        g.hit(x, y, w - 40.0F, 20.0F, button -> {
            g.selectedModule = m;
            if (button == 1) m.toggle();
            return true;
        });

        // Settings Body inside Card
        if (m.getSettings() != null && !m.getSettings().isEmpty()) {
            float curY = y + 22.0F;
            float innerX = x + 7.0F;
            float innerW = w - 14.0F;

            for (Setting<?> setting : m.getSettings()) {
                if (setting == null || !setting.isVisible()) continue;

                float sH = settingHeight(setting);
                renderSettingRow(g, m, setting, innerX, curY, innerW, sH);
                curY += sH + 3.5F;
            }
        }
    }

    private static void renderSettingRow(SystemGui g, Module ownerModule, Setting<?> setting, float x, float y, float w, float h) {
        if (setting instanceof CheckBox cb) {
            renderCheckBox(g, ownerModule, cb, x, y, w, h);
        } else if (setting instanceof SliderSetting slider) {
            renderSlider(g, ownerModule, slider, x, y, w, h);
        } else if (setting instanceof ModeSetting mode) {
            renderMode(g, ownerModule, mode, x, y, w, h);
        } else if (setting instanceof MultiModeSetting multi) {
            renderMultiMode(g, ownerModule, multi, x, y, w, h);
        } else if (setting instanceof BindSetting bind) {
            renderBind(g, ownerModule, bind, x, y, w, h);
        } else if (setting instanceof ColorSetting color) {
            renderColor(g, ownerModule, color, x, y, w, h);
        }
    }

    private static void renderCheckBox(SystemGui g, Module ownerModule, CheckBox setting, float x, float y, float w, float h) {
        boolean hover = g.hovered(x, y, w, h);
        var font = g.font(5.6F);
        g.text(font, g.clip(font, setting.getName(), w - 16.0F), x, y + 1.0F, hover ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_SECONDARY());

        float checkX = x + w - 10.0F;
        float checkY = y + 2.0F;
        g.checkbox(checkX, checkY, setting.getValue(), setting);

        g.hit(x, y, w, h, button -> {
            g.selectedModule = ownerModule;
            setting.setValue(!setting.getValue());
            return true;
        });
    }

    private static void renderSlider(SystemGui g, Module ownerModule, SliderSetting setting, float x, float y, float w, float h) {
        var nameFont = g.font(5.6F);
        var valFont = g.font(5.2F);
        String name = setting.getName();
        String valStr = String.valueOf(setting.getValue());

        float nameW = nameFont.font().getWidth(name, nameFont.size());
        float valW = valFont.font().getWidth(valStr, valFont.size());

        g.text(nameFont, g.clip(nameFont, name, w - valW - 4.0F), x, y + 1.0F, SystemTheme.TEXT_SECONDARY());
        float valX = x + w - valW;
        g.text(valFont, valStr, valX, y + 1.0F, SystemTheme.TEXT_PRIMARY());

        float trackY = y + 11.0F;
        float min = setting.getMin();
        float max = setting.getMax();
        float frac = (max - min) <= 0.0F ? 0.0F : (setting.getValue() - min) / (max - min);

        g.slider(x, trackY, w, frac, setting, fraction -> {
            g.selectedModule = ownerModule;
            float newVal = min + (float) fraction * (max - min);
            float step = setting.getStep();
            float stepped = step > 0.0F ? Math.round(newVal / step) * step : newVal;
            setting.setValue(stepped);
        });
    }

    private static void renderMode(SystemGui g, Module ownerModule, ModeSetting setting, float x, float y, float w, float h) {
        List<String> modes = setting.getModes();

        // If <= 3 options, render as Segmented Switch Pill Bar!
        if (modes != null && modes.size() <= 3) {
            var labelFont = g.font(5.4F);
            g.text(labelFont, g.clip(labelFont, setting.getName(), w), x, y + 1.0F, SystemTheme.TEXT_SECONDARY());

            float segmentedY = y + 9.0F;
            float segmentedH = 11.0F;
            g.rect(x, segmentedY, w, segmentedH, 3.5F, ColorUtil.rgba(14, 16, 22, 180));

            float btnW = w / modes.size();
            for (int i = 0; i < modes.size(); i++) {
                String mode = modes.get(i);
                boolean active = setting.is(mode);
                float bx = x + i * btnW;

                if (active) {
                    g.rect(bx + 1.0F, segmentedY + 1.0F, btnW - 2.0F, segmentedH - 2.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 20));
                    g.outline(bx + 1.0F, segmentedY + 1.0F, btnW - 2.0F, segmentedH - 2.0F, 3.0F, 0.4F, SystemTheme.BORDER_ACTIVE());
                }

                var modeFont = g.font(4.8F);
                g.textCenter(modeFont, g.clip(modeFont, mode, btnW - 4.0F), bx + btnW / 2.0F, g.textY(segmentedY, segmentedH, modeFont), active ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_MUTED());

                g.hit(bx, segmentedY, btnW, segmentedH, button -> {
                    g.selectedModule = ownerModule;
                    setting.setValue(mode);
                    return true;
                });
            }
            return;
        }

        // Standard Dropdown for > 3 modes
        String key = "sys_drop:" + System.identityHashCode(setting);
        boolean expState = isExpanded(setting);
        float exp = Mth.clamp(ModernAnim.value(key, expState ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var font = g.font(5.6F);
        var subFont = g.font(5.0F);
        String current = setting.getValue();
        float modeW = Math.min(w * 0.50F, subFont.font().getWidth(current, subFont.size()) + 12.0F);

        g.text(font, g.clip(font, setting.getName(), w - modeW - 4.0F), x, g.textY(y, 13.0F, font), SystemTheme.TEXT_SECONDARY());
        float modeX = x + w - modeW;
        g.rect(modeX, y + 1.0F, modeW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, g.clip(subFont, current, modeW - 10.0F), modeX + 3.0F, g.textY(y + 1.0F, 11.0F, subFont), SystemTheme.TEXT_PRIMARY());

        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            toggleExpanded(setting);
            return true;
        });

        if (exp > 0.01F && modes != null) {
            float dropH = (modes.size() * 11.0F + 2.0F) * exp;
            g.pushClip(x, y + 13.0F, w, dropH);
            float optionY = y + 13.0F + 1.0F;

            for (String mode : modes) {
                boolean active = setting.is(mode);
                boolean hov = g.hovered(x, optionY, w, 11.0F);
                if (active || hov) {
                    g.rect(x, optionY, w, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, active ? 20 : 10));
                }

                g.text(subFont, g.clip(subFont, mode, w - 10.0F), x + 6.0F, g.textY(optionY, 11.0F, subFont), active ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_SECONDARY());
                g.hit(x, optionY, w, 11.0F, button -> {
                    g.selectedModule = ownerModule;
                    setting.setValue(mode);
                    setExpanded(setting, false);
                    return true;
                });
                optionY += 11.0F;
            }

            g.popClip();
        }
    }

    private static void renderMultiMode(SystemGui g, Module ownerModule, MultiModeSetting setting, float x, float y, float w, float h) {
        String key = "sys_drop:" + System.identityHashCode(setting);
        boolean expState = isExpanded(setting);
        float exp = Mth.clamp(ModernAnim.value(key, expState ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var font = g.font(5.6F);
        var subFont = g.font(5.0F);
        String display = setting.getValue() != null ? String.join(", ", setting.getValue()) : "";
        float pillW = Math.min(w * 0.50F, subFont.font().getWidth(display, subFont.size()) + 12.0F);

        g.text(font, g.clip(font, setting.getName(), w - pillW - 4.0F), x, g.textY(y, 13.0F, font), SystemTheme.TEXT_SECONDARY());
        float pillX = x + w - pillW;
        g.rect(pillX, y + 1.0F, pillW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, g.clip(subFont, display, pillW - 10.0F), pillX + 3.0F, g.textY(y + 1.0F, 11.0F, subFont), SystemTheme.TEXT_PRIMARY());

        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            toggleExpanded(setting);
            return true;
        });

        if (exp > 0.01F && setting.getModes() != null) {
            float dropH = (setting.getModes().size() * 11.0F + 2.0F) * exp;
            g.pushClip(x, y + 13.0F, w, dropH);
            float optionY = y + 13.0F + 1.0F;

            for (String mode : setting.getModes()) {
                boolean active = setting.isEnabled(mode);
                boolean hov = g.hovered(x, optionY, w, 11.0F);
                if (active || hov) {
                    g.rect(x, optionY, w, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, active ? 20 : 10));
                }

                g.text(subFont, g.clip(subFont, mode, w - 10.0F), x + 6.0F, g.textY(optionY, 11.0F, subFont), active ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_SECONDARY());
                g.hit(x, optionY, w, 11.0F, button -> {
                    g.selectedModule = ownerModule;
                    setting.toggle(mode);
                    return true;
                });
                optionY += 11.0F;
            }

            g.popClip();
        }
    }

    private static void renderBind(SystemGui g, Module ownerModule, BindSetting setting, float x, float y, float w, float h) {
        var font = g.font(5.6F);
        var subFont = g.font(5.0F);
        boolean binding = g.isBinding(setting);
        String bindStr = binding ? "..." : (setting.isEmpty() ? "NONE" : KeyUtil.getKeyName(setting.get(0)));
        float bindW = subFont.font().getWidth(bindStr, subFont.size()) + 8.0F;

        g.text(font, g.clip(font, setting.getName(), w - bindW - 4.0F), x, g.textY(y, 13.0F, font), SystemTheme.TEXT_SECONDARY());
        float bindX = x + w - bindW;
        g.rect(bindX, y + 1.0F, bindW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, bindStr, bindX + 4.0F, g.textY(y + 1.0F, 11.0F, subFont), binding ? SystemTheme.WHITE() : SystemTheme.TEXT_PRIMARY());
        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            g.startBind(setting);
            return true;
        });
    }

    private static void renderColor(SystemGui g, Module ownerModule, ColorSetting setting, float x, float y, float w, float h) {
        var font = g.font(5.6F);
        g.text(font, g.clip(font, setting.getName(), w - 16.0F), x, g.textY(y, 13.0F, font), SystemTheme.TEXT_SECONDARY());
        g.rect(x + w - 10.0F, y + 2.0F, 9.0F, 8.5F, 2.0F, setting.getValue());
        g.outline(x + w - 10.0F, y + 2.0F, 9.0F, 8.5F, 2.0F, 0.4F, SystemTheme.BORDER());
        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            return true;
        });
    }

    private static void renderSettingsPages(SystemGui g, float x, float y, float w, float h) {
        var titleFont = g.font(8.0F);
        g.text(titleFont, g.activeSection.getTitle(), x, y, SystemTheme.TEXT_PRIMARY());

        float itemY = y + 16.0F;
        if (g.activeSection == SystemGui.Section.CONFIGS) {
            ConfigManager cfgManager = Client.INSTANCE != null ? Client.INSTANCE.configManager : null;
            List<String> configs = cfgManager != null ? cfgManager.getAvailableConfigs() : List.of("default");

            for (String cfgName : configs) {
                boolean active = cfgManager != null && cfgName.equalsIgnoreCase(cfgManager.getCurrentConfig());
                g.rect(x, itemY, w, 22.0F, 5.0F, SystemTheme.CARD());
                g.outline(x, itemY, w, 22.0F, 5.0F, 0.4F, active ? SystemTheme.BORDER_ACTIVE() : SystemTheme.BORDER());
                g.text(g.font(7.0F), cfgName, x + 8.0F, itemY + 3.5F, SystemTheme.TEXT_PRIMARY());

                float loadX = x + w - 90.0F;
                g.rect(loadX, itemY + 3.0F, 40.0F, 16.0F, 4.0F, ColorUtil.rgba(255, 255, 255, active ? 25 : 10));
                g.textCenter(g.font(5.5F), "Загрузить", loadX + 20.0F, g.textY(itemY + 3.0F, 16.0F, g.font(5.5F)), SystemTheme.TEXT_PRIMARY());
                g.hit(loadX, itemY + 3.0F, 40.0F, 16.0F, button -> {
                    if (cfgManager != null) cfgManager.loadConfig(cfgName, true);
                    return true;
                });

                itemY += 25.0F;
            }
        } else if (g.activeSection == SystemGui.Section.FRIENDS) {
            FriendManager friendManager = Client.INSTANCE != null ? Client.INSTANCE.friendManager : null;
            java.util.Collection<String> friends = friendManager != null ? friendManager.getFriends() : List.of();

            if (friends.isEmpty()) {
                g.text(g.font(7.0F), "Список друзей пуст", x, itemY + 10.0F, SystemTheme.TEXT_MUTED());
            } else {
                for (String friendNick : friends) {
                    g.rect(x, itemY, w, 20.0F, 5.0F, SystemTheme.CARD());
                    g.text(g.font(7.0F), friendNick, x + 8.0F, itemY + 3.0F, SystemTheme.TEXT_PRIMARY());
                    itemY += 23.0F;
                }
            }
        } else if (g.activeSection == SystemGui.Section.COSMETICS) {
            g.rect(x, itemY, w, 40.0F, 6.0F, SystemTheme.CARD());
            g.text(g.font(7.5F), "Косметические предметы и эффекты", x + 10.0F, itemY + 6.0F, SystemTheme.TEXT_PRIMARY());
            g.text(g.font(5.5F), "В разработке (BETA)", x + 10.0F, itemY + 18.0F, SystemTheme.TEXT_MUTED());
        } else {
            g.rect(x, itemY, w, 30.0F, 6.0F, SystemTheme.CARD());
            g.text(g.font(7.0F), "Раздел " + g.activeSection.getTitle(), x + 10.0F, itemY + 8.0F, SystemTheme.TEXT_PRIMARY());
        }
    }
}
