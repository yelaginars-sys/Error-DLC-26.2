package error.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    @Unique
    private static final Identifier PHOTO = Identifier.fromNamespaceAndPath("error", "images/ui/cutie.png");

    @Unique
    private long lastTime = -1L;

    @Unique
    private float animTime = 0;

    // Floating Particles for Splash Screen Animation
    @Unique
    private final List<SplashParticle> particles = new ArrayList<>();
    @Unique
    private final Random random = new Random();

    @Unique
    private static class SplashParticle {
        float x, y;
        float vx, vy;
        float size;
        float alphaMult;

        SplashParticle(float x, float y, float vx, float vy, float size, float alphaMult) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alphaMult = alphaMult;
        }

        void update(int width, int height) {
            x += vx;
            y += vy;
            if (x < 0) x = width;
            if (x > width) x = 0;
            if (y < 0) y = height;
            if (y > height) y = 0;
        }
    }

    @Unique
    private void initParticles(int width, int height) {
        if (particles.isEmpty() && width > 0 && height > 0) {
            for (int i = 0; i < 40; i++) {
                float px = random.nextFloat() * width;
                float py = random.nextFloat() * height;
                float vx = (random.nextFloat() - 0.5F) * 0.4F;
                float vy = -0.2F - random.nextFloat() * 0.5F;
                float size = 1.5F + random.nextFloat() * 2.5F;
                float alpha = 0.3F + random.nextFloat() * 0.7F;
                particles.add(new SplashParticle(px, py, vx, vy, size, alpha));
            }
        }
    }

    @Unique
    private static float smoothStep(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        ci.cancel();

        if (Thread.currentThread().getPriority() != Thread.MAX_PRIORITY) {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            } catch (Throwable ignored) {}
        }

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

        initParticles(width, height);

        long currentTime = Util.getMillis();

        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = currentTime;
        }

        if (RenderExtend.sttime == -1L) {
            RenderExtend.sttime = currentTime;
        }

        long elapsed = currentTime - RenderExtend.sttime;
        boolean isStartup = (this.minecraft.gui.screen() == null);
        long animStartDelay = isStartup ? 1000L : 0L;

        if (this.lastTime == -1L) {
            this.lastTime = currentTime;
        }
        float deltaTime = (currentTime - this.lastTime) / 1000f;
        this.lastTime = currentTime;

        float progressDelta = Math.min(deltaTime, 0.03f);

        if (elapsed >= animStartDelay) {
            this.animTime += progressDelta;
        }

        float targetProgress;
        if (elapsed < animStartDelay) {
            targetProgress = 0;
        } else {
            if (!this.reload.isDone()) {
                float progressTime = Math.max(0.0F, (elapsed - animStartDelay) / 1000f);
                float simulated = Mth.clamp(progressTime * 0.51f, 0, 0.92f);
                targetProgress = Math.max(simulated, this.reload.getActualProgress() * 0.92f);
            } else {
                targetProgress = 1;
            }
        }

        float catchUpSpeed = !this.reload.isDone() ? 0.2f : 0.5f;
        if (this.currentProgress < targetProgress) {
            this.currentProgress = Math.min(this.currentProgress + catchUpSpeed * progressDelta, targetProgress);
        } else {
            this.currentProgress = Mth.clamp(this.currentProgress * 0.98F + targetProgress * 0.02F, 0, 1);
        }
        float fadeOutProgress = this.fadeOutStart > -1L ? (float)(currentTime - this.fadeOutStart) / 1200 : 0;

        if (fadeOutProgress >= 1) {
            this.minecraft.gui.setOverlay(null);
            RenderExtend.sttime = -1L;
            return;
        }

        if (this.reload.isDone() && this.minecraft.gui.screen() != null) {
            this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(guiGraphicsExtractor, mouseX, mouseY, partialTick);
        }

        float alpha = 1;
        if (this.fadeOutStart > -1L) {
            alpha = 1 - smoothStep(fadeOutProgress);
        } else if (this.fadeIn && this.fadeInStart > -1L) {
            float fadeInProgress = (float)(currentTime - this.fadeInStart) / 1000;
            alpha = smoothStep(fadeInProgress);
        }

        int alphaInt = Math.round(255 * alpha);
        if (alphaInt <= 0) return;

        RenderExtend.enter2D(null, guiGraphicsExtractor, null);
        try {
            Render2DUtil.beginFrame();

            // Background texture
            Identifier bgTex = error.ui.mainmenu.CustomTitleScreen.getCurrentBgTexture();

            // Smooth Breathing Zoom (1.06x -> 1.0x as loading progresses with gentle wave)
            float wave = (float) (Math.sin(currentTime * 0.002D) * 0.015D);
            float zoom = 1.06F - (this.currentProgress * 0.06F) + wave;
            float bgW = width * zoom;
            float bgH = height * zoom;
            float bgX = (width - bgW) / 2.0F;
            float bgY = (height - bgH) / 2.0F;

            Render2D.drawTexture(bgTex, bgX, bgY, bgW, bgH, 0.0F, ColorUtil.applyAlpha(0xFFFFFFFF, alpha));

            // Dark Vignette Backdrop
            int ambientBg = ColorUtil.rgba(8, 8, 12, (int) (175 * alpha));
            Render2D.drawRect(0, 0, width, height, ambientBg);

            // Floating Starfield Particles Animation
            int themeAccent = Theme.getAccentColor();
            for (SplashParticle p : particles) {
                p.update(width, height);
                int pCol = ColorUtil.withAlpha(themeAccent, (int) (180 * alpha * p.alphaMult));
                Render2D.drawRoundedRect(p.x - p.size / 2.0F, p.y - p.size / 2.0F, p.size, p.size, p.size / 2.0F, pCol);
            }

            // Center Logo & Title with expanding wave rings
            float centerX = width / 2.0F;
            float centerY = height / 2.0F - 30.0F;

            // Expanding Wave Ring Animation
            float waveTimer = (currentTime % 2400L) / 2400.0F;
            float ringSize = 40.0F + waveTimer * 60.0F;
            int ringAlpha = (int) ((1.0F - waveTimer) * 110.0F * alpha);
            Render2D.drawRoundedOutline(centerX - ringSize / 2.0F, centerY - ringSize / 2.0F - 4.0F, ringSize, ringSize, ringSize / 2.0F, 1.5F, ColorUtil.withAlpha(themeAccent, ringAlpha));

            float pulse = (float) (Math.sin(currentTime * 0.004D) * 0.18D + 0.82D);
            int glowColor = ColorUtil.withAlpha(themeAccent, (int) (140 * alpha * pulse));
            int logoColor = ColorUtil.withAlpha(themeAccent, alpha);

            Render2D.drawShadow(centerX - 26.0F, centerY - 26.0F, 52.0F, 52.0F, 26.0F, 14.0F, glowColor);
            Fonts.drawCenteredIcon(IconUse.LOGO, centerX, centerY - 14.0F, 22.0F, logoColor);

            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "ERROR DLC", centerX, centerY + 20.0F, 17.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "PREMIUM CLIENT EXPERIENCE", centerX, centerY + 38.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.rgba(190, 190, 210, 255), alpha));

            // Bottom Liquid Glass Progress Bar
            float barW = 250.0F;
            float barH = 14.0F;
            float barX = (width - barW) / 2.0F;
            float barY = height - 48.0F;

            // Real Liquid Glass Container (Drop Shadow + Translucent Base + Subtle Glass Top Gloss + Clean Edge Border)
            Render2D.drawShadow(barX, barY, barW, barH, 7.0F, 8.0F, ColorUtil.rgba(0, 0, 0, (int) (160 * alpha)));
            Render2D.drawRoundedRect(barX, barY, barW, barH, 7.0F, ColorUtil.rgba(12, 12, 18, (int) (190 * alpha)));
            Render2D.drawRoundedRect(barX, barY, barW, barH * 0.45F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));
            Render2D.drawRoundedOutline(barX, barY, barW, barH, 7.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (45 * alpha)));

            // Liquid Wave Fill
            float fillMargin = 2.0F;
            float maxFillW = barW - (fillMargin * 2.0F);
            float fillW = Math.max(4.0F, maxFillW * Mth.clamp(this.currentProgress, 0.0F, 1.0F));
            float fillH = barH - (fillMargin * 2.0F);

            int barAccent = ColorUtil.withAlpha(themeAccent, alpha);
            Render2D.drawRoundedRect(barX + fillMargin, barY + fillMargin, fillW, fillH, 5.0F, barAccent);

            // Dynamic Moving Shine Gloss along the progress bar
            float shineX = barX + fillMargin + ((currentTime * 0.15F) % Math.max(1.0F, fillW));
            if (shineX < barX + fillMargin + fillW - 8.0F) {
                Render2D.drawRoundedRect(shineX, barY + fillMargin, 8.0F, fillH, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (90 * alpha)));
            }

            // Glowing Tip
            if (fillW > 6.0F) {
                Render2D.drawShadow(barX + fillMargin + fillW - 6.0F, barY + fillMargin, 6.0F, fillH, 3.0F, 6.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha * 0.8F));
            }

            // Status message & percentage text
            String status = this.currentProgress < 0.35F ? "Загрузка ресурсов..." : (this.currentProgress < 0.85F ? "Инициализация модулей..." : "Готово!");
            int percent = (int) (Mth.clamp(this.currentProgress, 0.0F, 1.0F) * 100);

            Fonts.drawString(Fonts.SF_MEDIUM, status, barX, barY - 13.0F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(200, 200, 215, 255), alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, percent + "%", barX + barW - Fonts.SF_MEDIUM.getWidth(percent + "%", 6.0F), barY - 13.0F, 6.0F, ColorUtil.applyAlpha(barAccent, alpha));

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }
}