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
 * Energy AimAssist Rotation algorithm port (Util33).
 * Soft client assist interpolation with free crosshair movement when aimed.
 */
public final class EnergyAimAssistRotation implements AuraRotation {

    @Override
    public String getName() {
        return "AimAssist (En)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        AuraModule aura = AuraModule.INSTANCE;
        float power = aura != null ? aura.enAimAssistPower.getValue() : 0.5F;

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        // If player is already looking directly at target, keep smooth control
        boolean isLooking = RayTraceUtils.isLookingAt(target, aura != null ? aura.attackRange.getValue() : 3.0F, currentYaw, currentPitch);
        if (isLooking) {
            // Soft hold on target
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 delta = center.subtract(eyePos);

        float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        float assistSpeed = power * 28.0F;
        float stepYaw = Mth.clamp(deltaYaw * (power * 0.55F), -assistSpeed, assistSpeed);
        float stepPitch = Mth.clamp(deltaPitch * (power * 0.55F), -assistSpeed, assistSpeed);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
