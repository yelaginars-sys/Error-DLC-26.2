package error.util.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import error.IMinecraft;
import error.util.client.persiki.KeyUtil;

/**
 */
public final class KeyCheck implements IMinecraft {

    private static final KeyCheck INSTANCE = new KeyCheck();

    public static boolean isPressed(KeyMapping keyMapping) {
        if (keyMapping == null) return false;
        InputConstants.Key key = InputConstants.getKey(keyMapping.saveString());
        long handle = INSTANCE.hwindow();

        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getValue()) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetKey(handle, key.getValue()) == GLFW.GLFW_PRESS;
        }
    }

    public static boolean isKeyDown(int code) {
        if (code == 0 || code == GLFW.GLFW_KEY_UNKNOWN) return false;
        long handle = INSTANCE.hwindow();

        if (KeyUtil.isMouseButton(code)) {
            int btn = KeyUtil.toMouseButton(code);
            return GLFW.glfwGetMouseButton(handle, btn) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetKey(handle, code) == GLFW.GLFW_PRESS;
        }
    }
}