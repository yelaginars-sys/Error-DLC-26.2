package error.ui.csgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;

import java.util.ArrayList;
import java.util.List;

public final class CsClickGuiModel {

    public record CategoryModel(String name, int icon, List<ModuleModel> modules) {
        public String description() {
            return switch (name) {
                case "COMBAT" -> "Настройка аима, атак, автоматических эффектов и защиты.";
                case "MOVEMENT" -> "Параметры скорости, полёта и перемещения.";
                case "PLAYER" -> "Автоматизация инвентаря, использования предметов и игрока.";
                case "RENDER" -> "Отображение HUD, шейдеров, визуалов и анимаций.";
                case "MISC" -> "Вспомогательные функции, звуки и настройки клиента.";
                default -> "Настройки функций и возможностей клиента.";
            };
        }
    }

    public static final class ModuleModel {
        public final String name, description;
        public final List<SettingModel> settings;
        public boolean enabled;
        public final Module backing;

        ModuleModel(Module backing, List<SettingModel> settings) {
            this.backing = backing;
            this.name = backing.getName();
            this.description = backing.getDescription();
            this.enabled = backing.isEnabled();
            this.settings = List.copyOf(settings);
        }

        public void toggle() {
            backing.toggle();
            this.enabled = backing.isEnabled();
        }
    }

    public interface SettingModel {
        String name();
        String hint();
        boolean visible();
    }

    public static final class ToggleModel implements SettingModel {
        public final CheckBox setting;

        public ToggleModel(CheckBox setting) {
            this.setting = setting;
        }

        @Override public String name() { return setting.getName(); }
        @Override public String hint() { return ""; }
        @Override public boolean visible() { return setting.isVisible(); }

        public boolean getValue() { return setting.getValue(); }
        public void toggle() { setting.setValue(!setting.getValue()); }
    }

    public static final class SliderModel implements SettingModel {
        public final SliderSetting setting;

        public SliderModel(SliderSetting setting) {
            this.setting = setting;
        }

        @Override public String name() { return setting.getName(); }
        @Override public String hint() { return ""; }
        @Override public boolean visible() { return setting.isVisible(); }

        public float min() { return setting.getMin(); }
        public float max() { return setting.getMax(); }
        public float value() { return setting.getValue(); }
        public void setValue(float val) { setting.setValue(val); }
    }

    public static final class ChoiceModel implements SettingModel {
        public final ModeSetting setting;

        public ChoiceModel(ModeSetting setting) {
            this.setting = setting;
        }

        @Override public String name() { return setting.getName(); }
        @Override public String hint() { return ""; }
        @Override public boolean visible() { return setting.isVisible(); }

        public List<String> values() { return setting.getModes(); }
        public String value() { return setting.getValue(); }
        public int selected() {
            List<String> modes = setting.getModes();
            for (int i = 0; i < modes.size(); i++) {
                if (modes.get(i).equalsIgnoreCase(setting.getValue())) return i;
            }
            return 0;
        }
        public void select(int index) {
            List<String> modes = setting.getModes();
            if (index >= 0 && index < modes.size()) {
                setting.setValue(modes.get(index));
            }
        }
    }

    public static final class KeyModel implements SettingModel {
        public final BindSetting setting;

        public KeyModel(BindSetting setting) {
            this.setting = setting;
        }

        @Override public String name() { return setting.getName(); }
        @Override public String hint() { return ""; }
        @Override public boolean visible() { return setting.isVisible(); }

        public String getDisplay() { return setting.getDisplayValue(); }
        public void setKey(int key) { setting.setSingle(key); }
    }

    public static List<CategoryModel> create() {
        List<CategoryModel> categories = new ArrayList<>();
        if (Client.INSTANCE == null || Client.INSTANCE.moduleManager == null) return categories;

        for (Category cat : Category.values()) {
            List<Module> catModules = Client.INSTANCE.moduleManager.getByCategory(cat);
            List<ModuleModel> moduleModels = new ArrayList<>();

            for (Module m : catModules) {
                List<SettingModel> settingModels = new ArrayList<>();
                for (Setting<?> s : m.getSettings()) {
                    if (s instanceof CheckBox cb) {
                        settingModels.add(new ToggleModel(cb));
                    } else if (s instanceof SliderSetting sl) {
                        settingModels.add(new SliderModel(sl));
                    } else if (s instanceof ModeSetting ms) {
                        settingModels.add(new ChoiceModel(ms));
                    } else if (s instanceof BindSetting bs) {
                        settingModels.add(new KeyModel(bs));
                    }
                }
                moduleModels.add(new ModuleModel(m, settingModels));
            }

            int iconCode = switch (cat.name()) {
                case "COMBAT" -> 0xE5C3;
                case "MOVEMENT" -> 0xE5C4;
                case "PLAYER" -> 0xE5C5;
                case "RENDER" -> 0xE5C6;
                default -> 0xE5C7;
            };

            categories.add(new CategoryModel(cat.name(), iconCode, moduleModels));
        }

        return categories;
    }
}
