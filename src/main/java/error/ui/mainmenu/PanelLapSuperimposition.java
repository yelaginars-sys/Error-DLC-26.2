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
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PanelLapSuperimposition {
    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/icons/union.png");
    private static final Identifier ARROW_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/icons/arrow.png");
    private static final Identifier DOTS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/icons/dots.png");
    private static final Identifier CROSS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/icons/cross.png");

    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

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
        int mainBorder = ColorUtil.rgba(26, 30, 40, (int) (255 * alpha));
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));

        Render2D.drawShadow(x, y, winW, winH, 10.0F, 12.0F, shadowCol);
        Render2D.drawRoundedRect(x, y, winW, winH, 10.0F, mainBg);
        Render2D.drawRoundedOutline(x, y, winW, winH, 10.0F, 1.0F, mainBorder);

        float sideW = 120.0F;
        float headerH = 36.0F;
        float contentX = x + sideW;
        float contentW = winW - sideW;
        float contentY = y + headerH;
        float contentH = winH - headerH;

        // Vertical divider separating sidebar from main area
        Render2D.drawRoundedRect(x + sideW - 1.0F, y, 1.0F, winH, 0.0F, ColorUtil.rgba(22, 25, 34, (int) (220 * alpha)));

        // 1. SIDEBAR
        renderSidebar(state, x, y, sideW, winH, mouseX, mouseY, alpha);

        // 2. HEADER BAR
        renderHeader(state, contentX, y, contentW, headerH, mouseX, mouseY, alpha);

        // 3. MAIN CONTENT (stacked cards layout)
        renderContent(state, contentX, contentY, contentW, contentH, mouseX, mouseY, alpha);

        // 4. FLOATING POPUPS
        renderPopups(state, mouseX, mouseY, alpha);

        extractor.pose().popMatrix();
    }

    private void renderSidebar(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        // Top logo (Nursultan Union logo / Error logo)
        float logoSize = 16.0F;
        Render2D.drawTexture(LOGO_TEX, x + 18.0F, y + 14.0F, logoSize, logoSize, ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)));

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
                int activeBg = ColorUtil.rgba(24, 34, 56, (int) (230 * alpha));
                int activeBorder = ColorUtil.rgba(42, 58, 92, (int) (200 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, activeBg);
                Render2D.drawRoundedOutline(itemX, curY, itemW, itemH, 5.0F, 1.0F, activeBorder);
            } else if (hovered) {
                int hovBg = ColorUtil.rgba(20, 23, 31, (int) (180 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, hovBg);
            }

            int iconCol = active ? ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)) : ColorUtil.rgba(105, 112, 130, (int) (200 * alpha));
            int textCol = active ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 146, (int) (210 * alpha));

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
                int activeBg = ColorUtil.rgba(24, 34, 56, (int) (230 * alpha));
                int activeBorder = ColorUtil.rgba(42, 58, 92, (int) (200 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, activeBg);
                Render2D.drawRoundedOutline(itemX, curY, itemW, itemH, 5.0F, 1.0F, activeBorder);
            } else if (hovered) {
                int hovBg = ColorUtil.rgba(20, 23, 31, (int) (180 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 5.0F, hovBg);
            }

            int iconCol = active ? ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)) : ColorUtil.rgba(105, 112, 130, (int) (200 * alpha));
            int textCol = active ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 146, (int) (210 * alpha));

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

        int circleBg = ColorUtil.rgba(26, 30, 42, (int) (255 * alpha));
        Render2D.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0F, circleBg);
        Render2D.drawRoundedOutline(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0F, 1.0F, ColorUtil.rgba(40, 46, 62, (int) (200 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "?", avatarX + 7.5F, avatarY + 5.5F, 8.0F, ColorUtil.rgba(150, 160, 185, (int) (240 * alpha)));

        float textX = avatarX + avatarSize + 8.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "walfini", textX, avatarY + 2.5F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "До 18 января 2038", textX, avatarY + 11.5F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (220 * alpha)));

        // Search Input
        float searchW = 120.0F;
        float searchH = 18.0F;
        float searchX = x + w - searchW - 36.0F;
        float searchY = y + 9.0F;

        int sBg = ColorUtil.rgba(18, 20, 28, (int) (240 * alpha));
        int sBorder = state.isSearchFocused() ? ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)) : ColorUtil.rgba(28, 32, 44, (int) (220 * alpha));
        Render2D.drawRoundedRect(searchX, searchY, searchW, searchH, 5.0F, sBg);
        Render2D.drawRoundedOutline(searchX, searchY, searchW, searchH, 5.0F, 1.0F, sBorder);

        Fonts.drawIcon(IconUse.SEARCH, searchX + 6.0F, searchY + 5.0F, 7.5F, ColorUtil.rgba(100, 108, 126, (int) (220 * alpha)));

        String sQuery = state.getSearchQuery();
        String sDisp = sQuery.isEmpty() ? (state.isSearchFocused() ? "|" : "Поиск") : sQuery;
        int sTextCol = sQuery.isEmpty() && !state.isSearchFocused() ? ColorUtil.rgba(85, 92, 110, (int) (200 * alpha)) : ColorUtil.rgba(230, 235, 250, (int) (255 * alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, sDisp, searchX + 18.0F, searchY + 5.0F, 7.0F, sTextCol);

        // CTRL+F pill badge
        float badgeW = 34.0F;
        float badgeH = 12.0F;
        float badgeX = searchX + searchW - badgeW - 3.0F;
        float badgeY = searchY + 3.0F;
        Render2D.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 3.0F, ColorUtil.rgba(26, 30, 42, (int) (220 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "CTRL+F", badgeX + 4.0F, badgeY + 2.5F, 5.5F, ColorUtil.rgba(115, 124, 142, (int) (220 * alpha)));

        // Client Settings gear button
        float gearX = x + w - 26.0F;
        float gearY = searchY;
        float gearSize = 18.0F;
        boolean gearHover = mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize;
        int gearBg = gearHover ? ColorUtil.rgba(26, 32, 48, (int) (240 * alpha)) : ColorUtil.rgba(18, 20, 28, (int) (220 * alpha));
        Render2D.drawRoundedRect(gearX, gearY, gearSize, gearSize, 5.0F, gearBg);
        Render2D.drawRoundedOutline(gearX, gearY, gearSize, gearSize, 5.0F, 1.0F, ColorUtil.rgba(28, 32, 44, (int) (220 * alpha)));
        Fonts.drawIcon(IconUse.GEAR, gearX + 4.5F, gearY + 4.5F, 8.5F, gearHover ? ColorUtil.rgba(79, 128, 255, (int) (255 * alpha)) : ColorUtil.rgba(115, 124, 142, (int) (220 * alpha)));
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

    private static class CardGroup {
        String title;
        List<Module> modules = new ArrayList<>();
        CardGroup(String title) { this.title = title; }
    }

    private void renderModulesTwoColumns(PanelLapState state, Category cat, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        float colGap = 8.0F;
        float colW = (w - colGap) / 2.0F;

        List<CardGroup> leftGroups = new ArrayList<>();
        List<CardGroup> rightGroups = new ArrayList<>();

        List<Module> all = getFilteredModules(state, cat);

        if (cat == Category.COMBAT) {
            CardGroup draka = new CardGroup("Драка");
            CardGroup bazovye = new CardGroup("Базовые");
            CardGroup instr = new CardGroup("Инструменты");
            CardGroup ostalnoe = new CardGroup("Остальное");

            for (Module m : all) {
                String n = m.getName().toLowerCase();
                if (n.contains("aura") || n.contains("trigger") || n.contains("aim") || n.contains("explosion") || n.contains("crystal")) {
                    draka.modules.add(m);
                } else if (n.contains("swap") || n.contains("release") || n.contains("totem")) {
                    bazovye.modules.add(m);
                } else if (n.contains("mace") || n.contains("wind") || n.contains("throw") || n.contains("critical")) {
                    instr.modules.add(m);
                } else {
                    ostalnoe.modules.add(m);
                }
            }

            // Fallbacks so groups are never empty if modules exist
            if (draka.modules.isEmpty() && !all.isEmpty()) draka.modules.addAll(all.subList(0, Math.min(4, all.size())));
            leftGroups.add(draka);
            if (!bazovye.modules.isEmpty()) leftGroups.add(bazovye);

            if (instr.modules.isEmpty() && all.size() > 4) instr.modules.addAll(all.subList(4, all.size()));
            rightGroups.add(instr);
            if (!ostalnoe.modules.isEmpty()) rightGroups.add(ostalnoe);

        } else if (cat == Category.RENDER) {
            CardGroup iface = new CardGroup("Интерфейс");
            CardGroup mir = new CardGroup("Мир");

            for (Module m : all) {
                String n = m.getName().toLowerCase();
                if (n.contains("hud") || n.contains("interface") || n.contains("click") || n.contains("notif")) {
                    iface.modules.add(m);
                } else {
                    mir.modules.add(m);
                }
            }
            leftGroups.add(iface);
            rightGroups.add(mir);

        } else if (cat == Category.MOVEMENT) {
            CardGroup speed = new CardGroup("Скорость");
            CardGroup flight = new CardGroup("Полёт");

            for (Module m : all) {
                String n = m.getName().toLowerCase();
                if (n.contains("sprint") || n.contains("speed") || n.contains("timer")) {
                    speed.modules.add(m);
                } else {
                    flight.modules.add(m);
                }
            }
            leftGroups.add(speed);
            rightGroups.add(flight);

        } else if (cat == Category.PLAYER) {
            CardGroup inv = new CardGroup("Инвентарь");
            CardGroup acts = new CardGroup("Действия");

            for (Module m : all) {
                String n = m.getName().toLowerCase();
                if (n.contains("swap") || n.contains("tool") || n.contains("pearl") || n.contains("item")) {
                    inv.modules.add(m);
                } else {
                    acts.modules.add(m);
                }
            }
            leftGroups.add(inv);
            rightGroups.add(acts);

        } else {
            CardGroup helpers = new CardGroup("Помощники");
            CardGroup utils = new CardGroup("Утилиты");

            for (int i = 0; i < all.size(); i++) {
                if (i % 2 == 0) helpers.modules.add(all.get(i));
                else utils.modules.add(all.get(i));
            }
            leftGroups.add(helpers);
            rightGroups.add(utils);
        }

        renderStackedCardGroups(state, leftGroups, x, y, colW, h, mouseX, mouseY, alpha);
        renderStackedCardGroups(state, rightGroups, x + colW + colGap, y, colW, h, mouseX, mouseY, alpha);
    }

    private void renderStackedCardGroups(PanelLapState state, List<CardGroup> groups, float x, float y, float w, float maxH, int mouseX, int mouseY, float alpha) {
        float curY = y;
        for (CardGroup g : groups) {
            if (curY > y + maxH - 24.0F) break;
            float cardH = 22.0F + (Math.max(1, g.modules.size()) * 20.0F) + 6.0F;
            cardH = Math.min(cardH, y + maxH - curY);

            renderSingleCard(state, g.title, g.modules, x, curY, w, cardH, mouseX, mouseY, alpha);
            curY += cardH + 8.0F;
        }
    }

    private void renderSingleCard(PanelLapState state, String title, List<Module> modules, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        int cardBg = ColorUtil.rgba(16, 18, 25, (int) (245 * alpha));
        int cardBorder = ColorUtil.rgba(24, 28, 38, (int) (255 * alpha));

        Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 1.0F, cardBorder);

        // Header
        float headH = 21.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 9.0F, y + 6.5F, 7.5F, ColorUtil.rgba(225, 230, 245, (int) (245 * alpha)));

        // Arrow chevron texture
        float arrowSize = 9.0F;
        Render2D.drawTexture(ARROW_TEX, x + w - arrowSize - 8.0F, y + 6.0F, arrowSize, arrowSize, ColorUtil.rgba(79, 128, 255, (int) (220 * alpha)));

        float rowY = y + headH + 2.0F;
        float rowH = 20.0F;

        if (modules.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Нет модулей", x + 10.0F, rowY + 5.0F, 7.0F, ColorUtil.rgba(85, 92, 110, (int) (180 * alpha)));
            return;
        }

        for (Module mod : modules) {
            if (rowY + rowH > y + h - 2.0F) break;

            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= rowY && mouseY <= rowY + rowH;
            if (hovered) {
                Render2D.drawRoundedRect(x + 3.0F, rowY, w - 6.0F, rowH, 4.0F, ColorUtil.rgba(22, 25, 35, (int) (160 * alpha)));
            }

            int nameCol = mod.isState() ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(130, 138, 155, (int) (220 * alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, mod.getName(), x + 9.0F, rowY + 5.5F, 7.0F, nameCol);

            float toggleW = 18.0F;
            float toggleH = 9.0F;
            float toggleX = x + w - toggleW - 8.0F;
            float toggleY = rowY + 5.5F;

            // Dots `•••` texture
            float dotsX = toggleX - 16.0F;
            float dotsY = rowY + 8.5F;
            boolean dotsHover = mouseX >= dotsX && mouseX <= dotsX + 12.0F && mouseY >= dotsY - 2.0F && mouseY <= dotsY + 10.0F;
            int dotsCol = dotsHover ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(80, 88, 105, (int) (200 * alpha));
            Render2D.drawTexture(DOTS_TEX, dotsX, dotsY, 10.0F, 3.0F, dotsCol);

            // Bind icon if bound
            if (mod.getBind().isBound()) {
                String bName = mod.getBind().getDisplayValue();
                float bW = Fonts.SF_MEDIUM.getWidth(bName, 5.0F) + 5.0F;
                float bX = dotsX - bW - 4.0F;
                Render2D.drawRoundedRect(bX, rowY + 4.5F, bW, 10.0F, 2.5F, ColorUtil.rgba(28, 34, 48, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, bName, bX + 2.5F, rowY + 6.5F, 5.0F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));
            }

            // Toggle switch pill
            renderToggleSwitch(toggleX, toggleY, toggleW, toggleH, mod.isState(), alpha);

            rowY += rowH;
        }
    }

    private void renderToggleSwitch(float tx, float ty, float tw, float th, boolean state, float alpha) {
        if (state) {
            int onBg = ColorUtil.rgba(75, 119, 242, (int) (250 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, onBg);
            Render2D.drawRoundedRect(tx + tw - th + 1.0F, ty + 1.0F, th - 2.0F, th - 2.0F, (th - 2.0F) / 2.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
        } else {
            int offBg = ColorUtil.rgba(25, 28, 38, (int) (240 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, offBg);
            Render2D.drawRoundedOutline(tx, ty, tw, th, th / 2.0F, 0.8F, ColorUtil.rgba(42, 48, 64, (int) (200 * alpha)));
            Render2D.drawRoundedRect(tx + 1.5F, ty + 1.5F, th - 3.0F, th - 3.0F, (th - 3.0F) / 2.0F, ColorUtil.rgba(70, 76, 92, (int) (220 * alpha)));
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
        float popH = 34.0F + Math.max(1, settings.size()) * 20.0F + 6.0F;

        popX = Math.max(state.getPanelX() + 10.0F, Math.min(popX, state.getPanelX() + state.getPanelWidth() - popW - 10.0F));
        popY = Math.max(state.getPanelY() + 10.0F, Math.min(popY, state.getPanelY() + state.getPanelHeight() - popH - 10.0F));

        int bg = ColorUtil.rgba(16, 18, 25, (int) (250 * alpha));
        int border = ColorUtil.rgba(32, 38, 52, (int) (240 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 7.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 7.0F, 1.0F, border);

        // Header
        Fonts.drawString(Fonts.SF_MEDIUM, ":: " + mod.getName(), popX + 10.0F, popY + 7.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, mod.getCategory().getDisplayName(), popX + 10.0F, popY + 16.5F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (220 * alpha)));

        // Close button (cross.png)
        float closeX = popX + popW - 16.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 8.0F && mouseY >= closeY && mouseY <= closeY + 8.0F;
        Render2D.drawTexture(CROSS_TEX, closeX, closeY, 8.0F, 8.0F, closeHover ? ColorUtil.rgba(255, 90, 90, (int) (255 * alpha)) : ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 27.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

        float rowY = popY + 32.0F;
        float rowH = 19.0F;

        if (settings.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Нет настроек", popX + 12.0F, rowY + 5.0F, 7.0F, ColorUtil.rgba(90, 96, 114, (int) (200 * alpha)));
            return;
        }

        for (Setting<?> s : settings) {
            String sName = s.getName();
            Fonts.drawString(Fonts.SF_MEDIUM, sName, popX + 10.0F, rowY + 5.0F, 6.5F, ColorUtil.rgba(190, 195, 210, (int) (240 * alpha)));

            if (s instanceof CheckBox cb) {
                float tw = 17.0F;
                float th = 8.5F;
                float tx = popX + popW - tw - 10.0F;
                float ty = rowY + 5.0F;
                renderToggleSwitch(tx, ty, tw, th, cb.getValue(), alpha);
            } else if (s instanceof SliderSetting sl) {
                float valTextW = Fonts.SF_MEDIUM.getWidth(String.format(Locale.ROOT, "%.1f", sl.getValue()), 6.5F);
                Fonts.drawString(Fonts.SF_MEDIUM, String.format(Locale.ROOT, "%.1f", sl.getValue()), popX + popW - valTextW - 10.0F, rowY + 5.0F, 6.5F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));
            } else if (s instanceof ModeSetting ms) {
                String mVal = ms.getValue();
                float valW = Fonts.SF_MEDIUM.getWidth(mVal, 6.0F) + 12.0F;
                float bX = popX + popW - valW - 10.0F;
                Render2D.drawRoundedRect(bX, rowY + 3.5F, valW, 11.5F, 3.0F, ColorUtil.rgba(24, 28, 40, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, mVal + " ⌵", bX + 3.0F, rowY + 5.5F, 5.5F, ColorUtil.rgba(175, 185, 205, (int) (240 * alpha)));
            } else if (s instanceof MultiModeSetting mms) {
                String mVal = mms.getValue().isEmpty() ? "Все" : mms.getValue().get(0);
                float valW = Fonts.SF_MEDIUM.getWidth(mVal, 6.0F) + 12.0F;
                float bX = popX + popW - valW - 10.0F;
                Render2D.drawRoundedRect(bX, rowY + 3.5F, valW, 11.5F, 3.0F, ColorUtil.rgba(24, 28, 40, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, mVal + " ⌵", bX + 3.0F, rowY + 5.5F, 5.5F, ColorUtil.rgba(175, 185, 205, (int) (240 * alpha)));
            }

            rowY += rowH;
        }
    }

    private void renderModuleBindModal(PanelLapState state, Module mod, int mouseX, int mouseY, float alpha) {
        float popW = 155.0F;
        float popH = 88.0F;
        float popX = state.getPopupX();
        float popY = state.getPopupY();

        popX = Math.max(state.getPanelX() + 10.0F, Math.min(popX, state.getPanelX() + state.getPanelWidth() - popW - 10.0F));
        popY = Math.max(state.getPanelY() + 10.0F, Math.min(popY, state.getPanelY() + state.getPanelHeight() - popH - 10.0F));

        int bg = ColorUtil.rgba(16, 18, 25, (int) (250 * alpha));
        int border = ColorUtil.rgba(32, 38, 52, (int) (240 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 7.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 7.0F, 1.0F, border);

        // Header
        Fonts.drawString(Fonts.SF_MEDIUM, ":: " + mod.getName(), popX + 10.0F, popY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float closeX = popX + popW - 16.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 8.0F && mouseY >= closeY && mouseY <= closeY + 8.0F;
        Render2D.drawTexture(CROSS_TEX, closeX, closeY, 8.0F, 8.0F, closeHover ? ColorUtil.rgba(255, 90, 90, (int) (255 * alpha)) : ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 22.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

        // Row 1: Бинд
        Fonts.drawString(Fonts.SF_MEDIUM, "Бинд", popX + 10.0F, popY + 28.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));
        String bKey = state.isListeningBind() ? "..." : (mod.getBind().isBound() ? mod.getBind().getDisplayValue() : "n/a");
        float bW = 26.0F;
        float bX = popX + popW - bW - 10.0F;
        Render2D.drawRoundedRect(bX, popY + 26.0F, bW, 11.0F, 3.0F, ColorUtil.rgba(26, 30, 42, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, bKey, bX + bW / 2.0F, popY + 28.0F, 5.5F, ColorUtil.rgba(79, 128, 255, (int) (240 * alpha)));

        // Row 2: Видимость
        Fonts.drawString(Fonts.SF_MEDIUM, "Видимость", popX + 10.0F, popY + 46.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 27.0F, popY + 45.0F, 17.0F, 8.5F, true, alpha);

        // Row 3: Тип (Hold / Toggle chips)
        Fonts.drawString(Fonts.SF_MEDIUM, "Тип", popX + 10.0F, popY + 65.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));

        float chipW = 26.0F;
        float chipH = 11.5F;
        float chipY = popY + 63.0F;

        float holdX = popX + popW - (chipW * 2.0F) - 13.0F;
        Render2D.drawRoundedRect(holdX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(22, 26, 36, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Hold", holdX + chipW / 2.0F, chipY + 2.5F, 5.0F, ColorUtil.rgba(110, 118, 136, (int) (200 * alpha)));

        float toggleChipX = holdX + chipW + 3.0F;
        Render2D.drawRoundedRect(toggleChipX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(75, 119, 242, (int) (240 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Toggle", toggleChipX + chipW / 2.0F, chipY + 2.5F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }

    private void renderClientSettingsModal(PanelLapState state, int mouseX, int mouseY, float alpha) {
        float popW = 165.0F;
        float popH = 75.0F;
        float popX = state.getPanelX() + state.getPanelWidth() - popW - 10.0F;
        float popY = state.getPanelY() + 40.0F;

        int bg = ColorUtil.rgba(16, 18, 25, (int) (250 * alpha));
        int border = ColorUtil.rgba(32, 38, 52, (int) (240 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 7.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 7.0F, 1.0F, border);

        Fonts.drawString(Fonts.SF_MEDIUM, ":: Настройки клиента", popX + 10.0F, popY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float closeX = popX + popW - 16.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 8.0F && mouseY >= closeY && mouseY <= closeY + 8.0F;
        Render2D.drawTexture(CROSS_TEX, closeX, closeY, 8.0F, 8.0F, closeHover ? ColorUtil.rgba(255, 90, 90, (int) (255 * alpha)) : ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 22.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Звуки кликов", popX + 10.0F, popY + 30.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 27.0F, popY + 29.0F, 17.0F, 8.5F, true, alpha);

        Fonts.drawString(Fonts.SF_MEDIUM, "Размытие фона", popX + 10.0F, popY + 48.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 27.0F, popY + 47.0F, 17.0F, 8.5F, true, alpha);
    }

    private void renderConfigsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Пресеты и конфигурации", x + 10.0F, y + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Управление локальными пресетами и облачными кодами", x + 10.0F, y + 22.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float cardY = y + 40.0F;
        Render2D.drawRoundedRect(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, ColorUtil.rgba(16, 18, 25, (int) (240 * alpha)));
        Render2D.drawRoundedOutline(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, 1.0F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Default.cfg", x + 20.0F, cardY + 8.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Базовая конфигурация клиента", x + 20.0F, cardY + 18.0F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float loadBtnW = 42.0F;
        float loadBtnX = x + w - 20.0F - loadBtnW - 8.0F;
        Render2D.drawRoundedRect(loadBtnX, cardY + 9.0F, loadBtnW, 16.0F, 4.0F, ColorUtil.rgba(75, 119, 242, (int) (240 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + loadBtnW / 2.0F, cardY + 13.5F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }

    private void renderAccountsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Аккаунты", x + 10.0F, y + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Управление учётными записями Minecraft", x + 10.0F, y + 22.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float cardY = y + 40.0F;
        Render2D.drawRoundedRect(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, ColorUtil.rgba(16, 18, 25, (int) (240 * alpha)));
        Render2D.drawRoundedOutline(x + 10.0F, cardY, w - 20.0F, 34.0F, 6.0F, 1.0F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

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

        // 1. Modals
        if (state.getActiveModuleSettings() != null) {
            float popW = 185.0F;
            float popX = state.getPopupX();
            float popY = state.getPopupY();
            List<Setting<?>> settings = state.getActiveModuleSettings().getSettings();
            float popH = 34.0F + Math.max(1, settings.size()) * 20.0F + 6.0F;

            popX = Math.max(x + 10.0F, Math.min(popX, x + winW - popW - 10.0F));
            popY = Math.max(y + 10.0F, Math.min(popY, y + winH - popH - 10.0F));

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    float closeX = popX + popW - 16.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F) {
                        state.setActiveModuleSettings(null);
                        return true;
                    }

                    float rowY = popY + 32.0F;
                    float rowH = 19.0F;
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
            float popW = 155.0F;
            float popH = 88.0F;
            float popX = state.getPopupX();
            float popY = state.getPopupY();

            popX = Math.max(x + 10.0F, Math.min(popX, x + winW - popW - 10.0F));
            popY = Math.max(y + 10.0F, Math.min(popY, y + winH - popH - 10.0F));

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    float closeX = popX + popW - 16.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX && mouseX <= closeX + 12.0F && mouseY >= closeY && mouseY <= closeY + 12.0F) {
                        state.setActiveModuleBind(null);
                        state.setListeningBind(false);
                        return true;
                    }

                    float bW = 26.0F;
                    float bX = popX + popW - bW - 10.0F;
                    if (mouseX >= bX && mouseX <= bX + bW && mouseY >= popY + 26.0F && mouseY <= popY + 37.0F) {
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
            float popW = 165.0F;
            float popH = 75.0F;
            float popX = x + winW - popW - 10.0F;
            float popY = y + 40.0F;

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    float closeX = popX + popW - 16.0F;
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
                float gearX = x + winW - 26.0F;
                float searchW = 120.0F;
                float searchX = x + winW - searchW - 36.0F;

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
        float sideW = 120.0F;
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
            float colGap = 8.0F;
            float colW = (contentW - 20.0F - colGap) / 2.0F;

            List<Module> all = getFilteredModules(state, state.getCurrentCategory());

            if (checkColumnModuleInteraction(state, all, contentX + 10.0F, contentY + 6.0F, colW * 2.0F + colGap, contentH - 12.0F, mouseX, mouseY, button, action)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkColumnModuleInteraction(PanelLapState state, List<Module> all, float x, float y, float totalW, float h, int mouseX, int mouseY, int button, int action) {
        if (action != GLFW.GLFW_PRESS) return false;

        float colGap = 8.0F;
        float colW = (totalW - colGap) / 2.0F;

        float headH = 21.0F;
        float rowH = 20.0F;

        for (int i = 0; i < all.size(); i++) {
            Module mod = all.get(i);
            int col = i % 2;
            int row = i / 2;

            float curX = x + col * (colW + colGap);
            float curY = y + headH + (row * rowH);

            if (mouseX >= curX && mouseX <= curX + colW && mouseY >= curY && mouseY <= curY + rowH) {
                float toggleW = 18.0F;
                float toggleX = curX + colW - toggleW - 8.0F;
                float dotsX = toggleX - 16.0F;

                if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    state.setActiveModuleBind(mod);
                    state.setPopupX(mouseX);
                    state.setPopupY(mouseY);
                    return true;
                }

                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    state.setActiveModuleSettings(mod);
                    state.setPopupX(mouseX);
                    state.setPopupY(mouseY);
                    return true;
                }

                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (mouseX >= dotsX && mouseX <= dotsX + 14.0F) {
                        state.setActiveModuleSettings(mod);
                        state.setPopupX(mouseX);
                        state.setPopupY(mouseY);
                    } else {
                        mod.toggle();
                    }
                    return true;
                }
            }
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