package error.ui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.account.AccountManager;
import error.ui.mainmenu.popup.SettingsPopup;
import error.module.Category;
import error.module.Module;
import error.module.impl.render.ClickGui;
import error.setting.SettingRenderer;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import java.util.ArrayList;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PanelLapSuperimposition {
    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "images/logo.png");
    private boolean dragging;
    private float dragOffsetX, dragOffsetY;
    private final SettingsPopup settingsPopup = new SettingsPopup();

    public void render(Minecraft mc, GuiGraphicsExtractor extractor, PanelLapState state, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        state.update();
        float openProgress = state.getOpenAnimation().getValue();
        if (openProgress <= 0.001F) return;

        if (!state.isPositionInitialized()) {
            state.setPanelX((screenWidth - state.getPanelWidth()) / 2.0F);
            state.setPanelY((screenHeight - state.getPanelHeight()) / 2.0F);
            state.setPositionInitialized(true);
        }

        if (dragging) {
            state.setPanelX(mouseX - dragOffsetX);
            state.setPanelY(mouseY - dragOffsetY);
        }

        float x = state.getPanelX();
        float y = state.getPanelY();
        float w = state.getPanelWidth();
        float h = state.getPanelHeight();

        float easeProgress = openProgress * openProgress * (3.0F - 2.0F * openProgress);
        float centerX = x + (w / 2.0F);
        float centerY = y + (h / 2.0F);
        float scale = 0.92F + (0.08F * easeProgress);

        float mainGuiAlpha = easeProgress;

        if (mainGuiAlpha > 0.01F) {
            // Ambient full-screen backdrop rendered BEFORE pose matrix scale transform
            boolean isBlur = Theme.getBackgroundMode().equalsIgnoreCase("Blur");
            if (isBlur) {
                int ambientBg = ColorUtil.rgba(10, 8, 16, (int) (140 * mainGuiAlpha));
                Render2D.drawRect(0, 0, screenWidth, screenHeight, ambientBg);
                Render2D.drawBlur(0, 0, screenWidth, screenHeight, 0.0F, 16.0F, ColorUtil.rgba(0, 0, 0, 80), mainGuiAlpha);
            } else {
                int ambientBg = ColorUtil.rgba(10, 8, 16, (int) (75 * mainGuiAlpha));
                Render2D.drawRect(0, 0, screenWidth, screenHeight, ambientBg);
            }
        }

        extractor.pose().pushMatrix();
        extractor.pose().translate(centerX, centerY);
        extractor.pose().scale(scale, scale);
        extractor.pose().translate(-centerX, -centerY);

        if (mainGuiAlpha > 0.01F) {
            // Top-Right Accent Beam
            int spotlightCol = ColorUtil.rgba(ColorUtil.red(Theme.getAccentColor()), ColorUtil.green(Theme.getAccentColor()), ColorUtil.blue(Theme.getAccentColor()), (int) (40 * mainGuiAlpha));
            Render2D.drawRoundedRect(screenWidth * 0.55F, -60.0F, screenWidth * 0.5F, 220.0F, 100.0F, spotlightCol);

            float sideW = 140.0F;
            float contentW = 385.0F;
            float totalW = sideW + contentW;
            float contentX = x + sideW;

            int themeAccent = Theme.getAccentColor();
            int ar = ColorUtil.red(themeAccent);
            int ag = ColorUtil.green(themeAccent);
            int ab = ColorUtil.blue(themeAccent);

            int laserCol = ColorUtil.multiplyAlpha(themeAccent, mainGuiAlpha);
            int laserGlow = ColorUtil.rgba(ar, ag, ab, (int) (110 * mainGuiAlpha));

            renderDescriptionAboveGui(state, centerX, y - 18.0F, mainGuiAlpha);

            // 1. SINGLE UNIFIED CONTAINER WINDOW (Liquid Glass Frame)
            int liquidGlassFill = ColorUtil.rgba((int)(18*0.85F + ar*0.15F), (int)(16*0.85F + ag*0.15F), (int)(24*0.85F + ab*0.15F), (int) (225 * mainGuiAlpha));
            int shadowColor = ColorUtil.rgba(0, 0, 0, (int) (180 * mainGuiAlpha));
            int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (55 * mainGuiAlpha));
            int glassHalo = ColorUtil.rgba(ar, ag, ab, (int) (35 * mainGuiAlpha));

            // Single Outer Window Frame (Shadow + Blur + Fill + Glass Border)
            Render2D.drawShadow(x, y, totalW, h, 14.0F, 14.0F, shadowColor);
            Render2D.drawShadow(x, y, totalW, h, 14.0F, 8.0F, glassHalo);
            Render2D.drawBlur(x, y, totalW, h, 14.0F, 24.0F, liquidGlassFill, mainGuiAlpha);
            Render2D.drawRoundedRect(x, y, totalW, h, 14.0F, liquidGlassFill);
            Render2D.drawRoundedOutline(x, y, totalW, h, 14.0F, 1.2F, glassBorder);

            // Top Neon Accent Line Across Entire Header
            Render2D.drawRoundedRect(x + 12.0F, y + 2.0F, totalW - 24.0F, 2.5F, 1.2F, laserCol);
            Render2D.drawShadow(x + 12.0F, y + 2.0F, totalW - 24.0F, 2.5F, 1.2F, 4.0F, laserGlow);

            // Subtle Vertical Separator Line between Left Sidebar and Right Content
            Render2D.drawRoundedRect(x + sideW, y + 36.0F, 1.0F, h - 46.0F, 0.0F, ColorUtil.rgba(255, 255, 255, (int) (22 * mainGuiAlpha)));

            // Top Left Branding Header
            float dropdownY = y + 10.0F;
            float brandW = sideW - 16.0F;
            int pillGlass = ColorUtil.rgba((int)(30*0.85F + ar*0.15F), (int)(28*0.85F + ag*0.15F), (int)(38*0.85F + ab*0.15F), (int) (140 * mainGuiAlpha));
            int pillBorder = ColorUtil.rgba(255, 255, 255, (int) (30 * mainGuiAlpha));

            Render2D.drawTexture(LOGO_TEX, x + 12.0F, dropdownY + 1.0F, 14.0F, 14.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * mainGuiAlpha)));
            float brandX = x + 12.0F + 14.0F + 6.0F;
            Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC ", brandX, dropdownY + 3.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * mainGuiAlpha)));
            brandX += Fonts.SF_MEDIUM.getWidth("Error DLC ", 8.5F);
            Fonts.drawString(Fonts.SF_MEDIUM, "26.2", brandX, dropdownY + 3.0F, 8.5F, ColorUtil.rgba(ar, ag, ab, (int) (240 * mainGuiAlpha)));

            // Categories List (Clean English Titles & Neon Active Bar)
            float catY = y + 38.0F;
            for (Category category : Category.values()) {
                boolean active = state.getCurrentTab() == PanelLapState.Tab.CATEGORY && category == state.getCurrentCategory();

                if (active) {
                    int activeCatBg = ColorUtil.rgba((int)(40*0.75F + ar*0.25F), (int)(38*0.75F + ag*0.25F), (int)(52*0.75F + ab*0.25F), (int) (180 * mainGuiAlpha));
                    int activeCatBorder = ColorUtil.rgba((int)(80*0.7F + ar*0.3F), (int)(75*0.7F + ag*0.3F), (int)(105*0.7F + ab*0.3F), (int) (160 * mainGuiAlpha));
                    Render2D.drawRoundedRect(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, activeCatBg);
                    Render2D.drawRoundedOutline(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, 1.0F, activeCatBorder);
                    Render2D.drawRoundedRect(x + 10.0F, catY + 3.0F, 3.0F, 13.0F, 1.5F, laserCol);
                    Render2D.drawShadow(x + 10.0F, catY + 3.0F, 3.0F, 13.0F, 1.5F, 4.0F, laserGlow);
                }

                int col = active ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(200, 195, 215, 180);
                Fonts.drawIcon(getCategoryIcon(category), x + 18.0F, catY + 4.5F, 9.5F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                Fonts.drawString(Fonts.SF_MEDIUM, category.name(), x + 32.0F, catY + 5.0F, 8.0F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                catY += 23.0F;
            }

            // Bottom Search Input
            float searchY = y + h - 50.0F;
            int curSearchBorder = state.isSearchFocused() ? laserCol : pillBorder;
            Render2D.drawRoundedRect(x + 8.0F, searchY, sideW - 16.0F, 18.0F, 6.0F, pillGlass);
            Render2D.drawRoundedOutline(x + 8.0F, searchY, sideW - 16.0F, 18.0F, 6.0F, 1.0F, curSearchBorder);

            int searchTextColor = state.isSearchFocused() || !state.getSearchQuery().isEmpty()
                    ? ColorUtil.rgba(255, 255, 255, (int) (240 * mainGuiAlpha))
                    : ColorUtil.rgba(180, 180, 200, (int) (160 * mainGuiAlpha));

            Fonts.drawIcon(IconUse.SEARCH, x + 14.0F, searchY + 4.5F, 8.0F, ColorUtil.multiplyAlpha(searchTextColor, mainGuiAlpha));

            String searchDisplay;
            if (state.getSearchQuery().isEmpty()) {
                searchDisplay = state.isSearchFocused() ? "|" : "Search...";
            } else {
                boolean cursorBlink = state.isSearchFocused() && (System.currentTimeMillis() % 1000 > 500);
                searchDisplay = state.getSearchQuery() + (cursorBlink ? "|" : "");
            }
            Fonts.drawString(Fonts.SF_MEDIUM, searchDisplay, x + 25.0F, searchY + 4.5F, 7.5F, searchTextColor);

            // User Profile Footer: Nickname "Walfini Develop" and Avatar
            float userY = y + h - 28.0F;
            String curUser = "Walfini Develop";

            Render2D.drawShadow(x + 10.0F, userY + 1.0F, 18.0F, 18.0F, 9.0F, 6.0F, laserGlow);
            Render2D.drawCustomAvatar(x + 10.0F, userY + 1.0F, 18.0F, 9.0F, mainGuiAlpha);
            Render2D.drawRoundedOutline(x + 10.0F, userY + 1.0F, 18.0F, 18.0F, 9.0F, 1.0F, laserCol);

            Fonts.drawString(Fonts.SF_MEDIUM, curUser, x + 34.0F, userY + 5.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * mainGuiAlpha)));
            int betaBg = ColorUtil.rgba(ar, ag, ab, (int) (140 * mainGuiAlpha));
            Render2D.drawRoundedRect(x + sideW - 36.0F, userY + 4.5F, 24.0F, 11.0F, 3.0F, betaBg);
            Render2D.drawRoundedOutline(x + sideW - 36.0F, userY + 4.5F, 24.0F, 11.0F, 3.0F, 1.0F, laserCol);
            Fonts.drawString(Fonts.SF_MEDIUM, "BETA", x + sideW - 34.0F, userY + 6.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * mainGuiAlpha)));

            // 2. RIGHT MAIN CONTENT AREA INSIDE UNIFIED CONTAINER
            float breadY = y + 10.0F;
            Fonts.drawString(Fonts.SF_MEDIUM, "ERROR DLC  /  " + state.getCurrentCategory().name(), contentX + 14.0F, breadY + 2.0F, 8.5F, ColorUtil.rgba(230, 225, 245, (int) (240 * mainGuiAlpha)));

            float themeBtnW = 68.0F;
            float themeBtnH = 17.0F;
            float themeBtnX = contentX + contentW - themeBtnW - 12.0F;
            float themeBtnY = breadY;

            boolean themeHovered = mouseX >= themeBtnX && mouseX <= themeBtnX + themeBtnW && mouseY >= themeBtnY && mouseY <= themeBtnY + themeBtnH;
            boolean themeOpen = state.isSettingsPopupOpen();

            int themeBtnBg = themeOpen || themeHovered
                    ? ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), (int) (65 * mainGuiAlpha))
                    : pillGlass;
            int themeBtnBorder = themeOpen || themeHovered ? laserCol : pillBorder;

            Render2D.drawRoundedRect(themeBtnX, themeBtnY, themeBtnW, themeBtnH, 5.0F, themeBtnBg);
            Render2D.drawRoundedOutline(themeBtnX, themeBtnY, themeBtnW, themeBtnH, 5.0F, 1.0F, themeBtnBorder);
            // Glowing Accent Color Dot matching HUD and theme
            Render2D.drawShadow(themeBtnX + 7.0F, themeBtnY + 5.5F, 6.0F, 6.0F, 3.0F, 3.0F, laserGlow);
            Render2D.drawRoundedRect(themeBtnX + 7.0F, themeBtnY + 5.5F, 6.0F, 6.0F, 3.0F, laserCol);
            Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Theme"), themeBtnX + 17.0F, themeBtnY + 3.5F, 7.5F, ColorUtil.rgba(240, 240, 255, (int) (230 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, themeOpen ? "^" : "v", themeBtnX + themeBtnW - 11.0F, themeBtnY + 4.0F, 6.0F, ColorUtil.rgba(200, 200, 220, (int) (180 * mainGuiAlpha)));

            renderSearchAndHintsUnderGui(state, centerX, y + h + 16.0F, mainGuiAlpha);
        }

        // Inner Modules Scroll Area
        float sideW = 140.0F;
        float contentX = x + sideW;
        float contentY = y + 32.0F;
        float contentW = 385.0F;
        float contentH = h - 40.0F;

        float catProgress = state.getCategoryAnim().getValue();
        float contentAlpha = easeProgress * catProgress;
        float slideY = (1.0F - catProgress) * (state.getCategoryDirection() * 12.0F);

        state.setHoveredModule(null);

        Render2D.pushScissor(contentX + 4.0F, contentY, contentW - 8.0F, contentH);
        if (state.getCurrentCategory() == Category.CONFIGS) {
            renderConfigs(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        } else if (state.getCurrentCategory() == Category.FRIENDS) {
            renderFriends(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        } else if (state.getCurrentCategory() == Category.COSMETICS) {
            renderCosmetics(state, extractor, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        } else if (state.getCurrentCategory() == Category.THEMES) {
            renderThemes(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        } else if (state.getCurrentCategory() == Category.EVENTS) {
            renderEvents(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        } else {
            renderModules(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        }
        Render2D.popScissor();

        // Settings Popup Dropdown
        float popupProgress = state.getSettingsPopupAnim().getValue();
        if (popupProgress > 0.01F) {
            float popupW = settingsPopup.getWidth();
            float themeBtnW = 68.0F;
            float themeBtnX = contentX + contentW - themeBtnW - 12.0F;
            float themeBtnY = y + 8.0F;
            float popupX = themeBtnX + themeBtnW - popupW;
            float popupY = themeBtnY + 17.0F + 5.0F;
            settingsPopup.render(popupX, popupY, mouseX, mouseY, mainGuiAlpha * popupProgress);
        }

        extractor.pose().popMatrix();

        if (state.getActiveModal() != null) {
            state.getActiveModal().render(mouseX, mouseY, screenWidth, screenHeight, openProgress);
            if (state.getActiveModal().isFinished()) {
                state.setActiveModal(null);
            }
        }
    }

    private void renderDescriptionAboveGui(PanelLapState state, float centerX, float targetY, float alpha) {
        float descProgress = state.getDescAnim().getValue();
        if (descProgress <= 0.01F || state.getLastDescription().isEmpty()) return;

        float effectiveAlpha = alpha * descProgress;
        float slideOffset = (1.0F - descProgress) * 4.0F;
        float drawY = targetY - slideOffset;

        String desc = Localization.get(state.getLastDescription());
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, desc, centerX, drawY + 3.0F, 9.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, effectiveAlpha));
    }

    private void renderSearchAndHintsUnderGui(PanelLapState state, float centerX, float startY, float alpha) {
        String hint = "MB3 / ⌨ — Бинд  •  ПКМ — Настройки модуля";
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, hint, centerX, startY, 7.5F, ColorUtil.multiplyAlpha(ColorUtil.rgba(180, 180, 200, 200), alpha * 0.85F));
    }

    private void renderModules(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        List<Module> list = getFilteredModules(state);
        float colWidth = totalWidth;
        float currentScroll = state.getScrollOffset();
        float currentY = startY - currentScroll;

        for (Module module : list) {
            float modX = startX;
            float modY = currentY;

            module.getExpandAnim().setTarget(module.isExpanded() ? 1.0F : 0.0F);
            module.getExpandAnim().update();

            float totalSetH = 0.0F;
            for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                sr.updateVisibility();
                float sH = sr.getHeight();
                if (sH > 0.1F) {
                    totalSetH += sH + 2.0F;
                }
            }

            float cardHeight = 22.0F + (module.getExpandAnim().getValue() * (totalSetH + 4.0F));

            boolean inVisibleArea = mouseY >= startY && mouseY <= startY + totalHeight;
            boolean hovered = inVisibleArea && mouseX >= modX && mouseX <= modX + colWidth && mouseY >= modY && mouseY <= modY + cardHeight;

            if (hovered && module.hasDescription() && !state.isHoldActive()) {
                state.setHoveredModule(module);
            }

            // Pure liquid glass translucent container (NO heavy dark opaque blocks!)
            int cardBg = hovered ? ColorUtil.rgba(255, 255, 255, (int) (14 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (6 * alpha));
            int cardOutline = hovered ? ColorUtil.rgba(255, 255, 255, (int) (32 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (16 * alpha));

            Render2D.drawRoundedRect(modX, modY, colWidth, cardHeight, 7.0F, cardBg);
            Render2D.drawRoundedOutline(modX, modY, colWidth, cardHeight, 7.0F, 1.0F, cardOutline);

            // Module Title
            int textCol = module.isState() ? ColorUtil.WHITE : ColorUtil.rgba(205, 200, 220, 200);
            Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), modX + 8.0F, modY + 5.5F, 8.5F, ColorUtil.multiplyAlpha(textCol, alpha));

            // Module State Toggle Switch Pill
            float toggleW = 20.0F;
            float toggleH = 10.0F;
            float toggleX = modX + colWidth - toggleW - 8.0F;
            float toggleY = modY + 6.0F;

            if (module.isState()) {
                int accent = Theme.getAccentColor();
                int toggleBg = ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (240 * alpha));
                Render2D.drawShadow(toggleX, toggleY, toggleW, toggleH, 5.0F, 4.0F, ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (110 * alpha)));
                Render2D.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, 5.0F, toggleBg);
                Render2D.drawRoundedRect(toggleX + toggleW - 8.0F, toggleY + 1.5F, 7.0F, 7.0F, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
            } else {
                int toggleBg = ColorUtil.rgba(255, 255, 255, (int) (20 * alpha));
                Render2D.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, 5.0F, toggleBg);
                Render2D.drawRoundedOutline(toggleX, toggleY, toggleW, toggleH, 5.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));
                Render2D.drawRoundedRect(toggleX + 2.0F, toggleY + 1.5F, 7.0F, 7.0F, 3.5F, ColorUtil.rgba(200, 195, 215, (int) (200 * alpha)));
            }

            // Keybind Pill & Keyboard Icon
            boolean isBound = module.getBind().isBound();
            float bindFontSz = 7.0F;
            float bindPillH = 10.0F;
            float bindPillW;
            float bindPillX;
            float bindPillY = modY + 6.0F;
            int accent = Theme.getAccentColor();

            if (isBound) {
                String bindText = module.getBind().getDisplayValue();
                bindPillW = Fonts.SF_MEDIUM.getWidth(bindText, bindFontSz) + 8.0F;
                bindPillX = toggleX - bindPillW - 6.0F;

                boolean bindHovered = hovered && mouseX >= bindPillX && mouseX <= bindPillX + bindPillW && mouseY >= bindPillY && mouseY <= bindPillY + bindPillH;
                int bindBg = bindHovered ? ColorUtil.multiplyAlpha(accent, 0.45F * alpha) : ColorUtil.multiplyAlpha(accent, 0.22F * alpha);
                int bindBorder = ColorUtil.multiplyAlpha(accent, alpha);

                Render2D.drawRoundedRect(bindPillX, bindPillY, bindPillW, bindPillH, 3.5F, bindBg);
                Render2D.drawRoundedOutline(bindPillX, bindPillY, bindPillW, bindPillH, 3.5F, 1.0F, bindBorder);
                Fonts.drawString(Fonts.SF_MEDIUM, bindText, bindPillX + 4.0F, bindPillY + 1.5F, bindFontSz, ColorUtil.rgba(255, 255, 255, (int) ((bindHovered ? 255 : 230) * alpha)));
            } else {
                bindPillW = 14.0F;
                bindPillX = toggleX - bindPillW - 6.0F;

                boolean bindHovered = hovered && mouseX >= bindPillX && mouseX <= bindPillX + bindPillW && mouseY >= bindPillY && mouseY <= bindPillY + bindPillH;
                int bindBg = bindHovered ? ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (14 * alpha));
                int bindBorder = ColorUtil.rgba(255, 255, 255, (int) (25 * alpha));

                Render2D.drawRoundedRect(bindPillX, bindPillY, bindPillW, bindPillH, 3.5F, bindBg);
                Render2D.drawRoundedOutline(bindPillX, bindPillY, bindPillW, bindPillH, 3.5F, 1.0F, bindBorder);
                Fonts.drawIcon(IconUse.KEYBOARD, bindPillX + 3.0F, bindPillY + 1.5F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) ((bindHovered ? 255 : 170) * alpha)));
            }

            if (module.getExpandAnim().getValue() > 0.02F) {
                float setY = modY + 22.0F;
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    float sH = sr.getHeight();
                    if (sH > 0.5F) {
                        sr.render(modX + 6.0F, setY, colWidth - 12.0F, sH, mouseX, mouseY, alpha * module.getExpandAnim().getValue());
                        setY += sH + 2.0F;
                    }
                }
            }

            currentY += cardHeight + 4.0F;
        }

        float totalContentH = currentY + currentScroll - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private List<Module> getFilteredModules(PanelLapState state) {
        String query = state.getSearchQuery().trim().toLowerCase();
        return Client.INSTANCE.moduleManager.getModules().stream()
                .filter(m -> query.isEmpty() ? m.getCategory() == state.getCurrentCategory() : m.getName().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    private void renderConfigs(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float currentY = startY - state.getScrollOffset();
        int accent = Theme.getAccentColor();

        float rowH = 20.0F;
        float createW = (totalWidth - 10.0F) * 0.5F;
        float createX = startX;
        float inputW = createW - 55.0F;
        float btnW = 50.0F;

        int createBorder = state.isConfigInputFocused() ? accent : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));
        Render2D.drawRoundedRect(createX, currentY, inputW, rowH, 5.0F, ColorUtil.rgba(30, 25, 40, (int) (160 * alpha)));
        Render2D.drawRoundedOutline(createX, currentY, inputW, rowH, 5.0F, 1.0F, createBorder);

        String cfgDisplay = state.getConfigInput().isEmpty() ? (state.isConfigInputFocused() ? "|" : "Имя конфига...") : state.getConfigInput() + (state.isConfigInputFocused() && System.currentTimeMillis() % 1000 > 500 ? "|" : "");
        int cfgColor = state.getConfigInput().isEmpty() && !state.isConfigInputFocused() ? ColorUtil.rgba(160, 155, 175, (int) (160 * alpha)) : ColorUtil.rgba(240, 240, 255, (int) (240 * alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, cfgDisplay, createX + 6.0F, currentY + 5.5F, 7.5F, cfgColor);

        Render2D.drawRoundedRect(createX + inputW + 5.0F, currentY, btnW, rowH, 5.0F, ColorUtil.multiplyAlpha(accent, 0.70F * alpha));
        Render2D.drawRoundedOutline(createX + inputW + 5.0F, currentY, btnW, rowH, 5.0F, 1.0F, accent);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Создать", createX + inputW + 5.0F + btnW / 2.0F, currentY + 5.5F, 7.0F, 0xFFFFFFFF);

        float importX = createX + createW + 10.0F;
        float importInputW = createW - 55.0F;

        int importBorder = state.isShareCodeInputFocused() ? accent : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));
        Render2D.drawRoundedRect(importX, currentY, importInputW, rowH, 5.0F, ColorUtil.rgba(30, 25, 40, (int) (160 * alpha)));
        Render2D.drawRoundedOutline(importX, currentY, importInputW, rowH, 5.0F, 1.0F, importBorder);

        String codeDisplay = state.getShareCodeInput().isEmpty() ? (state.isShareCodeInputFocused() ? "|" : "Вставьте код...") : state.getShareCodeInput() + (state.isShareCodeInputFocused() && System.currentTimeMillis() % 1000 > 500 ? "|" : "");
        int codeColor = state.getShareCodeInput().isEmpty() && !state.isShareCodeInputFocused() ? ColorUtil.rgba(160, 155, 175, (int) (160 * alpha)) : ColorUtil.rgba(240, 240, 255, (int) (240 * alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, codeDisplay, importX + 6.0F, currentY + 5.5F, 7.5F, codeColor);

        Render2D.drawRoundedRect(importX + importInputW + 5.0F, currentY, btnW, rowH, 5.0F, ColorUtil.multiplyAlpha(accent, 0.70F * alpha));
        Render2D.drawRoundedOutline(importX + importInputW + 5.0F, currentY, btnW, rowH, 5.0F, 1.0F, accent);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Импорт", importX + importInputW + 5.0F + btnW / 2.0F, currentY + 5.5F, 7.0F, 0xFFFFFFFF);

        currentY += rowH + 12.0F;

        List<String> configs = Client.INSTANCE.configManager.getAvailableConfigs();
        Fonts.drawString(Fonts.SF_MEDIUM, "Сохраненные конфигурации (" + configs.size() + "):", startX, currentY, 8.0F, ColorUtil.rgba(200, 195, 215, (int) (200 * alpha)));
        currentY += 14.0F;

        String activeConfig = Client.INSTANCE.configManager.getCurrentConfig();

        for (String config : configs) {
            float cardH = 28.0F;
            boolean isActive = config.equalsIgnoreCase(activeConfig);
            boolean cardHovered = mouseX >= startX && mouseX <= startX + totalWidth && mouseY >= currentY && mouseY <= currentY + cardH;

            int cardBg = cardHovered ? ColorUtil.rgba(255, 255, 255, (int) (16 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (8 * alpha));
            int cardOutline = isActive ? accent : (cardHovered ? ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)));

            Render2D.drawRoundedRect(startX, currentY, totalWidth, cardH, 6.0F, cardBg);
            Render2D.drawRoundedOutline(startX, currentY, totalWidth, cardH, 6.0F, 1.0F, cardOutline);

            Fonts.drawIcon(IconUse.GEAR, startX + 8.0F, currentY + 8.0F, 9.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, config, startX + 22.0F, currentY + 8.5F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

            if (isActive) {
                float activeTagX = startX + 22.0F + Fonts.SF_MEDIUM.getWidth(config, 8.5F) + 8.0F;
                Render2D.drawRoundedRect(activeTagX, currentY + 7.0F, 44.0F, 12.0F, 3.0F, ColorUtil.multiplyAlpha(accent, 0.40F * alpha));
                Fonts.drawString(Fonts.SF_MEDIUM, "АКТИВЕН", activeTagX + 4.0F, currentY + 9.0F, 6.5F, 0xFFFFFFFF);
            }

            float actionBtnW = 46.0F;
            float actionBtnH = 16.0F;
            float actionY = currentY + 6.0F;

            float delX = startX + totalWidth - actionBtnW - 6.0F;
            boolean isDefault = config.equalsIgnoreCase("default");
            int delBg = isDefault ? ColorUtil.rgba(40, 35, 45, (int) (80 * alpha)) : ColorUtil.rgba(180, 50, 60, (int) (140 * alpha));
            Render2D.drawRoundedRect(delX, actionY, actionBtnW, actionBtnH, 4.0F, delBg);
            Render2D.drawRoundedOutline(delX, actionY, actionBtnW, actionBtnH, 4.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * alpha)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Удалить", delX + actionBtnW / 2.0F, actionY + 4.0F, 6.5F, isDefault ? ColorUtil.rgba(140, 135, 150, (int) (140 * alpha)) : 0xFFFFFFFF);

            float codeX = delX - actionBtnW - 4.0F;
            Render2D.drawRoundedRect(codeX, actionY, actionBtnW, actionBtnH, 4.0F, ColorUtil.rgba(100, 60, 160, (int) (140 * alpha)));
            Render2D.drawRoundedOutline(codeX, actionY, actionBtnW, actionBtnH, 4.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * alpha)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Код", codeX + actionBtnW / 2.0F, actionY + 4.0F, 6.5F, 0xFFFFFFFF);

            float saveX = codeX - actionBtnW - 4.0F;
            Render2D.drawRoundedRect(saveX, actionY, actionBtnW, actionBtnH, 4.0F, ColorUtil.rgba(50, 120, 180, (int) (140 * alpha)));
            Render2D.drawRoundedOutline(saveX, actionY, actionBtnW, actionBtnH, 4.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * alpha)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Сохранить", saveX + actionBtnW / 2.0F, actionY + 4.0F, 6.5F, 0xFFFFFFFF);

            float loadX = saveX - actionBtnW - 4.0F;
            Render2D.drawRoundedRect(loadX, actionY, actionBtnW, actionBtnH, 4.0F, ColorUtil.multiplyAlpha(accent, 0.60F * alpha));
            Render2D.drawRoundedOutline(loadX, actionY, actionBtnW, actionBtnH, 4.0F, 1.0F, accent);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadX + actionBtnW / 2.0F, actionY + 4.0F, 6.5F, 0xFFFFFFFF);

            currentY += cardH + 4.0F;
        }

        float totalContentH = currentY + state.getScrollOffset() - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private void renderFriends(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float currentY = startY - state.getScrollOffset();
        int accent = Theme.getAccentColor();

        float rowH = 20.0F;
        float inputW = totalWidth - 65.0F;
        float btnW = 60.0F;

        int friendBorder = state.isFriendInputFocused() ? accent : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));
        Render2D.drawRoundedRect(startX, currentY, inputW, rowH, 5.0F, ColorUtil.rgba(30, 25, 40, (int) (160 * alpha)));
        Render2D.drawRoundedOutline(startX, currentY, inputW, rowH, 5.0F, 1.0F, friendBorder);

        String friendDisplay = state.getFriendInput().isEmpty() ? (state.isFriendInputFocused() ? "|" : "Никнейм друга...") : state.getFriendInput() + (state.isFriendInputFocused() && System.currentTimeMillis() % 1000 > 500 ? "|" : "");
        int friendColor = state.getFriendInput().isEmpty() && !state.isFriendInputFocused() ? ColorUtil.rgba(160, 155, 175, (int) (160 * alpha)) : ColorUtil.rgba(240, 240, 255, (int) (240 * alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, friendDisplay, startX + 8.0F, currentY + 5.5F, 7.5F, friendColor);

        Render2D.drawRoundedRect(startX + inputW + 5.0F, currentY, btnW, rowH, 5.0F, ColorUtil.multiplyAlpha(accent, 0.70F * alpha));
        Render2D.drawRoundedOutline(startX + inputW + 5.0F, currentY, btnW, rowH, 5.0F, 1.0F, accent);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Добавить", startX + inputW + 5.0F + btnW / 2.0F, currentY + 5.5F, 7.0F, 0xFFFFFFFF);

        currentY += rowH + 12.0F;

        java.util.Set<String> friends = error.friend.FriendManager.getInstance().getFriends();
        Fonts.drawString(Fonts.SF_MEDIUM, "Список друзей (" + friends.size() + "):", startX, currentY, 8.0F, ColorUtil.rgba(200, 195, 215, (int) (200 * alpha)));
        currentY += 14.0F;

        for (String friendName : friends) {
            float cardH = 26.0F;
            boolean cardHovered = mouseX >= startX && mouseX <= startX + totalWidth && mouseY >= currentY && mouseY <= currentY + cardH;

            int cardBg = cardHovered ? ColorUtil.rgba(255, 255, 255, (int) (16 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (8 * alpha));
            int cardOutline = cardHovered ? ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alpha));

            Render2D.drawRoundedRect(startX, currentY, totalWidth, cardH, 6.0F, cardBg);
            Render2D.drawRoundedOutline(startX, currentY, totalWidth, cardH, 6.0F, 1.0F, cardOutline);

            java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + friendName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Identifier skinTexture = DefaultPlayerSkin.get(uuid).body().texturePath();

            Render2D.drawHead(skinTexture, startX + 6.0F, currentY + 4.0F, 18.0F, 9.0F, alpha);
            Render2D.drawRoundedOutline(startX + 6.0F, currentY + 4.0F, 18.0F, 18.0F, 9.0F, 1.0F, accent);

            Fonts.drawString(Fonts.SF_MEDIUM, friendName, startX + 30.0F, currentY + 7.5F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

            float remBtnW = 55.0F;
            float remBtnH = 16.0F;
            float remX = startX + totalWidth - remBtnW - 6.0F;
            float remY = currentY + 5.0F;

            Render2D.drawRoundedRect(remX, remY, remBtnW, remBtnH, 4.0F, ColorUtil.rgba(180, 50, 60, (int) (140 * alpha)));
            Render2D.drawRoundedOutline(remX, remY, remBtnW, remBtnH, 4.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * alpha)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Удалить", remX + remBtnW / 2.0F, remY + 4.0F, 6.5F, 0xFFFFFFFF);

            currentY += cardH + 4.0F;
        }

        float totalContentH = currentY + state.getScrollOffset() - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private boolean handleConfigsClick(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float currentY = startY - state.getScrollOffset();
        float rowH = 20.0F;

        float createW = (totalWidth - 10.0F) * 0.5F;
        float createX = startX;
        float inputW = createW - 55.0F;
        float btnW = 50.0F;

        if (mouseX >= createX && mouseX <= createX + inputW && mouseY >= currentY && mouseY <= currentY + rowH) {
            state.setConfigInputFocused(true);
            state.setShareCodeInputFocused(false);
            state.setFriendInputFocused(false);
            return true;
        }

        float createBtnX = createX + inputW + 5.0F;
        if (mouseX >= createBtnX && mouseX <= createBtnX + btnW && mouseY >= currentY && mouseY <= currentY + rowH) {
            if (!state.getConfigInput().trim().isEmpty()) {
                Client.INSTANCE.configManager.saveConfig(state.getConfigInput().trim(), true);
                state.setConfigInput("");
            }
            state.setConfigInputFocused(false);
            return true;
        }

        float importX = createX + createW + 10.0F;
        float importInputW = createW - 55.0F;

        if (mouseX >= importX && mouseX <= importX + importInputW && mouseY >= currentY && mouseY <= currentY + rowH) {
            state.setShareCodeInputFocused(true);
            state.setConfigInputFocused(false);
            state.setFriendInputFocused(false);
            return true;
        }

        float importBtnX = importX + importInputW + 5.0F;
        if (mouseX >= importBtnX && mouseX <= importBtnX + btnW && mouseY >= currentY && mouseY <= currentY + rowH) {
            if (!state.getShareCodeInput().trim().isEmpty()) {
                Client.INSTANCE.configManager.loadShareCode(state.getShareCodeInput().trim(), true);
                state.setShareCodeInput("");
            }
            state.setShareCodeInputFocused(false);
            return true;
        }

        state.setConfigInputFocused(false);
        state.setShareCodeInputFocused(false);

        currentY += rowH + 12.0F + 14.0F;

        List<String> configs = Client.INSTANCE.configManager.getAvailableConfigs();
        for (String config : configs) {
            float cardH = 28.0F;
            if (mouseY >= currentY && mouseY <= currentY + cardH) {
                float actionBtnW = 46.0F;
                float actionBtnH = 16.0F;
                float actionY = currentY + 6.0F;

                float delX = startX + totalWidth - actionBtnW - 6.0F;
                if (mouseX >= delX && mouseX <= delX + actionBtnW && mouseY >= actionY && mouseY <= actionY + actionBtnH) {
                    if (!config.equalsIgnoreCase("default")) {
                        Client.INSTANCE.configManager.deleteConfig(config);
                    }
                    return true;
                }

                float codeX = delX - actionBtnW - 4.0F;
                if (mouseX >= codeX && mouseX <= codeX + actionBtnW && mouseY >= actionY && mouseY <= actionY + actionBtnH) {
                    state.setActiveModal(new error.ui.mainmenu.popup.ShareCodeModal(config));
                    return true;
                }

                float saveX = codeX - actionBtnW - 4.0F;
                if (mouseX >= saveX && mouseX <= saveX + actionBtnW && mouseY >= actionY && mouseY <= actionY + actionBtnH) {
                    Client.INSTANCE.configManager.saveConfig(config, true);
                    return true;
                }

                float loadX = saveX - actionBtnW - 4.0F;
                if (mouseX >= loadX && mouseX <= loadX + actionBtnW && mouseY >= actionY && mouseY <= actionY + actionBtnH) {
                    Client.INSTANCE.configManager.loadConfig(config, true);
                    return true;
                }
            }
            currentY += cardH + 4.0F;
        }

        return true;
    }

    private boolean handleFriendsClick(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float currentY = startY - state.getScrollOffset();
        float rowH = 20.0F;
        float inputW = totalWidth - 65.0F;
        float btnW = 60.0F;

        if (mouseX >= startX && mouseX <= startX + inputW && mouseY >= currentY && mouseY <= currentY + rowH) {
            state.setFriendInputFocused(true);
            state.setConfigInputFocused(false);
            state.setShareCodeInputFocused(false);
            return true;
        }

        float addBtnX = startX + inputW + 5.0F;
        if (mouseX >= addBtnX && mouseX <= addBtnX + btnW && mouseY >= currentY && mouseY <= currentY + rowH) {
            if (!state.getFriendInput().trim().isEmpty()) {
                error.friend.FriendManager.getInstance().addFriend(state.getFriendInput().trim());
                error.util.client.persiki.ChatUtil.success("Друг '" + state.getFriendInput().trim() + "' добавлен!");
                state.setFriendInput("");
            }
            state.setFriendInputFocused(false);
            return true;
        }

        state.setFriendInputFocused(false);

        currentY += rowH + 12.0F + 14.0F;

        java.util.Set<String> friends = error.friend.FriendManager.getInstance().getFriends();
        for (String friendName : new java.util.ArrayList<>(friends)) {
            float cardH = 26.0F;
            if (mouseY >= currentY && mouseY <= currentY + cardH) {
                float remBtnW = 55.0F;
                float remBtnH = 16.0F;
                float remX = startX + totalWidth - remBtnW - 6.0F;
                float remY = currentY + 5.0F;

                if (mouseX >= remX && mouseX <= remX + remBtnW && mouseY >= remY && mouseY <= remY + remBtnH) {
                    error.friend.FriendManager.getInstance().removeFriend(friendName);
                    error.util.client.persiki.ChatUtil.success("Друг '" + friendName + "' удален.");
                    return true;
                }
            }
            currentY += cardH + 4.0F;
        }

        return true;
    }

    private static float previewYaw = 0.0F;
    private static float previewPitch = 0.0F;
    private static boolean previewDragging = false;
    private static float dragStartX, dragStartY;

    private void renderCosmetics(PanelLapState state, GuiGraphicsExtractor extractor, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float previewW = 140.0F;
        float previewH = totalHeight;
        float previewX = startX + totalWidth - previewW;
        float modulesW = totalWidth - previewW - 12.0F;

        // Render Modules on the left side of Cosmetics tab
        renderModules(state, startX, startY, modulesW, totalHeight, mouseX, mouseY, alpha);

        // Render 3D Player Preview Card on the right side
        int previewBg = ColorUtil.rgba(255, 255, 255, (int) (10 * alpha));
        int previewOutline = ColorUtil.rgba(255, 255, 255, (int) (24 * alpha));
        int accent = Theme.getAccentColor();

        Render2D.drawRoundedRect(previewX, startY, previewW, previewH, 8.0F, previewBg);
        Render2D.drawRoundedOutline(previewX, startY, previewW, previewH, 8.0F, 1.0F, previewOutline);

        // Header Title
        Fonts.drawString(Fonts.SF_MEDIUM, "Предпросмотр", previewX + 10.0F, startY + 8.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (230 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Зажмите ЛКМ для вращения", previewX + 10.0F, startY + 20.0F, 6.5F, ColorUtil.rgba(170, 165, 185, (int) (170 * alpha)));

        // Handle Mouse Dragging Rotation
        boolean hovered = mouseX >= previewX && mouseX <= previewX + previewW && mouseY >= startY + 30.0F && mouseY <= startY + previewH - 20.0F;
        if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            if (!previewDragging && hovered) {
                previewDragging = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
            } else if (previewDragging) {
                previewYaw += (mouseX - dragStartX) * 1.2F;
                previewPitch = Math.max(-45.0F, Math.min(45.0F, previewPitch + (mouseY - dragStartY) * 0.8F));
                dragStartX = mouseX;
                dragStartY = mouseY;
            }
        } else {
            previewDragging = false;
        }

        // 3D Player Model Rendering inside GUI
        if (Minecraft.getInstance().player != null) {
            int playerScale = 50;

            try {
                net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                        extractor,
                        (int) (previewX + 10.0F),
                        (int) (startY + 30.0F),
                        (int) (previewX + previewW - 10.0F),
                        (int) (startY + previewH - 25.0F),
                        playerScale,
                        0.06F,
                        previewYaw + mouseX,
                        previewPitch + mouseY,
                        Minecraft.getInstance().player
                );
            } catch (Throwable ignored) {}
        }

        // Reset rotation button
        float resetBtnW = 60.0F;
        float resetBtnH = 14.0F;
        float resetBtnX = previewX + (previewW - resetBtnW) / 2.0F;
        float resetBtnY = startY + previewH - 18.0F;

        boolean resetHovered = mouseX >= resetBtnX && mouseX <= resetBtnX + resetBtnW && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH;
        int resetBg = resetHovered ? ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (120 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alpha));
        Render2D.drawRoundedRect(resetBtnX, resetBtnY, resetBtnW, resetBtnH, 4.0F, resetBg);
        Render2D.drawRoundedOutline(resetBtnX, resetBtnY, resetBtnW, resetBtnH, 4.0F, 1.0F, resetHovered ? accent : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Сброс", resetBtnX + resetBtnW / 2.0F, resetBtnY + 3.0F, 6.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));

        if (resetHovered && org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            previewYaw = 0.0F;
            previewPitch = 0.0F;
        }
    }

    private IconUse getCategoryIcon(Category category) {
        return switch (category) {
            case COMBAT -> IconUse.FIGHT;
            case MOVEMENT -> IconUse.MOVEMENT;
            case PLAYER -> IconUse.PLAYER;
            case RENDER -> IconUse.RENDER;
            case COSMETICS -> IconUse.STAR;
            case MISC -> IconUse.MISC;
            case CONFIGS -> IconUse.GEAR;
            case FRIENDS -> IconUse.GROUP;
            case THEMES -> IconUse.SPUTNIK;
            case EVENTS -> IconUse.COMPASS;
        };
    }

    private void renderEvents(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float currentY = startY - state.getScrollOffset();
        int accent = Theme.getAccentColor();

        // Server Sub-tabs
        String[] serverTabs = new String[] {"HolyWorld", "FunTime", "SpookyTime"};
        float tabW = (totalWidth - 12.0F) / 3.0F;
        float tabH = 22.0F;

        for (int i = 0; i < serverTabs.length; i++) {
            float tx = startX + i * (tabW + 6.0F);
            float ty = currentY;
            String serverName = serverTabs[i];

            boolean selected = state.getEventServerTab().equalsIgnoreCase(serverName);
            boolean hovered = mouseX >= tx && mouseX <= tx + tabW && mouseY >= ty && mouseY <= ty + tabH;

            int bg = selected ? ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (70 * alpha))
                              : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (10 * alpha)));
            int border = selected ? accent : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

            Render2D.drawRoundedRect(tx, ty, tabW, tabH, 5.0F, bg);
            Render2D.drawRoundedOutline(tx, ty, tabW, ty + tabH, 5.0F, 1.0F, border);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, serverName, tx + tabW / 2.0F, ty + 6.0F, 7.5F, selected ? 0xFFFFFFFF : ColorUtil.rgba(200, 200, 220, (int) (200 * alpha)));
        }
        currentY += tabH + 16.0F;

        String currentServer = state.getEventServerTab();
        Fonts.drawString(Fonts.SF_MEDIUM, "Активные ивенты — " + currentServer, startX, currentY, 8.5F, ColorUtil.rgba(240, 240, 255, (int) (240 * alpha)));
        currentY += 14.0F;

        // Render event cards based on currentServer tab
        record EventCard(String name, String location, String status, String timer, int statusColor) {}
        List<EventCard> eventCards = new ArrayList<>();

        if (currentServer.equalsIgnoreCase("HolyWorld")) {
            eventCards.add(new EventCard("Мистический Сундук", "Анархия #1 (X: 1420, Z: -850)", "АКТИВЕН", "04:12", ColorUtil.rgba(40, 220, 100, 255)));
            eventCards.add(new EventCard("Упавший Метеорит", "Анархия #3 (X: -610, Z: 1100)", "СПАВН ЧЕРЕЗ", "12:45", ColorUtil.rgba(255, 180, 40, 255)));
            eventCards.add(new EventCard("Извержение Вулкана", "Анархия #2 (X: 2040, Z: 450)", "ОЖИДАНИЕ", "28:10", ColorUtil.rgba(160, 160, 180, 255)));
        } else if (currentServer.equalsIgnoreCase("FunTime")) {
            eventCards.add(new EventCard("Мистический Сундук", "Анархия #102 (X: 890, Z: -1230)", "АКТИВЕН", "02:30", ColorUtil.rgba(40, 220, 100, 255)));
            eventCards.add(new EventCard("Караван с Наградой", "Анархия #105 (X: -1450, Z: 320)", "СПАВН ЧЕРЕЗ", "08:15", ColorUtil.rgba(255, 180, 40, 255)));
            eventCards.add(new EventCard("Андайн Босс", "Спавн Анархии #101", "ОЖИДАНИЕ", "45:00", ColorUtil.rgba(160, 160, 180, 255)));
        } else { // SpookyTime
            eventCards.add(new EventCard("Тайный Сундук", "Гриф #1 (X: 530, Z: -920)", "АКТИВЕН", "06:40", ColorUtil.rgba(40, 220, 100, 255)));
            eventCards.add(new EventCard("Сумеречный Босс", "Гриф #2 (X: -340, Z: 1480)", "СПАВН ЧЕРЕЗ", "15:20", ColorUtil.rgba(255, 180, 40, 255)));
        }

        for (EventCard card : eventCards) {
            float cardH = 34.0F;
            boolean hovered = mouseX >= startX && mouseX <= startX + totalWidth && mouseY >= currentY && mouseY <= currentY + cardH;

            int cardBg = hovered ? ColorUtil.rgba(255, 255, 255, (int) (16 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (8 * alpha));
            int cardOutline = hovered ? ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alpha));

            Render2D.drawRoundedRect(startX, currentY, totalWidth, cardH, 6.0F, cardBg);
            Render2D.drawRoundedOutline(startX, currentY, totalWidth, cardH, 6.0F, 1.0F, cardOutline);

            // Icon indicator
            Render2D.drawShadow(startX + 8.0F, currentY + 7.0F, 20.0F, 20.0F, 4.0F, 3.0F, ColorUtil.withAlpha(accent, (int) (100 * alpha)));
            Render2D.drawRoundedRect(startX + 8.0F, currentY + 7.0F, 20.0F, 20.0F, 5.0F, ColorUtil.multiplyAlpha(accent, 0.4F * alpha));
            Fonts.drawCenteredIcon(IconUse.COMPASS, startX + 18.0F, currentY + 12.0F, 10.0F, ColorUtil.rgba(240, 240, 255, (int) (240 * alpha)));

            // Event Title & Location
            Fonts.drawString(Fonts.SF_MEDIUM, card.name(), startX + 34.0F, currentY + 6.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, card.location(), startX + 34.0F, currentY + 18.0F, 6.5F, ColorUtil.rgba(170, 170, 190, (int) (200 * alpha)));

            // Status Badge & Timer
            float statusW = 85.0F;
            float statusX = startX + totalWidth - statusW - 8.0F;
            float statusY = currentY + 8.0F;

            Render2D.drawRoundedRect(statusX, statusY, statusW, 18.0F, 4.0F, ColorUtil.multiplyAlpha(card.statusColor(), 0.25F * alpha));
            Render2D.drawRoundedOutline(statusX, statusY, statusW, 18.0F, 4.0F, 1.0F, ColorUtil.multiplyAlpha(card.statusColor(), 0.7F * alpha));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, card.status() + "  " + card.timer(), statusX + statusW / 2.0F, statusY + 4.5F, 6.5F, 0xFFFFFFFF);

            currentY += cardH + 6.0F;
        }

        float totalContentH = currentY + state.getScrollOffset() - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private boolean handleEventsClick(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float currentY = startY - state.getScrollOffset();

        String[] serverTabs = new String[] {"HolyWorld", "FunTime", "SpookyTime"};
        float tabW = (totalWidth - 12.0F) / 3.0F;
        float tabH = 22.0F;

        for (int i = 0; i < serverTabs.length; i++) {
            float tx = startX + i * (tabW + 6.0F);
            float ty = currentY;
            if (mouseX >= tx && mouseX <= tx + tabW && mouseY >= ty && mouseY <= ty + tabH) {
                state.setEventServerTab(serverTabs[i]);
                return true;
            }
        }

        return false;
    }

    private void renderThemes(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float currentY = startY - state.getScrollOffset();
        int accent = Theme.getAccentColor();

        // Section 1: Presets
        Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Пресеты тем"), startX, currentY, 8.5F, ColorUtil.rgba(240, 240, 255, (int) (240 * alpha)));
        currentY += 14.0F;

        record ThemePreset(String name, int color) {}
        ThemePreset[] presets = new ThemePreset[] {
            new ThemePreset("Error DLC", ColorUtil.rgba(0, 160, 255, 255)),
            new ThemePreset("Debuda Violet", ColorUtil.rgba(139, 92, 246, 255)),
            new ThemePreset("Cyber Rose", ColorUtil.rgba(235, 75, 180, 255)),
            new ThemePreset("Emerald", ColorUtil.rgba(16, 185, 129, 255)),
            new ThemePreset("Sunset Amber", ColorUtil.rgba(245, 158, 11, 255)),
            new ThemePreset("Crimson Red", ColorUtil.rgba(239, 68, 68, 255)),
            new ThemePreset("Toxic Lime", ColorUtil.rgba(132, 204, 22, 255))
        };

        float presetW = (totalWidth - 12.0F) / 3.0F;
        float presetH = 24.0F;
        for (int i = 0; i < presets.length; i++) {
            float px = startX + (i % 3) * (presetW + 6.0F);
            float py = currentY + (i / 3) * (presetH + 6.0F);

            ThemePreset preset = presets[i];
            boolean selected = (accent == preset.color());
            boolean hovered = mouseX >= px && mouseX <= px + presetW && mouseY >= py && mouseY <= py + presetH;

            int bg = selected ? ColorUtil.rgba(ColorUtil.red(preset.color()), ColorUtil.green(preset.color()), ColorUtil.blue(preset.color()), (int) (60 * alpha))
                              : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (10 * alpha)));
            int border = selected ? preset.color() : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

            Render2D.drawRoundedRect(px, py, presetW, presetH, 5.0F, bg);
            Render2D.drawRoundedOutline(px, py, presetW, presetH, 5.0F, 1.0F, border);

            // Color circle dot
            Render2D.drawShadow(px + 7.0F, py + 7.0F, 10.0F, 10.0F, 4.0F, 3.0F, ColorUtil.withAlpha(preset.color(), (int) (150 * alpha)));
            Render2D.drawRoundedRect(px + 7.0F, py + 7.0F, 10.0F, 10.0F, 5.0F, preset.color());

            Fonts.drawString(Fonts.SF_MEDIUM, preset.name(), px + 22.0F, py + 7.0F, 7.0F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));
        }
        currentY += ((presets.length + 2) / 3) * (presetH + 6.0F) + 10.0F;

        // Section 2: Background Modes
        Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Режим фона (Background)"), startX, currentY, 8.5F, ColorUtil.rgba(240, 240, 255, (int) (240 * alpha)));
        currentY += 14.0F;

        String[] bgModes = new String[] {"Blur", "Dark Glass", "Translucent", "Solid"};
        float bgW = (totalWidth - 18.0F) / 4.0F;
        float bgH = 22.0F;
        for (int i = 0; i < bgModes.length; i++) {
            float bx = startX + i * (bgW + 6.0F);
            float by = currentY;
            String mode = bgModes[i];

            boolean selected = Theme.getBackgroundMode().equalsIgnoreCase(mode);
            boolean hovered = mouseX >= bx && mouseX <= bx + bgW && mouseY >= by && mouseY <= by + bgH;

            int bg = selected ? ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (70 * alpha))
                              : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (10 * alpha)));
            int border = selected ? accent : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

            Render2D.drawRoundedRect(bx, by, bgW, bgH, 5.0F, bg);
            Render2D.drawRoundedOutline(bx, by, bgW, bgH, 5.0F, 1.0F, border);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, mode, bx + bgW / 2.0F, by + 6.0F, 7.0F, selected ? 0xFFFFFFFF : ColorUtil.rgba(200, 200, 220, (int) (200 * alpha)));
        }
        currentY += bgH + 16.0F;

        // Section 3: Glass Styles
        Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Стиль стекла (Glass Style)"), startX, currentY, 8.5F, ColorUtil.rgba(240, 240, 255, (int) (240 * alpha)));
        currentY += 14.0F;

        String[] glassStyles = new String[] {"Liquid Glass", "Lumen Glow", "Flat Minimal"};
        float glassW = (totalWidth - 12.0F) / 3.0F;
        float glassH = 22.0F;
        for (int i = 0; i < glassStyles.length; i++) {
            float gx = startX + i * (glassW + 6.0F);
            float gy = currentY;
            String style = glassStyles[i];

            boolean selected = Theme.getGlassStyle().equalsIgnoreCase(style);
            boolean hovered = mouseX >= gx && mouseX <= gx + glassW && mouseY >= gy && mouseY <= gy + glassH;

            int bg = selected ? ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (70 * alpha))
                              : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (10 * alpha)));
            int border = selected ? accent : (hovered ? ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

            Render2D.drawRoundedRect(gx, gy, glassW, glassH, 5.0F, bg);
            Render2D.drawRoundedOutline(gx, gy, glassW, glassH, 5.0F, 1.0F, border);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, style, gx + glassW / 2.0F, gy + 6.0F, 7.0F, selected ? 0xFFFFFFFF : ColorUtil.rgba(200, 200, 220, (int) (200 * alpha)));
        }
        currentY += glassH + 16.0F;

        // Section 4: RGB Sliders
        Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Кастомный акцент (RGB Sliders)"), startX, currentY, 8.5F, ColorUtil.rgba(240, 240, 255, (int) (240 * alpha)));
        currentY += 14.0F;

        int r = ColorUtil.red(accent);
        int g = ColorUtil.green(accent);
        int b = ColorUtil.blue(accent);

        String[] channelLabels = new String[] {"Red: " + r, "Green: " + g, "Blue: " + b};
        int[] channelVals = new int[] {r, g, b};
        int[] channelColors = new int[] {ColorUtil.rgba(239, 68, 68, 255), ColorUtil.rgba(16, 185, 129, 255), ColorUtil.rgba(59, 130, 246, 255)};

        for (int i = 0; i < 3; i++) {
            float sliderY = currentY;
            float sliderH = 18.0F;
            float sliderW = totalWidth;

            Render2D.drawRoundedRect(startX, sliderY, sliderW, sliderH, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (8 * alpha)));
            Render2D.drawRoundedOutline(startX, sliderY, sliderW, sliderH, 4.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)));

            float fillW = (channelVals[i] / 255.0F) * sliderW;
            if (fillW > 0) {
                Render2D.drawRoundedRect(startX, sliderY, fillW, sliderH, 4.0F, ColorUtil.multiplyAlpha(channelColors[i], 0.45F * alpha));
            }

            Fonts.drawString(Fonts.SF_MEDIUM, channelLabels[i], startX + 8.0F, sliderY + 5.0F, 7.0F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));

            currentY += sliderH + 6.0F;
        }

        float totalContentH = currentY + state.getScrollOffset() - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private boolean handleThemesClick(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float currentY = startY - state.getScrollOffset();

        // Presets header offset
        currentY += 14.0F;

        record ThemePreset(String name, int color) {}
        ThemePreset[] presets = new ThemePreset[] {
            new ThemePreset("Error DLC", ColorUtil.rgba(0, 160, 255, 255)),
            new ThemePreset("Debuda Violet", ColorUtil.rgba(139, 92, 246, 255)),
            new ThemePreset("Cyber Rose", ColorUtil.rgba(235, 75, 180, 255)),
            new ThemePreset("Emerald", ColorUtil.rgba(16, 185, 129, 255)),
            new ThemePreset("Sunset Amber", ColorUtil.rgba(245, 158, 11, 255)),
            new ThemePreset("Crimson Red", ColorUtil.rgba(239, 68, 68, 255)),
            new ThemePreset("Toxic Lime", ColorUtil.rgba(132, 204, 22, 255))
        };

        float presetW = (totalWidth - 12.0F) / 3.0F;
        float presetH = 24.0F;
        for (int i = 0; i < presets.length; i++) {
            float px = startX + (i % 3) * (presetW + 6.0F);
            float py = currentY + (i / 3) * (presetH + 6.0F);

            if (mouseX >= px && mouseX <= px + presetW && mouseY >= py && mouseY <= py + presetH) {
                Theme.setAccentColor(presets[i].color());
                return true;
            }
        }
        currentY += ((presets.length + 2) / 3) * (presetH + 6.0F) + 10.0F;

        // Background modes
        currentY += 14.0F;
        String[] bgModes = new String[] {"Blur", "Dark Glass", "Translucent", "Solid"};
        float bgW = (totalWidth - 18.0F) / 4.0F;
        float bgH = 22.0F;
        for (int i = 0; i < bgModes.length; i++) {
            float bx = startX + i * (bgW + 6.0F);
            float by = currentY;
            if (mouseX >= bx && mouseX <= bx + bgW && mouseY >= by && mouseY <= by + bgH) {
                Theme.setBackgroundMode(bgModes[i]);
                return true;
            }
        }
        currentY += bgH + 16.0F;

        // Glass styles
        currentY += 14.0F;
        String[] glassStyles = new String[] {"Liquid Glass", "Lumen Glow", "Flat Minimal"};
        float glassW = (totalWidth - 12.0F) / 3.0F;
        float glassH = 22.0F;
        for (int i = 0; i < glassStyles.length; i++) {
            float gx = startX + i * (glassW + 6.0F);
            float gy = currentY;
            if (mouseX >= gx && mouseX <= gx + glassW && mouseY >= gy && mouseY <= gy + glassH) {
                Theme.setGlassStyle(glassStyles[i]);
                return true;
            }
        }
        currentY += glassH + 16.0F;

        // RGB Sliders
        currentY += 14.0F;
        int accent = Theme.getAccentColor();
        int r = ColorUtil.red(accent);
        int g = ColorUtil.green(accent);
        int b = ColorUtil.blue(accent);

        for (int i = 0; i < 3; i++) {
            float sliderY = currentY;
            float sliderH = 18.0F;
            float sliderW = totalWidth;

            if (mouseX >= startX && mouseX <= startX + sliderW && mouseY >= sliderY && mouseY <= sliderY + sliderH) {
                float val = Math.max(0.0F, Math.min(1.0F, (mouseX - startX) / sliderW));
                int newChan = Math.round(val * 255.0F);
                if (i == 0) r = newChan;
                else if (i == 1) g = newChan;
                else if (i == 2) b = newChan;

                Theme.setAccentColor(ColorUtil.rgba(r, g, b, 255));
                return true;
            }
            currentY += sliderH + 6.0F;
        }

        return false;
    }

    public boolean handleMouseButton(PanelLapState state, int mouseX, int mouseY, int button, int action) {
        if (action == GLFW.GLFW_RELEASE) {
            dragging = false;
            if (state.getActiveModal() != null) {
                state.getActiveModal().mouseReleased(mouseX, mouseY, button);
            }
            if (state.isSettingsPopupOpen()) {
                settingsPopup.mouseReleased(mouseX, mouseY, button);
            }
            for (Module module : Client.INSTANCE.moduleManager.getModules()) {
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    sr.mouseReleased(mouseX, mouseY, button);
                }
            }
            return false;
        }

        if (action != GLFW.GLFW_PRESS) return false;

        // 1. Modals have top priority
        if (state.getActiveModal() != null) {
            if (state.getActiveModal().mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        float x = state.getPanelX();
        float y = state.getPanelY();
        float w = state.getPanelWidth();
        float h = state.getPanelHeight();

        float sideW = 140.0F;
        float contentX = x + sideW;
        float contentW = 385.0F;

        // Theme Settings button and dropdown popup
        float themeBtnW = 68.0F;
        float themeBtnH = 17.0F;
        float themeBtnX = contentX + contentW - themeBtnW - 12.0F;
        float themeBtnY = y + 8.0F;
        float popupW = settingsPopup.getWidth();
        float popupX = themeBtnX + themeBtnW - popupW;
        float popupY = themeBtnY + 17.0F + 5.0F;

        if (state.isSettingsPopupOpen()) {
            if (settingsPopup.mouseClicked(popupX, popupY, mouseX, mouseY, button)) {
                return true;
            }
            if (mouseX >= themeBtnX && mouseX <= themeBtnX + themeBtnW && mouseY >= themeBtnY && mouseY <= themeBtnY + themeBtnH) {
                state.closeSettingsPopup();
                return true;
            }
            state.closeSettingsPopup();
        } else if (mouseX >= themeBtnX && mouseX <= themeBtnX + themeBtnW && mouseY >= themeBtnY && mouseY <= themeBtnY + themeBtnH) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                state.toggleSettingsPopup();
                return true;
            }
        }

        // 2. Category Clicks (Sidebar) - Hitbox precisely aligned with render
        float catY = y + 35.0F;
        for (Category category : Category.values()) {
            if (mouseX >= x + 8.0F && mouseX <= x + sideW - 8.0F && mouseY >= catY && mouseY <= catY + 19.0F) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    state.switchCategory(category);
                    state.setSearchFocused(false);
                    return true;
                }
            }
            catY += 23.0F;
        }

        // 3. Search Box Click
        float searchY = y + h - 50.0F;
        if (mouseX >= x + 8.0F && mouseX <= x + sideW - 8.0F && mouseY >= searchY && mouseY <= searchY + 18.0F) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                state.setSearchFocused(true);
                return true;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            state.setSearchFocused(false);
        }

        // 4. Panel Header Dragging
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean inSidebarHeader = mouseX >= x && mouseX <= x + sideW && mouseY >= y && mouseY <= y + 32.0F;
            boolean inContentHeader = mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= y && mouseY <= y + 30.0F;
            boolean onThemeBtn = mouseX >= themeBtnX && mouseX <= themeBtnX + themeBtnW && mouseY >= themeBtnY && mouseY <= themeBtnY + themeBtnH;
            if ((inSidebarHeader || inContentHeader) && !onThemeBtn) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
        }

        // 5. Module Toggles & Setting Interactions
        float contentY = y + 32.0F;
        float contentH = h - 40.0F;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            if (state.getCurrentCategory() == Category.CONFIGS) {
                return handleConfigsClick(state, contentX + 8.0F, contentY, contentW - 16.0F, contentH, mouseX, mouseY, button);
            } else if (state.getCurrentCategory() == Category.FRIENDS) {
                return handleFriendsClick(state, contentX + 8.0F, contentY, contentW - 16.0F, contentH, mouseX, mouseY, button);
            } else if (state.getCurrentCategory() == Category.THEMES) {
                return handleThemesClick(state, contentX + 8.0F, contentY, contentW - 16.0F, contentH, mouseX, mouseY, button);
            } else if (state.getCurrentCategory() == Category.EVENTS) {
                return handleEventsClick(state, contentX + 8.0F, contentY, contentW - 16.0F, contentH, mouseX, mouseY, button);
            }

            List<Module> list = getFilteredModules(state);
            float currentY = contentY - state.getScrollOffset();

            for (Module module : list) {
                float modY = currentY;
                float totalSetH = 0.0F;
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    sr.updateVisibility();
                    float sH = sr.getHeight();
                    if (sH > 0.1F) totalSetH += sH + 2.0F;
                }

                float cardHeight = 22.0F + (module.getExpandAnim().getValue() * (totalSetH + 4.0F));

                if (mouseY >= modY && mouseY <= modY + cardHeight) {
                    float colWidth = contentW - 16.0F;
                    float modX = contentX + 8.0F;
                    float toggleW = 20.0F;
                    float toggleX = modX + colWidth - toggleW - 8.0F;

                    boolean isBound = module.getBind().isBound();
                    float bindFontSz = 7.0F;
                    float bindPillH = 10.0F;
                    float bindPillW = isBound ? (Fonts.SF_MEDIUM.getWidth(module.getBind().getDisplayValue(), bindFontSz) + 8.0F) : 14.0F;
                    float bindPillX = toggleX - bindPillW - 6.0F;
                    float bindPillY = modY + 6.0F;

                    // Middle Click anywhere on card OR Click directly on Keyboard Icon -> Open Bind Modal!
                    boolean clickedPill = mouseX >= bindPillX && mouseX <= bindPillX + bindPillW && mouseY >= bindPillY && mouseY <= bindPillY + bindPillH;
                    if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || clickedPill) {
                        state.setActiveModal(new error.ui.mainmenu.popup.BindModal(module));
                        return true;
                    }

                    // Header click (first 22 pixels)
                    if (mouseY <= modY + 22.0F) {
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            module.toggle();
                            return true;
                        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                            module.setExpanded(!module.isExpanded());
                            return true;
                        }
                    } else if (module.getExpandAnim().getValue() > 0.05F) {
                        // Settings area click - dispatch directly to the module's setting renderers
                        for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                            sr.mouseClicked(mouseX, mouseY, button);
                        }
                        return true;
                    }
                }

                currentY += cardHeight + 4.0F;
            }
        }

        return false;
    }

    public boolean handleKey(PanelLapState state, int key, int scanCode, int modifiers) {
        if (state.getActiveModal() != null) {
            if (state.getActiveModal().keyPressed(key, scanCode, modifiers)) {
                return true;
            }
        }

        if (state.isSettingsPopupOpen()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                state.closeSettingsPopup();
                return true;
            }
        }

        if (state.isSearchFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                state.setSearchFocused(false);
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String query = state.getSearchQuery();
                if (!query.isEmpty()) {
                    state.setSearchQuery(query.substring(0, query.length() - 1));
                }
                return true;
            }
            return true;
        }

        if (state.isConfigInputFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !state.getConfigInput().trim().isEmpty()) {
                    Client.INSTANCE.configManager.saveConfig(state.getConfigInput().trim(), true);
                    state.setConfigInput("");
                }
                state.setConfigInputFocused(false);
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String input = state.getConfigInput();
                if (!input.isEmpty()) {
                    state.setConfigInput(input.substring(0, input.length() - 1));
                }
                return true;
            }
            return true;
        }

        if (state.isShareCodeInputFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !state.getShareCodeInput().trim().isEmpty()) {
                    Client.INSTANCE.configManager.loadShareCode(state.getShareCodeInput().trim(), true);
                    state.setShareCodeInput("");
                }
                state.setShareCodeInputFocused(false);
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String input = state.getShareCodeInput();
                if (!input.isEmpty()) {
                    state.setShareCodeInput(input.substring(0, input.length() - 1));
                }
                return true;
            }
            return true;
        }

        if (state.isFriendInputFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !state.getFriendInput().trim().isEmpty()) {
                    error.friend.FriendManager.getInstance().addFriend(state.getFriendInput().trim());
                    error.util.client.persiki.ChatUtil.success("Друг '" + state.getFriendInput().trim() + "' добавлен!");
                    state.setFriendInput("");
                }
                state.setFriendInputFocused(false);
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String input = state.getFriendInput();
                if (!input.isEmpty()) {
                    state.setFriendInput(input.substring(0, input.length() - 1));
                }
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            PanelRefractions.close(Minecraft.getInstance());
            return true;
        }

        for (Module module : Client.INSTANCE.moduleManager.getModules()) {
            if (module.isExpanded() || module.getExpandAnim().getValue() > 0.01F) {
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    sr.keyPressed(key, scanCode, modifiers);
                }
            }
        }

        return false;
    }

    public void handleChar(PanelLapState state, int codePoint) {
        if (state.getActiveModal() != null) {
            state.getActiveModal().charTyped(codePoint);
            return;
        }

        if (state.isSearchFocused()) {
            char c = (char) codePoint;
            if (c >= 32 && c != 127) {
                state.setSearchQuery(state.getSearchQuery() + c);
            }
            return;
        }

        if (state.isConfigInputFocused()) {
            char c = (char) codePoint;
            if (c >= 32 && c != 127) {
                state.setConfigInput(state.getConfigInput() + c);
            }
            return;
        }

        if (state.isShareCodeInputFocused()) {
            char c = (char) codePoint;
            if (c >= 32 && c != 127) {
                state.setShareCodeInput(state.getShareCodeInput() + c);
            }
            return;
        }

        if (state.isFriendInputFocused()) {
            char c = (char) codePoint;
            if (c >= 32 && c != 127) {
                state.setFriendInput(state.getFriendInput() + c);
            }
            return;
        }

        for (Module module : Client.INSTANCE.moduleManager.getModules()) {
            if (module.isExpanded() || module.getExpandAnim().getValue() > 0.01F) {
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    sr.charTyped(codePoint);
                }
            }
        }
    }

    public void handleScroll(PanelLapState state, double vertical, int mouseX, int mouseY) {
        state.scroll((float) (vertical * 22.0F));
    }
}