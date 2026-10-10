package error.mixin.gui;

import error.mixin.accessor.AbstractSliderButtonAccessor;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.display.batch.DisplayBatcher;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin {

    @Unique private static final Color ERROR$FADE_WHITE = Color.rgba(255, 255, 255, 32);
    @Unique private float error$hoverAnim = 0.0F;

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void renderCustomSlider(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        if (!self.visible) return;
        if (self.getWidth() <= 0 || self.getHeight() <= 0) {
            ci.cancel();
            return;
        }

        boolean isHover = (self.isHovered() || self.isFocused()) && self.active;
        this.error$hoverAnim = Math.clamp(this.error$hoverAnim + (isHover ? 0.15F : -0.15F), 0.0F, 1.0F);

        float x = self.getX();
        float y = self.getY();
        float w = self.getWidth();
        float h = self.getHeight();

        int themeAccent = Theme.getAccentColor();
        float radius = Math.min(6.5F, h / 2.0F);

        // 1. Kawase Blur & Specular Outline Shader Pass
        boolean batcherStartedLocally = false;
        if (!DisplayBatcher.active()) {
            DisplayBatcher.begin(extractor);
            batcherStartedLocally = true;
        }

        Color blurTint;
        if (Theme.isNewYear()) {
            blurTint = self.active
                    ? Color.rgba(10, 26, 48, Math.round(95 + this.error$hoverAnim * 25))
                    : Color.rgba(8, 18, 34, 70);
        } else if (Theme.isBlack()) {
            blurTint = self.active
                    ? Color.rgba(10, 10, 14, Math.round(140 + this.error$hoverAnim * 30))
                    : Color.rgba(7, 7, 10, 100);
        } else { // Liquid Glass
            blurTint = self.active
                    ? Color.rgba(0, 0, 0, Math.round(75 + this.error$hoverAnim * 15))
                    : Color.rgba(0, 0, 0, 50);
        }

        Blur.of(x, y, w, h)
                .radius(radius)
                .type(BlurType.KAWASE)
                .strength(Theme.isBlack() ? 3 : 4)
                .tint(blurTint)
                .render(extractor);

        Color topOutline;
        Color bottomOutline;
        if (Theme.isNewYear()) {
            Color iceTop = Color.rgba(190, 238, 255, Math.round(180 + this.error$hoverAnim * 60));
            Color iceBottom = Color.rgba(90, 190, 255, Math.round(70 + this.error$hoverAnim * 45));
            topOutline = iceTop;
            bottomOutline = iceBottom;
        } else if (Theme.isBlack()) {
            Color hoverTop = Color.of(themeAccent).withAlpha(0.65F);
            Color hoverBottom = Color.of(themeAccent).withAlpha(0.25F);
            topOutline = Color.rgba(255, 255, 255, 30).lerp(hoverTop, this.error$hoverAnim);
            bottomOutline = Color.rgba(255, 255, 255, 12).lerp(hoverBottom, this.error$hoverAnim);
        } else if (self.active) {
            Color hoverTop = Color.of(themeAccent).lerp(Color.WHITE, 0.35F);
            Color hoverBottom = ERROR$FADE_WHITE.lerp(Color.of(themeAccent).withAlpha(0.40F), this.error$hoverAnim);
            topOutline = Color.WHITE.lerp(hoverTop, this.error$hoverAnim);
            bottomOutline = ERROR$FADE_WHITE.lerp(hoverBottom, this.error$hoverAnim);
        } else {
            topOutline = Color.rgba(255, 255, 255, 22);
            bottomOutline = Color.rgba(255, 255, 255, 8);
        }

        Outline.of(x, y, w, h)
                .radius(radius)
                .thickness(0.80F)
                .verticalGradient(topOutline, bottomOutline)
                .render(extractor);

        DisplayBatcher.flush();
        if (batcherStartedLocally) {
            DisplayBatcher.end();
        }

        // 2. 2D Elements Pass: Soft ambient shadow, translucent track, progress fill, handle & text
        error.util.RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            float shadowBlur = Theme.isBlack() ? 2.0F : 2.8F;
            int shadowAlpha = (int) ((Theme.isBlack() ? 36 : 24) * (self.active ? 1.0F : 0.4F));
            Render2D.drawShadow(x, y, w, h, radius, shadowBlur, ColorUtil.rgba(0, 0, 0, shadowAlpha));

            int baseFill;
            int outlineCol;
            if (Theme.isBlack()) {
                baseFill = self.active
                        ? ColorUtil.rgba(14, 14, 19, (int) (185 + 35 * this.error$hoverAnim))
                        : ColorUtil.rgba(11, 11, 15, 140);
                outlineCol = self.active
                        ? ColorUtil.interpolateColor(ColorUtil.rgba(255, 255, 255, 20), ColorUtil.withAlpha(themeAccent, 180), this.error$hoverAnim)
                        : ColorUtil.rgba(255, 255, 255, 10);
            } else if (Theme.isNewYear()) {
                baseFill = self.active
                        ? ColorUtil.rgba(12, 30, 52, (int) (110 + 40 * this.error$hoverAnim))
                        : ColorUtil.rgba(9, 20, 36, 90);
                outlineCol = self.active
                        ? ColorUtil.interpolateColor(ColorUtil.rgba(120, 210, 255, 160), ColorUtil.rgba(210, 248, 255, 240), this.error$hoverAnim)
                        : ColorUtil.rgba(90, 185, 235, 75);
            } else { // Liquid Glass
                baseFill = ColorUtil.rgba(14, 18, 26, (int) (115 + this.error$hoverAnim * 35));
                outlineCol = self.active
                        ? ColorUtil.interpolateColor(ColorUtil.rgba(255, 255, 255, 22), ColorUtil.withAlpha(themeAccent, 170), this.error$hoverAnim)
                        : ColorUtil.rgba(255, 255, 255, 10);
            }

            Render2D.drawRoundedRect(x, y, w, h, radius, baseFill);
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.75F, outlineCol);
            if (Theme.isNewYear()) {
                Render2D.drawFrostSheen(x, y, w, h, radius, self.active ? (0.75F + 0.25F * this.error$hoverAnim) : 0.50F);
            }

            double val = 0.0;
            if (this instanceof AbstractSliderButtonAccessor accessor) {
                val = Math.clamp(accessor.getValue(), 0.0, 1.0);
            }

            // Slider progress bar
            float fillW = Math.max(4.0F, (w - 6.0F) * (float) val);
            Render2D.drawRoundedRect(x + 3.0F, y + h - 4.5F, fillW, 2.0F, 1.0F,
                    ColorUtil.withAlpha(themeAccent, (int) (220 * (self.active ? 1.0F : 0.5F))));

            // Slider handle indicator
            float handleX = x + 3.0F + (w - 10.0F) * (float) val;
            Render2D.drawRoundedRect(handleX, y + 3.0F, 4.0F, h - 6.0F, 2.0F,
                    ColorUtil.rgba(255, 255, 255, (int) (180 + this.error$hoverAnim * 60)));

            Component msg = self.getMessage();
            if (msg != null) {
                String rawText = msg.getString();
                if (rawText != null && !rawText.isEmpty()) {
                    String text = rawText.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
                    float fontSize = Math.clamp(h * 0.40F, 6.0F, 8.5F);

                    float textWidth = Fonts.SF_MEDIUM.getWidth(text, fontSize);
                    float maxTextW = w - 10.0F;
                    if (textWidth > maxTextW && textWidth > 0.0F) {
                        fontSize = Math.max(5.0F, fontSize * (maxTextW / textWidth));
                    }

                    float fontY = y + (h - fontSize) / 2.0F - 0.5F;
                    int textColor = self.active
                            ? ColorUtil.interpolateColor(0xFFE2E8F0, 0xFFFFFFFF, this.error$hoverAnim)
                            : 0xFF64748B;

                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, x + w / 2.0F, fontY, fontSize, textColor);
                }
            }

            Render2DUtil.flush();
            ci.cancel();
        } finally {
            error.util.RenderExtend.exit2D();
        }
    }
}
