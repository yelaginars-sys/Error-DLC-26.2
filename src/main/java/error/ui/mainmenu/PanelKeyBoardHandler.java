package error.ui.mainmenu;

import org.lwjgl.glfw.GLFW;
import error.Client;
import error.IMinecraft;
import error.event.EventTarget;
import error.event.list.*;
import error.module.impl.render.ClickGui;

/**
 * Create by daun kvass
 */
public final class PanelKeyBoardHandler implements IMinecraft {

    @EventTarget(priority = 1000)
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getKey() == GLFW.GLFW_KEY_F11) return;

        if (error.module.impl.misc.UnHook.unhooked) return;

        ClickGui clickGui = Client.INSTANCE.moduleManager.getClickGui();
        boolean isGuiKey = clickGui != null && clickGui.getBind().matches(event.getKey());

        if (clickGui != null && !clickGui.getBind().isBound() && event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            isGuiKey = true;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && isGuiKey) {
            if (PanelRefractions.toggle(mc)) {
                event.cancel();
                return;
            }
        }

        if (!PanelRefractions.isOpen()) {
            if (event.getAction() == GLFW.GLFW_PRESS && screen() == null) {
                Client.INSTANCE.moduleManager.onKey(event.getKey());
            }
            return;
        }

        boolean pressOrRepeat = event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT;
        if (pressOrRepeat && event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            PanelRefractions.close(mc);
            event.cancel();
            return;
        }

        if (pressOrRepeat && PanelRefractions.handleKey(event.getKey(), event.getScanCode(), event.getModifiers())) {
            event.cancel();
            return;
        }

        if (PanelRefractions.isTyping()) {
            event.cancel();
        }
    }

    @EventTarget(priority = 1000)
    public void onCharacterInput(CharacterInputEvent event) {
        if (PanelRefractions.isOpen()) {
            PanelRefractions.handleChar(event.getCodePoint());
            if (PanelRefractions.isTyping()) {
                event.cancel();
            }
        }
    }

    @EventTarget(priority = 1000)
    public void onMouseInput(MouseInputEvent event) {
        ClickGui clickGui = Client.INSTANCE.moduleManager.getClickGui();
        boolean isGuiMouse = clickGui != null && clickGui.getBind().matchesMouse(event.getButton());

        if (isGuiMouse && event.getAction() == GLFW.GLFW_PRESS) {
            if (PanelRefractions.toggle(mc)) {
                event.cancel();
                return;
            }
        }

        if (!PanelRefractions.isOpen()) {
            if (event.getAction() == GLFW.GLFW_PRESS && screen() == null) {
                Client.INSTANCE.moduleManager.onMouse(event.getButton());
            }
            return;
        }

        if (PanelRefractions.handleMouseButton(mc, event.getButton(), event.getAction())) {
            event.cancel();
        }
    }

    @EventTarget(priority = 1000)
    public void onScreenKey(ScreenKeyEvent event) {
        if (PanelRefractions.isTyping()) event.cancel();
    }

    @EventTarget(priority = 1000)
    public void onScreenMouseButton(ScreenMouseButtonEvent event) {
        if (PanelRefractions.isOpen()) event.cancel();
    }
}