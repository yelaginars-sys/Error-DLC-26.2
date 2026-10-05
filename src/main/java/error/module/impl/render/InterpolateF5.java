package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import net.minecraft.util.Mth;

public class InterpolateF5 extends Module {

    public static InterpolateF5 INSTANCE;

    private static final float SWITCH_ANIM_SPEED = 0.26F;
    private static final float DISTANCE_SPEED = 0.13F;
    private static final float ROTATION_SMOOTH = 0.28F;
    private static final float CAMERA_DISTANCE = 4.1F;
    private static final float SNEAK_OFFSET = 0.5F;
    private static final float JUMP_MULTIPLIER = 2.0F;
    private static final float ANIM_SPEED = 0.13F;

    private float currentDistance;
    private float prevDistance;
    private float currentYaw;
    private float prevYaw;
    private float currentPitch;
    private float prevPitch;
    private float heightOffset;
    private float prevHeightOffset;
    private boolean switchAnimating;
    private boolean wasThirdPerson;
    private boolean needsInit = true;

    public InterpolateF5() {
        super("Interpolate F5", "Плавная анимация камеры от третьего лица", Category.RENDER);
        INSTANCE = this;
    }

    public static InterpolateF5 getInstance() {
        return INSTANCE;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null) return;

        boolean isThirdPerson = !mc.options.getCameraType().isFirstPerson();

        if (isThirdPerson && !wasThirdPerson) initCamera(true);
        if (!isThirdPerson && wasThirdPerson) {
            needsInit = true;
            switchAnimating = false;
        }

        wasThirdPerson = isThirdPerson;
        if (isThirdPerson) updateCamera();
    }

    private void initCamera(boolean animateSwitch) {
        if (mc.player == null) return;

        currentYaw = prevYaw = getReferenceYaw();
        currentPitch = prevPitch = getReferencePitch();
        currentDistance = prevDistance = animateSwitch ? 0.0F : CAMERA_DISTANCE;
        heightOffset = prevHeightOffset = 0.0F;
        switchAnimating = animateSwitch;
        needsInit = false;
    }

    private void updateCamera() {
        if (mc.player == null) return;
        if (needsInit) {
            initCamera(true);
            return;
        }

        prevYaw = currentYaw;
        prevPitch = currentPitch;
        prevDistance = currentDistance;
        prevHeightOffset = heightOffset;

        float rotationSpeed = ROTATION_SMOOTH;

        currentYaw += Mth.wrapDegrees(getReferenceYaw() - currentYaw) * rotationSpeed;
        currentPitch = Mth.clamp(currentPitch + (getReferencePitch() - currentPitch) * rotationSpeed, -90.0F, 90.0F);

        float distanceSpeed = switchAnimating ? SWITCH_ANIM_SPEED : DISTANCE_SPEED;
        currentDistance += (CAMERA_DISTANCE - currentDistance) * distanceSpeed;
        if (switchAnimating && Math.abs(CAMERA_DISTANCE - currentDistance) <= 0.02F) {
            currentDistance = CAMERA_DISTANCE;
            switchAnimating = false;
        }

        float targetOffset = 0.0F;
        if (mc.player.isCrouching()) {
            targetOffset = -SNEAK_OFFSET;
        }
        if (!mc.player.onGround()) {
            targetOffset += (float) (-mc.player.getDeltaMovement().y * JUMP_MULTIPLIER);
        }

        heightOffset += (targetOffset - heightOffset) * ANIM_SPEED;
    }

    public float getInterpolatedYaw(float partialTicks) {
        if (mc.player == null) return 0.0F;
        return prevYaw + (currentYaw - prevYaw) * partialTicks;
    }

    public float getInterpolatedPitch(float partialTicks) {
        if (mc.player == null) return 0.0F;
        return Mth.clamp(prevPitch + (currentPitch - prevPitch) * partialTicks, -90.0F, 90.0F);
    }

    public float getInterpolatedDistance(float partialTicks) {
        return prevDistance + (currentDistance - prevDistance) * partialTicks;
    }

    public float getInterpolatedHeightOffset(float partialTicks) {
        return prevHeightOffset + (heightOffset - prevHeightOffset) * partialTicks;
    }

    private float getReferenceYaw() {
        return mc.player != null ? mc.player.getYRot() : 0.0F;
    }

    private float getReferencePitch() {
        return mc.player != null ? mc.player.getXRot() : 0.0F;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        needsInit = true;
        wasThirdPerson = false;

        if (mc.player != null && !mc.options.getCameraType().isFirstPerson()) {
            initCamera(true);
            wasThirdPerson = true;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        needsInit = true;
        heightOffset = 0.0F;
        prevHeightOffset = 0.0F;
    }
}
