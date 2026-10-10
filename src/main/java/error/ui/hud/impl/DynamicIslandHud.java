package error.ui.hud.impl;

import com.mojang.blaze3d.platform.NativeImage;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import error.util.render.font.MsdfFont;
import error.util.display.batch.DisplayBatcher;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DynamicIslandHud extends HudElement implements IMinecraft {

    public enum IslandState {
        NORMAL,
        MUSIC,
        NOTIFICATION,
        PVP
    }

    private static final float NORMAL_HEIGHT = 20.0F;
    private static final float NOTIFICATION_HEIGHT = 20.0F;
    private static final float MUSIC_HEIGHT = 28.0F;
    private static final float MUSIC_CHAT_HEIGHT = 30.0F;
    private static final float PVP_HEIGHT = 22.0F;

    private static final float INFO_HEIGHT = 14.0F;
    private static final float GAP_BETWEEN = 6.0F;

    private static final float RADIUS = 8.5F;
    private static final float HEAD_SIZE = 9.0F;
    private static final float HEAD_RADIUS = 4.5F;
    private static final float COMPACT_ART_SIZE = 11.0F;
    private static final float EXPANDED_ART_SIZE = 18.0F;

    private static final long CONTENT_TRANSITION_DURATION = 260L;
    private static final long MUSIC_EXPANDED_HOLD = 3500L;
    private static final long MEDIA_REFRESH_INTERVAL = 2500L;

    // Animations
    private final Animation widthAnimation = new Animation(98.0F, 0.28F);
    private final Animation heightAnimation = new Animation(NORMAL_HEIGHT, 0.28F);
    private final Animation infoWidthAnimation = new Animation(0.0F, 0.25F);
    private final Animation subIslandAlphaAnim = new Animation(0.0F, 0.25F);
    private final Animation yAnimation = new Animation(4.0F, 0.28F);
    private boolean layoutInitialized = false;
    private boolean positionCentered = false;

    // Notification State
    private static volatile String notificationText = "";
    private static volatile boolean notificationPositive = true;
    private static volatile long notificationUntil = 0L;
    private static volatile long notificationScrollStarted = 0L;
    private static volatile long notificationDuration = 2500L;

    public static void showNotification(String text, boolean positive, long durationMs) {
        if (text != null && (text.contains("Подключение") || text.contains("Connecting"))) {
            text = "Подключение...";
        }
        notificationText = text;
        notificationPositive = positive;
        notificationDuration = durationMs;
        long now = System.currentTimeMillis();
        notificationScrollStarted = now;
        notificationUntil = now + durationMs;
    }

    // PvP & Server BossBar State
    private static volatile LerpingBossEvent activePvPEvent = null;
    private static volatile String pvpTitle = "";
    private static volatile int pvpSeconds = -1;
    private static volatile float pvpProgress = 1.0F;
    private static volatile long pvpLastSeen = 0L;
    private static volatile int activeBossBarCount = 0;

    // Media Player State
    private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean mediaPolling = new AtomicBoolean(false);
    private volatile IMediaSession activeMediaSession = null;
    private volatile String trackTitle = "";
    private volatile String trackArtist = "";
    private volatile boolean trackPlaying = false;
    private volatile Identifier artworkTexture = null;
    private volatile int artworkHash = 0;
    private volatile long musicExpandedUntil = 0L;
    private String lastTrackKey = "";
    private long lastMediaRefresh = 0L;
    private long dynamicTextureCounter = 0L;

    // State Transitions
    private IslandState displayedState = IslandState.NORMAL;
    private IslandState previousState = IslandState.NORMAL;
    private long contentTransitionStarted = 0L;

    public static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "textures/logo_nonfone.png");
    public static final Identifier AVATAR_TEX = Identifier.fromNamespaceAndPath("error", "images/avatar.jpg");

    // Media Control Vector Icons
    private static final Identifier MEDIA_PLAY_TEX = Identifier.fromNamespaceAndPath("error", "textures/media/play.png");
    private static final Identifier MEDIA_PAUSE_TEX = Identifier.fromNamespaceAndPath("error", "textures/media/pause.png");
    private static final Identifier MEDIA_PREV_TEX = Identifier.fromNamespaceAndPath("error", "textures/media/prev.png");
    private static final Identifier MEDIA_NEXT_TEX = Identifier.fromNamespaceAndPath("error", "textures/media/next.png");

    // Metrics Display Settings
    private boolean showSubIsland = true;
    private boolean showAvatar = true;
    private boolean showFps = true;
    private boolean showPing = true;
    private boolean showTps = true;
    private boolean showServer = true;

    // Media Control Button Hitboxes
    private float prevButtonX, prevButtonY, prevButtonSize;
    private float playButtonX, playButtonY, playButtonSize;
    private float nextButtonX, nextButtonY, nextButtonSize;

    public DynamicIslandHud() {
        super("dynamic_island", "Dynamic Island", 240.0F, 4.0F, 180.0F, NORMAL_HEIGHT + GAP_BETWEEN + INFO_HEIGHT, true);
    }

    public static void postNotification(String text, boolean positive) {
        if (text == null || text.isBlank()) return;
        String clean = text.replace('\n', ' ').trim();
        if (clean.contains("Подключение") || clean.contains("Connecting")) {
            clean = "Подключение...";
        }
        notificationText = clean;
        notificationPositive = positive;
        long now = System.currentTimeMillis();
        float textWidth = Fonts.SF_MEDIUM.getWidth(notificationText, 8.0F);
        long duration = Math.max(2200L, (long) (textWidth * 26.0F));
        notificationDuration = duration;
        notificationScrollStarted = now;
        notificationUntil = now + duration;
    }

    public static boolean interceptBossBars(Map<UUID, LerpingBossEvent> events) {
        if (events == null || events.isEmpty()) {
            activePvPEvent = null;
            activeBossBarCount = 0;
            return false;
        }

        boolean interceptedPvP = false;
        int nonPvpCount = 0;

        for (LerpingBossEvent event : events.values()) {
            String raw = event.getName().getString();
            String clean = raw.replaceAll("§[0-9a-fk-or]", "").trim();
            String lower = clean.toLowerCase();
            if (lower.contains("бой") || lower.contains("боя") || lower.contains("pvp")
                    || lower.contains("пвп") || lower.contains("combat") || lower.contains("кд")
                    || lower.contains("в бою") || lower.contains("дуэль")) {
                activePvPEvent = event;
                pvpTitle = clean;
                pvpProgress = event.getProgress();
                pvpLastSeen = System.currentTimeMillis();

                int parsed = -1;
                Matcher m = Pattern.compile("(\\d+):(\\d+)").matcher(lower);
                if (m.find()) {
                    parsed = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
                } else {
                    m = Pattern.compile("(\\d+)").matcher(lower);
                    if (m.find()) {
                        parsed = Integer.parseInt(m.group(1));
                    }
                }
                pvpSeconds = parsed;
                interceptedPvP = true;
            } else {
                nonPvpCount++;
            }
        }

        if (!interceptedPvP) {
            activePvPEvent = null;
        }

        activeBossBarCount = interceptedPvP ? 0 : nonPvpCount;
        return interceptedPvP;
    }

    private void refreshMedia() {
        long now = System.currentTimeMillis();
        if (now - lastMediaRefresh < MEDIA_REFRESH_INTERVAL || !mediaPolling.compareAndSet(false, true)) return;
        lastMediaRefresh = now;

        mediaExecutor.submit(() -> {
            try {
                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                if (sessions == null || sessions.isEmpty()) {
                    activeMediaSession = null;
                    trackTitle = "";
                    trackArtist = "";
                    trackPlaying = false;
                    return;
                }

                IMediaSession selected = null;
                for (IMediaSession s : sessions) {
                    if (s == null || s.getMedia() == null) continue;
                    MediaInfo m = s.getMedia();
                    if (m.getTitle() == null || m.getTitle().isBlank()) continue;
                    if (selected == null || m.getPlaying()) {
                        selected = s;
                    }
                    if (m.getPlaying()) break;
                }

                if (selected == null || selected.getMedia() == null) {
                    activeMediaSession = null;
                    trackTitle = "";
                    trackArtist = "";
                    trackPlaying = false;
                    return;
                }

                MediaInfo media = selected.getMedia();
                String newTitle = media.getTitle().trim();
                String newArtist = media.getArtist() == null ? "" : media.getArtist().trim();
                String key = newTitle + "\n" + newArtist;

                if (!key.equals(lastTrackKey)) {
                    lastTrackKey = key;
                    musicExpandedUntil = System.currentTimeMillis() + CONTENT_TRANSITION_DURATION + MUSIC_EXPANDED_HOLD;
                }

                trackTitle = newTitle;
                trackArtist = newArtist;
                trackPlaying = media.getPlaying();
                activeMediaSession = selected;

                updateArtwork(media.getArtworkPng());
            } catch (Throwable ignored) {
                activeMediaSession = null;
                trackTitle = "";
                trackArtist = "";
                trackPlaying = false;
            } finally {
                mediaPolling.set(false);
            }
        });
    }

    private void updateArtwork(byte[] artworkPng) {
        if (artworkPng == null || artworkPng.length == 0) {
            artworkTexture = null;
            artworkHash = 0;
            return;
        }

        int hash = Arrays.hashCode(artworkPng);
        if (hash == artworkHash) return;
        artworkHash = hash;

        mc.execute(() -> {
            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(artworkPng));
                Identifier id = Identifier.fromNamespaceAndPath("error", "island_art_" + (++dynamicTextureCounter));
                DynamicTexture texture = new DynamicTexture(() -> id.toString(), image);
                mc.getTextureManager().register(id, texture);
                this.artworkTexture = id;
            } catch (Throwable ignored) {
                this.artworkTexture = null;
            }
        });
    }

    private boolean isPvPActive(long now) {
        return activePvPEvent != null || (now - pvpLastSeen < 1200L && !pvpTitle.isEmpty());
    }

    private boolean hasTrack() {
        return !trackTitle.isBlank();
    }

    private boolean isInteractiveScreen() {
        return mc.gui != null && (mc.gui.screen() instanceof ChatScreen || mc.gui.screen() instanceof error.ui.clickgui.LiquidClickGui);
    }

    private boolean shouldShowExpandedMusic(long now) {
        return hasTrack() && (now < musicExpandedUntil || isInteractiveScreen());
    }

    private void updateContentState(IslandState nextState, long now) {
        if (nextState == this.displayedState) return;
        this.previousState = this.displayedState;
        this.displayedState = nextState;
        this.contentTransitionStarted = now;
    }

    private float getTargetWidth(IslandState state) {
        boolean chatOpen = isInteractiveScreen();

        return switch (state) {
            case PVP -> {
                String badgeText = pvpSeconds >= 0 ? (pvpSeconds + "s") : "PVP";
                float badgeW = Fonts.SF_MEDIUM.getWidth(badgeText, 6.5F) + 6.0F;
                float titleW = Fonts.SF_MEDIUM.getWidth("Режим PvP", 7.5F);
                yield Math.max(95.0F, 22.0F + badgeW + 6.0F + titleW + 10.0F);
            }
            case NOTIFICATION -> {
                float textW = Fonts.SF_MEDIUM.getWidth(notificationText, 8.0F);
                yield Math.max(95.0F, Math.min(270.0F, textW + 36.0F));
            }
            case MUSIC -> {
                float titleW = Fonts.SF_MEDIUM.getWidth(trackTitle, 8.0F);
                float artistW = Fonts.SF_MEDIUM.getWidth(trackArtist, 6.5F);
                float baseW = chatOpen ? 220.0F : 190.0F;
                float maxW = chatOpen ? 280.0F : 255.0F;
                float pad = chatOpen ? 100.0F : 80.0F;
                yield Math.max(baseW, Math.min(maxW, Math.max(titleW, artistW) + pad));
            }
            case NORMAL -> {
                float logoSize = 13.0F;
                float brandTextW = Fonts.SF_MEDIUM.getWidth("Error DLC ", 8.0F) + Fonts.SF_MEDIUM.getWidth("26.2", 8.0F);
                float brandW = logoSize + 4.5F + brandTextW;
                float mediaW = hasTrack() ? (COMPACT_ART_SIZE + 20.0F) : 0.0F;
                yield brandW + mediaW + 18.0F;
            }
        };
    }

    private float getTargetHeight(IslandState state) {
        return switch (state) {
            case PVP -> PVP_HEIGHT;
            case NOTIFICATION -> NOTIFICATION_HEIGHT;
            case MUSIC -> isInteractiveScreen() ? MUSIC_CHAT_HEIGHT : MUSIC_HEIGHT;
            case NORMAL -> NORMAL_HEIGHT;
        };
    }

    private float calculateInfoWidth() {
        if (!showSubIsland) return 0.0F;

        int activeItems = 0;
        float totalW = 12.0F;

        if (showAvatar) {
            totalW += HEAD_SIZE;
            activeItems++;
        }
        if (showFps) {
            totalW += Fonts.SF_MEDIUM.getWidth(mc.getFps() + " fps", 7.0F);
            activeItems++;
        }
        if (showPing) {
            totalW += Fonts.SF_MEDIUM.getWidth(getPingText() + " ms", 7.0F);
            activeItems++;
        }
        if (showTps) {
            totalW += Fonts.SF_MEDIUM.getWidth("20.0 tps", 7.0F);
            activeItems++;
        }
        if (showServer) {
            totalW += Fonts.SF_MEDIUM.getWidth(getServerAddress(), 7.0F);
            activeItems++;
        }

        if (activeItems == 0) return 0.0F;
        totalW += (activeItems - 1) * 10.0F;
        return totalW;
    }

    private int getHudAccent() {
        return error.module.impl.render.Interface.INSTANCE != null ? error.module.impl.render.Interface.INSTANCE.getHudColor() : Theme.getAccentColor();
    }

    @Override
    public boolean shouldRender() {
        if (error.module.impl.render.Interface.INSTANCE == null
                || !error.module.impl.render.Interface.INSTANCE.isEnabled()
                || !error.module.impl.render.Interface.INSTANCE.dynamicIsland.getValue()) {
            return false;
        }
        return enabled && mc.gui != null;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.gui == null) return;
        if (error.module.impl.render.Interface.INSTANCE == null
                || !error.module.impl.render.Interface.INSTANCE.isEnabled()
                || !error.module.impl.render.Interface.INSTANCE.dynamicIsland.getValue()) {
            return;
        }

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        refreshMedia();

        long now = System.currentTimeMillis();

        IslandState targetState;
        if (isPvPActive(now)) {
            targetState = IslandState.PVP;
        } else if (now < notificationUntil) {
            targetState = IslandState.NOTIFICATION;
        } else if (shouldShowExpandedMusic(now)) {
            targetState = IslandState.MUSIC;
        } else {
            targetState = IslandState.NORMAL;
        }

        updateContentState(targetState, now);

        float targetIslandW = getTargetWidth(this.displayedState);
        float targetIslandH = getTargetHeight(this.displayedState);
        float infoW = calculateInfoWidth();
        infoWidthAnimation.setTarget(infoW);
        infoWidthAnimation.update();
        float animatedInfoW = infoWidthAnimation.getValue();

        subIslandAlphaAnim.setTarget(infoW > 0.0F ? 1.0F : 0.0F);
        subIslandAlphaAnim.update();
        float subIslandAlpha = alpha * subIslandAlphaAnim.getValue();

        float maxTotalW = Math.max(targetIslandW, Math.max(infoW, animatedInfoW));
        float totalH = targetIslandH + (subIslandAlpha > 0.01F ? (GAP_BETWEEN + INFO_HEIGHT) : 0.0F);

        if (!layoutInitialized) {
            widthAnimation.setValue(targetIslandW);
            heightAnimation.setValue(targetIslandH);
            infoWidthAnimation.setValue(infoW);
            layoutInitialized = true;
        }

        float defaultY = 4.0F;
        float targetY = (activeBossBarCount > 0 && !dragging) ? (4.0F + (activeBossBarCount * 14.0F)) : defaultY;
        yAnimation.setTarget(targetY);
        yAnimation.update();

        if (mc.getWindow() != null) {
            this.x = (mc.getWindow().getGuiScaledWidth() - maxTotalW) / 2.0F;
            this.y = yAnimation.getValue();
            positionCentered = true;
        }

        widthAnimation.setTarget(targetIslandW);
        widthAnimation.update();
        heightAnimation.setTarget(targetIslandH);
        heightAnimation.update();

        float curIslandW = widthAnimation.getValue();
        float curIslandH = heightAnimation.getValue();

        this.width = maxTotalW;
        this.height = totalH;

        float boundsX = getX();
        float boundsY = dragging ? getY() : yAnimation.getValue();
        float centerX = boundsX + (this.width / 2.0F);

        float islandX = centerX - (curIslandW / 2.0F);
        float islandY = boundsY;

        int themeAccent = getHudAccent();

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor != null && (Theme.isLiquidGlass() || Theme.isNewYear())) {
            float shellRadius = curIslandH / 2.0F;
            Blur.of(islandX, islandY, curIslandW, curIslandH)
                    .radius(Math.round(shellRadius))
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(0, 0, 0, Math.round(75 * alpha)))
                    .alpha(alpha)
                    .render(extractor);

            Color topOutline;
            Color botOutline;
            if (this.displayedState == IslandState.PVP) {
                topOutline = Color.rgba(255, 95, 115, Math.round(180 * alpha));
                botOutline = Color.rgba(215, 35, 55, Math.round(120 * alpha));
            } else if (Theme.isNewYear()) {
                topOutline = Color.rgba(160, 230, 255, Math.round(160 * alpha));
                botOutline = Color.rgba(110, 205, 255, Math.round(110 * alpha));
            } else {
                topOutline = Color.WHITE;
                botOutline = Color.rgba(255, 255, 255, 32);
            }

            Outline.of(islandX, islandY, curIslandW, curIslandH)
                    .radius(Math.round(shellRadius))
                    .thickness(1.0F)
                    .verticalGradient(topOutline, botOutline)
                    .alpha(alpha)
                    .render(extractor);

            if (subIslandAlpha > 0.01F && animatedInfoW > 1.0F) {
                float infoX = centerX - (animatedInfoW / 2.0F);
                float infoY = islandY + curIslandH + GAP_BETWEEN;
                float subRadius = INFO_HEIGHT / 2.0F;

                Blur.of(infoX, infoY, animatedInfoW, INFO_HEIGHT)
                        .radius(Math.round(subRadius))
                        .type(BlurType.KAWASE)
                        .strength(4)
                        .tint(Color.rgba(0, 0, 0, Math.round(75 * subIslandAlpha)))
                        .alpha(subIslandAlpha)
                        .render(extractor);

                Color subTopOutline = Theme.isNewYear() ? Color.rgba(160, 230, 255, Math.round(160 * subIslandAlpha)) : Color.WHITE;
                Color subBotOutline = Theme.isNewYear() ? Color.rgba(110, 205, 255, Math.round(110 * subIslandAlpha)) : Color.rgba(255, 255, 255, 32);
                Outline.of(infoX, infoY, animatedInfoW, INFO_HEIGHT)
                    .radius(Math.round(subRadius))
                    .thickness(1.0F)
                    .verticalGradient(subTopOutline, subBotOutline)
                    .alpha(subIslandAlpha)
                    .render(extractor);
            }

            DisplayBatcher.flush();
        }

        // 1. RENDER TOP DYNAMIC ISLAND
        renderIslandShell(islandX, islandY, curIslandW, curIslandH, themeAccent, alpha);

        float progress = getContentProgress(now);
        boolean transitioning = progress < 1.0F && this.previousState != this.displayedState;

        Render2D.pushScissor(islandX, islandY, curIslandW, curIslandH);
        if (transitioning) {
            float slide = easeOutCubic(progress);
            float prevAlpha = alpha * (1.0F - slide);
            float curAlpha = alpha * slide;
            drawStateContent(this.previousState, islandX - curIslandW * slide * 0.4F, islandY, curIslandW, curIslandH, themeAccent, prevAlpha, now);
            drawStateContent(this.displayedState, islandX + curIslandW * (1.0F - slide) * 0.4F, islandY, curIslandW, curIslandH, themeAccent, curAlpha, now);
        } else {
            drawStateContent(this.displayedState, islandX, islandY, curIslandW, curIslandH, themeAccent, alpha, now);
        }
        Render2D.popScissor();

        // 2. RENDER SUB-ISLAND INFORMATION BAR (Smoothly animated width and alpha)
        if (subIslandAlpha > 0.01F && animatedInfoW > 1.0F) {
            float infoX = centerX - (animatedInfoW / 2.0F);
            float infoY = islandY + curIslandH + GAP_BETWEEN;
            renderSubIslandInfoBar(infoX, infoY, animatedInfoW, INFO_HEIGHT, themeAccent, subIslandAlpha, now);
        }
    }

    private void renderIslandShell(float x, float y, float w, float h, int themeAccent, float alpha) {
        float radius = h / 2.0F;
        float shadowBlur = y < 15.0F ? Math.max(0.0F, Math.min(y - 1.0F, 3.5F)) : 3.5F;

        if (this.displayedState == IslandState.PVP) {
            int pvpGlow = ColorUtil.rgba(255, 35, 55, (int) (60 * alpha));
            int pvpFill = ColorUtil.rgba(24, 7, 12, (int) (235 * alpha));
            int pvpBorder = ColorUtil.rgba(255, 75, 95, (int) (180 * alpha));

            if (shadowBlur > 1.0F) {
                Render2D.drawShadow(x, y, w, h, radius, shadowBlur, ColorUtil.rgba(0, 0, 0, (int) (60 * alpha)));
                Render2D.drawShadow(x, y, w, h, radius, 3.0F, pvpGlow);
            }
            Render2D.drawRoundedRect(x, y, w, h, radius, pvpFill);
            if (!Theme.isLiquidGlass() && !Theme.isNewYear()) {
                Render2D.drawRoundedOutline(x, y, w, h, radius, 1.0F, pvpBorder);
            }
        } else if (Theme.isBlack()) {
            if (shadowBlur > 1.0F) {
                Render2D.drawShadow(x, y, w, h, radius, shadowBlur, ColorUtil.rgba(0, 0, 0, (int) (45 * alpha)));
            }
            Render2D.drawRoundedRect(x, y, w, h, radius, ColorUtil.rgba(14, 14, 16, (int) (240 * alpha)));
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)));
        } else if (Theme.isNewYear()) {
            if (shadowBlur > 1.0F) {
                Render2D.drawShadow(x, y, w, h, radius, shadowBlur, ColorUtil.rgba(20, 60, 95, (int) (35 * alpha)));
            }
            int frostTint = ColorUtil.rgba(10, 24, 44, (int) (135 * alpha));
            int iceBorder = ColorUtil.rgba(180, 230, 255, (int) (125 * alpha));
            Render2D.drawRoundedRect(x, y, w, h, radius, frostTint);
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.9F, iceBorder);
            Render2D.drawFrostSheen(x, y, w, h, radius, alpha);
        } else {
            int primaryAccent = getHudAccent();
            int lightDarken = ColorUtil.rgba(12, 16, 28, (int) (45 * alpha));
            int frostedTint = ColorUtil.rgba(255, 255, 255, (int) (16 * alpha));
            int glassBorder = ColorUtil.withAlpha(primaryAccent, (int) (65 * alpha));

            Render2D.drawRoundedRect(x, y, w, h, radius, lightDarken);
            Render2D.drawRoundedRect(x, y, w, h, radius, frostedTint);
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.7F, glassBorder);
        }
    }

    private void renderSubIslandInfoBar(float x, float y, float w, float h, int themeAccent, float alpha, long now) {
        if (w <= 0.0F) return;

        float subRadius = h / 2.0F;
        int primaryAccent = getHudAccent();

        if (Theme.isBlack()) {
            Render2D.drawShadow(x, y, w, h, subRadius, 2.5F, ColorUtil.rgba(0, 0, 0, (int) (42 * alpha)));
            Render2D.drawRoundedRect(x, y, w, h, subRadius, ColorUtil.rgba(14, 14, 16, (int) (240 * alpha)));
            Render2D.drawRoundedOutline(x, y, w, h, subRadius, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (18 * alpha)));
        } else if (Theme.isNewYear()) {
            Render2D.drawShadow(x, y, w, h, subRadius, 2.5F, ColorUtil.rgba(20, 60, 95, (int) (30 * alpha)));
            int frostTint = ColorUtil.rgba(10, 24, 44, (int) (135 * alpha));
            int iceBorder = ColorUtil.rgba(180, 230, 255, (int) (125 * alpha));
            Render2D.drawRoundedRect(x, y, w, h, subRadius, frostTint);
            Render2D.drawRoundedOutline(x, y, w, h, subRadius, 0.9F, iceBorder);
            Render2D.drawFrostSheen(x, y, w, h, subRadius, alpha);
        } else {
            int lightDarken = ColorUtil.rgba(12, 16, 28, (int) (45 * alpha));
            int frostedTint = ColorUtil.rgba(255, 255, 255, (int) (16 * alpha));
            int glassBorder = ColorUtil.withAlpha(primaryAccent, (int) (65 * alpha));

            Render2D.drawRoundedRect(x, y, w, h, subRadius, lightDarken);
            Render2D.drawRoundedRect(x, y, w, h, subRadius, frostedTint);
            Render2D.drawRoundedOutline(x, y, w, h, subRadius, 0.7F, glassBorder);
        }

        float curX = x + 6.0F;
        float textY = y + Fonts.SF_MEDIUM.centeredTextY(h / 2.0F, 7.0F);
        float dotY = y + (h - 2.0F) / 2.0F;
        boolean drawnAny = false;

        // Custom Profile Avatar
        if (showAvatar) {
            float avatarY = y + (h - HEAD_SIZE) / 2.0F;
            Render2D.drawCustomAvatar(curX, avatarY, HEAD_SIZE, HEAD_SIZE / 2.0F, alpha);
            curX += HEAD_SIZE;
            drawnAny = true;
        }

        // FPS Metric
        if (showFps) {
            if (drawnAny) curX = drawDot(curX, dotY, themeAccent, alpha);
            String fps = mc.getFps() + " fps";
            Fonts.drawString(Fonts.SF_MEDIUM, fps, curX, textY, 7.0F, ColorUtil.rgba(225, 230, 245, (int) (220 * alpha)));
            curX += Fonts.SF_MEDIUM.getWidth(fps, 7.0F);
            drawnAny = true;
        }

        // Ping Metric
        if (showPing) {
            if (drawnAny) curX = drawDot(curX, dotY, themeAccent, alpha);
            String ping = getPingText() + " ms";
            Fonts.drawString(Fonts.SF_MEDIUM, ping, curX, textY, 7.0F, ColorUtil.rgba(225, 230, 245, (int) (220 * alpha)));
            curX += Fonts.SF_MEDIUM.getWidth(ping, 7.0F);
            drawnAny = true;
        }

        // Server / TPS Metric
        if (showTps) {
            if (drawnAny) curX = drawDot(curX, dotY, themeAccent, alpha);
            String tps = "20.0 tps";
            Fonts.drawString(Fonts.SF_MEDIUM, tps, curX, textY, 7.0F, ColorUtil.rgba(225, 230, 245, (int) (220 * alpha)));
            curX += Fonts.SF_MEDIUM.getWidth(tps, 7.0F);
            drawnAny = true;
        }

        // Server IP Metric
        if (showServer) {
            if (drawnAny) curX = drawDot(curX, dotY, themeAccent, alpha);
            String srv = getServerAddress();
            Fonts.drawString(Fonts.SF_MEDIUM, srv, curX, textY, 7.0F, ColorUtil.rgba(225, 230, 245, (int) (220 * alpha)));
            curX += Fonts.SF_MEDIUM.getWidth(srv, 7.0F);
            drawnAny = true;
        }
    }

    private void drawStateContent(IslandState state, float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        switch (state) {
            case PVP -> drawPvP(x, y, width, height, alpha, now);
            case NOTIFICATION -> drawNotification(x, y, width, height, themeAccent, alpha, now);
            case MUSIC -> drawMusic(x, y, width, height, themeAccent, alpha, now);
            case NORMAL -> drawNormal(x, y, width, height, themeAccent, alpha, now);
        }
    }

    private void drawPvP(float x, float y, float width, float height, float alpha, long now) {
        float centerY = y + height / 2.0F;

        // Pulsating Combat Icon
        float pulse = 0.85F + 0.15F * (float) Math.sin(now / 110.0D);
        int swordCol = ColorUtil.rgba(255, (int) (80 * pulse), (int) (90 * pulse), (int) (245 * alpha));
        float iconSize = 9.0F;
        float iconY = centerY - iconSize / 2.0F;
        Fonts.drawIcon(IconUse.FIGHT, x + 8.0F, iconY, iconSize, swordCol);

        float curX = x + 21.0F;

        // Time Badge: [ 14s ] or [ PVP ]
        String badgeText = pvpSeconds >= 0 ? (pvpSeconds + "s") : "PVP";
        float badgeFontSize = 6.5F;
        float badgeW = Fonts.SF_MEDIUM.getWidth(badgeText, badgeFontSize) + 8.0F;
        float badgeH = 12.0F;
        float badgeY = centerY - badgeH / 2.0F;

        Render2D.drawRoundedRect(curX, badgeY, badgeW, badgeH, 3.5F, ColorUtil.rgba(215, 35, 55, (int) (225 * alpha)));
        Render2D.drawRoundedOutline(curX, badgeY, badgeW, badgeH, 3.5F, 0.8F, ColorUtil.rgba(255, 120, 140, (int) (180 * alpha)));
        float badgeTextY = Fonts.SF_MEDIUM.centeredTextY(centerY, badgeFontSize);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, badgeText, curX + badgeW / 2.0F, badgeTextY, badgeFontSize, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        curX += badgeW + 6.0F;

        // Compact PvP Title
        String display = "Режим PvP";
        float titleFontSize = 7.5F;
        float titleTextY = Fonts.SF_MEDIUM.centeredTextY(centerY, titleFontSize);
        Fonts.drawString(Fonts.SF_MEDIUM, display, curX, titleTextY, titleFontSize, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
    }

    private void drawNotification(float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        int dotColor = notificationPositive
                ? ColorUtil.rgba(85, 255, 135, (int) (245 * alpha))
                : ColorUtil.rgba(255, 80, 100, (int) (245 * alpha));

        float totalDuration = Math.max(100L, notificationDuration);
        float elapsed = now - notificationScrollStarted;
        float remainingRatio = Math.max(0.0F, Math.min(1.0F, 1.0F - (elapsed / totalDuration)));

        float ringRadius = 4.2F;
        float ringThickness = 1.3F;
        float cx = x + 12.5F;
        float cy = y + height / 2.0F;

        // Subtle background hollow track ring
        Render2D.drawCircleOutline(cx, cy, ringRadius, ringThickness, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));

        // Circular timer outline ring that winds down to 0
        if (remainingRatio > 0.001F) {
            float sweepAngle = 360.0F * remainingRatio;
            int ringColor = ColorUtil.rgba(ColorUtil.red(dotColor), ColorUtil.green(dotColor), ColorUtil.blue(dotColor), (int) (240 * alpha));
            Render2D.drawArc(cx, cy, ringRadius, ringThickness, -90.0F, sweepAngle, ringColor);
        }

        float textX = x + 23.0F;
        float textY = y + (height - 8.0F) / 2.0F;
        float availableW = width - 31.0F;

        drawMarquee(Fonts.SF_MEDIUM, notificationText, textX, textY, availableW, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)), now - notificationScrollStarted, false);
    }

    private void drawMusic(float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
        float artSize = chatOpen ? 22.0F : EXPANDED_ART_SIZE;
        float artX = x + 8.0F;
        float artY = y + (height - artSize) / 2.0F;

        // Album Art
        if (artworkTexture != null) {
            Render2D.drawTexture(artworkTexture, artX, artY, artSize, artSize, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
            Render2D.drawRoundedOutline(artX, artY, artSize, artSize, 5.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));
        } else {
            Render2D.drawRoundedRect(artX, artY, artSize, artSize, 5.0F, ColorUtil.rgba(36, 32, 48, (int) (220 * alpha)));
            drawEqualizer(artX + (artSize - 9.0F) / 2.0F, artY + artSize / 2.0F, 4, 1.8F, 1.0F, themeAccent, alpha, now);
        }

        float textX = artX + artSize + 8.0F;
        float controlsTotalW = 46.0F;
        float rightMargin = 12.0F;
        float controlsStartX = x + width - controlsTotalW - rightMargin;
        float availableTextW = Math.max(35.0F, (controlsStartX - 6.0F) - textX);

        float titleY = y + (height - 18.0F) / 2.0F;
        float artistY = titleY + 10.0F;

        drawMarquee(Fonts.SF_MEDIUM, trackTitle, textX, titleY, availableTextW, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)), now - contentTransitionStarted, false);
        String artist = trackArtist.isBlank() ? (trackPlaying ? "Воспроизведение" : "Пауза") : trackArtist;
        drawMarquee(Fonts.SF_MEDIUM, artist, textX, artistY, availableTextW, 6.5F, ColorUtil.rgba(180, 180, 205, (int) (200 * alpha)), now - contentTransitionStarted + 350L, false);

        drawMusicControls(controlsStartX, y + (height - 15.0F) / 2.0F, alpha);
    }

    private void drawMusicControls(float startX, float centerY, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() / mc.getWindow().getGuiScale();
        double mouseY = mc.mouseHandler.ypos() / mc.getWindow().getGuiScale();

        float prevSize = 14.0F;
        float playSize = 17.0F;
        float nextSize = 14.0F;
        float gap = 5.0F;

        // Alignment: Center play button vertically, align prev & next to play button's vertical center
        this.prevButtonX = startX;
        this.prevButtonY = centerY + (playSize - prevSize) / 2.0F;
        this.prevButtonSize = prevSize;

        this.playButtonX = this.prevButtonX + prevSize + gap;
        this.playButtonY = centerY;
        this.playButtonSize = playSize;

        this.nextButtonX = this.playButtonX + playSize + gap;
        this.nextButtonY = centerY + (playSize - nextSize) / 2.0F;
        this.nextButtonSize = nextSize;

        // 1. Previous Button
        boolean prevHover = isHovered(mouseX, mouseY, prevButtonX, prevButtonY, prevButtonSize, prevButtonSize);
        drawLiquidGlassButton(prevButtonX, prevButtonY, prevButtonSize, alpha, prevHover);
        int prevIconCol = prevHover ? Theme.getAccentColor() : ColorUtil.rgba(240, 240, 255, (int) (235 * alpha));
        float pIconPad = 3.0F;
        Render2D.drawTexture(MEDIA_PREV_TEX, prevButtonX + pIconPad, prevButtonY + pIconPad, prevButtonSize - pIconPad * 2.0F, prevButtonSize - pIconPad * 2.0F, 0.0F, prevIconCol);

        // 2. Play / Pause Button
        boolean playHover = isHovered(mouseX, mouseY, playButtonX, playButtonY, playButtonSize, playButtonSize);
        drawLiquidGlassButton(playButtonX, playButtonY, playButtonSize, alpha, playHover);
        int playIconCol = playHover ? Theme.getAccentColor() : ColorUtil.rgba(255, 255, 255, (int) (250 * alpha));
        float plIconPad = 3.5F;
        Identifier playPauseTex = trackPlaying ? MEDIA_PAUSE_TEX : MEDIA_PLAY_TEX;
        Render2D.drawTexture(playPauseTex, playButtonX + plIconPad, playButtonY + plIconPad, playButtonSize - plIconPad * 2.0F, playButtonSize - plIconPad * 2.0F, 0.0F, playIconCol);

        // 3. Next Button
        boolean nextHover = isHovered(mouseX, mouseY, nextButtonX, nextButtonY, nextButtonSize, nextButtonSize);
        drawLiquidGlassButton(nextButtonX, nextButtonY, nextButtonSize, alpha, nextHover);
        int nextIconCol = nextHover ? Theme.getAccentColor() : ColorUtil.rgba(240, 240, 255, (int) (235 * alpha));
        float nIconPad = 3.0F;
        Render2D.drawTexture(MEDIA_NEXT_TEX, nextButtonX + nIconPad, nextButtonY + nIconPad, nextButtonSize - nIconPad * 2.0F, nextButtonSize - nIconPad * 2.0F, 0.0F, nextIconCol);
    }

    private void drawLiquidGlassButton(float bx, float by, float size, float alpha, boolean hovered) {
        float r = size / 2.0F;
        float cx = bx + r;
        float cy = by + r;
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (65 * alpha));
        int glassFill = hovered
                ? ColorUtil.rgba(255, 255, 255, (int) (40 * alpha))
                : ColorUtil.rgba(255, 255, 255, (int) (16 * alpha));
        int glassBorder = hovered
                ? ColorUtil.withAlpha(Theme.getAccentColor(), (int) (220 * alpha))
                : ColorUtil.rgba(255, 255, 255, (int) (40 * alpha));

        Render2D.drawShadow(bx, by, size, size, r, 3.5F, shadowCol);
        Render2D.drawCircle(cx, cy, r, glassFill);
        Render2D.drawCircleOutline(cx, cy, r, 0.8F, glassBorder);
    }

    private void drawNormal(float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        float curX = x + 8.0F;
        float textY = y + (height - 8.0F) / 2.0F;

        // Client PNG Logo & Title (B&W in Black mode, theme accent in Energy/Liquid modes)
        float logoSize = 13.0F;
        int logoColor = Theme.isBlack()
                ? ColorUtil.rgba(255, 255, 255, (int) (245 * alpha))
                : ColorUtil.multiplyAlpha(themeAccent, alpha);
        Render2D.drawTexture(LOGO_TEX, curX, y + (height - logoSize) / 2.0F, logoSize, logoSize, logoColor);
        curX += logoSize + 4.5F;

        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC ", curX, textY, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));
        curX += Fonts.SF_MEDIUM.getWidth("Error DLC ", 8.0F);
        Fonts.drawString(Fonts.SF_MEDIUM, "26.2", curX, textY, 8.0F, ColorUtil.rgba(150, 150, 165, (int) (220 * alpha)));

        // If music playing: mini album art & wave equalizer on right edge
        if (hasTrack()) {
            float rightX = x + width - 36.0F;
            if (artworkTexture != null) {
                Render2D.drawTexture(artworkTexture, rightX, y + (height - COMPACT_ART_SIZE) / 2.0F, COMPACT_ART_SIZE, COMPACT_ART_SIZE, 3.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));
            } else {
                Render2D.drawRoundedRect(rightX, y + (height - COMPACT_ART_SIZE) / 2.0F, COMPACT_ART_SIZE, COMPACT_ART_SIZE, 3.0F, ColorUtil.rgba(45, 38, 58, (int) (200 * alpha)));
            }
            drawEqualizer(rightX + COMPACT_ART_SIZE + 4.0F, y + height / 2.0F, 3, 2.0F, 1.0F, themeAccent, alpha, now);
        }
    }

    @Override
    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        float width = 145.0F;
        String[] options = {
            "Под-островок: " + (showSubIsland ? "ВКЛ" : "ВЫКЛ"),
            "Аватарка: " + (showAvatar ? "ВКЛ" : "ВЫКЛ"),
            "FPS: " + (showFps ? "ВКЛ" : "ВЫКЛ"),
            "Ping: " + (showPing ? "ВКЛ" : "ВЫКЛ"),
            "TPS: " + (showTps ? "ВКЛ" : "ВЫКЛ"),
            "Сервер: " + (showServer ? "ВКЛ" : "ВЫКЛ")
        };
        float height = options.length * 18.0F + 8.0F;

        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));
        int glassFill = ColorUtil.rgba(20, 18, 28, (int) (235 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (45 * alpha));
        int themeAccent = getHudAccent();

        Render2D.drawShadow(menuX, menuY, width, height, 7.0F, 10.0F, shadowCol);
        Render2D.drawBlur(menuX, menuY, width, height, 7.0F, 16.0F, glassFill, alpha);
        Render2D.drawRoundedRect(menuX, menuY, width, height, 7.0F, glassFill);
        Render2D.drawRoundedOutline(menuX, menuY, width, height, 7.0F, 1.0F, glassBorder);

        float itemY = menuY + 4.0F;
        for (int i = 0; i < options.length; i++) {
            boolean active = switch (i) {
                case 0 -> showSubIsland;
                case 1 -> showAvatar;
                case 2 -> showFps;
                case 3 -> showPing;
                case 4 -> showTps;
                case 5 -> showServer;
                default -> false;
            };
            boolean hovered = mouseX >= menuX && mouseX <= menuX + width && mouseY >= itemY && mouseY <= itemY + 18.0F;

            if (hovered) {
                Render2D.drawRoundedRect(menuX + 4.0F, itemY, width - 8.0F, 17.0F, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));
            }

            int textColor = active ? themeAccent : ColorUtil.rgba(220, 220, 235, (int) (220 * alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, options[i], menuX + 10.0F, itemY + 4.0F, 7.5F, textColor);
            itemY += 18.0F;
        }

        return height;
    }

    @Override
    public boolean handleContextMenuClick(float menuX, float menuY, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        float width = 145.0F;
        float itemY = menuY + 4.0F;
        for (int i = 0; i < 6; i++) {
            if (mouseX >= menuX && mouseX <= menuX + width && mouseY >= itemY && mouseY <= itemY + 18.0F) {
                switch (i) {
                    case 0 -> showSubIsland = !showSubIsland;
                    case 1 -> showAvatar = !showAvatar;
                    case 2 -> showFps = !showFps;
                    case 3 -> showPing = !showPing;
                    case 4 -> showTps = !showTps;
                    case 5 -> showServer = !showServer;
                }
                return true;
            }
            itemY += 18.0F;
        }
        return false;
    }

    private float drawDot(float cx, float cy, int color, float alpha) {
        int dotCol = ColorUtil.rgba(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), (int) (180 * alpha));
        float startX = cx + 4.0F;
        Render2D.drawRoundedRect(startX, cy, 2.0F, 2.0F, 1.0F, dotCol);
        return startX + 2.0F + 4.0F;
    }

    private void drawEqualizer(float x, float centerY, int barCount, float gap, float barWidth, int color, float alpha, long now) {
        for (int i = 0; i < barCount; i++) {
            float wave = trackPlaying ? (float) Math.sin((double) now / 140.0D + (double) i * 1.3D) : 0.0F;
            float barH = 4.0F + (wave + 1.0F) * 2.2F;
            int barCol = ColorUtil.rgba(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), (int) ((trackPlaying ? 240 : 130) * alpha));
            Render2D.drawRoundedRect(x + (float) i * (barWidth + gap), centerY - barH / 2.0F, barWidth, barH, barWidth / 2.0F, barCol);
        }
    }

    private void drawMarquee(MsdfFont font, String text, float x, float y, float availableWidth, float fontSize, int color, long elapsed, boolean forceScroll) {
        float textWidth = font.getWidth(text, fontSize);
        if (textWidth <= availableWidth && !forceScroll) {
            Fonts.drawString(font, text, x, y, fontSize, color);
            return;
        }

        Render2D.pushScissor(x, y - 2.0F, availableWidth, font.textHeight(fontSize) + 4.0F);
        float gap = 24.0F;
        float cycle = textWidth + gap;
        float speed = 0.045F;
        float offset = (float) Math.max(0L, elapsed) * speed;
        offset %= cycle;
        float startX = x - offset;
        Fonts.drawString(font, text, startX, y, fontSize, color);
        Fonts.drawString(font, text, startX + cycle, y, fontSize, color);
        Render2D.popScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (this.displayedState != IslandState.MUSIC || this.activeMediaSession == null) return false;

        if (isHovered(mouseX, mouseY, prevButtonX, prevButtonY, prevButtonSize, prevButtonSize)) {
            CompletableFuture.runAsync(() -> {
                try { activeMediaSession.previous(); } catch (Throwable ignored) {}
            });
            return true;
        }
        if (isHovered(mouseX, mouseY, playButtonX, playButtonY, playButtonSize, playButtonSize)) {
            CompletableFuture.runAsync(() -> {
                try { activeMediaSession.playPause(); } catch (Throwable ignored) {}
            });
            return true;
        }
        if (isHovered(mouseX, mouseY, nextButtonX, nextButtonY, nextButtonSize, nextButtonSize)) {
            CompletableFuture.runAsync(() -> {
                try { activeMediaSession.next(); } catch (Throwable ignored) {}
            });
            return true;
        }

        return false;
    }

    private boolean isHovered(double mx, double my, float bx, float by, float bw, float bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private float getContentProgress(long now) {
        long elapsed = now - contentTransitionStarted;
        if (elapsed <= 0L) return 0.0F;
        return Math.min(1.0F, (float) elapsed / (float) CONTENT_TRANSITION_DURATION);
    }

    private float easeOutCubic(float x) {
        return 1.0F - (float) Math.pow(1.0F - x, 3);
    }

    private String getPingText() {
        if (mc.getConnection() == null || mc.player == null) return "--";
        var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (entry == null || entry.getLatency() <= 0) return "--";
        return String.valueOf(entry.getLatency());
    }

    @Override
    public void startDragging(double mouseX, double mouseY) {
        this.dragging = false;
    }

    private String getServerAddress() {
        try {
            if (mc.getConnection() != null && mc.getConnection().getConnection() != null) {
                if (mc.getSingleplayerServer() != null) return "Singleplayer";
                String addr = mc.getConnection().getConnection().getRemoteAddress().toString();
                String host = addr.split(":")[0].replace("/", "");
                if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || host.equalsIgnoreCase("127.0.0.1") || host.equalsIgnoreCase("localhost") || host.toLowerCase().contains("connecting")) {
                    return "Connecting...";
                }
                String lettersOnly = host.replaceAll("[0-9]", "").replaceAll("^\\.+|\\.+$", "");
                if (lettersOnly.isBlank()) return "Server";
                return lettersOnly;
            }
        } catch (Exception ignored) {}
        return "Connecting...";
    }
}

