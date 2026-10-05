package error.mixin.gui;

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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {

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

        // 1. Liquid Glass Kawase Blur Pass
        boolean batcherStartedLocally = false;
        if (!DisplayBatcher.active()) {
            DisplayBatcher.begin(extractor);
            batcherStartedLocally = true;
        }

        Color blurTint = self.active
                ? Color.rgba(0, 0, 0, Math.round(55 + this.error$hoverAnim * 20))
                : Color.rgba(0, 0, 0, 40);

        Blur.of(x, y, w, h)
                .radius(radius)
                .type(BlurType.KAWASE)
                .strength(self.active ? 4 : 2)
                .tint(blurTint)
                .render(extractor);

        // 2. Liquid Glass Outline Pass (top-to-bottom specular light gradient)
        Color topOutline;
        Color bottomOutline;
        if (self.active) {
            Color defaultTop = Color.WHITE.withAlpha(0.60F);
            Color defaultBottom = Color.rgba(255, 255, 255, 28);
            Color hoverTop = Color.of(themeAccent).withAlpha(0.95F);
            Color hoverBottom = Color.of(themeAccent).withAlpha(0.35F);
            topOutline = defaultTop.lerp(hoverTop, this.error$hoverAnim);
            bottomOutline = defaultBottom.lerp(hoverBottom, this.error$hoverAnim);
        } else {
            topOutline = Color.rgba(255, 255, 255, 24);
            bottomOutline = Color.rgba(255, 255, 255, 10);
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

        // 3. 2D Elements Pass: Glow, Specular Sheen & SF Typography
        error.util.RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // Liquid glow shadow when active or hovered
            if (self.active && this.error$hoverAnim > 0.02F) {
                Render2D.drawShadow(x, y, w, h, radius + 2.0F, 4.0F,
                        ColorUtil.withAlpha(themeAccent, (int) (60 * this.error$hoverAnim)));
            }

            // Subtle translucent glass body tint
            int glassBodyColor = self.active
                    ? ColorUtil.rgba(14, 18, 28, (int) (35 + 20 * this.error$hoverAnim))
                    : ColorUtil.rgba(10, 12, 18, 60);
            Render2D.drawRoundedRect(x, y, w, h, radius, glassBodyColor);

            // Inner liquid glow tinted by accent color when hovered
            if (self.active && this.error$hoverAnim > 0.02F) {
                Render2D.drawRoundedRect(x, y, w, h, radius,
                        ColorUtil.withAlpha(themeAccent, (int) (24 * this.error$hoverAnim)));
            }

            // Top specular glass highlight (soft reflection across top half)
            if (self.active && h > 10.0F) {
                float shineH = Math.max(2.0F, h * 0.42F);
                Render2D.drawRoundedRect(x + 1.2F, y + 1.0F, w - 2.4F, shineH, Math.max(1.0F, radius - 1.0F),
                        ColorUtil.rgba(255, 255, 255, (int) (12 + 14 * this.error$hoverAnim)));
            }

            // Button label with custom San Francisco font
            Component msg = self.getMessage();
            if (msg != null) {
                String rawText = msg.getString();
                if (rawText != null && !rawText.isEmpty()) {
                    String text = rawText.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
                    float fontSize = 7.5F;

                    float textWidth = Fonts.SF_MEDIUM.getWidth(text, fontSize);
                    float maxTextW = w - 10.0F;
                    if (textWidth > maxTextW && textWidth > 0.0F) {
                        fontSize = Math.max(5.0F, fontSize * (maxTextW / textWidth));
                    }

                    float fontY = y + (h - fontSize) / 2.0F - 0.5F;
                    int textColor = self.active
                            ? ColorUtil.rgba(255, 255, 255, (int) ((0.92F + 0.08F * this.error$hoverAnim) * 255))
                            : ColorUtil.rgba(155, 160, 175, 130);

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
