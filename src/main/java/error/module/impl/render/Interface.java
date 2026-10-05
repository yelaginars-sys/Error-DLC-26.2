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
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

public final class Interface extends Module {

    public static Interface INSTANCE;

    public final CheckBox dynamicIsland    = checkbox("Dynamic Island", true);
    public final CheckBox gps              = checkbox("GPS Навигация", true);
    public final CheckBox armorHud         = checkbox("Armor HUD", true);
    public final ModeSetting armorPosition = mode("Позиция брони", "Над иконками голода", "Над иконками голода", "Свободная");
    public final CheckBox targetHud        = checkbox("Target HUD", true);
    public final CheckBox targetHudParticles = checkbox("Частицы TargetHud", true);
    public final CheckBox cooldowns        = checkbox("Cooldowns", true);
    public final CheckBox staffList        = checkbox("Staff List", true);
    public final CheckBox binds            = checkbox("Binds", true);
    public final CheckBox potions          = checkbox("Potions", true);
    public final CheckBox serverHelper     = checkbox("Server Helper", true);
    public final CheckBox customHotbar     = checkbox("Custom Hotbar", true);
    public final CheckBox customScoreboard = checkbox("Custom Scoreboard", true);
    public final ModeSetting scoreboardMode = mode("Режим Скорборда", "Кастомный", "Кастомный", "Ванильный", "Скрыт");
    public final CheckBox scoreboardRemoveScores = checkbox("Скрывать числа скорборда", true);
    public final CheckBox scoreboardShadow = checkbox("Тень текста Скорборда", true);
    public final SliderSetting scoreboardScale = slider("Размер Скорборда", 1.0F, 0.5F, 2.0F, 0.05F);
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
            } else if (el instanceof error.ui.hud.impl.ArmorHud) {
                boolean on = active && this.armorHud.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.TargetHud) {
                boolean on = active && this.targetHud.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.KeyBindsHud) {
                boolean on = active && this.binds.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.PotionsHud) {
                boolean on = active && this.potions.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.ServerHelperHud) {
                boolean on = active && this.serverHelper.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.CustomHotbarHud) {
                boolean on = active && this.customHotbar.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.CustomScoreboardHud) {
                boolean on = active && this.customScoreboard.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.CooldownHud) {
                boolean on = active && this.cooldowns.getValue();
                el.setEnabled(on);
                if (!on) {
                    el.getFadeAnim().setTarget(0.0F);
                    el.getFadeAnim().setValue(0.0F);
                }
            } else if (el instanceof error.ui.hud.impl.StaffHud) {
                boolean on = active && this.staffList.getValue();
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
