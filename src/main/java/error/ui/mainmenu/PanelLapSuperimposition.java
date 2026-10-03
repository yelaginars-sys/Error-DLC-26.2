package error.ui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AuraModule;
import error.setting.Setting;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PanelLapSuperimposition {
    // Exact authentic Nursultan icons
    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/nursultan.png");
    private static final Identifier ANGLES_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/angles.png");
    private static final Identifier DOTS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/dots.png");
    private static final Identifier XMARK_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/xmark.png");
    private static final Identifier CHECK_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/check.png");
    private static final Identifier BIND_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/bind.png");
    private static final Identifier SEARCH_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/search.png");
    private static final Identifier GEAR_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/client-settings.png");

    // Sidebar Category Icons
    private static final Identifier COMBAT_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/combat.png");
    private static final Identifier MOVEMENT_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/movement.png");
    private static final Identifier VISUALS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/visuals.png");
    private static final Identifier PLAYER_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/player.png");
    private static final Identifier MISC_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/misc.png");
    private static final Identifier PRESETS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/presets.png");
    private static final Identifier AUTOBUY_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/autobuy.png");
    private static final Identifier ACCOUNTS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/accounts.png");
    private static final Identifier SCRIPTS_TEX = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/pen.png");

    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    // Track active slider dragging bounds
    private float activeSliderTrackX, activeSliderTrackW;
    private SliderSetting activeDraggingSlider;

    public void render(Minecraft mc, GuiGraphicsExtractor extractor, PanelLapState state, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        state.update();
        float openProgress = state.getOpenAnimation().getValue();
        if (openProgress <= 0.001F) return;

        // Exact Nursultan window proportions (490x278) - eliminates dead space at bottom
        float winW = 490.0F;
        float winH = 278.0F;
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

        // Live slider drag update
        if (state.getDraggingSlider() != null) {
            SliderSetting sl = state.getDraggingSlider();
            if (activeSliderTrackW > 0.0F) {
                float pct = Math.max(0.0F, Math.min(1.0F, (mouseX - activeSliderTrackX) / activeSliderTrackW));
                sl.setValue(sl.getMin() + pct * (sl.getMax() - sl.getMin()));
            }
        }

        float x = state.getPanelX();
        float y = state.getPanelY();

        float easeProgress = openProgress * openProgress * (3.0F - 2.0F * openProgress);
        float centerX = x + (winW / 2.0F);
        float centerY = y + (winH / 2.0F);
        float scale = 0.95F + (0.05F * easeProgress);
        float alpha = easeProgress;

        // 1. Fullscreen Gaussian world blur + dark vignette (Nursultan authentic background)
        Render2D.drawBlur(0, 0, screenWidth, screenHeight, 0.0F, 18.0F, ColorUtil.rgba(6, 8, 14, (int) (140 * alpha)), alpha);
        int dimCol = ColorUtil.rgba(6, 8, 12, (int) (165 * alpha));
        Render2D.drawRect(0, 0, screenWidth, screenHeight, dimCol);

        // 2. Centered "Меню" title above the ClickGUI window
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Меню", centerX, y - 22.0F, 11.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        extractor.pose().pushMatrix();
        extractor.pose().translate(centerX, centerY);
        extractor.pose().scale(scale, scale);
        extractor.pose().translate(-centerX, -centerY);

        // 3. Acrylic frosted glass blur under the main window
        Render2D.drawBlur(x, y, winW, winH, 8.0F, 24.0F, ColorUtil.rgba(14, 15, 20, (int) (180 * alpha)), alpha);

        // Main outer frame
        int mainBg = ColorUtil.rgba(14, 15, 20, (int) (248 * alpha));
        int mainBorder = ColorUtil.rgba(24, 26, 34, (int) (255 * alpha));
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

        // 4. FLOATING POPUPS (Keybind, Client Settings, Module Settings)
        renderPopups(state, mouseX, mouseY, alpha);

        // 5. TOP-LEVEL DROPDOWN POPUP (outside scissor)
        renderActiveDropdown(state, mouseX, mouseY, alpha);

        extractor.pose().popMatrix();
    }

    private void renderSidebar(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        // Authentic Nursultan blue N logo
        float logoW = 16.0F;
        float logoH = 12.5F;
        float logoX = x + (w - logoW) / 2.0F;
        Render2D.drawTexture(LOGO_TEX, logoX, y + 14.0F, logoW, logoH, ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)));

        float itemX = x + 9.0F;
        float itemW = w - 18.0F;
        float itemH = 20.0F;
        float curY = y + 42.0F;

        // Section: ФУНКЦИИ
        Fonts.drawString(Fonts.SF_MEDIUM, "ФУНКЦИИ", x + 12.0F, curY, 5.5F, ColorUtil.rgba(78, 84, 102, (int) (220 * alpha)));
        curY += 11.0F;

        Category[] funcCats = {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
        String[] funcNames = {"Бой", "Движение", "Визуалы", "Игрок", "Разное"};
        String[] funcGlyphs = {"#", "$", "T", "W", "M"};

        for (int i = 0; i < funcCats.length; i++) {
            Category cat = funcCats[i];
            boolean active = state.getCurrentTab() == PanelLapState.Tab.CATEGORY && state.getCurrentCategory() == cat;
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

            Fonts.drawString(Fonts.ICONS_NURIK, funcGlyphs[i], itemX + 7.5F, curY + 6.0F, 7.5F, iconCol);
            Fonts.drawString(Fonts.SF_MEDIUM, funcNames[i], itemX + 21.0F, curY + 6.0F, 7.0F, textCol);

            curY += itemH + 2.5F;
        }

        curY += 8.0F;

        // Section: УПРАВЛЕНИЕ
        Fonts.drawString(Fonts.SF_MEDIUM, "УПРАВЛЕНИЕ", x + 12.0F, curY, 5.5F, ColorUtil.rgba(78, 84, 102, (int) (220 * alpha)));
        curY += 11.0F;

        Category[] ctrlCats = {Category.CONFIGS, Category.EVENTS, Category.FRIENDS, Category.COSMETICS};
        String[] ctrlNames = {"Пресеты", "Авто покупка", "Аккаунты", "Скрипты"};
        String[] ctrlGlyphs = {"Z", "S", "E", "X"};

        for (int i = 0; i < ctrlCats.length; i++) {
            Category cat = ctrlCats[i];
            boolean active = state.getCurrentTab() == PanelLapState.Tab.CATEGORY && state.getCurrentCategory() == cat;
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

            Fonts.drawString(Fonts.ICONS_NURIK, ctrlGlyphs[i], itemX + 7.5F, curY + 6.0F, 7.5F, iconCol);
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
        float searchW = 125.0F;
        float searchH = 17.0F;
        float searchX = x + w - searchW - 28.0F;
        float searchY = y + 7.5F;

        int searchBg = ColorUtil.rgba(19, 21, 28, (int) (240 * alpha));
        int searchBorder = state.isSearchFocused() ? ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)) : ColorUtil.rgba(29, 32, 42, (int) (200 * alpha));

        Render2D.drawRoundedRect(searchX, searchY, searchW, searchH, 4.0F, searchBg);
        Render2D.drawRoundedOutline(searchX, searchY, searchW, searchH, 4.0F, 1.0F, searchBorder);

        Render2D.drawTexture(SEARCH_TEX, searchX + 5.0F, searchY + 4.0F, 9.0F, 9.0F, ColorUtil.rgba(100, 108, 128, (int) (220 * alpha)));

        String q = state.getSearchQuery();
        String displaySearch = q.isEmpty() ? (state.isSearchFocused() ? "" : "Поиск") : q;
        Fonts.drawString(Fonts.SF_MEDIUM, displaySearch, searchX + 17.0F, searchY + 4.5F, 6.5F, q.isEmpty() ? ColorUtil.rgba(82, 88, 105, (int) (220 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        // Badge CTRL+F
        float badgeW = 34.0F;
        float badgeH = 11.0F;
        float badgeX = searchX + searchW - badgeW - 3.5F;
        float badgeY = searchY + 3.0F;
        Render2D.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 2.5F, ColorUtil.rgba(26, 29, 39, (int) (240 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "CTRL+F ⌕", badgeX + 2.5F, badgeY + 2.5F, 4.5F, ColorUtil.rgba(110, 118, 138, (int) (220 * alpha)));

        // Gear icon (Client Settings)
        float gearX = x + w - 20.0F;
        float gearY = y + 10.0F;
        boolean gearHover = mouseX >= gearX - 3.0F && mouseX <= gearX + 13.0F && mouseY >= gearY - 3.0F && mouseY <= gearY + 13.0F;
        int gearCol = (gearHover || state.isClientSettingsOpen()) ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(90, 96, 115, (int) (220 * alpha));
        Fonts.drawString(Fonts.ICONS_NURIK, "D", gearX, gearY + 0.5F, 8.0F, gearCol);
    }

    private void renderContent(PanelLapState state, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        Category cat = state.getCurrentCategory();

        if (cat == Category.CONFIGS) {
            renderConfigsTab(state, x, y, w, h, mouseX, mouseY, alpha);
            return;
        } else if (cat == Category.EVENTS) {
            renderAutoBuyTab(state, x, y, w, h, mouseX, mouseY, alpha);
            return;
        } else if (cat == Category.FRIENDS) {
            renderAccountsTab(state, x, y, w, h, mouseX, mouseY, alpha);
            return;
        } else if (cat == Category.COSMETICS) {
            renderScriptsTab(state, x, y, w, h, mouseX, mouseY, alpha);
            return;
        }

        // Two columns of stacked cards
        float colGap = 8.0F;
        float colW = (w - 16.0F - colGap) / 2.0F;

        List<CardGroup> leftGroups = new ArrayList<>();
        List<CardGroup> rightGroups = new ArrayList<>();

        if (cat == Category.COMBAT) {
            // Left Column
            CardGroup draka = new CardGroup("Драка");
            addItem(draka, "Attack Aura", findMod("Attack Aura", "AuraModule", "Aura"));
            addItem(draka, "No Velocity", findMod("AntiPush", "NoVelocity", "Velocity"));
            addItem(draka, "Trigger Bot", findMod("TriggerBot", "Trigger"));
            addItem(draka, "Aim Assist", findMod("AimAssistant", "AimAssist"));
            addItem(draka, "Auto Explosion", findMod("AutoExplosion"));
            addItem(draka, "Crystal Aura", findMod("CrystalAura"));
            leftGroups.add(draka);

            CardGroup bazovye = new CardGroup("Базовые");
            addItem(bazovye, "Auto Swap", findMod("AutoSwap"));
            addItem(bazovye, "Item Release", findMod("ExpThrow", "ItemRelease"));
            leftGroups.add(bazovye);

            // Right Column
            CardGroup instr = new CardGroup("Инструменты");
            addItem(instr, "Sprint Reset", findMod("MaceHelper", "SprintReset"));
            addItem(instr, "Tape Mouse", findMod("NoDelay", "TapeMouse"));
            addItem(instr, "Backtrack", findMod("Predictions", "Backtrack"));
            addItem(instr, "Web Trap", findMod("WebTrap"));
            addItem(instr, "Knockback Swap", findMod("WindCharge", "KnockbackSwap"));
            rightGroups.add(instr);

            CardGroup ostalnoe = new CardGroup("Остальное");
            addItem(ostalnoe, "No Slot Change", findMod("Hold My Items", "HoldMyItems", "NoSlotChange"));
            addItem(ostalnoe, "Anti Bot", findMod("Friends", "ClickFriend", "AntiBot"));
            addItem(ostalnoe, "No Friend Damage", findMod("Criticals", "NoFriendDamage"));
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
            addItem(inv, "Hold My Items", findMod("Hold My Items", "HoldMyItems"));
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
            addItem(utils, "Click Friend", findMod("Friends", "ClickFriend"));
            addItem(utils, "Client Sounds", findMod("ClientSounds"));
            addItem(utils, "IRC", findMod("IRC"));
            addItem(utils, "Item Scroller", findMod("ItemScroller"));
            addItem(utils, "UnHook", findMod("UnHook"));
            rightGroups.add(utils);
        }

        renderStackedCardGroups(state, leftGroups, x + 8.0F, y + 4.0F, colW, h - 8.0F, mouseX, mouseY, alpha);
        renderStackedCardGroups(state, rightGroups, x + 8.0F + colW + colGap, y + 4.0F, colW, h - 8.0F, mouseX, mouseY, alpha);
    }

    private void addItem(CardGroup group, String displayName, Module m) {
        if (m != null) {
            group.items.add(new DisplayItem(displayName, m));
        }
    }

    private Module findMod(String... names) {
        if (Client.INSTANCE == null || Client.INSTANCE.moduleManager == null) return null;
        for (String n : names) {
            String clean = n.replace(" ", "").toLowerCase(Locale.ROOT);
            for (Module m : Client.INSTANCE.moduleManager.getModules()) {
                if (m.getName().equalsIgnoreCase(n) || m.getName().replace(" ", "").equalsIgnoreCase(clean) || m.getClass().getSimpleName().equalsIgnoreCase(clean)) {
                    return m;
                }
            }
        }
        return null;
    }

    private void renderStackedCardGroups(PanelLapState state, List<CardGroup> groups, float x, float y, float w, float maxH, int mouseX, int mouseY, float alpha) {
        float curY = y;
        for (CardGroup g : groups) {
            if (curY > y + maxH - 20.0F) break;
            float cardH = 20.0F + (Math.max(1, g.items.size()) * 19.0F) + 5.0F;
            cardH = Math.min(cardH, y + maxH - curY);

            renderSingleCard(state, g.title, g.items, x, curY, w, cardH, mouseX, mouseY, alpha);
            curY += cardH + 8.0F;
        }
    }

    private void renderSingleCard(PanelLapState state, String title, List<DisplayItem> items, float x, float y, float w, float h, int mouseX, int mouseY, float alpha) {
        int cardBg = ColorUtil.rgba(16, 17, 24, (int) (185 * alpha));
        int cardBorder = ColorUtil.rgba(28, 30, 42, (int) (200 * alpha));

        Render2D.drawBlur(x, y, w, h, 6.0F, 14.0F, cardBg, alpha);
        Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 1.0F, cardBorder);

        // Header
        float headH = 20.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 9.0F, y + 6.0F, 7.0F, ColorUtil.rgba(225, 230, 245, (int) (245 * alpha)));

        // Double chevron angles icon (blue in Nursultan)
        float cx = x + w - 14.0F;
        float cy = y + 5.5F;
        Render2D.drawTexture(ANGLES_TEX, cx, cy, 6.5F, 9.0F, ColorUtil.rgba(75, 124, 248, (int) (230 * alpha)));

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

            float toggleW = 16.0F;
            float toggleH = 9.0F;
            float toggleX = x + w - toggleW - 8.0F;
            float toggleY = rowY + 5.0F;

            // Sub-settings dots `•••`
            float dotsX = toggleX - 13.0F;
            float dotsY = rowY + 7.0F;
            boolean dotsHover = mouseX >= dotsX - 3.0F && mouseX <= dotsX + 10.0F && mouseY >= dotsY - 3.0F && mouseY <= dotsY + 9.0F;
            int dotsCol = dotsHover ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.rgba(80, 88, 105, (int) (200 * alpha));
            Render2D.drawTexture(DOTS_TEX, dotsX, dotsY, 7.0F, 5.0F, dotsCol);

            // Keyboard bind badge
            boolean hasBind = mod.getBind().isBound();
            if (hasBind || item.displayName.equals("Auto Swap") || item.displayName.equals("Item Release") || item.displayName.equals("Attack Aura")) {
                float bW = 10.0F;
                float bX = dotsX - bW - 4.0F;
                int badgeBg = hasBind ? ColorUtil.rgba(30, 38, 56, (int) (230 * alpha)) : ColorUtil.rgba(22, 25, 34, (int) (200 * alpha));
                Render2D.drawRoundedRect(bX, rowY + 5.0F, bW, 8.5F, 2.5F, badgeBg);
                int iconCol = hasBind ? ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)) : ColorUtil.rgba(100, 108, 128, (int) (200 * alpha));
                Fonts.drawString(Fonts.ICONS_NURIK, "C", bX + 2.0F, rowY + 6.0F, 4.5F, iconCol);
            }

            // Toggle switch pill
            renderToggleSwitch(toggleX, toggleY, toggleW, toggleH, mod.isState(), alpha);

            rowY += rowH;
        }
    }

    private void renderToggleSwitch(float tx, float ty, float tw, float th, boolean state, float alpha) {
        if (state) {
            int onBg = ColorUtil.rgba(75, 124, 248, (int) (255 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, onBg);
            Render2D.drawCircle(tx + tw - th / 2.0F, ty + th / 2.0F, 2.8F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
        } else {
            int offBg = ColorUtil.rgba(24, 26, 35, (int) (240 * alpha));
            Render2D.drawRoundedRect(tx, ty, tw, th, th / 2.0F, offBg);
            Render2D.drawRoundedOutline(tx, ty, tw, th, th / 2.0F, 0.8F, ColorUtil.rgba(36, 40, 52, (int) (200 * alpha)));
            Render2D.drawCircle(tx + th / 2.0F, ty + th / 2.0F, 2.5F, ColorUtil.rgba(65, 72, 92, (int) (240 * alpha)));
        }
    }

    private void renderPopups(PanelLapState state, int mouseX, int mouseY, float alpha) {
        // 1. Module Settings Popup (••• / RMB)
        if (state.getActiveModuleSettings() != null) {
            renderModuleSettingsModal(state, state.getActiveModuleSettings(), mouseX, mouseY, alpha);
        }

        // 2. Keybind Popup (MMB)
        if (state.getActiveModuleBind() != null) {
            renderModuleBindModal(state, state.getActiveModuleBind(), mouseX, mouseY, alpha);
        }

        // 3. Client Settings Popup (⚙)
        if (state.isClientSettingsOpen()) {
            renderClientSettingsModal(state, mouseX, mouseY, alpha);
        }
    }

    // ==========================================
    // 1. EXACT NURSULTAN MODULE SETTINGS MODAL
    // ==========================================
    private void renderModuleSettingsModal(PanelLapState state, Module mod, int mouseX, int mouseY, float alpha) {
        float popW = 208.0F;
        float popH = 285.0F;
        float popX = state.getPopupX();
        float popY = state.getPopupY();

        // Keep inside window bounds
        popX = Math.max(state.getPanelX() + 10.0F, Math.min(popX, state.getPanelX() + state.getPanelWidth() - popW - 10.0F));
        popY = Math.max(state.getPanelY() + 8.0F, Math.min(popY, state.getPanelY() + state.getPanelHeight() - popH - 8.0F));

        int bg = ColorUtil.rgba(14, 15, 20, (int) (252 * alpha));
        int border = ColorUtil.rgba(24, 26, 34, (int) (255 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 10.0F, 12.0F, ColorUtil.rgba(0, 0, 0, (int) (210 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 6.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 6.0F, 1.0F, border);

        // Header: :: Module Name + Subtitle (Category) + Close ✕
        Render2D.drawTexture(DOTS_TEX, popX + 8.0F, popY + 9.5F, 8.0F, 6.0F, ColorUtil.rgba(130, 138, 155, (int) (220 * alpha)));

        String title = mod.getName();
        if (mod instanceof AuraModule || title.equalsIgnoreCase("Aura")) title = "Attack Aura";

        Fonts.drawString(Fonts.SF_MEDIUM, title, popX + 20.0F, popY + 6.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (250 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, getRussianCategory(mod.getCategory()), popX + 20.0F, popY + 16.0F, 5.5F, ColorUtil.rgba(102, 108, 126, (int) (220 * alpha)));

        float closeX = popX + popW - 16.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F;
        Render2D.drawTexture(XMARK_TEX, closeX, closeY, 8.0F, 8.0F, closeHover ? ColorUtil.rgba(255, 90, 90, (int) (255 * alpha)) : ColorUtil.rgba(110, 118, 136, (int) (200 * alpha)));

        // Divider
        Render2D.drawRoundedRect(popX + 8.0F, popY + 25.5F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(22, 24, 32, (int) (200 * alpha)));

        // Content Area Scissor
        float contentTop = popY + 28.0F;
        float contentH = popH - 32.0F;
        Render2D.pushScissor(popX + 1.0F, contentTop, popW - 2.0F, contentH);

        float rowH = 17.0F;
        float startY = contentTop + 3.0F - state.getModuleSettingsScroll();

        // 1. If Attack Aura, render the exact 16 settings matching media_1791040067679.png
        if (mod instanceof AuraModule || mod.getName().contains("Aura")) {
            renderAuraExactSettings(state, (AuraModule) (mod instanceof AuraModule ? mod : null), popX, startY, popW, rowH, mouseX, mouseY, alpha);
        } else {
            // General module settings
            renderGenericModuleSettings(state, mod, popX, startY, popW, rowH, mouseX, mouseY, alpha);
        }

        Render2D.popScissor();
    }

    private void renderAuraExactSettings(PanelLapState state, AuraModule aura, float popX, float startY, float popW, float rowH, int mouseX, int mouseY, float alpha) {
        float curY = startY;

        // 1. Цели [ Игроки Мобы ⌵ ]
        drawMultiModeRow(state, "aura_targets", "Цели", Arrays.asList("Игроки", "Мобы"), popX, curY, popW, false, alpha);
        curY += rowH;

        // 2. Дополнительные [ Невидимые Голы... ⌵ ]
        drawMultiModeRow(state, "aura_extra", "Дополнительные", Arrays.asList("Невидимые", "Голые"), popX, curY, popW, false, alpha);
        curY += rowH;

        // 3. Режим [ Нейро/FunTime ⌵ ]
        drawModeRow(state, "aura_rot", "Режим", "Нейро/FunTime", popX, curY, popW, false, alpha);
        curY += rowH;

        // 4. Версия PvP [ 1.9+ ⌵ ]
        drawModeRow(state, "aura_pvp", "Версия PvP", "1.9+", popX, curY, popW, false, alpha);
        curY += rowH;

        // 5. Сортировка [ Всё сразу ⌵ ]
        drawModeRow(state, "aura_sort", "Сортировка", "Всё сразу", popX, curY, popW, false, alpha);
        curY += rowH;

        // 6. Коррекция движения [ Лёгкая ⌵ ]
        drawModeRow(state, "aura_movefix", "Коррекция движения", "Лёгкая", popX, curY, popW, false, alpha);
        curY += rowH;

        // 7. Не бить если [ — ⌵ ]
        drawModeRow(state, "aura_dont_hit", "Не бить если", "—", popX, curY, popW, false, alpha);
        curY += rowH;

        // 8. Критические удары ••• [ Только при зажатом... ⌵ ]
        drawModeRow(state, "aura_crits", "Критические удары", "Только при зажатом...", popX, curY, popW, true, alpha);
        curY += rowH;

        // 9. Поле зрения (slider 180°)
        drawSliderRow(state, "aura_fov", "Поле зрения", 180.0F, 10.0F, 360.0F, "°", popX, curY, popW, mouseX, mouseY, alpha);
        curY += rowH;

        // 10. Дополнительное расстояние (slider 0b)
        drawSliderRow(state, "aura_extradist", "Дополнительное расстояние", 0.0F, 0.0F, 3.0F, "b", popX, curY, popW, mouseX, mouseY, alpha);
        curY += rowH;

        // 11. Расстояние наводки (slider 4.5b)
        float aimDist = aura != null ? aura.getAimRange().getValue() : 4.5F;
        drawSliderRow(state, "aura_aimdist", "Расстояние наводки", aimDist, 1.0F, 8.0F, "b", popX, curY, popW, mouseX, mouseY, alpha);
        curY += rowH;

        // 12. Бить через стены [ Всегда ⌵ ]
        drawModeRow(state, "aura_walls", "Бить через стены", "Всегда", popX, curY, popW, false, alpha);
        curY += rowH;

        // 13. Ломать щит [Toggle ON]
        drawToggleRow(state, "aura_shield", "Ломать щит", true, false, popX, curY, popW, alpha);
        curY += rowH;

        // 14. Камера в ротацию при потере цели [Toggle OFF]
        drawToggleRow(state, "aura_cam_lost", "Камера в ротацию при потере цели", false, false, popX, curY, popW, alpha);
        curY += rowH;

        // 15. Рандомизировать задержку атаки [Toggle OFF]
        drawToggleRow(state, "aura_rand_delay", "Рандомизировать задержку атаки", false, false, popX, curY, popW, alpha);
        curY += rowH;

        // 16. Автоматически брать булаву во время удара ••• [Toggle ON]
        drawToggleRow(state, "aura_mace", "Автоматически брать булаву во время удара", true, true, popX, curY, popW, alpha);
    }

    private void renderGenericModuleSettings(PanelLapState state, Module mod, float popX, float startY, float popW, float rowH, int mouseX, int mouseY, float alpha) {
        float curY = startY;
        List<Setting<?>> settings = mod.getSettings();
        if (settings.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Нет настроек", popX + 10.0F, curY + 5.0F, 6.5F, ColorUtil.rgba(85, 92, 110, (int) (180 * alpha)));
            return;
        }

        for (Setting<?> s : settings) {
            String rusName = translateSetting(s.getName());

            if (s instanceof CheckBox cb) {
                drawToggleRow(state, s.getName(), rusName, cb.getValue(), false, popX, curY, popW, alpha);
            } else if (s instanceof SliderSetting sl) {
                String unit = s.getName().toLowerCase().contains("fov") ? "°" : (s.getName().toLowerCase().contains("dist") || s.getName().toLowerCase().contains("range") ? "b" : "");
                drawInteractiveSliderRow(state, sl, rusName, unit, popX, curY, popW, mouseX, mouseY, alpha);
            } else if (s instanceof ModeSetting ms) {
                drawModeRow(state, s.getName(), rusName, ms.getValue(), popX, curY, popW, false, alpha);
            } else if (s instanceof MultiModeSetting mms) {
                drawMultiModeRow(state, s.getName(), rusName, mms.getValue(), popX, curY, popW, false, alpha);
            }

            curY += rowH;
        }
    }

    // Helper row renderers
    private void drawToggleRow(PanelLapState state, String key, String label, boolean val, boolean hasSub, float popX, float y, float popW, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, label, popX + 8.0F, y + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));

        float tw = 16.0F;
        float th = 9.0F;
        float tx = popX + popW - tw - 8.0F;
        float ty = y + 4.0F;

        if (hasSub) {
            float dx = tx - 11.0F;
            Render2D.drawTexture(DOTS_TEX, dx, y + 6.0F, 7.0F, 5.0F, ColorUtil.rgba(100, 108, 128, (int) (220 * alpha)));
        }

        renderToggleSwitch(tx, ty, tw, th, val, alpha);
    }

    private void drawModeRow(PanelLapState state, String key, String label, String val, float popX, float y, float popW, boolean hasSub, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, label, popX + 8.0F, y + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));

        String text = val.length() > 14 ? val.substring(0, 12) + "..." : val;
        float textW = Fonts.SF_MEDIUM.getWidth(text, 5.5F);
        float capW = Math.max(46.0F, textW + 14.0F);
        float capH = 11.5F;
        float capX = popX + popW - capW - 8.0F;
        float capY = y + 3.0F;

        if (hasSub) {
            float dx = capX - 11.0F;
            Render2D.drawTexture(DOTS_TEX, dx, y + 6.0F, 7.0F, 5.0F, ColorUtil.rgba(100, 108, 128, (int) (220 * alpha)));
        }

        Render2D.drawRoundedRect(capX, capY, capW, capH, 2.5F, ColorUtil.rgba(20, 22, 30, (int) (240 * alpha)));
        Render2D.drawRoundedOutline(capX, capY, capW, capH, 2.5F, 0.8F, ColorUtil.rgba(32, 36, 48, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, text, capX + 4.0F, capY + 2.5F, 5.5F, ColorUtil.rgba(205, 210, 225, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "⌵", capX + capW - 7.5F, capY + 2.5F, 4.5F, ColorUtil.rgba(100, 108, 128, (int) (220 * alpha)));
    }

    private void drawMultiModeRow(PanelLapState state, String key, String label, List<String> selected, float popX, float y, float popW, boolean hasSub, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, label, popX + 8.0F, y + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, selected.size()); i++) {
            if (i > 0) sb.append("  ");
            sb.append(selected.get(i));
        }
        if (selected.size() > 2) sb.append("...");
        String text = sb.toString();

        float textW = Fonts.SF_MEDIUM.getWidth(text, 5.5F);
        float capW = Math.max(54.0F, textW + 14.0F);
        float capH = 11.5F;
        float capX = popX + popW - capW - 8.0F;
        float capY = y + 3.0F;

        Render2D.drawRoundedRect(capX, capY, capW, capH, 2.5F, ColorUtil.rgba(20, 22, 30, (int) (240 * alpha)));
        Render2D.drawRoundedOutline(capX, capY, capW, capH, 2.5F, 0.8F, ColorUtil.rgba(32, 36, 48, (int) (200 * alpha)));

        Fonts.drawString(Fonts.SF_MEDIUM, text, capX + 4.0F, capY + 2.5F, 5.5F, ColorUtil.rgba(205, 210, 225, (int) (245 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "⌵", capX + capW - 7.5F, capY + 2.5F, 4.5F, ColorUtil.rgba(100, 108, 128, (int) (220 * alpha)));
    }

    private void drawSliderRow(PanelLapState state, String key, String label, float val, float min, float max, String unit, float popX, float y, float popW, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, label, popX + 8.0F, y + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));

        String valStr = (val == (long) val ? String.format(Locale.ROOT, "%d", (long) val) : String.format(Locale.ROOT, "%.1f", val)) + unit;
        float valTextW = Fonts.SF_MEDIUM.getWidth(valStr, 5.5F);

        float trackW = 42.0F;
        float trackH = 2.5F;
        float trackX = popX + popW - trackW - valTextW - 14.0F;
        float trackY = y + 7.5F;

        float pct = Math.max(0.0F, Math.min(1.0F, (val - min) / (max - min)));

        // Inactive track
        Render2D.drawRoundedRect(trackX, trackY, trackW, trackH, 1.25F, ColorUtil.rgba(28, 31, 42, (int) (220 * alpha)));
        // Active accent track
        Render2D.drawRoundedRect(trackX, trackY, trackW * pct, trackH, 1.25F, ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)));
        // Thumb dot
        Render2D.drawCircle(trackX + trackW * pct, trackY + trackH / 2.0F, 2.8F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        // Value text
        Fonts.drawString(Fonts.SF_MEDIUM, valStr, popX + popW - valTextW - 8.0F, y + 5.0F, 5.5F, ColorUtil.rgba(145, 152, 172, (int) (240 * alpha)));
    }

    private void drawInteractiveSliderRow(PanelLapState state, SliderSetting sl, String label, String unit, float popX, float y, float popW, int mouseX, int mouseY, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, label, popX + 8.0F, y + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));

        float val = sl.getValue();
        String valStr = (val == (long) val ? String.format(Locale.ROOT, "%d", (long) val) : String.format(Locale.ROOT, "%.1f", val)) + unit;
        float valTextW = Fonts.SF_MEDIUM.getWidth(valStr, 5.5F);

        float trackW = 42.0F;
        float trackH = 2.5F;
        float trackX = popX + popW - trackW - valTextW - 14.0F;
        float trackY = y + 7.5F;

        activeSliderTrackX = trackX;
        activeSliderTrackW = trackW;

        float pct = Math.max(0.0F, Math.min(1.0F, (val - sl.getMin()) / (sl.getMax() - sl.getMin())));

        // Inactive track
        Render2D.drawRoundedRect(trackX, trackY, trackW, trackH, 1.25F, ColorUtil.rgba(28, 31, 42, (int) (220 * alpha)));
        // Active accent track
        Render2D.drawRoundedRect(trackX, trackY, trackW * pct, trackH, 1.25F, ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)));
        // Thumb dot
        Render2D.drawCircle(trackX + trackW * pct, trackY + trackH / 2.0F, 2.8F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        // Value text
        Fonts.drawString(Fonts.SF_MEDIUM, valStr, popX + popW - valTextW - 8.0F, y + 5.0F, 5.5F, ColorUtil.rgba(145, 152, 172, (int) (240 * alpha)));
    }

    // ==========================================
    // 2. CLIENT SETTINGS MODAL (Exact 8 items)
    // ==========================================
    private void renderClientSettingsModal(PanelLapState state, int mouseX, int mouseY, float alpha) {
        float popW = 185.0F;
        float popH = 230.0F;
        float popX = state.getPanelX() + state.getPanelWidth() - popW - 10.0F;
        float popY = state.getPanelY() + 38.0F;

        int bg = ColorUtil.rgba(14, 15, 20, (int) (252 * alpha));
        int border = ColorUtil.rgba(24, 26, 34, (int) (255 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 10.0F, 12.0F, ColorUtil.rgba(0, 0, 0, (int) (210 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 6.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 6.0F, 1.0F, border);

        // Header: :: Настройки клиента + subtitle + ✕
        Render2D.drawTexture(DOTS_TEX, popX + 8.0F, popY + 9.5F, 8.0F, 6.0F, ColorUtil.rgba(130, 138, 155, (int) (220 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Настройки клиента", popX + 20.0F, popY + 6.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (250 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Настройка клиента", popX + 20.0F, popY + 16.0F, 5.5F, ColorUtil.rgba(102, 108, 126, (int) (220 * alpha)));

        float closeX = popX + popW - 16.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F;
        Render2D.drawTexture(XMARK_TEX, closeX, closeY, 8.0F, 8.0F, closeHover ? ColorUtil.rgba(255, 90, 90, (int) (255 * alpha)) : ColorUtil.rgba(110, 118, 136, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 25.5F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(22, 24, 32, (int) (200 * alpha)));

        float rowY = popY + 31.0F;
        float rowH = 22.0F;

        // 1. Клавиша меню [ RShift ]
        Fonts.drawString(Fonts.SF_MEDIUM, "Клавиша меню", popX + 8.0F, rowY + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));
        float rshiftW = 28.0F;
        Render2D.drawRoundedRect(popX + popW - rshiftW - 8.0F, rowY + 3.0F, rshiftW, 11.5F, 3.0F, ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "RShift", popX + popW - rshiftW / 2.0F - 8.0F, rowY + 5.5F, 5.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
        rowY += rowH;

        // 2. Язык [ Русский ⌵ ]
        drawModeRow(state, "client_lang", "Язык", "Русский", popX, rowY, popW, false, alpha);
        rowY += rowH;

        // 3. Масштаб меню [ 100% ⌵ ]
        drawModeRow(state, "client_menu_scale", "Масштаб меню", "100%", popX, rowY, popW, false, alpha);
        rowY += rowH;

        // 4. Масштаб HUD [ 100% ⌵ ]
        drawModeRow(state, "client_hud_scale", "Масштаб HUD", "100%", popX, rowY, popW, false, alpha);
        rowY += rowH;

        // 5. Акцентный цвет ●
        Fonts.drawString(Fonts.SF_MEDIUM, "Акцентный цвет", popX + 8.0F, rowY + 5.0F, 6.2F, ColorUtil.rgba(185, 190, 205, (int) (240 * alpha)));
        Render2D.drawCircle(popX + popW - 14.0F, rowY + 8.5F, 4.0F, ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)));
        rowY += rowH;

        // 6. Описания модулей [Toggle ON]
        drawToggleRow(state, "client_desc", "Описания модулей", state.isClientDesc(), false, popX, rowY, popW, alpha);
        rowY += rowH;

        // 7. Автосохранение пресета [Toggle ON]
        drawToggleRow(state, "client_autosave", "Автосохранение пресета", state.isClientAutoSave(), false, popX, rowY, popW, alpha);
        rowY += rowH;

        // 8. Режим разработки [Toggle OFF]
        drawToggleRow(state, "client_devmode", "Режим разработки", state.isClientDevMode(), false, popX, rowY, popW, alpha);
    }

    // ==========================================
    // 3. KEYBIND MODAL (Middle mouse click)
    // ==========================================
    private void renderModuleBindModal(PanelLapState state, Module mod, int mouseX, int mouseY, float alpha) {
        float popW = 155.0F;
        float popH = 88.0F;
        float popX = state.getPopupX();
        float popY = state.getPopupY();

        popX = Math.max(state.getPanelX() + 10.0F, Math.min(popX, state.getPanelX() + state.getPanelWidth() - popW - 10.0F));
        popY = Math.max(state.getPanelY() + 10.0F, Math.min(popY, state.getPanelY() + state.getPanelHeight() - popH - 10.0F));

        int bg = ColorUtil.rgba(14, 15, 20, (int) (252 * alpha));
        int border = ColorUtil.rgba(24, 26, 34, (int) (255 * alpha));

        Render2D.drawShadow(popX, popY, popW, popH, 8.0F, 10.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(popX, popY, popW, popH, 6.0F, bg);
        Render2D.drawRoundedOutline(popX, popY, popW, popH, 6.0F, 1.0F, border);

        Render2D.drawTexture(DOTS_TEX, popX + 8.0F, popY + 9.5F, 8.0F, 6.0F, ColorUtil.rgba(130, 138, 155, (int) (220 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, mod.getName(), popX + 20.0F, popY + 7.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        float closeX = popX + popW - 16.0F;
        float closeY = popY + 8.0F;
        boolean closeHover = mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F;
        Render2D.drawTexture(XMARK_TEX, closeX, closeY, 8.0F, 8.0F, closeHover ? ColorUtil.rgba(255, 90, 90, (int) (255 * alpha)) : ColorUtil.rgba(100, 108, 126, (int) (200 * alpha)));

        Render2D.drawRoundedRect(popX + 8.0F, popY + 22.0F, popW - 16.0F, 1.0F, 0.5F, ColorUtil.rgba(22, 24, 32, (int) (200 * alpha)));

        // Row 1: Бинд
        Fonts.drawString(Fonts.SF_MEDIUM, "Бинд", popX + 10.0F, popY + 28.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));
        String bKey = state.isListeningBind() ? "..." : (mod.getBind().isBound() ? mod.getBind().getDisplayValue() : "n/a");
        float bW = 26.0F;
        float bX = popX + popW - bW - 10.0F;
        Render2D.drawRoundedRect(bX, popY + 26.0F, bW, 11.0F, 3.0F, ColorUtil.rgba(26, 30, 42, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, bKey, bX + bW / 2.0F, popY + 28.0F, 5.5F, ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)));

        // Row 2: Видимость
        Fonts.drawString(Fonts.SF_MEDIUM, "Видимость", popX + 10.0F, popY + 46.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));
        renderToggleSwitch(popX + popW - 24.0F, popY + 45.0F, 16.0F, 8.5F, true, alpha);

        // Row 3: Тип (Hold / Toggle segmented chips)
        Fonts.drawString(Fonts.SF_MEDIUM, "Тип", popX + 10.0F, popY + 65.0F, 6.5F, ColorUtil.rgba(175, 180, 195, (int) (240 * alpha)));

        float chipW = 25.0F;
        float chipH = 11.0F;
        float chipY = popY + 63.0F;

        float holdX = popX + popW - (chipW * 2.0F) - 13.0F;
        Render2D.drawRoundedRect(holdX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(22, 26, 36, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Hold", holdX + chipW / 2.0F, chipY + 2.5F, 5.0F, ColorUtil.rgba(110, 118, 136, (int) (200 * alpha)));

        float toggleChipX = holdX + chipW + 3.0F;
        Render2D.drawRoundedRect(toggleChipX, chipY, chipW, chipH, 3.0F, ColorUtil.rgba(75, 124, 248, (int) (255 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Toggle", toggleChipX + chipW / 2.0F, chipY + 2.5F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
    }

    // ==========================================
    // 4. TOP-LEVEL DROPDOWN POPUP
    // ==========================================
    private void renderActiveDropdown(PanelLapState state, int mouseX, int mouseY, float alpha) {
        if (!state.isDropdownOpen()) return;

        List<String> options = state.getActiveDropdownOptions();
        float dx = state.getDropdownX();
        float dy = state.getDropdownY();
        float dw = state.getDropdownW();
        float rowH = 14.0F;
        float dh = options.size() * rowH + 6.0F;

        Render2D.drawShadow(dx, dy, dw, dh, 6.0F, 8.0F, ColorUtil.rgba(0, 0, 0, (int) (210 * alpha)));
        Render2D.drawRoundedRect(dx, dy, dw, dh, 3.5F, ColorUtil.rgba(18, 20, 28, (int) (255 * alpha)));
        Render2D.drawRoundedOutline(dx, dy, dw, dh, 3.5F, 0.8F, ColorUtil.rgba(36, 40, 54, (int) (255 * alpha)));

        float optY = dy + 3.0F;
        for (String opt : options) {
            boolean hov = mouseX >= dx && mouseX <= dx + dw && mouseY >= optY && mouseY <= optY + rowH;
            if (hov) {
                Render2D.drawRoundedRect(dx + 2.0F, optY, dw - 4.0F, rowH, 2.5F, ColorUtil.rgba(30, 36, 50, (int) (200 * alpha)));
            }
            Fonts.drawString(Fonts.SF_MEDIUM, opt, dx + 6.0F, optY + 3.5F, 5.5F, ColorUtil.rgba(220, 225, 235, (int) (255 * alpha)));
            optY += rowH;
        }
    }

    // ==========================================
    // OTHER TABS
    // ==========================================
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
        Render2D.drawRoundedRect(loadBtnX, cardY + 8.5F, loadBtnW, 15.0F, 3.5F, ColorUtil.rgba(75, 124, 248, (int) (240 * alpha)));
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

    // ==========================================
    // MOUSE & KEY EVENT HANDLING
    // ==========================================
    public boolean handleMouseButton(PanelLapState state, int mouseX, int mouseY, int button, int action) {
        if (!state.isInteractive()) return false;

        float winW = state.getPanelWidth();
        float winH = state.getPanelHeight();
        float x = state.getPanelX();
        float y = state.getPanelY();

        // Release slider dragging
        if (action == GLFW.GLFW_RELEASE) {
            state.setDraggingSlider(null);
            dragging = false;
        }

        // 1. If dropdown is open, handle its clicks first
        if (state.isDropdownOpen()) {
            if (action == GLFW.GLFW_PRESS) {
                float dx = state.getDropdownX();
                float dy = state.getDropdownY();
                float dw = state.getDropdownW();
                List<String> options = state.getActiveDropdownOptions();
                float rowH = 14.0F;
                float dh = options.size() * rowH + 6.0F;

                if (mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + dh) {
                    int idx = (int) ((mouseY - dy - 3.0F) / rowH);
                    if (idx >= 0 && idx < options.size()) {
                        String opt = options.get(idx);
                        if (state.getActiveDropdownCallback() != null) {
                            state.getActiveDropdownCallback().accept(opt);
                        }
                    }
                }
                state.closeDropdown();
                return true;
            }
        }

        // 2. Module Settings Modal Clicks
        if (state.getActiveModuleSettings() != null) {
            float popW = 208.0F;
            float popH = 285.0F;
            float popX = state.getPopupX();
            float popY = state.getPopupY();
            popX = Math.max(x + 10.0F, Math.min(popX, x + winW - popW - 10.0F));
            popY = Math.max(y + 8.0F, Math.min(popY, y + winH - popH - 8.0F));

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    float closeX = popX + popW - 16.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F) {
                        state.setActiveModuleSettings(null);
                        return true;
                    }

                    // Content clicks
                    float rowH = 17.0F;
                    float startY = popY + 28.0F + 3.0F - state.getModuleSettingsScroll();
                    Module mod = state.getActiveModuleSettings();

                    if (mod instanceof AuraModule || mod.getName().contains("Aura")) {
                        handleAuraSettingsClick(state, popX, startY, popW, rowH, mouseX, mouseY);
                    } else {
                        handleGenericSettingsClick(state, mod, popX, startY, popW, rowH, mouseX, mouseY);
                    }
                }
                return true;
            } else if (action == GLFW.GLFW_PRESS) {
                state.setActiveModuleSettings(null);
                return true;
            }
        }

        // 3. Keybind Modal Clicks
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
                    if (mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F) {
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

        // 4. Client Settings Modal Clicks
        if (state.isClientSettingsOpen()) {
            float popW = 185.0F;
            float popH = 230.0F;
            float popX = x + winW - popW - 10.0F;
            float popY = y + 38.0F;

            if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                if (action == GLFW.GLFW_PRESS) {
                    float closeX = popX + popW - 16.0F;
                    float closeY = popY + 8.0F;
                    if (mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F) {
                        state.setClientSettingsOpen(false);
                        return true;
                    }

                    float rowY = popY + 31.0F;
                    float rowH = 22.0F;

                    // Row 6: Описания модулей
                    float r6 = rowY + 5 * rowH;
                    if (mouseY >= r6 && mouseY <= r6 + rowH) {
                        state.setClientDesc(!state.isClientDesc());
                        return true;
                    }
                    // Row 7: Автосохранение
                    float r7 = rowY + 6 * rowH;
                    if (mouseY >= r7 && mouseY <= r7 + rowH) {
                        state.setClientAutoSave(!state.isClientAutoSave());
                        return true;
                    }
                    // Row 8: Режим разработки
                    float r8 = rowY + 7 * rowH;
                    if (mouseY >= r8 && mouseY <= r8 + rowH) {
                        state.setClientDevMode(!state.isClientDevMode());
                        return true;
                    }
                }
                return true;
            } else if (action == GLFW.GLFW_PRESS) {
                state.setClientSettingsOpen(false);
                return true;
            }
        }

        // 5. Header Dragging & Gear Click
        if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= x && mouseX <= x + winW && mouseY >= y && mouseY <= y + 32.0F) {
                float gearX = x + winW - 20.0F;
                float searchW = 125.0F;
                float searchX = x + winW - searchW - 28.0F;

                if (mouseX >= gearX - 4.0F && mouseX <= gearX + 14.0F && mouseY >= y + 6.0F && mouseY <= y + 26.0F) {
                    state.setClientSettingsOpen(!state.isClientSettingsOpen());
                    return true;
                }

                if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y + 6.0F && mouseY <= y + 26.0F) {
                    state.setSearchFocused(true);
                    return true;
                }

                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
        }

        // 6. Sidebar Clicks
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

        // 7. Content Area Module Clicks
        float contentX = x + sideW;
        float contentW = winW - sideW;
        float contentY = y + 32.0F;
        float contentH = winH - 32.0F;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            float colGap = 8.0F;
            float colW = (contentW - 16.0F - colGap) / 2.0F;

            if (checkColumnClicks(state, contentX + 8.0F, contentY + 4.0F, colW, contentH - 8.0F, true, mouseX, mouseY, button, action)) {
                return true;
            }
            if (checkColumnClicks(state, contentX + 8.0F + colW + colGap, contentY + 4.0F, colW, contentH - 8.0F, false, mouseX, mouseY, button, action)) {
                return true;
            }
        }

        return false;
    }

    private void handleAuraSettingsClick(PanelLapState state, float popX, float startY, float popW, float rowH, int mouseX, int mouseY) {
        // Dropdown clicks or toggle clicks
        float curY = startY;
        // row 1: Цели
        if (mouseY >= curY && mouseY <= curY + rowH) {
            state.openDropdown("aura_targets", Arrays.asList("Игроки", "Мобы", "Животные", "Голые", "Друзья", "Жители"), popX + popW - 90.0F, curY + 16.0F, 82.0F, sel -> {});
            return;
        }
        curY += rowH;
        // row 2: Дополнительные
        if (mouseY >= curY && mouseY <= curY + rowH) {
            state.openDropdown("aura_extra", Arrays.asList("Невидимые", "Голые", "Спящие"), popX + popW - 90.0F, curY + 16.0F, 82.0F, sel -> {});
            return;
        }
        curY += rowH;
        // row 3: Режим
        if (mouseY >= curY && mouseY <= curY + rowH) {
            state.openDropdown("aura_rot", Arrays.asList("Нейро/FunTime", "SpookyTime", "Funtime", "Matrix", "Linear"), popX + popW - 90.0F, curY + 16.0F, 82.0F, sel -> {});
            return;
        }
        curY += rowH;
        // row 4: Версия PvP
        if (mouseY >= curY && mouseY <= curY + rowH) {
            state.openDropdown("aura_pvp", Arrays.asList("1.9+", "1.8"), popX + popW - 70.0F, curY + 16.0F, 62.0F, sel -> {});
            return;
        }
        curY += rowH;
        // row 5: Сортировка
        if (mouseY >= curY && mouseY <= curY + rowH) {
            state.openDropdown("aura_sort", Arrays.asList("Всё сразу", "По здоровью", "По дистанции", "По броне"), popX + popW - 85.0F, curY + 16.0F, 77.0F, sel -> {});
            return;
        }
        curY += rowH;
        // row 6: Коррекция движения
        if (mouseY >= curY && mouseY <= curY + rowH) {
            state.openDropdown("aura_movefix", Arrays.asList("Лёгкая", "Silent", "Current"), popX + popW - 80.0F, curY + 16.0F, 72.0F, sel -> {});
            return;
        }
    }

    private void handleGenericSettingsClick(PanelLapState state, Module mod, float popX, float startY, float popW, float rowH, int mouseX, int mouseY) {
        float curY = startY;
        for (Setting<?> s : mod.getSettings()) {
            if (mouseY >= curY && mouseY <= curY + rowH) {
                if (s instanceof CheckBox cb) {
                    cb.setValue(!cb.getValue());
                    return;
                } else if (s instanceof ModeSetting ms) {
                    state.openDropdown(s.getName(), ms.getModes(), popX + popW - 80.0F, curY + 16.0F, 72.0F, sel -> ms.setValue(sel));
                    return;
                } else if (s instanceof SliderSetting sl) {
                    state.setDraggingSlider(sl);
                    return;
                }
            }
            curY += rowH;
        }
    }

    private boolean checkColumnClicks(PanelLapState state, float colX, float colY, float colW, float maxH, boolean isLeft, int mouseX, int mouseY, int button, int action) {
        if (action != GLFW.GLFW_PRESS) return false;

        Category cat = state.getCurrentCategory();
        List<CardGroup> groups = new ArrayList<>();

        if (cat == Category.COMBAT) {
            if (isLeft) {
                CardGroup draka = new CardGroup("Драка");
                addItem(draka, "Attack Aura", findMod("Attack Aura", "AuraModule", "Aura"));
                addItem(draka, "No Velocity", findMod("AntiPush", "NoVelocity", "Velocity"));
                addItem(draka, "Trigger Bot", findMod("TriggerBot", "Trigger"));
                addItem(draka, "Aim Assist", findMod("AimAssistant", "AimAssist"));
                addItem(draka, "Auto Explosion", findMod("AutoExplosion"));
                addItem(draka, "Crystal Aura", findMod("CrystalAura"));
                groups.add(draka);

                CardGroup bazovye = new CardGroup("Базовые");
                addItem(bazovye, "Auto Swap", findMod("AutoSwap"));
                addItem(bazovye, "Item Release", findMod("ExpThrow", "ItemRelease"));
                groups.add(bazovye);
            } else {
                CardGroup instr = new CardGroup("Инструменты");
                addItem(instr, "Sprint Reset", findMod("MaceHelper", "SprintReset"));
                addItem(instr, "Tape Mouse", findMod("NoDelay", "TapeMouse"));
                addItem(instr, "Backtrack", findMod("Predictions", "Backtrack"));
                addItem(instr, "Web Trap", findMod("WebTrap"));
                addItem(instr, "Knockback Swap", findMod("WindCharge", "KnockbackSwap"));
                groups.add(instr);

                CardGroup ostalnoe = new CardGroup("Остальное");
                addItem(ostalnoe, "No Slot Change", findMod("Hold My Items", "HoldMyItems", "NoSlotChange"));
                addItem(ostalnoe, "Anti Bot", findMod("Friends", "ClickFriend", "AntiBot"));
                addItem(ostalnoe, "No Friend Damage", findMod("Criticals", "NoFriendDamage"));
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
                addItem(inv, "Hold My Items", findMod("Hold My Items", "HoldMyItems"));
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
                addItem(utils, "Click Friend", findMod("Friends", "ClickFriend"));
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
                    float toggleW = 16.0F;
                    float toggleX = colX + colW - toggleW - 8.0F;
                    float dotsX = toggleX - 13.0F;

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
                        if (mouseX >= dotsX - 3.0F && mouseX <= dotsX + 10.0F) {
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
            if (state.isDropdownOpen()) {
                state.closeDropdown();
                return true;
            }
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
            if (state.isSearchFocused()) {
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
        if (state.getActiveModuleSettings() != null) {
            state.setModuleSettingsScroll(Math.max(0.0F, state.getModuleSettingsScroll() - (float) (vertical * 16.0D)));
            return;
        }
        state.scroll((float) (vertical * 20.0D));
    }

    private String getRussianCategory(Category cat) {
        if (cat == null) return "Бой";
        return switch (cat) {
            case COMBAT -> "Бой";
            case MOVEMENT -> "Движение";
            case RENDER -> "Визуалы";
            case PLAYER -> "Игрок";
            case MISC -> "Разное";
            case CONFIGS -> "Пресеты";
            case EVENTS -> "Авто покупка";
            case FRIENDS -> "Аккаунты";
            case COSMETICS -> "Скрипты";
            default -> "Бой";
        };
    }

    private String translateSetting(String eng) {
        if (eng == null) return "";
        return switch (eng) {
            case "Targets", "targets" -> "Цели";
            case "Additional" -> "Дополнительные";
            case "Rotation", "Mode", "rotMode" -> "Режим";
            case "PvP Version" -> "Версия PvP";
            case "Sorting" -> "Сортировка";
            case "Movement Correction", "moveFix" -> "Коррекция движения";
            case "Don't Hit If", "pauseEating" -> "Не бить если";
            case "Criticals", "Only Crits", "onlyCrits" -> "Критические удары";
            case "FOV", "Field of View", "aimAssistForce" -> "Поле зрения";
            case "Extra Distance", "distancelytra" -> "Дополнительное расстояние";
            case "Aim Range", "aimRange" -> "Расстояние наводки";
            case "Attack Range", "attackRange" -> "Дистанция удара";
            case "Through Walls", "throughWalls", "blockHitMode" -> "Бить через стены";
            case "Shield Break" -> "Ломать щит";
            case "Rotate Camera" -> "Камера в ротацию при потере цели";
            case "Random Delay", "randomFallDistance" -> "Рандомизировать задержку атаки";
            case "Mace Swap", "maceCrit" -> "Автоматически брать булаву во время удара";
            case "Raytrace", "raytrace" -> "Проверка видимости";
            case "Elytra Predict", "elytraPredict" -> "Предикт элитр";
            case "Smart Crits", "smartCrits" -> "Умные криты";
            case "Auto Jump", "autoJump" -> "Авто прыжок";
            case "Auto Eat", "autoEat" -> "Авто еда";
            case "Backtrack", "backtrack" -> "Бэктрэк";
            case "Dynamic Island" -> "Динамический остров";
            case "GPS Navigation" -> "GPS Навигация";
            case "Snapping" -> "Привязка элементов";
            case "Collisions" -> "Коллизия";
            case "Guidelines" -> "Направляющие";
            default -> eng;
        };
    }

    private static class CardGroup {
        final String title;
        final List<DisplayItem> items = new ArrayList<>();
        CardGroup(String title) { this.title = title; }
    }

    private static class DisplayItem {
        final String displayName;
        final Module module;
        DisplayItem(String displayName, Module module) {
            this.displayName = displayName;
            this.module = module;
        }
    }
}