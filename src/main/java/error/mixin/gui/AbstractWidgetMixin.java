package error.mixin.gui;

import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    @Shadow protected boolean hovered;
    @Shadow public abstract int getX();
    @Shadow public abstract int getY();
    @Shadow public abstract Component getMessage();
    @Shadow public abstract boolean isHovered();
    @Shadow public abstract boolean isFocused();

    @Unique private float error$hoverAnim = 0.0F;

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void renderCustomWidget(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.visible) return;

        Object self = this;
        if (self instanceof EditBox
                || self instanceof Checkbox
                || self instanceof AbstractSelectionList) {
            return;
        }

        if (this.width <= 0 || this.height <= 0) {
            ci.cancel();
            return;
        }

        this.hovered = mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

        boolean isHover = (this.hovered || this.isFocused()) && this.active;
        this.error$hoverAnim = Math.clamp(this.error$hoverAnim + (isHover ? 0.15F : -0.15F), 0.0F, 1.0F);

        float x = this.getX();
        float y = this.getY();
        float w = this.width;
        float h = this.height;
        float radius = Math.min(6.0F, h / 2.0F);

        int bg = !this.active ? ColorUtil.rgba(18, 18, 24, 100) : ColorUtil.rgba(20, 20, 26, (int) ((0.55F + this.error$hoverAnim * 0.20F) * 255));
        int outlineColor = !this.active ? ColorUtil.rgba(255, 255, 255, 15) : ColorUtil.rgba(255, 255, 255, (int) ((0.10F + this.error$hoverAnim * 0.18F) * 255));

        Render2D.drawRoundedRect(x, y, w, h, radius, bg);
        Render2D.drawRoundedOutline(x, y, w, h, radius, 0.8F, outlineColor);

        if (self instanceof AbstractSliderButton slider) {
            double value = ((error.mixin.accessor.AbstractSliderButtonAccessor) slider).getValue();
            float progress = (float) Math.clamp(value, 0.0, 1.0);
            if (this.active) {
                float knobW = 4.0F;
                float knobPad = 3.0F;
                float knobX = x + knobPad + (w - knobPad * 2.0F - knobW) * progress;
                float knobY = y + 2.5F;
                float knobH = h - 5.0F;
                float knobRadius = knobW / 2.0F;
                int knobColor = ColorUtil.rgba(255, 255, 255, (int) ((0.85F + 0.15F * this.error$hoverAnim) * 255));
                Render2D.drawRoundedRect(knobX, knobY, knobW, knobH, knobRadius, knobColor);
            }
        }

        Component msg = this.getMessage();
        if (msg != null) {
            String rawText = msg.getString();
            if (rawText != null && !rawText.isEmpty()) {
                String text = rawText.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
                float fontSize = 7.5F;

                float textWidth = Fonts.SF_MEDIUM.getWidth(text, fontSize);
                float maxTextW = w - (self instanceof AbstractSliderButton ? 16.0F : 10.0F);
                if (textWidth > maxTextW && textWidth > 0.0F) {
                    fontSize = Math.max(5.0F, fontSize * (maxTextW / textWidth));
                }

                float fontY = y + (h - fontSize) / 2.0F - 0.5F;
                int textColor = this.active ? ColorUtil.rgba(245, 245, 255, 255) : ColorUtil.rgba(160, 160, 180, 180);

                Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, x + w / 2.0F, fontY, fontSize, textColor);
            }
        }

        ci.cancel();
    }
}
