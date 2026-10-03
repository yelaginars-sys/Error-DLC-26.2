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
    private static final Identifier CROSS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/icons/cross.png");

    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    public void render(Minecraft mc, GuiGraphicsExtractor extractor, PanelLapState state, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        state.update();
        float openProgress = state.getOpenAnimation().getValue();
        if (openProgress <= 0.001F) return;

        // Exact Nursultan window proportions (490x350)
        float winW = 490.0F;
        float winH = 350.0F;
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
        float scale = 0.95F + (0.05F * easeProgress);
        float alpha = easeProgress;

        // Dark background dimming
        int dimCol = ColorUtil.rgba(6, 8, 12, (int) (165 * alpha));
        Render2D.drawRect(0, 0, screenWidth, screenHeight, dimCol);

        extractor.pose().pushMatrix();
        extractor.pose().translate(centerX, centerY);
        extractor.pose().scale(scale, scale);
        extractor.pose().translate(-centerX, -centerY);

        // Main outer frame
        int mainBg = ColorUtil.rgba(15, 16, 21, (int) (248 * alpha));
        int mainBorder = ColorUtil.rgba(25, 27, 34, (int) (255 * alpha));
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (190 * alpha));

        Render2D.drawShadow(x, y, winW, winH, 8.0F, 12.0F, shadowCol);
        Render2D.drawRoundedRect(x, y, winW, winH, 8.0F, mainBg);
        Render2D.drawRoundedOutline(x, y, winW, winH, 8.0F, 1.0F, mainBorder);

        float sideW = 114.0F;
        float headerH = 32.0F;
        float contentX = x + sideW;
        float contentW = winW - sideW;
        float contentY = y + headerH;
        float contentH = winH - headerH;

        // Vertical sidebar separator line
        Render2D.drawRoundedRect(x + sideW - 1.0F, y, 1.0F, winH, 0.0F, ColorUtil.rgba(22, 24, 32, (int) (220 * alpha)));

        // 1. SIDEBAR
        renderSidebar(state, x, y, sideW, winH, mouseX, mouseY, alpha);

        // 2. HEADER
        renderHeader(state, contentX, y, contentW, headerH, mouseX, mouseY, alpha);

        // 3. MAIN CONTENT (Stacked symmetrical cards)
        renderContent(state, contentX, contentY, contentW, contentH, mouseX, mouseY, alpha);

        // 4. FLOATING POPUPS
        renderPopups(state, mouseX, mouseY, alpha);

        extractor.pose().popMatrix();
    }

    private void renderSidebar(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        // Nursultan Union logo icon
        float logoSize = 15.0F;
        float logoX = x + (w - logoSize) / 2.0F;
        Render2D.drawTexture(LOGO_TEX, logoX, y + 14.0F, logoSize, logoSize, ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)));

        float itemX = x + 9.0F;
        float itemW = w - 18.0F;
        float itemH = 20.0F;
        float curY = y + 42.0F;

        // Section: ФУНКЦИИ
        Fonts.drawString(Fonts.SF_MEDIUM, "ФУНКЦИИ", x + 12.0F, curY, 5.5F, ColorUtil.rgba(78, 84, 102, (int) (220 * alpha)));
        curY += 11.0F;

        Category[] funcCats = {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
        String[] funcNames = {"Бой", "Движение", "Визуалы", "Игрок", "Разное"};
        IconUse[] funcIcons = {IconUse.FIGHT, IconUse.MOVEMENT, IconUse.RENDER, IconUse.PLAYER, IconUse.MISC};

        for (int i = 0; i < funcCats.length; i++) {
            Category cat = funcCats[i];
            boolean active = state.getCurrentCategory() == cat;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH;

            if (active) {
                int activeBg = ColorUtil.rgba(23, 32, 54, (int) (240 * alpha));
                int activeBorder = ColorUtil.rgba(37, 52, 86, (int) (220 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 4.5F, activeBg);
                Render2D.drawRoundedOutline(itemX, curY, itemW, itemH, 4.5F, 1.0F, activeBorder);
            } else if (hovered) {
                int hovBg = ColorUtil.rgba(18, 21, 30, (int) (180 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 4.5F, hovBg);
            }

            int iconCol = active ? ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)) : ColorUtil.rgba(90, 98, 118, (int) (200 * alpha));
            int textCol = active ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(110, 118, 136, (int) (210 * alpha));

            Fonts.drawIcon(funcIcons[i], itemX + 7.0F, curY + 6.0F, 7.5F, iconCol);
            Fonts.drawString(Fonts.SF_MEDIUM, funcNames[i], itemX + 21.0F, curY + 6.0F, 7.0F, textCol);

            curY += itemH + 2.5F;
        }

        curY += 8.0F;

        // Section: УПРАВЛЕНИЕ
        Fonts.drawString(Fonts.SF_MEDIUM, "УПРАВЛЕНИЕ", x + 12.0F, curY, 5.5F, ColorUtil.rgba(78, 84, 102, (int) (220 * alpha)));
        curY += 11.0F;

        Category[] ctrlCats = {Category.CONFIGS, Category.EVENTS, Category.FRIENDS, Category.COSMETICS};
        String[] ctrlNames = {"Пресеты", "Авто покупка", "Аккаунты", "Скрипты"};
        IconUse[] ctrlIcons = {IconUse.CUBE, IconUse.GLOBE, IconUse.PERSONS, IconUse.SCRIPT};

        for (int i = 0; i < ctrlCats.length; i++) {
            Category cat = ctrlCats[i];
            boolean active = state.getCurrentCategory() == cat;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH;

            if (active) {
                int activeBg = ColorUtil.rgba(23, 32, 54, (int) (240 * alpha));
                int activeBorder = ColorUtil.rgba(37, 52, 86, (int) (220 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 4.5F, activeBg);
                Render2D.drawRoundedOutline(itemX, curY, itemW, itemH, 4.5F, 1.0F, activeBorder);
            } else if (hovered) {
                int hovBg = ColorUtil.rgba(18, 21, 30, (int) (180 * alpha));
                Render2D.drawRoundedRect(itemX, curY, itemW, itemH, 4.5F, hovBg);
            }

            int iconCol = active ? ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)) : ColorUtil.rgba(90, 98, 118, (int) (200 * alpha));
            int textCol = active ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(110, 118, 136, (int) (210 * alpha));

            Fonts.drawIcon(ctrlIcons[i], itemX + 7.0F, curY + 6.0F, 7.5F, iconCol);
            Fonts.drawString(Fonts.SF_MEDIUM, ctrlNames[i], itemX + 21.0F, curY + 6.0F, 7.0F, textCol);

            curY += itemH + 2.5F;
        }
    }

    private void renderHeader(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        // User profile avatar & name
        float avatarX = x + 12.0F;
        float avatarY = y + 7.0F;
        float avatarSize = 18.0F;

        int circleBg = ColorUtil.rgba(24, 28, 38, (int) (255 * alpha));
        Render2D.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0F, circleBg);
        Render2D.drawRoundedOutline(avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0F, 1.0F, ColorUtil.rgba(36, 42, 58, (int) (200 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "?", avatarX + 6.5F, avatarY + 4.5F, 7.5F, ColorUtil.rgba(140, 150, 175, (int) (240 * alpha)));

        float textX = avatarX + avatarSize + 7.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "walfini", textX, avatarY + 1.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "До 18 января 2038", textX, avatarY + 10.0F, 5.5F, ColorUtil.rgba(92, 99, 118, (int) (220 * alpha)));

        // Search Input
        float searchW = 115.0F;
        float searchH = 17.0F;
        float searchX = x + w - searchW - 32.0F;
        float searchY = y + 7.5F;

        int sBg = ColorUtil.rgba(18, 21, 30, (int) (240 * alpha));
        int sBorder = state.isSearchFocused() ? ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)) : ColorUtil.rgba(26, 30, 42, (int) (220 * alpha));
        Render2D.drawRoundedRect(searchX, searchY, searchW, searchH, 4.5F, sBg);
        Render2D.drawRoundedOutline(searchX, searchY, searchW, searchH, 4.5F, 1.0F, sBorder);

        Fonts.drawIcon(IconUse.SEARCH, searchX + 5.5F, searchY + 4.5F, 7.0F, ColorUtil.rgba(92, 99, 118, (int) (220 * alpha)));

        String sQuery = state.getSearchQuery();
        String sDisp = sQuery.isEmpty() ? (state.isSearchFocused() ? "|" : "Поиск") : sQuery;
        int sTextCol = sQuery.isEmpty() && !state.isSearchFocused() ? ColorUtil.rgba(80, 86, 104, (int) (200 * alpha)) : ColorUtil.rgba(230, 235, 250, (int) (255 * alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, sDisp, searchX + 16.0F, searchY + 4.5F, 6.5F, sTextCol);

        // CTRL+F pill badge
        float badgeW = 30.0F;
        float badgeH = 11.0F;
        float badgeX = searchX + searchW - badgeW - 3.0F;
        float badgeY = searchY + 3.0F;
        Render2D.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 2.5F, ColorUtil.rgba(24, 28, 40, (int) (220 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "CTRL+F", badgeX + 3.5F, badgeY + 2.0F, 5.0F, ColorUtil.rgba(110, 118, 136, (int) (220 * alpha)));

        // Client Settings gear button
        float gearX = x + w - 24.0F;
        float gearY = searchY;
        float gearSize = 17.0F;
        boolean gearHover = mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize;
        int gearBg = gearHover ? ColorUtil.rgba(24, 30, 44, (int) (240 * alpha)) : ColorUtil.rgba(18, 21, 30, (int) (220 * alpha));
        Render2D.drawRoundedRect(gearX, gearY, gearSize, gearSize, 4.5F, gearBg);
        Render2D.drawRoundedOutline(gearX, gearY, gearSize, gearSize, 4.5F, 1.0F, ColorUtil.rgba(26, 30, 42, (int) (220 * alpha)));
        Fonts.drawIcon(IconUse.GEAR, gearX + 4.0F, gearY + 4.0F, 8.0F, gearHover ? ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)) : ColorUtil.rgba(110, 118, 136, (int) (220 * alpha)));
    }

    private void renderContent(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Category cat = state.getCurrentCategory();

        Render2D.pushScissor(x, y, w, h);
        if (cat == Category.CONFIGS) {
            renderConfigsTab(state, x + 8.0F, y + 8.0F, w - 16.0F, h - 16.0F, mouseX, mouseY, alpha);
        } else if (cat == Category.FRIENDS) {
            renderAccountsTab(state, x + 8.0F, y + 8.0F, w - 16.0F, h - 16.0F, mouseX, mouseY, alpha);
        } else if (cat == Category.COSMETICS) {
            renderScriptsTab(state, x + 8.0F, y + 8.0F, w - 16.0F, h - 16.0F, mouseX, mouseY, alpha);
        } else if (cat == Category.EVENTS) {
            renderAutoBuyTab(state, x + 8.0F, y + 8.0F, w - 16.0F, h - 16.0F, mouseX, mouseY, alpha);
        } else {
            renderModulesTwoColumns(state, cat, x + 8.0F, y + 6.0F, w - 16.0F, h - 12.0F, mouseX, mouseY, alpha);
        }
        Render2D.popScissor();
    }

    private static class DisplayItem {
        String displayName;
        Module module;
        DisplayItem(String displayName, Module module) {
            this.displayName = displayName;
            this.module = module;
        }
    }

    private static class CardGroup {
        String title;
        List<DisplayItem> items = new ArrayList<>();
        CardGroup(String title) { this.title = title; }
    }

    private void renderModulesTwoColumns(PanelLapState state, Category cat, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        float colGap = 8.0F;
        float colW = (w - colGap) / 2.0F;

        List<CardGroup> leftGroups = new ArrayList<>();
        List<CardGroup> rightGroups = new ArrayList<>();

        if (cat == Category.COMBAT) {
            // Exactly matching Image 2!
            CardGroup draka = new CardGroup("Драка");
            addItem(draka, "Attack Aura", findMod("AuraModule", "AttackAura", "Aura"));
            addItem(draka, "No Velocity", findMod("AntiPush"));
            addItem(draka, "Trigger Bot", findMod("TriggerBot"));
            addItem(draka, "Aim Assist", findMod("AimAssistant"));
            addItem(draka, "Auto Explosion", findMod("AutoExplosion"));
            addItem(draka, "Crystal Aura", findMod("CrystalAura"));
            leftGroups.add(draka);

            CardGroup bazovye = new CardGroup("Базовые");
            addItem(bazovye, "Auto Swap", findMod("AutoSwap"));
            addItem(bazovye, "Item Release", findMod("ExpThrow"));
            leftGroups.add(bazovye);

            CardGroup instr = new CardGroup("Инструменты");
            addItem(instr, "Sprint Reset", findMod("MaceHelper"));
            addItem(instr, "Tape Mouse", findMod("NoDelay"));
            addItem(instr, "Backtrack", findMod("Predictions"));
            addItem(instr, "Web Trap", findMod("WebTrap"));
            addItem(instr, "Knockback Swap", findMod("WindCharge"));
            rightGroups.add(instr);

            CardGroup ostalnoe = new CardGroup("Остальное");
            addItem(ostalnoe, "No Slot Change", findMod("HoldMyItems"));
            addItem(ostalnoe, "Anti Bot", findMod("ClickFriend"));
            addItem(ostalnoe, "No Friend Damage", findMod("Criticals"));
            rightGroups.add(ostalnoe);

        } else if (cat == Category.RENDER) {
            CardGroup iface = new CardGroup("Интерфейс");
            addItem(iface, "HUD", findMod("Interface", "HUD"));
            addItem(iface, "Notifications", findMod("Notification"));
            addItem(iface, "ClickGUI", findMod("ClickGui"));
            leftGroups.add(iface);

            CardGroup mir = new CardGroup("Мир");
            addItem(mir, "Ambience", findMod("Ambience"));
            addItem(mir, "Better Minecraft", findMod("BetterMinecraft"));
            addItem(mir, "Block Highlight", findMod("BlockHighlight"));
            addItem(mir, "Block Outline", findMod("BlockOutline"));
            addItem(mir, "Custom Models", findMod("CustomModels"));
            addItem(mir, "Firework ESP", findMod("FireworkESP"));
            addItem(mir, "Predictions", findMod("Predictions"));
            addItem(mir, "Swing Animation", findMod("SwingAnimation"));
            addItem(mir, "View Model", findMod("ViewModel"));
            addItem(mir, "World Particles", findMod("WorldParticles"));
            rightGroups.add(mir);

        } else if (cat == Category.MOVEMENT) {
            CardGroup speed = new CardGroup("Скорость");
            addItem(speed, "Sprint", findMod("Sprint"));
            addItem(speed, "Water Speed", findMod("WaterSpeed"));
            addItem(speed, "Timer", findMod("Timer"));
            leftGroups.add(speed);

            CardGroup flight = new CardGroup("Полёт");
            addItem(flight, "Elytra Booster", findMod("ElytraBooster"));
            addItem(flight, "Elytra Motion", findMod("ElytraMotion"));
            addItem(flight, "No Fall", findMod("NoFall"));
            rightGroups.add(flight);

        } else if (cat == Category.PLAYER) {
            CardGroup inv = new CardGroup("Инвентарь");
            addItem(inv, "Auto Swap", findMod("AutoSwap"));
            addItem(inv, "Auto Tool", findMod("AutoTool"));
            addItem(inv, "Click Pearl", findMod("ClickPearl"));
            addItem(inv, "Elytra Swap", findMod("ElytraSwap"));
            addItem(inv, "Hold My Items", findMod("HoldMyItems"));
            leftGroups.add(inv);

            CardGroup acts = new CardGroup("Действия");
            addItem(acts, "Anti Push", findMod("AntiPush"));
            addItem(acts, "Gui Walk", findMod("GuiWalk"));
            addItem(acts, "Air Stuck", findMod("AirStuck"));
            rightGroups.add(acts);

        } else {
            CardGroup helpers = new CardGroup("Помощники");
            addItem(helpers, "AH Helper", findMod("AHHelper"));
            addItem(helpers, "Server Helper", findMod("ServerHelper"));
            addItem(helpers, "Auto Accept", findMod("AutoAccept"));
            addItem(helpers, "Auto Captcha", findMod("AutoCaptcha"));
            addItem(helpers, "Auto Msg", findMod("AutoMsg"));
            addItem(helpers, "Auto Sell", findMod("AutoSell"));
            leftGroups.add(helpers);

            CardGroup utils = new CardGroup("Утилиты");
            addItem(utils, "Free Cam", findMod("FreeCam"));
            addItem(utils, "Full Bright", findMod("FullBright"));
            addItem(utils, "Click Friend", findMod("ClickFriend"));
            addItem(utils, "Client Sounds", findMod("ClientSounds"));
            addItem(utils, "IRC", findMod("IRC"));
            addItem(utils, "Item Scroller", findMod("ItemScroller"));
            addItem(utils, "UnHook", findMod("UnHook"));
            rightGroups.add(utils);
        }

        renderStackedCardGroups(state, leftGroups, x, y, colW, h, mouseX, mouseY, alpha);
        renderStackedCardGroups(state, rightGroups, x + colW + colGap, y, colW, h, mouseX, mouseY, alpha);
    }

    private void addItem(CardGroup group, String displayName, Module m) {
        if (m != null) {
            group.items.add(new DisplayItem(displayName, m));
        }
    }

    private Module findMod(String... names) {
        for (String n : names) {
            Module m = Client.INSTANCE.moduleManager.getModule(n);
            if (m != null) return m;
        }
        return null;
    }

    private void renderStackedCardGroups(PanelLapState state, List<CardGroup> groups, float x, float y, float w, float maxH, int mouseX, int mouseY, float alpha) {
        float curY = y;
        for (CardGroup g : groups) {
            if (curY > y + maxH - 24.0F) break;
            float cardH = 20.0F + (Math.max(1, g.items.size()) * 19.0F) + 5.0F;
            cardH = Math.min(cardH, y + maxH - curY);

            renderSingleCard(state, g.title, g.items, x, curY, w, cardH, mouseX, mouseY, alpha);
            curY += cardH + 8.0F;
        }
    }

    private void renderSingleCard(PanelLapState state, String title, List<DisplayItem> items, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        int cardBg = ColorUtil.rgba(18, 19, 26, (int) (248 * alpha));
        int cardBorder = ColorUtil.rgba(27, 29, 38, (int) (255 * alpha));

        Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 1.0F, cardBorder);

        // Header
        float headH = 20.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 9.0F, y + 6.0F, 7.0F, ColorUtil.rgba(225, 230, 245, (int) (245 * alpha)));

        // Elegant double chevron vector arrows `︿ ﹀` in soft blue
        float cx = x + w - 14.0F;
        float cy = y + 6.0F;
        int chevCol = ColorUtil.rgba(75, 124, 248, (int) (220 * alpha));
        Render2D.drawRoundedRect(cx, cy + 1.0F, 3.5F, 1.0F, 0.5F, chevCol);
        Render2D.drawRoundedRect(cx + 2.5F, cy, 3.5F, 1.0F, 0.5F, chevCol);
        Render2D.drawRoundedRect(cx, cy + 4.5F, 3.5F, 1.0F, 0.5F, chevCol);
        Render2D.drawRoundedRect(cx + 2.5F, cy + 5.5F, 3.5F, 1.0F, 0.5F, chevCol);

        float rowY = y + headH + 1.0F;
        float rowH = 19.0F;

        if (items.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Нет модулей", x + 10.0F, rowY + 5.0F, 6.5F, ColorUtil.rgba(85, 92, 110, (int) (180 * alpha)));
            return;
        }

        for (DisplayItem item : items) {
            if (rowY + rowH > y + h - 1.0F) break;

            Module mod = item.module;
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= rowY && mouseY <= rowY + rowH;
            if (hovered) {
                Render2D.drawRoundedRect(x + 3.0F, rowY, w - 6.0F, rowH, 3.5F, ColorUtil.rgba(23, 26, 36, (int) (160 * alpha)));
            }

            int nameCol = mod.isState() ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(120, 128, 146, (int) (220 * alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, item.displayName, x + 9.0F, rowY + 5.5F, 6.5F, nameCol);

            float toggleW = 17.0F;
            float toggleH = 9.0F;
            float toggleX = x + w - toggleW - 8.0F;
            float toggleY = rowY + 5.0F;

            // Crisp circular three dots `• • •`
            float dotsX = toggleX - 15.0F;
            float dotsY = rowY + 5.0F;
            boolean dotsHover = mouseX >= dotsX - 2.0F && mouseX <= dotsX + 11.0F && mouseY >= dotsY && mouseY <= dotsY + 9.0F;
            int dotsCol = dotsHover ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(80, 88, 105, (int) (200 * alpha));
            for (int d = 0; d < 3; d++) {
                Render2D.drawCircle(dotsX + d * 3.5F, dotsY + 4.5F, 0.9F, dotsCol);
            }

            // Keyboard bind badge if bound
            if (mod.getBind().isBound()) {
                String bName = mod.getBind().getDisplayValue();
                float bW = Fonts.SF_MEDIUM.getWidth(bName, 4.5F) + 5.0F;
                float bX = dotsX - bW - 4.0F;
                Render2D.drawRoundedRect(bX, rowY + 4.5F, bW, 9.5F, 2.5F, ColorUtil.rgba(26, 32, 46, (int) (220 * alpha)));
                Fonts.drawString(Fonts.SF_MEDIUM, bName, bX + 2.5F, rowY + 6.0F, 4.5F, ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)));
            }

            // Toggle switch pill
            renderToggleSwitch(toggleX, toggleY, toggleW, toggleH, mod.isState(), alpha);

            rowY += rowH;
        }
    }

    private void renderToggleSwitch(float tx, float ty, float tw, float th, boolean state, float alpha) {
        if (state) {
            int onBg = ColorUtil.rgba(64, 112, 255, (int) (250 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, onBg);
            Render2D.drawCircle(tx + tw - th / 2.0F, ty + th / 2.0F, 3.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
        } else {
            int offBg = ColorUtil.rgba(25, 28, 37, (int) (240 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, offBg);
            Render2D.drawRoundedOutline(tx, ty, tw, th, th / 2.0F, 0.8F, ColorUtil.rgba(38, 42, 55, (int) (200 * alpha)));
            Render2D.drawCircle(tx + th / 2.0F, ty + th / 2.0F, 2.8F, ColorUtil.rgba(58, 64, 82, (int) (240 * alpha)));
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
                Fonts.drawString(Fonts.SF_MEDIUM, String.format(Locale.ROOT, "%.1f", sl.getValue()), popX + popW - valTextW - 10.0F, rowY + 5.0F, 6.5F, ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)));
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
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, bKey, bX + bW / 2.0F, popY + 28.0F, 5.5F, ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)));

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
        Render2D.drawRoundedRect(toggleChipX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(64, 112, 255, (int) (240 * alpha)));
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
        Fonts.drawString(Fonts.SF_MEDIUM, "Пресеты и конфигурации", x + 10.0F, y + 10.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Управление локальными пресетами и облачными кодами", x + 10.0F, y + 21.0F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float cardY = y + 38.0F;
        Render2D.drawRoundedRect(x + 10.0F, cardY, w - 20.0F, 32.0F, 5.5F, ColorUtil.rgba(16, 18, 25, (int) (240 * alpha)));
        Render2D.drawRoundedOutline(x + 10.0F, cardY, w - 20.0F, 32.0F, 5.5F, 1.0F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Default.cfg", x + 18.0F, cardY + 7.5F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Базовая конфигурация клиента", x + 18.0F, cardY + 17.0F, 5.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float loadBtnW = 40.0F;
        float loadBtnX = x + w - 20.0F - loadBtnW - 8.0F;
        Render2D.drawRoundedRect(loadBtnX, cardY + 8.5F, loadBtnW, 15.0F, 3.5F, ColorUtil.rgba(64, 112, 255, (int) (240 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + loadBtnW / 2.0F, cardY + 12.5F, 5.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }

    private void renderAccountsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Аккаунты", x + 10.0F, y + 10.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Управление учётными записями Minecraft", x + 10.0F, y + 21.0F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        float cardY = y + 38.0F;
        Render2D.drawRoundedRect(x + 10.0F, cardY, w - 20.0F, 32.0F, 5.5F, ColorUtil.rgba(16, 18, 25, (int) (240 * alpha)));
        Render2D.drawRoundedOutline(x + 10.0F, cardY, w - 20.0F, 32.0F, 5.5F, 1.0F, ColorUtil.rgba(26, 30, 42, (int) (200 * alpha)));

        String name = Minecraft.getInstance().getUser().getName();
        Fonts.drawString(Fonts.SF_MEDIUM, name, x + 18.0F, cardY + 7.5F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Активный оффлайн аккаунт", x + 18.0F, cardY + 17.0F, 5.5F, ColorUtil.rgba(75, 124, 248, (int) (220 * alpha)));
    }

    private void renderScriptsTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Скрипты", x + 10.0F, y + 10.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Пользовательские скрипты расширения", x + 10.0F, y + 21.0F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Папка со скриптами пуста (.minecraft/error/scripts)", x + 10.0F, y + 45.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));
    }

    private void renderAutoBuyTab(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Авто покупка", x + 10.0F, y + 10.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Автоматическая скупка предметов на аукционе", x + 10.0F, y + 21.0F, 6.0F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Настройте список целевых предметов для AHHelper", x + 10.0F, y + 45.0F, 6.5F, ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));
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
                    if (mouseX >= closeX && mouseX <= closeX + 8.0F && mouseY >= closeY && mouseY <= closeY + 8.0F) {
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
                    if (mouseX >= closeX && mouseX <= closeX + 8.0F && mouseY >= closeY && mouseY <= closeY + 8.0F) {
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
                    if (mouseX >= closeX && mouseX <= closeX + 8.0F && mouseY >= closeY && mouseY <= closeY + 8.0F) {
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
            if (mouseX >= x && mouseX <= x + winW && mouseY >= y && mouseY <= y + 32.0F) {
                float gearX = x + winW - 24.0F;
                float searchW = 115.0F;
                float searchX = x + winW - searchW - 32.0F;

                if (mouseX >= gearX && mouseX <= gearX + 17.0F && mouseY >= y + 7.5F && mouseY <= y + 24.5F) {
                    state.setClientSettingsOpen(!state.isClientSettingsOpen());
                    return true;
                }

                if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y + 7.5F && mouseY <= y + 24.5F) {
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
        float sideW = 114.0F;
        if (mouseX >= x && mouseX <= x + sideW && action == GLFW.GLFW_PRESS) {
            float itemX = x + 9.0F;
            float itemW = sideW - 18.0F;
            float itemH = 20.0F;
            float curY = y + 42.0F + 11.0F;

            Category[] funcCats = {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
            for (Category cat : funcCats) {
                if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH) {
                    state.switchCategory(cat);
                    return true;
                }
                curY += itemH + 2.5F;
            }

            curY += 8.0F + 11.0F;
            Category[] ctrlCats = {Category.CONFIGS, Category.EVENTS, Category.FRIENDS, Category.COSMETICS};
            for (Category cat : ctrlCats) {
                if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= curY && mouseY <= curY + itemH) {
                    state.switchCategory(cat);
                    return true;
                }
                curY += itemH + 2.5F;
            }
        }

        // 4. Content Area Module Clicks
        float contentX = x + sideW;
        float contentW = winW - sideW;
        float contentY = y + 32.0F;
        float contentH = winH - 32.0F;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            float colGap = 8.0F;
            float colW = (contentW - 16.0F - colGap) / 2.0F;

            if (checkColumnClicks(state, contentX + 8.0F, contentY + 6.0F, colW, contentH - 12.0F, true, mouseX, mouseY, button, action)) {
                return true;
            }
            if (checkColumnClicks(state, contentX + 8.0F + colW + colGap, contentY + 6.0F, colW, contentH - 12.0F, false, mouseX, mouseY, button, action)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkColumnClicks(PanelLapState state, float colX, float colY, float colW, float maxH, boolean isLeft, int mouseX, int mouseY, int button, int action) {
        if (action != GLFW.GLFW_PRESS) return false;

        Category cat = state.getCurrentCategory();
        List<CardGroup> groups = new ArrayList<>();

        if (cat == Category.COMBAT) {
            if (isLeft) {
                CardGroup draka = new CardGroup("Драка");
                addItem(draka, "Attack Aura", findMod("AuraModule", "AttackAura", "Aura"));
                addItem(draka, "No Velocity", findMod("AntiPush"));
                addItem(draka, "Trigger Bot", findMod("TriggerBot"));
                addItem(draka, "Aim Assist", findMod("AimAssistant"));
                addItem(draka, "Auto Explosion", findMod("AutoExplosion"));
                addItem(draka, "Crystal Aura", findMod("CrystalAura"));
                groups.add(draka);

                CardGroup bazovye = new CardGroup("Базовые");
                addItem(bazovye, "Auto Swap", findMod("AutoSwap"));
                addItem(bazovye, "Item Release", findMod("ExpThrow"));
                groups.add(bazovye);
            } else {
                CardGroup instr = new CardGroup("Инструменты");
                addItem(instr, "Sprint Reset", findMod("MaceHelper"));
                addItem(instr, "Tape Mouse", findMod("NoDelay"));
                addItem(instr, "Backtrack", findMod("Predictions"));
                addItem(instr, "Web Trap", findMod("WebTrap"));
                addItem(instr, "Knockback Swap", findMod("WindCharge"));
                groups.add(instr);

                CardGroup ostalnoe = new CardGroup("Остальное");
                addItem(ostalnoe, "No Slot Change", findMod("HoldMyItems"));
                addItem(ostalnoe, "Anti Bot", findMod("ClickFriend"));
                addItem(ostalnoe, "No Friend Damage", findMod("Criticals"));
                groups.add(ostalnoe);
            }
        } else if (cat == Category.RENDER) {
            if (isLeft) {
                CardGroup iface = new CardGroup("Интерфейс");
                addItem(iface, "HUD", findMod("Interface", "HUD"));
                addItem(iface, "Notifications", findMod("Notification"));
                addItem(iface, "ClickGUI", findMod("ClickGui"));
                groups.add(iface);
            } else {
                CardGroup mir = new CardGroup("Мир");
                addItem(mir, "Ambience", findMod("Ambience"));
                addItem(mir, "Better Minecraft", findMod("BetterMinecraft"));
                addItem(mir, "Block Highlight", findMod("BlockHighlight"));
                addItem(mir, "Block Outline", findMod("BlockOutline"));
                addItem(mir, "Custom Models", findMod("CustomModels"));
                addItem(mir, "Firework ESP", findMod("FireworkESP"));
                addItem(mir, "Predictions", findMod("Predictions"));
                addItem(mir, "Swing Animation", findMod("SwingAnimation"));
                addItem(mir, "View Model", findMod("ViewModel"));
                addItem(mir, "World Particles", findMod("WorldParticles"));
                groups.add(mir);
            }
        } else if (cat == Category.MOVEMENT) {
            if (isLeft) {
                CardGroup speed = new CardGroup("Скорость");
                addItem(speed, "Sprint", findMod("Sprint"));
                addItem(speed, "Water Speed", findMod("WaterSpeed"));
                addItem(speed, "Timer", findMod("Timer"));
                groups.add(speed);
            } else {
                CardGroup flight = new CardGroup("Полёт");
                addItem(flight, "Elytra Booster", findMod("ElytraBooster"));
                addItem(flight, "Elytra Motion", findMod("ElytraMotion"));
                addItem(flight, "No Fall", findMod("NoFall"));
                groups.add(flight);
            }
        } else if (cat == Category.PLAYER) {
            if (isLeft) {
                CardGroup inv = new CardGroup("Инвентарь");
                addItem(inv, "Auto Swap", findMod("AutoSwap"));
                addItem(inv, "Auto Tool", findMod("AutoTool"));
                addItem(inv, "Click Pearl", findMod("ClickPearl"));
                addItem(inv, "Elytra Swap", findMod("ElytraSwap"));
                addItem(inv, "Hold My Items", findMod("HoldMyItems"));
                groups.add(inv);
            } else {
                CardGroup acts = new CardGroup("Действия");
                addItem(acts, "Anti Push", findMod("AntiPush"));
                addItem(acts, "Gui Walk", findMod("GuiWalk"));
                addItem(acts, "Air Stuck", findMod("AirStuck"));
                groups.add(acts);
            }
        } else {
            if (isLeft) {
                CardGroup helpers = new CardGroup("Помощники");
                addItem(helpers, "AH Helper", findMod("AHHelper"));
                addItem(helpers, "Server Helper", findMod("ServerHelper"));
                addItem(helpers, "Auto Accept", findMod("AutoAccept"));
                addItem(helpers, "Auto Captcha", findMod("AutoCaptcha"));
                addItem(helpers, "Auto Msg", findMod("AutoMsg"));
                addItem(helpers, "Auto Sell", findMod("AutoSell"));
                groups.add(helpers);
            } else {
                CardGroup utils = new CardGroup("Утилиты");
                addItem(utils, "Free Cam", findMod("FreeCam"));
                addItem(utils, "Full Bright", findMod("FullBright"));
                addItem(utils, "Click Friend", findMod("ClickFriend"));
                addItem(utils, "Client Sounds", findMod("ClientSounds"));
                addItem(utils, "IRC", findMod("IRC"));
                addItem(utils, "Item Scroller", findMod("ItemScroller"));
                addItem(utils, "UnHook", findMod("UnHook"));
                groups.add(utils);
            }
        }

        float curY = colY;
        for (CardGroup g : groups) {
            float cardH = 20.0F + (Math.max(1, g.items.size()) * 19.0F) + 5.0F;
            float rowY = curY + 20.0F + 1.0F;
            float rowH = 19.0F;

            for (DisplayItem it : g.items) {
                if (mouseX >= colX && mouseX <= colX + colW && mouseY >= rowY && mouseY <= rowY + rowH) {
                    Module mod = it.module;
                    float toggleW = 17.0F;
                    float toggleX = colX + colW - toggleW - 8.0F;
                    float dotsX = toggleX - 15.0F;

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
                        if (mouseX >= dotsX - 2.0F && mouseX <= dotsX + 11.0F) {
                            state.setActiveModuleSettings(mod);
                            state.setPopupX(mouseX);
                            state.setPopupY(mouseY);
                        } else {
                            mod.toggle();
                        }
                        return true;
                    }
                }
                rowY += rowH;
            }
            curY += cardH + 8.0F;
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