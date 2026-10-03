package error.module.impl.render;

import org.lwjgl.glfw.GLFW;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.ui.nova.NovaGui;

public class ClickGui extends Module {

    public static ClickGui INSTANCE;

    public final BindSetting holdKey = bind("Bind", GLFW.GLFW_KEY_LEFT_ALT);

    public ClickGui() {
        super("ClickGUI", "Интерфейс настройки функций и визуального стиля клиента", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        if (screen() instanceof NovaGui) {
            mc.setScreenAndShow(null);
        } else {
            mc.setScreenAndShow(new NovaGui());
        }
        this.setState(false);
    }

    public boolean isHoldKeyPressed(long windowHandle) {
        if (holdKey == null || holdKey.isEmpty()) return false;
        for (int code : holdKey.getValue()) {
            if (BindSetting.isMouse(code)) {
                int button = BindSetting.rawButton(code);
                if (GLFW.glfwGetMouseButton(windowHandle, button) == GLFW.GLFW_PRESS) {
                    return true;
                }
            } else if (BindSetting.isKeyboard(code)) {
                if (GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS) {
                    return true;
                }
            }
        }
        return false;
    }
}