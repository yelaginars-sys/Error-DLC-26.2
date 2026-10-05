package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.math.Animation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    public static Zoom INSTANCE;

    public final ModeSetting mode = mode("Режим работы", "Hold", "Hold", "Toggle");
    public final BindSetting bind = bind("Кнопка", GLFW.GLFW_KEY_C);
    public final SliderSetting factor = slider("Кратность", 3.0F, 1.5F, 10.0F, 0.5F);
    public final CheckBox smooth = checkbox("Плавный зум", true);
    public final CheckBox scrollZoom = checkbox("Зум колесиком", true);

    private boolean active = false;
    private boolean wasKeyPressed = false;
    private float targetZoom = 3.0F;
    private final Animation zoomAnim = new Animation(1.0F, 0.22F);

    public Zoom() {
        super("Zoom", "Приближение камеры с настройкой кратности и колесиком", Category.RENDER);
        INSTANCE = this;
    }

    public void update() {
        if (mc.player == null || mc.getWindow() == null || screen() != null) {
            if (active) stopZoom();
            updateAnimation();
            return;
        }

        if (!isEnabled() || !bind.isBound()) {
            if (active) stopZoom();
            updateAnimation();
            return;
        }

        long windowHandle = mc.getWindow().handle();
        int keyCode = bind.get(0);
        boolean isDown = isKeyDown(windowHandle, keyCode);

        boolean isHold = mode.is("Hold");

        if (isHold) {
            if (isDown && !active) {
                startZoom();
            } else if (!isDown && active) {
                stopZoom();
            }
        } else {
            if (isDown) {
                if (!wasKeyPressed) {
                    if (active) {
                        stopZoom();
                    } else {
                        startZoom();
                    }
                }
                wasKeyPressed = true;
            } else {
                wasKeyPressed = false;
            }
        }

        updateAnimation();
    }

    private void updateAnimation() {
        if (active) {
            zoomAnim.setTarget(targetZoom);
        } else {
            zoomAnim.setTarget(1.0F);
        }
        if (!smooth.getValue()) {
            zoomAnim.setValue(active ? targetZoom : 1.0F);
        } else {
            zoomAnim.update();
        }
    }

    public void startZoom() {
        if (active) return;
        active = true;
        targetZoom = factor.getValue();
    }

    public void stopZoom() {
        if (!active) return;
        active = false;
    }

    public void onScroll(double vertical) {
        if (!active || !scrollZoom.getValue()) return;
        targetZoom = Mth.clamp(targetZoom + (float) (vertical * 0.8F), 1.2F, 30.0F);
    }

    public boolean isActive() {
        return isEnabled() && (active || zoomAnim.getValue() > 1.01F);
    }

    public float getEffectiveZoom() {
        if (!isEnabled()) return 1.0F;
        return Math.max(1.0F, zoomAnim.getValue());
    }

    public float getCurrentZoom() {
        return getEffectiveZoom();
    }

    @Override
    protected void onDisable() {
        stopZoom();
        wasKeyPressed = false;
        zoomAnim.setValue(1.0F);
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
