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
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CustomTitleScreen extends Screen {

    private static final Identifier BG_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/background.png");
    private static final Identifier SINGLEPLAYER_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/singleplayer.png");
    private static final Identifier MULTIPLAYER_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/multiplayer.png");
    private static final Identifier ACCOUNT_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/accountmanager.png");
    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("client", "textures/hud/logo.png");

    // Subtitle caches
    private static String cachedSingleplayerSubRu = "Локальные миры";
    private static String cachedMultiplayerSubRu = "SpookyTime • Загрузка...";
    private static long lastOnlineFetchTime = 0;
    private static boolean isFetchingOnline = false;

    // Transition & Account Modal state
    private boolean accountModalOpen = false;
    private float accountModalAnim = 0.0F;
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

    // Hover animations: 0: Singleplayer, 1: Multiplayer, 2: Accounts, 3: Settings, 4: Exit
    private final float[] heroHoverAnims = new float[3];
    private final float[] pillHoverAnims = new float[2];

    public CustomTitleScreen() {
        super(Component.literal("Main Menu"));
    }

    public static void loadWallpaper() {}

    @Override
    protected void init() {
        super.init();
        AccountManager.getInstance().applyActiveSession();
        this.screenAlpha = 1.0F;
        this.targetScreen = null;
        this.selectedAccount = AccountManager.getInstance().getActiveAccount();
        this.startTime = System.currentTimeMillis();

        loadLastWorldInfo();
        fetchSpookyTimeOnline();
    }

    private void switchScreen(Screen screen) {
        this.targetScreen = screen;
    }

    private String getPlayerUsername() {
        if (this.minecraft != null && this.minecraft.getUser() != null && this.minecraft.getUser().getName() != null) {
            return this.minecraft.getUser().getName();
        }
        return "Player";
    }

    private void loadLastWorldInfo() {
        if (this.minecraft == null) return;
        try {
            LevelStorageSource storage = this.minecraft.getLevelSource();
            if (storage == null) return;
            LevelStorageSource.LevelCandidates levelList = storage.findLevelCandidates();
            if (levelList.isEmpty()) {
                cachedSingleplayerSubRu = "Нет миров • Создать";
                return;
            }

            storage.loadLevelSummaries(levelList).thenAccept(summaries -> {
                if (summaries != null && !summaries.isEmpty()) {
                    List<LevelSummary> sorted = new ArrayList<>(summaries);
                    sorted.sort((a, b) -> Long.compare(b.getLastPlayed(), a.getLastPlayed()));
                    LevelSummary latest = sorted.get(0);

                    String worldName = latest.getLevelName();
                    if (worldName == null || worldName.trim().isEmpty()) {
                        worldName = "Мир";
                    }
                    if (worldName.length() > 14) {
                        worldName = worldName.substring(0, 12) + "…";
                    }

                    long lastPlayed = latest.getLastPlayed();
                    String timeAgoRu = formatTimeAgoRu(lastPlayed);
                    cachedSingleplayerSubRu = worldName + " • " + timeAgoRu;
                } else {
                    cachedSingleplayerSubRu = "Нет миров • Создать";
                }
            }).exceptionally(e -> {
                cachedSingleplayerSubRu = "Локальные миры";
                return null;
            });
        } catch (Exception ignored) {}
    }

    private static String formatTimeAgoRu(long timestamp) {
        if (timestamp <= 0) return "недавно";
        long now = System.currentTimeMillis();
        long diffMs = Math.max(0, now - timestamp);
        long diffSec = diffMs / 1000L;
        long diffMin = diffSec / 60L;
        long diffHours = diffMin / 60L;
        long diffDays = diffHours / 24L;
        long diffMonths = diffDays / 30L;

        if (diffMin < 1) return "только что";
        if (diffMin < 60) return diffMin + " мин. назад";
        if (diffHours < 24) return diffHours + " ч. назад";
        if (diffDays < 30) return diffDays + " дн. назад";
        return diffMonths + " мес. назад";
    }

    private void fetchSpookyTimeOnline() {
        long now = System.currentTimeMillis();
        if (now - lastOnlineFetchTime < 60_000L && !cachedMultiplayerSubRu.contains("Загрузка")) return;
        if (isFetchingOnline) return;
        isFetchingOnline = true;

        CompletableFuture.runAsync(() -> {
            int online = -1;
            String[] endpoints = new String[]{
                    "https://api.mcsrvstat.us/3/play.spookytime.net",
                    "https://api.mcsrvstat.us/3/spookytime.net",
                    "https://api.mcsrvstat.us/3/mc.spookytime.ru"
            };

            for (String urlStr : endpoints) {
                try {
                    java.net.URI uri = java.net.URI.create(urlStr);
                    java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(3))
                            .build();
                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                            .uri(uri)
                            .header("User-Agent", "Mozilla/5.0")
                            .timeout(java.time.Duration.ofSeconds(4))
                            .GET()
                            .build();

                    java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                        if (obj.has("players") && obj.getAsJsonObject("players").has("online")) {
                            online = obj.getAsJsonObject("players").get("online").getAsInt();
                            if (online >= 0) break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            lastOnlineFetchTime = System.currentTimeMillis();
            isFetchingOnline = false;

            if (online >= 0) {
                String formatted = String.format(Locale.US, "%,d", online).replace(',', ' ');
                cachedMultiplayerSubRu = "SpookyTime • " + formatted + " онлайн";
            } else {
                if (cachedMultiplayerSubRu.contains("Загрузка")) {
                    cachedMultiplayerSubRu = "SpookyTime • Серверы";
                }
            }
        });
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected void extractPanorama(GuiGraphicsExtractor extractor, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenHeight = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

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

            // 1. Fullscreen Background Texture with Vignette Gradient
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(6, 8, 12, (int) (255 * this.screenAlpha)));
            Render2D.drawTexture(BG_TEX, 0, 0, screenWidth, screenHeight, ColorUtil.rgba(240, 245, 255, (int) (255 * this.screenAlpha)));
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(5, 8, 14, (int) (45 * this.screenAlpha)));

            // Top and bottom cinematic dark gradient fade
            int topFade = ColorUtil.rgba(3, 5, 8, (int) (45 * this.screenAlpha));
            int botFade = ColorUtil.rgba(3, 5, 8, (int) (95 * this.screenAlpha));
            Render2D.drawGradientRound(0, 0, screenWidth, screenHeight, 0.0F, topFade, topFade, botFade, botFade);

            // 2. Top-Left Branding
            drawTopLeftBranding(14.0F, 12.0F, this.screenAlpha);

            // 3. Main Menu Central Greeting & Hero Cards Layout
            renderMainMenuUI(screenWidth, screenHeight, mouseX, mouseY);

            // 4. Accounts Manager Liquid Glass Modal
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

    private void drawTopLeftBranding(float x, float y, float alphaVal) {
        float logoSize = 14.0F;
        Render2D.drawTexture(LOGO_TEX, x, y, logoSize, logoSize, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));

        float textX = x + logoSize + 6.0F;
        float textY = y + logoSize / 2.0F - 4.0F;
        int textCol = ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", textX, textY, 8.5F, textCol);
    }

    private void renderMainMenuUI(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 2.0F;

        // 1. Center Greeting Header
        String username = getPlayerUsername();
        String title = "С возвращением, " + username;
        String sub = "Выбери, с чего начать";

        float headerY = centerY - 105.0F;
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, title, centerX, headerY, 11.5F, ColorUtil.rgba(255, 255, 255, (int) (250 * this.screenAlpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, sub, centerX, headerY + 16.5F, 6.0F, ColorUtil.rgba(200, 215, 235, (int) (215 * this.screenAlpha)));

        // 2. Three Hero Cards Row (Singleplayer, Multiplayer, Accounts)
        float cardW = 126.0F;
        float cardH = 162.0F;
        float gap = 12.0F;
        float totalW = 3 * cardW + 2 * gap;
        float startX = centerX - totalW / 2.0F;
        float cardsY = centerY - cardH / 2.0F + 10.0F;

        boolean modalActive = accountModalOpen || targetScreen != null;

        // Card 0: Singleplayer
        float spX = startX;
        boolean spHover = !modalActive && mouseX >= spX && mouseX <= spX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH;
        heroHoverAnims[0] = Mth.clamp(heroHoverAnims[0] + (spHover ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderHeroCard(spX, cardsY, cardW, cardH, "Одиночная игра", cachedSingleplayerSubRu, "Играть →", SINGLEPLAYER_TEX, heroHoverAnims[0]);

        // Card 1: Multiplayer
        float mpX = startX + cardW + gap;
        boolean mpHover = !modalActive && mouseX >= mpX && mouseX <= mpX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH;
        heroHoverAnims[1] = Mth.clamp(heroHoverAnims[1] + (mpHover ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderHeroCard(mpX, cardsY, cardW, cardH, "Сетевая игра", cachedMultiplayerSubRu, "Играть →", MULTIPLAYER_TEX, heroHoverAnims[1]);

        // Card 2: Accounts
        float accCount = Math.max(1, AccountManager.getInstance().getAccounts().size());
        String accSub = username + " • " + (int) accCount + " сохранено";
        float accX = startX + (cardW + gap) * 2;
        boolean accHover = !modalActive && mouseX >= accX && mouseX <= accX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH;
        heroHoverAnims[2] = Mth.clamp(heroHoverAnims[2] + (accHover ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderHeroCard(accX, cardsY, cardW, cardH, "Аккаунты", accSub, "Выбрать →", ACCOUNT_TEX, heroHoverAnims[2]);

        // 3. Bottom Action Pills (Settings, Exit)
        float pillY = cardsY + cardH + 16.0F;
        float pillH = 18.0F;

        float setPillW = 86.0F;
        float exitPillW = 68.0F;
        float pillGap = 8.0F;
        float totalPillsW = setPillW + exitPillW + pillGap;
        float pillStartX = centerX - totalPillsW / 2.0F;

        // Settings Pill
        float setPillX = pillStartX;
        boolean setPillHover = !modalActive && mouseX >= setPillX && mouseX <= setPillX + setPillW && mouseY >= pillY && mouseY <= pillY + pillH;
        pillHoverAnims[0] = Mth.clamp(pillHoverAnims[0] + (setPillHover ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderBottomPill(setPillX, pillY, setPillW, pillH, "Настройки", pillHoverAnims[0], false);

        // Exit Pill
        float exitPillX = pillStartX + setPillW + pillGap;
        boolean exitPillHover = !modalActive && mouseX >= exitPillX && mouseX <= exitPillX + exitPillW && mouseY >= pillY && mouseY <= pillY + pillH;
        pillHoverAnims[1] = Mth.clamp(pillHoverAnims[1] + (exitPillHover ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderBottomPill(exitPillX, pillY, exitPillW, pillH, "Выход", pillHoverAnims[1], true);
    }

    private void renderHeroCard(float x, float y, float w, float h, String title, String subtitle, String buttonText, Identifier bannerTex, float hoverAnim) {
        float drawY = y - 3.5F * hoverAnim;
        float radius = 9.0F;

        // Card glass background
        int bgCol = ColorUtil.rgba(18, 20, 28, (int) ((0.75F + 0.10F * hoverAnim) * 255 * this.screenAlpha));
        int borderCol = ColorUtil.rgba(255, 255, 255, (int) ((0.08F + 0.14F * hoverAnim) * 255 * this.screenAlpha));

        Render2D.drawRoundedRect(x, drawY, w, h, radius, bgCol);
        Render2D.drawRoundedOutline(x, drawY, w, h, radius, 0.9F, borderCol);

        // Top Banner Image Container
        float bannerW = w - 12.0F;
        float bannerH = 88.0F;
        float bannerX = x + 6.0F;
        float bannerY = drawY + 6.0F;

        Render2D.drawTexture(bannerTex, bannerX, bannerY, bannerW, bannerH, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * this.screenAlpha)));
        Render2D.drawRoundedOutline(bannerX, bannerY, bannerW, bannerH, 6.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (30 * this.screenAlpha)));

        // Title & Subtitle below banner
        float titleY = drawY + 102.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, title, x + 10.0F, titleY, 7.5F, ColorUtil.rgba(250, 250, 255, (int) (250 * this.screenAlpha)));

        float subY = titleY + 11.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, subtitle, x + 10.0F, subY, 5.2F, ColorUtil.rgba(170, 185, 210, (int) (200 * this.screenAlpha)));

        // Button Pill at bottom of card
        float btnW = w - 20.0F;
        float btnH = 18.0F;
        float btnX = x + 10.0F;
        float btnY = drawY + h - btnH - 8.0F;

        int accent = Theme.getAccentColor();
        int btnBg = hoverAnim > 0.01F ? ColorUtil.withAlpha(accent, (int) ((0.60F + 0.30F * hoverAnim) * 255 * this.screenAlpha))
                : ColorUtil.rgba(30, 34, 48, (int) (220 * this.screenAlpha));

        Render2D.drawRoundedRect(btnX, btnY, btnW, btnH, 5.0F, btnBg);
        Render2D.drawRoundedOutline(btnX, btnY, btnW, btnH, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (40 * this.screenAlpha)));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, buttonText, btnX + btnW / 2.0F, btnY + 4.5F, 6.2F, ColorUtil.rgba(255, 255, 255, (int) (250 * this.screenAlpha)));
    }

    private void renderBottomPill(float x, float y, float w, float h, String text, float hoverAnim, boolean isDanger) {
        float drawY = y - 1.5F * hoverAnim;
        float radius = 5.0F;

        int bgCol;
        int outlineCol;

        if (isDanger) {
            bgCol = ColorUtil.rgba(200, 45, 55, (int) ((0.25F + 0.35F * hoverAnim) * 255 * this.screenAlpha));
            outlineCol = ColorUtil.rgba(240, 60, 70, (int) ((0.20F + 0.40F * hoverAnim) * 255 * this.screenAlpha));
        } else {
            bgCol = ColorUtil.rgba(22, 25, 35, (int) ((0.60F + 0.20F * hoverAnim) * 255 * this.screenAlpha));
            outlineCol = ColorUtil.rgba(255, 255, 255, (int) ((0.08F + 0.16F * hoverAnim) * 255 * this.screenAlpha));
        }

        Render2D.drawRoundedRect(x, drawY, w, h, radius, bgCol);
        Render2D.drawRoundedOutline(x, drawY, w, h, radius, 0.7F, outlineCol);

        int textCol = isDanger ? ColorUtil.rgba(255, 180, 180, (int) (250 * this.screenAlpha))
                : ColorUtil.rgba(245, 245, 250, (int) (245 * this.screenAlpha));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, x + w / 2.0F, drawY + 4.5F, 6.5F, textCol);
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

        Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(0, 0, 0, (int) (160 * alpha)));
        Render2D.drawBlur(0, 0, screenWidth, screenHeight, 0.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (100 * alpha)), alpha);

        int modalGlassFill = ColorUtil.rgba(16, 14, 26, (int) (235 * alpha));
        int modalGlassBorder = ColorUtil.rgba(255, 255, 255, (int) (45 * alpha));

        Render2D.drawShadow(modalX, modalY, modalW, modalH, 12.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (240 * alpha)));
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 12.0F, 22.0F, modalGlassFill, alpha);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 12.0F, modalGlassFill);
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 12.0F, 1.0F, modalGlassBorder);

        Render2D.drawRoundedRect(modalX + 16.0F, modalY + 1.0F, modalW - 32.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (45 * alpha)));

        float leftW = 245.0F;
        float leftX = modalX + 14.0F;
        float leftY = modalY + 14.0F;

        Fonts.drawString(Fonts.SF_MEDIUM, "Аккаунты", leftX, leftY + 2.0F, 10.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

        float searchY = leftY + 20.0F;
        float searchW = leftW;
        float searchH = 19.0F;
        int searchCol = searchFocused ? accent : ColorUtil.rgba(255, 255, 255, 35);
        Render2D.drawRoundedRect(leftX, searchY, searchW, searchH, 5.0F, ColorUtil.rgba(22, 26, 42, (int) (220 * alpha)));
        Render2D.drawRoundedOutline(leftX, searchY, searchW, searchH, 5.0F, 0.8F, ColorUtil.applyAlpha(searchCol, alpha));

        boolean blink = (System.currentTimeMillis() / 500L) % 2 == 0;
        String displaySearch = searchFilter.isEmpty() ? "Поиск..." : searchFilter + (searchFocused && blink ? "|" : "");
        Fonts.drawString(Fonts.SF_MEDIUM, displaySearch, leftX + 8.0F, searchY + 5.5F, 6.5F, ColorUtil.multiplyAlpha(searchCol, alpha));

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

                boolean isFav = AccountManager.getInstance().isFavorite(acc);
                int starCol = isFav ? ColorUtil.rgba(255, 215, 0, 255) : ColorUtil.rgba(130, 140, 160, 180);
                Fonts.drawString(Fonts.SF_MEDIUM, isFav ? "★" : "☆", leftX + leftW - 14.0F, itemY + 6.5F, 7.5F, ColorUtil.applyAlpha(starCol, alpha));
            }
            itemY += itemH + 3.0F;
        }
        Render2D.popScissor();

        float rightX = leftX + leftW + 16.0F;
        float rightW = modalW - (rightX - modalX) - 14.0F;

        String targetAcc = selectedAccount.isEmpty() ? this.minecraft.getUser().getName() : selectedAccount;
        UUID targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetAcc).getBytes(StandardCharsets.UTF_8));
        Identifier targetSkin = DefaultPlayerSkin.get(targetUuid).body().texturePath();

        float headBig = 46.0F;
        float avatarX = rightX + (rightW - headBig) / 2.0F;
        float avatarY = modalY + 34.0F;

        Render2D.drawShadow(avatarX, avatarY, headBig, headBig, 10.0F, 12.0F, ColorUtil.withAlpha(accent, (int) (120 * alpha)));
        Render2D.drawHead(targetSkin, avatarX, avatarY, headBig, 8.0F, alpha);

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, targetAcc, rightX + rightW / 2.0F, avatarY + headBig + 10.0F, 9.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Offline / Session", rightX + rightW / 2.0F, avatarY + headBig + 24.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.rgba(170, 185, 210, 255), alpha));

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
            if (accountModalOpen) {
                float modalW = 430.0F;
                float modalH = 270.0F;
                float modalX = (screenWidth - modalW) / 2.0F;
                float modalY = (screenHeight - modalH) / 2.0F;

                if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                    accountModalOpen = false;
                    return true;
                }

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

            // Main Menu Cards & Pills Interaction
            float centerX = screenWidth / 2.0F;
            float centerY = screenHeight / 2.0F;
            float cardW = 126.0F;
            float cardH = 162.0F;
            float gap = 12.0F;
            float totalW = 3 * cardW + 2 * gap;
            float startX = centerX - totalW / 2.0F;
            float cardsY = centerY - cardH / 2.0F + 10.0F;

            if (targetScreen == null) {
                // Card 0: Singleplayer
                float spX = startX;
                if (mouseX >= spX && mouseX <= spX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH) {
                    switchScreen(new SelectWorldScreen(this));
                    return true;
                }

                // Card 1: Multiplayer
                float mpX = startX + cardW + gap;
                if (mouseX >= mpX && mouseX <= mpX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH) {
                    switchScreen(new JoinMultiplayerScreen(this));
                    return true;
                }

                // Card 2: Accounts
                float accX = startX + (cardW + gap) * 2;
                if (mouseX >= accX && mouseX <= accX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH) {
                    this.accountModalOpen = true;
                    return true;
                }

                // Bottom Action Pills
                float pillY = cardsY + cardH + 16.0F;
                float pillH = 18.0F;
                float setPillW = 86.0F;
                float exitPillW = 68.0F;
                float pillGap = 8.0F;
                float totalPillsW = setPillW + exitPillW + pillGap;
                float pillStartX = centerX - totalPillsW / 2.0F;

                // Settings Pill
                float setPillX = pillStartX;
                if (mouseX >= setPillX && mouseX <= setPillX + setPillW && mouseY >= pillY && mouseY <= pillY + pillH) {
                    switchScreen(new OptionsScreen(this, this.minecraft.options, false));
                    return true;
                }

                // Exit Pill
                float exitPillX = pillStartX + setPillW + pillGap;
                if (mouseX >= exitPillX && mouseX <= exitPillX + exitPillW && mouseY >= pillY && mouseY <= pillY + pillH) {
                    this.minecraft.stop();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, isLeftClick);
    }
}