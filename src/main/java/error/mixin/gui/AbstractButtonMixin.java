package error.mixin.gui;

import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

        error.util.RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            boolean isHover = (self.isHovered() || self.isFocused()) && self.active;
            this.error$hoverAnim = Math.clamp(this.error$hoverAnim + (isHover ? 0.15F : -0.15F), 0.0F, 1.0F);

            float x = self.getX();
            float y = self.getY();
            float w = self.getWidth();
            float h = self.getHeight();

            int themeAccent = error.util.client.clients.Theme.getAccentColor();
            float radius = Math.min(6.5F, h / 2.0F);

            int bg;
            int outlineColor;
            if (!self.active) {
                bg = ColorUtil.rgba(16, 18, 26, 80);
                outlineColor = ColorUtil.rgba(255, 255, 255, 15);
            } else {
                int bgAlpha = (int) ((0.52F + this.error$hoverAnim * 0.28F) * 255);
                bg = ColorUtil.rgba(18, 22, 34, bgAlpha);
                outlineColor = ColorUtil.interpolateColor(ColorUtil.rgba(255, 255, 255, 30), ColorUtil.withAlpha(themeAccent, 210), this.error$hoverAnim);
            }

            if (self.active && this.error$hoverAnim > 0.05F) {
                Render2D.drawShadow(x, y, w, h, radius + 2.0F, 3.5F, ColorUtil.withAlpha(themeAccent, (int) (45 * this.error$hoverAnim)));
            }

            Render2D.drawRoundedRect(x, y, w, h, radius, bg);
            if (self.active && this.error$hoverAnim > 0.05F) {
                Render2D.drawRoundedRect(x, y, w, h, radius, ColorUtil.withAlpha(themeAccent, (int) (30 * this.error$hoverAnim)));
            }
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.8F, outlineColor);

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
                            : ColorUtil.rgba(160, 160, 175, 140);

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
