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
        ClickGui clickGui = Client.INSTANCE.moduleManager.getClickGui();
        boolean isHoldDown = clickGui != null && clickGui.isHoldKeyPressed(mc.getWindow().handle());
        state.setHoldActive(isHoldDown);

        state.update();
        float openProgress = state.getOpenAnimation().getValue();
        if (openProgress <= 0.001F) return;

        if (!state.isPositionInitialized()) {
            state.setPanelX((screenWidth - state.getPanelWidth()) / 2.0F);
            state.setPanelY((screenHeight - state.getPanelHeight()) / 2.0F);
            state.setPositionInitialized(true);
        }

        if (dragging && !state.isHoldActive()) {
            state.setPanelX(mouseX - dragOffsetX);
            state.setPanelY(mouseY - dragOffsetY);
        }

        float x = state.getPanelX();
        float y = state.getPanelY();
        float w = state.getPanelWidth();
        float h = state.getPanelHeight();

        float centerX = x + (w / 2.0F);
        float centerY = y + (h / 2.0F);
        float scale = 0.94F + (0.06F * openProgress);

        extractor.pose().pushMatrix();
        extractor.pose().translate(centerX, centerY);
        extractor.pose().scale(scale, scale);
        extractor.pose().translate(-centerX, -centerY);

        float holdProgress = state.getHoldAnim().getValue();
        float mainGuiAlpha = openProgress * (1.0F - holdProgress);

        if (mainGuiAlpha > 0.01F) {
            // Fullscreen Background Spotlight Beam & Dot Mesh Atmosphere (Matching Photo 1:1)
            int ambientBg = ColorUtil.rgba(7, 6, 11, (int) (210 * mainGuiAlpha));
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ambientBg);

            // Top-Right Purple Spotlight Beam Accent
            int spotlightCol = ColorUtil.rgba(195, 125, 245, (int) (65 * mainGuiAlpha));
            Render2D.drawRoundedRect(screenWidth * 0.5F, -50.0F, screenWidth * 0.6F, 220.0F, 100.0F, spotlightCol);

            // Glowing Neon Purple Connector Laser Trace Line around bottom of panels
            float traceY = y + h + 10.0F;
            int laserCol = ColorUtil.rgba(228, 142, 216, (int) (220 * mainGuiAlpha));
            int laserGlow = ColorUtil.rgba(228, 142, 216, (int) (90 * mainGuiAlpha));

            Render2D.drawShadow(x - 10.0F, traceY - 1.0F, w + 20.0F, 3.0F, 3.0F, 6.0F, laserGlow);
            Render2D.drawRoundedRect(x - 10.0F, traceY, w + 20.0F, 2.0F, 1.0F, laserCol);
            Render2D.drawRoundedRect(x - 12.0F, traceY - 1.0F, 4.0F, 4.0F, 2.0F, ColorUtil.rgba(232, 163, 222, (int) (255 * mainGuiAlpha)));
            Render2D.drawRoundedRect(x + w + 8.0F, traceY - 1.0F, 4.0F, 4.0F, 2.0F, ColorUtil.rgba(228, 142, 216, (int) (255 * mainGuiAlpha)));

            renderDescriptionAboveGui(state, centerX, y - 18.0F, mainGuiAlpha);

            // 1. NARROW LEFT SIDEBAR PANEL (sideW = 145.0F)
            float sideW = 145.0F;
            int cardBg = ColorUtil.rgba(20, 16, 28, (int) (185 * mainGuiAlpha));
            int shadowColor = ColorUtil.rgba(0, 0, 0, (int) (170 * mainGuiAlpha));
            int topGloss = ColorUtil.rgba(255, 255, 255, (int) (20 * mainGuiAlpha));
            int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (35 * mainGuiAlpha));

            Render2D.drawShadow(x, y, sideW, h, 12.0F, 10.0F, shadowColor);
            Render2D.drawRoundedRect(x, y, sideW, h, 12.0F, cardBg);
            Render2D.drawRoundedRect(x, y, sideW, h * 0.40F, 12.0F, topGloss);
            Render2D.drawRoundedOutline(x, y, sideW, h, 12.0F, 1.0F, glassBorder);

            // Top Title & Version Dropdown ("Rose 1.21.11 ▾")
            float dropdownY = y + 8.0F;
            Render2D.drawRoundedRect(x + 8.0F, dropdownY, sideW - 16.0F, 20.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (15 * mainGuiAlpha)));
            Render2D.drawRoundedOutline(x + 8.0F, dropdownY, sideW - 16.0F, 20.0F, 6.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * mainGuiAlpha)));

            int activePillCol = ColorUtil.rgba(235, 135, 215, (int) (110 * mainGuiAlpha));
            Render2D.drawRoundedRect(x + 12.0F, dropdownY + 3.0F, 14.0F, 14.0F, 4.0F, activePillCol);
            Fonts.drawString(Fonts.SF_MEDIUM, "R", x + 16.5F, dropdownY + 5.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * mainGuiAlpha)));

            Fonts.drawString(Fonts.SF_MEDIUM, "Rose", x + 31.0F, dropdownY + 5.5F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "1.21.11 ▾", x + sideW - 44.0F, dropdownY + 6.0F, 7.5F, ColorUtil.rgba(180, 180, 200, (int) (180 * mainGuiAlpha)));

            // Categories List
            float catY = y + 36.0F;
            for (Category category : Category.values()) {
                boolean active = state.getCurrentTab() == PanelLapState.Tab.CATEGORY && category == state.getCurrentCategory();

                if (category == Category.COMBAT) {
                    Fonts.drawString(Fonts.SF_MEDIUM, "COMBAT", x + 12.0F, catY, 7.0F, ColorUtil.rgba(160, 150, 180, (int) (140 * mainGuiAlpha)));
                    Fonts.drawString(Fonts.SF_MEDIUM, "v 35", x + sideW - 32.0F, catY, 7.0F, ColorUtil.rgba(160, 150, 180, (int) (140 * mainGuiAlpha)));
                    catY += 10.0F;
                } else if (category == Category.RENDER) {
                    Fonts.drawString(Fonts.SF_MEDIUM, "VISUAL", x + 12.0F, catY, 7.0F, ColorUtil.rgba(160, 150, 180, (int) (140 * mainGuiAlpha)));
                    Fonts.drawString(Fonts.SF_MEDIUM, "v 46", x + sideW - 32.0F, catY, 7.0F, ColorUtil.rgba(160, 150, 180, (int) (140 * mainGuiAlpha)));
                    catY += 10.0F;
                } else if (category == Category.MISC) {
                    Fonts.drawString(Fonts.SF_MEDIUM, "OTHER", x + 12.0F, catY, 7.0F, ColorUtil.rgba(160, 150, 180, (int) (140 * mainGuiAlpha)));
                    Fonts.drawString(Fonts.SF_MEDIUM, "v 20", x + sideW - 32.0F, catY, 7.0F, ColorUtil.rgba(160, 150, 180, (int) (140 * mainGuiAlpha)));
                    catY += 10.0F;
                }

                if (active) {
                    Render2D.drawShadow(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, 6.0F, ColorUtil.rgba(230, 120, 210, (int) (100 * mainGuiAlpha)));
                    Render2D.drawRoundedRect(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, activePillCol);
                    Render2D.drawRoundedOutline(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, 1.0F, ColorUtil.rgba(250, 160, 235, (int) (190 * mainGuiAlpha)));
                } else {
                    Render2D.drawRoundedRect(x + 8.0F, catY, sideW - 16.0F, 19.0F, 6.0F, ColorUtil.rgba(46, 38, 60, (int) (120 * mainGuiAlpha)));
                }

                int col = active ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(200, 195, 215, 200);
                Fonts.drawIcon(getCategoryIcon(category), x + 14.0F, catY + 4.5F, 9.5F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                Fonts.drawString(Fonts.SF_MEDIUM, Localization.get(category.getDisplayName()), x + 28.0F, catY + 5.0F, 8.5F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                catY += 23.0F;
            }

            // Bottom Search Input
            float searchY = y + h - 50.0F;
            Render2D.drawRoundedRect(x + 8.0F, searchY, sideW - 16.0F, 18.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (15 * mainGuiAlpha)));
            Render2D.drawRoundedOutline(x + 8.0F, searchY, sideW - 16.0F, 18.0F, 6.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "🔍 Search", x + 14.0F, searchY + 4.5F, 7.5F, ColorUtil.rgba(180, 180, 200, (int) (160 * mainGuiAlpha)));

            // User Profile Footer ("Zodiac BETA")
            float userY = y + h - 28.0F;
            String curUser = Minecraft.getInstance().getUser().getName();
            Render2D.drawRoundedRect(x + 8.0F, userY, sideW - 16.0F, 20.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (15 * mainGuiAlpha)));

            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + curUser).getBytes(StandardCharsets.UTF_8));
            Identifier skinTexture = DefaultPlayerSkin.get(uuid).body().texturePath();
            Render2D.drawHead(skinTexture, x + 12.0F, userY + 3.0F, 14.0F, 2.0F, mainGuiAlpha);

            Fonts.drawString(Fonts.SF_MEDIUM, curUser, x + 30.0F, userY + 5.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * mainGuiAlpha)));
            Render2D.drawRoundedRect(x + sideW - 36.0F, userY + 4.5F, 24.0F, 11.0F, 3.0F, ColorUtil.rgba(180, 120, 240, (int) (140 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "BETA", x + sideW - 34.0F, userY + 6.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * mainGuiAlpha)));

            // 2. EXTRA-WIDE RIGHT MAIN CONTENT PANEL (contentW = 378.0F)
            float contentX = x + sideW + 12.0F;
            float contentW = 378.0F;

            Render2D.drawShadow(contentX, y, contentW, h, 12.0F, 10.0F, shadowColor);
            Render2D.drawRoundedRect(contentX, y, contentW, h, 12.0F, cardBg);
            Render2D.drawRoundedRect(contentX, y, contentW, h * 0.40F, 12.0F, topGloss);
            Render2D.drawRoundedOutline(contentX, y, contentW, h, 12.0F, 1.0F, glassBorder);

            // Top Breadcrumb & Config Dropdown
            float breadY = y + 8.0F;
            Fonts.drawString(Fonts.SF_MEDIUM, "Rose  /  Categories  /  " + state.getCurrentCategory().getDisplayName(), contentX + 12.0F, breadY + 3.0F, 8.0F, ColorUtil.rgba(200, 195, 215, (int) (180 * mainGuiAlpha)));

            Render2D.drawRoundedRect(contentX + contentW - 70.0F, breadY, 58.0F, 17.0F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (15 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "Custom v", contentX + contentW - 62.0F, breadY + 3.5F, 7.5F, ColorUtil.rgba(240, 240, 255, (int) (220 * mainGuiAlpha)));

            // Sub-mode Pills Bar
            float subY = y + 30.0F;
            Render2D.drawRoundedRect(contentX + 12.0F, subY, 110.0F, 17.0F, 5.0F, ColorUtil.rgba(46, 38, 60, (int) (120 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "Коррекция движения", contentX + 16.0F, subY + 3.5F, 7.0F, ColorUtil.rgba(200, 195, 215, (int) (180 * mainGuiAlpha)));

            Render2D.drawRoundedRect(contentX + 128.0F, subY, 95.0F, 17.0F, 5.0F, activePillCol);
            Render2D.drawRoundedOutline(contentX + 128.0F, subY, 95.0F, 17.0F, 5.0F, 1.0F, ColorUtil.rgba(250, 160, 235, (int) (190 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "Сфокусированная", contentX + 133.0F, subY + 3.5F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * mainGuiAlpha)));

            Render2D.drawRoundedRect(contentX + 228.0F, subY, 70.0F, 17.0F, 5.0F, ColorUtil.rgba(46, 38, 60, (int) (120 * mainGuiAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "Свободная", contentX + 234.0F, subY + 3.5F, 7.0F, ColorUtil.rgba(200, 195, 215, (int) (180 * mainGuiAlpha)));

            renderSearchAndHintsUnderGui(state, centerX, y + h + 16.0F, mainGuiAlpha);
        }

        // Inner Modules Scroll Area
        float sideW = 145.0F;
        float contentX = x + sideW + 12.0F;
        float contentY = y + 54.0F;
        float contentW = 378.0F;
        float contentH = h - 64.0F;

        float catProgress = state.getCategoryAnim().getValue();
        float contentAlpha = openProgress * catProgress;
        float slideY = (1.0F - catProgress) * (state.getCategoryDirection() * 12.0F);

        state.setHoveredModule(null);

        Render2D.pushScissor(contentX + 4.0F, contentY, contentW - 8.0F, contentH);
        renderModules(state, contentX + 8.0F, contentY + slideY, contentW - 16.0F, contentH, mouseX, mouseY, contentAlpha);
        Render2D.popScissor();

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
        String hint = Localization.get("Hold Left Alt to inspect");
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, hint, centerX, startY, 7.5F, ColorUtil.multiplyAlpha(ColorUtil.rgba(180, 180, 200, 200), alpha * 0.85F));
    }

    private void renderModules(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        List<Module> list = getFilteredModules(state);
        float colWidth = totalWidth;
        float currentScroll = state.getScrollOffset();
        float currentY = startY - currentScroll;

        int activeCardBg = ColorUtil.rgba(16, 14, 24, (int) (180 * alpha));
        int inactiveCardBg = ColorUtil.rgba(12, 10, 18, (int) (140 * alpha));

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

            int cardBg = module.isState() ? activeCardBg : inactiveCardBg;

            // Sub-box Card Backdrop
            Render2D.drawRoundedRect(modX, modY, colWidth, cardHeight, 6.0F, ColorUtil.multiplyAlpha(cardBg, alpha));
            Render2D.drawRoundedOutline(modX, modY, colWidth, cardHeight, 6.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

            // Module Title
            int textCol = module.isState() ? ColorUtil.WHITE : ColorUtil.rgba(200, 195, 215, 200);
            Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), modX + 8.0F, modY + 5.5F, 8.5F, ColorUtil.multiplyAlpha(textCol, alpha));

            // Module State Toggle Pill
            float toggleW = 20.0F;
            float toggleH = 10.0F;
            float toggleX = modX + colWidth - toggleW - 8.0F;
            float toggleY = modY + 6.0F;

            int toggleBg = module.isState() ? ColorUtil.rgba(225, 135, 215, (int) (200 * alpha)) : ColorUtil.rgba(45, 40, 60, (int) (140 * alpha));
            Render2D.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, 5.0F, toggleBg);
            float knobX = module.isState() ? toggleX + toggleW - 8.0F : toggleX + 2.0F;
            Render2D.drawRoundedRect(knobX, toggleY + 1.5F, 7.0F, 7.0F, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

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
        if (action != GLFW.GLFW_PRESS) return false;

        float x = state.getPanelX();
        float y = state.getPanelY();
        float w = state.getPanelWidth();
        float h = state.getPanelHeight();

        float sideW = 145.0F;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Category Clicks
            float catY = y + 46.0F;
            for (Category category : Category.values()) {
                if (mouseX >= x + 8.0F && mouseX <= x + sideW - 8.0F && mouseY >= catY && mouseY <= catY + 19.0F) {
                    state.switchCategory(category);
                    return true;
                }
                catY += 23.0F;
                if (category == Category.COMBAT || category == Category.RENDER || category == Category.MISC) catY += 10.0F;
            }

            // Panel Dragging
            if (mouseY >= y && mouseY <= y + 30.0F && mouseX >= x && mouseX <= x + w) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
        }

        // Module Toggles & Expand Clicks
        float contentX = x + sideW + 12.0F;
        float contentY = y + 54.0F;
        float contentW = 378.0F;
        float contentH = h - 64.0F;

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
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        module.toggle();
                        return true;
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        module.setExpanded(!module.isExpanded());
                        return true;
                    }
                }

                currentY += cardHeight + 4.0F;
            }
        }

        if (action == GLFW.GLFW_RELEASE) {
            dragging = false;
        }

        return false;
    }

    public boolean handleKey(PanelLapState state, int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            PanelRefractions.close(Minecraft.getInstance());
            return true;
        }
        return false;
    }

    public void handleChar(PanelLapState state, int codePoint) {
    }

    public void handleScroll(PanelLapState state, double vertical, int mouseX, int mouseY) {
        state.scroll((float) (-vertical * 18.0F));
    }
}