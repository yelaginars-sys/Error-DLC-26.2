package error.setting.impl;

import lombok.Getter;
import error.setting.Setting;
import error.setting.render.ModeRenderer;
import error.setting.SettingRenderer;

import java.util.Arrays;
import java.util.List;

/**
 */
@Getter
public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name, defaultMode);
        this.modes = Arrays.asList(modes);
    }

    public void cycle() {
        int index = modes.indexOf(getValue());
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }


    public boolean is(String mode) {
        return getValue().equalsIgnoreCase(mode);
    }

    @Override
    public SettingRenderer<?> createRenderer() {
        return new ModeRenderer(this);
    }
}