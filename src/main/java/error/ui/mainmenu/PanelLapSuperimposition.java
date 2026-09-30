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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PanelLapSuperimposition {
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

        float easeProgress = 1.0F - (float) Math.pow(1.0F - openProgress, 3);
        float centerX = x + (w / 2.0F);
        float centerY = y + (h / 2.0F);
        float scale = 0.88F + (0.12F * easeProgress);

        extractor.pose().pushMatrix();
        extractor.pose().translate(centerX, centerY);
        extractor.pose().scale(scale, scale);
        extractor.pose().translate(-centerX, -centerY);

        float mainGuiAlpha = easeProgress;

        if (mainGuiAlpha > 0.01F) {
            // Ambient backdrop matching Theme backgroundMode setting ("Blur" vs "None")
            boolean isBlur = Theme.getBackgroundMode().equalsIgnoreCase("Blur");
            if (isBlur) {
                int ambientBg = ColorUtil.rgba(10, 8, 16, (int) (130 * mainGuiAlpha));
                Render2D.drawRect(0, 0, screenWidth, screenHeight, ambientBg);
                Render2D.drawBlur(0, 0, screenWidth, screenHeight, 0.0F, 16.0F, ColorUtil.rgba(0, 0, 0, 80), mainGuiAlpha);
            } else {
                int ambientBg = ColorUtil.rgba(10, 8, 16, (int) (65 * mainGuiAlpha));
                Render2D.drawRect(0, 0, screenWidth, screenHeight, ambientBg);
            }

            // Top-Right Purple Spotlight Beam Accent
            int spotlightCol = ColorUtil.rgba(195, 125, 245, (int) (40 * mainGuiAlpha));
            Render2D.drawRoundedRect(screenWidth * 0.55F, -60.0F, screenWidth * 0.5F, 220.0F, 100.0F, spotlightCol);

            // Ambient Floating Rings in background
            int ringCol = ColorUtil.rgba(215, 170, 245, (int) (35 * mainGuiAlpha));
            Render2D.drawRoundedOutline(screenWidth * 0.22F, screenHeight * 0.75F, 18.0F, 18.0F, 9.0F, 1.0F, ringCol);
            Render2D.drawRoundedOutline(screenWidth * 0.76F, screenHeight * 0.22F, 14.0F, 14.0F, 7.0F, 1.0F, ringCol);
            Render2D.drawRoundedOutline(screenWidth * 0.82F, screenHeight * 0.72F, 22.0F, 22.0F, 11.0F, 1.0F, ringCol);

            float sideW = 145.0F;
            float contentX = x + sideW + 12.0F;
            float contentW = 380.0F;

            int themeAccent = Theme.getAccentColor();
            int laserCol = ColorUtil.multiplyAlpha(themeAccent, mainGuiAlpha);
            int laserGlow = ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), (int) (110 * mainGuiAlpha));

            renderDescriptionAboveGui(state, centerX, y - 18.0F, mainGuiAlpha);

            // 1. NARROW LEFT SIDEBAR PANEL (sideW = 145.0F, Authentic Frosted Liquid Glass)
            int liquidGlassFill = ColorUtil.rgba(45, 38, 54, (int) (115 * mainGuiAlpha));
            int shadowColor = ColorUtil.rgba(0, 0, 0, (int) (160 * mainGuiAlpha));
            int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (65 * mainGuiAlpha));
            int glassHalo = ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), (int) (25 * mainGuiAlpha));

            // Real Liquid Glass Blur + Translucent Fill + Glass Border + Glass Halo
            Render2D.drawShadow(x, y, sideW, h, 14.0F, 12.0F, shadowColor);
            Render2D.drawShadow(x, y, sideW, h, 14.0F, 6.0F, glassHalo);
            Render2D.drawBlur(x, y, sideW, h, 14.0F, 22.0F, liquidGlassFill, mainGuiAlpha);
            Render2D.drawRoundedRect(x, y, sideW, h, 14.0F, liquidGlassFill);
            Render2D.drawRoundedOutline(x, y, sideW, h, 14.0F, 1.0F, glassBorder);

            // Top Header: Error DLC 26.2 Branding & Client Logo
            float dropdownY = y + 8.0F;
            float brandW = sideW - 16.0F;
            int pillGlass = ColorUtil.rgba(65, 58, 80, (int) (115 * mainGuiAlpha));
            int pillBorder = ColorUtil.rgba(255, 255, 255, (int) (25 * mainGuiAlpha));

            Render2D.drawRoundedRect(x + 8.0F, dropdownY, brandW, 19.0F, 6.0F, pillGlass);
            Render2D.drawRoundedOutline(x + 8.0F, dropdownY, brandW, 19.0F, 6.0F, 1.0F, pillBorder);

            // Glowing Client Logo & Client Name
            Fonts.drawIcon(IconUse.LOGO, x + 14.0F, dropdownY + 4.5F, 10.0F, laserCol);
            Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC 26.2", x + 28.0F, dropdownY + 5.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * mainGuiAlpha)));

            // Categories List
            float catY = y + 35.0F;
            for (Category category : Category.values()) {
                boolean active = state.getCurrentTab() == PanelLapState.Tab.CATEGORY && category == state.getCurrentCategory();

                if (active) {
                    // Active button with Vertical Pink Accent Bar on Left Edge (Matching media_1790804005720.png 1:1)
                    Render2D.drawRoundedRect(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, ColorUtil.rgba(75, 68, 96, (int) (125 * mainGuiAlpha)));
                    Render2D.drawRoundedOutline(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * mainGuiAlpha)));
                    Render2D.drawRoundedRect(x + 10.0F, catY + 3.0F, 3.0F, 13.0F, 1.5F, laserCol);
                    Render2D.drawShadow(x + 10.0F, catY + 3.0F, 3.0F, 13.0F, 1.5F, 4.0F, laserGlow);
                }

                int col = active ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(200, 195, 215, 180);
                Fonts.drawIcon(getCategoryIcon(category), x + 18.0F, catY + 4.5F, 9.5F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                Fonts.drawString(Fonts.SF_MEDIUM, Localization.get(category.getDisplayName()), x + 32.0F, catY + 5.0F, 8.5F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                catY += 23.0F;
            }

            // Bottom Search Input with Pink Glowing Dot on Right Side
            float searchY = y + h - 50.0F;
            int curSearchBorder = state.isSearchFocused() ? laserCol : pillBorder;
            Render2D.drawRoundedRect(x + 8.0F, searchY, sideW - 16.0F, 18.0F, 6.0F, pillGlass);
            Render2D.drawRoundedOutline(x + 8.0F, searchY, sideW - 16.0F, 18.0F, 6.0F, 1.0F, curSearchBorder);

            String searchDisplay;
            if (state.getSearchQuery().isEmpty()) {
                searchDisplay = state.isSearchFocused() ? "⌕  |" : "⌕  Search...";
            } else {
                boolean cursorBlink = state.isSearchFocused() && (System.currentTimeMillis() % 1000 > 500);
                searchDisplay = "⌕  " + state.getSearchQuery() + (cursorBlink ? "|" : "");
            }
            int searchTextColor = state.isSearchFocused() || !state.getSearchQuery().isEmpty()
                    ? ColorUtil.rgba(255, 255, 255, (int) (240 * mainGuiAlpha))
                    : ColorUtil.rgba(180, 180, 200, (int) (160 * mainGuiAlpha));
            Fonts.drawString(Fonts.SF_MEDIUM, searchDisplay, x + 14.0F, searchY + 4.5F, 7.5F, searchTextColor);

            // Accent Glowing Dot on Right Side of Search Bar
            Render2D.drawRoundedRect(x + sideW - 18.0F, searchY + 5.0F, 6.0F, 8.0F, 4.0F, laserCol);

            // User Profile Footer with Circular Avatar & Glowing Ring ("Zodiac BETA" - No surrounding card!)
            float userY = y + h - 28.0F;
            String curUser = Minecraft.getInstance().getUser().getName();

            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + curUser).getBytes(StandardCharsets.UTF_8));
            Identifier skinTexture = DefaultPlayerSkin.get(uuid).body().texturePath();

            // Circular Avatar with Glowing Ring
            Render2D.drawShadow(x + 10.0F, userY + 1.0F, 18.0F, 18.0F, 9.0F, 6.0F, laserGlow);
            Render2D.drawHead(skinTexture, x + 10.0F, userY + 1.0F, 18.0F, 9.0F, mainGuiAlpha);
            Render2D.drawRoundedOutline(x + 10.0F, userY + 1.0F, 18.0F, 18.0F, 9.0F, 1.0F, laserCol);

            Fonts.drawString(Fonts.SF_MEDIUM, curUser, x + 34.0F, userY + 5.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * mainGuiAlpha)));
            Render2D.drawRoundedRect(x + sideW - 36.0F, userY + 4.5F, 24.0F, 11.0F, 3.0F, ColorUtil.rgba(140, 80, 180, (int) (140 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "BETA", x + sideW - 34.0F, userY + 6.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * mainGuiAlpha)));

            // 2. EXTRA-WIDE RIGHT MAIN CONTENT PANEL (contentW = 380.0F, Authentic Frosted Liquid Glass)

            // Liquid Glass Blur + Translucent Fill + Specular Gloss + Glass Border + Glass Halo
            Render2D.drawShadow(contentX, y, contentW, h, 14.0F, 12.0F, shadowColor);
            Render2D.drawShadow(contentX, y, contentW, h, 14.0F, 6.0F, glassHalo);
            Render2D.drawBlur(contentX, y, contentW, h, 14.0F, 22.0F, liquidGlassFill, mainGuiAlpha);
            Render2D.drawRoundedRect(contentX, y, contentW, h, 14.0F, liquidGlassFill);
            Render2D.drawRoundedOutline(contentX, y, contentW, h, 14.0F, 1.0F, glassBorder);

            // Top Breadcrumb & Theme Settings Dropdown Button
            float breadY = y + 8.0F;
            Fonts.drawString(Fonts.SF_MEDIUM, "Error  /  " + Localization.get(state.getCurrentCategory().getDisplayName()), contentX + 14.0F, breadY + 4.0F, 8.0F, ColorUtil.rgba(200, 195, 215, (int) (180 * mainGuiAlpha)));

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
        float sideW = 145.0F;
        float contentX = x + sideW + 12.0F;
        float contentY = y + 32.0F;
        float contentW = 380.0F;
        float contentH = h - 40.0F;

        float catProgress = state.getCategoryAnim().getValue();
        float contentAlpha = easeProgress * catProgress;
        float slideY = (1.0F - catProgress) * (state.getCategoryDirection() * 12.0F);

        state.setHoveredModule(null);

        Render2D.pushScissor(contentX + 4.0F, contentY, contentW - 8.0F, contentH);
        renderModules(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
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
            String bindText = isBound ? "⌨ " + module.getBind().getDisplayValue() : "⌨";
            float bindFontSz = 7.0F;
            float bindTextW = Fonts.SF_MEDIUM.getWidth(bindText, bindFontSz);
            float bindPillW = bindTextW + 8.0F;
            float bindPillH = 10.0F;
            float bindPillX = toggleX - bindPillW - 6.0F;
            float bindPillY = modY + 6.0F;

            boolean bindHovered = hovered && mouseX >= bindPillX && mouseX <= bindPillX + bindPillW && mouseY >= bindPillY && mouseY <= bindPillY + bindPillH;
            int accent = Theme.getAccentColor();
            int bindBg = bindHovered ? ColorUtil.multiplyAlpha(accent, 0.40F * alpha)
                    : (isBound ? ColorUtil.multiplyAlpha(accent, 0.22F * alpha) : ColorUtil.rgba(255, 255, 255, (int) (14 * alpha)));
            int bindBorder = bindHovered || isBound ? ColorUtil.multiplyAlpha(accent, alpha) : ColorUtil.rgba(255, 255, 255, (int) (25 * alpha));

            Render2D.drawRoundedRect(bindPillX, bindPillY, bindPillW, bindPillH, 3.5F, bindBg);
            Render2D.drawRoundedOutline(bindPillX, bindPillY, bindPillW, bindPillH, 3.5F, 1.0F, bindBorder);
            Fonts.drawString(Fonts.SF_MEDIUM, bindText, bindPillX + 4.0F, bindPillY + 1.5F, bindFontSz, ColorUtil.rgba(255, 255, 255, (int) ((bindHovered ? 255 : (isBound ? 230 : 160)) * alpha)));

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

    private IconUse getCategoryIcon(Category category) {
        return switch (category) {
            case COMBAT -> IconUse.FIGHT;
            case MOVEMENT -> IconUse.MOVEMENT;
            case PLAYER -> IconUse.PLAYER;
            case RENDER -> IconUse.RENDER;
            case MISC -> IconUse.MISC;
        };
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

        float sideW = 145.0F;
        float contentX = x + sideW + 12.0F;
        float contentW = 380.0F;

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
                    String bindText = isBound ? "⌨ " + module.getBind().getDisplayValue() : "⌨";
                    float bindFontSz = 7.0F;
                    float bindTextW = Fonts.SF_MEDIUM.getWidth(bindText, bindFontSz);
                    float bindPillW = bindTextW + 8.0F;
                    float bindPillH = 10.0F;
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