package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;

public final class Interface extends Module {

    public final CheckBox dynamicIsland = checkbox("Dynamic Island", true);
    public final CheckBox gps           = checkbox("GPS Navigation", true);
    public final CheckBox snapping      = checkbox("Snapping", true);
    public final CheckBox collisions    = checkbox("Collisions", true);
    public final CheckBox guidelines    = checkbox("Guidelines", true);

    public Interface() {
        super("HUD", "Отображение ХУДа и элементов интерфейса", Category.RENDER);
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

    private void updateHudState() {
        HudManager manager = HudManager.getInstance();
        manager.setSnappingEnabled(this.snapping.getValue());
        manager.setCollisionsEnabled(this.collisions.getValue());
        manager.setShowGuidelines(this.guidelines.getValue());

        boolean active = this.isEnabled();
        for (HudElement el : manager.getElements()) {
            if (el instanceof error.ui.hud.impl.DynamicIslandHud) {
                el.setEnabled(active && this.dynamicIsland.getValue());
            } else if (el instanceof error.ui.hud.impl.GpsHud) {
                el.setEnabled(active && this.gps.getValue());
            } else {
                el.setEnabled(active);
            }
        }
    }
}
