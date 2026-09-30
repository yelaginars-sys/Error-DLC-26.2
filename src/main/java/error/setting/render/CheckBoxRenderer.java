package error.setting.render;

import error.setting.SettingRenderer;
import error.setting.impl.CheckBox;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

/**
 * Create by daun kvass
 */
public class CheckBoxRenderer extends SettingRenderer<CheckBox> {
    private final Animation toggleAnim = new Animation(0.0F, 0.25F);
    private final Animation scrollAnim = new Animation(0.0F, 0.18F);
    private float lastX, lastY, lastW, lastH;

    public CheckBoxRenderer(CheckBox setting) {
        super(setting);
        toggleAnim.setValue(setting.getValue() ? 1.0F : 0.0F);
    }

    @Override
    public float getHeight() {
        return 16.0F * visAnim.getValue();
    }

    @Override
    public void render(float x, float y, float width, float height, int mouseX, int mouseY, float alpha) {
        if (visAnim.getValue() <= 0.01F) return;

        this.lastX = x; this.lastY = y; this.lastW = width; this.lastH = height;
        float effectiveAlpha = alpha * visAnim.getValue();

        toggleAnim.setTarget(setting.getValue() ? 1.0F : 0.0F);
        toggleAnim.update();

        float switchW = 16.0F;
        float switchH = 9.0F;
        float switchX = x + width - switchW - 6.0F;
        float switchY = y + (height - switchH) / 2.0F;

        String displayName = Localization.get(setting.getName());
        float textStartX = x + 6.0F;
        float maxTextW = Math.max(0.0F, switchX - textStartX - 5.0F);
        float textW = Fonts.SF_MEDIUM.getWidth(displayName, 9.5F);
        float overflow = Math.max(0.0F, textW - maxTextW);

        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);
        scrollAnim.setTarget((hovered && overflow > 0.0F) ? 1.0F : 0.0F);
        scrollAnim.update();

        float scrollOffset = overflow * scrollAnim.getValue();

        Render2D.pushScissor(textStartX, y, maxTextW, height);
        Fonts.drawString(Fonts.SF_MEDIUM, displayName, textStartX - scrollOffset, y + 3.0F, 9.5F,
                ColorUtil.multiplyAlpha(setting.getValue() ? Theme.TEXT_MAIN : Theme.TEXT_MUTED, effectiveAlpha));
        Render2D.popScissor();

        int offColor = ColorUtil.rgba(255, 255, 255, 25);
        int activeColor = ColorUtil.rgba(235, 145, 225, 250);
        int currentBg = ColorUtil.lerp(offColor, activeColor, toggleAnim.getValue());

        if (toggleAnim.getValue() > 0.05F) {
            Render2D.drawShadow(switchX, switchY, switchW, switchH, switchH / 2.0F, 4.0F, ColorUtil.rgba(230, 135, 220, (int) (100 * toggleAnim.getValue() * effectiveAlpha)));
        }
        Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, switchH / 2.0F, ColorUtil.multiplyAlpha(currentBg, effectiveAlpha));
        Render2D.drawRoundedOutline(switchX, switchY, switchW, switchH, switchH / 2.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (35 * effectiveAlpha)));

        float knobSize = 6.0F;
        float knobMinX = switchX + 1.5F;
        float knobMaxX = switchX + switchW - knobSize - 1.5F;
        float knobX = knobMinX + (knobMaxX - knobMinX) * toggleAnim.getValue();
        float knobY = switchY + (switchH - knobSize) / 2.0F;

        Render2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0F, ColorUtil.multiplyAlpha(0xFFFFFFFF, effectiveAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && visAnim.getValue() > 0.5F && isHovered(mouseX, mouseY, lastX, lastY, lastW, lastH)) {
            setting.toggle();
        }
    }
}