package error.setting.render;

import error.setting.SettingRenderer;
import error.setting.impl.ModeSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class ModeRenderer extends SettingRenderer<ModeSetting> {
    private final Map<String, float[]> chipBounds = new HashMap<>();
    private float calculatedHeight = 26.0F;
    private float lastRenderWidth = 115.0F;

    public ModeRenderer(ModeSetting setting) {
        super(setting);
        this.calculatedHeight = calculateRequiredHeight(this.lastRenderWidth);
    }

    private float calculateRequiredHeight(float width) {
        float startX = 6.0F;
        float curX = startX;
        float curY = 12.5F;
        float chipH = 12.0F;
        float gap = 2.5F;
        float maxRowW = Math.max(20.0F, width - 12.0F);

        float fontSize = 8.5F;
        float padX = 4.5F;

        for (String mode : setting.getModes()) {
            String displayMode = Localization.get(mode);
            float textW = Fonts.SF_MEDIUM.getWidth(displayMode, fontSize);
            float chipW = textW + (padX * 2.0F);

            if (curX + chipW > startX + maxRowW && curX > startX) {
                curX = startX;
                curY += chipH + gap;
            }
            curX += chipW + gap;
        }

        return curY + chipH + 3.0F;
    }

    @Override
    public float getHeight() {
        return this.calculatedHeight * visAnim.getValue();
    }

    @Override
    public void render(float x, float y, float width, float height, int mouseX, int mouseY, float alpha) {
        if (visAnim.getValue() <= 0.01F) return;

        this.lastRenderWidth = width;
        float effectiveAlpha = alpha * visAnim.getValue();

        String displayName = Localization.get(setting.getName());
        Fonts.drawString(Fonts.SF_MEDIUM, displayName, x + 6.0F, y + 1.5F, 9.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MAIN, effectiveAlpha));

        chipBounds.clear();
        float startX = x + 6.0F;
        float curX = startX;
        float curY = y + 12.5F;
        float chipH = 12.0F;
        float gap = 2.5F;
        float maxRowW = Math.max(20.0F, width - 12.0F);

        float fontSize = 8.5F;
        float padX = 4.5F;

        for (String mode : setting.getModes()) {
            boolean active = setting.is(mode);
            String displayMode = Localization.get(mode);

            float textW = Fonts.SF_MEDIUM.getWidth(displayMode, fontSize);
            float chipW = textW + (padX * 2.0F);

            if (curX + chipW > startX + maxRowW && curX > startX) {
                curX = startX;
                curY += chipH + gap;
            }

            chipBounds.put(mode, new float[]{curX, curY, chipW, chipH});

            if (active) {
                int accent = Theme.getAccentColor();
                int activeBg = ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (125 * effectiveAlpha));
                int activeOutline = ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (210 * effectiveAlpha));
                int glowCol = ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (95 * effectiveAlpha));
                Render2D.drawShadow(curX, curY, chipW, chipH, 4.0F, 4.0F, glowCol);
                Render2D.drawRoundedRect(curX, curY, chipW, chipH, 4.0F, activeBg);
                Render2D.drawRoundedOutline(curX, curY, chipW, chipH, 4.0F, 1.0F, activeOutline);
            } else {
                int inactiveBg = ColorUtil.rgba(255, 255, 255, (int) (12 * effectiveAlpha));
                int inactiveOutline = ColorUtil.rgba(255, 255, 255, (int) (20 * effectiveAlpha));
                Render2D.drawRoundedRect(curX, curY, chipW, chipH, 4.0F, inactiveBg);
                Render2D.drawRoundedOutline(curX, curY, chipW, chipH, 4.0F, 1.0F, inactiveOutline);
            }
            int textCol = active ? 0xFFFFFFFF : ColorUtil.rgba(215, 210, 230, (int) (210 * effectiveAlpha));
            Fonts.drawString(Fonts.SF_MEDIUM, displayMode, curX + padX, curY + 2.0F, fontSize, textCol);

            curX += chipW + gap;
        }

        this.calculatedHeight = (curY - y) + chipH + 3.0F;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && visAnim.getValue() > 0.5F) {
            for (Map.Entry<String, float[]> entry : chipBounds.entrySet()) {
                float[] b = entry.getValue();
                if (isHovered(mouseX, mouseY, b[0], b[1], b[2], b[3])) {
                    setting.setValue(entry.getKey());
                    break;
                }
            }
        }
    }
}