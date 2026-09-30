package error.module.impl.render;

import org.lwjgl.glfw.GLFW;
import error.ui.mainmenu.PanelRefractions;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;

/**
 * Create by daun kvass
 */
public class ClickGui extends Module {

    public static ClickGui INSTANCE;

    public final BindSetting holdKey = bind("Bind", GLFW.GLFW_KEY_LEFT_ALT);

    public ClickGui() {
        super("ClickGUI", "Интерфейс настройки функций и визуального стиля клиента", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        error.util.client.ClientSoundPlayer.playGuiOpen();
        PanelRefractions.open(mc);
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