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
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {

    @Unique private static final Color ERROR$FADE_WHITE = Color.rgba(255, 255, 255, 32);
    @Unique private float error$hoverAnim = 0.0F;

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void renderCustomButton(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        if (!self.visible) return;
        if (self.getWidth() <= 0 || self.getHeight() <= 0) {
            ci.cancel();
            return;
        }

        String className = self.getClass().getName();
        if (className.contains("Tab") || className.contains("Recipe") || className.contains("Page") || className.contains("Book") || className.contains("Creative")) {
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

        // 1. Exact Liquid Glass Kawase Blur Pass from ClickGUI
        boolean batcherStartedLocally = false;
        if (!DisplayBatcher.active()) {
            DisplayBatcher.begin(extractor);
            batcherStartedLocally = true;
        }

        Color blurTint = self.active
                ? Color.rgba(0, 0, 0, Math.round(75 + this.error$hoverAnim * 15))
                : Color.rgba(0, 0, 0, 50);

        Blur.of(x, y, w, h)
                .radius(radius)
                .type(BlurType.KAWASE)
                .strength(4)
                .tint(blurTint)
                .render(extractor);

        // 2. Liquid Glass Outline Pass (top-to-bottom specular light gradient matching ClickGUI)
        Color topOutline;
        Color bottomOutline;
        if (self.active) {
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
                .thickness(1.0F)
                .verticalGradient(topOutline, bottomOutline)
                .render(extractor);

        DisplayBatcher.flush();
        if (batcherStartedLocally) {
            DisplayBatcher.end();
        }

        // 3. 2D Elements Pass: Liquid Shadow, Darken Layer, Frosted Sheen, Specular Highlight & Typography
        error.util.RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // Ambient glass shadow
            Render2D.drawShadow(x, y, w, h, radius + 1.0F, 3.5F,
                    ColorUtil.rgba(0, 0, 0, (int) (65 * (self.active ? 1.0F : 0.4F))));

            // Dynamic accent glow shadow when hovered
            if (self.active && this.error$hoverAnim > 0.02F) {
                Render2D.drawShadow(x, y, w, h, radius + 2.0F, 6.0F,
                        ColorUtil.withAlpha(themeAccent, (int) (75 * this.error$hoverAnim)));
            }

            // Dark translucent liquid base (cardDarken from ClickGUI)
            int cardDarken = self.active
                    ? ColorUtil.rgba(18, 22, 34, (int) (130 + 35 * this.error$hoverAnim))
                    : ColorUtil.rgba(14, 16, 24, 85);
            Render2D.drawRoundedRect(x, y, w, h, radius, cardDarken);

            // Frosted glass sheen (cardFrosted from ClickGUI)
            int cardFrosted = self.active
                    ? ColorUtil.rgba(255, 255, 255, (int) (12 + 18 * this.error$hoverAnim))
                    : ColorUtil.rgba(255, 255, 255, 6);
            Render2D.drawRoundedRect(x, y, w, h, radius, cardFrosted);

            // Inner liquid glow tinted by accent color when hovered
            if (self.active && this.error$hoverAnim > 0.02F) {
                Render2D.drawRoundedRect(x, y, w, h, radius,
                        ColorUtil.withAlpha(themeAccent, (int) (35 * this.error$hoverAnim)));
            }

            // Top specular glass highlight (soft reflection across top half)
            if (self.active && h > 10.0F) {
                float shineH = Math.max(2.0F, h * 0.44F);
                Render2D.drawRoundedRect(x + 1.2F, y + 1.0F, w - 2.4F, shineH, Math.max(1.0F, radius - 1.0F),
                        ColorUtil.rgba(255, 255, 255, (int) (14 + 18 * this.error$hoverAnim)));
            }

            // Crisp 2D rounded outline
            int outlineCol = self.active
                    ? ColorUtil.interpolateColor(ColorUtil.rgba(255, 255, 255, 28), ColorUtil.withAlpha(themeAccent, 210), this.error$hoverAnim)
                    : ColorUtil.rgba(255, 255, 255, 14);
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.75F, outlineCol);

            // Slider progress bar if widget is a slider
            if (self instanceof AbstractSliderButton slider && self instanceof AbstractSliderButtonAccessor accessor) {
                double val = accessor.getValue();
                float fillW = Math.max(4.0F, (w - 6.0F) * (float) val);
                Render2D.drawRoundedRect(x + 3.0F, y + h - 4.5F, fillW, 2.0F, 1.0F,
                        ColorUtil.withAlpha(themeAccent, (int) (220 * (self.active ? 1.0F : 0.5F))));
            }

            // Button label with custom San Francisco font
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
