package error.setting.impl;

import error.setting.Setting;
import error.setting.SettingRenderer;
import error.setting.render.HeaderRenderer;

/**
 * Create by daun kvass
 */
public class HeaderSetting extends Setting<String> {

    public HeaderSetting(String name) {
        super(name, name);
    }

    @Override
    public SettingRenderer<?> createRenderer() {
        return new HeaderRenderer(this);
    }
}