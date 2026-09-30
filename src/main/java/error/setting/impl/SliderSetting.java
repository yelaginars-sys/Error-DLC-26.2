package error.setting.impl;

import lombok.Getter;
import error.setting.Setting;
import error.setting.SettingRenderer;
import error.setting.render.SliderRenderer;

/**
 * Create by daun kvass
 */
@Getter
public class SliderSetting extends Setting<Float> {
    private final float min;
    private final float max;
    private final float step;

    public SliderSetting(String name, float defaultValue, float min, float max, float step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public float get() {
        return getValue();
    }

    @Override
    public void setValue(Float value) {
        float precision = 1.0F / step;
        float rounded = Math.round(Math.max(min, Math.min(max, value)) * precision) / precision;
        super.setValue(rounded);
    }

    @Override
    public SettingRenderer<?> createRenderer() {
        return new SliderRenderer(this);
    }
}