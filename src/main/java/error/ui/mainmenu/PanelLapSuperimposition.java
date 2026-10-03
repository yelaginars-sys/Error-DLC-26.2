package error.ui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PanelLapSuperimposition {
    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "images/logo.png");

    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    // Slider dragging inside popup
    private SliderSetting draggingSlider = null;
    private float draggingSliderX = 0.0F;
    private float draggingSliderW = 0.0F;

    public void render(Minecraft mc, GuiGraphicsExtractor extractor, PanelLapState state, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        state.update();
        float openProgress = state.getOpenAnimation().getValue();
        if (openProgress <= 0.001F) return;

        float winW = 590.0F;
        float winH = 390.0F;
        state.setPanelWidth(winW);
        state.setPanelHeight(winH);

        if (!state.isPositionInitialized()) {
            state.setPanelX((screenWidth - winW) / 2.0F);
            state.setPanelY((screenHeight - winH) / 2.0F);
            state.setPositionInitialized(true);
        }

        if (dragging) {
            state.setPanelX(mouseX - dragOffsetX);
            state.setPanelY(mouseY - dragOffsetY);
        }

        float x = state.getPanelX();
        float y = state.getPanelY();

        float easeProgress = openProgress * openProgress * (3.0F - 2.0F * openProgress);
        float centerX = x + (winW / 2.0F);
        float centerY = y + (winH / 2.0F);
        float scale = 0.94F + (0.06F * easeProgress);
        float alpha = easeProgress;

        // Dark background screen dimming
        int dimCol = ColorUtil.rgba(6, 8, 12, (int) (165 * alpha));
        Render2D.drawRect(0, 0, screenWidth, screenHeight, dimCol);

        extractor.pose().pushMatrix();
        extractor.pose().translate(centerX, centerY);
        extractor.pose().scale(scale, scale);
        extractor.pose().translate(-centerX, -centerY);

        // Outer container
        int mainBg = ColorUtil.rgba(14, 16, 22, (int) (248 * alpha));
        int mainBorder = ColorUtil.rgba(28, 32, 44, (int) (255 * alpha));
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));

        Render2D.drawShadow(x, y, winW, winH, 10.0F, 12.0F, shadowCol);
        Render2D.drawRoundedRect(x, y, winW, winH, 10.0F, mainBg);
        Render2D.drawRoundedOutline(x, y, winW, winH, 10.0F, 1.0F, mainBorder);

        float sideW = 126.0F;
        float headerH = 36.0F;
        float contentX = x + sideW;
        float contentW = winW - sideW;
        float contentY = y + headerH;
        float contentH = winH - headerH;

        // Vertical divider separating sidebar from main area
        Render2D.drawRoundedRect(x + sideW - 1.0F, y, 1.0F, winH, 0.0F, ColorUtil.rgba(24, 28, 38, (int) (220 * alpha)));

        // 1. SIDEBAR
        renderSidebar(state, x, y, sideW, winH, mouseX, mouseY, alpha);

        // 2. HEADER BAR
        renderHeader(state, contentX, y, contentW, headerH, mouseX, mouseY, alpha);

        // 3. MAIN CONTENT (2-column layout or special tabs)
        renderContent(state, contentX, contentY, contentW, contentH, mouseX, mouseY, alpha);

        // 4. FLOATING POPUPS (Settings, Bind, Client settings)
        renderPopups(state, mouseX, mouseY, alpha);

        extractor.pose().popMatrix();
    }

    private void renderSidebar(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        // Logo at top left
        float logoSize = 18.0F;
        Render2D.drawTexture(LOGO_TEX, x + 16.0F, y + 14.0F, logoSize, logoSize, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        float itemX = x + 10.0F;
        float itemW = w - 20.0F;
        float itemH = 21.0F;
        float curY = y + 46.0F;

        // Section: ФУНКЦИИ
        Fonts.drawString(Fonts.SF_MEDIUM, "ФУНКЦИИ", x + 14.0F, curY, 6.0F, ColorUtil.rgba(82, 88, 102, (int) (220 * alpha)));
        curY += 12.0F;

        Category[] funcCats = {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
        String[] funcNames = {"Бой", "Движение", "Визуалы", "Игрок", "Разное"};
        IconUse[] funcIcons = {IconUse.FIGHT, IconUse.MOVEMENT, IconUse.RENDER, IconUse.PLAYER, IconUse.MISC};

        for (int i = 0; i < funcCats.length; i++) {
            Category cat = funcCats[i];
            boolean active = state.getCurrentCategory() == cat;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH;

            if (active) {
                int activeBg = ColorUtil.rgba(26, 36, 60, (int) (230 * alpha));
                int activeBorder = ColorUtil.rgba(46, 62, 100, (int) (200 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, activeBg);
                Render2D.drawRoundedOutline(itemX, curY, itemW, itemH, 5.0F, 1.0F, activeBorder);
            } else if (hovered) {
                int hovBg = ColorUtil.rgba(22, 25, 34, (int) (180 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, hovBg);
            }

            int iconCol = active ? ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)) : ColorUtil.rgba(115, 122, 140, (int) (200 * alpha));
            int textCol = active ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(135, 142, 160, (int) (210 * alpha));

            Fonts.drawIcon(funcIcons[i], itemX + 8.0F, curY + 6.0F, 8.5F, iconCol);
            Fonts.drawString(Fonts.SF_MEDIUM, funcNames[i], itemX + 23.0F, curY + 6.0F, 7.5F, textCol);

            curY += itemH + 3.0F;
        }

        curY += 10.0F;

        // Section: УПРАВЛЕНИЕ
        Fonts.drawString(Fonts.SF_MEDIUM, "УПРАВЛЕНИЕ", x + 14.0F, curY, 6.0F, ColorUtil.rgba(82, 88, 102, (int) (220 * alpha)));
        curY += 12.0F;

        Category[] ctrlCats = {Category.CONFIGS, Category.EVENTS, Category.FRIENDS, Category.COSMETICS};
        String[] ctrlNames = {"Пресеты", "Авто покупка", "Аккаунты", "Скрипты"};
        IconUse[] ctrlIcons = {IconUse.CUBE, IconUse.GLOBE, IconUse.PERSONS, IconUse.SCRIPT};

        for (int i = 0; i < ctrlCats.length; i++) {
            Category cat = ctrlCats[i];
            boolean active = state.getCurrentCategory() == cat;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH;

            if (active) {
                int activeBg = ColorUtil.rgba(26, 36, 60, (int) (230 * alpha));
                int activeBorder = ColorUtil.rgba(46, 62, 100, (int) (200 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, activeBg);
                Render2D.drawRoundedOutline(itemX, curY, itemW, itemH, 5.0F, 1.0F, activeBorder);
            } else if (hovered) {
                int hovBg = ColorUtil.rgba(22, 25, 34, (int) (180 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, hovBg);
            }

            int iconCol = active ? ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)) : ColorUtil.rgba(115, 122, 140, (int) (200 * alpha));
            int textCol = active ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(135, 142, 160, (int) (210 * alpha));

            Fonts.drawIcon(ctrlIcons[i], itemX + 8.0F, curY + 6.0F, 8.5F, iconCol);
            Fonts.drawString(Fonts.SF_MEDIUM, ctrlNames[i], itemX + 23.0F, curY + 6.0F, 7.5F, textCol);

            curY += itemH + 3.0F;
        }
    }

    private void renderHeader(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        // User profile avatar & name
        float avatarX = x + 14.0F;
        float avatarY = y + 8.0F;
        float avatarSize = 20.0F;

        int circleBg = ColorUtil.rgba(28, 32, 44, (int) (255 * alpha));
        Render2D.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0F, circleBg);
        Render2D.drawRoundedOutline(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0F, 1.0F, ColorUtil.rgba(44, 50, 68, (int) (200 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "?", avatarX + 7.5F, avatarY + 5.5F, 8.0F, ColorUtil.rgba(150, 160, 185, (int) (240 * alpha)));

        float textX = avatarX + avatarSize + 8.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "walfini", textX, avatarY + 2.5F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "До 18 января 2038", textX, avatarY + 11.5F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (220 * alpha)));

        // Search Input
        float searchW = 120.0F;
        float searchH = 18.0F;
        float searchX = x + w - searchW - 38.0F;
        float searchY = y + 9.0F;

        int sBg = ColorUtil.rgba(19, 22, 31, (int) (240 * alpha));
        int sBorder = state.isSearchFocused() ? ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)) : ColorUtil.rgba(30, 34, 48, (int) (220 * alpha));
        Render2D.drawRoundedRect(searchX, searchY, searchW, searchH, 5.0F, sBg);
        Render2D.drawRoundedOutline(searchX, searchY, searchW, searchH, 5.0F, 1.0F, sBorder);

        Fonts.drawIcon(IconUse.SEARCH, searchX + 6.0F, searchY + 5.0F, 7.5F, ColorUtil.rgba(100, 108, 126, (int) (220 * alpha)));

        String sQuery = state.getSearchQuery();
        String sDisp = sQuery.isEmpty() ? (state.isSearchFocused() ? "|" : "Поиск") : sQuery;
        int sTextCol = sQuery.isEmpty() && !state.isSearchFocused() ? ColorUtil.rgba(90, 96, 114, (int) (200 * alpha)) : ColorUtil.rgba(230, 235, 250, (int) (255 * alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, sDisp, searchX + 18.0F, searchY + 5.0F, 7.0F, sTextCol);

        // CTRL+F pill badge
        float badgeW = 34.0F;
        float badgeH = 12.0F;
        float badgeX = searchX + searchW - badgeW - 3.0F;
        float badgeY = searchY + 3.0F;
        Render2D.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 3.0F, ColorUtil.rgba(28, 32, 46, (int) (220 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "CTRL+F", badgeX + 4.0F, badgeY + 2.5F, 5.5F, ColorUtil.rgba(120, 128, 148, (int) (220 * alpha)));

        // Client Settings gear button
        float gearX = x + w - 28.0F;
        float gearY = searchY;
        float gearSize = 18.0F;
        boolean gearHover = mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize;
        int gearBg = gearHover ? ColorUtil.rgba(28, 34, 50, (int) (240 * alpha)) : ColorUtil.rgba(19, 22, 31, (int) (220 * alpha));
        Render2D.drawRoundedRect(gearX, gearY, gearSize, gearSize, 5.0F, gearBg);
        Render2D.drawRoundedOutline(gearX, gearY, gearSize, gearSize, 5.0F, 1.0F, ColorUtil.rgba(30, 34, 48, (int) (220 * alpha)));
        Fonts.drawIcon(IconUse.GEAR, gearX + 4.5F, gearY + 4.5F, 8.5F, gearHover ? ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 148, (int) (220 * alpha)));
    }

    private void renderContent(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Category cat = state.getCurrentCategory();

        Render2D.pushScissor(x, y, w, h);
        if (cat == Category.CONFIGS) {
            renderConfigsTab(state, x + 10.0F, y + 10.0F, w - 20.0F, h - 20.0F, mouseX, mouseY, alpha);
        } else if (cat == Category.FRIENDS) {
            renderAccountsTab(state, x + 10.0F, y + 10.0F, w - 20.0F, h - 20.0F, mouseX, mouseY, alpha);
        } else if (cat == Category.COSMETICS) {
            renderScriptsTab(state, x + 10.0F, y + 10.0F, w - 20.0F, h - 20.0F, mouseX, mouseY, alpha);
        } else if (cat == Category.EVENTS) {
            renderAutoBuyTab(state, x + 10.0F, y + 10.0F, w - 20.0F, h - 20.0F, mouseX, mouseY, alpha);
        } else {
            renderModulesTwoColumns(state, cat, x + 10.0F, y + 6.0F, w - 20.0F, h - 12.0F, mouseX, mouseY, alpha);
        }
        Render2D.popScissor();
    }

    private void renderModulesTwoColumns(PanelLapState state, Category cat, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        float colGap = 10.0F;
        float colW = (w - colGap) / 2.0F;

        String leftTitle = "Драка";
        String rightTitle = "Инструменты";

        List<Module> all = getFilteredModules(state, cat);
        List<Module> leftList = new ArrayList<>();
        List<Module> rightList = new ArrayList<>();

        if (cat == Category.RENDER) {
            leftTitle = "Интерфейс";
            rightTitle = "Мир";
            for (Module m : all) {
                if (m.getName().equalsIgnoreCase("HUD") || m.getName().equalsIgnoreCase("Interface") || m.getName().contains("Hud")) {
                    leftList.add(m);
                } else {
                    rightList.add(m);
                }
            }
        } else if (cat == Category.COMBAT) {
            leftTitle = "Драка";
            rightTitle = "Инструменты";
            for (int i = 0; i < all.size(); i++) {
                if (i % 2 == 0) leftList.add(all.get(i));
                else rightList.add(all.get(i));
            }
        } else if (cat == Category.MOVEMENT) {
            leftTitle = "Скорость";
            rightTitle = "Полёт";
            for (int i = 0; i < all.size(); i++) {
                if (i % 2 == 0) leftList.add(all.get(i));
                else rightList.add(all.get(i));
            }
        } else if (cat == Category.PLAYER) {
            leftTitle = "Инвентарь";
            rightTitle = "Действия";
            for (int i = 0; i < all.size(); i++) {
                if (i % 2 == 0) leftList.add(all.get(i));
                else rightList.add(all.get(i));
            }
        } else {
            leftTitle = "Помощники";
            rightTitle = "Утилиты";
            for (int i = 0; i < all.size(); i++) {
                if (i % 2 == 0) leftList.add(all.get(i));
                else rightList.add(all.get(i));
            }
        }

        renderModuleColumnCard(state, leftTitle, leftList, x, y, colW, h, mouseX, mouseY, alpha);
        renderModuleColumnCard(state, rightTitle, rightList, x + colW + colGap, y, colW, h, mouseX, mouseY, alpha);
    }

    private void renderModuleColumnCard(PanelLapState state, String title, List<Module> modules, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        int cardBg = ColorUtil.rgba(18, 20, 28, (int) (240 * alpha));
        int cardBorder = ColorUtil.rgba(28, 32, 44, (int) (220 * alpha));

        Render2D.drawRoundedRect(x, y, w, h, 7.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 7.0F, 1.0F, cardBorder);

        // Header with title and chevron
        float headH = 24.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 10.0F, y + 8.0F, 7.5F, ColorUtil.rgba(220, 225, 240, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "︿ ﹀", x + w - 24.0F, y + 8.0F, 6.0F, ColorUtil.rgba(79, 128, 255, (int) (220 * alpha)));

        Render2D.drawRoundedRect(x + 8.0F, y + headH, w - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(28, 32, 44, (int) (180 * alpha)));

        float rowY = y + headH + 4.0F;
        float rowH = 22.0F;

        if (modules.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Нет модулей", x + 12.0F, rowY + 6.0F, 7.0F, ColorUtil.rgba(90, 96, 114, (int) (180 * alpha)));
            return;
        }

        for (Module mod : modules) {
            if (rowY + rowH > y + h - 4.0F) break;

            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= rowY && mouseY <= rowY + rowH;
            if (hovered) {
                Render2D.drawRoundedRect(x + 4.0F, rowY, w - 8.0F, rowH, 4.0F, ColorUtil.rgba(25, 28, 38, (int) (160 * alpha)));
            }

            // Module name
            int nameCol = mod.isState() ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(180, 185, 200, (int) (220 * alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, mod.getName(), x + 10.0F, rowY + 6.5F, 7.5F, nameCol);

            // Right side buttons
            float toggleW = 20.0F;
            float toggleH = 10.0F;
            float toggleX = x + w - toggleW - 8.0F;
            float toggleY = rowY + 6.0F;

            // Three dots button `...` for settings
            float dotsX = toggleX - 18.0F;
            float dotsY = rowY + 5.0F;
            boolean dotsHover = mouseX >= dotsX && mouseX <= dotsX + 14.0F && mouseY >= dotsY && mouseY <= dotsY + 12.0F;
            int dotsCol = dotsHover ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(90, 98, 120, (int) (200 * alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, "•••", dotsX + 1.0F, dotsY + 1.0F, 6.5F, dotsCol);

            // Bind badge if bound
            if (mod.getBind().isBound()) {
                String bName = mod.getBind().getDisplayValue();
                float bW = Fonts.SF_MEDIUM.getWidth(bName, 5.5F) + 6.0F;
                float bX = dotsX - bW - 4.0F;
                Render2D.drawRoundedRect(bX, rowY + 5.5F, bW, 11.0F, 3.0F, ColorUtil.rgba(30, 36, 52, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, bName, bX + 3.0F, rowY + 7.5F, 5.5F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));
            }

            // Toggle switch pill
            renderToggleSwitch(toggleX, toggleY, toggleW, toggleH, mod.isState(), alpha);

            rowY += rowH + 2.0F;
        }
    }

    private void renderToggleSwitch(float tx, float ty, float tw, float th, boolean state, float alpha) {
        if (state) {
            int onBg = ColorUtil.rgba(79, 128, 255, (int) (245 * alpha));
            Render2D.drawShadow(tx, ty, tw, th, 4.0F, 4.0F, ColorUtil.rgba(79, 128, 255, (int) (110 * alpha)));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, onBg);
            Render2D.drawRoundedRect(tx + tw - th + 1.5F, ty + 1.5F, th - 3.0F, th - 3.0F, (th - 3.0F) / 2.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
        } else {
            int offBg = ColorUtil.rgba(28, 32, 44, (int) (240 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, offBg);
            Render2D.drawRoundedOutline(tx, ty, tw, th, th / 2.0F, 1.0F, ColorUtil.rgba(42, 48, 64, (int) (200 * alpha)));
            Render2D.drawRoundedRect(tx + 1.5F, ty + 1.5F, th - 3.0F, th - 3.0F, (th - 3.0F) / 2.0F, ColorUtil.rgba(90, 96, 114, (int) (220 * alpha)));
        }
    }

    private void renderPopups(PanelLapState state, int mouseX, int mouseY, float alpha) {
        // 1. Module Settings Popup
        if (state.getActiveModuleSettings() != null) {
            renderModuleSettingsModal(state, state.getActiveModuleSettings(), mouseX, mouseY, alpha);
        }

        // 2. Keybind Popup
        if (state.getActiveModuleBind() != null) {
            renderModuleBindModal(state, state.getActiveModuleBind(), mouseX, mouseY, alpha);
        }

        // 3. Client Settings Popup
        if (state.isClientSettingsOpen()) {
            renderClientSettingsModal(state, mouseX, mouseY, alpha);
        }
    }

    private void renderModuleSettingsModal(PanelLapState state, Module mod, int mouseX, int mouseY, float alpha) {
        float popW = 185.0F;
        float popX = state.getPopupX();
        float popY = state.getPopupY();

        List<Setting<?>> settings = mod.getSettings();
        float popH = 34.0F + Math.max(1, settings.size()) * 21.0F + 8.0F;

        // Clamp to stay within bounds
        popX = Math.max(state.getPanelX() + 10.0F, Math.min(popX, state.getPanelX() + state.getPanelWidth() - popW - 10.0F));
        popY = Math.max(state.getPanelY() + 10.0F, Math.min(popY, state.getPanelY() + state.getPanelHeight() - popH - 10.0F));

        int bg = ColorUtil.rgba(18, 20, 27, (int) (250 * alpha));
        int border = ColorUtil.rgba(36, 42, 58, (int) (240 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 7.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 7.0F, 1.0F, border);

        // Header
        Fonts.drawString(Fonts.SF_MEDIUM, ":: " + mod.getName(), popX + 10.0F, popY + 7.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, mod.getCategory().getDisplayName(), popX + 10.0F, popY + 16.5F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (220 * alpha)));

        // Close button ✕
        float closeX = popX + popW - 18.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "✕", closeX, closeY, 7.0F, closeHover ? ColorUtil.rgba(255, 100, 100, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 148, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 28.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(28, 32, 44, (int) (200 * alpha)));

        float rowY = popY + 33.0F;
        float rowH = 20.0F;

        if (settings.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Нет настроек", popX + 12.0F, rowY + 5.0F, 7.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));
            return;
        }

        for (Setting<?> s : settings) {
            String sName = s.getName();
            Fonts.drawString(Fonts.SF_MEDIUM, sName, popX + 10.0F, rowY + 5.5F, 7.0F, ColorUtil.rgba(200, 205, 220, (int) (240 * alpha)));

            if (s instanceof CheckBox cb) {
                float tw = 18.0F;
                float th = 9.0F;
                float tx = popX + popW - tw - 10.0F;
                float ty = rowY + 5.5F;
                renderToggleSwitch(tx, ty, tw, th, cb.getValue(), alpha);
            } else if (s instanceof SliderSetting sl) {
                float valTextW = Fonts.SF_MEDIUM.getWidth(String.format(Locale.ROOT, "%.1f", sl.getValue()), 6.5F);
                Fonts.drawString(Fonts.SF_MEDIUM, String.format(Locale.ROOT, "%.1f", sl.getValue()), popX + popW - valTextW - 10.0F, rowY + 5.5F, 6.5F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));
            } else if (s instanceof ModeSetting ms) {
                String mVal = ms.getValue();
                float valW = Fonts.SF_MEDIUM.getWidth(mVal, 6.5F) + 12.0F;
                float bX = popX + popW - valW - 10.0F;
                Render2D.drawRoundedRect(bX, rowY + 4.0F, valW, 12.0F, 3.0F, ColorUtil.rgba(26, 32, 46, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, mVal + " ⌵", bX + 3.0F, rowY + 6.5F, 6.0F, ColorUtil.rgba(180, 190, 215, (int) (240 * alpha)));
            } else if (s instanceof MultiModeSetting mms) {
                String mVal = mms.getValue().isEmpty() ? "Все" : mms.getValue().get(0);
                float valW = Fonts.SF_MEDIUM.getWidth(mVal, 6.5F) + 12.0F;
                float bX = popX + popW - valW - 10.0F;
                Render2D.drawRoundedRect(bX, rowY + 4.0F, valW, 12.0F, 3.0F, ColorUtil.rgba(26, 32, 46, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, mVal + " ⌵", bX + 3.0F, rowY + 6.5F, 6.0F, ColorUtil.rgba(180, 190, 215, (int) (240 * alpha)));
            }

            rowY += rowH;
        }
    }

    private void renderModuleBindModal(PanelLapState state, Module mod, int mouseX, int mouseY, float alpha) {
        float popW = 160.0F;
        float popH = 92.0F;
        float popX = state.getPopupX();
        float popY = state.getPopupY();

        popX = Math.max(state.getPanelX() + 10.0F, Math.min(popX, state.getPanelX() + state.getPanelWidth() - popW - 10.0F));
        popY = Math.max(state.getPanelY() + 10.0F, Math.min(popY, state.getPanelY() + state.getPanelHeight() - popH - 10.0F));

        int bg = ColorUtil.rgba(18, 20, 27, (int) (250 * alpha));
        int border = ColorUtil.rgba(36, 42, 58, (int) (240 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 7.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 7.0F, 1.0F, border);

        // Header
        Fonts.drawString(Fonts.SF_MEDIUM, ":: " + mod.getName(), popX + 10.0F, popY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float closeX = popX + popW - 18.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "✕", closeX, closeY, 7.0F, closeHover ? ColorUtil.rgba(255, 100, 100, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 148, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 22.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(28, 32, 44, (int) (200 * alpha)));

        // Row 1: Бинд
        Fonts.drawString(Fonts.SF_MEDIUM, "Бинд", popX + 10.0F, popY + 29.0F, 7.0F, ColorUtil.rgba(180, 185, 200, (int) (240 * alpha)));
        String bKey = state.isListeningBind() ? "..." : (mod.getBind().isBound() ? mod.getBind().getDisplayValue() : "n/a");
        float bW = 26.0F;
        float bX = popX + popW - bW - 10.0F;
        Render2D.drawRoundedRect(bX, popY + 27.0F, bW, 11.0F, 3.0F, ColorUtil.rgba(28, 32, 46, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, bKey, bX + bW / 2.0F, popY + 29.0F, 6.0F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));

        // Row 2: Видимость
        Fonts.drawString(Fonts.SF_MEDIUM, "Видимость", popX + 10.0F, popY + 48.0F, 7.0F, ColorUtil.rgba(180, 185, 200, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 28.0F, popY + 47.0F, 18.0F, 9.0F, true, alpha);

        // Row 3: Тип (Hold / Toggle chips)
        Fonts.drawString(Fonts.SF_MEDIUM, "Тип", popX + 10.0F, popY + 68.0F, 7.0F, ColorUtil.rgba(180, 185, 200, (int) (240 * alpha)));

        float chipW = 28.0F;
        float chipH = 12.0F;
        float chipY = popY + 66.0F;

        // Hold chip
        float holdX = popX + popW - (chipW * 2.0F) - 14.0F;
        Render2D.drawRoundedRect(holdX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(24, 28, 38, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Hold", holdX + chipW / 2.0F, chipY + 2.5F, 5.5F, ColorUtil.rgba(120, 128, 148, (int) (200 * alpha)));

        // Toggle chip (active by default)
        float toggleChipX = holdX + chipW + 4.0F;
        Render2D.drawRoundedRect(toggleChipX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Toggle", toggleChipX + chipW / 2.0F, chipY + 2.5F, 5.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }

    private void renderClientSettingsModal(PanelLapState state, int mouseX, int mouseY, float alpha) {
        float popW = 170.0F;
        float popH = 80.0F;
        float popX = state.getPanelX() + state.getPanelWidth() - popW - 10.0F;
        float popY = state.getPanelY() + 40.0F;

        int bg = ColorUtil.rgba(18, 20, 27, (int) (250 * alpha));
        int border = ColorUtil.rgba(36, 42, 58, (int) (240 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 7.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 7.0F, 1.0F, border);

        Fonts.drawString(Fonts.SF_MEDIUM, ":: Настройки клиента", popX + 10.0F, popY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float closeX = popX + popW - 18.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "✕", closeX, closeY, 7.0F, closeHover ? ColorUtil.rgba(255, 100, 100, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 148, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 22.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(28, 32, 44, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Звуки кликов", popX + 10.0F, popY + 30.0F, 7.0F, ColorUtil.rgba(180, 185, 200, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 28.0F, popY + 29.0F, 18.0F, 9.0F, true, alpha);

        Fonts.drawString(Fonts.SF_MEDIUM, "Размытие фона", popX + 10.0F, popY + 50.0F, 7.0F, ColorUtil.rgba(180, 185, 200, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 28.0F, popY + 49.0F, 18.0F, 9.0F, true, alpha);
    }

    private void renderConfigsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Пресеты и конфигурации", x + 10.0F, y + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Управление локальными пресетами и облачными кодами", x + 10.0F, y + 22.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        // Config item card
        float cardY = y + 40.0F;
        Render2D.drawRoundedRect(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, ColorUtil.rgba(20, 24, 34, (int) (220 * alpha)));
        Render2D.drawRoundedOutline(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, 1.0F, ColorUtil.rgba(32, 38, 54, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Default.cfg", x + 20.0F, cardY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Базовая конфигурация клиента", x + 20.0F, cardY + 18.0F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float loadBtnW = 42.0F;
        float loadBtnX = x + w - 20.0F - loadBtnW - 8.0F;
        Render2D.drawRoundedRect(loadBtnX, cardY + 9.0F, loadBtnW, 16.0F, 4.0F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + loadBtnW / 2.0F, cardY + 13.5F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }

    private void renderAccountsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Аккаунты", x + 10.0F, y + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Управление учётными записями Minecraft", x + 10.0F, y + 22.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float cardY = y + 40.0F;
        Render2D.drawRoundedRect(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, ColorUtil.rgba(20, 24, 34, (int) (220 * alpha)));
        Render2D.drawRoundedOutline(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, 1.0F, ColorUtil.rgba(32, 38, 54, (int) (200 * alpha)));

        String name = Minecraft.getInstance().getUser().getName();
        Fonts.drawString(Fonts.SF_MEDIUM, name, x + 20.0F, cardY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Активный оффлайн аккаунт", x + 20.0F, cardY + 18.0F, 6.0F, ColorUtil.rgba(79, 128, 255, (int) (220 * alpha)));
    }

    private void renderScriptsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Скрипты", x + 10.0F, y + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Пользовательские скрипты расширения", x + 10.0F, y + 22.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Папка со скриптами пуста (.minecraft/error/scripts)", x + 10.0F, y + 50.0F, 7.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));
    }

    private void renderAutoBuyTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Авто покупка", x + 10.0F, y + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Автоматическая скупка предметов на аукционе", x + 10.0F, y + 22.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Настройте список целевых предметов для AHHelper", x + 10.0F, y + 50.0F, 7.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));
    }

    private List<Module> getFilteredModules(PanelLapState state, Category cat) {
        String q = state.getSearchQuery().trim().toLowerCase();
        List<Module> list = new ArrayList<>();
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            if (m.getCategory() == cat) {
                if (q.isEmpty() || m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q)) {
                    list.add(m);
                }
            }
        }
        return list;
    }

    public boolean handleMouseButton(PanelLapState state, int mouseX, int mouseY, int button, int action) {
        if (!state.isInteractive()) return false;

        float winW = state.getPanelWidth();
        float winH = state.getPanelHeight();
        float x = state.getPanelX();
        float y = state.getPanelY();

        // 1. Check clicks in Modals
        if (state.getActiveModuleSettings() != null) {
            float popW = 185.0F;
            float popX = state.getPopupX();
            float popY = state.getPopupY();
            List<Setting<?>> settings = state.getActiveModuleSettings().getSettings();
            float popH = 34.0F + Math.max(1, settings.size()) * 21.0F + 8.0F;

            popX = Math.max(x + 10.0F, Math.min(popX, x + winW - popW - 10.0F));
            popY = Math.max(y + 10.0F, Math.min(popY, y + winH - popH - 10.0F));

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    // Close button
                    float closeX = popX + popW - 18.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F) {
                        state.setActiveModuleSettings(null);
                        return true;
                    }

                    // Settings interaction
                    float rowY = popY + 33.0F;
                    float rowH = 20.0F;
                    for (Setting<?> s : settings) {
                        if (mouseY >= rowY && mouseY <= rowY + rowH) {
                            if (s instanceof CheckBox cb) {
                                cb.setValue(!cb.getValue());
                                return true;
                            } else if (s instanceof ModeSetting ms) {
                                ms.cycle();
                                return true;
                            }
                        }
                        rowY += rowH;
                    }
                }
                return true;
            } else if (action == GLFW.GLFW_PRESS) {
                state.setActiveModuleSettings(null);
                return true;
            }
        }

        if (state.getActiveModuleBind() != null) {
            float popW = 160.0F;
            float popH = 92.0F;
            float popX = state.getPopupX();
            float popY = state.getPopupY();

            popX = Math.max(x + 10.0F, Math.min(popX, x + winW - popW - 10.0F));
            popY = Math.max(y + 10.0F, Math.min(popY, y + winH - popH - 10.0F));

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    // Close button
                    float closeX = popX + popW - 18.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F) {
                        state.setActiveModuleBind(null);
                        state.setListeningBind(false);
                        return true;
                    }

                    // Bind button click
                    float bW = 26.0F;
                    float bX = popX + popW - bW - 10.0F;
                    if (mouseX >= bX && mouseX <= bX + bW && mouseY >= popY + 27.0F && mouseY <= popY + 38.0F) {
                        state.setListeningBind(true);
                        return true;
                    }
                }
                return true;
            } else if (action == GLFW.GLFW_PRESS) {
                state.setActiveModuleBind(null);
                state.setListeningBind(false);
                return true;
            }
        }

        if (state.isClientSettingsOpen()) {
            float popW = 170.0F;
            float popH = 80.0F;
            float popX = x + winW - popW - 10.0F;
            float popY = y + 40.0F;

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    float closeX = popX + popW - 18.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F) {
                        state.setClientSettingsOpen(false);
                        return true;
                    }
                }
                return true;
            } else if (action == GLFW.GLFW_PRESS) {
                state.setClientSettingsOpen(false);
                return true;
            }
        }

        // 2. Dragging header window
        if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= x && mouseX <= x + winW && mouseY >= y && mouseY <= y + 36.0F) {
                // Header buttons check
                float sideW = 126.0F;
                float contentW = winW - sideW;
                float gearX = x + winW - 28.0F;
                float searchW = 120.0F;
                float searchX = x + winW - searchW - 38.0F;

                if (mouseX >= gearX && mouseX <= gearX + 18.0F && mouseY >= y + 9.0F && mouseY <= y + 27.0F) {
                    state.setClientSettingsOpen(!state.isClientSettingsOpen());
                    return true;
                }

                if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y + 9.0F && mouseY <= y + 27.0F) {
                    state.setSearchFocused(true);
                    return true;
                }

                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            dragging = false;
        }

        // 3. Sidebar clicks
        float sideW = 126.0F;
        if (mouseX >= x && mouseX <= x + sideW && action == GLFW.GLFW_PRESS) {
            float itemX = x + 10.0F;
            float itemW = sideW - 20.0F;
            float itemH = 21.0F;
            float curY = y + 46.0F + 12.0F;

            Category[] funcCats = {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
            for (Category cat : funcCats) {
                if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH) {
                    state.switchCategory(cat);
                    return true;
                }
                curY += itemH + 3.0F;
            }

            curY += 10.0F + 12.0F;
            Category[] ctrlCats = {Category.CONFIGS, Category.EVENTS, Category.FRIENDS, Category.COSMETICS};
            for (Category cat : ctrlCats) {
                if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH) {
                    state.switchCategory(cat);
                    return true;
                }
                curY += itemH + 3.0F;
            }
        }

        // 4. Content Area Module Clicks
        float contentX = x + sideW;
        float contentW = winW - sideW;
        float contentY = y + 36.0F;
        float contentH = winH - 36.0F;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            float colGap = 10.0F;
            float colW = (contentW - 20.0F - colGap) / 2.0F;

            List<Module> all = getFilteredModules(state, state.getCurrentCategory());
            List<Module> leftList = new ArrayList<>();
            List<Module> rightList = new ArrayList<>();

            if (state.getCurrentCategory() == Category.RENDER) {
                for (Module m : all) {
                    if (m.getName().equalsIgnoreCase("HUD") || m.getName().equalsIgnoreCase("Interface") || m.getName().contains("Hud")) {
                        leftList.add(m);
                    } else {
                        rightList.add(m);
                    }
                }
            } else {
                for (int i = 0; i < all.size(); i++) {
                    if (i % 2 == 0) leftList.add(all.get(i));
                    else rightList.add(all.get(i));
                }
            }

            if (handleColumnModuleClicks(state, leftList, contentX + 10.0F, contentY + 6.0F, colW, contentH - 12.0F, mouseX, mouseY, button, action)) {
                return true;
            }
            if (handleColumnModuleClicks(state, rightList, contentX + 10.0F + colW + colGap, contentY + 6.0F, colW, contentH - 12.0F, mouseX, mouseY, button, action)) {
                return true;
            }
        }

        return false;
    }

    private boolean handleColumnModuleClicks(PanelLapState state, List<Module> modules, float x, float y, float w, float h, int mouseX, int mouseY, int button, int action) {
        float headH = 24.0F;
        float rowY = y + headH + 4.0F;
        float rowH = 22.0F;

        for (Module mod : modules) {
            if (rowY + rowH > y + h - 4.0F) break;

            if (mouseX >= x && mouseX <= x + w && mouseY >= rowY && mouseY <= rowY + rowH) {
                if (action == GLFW.GLFW_PRESS) {
                    float toggleW = 20.0F;
                    float toggleH = 10.0F;
                    float toggleX = x + w - toggleW - 8.0F;
                    float dotsX = toggleX - 18.0F;

                    // Middle Click (СКМ / колесико) -> Open Bind Modal
                    if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        state.setActiveModuleBind(mod);
                        state.setPopupX(mouseX);
                        state.setPopupY(mouseY);
                        return true;
                    }

                    // Right Click (ПКМ) -> Open Module Settings
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        state.setActiveModuleSettings(mod);
                        state.setPopupX(mouseX);
                        state.setPopupY(mouseY);
                        return true;
                    }

                    // Left Click on `...` dots
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= dotsX && mouseX <= dotsX + 14.0F) {
                        state.setActiveModuleSettings(mod);
                        state.setPopupX(mouseX);
                        state.setPopupY(mouseY);
                        return true;
                    }

                    // Left Click on toggle switch or anywhere on row
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        mod.toggle();
                        return true;
                    }
                }
            }
            rowY += rowH + 2.0F;
        }
        return false;
    }

    public boolean handleKey(PanelLapState state, int key, int scanCode, int modifiers) {
        if (state.isListeningBind() && state.getActiveModuleBind() != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                state.getActiveModuleBind().getBind().clear();
            } else {
                state.getActiveModuleBind().getBind().setSingle(key);
            }
            state.setListeningBind(false);
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (state.getActiveModuleSettings() != null) {
                state.setActiveModuleSettings(null);
                return true;
            }
            if (state.getActiveModuleBind() != null) {
                state.setActiveModuleBind(null);
                return true;
            }
            if (state.isClientSettingsOpen()) {
                state.setClientSettingsOpen(false);
                return true;
            }
            state.beginClose();
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
            state.setSearchFocused(true);
            return true;
        }

        if (state.isSearchFocused()) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String q = state.getSearchQuery();
                if (!q.isEmpty()) {
                    state.setSearchQuery(q.substring(0, q.length() - 1));
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                state.setSearchFocused(false);
                return true;
            }
        }

        return false;
    }

    public void handleChar(PanelLapState state, int codePoint) {
        if (state.isSearchFocused()) {
            char c = (char) codePoint;
            if (c >= 32 && c != 127) {
                state.setSearchQuery(state.getSearchQuery() + c);
            }
        }
    }

    public void handleScroll(PanelLapState state, double vertical, int mouseX, int mouseY) {
        state.scroll((float) (vertical * 20.0D));
    }
}