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
import error.util.client.persiki.KeyUtil;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by daun kvass
 */
public final class PanelLapSuperimposition {
    private boolean dragging;
    private float dragOffsetX, dragOffsetY;
    private final SettingsPopup settingsPopup = new SettingsPopup();

    private static final int CARD_BG_INACTIVE = 0xFF111218;
    private static final int CARD_BG_ACTIVE_BASE = 0xFF181A26;

    private long lastAccountClickTime = 0;
    private String lastClickedAccount = "";

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
            renderDescriptionAboveGui(state, centerX, y - 18.0F, mainGuiAlpha);

            int accent = Theme.getAccentColor();
            float panelAlpha = 0.65F * mainGuiAlpha;

            int col1 = ColorUtil.multiplyAlpha(ColorUtil.lerp(0xFF141620, accent, 0.14F), panelAlpha);
            int col2 = ColorUtil.multiplyAlpha(0xFF0B0C10, panelAlpha);

            if (Theme.getBackgroundMode().equalsIgnoreCase("Blur")) {
                Render2D.drawBlur(x, y, w, h, 10.0F, col2, mainGuiAlpha);
            }
            Render2D.drawGradientRound(x, y, w, h, 8.0F, col1, col1, col2, col2);

            float sideW = 85.0F;
            int dividerCol = ColorUtil.multiplyAlpha(0xFFFFFFFF, 0.04F * mainGuiAlpha);
            Render2D.drawRect(x + sideW, y + 10, 1.0F, h - 20, dividerCol);

            float catY = y + 14.0F;
            for (Category category : Category.values()) {
                boolean active = state.getCurrentTab() == PanelLapState.Tab.CATEGORY && category == state.getCurrentCategory();
                if (active) {
                    Render2D.drawRoundedRect(x + 5, catY, sideW - 10, 20, 3.5F, ColorUtil.multiplyAlpha(Theme.getAccentWithAlpha(40), mainGuiAlpha));
                }
                int col = active ? Theme.getAccentColor() : Theme.TEXT_MUTED;
                Fonts.drawIcon(getCategoryIcon(category), x + 10, catY + 4.5F, 10.5F, ColorUtil.multiplyAlpha(col, mainGuiAlpha));
                Fonts.drawString(Fonts.SF_MEDIUM, Localization.get(category.getDisplayName()), x + 25, catY + 5.0F, 10.5F, ColorUtil.multiplyAlpha(active ? Theme.TEXT_MAIN : Theme.TEXT_MUTED, mainGuiAlpha));
                catY += 23.0F;
            }

            boolean bindsActive = state.getCurrentTab() == PanelLapState.Tab.BINDS;
            if (bindsActive) {
                Render2D.drawRoundedRect(x + 5, catY, sideW - 10, 20, 3.5F, ColorUtil.multiplyAlpha(Theme.getAccentWithAlpha(40), mainGuiAlpha));
            }
            Fonts.drawIcon(IconUse.INFO, x + 10, catY + 4.5F, 10.5F, ColorUtil.multiplyAlpha(bindsActive ? Theme.getAccentColor() : Theme.TEXT_MUTED, mainGuiAlpha));
            Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Binds"), x + 25, catY + 5.0F, 10.5F, ColorUtil.multiplyAlpha(bindsActive ? Theme.TEXT_MAIN : Theme.TEXT_MUTED, mainGuiAlpha));
            catY += 23.0F;

            boolean accountsActive = state.getCurrentTab() == PanelLapState.Tab.ACCOUNTS;
            if (accountsActive) {
                Render2D.drawRoundedRect(x + 5, catY, sideW - 10, 20, 3.5F, ColorUtil.multiplyAlpha(Theme.getAccentWithAlpha(40), mainGuiAlpha));
            }
            Fonts.drawIcon(IconUse.PLAYER, x + 10, catY + 4.5F, 10.5F, ColorUtil.multiplyAlpha(accountsActive ? Theme.getAccentColor() : Theme.TEXT_MUTED, mainGuiAlpha));
            Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Accounts"), x + 25, catY + 5.0F, 10.5F, ColorUtil.multiplyAlpha(accountsActive ? Theme.TEXT_MAIN : Theme.TEXT_MUTED, mainGuiAlpha));

            float gearX = x + 10;
            float gearY = y + h - 20;
            Fonts.drawIcon(IconUse.GEAR, gearX, gearY, 11.5F, ColorUtil.multiplyAlpha(state.isSettingsPopupOpen() ? Theme.getAccentColor() : Theme.TEXT_MUTED, mainGuiAlpha));

