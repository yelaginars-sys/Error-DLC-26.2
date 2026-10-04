package error.ui.mainmenu;

import error.account.AccountManager;
import error.util.RenderExtend;
import error.util.client.BackgroundAudioPlayer;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class CustomTitleScreen extends Screen {

    private static final Identifier BG_FALLBACK = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/background.png");
    private static final Identifier SINGLEPLAYER_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/singleplayer.png");
    private static final Identifier MULTIPLAYER_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/multiplayer.png");
    private static final Identifier ACCOUNT_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/accountmanager.png");
    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("client", "textures/hud/logo.png");
    private static final Identifier SPEAKER_ICON = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/speaker.png");
    private static final Identifier SPEAKER_MUTE_ICON = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/speaker_mute.png");

    private static final int TOTAL_VIDEO_FRAMES = 744;
    private static final float VIDEO_FPS = 15.0F;

    // Subtitle caches
    private static String cachedSingleplayerSubRu = "Локальные миры";
    private static String cachedMultiplayerSubRu = "SpookyTime • Загрузка...";
    private static long lastOnlineFetchTime = 0;
    private static boolean isFetchingOnline = false;

    // State
    private float screenAlpha = 1.0F;
    private Screen targetScreen = null;
    private long startTime = System.currentTimeMillis();

    // Volume Slider State
    private boolean draggingVolume = false;

    // Hover animations: 0: Singleplayer, 1: Multiplayer, 2: Accounts
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
        this.startTime = System.currentTimeMillis();

        // Start lopped video background audio
        BackgroundAudioPlayer.getInstance().start();

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
                BackgroundAudioPlayer.getInstance().stop();
                this.minecraft.setScreenAndShow(this.targetScreen);
                return;
            }
        } else {
            this.screenAlpha = Math.min(1.0F, this.screenAlpha + 0.10F);
        }

        // Handle volume slider dragging
        if (draggingVolume) {
            float volX = 14.0F + 22.0F;
            float volW = 66.0F;
            float newVol = Math.clamp((mouseX - volX) / volW, 0.0F, 1.0F);
            BackgroundAudioPlayer.getInstance().setVolume(newVol);
        }

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            // 1. DYNAMIC VIDEO BACKGROUND FRAME RENDER
            long elapsed = Math.max(0L, System.currentTimeMillis() - startTime);
            int frameIndex = (int) ((elapsed / 1000.0D * VIDEO_FPS) % TOTAL_VIDEO_FRAMES) + 1;
            String frameName = String.format(Locale.US, "frame_%04d.png", frameIndex);
            Identifier videoFrameTex = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/frames/" + frameName);

            // Dark base background
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(8, 10, 16, (int) (255 * this.screenAlpha)));

            // High-res static fallback
            Render2D.drawTexture(BG_FALLBACK, 0, 0, screenWidth, screenHeight, ColorUtil.rgba(255, 255, 255, (int) (180 * this.screenAlpha)));

            // Live video frame texture
            Render2D.drawTexture(videoFrameTex, 0, 0, screenWidth, screenHeight, ColorUtil.rgba(255, 255, 255, (int) (245 * this.screenAlpha)));

            // 2. SOFT ATMOSPHERE & DARK GRADIENT OVERLAY
            int topFade = ColorUtil.rgba(4, 6, 12, (int) (55 * this.screenAlpha));
            int botFade = ColorUtil.rgba(4, 6, 12, (int) (105 * this.screenAlpha));
            Render2D.drawGradientRound(0, 0, screenWidth, screenHeight, 0.0F, topFade, topFade, botFade, botFade);
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(6, 8, 14, (int) (30 * this.screenAlpha)));

            // 3. Top-Left Branding & Volume Slider
            drawTopLeftBranding(14.0F, 12.0F, this.screenAlpha);
            drawVolumeSlider(14.0F, 32.0F, mouseX, mouseY, this.screenAlpha);

            // 4. Main Menu Central Greeting & Hero Cards Layout
            renderMainMenuUI(screenWidth, screenHeight, mouseX, mouseY);

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

    private void drawVolumeSlider(float x, float y, int mouseX, int mouseY, float alphaVal) {
        float totalW = 124.0F;
        float totalH = 18.0F;
        float radius = 9.0F;

        // Container Liquid Glass Background & Shadow
        int accent = Theme.getAccentColor();
        Render2D.drawShadow(x, y, totalW, totalH, 6.0F, 8.0F, ColorUtil.rgba(0, 0, 0, (int) (120 * alphaVal)));
        Render2D.drawRoundedRect(x, y, totalW, totalH, radius, ColorUtil.rgba(14, 16, 24, (int) (210 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, totalW, totalH, radius, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (40 * alphaVal)));

        // Sound Icon
        float vol = BackgroundAudioPlayer.getInstance().getVolume();
        Identifier iconTex = vol <= 0.001F ? SPEAKER_MUTE_ICON : SPEAKER_ICON;
        Render2D.drawTexture(iconTex, x + 6.0F, y + 4.0F, 10.0F, 10.0F, ColorUtil.rgba(255, 255, 255, (int) (230 * alphaVal)));

        // Slider Bar Track
        float barX = x + 22.0F;
        float barH = 4.0F;
        float barY = y + (totalH - barH) / 2.0F;
        float barW = 66.0F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, 2.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * alphaVal)));

        float filledW = Math.max(2.0F, barW * vol);
        Render2D.drawRoundedRect(barX, barY, filledW, barH, 2.0F, ColorUtil.withAlpha(accent, (int) (240 * alphaVal)));

        // Slider Knob Circle with Glow
        float knobX = barX + (barW - 7.0F) * vol;
        float knobY = y + (totalH - 7.0F) / 2.0F;
        Render2D.drawShadow(knobX - 1.0F, knobY - 1.0F, 9.0F, 9.0F, 3.0F, 4.0F, ColorUtil.withAlpha(accent, (int) (150 * alphaVal)));
        Render2D.drawRoundedRect(knobX, knobY, 7.0F, 7.0F, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));

        // Volume % Text
        int pct = (int) Math.round(vol * 100.0F);
        Fonts.drawString(Fonts.SF_MEDIUM, pct + "%", x + 94.0F, y + 5.5F, 6.2F, ColorUtil.rgba(215, 220, 240, (int) (230 * alphaVal)));
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

        boolean modalActive = targetScreen != null;

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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int screenWidth = this.width;
        int screenHeight = this.height;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Volume Slider Click / Drag
            float volX = 14.0F;
            float volY = 32.0F;
            float volW = 124.0F;
            float volH = 18.0F;

            if (mouseX >= volX && mouseX <= volX + volW && mouseY >= volY && mouseY <= volY + volH) {
                this.draggingVolume = true;
                float sliderBarX = volX + 22.0F;
                float sliderBarW = 66.0F;
                float newVol = Math.clamp((float) ((mouseX - sliderBarX) / sliderBarW), 0.0F, 1.0F);
                BackgroundAudioPlayer.getInstance().setVolume(newVol);
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

                // Card 2: Accounts -> Opens Exclusive AltManagerScreen
                float accX = startX + (cardW + gap) * 2;
                if (mouseX >= accX && mouseX <= accX + cardW && mouseY >= cardsY && mouseY <= cardsY + cardH) {
                    switchScreen(new AltManagerScreen(this));
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

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.draggingVolume = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        BackgroundAudioPlayer.getInstance().stop();
        super.removed();
    }
}