package dev.liteproject.client.ui;

import dev.liteproject.client.ClientCore;
import dev.liteproject.client.module.ModuleCategory;
import dev.liteproject.client.render.ui.AnimatedFloat;
import dev.liteproject.client.render.ui.SliderModel;
import java.util.ArrayList;
import java.util.List;

public final class CsClickGuiModel {
	public record Category(String name, int icon, List<Module> modules) {
        public String description(){
            return switch(name){
                case "Combat"->"Configure targeting, combat feedback and defensive tools.";
                case "Player"->"Customize first-person behavior and player controls.";
                case "Interface"->"Tune the on-screen HUD and client information.";
                case "Render"->"Explore materials, effects and visual rendering.";
                default->"Utilities, world tools and client preferences.";
            };
        }
    }

	public static final class Module {
		public final String name, description;
		public final int icon;
		public final List<Setting> settings;
		public final AnimatedFloat animation;
		public boolean enabled;
		private final dev.liteproject.client.module.Module backing;

		Module(dev.liteproject.client.module.Module backing, List<Setting> settings) {
			this.backing = backing;
			name = backing.name();
			description = backing.description();
			icon = backing.icon();
			enabled = backing.enabled();
			this.settings = List.copyOf(settings);
			animation = new AnimatedFloat(enabled ? 1 : 0);
		}

        public String id(){return backing.id();}
		public void toggle() {
			enabled = !enabled;
			backing.setEnabled(enabled);
			animation.setTarget(enabled ? 1 : 0);
		}
	}

	public sealed interface Setting permits Toggle, Slider, Choice, Group, ColorValue, Key {
		String name();
		String hint();
		default boolean visible() { return true; }
	}

	public static final class Toggle implements Setting {
		private final dev.liteproject.client.setting.BooleanSetting backing;
		public final AnimatedFloat animation;
		public boolean value;

		Toggle(dev.liteproject.client.setting.BooleanSetting backing) {
			this.backing = backing;
			value = backing.enabled();
			animation = new AnimatedFloat(value ? 1 : 0);
		}
		public String name() { return backing.name(); }
		public String hint() { return backing.hint(); }
		public boolean visible() { return backing.visible(); }
		public void toggle() {
			value = !value;
			backing.set(value);
			animation.setTarget(value ? 1 : 0);
		}
	}

	public static final class Slider implements Setting {
		private final dev.liteproject.client.setting.NumberSetting backing;
		private final String suffix;
		public final SliderModel model;

		Slider(dev.liteproject.client.setting.NumberSetting backing) {
			this.backing = backing;
			suffix = backing.suffix();
			model = new SliderModel(backing.min(), backing.max(), backing.value());
		}
		public String name() { return backing.name(); }
		public String hint() { return backing.hint(); }
		public boolean visible() { return backing.visible(); }
		public String value() {
			float value = model.value();
			String formatted = Math.abs(value - Math.round(value)) < .01f
					? Integer.toString(Math.round(value))
					: String.format(java.util.Locale.ROOT, "%.1f", value);
			return formatted + suffix;
		}
		public void sync() { backing.set(model.value()); }
	}

	public static final class Choice implements Setting {
		private final dev.liteproject.client.setting.ModeSetting backing;
		public final List<String> values;
		public int selected;

		Choice(dev.liteproject.client.setting.ModeSetting backing) {
			this.backing = backing;
			values = backing.modes();
			selected = Math.max(0, values.indexOf(backing.value()));
		}
		public String name() { return backing.name(); }
		public String hint() { return backing.hint(); }
		public boolean visible() { return backing.visible(); }
		public String value() { return values.get(selected); }
		public void next() { select((selected + 1) % values.size()); }
		public void select(int index) {
			selected = Math.max(0, Math.min(values.size() - 1, index));
			backing.set(value());
		}
	}

	public static final class Group implements Setting {
		private final dev.liteproject.client.setting.MultiBooleanSetting backing;
		public final List<Toggle> values;

		Group(dev.liteproject.client.setting.MultiBooleanSetting backing) {
			this.backing = backing;
			values = backing.values().stream().map(Toggle::new).toList();
		}
		public String name() { return backing.name(); }
		public String hint() { return backing.hint(); }
		public boolean visible() { return backing.visible(); }
		public String summary() {
			long enabled = values.stream().filter(value -> value.value).count();
			return enabled + "/" + values.size() + " enabled";
		}
	}

	public static final class ColorValue implements Setting {
		private final dev.liteproject.client.setting.ColorSetting backing;
		ColorValue(dev.liteproject.client.setting.ColorSetting backing){this.backing=backing;}
		public String name(){return backing.name();}
		public String hint(){return backing.hint();}
		public boolean visible(){return backing.visible();}
		public int argb(){return backing.argb();}
		public String value(){return backing.hex();}
		public void nextHue(){backing.nextHue();}
		public float hue(){return backing.hue();}
		public float saturation(){return backing.saturation();}
		public float brightness(){return backing.brightness();}
		public float alpha(){return backing.alpha();}
		public void setHsb(float hue,float saturation,float brightness){backing.setHsb(hue,saturation,brightness);}
		public void setAlpha(float alpha){backing.setAlpha(alpha);}
	}
	public record Key(String name, String hint, String key) implements Setting {}

    public static List<Category> create(){
        return List.of(
            new Category("Combat",0xE8F4,modules(module->isCombat(module.id()))),
            new Category("Player",0xE7FD,modules(module->module.category()==ModuleCategory.PLAYER)),
            new Category("Interface",0xE8B8,modules(module->module.category()==ModuleCategory.HUD||module.id().equals("interface"))),
            new Category("Render",0xE3A6,modules(module->module.category()==ModuleCategory.RENDER&&!isCombat(module.id())&&!module.id().equals("interface"))),
            new Category("Misc",0xE5D2,modules(module->module.category()==ModuleCategory.WORLD||module.category()==ModuleCategory.UTILITY)));
    }
    private static boolean isCombat(String id){
        return id.equals("target_esp")||id.equals("hit_wave")||id.equals("hit_color")||id.equals("kill_effect");
    }
    private static List<Module> modules(java.util.function.Predicate<dev.liteproject.client.module.Module> predicate){
        return ClientCore.getInstance().modules().all().stream()
            .filter(predicate)
            .filter(module->!module.id().equals("client_diagnostics"))
            .map(CsClickGuiModel::live)
            .toList();
    }	private static Module live(dev.liteproject.client.module.Module core) {
		List<Setting> settings = new ArrayList<>();
		for (dev.liteproject.client.setting.Setting<?> setting : core.settings()) {
			if (setting instanceof dev.liteproject.client.setting.BooleanSetting value) {
				settings.add(new Toggle(value));
			} else if (setting instanceof dev.liteproject.client.setting.NumberSetting value) {
				settings.add(new Slider(value));
			} else if (setting instanceof dev.liteproject.client.setting.ModeSetting value) {
				settings.add(new Choice(value));
			} else if (setting instanceof dev.liteproject.client.setting.MultiBooleanSetting value) {
				settings.add(new Group(value));
			} else if (setting instanceof dev.liteproject.client.setting.ColorSetting value) {
				settings.add(new ColorValue(value));
			}
		}
		return new Module(core, settings);
	}


	private CsClickGuiModel() {}
}
