package error.ui.mainmenu;

import error.account.AccountManager;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomTitleScreen extends Screen {

    private static final Identifier BANNER_SINGLEPLAYER = Identifier.fromNamespaceAndPath("error", "images/ui/title/title.png");
    private static final Identifier BANNER_MULTIPLAYER  = Identifier.fromNamespaceAndPath("error", "images/ui/title/title2.png");
    private static final Identifier LOGO_TEXTURE        = Identifier.fromNamespaceAndPath("error", "images/logo.png");

    // Modal state for Accounts
    private boolean accountModalOpen = false;
    private float accountModalAnim = 0.0F;

    // Transition & Animation states
    private float screenAlpha = 1.0F;
    private Screen targetScreen = null;
    private long startTime = System.currentTimeMillis();

    // Account Manager State
    private String selectedAccount = "";
    private String searchFilter = "";
    private int selectedTab = 0; // 0: Все, 1: Избранные
    private boolean searchFocused = false;
    private float accountScroll = 0.0F;
    private float maxAccountScroll = 0.0F;

    // Button hover animations: 0: MP, 1: SP, 2: Acc, 3: Set, 4: Exit
    private final float[] mainBtnAnims = new float[5];

    public CustomTitleScreen() {
        super(Component.literal("Main Menu"));
    }

    public static void loadWallpaper() {
        // Compatibility stub for Client.java
    }

    @Override
    protected void init() {
        super.init();
        AccountManager.getInstance().applyActiveSession();
        this.screenAlpha = 1.0F;
        this.targetScreen = null;
        this.selectedAccount = AccountManager.getInstance().getActiveAccount();
        this.startTime = System.currentTimeMillis();
    }

    private void switchScreen(Screen screen) {
        this.targetScreen = screen;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Custom background rendered directly in extractRenderState
    }

    @Override
    protected void extractPanorama(GuiGraphicsExtractor extractor, float partialTick) {
        // Disable vanilla CubeMap panorama
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenHeight = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

        // Transition fade
        if (this.targetScreen != null) {
            this.screenAlpha = Math.max(0.0F, this.screenAlpha - 0.12F);
            if (this.screenAlpha <= 0.01F) {
                this.minecraft.setScreenAndShow(this.targetScreen);
                return;
            }
        } else {
            this.screenAlpha = Math.min(1.0F, this.screenAlpha + 0.10F);
        }

        this.accountModalAnim = Mth.clamp(this.accountModalAnim + (accountModalOpen ? 0.10F : -0.10F), 0.0F, 1.0F);

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            // 1. Solid Pure Black Background
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(0, 0, 0, 255));

            // Animated Night Sky Background (Procedural Stars, Nebula, Aurora, Snow)
            WinterMenuRenderer.render(screenWidth, screenHeight, this.screenAlpha, 0.0F);

            // 2. Central Liquid Glass Menu UI (Logo, Title, Multiplayer, Singleplayer, Accounts, Settings, Exit)
            renderMainMenuUI(screenWidth, screenHeight, mouseX, mouseY);

            // 3. Accounts Manager Liquid Glass Modal
            if (this.accountModalAnim > 0.001F) {
                renderAccountModal(screenWidth, screenHeight, mouseX, mouseY, this.accountModalAnim);
            }

            Render2DUtil.flush();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderMainMenuUI(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 2.0F;
        int accent = Theme.getAccentColor();
        long elapsed = System.currentTimeMillis() - startTime;

        // Smooth floating motion for logo
        float logoFloat = (float) Math.sin(elapsed / 450.0D) * 3.5F;

        // 1. Central Logo & Brand Text
        float logoSize = 64.0F;
        float logoY = centerY - 152.0F + logoFloat;
        float logoX = centerX - logoSize / 2.0F;

        // Super Smooth Soft Radial Glow (Multi-layered exponential falloff into background)
        int ar = ColorUtil.red(accent);
        int ag = ColorUtil.green(accent);
        int ab = ColorUtil.blue(accent);

        for (int i = 6; i >= 1; i--) {
            float sizeOffset = i * 16.0F;
            float radius = (logoSize + sizeOffset * 2.0F) / 2.0F;
            float shadowRadius = i * 14.0F;
            int layerAlpha = (int) ((14.0F / (i * i)) * this.screenAlpha);
            if (layerAlpha > 0) {
                Render2D.drawShadow(
                        logoX - sizeOffset,
                        logoY - sizeOffset,
                        logoSize + sizeOffset * 2.0F,
                        logoSize + sizeOffset * 2.0F,
                        radius,
                        shadowRadius,
                        ColorUtil.rgba(ar, ag, ab, layerAlpha)
                );
            }
        }

        Render2D.drawTexture(LOGO_TEXTURE, logoX, logoY, logoSize, logoSize,
                ColorUtil.rgba(255, 255, 255, (int) (255 * this.screenAlpha)));

        // Client Name & Version
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Error DLC", centerX, logoY + logoSize + 10.0F, 14.0F,
                ColorUtil.rgba(250, 250, 255, (int) (255 * this.screenAlpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "26.2", centerX, logoY + logoSize + 28.0F, 8.0F,
                ColorUtil.rgba(160, 175, 205, (int) (200 * this.screenAlpha)));

        // 2. Central Cards Layout (Multiplayer & Singleplayer)
        float cardW = 190.0F;
        float cardH = 124.0F;
        float gap = 12.0F;
        float totalW = cardW * 2.0F + gap;
        float startX = (screenWidth - totalW) / 2.0F;
        float startY = centerY - 22.0F;

        boolean modalActive = accountModalOpen || targetScreen != null;

        // Card 0: Multiplayer (Left side)
        float mpX = startX;
        float mpY = startY;
        boolean mpHovered = !modalActive && mouseX >= mpX && mouseX <= mpX + cardW && mouseY >= mpY && mouseY <= mpY + cardH;
        mainBtnAnims[0] = Mth.clamp(mainBtnAnims[0] + (mpHovered ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderFeatureCard(mpX, mpY, cardW, cardH, "Multiplayer", IconUse.GROUP,
                "Play with friends over a local network or connect to dedicated servers.",
                BANNER_MULTIPLAYER, mainBtnAnims[0], accent);

        // Card 1: Singleplayer (Right side)
        float spX = startX + cardW + gap;
        float spY = startY;
        boolean spHovered = !modalActive && mouseX >= spX && mouseX <= spX + cardW && mouseY >= spY && mouseY <= spY + cardH;
        mainBtnAnims[1] = Mth.clamp(mainBtnAnims[1] + (spHovered ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderFeatureCard(spX, spY, cardW, cardH, "Singleplayer", IconUse.PERSONS,
                "Dive into your own adventure. Create new worlds with unique generation settings.",
                BANNER_SINGLEPLAYER, mainBtnAnims[1], accent);

        // Row 2: Accounts & Settings Buttons
        float row2Y = startY + cardH + gap;
        float subBtnH = 30.0F;

        // Button 2: Accounts
        float accX = startX;
        boolean accHovered = !modalActive && mouseX >= accX && mouseX <= accX + cardW && mouseY >= row2Y && mouseY <= row2Y + subBtnH;
        mainBtnAnims[2] = Mth.clamp(mainBtnAnims[2] + (accHovered ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderRowButton(accX, row2Y, cardW, subBtnH, "Accounts", IconUse.INFO, mainBtnAnims[2], accent);

        // Button 3: Settings
        float setX = startX + cardW + gap;
        boolean setHovered = !modalActive && mouseX >= setX && mouseX <= setX + cardW && mouseY >= row2Y && mouseY <= row2Y + subBtnH;
        mainBtnAnims[3] = Mth.clamp(mainBtnAnims[3] + (setHovered ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderRowButton(setX, row2Y, cardW, subBtnH, "Settings", IconUse.GEAR, mainBtnAnims[3], accent);

        // Row 3: Exit (Full Width)
        float row3Y = row2Y + subBtnH + gap;
        boolean exitHovered = !modalActive && mouseX >= startX && mouseX <= startX + totalW && mouseY >= row3Y && mouseY <= row3Y + subBtnH;
        mainBtnAnims[4] = Mth.clamp(mainBtnAnims[4] + (exitHovered ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderRowButton(startX, row3Y, totalW, subBtnH, "Exit", IconUse.EXIT, mainBtnAnims[4], ColorUtil.rgba(245, 65, 80, 255));
    }

    private void renderFeatureCard(float x, float y, float w, float h, String title, IconUse icon, String desc, Identifier bannerTex, float hoverAnim, int accent) {
        // Animated elevation on hover
        float drawY = y - 3.0F * hoverAnim;

        int liquidGlassFill = ColorUtil.rgba(18, 16, 28, (int) ((200 + 25 * hoverAnim) * this.screenAlpha));
        int glassBorder = ColorUtil.lerp(
                ColorUtil.rgba(255, 255, 255, (int) (35 * this.screenAlpha)),
                ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (180 * this.screenAlpha)),
                hoverAnim
        );
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (160 * this.screenAlpha));

        // Authentic Liquid Glass Blur & Background Container
        Render2D.drawShadow(x, drawY, w, h, 12.0F, 12.0F, shadowCol);
        if (hoverAnim > 0.01F) {
            Render2D.drawShadow(x, drawY, w, h, 12.0F, 12.0F,
                    ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (50 * hoverAnim * this.screenAlpha)));
        }

        Render2D.glass(x, drawY, w, h, this.screenAlpha, 12.0F, 32.0F, 26.0F);
        Render2D.drawRoundedRect(x, drawY, w, h, 12.0F, liquidGlassFill);
        Render2D.drawRoundedOutline(x, drawY, w, h, 12.0F, 1.0F, glassBorder);

        // Header: Title & Icon
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 12.0F, drawY + 10.0F, 9.0F, ColorUtil.rgba(250, 250, 255, (int) (250 * this.screenAlpha)));
        int iconCol = ColorUtil.lerp(ColorUtil.rgba(180, 195, 220, (int) (190 * this.screenAlpha)), accent, hoverAnim);
        Fonts.drawIcon(icon, x + w - 24.0F, drawY + 9.5F, 9.0F, iconCol);

        // Description text
        if (title.equalsIgnoreCase("Singleplayer")) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Dive into your own adventure.", x + 12.0F, drawY + 24.0F, 6.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * this.screenAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "Create new worlds with unique", x + 12.0F, drawY + 32.0F, 6.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * this.screenAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "generation settings and game rules.", x + 12.0F, drawY + 40.0F, 6.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * this.screenAlpha)));
        } else {
            Fonts.drawString(Fonts.SF_MEDIUM, "Play with friends over a local", x + 12.0F, drawY + 24.0F, 6.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * this.screenAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "network or connect to dedicated", x + 12.0F, drawY + 32.0F, 6.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * this.screenAlpha)));
            Fonts.drawString(Fonts.SF_MEDIUM, "servers.", x + 12.0F, drawY + 40.0F, 6.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * this.screenAlpha)));
        }

        // Bottom Image Banner Box with rounded corners
        float bannerW = w - 20.0F;
        float bannerH = 48.0F;
        float bannerX = x + 10.0F;
        float bannerY = drawY + h - bannerH - 10.0F;

        Render2D.drawTexture(bannerTex, bannerX, bannerY, bannerW, bannerH, 7.0F, ColorUtil.rgba(225, 235, 255, (int) (225 * this.screenAlpha)));
        Render2D.drawRoundedOutline(bannerX, bannerY, bannerW, bannerH, 7.0F, 0.9F, ColorUtil.rgba(255, 255, 255, (int) (35 * this.screenAlpha)));
    }

    private void renderRowButton(float x, float y, float w, float h, String title, IconUse icon, float hoverAnim, int accent) {
        float drawY = y - 2.0F * hoverAnim;

        int liquidGlassFill = ColorUtil.rgba(18, 16, 28, (int) ((200 + 25 * hoverAnim) * this.screenAlpha));
        int glassBorder = ColorUtil.lerp(
                ColorUtil.rgba(255, 255, 255, (int) (35 * this.screenAlpha)),
                ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (180 * this.screenAlpha)),
                hoverAnim
        );
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (150 * this.screenAlpha));

        Render2D.drawShadow(x, drawY, w, h, 9.0F, 9.0F, shadowCol);
        if (hoverAnim > 0.01F) {
            Render2D.drawShadow(x, drawY, w, h, 9.0F, 8.0F,
                    ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (45 * hoverAnim * this.screenAlpha)));
        }

        Render2D.glass(x, drawY, w, h, this.screenAlpha, 9.0F, 32.0F, 26.0F);
        Render2D.drawRoundedRect(x, drawY, w, h, 9.0F, liquidGlassFill);
        Render2D.drawRoundedOutline(x, drawY, w, h, 9.0F, 1.0F, glassBorder);

        float textY = drawY + (h - 8.0F) / 2.0F - 0.5F;
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 14.0F, textY, 8.5F, ColorUtil.rgba(250, 250, 255, (int) (250 * this.screenAlpha)));

        int iconCol = ColorUtil.lerp(ColorUtil.rgba(180, 195, 220, (int) (190 * this.screenAlpha)), accent, hoverAnim);
        Fonts.drawIcon(icon, x + w - 22.0F, textY - 0.5F, 8.5F, iconCol);
    }

    /**
     * Account Manager Liquid Glass modal window.
     */
    private void renderAccountModal(int screenWidth, int screenHeight, int mouseX, int mouseY, float alpha) {
        float modalW = 430.0F;
        float modalH = 270.0F;
        float modalX = (screenWidth - modalW) / 2.0F;
        float modalY = (screenHeight - modalH) / 2.0F;

        int accent = Theme.getAccentColor();

        // Dark dim backdrop with glass blur
        Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(0, 0, 0, (int) (160 * alpha)));
        Render2D.drawBlur(0, 0, screenWidth, screenHeight, 0.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (100 * alpha)), alpha);

        // Liquid Glass Modal Container
        int modalGlassFill = ColorUtil.rgba(16, 14, 26, (int) (235 * alpha));
        int modalGlassBorder = ColorUtil.rgba(255, 255, 255, (int) (45 * alpha));

        Render2D.drawShadow(modalX, modalY, modalW, modalH, 12.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (240 * alpha)));
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 12.0F, 22.0F, modalGlassFill, alpha);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 12.0F, modalGlassFill);
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 12.0F, 1.0F, modalGlassBorder);

        // Top specular line
        Render2D.drawRoundedRect(modalX + 16.0F, modalY + 1.0F, modalW - 32.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (45 * alpha)));

        // Left Accounts List Pane
        float leftW = 245.0F;
        float leftX = modalX + 14.0F;
        float leftY = modalY + 14.0F;

        // Title
        Fonts.drawString(Fonts.SF_MEDIUM, "Аккаунты", leftX, leftY + 2.0F, 10.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

        // Search Bar
        float searchY = leftY + 20.0F;
        float searchW = leftW;
        float searchH = 19.0F;
        int searchCol = searchFocused ? accent : ColorUtil.rgba(255, 255, 255, 35);
        Render2D.drawRoundedRect(leftX, searchY, searchW, searchH, 5.0F, ColorUtil.rgba(22, 26, 42, (int) (220 * alpha)));
        Render2D.drawRoundedOutline(leftX, searchY, searchW, searchH, 5.0F, 0.8F, ColorUtil.applyAlpha(searchCol, alpha));

        boolean blink = (System.currentTimeMillis() / 500L) % 2 == 0;
        String displaySearch = searchFilter.isEmpty() ? "Поиск..." : searchFilter + (searchFocused && blink ? "|" : "");
        Fonts.drawString(Fonts.SF_MEDIUM, displaySearch, leftX + 8.0F, searchY + 5.5F, 6.5F, ColorUtil.multiplyAlpha(searchCol, alpha));

        // Tabs Row (Все / Избранные)
        float tabY = searchY + searchH + 6.0F;
        String[] tabs = {"Все", "Избранные"};
        float tabW = (leftW - 4.0F) / 2.0F;

        for (int i = 0; i < 2; i++) {
            float tx = leftX + i * (tabW + 4.0F);
            boolean sel = (selectedTab == i);
            int tabBg = sel ? ColorUtil.withAlpha(accent, (int) (190 * alpha)) : ColorUtil.rgba(26, 30, 48, (int) (190 * alpha));
            Render2D.drawRoundedRect(tx, tabY, tabW, 15.0F, 4.0F, tabBg);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, tabs[i], tx + tabW / 2.0F, tabY + 4.0F, 6.5F,
                    ColorUtil.applyAlpha(sel ? ColorUtil.WHITE : ColorUtil.rgba(180, 195, 220, 255), alpha));
        }

        // Accounts List Area
        float listY = tabY + 19.0F;
        float listH = modalH - (listY - modalY) - 14.0F;

        List<String> allAccs = AccountManager.getInstance().getAccounts();
        List<String> filtered = new ArrayList<>();
        for (String acc : allAccs) {
            if (!searchFilter.isEmpty() && !acc.toLowerCase().contains(searchFilter.toLowerCase())) continue;
            if (selectedTab == 1 && !AccountManager.getInstance().isFavorite(acc)) continue;
            filtered.add(acc);
        }

        float itemH = 22.0F;
        maxAccountScroll = Math.max(0.0F, filtered.size() * (itemH + 3.0F) - listH);

        Render2D.pushScissor(leftX, listY, leftW, listH);
        float itemY = listY - accountScroll;

        for (String acc : filtered) {
            if (itemY + itemH >= listY && itemY <= listY + listH) {
                boolean active = acc.equalsIgnoreCase(selectedAccount);
                boolean hovered = mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= itemY && mouseY <= itemY + itemH;

                int itemBg = active ? ColorUtil.withAlpha(accent, (int) (160 * alpha))
                        : (hovered ? ColorUtil.rgba(34, 40, 62, (int) (200 * alpha)) : ColorUtil.rgba(22, 26, 42, (int) (170 * alpha)));
                Render2D.drawRoundedRect(leftX, itemY, leftW, itemH, 5.0F, itemBg);
                Render2D.drawRoundedOutline(leftX, itemY, leftW, itemH, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));

                UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + acc).getBytes(StandardCharsets.UTF_8));
                Identifier skin = DefaultPlayerSkin.get(uuid).body().texturePath();
                Render2D.drawHead(skin, leftX + 4.0F, itemY + 3.0F, 16.0F, 3.0F, alpha);

                Fonts.drawString(Fonts.SF_MEDIUM, acc, leftX + 25.0F, itemY + 6.5F, 7.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

                // Star icon for favorites
                boolean isFav = AccountManager.getInstance().isFavorite(acc);
                int starCol = isFav ? ColorUtil.rgba(255, 215, 0, 255) : ColorUtil.rgba(130, 140, 160, 180);
                Fonts.drawString(Fonts.SF_MEDIUM, isFav ? "★" : "☆", leftX + leftW - 14.0F, itemY + 6.5F, 7.5F, ColorUtil.applyAlpha(starCol, alpha));
            }
            itemY += itemH + 3.0F;
        }
        Render2D.popScissor();

        // Right Pane - Selected Account Info
        float rightX = leftX + leftW + 16.0F;
        float rightW = modalW - (rightX - modalX) - 14.0F;

        String targetAcc = selectedAccount.isEmpty() ? this.minecraft.getUser().getName() : selectedAccount;
        UUID targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetAcc).getBytes(StandardCharsets.UTF_8));
        Identifier targetSkin = DefaultPlayerSkin.get(targetUuid).body().texturePath();

        // Avatar & Info
        float headBig = 46.0F;
        float avatarX = rightX + (rightW - headBig) / 2.0F;
        float avatarY = modalY + 34.0F;

        Render2D.drawShadow(avatarX, avatarY, headBig, headBig, 10.0F, 12.0F, ColorUtil.withAlpha(accent, (int) (120 * alpha)));
        Render2D.drawHead(targetSkin, avatarX, avatarY, headBig, 8.0F, alpha);

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, targetAcc, rightX + rightW / 2.0F, avatarY + headBig + 10.0F, 9.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Offline / Session", rightX + rightW / 2.0F, avatarY + headBig + 24.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.rgba(170, 185, 210, 255), alpha));

        // Use Account Button
        float btnW = rightW;
        float btnH = 26.0F;
        float btnX = rightX;
        float btnY = modalY + modalH - btnH - 14.0F;

        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnCol = btnHover ? ColorUtil.withAlpha(accent, (int) (245 * alpha)) : ColorUtil.withAlpha(accent, (int) (195 * alpha));

        Render2D.drawShadow(btnX, btnY, btnW, btnH, 7.0F, 9.0F, ColorUtil.withAlpha(accent, (int) (110 * alpha)));
        Render2D.drawRoundedRect(btnX, btnY, btnW, btnH, 7.0F, btnCol);
        Render2D.drawRoundedOutline(btnX, btnY, btnW, btnH, 7.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (70 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Использовать", btnX + btnW / 2.0F, btnY + 8.0F, 8.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int screenWidth = this.width;
        int screenHeight = this.height;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // 1. Account Modal Interaction
            if (accountModalOpen) {
                float modalW = 430.0F;
                float modalH = 270.0F;
                float modalX = (screenWidth - modalW) / 2.0F;
                float modalY = (screenHeight - modalH) / 2.0F;

                // Close on click outside modal
                if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                    accountModalOpen = false;
                    return true;
                }

                // Left Accounts List click
                float leftW = 245.0F;
                float leftX = modalX + 14.0F;
                float listY = modalY + 14.0F + 20.0F + 19.0F + 6.0F + 15.0F + 4.0F;
                float listH = modalH - (listY - modalY) - 14.0F;

                List<String> allAccs = AccountManager.getInstance().getAccounts();
                List<String> filtered = new ArrayList<>();
                for (String acc : allAccs) {
                    if (!searchFilter.isEmpty() && !acc.toLowerCase().contains(searchFilter.toLowerCase())) continue;
                    if (selectedTab == 1 && !AccountManager.getInstance().isFavorite(acc)) continue;
                    filtered.add(acc);
                }

                float itemH = 22.0F;
                float itemY = listY - accountScroll;

                for (String acc : filtered) {
                    if (itemY + itemH >= listY && itemY <= listY + listH) {
                        if (mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= itemY && mouseY <= itemY + itemH) {
                            if (mouseX >= leftX + leftW - 20.0F) {
                                AccountManager.getInstance().toggleFavorite(acc);
                            } else {
                                this.selectedAccount = acc;
                            }
                            return true;
                        }
                    }
                    itemY += itemH + 3.0F;
                }

                // Account "Использовать" Button
                float rightX = leftX + leftW + 16.0F;
                float rightW = modalW - (rightX - modalX) - 14.0F;
                float btnH = 26.0F;
                float btnX = rightX;
                float btnY = modalY + modalH - btnH - 14.0F;

                if (mouseX >= btnX && mouseX <= btnX + rightW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    if (!selectedAccount.isEmpty()) {
                        AccountManager.getInstance().setActiveAccount(selectedAccount);
                        AccountManager.getInstance().applyActiveSession();
                    }
                    accountModalOpen = false;
                    return true;
                }

                return true;
            }

            // 2. Main Menu Navigation Cards & Buttons
            float cardW = 190.0F;
            float cardH = 124.0F;
            float gap = 12.0F;
            float totalW = cardW * 2.0F + gap;
            float startX = (screenWidth - totalW) / 2.0F;
            float centerY = screenHeight / 2.0F;
            float startY = centerY - 22.0F;

            if (targetScreen == null) {
                // Multiplayer Card (Left)
                float mpX = startX;
                float mpY = startY;
                if (mouseX >= mpX && mouseX <= mpX + cardW && mouseY >= mpY && mouseY <= mpY + cardH) {
                    switchScreen(new JoinMultiplayerScreen(this));
                    return true;
                }

                // Singleplayer Card (Right)
                float spX = startX + cardW + gap;
                float spY = startY;
                if (mouseX >= spX && mouseX <= spX + cardW && mouseY >= spY && mouseY <= spY + cardH) {
                    switchScreen(new SelectWorldScreen(this));
                    return true;
                }

                // Row 2: Accounts & Settings
                float row2Y = startY + cardH + gap;
                float subBtnH = 30.0F;

                // Button 2: Accounts
                float accX = startX;
                if (mouseX >= accX && mouseX <= accX + cardW && mouseY >= row2Y && mouseY <= row2Y + subBtnH) {
                    this.accountModalOpen = true;
                    return true;
                }

                // Button 3: Settings
                float setX = startX + cardW + gap;
                if (mouseX >= setX && mouseX <= setX + cardW && mouseY >= row2Y && mouseY <= row2Y + subBtnH) {
                    switchScreen(new OptionsScreen(this, this.minecraft.options, false));
                    return true;
                }

                // Row 3: Exit
                float row3Y = row2Y + subBtnH + gap;
                if (mouseX >= startX && mouseX <= startX + totalW && mouseY >= row3Y && mouseY <= row3Y + subBtnH) {
                    this.minecraft.stop();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, isLeftClick);
    }
}