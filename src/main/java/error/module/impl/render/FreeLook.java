package error.module.impl.render;

import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;

/**
 */
public class FreeLook extends Module {

    public static FreeLook INSTANCE;

    public final ModeSetting mode = mode("Mode activity", "Hold", "Hold", "Toggle");
    public final BindSetting bind = bind("Bind", GLFW.GLFW_KEY_V);

    public static float cameraYaw = 0.0F;
    public static float cameraPitch = 0.0F;

    private boolean active = false;
    private boolean wasKeyPressed = false;
    private CameraType previousCameraType = CameraType.FIRST_PERSON;

    public FreeLook() {
        super("FreeLook", "Свободный обзор камеры", Category.MISC);
        INSTANCE = this;
    }

    public void update() {
        if (mc.player == null || mc.getWindow() == null) {
            if (active) stopFreeLook();
            return;
        }

        if (!isEnabled() || !bind.isBound()) {
            if (active) stopFreeLook();
            return;
        }

        long windowHandle = mc.getWindow().handle();
        int keyCode = bind.get(0);
        boolean isDown = isKeyDown(windowHandle, keyCode);

        boolean isHold = mode.is("Hold");

        if (isHold) {
            if (isDown && !active) {
                startFreeLook();
            } else if (!isDown && active) {
                stopFreeLook();
            }
        } else {
            if (isDown) {
                if (!wasKeyPressed) {
                    if (active) {
                        stopFreeLook();
                    } else {
                        startFreeLook();
                    }
                }
                wasKeyPressed = true;
            } else {
                wasKeyPressed = false;
            }
        }
    }

    public void startFreeLook() {
        if (active || mc.player == null) return;
        active = true;

        previousCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
    }

    public void stopFreeLook() {
        if (!active) return;
        active = false;

        if (mc.options != null) {
            mc.options.setCameraType(previousCameraType);
        }
    }

    public void handleTurn(double yRot, double xRot) {
        if (!active) return;

        float sensFactor = 0.15F;
        float deltaYaw = (float) yRot * sensFactor * ( 1.0F);
        float deltaPitch = (float) xRot * sensFactor * (1.0F);

        cameraYaw += deltaYaw;
        cameraPitch = Mth.clamp(cameraPitch + deltaPitch, -90.0F, 90.0F);
    }

    public boolean isActive() {
        return isEnabled() && active;
    }

    @Override
    protected void onDisable() {
        stopFreeLook();
        wasKeyPressed = false;
    }

    private boolean isKeyDown(long windowHandle, int code) {
        if (BindSetting.isMouse(code)) {
            int button = BindSetting.rawButton(code);
            return GLFW.glfwGetMouseButton(windowHandle, button) == GLFW.GLFW_PRESS;
        } else if (BindSetting.isKeyboard(code)) {
            return GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}