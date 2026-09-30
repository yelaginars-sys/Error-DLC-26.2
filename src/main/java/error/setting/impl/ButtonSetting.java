package error.setting.impl;

import error.setting.Setting;
import error.setting.SettingRenderer;
import error.setting.render.ButtonSettingRenderer;

/**
 * Create by daun kvass
 */
public class ButtonSetting extends Setting<Runnable> {

    public ButtonSetting(String name, Runnable action) {
        super(name, action);
    }

    public void run() {
        if (getValue() != null) {
            getValue().run();
        }
    }

    @Override
    public SettingRenderer<?> createRenderer() {
        return new ButtonSettingRenderer(this);
    }
}