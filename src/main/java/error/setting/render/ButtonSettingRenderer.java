package error.setting.render;

import error.setting.SettingRenderer;
import error.setting.impl.ButtonSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

/**
 * Create by daun kvass
 */
public class ButtonSettingRenderer extends SettingRenderer<ButtonSetting> {
    private final Animation hoverAnim = new Animation(0.0F, 0.2F);
    private float lastX, lastY, lastW, lastH;

    public ButtonSettingRenderer(ButtonSetting setting) {
        super(setting);
    }

    @Override
    public float getHeight() {
        return 22.0F * visAnim.getValue();
    }

    @Override
    public void render(float x, float y, float width, float height, int mouseX, int mouseY, float alpha) {
        if (visAnim.getValue() <= 0.01F) return;

        this.lastX = x; this.lastY = y; this.lastW = width; this.lastH = height;
        float effectiveAlpha = alpha * visAnim.getValue();

        float btnX = x + 6.0F;
        float btnY = y + 2.0F;
        float btnW = width - 12.0F;
        float btnH = height - 4.0F;

        boolean hovered = isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
        hoverAnim.setTarget(hovered ? 1.0F : 0.0F);
        hoverAnim.update();

        int hoverColor = ColorUtil.withAlpha(Theme.getAccentColor(), 180);
        int outlineColor = ColorUtil.lerp(ColorUtil.rgba(1, 1, 1, 100), hoverColor, hoverAnim.getValue());

        Render2D.drawShadow(btnX, btnY, btnW, btnH, 4, 4,
                ColorUtil.multiplyAlpha(outlineColor, effectiveAlpha));

        String displayName = Localization.get(setting.getName());
        float textW = Fonts.SF_MEDIUM.getWidth(displayName, 9.0F);
        Fonts.drawString(Fonts.SF_MEDIUM, displayName,
                btnX + (btnW - textW) / 2.0F, btnY + (btnH - 9.0F) / 2.0F + 1.0F, 9.0F,
                ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, effectiveAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && visAnim.getValue() > 0.5F && isHovered(mouseX, mouseY, lastX + 6.0F, lastY + 2.0F, lastW - 12.0F, lastH - 4.0F)) {
            setting.run();
        }
    }
}