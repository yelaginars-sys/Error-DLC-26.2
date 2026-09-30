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
    private static final float NOTIFICATION_HEIGHT = 22.0F;
    private static final float MUSIC_HEIGHT = 32.0F;
    private static final float MUSIC_CHAT_HEIGHT = 36.0F;
    private static final float PVP_HEIGHT = 24.0F;

    private static final float INFO_HEIGHT = 16.0F;
    private static final float GAP_BETWEEN = 4.0F;

    private static final float RADIUS = 7.0F;
    private static final float HEAD_SIZE = 11.0F;
    private static final float HEAD_RADIUS = 2.5F;
    private static final float COMPACT_ART_SIZE = 13.0F;
    private static final float EXPANDED_ART_SIZE = 24.0F;

    private static final long CONTENT_TRANSITION_DURATION = 260L;
    private static final long MUSIC_EXPANDED_HOLD = 3500L;
    private static final long MEDIA_REFRESH_INTERVAL = 600L;

    // Animations
    private final Animation widthAnimation = new Animation(180.0F, 0.28F);
    private final Animation heightAnimation = new Animation(NORMAL_HEIGHT, 0.28F);
    private boolean layoutInitialized = false;
    private boolean positionCentered = false;

    // Notification State
    private static volatile String notificationText = "";
    private static volatile boolean notificationPositive = true;
    private static volatile long notificationUntil = 0L;
    private static volatile long notificationScrollStarted = 0L;

    // PvP BossBar State (Enhanced with Lumen Regex)
    private static volatile LerpingBossEvent activePvPEvent = null;
    private static volatile String pvpTitle = "";
    private static volatile int pvpSeconds = -1;
    private static volatile float pvpProgress = 1.0F;
    private static volatile long pvpLastSeen = 0L;

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

    // Media Control Button Hitboxes
    private float prevButtonX, playButtonX, nextButtonX;
    private float musicButtonY, musicButtonSize;

    public DynamicIslandHud() {
        super("dynamic_island", "Dynamic Island", 240.0F, 8.0F, 180.0F, NORMAL_HEIGHT + GAP_BETWEEN + INFO_HEIGHT, true);
    }

    public static void postNotification(String text, boolean positive) {
        if (text == null || text.isBlank()) return;
        notificationText = text.replace('\n', ' ').trim();
        notificationPositive = positive;
        long now = System.currentTimeMillis();
        notificationScrollStarted = now + CONTENT_TRANSITION_DURATION;
        float textWidth = notificationText.length() * 5.2F;
        long scrollDuration = Math.max(2500L, (long) ((textWidth + 30.0F) / 0.05F));
        notificationUntil = notificationScrollStarted + scrollDuration;
    }

    public static boolean interceptBossBars(Map<UUID, LerpingBossEvent> events) {
        if (events == null || events.isEmpty()) {
            activePvPEvent = null;
            return false;
        }

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

                // Lumen-style Regex time parser
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
                return true;
            }
        }

        activePvPEvent = null;
        return false;
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

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(artworkPng));
            Identifier id = Identifier.fromNamespaceAndPath("error", "island_art_" + (++dynamicTextureCounter));
            DynamicTexture texture = new DynamicTexture(() -> id.toString(), image);
            mc.getTextureManager().register(id, texture);
            this.artworkTexture = id;
        } catch (Throwable ignored) {
            this.artworkTexture = null;
        }
    }

    private boolean isPvPActive(long now) {
        return activePvPEvent != null || (now - pvpLastSeen < 1200L && !pvpTitle.isEmpty());
    }

    private boolean hasTrack() {
        return !trackTitle.isBlank();
    }

    private boolean shouldShowExpandedMusic(long now) {
        return hasTrack() && (now < musicExpandedUntil || mc.gui.screen() instanceof ChatScreen);
    }

    private void updateContentState(IslandState nextState, long now) {
        if (nextState == this.displayedState) return;
        this.previousState = this.displayedState;
        this.displayedState = nextState;
        this.contentTransitionStarted = now;
    }

    private float getTargetWidth(IslandState state) {
        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;

        return switch (state) {
            case PVP -> {
                String badgeText = pvpSeconds >= 0 ? (pvpSeconds + "s") : "PVP";
                float badgeW = Fonts.SF_MEDIUM.getWidth(badgeText, 6.5F) + 6.0F;
                float titleW = Fonts.SF_MEDIUM.getWidth(pvpTitle.isEmpty() ? "Режим PvP" : pvpTitle, 7.5F);
                yield Math.max(160.0F, 22.0F + badgeW + 6.0F + titleW + 16.0F);
            }
            case NOTIFICATION -> {
                float textW = Fonts.SF_MEDIUM.getWidth(notificationText, 8.0F);
                yield Math.max(150.0F, Math.min(260.0F, textW + 34.0F));
            }
            case MUSIC -> {
                float titleW = Fonts.SF_MEDIUM.getWidth(trackTitle, 8.0F);
                float artistW = Fonts.SF_MEDIUM.getWidth(trackArtist, 6.5F);
                float baseW = chatOpen ? 215.0F : 190.0F;
                float maxW = chatOpen ? 270.0F : 250.0F;
                float pad = chatOpen ? 100.0F : 75.0F;
                yield Math.max(baseW, Math.min(maxW, Math.max(titleW, artistW) + pad));
            }
            case NORMAL -> {
                float brandW = Fonts.ICONS.getWidth(IconUse.LOGO.glyph, 9.0F) + 4.0F + Fonts.SF_MEDIUM.getWidth("Error DLC 26.2", 8.0F);
                float mediaW = hasTrack() ? (COMPACT_ART_SIZE + 24.0F) : 0.0F;
                yield Math.max(160.0F, brandW + mediaW + 28.0F);
            }
        };
    }

    private float getTargetHeight(IslandState state) {
        return switch (state) {
            case PVP -> PVP_HEIGHT;
            case NOTIFICATION -> NOTIFICATION_HEIGHT;
            case MUSIC -> mc.gui.screen() instanceof ChatScreen ? MUSIC_CHAT_HEIGHT : MUSIC_HEIGHT;
            case NORMAL -> NORMAL_HEIGHT;
        };
    }

    private float calculateInfoWidth() {
        String username = mc.player != null ? mc.player.getScoreboardName() : "User";
        float userW = HEAD_SIZE + 3.0F + Fonts.SF_MEDIUM.getWidth(username, 7.0F);
        float fpsW = Fonts.ICONS.getWidth(IconUse.FPS.glyph, 7.0F) + Fonts.SF_MEDIUM.getWidth(" " + mc.getFps() + "fps", 7.0F);
        float pingW = Fonts.ICONS.getWidth(IconUse.PING.glyph, 7.0F) + Fonts.SF_MEDIUM.getWidth(" " + getPingText() + "ms", 7.0F);
        float tpsW = Fonts.ICONS.getWidth(IconUse.TPS.glyph, 7.0F) + Fonts.SF_MEDIUM.getWidth(" 20tps", 7.0F);
        String server = getServerAddress();
        float srvW = Fonts.ICONS.getWidth(IconUse.GLOBE.glyph, 7.0F) + Fonts.SF_MEDIUM.getWidth(" " + server, 7.0F);
        float dotsW = 4 * 10.0F;
        return 12.0F + userW + dotsW + fpsW + pingW + tpsW + srvW + 12.0F;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        fadeAnim.setTarget(1.0F);
        fadeAnim.update();
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

        float maxTotalW = Math.max(targetIslandW, infoW);
        float totalH = targetIslandH + GAP_BETWEEN + INFO_HEIGHT;

        if (!layoutInitialized) {
            widthAnimation.setValue(targetIslandW);
            heightAnimation.setValue(targetIslandH);
            layoutInitialized = true;
        }

        if (!positionCentered && mc.getWindow() != null) {
            this.x = (mc.getWindow().getGuiScaledWidth() - maxTotalW) / 2.0F;
            this.y = 8.0F;
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
        float boundsY = getY();
        float centerX = boundsX + (this.width / 2.0F);

        float islandX = centerX - (curIslandW / 2.0F);
        float islandY = boundsY;

        int themeAccent = Theme.getAccentColor();

        // 1. RENDER TOP DYNAMIC ISLAND
        renderIslandShell(islandX, islandY, curIslandW, curIslandH, themeAccent, alpha);

        float progress = getContentProgress(now);
        boolean transitioning = progress < 1.0F && this.previousState != this.displayedState;

        Render2D.pushScissor(islandX, islandY, curIslandW, curIslandH);
        if (transitioning) {
            float slide = easeOutCubic(progress);
            drawStateContent(this.previousState, islandX - curIslandW * slide, islandY, curIslandW, curIslandH, themeAccent, alpha, now);
            drawStateContent(this.displayedState, islandX + curIslandW * (1.0F - slide), islandY, curIslandW, curIslandH, themeAccent, alpha, now);
        } else {
            drawStateContent(this.displayedState, islandX, islandY, curIslandW, curIslandH, themeAccent, alpha, now);
        }
        Render2D.popScissor();

        // 2. RENDER SUB-ISLAND INFORMATION BAR (Lumen-Style Info Bar Under the Island)
        float infoX = centerX - (infoW / 2.0F);
        float infoY = islandY + curIslandH + GAP_BETWEEN;
        renderSubIslandInfoBar(infoX, infoY, infoW, INFO_HEIGHT, themeAccent, alpha, now);
    }

    private void renderIslandShell(float x, float y, float w, float h, int themeAccent, float alpha) {
        if (this.displayedState == IslandState.PVP) {
            int pvpGlow = ColorUtil.rgba(255, 45, 65, (int) (125 * alpha));
            int pvpFill = ColorUtil.rgba(28, 8, 14, (int) (225 * alpha));
            int pvpBorder = ColorUtil.rgba(255, 75, 95, (int) (180 * alpha));

            Render2D.drawShadow(x, y, w, h, RADIUS, 12.0F, ColorUtil.rgba(0, 0, 0, (int) (160 * alpha)));
            Render2D.drawShadow(x, y, w, h, RADIUS, 8.0F, pvpGlow);
            Render2D.drawBlur(x, y, w, h, RADIUS, 16.0F, pvpFill, alpha);
            Render2D.drawRoundedRect(x, y, w, h, RADIUS, pvpFill);
            Render2D.drawRoundedOutline(x, y, w, h, RADIUS, 1.0F, pvpBorder);
        } else {
            int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (150 * alpha));
            int haloCol = ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), (int) (35 * alpha));
            int glassFill = ColorUtil.rgba(20, 18, 28, (int) (225 * alpha));
            int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (40 * alpha));

            Render2D.drawShadow(x, y, w, h, RADIUS, 12.0F, shadowCol);
            Render2D.drawShadow(x, y, w, h, RADIUS, 6.0F, haloCol);
            Render2D.drawBlur(x, y, w, h, RADIUS, 18.0F, glassFill, alpha);
            Render2D.drawRoundedRect(x, y, w, h, RADIUS, glassFill);
            Render2D.drawRoundedOutline(x, y, w, h, RADIUS, 1.0F, glassBorder);

            // Specular top gloss
            Render2D.drawRoundedRect(x + 6.0F, y + 1.0F, w - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (30 * alpha)));
        }
    }

    private void renderSubIslandInfoBar(float x, float y, float w, float h, int themeAccent, float alpha, long now) {
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (130 * alpha));
        int glassFill = ColorUtil.rgba(18, 16, 24, (int) (210 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));

        Render2D.drawShadow(x, y, w, h, 5.0F, 8.0F, shadowCol);
        Render2D.drawBlur(x, y, w, h, 5.0F, 14.0F, glassFill, alpha);
        Render2D.drawRoundedRect(x, y, w, h, 5.0F, glassFill);
        Render2D.drawRoundedOutline(x, y, w, h, 5.0F, 1.0F, glassBorder);

        float curX = x + 6.0F;
        float textY = y + (h - 7.0F) / 2.0F;
        float dotY = y + (h - 2.4F) / 2.0F;

        // Player Head
        if (mc.player instanceof AbstractClientPlayer clientPlayer) {
            float headY = y + (h - HEAD_SIZE) / 2.0F;
            Render2D.drawHead(clientPlayer, curX, headY, HEAD_SIZE, HEAD_RADIUS, alpha);
            curX += HEAD_SIZE + 3.0F;
        }

        // Username
        String username = mc.player != null ? mc.player.getScoreboardName() : "User";
        Fonts.drawString(Fonts.SF_MEDIUM, username, curX, textY, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (235 * alpha)));
        curX += Fonts.SF_MEDIUM.getWidth(username, 7.0F) + 5.0F;

        // Separator Dot
        curX = drawDot(curX, dotY, themeAccent, alpha);

        // FPS Metric
        Fonts.drawIcon(IconUse.FPS, curX, textY - 0.5F, 7.0F, themeAccent);
        curX += Fonts.ICONS.getWidth(IconUse.FPS.glyph, 7.0F);
        String fps = " " + mc.getFps() + "fps";
        Fonts.drawString(Fonts.SF_MEDIUM, fps, curX, textY, 7.0F, ColorUtil.rgba(215, 215, 230, (int) (210 * alpha)));
        curX += Fonts.SF_MEDIUM.getWidth(fps, 7.0F) + 5.0F;

        // Separator Dot
        curX = drawDot(curX, dotY, themeAccent, alpha);

        // Ping Metric
        Fonts.drawIcon(IconUse.PING, curX, textY - 0.5F, 7.0F, themeAccent);
        curX += Fonts.ICONS.getWidth(IconUse.PING.glyph, 7.0F);
        String ping = " " + getPingText() + "ms";
        Fonts.drawString(Fonts.SF_MEDIUM, ping, curX, textY, 7.0F, ColorUtil.rgba(215, 215, 230, (int) (210 * alpha)));
        curX += Fonts.SF_MEDIUM.getWidth(ping, 7.0F) + 5.0F;

        // Separator Dot
        curX = drawDot(curX, dotY, themeAccent, alpha);

        // Server / TPS Metric
        Fonts.drawIcon(IconUse.TPS, curX, textY - 0.5F, 7.0F, themeAccent);
        curX += Fonts.ICONS.getWidth(IconUse.TPS.glyph, 7.0F);
        String tps = " 20tps";
        Fonts.drawString(Fonts.SF_MEDIUM, tps, curX, textY, 7.0F, ColorUtil.rgba(215, 215, 230, (int) (210 * alpha)));
        curX += Fonts.SF_MEDIUM.getWidth(tps, 7.0F) + 5.0F;

        // Separator Dot
        curX = drawDot(curX, dotY, themeAccent, alpha);

        // Server IP Metric
        Fonts.drawIcon(IconUse.GLOBE, curX, textY - 0.5F, 7.0F, themeAccent);
        curX += Fonts.ICONS.getWidth(IconUse.GLOBE.glyph, 7.0F);
        String srv = " " + getServerAddress();
        Fonts.drawString(Fonts.SF_MEDIUM, srv, curX, textY, 7.0F, ColorUtil.rgba(215, 215, 230, (int) (210 * alpha)));
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
        float textY = y + (height - 8.0F) / 2.0F - 1.0F;

        // Pulsating Combat Icon
        float pulse = 0.85F + 0.15F * (float) Math.sin(now / 110.0D);
        int swordCol = ColorUtil.rgba(255, (int) (80 * pulse), (int) (90 * pulse), (int) (245 * alpha));
        Fonts.drawIcon(IconUse.FIGHT, x + 8.0F, textY + 0.5F, 9.0F, swordCol);

        float curX = x + 21.0F;

        // Lumen-style Time Badge: [ 14s ] or [ PVP ]
        String badgeText = pvpSeconds >= 0 ? (pvpSeconds + "s") : "PVP";
        float badgeW = Fonts.SF_MEDIUM.getWidth(badgeText, 6.5F) + 6.0F;
        float badgeH = 11.0F;
        float badgeY = y + (height - badgeH) / 2.0F - 1.0F;

        Render2D.drawRoundedRect(curX, badgeY, badgeW, badgeH, 3.0F, ColorUtil.rgba(215, 35, 55, (int) (220 * alpha)));
        Render2D.drawRoundedOutline(curX, badgeY, badgeW, badgeH, 3.0F, 1.0F, ColorUtil.rgba(255, 120, 140, (int) (180 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, badgeText, curX + 3.0F, badgeY + 2.0F, 6.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)));

        curX += badgeW + 5.0F;

        // PvP Title
        String display = pvpTitle.isEmpty() ? "Режим PvP" : pvpTitle;
        Fonts.drawString(Fonts.SF_MEDIUM, display, curX, textY, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        // Progress bar at the bottom
        float barW = width - 16.0F;
        float barH = 2.0F;
        float barX = x + 8.0F;
        float barY = y + height - 3.5F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * alpha)));
        float fillW = Math.max(2.0F, barW * Math.min(1.0F, Math.max(0.0F, pvpProgress)));
        Render2D.drawRoundedRect(barX, barY, fillW, barH, 1.0F, ColorUtil.rgba(255, 65, 85, (int) (230 * alpha)));
    }

    private void drawNotification(float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        int dotColor = notificationPositive
                ? ColorUtil.rgba(85, 255, 135, (int) (245 * alpha))
                : ColorUtil.rgba(255, 80, 100, (int) (245 * alpha));

        float dotSize = 5.0F;
        float dotX = x + 10.0F;
        float dotY = y + (height - dotSize) / 2.0F;
        Render2D.drawShadow(dotX, dotY, dotSize, dotSize, dotSize / 2.0F, 4.0F, dotColor);
        Render2D.drawRoundedRect(dotX, dotY, dotSize, dotSize, dotSize / 2.0F, dotColor);

        float textX = x + 22.0F;
        float textY = y + (height - 8.0F) / 2.0F;
        float availableW = width - 30.0F;

        drawMarquee(Fonts.SF_MEDIUM, notificationText, textX, textY, availableW, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)), now - notificationScrollStarted, true);
    }

    private void drawMusic(float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
        float artSize = chatOpen ? 24.0F : EXPANDED_ART_SIZE;
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
        float controlsW = chatOpen ? 46.0F : 24.0F;
        float availableTextW = Math.max(50.0F, width - (textX - x) - controlsW - 6.0F);

        float titleY = y + (chatOpen ? 6.5F : 5.0F);
        float artistY = y + (chatOpen ? 18.5F : 16.5F);

        drawMarquee(Fonts.SF_MEDIUM, trackTitle, textX, titleY, availableTextW, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)), now - contentTransitionStarted, false);
        String artist = trackArtist.isBlank() ? (trackPlaying ? "Воспроизведение" : "Пауза") : trackArtist;
        drawMarquee(Fonts.SF_MEDIUM, artist, textX, artistY, availableTextW, 6.5F, ColorUtil.rgba(180, 180, 205, (int) (200 * alpha)), now - contentTransitionStarted + 350L, false);

        if (chatOpen) {
            drawMusicControls(x + width - 48.0F, y + (height - 12.0F) / 2.0F, alpha);
        } else {
            drawEqualizer(x + width - 20.0F, y + height / 2.0F, 5, 2.5F, 1.4F, themeAccent, alpha, now);
        }
    }

    private void drawMusicControls(float x, float y, float alpha) {
        float btnSize = 12.0F;
        float gap = 3.0F;

        this.prevButtonX = x;
        this.playButtonX = x + btnSize + gap;
        this.nextButtonX = x + (btnSize + gap) * 2.0F;
        this.musicButtonY = y;
        this.musicButtonSize = btnSize;

        // Previous Button
        drawButtonBg(prevButtonX, y, btnSize, alpha);
        Fonts.drawString(Fonts.SF_MEDIUM, "«", prevButtonX + 2.5F, y + 1.0F, 8.0F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));

        // Play/Pause Button
        drawButtonBg(playButtonX, y, btnSize, alpha);
        String playGlyph = trackPlaying ? "❚❚" : "▶";
        float glyphOffX = trackPlaying ? 2.5F : 3.5F;
        Fonts.drawString(Fonts.SF_MEDIUM, playGlyph, playButtonX + glyphOffX, y + (trackPlaying ? 2.0F : 1.5F), 6.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        // Next Button
        drawButtonBg(nextButtonX, y, btnSize, alpha);
        Fonts.drawString(Fonts.SF_MEDIUM, "»", nextButtonX + 3.0F, y + 1.0F, 8.0F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));
    }

    private void drawButtonBg(float bx, float by, float size, float alpha) {
        Render2D.drawRoundedRect(bx, by, size, size, 3.0F, ColorUtil.rgba(60, 52, 76, (int) (160 * alpha)));
        Render2D.drawRoundedOutline(bx, by, size, size, 3.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (40 * alpha)));
    }

    private void drawNormal(float x, float y, float width, float height, int themeAccent, float alpha, long now) {
        float curX = x + 8.0F;
        float textY = y + (height - 8.0F) / 2.0F;

        // Error DLC 26.2 Client Branding
        Fonts.drawIcon(IconUse.LOGO, curX, textY + 0.5F, 9.0F, themeAccent);
        curX += Fonts.ICONS.getWidth(IconUse.LOGO.glyph, 9.0F) + 4.0F;

        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC 26.2", curX, textY, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

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

    private float drawDot(float cx, float cy, int color, float alpha) {
        int dotCol = ColorUtil.rgba(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), (int) (180 * alpha));
        Render2D.drawRoundedRect(cx, cy, 2.2F, 2.2F, 1.1F, dotCol);
        return cx + 2.2F + 5.0F;
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
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !(mc.gui.screen() instanceof ChatScreen)) return false;
        if (this.displayedState != IslandState.MUSIC || this.activeMediaSession == null) return false;

        if (isHovered(mouseX, mouseY, prevButtonX, musicButtonY, musicButtonSize, musicButtonSize)) {
            CompletableFuture.runAsync(() -> {
                try { activeMediaSession.previous(); } catch (Throwable ignored) {}
            });
            return true;
        }
        if (isHovered(mouseX, mouseY, playButtonX, musicButtonY, musicButtonSize, musicButtonSize)) {
            CompletableFuture.runAsync(() -> {
                try { activeMediaSession.playPause(); } catch (Throwable ignored) {}
            });
            return true;
        }
        if (isHovered(mouseX, mouseY, nextButtonX, musicButtonY, musicButtonSize, musicButtonSize)) {
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
        if (mc.getConnection() == null || mc.player == null) return "0";
        var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return entry != null ? String.valueOf(entry.getLatency()) : "0";
    }

    private String getServerAddress() {
        try {
            if (mc.getConnection() != null && mc.getConnection().getConnection() != null) {
                if (mc.getSingleplayerServer() != null) return "Singleplayer";
                String addr = mc.getConnection().getConnection().getRemoteAddress().toString();
                return addr.split(":")[0].replace("/", "");
            }
        } catch (Exception ignored) {}
        return "localhost";
    }
}
