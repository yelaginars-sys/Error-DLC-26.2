package error.setting.impl;

import error.setting.Setting;
import error.setting.render.CheckBoxRenderer;
import error.setting.SettingRenderer;

/**
 * Create by daun kvass
 */
public class CheckBox extends Setting<Boolean> {
    public CheckBox(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public boolean get() {
        return getValue();
    }

    public void toggle() {
        setValue(!getValue());
    }

    @Override
    public SettingRenderer<?> createRenderer() {
        return new CheckBoxRenderer(this);
    }
}