            renderSearchAndHintsUnderGui(state, centerX, y + h + 6, mainGuiAlpha);

            float popupAlpha = mainGuiAlpha * state.getSettingsPopupAnim().getValue();
            float popupY = gearY - settingsPopup.getHeight() - 6;
            settingsPopup.render(x + 6, popupY, mouseX, mouseY, popupAlpha);
        }

        float sideW = 85.0F;
        float catProgress = state.getCategoryAnim().getValue();
        float contentAlpha = openProgress * catProgress;
        float slideY = (1.0F - catProgress) * (state.getCategoryDirection() * 12.0F);

        state.setHoveredModule(null);

        float contentX = x + sideW + 8;
        float contentY = y + 8;
        float contentW = w - sideW - 16;
        float contentH = h - 16;

        Render2D.pushScissor(contentX, contentY, contentW, contentH);

        if (state.getCurrentTab() == PanelLapState.Tab.BINDS) {
            if (mainGuiAlpha > 0.01F) {
                renderBindsTab(state, contentX, contentY + slideY, contentW, contentH, mouseX, mouseY, contentAlpha * (1.0F - holdProgress));
            }
        } else if (state.getCurrentTab() == PanelLapState.Tab.ACCOUNTS) {
            if (mainGuiAlpha > 0.01F) {
                renderAccountsTab(state, contentX, contentY + slideY, contentW, contentH, mouseX, mouseY, contentAlpha * (1.0F - holdProgress));
            }
        } else {
            renderModules(state, contentX, contentY + slideY, contentW, contentH, mouseX, mouseY, contentAlpha);
        }

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
        float currentW = state.getSearchWidthAnim().getValue();
        float searchH = 16.0F;
        float drawX = centerX - (currentW / 2.0F);

        if (currentW > 4.0F) {
            if (Theme.getBackgroundMode().equalsIgnoreCase("Blur")) {
                Render2D.drawBlur(drawX, startY, currentW, searchH, 6.0F, ColorUtil.multiplyAlpha(0xFF0C0D14, alpha * 0.7F), alpha);
            }

            int searchBg = state.isSearchFocused() ? 0xCC0E1018 : 0x880E1018;
            Render2D.drawRoundedRect(drawX, startY, currentW, searchH, 3.5F, ColorUtil.multiplyAlpha(searchBg, alpha));

            int iconCol = state.isSearchFocused() ? Theme.getAccentColor() : Theme.TEXT_MUTED;
            Fonts.drawIcon(IconUse.SEARCH, drawX + 6, startY + 3.5F, 9.0F, ColorUtil.multiplyAlpha(iconCol, alpha));

            String text = state.getSearchQuery() + (state.isSearchFocused() ? "|" : "");
            Fonts.drawString(Fonts.SF_MEDIUM, text, drawX + 19, startY + 2.5f, 8.5f, ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, alpha));
        } else {
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, Localization.get("Ctrl + F to Search"), centerX, startY + 3.0F, 9.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha * 0.95F));
        }

        String hint = state.getCurrentTab() == PanelLapState.Tab.ACCOUNTS
                ? Localization.get("Double click to select & relogin")
                : Localization.get("Hold Left Alt  to inspect");
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, hint, centerX, startY + 18.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha * 0.85F));
    }

    private void renderAccountsTab(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float currentScroll = state.getScrollOffset();
        float topY = startY - currentScroll;
        float addBtnH = 16.0F;

        float addAnim = state.getAccountAddAnim().getValue();
        float baseBtnW = 68.0F;
        float expandedW = 165.0F;
        float currentBarW = baseBtnW + (expandedW - baseBtnW) * addAnim;

        boolean isAddOpen = state.isAccountAddOpen() || addAnim > 0.05F;

        if (!isAddOpen) {
            boolean addHover = mouseX >= startX && mouseX <= startX + baseBtnW && mouseY >= topY && mouseY <= topY + addBtnH;
            Render2D.drawRoundedRect(startX, topY, baseBtnW, addBtnH, 3.0F,
                    ColorUtil.multiplyAlpha(addHover ? Theme.getAccentColor() : 0x40181A26, alpha));
            Fonts.drawIcon(IconUse.ADD, startX + 5.0F, topY + 4.0F, 8.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("Add"), startX + 16.0F, topY + 4.0F, 8.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
        } else {
            Render2D.drawRoundedRect(startX, topY, currentBarW, addBtnH, 4,
                    ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha * 0.17F));

            Render2D.pushScissor(startX, topY, currentBarW - 20.0F, addBtnH);

            boolean blink = (System.currentTimeMillis() / 450) % 2 == 0;
            String text = state.getAccountAddQuery();
            String display = text.isEmpty() ? Localization.get("Nickname...") : text + (blink ? "|" : "");
            int textCol = text.isEmpty() ? 0xFF606272 : 0xFFFFFFFF;

            Fonts.drawString(Fonts.SF_MEDIUM, display, startX + 6.0F, topY + 4.0F, 8.5F, ColorUtil.multiplyAlpha(textCol, alpha));
            Render2D.popScissor();

            if (addAnim > 0.7F) {
                float iconX = startX + currentBarW - 14.0F;
                float iconY = topY + 4.0F;
                boolean iconHover = mouseX >= iconX - 2 && mouseX <= iconX + 12 && mouseY >= topY && mouseY <= topY + addBtnH;
                int addFriendCol = iconHover ? 0xFF10B981 : Theme.getAccentColor();

                Fonts.drawIcon(IconUse.ADDFRIEND, iconX, iconY, 8.5F, ColorUtil.multiplyAlpha(addFriendCol, alpha * addAnim));
            }
        }

        float cardStartY = topY + addBtnH + 6.0F;
        float colWidth = (totalWidth - 6) / 2.0F;
        float col1Y = cardStartY;
        float col2Y = cardStartY;
        float cardH = 26.0F;

        String query = state.getSearchQuery().trim().toLowerCase();
        List<String> accounts = AccountManager.getInstance().getSortedAccounts().stream()
                .filter(name -> query.isEmpty() || name.toLowerCase().contains(query))
                .collect(Collectors.toList());

        String currentName = Minecraft.getInstance().getUser().getName();

        for (String acc : accounts) {
            boolean isCol1 = col1Y <= col2Y;
            float accX = isCol1 ? startX : startX + colWidth + 6;
            float accY = isCol1 ? col1Y : col2Y;

            boolean isActive = acc.equalsIgnoreCase(currentName);
            boolean isFav = AccountManager.getInstance().isFavorite(acc);
            boolean inVisibleArea = mouseY >= startY && mouseY <= startY + totalHeight;
            boolean hovered = inVisibleArea && mouseX >= accX && mouseX <= accX + colWidth && mouseY >= accY && mouseY <= accY + cardH;

            int cardBg = CARD_BG_INACTIVE;
            if (isActive) {
                cardBg = ColorUtil.lerp(CARD_BG_ACTIVE_BASE, Theme.getAccentColor(), 0.28F);
            } else if (isFav) {
                cardBg = ColorUtil.lerp(CARD_BG_INACTIVE, 0xFF3D3210, 0.45F);
            } else if (hovered) {
                cardBg = 0xFF191B24;
            }

            float cardAlpha = isActive ? 0.75F : (isFav ? 0.65F : 0.45F);
            Render2D.drawRoundedRect(accX, accY, colWidth, cardH, 4.0F, ColorUtil.multiplyAlpha(cardBg, alpha * cardAlpha));

            if (isActive) {
                Render2D.drawRoundedOutline(accX, accY, colWidth, cardH, 4.0F, 1.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha * 0.9F));
            }

            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + acc).getBytes(StandardCharsets.UTF_8));
            Identifier skinTexture = DefaultPlayerSkin.get(uuid).body().texturePath();
            Render2D.drawHead(skinTexture, accX + 4.0F, accY + 4.0F, 18.0F, 3.0F, ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha));

            String displayName = Fonts.SF_MEDIUM.trimToWidth(acc, colWidth - 72.0F, 10.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, displayName, accX + 25.0F, accY + 7.5F, 10.0F,
                    ColorUtil.multiplyAlpha(isActive ? ColorUtil.WHITE : (isFav ? 0xFFFFF1AA : Theme.TEXT_MAIN), alpha));

            float starX = accX + colWidth - 42.0F;
            float starY = accY + 8.0F;
            boolean starHover = mouseX >= starX && mouseX <= starX + 11.0F && mouseY >= starY && mouseY <= starY + 14.0F;
            int starColor = isFav ? 0xFFFFD700 : (starHover ? 0xFFFFF275 : Theme.TEXT_MUTED);
            Fonts.drawIcon(IconUse.STAR, starX, starY, 9.5F, ColorUtil.multiplyAlpha(starColor, alpha));

            float linkX = accX + colWidth - 28.0F;
            float linkY = accY + 8.0F;
            boolean linkHover = mouseX >= linkX && mouseX <= linkX + 11.0F && mouseY >= linkY && mouseY <= linkY + 14.0F;
            int linkColor = linkHover ? Theme.getAccentColor() : Theme.TEXT_MUTED;
            Fonts.drawIcon(IconUse.LINK, linkX, linkY, 9.5F, ColorUtil.multiplyAlpha(linkColor, alpha));

            float crossX = accX + colWidth - 14.0F;
            float crossY = accY + 8.0F;
            boolean crossHover = mouseX >= crossX && mouseX <= crossX + 11.0F && mouseY >= crossY && mouseY <= crossY + 14.0F;
            int crossColor = crossHover ? 0xFFEF4444 : Theme.TEXT_MUTED;
            Fonts.drawIcon(IconUse.CROSS, crossX, crossY, 9.5F, ColorUtil.multiplyAlpha(crossColor, alpha));

            if (isCol1) col1Y += cardH + 4.0F;
            else col2Y += cardH + 4.0F;
        }

        if (accounts.isEmpty()) {
            float emptyY = startY + totalHeight / 2.0F - 6.0F;
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, Localization.get("No accounts"), startX + totalWidth / 2.0F, emptyY, 10.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        }

        float totalContentH = Math.max(col1Y, col2Y) + currentScroll - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private void renderModules(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        List<Module> list = getFilteredModules(state);
        float colWidth = (totalWidth - 6) / 2.0F;
        float currentScroll = state.getScrollOffset();
        float col1Y = startY - currentScroll;
        float col2Y = startY - currentScroll;

        boolean isLAltDown = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;

        long time = System.currentTimeMillis();
        int dotPhase = (int) ((time / 260) % 4);
        String dotAnim = ".".repeat(Math.max(1, dotPhase));

        int activeCardBg = ColorUtil.lerp(CARD_BG_ACTIVE_BASE, Theme.getAccentColor(), 0.18F);

        for (Module module : list) {
            boolean isCol1 = col1Y <= col2Y;
            float targetModX = isCol1 ? startX : startX + colWidth + 6;
            float targetModY = isCol1 ? col1Y : col2Y;

            module.updatePosition(targetModX, targetModY);

            float modX = module.getPosXAnim().getValue() + module.getShakeOffset();
            float modY = module.getPosYAnim().getValue();

            module.getExpandAnim().setTarget(module.isExpanded() ? 1.0F : 0.0F);
            module.getExpandAnim().update();

            module.getBindAnim().setTarget(isLAltDown || module.isBinding() ? 1.0F : 0.0F);
            module.getBindAnim().update();

            float totalSetH = 0.0F;
            for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                sr.updateVisibility();
                float sH = sr.getHeight();
                if (sH > 0.1F) {
                    totalSetH += sH + 2.0F;
                }
            }

            float cardHeight = 24.0F + (module.getExpandAnim().getValue() * (totalSetH + 2.0F));

            boolean inVisibleArea = mouseY >= startY && mouseY <= startY + totalHeight;
            boolean hovered = inVisibleArea && mouseX >= modX && mouseX <= modX + colWidth && mouseY >= modY && mouseY <= modY + cardHeight;

            if (state.isHoldActive() && !hovered) {
                if (isCol1) col1Y += cardHeight + 4.0F;
                else col2Y += cardHeight + 4.0F;
                continue;
            }

            if (hovered) {
                if (module.hasDescription() && !state.isHoldActive()) {
                    state.setHoveredModule(module);
                } else if (!state.isHoldActive()) {
                    module.triggerShake();
                }
            }

            float modAlpha = state.isHoldActive() ? state.getOpenAnimation().getValue() : alpha;
            int cardBg = module.isState() ? activeCardBg : CARD_BG_INACTIVE;
            float cardAlpha = module.isState() ? 0.65F : 0.45F;

            if (state.isHoldActive()) {
                cardBg = 0xEE11131C;
                cardAlpha = 0.95F;
                Render2D.drawBlur(modX, modY, colWidth, cardHeight, 10.0F, 0x880C0D14, modAlpha);
                Render2D.drawRoundedOutline(modX, modY, colWidth, cardHeight, 4.0F, 1.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), modAlpha * 0.8F));
            }

            Render2D.drawRoundedRect(modX, modY, colWidth, cardHeight, 4.0F, ColorUtil.multiplyAlpha(cardBg, modAlpha * cardAlpha));

            Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), modX + 7, modY + 6.5F, 10.5F,
                    ColorUtil.multiplyAlpha(module.isState() ? Theme.TEXT_MAIN : Theme.TEXT_MUTED, modAlpha));

            float bAnim = module.getBindAnim().getValue();
            if (bAnim > 0.01F && !state.isHoldActive()) {
                boolean isBinding = module.isBinding();
                String badgeText = isBinding ? dotAnim : module.getBind().getDisplayValue();
                float textW = Fonts.SF_MEDIUM.getWidth(badgeText, 8.5F);
                float bw = Math.max(16.0F, textW + 8.0F);

                float slideOffset = (1.0F - bAnim) * 10.0F;
                float bx = modX + colWidth - bw - 6 + slideOffset;

                int bg = isBinding ? Theme.getAccentColor() : 0x50202230;
                int textCol = isBinding ? ColorUtil.WHITE : Theme.TEXT_MUTED;

                Render2D.drawRoundedRect(bx, modY + 4.5F, bw, 13.0F, 2.5F, ColorUtil.multiplyAlpha(bg, modAlpha * bAnim));
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, badgeText, bx + (bw / 2.0F), modY + 5.5F, 8.5F, ColorUtil.multiplyAlpha(textCol, modAlpha * bAnim));
            }

            if (module.getExpandAnim().getValue() > 0.02F) {
                float setY = modY + 24.0F;
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    float sH = sr.getHeight();
                    if (sH > 0.5F) {
                        sr.render(modX + 2, setY, colWidth - 4, sH, mouseX, mouseY, modAlpha * module.getExpandAnim().getValue());
                        setY += sH + 2.0F;
                    }
                }
            }

            if (isCol1) col1Y += cardHeight + 4.0F;
            else col2Y += cardHeight + 4.0F;
        }

        float totalContentH = Math.max(col1Y, col2Y) + currentScroll - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    private void renderBindsTab(PanelLapState state, float startX, float startY, float totalWidth, float totalHeight, int mouseX, int mouseY, float alpha) {
        float currentScroll = state.getScrollOffset();
        float filterY = startY - currentScroll;
        float bW = 48.0F, uW = 56.0F;
        float btnH = 15.0F;

        Render2D.drawRoundedRect(startX, filterY, bW, btnH, 3.0F, ColorUtil.multiplyAlpha(state.isBindsShowBound() ? Theme.getAccentColor() : 0x40181A26, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, Localization.get("Bound"), startX + (bW / 2.0F), filterY + 3.5F, 9.0F, ColorUtil.multiplyAlpha(state.isBindsShowBound() ? ColorUtil.WHITE : Theme.TEXT_MUTED, alpha));

        Render2D.drawRoundedRect(startX + bW + 4, filterY, uW, btnH, 3.0F, ColorUtil.multiplyAlpha(state.isBindsShowUnbound() ? Theme.getAccentColor() : 0x40181A26, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, Localization.get("Unbound"), startX + bW + 4 + (uW / 2.0F), filterY + 3.5F, 9.0F, ColorUtil.multiplyAlpha(state.isBindsShowUnbound() ? ColorUtil.WHITE : Theme.TEXT_MUTED, alpha));

        float cardY = filterY + btnH + 5.0F;
        float colWidth = (totalWidth - 6) / 2.0F;
        float col1Y = cardY, col2Y = cardY;
        float cardH = 22.0F;

        for (Module module : Client.INSTANCE.moduleManager.getModules()) {
            boolean isBound = module.getBind().isBound();
            if (isBound && !state.isBindsShowBound()) continue;
            if (!isBound && !state.isBindsShowUnbound()) continue;

            boolean isCol1 = col1Y <= col2Y;
            float targetModX = isCol1 ? startX : startX + colWidth + 6;
            float targetModY = isCol1 ? col1Y : col2Y;

            module.updatePosition(targetModX, targetModY);

            float modX = module.getPosXAnim().getValue();
            float modY = module.getPosYAnim().getValue();

            Render2D.drawRoundedRect(modX, modY, colWidth, cardH, 4.0F, ColorUtil.multiplyAlpha(CARD_BG_INACTIVE, alpha * 0.45F));

            Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), modX + 7, modY + 6.0F, 9.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, alpha));

            String keyName = module.isBinding() ? "..." : module.getBind().getDisplayValue();
            float textW = Fonts.SF_MEDIUM.getWidth(keyName, 8.5F);
            float kw = Math.max(18.0F, textW + 8.0F);
            float kx = modX + colWidth - kw - 6;
            float ky = modY + 4.5F;

            int bg = module.isBinding() ? Theme.getAccentColor() : 0x50202230;
            int textCol = module.isBinding() ? ColorUtil.WHITE : Theme.TEXT_MUTED;

            Render2D.drawRoundedRect(kx, ky, kw, 13.0F, 2.5F, ColorUtil.multiplyAlpha(bg, alpha));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, keyName, kx + (kw / 2.0F), ky + 1.5F, 8.5F, ColorUtil.multiplyAlpha(textCol, alpha));

            if (isCol1) col1Y += cardH + 4.0F;
            else col2Y += cardH + 4.0F;
        }

        float totalContentH = Math.max(col1Y, col2Y) + currentScroll - startY;
        state.setMaxScroll(Math.max(0.0F, totalContentH - totalHeight + 10.0F));
    }

    public void handleScroll(PanelLapState state, double deltaY, int mouseX, int mouseY) {
        float x = state.getPanelX() + 85.0F;
        float y = state.getPanelY();
        float w = state.getPanelWidth() - 85.0F;
        float h = state.getPanelHeight();

        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            state.scroll((float) deltaY * 24.0F);
        }
    }

    private List<Module> getFilteredModules(PanelLapState state) {
        String query = state.getSearchQuery().trim().toLowerCase();
        if (query.isEmpty()) {
            return Client.INSTANCE.moduleManager.getByCategory(state.getCurrentCategory());
        }

        String[] tokens = Arrays.stream(query.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        return Client.INSTANCE.moduleManager.getModules().stream()
                .filter(m -> {
                    String originalName = m.getName().toLowerCase();
                    for (String token : tokens) {
                        if (originalName.contains(token)) return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public boolean handleMouseButton(PanelLapState state, int mouseX, int mouseY, int button, int action) {
        if (state.getActiveModal() != null) {
            if (action == GLFW.GLFW_PRESS) {
                state.getActiveModal().mouseClicked(mouseX, mouseY, button);
            } else if (action == GLFW.GLFW_RELEASE) {
                state.getActiveModal().mouseReleased(mouseX, mouseY, button);
            }
            return true;
        }

        float x = state.getPanelX();
        float y = state.getPanelY();
        float w = state.getPanelWidth();
        float h = state.getPanelHeight();
        float gearY = y + h - 20;

        if (action == GLFW.GLFW_PRESS) {
            for (Module m : Client.INSTANCE.moduleManager.getModules()) {
                if (m.isBinding()) {
                    m.getBind().setSingle(KeyUtil.fromMouseButton(button));
                    m.setBinding(false);
                    return true;
                }
            }
        }

        if (state.isSettingsPopupOpen()) {
            float popupY = gearY - settingsPopup.getHeight() - 6;
            if (action == GLFW.GLFW_PRESS) {
                if (settingsPopup.mouseClicked(x + 6, popupY, mouseX, mouseY, button)) {
                    return true;
                } else {
                    state.closeSettingsPopup();
                }
            } else if (action == GLFW.GLFW_RELEASE) {
                settingsPopup.mouseReleased(mouseX, mouseY, button);
            }
        }

        if (action == GLFW.GLFW_RELEASE) {
            dragging = false;
            for (Module module : Client.INSTANCE.moduleManager.getModules()) {
                if (module.isExpanded()) {
                    for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                        sr.mouseReleased(mouseX, mouseY, button);
                    }
                }
            }
            return true;
        }

        if (action != GLFW.GLFW_PRESS) return true;

        float sideW = 85.0F;

        if (!state.isHoldActive() && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 16) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }

        float gearX = x + 10;
        if (mouseX >= gearX && mouseX <= gearX + 16 && mouseY >= gearY && mouseY <= gearY + 16) {
            state.toggleSettingsPopup();
            return true;
        }

        float catY = y + 14.0F;
        for (Category category : Category.values()) {
            if (mouseX >= x + 5 && mouseX <= x + sideW - 5 && mouseY >= catY && mouseY <= catY + 20) {
                state.switchCategory(category);
                return true;
            }
            catY += 23.0F;
        }

        if (mouseX >= x + 5 && mouseX <= x + sideW - 5 && mouseY >= catY && mouseY <= catY + 20) {
            state.openBindsTab();
            return true;
        }
        catY += 23.0F;

        if (mouseX >= x + 5 && mouseX <= x + sideW - 5 && mouseY >= catY && mouseY <= catY + 20) {
            state.openAccountsTab();
            return true;
        }

        float centerX = x + (w / 2.0F);
        float searchY = y + h + 6;
        float curW = Math.max(90.0F, state.getSearchWidthAnim().getValue());
        if (mouseX >= centerX - (curW / 2.0F) && mouseX <= centerX + (curW / 2.0F) && mouseY >= searchY && mouseY <= searchY + 16) {
            state.setSearchFocused(true);
            return true;
        } else {
            state.setSearchFocused(false);
        }

        float contentYStart = y + 8;
        float contentYEnd = y + h - 8;

        if (mouseY < contentYStart || mouseY > contentYEnd || mouseX < x + sideW) {
            return true;
        }

        float startX = x + sideW + 8;
        float totalWidth = w - sideW - 16;
        float colWidth = (totalWidth - 6) / 2.0F;

        if (state.getCurrentTab() == PanelLapState.Tab.ACCOUNTS) {
            float currentScroll = state.getScrollOffset();
            float topY = contentYStart - currentScroll;
            float addBtnH = 16.0F;

            float addAnim = state.getAccountAddAnim().getValue();
            float baseBtnW = 68.0F;
            float expandedW = 165.0F;
            float currentBarW = baseBtnW + (expandedW - baseBtnW) * addAnim;

            if (mouseX >= startX && mouseX <= startX + currentBarW && mouseY >= topY && mouseY <= topY + addBtnH) {
                if (!state.isAccountAddOpen()) {
                    state.setAccountAddOpen(true);
                    state.setAccountAddQuery("");
                } else {
                    float iconX = startX + currentBarW - 14.0F;
                    if (mouseX >= iconX - 4.0F) {
                        confirmAddInline(state);
                    }
                }
                return true;
            } else if (state.isAccountAddOpen()) {
                state.setAccountAddOpen(false);
            }

            float cardStartY = topY + addBtnH + 6.0F;
            float col1Y = cardStartY;
            float col2Y = cardStartY;
            float cardH = 26.0F;

            String query = state.getSearchQuery().trim().toLowerCase();
            List<String> accounts = AccountManager.getInstance().getSortedAccounts().stream()
                    .filter(name -> query.isEmpty() || name.toLowerCase().contains(query))
                    .collect(Collectors.toList());

            for (String acc : accounts) {
                boolean isCol1 = col1Y <= col2Y;
                float accX = isCol1 ? startX : startX + colWidth + 6;
                float accY = isCol1 ? col1Y : col2Y;

                if (mouseX >= accX && mouseX <= accX + colWidth && mouseY >= accY && mouseY <= accY + cardH) {
                    float starX = accX + colWidth - 42.0F;
                    float linkX = accX + colWidth - 28.0F;
                    float crossX = accX + colWidth - 14.0F;
                    float iconY = accY + 6.0F;

                    if (mouseX >= starX && mouseX <= starX + 12.0F && mouseY >= iconY && mouseY <= iconY + 14.0F) {
                        AccountManager.getInstance().toggleFavorite(acc);
                        return true;
                    }

                    if (mouseX >= linkX && mouseX <= linkX + 12.0F && mouseY >= iconY && mouseY <= iconY + 14.0F) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(acc);
                        return true;
                    }

                    if (mouseX >= crossX && mouseX <= crossX + 12.0F && mouseY >= iconY && mouseY <= iconY + 14.0F) {
                        AccountManager.getInstance().removeAccount(acc);
                        return true;
                    }

                    long now = System.currentTimeMillis();
                    if (acc.equals(lastClickedAccount) && (now - lastAccountClickTime) < 400) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null) {
                            AccountManager.getInstance().relogin(acc);
                        } else {
                            AccountManager.getInstance().setSession(acc);
                        }
                        lastAccountClickTime = 0;
                        lastClickedAccount = "";
                    } else {
                        lastAccountClickTime = now;
                        lastClickedAccount = acc;
                    }
                    return true;
                }

                if (isCol1) col1Y += cardH + 4.0F;
                else col2Y += cardH + 4.0F;
            }
            return true;
        }

        if (state.getCurrentTab() == PanelLapState.Tab.BINDS) {
            float filterY = contentYStart - state.getScrollOffset();
            float bW = 48.0F, uW = 56.0F;

            if (mouseX >= startX && mouseX <= startX + bW && mouseY >= filterY && mouseY <= filterY + 15) {
                state.setBindsShowBound(!state.isBindsShowBound());
                return true;
            }
            if (mouseX >= startX + bW + 4 && mouseX <= startX + bW + 4 + uW && mouseY >= filterY && mouseY <= filterY + 15) {
                state.setBindsShowUnbound(!state.isBindsShowUnbound());
                return true;
            }

            for (Module module : Client.INSTANCE.moduleManager.getModules()) {
                boolean isBound = module.getBind().isBound();
                if (isBound && !state.isBindsShowBound()) continue;
                if (!isBound && !state.isBindsShowUnbound()) continue;

                float modX = module.getPosXAnim().getValue();
                float modY = module.getPosYAnim().getValue();

                if (mouseX >= modX && mouseX <= modX + colWidth && mouseY >= modY && mouseY <= modY + 22.0F) {
                    module.setBinding(true);
                    return true;
                }
            }
            return true;
        }

        for (Module module : getFilteredModules(state)) {
            float modX = module.getPosXAnim().getValue() + module.getShakeOffset();
            float modY = module.getPosYAnim().getValue();

            if (mouseX >= modX && mouseX <= modX + colWidth && mouseY >= modY && mouseY <= modY + 24) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    module.toggle();
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (module.getSettingRenderers().isEmpty()) {
                        module.triggerShake();
                    } else {
                        module.setExpanded(!module.isExpanded());
                    }
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    module.setBinding(true);
                }
                return true;
            }

            if (module.isExpanded() && mouseX >= modX && mouseX <= modX + colWidth) {
                float setY = modY + 24.0F;
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    float sH = sr.getHeight();
                    if (sH > 0.5F) {
                        if (mouseY >= setY && mouseY <= setY + sH && setY >= contentYStart && setY + sH <= contentYEnd) {
                            sr.mouseClicked(mouseX, mouseY, button);
                            return true;
                        }
                        setY += sH + 2.0F;
                    }
                }
            }
        }

        return true;
    }

    public boolean handleKey(PanelLapState state, int key, int scanCode, int modifiers) {
        if (state.getActiveModal() != null) {
            return state.getActiveModal().keyPressed(key, scanCode, modifiers);
        }

        if (state.isAccountAddOpen()) {
            if (key == GLFW.GLFW_KEY_ENTER) {
                confirmAddInline(state);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                state.setAccountAddOpen(false);
                state.setAccountAddQuery("");
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !state.getAccountAddQuery().isEmpty()) {
                state.setAccountAddQuery(state.getAccountAddQuery().substring(0, state.getAccountAddQuery().length() - 1));
                return true;
            }
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_V) {
                String paste = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (paste != null) {
                    paste = paste.replaceAll("[^a-zA-Z0-9_]", "");
                    String res = state.getAccountAddQuery() + paste;
                    if (res.length() > 16) res = res.substring(0, 16);
                    state.setAccountAddQuery(res);
                }
                return true;
            }
            return true;
        }

        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            if (m.isBinding()) {
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    m.setBinding(false);
                } else if (key == GLFW.GLFW_KEY_DELETE) {
                    m.getBind().clear();
                    m.setBinding(false);
                } else {
                    m.getBind().setSingle(key);
                    m.setBinding(false);
                }
                return true;
            }
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
            state.setSearchFocused(true);
            return true;
        }

        if (state.isSearchFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                state.setSearchFocused(false);
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !state.getSearchQuery().isEmpty()) {
                state.setSearchQuery(state.getSearchQuery().substring(0, state.getSearchQuery().length() - 1));
                state.resetScroll();
                return true;
            }
            return true;
        }

        for (Module module : Client.INSTANCE.moduleManager.getModules()) {
            if (module.isExpanded()) {
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

        if (state.isAccountAddOpen()) {
            if (state.getAccountAddQuery().length() < 16 && (Character.isLetterOrDigit(codePoint) || codePoint == '_')) {
                state.setAccountAddQuery(state.getAccountAddQuery() + (char) codePoint);
            }
            return;
        }

        if (state.isSearchFocused()) {
            state.setSearchQuery(state.getSearchQuery() + (char) codePoint);
            state.resetScroll();
            return;
        }

        for (Module module : Client.INSTANCE.moduleManager.getModules()) {
            if (module.isExpanded()) {
                for (SettingRenderer<?> sr : module.getSettingRenderers()) {
                    sr.charTyped(codePoint);
                }
            }
        }
    }

    private void confirmAddInline(PanelLapState state) {
        String name = state.getAccountAddQuery().trim();
        if (!name.isEmpty()) {
            AccountManager.getInstance().addAccount(name);
        }
        state.setAccountAddQuery("");
        state.setAccountAddOpen(false);
    }

    private IconUse getCategoryIcon(Category cat) {
        return switch (cat) {
            case COMBAT -> IconUse.FIGHT;
            case MOVEMENT -> IconUse.MOVEMENT;
            case RENDER -> IconUse.RENDER;
            case PLAYER -> IconUse.PLAYER;
            case MISC -> IconUse.MISC;
        };
    }
}