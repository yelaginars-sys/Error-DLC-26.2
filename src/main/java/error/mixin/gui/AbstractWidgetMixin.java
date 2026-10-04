package error.mixin.gui;

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

        // Do NOT intercept lists or container view slots
        if (self instanceof AbstractSelectionList || className.contains("SelectionList") || className.contains("Container")) {
            return;
        }

        if (this.width <= 0 || this.height <= 0) {
            ci.cancel();
            return;
        }

        Render2DUtil.beginFrame();
        try {
            boolean isHover = (this.isHovered() || this.isFocused()) && this.active;
            this.error$hoverAnim = Math.clamp(this.error$hoverAnim + (isHover ? 0.15F : -0.15F), 0.0F, 1.0F);

            float x = this.getX();
            float y = this.getY();
            float w = this.width;
            float h = this.height;

            // 1. Checkbox
            if (self instanceof Checkbox cb) {
                float boxSize = Math.min(14.0F, h);
                float boxY = y + (h - boxSize) / 2.0F;

                int bg = this.active ? ColorUtil.rgba(20, 24, 36, (int) ((0.60F + this.error$hoverAnim * 0.25F) * 255)) : ColorUtil.rgba(14, 16, 22, 100);
                int outlineColor = this.active ? ColorUtil.rgba(255, 255, 255, (int) ((0.15F + this.error$hoverAnim * 0.40F) * 255)) : ColorUtil.rgba(255, 255, 255, 20);

                Render2D.drawRoundedRect(x, boxY, boxSize, boxSize, 3.5F, bg);
                Render2D.drawRoundedOutline(x, boxY, boxSize, boxSize, 3.5F, 0.75F, outlineColor);

                if (cb.selected()) {
                    int checkBg = ColorUtil.rgba(37, 117, 252, (int) ((0.85F + this.error$hoverAnim * 0.15F) * 255));
                    Render2D.drawRoundedRect(x + 2.5F, boxY + 2.5F, boxSize - 5.0F, boxSize - 5.0F, 2.0F, checkBg);
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✓", x + boxSize / 2.0F, boxY + 1.5F, 7.0F, 0xFFFFFFFF);
                }

                Component msg = this.getMessage();
                if (msg != null && !msg.getString().isEmpty()) {
                    String text = msg.getString().replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
                    float textX = x + boxSize + 6.0F;
                    float fontY = y + (h - 7.0F) / 2.0F - 0.5F;
                    int textColor = this.active ? ColorUtil.rgba(240, 245, 255, 240) : ColorUtil.rgba(150, 155, 170, 140);
                    Fonts.drawString(Fonts.SF_MEDIUM, text, textX, fontY, 7.0F, textColor);
                }

                Render2DUtil.flush();
                ci.cancel();
                return;
            }

            // 2. EditBox (Text Input)
            if (self instanceof EditBox) {
                int bg = ColorUtil.rgba(14, 16, 24, 200);
                int border = this.isFocused() ? ColorUtil.rgba(255, 255, 255, 120) : ColorUtil.rgba(255, 255, 255, 30);

                Render2D.drawRoundedRect(x, y, w, h, 4.5F, bg);
                Render2D.drawRoundedOutline(x, y, w, h, 4.5F, 0.8F, border);
                Render2DUtil.flush();
                // Allow vanilla EditBox cursor & text rendering to extract on top
                return;
            }

            // 2.5 Small Icon Buttons (Language, Accessibility, Lock, Friends icons <= 24px wide)
            if (w <= 24.0F) {
                float iconRadius = Math.min(5.0F, h / 2.0F);
                int bg;
                int outlineColor;
                if (!this.active) {
                    bg = ColorUtil.rgba(16, 18, 26, 80);
                    outlineColor = ColorUtil.rgba(255, 255, 255, 15);
                } else {
                    int bgAlpha = (int) ((0.52F + this.error$hoverAnim * 0.28F) * 255);
                    bg = ColorUtil.rgba(20, 24, 36, bgAlpha);

                    int outlineAlpha = (int) ((0.14F + this.error$hoverAnim * 0.45F) * 255);
                    outlineColor = ColorUtil.rgba(255, 255, 255, outlineAlpha);
                }

                Render2D.drawRoundedRect(x, y, w, h, iconRadius, bg);
                Render2D.drawRoundedOutline(x, y, w, h, iconRadius, 0.8F, outlineColor);

                Component msg = this.getMessage();
                String msgText = msg != null ? msg.getString().toLowerCase() : "";
                String iconGlyph = error.util.render.font.IconUse.GLOBE.getGlyph();

                if (msgText.contains("язык") || msgText.contains("lang") || className.contains("Language")) {
                    iconGlyph = error.util.render.font.IconUse.GLOBE.getGlyph();
                } else if (msgText.contains("доступн") || msgText.contains("access") || className.contains("Accessibility")) {
                    iconGlyph = error.util.render.font.IconUse.PERSONS.getGlyph();
                } else if (msgText.contains("друг") || msgText.contains("friend") || msgText.contains("realms") || className.contains("Social")) {
                    iconGlyph = error.util.render.font.IconUse.GROUP.getGlyph();
                }

                int textColor = this.active
                        ? ColorUtil.rgba(255, 255, 255, (int) ((0.92F + 0.08F * this.error$hoverAnim) * 255))
                        : ColorUtil.rgba(160, 160, 175, 140);

                Fonts.drawCenteredString(Fonts.ICONS, iconGlyph, x + w / 2.0F, y + (h - 9.0F) / 2.0F - 0.5F, 9.0F, textColor);

                Render2DUtil.flush();
                ci.cancel();
                return;
            }

            // 3. All Buttons, Sliders, CycleButtons, and other widgets
            float radius = Math.min(6.5F, h / 2.0F);

            int bg;
            int outlineColor;
            if (!this.active) {
                bg = ColorUtil.rgba(16, 18, 26, 80);
                outlineColor = ColorUtil.rgba(255, 255, 255, 15);
            } else {
                int bgAlpha = (int) ((0.52F + this.error$hoverAnim * 0.28F) * 255);
                bg = ColorUtil.rgba(20, 24, 36, bgAlpha);

                int outlineAlpha = (int) ((0.14F + this.error$hoverAnim * 0.45F) * 255);
                outlineColor = ColorUtil.rgba(255, 255, 255, outlineAlpha);
            }

            Render2D.drawRoundedRect(x, y, w, h, radius, bg);
            Render2D.drawRoundedOutline(x, y, w, h, radius, 0.8F, outlineColor);

            // Slider progress bar & knob handle
            if (self instanceof AbstractSliderButton slider) {
                double value = ((error.mixin.accessor.AbstractSliderButtonAccessor) slider).getValue();
                float progress = (float) Math.clamp(value, 0.0, 1.0);
                if (this.active && progress > 0.005F) {
                    float fillW = Math.max(radius * 2.0F, (w - 2.0F) * progress);
                    fillW = Math.min(fillW, w - 2.0F);
                    int fillColor = ColorUtil.rgba(255, 255, 255, (int) ((0.10F + 0.10F * this.error$hoverAnim) * 255));
                    Render2D.drawRoundedRect(x + 1.0F, y + 1.0F, fillW, h - 2.0F, radius - 1.0F, fillColor);

                    float knobW = 4.0F;
                    float knobPad = 3.0F;
                    float knobX = x + knobPad + (w - knobPad * 2.0F - knobW) * progress;
                    float knobY = y + 2.5F;
                    float knobH = h - 5.0F;
                    int knobColor = ColorUtil.rgba(255, 255, 255, (int) ((0.80F + 0.20F * this.error$hoverAnim) * 255));
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
                    float maxTextW = w - (self instanceof AbstractSliderButton ? 18.0F : 10.0F);
                    if (textWidth > maxTextW && textWidth > 0.0F) {
                        fontSize = Math.max(5.0F, fontSize * (maxTextW / textWidth));
                    }

                    float fontY = y + (h - fontSize) / 2.0F - 0.5F;
                    int textColor = this.active
                            ? ColorUtil.rgba(255, 255, 255, (int) ((0.92F + 0.08F * this.error$hoverAnim) * 255))
                            : ColorUtil.rgba(160, 160, 175, 140);

                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, x + w / 2.0F, fontY, fontSize, textColor);
                }
            }

            Render2DUtil.flush();
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
