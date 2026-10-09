package error.util.rotation;

import error.module.impl.combat.AuraModule;
import error.util.AuraRotation;
import error.util.RayTraceUtils;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Error ML Rotation algorithm port (Util107).
 * Machine learning spline prediction with optional only-hits locking.
 */
public final class ErrorMLRotation implements AuraRotation {

    @Override
    public String getName() {
        return "ML (Error)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        AuraModule aura = AuraModule.INSTANCE;
        boolean onlyHits = aura != null && aura.enOnlyHits.getValue();

        Vec3 eyePos = player.getEyePosition();
        Vec3 aimPos = target.getBoundingBox().getCenter().add(0.0, 0.12, 0.0);
        Vec3 delta = aimPos.subtract(eyePos);

        float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        boolean isAimed = RayTraceUtils.isLookingAt(target, aura != null ? aura.attackRange.getValue() : 3.0F, currentYaw, currentPitch);
        if (onlyHits && !isAimed && !attackLikely) {
            // Smoothly track without locking completely until attack window
            float softSpeed = 35.0F;
            float stepYaw = Mth.clamp(deltaYaw * 0.45F, -softSpeed, softSpeed);
            float stepPitch = Mth.clamp(deltaPitch * 0.45F, -softSpeed, softSpeed);
            RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
            return;
        }

        float speed = 85.0F;
        float stepYaw = Mth.clamp(deltaYaw * 0.85F, -speed, speed);
        float stepPitch = Mth.clamp(deltaPitch * 0.85F, -speed, speed);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
