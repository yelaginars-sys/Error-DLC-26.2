package error.ui.mainmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import error.account.AccountManager;
import error.event.list.Render2DEvent;
import error.ui.hud.HudManager;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CustomTitleScreen extends Screen {

    private static final Identifier[] BACKGROUNDS = new Identifier[]{
            Identifier.fromNamespaceAndPath("error", "images/ui/title/title.png"),
            Identifier.fromNamespaceAndPath("error", "images/ui/title/title2.png"),
            Identifier.fromNamespaceAndPath("error", "images/ui/title/title3.png"),
            Identifier.fromNamespaceAndPath("error", "images/ui/title/title4.png"),
            Identifier.fromNamespaceAndPath("error", "images/ui/title/title5.png"),
            Identifier.fromNamespaceAndPath("error", "images/ui/title/title6.png")
    };

    private static final String[] WALLPAPER_NAMES = {
            "Золотой час", "Токийский гуль", "Киберпанк", "Закат", "Туман", "Ночной город"
    };

    private static int currentBgIndex = 0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Modal & Widget states
    private boolean accountModalOpen = false;
    private float accountModalAnim = 0.0F;

    private boolean bgSelectorOpen = false;
    private float bgSelectorAnim = 0.0F;

    // Corner Hover Animations
    private float bgHoverAnim = 0.0F;
    private float accountHoverAnim = 0.0F;

    // Transition Alpha
    private float screenAlpha = 0.0F;
    private Screen targetScreen = null;

    // Account Manager State
    private String selectedAccount = "";
    private String searchFilter = "";
    private int selectedTab = 0; // 0: Все, 1: Избранные, 2: В базе
    private boolean searchFocused = false;
    private float accountScroll = 0.0F;
    private float maxAccountScroll = 0.0F;

    // Dock hover animations
    private final float[] dockHoverAnims = new float[5];

    static {
        loadWallpaper();
    }

    public CustomTitleScreen() {
        super(Component.literal("Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        AccountManager.getInstance().applyActiveSession();
        loadWallpaper();
        this.screenAlpha = 0.0F;
        this.targetScreen = null;
        this.selectedAccount = AccountManager.getInstance().getActiveAccount();
    }

    public static void loadWallpaper() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("ErrorDLC/title");
            String json = prefs.get("wallpaper_json", null);
            if (json == null || json.isEmpty()) return;
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("wallpaperIndex")) {
                int index = root.get("wallpaperIndex").getAsInt();
                if (index >= 0 && index < BACKGROUNDS.length) {
                    currentBgIndex = index;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveWallpaper() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("ErrorDLC/title");
            JsonObject root = new JsonObject();
            root.addProperty("wallpaperIndex", currentBgIndex);
            prefs.put("wallpaper_json", GSON.toJson(root));
            prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchScreen(Screen screen) {
        this.targetScreen = screen;
    }

    private double lastMouseX = -1, lastMouseY = -1;
    private long lastMouseMoveTime = 0L;

    public static Identifier getCurrentBgTexture() {
        return BACKGROUNDS[currentBgIndex];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width;
        int screenHeight = this.height;

        // Screen transition fade
        if (this.targetScreen != null) {
            this.screenAlpha = Math.max(0.0F, this.screenAlpha - 0.08F);
            if (this.screenAlpha <= 0.01F) {
                this.minecraft.gui.setScreen(this.targetScreen);
                return;
            }
        } else {
            this.screenAlpha = Math.min(1.0F, this.screenAlpha + 0.08F);
        }

        // Modal animations
        this.accountModalAnim = Mth.clamp(this.accountModalAnim + (accountModalOpen ? 0.08F : -0.08F), 0.0F, 1.0F);
        this.bgSelectorAnim = Mth.clamp(this.bgSelectorAnim + (bgSelectorOpen ? 0.08F : -0.08F), 0.0F, 1.0F);

        // Track Mouse Movement
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.lastMouseMoveTime = System.currentTimeMillis();
        }
        boolean isMouseMoving = (System.currentTimeMillis() - this.lastMouseMoveTime) < 2500L;

        // Corner Hover Checks with Mouse Movement Slide-in
        boolean isBgCornerHovered = !accountModalOpen && targetScreen == null &&
                (isMouseMoving || bgSelectorOpen || (mouseX < 180 && mouseY > screenHeight - 65));
        this.bgHoverAnim = Mth.clamp(this.bgHoverAnim + (isBgCornerHovered ? 0.10F : -0.10F), 0.0F, 1.0F);

        boolean isAccountCornerHovered = !bgSelectorOpen && targetScreen == null &&
                (isMouseMoving || accountModalOpen || (mouseX > screenWidth - 180 && mouseY > screenHeight - 65));
        this.accountHoverAnim = Mth.clamp(this.accountHoverAnim + (isAccountCornerHovered ? 0.10F : -0.10F), 0.0F, 1.0F);

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            // Background Image
            Render2D.drawTexture(BACKGROUNDS[currentBgIndex], 0, 0, screenWidth, screenHeight, 0.0F, 0xFFFFFFFF);

            // Center Clock & Date
            renderCenterClock(screenWidth, screenHeight);

            // Bottom Center Dock Bar
            renderBottomDock(screenWidth, screenHeight, mouseX, mouseY);

            // Bottom Left Hover Widget ("Сменить фон")
            if (this.bgHoverAnim > 0.001F) {
                renderBottomLeftWidget(screenWidth, screenHeight, mouseX, mouseY, this.bgHoverAnim);
            }

            // Bottom Right Hover Widget ("Сменить аккаунт")
            if (this.accountHoverAnim > 0.001F) {
                renderBottomRightWidget(screenWidth, screenHeight, mouseX, mouseY, this.accountHoverAnim);
            }

            // Wallpaper Selector Modal
            if (this.bgSelectorAnim > 0.001F) {
                renderWallpaperModal(extractor, screenWidth, screenHeight, mouseX, mouseY, this.bgSelectorAnim);
            }

            // Account Manager Modal (Photo 2 UI)
            if (this.accountModalAnim > 0.001F) {
                renderAccountModal(screenWidth, screenHeight, mouseX, mouseY, this.accountModalAnim);
            }

            // Transition Overlay
            if (this.screenAlpha < 0.999F) {
                int fadeOverlay = ColorUtil.rgba(0, 0, 0, (int) ((1.0F - this.screenAlpha) * 255));
                Render2D.drawRect(0, 0, screenWidth, screenHeight, fadeOverlay);
            }

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderCenterClock(int screenWidth, int screenHeight) {
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 3.0F;

        // Eye Logo Icon above Date
        Fonts.drawCenteredIcon(IconUse.LOGO, centerX, centerY - 28.0F, 14.0F, Theme.getAccentColor());

        // Date String (Russian format)
        String dateStr = "Среда, 30 сентября";
        try {
            LocalDate now = LocalDate.now();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("EEEE, d MMMM", new Locale("ru"));
            dateStr = now.format(df);
            dateStr = dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1);
        } catch (Exception ignored) {}

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, dateStr, centerX, centerY - 10.0F, 7.5F, ColorUtil.rgba(220, 220, 230, 220));

        // Large Clock HH:mm
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, timeStr, centerX, centerY + 2.0F, 28.0F, ColorUtil.WHITE);

        // Subtitle Greeting
        int hour = LocalTime.now().getHour();
        String greeting = (hour >= 6 && hour < 12) ? "Доброе утро" : (hour >= 12 && hour < 18) ? "Добрый день" : (hour >= 18 && hour < 23) ? "Добрый вечер" : "Доброй ночи";
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, greeting + ", " + this.minecraft.getUser().getName(), centerX, centerY + 34.0F, 7.0F, ColorUtil.rgba(200, 200, 210, 190));
    }

    private void renderBottomDock(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        float btnSize = 22.0F;
        float spacing = 8.0F;
        int count = 5;
        float dockW = count * btnSize + (count + 1) * spacing;
        float dockH = 30.0F;
        float dockX = (screenWidth - dockW) / 2.0F;
        float dockY = screenHeight - dockH - 12.0F;

        // Capsule Background - Liquid Translucent Glass (No Outline)
        Render2D.drawShadow(dockX, dockY, dockW, dockH, 15.0F, 8.0F, ColorUtil.rgba(0, 0, 0, 140));
        Render2D.drawRoundedRect(dockX, dockY, dockW, dockH, 15.0F, ColorUtil.rgba(14, 14, 20, 195));

        IconUse[] icons = {
                IconUse.PERSONS,
                IconUse.GLOBE,
                IconUse.STAFF,
                IconUse.GEAR,
                IconUse.EXIT
        };

        for (int i = 0; i < count; i++) {
            float bx = dockX + spacing + i * (btnSize + spacing);
            float by = dockY + (dockH - btnSize) / 2.0F;
            boolean hovered = !accountModalOpen && !bgSelectorOpen && targetScreen == null &&
                    mouseX >= bx && mouseX <= bx + btnSize && mouseY >= by && mouseY <= by + btnSize;

            dockHoverAnims[i] = Mth.clamp(dockHoverAnims[i] + (hovered ? 0.14F : -0.14F), 0.0F, 1.0F);
            float hAnim = dockHoverAnims[i];

            int btnBg = ColorUtil.lerp(ColorUtil.rgba(25, 25, 35, 150), Theme.getAccentWithAlpha(190), hAnim);
            Render2D.drawRoundedRect(bx, by, btnSize, btnSize, 11.0F, btnBg);

            int iconCol = ColorUtil.lerp(ColorUtil.rgba(220, 220, 230, 220), ColorUtil.WHITE, hAnim);
            Fonts.drawCenteredIcon(icons[i], bx + btnSize / 2.0F, by + (btnSize - 10.0F) / 2.0F, 10.0F, iconCol);
        }
    }

    private void renderBottomLeftWidget(int screenWidth, int screenHeight, int mouseX, int mouseY, float alpha) {
        float w = 135.0F;
        float h = 26.0F;
        float x = 12.0F - (1.0F - alpha) * 160.0F;
        float y = screenHeight - h - 12.0F;

        int bgColor = ColorUtil.rgba(14, 14, 20, (int) (195 * alpha));
        int shadowColor = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));

        Render2D.drawShadow(x, y, w, h, 6.0F, 6.0F, shadowColor);
        Render2D.drawRoundedRect(x, y, w, h, 6.0F, bgColor);

        // Preview thumbnail icon
        float thumbW = 20.0F;
        float thumbH = 18.0F;
        float thumbX = x + 4.0F;
        float thumbY = y + (h - thumbH) / 2.0F;
        Render2D.drawTexture(BACKGROUNDS[currentBgIndex], thumbX, thumbY, thumbW, thumbH, 3.0F, ColorUtil.applyAlpha(0xFFFFFFFF, alpha));

        // Name & Button subtitle
        String bgName = WALLPAPER_NAMES[currentBgIndex];
        Fonts.drawString(Fonts.SF_MEDIUM, bgName, thumbX + thumbW + 5.0F, y + 4.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Сменить фон", thumbX + thumbW + 5.0F, y + 13.5F, 5.5F, ColorUtil.applyAlpha(Theme.getAccentColor(), alpha));
    }

    private void renderBottomRightWidget(int screenWidth, int screenHeight, int mouseX, int mouseY, float alpha) {
        String curUser = this.minecraft.getUser().getName();
        float w = 135.0F;
        float h = 26.0F;
        float x = screenWidth - w - 12.0F + (1.0F - alpha) * 160.0F;
        float y = screenHeight - h - 12.0F;

        int bgColor = ColorUtil.rgba(14, 14, 20, (int) (195 * alpha));
        int shadowColor = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));

        Render2D.drawShadow(x, y, w, h, 6.0F, 6.0F, shadowColor);
        Render2D.drawRoundedRect(x, y, w, h, 6.0F, bgColor);

        // Player Head
        float headSize = 18.0F;
        float headX = x + 5.0F;
        float headY = y + (h - headSize) / 2.0F;

        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + curUser).getBytes(StandardCharsets.UTF_8));
        Identifier skin = DefaultPlayerSkin.get(uuid).body().texturePath();
        Render2D.drawHead(skin, headX, headY, headSize, 3.0F, alpha);

        // Username & Subtitle
        Fonts.drawString(Fonts.SF_MEDIUM, curUser, headX + headSize + 5.0F, y + 4.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Сменить аккаунт", headX + headSize + 5.0F, y + 13.5F, 5.5F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 190, 255), alpha));
    }

    private void renderWallpaperModal(GuiGraphicsExtractor extractor, int screenWidth, int screenHeight, int mouseX, int mouseY, float alpha) {
        float modalW = 180.0F;
        float modalH = 110.0F;
        float modalX = 12.0F;
        float modalY = screenHeight - modalH - 44.0F;

        Render2D.drawShadow(modalX, modalY, modalW, modalH, 8.0F, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alpha)));
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 8.0F, ColorUtil.multiplyAlpha(0xEE111218, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Выбор фона", modalX + 12.0F, modalY + 8.0F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, alpha));

        float previewW = 124.0F;
        float previewH = 70.0F;
        float previewX = modalX + (modalW - previewW) / 2.0F;
        float previewY = modalY + 22.0F;

        Render2D.drawTexture(BACKGROUNDS[currentBgIndex], previewX, previewY, previewW, previewH, 4.0F, ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha));

        float arrowSize = 16.0F;
        float arrowY = previewY + (previewH - arrowSize) / 2.0F;

        float leftArrowX = previewX - arrowSize - 4.0F;
        boolean leftHover = mouseX >= leftArrowX && mouseX <= leftArrowX + arrowSize && mouseY >= arrowY && mouseY <= arrowY + arrowSize;
        Render2D.drawRoundedRect(leftArrowX, arrowY, arrowSize, arrowSize, 4.0F, ColorUtil.multiplyAlpha(leftHover ? 0xFF2A2D3D : 0x551E202C, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "<", leftArrowX + 5.0F, arrowY + 3.0F, 8.0F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));

        float rightArrowX = previewX + previewW + 4.0F;
        boolean rightHover = mouseX >= rightArrowX && mouseX <= rightArrowX + arrowSize && mouseY >= arrowY && mouseY <= arrowY + arrowSize;
        Render2D.drawRoundedRect(rightArrowX, arrowY, arrowSize, arrowSize, 4.0F, ColorUtil.multiplyAlpha(rightHover ? 0xFF2A2D3D : 0x551E202C, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, ">", rightArrowX + 5.0F, arrowY + 3.0F, 8.0F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));

        String pageInfo = WALLPAPER_NAMES[currentBgIndex] + " (" + (currentBgIndex + 1) + "/" + BACKGROUNDS.length + ")";
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, pageInfo, modalX + (modalW / 2.0F), previewY + previewH + 4.0F, 6.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
    }

    private void renderAccountModal(int screenWidth, int screenHeight, int mouseX, int mouseY, float alpha) {
        // Dark Backdrop
        Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.multiplyAlpha(0x99000000, alpha));

        float modalW = 460.0F;
        float modalH = 270.0F;
        float modalX = (screenWidth - modalW) / 2.0F;
        float modalY = (screenHeight - modalH) / 2.0F;

        Render2D.drawShadow(modalX, modalY, modalW, modalH, 10.0F, 8.0F, ColorUtil.rgba(0, 0, 0, (int) (180 * alpha)));
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 10.0F, ColorUtil.multiplyAlpha(0xEE111218, alpha));

        // LEFT PANE (Account List & Controls)
        float leftW = 260.0F;
        float leftX = modalX + 10.0F;
        float leftY = modalY + 10.0F;

        // Header: "Аккаунты" & stats
        List<String> allAccounts = AccountManager.getInstance().getAccounts();
        Fonts.drawString(Fonts.SF_MEDIUM, "Аккаунты", leftX + 4.0F, leftY + 2.0F, 10.0F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
        String statsStr = "Сохранено: " + allAccounts.size();
        Fonts.drawString(Fonts.SF_MEDIUM, statsStr, leftX + 65.0F, leftY + 5.0F, 5.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));

        // Search Bar
        float searchY = leftY + 18.0F;
        float searchW = leftW - 8.0F;
        float searchH = 18.0F;
        Render2D.drawRoundedRect(leftX, searchY, searchW, searchH, 4.0F, ColorUtil.multiplyAlpha(0x551E202C, alpha));
        if (searchFocused) {
            Render2D.drawRoundedOutline(leftX, searchY, searchW, searchH, 4.0F, 1.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha));
        }
        boolean blink = (System.currentTimeMillis() / 450) % 2 == 0;
        String displaySearch = searchFilter.isEmpty() ? "Поиск по нику..." : searchFilter + (searchFocused && blink ? "|" : "");
        int searchCol = searchFilter.isEmpty() ? 0xFF65687A : ColorUtil.WHITE;
        Fonts.drawString(Fonts.SF_MEDIUM, displaySearch, leftX + 6.0F, searchY + 5.0F, 6.0F, ColorUtil.multiplyAlpha(searchCol, alpha));

        // Filter Tabs: Все (X) | Избранные (Y)
        float tabY = searchY + searchH + 6.0F;
        String[] tabs = {"Все (" + allAccounts.size() + ")", "Избранные"};
        float tabW = 65.0F;
        for (int i = 0; i < tabs.length; i++) {
            float tx = leftX + i * (tabW + 4.0F);
            boolean isSel = selectedTab == i;
            int tabBg = isSel ? Theme.getAccentWithAlpha(180) : ColorUtil.rgba(28, 30, 40, 160);
            Render2D.drawRoundedRect(tx, tabY, tabW, 14.0F, 3.0F, ColorUtil.multiplyAlpha(tabBg, alpha));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, tabs[i], tx + tabW / 2.0F, tabY + 3.5F, 5.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
        }

        // Account List Box
        float listY = tabY + 18.0F;
        float listH = 150.0F;
        Render2D.pushScissor(leftX, listY, searchW, listH);

        List<String> sortedAccs = AccountManager.getInstance().getSortedAccounts();
        if (selectedTab == 1) {
            sortedAccs.removeIf(a -> !AccountManager.getInstance().isFavorite(a));
        }
        if (!searchFilter.trim().isEmpty()) {
            sortedAccs.removeIf(a -> !a.toLowerCase().contains(searchFilter.trim().toLowerCase()));
        }

        String curUser = this.minecraft.getUser().getName();
        float cardH = 22.0F;
        float cardY = listY - this.accountScroll;

        for (String acc : sortedAccs) {
            boolean isCur = acc.equalsIgnoreCase(curUser);
            boolean isSel = acc.equalsIgnoreCase(selectedAccount);
            boolean isFav = AccountManager.getInstance().isFavorite(acc);
            boolean inScissor = mouseY >= listY && mouseY <= listY + listH;
            boolean hovered = inScissor && mouseX >= leftX && mouseX <= leftX + searchW && mouseY >= cardY && mouseY <= cardY + cardH;

            int cardBg = isSel ? Theme.getAccentWithAlpha(160) : (hovered ? ColorUtil.rgba(35, 37, 50, 180) : ColorUtil.rgba(20, 22, 30, 140));
            Render2D.drawRoundedRect(leftX, cardY, searchW, cardH, 4.0F, ColorUtil.multiplyAlpha(cardBg, alpha));

            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + acc).getBytes(StandardCharsets.UTF_8));
            Identifier skin = DefaultPlayerSkin.get(uuid).body().texturePath();
            Render2D.drawHead(skin, leftX + 4.0F, cardY + 3.0F, 16.0F, 2.0F, alpha);

            Fonts.drawString(Fonts.SF_MEDIUM, acc, leftX + 24.0F, cardY + 6.5F, 6.5F, ColorUtil.multiplyAlpha(isCur ? Theme.getAccentColor() : ColorUtil.WHITE, alpha));

            float starX = leftX + searchW - 16.0F;
            int starCol = isFav ? 0xFFFFD700 : ColorUtil.rgba(120, 120, 130, 255);
            Fonts.drawIcon(IconUse.STAR, starX, cardY + 6.0F, 8.0F, ColorUtil.multiplyAlpha(starCol, alpha));

            cardY += cardH + 3.0F;
        }

        Render2D.popScissor();
        this.maxAccountScroll = Math.max(0.0F, (cardY + this.accountScroll) - listY - listH);

        // Bottom Action Controls (Новый ник + Добавить + Случайный)
        float botY = listY + listH + 6.0F;
        float addBtnW = 75.0F;
        float randBtnW = 65.0F;
        int pinkBtnColor = ColorUtil.rgba(235, 80, 140, 255);

        Render2D.drawRoundedRect(leftX, botY, addBtnW, 16.0F, 4.0F, ColorUtil.multiplyAlpha(pinkBtnColor, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "+ Добавить", leftX + addBtnW / 2.0F, botY + 4.0F, 6.0F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));

        Render2D.drawRoundedRect(leftX + addBtnW + 6.0F, botY, randBtnW, 16.0F, 4.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(35, 37, 48, 200), alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Случайный", leftX + addBtnW + 6.0F + randBtnW / 2.0F, botY + 4.0F, 5.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));

        // RIGHT PANE (Selected Account Info & Skin Display)
        float rightX = modalX + leftW + 15.0F;
        float rightW = modalW - leftW - 25.0F;

        String targetAcc = selectedAccount.isEmpty() ? curUser : selectedAccount;

        // Top Status Badge: "Сейчас: <nick>"
        Fonts.drawString(Fonts.SF_MEDIUM, "Сейчас: " + targetAcc, rightX, leftY + 2.0F, 6.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));

        // Skin Card Box
        float skinBoxY = leftY + 18.0F;
        float skinBoxH = 150.0F;
        Render2D.drawRoundedRect(rightX, skinBoxY, rightW, skinBoxH, 6.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(20, 22, 30, 160), alpha));

        UUID targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetAcc).getBytes(StandardCharsets.UTF_8));
        Identifier targetSkin = DefaultPlayerSkin.get(targetUuid).body().texturePath();
        Render2D.drawHead(targetSkin, rightX + (rightW - 48.0F) / 2.0F, skinBoxY + 10.0F, 48.0F, 4.0F, alpha);

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, targetAcc, rightX + rightW / 2.0F, skinBoxY + 64.0F, 7.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "В сети", rightX + rightW / 2.0F, skinBoxY + 74.0F, 5.5F, ColorUtil.multiplyAlpha(ColorUtil.rgba(65, 220, 120, 255), alpha));

        float infoY = skinBoxY + 86.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Добавлен: 30 сентября", rightX + 6.0F, infoY, 5.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "Последний сервер: mail.su.fun", rightX + 6.0F, infoY + 8.0F, 5.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, "UUID: " + targetUuid.toString().substring(0, 13) + "...", rightX + 6.0F, infoY + 16.0F, 5.0F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));

        // Big Green Action Button: "Войти в этот аккаунт"
        float loginBtnY = skinBoxY + skinBoxH + 8.0F;
        float loginBtnH = 20.0F;
        int greenBtnCol = ColorUtil.rgba(45, 180, 95, 255);
        Render2D.drawRoundedRect(rightX, loginBtnY, rightW, loginBtnH, 5.0F, ColorUtil.multiplyAlpha(greenBtnCol, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Войти в этот аккаунт", rightX + rightW / 2.0F, loginBtnY + 5.5F, 6.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));

        // Bottom Action Buttons: Удалить
        float delBtnY = loginBtnY + loginBtnH + 6.0F;
        Render2D.drawRoundedRect(rightX, delBtnY, rightW, 16.0F, 4.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(200, 50, 50, 200), alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Удалить аккаунт", rightX + rightW / 2.0F, delBtnY + 4.0F, 5.5F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || this.targetScreen != null) {
            return super.mouseClicked(event, bl);
        }

        double mouseX = event.x();
        double mouseY = event.y();
        int screenWidth = this.width;
        int screenHeight = this.height;

        // Corner Hover Clicks
        if (!accountModalOpen && !bgSelectorOpen) {
            // Left Corner (Background Widget)
            if (mouseX < 160 && mouseY > screenHeight - 50) {
                this.bgSelectorOpen = !this.bgSelectorOpen;
                return true;
            }
            // Right Corner (Account Widget)
            if (mouseX > screenWidth - 170 && mouseY > screenHeight - 50) {
                this.accountModalOpen = true;
                this.bgSelectorOpen = false;
                return true;
            }
        }

        // Bottom Dock Clicks
        float btnSize = 22.0F;
        float spacing = 8.0F;
        int count = 5;
        float dockW = count * btnSize + (count + 1) * spacing;
        float dockH = 30.0F;
        float dockX = (screenWidth - dockW) / 2.0F;
        float dockY = screenHeight - dockH - 12.0F;

        if (!accountModalOpen && !bgSelectorOpen && mouseY >= dockY && mouseY <= dockY + dockH) {
            for (int i = 0; i < count; i++) {
                float bx = dockX + spacing + i * (btnSize + spacing);
                if (mouseX >= bx && mouseX <= bx + btnSize) {
                    switch (i) {
                        case 0 -> switchScreen(new SelectWorldScreen(this));
                        case 1 -> switchScreen(new JoinMultiplayerScreen(this));
                        case 2 -> {
                            this.accountModalOpen = true;
                            this.bgSelectorOpen = false;
                        }
                        case 3 -> switchScreen(new OptionsScreen(this, this.minecraft.options, false));
                        case 4 -> switchScreen(new ConfirmScreen(
                                (confirmed) -> {
                                    if (confirmed) this.minecraft.stop();
                                    else this.minecraft.gui.setScreen(this);
                                },
                                Component.literal(Localization.get("Quit")),
                                Component.literal(Localization.get("Are you sure you want to quit?"))
                        ));
                    }
                    return true;
                }
            }
        }

        // Wallpaper Selector Clicks
        if (this.bgSelectorOpen) {
            float modalW = 180.0F;
            float modalH = 110.0F;
            float modalX = 12.0F;
            float modalY = screenHeight - modalH - 44.0F;

            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                this.bgSelectorOpen = false;
            } else {
                float previewW = 124.0F;
                float previewH = 70.0F;
                float previewX = modalX + (modalW - previewW) / 2.0F;
                float previewY = modalY + 22.0F;
                float arrowSize = 16.0F;
                float arrowY = previewY + (previewH - arrowSize) / 2.0F;

                float leftArrowX = previewX - arrowSize - 4.0F;
                if (mouseX >= leftArrowX && mouseX <= leftArrowX + arrowSize && mouseY >= arrowY && mouseY <= arrowY + arrowSize) {
                    currentBgIndex = (currentBgIndex - 1 + BACKGROUNDS.length) % BACKGROUNDS.length;
                    saveWallpaper();
                    return true;
                }

                float rightArrowX = previewX + previewW + 4.0F;
                if (mouseX >= rightArrowX && mouseX <= rightArrowX + arrowSize && mouseY >= arrowY && mouseY <= arrowY + arrowSize) {
                    currentBgIndex = (currentBgIndex + 1) % BACKGROUNDS.length;
                    saveWallpaper();
                    return true;
                }
                return true;
            }
        }

        // Account Modal Clicks
        if (this.accountModalOpen) {
            float modalW = 460.0F;
            float modalH = 270.0F;
            float modalX = (screenWidth - modalW) / 2.0F;
            float modalY = (screenHeight - modalH) / 2.0F;

            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                this.accountModalOpen = false;
                this.searchFocused = false;
                return true;
            }

            float leftW = 260.0F;
            float leftX = modalX + 10.0F;
            float leftY = modalY + 10.0F;
            float searchY = leftY + 18.0F;
            float searchW = leftW - 8.0F;
            float searchH = 18.0F;

            // Search Bar Focus
            if (mouseX >= leftX && mouseX <= leftX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Tabs
            float tabY = searchY + searchH + 6.0F;
            float tabW = 65.0F;
            for (int i = 0; i < 2; i++) {
                float tx = leftX + i * (tabW + 4.0F);
                if (mouseX >= tx && mouseX <= tx + tabW && mouseY >= tabY && mouseY <= tabY + 14.0F) {
                    this.selectedTab = i;
                    return true;
                }
            }

            // Account List Item Select & Favorite Toggle
            float listY = tabY + 18.0F;
            float listH = 150.0F;
            if (mouseY >= listY && mouseY <= listY + listH && mouseX >= leftX && mouseX <= leftX + searchW) {
                List<String> sortedAccs = AccountManager.getInstance().getSortedAccounts();
                if (selectedTab == 1) {
                    sortedAccs.removeIf(a -> !AccountManager.getInstance().isFavorite(a));
                }
                if (!searchFilter.trim().isEmpty()) {
                    sortedAccs.removeIf(a -> !a.toLowerCase().contains(searchFilter.trim().toLowerCase()));
                }

                float cardH = 22.0F;
                float cardY = listY - this.accountScroll;

                for (String acc : sortedAccs) {
                    if (mouseY >= cardY && mouseY <= cardY + cardH) {
                        float starX = leftX + searchW - 16.0F;
                        if (mouseX >= starX && mouseX <= starX + 14) {
                            AccountManager.getInstance().toggleFavorite(acc);
                            return true;
                        }

                        this.selectedAccount = acc;
                        return true;
                    }
                    cardY += cardH + 3.0F;
                }
            }

            // Bottom Add & Random Buttons
            float botY = listY + listH + 6.0F;
            float addBtnW = 75.0F;
            float randBtnW = 65.0F;
            if (mouseY >= botY && mouseY <= botY + 16.0F) {
                if (mouseX >= leftX && mouseX <= leftX + addBtnW) {
                    // Add Button
                    if (!searchFilter.trim().isEmpty()) {
                        String newAcc = searchFilter.trim();
                        AccountManager.getInstance().addAccount(newAcc);
                        AccountManager.getInstance().setSession(newAcc);
                        this.selectedAccount = newAcc;
                        this.searchFilter = "";
                    }
                    return true;
                }
                if (mouseX >= leftX + addBtnW + 6.0F && mouseX <= leftX + addBtnW + 6.0F + randBtnW) {
                    // Random Button
                    String randName = "User_" + (100 + new Random().nextInt(900));
                    AccountManager.getInstance().addAccount(randName);
                    AccountManager.getInstance().setSession(randName);
                    this.selectedAccount = randName;
                    return true;
                }
            }

            // Right Pane Action Buttons (Login / Delete)
            float rightX = modalX + leftW + 15.0F;
            float rightW = modalW - leftW - 25.0F;
            float skinBoxY = leftY + 18.0F;
            float skinBoxH = 150.0F;
            float loginBtnY = skinBoxY + skinBoxH + 8.0F;
            float loginBtnH = 20.0F;
            float delBtnY = loginBtnY + loginBtnH + 6.0F;

            // Login Button
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= loginBtnY && mouseY <= loginBtnY + loginBtnH) {
                if (!selectedAccount.isEmpty()) {
                    AccountManager.getInstance().setSession(selectedAccount);
                }
                return true;
            }

            // Delete Button
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= delBtnY && mouseY <= delBtnY + 16.0F) {
                if (!selectedAccount.isEmpty()) {
                    AccountManager.getInstance().removeAccount(selectedAccount);
                    this.selectedAccount = AccountManager.getInstance().getActiveAccount();
                }
                return true;
            }

            return true;
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.accountModalOpen) {
            this.accountScroll = Mth.clamp(this.accountScroll - (float) verticalAmount * 16.0F, 0.0F, this.maxAccountScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (this.bgSelectorOpen && (event.isEscape() || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
            this.bgSelectorOpen = false;
            return true;
        }

        if (this.accountModalOpen) {
            if (event.isEscape() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.accountModalOpen = false;
                this.searchFocused = false;
                return true;
            }

            if (this.searchFocused) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    if (!searchFilter.trim().isEmpty()) {
                        String newAcc = searchFilter.trim();
                        AccountManager.getInstance().addAccount(newAcc);
                        AccountManager.getInstance().setSession(newAcc);
                        this.selectedAccount = newAcc;
                        this.searchFilter = "";
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.searchFilter.isEmpty()) {
                    this.searchFilter = this.searchFilter.substring(0, this.searchFilter.length() - 1);
                    return true;
                }
                if (event.hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
                    String paste = this.minecraft.keyboardHandler.getClipboard();
                    if (paste != null) {
                        paste = paste.replaceAll("[^a-zA-Z0-9_]", "");
                        String res = this.searchFilter + paste;
                        if (res.length() > 16) res = res.substring(0, 16);
                        this.searchFilter = res;
                    }
                    return true;
                }

                if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                    if (this.searchFilter.length() < 16) {
                        char c = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
                        if (event.hasShiftDown()) c = Character.toUpperCase(c);
                        this.searchFilter += c;
                    }
                    return true;
                }

                if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                    if (this.searchFilter.length() < 16) {
                        this.searchFilter += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
                    }
                    return true;
                }

                if (keyCode == GLFW.GLFW_KEY_MINUS && event.hasShiftDown()) {
                    if (this.searchFilter.length() < 16) {
                        this.searchFilter += "_";
                    }
                    return true;
                }
            }
            return true;
        }

        return super.keyPressed(event);
    }
}