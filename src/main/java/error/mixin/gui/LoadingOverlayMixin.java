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
import error.util.render.Render2D;
import error.util.render.Render2DUtil;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Create by daun kvass
 */
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
    @Unique
    private static float smoothStep(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        if (Thread.currentThread().getPriority() != Thread.MAX_PRIORITY) {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            } catch (Throwable ignored) {}
        }

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

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

        int bgCol = ColorUtil.rgba(0, 0, 0, alphaInt);

        RenderExtend.enter2D(null, guiGraphicsExtractor, null);
        try {
            Render2DUtil.beginFrame();

            // Background texture matching CustomTitleScreen
            Identifier bgTex = error.ui.mainmenu.CustomTitleScreen.getCurrentBgTexture();

            // Animated Zoom Effect (camera zoom-in from 1.08x to 1.0x as loading progresses)
            float zoom = 1.08F - (this.currentProgress * 0.08F);
            float bgW = width * zoom;
            float bgH = height * zoom;
            float bgX = (width - bgW) / 2.0F;
            float bgY = (height - bgH) / 2.0F;

            Render2D.drawTexture(bgTex, bgX, bgY, bgW, bgH, 0.0F, ColorUtil.applyAlpha(0xFFFFFFFF, alpha));

            // Dark ambient overlay
            int ambientBg = ColorUtil.rgba(10, 10, 14, (int) (160 * alpha));
            Render2D.drawRect(0, 0, width, height, ambientBg);

            // Center Logo & Title with pulse glow
            float centerX = width / 2.0F;
            float centerY = height / 2.0F - 30.0F;

            float pulse = (float) (Math.sin(currentTime * 0.004D) * 0.15D + 0.85D);
            int glowColor = ColorUtil.applyAlpha(error.util.client.clients.Theme.getAccentColor(), (int) (120 * alpha * pulse));
            int logoColor = ColorUtil.applyAlpha(error.util.client.clients.Theme.getAccentColor(), alpha);

            Render2D.drawShadow(centerX - 24.0F, centerY - 24.0F, 48.0F, 48.0F, 24.0F, 12.0F, glowColor);
            error.util.render.font.Fonts.drawCenteredIcon(error.util.render.font.IconUse.LOGO, centerX, centerY - 14.0F, 20.0F, logoColor);

            error.util.render.font.Fonts.drawCenteredString(error.util.render.font.Fonts.SF_MEDIUM, "ERROR DLC", centerX, centerY + 18.0F, 16.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));
            error.util.render.font.Fonts.drawCenteredString(error.util.render.font.Fonts.SF_MEDIUM, "PREMIUM CLIENT EXPERIENCE", centerX, centerY + 36.0F, 6.5F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 200, 255), alpha));

            // Bottom Liquid Glass Progress Bar
            float barW = 240.0F;
            float barH = 14.0F;
            float barX = (width - barW) / 2.0F;
            float barY = height - 48.0F;

            // Liquid Glass Container (No Outline)
            Render2D.drawShadow(barX, barY, barW, barH, 7.0F, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alpha)));
            Render2D.drawRoundedRect(barX, barY, barW, barH, 7.0F, ColorUtil.rgba(14, 14, 20, (int) (195 * alpha)));

            // Animated Bar Fill
            float fillMargin = 2.0F;
            float maxFillW = barW - (fillMargin * 2.0F);
            float fillW = Math.max(4.0F, maxFillW * Mth.clamp(this.currentProgress, 0.0F, 1.0F));
            float fillH = barH - (fillMargin * 2.0F);

            int barAccent = ColorUtil.applyAlpha(error.util.client.clients.Theme.getAccentColor(), alpha);
            Render2D.drawRoundedRect(barX + fillMargin, barY + fillMargin, fillW, fillH, 4.5F, barAccent);

            // Glowing bar tip
            if (fillW > 6.0F) {
                Render2D.drawShadow(barX + fillMargin + fillW - 6.0F, barY + fillMargin, 6.0F, fillH, 3.0F, 4.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha * 0.6F));
            }

            // Status message & percentage text
            String status = this.currentProgress < 0.35F ? "Загрузка ресурсов..." : (this.currentProgress < 0.85F ? "Инициализация модулей..." : "Готово!");
            int percent = (int) (Mth.clamp(this.currentProgress, 0.0F, 1.0F) * 100);

            error.util.render.font.Fonts.drawString(error.util.render.font.Fonts.SF_MEDIUM, status, barX, barY - 12.0F, 6.0F, ColorUtil.applyAlpha(ColorUtil.rgba(200, 200, 215, 255), alpha));
            error.util.render.font.Fonts.drawString(error.util.render.font.Fonts.SF_MEDIUM, percent + "%", barX + barW - error.util.render.font.Fonts.SF_MEDIUM.getWidth(percent + "%", 6.0F), barY - 12.0F, 6.0F, ColorUtil.applyAlpha(barAccent, alpha));

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }
}