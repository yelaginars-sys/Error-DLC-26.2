package error.mixin.gui;

import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {

    @Shadow public int width;
    @Shadow public int height;
    @Shadow public boolean active;
    @Shadow public boolean visible;
    @Shadow public abstract int getX();
    @Shadow public abstract int getY();
    @Shadow public abstract Component getMessage();
    @Shadow public abstract boolean isHovered();
    @Shadow public abstract boolean isFocused();

    @Unique private float error$hoverAnim = 0.0F;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void renderCustomWidget(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.visible) return;

        Object self = this;
        String className = self.getClass().getName();

        // Do NOT intercept edit boxes, checkboxes, lists, tab bars
        if (self instanceof EditBox
                || self instanceof Checkbox
                || self instanceof AbstractSelectionList
                || className.contains("SelectionList")
                || className.contains("EditBox")
                || className.contains("TabButton")) {
            return;
        }

        // Intercept all AbstractButtons (Button, CycleButton, SpriteIconButton, ImageButton) and Sliders
        if (!(self instanceof AbstractButton || self instanceof AbstractSliderButton)) {
            return;
        }

        if (this.width <= 0 || this.height <= 0) {
            ci.cancel();
            return;
        }

        boolean needExit = false;
        if (RenderExtend.currentGuiGraphicsExtractor() == null) {
            RenderExtend.enter2D(null, extractor, null);
            needExit = true;
        }

        Render2DUtil.beginFrame();
        try {
            boolean isHover = (this.isHovered() || this.isFocused()) && this.active;
            this.error$hoverAnim = Math.clamp(this.error$hoverAnim + (isHover ? 0.15F : -0.15F), 0.0F, 1.0F);

            float x = this.getX();
            float y = this.getY();
            float w = this.width;
            float h = this.height;
            float radius = Math.min(6.5F, h / 2.0F);

            // Excellent style liquid glass button:
            int bg;
            int outlineColor;
            if (!this.active) {
                bg = ColorUtil.rgba(18, 20, 26, 75);
                outlineColor = ColorUtil.rgba(255, 255, 255, 12);
            } else {
                int bgAlpha = (int) ((0.48F + this.error$hoverAnim * 0.22F) * 255);
                bg = ColorUtil.rgba(22, 24, 34, bgAlpha);

                int outlineAlpha = (int) ((0.12F + this.error$hoverAnim * 0.35F) * 255);
                outlineColor = ColorUtil.rgba(255, 255, 255, outlineAlpha);
            }

            Render2D.drawRoundedRect(x, y, w, h, radius, bg);
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.75F, outlineColor);

            // Slider progress bar / knob
            if (self instanceof AbstractSliderButton slider) {
                double value = ((error.mixin.accessor.AbstractSliderButtonAccessor) slider).getValue();
                float progress = (float) Math.clamp(value, 0.0, 1.0);
                if (this.active && progress > 0.005F) {
                    float fillW = Math.max(radius * 2.0F, (w - 2.0F) * progress);
                    fillW = Math.min(fillW, w - 2.0F);
                    int fillColor = ColorUtil.rgba(255, 255, 255, (int) ((0.08F + 0.08F * this.error$hoverAnim) * 255));
                    Render2D.drawRoundedRect(x + 1.0F, y + 1.0F, fillW, h - 2.0F, radius - 1.0F, fillColor);

                    float knobW = 4.0F;
                    float knobPad = 3.0F;
                    float knobX = x + knobPad + (w - knobPad * 2.0F - knobW) * progress;
                    float knobY = y + 2.5F;
                    float knobH = h - 5.0F;
                    int knobColor = ColorUtil.rgba(255, 255, 255, (int) ((0.75F + 0.25F * this.error$hoverAnim) * 255));
                    Render2D.drawRoundedRect(knobX, knobY, knobW, knobH, knobW / 2.0F, knobColor);
                }
            }

            Component msg = this.getMessage();
            if (msg != null) {
                String rawText = msg.getString();
                if (rawText != null && !rawText.isEmpty()) {
                    String text = rawText.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
                    float fontSize = 7.5F;

                    float textWidth = Fonts.SF_MEDIUM.getWidth(text, fontSize);
                    float maxTextW = w - (self instanceof AbstractSliderButton ? 18.0F : 12.0F);
                    if (textWidth > maxTextW && textWidth > 0.0F) {
                        fontSize = Math.max(5.0F, fontSize * (maxTextW / textWidth));
                    }

                    float fontY = y + (h - fontSize) / 2.0F - 0.5F;
                    int textColor = this.active
                            ? ColorUtil.rgba(255, 255, 255, (int) ((0.90F + 0.10F * this.error$hoverAnim) * 255))
                            : ColorUtil.rgba(160, 160, 175, 140);

                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, x + w / 2.0F, fontY, fontSize, textColor);
                }
            }

            Render2DUtil.flush();
            ci.cancel();
        } finally {
            if (needExit) {
                RenderExtend.exit2D();
            }
        }
    }
}
