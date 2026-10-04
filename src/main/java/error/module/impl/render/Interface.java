package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

public final class Interface extends Module {

    public static Interface INSTANCE;

    public final CheckBox dynamicIsland    = checkbox("Dynamic Island", true);
    public final CheckBox gps              = checkbox("GPS Навигация", true);
    public final ModeSetting colorMode     = mode("Цвет HUD", "Тема", "Тема", "Свой");
    public final ColorSetting customColor  = color("Свой цвет", ColorUtil.rgba(0, 180, 255, 255));

    public Interface() {
        super("HUD", "Отображение ХУДа и элементов интерфейса", Category.RENDER);
        INSTANCE = this;
        updateHudState();
    }

    public static Interface getInstance() {
        return INSTANCE;
    }

    public int getHudColor() {
        if ("Свой".equalsIgnoreCase(colorMode.getValue())) {
            return customColor.getValue();
        }
        return Theme.getAccentColor();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateHudState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        updateHudState();
    }

    @EventTarget(priority = 1000)
    public void onRender2D(Render2DEvent event) {
        updateHudState();
    }

    public void updateHudState() {
        HudManager manager = HudManager.getInstance();
        if (manager == null) return;

        boolean active = this.isEnabled();
        for (HudElement el : manager.getElements()) {
            if (el instanceof error.ui.hud.impl.DynamicIslandHud) {
                boolean on = active && this.dynamicIsland.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.GpsHud) {
                boolean on = active && this.gps.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else {
                el.setEnabled(active);
                if (!active) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            }
        }
    }
}
