package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.ui.script.ScriptScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Script module ported from exclusive.
 * Allows managing and executing user scripts via dedicated Script Editor window.
 */
public class Script extends Module {

    public final BindSetting bind = bind("Бинд окна", GLFW.GLFW_KEY_UNKNOWN);

    public Script() {
        super("Script", "Запуск и редактирование пользовательских скриптов", Category.MISC);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        if (mc.gui != null && mc.gui.screen() == null) {
            mc.setScreenAndShow(new ScriptScreen());
        }
    }

    @EventTarget
    public void onKey(KeyboardInputEvent event) {
        if (!inGame() || player() == null || event.getAction() != GLFW.GLFW_PRESS) return;

        if (bind.matches(event.getKey())) {
            if (mc.gui != null && mc.gui.screen() instanceof ScriptScreen) {
                mc.setScreenAndShow(null);
            } else {
                mc.setScreenAndShow(new ScriptScreen());
            }
        }
    }
}
