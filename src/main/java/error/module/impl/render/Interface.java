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

    public final ModeSetting style         = mode("Style", "Liquid Glass", "Liquid Glass", "Default Black");
    public final ColorSetting accentColor  = color("Accent Color", ColorUtil.rgba(75, 124, 248, 255));
    public final CheckBox watermark        = checkbox("Watermark", true);
    public final CheckBox targetHud        = checkbox("Target HUD", true);
    public final CheckBox potionHud        = checkbox("Potions", true);
    public final CheckBox cooldownsHud     = checkbox("Cooldowns", true);
    public final CheckBox keybindsHud      = checkbox("Keybinds", true);
    public final CheckBox armorHud         = checkbox("Armor HUD", true);
    public final CheckBox blurBackground   = checkbox("Blur Background", true);
    public final CheckBox dynamicIsland    = checkbox("Dynamic Island", false);
    public final CheckBox gps              = checkbox("GPS Navigation", false);
    public final CheckBox snapping         = checkbox("Snapping", true);
    public final CheckBox collisions       = checkbox("Collisions", true);
    public final CheckBox guidelines       = checkbox("Guidelines", true);

    public Interface() {
        super("HUD", "Отображение ХУДа и элементов интерфейса", Category.RENDER);
        INSTANCE = this;
        updateHudState();
    }

    public static Interface getInstance() {
        return INSTANCE;
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
        if (!error.config.ConfigManager.isLoadingConfig) {
            this.accentColor.setValue(Theme.getAccentColor());
        }
        Theme.setGlassStyle(this.style.getValue());
        Theme.setBackgroundMode("None");

        HudManager manager = HudManager.getInstance();
        if (manager == null) return;
        manager.setSnappingEnabled(this.snapping.getValue());
        manager.setCollisionsEnabled(this.collisions.getValue());
        manager.setShowGuidelines(this.guidelines.getValue());

        boolean active = this.isEnabled();
        for (HudElement el : manager.getElements()) {
            if (el instanceof error.ui.hud.impl.WatermarkHudElement) {
                el.setEnabled(active && this.watermark.getValue());
            } else if (el instanceof error.ui.hud.impl.TargetHudElement) {
                el.setEnabled(active && this.targetHud.getValue());
            } else if (el instanceof error.ui.hud.impl.PotionHudElement) {
                el.setEnabled(active && this.potionHud.getValue());
            } else if (el instanceof error.ui.hud.impl.CooldownsHudElement) {
                el.setEnabled(active && this.cooldownsHud.getValue());
            } else if (el instanceof error.ui.hud.impl.KeybindsHudElement) {
                el.setEnabled(active && this.keybindsHud.getValue());
            } else if (el instanceof error.ui.hud.impl.ArmorHudElement) {
                el.setEnabled(active && this.armorHud.getValue());
            } else if (el instanceof error.ui.hud.impl.DynamicIslandHud) {
                el.setEnabled(active && this.dynamicIsland.getValue());
            } else if (el instanceof error.ui.hud.impl.GpsHud) {
                el.setEnabled(active && this.gps.getValue());
            } else {
                el.setEnabled(active);
            }
        }
    }
}
