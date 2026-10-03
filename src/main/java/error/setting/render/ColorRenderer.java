package error.setting.render;

import error.ui.mainmenu.PanelRefractions;
import error.ui.mainmenu.popup.ColorPickerModal;
import error.setting.SettingRenderer;
import error.setting.impl.ColorSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

/**
 */
public class ColorRenderer extends SettingRenderer<ColorSetting> {
    private float lastX, lastY, lastW, lastH;

    public ColorRenderer(ColorSetting setting) {
        super(setting);
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

        String displayName = Localization.get(setting.getName());
        Fonts.drawString(Fonts.SF_MEDIUM, displayName, x + 6, y + 2.5F, 9.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, effectiveAlpha));

        float previewW = 14.0F;
        float previewH = 8.0F;
        float previewX = x + width - previewW - 6.0F;
        float previewY = y + (height - previewH) / 2.0F;

        Render2D.drawRoundedRect(previewX, previewY, previewW, previewH, 2.0F, ColorUtil.multiplyAlpha(setting.getValue(), effectiveAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && visAnim.getValue() > 0.5F && isHovered(mouseX, mouseY, lastX, lastY, lastW, lastH)) {
            float spawnX = lastX + lastW + 8.0F;
            float spawnY = lastY;
            PanelRefractions.openModal(new ColorPickerModal(setting, spawnX, spawnY));
        }
    }
}