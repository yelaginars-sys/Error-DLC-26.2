package error.ui.hud.impl;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaPlayerHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);
    private static final MediaInfo PLACEHOLDER = new MediaInfo("Название трека", "Артист", new byte[0], 0L, 0L, false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean polling = new AtomicBoolean(false);

    private volatile MediaInfo media = PLACEHOLDER;
    private volatile boolean hasMedia = false;
    private volatile long lastUpdate = 0L;
    private float progressAnim = 0.0F;
    private long lastPoll = 0L;

    public MediaPlayerHud() {
        super("media_player", "Media Player", 140.0F, 10.0F, 100.0F, 30.0F);
    }

    private void poll() {
        long now = System.currentTimeMillis();
        if (now - lastPoll < 500L || !polling.compareAndSet(false, true)) return;
        lastPoll = now;
        executor.submit(() -> {
            try {
                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                if (sessions == null || sessions.isEmpty()) {
                    hasMedia = false;
                    return;
                }
                IMediaSession best = sessions.stream()
                        .filter(s -> s != null && s.getMedia() != null)
                        .max(Comparator
                                .comparing((IMediaSession s) -> isMusicApp(s.getOwner()))
                                .thenComparing(s -> s.getMedia().getPlaying())
                                .thenComparing(s -> !s.getMedia().getTitle().isEmpty() && !s.getMedia().getArtist().isEmpty()))
                        .orElse(null);
                if (best == null || best.getMedia() == null) {
                    hasMedia = false;
                    return;
                }
                MediaInfo info = best.getMedia();
                if (info.getTitle().isEmpty() && info.getArtist().isEmpty()) {
                    hasMedia = false;
                    return;
                }
                media = info;
                hasMedia = true;
                lastUpdate = System.currentTimeMillis();
            } catch (Throwable ignored) {
                hasMedia = false;
            } finally {
                polling.set(false);
            }
        });
    }

    private boolean isMusicApp(String owner) {
        if (owner == null) return false;
        String o = owner.toLowerCase();
        return o.contains("soundcloud") || o.contains("spotify") || o.contains("yandex") || o.contains("яндекс")
                || o.contains("vk") || o.contains("вконтакте") || o.contains("apple music") || o.contains("itunes")
                || o.contains("deezer") || o.contains("tidal") || o.contains("amazon music") || o.contains("foobar")
                || o.contains("aimp") || o.contains("winamp") || o.contains("music") || o.contains("музыка");
    }

    @Override
    public void draw(Render2DEvent event) {
        poll();

        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);
        boolean recent = System.currentTimeMillis() - lastUpdate < 3000L;
        boolean visible = (hasMedia && (media.getPlaying() || recent)) || editing;

        fadeAnim.setTarget(visible ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float width = 100.0F;
        float height = 30.0F;
        float radius = 5.0F;
        float pad = 3.5F;
        float artSize = 23.0F;

        this.width = width;
        this.height = height;

        int primaryColor = Theme.getAccentColor();
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));

        // Background with blur and theme accent glow
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawBlur(drawX, drawY, width, height, radius, 12.0F, bgColor, alpha);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Artwork Box
        float artX = drawX + pad;
        float artY = drawY + (height - artSize) / 2.0F;
        Render2D.drawRoundedRect(artX, artY, artSize, artSize, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));
        Fonts.drawString(Fonts.SF_MEDIUM, "▶", artX + 7.5F, artY + 6.5F, 8.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Track Title & Artist Text
        float textX = artX + artSize + pad;
        String title = media.getTitle().isEmpty() ? "Название трека" : media.getTitle();
        String artist = media.getArtist().isEmpty() ? "Артист" : media.getArtist();

        if (title.length() > 14) title = title.substring(0, 14) + "..";
        if (artist.length() > 16) artist = artist.substring(0, 16) + "..";

        Fonts.drawString(Fonts.SF_MEDIUM, title, textX, drawY + 4.5F, 6.5F, ColorUtil.applyAlpha(ColorUtil.rgba(240, 240, 240, 255), alpha));
        Fonts.drawString(Fonts.SF_MEDIUM, artist, textX, drawY + 12.5F, 5.5F, ColorUtil.applyAlpha(ColorUtil.rgba(175, 175, 175, 255), alpha));

        // Progress Line
        float barX = textX;
        float barW = drawX + width - pad - barX;
        float barY = drawY + height - pad - 4.5F;
        float barH = 1.5F;

        long duration = media.getDuration();
        long position = Math.max(0L, Math.min(media.getPosition(), Math.max(duration, 1L)));
        float ratio = duration > 0L ? Math.max(0.0F, Math.min(1.0F, (float) position / duration)) : 0.0F;

        progressAnim = progressAnim + (ratio - progressAnim) * 0.12F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, barH / 2.0F, ColorUtil.rgba(255, 255, 255, (int) (38 * alpha)));
        float fillW = barW * progressAnim;
        if (fillW > 0.5F) {
            Render2D.drawRoundedRect(barX, barY, fillW, barH, barH / 2.0F, ColorUtil.applyAlpha(primaryColor, alpha));
        }
    }
}
