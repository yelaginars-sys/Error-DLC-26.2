package error.setting.impl;

import error.setting.Setting;
import error.setting.SettingRenderer;
import error.setting.render.ColorRenderer;

/**
 * Create by daun kvass
 */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int defaultColor) {
        super(name, defaultColor);
    }

    @Override
    public SettingRenderer<?> createRenderer() {
        return new ColorRenderer(this);
    }
}