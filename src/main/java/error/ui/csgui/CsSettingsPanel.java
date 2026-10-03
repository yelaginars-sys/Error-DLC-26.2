package error.ui.csgui;

import error.ui.csgui.CsClickGuiModel.*;
import error.util.client.clients.Theme;

import java.util.List;

public final class CsSettingsPanel {

    private ChoiceModel openChoice = null;
    private ColorValueModel openColor = null;
    private Dropdown activeDropdown = null;
    private ColorPicker activeColorPicker = null;
    private ColorDrag colorDrag = null;

    private record Dropdown(ChoiceModel choice, float x, float y, float w, float headerH, float rowH, boolean up) {
        float listY() {
            float h = choice.values().size() * rowH + 4;
            return up ? y - h - 2 : y + headerH + 2;
        }

        boolean contains(float mx, float my) {
            float h = choice.values().size() * rowH + 6;
            return hit(mx, my, x, y, w, headerH) || hit(mx, my, x, listY(), w, h);
        }
    }

    private record ColorPicker(ColorValueModel color, float x, float y, float w, float h) {
        boolean contains(float mx, float my) {
            return hit(mx, my, x, y, w, h);
        }
    }

    private enum ColorDrag { SATURATION_BRIGHTNESS, HUE, ALPHA }

    public static class ColorValueModel {
        private float hue = 0.5f, sat = 0.8f, bright = 0.9f, alpha = 1.0f;
        public float hue() { return hue; }
        public float saturation() { return sat; }
        public float brightness() { return bright; }
        public float alpha() { return alpha; }
        public void setHsb(float h, float s, float b) { this.hue = h; this.sat = s; this.bright = b; }
        public void setAlpha(float a) { this.alpha = a; }
        public int argb() { return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, sat, bright) & 0xFFFFFF); }
        public String value() { return String.format("#%06X", argb() & 0xFFFFFF); }
    }

    public static float calculateHeight(ModuleModel m, float scale) {
        float h = 10 * scale;
        for (SettingModel s : m.settings) {
            if (s.visible()) {
                h += 30 * scale;
            }
        }
        return h;
    }

    public void draw(UiDrawList ui, ModuleModel m, float x, float y, float w, float h, float scale,
                     int mx, int my, boolean down, boolean pressed, KeyModel bindingKey) {
        int accent = Theme.getAccentColor();

        // Panel background (LiquidGlass + Shadow)
        ui.shadow(x, y, w, h, 6 * scale, 10.0F, 0x90000000)
          .blur(x, y, w, h, 6 * scale, 14.0F, 0xE512101A, 1.0F)
          .roundedRect(x, y, w, h, 6 * scale, 0xE512101A)
          .roundedOutline(x, y, w, h, 6 * scale, 1.0F, 0xFF352D42);

        // Header Module Name & Description
        ui.strongText(x + 10 * scale, y + 12 * scale, 8.5F * scale, 0xFFFFFFFF, m.name)
          .text(x + 10 * scale, y + 24 * scale, 6.5F * scale, 0xFFA098B0, m.description);

        float curY = y + 36 * scale;

        for (SettingModel s : m.settings) {
            if (!s.visible()) continue;

            if (s instanceof ToggleModel toggle) {
                boolean hovered = hit(mx, my, x + 8 * scale, curY, w - 16 * scale, 24 * scale);
                if (pressed && hovered) {
                    toggle.toggle();
                }
                int boxBg = toggle.getValue() ? accent : 0xFF242030;
                ui.text(x + 10 * scale, curY + 12 * scale, 7.0F * scale, toggle.getValue() ? 0xFFFFFFFF : 0xFFB0A8C0, toggle.name())
                  .roundedRect(x + w - 24 * scale, curY + 6 * scale, 14 * scale, 14 * scale, 3.5F * scale, boxBg)
                  .roundedOutline(x + w - 24 * scale, curY + 6 * scale, 14 * scale, 14 * scale, 3.5F * scale, 1.0F, 0xFF453D54);
                curY += 28 * scale;
            } else if (s instanceof SliderModel slider) {
                float sliderW = w - 20 * scale;
                float sliderX = x + 10 * scale;
                float sliderY = curY + 16 * scale;
                float sliderH = 6 * scale;

                if (down && hit(mx, my, sliderX, curY, sliderW, 24 * scale)) {
                    float pct = Math.max(0, Math.min(1, (mx - sliderX) / sliderW));
                    float val = slider.min() + pct * (slider.max() - slider.min());
                    slider.setValue(val);
                }

                float pct = Math.max(0, Math.min(1, (slider.value() - slider.min()) / (slider.max() - slider.min())));
                String valStr = String.format("%.1f", slider.value());

                ui.text(sliderX, curY + 8 * scale, 7.0F * scale, 0xFFD0C8E0, slider.name())
                  .rightText(x + w - 10 * scale, curY + 8 * scale, 6.5F * scale, accent, valStr, false)
                  .roundedRect(sliderX, sliderY, sliderW, sliderH, 3.0F * scale, 0xFF242030)
                  .roundedRect(sliderX, sliderY, sliderW * pct, sliderH, 3.0F * scale, accent);
                curY += 28 * scale;
            } else if (s instanceof ChoiceModel choice) {
                boolean hovered = hit(mx, my, x + 8 * scale, curY, w - 16 * scale, 24 * scale);
                if (pressed && hovered) {
                    choice.select((choice.selected() + 1) % choice.values().size());
                    if (m.backing instanceof error.module.impl.misc.ClientSounds) {
                        error.util.client.ClientSoundPlayer.playModePreview(choice.value());
                    }
                }
                ui.text(x + 10 * scale, curY + 12 * scale, 7.0F * scale, 0xFFD0C8E0, choice.name())
                  .rightText(x + w - 10 * scale, curY + 12 * scale, 7.0F * scale, accent, choice.value(), false);
                curY += 28 * scale;
            } else if (s instanceof KeyModel keyModel) {
                boolean hovered = hit(mx, my, x + 8 * scale, curY, w - 16 * scale, 24 * scale);
                String display = (bindingKey == keyModel) ? "[ Нажмите клавишу ]" : keyModel.getDisplay();
                ui.text(x + 10 * scale, curY + 12 * scale, 7.0F * scale, 0xFFD0C8E0, keyModel.name())
                  .rightText(x + w - 10 * scale, curY + 12 * scale, 7.0F * scale, accent, display, false);
                curY += 28 * scale;
            }
        }
    }

    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
