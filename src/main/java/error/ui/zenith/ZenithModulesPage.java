package error.ui.zenith;

import error.Client;
import error.config.ConfigManager;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.ui.modern.ModernAnim;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.KeyUtil;
import error.util.render.Render2DUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ZenithModulesPage {

    private static final Set<Object> expandedSettings = new HashSet<>();
    private static final String[] SUB_TABS = new String[]{"Все", "Модули", "Конфиги", "Друзья", "Темы"};

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

    public static void renderContent(ZenithGui g, float x, float y, float w, float h) {
        // 1. Horizontal Sub-Tabs Pills
        float tabW = 46.0F;
        float tabH = 14.0F;
        float tabGap = 3.0F;

        for (int i = 0; i < SUB_TABS.length; i++) {
            String tabName = SUB_TABS[i];
            float tx = x + i * (tabW + tabGap);
            boolean active = g.subTab.equalsIgnoreCase(tabName);

            g.pill(tx, y, tabW, tabH, tabName, active, "z_subtab_" + tabName, button -> {
                g.subTab = tabName;
                return true;
            });
        }

        float mainAreaY = y + tabH + 6.0F;
        float mainAreaH = h - (tabH + 6.0F);

        float rightPreviewW = 150.0F;
        float middleGridW = w - rightPreviewW - 8.0F;

        // 2. Middle Section Grid
        if (g.subTab.equalsIgnoreCase("Конфиги")) {
            renderConfigsPage(g, x, mainAreaY, middleGridW, mainAreaH);
        } else if (g.subTab.equalsIgnoreCase("Друзья")) {
            renderFriendsPage(g, x, mainAreaY, middleGridW, mainAreaH);
        } else if (g.subTab.equalsIgnoreCase("Темы")) {
            renderThemesPage(g, x, mainAreaY, middleGridW, mainAreaH);
        } else {
            renderModulesGrid(g, x, mainAreaY, middleGridW, mainAreaH);
        }

        // 3. Right 3D Player & Selected Module Description Inspector Panel
        float rightPanelX = x + middleGridW + 8.0F;
        renderRightInspectorPanel(g, rightPanelX, mainAreaY, rightPreviewW, mainAreaH);
    }

    private static void renderModulesGrid(ZenithGui g, float x, float y, float w, float h) {
        Category cat = g.activeSection.getCategory();
        List<Module> modules = g.getModulesForCategory(cat, g.searchQuery);

        g.pushClip(x, y, w, h);

        float colGap = 6.0F;
        float cardW = (w - colGap) / 2.0F;

        float leftY = y - g.leftScroll;
        float rightY = y - g.leftScroll;

        if (modules.isEmpty()) {
            g.text(g.font(7.5F), "Модули не найдены", x + 4.0F, y + 10.0F, ZenithTheme.TEXT_MUTED());
        } else {
            for (Module m : modules) {
                float cardH = calculateCardHeight(m);
                if (leftY <= rightY) {
                    renderFullModuleCard(g, m, x, leftY, cardW, cardH);
                    leftY += cardH + colGap;
                } else {
                    renderFullModuleCard(g, m, x + cardW + colGap, rightY, cardW, cardH);
                    rightY += cardH + colGap;
                }
            }
        }

        g.popClip();

        float totalH = Math.max(leftY, rightY) - (y - g.leftScroll);
        g.maxLeftScroll = Math.max(0.0F, totalH - h);
    }

    private static float calculateCardHeight(Module m) {
        float h = 22.0F; // Header height
        if (m.getSettings() != null && !m.getSettings().isEmpty()) {
            h += 4.0F;
            for (Setting<?> setting : m.getSettings()) {
                if (setting != null && setting.isVisible()) {
                    h += settingHeight(setting) + 3.0F;
                }
            }
        }
        return h + 4.0F;
    }

    private static float settingHeight(Object setting) {
        if (setting instanceof ModeSetting mode && isExpanded(mode)) {
            return 13.0F + (mode.getModes().size() * 11.0F + 2.0F);
        }
        if (setting instanceof MultiModeSetting multi && isExpanded(multi)) {
            return 13.0F + (multi.getModes().size() * 11.0F + 2.0F);
        }
        return 13.0F;
    }

    private static void renderFullModuleCard(ZenithGui g, Module m, float x, float y, float w, float h) {
        boolean isSelected = (g.selectedModule == m);
        boolean isEnabled = m.isEnabled();
        boolean hovered = g.hovered(x, y, w, h);

        float hAnim = ModernAnim.value("z_modcard:" + m.getName(), isSelected ? 1.0F : (hovered ? 0.5F : 0.0F), 14.0F);

        int cardBg = isEnabled ? ColorUtil.rgba(24, 20, 32, 160) : ColorUtil.rgba(18, 20, 28, 130);
        int borderCol = isSelected ? ZenithTheme.BORDER_ACTIVE() : (hovered ? ColorUtil.rgba(255, 255, 255, 25) : ZenithTheme.BORDER());

        g.rect(x, y, w, h, 6.0F, cardBg);
        g.outline(x, y, w, h, 6.0F, 0.4F, borderCol);

        // Header
        var titleFont = g.font(7.5F);
        int titleCol = isEnabled ? ZenithTheme.TEXT_PRIMARY() : ZenithTheme.TEXT_SECONDARY();
        g.text(titleFont, g.clip(titleFont, m.getName(), w - 28.0F), x + 8.0F, y + 3.5F, titleCol);

        var categoryFont = g.font(5.5F);
        String catName = m.getCategory() != null ? m.getCategory().getDisplayName() : "MODULE";
        g.text(categoryFont, catName, x + 8.0F, y + 13.5F, ZenithTheme.TEXT_MUTED());

        // Top Right Toggle Switch
        float toggleX = x + w - 21.0F;
        float toggleY = y + 6.0F;
        g.toggle(toggleX, toggleY, isEnabled, "z_modtgl:" + m.getName());

        g.hit(toggleX, toggleY - 1.0F, 18.0F, 12.0F, button -> {
            g.selectedModule = m;
            m.toggle();
            return true;
        });

        g.hit(x, y, w - 24.0F, 22.0F, button -> {
            g.selectedModule = m;
            if (button == 1) m.toggle();
            return true;
        });

        // Settings Body inside Card
        if (m.getSettings() != null && !m.getSettings().isEmpty()) {
            float curY = y + 24.0F;
            float innerX = x + 6.0F;
            float innerW = w - 12.0F;

            for (Setting<?> setting : m.getSettings()) {
                if (setting == null || !setting.isVisible()) continue;

                float sH = settingHeight(setting);
                renderSettingRow(g, m, setting, innerX, curY, innerW, sH);
                curY += sH + 3.0F;
            }
        }
    }

    private static void renderSettingRow(ZenithGui g, Module ownerModule, Setting<?> setting, float x, float y, float w, float h) {
        if (setting instanceof SliderSetting slider) {
            renderSlider(g, ownerModule, slider, x, y, w);
        } else if (setting instanceof CheckBox cb) {
            renderCheckBox(g, ownerModule, cb, x, y, w);
        } else if (setting instanceof ModeSetting mode) {
            renderMode(g, ownerModule, mode, x, y, w);
        } else if (setting instanceof MultiModeSetting multi) {
            renderMultiMode(g, ownerModule, multi, x, y, w);
        } else if (setting instanceof BindSetting bind) {
            renderBind(g, ownerModule, bind, x, y, w);
        } else if (setting instanceof ColorSetting color) {
            renderColor(g, ownerModule, color, x, y, w);
        }
    }

    private static void renderSlider(ZenithGui g, Module ownerModule, SliderSetting setting, float x, float y, float w) {
        var nameFont = g.font(5.8F);
        var valFont = g.font(5.5F);
        String name = setting.getName();
        String valStr = String.valueOf(setting.getValue());

        float nameW = nameFont.font().getWidth(name, nameFont.size());
        float valW = valFont.font().getWidth(valStr, valFont.size());

        g.text(nameFont, g.clip(nameFont, name, w * 0.55F), x, g.textY(y, 13.0F, nameFont), ZenithTheme.TEXT_SECONDARY());
        float valX = x + w - valW;
        g.text(valFont, valStr, valX, g.textY(y, 13.0F, valFont), ZenithTheme.ACCENT());

        float sliderX = x + Math.min(nameW + 4.0F, w * 0.55F + 2.0F);
        float sliderW = (valX - 4.0F) - sliderX;
        float min = setting.getMin();
        float max = setting.getMax();
        float frac = (max - min) <= 0.0F ? 0.0F : (setting.getValue() - min) / (max - min);

        if (sliderW > 6.0F) {
            g.slider(sliderX, y + 5.0F, sliderW, frac, setting, fraction -> {
                g.selectedModule = ownerModule;
                float newVal = min + (float) fraction * (max - min);
                float step = setting.getStep();
                float stepped = step > 0.0F ? Math.round(newVal / step) * step : newVal;
                setting.setValue(stepped);
            });
        }
    }

    private static void renderCheckBox(ZenithGui g, Module ownerModule, CheckBox setting, float x, float y, float w) {
        boolean hover = g.hovered(x, y, w, 13.0F);
        var font = g.font(5.8F);
        g.text(font, g.clip(font, setting.getName(), w - 20.0F), x, g.textY(y, 13.0F, font), hover ? ZenithTheme.TEXT_PRIMARY() : ZenithTheme.TEXT_SECONDARY());

        float toggleW = 15.0F;
        float toggleH = 8.0F;
        float toggleX = x + w - toggleW;
        float toggleY = y + (13.0F - toggleH) / 2.0F;

        g.toggle(toggleX, toggleY, setting.getValue(), setting);
        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            setting.setValue(!setting.getValue());
            return true;
        });
    }

    private static void renderMode(ZenithGui g, Module ownerModule, ModeSetting setting, float x, float y, float w) {
        String key = "z_drop:" + System.identityHashCode(setting);
        boolean expState = isExpanded(setting);
        float exp = Mth.clamp(ModernAnim.value(key, expState ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var font = g.font(5.8F);
        var subFont = g.font(5.5F);
        String current = setting.getValue();
        float modeW = Math.min(w * 0.50F, subFont.font().getWidth(current, subFont.size()) + 12.0F);

        g.text(font, g.clip(font, setting.getName(), w - modeW - 4.0F), x, g.textY(y, 13.0F, font), ZenithTheme.TEXT_SECONDARY());
        float modeX = x + w - modeW;
        g.rect(modeX, y + 1.0F, modeW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, g.clip(subFont, current, modeW - 10.0F), modeX + 3.0F, g.textY(y + 1.0F, 11.0F, subFont), ZenithTheme.TEXT_PRIMARY());
        g.chevron(modeX + modeW - 3.0F, y + 6.5F, 2.0F, 180.0F * exp, ZenithTheme.ACCENT());

        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            toggleExpanded(setting);
            return true;
        });

        if (exp > 0.01F) {
            float dropH = (setting.getModes().size() * 11.0F + 2.0F) * exp;
            g.pushClip(x, y + 13.0F, w, dropH);
            float optionY = y + 13.0F + 1.0F;

            for (String mode : setting.getModes()) {
                boolean active = setting.is(mode);
                boolean hov = g.hovered(x, optionY, w, 11.0F);
                float modeAnim = ModernAnim.value("z_mode:" + System.identityHashCode(setting) + mode, active ? 1.0F : (hov ? 0.6F : 0.0F), 15.0F);
                if (modeAnim > 0.01F) {
                    g.rect(x, optionY, w, 11.0F, 3.0F, ColorUtil.multiplyAlpha(ZenithTheme.ACCENT(), modeAnim * 0.3F));
                }

                g.rect(x + 4.0F, optionY + 5.5F - 1.0F, 2.0F, 2.0F, 1.0F, active ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_MUTED());
                g.text(subFont, g.clip(subFont, mode, w - 14.0F), x + 10.0F, g.textY(optionY, 11.0F, subFont), active ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_SECONDARY());
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

    private static void renderMultiMode(ZenithGui g, Module ownerModule, MultiModeSetting setting, float x, float y, float w) {
        String key = "z_drop:" + System.identityHashCode(setting);
        boolean expState = isExpanded(setting);
        float exp = Mth.clamp(ModernAnim.value(key, expState ? 1.0F : 0.0F, 14.0F), 0.0F, 1.0F);
        var font = g.font(5.8F);
        var subFont = g.font(5.5F);
        String display = setting.getValue() != null ? String.join(", ", setting.getValue()) : "";
        float pillW = Math.min(w * 0.50F, subFont.font().getWidth(display, subFont.size()) + 12.0F);

        g.text(font, g.clip(font, setting.getName(), w - pillW - 4.0F), x, g.textY(y, 13.0F, font), ZenithTheme.TEXT_SECONDARY());
        float pillX = x + w - pillW;
        g.rect(pillX, y + 1.0F, pillW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, g.clip(subFont, display, pillW - 10.0F), pillX + 3.0F, g.textY(y + 1.0F, 11.0F, subFont), ZenithTheme.TEXT_PRIMARY());
        g.chevron(pillX + pillW - 3.0F, y + 6.5F, 2.0F, 180.0F * exp, ZenithTheme.ACCENT());

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
                float modeAnim = ModernAnim.value("z_multi:" + System.identityHashCode(setting) + mode, active ? 1.0F : (hov ? 0.6F : 0.0F), 15.0F);
                if (modeAnim > 0.01F) {
                    g.rect(x, optionY, w, 11.0F, 3.0F, ColorUtil.multiplyAlpha(ZenithTheme.ACCENT(), modeAnim * 0.3F));
                }

                g.rect(x + 4.0F, optionY + 5.5F - 1.0F, 2.0F, 2.0F, 1.0F, active ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_MUTED());
                g.text(subFont, g.clip(subFont, mode, w - 14.0F), x + 10.0F, g.textY(optionY, 11.0F, subFont), active ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_SECONDARY());
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

    private static void renderBind(ZenithGui g, Module ownerModule, BindSetting setting, float x, float y, float w) {
        var font = g.font(5.8F);
        var subFont = g.font(5.5F);
        boolean binding = g.isBinding(setting);
        String bindStr = binding ? "..." : (setting.isEmpty() ? "NONE" : KeyUtil.getKeyName(setting.get(0)));
        float bindW = subFont.font().getWidth(bindStr, subFont.size()) + 8.0F;

        g.text(font, g.clip(font, setting.getName(), w - bindW - 4.0F), x, g.textY(y, 13.0F, font), ZenithTheme.TEXT_SECONDARY());
        float bindX = x + w - bindW;
        g.rect(bindX, y + 1.0F, bindW, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        g.text(subFont, bindStr, bindX + 4.0F, g.textY(y + 1.0F, 11.0F, subFont), binding ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_PRIMARY());
        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            g.startBind(setting);
            return true;
        });
    }

    private static void renderColor(ZenithGui g, Module ownerModule, ColorSetting setting, float x, float y, float w) {
        var font = g.font(5.8F);
        g.text(font, g.clip(font, setting.getName(), w - 16.0F), x, g.textY(y, 13.0F, font), ZenithTheme.TEXT_SECONDARY());
        g.rect(x + w - 10.0F, y + 2.0F, 9.0F, 8.5F, 2.0F, setting.getValue());
        g.outline(x + w - 10.0F, y + 2.0F, 9.0F, 8.5F, 2.0F, 0.4F, ZenithTheme.BORDER());
        g.hit(x, y, w, 13.0F, button -> {
            g.selectedModule = ownerModule;
            return true;
        });
    }

    // --- 3. RIGHT INSPECTOR PANEL (3D PLAYER & SELECTED MODULE DESCRIPTION) ---
    private static void renderRightInspectorPanel(ZenithGui g, float x, float y, float w, float h) {
        int cardBg = ColorUtil.rgba(18, 20, 28, 150);
        g.rect(x, y, w, h, 7.0F, cardBg);
        g.outline(x, y, w, h, 7.0F, 0.4F, ZenithTheme.BORDER());

        var headerFont = g.font(7.5F);
        g.text(headerFont, "Предосмотр", x + 8.0F, y + 5.0F, ZenithTheme.TEXT_PRIMARY());

        Module sel = g.selectedModule;

        // Flush queued 2D background before extracting 3D entity geometry
        Render2DUtil.flush();

        // 3D Player Entity Inspector
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            try {
                int entityBoxH = (int) (h * 0.65F);
                int scale = (int) (entityBoxH * 0.42F);
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                    error.util.RenderExtend.currentGuiGraphicsExtractor(),
                    (int) (x + 8.0F), (int) (y + 16.0F),
                    (int) (x + w - 8.0F), (int) (y + 16.0F + entityBoxH),
                    scale, 0.0F, (float) g.mouseX, (float) g.mouseY, mc.player
                );
            } catch (Throwable ignored) {}
        }

        // Selected Module Info Card at Bottom of Right Panel
        if (sel != null) {
            float descH = h * 0.26F;
            float descY = y + h - descH - 6.0F;
            g.rect(x + 5.0F, descY, w - 10.0F, descH, 5.0F, ColorUtil.rgba(12, 14, 20, 160));
            g.outline(x + 5.0F, descY, w - 10.0F, descH, 5.0F, 0.4F, ZenithTheme.BORDER());

            String itemTitle = sel.getName().toUpperCase();
            String itemDesc = sel.getDescription() != null ? sel.getDescription() : "Модульный интерфейс клиента";

            var titleFont = g.font(6.5F);
            var subFont = g.font(5.5F);
            g.text(titleFont, g.clip(titleFont, itemTitle, w - 18.0F), x + 9.0F, descY + 4.0F, ZenithTheme.ACCENT());
            g.text(subFont, g.clip(subFont, itemDesc, w - 18.0F), x + 9.0F, descY + 15.0F, ZenithTheme.TEXT_MUTED());
        }
    }

    private static void renderConfigsPage(ZenithGui g, float x, float y, float w, float h) {
        var titleFont = g.font(8.0F);
        g.text(titleFont, "Менеджер Конфигураций", x, y, ZenithTheme.TEXT_PRIMARY());

        ConfigManager cfgManager = Client.INSTANCE != null ? Client.INSTANCE.configManager : null;
        List<String> configs = cfgManager != null ? cfgManager.getAvailableConfigs() : List.of("default");

        float itemY = y + 14.0F;
        float itemH = 22.0F;

        for (String cfgName : configs) {
            boolean active = cfgManager != null && cfgName.equalsIgnoreCase(cfgManager.getCurrentConfig());
            int cardBg = ColorUtil.rgba(20, 22, 32, active ? 160 : 100);

            g.rect(x, itemY, w, itemH, 5.0F, cardBg);
            g.outline(x, itemY, w, itemH, 5.0F, 0.4F, active ? ZenithTheme.ACCENT() : ZenithTheme.BORDER());

            g.text(g.font(7.0F), cfgName, x + 8.0F, itemY + 3.5F, active ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_PRIMARY());
            g.text(g.font(5.5F), active ? "Активный профиль" : "Сохраненный профиль", x + 8.0F, itemY + 12.5F, ZenithTheme.TEXT_MUTED());

            float loadX = x + w - 90.0F;
            g.pill(loadX, itemY + 3.0F, 40.0F, 16.0F, "Загрузить", active, "z_loadcfg" + cfgName, button -> {
                if (cfgManager != null) cfgManager.loadConfig(cfgName, true);
                return true;
            });

            float delX = x + w - 45.0F;
            g.pill(delX, itemY + 3.0F, 40.0F, 16.0F, "Удалить", false, "z_delcfg" + cfgName, button -> {
                if (cfgManager != null) cfgManager.deleteConfig(cfgName);
                return true;
            });

            itemY += itemH + 3.0F;
        }
    }

    private static void renderFriendsPage(ZenithGui g, float x, float y, float w, float h) {
        var titleFont = g.font(8.0F);
        g.text(titleFont, "Список Друзей Клиента", x, y, ZenithTheme.TEXT_PRIMARY());

        FriendManager friendManager = Client.INSTANCE != null ? Client.INSTANCE.friendManager : null;
        java.util.Collection<String> friends = friendManager != null ? friendManager.getFriends() : List.of();

        float itemY = y + 14.0F;
        float itemH = 20.0F;

        if (friends.isEmpty()) {
            g.text(g.font(7.0F), "Список друзей пуст", x, itemY + 10.0F, ZenithTheme.TEXT_MUTED());
        } else {
            for (String friendNick : friends) {
                int cardBg = ColorUtil.rgba(20, 22, 32, 100);
                g.rect(x, itemY, w, itemH, 5.0F, cardBg);
                g.outline(x, itemY, w, itemH, 5.0F, 0.4F, ZenithTheme.BORDER());

                g.text(g.font(7.0F), friendNick, x + 8.0F, itemY + 3.0F, ZenithTheme.TEXT_PRIMARY());
                g.text(g.font(5.5F), "Друг клиента", x + 8.0F, itemY + 11.5F, ZenithTheme.TEXT_MUTED());

                float delX = x + w - 48.0F;
                g.pill(delX, itemY + 2.5F, 42.0F, 15.0F, "Удалить", false, "z_delfriend" + friendNick, button -> {
                    if (friendManager != null) friendManager.removeFriend(friendNick);
                    return true;
                });

                itemY += itemH + 3.0F;
            }
        }
    }

    private static void renderThemesPage(ZenithGui g, float x, float y, float w, float h) {
        var titleFont = g.font(8.0F);
        g.text(titleFont, "Выбор Цветовой Палитры", x, y, ZenithTheme.TEXT_PRIMARY());

        float cardY = y + 14.0F;
        float cardH = 30.0F;
        g.rect(x, cardY, w, cardH, 5.0F, ColorUtil.rgba(20, 22, 32, 100));
        g.outline(x, cardY, w, cardH, 5.0F, 0.4F, ZenithTheme.BORDER());

        g.text(g.font(7.0F), "Акцентный цвет интерфейса", x + 8.0F, cardY + 5.0F, ZenithTheme.TEXT_PRIMARY());
        g.text(g.font(5.5F), "Выберите пресет цвета", x + 8.0F, cardY + 15.0F, ZenithTheme.TEXT_MUTED());

        float boxW = 18.0F;
        float boxH = 12.0F;
        float startBoxX = x + w - (error.ui.modern.ModernTheme.ACCENTS.length * (boxW + 4.0F)) - 8.0F;

        for (int i = 0; i < error.ui.modern.ModernTheme.ACCENTS.length; i++) {
            int color = error.ui.modern.ModernTheme.ACCENTS[i];
            float bx = startBoxX + i * (boxW + 4.0F);

            g.rect(bx, cardY + 9.0F, boxW, boxH, 3.0F, color);
            g.outline(bx, cardY + 9.0F, boxW, boxH, 3.0F, 0.5F, ColorUtil.rgba(255, 255, 255, 60));

            g.hit(bx, cardY + 9.0F, boxW, boxH, button -> {
                Theme.setAccentColor(color);
                return true;
            });
        }
    }
}
